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

public class AutomataInboundCallMessageFlowImpl extends InboundCallMessageFlowImpl {
	private static final Log LOG = LogFactory.getLog(AutomataInboundCallMessageFlowImpl.class);
	
	public AutomataInboundCallMessageFlowImpl() {
	}
	
	@Override
	public MediaNegotiationCommand initializeCall(ImmutableDialogInfo firstDialogInfo, ImmutableDialogInfo secondDialogInfo, ReadOnlyCallInfo callInfo, MediaDescription offerMediaDescription) {
		return initializeCall(firstDialogInfo, secondDialogInfo, callInfo, offerMediaDescription, false);
	}
	
	@Override
	protected MediaNegotiationCommand initializeCall(ImmutableDialogInfo firstDialogInfo, ImmutableDialogInfo secondDialogInfo, ReadOnlyCallInfo callInfo, MediaDescription offerMediaDescription, boolean suppressProxy) {
		LOG.debug(String.format("Initializing call %s using sip flow %s", callInfo.getId(), this.getClass().getSimpleName()));
		ImmutableDialogInfo inboundDialogInfo = firstDialogInfo.isInbound() ? firstDialogInfo : secondDialogInfo;
		ImmutableDialogInfo outboundDialogInfo = firstDialogInfo.isInbound() ? secondDialogInfo : firstDialogInfo;
    	
    	CallLegConnectionState inboundConnectionState = callInfo.getCallLegConnectionState(inboundDialogInfo.getId());
    	CallLegConnectionState outboundConnectionState = callInfo.getCallLegConnectionState(outboundDialogInfo.getId());
    	
    	MediaNegotiationCommand command = null;
    	if (outboundConnectionState.equals(CallLegConnectionState.Pending) || outboundConnectionState.equals(CallLegConnectionState.Completed)) {
	    	if (inboundConnectionState.equals(CallLegConnectionState.Pending)) {
	    		if (inboundDialogInfo.isSdpInInitialInvite()) {
	    			if (!suppressProxy) {
	    				command = new ProxyMediaOfferCommand(outboundDialogInfo, callInfo.getAutoTerminate(), callInfo.getId(), offerMediaDescription, outboundConnectionState, true);
	    			}
	    		} else {
	    			command = new ConnectAndHoldCommand(inboundDialogInfo, inboundConnectionState, callInfo.getAutoTerminate(), null);
	    		}
			} else if (inboundConnectionState.equals(CallLegConnectionState.Completed)) {
				command = new InitiateMediaNegotiationCommand(inboundDialogInfo, callInfo.getAutoTerminate(), callInfo.getId(), inboundConnectionState);   			
	    	}
		} else if (outboundConnectionState.equals(CallLegConnectionState.InProgress) && inboundConnectionState.equals(CallLegConnectionState.Pending)) {
			command = new ConnectAndHoldCommand(inboundDialogInfo, inboundConnectionState, callInfo.getAutoTerminate(), offerMediaDescription);
		}
    	return command;
	}
}
