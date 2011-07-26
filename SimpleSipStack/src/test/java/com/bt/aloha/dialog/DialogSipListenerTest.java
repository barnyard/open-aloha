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

 	

 	
 	
 
package com.bt.aloha.dialog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import gov.nist.javax.sip.header.RouteList;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.sdp.SdpFactory;
import javax.sip.ClientTransaction;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipFactory;
import javax.sip.TimeoutEvent;
import javax.sip.header.HeaderFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.easymock.classextension.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.callleg.OutboundCallLegBean;
import com.bt.aloha.callleg.OutboundCallLegBeanImpl;
import com.bt.aloha.dialog.DialogSipListener;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.event.AbstractDialogEvent;
import com.bt.aloha.dialog.event.DialogConnectedEvent;
import com.bt.aloha.dialog.event.DialogConnectionFailedEvent;
import com.bt.aloha.dialog.event.DialogDisconnectedEvent;
import com.bt.aloha.dialog.event.DialogRefreshCompletedEvent;
import com.bt.aloha.dialog.event.DialogTerminatedEvent;
import com.bt.aloha.dialog.event.DialogTerminationFailedEvent;
import com.bt.aloha.dialog.event.ReceivedDialogRefreshEvent;
import com.bt.aloha.dialog.outbound.OutboundDialogSipBeanImpl;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.TerminationCause;
import com.bt.aloha.dialog.state.TerminationMethod;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.stack.SimpleSipStack;
import com.bt.aloha.testing.SimpleSipStackPerClassTestCase;
import com.bt.aloha.testing.SipUnitPhone;
import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;

public class DialogSipListenerTest extends SimpleSipStackPerClassTestCase implements DialogSipListener  {
	private SimpleSipStack simpleSipStack;
	private OutboundCallLegBean outboundCallLegBean;
	private List<AbstractDialogEvent> eventVector = new Vector<AbstractDialogEvent>();
	private String dialogId;
	private DialogCollection dialogCollection;
	private Semaphore semaphore;
	private boolean acceptReceivedMediaOffer;

	@Before
	public void before() throws Exception {
		simpleSipStack = (SimpleSipStack)getApplicationContext().getBean("simpleSipStack");
		outboundCallLegBean = (OutboundCallLegBean)super.getApplicationContext().getBean("outboundCallLegBean");
		dialogCollection = (DialogCollection)super.getApplicationContext().getBean("dialogCollection");
		List<DialogSipListener> dialogListeners = new ArrayList<DialogSipListener>();
		dialogListeners.add(this);
		((OutboundDialogSipBeanImpl)outboundCallLegBean).setDialogSipListeners(dialogListeners);
	    ((OutboundCallLegBeanImpl)outboundCallLegBean).setSimpleSipStack(simpleSipStack);

		dialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
		this.semaphore = new Semaphore(0);
		acceptReceivedMediaOffer = true;
	}

	@After
	public void after() {
		eventVector.clear();
	}

	// Test that the connected event is raised for a connected dialog where the call leg is
	// to be automatically placed on hold and SDP was in initial invite
	@Test
	public void testOnDialogConnectedSdpInInviteAutoHold() throws Exception {
		// setup
		Response response = SipFactory.getInstance().createMessageFactory().createResponse(createInitialOkResponseWithOfferString(dialogId));
		ResponseEvent responseEvent = EasyMock.createNiceMock(ResponseEvent.class);
        EasyMock.expect(responseEvent.getResponse()).andStubReturn(response);
		EasyMock.replay(responseEvent);
		DialogInfo dialogInfo = dialogCollection.get(dialogId);
		dialogInfo.setDialogState(DialogState.Early);
		dialogInfo.setSdpInInitialInvite(true);
		dialogInfo.setAutomaticallyPlaceOnHold(true);
		dialogInfo.setApplicationData("app data");
		dialogCollection.replace(dialogInfo);
		simpleSipStack = EasyMock.createNiceMock(SimpleSipStack.class);
		EasyMock.replay(simpleSipStack);
		((OutboundCallLegBeanImpl)outboundCallLegBean).setSimpleSipStack(simpleSipStack);
		
		// act
		((OutboundCallLegBeanImpl)outboundCallLegBean).processResponse(responseEvent, dialogInfo);
		
		// assert
		assertTrue(this.semaphore.tryAcquire(5, TimeUnit.SECONDS));
		
		List<AbstractDialogEvent> events = filterDialogEventsForDialogId(eventVector, dialogId);
		assertEquals("No connected event", 1, events.size());
		assertEquals("Got different dialog id", dialogId, ((DialogConnectedEvent)events.get(0)).getId());
		assertEquals("app data", ((DialogConnectedEvent)events.get(0)).getApplicationData());
		
		String mediaDescription = SessionDescriptionHelper.getActiveMediaDescription(SdpFactory.getInstance().createSessionDescription(new String(response.getRawContent()))).toString();
		assertEquals("Unexpected", mediaDescription, ((DialogConnectedEvent)events.get(0)).getMediaDescription().toString());
	}
	
