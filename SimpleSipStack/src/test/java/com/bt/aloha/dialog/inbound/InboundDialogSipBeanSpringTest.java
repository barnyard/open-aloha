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
package com.bt.aloha.dialog.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import gov.nist.javax.sip.address.SipUri;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.sip.ClientTransaction;
import javax.sip.InvalidArgumentException;
import javax.sip.TransactionState;
import javax.sip.address.Address;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.RecordRouteHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.cafesip.sipunit.SipRequest;
import org.cafesip.sipunit.SipResponse;
import org.cafesip.sipunit.SipTransaction;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.dialog.DialogSipListener;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.event.DialogConnectedEvent;
import com.bt.aloha.dialog.event.DialogConnectionFailedEvent;
import com.bt.aloha.dialog.event.DialogDisconnectedEvent;
import com.bt.aloha.dialog.event.DialogRefreshCompletedEvent;
import com.bt.aloha.dialog.event.DialogTerminatedEvent;
import com.bt.aloha.dialog.event.DialogTerminationFailedEvent;
import com.bt.aloha.dialog.event.IncomingAction;
import com.bt.aloha.dialog.event.IncomingDialogEvent;
import com.bt.aloha.dialog.event.IncomingResponseCode;
import com.bt.aloha.dialog.event.ReceivedDialogRefreshEvent;
import com.bt.aloha.dialog.inbound.InboundDialogSipBean;
import com.bt.aloha.dialog.inbound.InboundDialogSipBeanImpl;
import com.bt.aloha.dialog.inbound.InboundDialogSipListener;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.TerminationCause;
import com.bt.aloha.eventing.EventFilter;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.testing.DialogListenerStubBase;
import com.bt.aloha.testing.SimpleSipStackPerClassTestCase;


public class InboundDialogSipBeanSpringTest extends SimpleSipStackPerClassTestCase implements InboundDialogSipListener {
	private InboundDialogSipBean inboundDialogSipBean;
	private DialogCollection dialogCollection;
	private boolean connectionFailed = false;
	private Random random = new Random((new Date()).getTime());
	private Semaphore connectionFailedSemaphore;
	private String firstRouteAddress = "sip:first";
	private String secondRouteAddress = "sip:second;name=dick";
	private Address firstRecordRouteAddress;
	private Address secondRecordRouteAddress;
	private ArrayList<Header> additionalHeaders;
	
	@Before
	public void before() throws Exception {
		inboundDialogSipBean = (InboundDialogSipBean)getApplicationContext().getBean("inboundCallLegBean");
		dialogCollection = (DialogCollection)getApplicationContext().getBean("dialogCollection");
		connectionFailedSemaphore = new Semaphore(0);
		
		firstRecordRouteAddress = getSipStack().getAddressFactory().createAddress(firstRouteAddress);
		secondRecordRouteAddress = getSipStack().getAddressFactory().createAddress(secondRouteAddress);
		additionalHeaders = new ArrayList<Header>();
		additionalHeaders.add(getSipStack().getHeaderFactory().createRecordRouteHeader(secondRecordRouteAddress));
		additionalHeaders.add(getSipStack().getHeaderFactory().createRecordRouteHeader(firstRecordRouteAddress));
	}
	
	@Test
	public void testInboundDialogSends404NotFoundWhenNoListenerDefined() throws Exception {
		// setup
		setOutboundCallTargetUsername("lord.lucan.inbound");
		
		// act
		assertTrue(getOutboundCall().initiateOutgoingCall(getRemoteSipAddress(), getRemoteSipProxy()));
		
		// assert
		assertOutboundCallResponses(new int[] {Response.NOT_FOUND});
	}
	
	public class DecliningInboundDialogListener extends DialogListenerStubBase implements EventFilter{
		public void onIncomingDialog(IncomingDialogEvent e) {
			e.setIncomingAction(IncomingAction.Reject);
			e.setResponseCode(IncomingResponseCode.Decline);
		}
		
		public boolean shouldDeliverEvent(Object event) {
			return event instanceof IncomingDialogEvent 
				&& ((IncomingDialogEvent)event).getToUri().contains("inbound.test.decline");
		}
	}
	
	public class HoldSilentInboundDialogListener extends DialogListenerStubBase implements EventFilter{
		public void onIncomingDialog(IncomingDialogEvent e) {
			e.setIncomingAction(IncomingAction.PlaceOnHold);
		}
		
		public boolean shouldDeliverEvent(Object event) {
			return event instanceof IncomingDialogEvent 
				&& ((IncomingDialogEvent)event).getToUri().contains("inbound.test.hold.silent");
		}
	}
	
