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

 	

 	
 	
 
package com.bt.aloha.collections.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.bt.aloha.state.StateInfoBase;
import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateManager;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;
import com.bt.aloha.util.Housekeepable;
import com.bt.aloha.util.HousekeeperAware;
import com.bt.aloha.util.HousekeeperOptimisticConcurrencyCollection;

public class InMemoryHousekeepingCollectionImpl<T extends StateInfoBase<T>> extends InMemoryCollectionImpl<T> implements HousekeeperOptimisticConcurrencyCollection<T>, ApplicationContextAware {

    protected static final int DEFAULT_TIMEOUT = 1440 * 60 * 1000;

    private static Log log = LogFactory.getLog(InMemoryHousekeepingCollectionImpl.class);

    private transient long maxTimeToLive = DEFAULT_TIMEOUT;

	private transient ApplicationContext applicationContext = null;

	private transient ConcurrentUpdateManager concurrentUpdateManager = new ConcurrentUpdateManagerImpl();

    public InMemoryHousekeepingCollectionImpl () {
    	super();
    }

	public void setApplicationContext(ApplicationContext aApplicationContext) {
		this.applicationContext = aApplicationContext;
	}

    @Override
	public void doExtraUpdates(T info, T newInfo) {
		super.doExtraUpdates(info, newInfo);
		newInfo.updateLastUsedTime();
		info.setLastUsedTime(newInfo.getLastUsedTime());
	}

	public void housekeep() {
        try {
            log.info(String.format("Housekeeping: info table size is %d", size()));

            List<String> expiredNotDead = new ArrayList<String>();
            for (String id : getAll().keySet()) {
                Semaphore s = getSemaphores().get(id);
            	s.acquire();
                try {
                    T info = getAll().get(id);
                    log.info(String.format("Housekeeping: trying to housekeep info %s (forced=%s, expired=%s, dead=%s, TTL=%d)", id, info.isHousekeepForced(), isExpired(info), info.isDead(), maxTimeToLive));
                    if (isExpired(info) || info.isHousekeepForced())
                    	if (info.isDead() || info.isHousekeepForced()) {
                    		log.info(String.format("Housekeeping: found an 'old' info, removing: %s",id));
                    		getAll().remove(id);
                    		getSemaphores().remove(id);
                    	} else
                    		expiredNotDead.add(id);
                    else
                    	log.debug(String.format("Housekeeping: info %s has not expired yet and will not be housekept",id));
                } catch (Throwable t) {
                    log.error("Error processing Info object during housekeeping: ", t);
                } finally {
                    s.release();
                }
            }
            // if we have expired objects which are not dead then prepare them for housekeeping
            for (final String expiredInfoId : expiredNotDead) {
            	try {
	            	T expiredInfo = get(expiredInfoId);
	            	String beanName = expiredInfo.getSimpleSipBeanId();
	            	log.info(String.format("Housekeeping: preparing an info %s for being housekept by %s", expiredInfoId, beanName));
	            	HousekeeperAware creatorBean = (HousekeeperAware) applicationContext.getBean(beanName);
            		creatorBean.killHousekeeperCandidate(expiredInfoId);
            	} catch (Throwable t) {
            		log.error(String.format("Unable to kill housekeeper candidate %s...will still remove from collection next housekeep", expiredInfoId), t);
            	} finally {
            		//force housekeeping next time for that object
            		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
						public void execute() {
							log.info(String.format("Housekeeping: setting housekeepForced flag to true in info %s", expiredInfoId));
							T info = get(expiredInfoId);
							info.setHousekeepForced(true);
							replace(info);
						}
						public String getResourceId() {
							return expiredInfoId;
						}
					};
					concurrentUpdateManager.executeConcurrentUpdate(concurrentUpdateBlock);
            	}
            }
        } catch (Throwable t) {
            log.error("Error processing timer callback", t);
        }
    }

    public void setMaxTimeToLive(long aMaxTimeToLive) {
        this.maxTimeToLive = aMaxTimeToLive;
    }

    private boolean isExpired(Housekeepable info){
        long lastUseTime = info.getLastUsedTime();
        long period = System.currentTimeMillis() - lastUseTime;
        if (period > maxTimeToLive) return true;
        return false;
    }
}

