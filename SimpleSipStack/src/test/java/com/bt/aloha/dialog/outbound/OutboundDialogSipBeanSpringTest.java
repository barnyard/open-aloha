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
package com.bt.aloha.dialog.outbound;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createNiceMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sdp.MediaDescription;
import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sip.ClientTransaction;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipFactory;
import javax.sip.address.Address;
import javax.sip.header.CSeqHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.Header;
import javax.sip.header.RecordRouteHeader;
import javax.sip.header.RouteHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.cafesip.sipunit.SipRequest;
import org.cafesip.sipunit.SipTransaction;
import org.easymock.classextension.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.callleg.CallLegHelper;
import com.bt.aloha.callleg.OutboundCallLegBeanImpl;
import com.bt.aloha.dialog.DialogBeanHelper;
import com.bt.aloha.dialog.DialogSipBeanBase;
import com.bt.aloha.dialog.DialogSipListener;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.event.DialogAlertingEvent;
import com.bt.aloha.dialog.event.DialogConnectedEvent;
import com.bt.aloha.dialog.event.DialogConnectionFailedEvent;
import com.bt.aloha.dialog.event.DialogDisconnectedEvent;
import com.bt.aloha.dialog.event.DialogRefreshCompletedEvent;
import com.bt.aloha.dialog.event.DialogTerminatedEvent;
import com.bt.aloha.dialog.event.DialogTerminationFailedEvent;
import com.bt.aloha.dialog.event.ReceivedDialogRefreshEvent;
import com.bt.aloha.dialog.outbound.OutboundDialogSipBeanImpl;
import com.bt.aloha.dialog.outbound.OutboundDialogSipListener;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.dialog.state.TerminationCause;
import com.bt.aloha.dialog.state.TerminationMethod;
import com.bt.aloha.eventing.EventDispatcher;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.stack.SimpleSipStack;
import com.bt.aloha.testing.SimpleSipStackPerClassTestCase;
import com.bt.aloha.testing.SipUnitPhone;
import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateManager;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;


public class OutboundDialogSipBeanSpringTest extends SimpleSipStackPerClassTestCase implements OutboundDialogSipListener {
    private DialogCollection dialogCollection;
    private OutboundCallLegBeanImpl outboundCallLegBean;
    private SimpleSipStack simpleSipStack;
    private EventDispatcher eventDispatcher;
    private String secondInboundPhoneSipAddress;
    private boolean connected;
    private boolean connectionFailed;
    private boolean terminated;
    private boolean alerting;
    private Semaphore connectedSemaphore;
    private Semaphore connectionFailedSemaphore;
    private Semaphore terminatedSemaphore;
    private Semaphore alertingSemaphore;
    private Semaphore receivedRefreshSemaphore;
    private String firstRouteAddress = "sip:first";
	private String secondRouteAddress = "sip:second;name=dick";
	private Address firstRecordRouteAddress;
	private Address secondRecordRouteAddress;
	private ArrayList<Header> additionalHeaders;
	private List<DialogConnectedEvent> connectedEventList;
	private List<ReceivedDialogRefreshEvent> receivedRefreshEventList;
	final AtomicInteger numberOfAcksSent = new AtomicInteger();
	final AtomicInteger numberOfAckResends = new AtomicInteger();
	private CallLegHelper existingCallLegHelper;
	boolean sendReinviteOkResponseOnReceivedRefresh = true;