	// test that an incoming dialog is routed to a bean we define
	@Test
	public void testInboundDialogRoutingToDestination() {
		// setup		
		setOutboundCallTargetUsername("inbound.test.decline");
		
		List<DialogSipListener> dialogListeners = new ArrayList<DialogSipListener>();
		dialogListeners.add(new DecliningInboundDialogListener());
		((InboundDialogSipBeanImpl)inboundDialogSipBean).setDialogSipListeners(dialogListeners);
		
		// act
		assertTrue(getOutboundCall().initiateOutgoingCall(getRemoteSipAddress(), getRemoteSipProxy()));
		
		// assert
		assertOutboundCallResponses(new int[] {Response.DECLINE});
	}
	
	// test that an incoming dialog is put on hold
	@Test
	public void testInboundDialogPlacedOnHoldSilent() throws Exception {
		// setup		
		setOutboundCallTargetUsername("inbound.test.hold.silent");
		HoldSilentInboundDialogListener holdSilentInboundDialogListener = new HoldSilentInboundDialogListener();
		
		List<DialogSipListener> dialogListeners = new ArrayList<DialogSipListener>();
		dialogListeners.add(holdSilentInboundDialogListener);
		((InboundDialogSipBeanImpl)inboundDialogSipBean).setDialogSipListeners(dialogListeners);
		
		// act
		assertTrue(getOutboundCall().initiateOutgoingCall(getRemoteSipAddress(), getRemoteSipProxy()));
		
		// assert
		assertOutboundCallResponses(new int[] {Response.OK});
		
		String content = new String(((SipResponse)getOutboundCall().getLastReceivedResponse()).getRawContent());
		assertTrue("No 0.0.0.0 in OK response", content.contains("0.0.0.0"));
	}
		
	public class CountingHoldingInboundDialogListener extends HoldSilentInboundDialogListener {
		final Vector<String> counter = new Vector<String>();

		public int getNumberOfCalls() {
			return counter.size();
		}
		
		@Override
		public boolean shouldDeliverEvent(Object event) {
			return true;
		}
		
		@Override
		public void onIncomingDialog(IncomingDialogEvent e) {
			counter.add("one");
			super.onIncomingDialog(e);
		}
	};
	
	static public class BackToBackDialogListenerTestBeanMock extends DialogListenerStubBase implements InboundDialogSipListener{
		private String dialogId;
		public void onIncomingDialog(IncomingDialogEvent e) {
			e.setIncomingAction(IncomingAction.None);
			this.dialogId = e.getId();
		}
	};	
	
    // test that agent-originated cancel results in a connection failed event
	@Test
	public void testProcessCancelFiresConnectionFailedEvent() throws Exception {
        // setup
		BackToBackDialogListenerTestBeanMock backDialogListenerTestBeanMock = new BackToBackDialogListenerTestBeanMock();
		List<DialogSipListener> inboundDialogListeners = new ArrayList<DialogSipListener>();
		inboundDialogListeners.add(backDialogListenerTestBeanMock);
		inboundDialogListeners.add(this);
		((InboundDialogSipBeanImpl)inboundDialogSipBean).setDialogSipListeners(inboundDialogListeners);
		
	    setOutboundCallTargetUsername("inboundphone");

		Request request = createInviteRequest();

		ClientTransaction inviteTrans = getSipStack().getSipProvider().getNewClientTransaction(request);
		inviteTrans.sendRequest();
		assertEquals(TransactionState.CALLING, inviteTrans.getState());
		for (int i=0; i<100 && !inviteTrans.getState().equals(TransactionState.PROCEEDING); i++)
			Thread.sleep(10);
		assertEquals(TransactionState.PROCEEDING, inviteTrans.getState());

		// act
		Request cancelRequest = inviteTrans.createCancel();
		ClientTransaction cancelTrans = getSipStack().getSipProvider().getNewClientTransaction(cancelRequest);
		cancelTrans.sendRequest();
		assertEquals(TransactionState.TRYING, cancelTrans.getState());
		for (int i=0; i<100 && !cancelTrans.getState().equals(TransactionState.COMPLETED); i++)
			Thread.sleep(10);
		assertEquals(TransactionState.COMPLETED, cancelTrans.getState());

		// assert
		assertTrue(connectionFailedSemaphore.tryAcquire(500, TimeUnit.MILLISECONDS));
		assertTrue("Connection Failed event was not fired", connectionFailed);
		assertEquals(DialogState.Terminated, dialogCollection.get(backDialogListenerTestBeanMock.dialogId).getDialogState());
		assertEquals(TerminationCause.RemotePartyHungUp, dialogCollection.get(backDialogListenerTestBeanMock.dialogId).getTerminationCause());
	}

