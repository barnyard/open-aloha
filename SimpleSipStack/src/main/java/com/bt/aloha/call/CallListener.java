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

 	

 	
 	
 
package com.bt.aloha.call;

import com.bt.aloha.call.event.CallConnectedEvent;
import com.bt.aloha.call.event.CallConnectionFailedEvent;
import com.bt.aloha.call.event.CallDisconnectedEvent;
import com.bt.aloha.call.event.CallTerminatedEvent;
import com.bt.aloha.call.event.CallTerminationFailedEvent;

/**
 * To listen to call events you must implement this interface and then add class to the CallBean listener list.
 */
public interface CallListener {
    /**
     * Fired when a call is connected
     * @param callConnectedEvent details of the event
     */
	void onCallConnected(CallConnectedEvent callConnectedEvent);

	/**
     * Fired when a call connection fails
     * @param callConectionFailedEvent details of the event
	 */
    void onCallConnectionFailed(CallConnectionFailedEvent callConectionFailedEvent);

	/**
     * Fired when a call is disconnected.
     * @param callDisconnectedEvent details of the event
	 */
    void onCallDisconnected(CallDisconnectedEvent callDisconnectedEvent);

    /**
     * Fired when a call is terminated
     * @param callTerminatedEvent details of the event
     */
	void onCallTerminated(CallTerminatedEvent callTerminatedEvent);

    /**
     * Fired when a call termination fails
     * @param callTerminationFailedEvent details of the event
     */
	void onCallTerminationFailed(CallTerminationFailedEvent callTerminationFailedEvent);
}
