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
package com.bt.aloha.callleg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sip.RequestEvent;
import javax.sip.address.Address;
import javax.sip.header.Header;
import javax.sip.message.Response;

import org.cafesip.sipunit.SipTransaction;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.callleg.OutboundCallLegBean;
import com.bt.aloha.callleg.OutboundCallLegBeanImpl;
import com.bt.aloha.callleg.OutboundCallLegListener;
import com.bt.aloha.callleg.event.CallLegAlertingEvent;
import com.bt.aloha.callleg.event.CallLegConnectedEvent;
import com.bt.aloha.callleg.event.CallLegConnectionFailedEvent;
import com.bt.aloha.callleg.event.CallLegDisconnectedEvent;
import com.bt.aloha.callleg.event.CallLegRefreshCompletedEvent;
import com.bt.aloha.callleg.event.CallLegTerminatedEvent;
import com.bt.aloha.callleg.event.CallLegTerminationFailedEvent;
import com.bt.aloha.callleg.event.ReceivedCallLegRefreshEvent;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.dialog.state.TerminationCause;
import com.bt.aloha.dialog.state.TerminationMethod;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.testing.SimpleSipStackPerClassTestCase;
import com.bt.aloha.testing.SipUnitPhone;
import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;


public class OutboundCallLegBeanSpringTest extends SimpleSipStackPerClassTestCase implements OutboundCallLegListener {

    private DialogCollection dialogCollection;
    private OutboundCallLegBean outboundCallLegBean;
    private String secondInboundPhoneSipAddress;
    private boolean connectionFailed;
    private boolean terminated;
    private Semaphore connectionFailedSemaphore;
    private Semaphore terminatedSemaphore;
    private Semaphore alertingSemaphore;
    private String firstRouteAddress = "sip:first";
	private String secondRouteAddress = "sip:second;name=dick";
	private Address firstRecordRouteAddress;
	private Address secondRecordRouteAddress;
	private ArrayList<Header> additionalHeaders;

    @Before
    public void before() throws Exception {
        secondInboundPhoneSipAddress = "sip:secondinboundphone@" + getHost() + ":" + getPort();
        outboundCallLegBean = (OutboundCallLegBean)getApplicationContext().getBean("outboundCallLegBean");
        dialogCollection = (DialogCollection)getApplicationContext().getBean("dialogCollection");
        List<OutboundCallLegListener> callLegListeners = new ArrayList<OutboundCallLegListener>();
        callLegListeners.add(this);
		((OutboundCallLegBeanImpl)outboundCallLegBean).setOutboundCallLegListeners(callLegListeners);
		connectionFailed = false;
		terminated = false;
        connectionFailedSemaphore = new Semaphore(0);
        terminatedSemaphore = new Semaphore(0);
        alertingSemaphore = new Semaphore(0);

		firstRecordRouteAddress = getSipStack().getAddressFactory().createAddress(firstRouteAddress);
		secondRecordRouteAddress = getSipStack().getAddressFactory().createAddress(secondRouteAddress);
		additionalHeaders = new ArrayList<Header>();
		additionalHeaders.add(getSipStack().getHeaderFactory().createRecordRouteHeader(firstRecordRouteAddress));
		additionalHeaders.add(getSipStack().getHeaderFactory().createRecordRouteHeader(secondRecordRouteAddress));
    }
    
    // tests that createCallLeg sets should auto place on hold to false
    @Test
    public void createCallLegSetsAutoPlaceOnHoldToFalse() {
    	// act
    	String firstDialogId = outboundCallLegBean.createCallLeg(URI.create(secondInboundPhoneSipAddress), getInboundPhoneSipUri(), 0);
    	
    	// assert
    	assertFalse("Should auto place on hold should be false", dialogCollection.get(firstDialogId).isAutomaticallyPlaceOnHold());
    }

