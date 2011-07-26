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
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.sdp.MediaDescription;
import javax.sdp.SessionDescription;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.Header;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.header.ProxyAuthorizationHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipRequest;
import org.cafesip.sipunit.SipTransaction;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.callleg.OutboundCallLegBean;
import com.bt.aloha.callleg.OutboundCallLegBeanImpl;
import com.bt.aloha.callleg.OutboundCallLegListener;
import com.bt.aloha.callleg.event.AbstractCallLegEvent;
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
import com.bt.aloha.dialog.state.TerminationCause;
import com.bt.aloha.dialog.state.TerminationMethod;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.testing.SimpleSipStackPerClassTestCase;
import com.bt.aloha.testing.SipUnitPhone;
import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;


public class CallLegBeanSpringTest extends SimpleSipStackPerClassTestCase implements OutboundCallLegListener {
	private static final String PROXY_AUTHENTICATION_REQUIRED = "Proxy Authentication Required";
	private OutboundCallLegBean outboundCallLegBean;
	private boolean terminated;
	private boolean disconnected;
	private boolean connectionFailed;
	private Semaphore connectedSemaphore;
	private Semaphore callLegRefreshCompletedSemaphore;
	private Semaphore terminatedSemaphore;
	private Semaphore disconnectedSemaphore;
	private Semaphore connectionFailedSemaphore;
	private DialogCollection dialogCollection;
	private Vector<AbstractCallLegEvent> events;
	private String firstDialogId;

	@Before
    public void before() throws Exception {
	    dialogCollection = (DialogCollection)getApplicationContext().getBean("dialogCollection");
		outboundCallLegBean = (OutboundCallLegBean)getApplicationContext().getBean("outboundCallLegBean");
		List<OutboundCallLegListener> outboundCallLegListeners = new ArrayList<OutboundCallLegListener>();
	    outboundCallLegListeners.add(this);
	    ((OutboundCallLegBeanImpl)outboundCallLegBean).setOutboundCallLegListeners(outboundCallLegListeners);

		terminated = false;
		disconnected = false;
		connectionFailed = false;
		connectedSemaphore = new Semaphore(0);
		callLegRefreshCompletedSemaphore = new Semaphore(0);
		terminatedSemaphore = new Semaphore(0);
		disconnectedSemaphore = new Semaphore(0);
		connectionFailedSemaphore = new Semaphore(0);
		events = new Vector<AbstractCallLegEvent>();
	}

	// test that application-originated bye results in a terminated event
	@Test
	public void testProcessByeResponseFiresTerminatedEvent() throws Exception {
        // setup
        String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
        outboundCallLegBean.connectCallLeg(firstDialogId);
        waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);

        // act
        outboundCallLegBean.terminateCallLeg(firstDialogId);
		waitForByeAndRespond(getInboundCall());