	// Test that the connected event is raised for a connected dialog where the call leg is
	// to be automatically placed on hold and SDP was NOT initial invite
	@Test
	public void testOnDialogConnectedSdpNotInInviteAutoHold() throws Exception {
		// setup
		Response response = SipFactory.getInstance().createMessageFactory().createResponse(createInitialOkResponseWithOfferString(dialogId));
		ResponseEvent responseEvent = EasyMock.createNiceMock(ResponseEvent.class);
        EasyMock.expect(responseEvent.getResponse()).andStubReturn(response);
		EasyMock.replay(responseEvent);
		DialogInfo dialogInfo = dialogCollection.get(dialogId);
		dialogInfo.setDialogState(DialogState.Early);
		dialogInfo.setSdpInInitialInvite(false);
		dialogInfo.setAutomaticallyPlaceOnHold(true);
		dialogInfo.setApplicationData("app data");
		dialogCollection.replace(dialogInfo);
		HeaderFactory headerFactory = EasyMock.createNiceMock(HeaderFactory.class);
		EasyMock.replay(headerFactory);
		Request request = EasyMock.createNiceMock(Request.class);
		EasyMock.replay(request);
		simpleSipStack = EasyMock.createNiceMock(SimpleSipStack.class);
		EasyMock.expect(simpleSipStack.createAckRequest(EasyMock.isA(ResponseEvent.class), EasyMock.isA(String.class), EasyMock.isA(RouteList.class))).andReturn(request);
		EasyMock.expect(simpleSipStack.getHeaderFactory()).andReturn(headerFactory);
		EasyMock.replay(simpleSipStack);
		((OutboundCallLegBeanImpl)outboundCallLegBean).setSimpleSipStack(simpleSipStack);
		
		// act
		((OutboundCallLegBeanImpl)outboundCallLegBean).processResponse(responseEvent, dialogInfo);
		
		// assert
		assertTrue(this.semaphore.tryAcquire(5, TimeUnit.SECONDS));
		List<AbstractDialogEvent> events = filterDialogEventsForDialogId(eventVector, dialogId);
		assertEquals("No connected event", 1, events.size());
		assertEquals("Got different dialog id", dialogId, ((DialogConnectedEvent)events.get(0)).getId());
		assertEquals("app data", ((DialogConnectedEvent)events.get(0)).getApplicationData());
		
		String mediaDescription = SessionDescriptionHelper.getActiveMediaDescription(SdpFactory.getInstance().createSessionDescription(new String(response.getRawContent()))).toString();
		assertEquals("Unexpected", mediaDescription, ((DialogConnectedEvent)events.get(0)).getMediaDescription().toString());
	}