	private Request createInviteRequest() throws ParseException, InvalidArgumentException {
		CallIdHeader callIdHeader = getSipStack().getSipProvider().getNewCallId();

		CSeqHeader cseqHeader = getSipStack().getHeaderFactory().createCSeqHeader((long)1, Request.INVITE);

		Address toAddress = getSipStack().getAddressFactory().createAddress(getSipStack().getAddressFactory().createURI(getRemoteSipAddress()));
		ToHeader toHeader = getSipStack().getHeaderFactory().createToHeader(toAddress, null);

		Address fromAddress = getSipStack().getAddressFactory().createAddress(getOutboundPhoneSipAddress());
		FromHeader fromHeader = getSipStack().getHeaderFactory().createFromHeader(fromAddress, Integer.toString(this.random.nextInt(Integer.MAX_VALUE)));

		Address contactAddress = getSipStack().getAddressFactory().createAddress(getOutboundPhoneSipAddress());
		ContactHeader contactHeader = getSipStack().getHeaderFactory().createContactHeader(contactAddress);

		MaxForwardsHeader maxForwardsHeader = getSipStack().getHeaderFactory().createMaxForwardsHeader(5);
		List<?> viaHeaders = getOutboundPhone().getViaHeaders();

		SipUri requestURI = (SipUri)getSipStack().getAddressFactory().createAddress("sip:" + getRemoteUser() + "@" + getRemoteHost()).getURI();
		Request request = getSipStack().getMessageFactory().createRequest(requestURI, Request.INVITE,
                callIdHeader, cseqHeader, fromHeader, toHeader, viaHeaders, maxForwardsHeader);

		Address routeAddress = getSipStack().getAddressFactory().createAddress(getRemoteSipAddress() + "/udp");
		request.addHeader(getSipStack().getHeaderFactory().createRouteHeader(routeAddress));
		request.addHeader(contactHeader);
		return request;
	}
	
	// test only one bean gets event
	@Test
	public void testInboundDialogTwoListenersOnlyOneEvent() {
		// setup
		setOutboundCallTargetUsername("inbound.test.two.listeners");
		
		CountingHoldingInboundDialogListener firstListener = new CountingHoldingInboundDialogListener();
		CountingHoldingInboundDialogListener secondListener = new CountingHoldingInboundDialogListener();
		
		List<DialogSipListener> dialogListeners = new ArrayList<DialogSipListener>();
		dialogListeners.add(firstListener);
		dialogListeners.add(secondListener);
		((InboundDialogSipBeanImpl)inboundDialogSipBean).setDialogSipListeners(dialogListeners);
		
		// act
		assertTrue(getOutboundCall().initiateOutgoingCall(getRemoteSipAddress(), getRemoteSipProxy()));
		
		// assert
		assertOutboundCallResponses(new int[] {Response.OK});
		assertEquals("Expected exactly 1 event for 1st listener", 1, firstListener.getNumberOfCalls());
		assertEquals("Expected exactly 0 events for 2nd listener", 0, secondListener.getNumberOfCalls());
	}
	
