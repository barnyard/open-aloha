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

public class InboundCallMessageFlowImpl extends DefaultCallMessageFlowImpl {
	private static final Log LOG = LogFactory.getLog(InboundCallMessageFlowImpl.class);
	
	public InboundCallMessageFlowImpl() {
	}

	/**
	 * This overload is necessary to specify the suppressproxy parameter. that parameter makes sure we only 
	 * proxy initial invite when joining calls, not when one of them has connected (at that point we've lost the offer media)
	 */
	@Override
	public MediaNegotiationCommand initializeCall(ImmutableDialogInfo firstDialogInfo, ImmutableDialogInfo secondDialogInfo, ReadOnlyCallInfo callInfo, MediaDescription offerMediaDescription) {
		return initializeCall(firstDialogInfo, secondDialogInfo, callInfo, offerMediaDescription, false);
	}
	
	protected MediaNegotiationCommand initializeCall(ImmutableDialogInfo firstDialogInfo, ImmutableDialogInfo secondDialogInfo, ReadOnlyCallInfo callInfo, MediaDescription offerMediaDescription, boolean suppressProxy) {		
		LOG.debug(String.format("Initializing call %s using sip flow %s", callInfo.getId(), this.getClass().getSimpleName()));
		ImmutableDialogInfo inboundDialogInfo = firstDialogInfo.isInbound() ? firstDialogInfo : secondDialogInfo;
		ImmutableDialogInfo outboundDialogInfo = firstDialogInfo.isInbound() ? secondDialogInfo : firstDialogInfo;
    	
    	CallLegConnectionState inboundConnectionState = callInfo.getCallLegConnectionState(inboundDialogInfo.getId());
    	CallLegConnectionState outboundConnectionState = callInfo.getCallLegConnectionState(outboundDialogInfo.getId());
    	
    	MediaNegotiationCommand command = null;
    	if (callInfo.areBothCallLegsConnected())
    		command = new InitiateMediaNegotiationCommand(inboundDialogInfo, callInfo.getAutoTerminate(), callInfo.getId(), inboundConnectionState);
    	else if (outboundConnectionState.equals(CallLegConnectionState.Pending) || outboundConnectionState.equals(CallLegConnectionState.Completed)) { 
    		if (inboundDialogInfo.isSdpInInitialInvite() && inboundConnectionState.equals(CallLegConnectionState.Pending)) {
    			command = new ProxyMediaOfferCommand(outboundDialogInfo, callInfo.getAutoTerminate(), callInfo.getId(), offerMediaDescription, outboundConnectionState, true);    		
    		} else if (inboundConnectionState.equals(CallLegConnectionState.Pending) || inboundConnectionState.equals(CallLegConnectionState.Completed)) {				
    			command = new InitiateMediaNegotiationCommand(outboundDialogInfo, callInfo.getAutoTerminate(), callInfo.getId(), outboundConnectionState);
    		}
    	}
    	
    	return command;
	}
	
	@Override
	public MediaNegotiationCommand processCallLegConnected(ImmutableDialogInfo eventDialogInfo, ImmutableDialogInfo otherDialogInfo, ReadOnlyCallInfo callInfo, boolean refreshOriginatedByCurrentCall, MediaDescription mediaDescription) {
		LOG.debug(String.format("Processing call leg connected event for call leg %s in call %s using sip flow %s, refresh originated by call is %s, media neg state is %s", eventDialogInfo.getId(), callInfo.getId(), this.getClass().getSimpleName(), refreshOriginatedByCurrentCall, callInfo.getMediaNegotiationState()));
    	InboundCallInfo inboundCallInfo;
    	if (callInfo instanceof InboundCallInfo)
    		inboundCallInfo = (InboundCallInfo)callInfo;
    	else
    		throw new ClassCastException("Expected an Inbound call info for inbound sip message flow!");
    	
    	MediaNegotiationCommand command = null;
    	if (refreshOriginatedByCurrentCall) {
    		command = super.processCallLegConnectedRefreshOriginatedByCurrentCall(eventDialogInfo, otherDialogInfo, callInfo, refreshOriginatedByCurrentCall, mediaDescription);
    	} else if (inboundCallInfo.getMediaNegotiationState().equals(MediaNegotiationState.Pending)) {
    		LOG.debug(String.format("Call leg connected event for dialog %s with state %s NOT originated by current call", eventDialogInfo.getId(), callInfo.getCallLegConnectionState(eventDialogInfo.getId())));
    		ImmutableDialogInfo firstDialogInfo = eventDialogInfo.getId().equals(callInfo.getFirstDialogId()) ? eventDialogInfo : otherDialogInfo;
	    	ImmutableDialogInfo secondDialogInfo = eventDialogInfo.getId().equals(callInfo.getSecondDialogId()) ? eventDialogInfo : otherDialogInfo;
	    	
	    	LOG.debug(String.format("Initiating media negotiation for call %s after connect - first leg is %s, second leg is %s", callInfo.getId(), firstDialogInfo.getId(), secondDialogInfo.getId()));	    	
	    	return initializeCall(firstDialogInfo, secondDialogInfo, callInfo, null, true);
    	}
    	return command;
	}
}
