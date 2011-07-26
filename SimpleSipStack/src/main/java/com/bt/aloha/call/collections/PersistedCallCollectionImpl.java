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

 	

 	
 	
 
package com.bt.aloha.call.collections;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.bt.aloha.call.state.CallInfo;
import com.bt.aloha.call.state.CallState;
import com.bt.aloha.dao.CallInfoDao;
import com.bt.aloha.util.CollectionHelper;

public class PersistedCallCollectionImpl implements CallCollection {

    private CallInfoDao callInfoDao;
	private PersistedCallCollectionHousekeepingRowCallbackHandler rowCallbackHandler;
	private long maxTimeToLive;
	private ConcurrentMap<String, Map<String, Object>> transients = new ConcurrentHashMap<String, Map<String, Object>>();

    public PersistedCallCollectionImpl(CallInfoDao aCallInfoDao) {
        this.callInfoDao = aCallInfoDao;
    }

    private CallInfo readTransients(CallInfo result) {
    	if (null == result) return null;
    	if (transients.containsKey(result.getId()))
    		result.setTransients(transients.get(result.getId()));
    	return result;
    }

    public CallInfo getCurrentCallForCallLeg(String callLegId) {
        CallInfo result = callInfoDao.findCallForDialogId(callLegId);
        return readTransients(result);
    }

    public CallInfo getCurrentCallForCallLeg(String callLegId, String callIdToIgnore) {
        CallInfo result = callInfoDao.findCallForDialogId(callLegId, callIdToIgnore);
        return readTransients(result);
    }

    public void add(CallInfo callInfo) {
    	if (null == callInfo)
    		throw new IllegalArgumentException("callInfo cannot be null");
    	transients.put(callInfo.getId(), callInfo.getTransients());
        callInfoDao.create(callInfo);
    }

    public void destroy() {
    	CollectionHelper.destroy(transients, this.getClass().getSimpleName());
    }

    public CallInfo get(String id) {
        CallInfo result = callInfoDao.read(id);
        return readTransients(result);
    }

    public ConcurrentMap<String, CallInfo> getAll() {
    	ConcurrentMap<String, CallInfo> result = callInfoDao.getAll();

    	for (CallInfo callInfo: result.values())
    		readTransients(callInfo);
    	
    	return result;
    }

    public void init() {
    	throw new UnsupportedOperationException();
    }

    public void remove(String id) {
    	transients.remove(id);
        callInfoDao.delete(id);
    }

    public void replace(CallInfo callInfo) {
    	transients.put(callInfo.getId(), callInfo.getTransients());
        callInfoDao.update(callInfo);
    }

    public int size() {
        return callInfoDao.size();
    }

    public int sizeTransients() {
    	if(transients!=null)
    		return transients.size();
    	return 0;
    }

    public void housekeep() {
    	List<String> transientsToBeCleaned = callInfoDao.findByHousekeeperFlags(Calendar.getInstance().getTimeInMillis() - maxTimeToLive);
    	callInfoDao.deleteByHousekeeperFlags(Calendar.getInstance().getTimeInMillis() - maxTimeToLive);
    	for(String callId: transientsToBeCleaned){
    		transients.remove(callId);
    	}
    	callInfoDao.updateByHousekeeperFlags(Calendar.getInstance().getTimeInMillis() - maxTimeToLive, this.rowCallbackHandler);
    }

    public void setMaxTimeToLive(long aMaxTimeToLive) {
        this.maxTimeToLive = aMaxTimeToLive;
    }

	public void setPersistedCallCollectionHousekeepingRowCallbackHandler(PersistedCallCollectionHousekeepingRowCallbackHandler aRowCallbackHandler) {
		this.rowCallbackHandler = aRowCallbackHandler;
		this.rowCallbackHandler.setCallCollection(this);
	}

    public ConcurrentMap<String, CallInfo> getAllConnectedCallsWithMaxDuration() {
    	ConcurrentMap<String, CallInfo> result = callInfoDao.findConnectedMaxDurationCalls();

    	for (CallInfo callInfo: result.values())
    		readTransients(callInfo);
    	
    	return result;
    }

	protected ConcurrentMap<String, Map<String, Object>> getTransients() {
		return transients;
	}

	public long getNumberOfConnectingCalls() {
		return callInfoDao.countByCallState(CallState.Connecting, transients.keySet());
	}
}