	// Test that the refresh received event is raised for the dialog where the call leg is
	// NOT to be automatically placed on hold and SDP was NOT in initial invite
	@Test
	public void testOnDialogReceivedRefreshSdpNotInInvite() throws Exception {
		// setup
		acceptReceivedMediaOffer = false;
		Response response = SipFactory.getInstance().createMessageFactory().createResponse(createInitialOkResponseWithOfferString(dialogId));
		ResponseEvent responseEvent = EasyMock.createNiceMock(ResponseEvent.class);
        EasyMock.expect(responseEvent.getResponse()).andStubReturn(response);
		EasyMock.replay(responseEvent);
		DialogInfo dialogInfo = dialogCollection.get(dialogId);
		dialogInfo.setDialogState(DialogState.Early);
		dialogInfo.setSdpInInitialInvite(false);
		dialogInfo.setAutomaticallyPlaceOnHold(false);
		dialogInfo.setApplicationData("app data");
		dialogCollection.replace(dialogInfo);
		
		// act
		((OutboundCallLegBeanImpl)outboundCallLegBean).processResponse(responseEvent, dialogInfo);
		
		// assert
		assertTrue(this.semaphore.tryAcquire(5, TimeUnit.SECONDS));
		List<AbstractDialogEvent> events = filterDialogEventsForDialogId(eventVector, dialogId);
		assertEquals("No refresh received event", 1, events.size());
		assertEquals("Got different dialog id", dialogId, ((ReceivedDialogRefreshEvent)events.get(0)).getId());
		assertEquals("app data", ((ReceivedDialogRefreshEvent)events.get(0)).getApplicationData());
		
		String mediaDescription = SessionDescriptionHelper.getActiveMediaDescription(SdpFactory.getInstance().createSessionDescription(new String(response.getRawContent()))).toString();
		assertEquals("Unexpected sdp", mediaDescription, ((ReceivedDialogRefreshEvent)events.get(0)).getMediaDescription().toString());
	}
	
	// Test that sending an ACK for initial invite raises refresh completed event IF the dialog
	// did NOT have SDP in initial invite
	@Test
	public void testAckWithSDPRaisesConnectedEvent() throws Exception {
		// act
		outboundCallLegBean.connectCallLeg(dialogId, AutoTerminateAction.False, "app data", null, false);
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		
		// assert
		this.semaphore.tryAcquire(5, TimeUnit.SECONDS);
		this.semaphore.tryAcquire(5, TimeUnit.SECONDS);
		
		List<AbstractDialogEvent> events = filterDialogEventsForDialogId(eventVector, dialogId);
		assertEquals("Expected 2 events", 2, events.size());
		assertEquals("Got different dialog id", dialogId, ((DialogConnectedEvent)events.get(1)).getId());
		assertEquals("app data", ((DialogConnectedEvent)events.get(1)).getApplicationData());
		
		assertEquals("Unexpected", getInboundPhoneHoldMediaDescription().toString(), ((DialogConnectedEvent)events.get(1)).getMediaDescription().toString());
	}
	
	// Test that the connection failed event is raised for a dialog returning an error response
	@Test
	public void testOnDialogConnectionFailed() throws Exception {
		// setup
		Response response = SipFactory.getInstance().createMessageFactory().createResponse(createInitialErrorResponseString(dialogId, Response.BUSY_HERE, "Busy Here"));
		ResponseEvent responseEvent = EasyMock.createNiceMock(ResponseEvent.class);
        EasyMock.expect(responseEvent.getResponse()).andStubReturn(response);
		EasyMock.replay(responseEvent);
		DialogInfo dialogInfo = dialogCollection.get(dialogId);
		dialogInfo.setDialogState(DialogState.Early);
		dialogCollection.replace(dialogInfo);
		
		// act
		((OutboundCallLegBeanImpl)outboundCallLegBean).processResponse(responseEvent, dialogInfo);

		// assert
		this.semaphore.tryAcquire(5, TimeUnit.SECONDS);
		List<AbstractDialogEvent> events = filterDialogEventsForDialogId(eventVector, dialogId);
		assertEquals("No connection failed event", 1, events.size());
		assertEquals("Got different dialog id", dialogId, ((DialogConnectionFailedEvent)events.get(0)).getId());
	}

	// Test that the termianate event is raised for a dialog
	@Test
	public void testOnDialogTerminatedEvent() throws Exception {
		Response response = SipFactory.getInstance().createMessageFactory().createResponse(createByeOkResponseString(dialogId));
		ResponseEvent responseEvent = EasyMock.createNiceMock(ResponseEvent.class);
        EasyMock.expect(responseEvent.getResponse()).andStubReturn(response);
		EasyMock.replay(responseEvent);
		DialogInfo dialogInfo = dialogCollection.get(dialogId);
		dialogInfo.setDialogState(DialogState.Confirmed);
		dialogCollection.replace(dialogInfo);
		
		// act
		((OutboundCallLegBeanImpl)outboundCallLegBean).processResponse(responseEvent, dialogInfo);

		// assert
		this.semaphore.tryAcquire(5, TimeUnit.SECONDS);
		List<AbstractDialogEvent> events = filterDialogEventsForDialogId(eventVector, dialogId);
		assertEquals("No terminated event", 1, events.size());
		assertEquals("Got different dialog id", dialogId, ((DialogTerminatedEvent)events.get(0)).getId());
	}

