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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.call.state.CallInfo;
import com.bt.aloha.call.state.CallState;
import com.bt.aloha.call.state.ImmutableCallInfo;
import com.bt.aloha.util.HousekeeperOptimisticConcurrencyCollection;

public class CallCollectionImpl implements CallCollection {
    private static Log log = LogFactory.getLog(CallCollectionImpl.class);
	private HousekeeperOptimisticConcurrencyCollection<CallInfo> collection;

    public CallCollectionImpl(HousekeeperOptimisticConcurrencyCollection<CallInfo> aCollection) {
    	log.debug(String.format("Creating CallCollection with %s ", aCollection.getClass().getSimpleName()));
    	this.collection = aCollection;
    	log.debug("collection size:" + this.collection.size());
    }

    public CallInfo getCurrentCallForCallLeg(String dialogId) {
    	return getCurrentCallForCallLeg(dialogId, null);
    }

    public CallInfo getCurrentCallForCallLeg(String dialogId, String callIdToIgnore) {
    	log.debug(String.format("Getting current call from CallCollection for dialog %s, ignoring call id %s", dialogId, callIdToIgnore));
        String mostRecentCallId = null;
        long mostRecentCallCreateTime = 0;
        ConcurrentMap<String, CallInfo> allCallInfos = getAll();
		for (Map.Entry<String, CallInfo> entry: allCallInfos.entrySet())
        {
		    if (entry.getKey().equals(callIdToIgnore))
                continue;
            ImmutableCallInfo callInfo = entry.getValue();
            if (dialogId.equals(callInfo.getFirstDialogId()) || dialogId.equals(callInfo.getSecondDialogId())) {
                long createTime = callInfo.getCreateTime();
                if (createTime > mostRecentCallCreateTime) {
                    mostRecentCallId = entry.getKey();
                    mostRecentCallCreateTime = createTime;
                }
            }

        }

        if (mostRecentCallId != null) {
            CallInfo result = collection.get(mostRecentCallId);
            if (null != result)
            	log.debug(String.format("Current call for dialog %s is %s, containing first dialogId %s and second dialog %s", dialogId, mostRecentCallId, result.getFirstDialogId(), result.getSecondDialogId()));
            else
            	log.debug(String.format("Current call (%s) for dialog %s not found, returning null", mostRecentCallId, dialogId));
            return result;
        } else {
            log.debug(String.format("Dialog %s not in a call", dialogId));
            return null;
        }
    }

	public void add(CallInfo callInfo) {
    	log.debug(String.format("Adding call %s to CallCollection ", callInfo.getId()));
		collection.add(callInfo);
	}

	public void destroy() {
		log.debug(String.format("Destroying CallCollection"));
		collection.destroy();
	}

	public CallInfo get(String callId) {
    	log.debug(String.format("Getting call %s from CallCollection", callId));
		log.debug("collection.getAll().size(): " + collection.getAll().size());
    	return collection.get(callId);
	}

	public void remove(String id) {
		log.debug(String.format("Removing call %s from CallCollection", id));
		collection.remove(id);
	}

	public void init() {
		log.debug(String.format("Initialization of CallCollection"));
		collection.init();
	}

	public void replace(CallInfo callInfo) {
		String callId = callInfo != null ? callInfo.getId(): "null";
		log.debug(String.format("Replacing call %s in CallCollection", callId));
		collection.replace(callInfo);
	}

	public void setMaxTimeToLive(long aCallMaxTimeToLive) {
		log.debug(String.format("Setting Max TTL in CallCollection to %d", aCallMaxTimeToLive));
		collection.setMaxTimeToLive(aCallMaxTimeToLive);
	}

	public int size() {
		return collection.size();
	}

	public void housekeep() {
		log.debug("HouseKeeping CallCollection");
		collection.housekeep();
	}

	public ConcurrentMap<String, CallInfo> getAll() {
		return collection.getAll();
	}

	public ConcurrentMap<String, CallInfo> getAllConnectedCallsWithMaxDuration() {
        ConcurrentMap<String, CallInfo> result = new ConcurrentHashMap<String, CallInfo>();
	    for(Map.Entry<String, CallInfo> entry: getAll().entrySet())
	        if (entry.getValue().getCallState().equals(CallState.Connected) && entry.getValue().getMaxDurationInMinutes() > 0)
	            result.put(entry.getKey(), entry.getValue());
	    return result;
    }

	public long getNumberOfConnectingCalls() {
		long counter = 0;
	    for(Map.Entry<String, CallInfo> entry: getAll().entrySet()) {
			CallState callState = entry.getValue().getCallState();
			if (CallState.Connecting.equals(callState))
				counter++;
		}
	    return counter;
	}
}
