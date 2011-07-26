/*
 * Aloha Open Source SIP Application Server- https://trac.osmosoft.com/Aloha
 *
 * Copyright (c) 2008, British Telecommunications plc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 3.0 of the License, or (at your option) any later
 * version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/**
 * (c) British Telecommunications plc, 2007, All Rights Reserved
 */
package com.bt.sdk.callcontrol.sip.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EhCacheCollectionImpl<T extends Versionable & CloneableObject<T>> implements OptimisticConcurrencyCollection<T> {
    public static final String FAILED_TO_READ_OBJECT_MESSAGE = "Failed to read info %s from collection %s: %s";
    public static final String FAILED_TO_REMOVE_OBJECT_MESSAGE = "Failed to remove info %s from collection %s: %s";

    private Log log = LogFactory.getLog(this.getClass());
    private Cache cache;
    private Cache semaphoreCache;
    private String cacheName;

    static {
        CacheManager.create();
    }

	public EhCacheCollectionImpl(String theCacheName) {
		super();
        this.cacheName = theCacheName;
		if ( ! CacheManager.getInstance().cacheExists(cacheName)) {
            Cache persistentCache = new Cache(cacheName, 100, true, true, 60*60, 60*60, true, 100);
		    CacheManager.getInstance().addCache(persistentCache);
		}
		cache = CacheManager.getInstance().getCache(cacheName);

        if ( ! CacheManager.getInstance().cacheExists(cacheName + "Semaphore")) {
            Cache persistentCache = new Cache(cacheName + "Semaphore", 100, true, true, 60*60, 60*60, true, 100);
            CacheManager.getInstance().addCache(persistentCache);
		}
        semaphoreCache = CacheManager.getInstance().getCache(cacheName + "Semaphore");
	}

	/**
	 * Override this method if there's extra updates to do to the new Info before we save it and release the Semaphore
	 * @param info
	 * @param newInfo
	 */
	public void doExtraUpdates(T info, T newInfo) {

	}

	@SuppressWarnings("unchecked")
	public T get(String infoId) {
    	if (infoId == null)
    		throw new IllegalArgumentException("Info id must not be null");

        Semaphore semaphore = (Semaphore)semaphoreCache.get(infoId).getObjectValue();
		if (semaphore == null) {
	    	log.debug(String.format("No info object for %s in %s, returning null ", infoId, this.getClass().getSimpleName()));
			return null;
		}

    	try {
			semaphore.acquire();
		} catch (InterruptedException e) {
			log.error(String.format(FAILED_TO_READ_OBJECT_MESSAGE, infoId, this.getClass().getSimpleName(), e.getMessage()));
            throw new CollectionAccessInterruptedException(String.format(FAILED_TO_READ_OBJECT_MESSAGE, infoId, this.getClass().getSimpleName(), e.getMessage()), e);
		}
		try {
            T result = ((T)cache.get(infoId).getObjectValue()).cloneObject();

            log.debug(String.format("Retrieved info %s with version %s", infoId, result.getVersionId()));
	    	return (T) result;
		} finally {
			semaphore.release();
		}
    }

    @SuppressWarnings("unchecked")
	public void replace(T info) {
    	if (info == null)
    		throw new IllegalArgumentException(String.format("Trying to replace element in collection %s with null info", this.getClass().getSimpleName()));

    	String infoId = info.getId();
    	log.debug(String.format("InMemoryInfoCollection replacing %s", infoId));
        if ( ! semaphoreCache.getKeys().contains(infoId))
            throw new IllegalArgumentException(String.format("Trying to replace non-existing info %s in collection %s", infoId, this.getClass().getSimpleName()));
        Semaphore semaphore = (Semaphore)semaphoreCache.get(infoId).getObjectValue();
		if (semaphore == null)
    		throw new IllegalArgumentException(String.format("Trying to replace non-existing info %s in collection %s", infoId, this.getClass().getSimpleName()));

    	try {
			semaphore.acquire();
		} catch (InterruptedException e) {
			log.error(String.format(FAILED_TO_READ_OBJECT_MESSAGE, infoId, this.getClass().getSimpleName(), e.getMessage()), e);
			throw new CollectionAccessInterruptedException(String.format(FAILED_TO_READ_OBJECT_MESSAGE, infoId, this.getClass().getSimpleName(), e.getMessage()), e);
		}

		try {
			//T oldInfo = infos.get(infoId);
            T oldInfo = (T)cache.get(infoId).getObjectValue();
			if ( ! oldInfo.getVersionId().equals(info.getVersionId()))
				throw new ConcurrentUpdateException(infoId, String.format("Info %s modified in collection %s, try again", infoId, this.getClass().getSimpleName()));

			T newInfo = info.cloneObject();
			newInfo.updateVersionId();
			doExtraUpdates(info, newInfo);
			//infos.put(infoId, (T)newInfo);
			cache.put(new Element(infoId, newInfo));
            info.setVersionId(newInfo.getVersionId());
	    	log.debug(String.format("Replaced info %s, new version %s", infoId, newInfo.getVersionId()));
		} finally {
			semaphore.release();
		}
    }

    @SuppressWarnings("unchecked")
	public void add(T info) {
    	if (info == null)
    		throw new IllegalArgumentException(String.format("Trying to add element in collection %s with null info", this.getClass().getSimpleName()));

    	String infoId = info.getId();
        if (semaphoreCache.getKeys().contains(infoId))
    		throw new IllegalArgumentException(String.format("Info %s already exists in collection %s, use replaceDialog instead", infoId, this.getClass().getSimpleName()));

    	//infos.put(infoId, (T)info.cloneObject());
        cache.put(new Element(infoId, info.cloneObject()));
        cache.flush();
        semaphoreCache.put(new Element(infoId, new Semaphore(1, true)));
        semaphoreCache.flush();
    	log.debug(String.format("Added info %s to %s", info.getId(), this.getClass().getSimpleName()));
    }

    public void remove(String infoId) {
        if ( ! semaphoreCache.getKeys().contains(infoId))
            return;
        Semaphore semaphore = (Semaphore)semaphoreCache.get(infoId).getObjectValue();
		if (semaphore == null)
			return;

    	try {
    		semaphore.acquire();
    	} catch (InterruptedException e) {
			log.error(String.format(FAILED_TO_REMOVE_OBJECT_MESSAGE, infoId, this.getClass().getSimpleName(), e.getMessage()));
            throw new CollectionAccessInterruptedException(String.format(FAILED_TO_REMOVE_OBJECT_MESSAGE, infoId, this.getClass().getSimpleName(), e.getMessage()), e);
    	}
    	try {
            boolean b = cache.remove(infoId);
            System.out.println(b + ":" + infoId);
                if (b) {
                    semaphoreCache.remove(infoId);
                log.info(String.format("Removed info %s", infoId));
            }
            else
                log.warn(String.format("Failed to find info %s", infoId));
		} finally {
			semaphore.release();
		}
    }

	public int size() {
        return cache.getKeys().size();
    }

	public void destroy() {
		log.debug(String.format("Destroying %s", this.getClass().getSimpleName()));
	}

	public void init() {
		log.debug(String.format("Initialization of %s", this.getClass().getSimpleName()));
	}

	@SuppressWarnings("unchecked")
    public ConcurrentMap<String, T> getAll() {
        ConcurrentMap<String, T> result = new ConcurrentHashMap<String, T>();
        for (Object key: cache.getKeys()) {
            result.put((String)key, (T)cache.get(key).getObjectValue());
        }
        return result;
	}

	protected ConcurrentMap<String, Semaphore> getSemaphores() {
        ConcurrentMap<String, Semaphore> result = new ConcurrentHashMap<String, Semaphore>();
        for (Object key: semaphoreCache.getKeys()) {
            result.put((String)key, (Semaphore)cache.get(key).getObjectValue());
        }
        return result;
	}
}