	// Test that the disconnect event is raised for a dialog
	@Test
	public void testOnDialogDisconnectedEvent() throws Exception {
		// setup
		Request request = SipFactory.getInstance().createMessageFactory().createRequest(createByeRequestString());
		ServerTransaction serverTransaction = EasyMock.createMock(ServerTransaction.class);
		serverTransaction.sendResponse(EasyMock.isA(Response.class));
		EasyMock.replay(serverTransaction);
		DialogInfo dialogInfo = dialogCollection.get(dialogId);
		dialogInfo.setDialogState(DialogState.Confirmed);
		dialogCollection.replace(dialogInfo);

		((OutboundCallLegBeanImpl)outboundCallLegBean).processRequest(request, serverTransaction, dialogInfo);

		// assert
		this.semaphore.tryAcquire(5, TimeUnit.SECONDS);
		List<AbstractDialogEvent> events = filterDialogEventsForDialogId(eventVector, dialogId);
		assertEquals("No disconnected event", 1, events.size());
		assertEquals("Got different dialog id", dialogId, ((DialogDisconnectedEvent)events.get(0)).getId());
		assertEquals(TerminationCause.RemotePartyHungUp, dialogCollection.get(dialogId).getTerminationCause());
	}

	// Test that the connection failed event is raised once a cancel succeeds
	@Test
	public void testOnDialogConnectionFailedOnCancel() throws Exception {
		// setup
		Response response = SipFactory.getInstance().createMessageFactory().createResponse(createInitialErrorResponseString(dialogId, Response.REQUEST_TERMINATED, "Request Terminated"));
		ResponseEvent responseEvent = EasyMock.createNiceMock(ResponseEvent.class);
        EasyMock.expect(responseEvent.getResponse()).andStubReturn(response);
		EasyMock.replay(responseEvent);
		DialogInfo dialogInfo = dialogCollection.get(dialogId);
		dialogInfo.setDialogState(DialogState.Early);
		dialogCollection.replace(dialogInfo);
		
		// act
		((OutboundCallLegBeanImpl)outboundCallLegBean).processResponse(responseEvent, dialogInfo);

		// assert
		this.semaphore.tryAcquire(5, TimeUnit.SECONDS);
		List<AbstractDialogEvent> events = filterDialogEventsForDialogId(eventVector, dialogId);
		assertEquals("No connection failed event", 1, events.size());
		assertEquals("Got different dialog id", dialogId, ((DialogConnectionFailedEvent)events.get(0)).getId());
	}

	// Test that the termianation failed event is raised for a dialog
	@Test
	public void testOnDialogTerminationFailedEvent() throws Exception {
		Response response = SipFactory.getInstance().createMessageFactory().createResponse(createByeErrorResponseString(dialogId));
		ResponseEvent responseEvent = EasyMock.createNiceMock(ResponseEvent.class);
        EasyMock.expect(responseEvent.getResponse()).andStubReturn(response);
		EasyMock.replay(responseEvent);
		DialogInfo dialogInfo = dialogCollection.get(dialogId);
		dialogInfo.setDialogState(DialogState.Confirmed);
		dialogCollection.replace(dialogInfo);
		
		// act
		((OutboundCallLegBeanImpl)outboundCallLegBean).processResponse(responseEvent, dialogInfo);

		// assert
		this.semaphore.tryAcquire(5, TimeUnit.SECONDS);
		List<AbstractDialogEvent> events = filterDialogEventsForDialogId(eventVector, dialogId);
		assertEquals("No termination failed event", 1, events.size());
		assertEquals("Got different dialog id", dialogId, ((DialogTerminationFailedEvent)events.get(0)).getId());
		assertEquals("Did not clear terminate flag", TerminationMethod.None, dialogCollection.get(dialogId).getTerminationMethod());
		assertEquals("Status should be terminated", DialogState.Terminated, dialogCollection.get(dialogId).getDialogState());
	}