	// test than a response to an inbound invite request echoes record route headers
	@Test
	public void testFinalResponsePreservesRecordRouteHeaders() throws Exception {
		// setup
		setOutboundCallTargetUsername("inbound.test.hold.silent");

		List<DialogSipListener> dialogListeners = new ArrayList<DialogSipListener>();
		dialogListeners.add(new HoldSilentInboundDialogListener());
		((InboundDialogSipBeanImpl)inboundDialogSipBean).setDialogSipListeners(dialogListeners);
		
		// act
		assertTrue(getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), getRemoteSipAddress(), getRemoteSipProxy(), additionalHeaders, null, null));
		
		// assert
		assertTrue(getOutboundCall().waitForAnswer(5000));
		ListIterator<?> recordRouteHeaderIterator = getOutboundCall().getLastReceivedResponse().getMessage().getHeaders(RecordRouteHeader.NAME);
		assertEquals(firstRecordRouteAddress, ((RecordRouteHeader)recordRouteHeaderIterator.next()).getAddress());
		assertEquals(secondRecordRouteAddress, ((RecordRouteHeader)recordRouteHeaderIterator.next()).getAddress());
		assertFalse(recordRouteHeaderIterator.hasNext());
	}

	// test than a response to a non-initial request echoes record route headers
	@Test
	public void testReinviteResponsePreservesRecordRouteHeaders() throws Exception {
		// setup
		setOutboundCallTargetUsername("inbound.test.hold.silent");

		List<DialogSipListener> dialogListeners = new ArrayList<DialogSipListener>();
		dialogListeners.add(new HoldSilentInboundDialogListener());
		dialogListeners.add(this);
		((InboundDialogSipBeanImpl)inboundDialogSipBean).setDialogSipListeners(dialogListeners);

		assertTrue(getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), getRemoteSipAddress(), getRemoteSipProxy(), additionalHeaders, null, null));		
		assertTrue(getOutboundCall().waitForAnswer(5000));
		assertTrue(getOutboundCall().sendInviteOkAck());
		ContentTypeHeader contentTypeHeader = getSipStack().getHeaderFactory().createContentTypeHeader("application", "sdp");
		additionalHeaders.add(0, contentTypeHeader);		
		
		// act
		SessionDescriptionHelper.setMediaDescription(getOutboundPhoneSdp(), getOutboundPhoneMediaDescription());
		SipTransaction reinviteTransaction = getOutboundCall().sendReinvite(getOutboundPhoneSipAddress(), getOutboundPhoneSipAddress(), additionalHeaders, null, getOutboundPhoneSdp().toString());
		assertNotNull(reinviteTransaction);
		Response response = waitForNonProvisionalResponse(getOutboundPhone(), reinviteTransaction);
		
		// assert
		ListIterator<?> recordRouteHeaderIterator = response.getHeaders(RecordRouteHeader.NAME);
		assertEquals(firstRecordRouteAddress, ((RecordRouteHeader)recordRouteHeaderIterator.next()).getAddress());
		assertEquals(secondRecordRouteAddress, ((RecordRouteHeader)recordRouteHeaderIterator.next()).getAddress());
		assertFalse(recordRouteHeaderIterator.hasNext());
	}
	
	// test that an intra-dialog request preserves and uses record-route information
	@Test
	public void testByeRequestRespectsRecordRouteInformation() throws Exception {
		// setup
		setOutboundCallTargetUsername("hangup");

		additionalHeaders = new ArrayList<Header>();
		additionalHeaders.add(getSipStack().getHeaderFactory().createRecordRouteHeader(getSipStack().getAddressFactory().createAddress(getOutboundPhoneSipAddress())));
		assertTrue(getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), getRemoteSipAddress(), getRemoteSipProxy(), additionalHeaders, null, null));		
		assertTrue(getOutboundCall().waitForAnswer(5000));
		assertTrue(getOutboundCall().sendInviteOkAck());
		
		// act
		this.getOutboundCall().listenForDisconnect();
		assertTrue(getOutboundCall().waitForDisconnect(8000));
		assertTrue(getOutboundCall().respondToDisconnect());
		
		// assert
		ListIterator<?> routeHeaderIterator = getOutboundCall().getLastReceivedRequest().getMessage().getHeaders(RouteHeader.NAME);
		assertTrue(((SipRequest)getOutboundCall().getLastReceivedRequest()).isBye());
		assertRecordRouteAddress(routeHeaderIterator);
		assertFalse(routeHeaderIterator.hasNext());
	}
	
	private void assertRecordRouteAddress(ListIterator<?> recordRouteHeaderIterator) {
		String address = ((RouteHeader)recordRouteHeaderIterator.next()).getAddress().getURI().toString();
		String[] addressParts = address.split(";");
		assertEquals(getOutboundPhoneSipAddress(), addressParts[0]);
		assertTrue("lr".equals(addressParts[1]) || "lr".equals(addressParts[2]));
		assertTrue("transport=udp".equals(addressParts[1]) || "transport=udp".equals(addressParts[2]));
		assertFalse(addressParts[1].equals(addressParts[2]));
	}
	
	public void onIncomingDialog(IncomingDialogEvent e) {
	}

	public void onDialogConnected(DialogConnectedEvent connectedEvent) {
	}

	public void onDialogConnectionFailed(DialogConnectionFailedEvent connectionFailedEvent) {
		connectionFailed = true;
		connectionFailedSemaphore.release();
	}

	public void onDialogDisconnected(DialogDisconnectedEvent disconnectedEvent) {
	}

	public void onDialogRefreshCompleted(DialogRefreshCompletedEvent callLegConnectedEvent) {
	}

	public void onDialogTerminated(DialogTerminatedEvent terminatedEvent) {
	}

	public void onDialogTerminationFailed(DialogTerminationFailedEvent terminationFailedEvent) {
	}

	public void onReceivedDialogRefresh(ReceivedDialogRefreshEvent receivedCallLegRefreshEvent) {
		((InboundDialogSipBeanImpl)inboundDialogSipBean).sendReinviteOkResponse(receivedCallLegRefreshEvent.getId(), SessionDescriptionHelper.generateHoldMediaDescription(receivedCallLegRefreshEvent.getMediaDescription()));
	}
}
