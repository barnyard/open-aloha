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

 	

 	
 	
 
package com.bt.aloha.call.state;

import javax.sdp.MediaDescription;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.dialog.state.ImmutableDialogInfo;

public class ThirdPartyCallMessageFlowImpl extends DefaultCallMessageFlowImpl {
	private static final Log LOG = LogFactory.getLog(ThirdPartyCallMessageFlowImpl.class);
	
	public ThirdPartyCallMessageFlowImpl() {
	}

	@Override
	public MediaNegotiationCommand initializeCall(ImmutableDialogInfo firstDialogInfo, ImmutableDialogInfo secondDialogInfo, ReadOnlyCallInfo callInfo, MediaDescription offerMediaDescription) {
		LOG.debug(String.format("Initializing call %s using sip flow %s", callInfo.getId(), this.getClass().getSimpleName()));
		MediaNegotiationCommand command = null;
		if (callInfo.areBothCallLegsConnected())
			command = new InitiateMediaNegotiationCommand(firstDialogInfo, callInfo.getAutoTerminate(), callInfo.getId(), callInfo.getCallLegConnectionState(firstDialogInfo.getId()));
    	else {
    		command = new ConnectAndHoldCommand(firstDialogInfo, callInfo.getCallLegConnectionState(firstDialogInfo.getId()), callInfo.getAutoTerminate(), null);
    	}
		
		return command;
	}
	
	@Override
	public MediaNegotiationCommand processCallLegConnected(ImmutableDialogInfo eventDialogInfo, ImmutableDialogInfo otherDialogInfo, ReadOnlyCallInfo callInfo, boolean refreshOriginatedByCurrentCall, MediaDescription mediaDescription) {
		LOG.debug(String.format("Processing call leg connected event for call leg %s in call %s using sip flow %s, refresh originated by call is %s", eventDialogInfo.getId(), callInfo.getId(), this.getClass().getSimpleName(), refreshOriginatedByCurrentCall));
		
		CallLegConnectionState firstConnectionState = callInfo.getCallLegConnectionState(callInfo.getFirstDialogId());
    	CallLegConnectionState secondConnectionState = callInfo.getCallLegConnectionState(callInfo.getSecondDialogId());
		
		MediaNegotiationCommand command = null;
		if (refreshOriginatedByCurrentCall) {
			processCallLegConnectedRefreshOriginatedByCurrentCall(eventDialogInfo, otherDialogInfo, callInfo, refreshOriginatedByCurrentCall, mediaDescription);
		} else if (firstConnectionState.equals(CallLegConnectionState.Completed) && secondConnectionState.equals(CallLegConnectionState.Pending)) {
			ImmutableDialogInfo secondDialogInfo = eventDialogInfo.getId().equals(callInfo.getSecondDialogId()) ? eventDialogInfo : otherDialogInfo;
			command = new InitiateMediaNegotiationCommand(secondDialogInfo, callInfo.getAutoTerminate(), callInfo.getId(), secondConnectionState);
		}
		
		return command;
	}	
}
