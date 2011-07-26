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

 	

 	
 	
 
package com.bt.aloha.dialog.state;

import gov.nist.javax.sip.header.RouteList;

import javax.sdp.MediaDescription;
import javax.sdp.SessionDescription;
import javax.sip.address.Address;
import javax.sip.message.Request;


public interface ReadOnlyDialogInfo extends ImmutableDialogInfo {

	boolean isSdpInInitialInvite();

	String getSipUserName();

	long getSequenceNumber();

	String getLocalTag();

	String getSipCallId();

	Address getLocalParty();

	Address getRemoteContact();

	Address getRemoteParty();

	String getRemoteTag();

	SessionDescription getSessionDescription();

	RouteList getRouteList();

	DialogState getDialogState();

	long getCallAnswerTimeout();

	TerminationCause getTerminationCause();

	TerminationMethod getTerminationMethod();

	boolean isAutoTerminate();

	ReinviteInProgress getReinviteInProgess();

	long getStartTime();

	int getDuration();

	MediaDescription getRemoteOfferMediaDescription();

	PendingReinvite getPendingReinvite();

	Object getApplicationData();

	long getLastReceivedOkSequenceNumber();

	Request getLastAckRequest();

	boolean isAutomaticallyPlaceOnHold();
	
	String getViaBranchId();
	
	boolean isMediaDialog();
}
