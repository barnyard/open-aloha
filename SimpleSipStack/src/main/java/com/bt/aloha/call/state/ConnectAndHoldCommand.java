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

import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.dialog.state.ImmutableDialogInfo;
import com.bt.aloha.stack.SessionDescriptionHelper;

public class ConnectAndHoldCommand extends MediaNegotiationCommandBase {
	private static final Log LOG = LogFactory.getLog(ConnectAndHoldCommand.class);
	private AutoTerminateAction autoTerminateAction;
	private MediaDescription offerMediaDescription;
	
	public ConnectAndHoldCommand(ImmutableDialogInfo aDialogInfo, CallLegConnectionState aCallLegConnectionState, AutoTerminateAction aAutoTerminateAction, MediaDescription aOfferMediaDescription) {
		super(aDialogInfo, aCallLegConnectionState);
		this.autoTerminateAction = aAutoTerminateAction;
		this.offerMediaDescription = aOfferMediaDescription;
	}
	
	public void execute() {
		LOG.debug(String.format("Connecting and HOLDing call leg %s with state %s", getDialogInfo().getId(), getCallLegConnectionState()));
		if (!getDialogInfo().isInbound() && getCallLegConnectionState().equals(CallLegConnectionState.Pending))
	    	getOutboundCallLegBean().connectCallLeg(getDialogInfo().getId(), autoTerminateAction, null, null, true);
		else if (getDialogInfo().isInbound() && getCallLegConnectionState().equals(CallLegConnectionState.Pending)) {
			if (getDialogInfo().isSdpInInitialInvite())
				getInboundCallLegBean().acceptIncomingCallLeg(getDialogInfo().getId(), SessionDescriptionHelper.generateHoldMediaDescription(offerMediaDescription), null);
			else
				getInboundCallLegBean().acceptIncomingCallLeg(getDialogInfo().getId(), SessionDescriptionHelper.generateHoldMediaDescription(), null);
		}
	}

	public void updateCallInfo(CallInfo callInfo) {
	}
}