    @Before
    public void before() throws Exception {
        secondInboundPhoneSipAddress = "sip:secondinboundphone@" + getHost() + ":" + getPort();
        outboundCallLegBean = (OutboundCallLegBeanImpl)getApplicationContext().getBean("outboundCallLegBean");
        dialogCollection = (DialogCollection)getApplicationContext().getBean("dialogCollection");
        simpleSipStack = (SimpleSipStack)getApplicationContext().getBean("simpleSipStack");
        eventDispatcher = (EventDispatcher)getApplicationContext().getBean("eventDispatcher");
        List<DialogSipListener> dialogListeners = new ArrayList<DialogSipListener>();
        dialogListeners.add(this);
		((OutboundDialogSipBeanImpl)outboundCallLegBean).setDialogSipListeners(dialogListeners);
		connected = false;
		connectionFailed = false;
		terminated = false;
        alerting = false;
        connectedSemaphore = new Semaphore(0);
        connectionFailedSemaphore = new Semaphore(0);
        alertingSemaphore = new Semaphore(0);
        terminatedSemaphore = new Semaphore(0);
        receivedRefreshSemaphore = new Semaphore(0);
		connectedEventList = Collections.synchronizedList(new ArrayList<DialogConnectedEvent>());
		receivedRefreshEventList = Collections.synchronizedList(new ArrayList<ReceivedDialogRefreshEvent>());

		existingCallLegHelper = (CallLegHelper)((DialogSipBeanBase)outboundCallLegBean).getDialogBeanHelper();

		firstRecordRouteAddress = getSipStack().getAddressFactory().createAddress(firstRouteAddress);
		secondRecordRouteAddress = getSipStack().getAddressFactory().createAddress(secondRouteAddress);
		additionalHeaders = new ArrayList<Header>();
		additionalHeaders.add(getSipStack().getHeaderFactory().createRecordRouteHeader(firstRecordRouteAddress));
		additionalHeaders.add(getSipStack().getHeaderFactory().createRecordRouteHeader(secondRecordRouteAddress));
    }

    @After
    public void after() {
    	outboundCallLegBean.setCallLegHelper(existingCallLegHelper);
    }

    /*
     * Test that the correct event is thrown on a connectionFailed event.
     */
    @Test
    public void testOnConnectionFailed() throws Exception {
        // setup
        String firstDialogId = outboundCallLegBean.createCallLeg(URI.create(secondInboundPhoneSipAddress), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);

        // act
        waitForCallSendTryingBusyHere(getInboundCall());

        // assert
        assertTrue(connectionFailedSemaphore.tryAcquire(1, TimeUnit.SECONDS));
        assertTrue("expected connectionFailed event not received", this.connectionFailed);
    }

    /*
     * Test that the correct event is thrown on an alerting event.
     */
    @Test
    public void testOnAlerting() throws Exception {
        // setup
        String firstDialogId = outboundCallLegBean.createCallLeg(URI.create(secondInboundPhoneSipAddress), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);

        // act
        waitForCallSendTryingRinging(getInboundCall());

        // assert
        assertTrue(alertingSemaphore.tryAcquire(1, TimeUnit.SECONDS));
        assertTrue("expected alerting event not received", alerting);
    }

    /*
     * Test that the alerting event is not thrown when we get ringing but dialog is already confirmed
     */
    @Test
    public void testOnAlertingNotDispatchedWhenDialogIsNotEarly() throws Exception {
        // setup
        String firstDialogId = outboundCallLegBean.createCallLeg(URI.create(secondInboundPhoneSipAddress), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);
        waitForCallSendOk(getInboundCall(), getInboundPhoneSdp());
        Thread.sleep(100);

        // act
        getInboundCall().sendIncomingCallResponse(Response.RINGING, "Ringing", 0);

        // assert
        assertFalse("Should not have received the alerting event", alertingSemaphore.tryAcquire(50, TimeUnit.MILLISECONDS));
        assertFalse("Unexpected alerting event received", alerting);
    }