    // tests that createCallLeg sets the username and password in DialogInfo if passed in on the SIP URI
    @Test
    public void createCallLegSetsUsernameAndPassword() {
    	// act
    	String firstDialogId = outboundCallLegBean.createCallLeg(URI.create(secondInboundPhoneSipAddress), URI.create("sip:fred.flintstone@bedrock.com;username=fred;password=wilma"), 0);
    	
    	// assert
    	assertEquals("fred", dialogCollection.get(firstDialogId).getUsername());
    	assertEquals("wilma", dialogCollection.get(firstDialogId).getPassword());
    	assertEquals("sip:fred.flintstone@bedrock.com",dialogCollection.get(firstDialogId).getRemoteParty().getURI().toString());
    }

    // tests that createCallLeg without seting the username and password results in DialogInfo containing nulls.
    @Test
    public void createCallLegNotSetsUsernameAndPassword() {
    	// act
    	String firstDialogId = outboundCallLegBean.createCallLeg(URI.create(secondInboundPhoneSipAddress), URI.create("sip:fred.flintstone@bedrock.com"), 0);
    	
    	// assert
    	assertNull(dialogCollection.get(firstDialogId).getUsername());
    	assertNull(dialogCollection.get(firstDialogId).getPassword());
    	assertEquals("sip:fred.flintstone@bedrock.com",dialogCollection.get(firstDialogId).getRemoteParty().getURI().toString());
    }
    
    // test that when a null 'from' is passed, an IllegalArgumentException is thrown
    @Test(expected=IllegalArgumentException.class)
    public void createCallLegWithNullFromArgument(){
    	outboundCallLegBean.createCallLeg(null, URI.create("sip:something@10.10.10.10"));
    }

    // test that when a null 'to' is passed, an IllegalArgumentException is thrown
    @Test(expected=IllegalArgumentException.class)
    public void createCallLegWithNullToArgument(){
    	outboundCallLegBean.createCallLeg(URI.create("sip:something@10.10.10.10"), null);
    }
    
	// test that no invite is sent out by the connectdialog method if a cancel has already been requested
    @Test
    public void testNoInviteIfAlreadyCancelling() throws Exception {
    	// setup
        String firstDialogId = outboundCallLegBean.createCallLeg(URI.create(secondInboundPhoneSipAddress), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.cancelCallLeg(firstDialogId);

        // act
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);

        // assert
        assertTrue(connectionFailedSemaphore.tryAcquire(1, TimeUnit.SECONDS));
        assertTrue("expected connectionFailed event not received", this.connectionFailed);
        assertFalse("Invite received when it shoudn't have been sent", getInboundCall().waitForIncomingCall(200));
        assertEquals(DialogState.Terminated, dialogCollection.get(firstDialogId).getDialogState());
        assertEquals(TerminationCause.TerminatedByServer, dialogCollection.get(firstDialogId).getTerminationCause());
	}

    // test that connectDialog sets application object and media description correctly
    @Test
    public void connectCallLegSetsApplicationObjectAndMediaDescription() throws SdpException {
    	// setup
    	String dialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
    	MediaDescription holdMediaDescription = SessionDescriptionHelper.generateHoldMediaDescription();
    	
    	// act
		outboundCallLegBean.connectCallLeg(dialogId, AutoTerminateAction.False, "something", holdMediaDescription, false);
		
		// assert
		ReadOnlyDialogInfo dialogInfo = dialogCollection.get(dialogId);
		assertEquals("something", (String)dialogInfo.getApplicationData());
		assertEquals(1, dialogInfo.getSessionDescription().getMediaDescriptions(false).size());
		assertEquals(holdMediaDescription.toString(), dialogInfo.getSessionDescription().getMediaDescriptions(false).get(0).toString());
		assertTrue(dialogInfo.isSdpInInitialInvite());
    }

    // test that default connectDialog sets application object and media description to null
    @Test
    public void connectCallLegDefaultSetsNullApplicationObjectAndMediaDescription() throws SdpException {
    	// setup
    	String dialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
    	
    	// act
		outboundCallLegBean.connectCallLeg(dialogId, AutoTerminateAction.False);
		
		// assert
		ReadOnlyDialogInfo dialogInfo = dialogCollection.get(dialogId);
		assertNull((String)dialogInfo.getApplicationData());
		assertNull(dialogInfo.getSessionDescription().getMediaDescriptions(false));
		assertFalse(dialogInfo.isSdpInInitialInvite());
    }

