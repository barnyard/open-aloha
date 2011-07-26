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

 	

 	
 	
 
package com.bt.aloha.batchtest;

import java.util.Vector;
import java.util.concurrent.ConcurrentMap;

import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.call.state.CallInfo;
import com.bt.aloha.util.CollectionAccessInterruptedException;
import com.bt.aloha.util.ConcurrentUpdateException;

public class CallCollectionImplWithStats implements CallCollection {
	private Vector<String> invocationCounter;
	private Vector<String> exceptionCounter;
    private CallCollection delegate;

	public CallCollectionImplWithStats(CallCollection aDelegate) {
		this.delegate = aDelegate;

		invocationCounter = new Vector<String>();
		exceptionCounter = new Vector<String>();
	}

	public void replace(CallInfo callInfo) {
		try {
			invocationCounter.add(callInfo.getId());
			delegate.replace(callInfo);
		} catch(ConcurrentUpdateException e) {
			exceptionCounter.add(callInfo.getId());
			throw e;
		}
	}

	public int getExceptionCounterSize() {
		return exceptionCounter.size();
	}

	public int getInvocationCounterSize() {
		return invocationCounter.size();
	}

    public void reset(){
        this.invocationCounter.clear();
        this.exceptionCounter.clear();
    }

    public CallInfo getCurrentCallForCallLeg(String callLegId) {
        return delegate.getCurrentCallForCallLeg(callLegId);
    }

    public CallInfo getCurrentCallForCallLeg(String callLegId, String callIdToIgnore) {
        return delegate.getCurrentCallForCallLeg(callLegId, callIdToIgnore);
    }

    public void add(CallInfo object) throws IllegalArgumentException {
        delegate.add(object);
    }

    public void destroy() {
        delegate.destroy();
    }

    public CallInfo get(String id) throws CollectionAccessInterruptedException {
        return delegate.get(id);
    }

    public ConcurrentMap<String, CallInfo> getAll() {
        return delegate.getAll();
    }

    public void init() {
        delegate.init();
    }

    public void remove(String id) throws CollectionAccessInterruptedException {
        delegate.remove(id);
    }

    public int size() {
        return delegate.size();
    }

    public void housekeep() {
        delegate.housekeep();
    }

    public void setMaxTimeToLive(long aMaxTimeToLive) {
        delegate.setMaxTimeToLive(aMaxTimeToLive);
    }

    public ConcurrentMap<String, CallInfo> getAllConnectedCallsWithMaxDuration() {
        return delegate.getAllConnectedCallsWithMaxDuration();
    }

	public long getNumberOfConnectingCalls() {
		return delegate.getNumberOfConnectingCalls();
	}
}
