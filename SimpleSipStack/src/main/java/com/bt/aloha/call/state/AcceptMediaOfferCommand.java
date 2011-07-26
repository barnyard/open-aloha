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

public class AcceptMediaOfferCommand extends MediaNegotiationCommandBase {
	private static final Log LOG = LogFactory.getLog(AcceptMediaOfferCommand.class);
	private MediaDescription answerMediaDescription;
	private MediaNegotiationState mediaNegotiationState;
	private MediaNegotiationMethod mediaNegotiationMethod;
	
	public AcceptMediaOfferCommand(ImmutableDialogInfo aDialogInfo, MediaDescription aAnswerMediaDescription, CallLegConnectionState aCallLegConnectionState, MediaNegotiationState aMediaNegotiationState, MediaNegotiationMethod aMediaNegotiationMethod) {
		super(aDialogInfo, aCallLegConnectionState);
		this.answerMediaDescription = aAnswerMediaDescription;
		this.mediaNegotiationState = aMediaNegotiationState;
		this.mediaNegotiationMethod = aMediaNegotiationMethod;
	}
	
	public void execute() {
		LOG.debug(String.format("ACCEPTING received media offer for dialog %s - call leg state is %s, media negotiation state and method are %s and %s", getDialogInfo().getId(), getCallLegConnectionState(), mediaNegotiationState, mediaNegotiationMethod));
		boolean offerInOkResponse;
		if (getCallLegConnectionState().equals(CallLegConnectionState.Completed)) {
			boolean mediaNegotiationInProgress = mediaNegotiationState.equals(MediaNegotiationState.Initiated) || mediaNegotiationState.equals(MediaNegotiationState.ProxiedOffer);
			offerInOkResponse = mediaNegotiationInProgress ? MediaNegotiationMethod.ReinviteRequest.equals(mediaNegotiationMethod) : false; 
		} else {
			offerInOkResponse = !getDialogInfo().isSdpInInitialInvite();
		}
		boolean initialInviteTransactionCompleted = getCallLegConnectionState().equals(CallLegConnectionState.Completed);

		if (getDialogInfo().isInbound()) {
			if (getCallLegConnectionState().equals(CallLegConnectionState.Pending) && getDialogInfo().isSdpInInitialInvite()) {
				getInboundCallLegBean().acceptIncomingCallLeg(getDialogInfo().getId(), answerMediaDescription, null);
			} else {
				getInboundCallLegBean().acceptReceivedMediaOffer(getDialogInfo().getId(), answerMediaDescription, offerInOkResponse, initialInviteTransactionCompleted);
			}
		} else {
			getOutboundCallLegBean().acceptReceivedMediaOffer(getDialogInfo().getId(), answerMediaDescription, offerInOkResponse, initialInviteTransactionCompleted);
		}
	}

	public void updateCallInfo(CallInfo callInfo) {
		callInfo.setMediaNegotiationState(MediaNegotiationState.Completed);
	}
}
