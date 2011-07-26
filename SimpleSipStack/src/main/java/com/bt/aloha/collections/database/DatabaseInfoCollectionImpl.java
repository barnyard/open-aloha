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

 	

 	
 	
 
package com.bt.aloha.collections.database;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import com.bt.aloha.dao.StateInfoDao;
import com.bt.aloha.state.StateInfoBase;
import com.bt.aloha.state.TransientInfo;
import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateManager;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;
import com.bt.aloha.util.HousekeeperOptimisticConcurrencyCollection;
import com.bt.aloha.util.OptimisticConcurrencyCollection;

public class DatabaseInfoCollectionImpl<T extends StateInfoBase<T>> implements HousekeeperOptimisticConcurrencyCollection<T> {
    protected static final int DEFAULT_TIMEOUT = 1440 * 60 * 1000;

    private StateInfoDao<T> collectionsDao = null;
    private ConcurrentUpdateManager concurrentUpdateManager = new ConcurrentUpdateManagerImpl();
    private long maxTimeToLive = DEFAULT_TIMEOUT;
    private String collectionTypeName;
    private DatabaseInfoCollectionHousekeepingRowCallBackHandler rowCallBackHandler;
    // TODO: why can't this just be a hashtable / hashmap? We only want an in-memory reference,
    // if we have one, and don't need to enforce concurrency constraints
    private OptimisticConcurrencyCollection<TransientInfo> transients;

    public DatabaseInfoCollectionImpl(HousekeeperOptimisticConcurrencyCollection<TransientInfo> transientsCollection, String aCollectionTypeName) {
        this.transients = transientsCollection;
        this.collectionTypeName = aCollectionTypeName;
    }

    public void add(T info) {
    	collectionsDao.add(info, collectionTypeName);
        Map<String, Object> infoTransients = info.getTransients();
        if (null != infoTransients && infoTransients.size() > 0) {
            this.transients.add(new TransientInfo(info.getId(), infoTransients));
        }
    }

    public T get(String infoId) {
    	T result = collectionsDao.get(infoId);
    	if (null == result) return result;
        TransientInfo transientInfo = this.transients.get(infoId);
        if (null != transientInfo) {
           result.setTransients(transientInfo.getTransientsMap());
        }
        return result;
    }


    public void remove(String infoId) {
    	collectionsDao.remove(infoId);
        this.transients.remove(infoId);
    }

    public void replace(final T info) {
    	collectionsDao.replace(info);
        final Map<String, Object> infoTransients = info.getTransients();
        if (null != infoTransients && infoTransients.size() > 0) {
            ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
                public void execute() {
                    TransientInfo transientInfo = transients.get(info.getId());
                    if(transientInfo!=null){
                        transientInfo.setTransientMap(infoTransients);
                        transients.replace(transientInfo);
                    }
                }

                public String getResourceId() {
                    return info.getId();
                }
            };
            concurrentUpdateManager.executeConcurrentUpdate(concurrentUpdateBlock);
        }
//        info.setVersionId(newInfo.getVersionId());
//        info.setLastUsedTime(newInfo.getLastUsedTime());
    }

    public void setMaxTimeToLive(long aMaxTimeToLive) {
        this.maxTimeToLive = aMaxTimeToLive;
    }

    public int size() {
        return collectionsDao.size(collectionTypeName);
    }

    public void housekeep() {
    	collectionsDao.housekeep(collectionTypeName, maxTimeToLive, rowCallBackHandler);
    }

    public void init() {
    }

    public void destroy() {
        this.transients.destroy();
    }

    public ConcurrentMap<String, T> getAll() {
        ConcurrentMap<String, T> map = collectionsDao.getAll(collectionTypeName);
        for (T item : map.values()) {
            TransientInfo transientInfo = this.transients.get(item.getId());
            if (null != transientInfo) {
               item.setTransients(transientInfo.getTransientsMap());
            }
        }
        return map;
    }

	public void setCollectionsDao(StateInfoDao<T> aCollectionsDao) {
		this.collectionsDao = aCollectionsDao;
	}

	public void setRowCallBackHandler(DatabaseInfoCollectionHousekeepingRowCallBackHandler aRowCallBackHandler) {
		this.rowCallBackHandler = aRowCallBackHandler;
	}
}
