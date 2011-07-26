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

public class ProxyMediaOfferCommand extends MediaNegotiationCommandBase {
	private static final Log LOG = LogFactory.getLog(ProxyMediaOfferCommand.class);
	private String callId;
	private AutoTerminateAction autoTerminateAction;
	private MediaDescription offerMediaDescription;
	private boolean proxyingInitialInviteMediaOffer;
	
	public ProxyMediaOfferCommand(ImmutableDialogInfo aDialogInfo, AutoTerminateAction aAutoTerminateAction, String aCallId, MediaDescription aOfferMediaDescription, CallLegConnectionState aCallLegConnectionState) {
		this(aDialogInfo, aAutoTerminateAction, aCallId, aOfferMediaDescription, aCallLegConnectionState, false);
	}
	
	public ProxyMediaOfferCommand(ImmutableDialogInfo aDialogInfo, AutoTerminateAction aAutoTerminateAction, String aCallId, MediaDescription aOfferMediaDescription, CallLegConnectionState aCallLegConnectionState, boolean aProxyingInitialMediaOffer) {
		super(aDialogInfo, aCallLegConnectionState);
		this.callId = aCallId;
		this.autoTerminateAction = aAutoTerminateAction;
		this.offerMediaDescription = aOfferMediaDescription;
		this.proxyingInitialInviteMediaOffer = aProxyingInitialMediaOffer;
	}
	
	public void execute() {
		LOG.debug(String.format("Proxying media offer originated by call %s to call leg %s with state %s", callId, getDialogInfo().getId(), getCallLegConnectionState()));
		if (getCallLegConnectionState().equals(CallLegConnectionState.Completed))
			reinviteCallLeg(getDialogInfo(), offerMediaDescription, autoTerminateAction, callId);
		else if (getCallLegConnectionState().equals(CallLegConnectionState.Pending)) {
			if (getDialogInfo().isInbound()) {
				if (!getDialogInfo().isSdpInInitialInvite())
					getInboundCallLegBean().acceptIncomingCallLeg(getDialogInfo().getId(), offerMediaDescription, callId);
			} else {
				getOutboundCallLegBean().connectCallLeg(getDialogInfo().getId(), autoTerminateAction, callId, offerMediaDescription, false);
			}
		}
	}

	public void updateCallInfo(CallInfo callInfo) {
		callInfo.setMediaNegotiationState(MediaNegotiationState.ProxiedOffer);
		if (proxyingInitialInviteMediaOffer)
			callInfo.setMediaNegotiationMethod(MediaNegotiationMethod.InitialInviteRequest);
		if (getCallLegConnectionState().equals(CallLegConnectionState.Pending) && !getDialogInfo().isInbound())
			callInfo.setCallLegConnectionState(getDialogInfo().getId(), CallLegConnectionState.InProgress);
	}
}
