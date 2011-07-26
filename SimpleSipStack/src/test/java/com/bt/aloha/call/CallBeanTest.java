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

 	

 	
 	
 
/**
 * (c) British Telecommunications plc, 2007, All Rights Reserved
 */
package com.bt.aloha.call;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sip.RequestEvent;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipTransaction;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.call.CallBean;
import com.bt.aloha.call.CallBeanImpl;
import com.bt.aloha.call.CallInformation;
import com.bt.aloha.call.CallListener;
import com.bt.aloha.call.event.CallConnectedEvent;
import com.bt.aloha.call.event.CallConnectionFailedEvent;
import com.bt.aloha.call.event.CallDisconnectedEvent;
import com.bt.aloha.call.event.CallTerminatedEvent;
import com.bt.aloha.call.event.CallTerminationFailedEvent;
import com.bt.aloha.call.state.CallInfo;
import com.bt.aloha.call.state.CallLegCausingTermination;
import com.bt.aloha.call.state.CallLegConnectionState;
import com.bt.aloha.call.state.CallState;
import com.bt.aloha.call.state.CallTerminationCause;
import com.bt.aloha.call.state.PendingCallReinvite;
import com.bt.aloha.call.state.ReadOnlyCallInfo;
import com.bt.aloha.call.state.ThirdPartyCallInfo;
import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.callleg.event.ReceivedCallLegRefreshEvent;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.event.ReceivedDialogRefreshEvent;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.dialog.state.TerminationCause;
import com.bt.aloha.dialog.state.TerminationMethod;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.testing.SipUnitPhone;
import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;

public class CallBeanTest extends CallBeanTestBase {
    public CallListener dummyCallListener;
    private String firstDialogId;
    private String secondDialogId;
    private String thirdDialogId;

	@Before
	public void before() throws Exception {
        dummyCallListener = new CallListener() {
            public void onCallConnected(CallConnectedEvent callConnectedEvent) {}
            public void onCallConnectionFailed(CallConnectionFailedEvent callConectionFailedEvent) {}
            public void onCallDisconnected(CallDisconnectedEvent callDisconnectedEvent) {}
            public void onCallTerminated(CallTerminatedEvent callTerminatedEvent) {}
            public void onCallTerminationFailed(CallTerminationFailedEvent callTerminationFailedEvent) {}
        };
	}