    // test that passing null for autoterminate will not reset it
	@Test
	public void connectCallLegUnchangedAutoterminateDoesNotChange() throws Exception {
		// setup
        final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
        	public void execute() {
		        DialogInfo dialogInfo = dialogCollection.get(firstDialogId);
		        dialogInfo.setAutoTerminate(true);
		        dialogCollection.replace(dialogInfo);
        	}
        	public String getResourceId() {
        		return firstDialogId;
        	}
        };
        new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

        // act
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.Unchanged);

		// assert
        assertTrue("Autoterminate should not have been reset.", dialogCollection.get(firstDialogId).isAutoTerminate());
	}


    // test that no invite is sent out by the connectdialog method if termination has already been requested
    @Test(expected=IllegalStateException.class)
    public void testNoInviteIfAlreadyTerminating() throws Exception {
    	// setup
        String firstDialogId = outboundCallLegBean.createCallLeg(URI.create(secondInboundPhoneSipAddress), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.terminateCallLeg(firstDialogId);

        // act
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);
	}

	// test that we get an InvalidStateException while cancelling a dialog that's in CONFIRMED state
	@Test(expected=IllegalStateException.class)
	public void testCancelConfirmedDialog() throws Exception {
		// setup
        final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
        	public void execute() {
		        DialogInfo dialogInfo = dialogCollection.get(firstDialogId);
		        dialogInfo.setDialogState(DialogState.Confirmed);
		        dialogCollection.replace(dialogInfo);
        	}

			public String getResourceId() {
				return firstDialogId;
			}
        };
        new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

        // act
       	outboundCallLegBean.cancelCallLeg(firstDialogId);
	}

	// test that we get an InvalidStateException while cancelling a dialog that's in TERMINATED state
	@Test(expected=IllegalStateException.class)
	public void testCancelTerminatedDialog() throws Exception {
		// setup
        final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);

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
        outboundCallLegBean.cancelCallLeg(firstDialogId);
	}

	// test that cancel on a created state sets TerminationMethod
	@Test
	public void testCancelCreatedStateTerminationMethod() throws Exception {
        // setup
        String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);

        // act
		outboundCallLegBean.cancelCallLeg(firstDialogId);

		// assert
		assertEquals(DialogState.Created, dialogCollection.get(firstDialogId).getDialogState());
		assertEquals(TerminationMethod.Cancel, dialogCollection.get(firstDialogId).getTerminationMethod());
		assertEquals(TerminationCause.TerminatedByServer, dialogCollection.get(firstDialogId).getTerminationCause());
	}

	// test that cancel on an initiated state sets TerminationMethod
	@Test
	public void testCancelInitiatedStateTerminationMethod() throws Exception {
        // setup
        String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);

        // act
		outboundCallLegBean.cancelCallLeg(firstDialogId);

		// assert
		assertEquals(DialogState.Initiated, dialogCollection.get(firstDialogId).getDialogState());
		assertEquals(TerminationMethod.Cancel, dialogCollection.get(firstDialogId).getTerminationMethod());
		assertEquals(TerminationCause.TerminatedByServer, dialogCollection.get(firstDialogId).getTerminationCause());
	}

	// test that cancel works on a dialog in early state
	@Test
	public void testCancelEarlyState() throws Exception {
        // setup
        String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);
        waitForCallSendTryingRinging(getInboundCall());
		getInboundPhone().listenRequestMessage();

        // act
		outboundCallLegBean.cancelCallLeg(firstDialogId);

		waitForCancelRespondOk(getInboundPhone());
		getInboundCall().sendIncomingCallResponse(Response.REQUEST_TERMINATED, "Cancel received", 0);

        // assert
		assertTrue(connectionFailedSemaphore.tryAcquire(500, TimeUnit.MILLISECONDS));
		assertTrue("Connection Failed event was not fired", connectionFailed);
		assertEquals(TerminationCause.TerminatedByServer, dialogCollection.get(firstDialogId).getTerminationCause());
	}

	// test that cancel works on a dialog in initiated state
	@Test
	public void testCancelInitiatedState() throws Exception {
        // setup
        String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);

        // act
		outboundCallLegBean.cancelCallLeg(firstDialogId);

		waitForCallSendTryingRinging(getInboundCall());
		getInboundPhone().listenRequestMessage();
		waitForCancelRespondOk(getInboundPhone());
		getInboundCall().sendIncomingCallResponse(Response.REQUEST_TERMINATED, "Cancel received", 0);

        // assert
		assertTrue(connectionFailedSemaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
		assertTrue("Connection Failed event was not fired", connectionFailed);
		assertEquals(TerminationCause.TerminatedByServer, dialogCollection.get(firstDialogId).getTerminationCause());
	}

	// test that terminate after cancel updates TerminationMethod
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
		assertEquals(TerminationMethod.Terminate, dialogCollection.get(firstDialogId).getTerminationMethod());

		respondToCancelWithOk(getInboundPhone(), reCancel);
		getInboundCall().sendIncomingCallResponse(Response.REQUEST_TERMINATED, "Cancel received", 0);

		assertTrue(connectionFailedSemaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
		assertTrue("Connection Failed event was not fired", connectionFailed);
		assertEquals(TerminationCause.TerminatedByServer, dialogCollection.get(firstDialogId).getTerminationCause());
	}

	// test that terminate of initial dialog results in terminated state right away and fires an onTerminated event
	@Test
	public void testTerminateInitial() throws Exception {
        // setup
        String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);

        // act
		outboundCallLegBean.terminateCallLeg(firstDialogId);

        // assert
		assertEquals("Dialog not set to terminated", DialogState.Terminated, dialogCollection.get(firstDialogId).getDialogState());
	}

	// test that connect of terminated dialog results in an illegal state exception
	@Test(expected=IllegalStateException.class)
	public void testConnectTerminatedDialogThrowsIllegalStateException() throws Exception {
        // setup
        final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
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
       	outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);
	}