        // assert
		assertTrue(terminatedSemaphore.tryAcquire(500, TimeUnit.MILLISECONDS));
		assertTrue("Terminated event was not fired", terminated);
		assertFalse("Disconnected event was fired", disconnected);
	}
	
	//Test that we can respond to a 407 Unauthorised with credentials
	@Test
	public void testProxyAuthorisationOfCallleg() throws Exception {
		//Setup
		URI toUri = getInboundPhoneSipUri();
		URI secureUri = getSecureURI(toUri, "fred", "flintstone");
		SipCall sipCall = startAuthorisationCall(secureUri);
		
		// act
		SipTransaction sipTransaction = sendProxyAuthRequired(sipCall);
		
		SipRequest authenticationInvite = sipCall.getLastReceivedRequest();
		ProxyAuthorizationHeader authHeader = (ProxyAuthorizationHeader)authenticationInvite.getMessage().getHeader(ProxyAuthorizationHeader.NAME);
		
		// assert
		acceptAuthorisationCall(toUri, authHeader, sipCall, sipTransaction);
	}
	
	// Test that we can respond to a 407 Unauthorised with credentials on the initial invite and subsequent
	// re-invites
	@Test
	public void testProxyAuthorisationOfCalllegWithReInvite() throws Exception {
//		//Setup
		//getSipStack().setTraceEnabled(true);
		URI toUri = getInboundPhoneSipUri();
		URI secureUri = getSecureURI(toUri, "fred", "flintstone");
		SipCall sipCall = startAuthorisationCall(secureUri);
		
		// act
		SipTransaction sipTransaction = sendProxyAuthRequired(sipCall); // waits for re-invite with auth
		
		SipRequest authenticationInvite = sipCall.getLastReceivedRequest();
		ProxyAuthorizationHeader inviteAuthHeader = (ProxyAuthorizationHeader)authenticationInvite.getMessage().getHeader(ProxyAuthorizationHeader.NAME);
		
		// assert
		acceptAuthorisationCall(toUri, inviteAuthHeader, sipCall, sipTransaction);

		// act - send reinvite
		outboundCallLegBean.reinviteCallLeg(firstDialogId, getHoldMediaDescription(), AutoTerminateAction.Unchanged, null);

		// listen for and respond to another re-invite
		assertTrue(sipCall.listenForReinvite());
		SipTransaction reinviteSipTransaction = sipCall.waitForReinvite(FIVE_THOUSAND);
		assertNotNull(reinviteSipTransaction);

		ArrayList<Header> headerList = buildProxyAuthenticateHeader();
		assertTrue(sipCall.respondToReinvite(reinviteSipTransaction, Response.PROXY_AUTHENTICATION_REQUIRED,
				PROXY_AUTHENTICATION_REQUIRED, 0, null, null, headerList, null, null));

		assertTrue(sipCall.listenForReinvite());
		SipTransaction sipTransaction1 = sipCall.waitForReinvite(FIVE_THOUSAND);
		SipRequest authenticationReinvite = sipCall.getLastReceivedRequest();
		
		assertTrue("no SDP in authorization re-invite", authenticationReinvite.getRawContent().length > 0);
		ProxyAuthorizationHeader reinviteAuthHeader = (ProxyAuthorizationHeader)authenticationReinvite.getMessage().getHeader(ProxyAuthorizationHeader.NAME);
		assertNotNull(reinviteAuthHeader);
		// assert
		
		checkAuthHeader(reinviteAuthHeader);
		
		//assertTrue(sipCall.respondToReinvite(sipTransaction, Response.TRYING, TRYING, 0, toUri.toString(), DISPLAY_NAME, null, APPLICATION, SDP));
		//assertTrue(sipCall.respondToReinvite(sipTransaction, Response.RINGING, RINGING, 0, toUri.toString(), DISPLAY_NAME, null, APPLICATION, SDP));
		SessionDescription sdp = getSipUnitPhoneSdp(SipUnitPhone.Inbound);
		MediaDescription mediaDescription = getSipUnitPhoneMediaDescription(SipUnitPhone.Inbound);
		SessionDescriptionHelper.setMediaDescription(sdp, mediaDescription);
		assertTrue(sipCall.respondToReinvite(sipTransaction1, Response.OK, OK, 0, toUri.toString(), DISPLAY_NAME, sdp.toString(), APPLICATION, SDP));
		
		// For some reason jain-sip doesn't pass the ACK onto sipunit here, so we can't check it
//		boolean ack = sipCall.waitForAck(FIVE_THOUSAND);
//		assertTrue("No ACK", ack);
//		assertTrue(new String(sipCall.getLastReceivedRequest().getRawContent()).contains(IN_IP4_0_0_0_0));
	}
	
	//Test that we get a callLegConnectionFailedEvent when we supply the wrong credentials to 407 challenge
	// and the server responds with a 403
	@Test
	public void testProxyAuthorisationOfCalllegBadCredentials403() throws Exception {
		//Setup
		URI toUri = getInboundPhoneSipUri();
		URI secureUri = getSecureURI(toUri, "fred", "flintstone");
		SipCall sipCall = startAuthorisationCall(secureUri);
		
		// act
		SipTransaction sipTransaction = sendProxyAuthRequired(sipCall);
		
		SipRequest authenticationInvite = sipCall.getLastReceivedRequest();
		ProxyAuthorizationHeader authHeader = (ProxyAuthorizationHeader)authenticationInvite.getMessage().getHeader(ProxyAuthorizationHeader.NAME);
		
		// assert
		checkAuthHeader(authHeader);
		
		// send rejection
		assertTrue(sipCall.respondToReinvite(sipTransaction, Response.FORBIDDEN, "Forbidden", 0, toUri.toString(), "display name", null, "application", "sdp"));
		waitForCallLegConnectionFailed(FIVE_THOUSAND, TerminationCause.Forbidden);
	}

	//Test that we get a callLegConnectionFailedEvent when we supply no credentials to 407 challenge
	// and the server responds with a 403
	@Test
	public void testProxyAuthorisationOfCalllegNoCredentials403() throws Exception {
		//Setup
		URI toUri = getInboundPhoneSipUri();
		//URI secureUri = getSecureURI(toUri, "fred", "flintstone");
		SipCall sipCall = startAuthorisationCall(toUri);
		
		// act
		SipTransaction sipTransaction = sendProxyAuthRequired(sipCall);
		
		SipRequest authenticationInvite = sipCall.getLastReceivedRequest();
		ProxyAuthorizationHeader authHeader = (ProxyAuthorizationHeader)authenticationInvite.getMessage().getHeader(ProxyAuthorizationHeader.NAME);
		assertNotNull(authHeader);
		
		// assert
		//checkAuthHeader(authHeader);
		
		// send rejection
		assertTrue(sipCall.respondToReinvite(sipTransaction, Response.FORBIDDEN, "Forbidden", 0, toUri.toString(), "display name", null, "application", "sdp"));
		waitForCallLegConnectionFailed(FIVE_THOUSAND, TerminationCause.Forbidden);
	}
	
	//Test that we get a callLegConnectionFailedEvent when we supply the wrong credentials to 407 challenge
	// and the server responds with another 407
	@Test
	public void testProxyAuthorisationOfCalllegBadCredentials407() throws Exception {
		//Setup
		URI toUri = getInboundPhoneSipUri();
		URI secureUri = getSecureURI(toUri, "fred", "flintstone");
		SipCall sipCall = startAuthorisationCall(secureUri);
		
		// act
		SipTransaction sipTransaction = sendProxyAuthRequired(sipCall);
		
		SipRequest authenticationInvite = sipCall.getLastReceivedRequest();
		ProxyAuthorizationHeader authHeader = (ProxyAuthorizationHeader)authenticationInvite.getMessage().getHeader(ProxyAuthorizationHeader.NAME);
		
		// assert
		checkAuthHeader(authHeader);

		// send rejection in the form of another 407
		ArrayList<Header> replaceHeaderList = new ArrayList<Header>();
		Header cseq = getSipStack().getHeaderFactory().createCSeqHeader(2L, "INVITE");
		replaceHeaderList.add(cseq);
		
		assertTrue(sipCall.respondToReinvite(sipTransaction, Response.PROXY_AUTHENTICATION_REQUIRED, 
				PROXY_AUTHENTICATION_REQUIRED, 0, toUri.toString(), "display name", buildProxyAuthenticateHeader(), replaceHeaderList, "application"));

		waitForCallLegConnectionFailed(FIVE_THOUSAND, TerminationCause.Forbidden);
	}
	
	//Test that we get a callLegConnectionFailedEvent when we supply no credentials to 407 challenge
	// and the server responds with another 407
	@Test
	public void testProxyAuthorisationOfCalllegNoCredentials407() throws Exception {
		//Setup
		URI toUri = getInboundPhoneSipUri();
		SipCall sipCall = startAuthorisationCall(toUri);
		
		// act
		SipTransaction sipTransaction = sendProxyAuthRequired(sipCall);
		
		SipRequest authenticationInvite = sipCall.getLastReceivedRequest();
		ProxyAuthorizationHeader authHeader = (ProxyAuthorizationHeader)authenticationInvite.getMessage().getHeader(ProxyAuthorizationHeader.NAME);
		assertNotNull(authHeader);
		
		// send rejection in the form of another 407
		ArrayList<Header> replaceHeaderList = new ArrayList<Header>();
		Header cseq = getSipStack().getHeaderFactory().createCSeqHeader(2L, "INVITE");
		replaceHeaderList.add(cseq);
		
		assertTrue(sipCall.respondToReinvite(sipTransaction, Response.PROXY_AUTHENTICATION_REQUIRED, 
				PROXY_AUTHENTICATION_REQUIRED, 0, toUri.toString(), "display name", buildProxyAuthenticateHeader(), replaceHeaderList, "application"));

		waitForCallLegConnectionFailed(FIVE_THOUSAND, TerminationCause.Forbidden);
	}
	
	private ArrayList<Header> buildProxyAuthenticateHeader() throws Exception {
		ArrayList<Header> headerList = new ArrayList<Header>();
		ProxyAuthenticateHeader h = getSipStack().getHeaderFactory().createProxyAuthenticateHeader("Digest");
		h.setNonce("bibblebobble");
		h.setRealm("bt.com");
		headerList.add(h);
		return headerList;
	}
	
	private SipTransaction sendProxyAuthRequired(SipCall sipCall) throws Exception {
		ArrayList<Header> headerList = buildProxyAuthenticateHeader();
		
		assertTrue(sipCall.listenForReinvite());
		assertTrue(sipCall.sendIncomingCallResponse(Response.PROXY_AUTHENTICATION_REQUIRED, 
				PROXY_AUTHENTICATION_REQUIRED, 0, headerList, null, null));
		SipTransaction sipTransaction = sipCall.waitForReinvite(FIVE_THOUSAND);
		assertNotNull(sipTransaction);
		return sipTransaction;
	}

	private void waitForCallLegConnectionFailed(int millis, TerminationCause cause) throws Exception {
		assertTrue("timed out waiting for CallLegConnectionFailedEvent", connectionFailedSemaphore.tryAcquire(millis, TimeUnit.MILLISECONDS));
		assertEquals(cause, ((CallLegConnectionFailedEvent)events.get(0)).getTerminationCause());
	}

	//Test that we can respond to a 401 Unauthorised with credentials
	@Test
	public void testAuthorisationOfCallleg() throws Exception {
		//Setup
		URI toUri = getInboundPhoneSipUri();
		URI secureUri = getSecureURI(toUri, "fred", "flintstone");
		SipCall sipCall = startAuthorisationCall(secureUri);
		
		// act
		ArrayList<Header> headerList = new ArrayList<Header>();
		WWWAuthenticateHeader h = getSipStack().getHeaderFactory().createWWWAuthenticateHeader("Digest");
		h.setNonce("bibblebobble");
		h.setRealm("bt.com");
		headerList.add(h);
		
		assertTrue(sipCall.listenForReinvite());
		assertTrue(sipCall.sendIncomingCallResponse(Response.UNAUTHORIZED, "UnAuthorised", 0,headerList,null,null));
		SipTransaction sipTransaction = sipCall.waitForReinvite(FIVE_THOUSAND);
		assertNotNull(sipTransaction);
		
		SipRequest authenticationInvite = sipCall.getLastReceivedRequest();
		AuthorizationHeader authHeader = (AuthorizationHeader)authenticationInvite.getMessage().getHeader(AuthorizationHeader.NAME);
		
		// assert
		acceptAuthorisationCall(toUri, authHeader, sipCall, sipTransaction);
	}

	private URI getSecureURI(URI toUri, String username, String password) {
		return URI.create(toUri.toString() + ";username=" + username + ";password=" + password);
	}
	
	private SipCall startAuthorisationCall(URI toUri) {
		firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), toUri);
        outboundCallLegBean.connectCallLeg(firstDialogId);
		SipCall sipCall = getSipUnitPhoneCall(SipUnitPhone.Inbound);
		assertTrue(sipCall.waitForIncomingCall(FIVE_THOUSAND));
		SipRequest inviteRequest = sipCall.getLastReceivedRequest();
		assertEquals(Request.INVITE, ((Request)inviteRequest.getMessage()).getMethod());
		return sipCall;
	}
	
	private void checkAuthHeader(AuthorizationHeader authHeader) {
		assertNotNull(authHeader);
		assertEquals("MD5",authHeader.getAlgorithm());
		assertEquals("bibblebobble", authHeader.getNonce());
		assertEquals("bt.com", authHeader.getRealm());
		assertEquals("Digest", authHeader.getScheme());
		assertEquals("fred", authHeader.getUsername());
	}

	private void acceptAuthorisationCall(URI toUri, AuthorizationHeader authHeader, SipCall sipCall, SipTransaction sipTransaction) {
		checkAuthHeader(authHeader);
		assertTrue(sipCall.respondToReinvite(sipTransaction, Response.TRYING, TRYING, 0, toUri.toString(), DISPLAY_NAME, null, APPLICATION, SDP));
		assertTrue(sipCall.respondToReinvite(sipTransaction, Response.RINGING, RINGING, 0, toUri.toString(), DISPLAY_NAME, null, APPLICATION, SDP));
		SessionDescription sdp = getSipUnitPhoneSdp(SipUnitPhone.Inbound);
		MediaDescription mediaDescription = getSipUnitPhoneMediaDescription(SipUnitPhone.Inbound);
		SessionDescriptionHelper.setMediaDescription(sdp, mediaDescription);
		assertTrue(sipCall.respondToReinvite(sipTransaction, Response.OK, OK, 0, toUri.toString(), DISPLAY_NAME, sdp.toString(), APPLICATION, SDP));
//		System.out.println("###############################################");
		//sipCall.listenForAck();
		boolean ack = sipCall.waitForAck(FIVE_THOUSAND);
//		System.out.println("111111111111111111111111 " + sipCall.getReturnCode());
//		System.out.println("222222222222222222222222 " + sipCall.getErrorMessage());
		//System.out.println(sipCall.getAllReceivedRequests());
		//System.out.println(sipCall.getAllReceivedResponses());
		assertTrue("No ACK", ack);
		assertTrue(new String(sipCall.getLastReceivedRequest().getRawContent()).contains(IN_IP4_0_0_0_0));
	}
	
	// test that application-originated cancel results in a connection failed event
	@Test
	public void testProcessCancelResponseFiresConnectionFailedEvent() throws Exception {
        // setup
        String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
        outboundCallLegBean.connectCallLeg(firstDialogId);
        waitForCallSendTryingRinging(getInboundCall());

		getInboundPhone().listenRequestMessage();

        // act
		outboundCallLegBean.cancelCallLeg(firstDialogId);
		waitForCancelRespondOk(getInboundPhone());

		getInboundCall().sendIncomingCallResponse(Response.REQUEST_TERMINATED, "Cancelled", 0);

        // assert
		assertTrue(connectionFailedSemaphore.tryAcquire(500, TimeUnit.MILLISECONDS));
		assertTrue("Connection Failed event was not fired", connectionFailed);
		assertFalse("Disconnected event was fired", disconnected);
		assertFalse("Terminated event was fired", terminated);
	}

	// test that passing null for autoterminate will not reset it
	@Test
	public void reinviteCallLegUnchangedAutoterminateDoesNotChange() throws Exception {
		// setup
        final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.True);

        waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);

		// act
        outboundCallLegBean.reinviteCallLeg(firstDialogId, getHoldMediaDescription(), AutoTerminateAction.Unchanged, null);

		// assert
        assertTrue("Autoterminate should not have been reset.", dialogCollection.get(firstDialogId).isAutoTerminate());
	}

	// test that we don't get an InvalidStateException while terminating a dialog that's in TERMINATED state
	@Test
	public void testTerminateTerminatedDialog() throws Exception {
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
        outboundCallLegBean.terminateCallLeg(firstDialogId);

        // assert
        // no exception
	}

	// test that terminate works on a dialog in confirmed state
	@Test
	public void testTerminateConfirmedState() throws Exception {
        // setup
        String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);

        waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);

        // act
		outboundCallLegBean.terminateCallLeg(firstDialogId);

		waitForByeAndRespond(getInboundCall());

        // assert
		assertTrue(terminatedSemaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
		assertTrue("Terminated event was not fired", terminated);
		assertEquals(TerminationCause.TerminatedByServer, dialogCollection.get(firstDialogId).getTerminationCause());
	}

	// test that we dont send BYE requests when TerminateMethod is Terminate and call terminate dialog
	@Test
	public void testTerminateConfirmedStateTerminationMethodSet() throws Exception {
        // setup
        final String firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
        outboundCallLegBean.connectCallLeg(firstDialogId, AutoTerminateAction.False);

        waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);

		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
        	public void execute() {
		        DialogInfo dialogInfo = dialogCollection.get(firstDialogId);
		        dialogInfo.setTerminationMethod(TerminationMethod.Terminate);
		        dialogInfo.setTerminationCause(TerminationCause.RemotePartyHungUp);
		        dialogCollection.replace(dialogInfo);
        	}
			public String getResourceId() {
				return firstDialogId;
			}
        };
        new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

        // act
		outboundCallLegBean.terminateCallLeg(firstDialogId);

		assertFalse("Unexpected BYE", getInboundCall().waitForDisconnect(500));
		assertEquals(TerminationCause.RemotePartyHungUp, dialogCollection.get(firstDialogId).getTerminationCause());
	}

	public void onCallLegConnected(CallLegConnectedEvent connectedEvent) {
		connectedSemaphore.release();
	}

	public void onCallLegConnectionFailed(CallLegConnectionFailedEvent connectionFailedEvent) {
		connectionFailed = true;
		events.add(connectionFailedEvent);
		connectionFailedSemaphore.release();
	}

	public void onCallLegAlerting(CallLegAlertingEvent alertingEvent) {
	}

	public void onCallLegDisconnected(CallLegDisconnectedEvent disconnectedEvent) {
		disconnected = true;
		disconnectedSemaphore.release();
	}

	public void onCallLegTerminated(CallLegTerminatedEvent terminateEvent) {
		terminated = true;
		terminatedSemaphore.release();
	}

	public void onCallLegTerminationFailed(CallLegTerminationFailedEvent terminationFailedEvent) {
	}

	public void onCallLegRefreshCompleted(CallLegRefreshCompletedEvent callLegConnectedEvent) {
		callLegRefreshCompletedSemaphore.release();
	}

	public void onReceivedCallLegRefresh(ReceivedCallLegRefreshEvent receivedCallLegRefreshEvent) {
	}
}