	// test that we don't accept two inbound dialogs
	@Test(expected=IllegalArgumentException.class)
	public void testExcepionThrownWhenTwoInboundDialogsJoined() throws Exception {
		// setup
		setOutboundCallTargetUsername(INBOUND_CALL_SETUP_USING_CALL_BEAN);

		assertTrue(getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), getRemoteSipAddress(), getRemoteSipProxy(), createHoldSessionDescription().toString(), "application", "sdp", null, null));
		assertTrue("No trying", getOutboundCall().waitOutgoingCallResponse(1000));

		// act
		callBean.joinCallLegs(inboundCallSetupUsingCallBean.inboundDialogId, inboundCallSetupUsingCallBean.inboundDialogId);
	}

    // test that we call add listeners one at a time to an existing callbean that has no listeners
    @Test
    public void testAddListenerToNewBean() {
        // setup
        int listenerCount = ((CallBeanImpl)callBean).getCallListeners().size();

        // act
        ((CallBean)callBean).addCallListener(dummyCallListener);

        // assert
        assertEquals(++listenerCount, ((CallBeanImpl)callBean).getCallListeners().size());

        // act
        ((CallBean)callBean).addCallListener(dummyCallListener);

        // assert
        assertEquals(listenerCount + 1, ((CallBeanImpl)callBean).getCallListeners().size());
    }

    // test that we call add listeners one at a time to an existing callbean that already has
    // som listeners
    @Test
    public void testAddListenerToBeanWithExistingArray() {
        // setup
        List<CallListener> callListenerList = new ArrayList<CallListener>();
        callListenerList.add(dummyCallListener);
        callListenerList.add(dummyCallListener);

        ((CallBeanImpl)callBean).setCallListeners(callListenerList);
        int listenerCount = ((CallBeanImpl)callBean).getCallListeners().size();

        // act
        ((CallBean)callBean).addCallListener(dummyCallListener);

        // assert
        assertEquals(++listenerCount, ((CallBeanImpl)callBean).getCallListeners().size());

        // act
        ((CallBean)callBean).addCallListener(dummyCallListener);

        // assert
        assertEquals(listenerCount + 1, ((CallBeanImpl)callBean).getCallListeners().size());
    }

	// test auto termination on outbound dialog hangup when joining an inbound and an outboudn dialog together in a call
	@Test
	public void testInboundOutboundBackToBackCallAutoTerminationOnOutboundHangup() throws Exception {
		// setup
		setOutboundCallTargetUsername(INBOUND_CALL_SETUP_USING_HOLD);
		inboundCallSetupUsingHold.setAutoTerminate(AutoTerminateAction.True);
		setupInboundToOutboundPhoneCallWithInitialHold();

		// act
		getInboundCall().disconnect();

		// assert
		assertTrue("Timed out waiting for BYE", getOutboundCall().waitForDisconnect(5000));
		assertTrue("Unable to respond to BYE", getOutboundCall().respondToDisconnect());
	}

	// test auto termination on inbound dialog hangup when joining an inbound and an outboudn dialog together in a call
	@Test
	public void testInboundOutboundBackToBackCallAutoTerminationOnInboundHangup() throws Exception {
		// setup
		setOutboundCallTargetUsername(INBOUND_CALL_SETUP_USING_HOLD);
		inboundCallSetupUsingHold.setAutoTerminate(AutoTerminateAction.True);
		setupInboundToOutboundPhoneCallWithInitialHold();

		// act
		getOutboundCall().disconnect();

		// assert
		assertTrue("Timed out waiting for BYE", getInboundCall().waitForDisconnect(5000));
		assertTrue("Unable to respond to BYE", getInboundCall().respondToDisconnect());
	}

	// test auto termination on inbound termination by app after joining an inbound and an outboudn dialog together in a call
	@Test
	public void testInboundOutboundBackToBackCallAutoTerminationOnInboundTermination() throws Exception {
		// setup
		setOutboundCallTargetUsername(INBOUND_CALL_SETUP_USING_HOLD);
		inboundCallSetupUsingHold.setAutoTerminate(AutoTerminateAction.True);
		setupInboundToOutboundPhoneCallWithInitialHold();

		// act
		inboundCallLegBean.terminateCallLeg(inboundCallSetupUsingHold.inboundDialogId);

		// assert
		assertTrue("Timed out waiting for inbound dialog BYE", getOutboundCall().waitForDisconnect(5000));
		assertTrue("Unable to respond to inbound dialog BYE", getOutboundCall().respondToDisconnect());
		assertTrue("Timed out waiting for outbound dialog BYE", getInboundCall().waitForDisconnect(5000));
	}

	/** test joining an inbound and outbound dialog together by first putting the inbound dialog on hold
	 *
	 *    |---INVITE (SDP)--->|                   |
     *    |<---OK (hold SDP)--|                   |
     *    ============= join call legs ============
     *    |                   |---INVITE (blank)->|
     *    |-------ACK-------->|                   |
     *    |                   |<----OK (offer)----|
     *    |<-ReINVITE (offer)-|                   |
     *    |----OK (answer)--->|                   |
     *    |<------ACK-------->|---ACK (answer)--->|
     */
	@Test
	public void testInboundOutboundBackToBackCallWithHoldOutboundLegConnectsFirst() throws Exception {
		// setup
		getOutboundCall().listenForReinvite();
		setOutboundCallTargetUsername(INBOUND_CALL_SETUP_USING_HOLD);

		// act
		SessionDescriptionHelper.setMediaDescription(getOutboundPhoneSdp(), getOutboundPhoneMediaDescription());
		assertTrue(getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), getRemoteSipAddress(), getRemoteSipProxy(), getOutboundPhoneSdp().toString(), "application", "sdp", null, null));
		assertWeGetOKWithHoldSdp();

		String outboundDialogId = outboundCallLegBean.createCallLeg(URI.create("sip:whatever"), getInboundPhoneSipUri());
		callBean.joinCallLegs(inboundCallSetupUsingHold.inboundDialogId, outboundDialogId);

		// invite-trying-ringing-ok-ack
		getOutboundCall().sendInviteOkAck();

		// reinvite
		waitForCallAssertMediaDescriptionSendTryingRingingOk(SipUnitPhone.Inbound, null);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Outbound, getInboundPhoneMediaDescription());
		waitForEmptyAck(SipUnitPhone.Outbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, getOutboundPhoneMediaDescription());

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.Outbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	/** test joining an inbound and outbound dialog together, where the inbound request is blank, by first putting the inbound dialog on hold
	 *
	 *    |--INVITE (blank)-->|                   |
     *    |<---OK (hold SDP)--|                   |
     *    ============= join call legs ============
     *    |                   |---INVITE (blank)->|
     *    |-ACK (hold resp)-->|                   |
     *    |                   |<----OK (offer)----|
     *    |<-ReINVITE (offer)-|                   |
     *    |----OK (answer)--->|                   |
     *    |<------ACK-------->|---ACK (answer)--->|
     */
	@Test
	public void testInboundOutboundBackToBackCallWithBlankInviteAndHoldOutboundLegConnectsFirst() throws Exception {
		// setup
		getOutboundCall().listenForReinvite();
		setOutboundCallTargetUsername(INBOUND_CALL_SETUP_USING_HOLD);

		// act
		assertTrue(getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), getRemoteSipAddress(), getRemoteSipProxy(), null, null, null, null, null));
		assertWeGetOKWithHoldSdp();

		String outboundDialogId = outboundCallLegBean.createCallLeg(URI.create("sip:whatever"), getInboundPhoneSipUri());
		callBean.joinCallLegs(inboundCallSetupUsingHold.inboundDialogId, outboundDialogId);

		// ack with hold
		SessionDescriptionHelper.setMediaDescription(getOutboundPhoneSdp(), getHoldMediaDescription());
		getOutboundCall().sendInviteOkAck(getOutboundPhoneSdp().toString(), "application", "sdp", null, null);

		// reinvite
		waitForCallAssertMediaDescriptionSendTryingRingingOk(SipUnitPhone.Inbound, null);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Outbound, getInboundPhoneMediaDescription());
		waitForEmptyAck(SipUnitPhone.Outbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, getOutboundPhoneMediaDescription());

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.Outbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	/** test joining an inbound and outbound dialog together by lettting CallBean send the OK to the
	 * inbound dialog when it gets the SDP from the OK response to the outbound request
	 *
	 *    |--INVITE (offer)-->|                   |
	 *    ============= join call legs ============
	 *    |                   |---INVITE (offer)->|
	 *    |                   |<----OK (answer)---|
     *    |<---OK (answer)----|                   |
     *    |-------ACK-------->|--------ACK------->|
     */
	@Test
	public void testInboundOutboundBackToBackCallHandledByCallBean() throws Exception {
		// setup
		setOutboundCallTargetUsername(INBOUND_CALL_SETUP_USING_CALL_BEAN);

		// act & assert
		SessionDescriptionHelper.setMediaDescription(getOutboundPhoneSdp(), getOutboundPhoneMediaDescription());
		assertTrue(getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), getRemoteSipAddress(), getRemoteSipProxy(), getOutboundPhoneSdp().toString(), "application", "sdp", null, null));

		// invite-trying-ringing-ok-ack
		waitForCallAssertMediaDescriptionSendTryingRingingOk(SipUnitPhone.Inbound, getOutboundPhoneMediaDescription());
		waitForEmptyAck(SipUnitPhone.Inbound);

		// inbound call ok
		assertTrue("No inbound ok", getOutboundCall().waitForAnswer(5000));
		assertEquals(Response.OK, getOutboundCall().getLastReceivedResponse().getStatusCode());
		assertMediaDescriptionInSessionDescription(getInboundPhoneMediaDescription(), new String(getOutboundCall().getLastReceivedResponse().getRawContent()));
		assertFalse("Should NOT have got connected event before both legs connected", connectedSemaphore.tryAcquire(200, TimeUnit.MILLISECONDS));

		assertTrue(getOutboundCall().sendInviteOkAck());

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.Outbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	/** test joining an inbound and outbound dialog together by letting CallBean send the OK to the
	 * inbound dialog when it gets the SDP from the OK response to the outbound request.
	 * The inbound dialog has no sdp in the initial invite
	 *
	 *    |--INVITE (blank)-->|                   |
	 *    ============= join call legs ============
	 *    |                   |---INVITE (blank)->|
	 *    |                   |<----OK (offer)----|
     *    |<----OK (offer)----|                   |
     *    |----ACK (answer--->|                   |
     *    |-------------------|---ACK (answer)--->|
     */
	@Test
	public void testInboundNoSdpOutboundBackToBackCallHandledByCallBean() throws Exception {
		// setup
		setOutboundCallTargetUsername(INBOUND_CALL_SETUP_USING_CALL_BEAN);
		getOutboundCall().listenForReinvite();

		// act
		assertTrue(getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), getRemoteSipAddress(), getRemoteSipProxy(), null, null, null, null, null));

		// assert
		waitForCallAssertMediaDescriptionSendTryingRingingOk(SipUnitPhone.Inbound, null);

		// inbound call ok
		assertTrue("No inbound ok", getOutboundCall().waitForAnswer(5000));
		assertEquals(Response.OK, getOutboundCall().getLastReceivedResponse().getStatusCode());
		assertMediaDescriptionInSessionDescription(getInboundPhoneMediaDescription(), new String(getOutboundCall().getLastReceivedResponse().getRawContent()));

		SessionDescriptionHelper.setMediaDescription(getOutboundPhoneSdp(), getOutboundPhoneMediaDescription());
		getOutboundCall().sendInviteOkAck(getOutboundPhoneSdp().toString(), "application", "sdp", null, null);

		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, getOutboundPhoneMediaDescription());

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.Outbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	// as above but ensuring logic works when dialog order is reversed
	@Test
	public void testInboundOutboundBackToBackCallHandledByCallBeanInboundDialogSecond() throws Exception {
		// setup
		inboundCallSetupUsingCallBean.reverseDialogOrder();

		// assert
		testInboundOutboundBackToBackCallHandledByCallBean();
	}

	/** test that an incoming dialog being joined to an already connected outbound dialog gets OK response after the outbound dialog's been reinvited
	 *
	 *    |                   |---INVITE (blank)->|
	 *    |                   |<----OK (offer)----|
	 *    |                   |----ACK (hold)---->|
	 *    |--INVITE (offer)-->|                   |
	 *    ============= join call legs ============
	 *    |                   |-ReINVITE (offer)->|
 	 *    |                   |<----OK (answer)---|
     *    |<---OK (answer)----|--------ACK------->|
     *    |-------ACK-------->|                   |
	 */
	@Test
	public void testIncomingDialogJoinedToConnectedOutboundDialog() throws Exception {
		// setup
		setOutboundCallTargetUsername(INBOUND_CALL_SETUP_USING_CALL_BEAN);
		getOutboundCall().listenForReinvite();

		// connect outbound call
		String outboundDialogId = outboundCallLegBean.createCallLeg(URI.create("sip:whatever"), getInboundPhoneSipUri());
		outboundCallLegBean.connectCallLeg(outboundDialogId);
		inboundCallSetupUsingCallBean.outboundDialogId = outboundDialogId;

		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);

		// act
		SessionDescriptionHelper.setMediaDescription(getOutboundPhoneSdp(), getOutboundPhoneMediaDescription());
		assertTrue(getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), getRemoteSipAddress(), getRemoteSipProxy(), getOutboundPhoneSdp().toString(), "application", "sdp", null, null));
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Inbound, getOutboundPhoneMediaDescription());

		assertTrue("No inbound ok", getOutboundCall().waitForAnswer(5000));
		assertEquals(Response.OK, getOutboundCall().getLastReceivedResponse().getStatusCode());
		assertMediaDescriptionInSessionDescription(getInboundPhoneMediaDescription(), new String(getOutboundCall().getLastReceivedResponse().getRawContent()));
		waitForEmptyAck(SipUnitPhone.Inbound);
		assertTrue(getOutboundCall().sendInviteOkAck());

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.Outbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	// as above but with dialog order reversed
	@Test
	public void testIncomingDialogJoinedToConnectedOutboundDialogReverseDialogOrder() throws Exception {
		// setup
		inboundCallSetupUsingCallBean.reverseDialogOrder();

		// act
		testIncomingDialogJoinedToConnectedOutboundDialog();
	}

	/** test that an incoming dialog with no media being joined to an already connected outbound dialog gets OK response after the outbound dialog's been reinvited
	 *
	 *    |                   |---INVITE (blank)->|
	 *    |                   |<----OK (offer)----|
	 *    |                   |----ACK (hold)---->|
	 *    |--INVITE (blank)-->|                   |
	 *    ============= join call legs ============
	 *    |                   |-ReINVITE (blank)->|
	 *    |                   |<----OK (offer)----|
     *    |----ACK (answer--->|                   |
     *    |-------------------|---ACK (answer)--->|
	 */
	@Test
	public void testIncomingDialogNoSdpJoinedToConnectedOutboundDialog() throws Exception {
		// setup
		setOutboundCallTargetUsername(INBOUND_CALL_SETUP_USING_CALL_BEAN);
		getOutboundCall().listenForReinvite();

		// connect outbound call
		String outboundDialogId = outboundCallLegBean.createCallLeg(URI.create("sip:whatever"), getInboundPhoneSipUri());
		outboundCallLegBean.connectCallLeg(outboundDialogId);
		inboundCallSetupUsingCallBean.outboundDialogId = outboundDialogId;

		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);

		// act
		assertTrue(getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), getRemoteSipAddress(), getRemoteSipProxy(), null, null, null, null, null));
		waitForEmptyReinviteRespondOk(SipUnitPhone.Inbound);

		assertTrue("No inbound ok", getOutboundCall().waitForAnswer(5000));
		assertEquals(Response.OK, getOutboundCall().getLastReceivedResponse().getStatusCode());
		assertMediaDescriptionInSessionDescription(getInboundPhoneMediaDescription(), new String(getOutboundCall().getLastReceivedResponse().getRawContent()));
		SessionDescriptionHelper.setMediaDescription(getOutboundPhoneSdp(), getOutboundPhoneMediaDescription());
		getOutboundCall().sendInviteOkAck(getOutboundPhoneSdp().toString(), "application", "sdp", null, null);

		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Inbound, getOutboundPhoneMediaDescription());

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.Outbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	/** test that an incoming dialog joined to a connecting outbound dialog gets rejected when the outbound dialog fails
	 *
	 *    |--INVITE (blank)-->|                   |
	 *    ============= join call legs ============
	 *    |                   |---INVITE (blank)->|
	 *    |                   |<----ERROR RESP----|
     *    |<----ERROR RESP----|                   |
	 */
	@Test
	public void testIncomingDialogRejectedAfterOutboundDialogConnectionFails() throws Exception {
		// setup
		setOutboundCallTargetUsername(INBOUND_CALL_SETUP_USING_CALL_BEAN);

		// act
		SessionDescriptionHelper.setMediaDescription(getOutboundPhoneSdp(), getOutboundPhoneMediaDescription());
		assertTrue(getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), getRemoteSipAddress(), getRemoteSipProxy(), getOutboundPhoneSdp().toString(), "application", "sdp", null, null));

		// invite
		waitForCallSendTryingBusyHere(getInboundCall());

		// inbound dialog reject
		assertOutboundCallResponses(new int[] {Response.TEMPORARILY_UNAVAILABLE});

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.Outbound);
	}

	/** test that an incoming dialog with no sdp is handled with HOLD action. And can then be joined into a call
     *
     *    |---INVITE (SDP)--->|                   |
     *    |<---OK (hold SDP)--|                   |
     *    |-------ACK-------->|                   |
     *    ============= join call legs ============
     *    |                   |---INVITE (blank)->|
     *    |                   |<----OK (offer)----|
     *    |<-ReINVITE (offer)-|                   |
     *    |----OK (answer)--->|                   |
     *    |<------ACK-------->|---ACK (answer)--->|
     */
	@Test
	public void testIncomingDialogNoSdpHoldFirstLaterJoinedInCall() throws Exception {
		// setup
		setOutboundCallTargetUsername(INBOUND_CALL_SETUP_USING_HOLD);

		// assert
		setupInboundToOutboundPhoneCallWithInitialHold();

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.Outbound);
	}

	// as above but with order reversed
	@Test
	public void testIncomingDialogNoSdpNoneActionJoinedInCallReverseDialogOrder() throws Exception {
		// setup
		inboundCallSetupUsingHold.reverseDialogOrder();

		// act
		testInboundNoSdpOutboundBackToBackCallHandledByCallBean();
	}

	static protected class SipUnitReinviteWaitHelper implements Runnable {
		private SipTransaction sipTransaction = null;
		private SipCall sipCall = null;
		private Thread t;

		public SipUnitReinviteWaitHelper(SipCall aSipCall) {
			this.sipCall = aSipCall;
		}

		public SipTransaction getSipTransaction() {
			return this.sipTransaction;
		}

		public void run() {
			sipTransaction = sipCall.waitForReinvite(5000);
		}

		public void start() {
			t = new Thread(this);
			t.start();
		}

		public void join() {
			try {
				t.join();
			} catch(InterruptedException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}
	}

	/**
	 * Tests that we set the callInfo autoterminate field w/ whatever was passed in
	 */
	@Test
	public void testSetAutoTerminateInCallInfo() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());

		// act
		String callId = callBean.joinCallLegs(firstDialogId, secondDialogId, AutoTerminateAction.True);

		// assert
		assertEquals(AutoTerminateAction.True, callCollection.get(callId).getAutoTerminate());
	}

	/**
	 * Tests that we set the callInfo autoterminate field to unchanged by default
	 */
	@Test
	public void testSetAutoTerminateInCallInfoDefaultsToUnchanged() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());

		// act
		String callId = callBean.joinCallLegs(firstDialogId, secondDialogId);

		// assert
		assertEquals(AutoTerminateAction.Unchanged, callCollection.get(callId).getAutoTerminate());
	}

	/** test that two dialogs being connected separately are joined into a call through reinvites upon connection
	 *
	 *    |<-INVITE (blank)---|                   |
     *    |-----OK (offer)--->|                   |
     *    |<---ACK (hold)-----|                   |
     *    |                   |---INVITE (blank)->|
     *    |                   |<----OK (offer)----|
     *    |                   |-----ACK (hold)--->|
     *    ============= join call legs ============
     *    |<-ReINVITE (blank)-|                   |
     *    |----OK (offer)---->|                   |
     *    |                   |-ReINVITE (offer)->|
	 *    |                   |<----OK (answer)---|
     *    |<---ACK (answer)---|-------ACK-------->|
	 */
	@Test
	public void testDefaultSipFlowTwoDialledDialogs() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());

		outboundCallLegBean.connectCallLeg(firstDialogId);
		outboundCallLegBean.connectCallLeg(secondDialogId);

		// invite-trying-ringing-ok-ack
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.SecondInbound);

		// act
		callBean.joinCallLegs(firstDialogId, secondDialogId);

		// assert
		waitForEmptyReinviteRespondOk(SipUnitPhone.Inbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());
		waitForEmptyAck(SipUnitPhone.SecondInbound);

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.SecondInbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	/** test that two non-connecting outgoing dialogs are joined together sequentially
	 * Also test that 1st invite to 1st dialog has no sdp but ack has sdp
	 * And 1st invite to 2nd dialog has no sdp but ack has sdp
     * And RE-invite to both dialogs has sdp but ack has no sdp
     *
     *    ============= join call legs ============
	 *    |<-INVITE (blank)---|                   |
     *    |-----OK (offer)--->|                   |
     *    |<---ACK (hold)-----|                   |
     *    |                   |---INVITE (blank)->|
     *    |                   |<----OK (offer)----|
     *    |<-ReINVITE (offer)-|                   |
     *    |----OK (answer)--->|                   |
     *    |<--------ACK-------|----ACK (answer)-->|
	 */
	@Test
	public void testThirdPartyCallTwoInitialDialogs() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());

		// act
		callBean.joinCallLegs(firstDialogId, secondDialogId);

		// assert
		// invite 1st leg
		assertTrue("Timed out waiting for incoming call", this.getInboundCall().waitForIncomingCall(5000));
		assertEquals(getInboundPhoneSipAddress(), this.getInboundCall().getLastReceivedRequest().getRequestURI());
		assertTrue(getInboundCall().getLastReceivedRequest().getContentLength() == 0);
		getInboundCall().sendIncomingCallResponse(Response.TRYING, "Trying", 0);
		getInboundCall().sendIncomingCallResponse(Response.RINGING, "Ringing", 0);

		// NOW check that we haven't had an invite for the second dialog yet
		assertFalse("Got invite for callee dialog in 3pc before ok-ing the caller dialog", getSecondInboundCall().waitForIncomingCall(200));

		// ok - ack
		SessionDescriptionHelper.setMediaDescription(getInboundPhoneSdp(), getInboundPhoneMediaDescription());
		getInboundCall().sendIncomingCallResponse(Response.OK, "OK", 0, getInboundPhoneSdp().toString(), "application", "sdp", null, null);
		assertTrue("Timed out waiting for ACK", this.getInboundCall().waitForAck(5000));
		assertTrue(new String(getInboundCall().getLastReceivedRequest().getRawContent()).contains("IN IP4 0.0.0.0"));

		// second invite
		assertTrue("Timed out waiting for second incoming call", getSecondInboundCall().waitForIncomingCall(5000));
		assertEquals(getSecondInboundPhoneSipAddress(), getSecondInboundCall().getLastReceivedRequest().getRequestURI());
		assertTrue(getSecondInboundCall().getLastReceivedRequest().getContentLength() == 0);

		// second trying - ringing - ok
		getSecondInboundCall().sendIncomingCallResponse(Response.TRYING, "Trying", 0);
		getSecondInboundCall().sendIncomingCallResponse(Response.RINGING, "Ringing", 0);
		SessionDescriptionHelper.setMediaDescription(getSecondInboundPhoneSdp(), getSecondInboundPhoneMediaDescription());
		getSecondInboundCall().sendIncomingCallResponse(Response.OK, "OK", 0, getSecondInboundPhoneSdp().toString(), "application", "sdp", null, null);

		// reinvite first dialog
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());
		// ack
		assertTrue("Timed out waiting for ACK", this.getSecondInboundCall().waitForAck(5000));
		// no sdp in ack for 2nd dialog
		assertMediaDescriptionInSessionDescription(getInboundPhoneMediaDescription(), SdpFactory.getInstance().createSessionDescription(new String(getSecondInboundCall().getLastReceivedRequest().getRawContent())));

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.SecondInbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