	// test request timeout terminates dialog
	@Test
	public void testDialogTerminatedOnInviteTimeout() throws Exception {
		// setup
		final ClientTransaction clientTransaction = EasyMock.createMock(ClientTransaction.class);
		TimeoutEvent timeoutEvent = new TimeoutEvent(this, clientTransaction, null);
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = dialogCollection.get(dialogId);
				dialogInfo.setInviteClientTransaction(clientTransaction);
				dialogCollection.replace(dialogInfo);
			}

			public String getResourceId() {
				return dialogId;
			}			
		};
		new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

		// act
		((OutboundCallLegBeanImpl)outboundCallLegBean).processTimeout(timeoutEvent, dialogId);
		this.semaphore.tryAcquire(5, TimeUnit.SECONDS);

		// assert
		List<AbstractDialogEvent> events = filterDialogEventsForDialogId(eventVector, dialogId);
		assertEquals("No connection failed event", 1, events.size());
		assertEquals("Got different dialog id", dialogId, ((DialogConnectionFailedEvent)events.get(0)).getId());
		assertEquals("Status should be terminated", DialogState.Terminated, dialogCollection.get(dialogId).getDialogState());		
		assertEquals("Termination cause should be timeout", TerminationCause.SipSessionError, dialogCollection.get(dialogId).getTerminationCause());
		assertEquals("Termination cause should be timeout", TerminationCause.SipSessionError, dialogCollection.get(dialogId).getTerminationCause());
		assertNull("Client transaction should have been nulled out", dialogCollection.get(dialogId).getInviteClientTransaction());
	}

	// test that dialog termination is initiated on reinvite failure
	@Test
	public void testDialogTerminatedOnReinviteTimeout() throws Exception {
		// setup
		outboundCallLegBean.connectCallLeg(dialogId, AutoTerminateAction.False);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		this.semaphore.tryAcquire(5, TimeUnit.SECONDS);

		// act
		final ClientTransaction clientTransaction = EasyMock.createMock(ClientTransaction.class);
		Request request = EasyMock.createMock(Request.class);
		EasyMock.expect(clientTransaction.getBranchId()).andReturn("aBranchId");
		EasyMock.expect(clientTransaction.getRequest()).andReturn(request);
		EasyMock.expect(request.getMethod()).andReturn(Request.INVITE);

		EasyMock.replay(request);
		EasyMock.replay(clientTransaction);
		
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = dialogCollection.get(dialogId);
				dialogInfo.setInviteClientTransaction(clientTransaction);
				dialogCollection.replace(dialogInfo);
			}

			public String getResourceId() {
				return dialogId;
			}			
		};
		new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

		TimeoutEvent timeoutEvent = new TimeoutEvent(this, clientTransaction, null);
		((OutboundCallLegBeanImpl)outboundCallLegBean).processTimeout(timeoutEvent, dialogId);

		// assert
		waitForByeAndRespond(getInboundCall());

		this.semaphore.tryAcquire(5, TimeUnit.SECONDS);
		List<AbstractDialogEvent> events = filterDialogEventsForDialogId(eventVector, dialogId);
		assertEquals("No terminated event", 2, events.size());
		assertEquals("Got different dialog id", dialogId, ((DialogTerminatedEvent)events.get(1)).getId());
		assertEquals("Did not clear terminate flag", TerminationMethod.None, dialogCollection.get(dialogId).getTerminationMethod());
		assertEquals("Status should be terminated", DialogState.Terminated, dialogCollection.get(dialogId).getDialogState());
		assertEquals("Termination cause should be timeout", TerminationCause.SipSessionError, dialogCollection.get(dialogId).getTerminationCause());
		assertNull("Client transaction should have been nulled out", dialogCollection.get(dialogId).getInviteClientTransaction());
	}

	// test that dialog termination is initiated on BYE failure
	@Test
	public void testDialogTerminatedOnByeTimeout() throws Exception {
		// setup
		DialogInfo dialogInfo = dialogCollection.get(dialogId);
		dialogInfo.setDialogState(DialogState.Confirmed);
		dialogCollection.replace(dialogInfo);

		ClientTransaction clientTransaction = EasyMock.createMock(ClientTransaction.class);
		Request request = EasyMock.createMock(Request.class);
		EasyMock.expect(clientTransaction.getRequest()).andReturn(request);
		EasyMock.expect(request.getMethod()).andReturn(Request.BYE);
		
		EasyMock.replay(request);
		EasyMock.replay(clientTransaction);
		
		TimeoutEvent timeoutEvent = new TimeoutEvent(this, clientTransaction, null);

		// act
		((OutboundCallLegBeanImpl)outboundCallLegBean).processTimeout(timeoutEvent, dialogId);

		// assert
		this.semaphore.tryAcquire(5, TimeUnit.SECONDS);
		List<AbstractDialogEvent> events = filterDialogEventsForDialogId(eventVector, dialogId);
		assertEquals("No terminated event", 1, events.size());
		assertEquals("Got different dialog id", dialogId, ((DialogTerminatedEvent)events.get(0)).getId());
		assertEquals("Termination flag shouldn't have been set", TerminationMethod.None, dialogCollection.get(dialogId).getTerminationMethod());
		assertEquals("Status should be terminated", DialogState.Terminated, dialogCollection.get(dialogId).getDialogState());
	}

	// test that when we receive re-invite for a dialog not in a call, we update fields in DialogInfo and fire IncomingDialogRefreshEvent
	@Test
	public void dialogNotInCallReceiveReinviteFiresReceivedDialogRefreshEvent() throws Exception {
		// setup
		Request request = SipFactory.getInstance().createMessageFactory().createRequest(createInviteRequestString());
		ServerTransaction serverTransaction = EasyMock.createMock(ServerTransaction.class);
		serverTransaction.sendResponse(EasyMock.isA(Response.class));
		EasyMock.replay(serverTransaction);
		DialogInfo dialogInfo = dialogCollection.get(dialogId);
		dialogInfo.setDialogState(DialogState.Confirmed);
		dialogCollection.replace(dialogInfo);

		((OutboundCallLegBeanImpl)outboundCallLegBean).processRequest(request, serverTransaction, dialogInfo);

		acceptReceivedMediaOffer = false;
		
		// assert
		this.semaphore.tryAcquire(5, TimeUnit.SECONDS);
		List<AbstractDialogEvent> events = filterDialogEventsForDialogId(eventVector, dialogId);
		assertEquals(1, events.size());
		assertTrue(events.get(0) instanceof ReceivedDialogRefreshEvent);
	}

	public void onDialogConnected(DialogConnectedEvent connectedEvent) {
		this.eventVector.add(connectedEvent);
		this.semaphore.release();
	}

	public void onDialogConnectionFailed(DialogConnectionFailedEvent connectionFailedEvent) {
		this.eventVector.add(connectionFailedEvent);
		this.semaphore.release();
	}

	public void onDialogDisconnected(DialogDisconnectedEvent disconnectedEvent) {
		this.eventVector.add(disconnectedEvent);
		this.semaphore.release();
	}

	public void onDialogTerminated(DialogTerminatedEvent terminatedEvent) {
		this.eventVector.add(terminatedEvent);
		this.semaphore.release();
	}

	public void onDialogTerminationFailed(DialogTerminationFailedEvent terminationFailedEvent) {
		this.eventVector.add(terminationFailedEvent);
		this.semaphore.release();
	}

	public void onDialogRefreshCompleted(DialogRefreshCompletedEvent callLegConnectedEvent) {
		this.eventVector.add(callLegConnectedEvent);
		this.semaphore.release();
	}

	public void onReceivedDialogRefresh(ReceivedDialogRefreshEvent receivedCallLegRefreshEvent) {
		if (acceptReceivedMediaOffer)
			outboundCallLegBean.acceptReceivedMediaOffer(receivedCallLegRefreshEvent.getId(), getInboundPhoneHoldMediaDescription(), true, false);
		this.eventVector.add(receivedCallLegRefreshEvent);
		this.semaphore.release();
	}
}
