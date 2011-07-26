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

 	

 	
 	
 
package com.bt.aloha.batchtest.ultramonkey;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.call.CallListener;
import com.bt.aloha.call.event.AbstractCallEvent;
import com.bt.aloha.call.event.CallConnectedEvent;
import com.bt.aloha.call.event.CallConnectionFailedEvent;
import com.bt.aloha.call.event.CallDisconnectedEvent;
import com.bt.aloha.call.event.CallTerminatedEvent;
import com.bt.aloha.call.event.CallTerminationFailedEvent;

public class SyncServiceImpl extends ServiceImpl implements CallListener {
	private static int TIMEOUT = 30; // seconds
	private static Log LOG = LogFactory.getLog(SyncServiceImpl.class);
	private Object makeCallLock = new Object();
	private Object terminateCallLock = new Object();
	private Map<String, Semaphore>callConnectedSemaphores = new Hashtable<String, Semaphore>();
	private Map<String, Semaphore>callTerminatedSemaphores = new Hashtable<String, Semaphore>();

	@Override
	public String makeCall(String caller, String callee) {
		String callId = null;
		synchronized (makeCallLock) {
			callId = super.makeCall(caller, callee);
			callConnectedSemaphores.put(callId, new Semaphore(0));
		}
		if (waitForCallConnectedEvent(callId)) {
			return callId;
		}
		throw new ServiceException("No call connected event received for callId " + callId);
	}

	@Override
	public void terminateCall(String callId) {
		synchronized (terminateCallLock) {
			super.terminateCall(callId);
			callTerminatedSemaphores.put(callId, new Semaphore(0));
		}
		if (waitForCallTerminatedEvent(callId)) {
			return;
		}
		throw new ServiceException("No call terminated event received for callId " + callId);
	}

	private boolean waitForCallConnectedEvent(String callId) {
		try {
			if (callConnectedSemaphores.get(callId).tryAcquire(TIMEOUT, TimeUnit.SECONDS)) {
				callConnectedSemaphores.remove(callId);
				return true;
			}
			return false;
		} catch (InterruptedException e) {
			throw new ServiceException("Timeout occurred while waiting for call event on callId " + callId, e);
		}
	}

	private boolean waitForCallTerminatedEvent(String callId) {
		try {
			if (callTerminatedSemaphores.get(callId).tryAcquire(TIMEOUT, TimeUnit.SECONDS)) {
				callTerminatedSemaphores.remove(callId);
				return true;
			}
			return false;
		} catch (InterruptedException e) {
			throw new ServiceException("Timeout occurred while waiting for call event on callId " + callId, e);
		}
	}

	private void onCallEvent(AbstractCallEvent event){
		LOG.debug(String.format("%s : %s", event.getClass().getSimpleName(), event.getCallId()));
		String callId = event.getCallId();
		if ((event instanceof CallConnectedEvent)) {
			synchronized (makeCallLock) {
				if (callConnectedSemaphores.containsKey(callId)) {
					callConnectedSemaphores.get(callId).release();
				}
			}
		}
		if ((event instanceof CallTerminatedEvent)) {
			synchronized (terminateCallLock) {
				if (callTerminatedSemaphores.containsKey(callId)) {
					callTerminatedSemaphores.get(callId).release();
				}
			}
		}
	}

	public void onCallConnected(CallConnectedEvent arg0) {
		onCallEvent(arg0);
	}

	public void onCallConnectionFailed(CallConnectionFailedEvent arg0) {
		onCallEvent(arg0);
	}

	public void onCallDisconnected(CallDisconnectedEvent arg0) {
		onCallEvent(arg0);
	}

	public void onCallTerminated(CallTerminatedEvent arg0) {
		onCallEvent(arg0);
	}

	public void onCallTerminationFailed(CallTerminationFailedEvent arg0) {
		onCallEvent(arg0);
	}
}