// TODO: should work like this
	/** test that a connecting caller and a non-connecting callee dialog are joined together in parallel
	 *
	 *    |<-INVITE (blank)---|                   |
	 *    ============= join call legs ============
     *    |-----OK (offer)--->|                   |
     *    |<---ACK (hold)-----|                   |
     *    |                   |---INVITE (blank)->|
     *    |                   |<----OK (offer)----|
     *    |<-ReINVITE (offer)-|                   |
     *    |----OK (answer)--->|                   |
     *    |<--------ACK-------|----ACK (answer)-->|
	 */
	@Test
	public void testDefaultSipFlowConnectingAndInitialDialogs() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());

		// act
		outboundCallLegBean.connectCallLeg(firstDialogId);
		callBean.joinCallLegs(firstDialogId, secondDialogId);

		assertTrue(getInboundCall().listenForReinvite());
		assertTrue(getSecondInboundCall().listenForReinvite());

		// assert
		// invite
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		waitForCallSendTryingRingingOk(SipUnitPhone.SecondInbound);
		waitForAckAssertMediaDescription(SipUnitPhone.SecondInbound, getSecondInboundPhoneHoldMediaDescription());

		// reinvite
		waitForEmptyReinviteRespondOk(SipUnitPhone.Inbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.SecondInbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

// TODO: should work like this
	/** test that a connected caller and a non-connecting callee dialog are joined together in parallel
	 *
	 *    |<-INVITE (blank)---|                   |
     *    |-----OK (offer)--->|                   |
     *    |<---ACK (hold)-----|                   |
     *    ============= join call legs ============
     *    |                   |---INVITE (blank)->|
     *    |                   |<----OK (offer)----|
     *    |<-ReINVITE (offer)-|                   |
     *    |----OK (answer)--->|                   |
     *    |<--------ACK-------|----ACK (answer)-->|
	 */
	@Test
	public void testDefaultSipFlowConnectedAndInitialDialogs() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());

		assertTrue(getInboundCall().listenForReinvite());
		assertTrue(getSecondInboundCall().listenForReinvite());

		outboundCallLegBean.connectCallLeg(firstDialogId);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);

		// act
   		callBean.joinCallLegs(firstDialogId, secondDialogId);

   		//assert
		waitForCallSendTryingRingingOk(SipUnitPhone.SecondInbound);

		// reinvite
		waitForEmptyReinviteRespondOk(SipUnitPhone.Inbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.SecondInbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	/** test that a connected caller and a connecting callee dialog are joined together in parallel
	 *
	 *    |<-INVITE (blank)---|                   |
     *    |-----OK (offer)--->|                   |
     *    |<---ACK (hold)-----|                   |
     *    |                   |---INVITE (blank)->|
     *    ============= join call legs ============
     *    |                   |<----OK (offer)----|
     *    |                   |----ACK (hold)---->|
     *    |<-ReINVITE (blank)-|                   |
     *    |----OK (offer)---->|                   |
     *    |                   |-ReINVITE (offer)->|
	 *    |                   |<----OK (answer)---|
     *    |<---ACK (answer)---|-------ACK-------->|
	 */
	@Test
	public void testDefaultSipFlowConnectedAndConnectingDialogs() throws Exception {
		// setup
		final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		final String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());

		// act
		outboundCallLegBean.connectCallLeg(firstDialogId);

		// assert
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		assertTrue(getInboundCall().listenForReinvite());

		// act
		outboundCallLegBean.connectCallLeg(secondDialogId);
		callBean.joinCallLegs(firstDialogId, secondDialogId);

		// assert
		waitForCallSendTryingRingingOk(SipUnitPhone.SecondInbound);

		// reinvite
		waitForEmptyReinviteRespondOk(SipUnitPhone.Inbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.SecondInbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	/** test that a non-connecting caller and a connecting callee dialog are joined together in parallel
	 *
	 *    |                   |---INVITE (blank)->|
	 *    ============= join call legs ============
	 *    |<-INVITE (blank)---|                   |
	 *    |                   |<----OK (offer)----|
     *    |-----OK (offer)--->|                   |
     *    |<---ACK (hold)-----|                   |
     *    |                   |-----ACK (hold)--->|
     *    |<-ReINVITE (blank)-|                   |
     *    |----OK (offer)---->|                   |
     *    |                   |-ReINVITE (offer)->|
	 *    |                   |<----OK (answer)---|
     *    |<---ACK (answer)---|-------ACK-------->|
	 */
	@Test
	public void testDefaultSipFlowInitialAndConnectingDialogs() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());

		// act
		outboundCallLegBean.connectCallLeg(secondDialogId);
		callBean.joinCallLegs(firstDialogId, secondDialogId);

		// assert
		// invite
		assertTrue("Timed out waiting for incoming call", this.getInboundCall().waitForIncomingCall(5000));
		assertEquals(getInboundPhoneSipAddress(), this.getInboundCall().getLastReceivedRequest().getRequestURI());

		assertTrue("Timed out waiting for second incoming call", getSecondInboundCall().waitForIncomingCall(5000));
		assertEquals(getSecondInboundPhoneSipAddress(), getSecondInboundCall().getLastReceivedRequest().getRequestURI());

		// trying - ringing - ok
		getInboundCall().sendIncomingCallResponse(Response.TRYING, "Trying", 0);
		getInboundCall().sendIncomingCallResponse(Response.RINGING, "Ringing", 0);
		assertTrue(getInboundCall().listenForReinvite());
		SessionDescriptionHelper.setMediaDescription(getInboundPhoneSdp(), getInboundPhoneMediaDescription());
		getInboundCall().sendIncomingCallResponse(Response.OK, "OK", 0, getInboundPhoneSdp().toString(), "application", "sdp", null, null);

		getSecondInboundCall().sendIncomingCallResponse(Response.TRYING, "Trying", 0);
		getSecondInboundCall().sendIncomingCallResponse(Response.RINGING, "Ringing", 0);
		assertTrue(getSecondInboundCall().listenForReinvite());
		SessionDescriptionHelper.setMediaDescription(getSecondInboundPhoneSdp(), getSecondInboundPhoneMediaDescription());
		getSecondInboundCall().sendIncomingCallResponse(Response.OK, "OK", 0, getSecondInboundPhoneSdp().toString(), "application", "sdp", null, null);

		// ack
		assertTrue("Timed out waiting for ACK", this.getInboundCall().waitForAck(5000));
		assertTrue("Timed out waiting for second call ACK", getSecondInboundCall().waitForAck(5000));

		// reinvite
		waitForEmptyReinviteRespondOk(SipUnitPhone.Inbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.SecondInbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

// TODO: remote back-to-back ack / reinvite
	/** test that a connected caller and a non-connecting callee dialog are joined together in parallel
	 *
	 *    |                   |---INVITE (blank)->|
	 *    |                   |<----OK (offer)----|
	 *    |                   |-----ACK (hold)--->|
	 *    ============= join call legs ============
	 *    |<-INVITE (blank)---|                   |
     *    |-----OK (offer)--->|                   |
     *    |<---ACK (hold)-----|                   |
     *    |<-ReINVITE (blank)-|                   |
     *    |----OK (offer)---->|                   |
     *    |                   |-ReINVITE (offer)->|
	 *    |                   |<----OK (answer)---|
     *    |<---ACK (answer)---|-------ACK-------->|
	 */
	@Test
	public void testDefaultSipFlowInitialAndConnectedDialogs() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());

		// act
		outboundCallLegBean.connectCallLeg(secondDialogId);

		// assert
		// invite
		SessionDescriptionHelper.setMediaDescription(getSecondInboundPhoneSdp(), getSecondInboundPhoneMediaDescription());
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.SecondInbound);
		assertTrue(getSecondInboundCall().listenForReinvite());

		// act
		callBean.joinCallLegs(firstDialogId, secondDialogId);

		// assert
		SessionDescriptionHelper.setMediaDescription(getInboundPhoneSdp(), getInboundPhoneMediaDescription());
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);

		// reinvite
		waitForEmptyReinviteRespondOk(SipUnitPhone.Inbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.SecondInbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	/** test that a connecting caller and a connected callee dialog are joined together
	 *
	 *    |                   |---INVITE (blank)->|
	 *    |                   |<----OK (offer)----|
	 *    |                   |-----ACK (hold)--->|
	 *    |<-INVITE (blank)---|                   |
	 *    ============= join call legs ============
     *    |-----OK (offer)--->|                   |
     *    |<---ACK (hold)-----|                   |
     *    |<-ReINVITE (blank)-|                   |
     *    |----OK (offer)---->|                   |
     *    |                   |-ReINVITE (offer)->|
	 *    |                   |<----OK (answer)---|
     *    |<---ACK (answer)---|-------ACK-------->|
	 */
	@Test
	public void testDefaultSipFlowConnectingAndConnectedDialogs() throws Exception {
		// setup
		final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		final String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		assertTrue(getInboundCall().listenForReinvite());
		assertTrue(getSecondInboundCall().listenForReinvite());

		// act
		outboundCallLegBean.connectCallLeg(secondDialogId);

		// assert
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.SecondInbound);

		// act
		outboundCallLegBean.connectCallLeg(firstDialogId);
		callBean.joinCallLegs(firstDialogId, secondDialogId);

		// assert
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);

		// reinvite
		waitForEmptyReinviteRespondOk(SipUnitPhone.Inbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.SecondInbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	// test that call creation returns a call id
	@Test
	public void testCreateReturnsCallId() throws Exception {
		// act
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());

		String callId = callBean.joinCallLegs(firstDialogId, secondDialogId);
		setupThirdPartyCall();

		// assert
		assertNotNull(callId);
		assertTrue(callId.length() > 0);
	}

	// test that two joined connected dialogs are terminated as expected
	@Test
	public void testThirdPartyCallTerminationConnectedDialogs() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());

		String callId = callBean.joinCallLegs(firstDialogId, secondDialogId);
		setupThirdPartyCall();

		// act
		callBean.terminateCall(callId);

		// assert
		assertTrue("No disconnect for dialog 1", getInboundCall().waitForDisconnect(5000));
		assertTrue("Dialog 1 failed to respond to disconnect", getInboundCall().respondToDisconnect());

		assertTrue("No disconnect for dialog 2", getSecondInboundCall().waitForDisconnect(5000));
		assertTrue("Dialog 2 failed to respond to disconnect", getSecondInboundCall().respondToDisconnect());
	}

	// test that duplicate terminate doesn't throws any exception
	@Test
	public void testThirdPartyCallDoubleTerminationDoesntFail() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());

		String callId = callBean.joinCallLegs(firstDialogId, secondDialogId);
		setupThirdPartyCall();

		callBean.terminateCall(callId);

		assertTrue("No disconnect for dialog 1", getInboundCall().waitForDisconnect(5000));
		assertTrue("Dialog 1 failed to respond to disconnect", getInboundCall().respondToDisconnect());

		assertTrue("No disconnect for dialog 2", getSecondInboundCall().waitForDisconnect(5000));
		assertTrue("Dialog 2 failed to respond to disconnect", getSecondInboundCall().respondToDisconnect());

		Thread.sleep(500);

		// act
		callBean.terminateCall(callId);
	}

	// test that termination of a non existent call fails
	@Test(expected=IllegalArgumentException.class)
	public void testThirdPartyCallTerminationNonExistentCalls() throws Exception {
		// act
		callBean.terminateCall("unknown_id");
	}

	// test that two joined non-connected dialogs are terminated as expected
	@Test
	public void testThirdPartyCallTerminationNonConnectedDialogs() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());

		String callId = callBean.joinCallLegs(firstDialogId, secondDialogId);

		// trying
		assertTrue("Timed out waiting for incoming call", this.getInboundCall().waitForIncomingCall(5000));
		getInboundCall().sendIncomingCallResponse(Response.TRYING, "Trying", 0);
		getInboundPhone().listenRequestMessage();
		Thread.sleep(500);

		// act
		callBean.terminateCall(callId);

		RequestEvent re = getInboundPhone().waitRequest(5000);
		assertTrue(re != null);
        while (re.getRequest().getMethod().equals(Request.CANCEL) == false) {
    		re = getInboundPhone().waitRequest(5000);
    		assertTrue(re != null);
        }
	}

    // test that an exception is thrown when first dialog is Terminating
	@Test(expected=IllegalStateException.class)
	public void testThirdPartyCallFirstDialogTerminating() throws Exception {
        // setup
        final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
        String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());

        final DialogCollection dialogCollection = (DialogCollection)getApplicationContext().getBean("dialogCollection");
        ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = dialogCollection.get(firstDialogId);
		        dialogInfo.setTerminationMethod(TerminationMethod.Terminate);
		        dialogCollection.replace(dialogInfo);
			}

			public String getResourceId() {
				return firstDialogId;
			}
        };
        new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

        // act
        callBean.joinCallLegs(firstDialogId, secondDialogId);
    }

    // test that an exception is thrown when first dialog is Terminated
	@Test(expected=IllegalStateException.class)
	public void testThirdPartyCallFirstDialogTerminated() throws Exception {
        // setup
        final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
        String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());

        final DialogCollection dialogCollection = (DialogCollection)getApplicationContext().getBean("dialogCollection");
        ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
		        DialogInfo dialogInfo = dialogCollection.get(firstDialogId);
		        dialogInfo.setDialogState(DialogState.Terminated);
		        dialogCollection.replace(dialogInfo);
			}

			public String getResourceId() {
				return firstDialogId;
			}
        };
        new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

        // act
        callBean.joinCallLegs(firstDialogId, secondDialogId);
    }

    // test that an exception is thrown when second dialog is Terminating
	@Test(expected=IllegalStateException.class)
	public void testThirdPartyCallSecondDialogTerminating() throws Exception {
        // setup
        final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
        String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());

        final DialogCollection dialogCollection = (DialogCollection)getApplicationContext().getBean("dialogCollection");
        ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
		        DialogInfo dialogInfo = dialogCollection.get(firstDialogId);
		        dialogInfo.setDialogState(DialogState.Terminated);
		        dialogCollection.replace(dialogInfo);
			}

			public String getResourceId() {
				return firstDialogId;
			}
        };
        new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

        // act
        callBean.joinCallLegs(firstDialogId, secondDialogId);
    }

    // test that an exception is thrown when second dialog is Terminated
	@Test(expected=IllegalStateException.class)
	public void testThirdPartyCallSecondDialogTerminated() throws Exception {
        // setup
        final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
        String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());

        final DialogCollection dialogCollection = (DialogCollection)getApplicationContext().getBean("dialogCollection");
        ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
		        DialogInfo dialogInfo = dialogCollection.get(firstDialogId);
		        dialogInfo.setDialogState(DialogState.Terminated);
		        dialogCollection.replace(dialogInfo);
			}

			public String getResourceId() {
				return firstDialogId;
			}
        };
        new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

        // act
        callBean.joinCallLegs(firstDialogId, secondDialogId);
    }

    // test that an exception is thrown when first dialog is non-existent
	@Test(expected=IllegalArgumentException.class)
	public void testThirdPartyCallFirstDialogNonExistent() throws Exception {
        // setup
        String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());

        // act
        callBean.joinCallLegs("blah", firstDialogId);
    }

    // test that an exception is thrown when second dialog is non-existent
	@Test(expected=IllegalArgumentException.class)
	public void testThirdPartyCallSecondDialogNonExistent() throws Exception {
        // setup
        String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());

        // act
        callBean.joinCallLegs(firstDialogId, "blah");
    }

    // test that an exception is thrown when first dialog id is null
	@Test(expected=IllegalArgumentException.class)
	public void testThirdPartyCallFirstDialogNull() throws Exception {
        // setup
        String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());

        // sct
        callBean.joinCallLegs(null, firstDialogId);
    }

    // test that an exception is thrown when second dialog id is null
	@Test(expected=IllegalArgumentException.class)
	public void testThirdPartyCallSecondDialogNull() throws Exception {
        // setup
        String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());

        // sct
        callBean.joinCallLegs(firstDialogId, null);
    }

	private void setupThirdPartyCall() throws Exception {
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		waitForCallSendTryingRingingOk(SipUnitPhone.SecondInbound);

		// reinvite
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());
		waitForAckAssertMediaDescription(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());
	}

	// test that two dialogs being joined together where one of them was previously in a call terminates the original call
	@Test
	public void testTwoDialogsJoinedOneAlreadyInACallOriginalCallTerminated() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		String thirdDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getThirdInboundPhoneSipUri());

		String callId = callBean.joinCallLegs(firstDialogId, secondDialogId);
		setupThirdPartyCall();

		// act
		String secondCallId = callBean.joinCallLegs(secondDialogId, thirdDialogId);

		// assert
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.ThirdInbound);

		// reinvites
		waitForEmptyReinviteRespondOk(SipUnitPhone.SecondInbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.ThirdInbound, getSecondInboundPhoneMediaDescription());
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.SecondInbound, getThirdInboundPhoneMediaDescription());

        waitForCallState(CallState.Terminated, callId, 600, 100);
        waitForCallState(CallState.Connected, secondCallId, 600, 100);
	}

    private void waitForCallState(CallState callState, String callId, int totalMilliseconds, int intervalMilliseconds) throws Exception {
        if (callState.equals(callCollection.get(callId).getCallState())) return;
        int loopCount = totalMilliseconds / intervalMilliseconds;
        for (int i = 0; i < loopCount; i++) {
            Thread.sleep(intervalMilliseconds);
            if (callState.equals(callCollection.get(callId).getCallState())) return;
        }
        fail("Call not " + callState);
    }

	// test that two dialogs being joined together where one is already being dialled terminates the original call
	@Test
	public void testTwoDialogsJoinedOneAlreadyBeingDialledOriginalCallTerminated() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		String thirdDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getThirdInboundPhoneSipUri());

		// act
		String callId = callBean.joinCallLegs(firstDialogId, secondDialogId);
		assertTrue(getInboundCall().waitForIncomingCall(5000));
		outboundCallLegBean.connectCallLeg(secondDialogId, AutoTerminateAction.Unchanged, null, null, true);
		String secondCallId = callBean.joinCallLegs(secondDialogId, thirdDialogId);

		// assert
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.SecondInbound);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.ThirdInbound);

		// reinvites
		waitForEmptyReinviteRespondOk(SipUnitPhone.SecondInbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.ThirdInbound, getSecondInboundPhoneMediaDescription());
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.SecondInbound, getThirdInboundPhoneMediaDescription());

        waitForCallState(CallState.Terminated, callId, 600, 100);
        waitForCallState(CallState.Connected, secondCallId, 600, 100);
	}

	// test that 2nd dialog is terminated when 1st dialog disconnects if the flag to auto-terminate-dialogs is set
	@Test
	public void testSecondDialogTerminatedWhenFirstDialogDisconnectsAutoTerminateDialogs() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		callBean.joinCallLegs(firstDialogId, secondDialogId, AutoTerminateAction.True);
		setupThirdPartyCall();

		// act
		getInboundCall().disconnect();
		// assert
		// second dialog should receive a disconnect
		assertTrue("Timed out waiting for BYE", getSecondInboundCall().waitForDisconnect(5000));
		assertTrue("Unable to respond to BYE", getSecondInboundCall().respondToDisconnect());
	}

	// test that 1st dialog is terminated when 2nd dialog disconnects if the flag to auto-terminate-dialogs is set
	@Test
	public void testFirstDialogTerminatedWhenSecondDialogDisconnectsAutoTerminateDialogs() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		callBean.joinCallLegs(firstDialogId, secondDialogId, AutoTerminateAction.True);
		setupThirdPartyCall();

		// act
		getSecondInboundCall().disconnect();

		// assert
		// first dialog should receive a disconnect
		assertTrue("Timed out waiting for BYE", getInboundCall().waitForDisconnect(5000));
		assertTrue("Unable to respond to BYE", getInboundCall().respondToDisconnect());
	}

	// test that 1st dialog is terminated when 2nd dialog responds to invite with an error response
	@Test
	public void testFirstDialogTerminatedWhenSecondDialogDoesNotConnectAutoTerminateDialogs() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		callBean.joinCallLegs(firstDialogId, secondDialogId, AutoTerminateAction.True);

		// invite-trying-ringing-ok-ack
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);

		// second invite-trying-ringing-busy
		waitForCallSendTryingBusyHere(getSecondInboundCall());

		// assert
		// first dialog should receive a disconnect
		assertTrue("Timed out waiting for BYE", getInboundCall().waitForDisconnect(5000));
		assertTrue("Unable to respond to BYE", getInboundCall().respondToDisconnect());
	}

	// test that 2nd dialog is terminated when 1st dialog is terminated if the flag to auto-terminate-dialogs is set
	@Test
	public void testSecondDialogTerminatedWhenFirstDialogTerminatedAutoTerminateDialogs() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		callBean.joinCallLegs(firstDialogId, secondDialogId, AutoTerminateAction.True);
		setupThirdPartyCall();

		// act
		outboundCallLegBean.terminateCallLeg(firstDialogId);
		assertTrue("Timed out waiting for BYE", getInboundCall().waitForDisconnect(5000));
		assertTrue("Unable to respond to BYE", getInboundCall().respondToDisconnect());

		// assert
		// second dialog should receive a disconnect
		assertTrue("Timed out waiting for BYE", getSecondInboundCall().waitForDisconnect(5000));
		assertTrue("Unable to respond to BYE", getSecondInboundCall().respondToDisconnect());
	}

	// test that 1st dialog is terminated when 2nd dialog is terminated if the flag to auto-terminate-dialogs is set
	@Test
	public void testFirstDialogTerminatedWhenSecondDialogTerminatedAutoTerminateDialogs() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		callBean.joinCallLegs(firstDialogId, secondDialogId, AutoTerminateAction.True);
		setupThirdPartyCall();

		// act
		outboundCallLegBean.terminateCallLeg(secondDialogId);
		assertTrue("Timed out waiting for BYE", getSecondInboundCall().waitForDisconnect(5000));
		assertTrue("Unable to respond to BYE", getSecondInboundCall().respondToDisconnect());

		// assert
		// first dialog should receive a disconnect
		assertTrue("Timed out waiting for BYE", getInboundCall().waitForDisconnect(5000));
		assertTrue("Unable to respond to BYE", getInboundCall().respondToDisconnect());
	}

	// tests that joining a dialog w/ a max call duration creates a call info w/ a max call duration
	@Test
	public void testCallInfoHasMaxDuration() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());

		// act
		String callId = callBean.joinCallLegs(firstDialogId, secondDialogId, AutoTerminateAction.False, 1);

		// assert
		ReadOnlyCallInfo callInfo = callCollection.get(callId);
		assertEquals(1, callInfo.getMaxDurationInMinutes());
	}

	// test that when a call leg is left hanging after other call leg has switched to another call, the hanging call leg gets a reinvite with hold sdp
	@Test
	public void testNoMediaForNonAutoTerminatedDialogOnOtherDialogSwitchedToNewCall() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		String thirdDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getThirdInboundPhoneSipUri());

		callBean.joinCallLegs(firstDialogId, secondDialogId, AutoTerminateAction.False);
		setupThirdPartyCall();
		callBean.joinCallLegs(secondDialogId, thirdDialogId);

		// invite, trying, ringing, ok, ack
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.ThirdInbound);

		// reinvites
		waitForEmptyReinviteRespondOk(SipUnitPhone.SecondInbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.ThirdInbound, getSecondInboundPhoneMediaDescription());
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.SecondInbound, getThirdInboundPhoneMediaDescription());

		// assert
		assertTrue(getInboundCall().listenForReinvite());
		SipTransaction reinviteTrans = getInboundCall().waitForReinvite(5000);
		assertNotNull("First dialog timed out waiting for reinvite", reinviteTrans);
		assertTrue("Hold sdp not received", new String(getInboundCall().getLastReceivedRequest().getRawContent()).indexOf("IN IP4 0.0.0.0") > -1);

		SessionDescriptionHelper.setMediaDescription(getInboundPhoneSdp(), getInactiveHoldMediaDescription());
		getInboundCall().respondToReinvite(reinviteTrans, Response.OK, "OK", 0, getInboundPhoneSipAddress(), "display name", getInboundPhoneSdp().toString(), "application", "sdp");
		assertTrue(getInboundCall().waitForAck(5000));
		assertEquals("Hold sdp not expected in ACK", 0, getInboundCall().getLastReceivedRequest().getContentLength());
	}

	// test that when a dialog is in a call and we receive an IncomingDialogRefreshEvent, we proxy the reinvite request to the 2nd party, and proxy back the response
	@Test
	public void testDialogRefreshEventForDialogInCallProxiesCorrectly() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		callBean.joinCallLegs(firstDialogId, secondDialogId);

		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		waitForCallSendTryingRingingOk(SipUnitPhone.SecondInbound);

		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());
		waitForAckAssertMediaDescription(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());
		waitForEmptyAck(SipUnitPhone.Inbound);

		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));

		// act
		SessionDescriptionHelper.setMediaDescription(getInboundPhoneSdp(), getThirdInboundPhoneMediaDescription());
		SipTransaction reinviteTransaction = getInboundCall().sendReinvite(getInboundPhoneSipAddress(), getInboundPhoneSipAddress(), getInboundPhoneSdp().toString(), "application", "sdp");

		// assert
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.SecondInbound, getThirdInboundPhoneMediaDescription());
		waitForReinviteOKResponseAndAssertMediaDescription(getInboundPhone(), reinviteTransaction, getSecondInboundPhoneMediaDescription());
        assertTrue(getInboundCall().sendReinviteOkAck(reinviteTransaction));
	}

	// test call status retrieval null dialog
	@Test(expected=IllegalArgumentException.class)
	public void testGetCallInformationNullCallId() throws Exception {
		// act
		callBean.getCallInformation(null);
	}

	// test call info retrieval unknown dialog
	@Test(expected=IllegalArgumentException.class)
	public void testGetCallInformatinoUnknownCallId() throws Exception {
		// act
		callBean.getCallInformation("unknown");
	}

	// Test that getter for call information returns correct data
	@Test
	public void testGetCallInformation() throws Exception {
		//setup
		CallInfo callInfo = new CallInfo("beanName", "a", "first", "second", AutoTerminateAction.True, 0);
		long startTime = Calendar.getInstance().getTimeInMillis() - 10;
		long endTime = Calendar.getInstance().getTimeInMillis();

		callInfo.setCallState(CallState.Terminated);
		callInfo.setCallTerminationCause(CallTerminationCause.RemotePartyHungUp, CallLegCausingTermination.Neither);
		callInfo.setStartTime(startTime);
		callInfo.setEndTime(endTime);
		callCollection.add(callInfo);

		//act
		CallInformation callInformation = callBean.getCallInformation(callInfo.getId());

		//assert
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(startTime);
		assertEquals(c, callInformation.getStartTime());
		assertEquals(callInfo.getDuration(), callInformation.getDuration());
		assertEquals(CallState.Terminated, callInformation.getCallState());
		assertEquals(CallTerminationCause.RemotePartyHungUp, callInformation.getCallTerminationCause());
		assertEquals(CallLegCausingTermination.Neither, callInformation.getCallLegCausingTermination());
	}

	/**
	 * Tests that after setting up a TPC no autoterminate w/ a regular dialog and a dialog with autoterminate true
	 * make sure the regular dialog is not set to autoterminate
	 */
	@Test
	public void testFirstCallLegAutoTerminateIsntJustTheSecondCallLegsAutoTerminate() throws Exception {
		// setup
		final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		final String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
        ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo info = dialogCollection.get(secondDialogId);
				info.setAutoTerminate(true);
				dialogCollection.replace(info);
			}

			public String getResourceId() {
				return secondDialogId;
			}
        };
        new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);
		callBean.joinCallLegs(firstDialogId, secondDialogId);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		waitForCallSendTryingRingingOk(SipUnitPhone.SecondInbound);

		// act
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());

		// assert
		ReadOnlyDialogInfo readonlyInfo1 = dialogCollection.get(firstDialogId);
		assertFalse("First Call leg should not have autoterminate set", readonlyInfo1.isAutoTerminate());
	}

	/**
	 * Tests the following scenario:
	 * 	 call legs 1 & 2 are connected w/ #2 set to autoterminate (like a media call)
	 *   call legs 3 & 1 are connected (in that order) so #2 autoterminates
	 *   call leg 3 disconnects but 1 shouldn't autoterminate
	 */
	@Test
	public void testCallLegsAutoTerminationSettingAffectedByOrderOfJoinCallLegParameters() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		final String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
        ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo info = dialogCollection.get(secondDialogId);
				info.setAutoTerminate(true);
				dialogCollection.replace(info);
			}

			public String getResourceId() {
				return secondDialogId;
			}
        };
        new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);
		String thirdDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getThirdInboundPhoneSipUri());

		callBean.joinCallLegs(firstDialogId, secondDialogId);
		setupThirdPartyCall();

		getSecondInboundCall().listenForDisconnect();
		callBean.joinCallLegs(thirdDialogId, firstDialogId);

		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.ThirdInbound);
		waitForEmptyReinviteRespondOk(SipUnitPhone.ThirdInbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Inbound, getThirdInboundPhoneMediaDescription());
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.ThirdInbound, getInboundPhoneMediaDescription());

		// second dialog should receive a disconnect
		assertTrue("Timed out waiting for BYE", getSecondInboundCall().waitForDisconnect(5000));
		assertTrue("Unable to respond to BYE", getSecondInboundCall().respondToDisconnect());

		getThirdInboundCall().listenForDisconnect();
		getInboundCall().listenForDisconnect();

		// act
		outboundCallLegBean.terminateCallLeg(thirdDialogId);

		// assert
		// third dialog should receive a disconnect
		assertTrue("Timed out waiting for BYE", getThirdInboundCall().waitForDisconnect(5000));
		assertTrue("Unable to respond to BYE", getThirdInboundCall().respondToDisconnect());

		// first dialog should not!
		assertEquals("First dialog should be connected", DialogState.Confirmed, dialogCollection.get(firstDialogId).getDialogState());
		assertFalse("Should not get a BYE", getInboundCall().waitForDisconnect(500));
	}

	/** test call setup for automaton 3pc
	 *
	 *    |<-INVITE (blank)---|                   |
     *    |-----OK (offer)--->|                   |
     *    |                   |---INVITE (offer)->|
     *    |                   |<----OK (answer)---|
     *    |<---ACK (answer)---|-------ACK-------->|
	 */
	@Test
	public void testAutomataThirdPartyCallSipFlow() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		makeCallLegAutomaton(secondDialogId);

		// act
		callBean.joinCallLegs(firstDialogId, secondDialogId);

		// assert
		setupAutomataThirdPartyCall();

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.SecondInbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	/** test call setup for automaton 3pc where the first leg is already connected
	 *
	 *    |<-INVITE (blank)---|                   |
     *    |-----OK (offer)--->|                   |
     *    |<---ACK (hold)-----|                   |
     *    ============= join call legs ============
     *    |<-ReINVITE (blank)-|                   |
     *    |----OK (offer)---->|                   |
     *    |                   |---INVITE (offer)->|
     *    |                   |<----OK (answer)---|
     *    |<---ACK (answer)---|--------ACK------->|
     */
	@Test
	public void testAutomataThirdPartyCallFirstLegConnected() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());

		makeCallLegAutomaton(secondDialogId);

		outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False, null, null, true);

		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);

		// act
		callBean.joinCallLegs(firstDialogId, secondDialogId);

		// assert
		waitForEmptyReinviteRespondOk(SipUnitPhone.Inbound);
		waitForCallAssertMediaDescriptionSendTryingRingingOk(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());
		assertTrue(getSecondInboundCall().waitForAck(5000));
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.SecondInbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	/** test call setup for automaton 3pc where the second leg is already connected
	 *
	 *    |                   |---INVITE (blank)->|
	 *    |                   |<----OK (offer)----|
	 *    |                   |-----ACK (hold)--->|
	 *    ============= join call legs ============
	 *    |<-INVITE (blank)---|                   |
     *    |-----OK (offer)--->|                   |
     *    |                   |-ReINVITE (offer)->|
     *    |                   |<----OK (answer)---|
     *    |<---ACK (answer)---|-------ACK-------->|
	 */
	@Test
	public void testAutomataThirdPartyCallSecondLegConnected() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());

		makeCallLegAutomaton(secondDialogId);

		outboundCallLegBean.connectCallLeg(secondDialogId, AutoTerminateAction.False, null, null, true);

		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.SecondInbound);

		// act
		callBean.joinCallLegs(firstDialogId, secondDialogId);

		// assert
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());
		assertTrue(getSecondInboundCall().waitForAck(5000));
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.SecondInbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	/** test call setup for automaton 3pc where the first leg is already connecting
	 *
	 *    |<-INVITE (blank)---|                   |
	 *    ============= join call legs ============
     *    |-----OK (offer)--->|                   |
     *    |<---ACK (hold)-----|                   |
     *    |<-ReINVITE (blank)-|                   |
     *    |----OK (offer)---->|                   |
     *    |                   |---INVITE (offer)->|
     *    |                   |<----OK (answer)---|
     *    |<---ACK (answer)---|--------ACK------->|
	 */
	@Test
	public void testAutomataThirdPartyCallFirstLegConnecting() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		makeCallLegAutomaton(secondDialogId);

		outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False, null, null, true);

		waitForCallSendTryingRinging(getInboundCall());

		// act
		callBean.joinCallLegs(firstDialogId, secondDialogId);
		respondWithInitialOk(SipUnitPhone.Inbound);
		assertTrue(getInboundCall().waitForAck(5000));

		// assert
		waitForEmptyReinviteRespondOk(SipUnitPhone.Inbound);
		waitForCallAssertMediaDescriptionSendTryingRingingOk(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());
		assertTrue(getSecondInboundCall().waitForAck(5000));
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.SecondInbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	/** test call setup for automaton 3pc where the second leg is already connecting
	 *
	 *    |                   |---INVITE (blank)->|
	 *    ============= join call legs ============
	 *    |<-INVITE (blank)---|                   |
	 *    |                   |<----OK (offer)----|
	 *    |                   |-----ACK (hold)--->|
     *    |-----OK (offer)--->|                   |
     *    |                   |-ReINVITE (offer)->|
     *    |                   |<----OK (answer)---|
     *    |<---ACK (answer)---|-------ACK-------->|
	 */
	@Test
	public void testAutomataThirdPartyCallSecondLegConnecting() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		makeCallLegAutomaton(secondDialogId);

		outboundCallLegBean.connectCallLeg(secondDialogId, AutoTerminateAction.False, null, null, true);

		waitForCallSendTryingRinging(getSecondInboundCall());

		// act
		callBean.joinCallLegs(firstDialogId, secondDialogId);
		respondWithInitialOk(SipUnitPhone.SecondInbound);
		assertTrue(getSecondInboundCall().waitForAck(5000));

		// assert
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.SecondInbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

