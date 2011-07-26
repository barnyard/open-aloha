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

 	

 	
 	
 
package com.bt.aloha.dialog;

import com.bt.aloha.dialog.event.DialogConnectedEvent;
import com.bt.aloha.dialog.event.DialogConnectionFailedEvent;
import com.bt.aloha.dialog.event.DialogDisconnectedEvent;
import com.bt.aloha.dialog.event.DialogRefreshCompletedEvent;
import com.bt.aloha.dialog.event.DialogTerminatedEvent;
import com.bt.aloha.dialog.event.DialogTerminationFailedEvent;
import com.bt.aloha.dialog.event.ReceivedDialogRefreshEvent;

public interface DialogSipListener {
	void onDialogConnected(DialogConnectedEvent connectedEvent);
	void onDialogConnectionFailed(DialogConnectionFailedEvent connectionFailedEvent);
	
	void onDialogDisconnected(DialogDisconnectedEvent disconnectedEvent);
	
	void onDialogTerminated(DialogTerminatedEvent terminatedEvent);
	void onDialogTerminationFailed(DialogTerminationFailedEvent terminationFailedEvent);
	
	void onDialogRefreshCompleted(DialogRefreshCompletedEvent callLegConnectedEvent);
	void onReceivedDialogRefresh(ReceivedDialogRefreshEvent receivedCallLegRefreshEvent);
}