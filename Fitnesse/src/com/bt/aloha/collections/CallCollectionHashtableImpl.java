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

 	

 	
 	
 
package com.bt.aloha.collections;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.call.state.CallInfo;

public class CallCollectionHashtableImpl implements CallCollection {
    private static Hashtable<String, CallInfo> callInfos = new Hashtable<String, CallInfo>();

	public void add(CallInfo callInfo) {
		callInfos.put(callInfo.getId(), callInfo);
	}

    public void replace(CallInfo callInfo) {
        add(callInfo);
    }

	public CallInfo get(String callId) {
		return callInfos.get(callId);
	}

	public CallInfo getCurrentCallForCallLeg(String dialogId) {
		return getCurrentCallForCallLeg(dialogId, null);
	}

	public CallInfo getCurrentCallForCallLeg(String dialogId, String callIdToIgnore) {
        CallInfo mostRecentCallInfo = null;
        long mostRecentCallCreateTime = 0;
    	Set<String> keySet = callInfos.keySet();
        if (keySet != null) {
            Iterator<String> iter = keySet.iterator();
            while (iter.hasNext()) {
            	String callId = iter.next();
            	if (callId.equals(callIdToIgnore))
            		continue;
            	
	            CallInfo callInfo = callInfos.get(callId);
	            if (dialogId.equals(callInfo.getFirstDialogId()) || dialogId.equals(callInfo.getSecondDialogId())) {
	                long createTime = callInfo.getCreateTime();
	                if(createTime > mostRecentCallCreateTime) {
	                	mostRecentCallInfo = callInfo;
	                	mostRecentCallCreateTime = createTime;
	                }
	            }
            }
        }
        if(mostRecentCallInfo != null)
        	return mostRecentCallInfo;
    	return null;
	}

	public void init() {
	}

	public void destroy() {
	}

	public void setHousekeepingInterval(long arg0) {

	}

	public int size() {
		return callInfos.size();
	}

	public void housekeep() {
		
	}

	public ConcurrentMap<String, CallInfo> getAll() {
		return null;
	}

	public void setMaxTimeToLive(long aMaxTimeToLive) {
	}

	public void remove(String id) {
	}

    public ConcurrentMap<String, CallInfo> getAllConnectedCallsWithMaxDuration() {
        // TODO Auto-generated method stub
        return null;
    }

	public long getNumberOfConnectingCalls() {
		// TODO Auto-generated method stub
		return 0;
	}
}
