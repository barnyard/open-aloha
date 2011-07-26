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
package com.bt.aloha.dialog;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.createNiceMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sdp.MediaDescription;
import javax.sdp.SessionDescription;
import javax.sip.ClientTransaction;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipFactory;
import javax.sip.TimeoutEvent;
import javax.sip.address.Address;
import javax.sip.header.CSeqHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentLengthHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.cafesip.sipunit.SipTransaction;
import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.callleg.CallLegHelper;
import com.bt.aloha.callleg.OutboundCallLegBeanImpl;
import com.bt.aloha.collections.memory.InMemoryHousekeepingCollectionImpl;
import com.bt.aloha.dialog.DialogBeanHelper;
import com.bt.aloha.dialog.DialogSipBeanBase;
import com.bt.aloha.dialog.DialogSipListener;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.collections.DialogCollectionImpl;
import com.bt.aloha.dialog.event.AbstractDialogEvent;
import com.bt.aloha.dialog.event.DialogAlertingEvent;
import com.bt.aloha.dialog.event.DialogConnectedEvent;
import com.bt.aloha.dialog.event.DialogConnectionFailedEvent;
import com.bt.aloha.dialog.event.DialogDisconnectedEvent;
import com.bt.aloha.dialog.event.DialogRefreshCompletedEvent;
import com.bt.aloha.dialog.event.DialogTerminatedEvent;
import com.bt.aloha.dialog.event.DialogTerminationFailedEvent;
import com.bt.aloha.dialog.event.ReceivedDialogRefreshEvent;
import com.bt.aloha.dialog.outbound.OutboundDialogSipListener;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.dialog.state.ReinviteInProgress;
import com.bt.aloha.dialog.state.TerminationCause;
import com.bt.aloha.dialog.state.TerminationMethod;
import com.bt.aloha.eventing.EventDispatcher;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.stack.SimpleSipStack;
import com.bt.aloha.testing.SimpleSipStackPerClassTestCase;
import com.bt.aloha.testing.SipUnitPhone;
import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateException;
import com.bt.aloha.util.ConcurrentUpdateManager;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;

public class DialogSipBeanSpringTest extends SimpleSipStackPerClassTestCase implements OutboundDialogSipListener {
	private DialogCollection dialogCollection;
	private SimpleSipStack simpleSipStack;
	private boolean dialogRefreshCompleted;
	private Semaphore dialogRefreshCompletedSemaphore;
	private Semaphore receivedDialogRefreshSemaphore;
	private Semaphore receivedDialogDisconnectedSemaphore;
	private Object dialogRefreshCompletedApplicationData;
	private OutboundCallLegBeanImpl outboundCallLegBean;
	private Vector<ReceivedDialogRefreshEvent> receivedDialogRefreshEvents = new Vector<ReceivedDialogRefreshEvent>();
	private Vector<AbstractDialogEvent> receivedDialogEvents;
	private boolean acceptDialogRefresh = true;

	@Before
    public void before() throws Exception {
		receivedDialogEvents = new Vector<AbstractDialogEvent>();
		simpleSipStack = (SimpleSipStack)getApplicationContext().getBean("simpleSipStack");
	    dialogCollection = (DialogCollection)getApplicationContext().getBean("dialogCollection");
		outboundCallLegBean = (OutboundCallLegBeanImpl)getApplicationContext().getBean("outboundCallLegBean");
		List<DialogSipListener> outboundDialogListeners = new ArrayList<DialogSipListener>();
	    outboundDialogListeners.add(this);
	    outboundCallLegBean.setDialogSipListeners(outboundDialogListeners);
	    outboundCallLegBean.setSimpleSipStack(simpleSipStack);

		dialogRefreshCompleted = false;
		dialogRefreshCompletedSemaphore = new Semaphore(0);
		receivedDialogRefreshSemaphore = new Semaphore(0);
		receivedDialogDisconnectedSemaphore = new Semaphore(0);
		dialogRefreshCompletedApplicationData = null;
	}

	// test that an error response to a reinvite results in dialog termination being initiated
	@Test
	public void testReinviteErrorResponseCallTerminated() throws Exception {
		// setup
        final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);

        waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);

		outboundCallLegBean.reinviteCallLeg(firstDialogId, SessionDescriptionHelper.generateHoldMediaDescription(), AutoTerminateAction.False, null);

		// act
		waitForReinviteRespondError(getInboundCall(), getInboundPhoneSipAddress());

		// assert
		assertTrue("Did not get a BYE", getInboundCall().waitForDisconnect(5000));
		assertEquals(TerminationMethod.Terminate, dialogCollection.get(firstDialogId).getTerminationMethod());
		assertTrue("Failed to respond to BYE", getInboundCall().respondToDisconnect());
		assertEquals(TerminationCause.SipSessionError, dialogCollection.get(firstDialogId).getTerminationCause());
	}


	// test that an ok response to a reinvite with null application data triggers a dialog refresh completed event, with null application data
	@Test
	public void testReinviteOkTriggersDialogRefreshCompletedEvent() throws Exception {
		// setup
		final String dialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
		Response response = SipFactory.getInstance().createMessageFactory().createResponse(createReinviteOkResponseWithOfferString(dialogId));
		ResponseEvent responseEvent = EasyMock.createNiceMock(ResponseEvent.class);
        EasyMock.expect(responseEvent.getResponse()).andStubReturn(response);
		EasyMock.replay(responseEvent);
		simpleSipStack = EasyMock.createNiceMock(SimpleSipStack.class);
		EasyMock.replay(simpleSipStack);
		((OutboundCallLegBeanImpl)outboundCallLegBean).setSimpleSipStack(simpleSipStack);
		
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = dialogCollection.get(dialogId);
				dialogInfo.setDialogState(DialogState.Confirmed);
				dialogInfo.setReinviteInProgess(ReinviteInProgress.SendingReinviteWithSessionDescription);
				dialogCollection.replace(dialogInfo);
			}

			public String getResourceId() {
				return dialogId;
			}
		};
		new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);
		
		// act
		((OutboundCallLegBeanImpl)outboundCallLegBean).processResponse(responseEvent, dialogCollection.get(dialogId));
		
		// assert
		dialogRefreshCompletedSemaphore.tryAcquire(5, TimeUnit.SECONDS);
		assertTrue("DialogRefreshCompleted event was not fired", dialogRefreshCompleted);
		assertNull("DialogRefreshCompletedApplicationData was non-null", dialogRefreshCompletedApplicationData);
	}


	@Test
	public void testInfoOKTriggersNoEvent() throws Exception {
		// setup
		final String dialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
		Response response = SipFactory.getInstance().createMessageFactory().createResponse(createInfoOkResponseString(dialogId));
		ResponseEvent responseEvent = EasyMock.createNiceMock(ResponseEvent.class);
        EasyMock.expect(responseEvent.getResponse()).andStubReturn(response);
		EasyMock.replay(responseEvent);
		// act
		((OutboundCallLegBeanImpl)outboundCallLegBean).processResponse(responseEvent, dialogCollection.get(dialogId));
		
		// assert
		assertEquals(0, receivedDialogEvents.size());
	}

	@Test
	public void testInfo481TriggersDialogDisconnectedEvent() throws Exception {
		// setup
		final String dialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
		Response response = SipFactory.getInstance().createMessageFactory().createResponse(createInfo481ResponseString(dialogId));
		ResponseEvent responseEvent = EasyMock.createNiceMock(ResponseEvent.class);
        EasyMock.expect(responseEvent.getResponse()).andStubReturn(response);
		EasyMock.replay(responseEvent);
		// act
		((OutboundCallLegBeanImpl)outboundCallLegBean).processResponse(responseEvent, dialogCollection.get(dialogId));
		
		// assert
		receivedDialogDisconnectedSemaphore.tryAcquire(5, TimeUnit.SECONDS);
		assertEquals(1, receivedDialogEvents.size());
		assertTrue(receivedDialogEvents.get(0) instanceof DialogDisconnectedEvent);
	}
	
	// test that a reinvite sets the invite in progress flag
	@Test
	public void testReinviteSetsInviteInProgress() throws Exception {
		// setup
        final String dialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = dialogCollection.get(dialogId);
				dialogInfo.setDialogState(DialogState.Confirmed);
				dialogInfo.setRemoteContact(getInboundPhoneSipAddress());
				dialogCollection.replace(dialogInfo);
			}

			public String getResourceId() {
				return dialogId;
			}
		};
		new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

        // act
		outboundCallLegBean.reinviteCallLeg(dialogId, SessionDescriptionHelper.generateHoldMediaDescription(), AutoTerminateAction.False, null);

		// assert
		assertEquals(ReinviteInProgress.SendingReinviteWithSessionDescription, dialogCollection.get(dialogId).getReinviteInProgess());
		assertNull(dialogCollection.get(dialogId).getPendingReinvite());
	}

	// test that a reinvite ok response clears the invite in progress flag
	@Test
	public void testReinviteOkResponseClearsInviteInProgress() throws Exception {
		// setup
        final String dialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
		Response response = SipFactory.getInstance().createMessageFactory().createResponse(createReinviteOkResponseWithOfferString(dialogId));
		ResponseEvent responseEvent = EasyMock.createNiceMock(ResponseEvent.class);
        EasyMock.expect(responseEvent.getResponse()).andStubReturn(response);
		EasyMock.replay(responseEvent);
		simpleSipStack = EasyMock.createNiceMock(SimpleSipStack.class);
		EasyMock.replay(simpleSipStack);
		((OutboundCallLegBeanImpl)outboundCallLegBean).setSimpleSipStack(simpleSipStack);
		
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = dialogCollection.get(dialogId);
				dialogInfo.setDialogState(DialogState.Confirmed);
				dialogInfo.setReinviteInProgess(ReinviteInProgress.SendingReinviteWithSessionDescription);
				dialogCollection.replace(dialogInfo);
			}

			public String getResourceId() {
				return dialogId;
			}
		};
		new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

        // act
		((OutboundCallLegBeanImpl)outboundCallLegBean).processResponse(responseEvent, dialogCollection.get(dialogId));
		
		// assert
		assertTrue(dialogRefreshCompletedSemaphore.tryAcquire(5, TimeUnit.SECONDS));
		assertEquals(ReinviteInProgress.None, dialogCollection.get(dialogId).getReinviteInProgess());
	}

	// test that a reinvite error response clears the invite in progress flag
	@Test
	public void testReinviteErrorResponseClearsInviteInProgress() throws Exception {
		// setup
        final String dialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
		Response response = SipFactory.getInstance().createMessageFactory().createResponse(createReinviteErrorResponseString(dialogId, Response.NOT_ACCEPTABLE_HERE, "Not Acceptable Here"));
		ResponseEvent responseEvent = EasyMock.createNiceMock(ResponseEvent.class);
        EasyMock.expect(responseEvent.getResponse()).andStubReturn(response);
		EasyMock.replay(responseEvent);
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = dialogCollection.get(dialogId);
				dialogInfo.setDialogState(DialogState.Confirmed);
				dialogCollection.replace(dialogInfo);
			}

			public String getResourceId() {
				return dialogId;
			}
		};
		new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

        // act
		((OutboundCallLegBeanImpl)outboundCallLegBean).processResponse(responseEvent, dialogCollection.get(dialogId));

		// assert
		assertEquals(ReinviteInProgress.None, dialogCollection.get(dialogId).getReinviteInProgess());
	}

	// test that a reinvite api call to a dialog with an invite in progress queues up the reinvite
	// without overwriting existing values
	@Test
	public void testReinviteWhenInviteInProgressQueuesRequest() throws Exception {
		// setup
        final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
        	public void execute() {
        		DialogInfo dialogInfo = dialogCollection.get(firstDialogId);
        		dialogInfo.setDialogState(DialogState.Confirmed);
        		dialogInfo.setTerminationMethod(TerminationMethod.None);
        		dialogInfo.setReinviteInProgess(ReinviteInProgress.SendingReinviteWithSessionDescription);
        		dialogInfo.setSessionDescription(createHoldSessionDescription());
        		dialogInfo.setRemoteOfferMediaDescription(getHoldMediaDescription());
        		dialogInfo.setAutoTerminate(false);
        		dialogInfo.setApplicationData("some app data");
        		dialogCollection.replace(dialogInfo);
        	}
			public String getResourceId() {
				return firstDialogId;
			}
        };
        new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

		// act
		outboundCallLegBean.reinviteCallLeg(firstDialogId, getInboundPhoneMediaDescription(), AutoTerminateAction.False, "some more app data");

		// assert
		ReadOnlyDialogInfo dialogInfo = dialogCollection.get(firstDialogId);
		assertNotNull(dialogInfo.getPendingReinvite());
		assertEquals(getInboundPhoneMediaDescription().toString(), dialogInfo.getPendingReinvite().getMediaDescription().toString());
		assertFalse(dialogInfo.getPendingReinvite().getAutoTerminate());
		assertEquals("some more app data", dialogInfo.getPendingReinvite().getApplicationData());
		assertEquals("some app data", dialogInfo.getApplicationData());
	}


	// test that a reinvite api call to a dialog with an invite in progress queues up the reinvite but doesn't change the autoterminate just yet
	@Test
	public void testReinviteWhenInviteInProgressQueuesRequestAndAutoTerminate() throws Exception {
		// setup
        final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
        	public void execute() {
        		DialogInfo dialogInfo = dialogCollection.get(firstDialogId);
        		dialogInfo.setDialogState(DialogState.Confirmed);
        		dialogInfo.setTerminationMethod(TerminationMethod.None);
        		dialogInfo.setReinviteInProgess(ReinviteInProgress.SendingReinviteWithSessionDescription);
        		dialogInfo.setSessionDescription(createHoldSessionDescription());
        		dialogInfo.setRemoteOfferMediaDescription(getHoldMediaDescription());
        		dialogInfo.setAutoTerminate(false);
        		dialogInfo.setApplicationData("some app data");
        		dialogCollection.replace(dialogInfo);
        	}
			public String getResourceId() {
				return firstDialogId;
			}
        };
        new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

		// act
		outboundCallLegBean.reinviteCallLeg(firstDialogId, getInboundPhoneMediaDescription(), AutoTerminateAction.True, "some app data");

		// assert
		ReadOnlyDialogInfo dialogInfo = dialogCollection.get(firstDialogId);
		assertFalse(dialogInfo.isAutoTerminate());
	}

	// test that a reinvite api call to a dialog with an invite in progress and a queued re-invite discards the old one and sets the new one
	@Test
	public void testReinviteWhenInviteInProgressAndQueuedInviteQueuesNewRequestAndDiscardsOldOne() throws Exception {
		// setup
        final String dialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
        	public void execute() {
        		DialogInfo dialogInfo = dialogCollection.get(dialogId);
        		dialogInfo.setDialogState(DialogState.Confirmed);
        		dialogInfo.setTerminationMethod(TerminationMethod.None);
        		dialogInfo.setReinviteInProgess(ReinviteInProgress.SendingReinviteWithSessionDescription);
        		dialogInfo.setSessionDescription(createHoldSessionDescription());
        		dialogInfo.setRemoteOfferMediaDescription(getHoldMediaDescription());        		
        		dialogInfo.setAutoTerminate(false);
        		dialogInfo.setApplicationData("some app data");
        		dialogCollection.replace(dialogInfo);
        	}
			public String getResourceId() {
				return dialogId;
			}
        };
        new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);
        
        outboundCallLegBean.reinviteCallLeg(dialogId, getInboundPhoneMediaDescription(), AutoTerminateAction.True, "discard some app data");

		// act
		outboundCallLegBean.reinviteCallLeg(dialogId, getSecondInboundPhoneMediaDescription(), AutoTerminateAction.False, "some app data");

		// assert
		ReadOnlyDialogInfo dialogInfo = dialogCollection.get(dialogId);
		assertNotNull(dialogInfo.getPendingReinvite());
		assertEquals(getSecondInboundPhoneMediaDescription().toString(), dialogInfo.getPendingReinvite().getMediaDescription().toString());
		assertFalse(dialogInfo.getPendingReinvite().getAutoTerminate());
		assertEquals("some app data", dialogInfo.getPendingReinvite().getApplicationData());
	}

	// test that when we receive an ok response to a reinvite, we send out a queued reinvite request if it exists & set the pended autoterminate
	@Test
	public void testReinviteOkSendsQueuedReinviteAndSetsPendedAutoTerminate() throws Exception {
		// setup
        final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);
        waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		
        outboundCallLegBean.reinviteCallLeg(firstDialogId, getInboundPhoneMediaDescription(), AutoTerminateAction.False, null);

		outboundCallLegBean.reinviteCallLeg(firstDialogId, getSecondInboundPhoneMediaDescription(), AutoTerminateAction.True, null);

		// act
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Inbound, getInboundPhoneMediaDescription());

		// assert
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());				
		assertTrue("Should now be autoterminate to true", dialogCollection.get(firstDialogId).isAutoTerminate());
	}

	// test that when we receive an ok response to a reinvite, we send out a queued reinvite request if it exists & 
	// don't set the autoterminate cause it was unchanged
	@Test
	public void testReinviteOkSendsQueuedReinviteAndNotSetsPendedAutoTerminate() throws Exception {
		// setup
        final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.True);
        waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		
        outboundCallLegBean.reinviteCallLeg(firstDialogId, getInboundPhoneMediaDescription(), AutoTerminateAction.True, null);

		outboundCallLegBean.reinviteCallLeg(firstDialogId, getSecondInboundPhoneMediaDescription(), AutoTerminateAction.Unchanged, null);

		// act
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Inbound, getInboundPhoneMediaDescription());

		// assert
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());				
		assertTrue("Should still be autoterminate to true", dialogCollection.get(firstDialogId).isAutoTerminate());
	}

	// test reinvite with no sdp
	@Test
	public void testReinviteNoSDP() throws Exception {
		// setup
        final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);
        waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		
		// act
        outboundCallLegBean.reinviteCallLeg(firstDialogId, null, AutoTerminateAction.False, null);
		
		// assert        
        assertEquals(ReinviteInProgress.SendingReinviteWithoutSessionDescription, dialogCollection.get(firstDialogId).getReinviteInProgess());
        waitForEmptyReinviteRespondOkAssertAckMediaDescription(SipUnitPhone.Inbound, getInboundPhoneHoldMediaDescription());
        assertEquals("Expected exactly 1 dialog refresh event", 1, receivedDialogRefreshEvents.size());
        assertEquals(ReinviteInProgress.None, dialogCollection.get(firstDialogId).getReinviteInProgess());
	}
	
	// test reinvite with no sdp when OK response delayed - ensure we only process the resends ONCE
	@Test
	public void testResentReinviteOkResponseNoExistingAck() throws Exception {
		// setup
		final String dialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
        	public void execute() {
        		DialogInfo dialogInfo = dialogCollection.get(dialogId);
        		dialogInfo.setDialogState(DialogState.Confirmed);
        		dialogInfo.setTerminationMethod(TerminationMethod.None);
        		dialogInfo.setReinviteInProgess(ReinviteInProgress.SendingReinviteWithoutSessionDescription);
        		dialogInfo.setSessionDescription(createHoldSessionDescription());
        		dialogInfo.setRemoteOfferMediaDescription(getHoldMediaDescription());        		
        		dialogInfo.setAutoTerminate(false);
        		dialogInfo.setApplicationData("some app data");
        		dialogCollection.replace(dialogInfo);
        	}
			public String getResourceId() {
				return dialogId;
			}
        };
        new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);
        
        CSeqHeader cseqHeader = createMock(CSeqHeader.class);
        expect(cseqHeader.getSeqNumber()).andStubReturn(6L);
        replay(cseqHeader);
        
        Response response = createMock(Response.class);
        expect(response.getRawContent()).andReturn(SessionDescriptionHelper.generateHoldMediaDescription().toString().getBytes()).anyTimes();
        expect(response.getHeader(ContactHeader.NAME)).andStubReturn(null);
        expect(response.getHeader(CSeqHeader.NAME)).andStubReturn(cseqHeader);
        replay(response);
        
        ResponseEvent responseEvent = createNiceMock(ResponseEvent.class);
        expect(responseEvent.getResponse()).andStubReturn(response);
        replay(responseEvent);
        
        acceptDialogRefresh = false;
        
		// act
		outboundCallLegBean.processReinviteOkResponse(responseEvent, dialogId);
		outboundCallLegBean.processReinviteOkResponse(responseEvent, dialogId);
		assertTrue(receivedDialogRefreshSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
		assertFalse(receivedDialogRefreshSemaphore.tryAcquire(200, TimeUnit.MILLISECONDS));
		
		// assert
		assertEquals(1, receivedDialogRefreshEvents.size());
		assertEquals(dialogId, receivedDialogRefreshEvents.get(0).getId());
	}

	// test that we resend a previously sent ACK upon receipt of a resent REinvite OK response
	@Test
	public void testResentReinviteOkResponseExistingAckResent() throws Exception {
		CallLegHelper existingCallLegHelper = (CallLegHelper)((DialogSipBeanBase)outboundCallLegBean).getDialogBeanHelper();
		try {
			// setup
			final AtomicInteger numberOfAcksSent = new AtomicInteger();
			final AtomicInteger numberOfAckResends = new AtomicInteger();
			CallLegHelper callLegHelper = new CallLegHelper() {
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
				protected void acceptReceivedMediaOffer(String dialogId, MediaDescription mediaDescription, boolean offerInOkResponse, boolean initialInviteTransactionCompleted) {
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
			};
			callLegHelper.setSimpleSipStack(simpleSipStack);
			outboundCallLegBean.setCallLegHelper(callLegHelper);
	
			final String dialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
			ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
	        	public void execute() {
	        		DialogInfo dialogInfo = dialogCollection.get(dialogId);
	        		dialogInfo.setDialogState(DialogState.Confirmed);
	        		dialogInfo.setTerminationMethod(TerminationMethod.None);
	        		dialogInfo.setReinviteInProgess(ReinviteInProgress.SendingReinviteWithoutSessionDescription);
	        		dialogInfo.setSessionDescription(createHoldSessionDescription());
	        		dialogInfo.setRemoteOfferMediaDescription(getHoldMediaDescription());        		
	        		dialogInfo.setAutoTerminate(false);
	        		dialogInfo.setApplicationData("some app data");
	        		dialogCollection.replace(dialogInfo);
	        	}
				public String getResourceId() {
					return dialogId;
				}
	        };
	        new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);
	        
	        CSeqHeader cseqHeader = createMock(CSeqHeader.class);
	        expect(cseqHeader.getSeqNumber()).andStubReturn(1L);
	        replay(cseqHeader);
	        
	        Address contactAddress = simpleSipStack.getAddressFactory().createAddress(getInboundPhoneSipAddress());
	        ContactHeader contactHeader = createMock(ContactHeader.class);
	        expect(contactHeader.getAddress()).andStubReturn(contactAddress);
	        replay(contactHeader);
	        
	        Response response = createMock(Response.class);
	        expect(response.getRawContent()).andReturn(SessionDescriptionHelper.generateHoldMediaDescription().toString().getBytes()).anyTimes();
	        expect(response.getHeader(ContactHeader.NAME)).andStubReturn(contactHeader);
	        expect(response.getHeader(CSeqHeader.NAME)).andStubReturn(cseqHeader);
	        replay(response);
	        
	        ResponseEvent responseEvent = createNiceMock(ResponseEvent.class);
	        expect(responseEvent.getResponse()).andStubReturn(response);
	        replay(responseEvent);        
	
	        // act
			outboundCallLegBean.processReinviteOkResponse(responseEvent, dialogId);
			assertTrue(receivedDialogRefreshSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
			outboundCallLegBean.processReinviteOkResponse(responseEvent, dialogId);
			assertFalse(receivedDialogRefreshSemaphore.tryAcquire(200, TimeUnit.MILLISECONDS));
			
			// assert
			assertEquals(1, receivedDialogRefreshEvents.size());
			assertEquals(2, numberOfAcksSent.get());
			assertEquals(1, numberOfAckResends.get());
			assertEquals(dialogId, receivedDialogRefreshEvents.get(0).getId());
		} finally {
			outboundCallLegBean.setCallLegHelper(existingCallLegHelper);
		}
	}
	
	// test that when we receive an error response to a reinvite, we don't send out a queued reinvite request if it exists
	@Test
	public void testReinviteErrorClearsReinviteInfoAndDoesNotSendQueuedReinvite() throws Exception {
		// setup
        final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);
		outboundCallLegBean.reinviteCallLeg(firstDialogId, SessionDescriptionHelper.generateHoldMediaDescription(), AutoTerminateAction.False, null);

        // act
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);

		// assert
		assertNull("Should not have sent a reinvite", getInboundCall().waitForReinvite(300));
		assertNull(dialogCollection.get(firstDialogId).getPendingReinvite());
	}

	// test that we get a concurrentupdateexception if the exception occurs beyond 10 tries
	@Test
	public void testProcessTimeoutEventConcurrentUpdateException() throws Exception {
		final Vector<Object> counter = new Vector<Object>();
		// setup
		DialogSipBeanBase mockBean = new DialogSipBeanBase() {
			@Override protected void endNonConfirmedDialog(ReadOnlyDialogInfo dialogInfo, TerminationMethod previousTerminationMethod) {}
		};

		DialogCollection dialogCollection = new DialogCollectionImpl(new InMemoryHousekeepingCollectionImpl<DialogInfo>()) {
			@Override
			public void replace(DialogInfo dialogInfo) {
				counter.add(new Object());
				throw new ConcurrentUpdateException(dialogInfo.getId(), "ups");
			}
		};
		mockBean.setDialogCollection(dialogCollection);

		Request request = EasyMock.createMock(Request.class);
		EasyMock.expect(request.getMethod()).andReturn(Request.BYE);
		EasyMock.replay(request);

		ClientTransaction clientTransaction = EasyMock.createMock(ClientTransaction.class);
		EasyMock.expect(clientTransaction.getRequest()).andReturn(request);
		EasyMock.replay(clientTransaction);

		TimeoutEvent timeoutEvent = EasyMock.createMock(TimeoutEvent.class);
		EasyMock.expect(timeoutEvent.isServerTransaction()).andReturn(false);
		EasyMock.expect(timeoutEvent.getClientTransaction()).andReturn(clientTransaction);
		EasyMock.replay(timeoutEvent);

		DialogInfo dialogInfo = new DialogInfo("id", "abc", "127.0.0.1");
		dialogCollection.add(dialogInfo);

		// act
		try {
			mockBean.processTimeout(timeoutEvent, dialogInfo.getId());
		} catch(ConcurrentUpdateException e) {
			// assert
			assertEquals(11, counter.size());
		}
	}
	
	// test that a reinvite received while a reinvite in progress responds with 491 Request Pending
	@Test
	public void testReinviteWhileAnotherIsInProgressCauses491Response() throws Exception {
		// setup
		final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		outboundCallLegBean.connectCallLeg(firstDialogId);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = dialogCollection.get(firstDialogId);
				dialogInfo.setReinviteInProgess(ReinviteInProgress.SendingReinviteWithSessionDescription);
				dialogCollection.replace(dialogInfo);
			}

			public String getResourceId() {
				return firstDialogId;
			}

		};
		new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

		// act
		SessionDescriptionHelper.setMediaDescription(getInboundPhoneSdp(), getInboundPhoneMediaDescription());
		SipTransaction reinviteTransaction = getInboundCall().sendReinvite(getInboundPhoneSipAddress(), getInboundPhoneSipAddress(), getInboundPhoneSdp().toString(), "application", "sdp");

		// assert
		waitForReinviteErrorResponse(getInboundPhone(), reinviteTransaction, Response.REQUEST_PENDING);
	}

	// test that a reinvite received with no media responds with a 488 Not Acceptable Here
	@Test
	public void testReinviteWithNoMediaCauses488Response() throws Exception {
		// setup
		final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		outboundCallLegBean.connectCallLeg(firstDialogId);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);

		// act
		SipTransaction reinviteTransaction = getInboundCall().sendReinvite(getInboundPhoneSipAddress(), getInboundPhoneSipAddress(), null, null, null, null, null);

		// assert
		waitForReinviteErrorResponse(getInboundPhone(), reinviteTransaction, Response.NOT_ACCEPTABLE_HERE);
	}
	
	@Test
	public void testReinviteAckResponseUpdatesDynamicMediaPayloadMap() throws Exception {
		// setup
		DialogInfo dialogInfo = new DialogInfo("id1234", "me", "127.0.0.1");
		dialogCollection.add(dialogInfo);
		
		MediaDescription mediaDescription = SessionDescriptionHelper.generateHoldMediaDescription();
		mediaDescription.setAttribute("rtpmap", "100 abc");
		SessionDescription sd = SessionDescriptionHelper.createSessionDescription("1.2.3.4", "whatever");
		Vector<MediaDescription> mediaDescriptionsVector = new Vector<MediaDescription>();
		mediaDescriptionsVector.add(mediaDescription);
		sd.setMediaDescriptions(mediaDescriptionsVector);
		
		ContentTypeHeader contentTypeHeader = SipFactory.getInstance().createHeaderFactory().createContentTypeHeader("application", "sdp");
		ContentLengthHeader contentLengthHeader = SipFactory.getInstance().createHeaderFactory().createContentLengthHeader(sd.toString().length());
		
		Request request = EasyMock.createMock(Request.class);
		EasyMock.expect(request.getHeader(ContentTypeHeader.NAME)).andReturn(contentTypeHeader);
		EasyMock.expect(request.getHeader(ContentLengthHeader.NAME)).andReturn(contentLengthHeader);
		EasyMock.expect(request.getRawContent()).andReturn(sd.toString().getBytes());
		EasyMock.replay(request);
		
		// act
		outboundCallLegBean.processReinviteAck(request, null, dialogInfo.getId());
		
		/// assrt
		dialogInfo = dialogCollection.get("id1234");
		assertEquals(1, dialogInfo.getDynamicMediaPayloadTypeMap().size());
		assertEquals("100", dialogInfo.getDynamicMediaPayloadTypeMap().get("abc"));
	}
	
	// test that the final response to reinvite clears reinvite transaction
	@Test
	public void finalReinviteResponseClearsServerTransaction() throws Exception {
		// setup
		DialogSipBeanBase bean = new DialogSipBeanBase() {
			@Override
			protected void endNonConfirmedDialog(ReadOnlyDialogInfo readOnlyDialogInfo, TerminationMethod previousTerminationMethod) {
			}
		};
		
		MessageFactory messageFactory = EasyMock.createNiceMock(MessageFactory.class);
		EasyMock.expect(messageFactory.createResponse(EasyMock.eq(Response.OK), EasyMock.isA(Request.class))).andReturn(null);
		EasyMock.replay(messageFactory);
		
		SimpleSipStack simpleSipStack = EasyMock.createNiceMock(SimpleSipStack.class);
		EasyMock.expect(simpleSipStack.getMessageFactory()).andReturn(messageFactory);
		simpleSipStack.addContactHeader(null, null);
		EasyMock.replay(simpleSipStack);
		bean.setSimpleSipStack(simpleSipStack);
		
		DialogBeanHelper dialogBeanHelper = new DialogBeanHelper() {
			@Override
			public void sendResponse(Response response, ServerTransaction serverTransaction) {
				// do nothing
			}
		};
		bean.setDialogBeanHelper(dialogBeanHelper);
		
		EventDispatcher eventDispatcher = new EventDispatcher();
		eventDispatcher.setTaskExecutor(new SimpleAsyncTaskExecutor());
		bean.setEventDispatcher(eventDispatcher);
		
		DialogInfo dialogInfo = new DialogInfo("a", "b", "c");
		dialogInfo.setLocalParty("sip:me");
		dialogInfo.setReinviteInProgess(ReinviteInProgress.ReceivedReinvite);
		dialogInfo.setInviteServerTransaction(EasyMock.createMock(ServerTransaction.class));
		
		dialogCollection.add(dialogInfo);
		bean.setDialogCollection(dialogCollection);
		
		// act
		bean.sendReinviteOkResponse(dialogInfo.getId(), SessionDescriptionHelper.generateHoldMediaDescription());
		
		// assert
		assertNull(dialogCollection.get(dialogInfo.getId()).getInviteServerTransaction());
	}

	public void onDialogConnected(DialogConnectedEvent connectedEvent) {
		receivedDialogEvents.add(connectedEvent);
	}

	public void onDialogConnectionFailed(DialogConnectionFailedEvent connectionFailedEvent) {
		receivedDialogEvents.add(connectionFailedEvent);
	}

	public void onDialogAlerting(DialogAlertingEvent alertingEvent) {
		receivedDialogEvents.add(alertingEvent);
	}

	public void onDialogDisconnected(DialogDisconnectedEvent disconnectedEvent) {
		receivedDialogEvents.add(disconnectedEvent);
		receivedDialogDisconnectedSemaphore.release();
	}

	public void onDialogTerminated(DialogTerminatedEvent terminateEvent) {
		receivedDialogEvents.add(terminateEvent);
	}

	public void onDialogTerminationFailed(DialogTerminationFailedEvent terminationFailedEvent) {
		receivedDialogEvents.add(terminationFailedEvent);
	}

	public void onDialogRefreshCompleted(DialogRefreshCompletedEvent callLegConnectedEvent) {
		receivedDialogEvents.add(callLegConnectedEvent);
		dialogRefreshCompleted = true;
		dialogRefreshCompletedApplicationData = callLegConnectedEvent.getApplicationData();
		dialogRefreshCompletedSemaphore.release();
	}

	public void onReceivedDialogRefresh(ReceivedDialogRefreshEvent receivedCallLegRefreshEvent) {
		receivedDialogEvents.add(receivedCallLegRefreshEvent);
		receivedDialogRefreshEvents.add(receivedCallLegRefreshEvent);
		if (acceptDialogRefresh)
			outboundCallLegBean.sendReinviteAck(receivedCallLegRefreshEvent.getId(), 
				SessionDescriptionHelper.generateHoldMediaDescription(receivedCallLegRefreshEvent.getMediaDescription()));
		receivedDialogRefreshSemaphore.release();
	}
}
