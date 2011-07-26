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

 	

 	
 	
 
package com.bt.aloha.batchtest.ultramonkey;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.call.CallBean;
import com.bt.aloha.callleg.InboundCallLegListener;
import com.bt.aloha.callleg.OutboundCallLegBean;
import com.bt.aloha.callleg.event.CallLegConnectedEvent;
import com.bt.aloha.callleg.event.CallLegConnectionFailedEvent;
import com.bt.aloha.callleg.event.CallLegDisconnectedEvent;
import com.bt.aloha.callleg.event.CallLegRefreshCompletedEvent;
import com.bt.aloha.callleg.event.CallLegTerminatedEvent;
import com.bt.aloha.callleg.event.CallLegTerminationFailedEvent;
import com.bt.aloha.callleg.event.IncomingCallLegEvent;
import com.bt.aloha.callleg.event.ReceivedCallLegRefreshEvent;
import com.bt.aloha.dialog.event.IncomingAction;
import com.bt.aloha.dialog.event.IncomingResponseCode;

/**
 * simple facade on top of SpringRing 
 */
public class ServiceImpl implements Service , InboundCallLegListener{
	
    private static final Log LOG = LogFactory.getLog(ServiceImpl.class);
	private CallBean callBean;
	private OutboundCallLegBean outboundCallLegBean;
	private int incomingCount;

	public String makeCall(String caller, String callee) {
		String callLegId1 = outboundCallLegBean.createCallLeg(URI.create(callee), URI.create(caller));
		String callLegId2 = outboundCallLegBean.createCallLeg(URI.create(caller), URI.create(callee));
		return callBean.joinCallLegs(callLegId1, callLegId2);
	}

	public void terminateCall(String callId) {
		callBean.terminateCall(callId);
	}

	public void onIncomingCallLeg(IncomingCallLegEvent incomingCallLegEvent) {
		LOG.info(String.format("onIncomingCallLeg(%s)", incomingCallLegEvent));
		incomingCount++;
	    //String incomingDialogId = incomingCallLegEvent.getId();
	    //String incomingUri = incomingCallLegEvent.getFromUri();
        incomingCallLegEvent.setIncomingCallAction(IncomingAction.Reject);
        incomingCallLegEvent.setResponseCode(IncomingResponseCode.Busy);

        //String callLegId = this.outboundCallLegBean.createCallLeg(URI.create(incomingUri), URI.create("sip:07918029610@10.238.67.22"));
        //LOG.info(this.callBean.joinCallLegs(incomingDialogId, callLegId));
	}

	public void onCallLegConnected(CallLegConnectedEvent arg0) {
		LOG.debug(arg0.getClass().getSimpleName());
	}

	public void onCallLegConnectionFailed(CallLegConnectionFailedEvent arg0) {
		LOG.debug(arg0.getClass().getSimpleName());
	}

	public void onCallLegDisconnected(CallLegDisconnectedEvent arg0) {
		LOG.debug(arg0.getClass().getSimpleName());
	}

	public void onCallLegRefreshCompleted(CallLegRefreshCompletedEvent arg0) {
		LOG.debug(arg0.getClass().getSimpleName());
	}

	public void onCallLegTerminated(CallLegTerminatedEvent arg0) {
		LOG.debug(arg0.getClass().getSimpleName());
	}

	public void onCallLegTerminationFailed(CallLegTerminationFailedEvent arg0) {
		LOG.debug(arg0.getClass().getSimpleName());
	}

	public void onReceivedCallLegRefresh(ReceivedCallLegRefreshEvent arg0) {
		LOG.debug(arg0.getClass().getSimpleName());
	}
	public void setCallBean(CallBean _callBean) {
		this.callBean = _callBean;
	}
	
	public void setOutboundCallLegBean(OutboundCallLegBean outboundCallLegBean) {
		this.outboundCallLegBean = outboundCallLegBean;
	}
	
	public int getIncomingCount() {
		return this.incomingCount;
	}
	
	public void setIncomingCount(int i) {
		this.incomingCount = i;
	}
}
