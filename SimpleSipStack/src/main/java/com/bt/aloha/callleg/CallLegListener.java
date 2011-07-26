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
package com.bt.aloha.callleg;

import com.bt.aloha.callleg.event.CallLegConnectedEvent;
import com.bt.aloha.callleg.event.CallLegConnectionFailedEvent;
import com.bt.aloha.callleg.event.CallLegDisconnectedEvent;
import com.bt.aloha.callleg.event.CallLegRefreshCompletedEvent;
import com.bt.aloha.callleg.event.CallLegTerminatedEvent;
import com.bt.aloha.callleg.event.CallLegTerminationFailedEvent;
import com.bt.aloha.callleg.event.ReceivedCallLegRefreshEvent;

/**
 * To listen to call leg events you must implement this interface and then add class to the CallLegBean listener list.
 */
public interface CallLegListener {
    /**
     * Event fired when the call leg is connected
     * @param connectedEvent details of the event
     */
	void onCallLegConnected(CallLegConnectedEvent connectedEvent);

    /**
     * Event fired when the call leg connection fails
     * @param connectionFailedEvent details of the event
     */
	void onCallLegConnectionFailed(CallLegConnectionFailedEvent connectionFailedEvent);

    /**
     * Event fired when the call leg is disconnected
     * @param disconnectedEvent details of the event
     */
	void onCallLegDisconnected(CallLegDisconnectedEvent disconnectedEvent);

    /**
     * Event fired when the call leg is terminated
     * @param terminatedEvent details of the event
     */
	void onCallLegTerminated(CallLegTerminatedEvent terminatedEvent);

    /**
     * Event fired when the call leg termination fails
     * @param terminationFailedEvent details of the event
     */
	void onCallLegTerminationFailed(CallLegTerminationFailedEvent terminationFailedEvent);

    /**
     * Event fired when the call leg refresh is completed
     * @param callLegConnectedEvent details of the event
     */
	void onCallLegRefreshCompleted(CallLegRefreshCompletedEvent callLegConnectedEvent);

    /**
     * Event fired when the call leg refresh is received from the remote party
     * @param receivedCallLegRefreshEvent details of the event
     */
	void onReceivedCallLegRefresh(ReceivedCallLegRefreshEvent receivedCallLegRefreshEvent);
}
