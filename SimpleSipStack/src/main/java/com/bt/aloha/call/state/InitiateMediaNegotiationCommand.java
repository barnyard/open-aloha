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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.dialog.state.ImmutableDialogInfo;

public class InitiateMediaNegotiationCommand extends MediaNegotiationCommandBase {
	private static final Log LOG = LogFactory.getLog(InitiateMediaNegotiationCommand.class);
	private String callId;
	private AutoTerminateAction autoTerminateAction;
	
	public InitiateMediaNegotiationCommand(ImmutableDialogInfo aDialogInfo, AutoTerminateAction aAutoTerminateAction, String aCallId, CallLegConnectionState aCallLegConnectionState) {
		super(aDialogInfo, aCallLegConnectionState);
		this.callId = aCallId;
		this.autoTerminateAction = aAutoTerminateAction;
	}
	
	public void execute() {
		LOG.debug(String.format("Initiating media negotiation for call %s via call leg %s with state %s", callId, getDialogInfo().getId(), getCallLegConnectionState()));
		if (getDialogInfo().isInbound() && !getCallLegConnectionState().equals(CallLegConnectionState.Completed)) {
			throw new IllegalStateException(String.format("Cannot initiate media negotiation via non-confirmed (%s) inbound dialog %s", getCallLegConnectionState(), getDialogInfo().getId()));
		}

		if (getCallLegConnectionState().equals(CallLegConnectionState.Completed)) {
			reinviteCallLeg(getDialogInfo(), null, autoTerminateAction, callId);
		} else if (getCallLegConnectionState().equals(CallLegConnectionState.Pending)) {
			getOutboundCallLegBean().connectCallLeg(getDialogInfo().getId(), autoTerminateAction, callId, null, false);
		}
	}

	public void updateCallInfo(CallInfo callInfo) {
		MediaNegotiationMethod mediaNegotiationMethod = null;
		if (getCallLegConnectionState().equals(CallLegConnectionState.Completed)) {
			mediaNegotiationMethod = MediaNegotiationMethod.ReinviteRequest;
		} else if (getCallLegConnectionState().equals(CallLegConnectionState.Pending)) {
			mediaNegotiationMethod = getDialogInfo().isInbound() ? MediaNegotiationMethod.InitialOkResponse : MediaNegotiationMethod.InitialInviteRequest;
		}
		
		callInfo.setMediaNegotiationState(MediaNegotiationState.Initiated);
		callInfo.setMediaNegotiationMethod(mediaNegotiationMethod);
		LOG.debug(String.format("Updated call info for call %s, set media negotiation state to %s and method to %s", callInfo.getId(), callInfo.getMediaNegotiationState(), callInfo.getMediaNegotiationMethod()));
	}
}