	// test that answer timeout sends cancel and fires connectionFailed event
	@Test
	public void testAnswerTimeoutFiresConnectionFailed() throws Exception {
        // setup
        String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 250);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);
        waitForCallSendTryingRinging(getInboundCall());

        // act
		getInboundPhone().listenRequestMessage();
        waitForCancelRespondOk(getInboundPhone());

		getInboundCall().sendIncomingCallResponse(Response.REQUEST_TERMINATED, "Cancelled", 0);

        // assert
		assertTrue(connectionFailedSemaphore.tryAcquire(500, TimeUnit.MILLISECONDS));
		assertTrue("Connection Failed event was not fired", connectionFailed);
		assertEquals(TerminationCause.CallAnswerTimeout, dialogCollection.get(firstDialogId).getTerminationCause());
	}

	// test that terminate after cancel only sends one cancel
	@Test
	public void testTerminateAfterCancelEarlyState() throws Exception {
        // setup
        String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);
        outboundCallLegBean.cancelCallLeg(firstDialogId);

        waitForCallSendTryingRinging(getInboundCall());
        getInboundPhone().listenRequestMessage();

        RequestEvent reCancel = waitForCancel(getInboundPhone());

        // act
		outboundCallLegBean.terminateCallLeg(firstDialogId);

        // assert
		RequestEvent reSubsequent = getInboundPhone().waitRequest(100);
		assertNull("Didn't expect a request", reSubsequent);

		respondToCancelWithOk(getInboundPhone(), reCancel);
		getInboundCall().sendIncomingCallResponse(Response.REQUEST_TERMINATED, "Cancel received", 0);

		assertTrue(connectionFailedSemaphore.tryAcquire(1, TimeUnit.SECONDS));
		assertTrue("Connection Failed event was not fired", connectionFailed);
	}

	// test that cancel goes out, ok comes back for invite, and dialog stays connected
	@Test
	public void testCancelEarlyStateConnectsAndDoesNotTerminate() throws Exception {
        // setup
        String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);
        waitForCallSendTryingRinging(getInboundCall());
		Thread.sleep(250);

        // act
		outboundCallLegBean.cancelCallLeg(firstDialogId);

		// send OK
		assertTrue(getInboundCall().sendIncomingCallResponse(Response.OK, "OK", 0, createHoldSessionDescription().toString(), "application", "sdp", null, null));

        // assert
		assertTrue(connectedSemaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
		assertTrue("Connected event was not fired", connected);
		assertEquals(TerminationMethod.None, dialogCollection.get(firstDialogId).getTerminationMethod());
		assertEquals(null, dialogCollection.get(firstDialogId).getTerminationCause());
		assertFalse(getInboundCall().waitForDisconnect(100));
	}

	// test that terminate sends out a CANCEL and then a BYE, in a race condition where the OK to the INVITE comes after CANCEL has gone out
	@Test
	public void testTerminateCancelAndByeRaceCondition() throws Exception {
        // setup
		getInboundPhone().listenRequestMessage();

        String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);

        waitForCallSendTryingRinging(getInboundCall());

		// act
		outboundCallLegBean.terminateCallLeg(firstDialogId);
		Thread.sleep(250);

		// send OK
		assertTrue(getInboundCall().sendIncomingCallResponse(Response.OK, "OK", 0, createHoldSessionDescription().toString(), "application", "sdp", null, null));

		waitForCancel(getInboundPhone());
		assertTrue(this.getInboundCall().waitForAck(5000));
		waitForByeAndRespond(getInboundCall());

        // assert
		assertTrue(terminatedSemaphore.tryAcquire(1, TimeUnit.SECONDS));
		assertTrue("Terminated event was not fired", terminated);
		assertEquals(TerminationCause.TerminatedByServer, dialogCollection.get(firstDialogId).getTerminationCause());
	}

	private void assertRecordRouteAddress(ListIterator<?> recordRouteHeaderIterator) {
		String address = ((RouteHeader)recordRouteHeaderIterator.next()).getAddress().getURI().toString();
		String[] addressParts = address.split(";");
		assertEquals(getInboundPhoneSipAddress(), addressParts[0]);
		assertTrue("lr".equals(addressParts[1]) || "lr".equals(addressParts[2]));
		assertTrue("transport=udp".equals(addressParts[1]) || "transport=udp".equals(addressParts[2]));
		assertFalse(addressParts[1].equals(addressParts[2]));
	}

	// test than a route set received in a final resposne is used in a subsequent ack request
	@Test
	public void testRouteSetFromFinalResponseUsedInAck() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
		additionalHeaders = new ArrayList<Header>();
		additionalHeaders.add(getSipStack().getHeaderFactory().createRecordRouteHeader(getSipStack().getAddressFactory().createAddress(getInboundPhoneSipAddress())));

		// act
		outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);

		// assert
		assertTrue(getInboundCall().waitForIncomingCall(5000));
		assertTrue(getInboundCall().sendIncomingCallResponse(200, "OK", 0, additionalHeaders, null, createHoldSessionDescription().toString()));
		assertTrue(getInboundCall().waitForAck(5000));

		ListIterator<?> recordRouteHeaderIterator = ((SipRequest)getInboundCall().getLastReceivedRequest()).getMessage().getHeaders(RouteHeader.NAME);
		assertRecordRouteAddress(recordRouteHeaderIterator);
		assertFalse(recordRouteHeaderIterator.hasNext());
	}

	// test than a route set received in a final resposne is used in a subsequent intra-dialog request
	@Test
	public void testRouteSetFromFinalResponseUsedInBye() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
		additionalHeaders = new ArrayList<Header>();
		additionalHeaders.add(getSipStack().getHeaderFactory().createRecordRouteHeader(getSipStack().getAddressFactory().createAddress(getInboundPhoneSipAddress())));

		outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);

		assertTrue(getInboundCall().waitForIncomingCall(5000));
		assertTrue(getInboundCall().sendIncomingCallResponse(200, "OK", 0, additionalHeaders, null, createHoldSessionDescription().toString()));
		assertTrue(getInboundCall().waitForAck(5000));

		// act
		getInboundCall().listenForDisconnect();
		outboundCallLegBean.terminateCallLeg(firstDialogId);
		assertTrue(getInboundCall().waitForDisconnect(5000));
		getInboundCall().respondToDisconnect();

		// assert
		ListIterator<?> recordRouteHeaderIterator = ((SipRequest)getInboundCall().getLastReceivedRequest()).getMessage().getHeaders(RouteHeader.NAME);
		assertRecordRouteAddress(recordRouteHeaderIterator);
		assertFalse(recordRouteHeaderIterator.hasNext());
	}

	// test than a response to a non-initial request echoes record route headers
	@Test
	public void testReinviteResponsePreservesRecordRouteHeaders() throws Exception {
		// setup
		String firstDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
		outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);

		additionalHeaders = new ArrayList<Header>();
		additionalHeaders.add(getSipStack().getHeaderFactory().createRecordRouteHeader(getSipStack().getAddressFactory().createAddress(getInboundPhoneSipAddress())));

		assertTrue(getInboundCall().waitForIncomingCall(5000));
		assertTrue(getInboundCall().sendIncomingCallResponse(200, "OK", 0, additionalHeaders, null, createHoldSessionDescription().toString()));
		assertTrue(getInboundCall().waitForAck(5000));

		ContentTypeHeader contentTypeHeader = getSipStack().getHeaderFactory().createContentTypeHeader("application", "sdp");
		additionalHeaders.add(0, contentTypeHeader);

		// act
		SessionDescriptionHelper.setMediaDescription(getInboundPhoneSdp(), getInboundPhoneMediaDescription());
		SipTransaction reinviteTransaction = getInboundCall().sendReinvite(getInboundPhoneSipAddress(), getInboundPhoneSipAddress(), additionalHeaders, null, getInboundPhoneSdp().toString());
		assertNotNull(reinviteTransaction);
		Response response = waitForNonProvisionalResponse(getInboundPhone(), reinviteTransaction);

		// assert
		ListIterator<?> recordRouteHeaderIterator = response.getHeaders(RecordRouteHeader.NAME);
		assertEquals(getInboundPhoneSipAddress(), ((RecordRouteHeader)recordRouteHeaderIterator.next()).getAddress().getURI().toString());
		assertFalse(recordRouteHeaderIterator.hasNext());
	}

	// temporary manual test for routing via two proxies (193 / 199) to softphone to prove routing
	@Test
	@Ignore
	public void manualTestForProxyStuff() throws Exception {
		String firstDialogId = outboundCallLegBean.createCallLeg(URI.create("sip:bollox"), URI.create("sip:raghav@radon190.nat.bt.com"), 0);
//		String firstDialogId = outboundCallLegBean.createCallLeg(URI.create("sip:bollox"), URI.create("sip:raghav@132.146.185.190"), 0);
		outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);
		Thread.sleep(10000);
		outboundCallLegBean.terminateCallLeg(firstDialogId);
		Thread.sleep(5000);
	}

	// test that a resent invite ok response is thrown away when no ack has yet been sent
	@Test
	public void testResentInviteOkResponseNoExistingAck() throws Exception {
		// setup
		final String dialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
        	public void execute() {
        		DialogInfo dialogInfo = dialogCollection.get(dialogId);
        		dialogInfo.setDialogState(DialogState.Initiated);
        		dialogInfo.setLastReceivedOkSequenceNumber(1);
        		dialogInfo.setLastAckRequest(null);
        		dialogInfo.setSessionDescription(createHoldSessionDescription());
        		dialogCollection.replace(dialogInfo);
        	}
			public String getResourceId() {
				return dialogId;
			}
        };
        new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

        Response resp = SipFactory.getInstance().createMessageFactory().createResponse(createInitialOkResponseString(dialogId));

        ResponseEvent responseEvent = createNiceMock(ResponseEvent.class);
        expect(responseEvent.getResponse()).andStubReturn(resp);
        replay(responseEvent);

		// act
		outboundCallLegBean.processInitialInviteResponse(responseEvent, dialogId);
		assertFalse(connectedSemaphore.tryAcquire(200, TimeUnit.MILLISECONDS));

		// assert
		assertEquals(0, connectedEventList.size());
	}

	// test that we resend a previously sent ACK upon receipt of a resent invite OK response
	@Test
	public void testResentInviteOkResponseExistingAckResent() throws Exception {
		try {
			// setup
			TestCallLegHelper callLegHelper = new TestCallLegHelper();
			callLegHelper.setSimpleSipStack(simpleSipStack);
			outboundCallLegBean.setCallLegHelper(callLegHelper);

			final String dialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
			ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
	        	public void execute() {
	        		DialogInfo dialogInfo = dialogCollection.get(dialogId);
	        		dialogInfo.setDialogState(DialogState.Initiated);
	        		dialogInfo.setSessionDescription(createHoldSessionDescription());
	        		dialogInfo.setRemoteOfferMediaDescription(getHoldMediaDescription());
	        		dialogInfo.setSdpInInitialInvite(false);
	        		dialogInfo.setAutomaticallyPlaceOnHold(true);
	        		dialogInfo.setAutoTerminate(false);
	        		dialogInfo.setApplicationData("some app data");
	        		dialogCollection.replace(dialogInfo);
	        	}
				public String getResourceId() {
					return dialogId;
				}
	        };
	        new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

	        Response response = SipFactory.getInstance().createMessageFactory().createResponse(createInitialOkResponseString(dialogId));

	        ResponseEvent responseEvent = createNiceMock(ResponseEvent.class);
	        expect(responseEvent.getResponse()).andStubReturn(response);
	        replay(responseEvent);

	        // act
	        outboundCallLegBean.processInitialInviteResponse(responseEvent, dialogId);
	        assertTrue(connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
			outboundCallLegBean.processInitialInviteResponse(responseEvent, dialogId);
			assertFalse(connectedSemaphore.tryAcquire(200, TimeUnit.MILLISECONDS));

			// assert
			assertEquals(1, connectedEventList.size());
			assertEquals(2, numberOfAcksSent.get());
			assertEquals(1, numberOfAckResends.get());
		} finally {
			outboundCallLegBean.setCallLegHelper(existingCallLegHelper);
		}
	}

	// test that when automatically place on hold is true, we place initial invite ok responses w/o SDP in the invite on hold
	@Test
	public void testInviteOkResponseWithoutSDPInInviteAndPlaceOnHoldTrue() throws Exception {
		// setup
		final String dialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
        	public void execute() {
        		DialogInfo dialogInfo = dialogCollection.get(dialogId);
        		dialogInfo.setDialogState(DialogState.Initiated);
        		dialogInfo.setAutomaticallyPlaceOnHold(true);
        		dialogInfo.setLastAckRequest(null);
        		dialogInfo.setSdpInInitialInvite(false);
        		dialogInfo.setInviteClientTransaction(EasyMock.createMock(ClientTransaction.class));
        		dialogCollection.replace(dialogInfo);
        	}
			public String getResourceId() {
				return dialogId;
			}
        };
        new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

        Response resp = SipFactory.getInstance().createMessageFactory().createResponse(createInitialOkResponseWithOfferString(dialogId));
        ResponseEvent responseEvent = createNiceMock(ResponseEvent.class);
        expect(responseEvent.getResponse()).andStubReturn(resp);
        replay(responseEvent);
        TestCallLegHelper helper = new TestCallLegHelper();
        outboundCallLegBean.setCallLegHelper(helper);

		// act
		outboundCallLegBean.processInitialInviteResponse(responseEvent, dialogId);

		// assert
		DialogInfo dialogInfo = dialogCollection.get(dialogId);
		assertNotNull("Should have sent an ack", dialogInfo.getLastAckRequest());
		assertEquals(1, numberOfAcksSent.get());
		assertTrue(connectedSemaphore.tryAcquire(5, TimeUnit.SECONDS));
		assertEquals(1, connectedEventList.size());
		assertNotNull(connectedEventList.get(0).getMediaDescription());
		assertEquals(helper.getActiveMediaDescriptionFromMessageBody(new String(responseEvent.getResponse().getRawContent())).toString(), connectedEventList.get(0).getMediaDescription().toString());
		assertNull(connectedEventList.get(0).getApplicationData());
		assertNull("Initial invite tx should have been cleared", dialogInfo.getInviteClientTransaction());
	}

	@Test
	public void testSendInitialInviteAck() {
		// setup
		final AtomicInteger ackSent = new AtomicInteger();

		DialogInfo dialogInfo = new DialogInfo("abc", "bean", "1.2.3.4");
		dialogInfo.setRemoteContact("sip:localhost");
		dialogInfo.setLocalParty("sip:localhost");
		dialogInfo.setRemoteParty("sip:localhost");
		dialogCollection.add(dialogInfo);

		DialogBeanHelper dialogBeanHelper = new DialogBeanHelper() {
			@Override
			public ClientTransaction sendRequest(Request request) {
				ackSent.incrementAndGet();
				SessionDescription sd;
				try {
					sd = SdpFactory.getInstance().createSessionDescription(new String(request.getRawContent()));
				} catch (SdpParseException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
				assertEquals(getHoldMediaDescription().toString(), SessionDescriptionHelper.getActiveMediaDescription(sd).toString());
				return null;
			}
		};
		OutboundDialogSipBeanImpl outboundDialogSipBeanImpl = new OutboundDialogSipBeanImpl();
		outboundDialogSipBeanImpl.setDialogBeanHelper(dialogBeanHelper);
		outboundDialogSipBeanImpl.setSimpleSipStack(simpleSipStack);
		outboundDialogSipBeanImpl.setDialogCollection(dialogCollection);
		outboundDialogSipBeanImpl.setEventDispatcher(eventDispatcher);

		// act
		outboundDialogSipBeanImpl.sendInitialInviteAck("abc", getHoldMediaDescription());

		// assert
		assertEquals(1, ackSent);
	}

	// test that when automatically place on hold is false, we don't immediately place initial invite ok responses w/o SDP in the invite on hold
	// and we raise received refresh event and not send ack
	@Test
	public void testInviteOkResponseWithoutSDPInInviteAndPlaceOnHoldFalse() throws Exception {
		// setup
		sendReinviteOkResponseOnReceivedRefresh = false;

		final String dialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
		int numberOfMediaDescription  = dialogCollection.get(dialogId).getSessionDescription().getMediaDescriptions(true).size();
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
        	public void execute() {
        		DialogInfo dialogInfo = dialogCollection.get(dialogId);
        		dialogInfo.setDialogState(DialogState.Initiated);
        		dialogInfo.setAutomaticallyPlaceOnHold(false);
        		dialogInfo.setLastAckRequest(null);
        		dialogInfo.setSdpInInitialInvite(false);
        		dialogInfo.setApplicationData("Hi");
        		dialogInfo.setInviteClientTransaction(EasyMock.createMock(ClientTransaction.class));
        		dialogCollection.replace(dialogInfo);
        	}
			public String getResourceId() {
				return dialogId;
			}
        };
        new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

        Response resp = SipFactory.getInstance().createMessageFactory().createResponse(createInitialOkResponseWithOfferString(dialogId));

        ResponseEvent responseEvent = createNiceMock(ResponseEvent.class);
        expect(responseEvent.getResponse()).andStubReturn(resp);
        replay(responseEvent);
        TestCallLegHelper helper = new TestCallLegHelper();
        outboundCallLegBean.setCallLegHelper(helper);

		// act
		outboundCallLegBean.processInitialInviteResponse(responseEvent, dialogId);

		// assert
		DialogInfo dialogInfo = dialogCollection.get(dialogId);
		assertNull("Should not have sent an ack yet", dialogInfo.getLastAckRequest());
		assertEquals(0, numberOfAcksSent.get());
		assertEquals("The media description count should not have changed", numberOfMediaDescription, dialogInfo.getSessionDescription().getMediaDescriptions(true).size());
		assertTrue(receivedRefreshSemaphore.tryAcquire(5, TimeUnit.SECONDS));
		assertEquals(1, receivedRefreshEventList.size());
		assertNotNull(receivedRefreshEventList.get(0).getMediaDescription());
		assertEquals(helper.getActiveMediaDescriptionFromMessageBody(new String(responseEvent.getResponse().getRawContent())).toString(), receivedRefreshEventList.get(0).getMediaDescription().toString());
		assertEquals("Hi", receivedRefreshEventList.get(0).getApplicationData());
		assertNull("Initial invite tx should have been cleared", dialogInfo.getInviteClientTransaction());
	}

	// test that when we send sdp in initial invite that we get those things in the event
	@Test
	public void testInviteOkResponseWithSDPInInviteGetsEventPopulated() throws Exception {
		// setup
		final String dialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        final Response resp = SipFactory.getInstance().createMessageFactory().createResponse(createInitialOkResponseWithOfferString(dialogId));
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
        	public void execute() {
        		DialogInfo dialogInfo = dialogCollection.get(dialogId);
        		dialogInfo.setDialogState(DialogState.Initiated);
        		dialogInfo.setAutomaticallyPlaceOnHold(false);
        		dialogInfo.setLastAckRequest(null);
        		dialogInfo.setSdpInInitialInvite(true);
        		dialogInfo.setInviteClientTransaction(EasyMock.createMock(ClientTransaction.class));
        		SessionDescriptionHelper.setMediaDescription(dialogInfo.getSessionDescription(), existingCallLegHelper.getActiveMediaDescriptionFromMessageBody(new String(resp.getRawContent())));
        		dialogCollection.replace(dialogInfo);
        	}
			public String getResourceId() {
				return dialogId;
			}
        };
        new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

		int numberOfMediaDescription  = dialogCollection.get(dialogId).getSessionDescription().getMediaDescriptions(true).size();
        ResponseEvent responseEvent = createNiceMock(ResponseEvent.class);
        expect(responseEvent.getResponse()).andStubReturn(resp);
        replay(responseEvent);

		// act
		outboundCallLegBean.processInitialInviteResponse(responseEvent, dialogId);

		// assert
		DialogInfo dialogInfo = dialogCollection.get(dialogId);
		assertNotNull("Should have sent an blank ack", dialogInfo.getLastAckRequest());
		assertEquals("The media description count should not have changed", numberOfMediaDescription, dialogInfo.getSessionDescription().getMediaDescriptions(true).size());
		assertTrue(connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
		assertEquals(1, connectedEventList.size());
		assertNotNull(connectedEventList.get(0).getMediaDescription());
		assertEquals(existingCallLegHelper.getActiveMediaDescriptionFromMessageBody(new String(responseEvent.getResponse().getRawContent())).toString(), connectedEventList.get(0).getMediaDescription().toString());
		assertNull(connectedEventList.get(0).getApplicationData());
		assertNull("Initial invite tx should have been cleared", dialogInfo.getInviteClientTransaction());
	}