// TODO: start negotiation with leg that connected first to avoid sending reinvite straight after ack
	/** test call setup for automaton 3pc where both legs are already connecting
	 *
	 *    |                   |---INVITE (blank)->|
	 *    |                   |<----OK (offer)----|
	 *    |<-INVITE (blank)---|                   |
	 *    |-----OK (offer)--->|                   |
	 *    ============= join call legs ============
	 *    |                   |-----ACK (hold)--->|
	 *    |<---ACK (hold)-----|                   |
	 *    |<-ReINVITE (blank)-|                   |
	 *    |-----OK (offer)--->|
     *    |                   |-ReINVITE (offer)->|
     *    |                   |<----OK (answer)---|
     *    |<---ACK (answer)---|-------ACK-------->|
	 */
	@Test
	public void testAutomataThirdPartyCallBothLegsConnecting() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		makeCallLegAutomaton(secondDialogId);

		outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False, null, null, true);
		outboundCallLegBean.connectCallLeg(secondDialogId, AutoTerminateAction.False, null, null, true);

		waitForCallSendTryingRinging(getInboundCall());
		waitForCallSendTryingRinging(getSecondInboundCall());

		// act
		callBean.joinCallLegs(firstDialogId, secondDialogId);
		respondWithInitialOk(SipUnitPhone.SecondInbound);
		respondWithInitialOk(SipUnitPhone.Inbound);
		assertTrue(getInboundCall().waitForAck(5000));
		assertTrue(getSecondInboundCall().waitForAck(5000));

		// assert
		waitForEmptyReinviteRespondOk(SipUnitPhone.Inbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.SecondInbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	/** test call setup for automaton 3pc where the media server rejects the invite offer
	 *
	 *    |<-INVITE (blank)---|                   |
     *    |-----OK (offer)--->|                   |
     *    |                   |---INVITE (offer)->|
     *    |                   |<--REJECT (error)--|
     *    |<---ACK (hold)-----|                   |
	 */
	@Test
	public void testAutomataThirdPartyCallSipFlowWhereAutomatonLegRejectsInvite() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		makeCallLegAutomaton(secondDialogId);

		// act
		callBean.joinCallLegs(firstDialogId, secondDialogId);

		// assert
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForCallSendTryingBusyHere(getSecondInboundCall());
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, getHoldMediaDescription());

		assertTrue("No Connection Failed event", connectionFailedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	/** test call setup inbound automaton 3pc
	 *
	 *    |--INVITE (offer)-->|                   |
	 *    |                   |---INVITE (offer)->|
	 *    |                   |<----OK (answer)---|
     *    |<---OK (answer)----|                   |
     *    |-------ACK-------->|-------ACK-------->|
	 */
	@Test
	public void testAutomataInboundCallSipFlow() throws Exception {
		// setup
		String outboundDialogId = outboundCallLegBean.createCallLeg(new URI("sip:test"), getInboundPhoneSipUri());
		inboundCallSetupUsingCallBean.outboundDialogId = outboundDialogId;
		makeCallLegAutomaton(outboundDialogId);

		setOutboundCallTargetUsername(INBOUND_CALL_SETUP_USING_CALL_BEAN);

		// act & assert
		SessionDescriptionHelper.setMediaDescription(getOutboundPhoneSdp(), getOutboundPhoneMediaDescription());
		assertTrue(getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), getRemoteSipAddress(), getRemoteSipProxy(), getOutboundPhoneSdp().toString(), "application", "sdp", null, null));

		// invite-trying-ringing-ok-ack
		waitForCallAssertMediaDescriptionSendTryingRingingOk(SipUnitPhone.Inbound, getOutboundPhoneMediaDescription());
		waitForEmptyAck(SipUnitPhone.Inbound);

		// inbound call ok
		assertTrue("No inbound ok", getOutboundCall().waitForAnswer(5000));
		assertEquals(Response.OK, getOutboundCall().getLastReceivedResponse().getStatusCode());
		assertMediaDescriptionInSessionDescription(getInboundPhoneMediaDescription(), new String(getOutboundCall().getLastReceivedResponse().getRawContent()));
		assertFalse("Should NOT have got connected event before both legs connected", connectedSemaphore.tryAcquire(200, TimeUnit.MILLISECONDS));

		assertTrue(getOutboundCall().sendInviteOkAck());

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.Outbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	/** test call setup for inbound automaton 3pc where the first leg is already connected
	 *
	 *    |---INVITE (SDP)--->|                   |
    *    |<---OK (hold SDP)--|                   |
    *    |-------ACK-------->|                   |
    *    ============= join call legs ============
    *    |<-ReINVITE (blank)-|                   |
    *    |-----OK (offer)--->|                   |
    *    |                   |---INVITE (offer)->|
    *    |                   |<----OK (answer)---|
    *    |<---ACK (answer)---|---------ACK------>|
    */
	@Test
	public void testAutomataInboundCallFirstLegConnected() throws Exception {
		// setup
		String outboundDialogId = outboundCallLegBean.createCallLeg(new URI("sip:test"), getInboundPhoneSipUri());
		inboundCallSetupUsingCallBean.outboundDialogId = outboundDialogId;
		makeCallLegAutomaton(outboundDialogId);

		getOutboundCall().listenForReinvite();
		setOutboundCallTargetUsername(INBOUND_CALL_SETUP_USING_HOLD);

		// initiate & connect inbound
		SessionDescriptionHelper.setMediaDescription(getOutboundPhoneSdp(), getOutboundPhoneMediaDescription());
		assertTrue(getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), getRemoteSipAddress(), getRemoteSipProxy(), getOutboundPhoneSdp().toString(), "application", "sdp", null, null));

		assertWeGetOKWithHoldSdp();
		getOutboundCall().sendInviteOkAck();

		// act
		callBean.joinCallLegs(inboundCallSetupUsingHold.inboundDialogId, outboundDialogId);

		// assert
		waitForEmptyReinviteRespondOk(SipUnitPhone.Outbound);
		waitForCallAssertMediaDescriptionSendTryingRingingOk(SipUnitPhone.Inbound, getOutboundPhoneMediaDescription());
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Outbound, getInboundPhoneMediaDescription());
		assertTrue(getInboundCall().waitForAck(5000));

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.Outbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	/** test inbound call setup for automaton 3pc where the second leg is already connected
	 *
	 *    |                   |---INVITE (blank)->|
	 *    |                   |<----OK (offer)----|
	 *    |                   |-----ACK (hold)--->|
	 *    ============= join call legs ============
	 *    |--INVITE (offer)---|                   |
	 *    |                   |-ReINVITE (offer)->|
	 *    |                   |<----OK (answer)---|
     *    |<----OK (answer)---|                   |
     *    |----ACK (answer)-->|-------ACK-------->|
	 */
	@Test
	public void testAutomataInboundCallSecondLegConnected() throws Exception {
		// setup
		String outboundDialogId = outboundCallLegBean.createCallLeg(new URI("sip:test"), getInboundPhoneSipUri());
		inboundCallSetupUsingCallBean.outboundDialogId = outboundDialogId;
		makeCallLegAutomaton(outboundDialogId);

		getOutboundCall().listenForReinvite();
		setOutboundCallTargetUsername(INBOUND_CALL_SETUP_USING_CALL_BEAN);

		outboundCallLegBean.connectCallLeg(outboundDialogId, AutoTerminateAction.False, null, null, true);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);

		// act
		SessionDescriptionHelper.setMediaDescription(getOutboundPhoneSdp(), getOutboundPhoneMediaDescription());
		assertTrue(getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), getRemoteSipAddress(), getRemoteSipProxy(), getOutboundPhoneSdp().toString(), "application", "sdp", null, null));

		// assert
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Inbound, getOutboundPhoneMediaDescription());
		assertTrue("No inbound ok", getOutboundCall().waitForAnswer(5000));
		assertEquals(Response.OK, getOutboundCall().getLastReceivedResponse().getStatusCode());
		assertMediaDescriptionInSessionDescription(getInboundPhoneMediaDescription(), new String(getOutboundCall().getLastReceivedResponse().getRawContent()));
		getOutboundCall().sendInviteOkAck();

		assertTrue(getInboundCall().waitForAck(5000));

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.Outbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	/** test call setup for inbound automaton 3pc where the first leg is already connecting
	 *
	 *    |--INVITE (offer)-->|                   |
	 *    |<----OK (answer)---|                   |
	 *    ============= join call legs ============
     *    |--------ACK------->|                   |
     *    |<-ReINVITE (blank)-|                   |
     *    |----OK (offer)---->|                   |
     *    |                   |---INVITE (offer)->|
     *    |                   |<----OK (answer)---|
     *    |<---ACK (answer)---|--------ACK------->|
	 */
	@Test
	public void testAutomataInboundCallInboundLegConnecting() throws Exception {
		// setup
		String outboundDialogId = outboundCallLegBean.createCallLeg(new URI("sip:test"), getInboundPhoneSipUri());
		inboundCallSetupUsingCallBean.outboundDialogId = outboundDialogId;
		makeCallLegAutomaton(outboundDialogId);

		getOutboundCall().listenForReinvite();
		setOutboundCallTargetUsername(INBOUND_CALL_SETUP_USING_HOLD);

		SessionDescriptionHelper.setMediaDescription(getOutboundPhoneSdp(), getOutboundPhoneMediaDescription());
		assertTrue(getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), getRemoteSipAddress(), getRemoteSipProxy(), getOutboundPhoneSdp().toString(), "application", "sdp", null, null));
		assertWeGetOKWithHoldSdp();

		// act
		callBean.joinCallLegs(inboundCallSetupUsingHold.inboundDialogId, outboundDialogId);
		getOutboundCall().sendInviteOkAck();

		// assert
		waitForEmptyReinviteRespondOk(SipUnitPhone.Outbound);
		waitForCallAssertMediaDescriptionSendTryingRingingOk(SipUnitPhone.Inbound, getOutboundPhoneMediaDescription());
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Outbound, getInboundPhoneMediaDescription());
		assertTrue(getInboundCall().waitForAck(5000));

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.Outbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	/** test call setup for inbound automaton 3pc where the first leg is already connecting with blank invite - we must
	 *  put it on hold so we can use it to initiate negotiation!
	 *
	 *    |--INVITE (blank)-->|                   |
	 *    ============= join call legs ============
	 *    |<----OK (answer)---|                   |
     *    |--------ACK------->|                   |
     *    |<-ReINVITE (blank)-|                   |
     *    |----OK (offer)---->|                   |
     *    |                   |---INVITE (offer)->|
     *    |                   |<----OK (answer)---|
     *    |<---ACK (answer)---|--------ACK------->|
	 */
	@Test
	public void testAutomataInboundCallInboundLegWithBlankInviteConnecting() throws Exception {
		// setup
		String outboundDialogId = outboundCallLegBean.createCallLeg(new URI("sip:test"), getInboundPhoneSipUri());
		inboundCallSetupUsingCallBean.outboundDialogId = outboundDialogId;
		makeCallLegAutomaton(outboundDialogId);

		getOutboundCall().listenForReinvite();
		setOutboundCallTargetUsername(INBOUND_CALL_SETUP_USING_CALL_BEAN);

		assertTrue(getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), getRemoteSipAddress(), getRemoteSipProxy(), null, null, null, null, null));

		// act
		assertWeGetOKWithHoldSdp();
		getOutboundCall().sendInviteOkAck();

		// assert
		waitForEmptyReinviteRespondOk(SipUnitPhone.Outbound);
		waitForCallAssertMediaDescriptionSendTryingRingingOk(SipUnitPhone.Inbound, getOutboundPhoneMediaDescription());
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Outbound, getInboundPhoneMediaDescription());
		assertTrue(getInboundCall().waitForAck(5000));

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.Outbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	/** test call setup for automaton inbound call where the second leg is already connecting
	 *
	 *    |                   |---INVITE (blank)->|
	 *    ============= join call legs ============
	 *    |--INVITE (offer)-->|                   |
	 *    |<-----OK (hold)----|                   |
	 *    |                   |<----OK (offer)----|
	 *    |--------ACK------->|                   |
	 *    |                   |-----ACK (hold)--->|
     *    |<-ReINVITE (blank)-|                   |
	 *    |-----OK (offer)--->|                   |
     *    |                   |-ReINVITE (offer)->|
     *    |                   |<----OK (answer)---|
     *    |<---ACK (answer)---|-------ACK-------->|
	 */
	@Test
	public void testAutomataInboundCallSecondLegConnecting() throws Exception {
		// setup
		String outboundDialogId = outboundCallLegBean.createCallLeg(new URI("sip:test"), getInboundPhoneSipUri());
		inboundCallSetupUsingCallBean.outboundDialogId = outboundDialogId;
		makeCallLegAutomaton(outboundDialogId);

		getOutboundCall().listenForReinvite();
		setOutboundCallTargetUsername(INBOUND_CALL_SETUP_USING_CALL_BEAN);

		outboundCallLegBean.connectCallLeg(outboundDialogId, AutoTerminateAction.False, null, null, true);
		waitForCallSendTryingRinging(getInboundCall());

		// act
		SessionDescriptionHelper.setMediaDescription(getOutboundPhoneSdp(), getOutboundPhoneMediaDescription());
		assertTrue(getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), getRemoteSipAddress(), getRemoteSipProxy(), getOutboundPhoneSdp().toString(), "application", "sdp", null, null));
		assertWeGetOKWithHoldSdp();
		respondWithInitialOk(SipUnitPhone.Inbound);

		// assert
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, getInboundPhoneHoldMediaDescription());
		getOutboundCall().sendInviteOkAck();

		waitForEmptyReinviteRespondOk(SipUnitPhone.Outbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Inbound, getOutboundPhoneMediaDescription());
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Outbound, getInboundPhoneMediaDescription());
		assertTrue(getInboundCall().waitForAck(5000));

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.Outbound);
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	/** test that call leg release upon termination of a non-auto-terminatng call when media negotiation completed and no reinvites in progress
	 *
	 *       1      SSS       2      SSS      3
	 *       |       |        |       |       |
	 *    Connect          Connect          Connect
	 *       |======join======|               |
	 *       |<-init-|        |               |
	 *       |-----offer----->|               |
	 *       |<----answer-----|               |
	 *       |                |======join=====|
	 *       |<-----hold------|      ...      |
	 */
	@Test
	public void testReleasedConnectedCallLegNoReinvitesInProgress() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		String thirdDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getThirdInboundPhoneSipUri());
		makeCallLegAutomaton(secondDialogId);

		callBean.joinCallLegs(firstDialogId, secondDialogId, AutoTerminateAction.False);
		setupAutomataThirdPartyCall();

		// act
		callBean.joinCallLegs(secondDialogId, thirdDialogId);

		// assert
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Inbound, getSecondInboundPhoneHoldMediaDescription());
	}

	/** test call leg release upon termination of a non-auto-terminatng call when releasing leg
	 * has not yet been connected
	 */
	@Test
	public void testReleasedOutboundCallLegWhenNotYetConnected() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		String thirdDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getThirdInboundPhoneSipUri());
		makeCallLegAutomaton(secondDialogId);

		callBean.joinCallLegs(firstDialogId, secondDialogId, AutoTerminateAction.False);


		// act
		callBean.joinCallLegs(secondDialogId, thirdDialogId);

		// assert
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, getInboundPhoneHoldMediaDescription());
		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.Outbound);
	}

	// test call leg release upon termination of a non-autoterminating call when the leg being
	// released is inbound and has not yet had a final response
	@Test
	public void testReleasedInboundCallLegWhenNotYetConnected() throws Exception {
		// setup
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		String thirdDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getThirdInboundPhoneSipUri());

		setOutboundCallTargetUsername(INBOUND_CALL_SETUP_USING_CALL_BEAN);
		assertTrue(getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), getRemoteSipAddress(), getRemoteSipProxy(), createHoldSessionDescription().toString(), "application", "sdp", null, null));
		assertTrue(getInboundCall().waitForIncomingCall(5000));
		callBean.joinCallLegs(inboundCallSetupUsingCallBean.inboundDialogId, secondDialogId, AutoTerminateAction.False);

		// act
		callBean.joinCallLegs(secondDialogId, thirdDialogId);

		// assert
		int lastResponse = Response.TRYING;
		while(lastResponse == Response.TRYING) {
			assertTrue("No response", getOutboundCall().waitOutgoingCallResponse(5000));
			lastResponse = getOutboundCall().getLastReceivedResponse().getStatusCode();
		}

		assertEquals("Expected error response", Response.TEMPORARILY_UNAVAILABLE, getOutboundCall().getLastReceivedResponse().getStatusCode());
	}

	// test call leg release upon termination of a non-auto-terminatng call when releasing leg
	// has started media negotiation
	@Test
	public void testReleasedFirstCallLegInThirdPartyCallMediaNegotiationInProgress() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		String thirdDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getThirdInboundPhoneSipUri());

		callBean.joinCallLegs(firstDialogId, secondDialogId, AutoTerminateAction.False);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		assertTrue(getSecondInboundCall().waitForIncomingCall(5000));

		// act
		callBean.joinCallLegs(secondDialogId, thirdDialogId);

		// assert
		SipTransaction reinviteTransaction = getInboundCall().waitForReinvite(5000);
		assertNotNull("No reinvite", reinviteTransaction);
		SessionDescription sessionDescription = SdpFactory.getInstance().createSessionDescription(new String(reinviteTransaction.getRequest().getRawContent()));
		assertMediaDescriptionInSessionDescription(getInboundPhoneHoldMediaDescription(), sessionDescription);

		assertNoFurtherMessages(SipUnitPhone.Inbound, SipUnitPhone.SecondInbound);
	}

	private void connectThreeOutboundDialogs() {
		firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		thirdDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getThirdInboundPhoneSipUri());

		outboundCallLegBean.connectCallLeg(firstDialogId);
		outboundCallLegBean.connectCallLeg(secondDialogId);
		outboundCallLegBean.connectCallLeg(thirdDialogId);

		// connect all three call legs
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.SecondInbound);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.ThirdInbound);
	}

	// test that we handle scenario where a call leg sends offer just after being released
	@Test
	public void testReleasedCallLegRespondsWithHoldToRefreshForPreviousCall() throws Exception {
		// setup
		connectThreeOutboundDialogs();

		callBean.joinCallLegs(firstDialogId, secondDialogId, AutoTerminateAction.False);
		// first leg in first call gets reinvite
		SipTransaction inboundReinviteTransaction = getInboundCall().waitForReinvite(5000);

		// act
		callBean.joinCallLegs(secondDialogId, thirdDialogId);
		// first leg in SECOND call gets reinvite
		getSecondInboundCall().waitForReinvite(5000);
		// first leg in FIRST call responds to reinivite
		getInboundCall().respondToReinvite(inboundReinviteTransaction, Response.OK, "OK", 0, getInboundPhoneSipAddress(), "display name", getInboundPhoneSdp().toString(), "application", "sdp");

		// assert
		// /first leg in FIRST call should get HOLD ACK
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Inbound, getInboundPhoneHoldMediaDescription());
	}

	// test that we handle scenario where media negotiation for a previous call is in progress, the offer
	// happened before the join, but the answer arrives inside the new call - the original call should be
	// sent a normal answer, followed by a hold reinvite
	@Test
	public void testReleaseCallLegAwaitingMediaAnswerGetsHoldAnswer() throws Exception {
		// setup
		connectThreeOutboundDialogs();

		callBean.joinCallLegs(firstDialogId, secondDialogId, AutoTerminateAction.False);
		// first leg in first call gets reinvite
		SipTransaction inboundReinviteTransaction = getInboundCall().waitForReinvite(5000);

		// first leg in FIRST call responds to reinivite
		getInboundCall().respondToReinvite(inboundReinviteTransaction, Response.OK, "OK", 0, getInboundPhoneSipAddress(), "display name", getInboundPhoneSdp().toString(), "application", "sdp");

		// second leg in FIRST call gets offer and responds
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());

		// act
		callBean.joinCallLegs(secondDialogId, thirdDialogId);

		// assert
		// /first leg in FIRST call should get response ACK...
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());

		// ... followed by HOLD reinvite
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Inbound, getInboundPhoneHoldMediaDescription());
		assertTrue(getInboundCall().waitForAck(5000));
	}

	// test that we handle scenario where we receive a refresh for a previous call
	@Test
	public void testCallLegReceivesRefreshForPreviousCall() throws Exception {
		// setup
		connectThreeOutboundDialogs();

		callBean.joinCallLegs(firstDialogId, secondDialogId, AutoTerminateAction.False);
		// first leg in first call gets reinvite
		SipTransaction inboundReinviteTransaction = getInboundCall().waitForReinvite(5000);

		// act
		callBean.joinCallLegs(firstDialogId, thirdDialogId);
		// first leg in FIRST call responds to reinivite
		getInboundCall().respondToReinvite(inboundReinviteTransaction, Response.OK, "OK", 0, getInboundPhoneSipAddress(), "display name", getInboundPhoneSdp().toString(), "application", "sdp");

		// assert
		// /first leg in FIRST call should get HOLD ACK
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Inbound, getInboundPhoneHoldMediaDescription());
		// now wait for queued reinvite to actually begin negotiation
		waitForEmptyReinviteRespondOk(SipUnitPhone.Inbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.ThirdInbound, getInboundPhoneMediaDescription());
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Inbound, getThirdInboundPhoneMediaDescription());
		// make sure we have a connected event
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	// test that we handle scenario where media negotiation for a previous call is in progress, leg 1 sends offer
	// and is awaiting response from leg 2 - but then gets joined to leg 3
	@Test
	public void testNewCallGetsMediaNegotiationResponseForPreviousCall() throws Exception {
		// setup
		connectThreeOutboundDialogs();

		callBean.joinCallLegs(firstDialogId, secondDialogId, AutoTerminateAction.False);

		// first leg in first call gets reinvite
		SipTransaction inboundReinviteTransaction = getInboundCall().waitForReinvite(5000);

		// first leg in FIRST call responds to reinivite
		getInboundCall().respondToReinvite(inboundReinviteTransaction, Response.OK, "OK", 0, getInboundPhoneSipAddress(), "display name", getInboundPhoneSdp().toString(), "application", "sdp");

		// second leg in FIRST call gets offer and responds
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());

		// act
		callBean.joinCallLegs(firstDialogId, thirdDialogId);

		// assert
		// /first leg in FIRST call should get response in ACK...
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());

		// second leg in FIRST call should get HOLD invite
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.SecondInbound, getInboundPhoneHoldMediaDescription());

		// ... followed immediately by an offer...
		waitForEmptyReinviteRespondOk(SipUnitPhone.Inbound);

	}

	// test correct handling of a join when a reinvite for an existing dialog is already in progress
	@Test
	public void testReinviteInProgressMediaNegotiationPostponed() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		String thirdDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getThirdInboundPhoneSipUri());
		makeCallLegAutomaton(secondDialogId);

		callBean.joinCallLegs(firstDialogId, secondDialogId, AutoTerminateAction.False);
		setupAutomataThirdPartyCall();
		// be nice and wait for cnonect event
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));

		// act
		// initiate reinvite to first leg of FIRST call
		SipTransaction reinviteTransaction = getInboundCall().sendReinvite(getInboundPhoneSipAddress(), getInboundPhoneSipAddress(), getInboundPhoneSdp().toString(), "application", "sdp");
		// ensure the offer is proxied to second leg in FIRST call
		SipTransaction proxiedOfferTransaction = waitForReinviteAssertMediaDescription(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());
		// ... then join first dialog in FIRST call to another dialog to set up THIRD call
		callBean.joinCallLegs(firstDialogId, thirdDialogId);

		// let second dialog in FIRST call send reinvite response, ensure it gets ack, then hold reinvite
		SessionDescriptionHelper.setMediaDescription(getSecondInboundPhoneSdp(), getSecondInboundPhoneMediaDescription());
		getSecondInboundCall().respondToReinvite(proxiedOfferTransaction, Response.OK, "OK", 0, getSecondInboundPhoneSipAddress(), "display name", getSecondInboundPhoneSdp().toString(), "application", "sdp");
		assertTrue(getSecondInboundCall().waitForAck(5000));
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.SecondInbound, getInboundPhoneHoldMediaDescription());

		// assert
		// ensure first dialog in FIRST call gets reinvite answer
		waitForReinviteOKResponseAndAssertMediaDescription(getInboundPhone(), reinviteTransaction, getSecondInboundPhoneMediaDescription());
		// .. followed by whatever needs to happyen to put dialog 3 (second leg in SECOND call on hold...
		waitForCallAssertMediaDescriptionSendTryingRingingOk(SipUnitPhone.ThirdInbound, null);
		waitForAckAssertMediaDescription(SipUnitPhone.ThirdInbound, getThirdInboundPhoneHoldMediaDescription());
		// followed by negotiation for legs 1 and 3
		waitForEmptyReinviteRespondOk(SipUnitPhone.Inbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.ThirdInbound, getInboundPhoneMediaDescription());
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Inbound, getThirdInboundPhoneMediaDescription());

		// and FINALLY a connected event for call 2... phew!
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
	}

	// test that two dialogs being joined together where one of them was previously in a call terminates the remaining dialog but NOT the dialog in the new call
	// 		if the auto-terminate-dialogs flag is set
	@Test
	public void testTwoDialogsJoinedOneAlreadyInACallOriginalDialogTerminatesAutoTerminateDialogs() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		String thirdDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getThirdInboundPhoneSipUri());

		String callId = callBean.joinCallLegs(firstDialogId, secondDialogId, AutoTerminateAction.True);
		setupThirdPartyCall();

		// act
		callBean.joinCallLegs(secondDialogId, thirdDialogId);

		// assert
		// invite
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.ThirdInbound);

		// reinvites
		waitForEmptyReinviteRespondOk(SipUnitPhone.SecondInbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.ThirdInbound, getSecondInboundPhoneMediaDescription());
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.SecondInbound, getThirdInboundPhoneMediaDescription());

		// assert
		// first dialog should receive a disconnect
		assertTrue("Timed out waiting for BYE", getInboundCall().waitForDisconnect(5000));
		assertTrue("Unable to respond to BYE", getInboundCall().respondToDisconnect());
		// second dialog should not
		assertEquals("Second dialog should be connected", DialogState.Confirmed, dialogCollection.get(secondDialogId).getDialogState());
		getSecondInboundCall().listenForDisconnect();
		assertFalse("Second dialog should not have got a BYE", getSecondInboundCall().waitForDisconnect(500));
		assertEquals("First dialog should be terminated", DialogState.Terminated, dialogCollection.get(firstDialogId).getDialogState());
		assertEquals("First dialog termincation cause should be auto-terminated", TerminationCause.AutoTerminated, dialogCollection.get(firstDialogId).getTerminationCause());
		assertEquals("Second call leg caused the termination", CallLegCausingTermination.Second, callCollection.get(callId).getCallLegCausingTermination());
		assertEquals("Call Termination cause should be CallLegDetached", CallTerminationCause.CallLegDetached, callCollection.get(callId).getCallTerminationCause());
	}

	// test that when a call leg is left hanging after other call leg has disconnected, the hanging call leg gets a reinvite with hold sdp
	@Test
	public void testNoMediaForNonAutoTerminatedDialogOnOtherDialogTermination() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		callBean.joinCallLegs(firstDialogId, secondDialogId, AutoTerminateAction.False);
		setupThirdPartyCall();

		// act
		outboundCallLegBean.terminateCallLeg(secondDialogId);
		assertTrue("Timed out waiting for BYE", getSecondInboundCall().waitForDisconnect(5000));
		assertTrue("Unable to respond to BYE", getSecondInboundCall().respondToDisconnect());

		// assert
		assertTrue(getInboundCall().listenForReinvite());
		SipTransaction reinviteTrans = getInboundCall().waitForReinvite(5000);
		assertNotNull("First dialog timed out waiting for reinvite", reinviteTrans);
        assertTrue("Hold sdp not received", new String(getInboundCall().getLastReceivedRequest().getRawContent()).indexOf("IN IP4 0.0.0.0") > -1);

        SessionDescriptionHelper.setMediaDescription(getInboundPhoneSdp(), getInactiveHoldMediaDescription());
		getInboundCall().respondToReinvite(reinviteTrans, Response.OK, "OK", 0, getInboundPhoneSipAddress(), "display name", getInboundPhoneSdp().toString(), "application", "sdp");
		assertTrue(getInboundCall().waitForAck(5000));
		assertEquals("un-expected SDP in ACK", 0, getInboundCall().getLastReceivedRequest().getContentLength());
	}

	// test that when a ReceivedCallLegRefreshEvent is received when the CallLegState is still InProgress, it queues up the event to be handled later
	@Test
	public void testReceivedCallLegRefreshWhileCallLegInProgress() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		String callId = ((CallBeanImpl)callBean).generateCallId();
		CallInfo callInfo = new ThirdPartyCallInfo("callBean", callId, firstDialogId, secondDialogId, AutoTerminateAction.True, 0);
		callInfo.setCallLegConnectionState(firstDialogId, CallLegConnectionState.InProgress);
		callInfo.setCallLegConnectionState(secondDialogId, CallLegConnectionState.Completed);
		callCollection.add(callInfo);
		ReceivedDialogRefreshEvent receivedDialogRefreshEvent = new ReceivedDialogRefreshEvent(firstDialogId, SessionDescriptionHelper.generateHoldMediaDescription(), getSecondInboundPhoneSipAddress(), null, false);
		ReceivedCallLegRefreshEvent receivedCallLegRefreshEvent = new ReceivedCallLegRefreshEvent(receivedDialogRefreshEvent);

		// act
		callBean.onReceivedCallLegRefresh(receivedCallLegRefreshEvent);

		// assert
		callInfo = callCollection.get(callId);
		assertNotNull(callInfo.getPendingCallReinvite());
		assertEquals(receivedCallLegRefreshEvent.getMediaDescription().toString(), callInfo.getPendingCallReinvite().getMediaDescription().toString());
	}

	// test that when an onCallLegConnectedEvent is processed, if there is a pending call reinvite, that invoke processing on it
	@Test
	public void testOnCallLegConnectedEventReleasesPendingCallReinvite() throws Exception {
		// setup
		final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		final String secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
		final String callId = callBean.joinCallLegs(firstDialogId, secondDialogId);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		getInboundCall().listenForReinvite();
		waitForCallSendTryingRingingOk(SipUnitPhone.SecondInbound);
		getSecondInboundCall().listenForAck();
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				CallInfo callInfo = callCollection.get(callId);
				ReceivedDialogRefreshEvent receivedDialogRefreshEvent = new ReceivedDialogRefreshEvent(secondDialogId, getSecondInboundPhoneHoldMediaDescription(), getSecondInboundPhoneSipAddress(), null, false);
				ReceivedCallLegRefreshEvent receivedCallLegRefreshEvent = new ReceivedCallLegRefreshEvent(receivedDialogRefreshEvent);
				callInfo.setPendingCallReinvite(new PendingCallReinvite(receivedCallLegRefreshEvent));
				callCollection.replace(callInfo);
			}

			public String getResourceId() {
				return callId;
			}
		};
		new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Inbound, null);

		// act
		waitForAckAssertMediaDescription(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());

		// assert
		waitForReinviteAssertMediaDescription(SipUnitPhone.Inbound, getSecondInboundPhoneHoldMediaDescription());
		assertNull(callCollection.get(callId).getPendingCallReinvite());
	}
}
