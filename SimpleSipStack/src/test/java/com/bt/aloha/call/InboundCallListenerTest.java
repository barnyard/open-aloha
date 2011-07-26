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

 	

 	
 	
 
package com.bt.aloha.call;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.cafesip.sipunit.SipCall;
import org.junit.Ignore;
import org.junit.Test;

import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.testing.SipUnitPhone;

public class InboundCallListenerTest extends CallListenerTestBase {
	@Override
	protected String joinCallLegs() throws Exception {
		setOutboundCallTargetUsername(INBOUND_CALL_SETUP_USING_CALL_BEAN);
		super.setupInboundToOutboundPhoneCallWithoutInitialHold();
		this.firstDialogId = inboundCallSetupUsingCallBean.inboundDialogId;
		this.secondDialogId = inboundCallSetupUsingCallBean.outboundDialogId;
		return inboundCallSetupUsingCallBean.callId;
	}
	
	@Override
	protected SipCall getFirstSipCall() {
		return getOutboundCall();
	}
	
	@Override
	protected SipCall getSecondSipCall() {
		return getInboundCall();
	}
	
	@Override
	protected void terminateCallLeg(String callLegId) {
		inboundCallLegBean.terminateCallLeg(callLegId);
	}
		
	// test that the connected event is not raised after successful negotiation of only 1 reinvite
	@Test
	@Ignore
	public void testCallNotConnectedAfterOneReInvite() throws Exception {
		// setup
		setOutboundCallTargetUsername(INBOUND_CALL_SETUP_USING_CALL_BEAN);
		getOutboundCall().listenForReinvite();;
		getInboundCall().listenForReinvite();
		
		SessionDescriptionHelper.setMediaDescription(getOutboundPhoneSdp(), getOutboundPhoneMediaDescription());
		assertTrue(getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), getRemoteSipAddress(), getRemoteSipProxy(), getOutboundPhoneSdp().toString(), "application", "sdp", null, null));

		// invite-trying-ringing-ok-ack
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Inbound, getOutboundPhoneMediaDescription());
		
		// assert
		assertFalse("Unexpected event", connectedSemaphore.tryAcquire(300, TimeUnit.MILLISECONDS));
	}
}