//	 test that a queued reinvite is released upon completion of ack transaction
    @Test
    public void testQueuedReinviteReleasedWhenACKSent() throws Exception {
          // setup
          String firstDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
          outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);

          waitForCallSendTryingRinging(getInboundCall());

          // act
          assertTrue(alertingSemaphore.tryAcquire(2, TimeUnit.SECONDS));
          outboundCallLegBean.reinviteCallLeg(firstDialogId, getSecondInboundPhoneMediaDescription(), AutoTerminateAction.True, "whatever");
          respondWithInitialOk(SipUnitPhone.Inbound);
          waitForAckAssertMediaDescription(SipUnitPhone.Inbound, getInboundPhoneHoldMediaDescription());;

          // assert
          waitForReinviteAssertMediaDescription(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());
    }

	public void onDialogConnectionFailed(DialogConnectionFailedEvent connectionFailedEvent) {
        connectionFailed = true;
        connectionFailedSemaphore.release();
    }

	public void onDialogAlerting(DialogAlertingEvent alertingEvent) {
        alerting = true;
        alertingSemaphore.release();
	}

	public void onDialogConnected(DialogConnectedEvent connectedEvent) {
		connected = true;
		connectedEventList.add(connectedEvent);
		connectedSemaphore.release();
	}

	public void onDialogDisconnected(DialogDisconnectedEvent disconnectedEvent) {
	}

	public void onDialogTerminated(DialogTerminatedEvent terminateEvent) {
		terminated = true;
		terminatedSemaphore.release();
	}

	public void onDialogTerminationFailed(DialogTerminationFailedEvent terminationFailedEvent) {
	}

	public void onDialogRefreshCompleted(DialogRefreshCompletedEvent callLegConnectedEvent) {
	}

	public void onReceivedDialogRefresh(ReceivedDialogRefreshEvent receivedCallLegRefreshEvent) {
		receivedRefreshEventList.add(receivedCallLegRefreshEvent);
		receivedRefreshSemaphore.release();

		if (sendReinviteOkResponseOnReceivedRefresh)
			((OutboundDialogSipBeanImpl)outboundCallLegBean).sendReinviteOkResponse(receivedCallLegRefreshEvent.getId(), SessionDescriptionHelper.generateHoldMediaDescription(receivedCallLegRefreshEvent.getMediaDescription()));
	}

	private class TestCallLegHelper extends CallLegHelper {
		public TestCallLegHelper() {
			super();
			setSimpleSipStack(simpleSipStack);
		}

		@Override
		protected void acceptReceivedMediaOffer(String dialogId, MediaDescription mediaDescription, boolean offerInOkResponse, boolean initialInviteTransactionCompleted) {
		}

		@Override
		public ClientTransaction sendRequest(Request request, boolean isResend) {
			assertEquals("Unexpected request", "ACK", request.getMethod());
			assertNotNull("Expected SDP in ACK", request.getContent());
			assertEquals("Unexpected seq number", 1, ((CSeqHeader)request.getHeader(CSeqHeader.NAME)).getSeqNumber());
			numberOfAcksSent.incrementAndGet();
			if (isResend)
				numberOfAckResends.incrementAndGet();
			return null;
		}

		@Override
		protected void endNonConfirmedDialog(ReadOnlyDialogInfo dialogInfo, TerminationMethod previousTerminationMethod) {
		}

		@Override
		protected ConcurrentUpdateManager getConcurrentUpdateManager() {
			return new ConcurrentUpdateManagerImpl();
		}

		@Override
		protected DialogCollection getDialogCollection() {
			return dialogCollection;
		}
	}
}
