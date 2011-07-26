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
package com.bt.aloha.collections.memory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.util.CloneableObject;
import com.bt.aloha.util.CollectionAccessInterruptedException;
import com.bt.aloha.util.ConcurrentUpdateException;
import com.bt.aloha.util.OptimisticConcurrencyCollection;
import com.bt.aloha.util.Versionable;

public class InMemoryCollectionImpl<T extends Versionable & CloneableObject<T>> implements
        OptimisticConcurrencyCollection<T> {
    public static final String FAILED_TO_READ_OBJECT_MESSAGE = "Failed to read info %s from collection %s: %s";
    public static final String FAILED_TO_REMOVE_OBJECT_MESSAGE = "Failed to remove info %s from collection %s: %s";

    private static Log log = LogFactory.getLog(InMemoryCollectionImpl.class);
    private ConcurrentMap<String, T> infos = new ConcurrentHashMap<String, T>();;
    private transient ConcurrentMap<String, Semaphore> semaphores;

    public InMemoryCollectionImpl() {
        super();
    }

    /**
     * Override this method if there's extra updates to do to the new Info
     * before we save it and release the Semaphore
     * 
     * @param info
     * @param newInfo
     */
    public void doExtraUpdates(T info, T newInfo) {

    }

    public T get(String infoId) {
        if (infoId == null)
            throw new IllegalArgumentException("Info id must not be null");

        Semaphore semaphore = getSemaphores().get(infoId);
        if (semaphore == null) {
            log.debug(String.format("No info object for %s in %s, returning null ", infoId, this.getClass()
                    .getSimpleName()));
            return null;
        }
        // log.debug("semaphores.size: " + getSemaphores().size());
        // log.debug("infos.size: " + infos.size());
        // log.debug("semaphore: " + semaphore.toString());

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error(String.format(FAILED_TO_READ_OBJECT_MESSAGE, infoId, this.getClass().getSimpleName(), e
                    .getMessage()));
            throw new CollectionAccessInterruptedException(String.format(FAILED_TO_READ_OBJECT_MESSAGE, infoId, this
                    .getClass().getSimpleName(), e.getMessage()), e);
        }
        try {
            if (infos.containsKey(infoId)) {
                T result = (T) infos.get(infoId).cloneObject();
                log.debug(String.format("Retrieved info %s with version %s", infoId, result.getVersionId()));
                return (T) result;
            }
            log.debug(String.format("No info object for %s in %s, returning null ", infoId, this.getClass()
                    .getSimpleName()));
            return null;
        } finally {
            semaphore.release();
        }
    }

    public void replace(T info) {
        if (info == null)
            throw new IllegalArgumentException(String.format(
                    "Trying to replace element in collection %s with null info", this.getClass().getSimpleName()));

        String infoId = info.getId();
        log.debug(String.format("InMemoryInfoCollection replacing %s", infoId));
        Semaphore semaphore = getSemaphores().get(infoId);
        if (semaphore == null)
            throw new IllegalArgumentException(String.format("Trying to replace non-existing info %s in collection %s",
                    infoId, this.getClass().getSimpleName()));

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error(String.format(FAILED_TO_READ_OBJECT_MESSAGE, infoId, this.getClass().getSimpleName(), e
                    .getMessage()), e);
            throw new CollectionAccessInterruptedException(String.format(FAILED_TO_READ_OBJECT_MESSAGE, infoId, this
                    .getClass().getSimpleName(), e.getMessage()), e);
        }

        try {
            T oldInfo = infos.get(infoId);
            if (!oldInfo.getVersionId().equals(info.getVersionId()))
                throw new ConcurrentUpdateException(infoId, String.format(
                        "Info %s modified in collection %s, try again", infoId, this.getClass().getSimpleName()));

            T newInfo = info.cloneObject();
            newInfo.updateVersionId();
            doExtraUpdates(info, newInfo);
            infos.put(infoId, (T) newInfo);
            info.setVersionId(newInfo.getVersionId());
            log.debug(String.format("Replaced info %s, new version %s", infoId, newInfo.getVersionId()));
        } finally {
            semaphore.release();
        }
    }

    public void add(T info) {
        if (info == null)
            throw new IllegalArgumentException(String.format("Trying to add element in collection %s with null info",
                    this.getClass().getSimpleName()));

        String infoId = info.getId();
        log.debug("infoId: " + infoId);
        if (getSemaphores().containsKey(infoId))
            throw new IllegalArgumentException(String.format(
                    "Info %s already exists in collection %s, use replaceDialog instead", infoId, this.getClass()
                            .getSimpleName()));

        infos.put(infoId, (T) info.cloneObject());
        getSemaphores().put(infoId, new Semaphore(1, true));
        log.debug(String.format("Added info %s to %s", info.getId(), this.getClass().getSimpleName()));
    }

    public void remove(String infoId) {
        Semaphore semaphore = getSemaphores().get(infoId);
        if (semaphore == null)
            return;

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error(String.format(FAILED_TO_REMOVE_OBJECT_MESSAGE, infoId, this.getClass().getSimpleName(), e
                    .getMessage()));
            throw new CollectionAccessInterruptedException(String.format(FAILED_TO_REMOVE_OBJECT_MESSAGE, infoId, this
                    .getClass().getSimpleName(), e.getMessage()), e);
        }
        try {
            if (infos.remove(infoId) != null) {
                getSemaphores().remove(infoId);
                log.info(String.format("Removed info %s", infoId));
            } else
                log.warn(String.format("Failed to find info %s", infoId));
        } finally {
            semaphore.release();
        }
    }

    public int size() {
        return infos.size();
    }

    public void destroy() {
        log.debug(String.format("Destroying %s", this.getClass().getSimpleName()));
    }

    public void init() {
        log.debug(String.format("Initialization of %s", this.getClass().getSimpleName()));
    }

    public ConcurrentMap<String, T> getAll() {
        return infos;
    }

    protected ConcurrentMap<String, Semaphore> getSemaphores() {
        if (semaphores == null) {
            semaphores = new ConcurrentHashMap<String, Semaphore>();
            for (String infoId : infos.keySet())
                semaphores.put(infoId, new Semaphore(1, true));
        }
        return semaphores;
    }
}
