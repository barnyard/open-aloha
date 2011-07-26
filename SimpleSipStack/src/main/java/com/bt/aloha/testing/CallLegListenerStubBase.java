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
package com.bt.aloha.testing;

import com.bt.aloha.callleg.CallLegListener;
import com.bt.aloha.callleg.event.CallLegConnectedEvent;
import com.bt.aloha.callleg.event.CallLegConnectionFailedEvent;
import com.bt.aloha.callleg.event.CallLegDisconnectedEvent;
import com.bt.aloha.callleg.event.CallLegRefreshCompletedEvent;
import com.bt.aloha.callleg.event.CallLegTerminatedEvent;
import com.bt.aloha.callleg.event.CallLegTerminationFailedEvent;
import com.bt.aloha.callleg.event.ReceivedCallLegRefreshEvent;

public class CallLegListenerStubBase implements CallLegListener {
	public CallLegListenerStubBase() {
		super();
	}
	public void onCallLegConnected(CallLegConnectedEvent connectedEvent) {
	}
	public void onCallLegConnectionFailed(CallLegConnectionFailedEvent connectionFailedEvent) {
	}
	public void onCallLegDisconnected(CallLegDisconnectedEvent disconnectedEvent) {
	}
	public void onCallLegRefreshCompleted(CallLegRefreshCompletedEvent callLegConnectedEvent) {
	}
	public void onCallLegTerminated(CallLegTerminatedEvent terminatedEvent) {
	}
	public void onCallLegTerminationFailed(CallLegTerminationFailedEvent terminationFailedEvent) {
	}
	public void onReceivedCallLegRefresh(ReceivedCallLegRefreshEvent receivedCallLegRefreshEvent) {
	}
}
