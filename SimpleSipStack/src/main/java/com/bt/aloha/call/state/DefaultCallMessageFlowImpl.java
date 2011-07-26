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
import com.bt.aloha.stack.SessionDescriptionHelper;

public class DefaultCallMessageFlowImpl implements CallMessageFlow {
	private static final Log LOG = LogFactory.getLog(DefaultCallMessageFlowImpl.class);

	public DefaultCallMessageFlowImpl() {
	}

	public MediaNegotiationCommand initializeCall(ImmutableDialogInfo firstDialogInfo, ImmutableDialogInfo secondDialogInfo, ReadOnlyCallInfo callInfo, MediaDescription offerMediaDescription) {
		LOG.debug(String.format("Initializing call %s using sip flow %s", callInfo.getId(), this.getClass().getSimpleName()));
		if (callInfo.areBothCallLegsConnected()) {
			return new InitiateMediaNegotiationCommand(firstDialogInfo, callInfo.getAutoTerminate(), callInfo.getId(), callInfo.getCallLegConnectionState(firstDialogInfo.getId()));
		} else {
			CallLegConnectionState firstConnectionState = callInfo.getCallLegConnectionState(firstDialogInfo.getId());
			CallLegConnectionState secondConnectionState = callInfo.getCallLegConnectionState(secondDialogInfo.getId());
			if (firstConnectionState.equals(CallLegConnectionState.Pending))
				return new ConnectAndHoldCommand(firstDialogInfo, firstConnectionState, callInfo.getAutoTerminate(), null);
			else if (secondConnectionState.equals(CallLegConnectionState.Pending))
				return new ConnectAndHoldCommand(secondDialogInfo, secondConnectionState, callInfo.getAutoTerminate(), null);
			else
				return null;
    	}
	}

	public MediaNegotiationCommand processCallLegConnected(ImmutableDialogInfo eventDialogInfo, ImmutableDialogInfo otherDialogInfo, ReadOnlyCallInfo callInfo, boolean refreshOriginatedByCurrentCall, MediaDescription mediaDescription) {
		LOG.debug(String.format("Processing call leg connected event for call leg %s in call %s using sip flow %s, refresh originated by call is %s", eventDialogInfo.getId(), callInfo.getId(), this.getClass().getSimpleName(), refreshOriginatedByCurrentCall));
		
		MediaNegotiationCommand command = null;
		if (refreshOriginatedByCurrentCall)
			command = processCallLegConnectedRefreshOriginatedByCurrentCall(eventDialogInfo, otherDialogInfo, callInfo, refreshOriginatedByCurrentCall, mediaDescription);
		else if (callInfo.areBothCallLegsConnected() && callInfo.getMediaNegotiationState().equals(MediaNegotiationState.Pending)) {
			ImmutableDialogInfo firstDialogInfo = eventDialogInfo.getId().equals(callInfo.getFirstDialogId()) ? eventDialogInfo : otherDialogInfo;
			command = new InitiateMediaNegotiationCommand(firstDialogInfo, callInfo.getAutoTerminate(), callInfo.getId(), callInfo.getCallLegConnectionState(firstDialogInfo.getId()));
		}
		return command;
	}
	
	protected MediaNegotiationCommand processCallLegConnectedRefreshOriginatedByCurrentCall(ImmutableDialogInfo eventDialogInfo, ImmutableDialogInfo otherDialogInfo, ReadOnlyCallInfo callInfo, boolean refreshOriginatedByCall, MediaDescription mediaDescription) {
		CallLegConnectionState otherConnectionState = callInfo.getCallLegConnectionState(otherDialogInfo.getId());    	
    	
		if (callInfo.getMediaNegotiationState().equals(MediaNegotiationState.Initiated)) {
			return new ProxyMediaOfferCommand(otherDialogInfo, callInfo.getAutoTerminate(), callInfo.getId(), mediaDescription, otherConnectionState);
		} else if (callInfo.getMediaNegotiationState().equals(MediaNegotiationState.ProxiedOffer)) {
			return new AcceptMediaOfferCommand(otherDialogInfo, mediaDescription, otherConnectionState, callInfo.getMediaNegotiationState(), callInfo.getMediaNegotiationMethod());
		}
		return null;
	}

	public MediaNegotiationCommand processCallLegRefreshCompleted(ImmutableDialogInfo firstDialogInfo, ImmutableDialogInfo secondDialogInfo, ReadOnlyCallInfo callInfo, boolean isEventForFirstCallLeg, MediaDescription answerMediaDescription) {
		LOG.debug(String.format("Processing call leg refresh completed event for call %s using sip flow %s", callInfo.getId(), this.getClass().getSimpleName()));
		ImmutableDialogInfo otherDialogInfo = isEventForFirstCallLeg ? secondDialogInfo : firstDialogInfo;
		
		LOG.debug("Proxying reinvite response back to dialog which originated the negotiation");
		return new AcceptMediaOfferCommand(otherDialogInfo, answerMediaDescription, callInfo.getCallLegConnectionState(otherDialogInfo.getId()), callInfo.getMediaNegotiationState(), callInfo.getMediaNegotiationMethod());
	}

	public MediaNegotiationCommand processReceivedCallLegRefresh(ImmutableDialogInfo eventDialogInfo, ImmutableDialogInfo otherDialogInfo, ReadOnlyCallInfo callInfo, boolean isOfferInOkResponse, MediaDescription offerMediaDescription) {
		LOG.debug(String.format("Processing received call leg refresh event for call %s using sip flow %s", callInfo.getId(), this.getClass().getSimpleName()));
		return new ProxyMediaOfferCommand(otherDialogInfo, callInfo.getAutoTerminate(), callInfo.getId(), offerMediaDescription, callInfo.getCallLegConnectionState(otherDialogInfo.getId()));
	}

	public MediaNegotiationCommand processReceivedCallLegRefreshForTerminatedCall(ImmutableDialogInfo eventDialogInfo, MediaDescription mediaDescription, ReadOnlyCallInfo terminatedCallInfo) {
		LOG.debug(String.format("Processing received call leg refresh event (call leg %s) for terminated call %s using sip flow %s", eventDialogInfo.getId(), terminatedCallInfo.getId(), this.getClass().getSimpleName()));
		MediaDescription holdMediaDescription = SessionDescriptionHelper.generateHoldMediaDescription(mediaDescription);

		return new AcceptMediaOfferCommand(eventDialogInfo, holdMediaDescription, terminatedCallInfo.getCallLegConnectionState(eventDialogInfo.getId()), terminatedCallInfo.getMediaNegotiationState(), terminatedCallInfo.getMediaNegotiationMethod());
	}

	public MediaNegotiationCommand processCallLegConnectionFailed(ImmutableDialogInfo eventDialogInfo, ImmutableDialogInfo otherDialogInfo, CallInfo callInfo) {
		return null;
	}
}