//	 test that terminate on an initiated state sets TerminationMethod
	@Test
	public void testTerminateInitiatedStateTerminationMethod() throws Exception {
        // setup
        String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);

        // act
		outboundCallLegBean.terminateCallLeg(firstDialogId);

		// assert
		assertEquals(DialogState.Initiated, dialogCollection.get(firstDialogId).getDialogState());
		assertEquals(TerminationMethod.Terminate, dialogCollection.get(firstDialogId).getTerminationMethod());
		assertEquals(TerminationCause.TerminatedByServer, dialogCollection.get(firstDialogId).getTerminationCause());
	}

	// test that terminate works on a dialog in early state
	@Test
	public void testTerminateEarlyState() throws Exception {
        // setup
        String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);
        waitForCallSendTryingRinging(getInboundCall());
		getInboundPhone().listenRequestMessage();

        // act
		outboundCallLegBean.terminateCallLeg(firstDialogId);

		waitForCancelRespondOk(getInboundPhone());
		getInboundCall().sendIncomingCallResponse(Response.REQUEST_TERMINATED, "Cancel received", 0);

        // assert
		assertTrue(connectionFailedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
		assertTrue("Connection Failed event was not fired", connectionFailed);
		assertEquals(TerminationMethod.None, dialogCollection.get(firstDialogId).getTerminationMethod());
		assertEquals(TerminationCause.TerminatedByServer, dialogCollection.get(firstDialogId).getTerminationCause());
	}

	// test that terminate works on a dialog in initiated state
	@Test
	public void testTerminateInitiatedState() throws Exception {
        // setup
        String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);

        // act
		outboundCallLegBean.terminateCallLeg(firstDialogId);

		waitForCallSendTryingRinging(getInboundCall());
		getInboundPhone().listenRequestMessage();
		waitForCancelRespondOk(getInboundPhone());
		getInboundCall().sendIncomingCallResponse(Response.REQUEST_TERMINATED, "Cancel received", 0);

        // assert
		assertTrue(connectionFailedSemaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
		assertTrue("Connection Failed event was not fired", connectionFailed);
		assertEquals(TerminationCause.TerminatedByServer, dialogCollection.get(firstDialogId).getTerminationCause());
	}

	// test that terminate works on a dialog in initiated state with no provisional response
	@Test
	public void testTerminateInitiatedStateNoProvisionalResponse() throws Exception {
        // setup
        String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);

        // act
		outboundCallLegBean.terminateCallLeg(firstDialogId);

		SessionDescriptionHelper.setMediaDescription(getInboundPhoneSdp(), getInboundPhoneMediaDescription());
		waitForCallSendOk(getInboundCall(), getInboundPhoneSdp());
		waitForByeAndRespond(getInboundCall());

        // assert
		assertTrue(terminatedSemaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
		assertTrue("Terminated event was not fired", terminated);
		assertEquals(TerminationCause.TerminatedByServer, dialogCollection.get(firstDialogId).getTerminationCause());
	}
	
	// test that reinvite to a connecting dialog is not sent, but queued
	@Test
	public void testReinviteToConnectingDialogIsQueued() throws Exception {
        // setup
		MediaDescription mediaDescription = SessionDescriptionHelper.generateHoldMediaDescription();
        String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);
        waitForCallSendTryingRinging(getInboundCall());
        assertTrue(alertingSemaphore.tryAcquire(2, TimeUnit.SECONDS));
        
        // act
        outboundCallLegBean.reinviteCallLeg(firstDialogId, mediaDescription, AutoTerminateAction.False, null);
        
        // assert
        DialogInfo dialogInfo = dialogCollection.get(firstDialogId);
        assertNotNull(dialogInfo.getPendingReinvite());
        assertEquals(mediaDescription.toString(), dialogInfo.getPendingReinvite().getMediaDescription().toString());
	}
	
	// test that we ack an ok response to a reinvite even after a bye request has gone out
	@Test
	public void testReinviteOkAckAfterBye() throws Exception {
		// setup
        String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		outboundCallLegBean.reinviteCallLeg(firstDialogId, SessionDescriptionHelper.generateHoldMediaDescription(), AutoTerminateAction.False, null);
		
		// act
		outboundCallLegBean.terminateCallLeg(firstDialogId);
		
		// assert
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Inbound, getHoldMediaDescription());
		assertTrue(getInboundCall().waitForDisconnect(5000));
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Inbound, null);
		assertTrue(getInboundCall().respondToDisconnect());
	}

	// test that an 491 response to a reinvite results in the re-invite being re-sent sometime between 2.1 and 4 seconds later
	@Test
	public void testReinviteError491ResponseReinviteResent() throws Exception {
		// setup
        final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);

        waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);

		outboundCallLegBean.reinviteCallLeg(firstDialogId, SessionDescriptionHelper.generateHoldMediaDescription(), AutoTerminateAction.False, null);

		// act
		waitForReinviteRespondError(getInboundCall(), getInboundPhoneSipAddress(), Response.REQUEST_PENDING, "Request Pending");

		// assert
		long start = System.currentTimeMillis();
		SipTransaction secondReinviteTransaction = getInboundCall().waitForReinvite(FIVE_THOUSAND);
		assertNotNull(secondReinviteTransaction);
		long end = System.currentTimeMillis();
		assertTrue((end - start) > 2099);
		assertTrue((end - start) < 4001);
		String sdp = new String(getInboundCall().getLastReceivedRequest().getRawContent());
		
		assertTrue(sdp.contains(SessionDescriptionHelper.generateHoldMediaDescription().toString()));
	}

    public void onCallLegConnectionFailed(CallLegConnectionFailedEvent connectionFailedEvent) {
        connectionFailed = true;
        connectionFailedSemaphore.release();
    }

	public void onCallLegAlerting(CallLegAlertingEvent alertingEvent) {
		alertingSemaphore.release();
	}

	public void onCallLegConnected(CallLegConnectedEvent connectedEvent) {
	}

	public void onCallLegDisconnected(CallLegDisconnectedEvent disconnectedEvent) {
	}

	public void onCallLegTerminated(CallLegTerminatedEvent terminateEvent) {
		terminated = true;
		terminatedSemaphore.release();
	}

	public void onCallLegTerminationFailed(CallLegTerminationFailedEvent terminationFailedEvent) {
	}

	public void onCallLegRefreshCompleted(CallLegRefreshCompletedEvent callLegConnectedEvent) {
	}

	public void onReceivedCallLegRefresh(ReceivedCallLegRefreshEvent receivedCallLegRefreshEvent) {
	}
}
