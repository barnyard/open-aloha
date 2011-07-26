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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Vector;

import javax.sip.ClientTransaction;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.Timeout;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionAlreadyExistsException;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.AcceptEncodingHeader;
import javax.sip.header.AcceptHeader;
import javax.sip.header.AcceptLanguageHeader;
import javax.sip.header.AllowHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentLengthHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.SupportedHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.cafesip.sipunit.SipTransaction;
import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.callleg.OutboundCallLegBean;
import com.bt.aloha.dialog.DialogBeanHelper;
import com.bt.aloha.dialog.DialogJainSipListener;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.stack.SimpleSipStack;
import com.bt.aloha.stack.StackException;
import com.bt.aloha.testing.SimpleSipStackPerClassTestCase;


public class DialogJainSipListenerTest extends SimpleSipStackPerClassTestCase {
	private OutboundCallLegBean outboundCallLegBean;
	private DialogCollection dialogCollection;
	private DialogJainSipListener dialogSipListener;

	@Before
	public void before() {
		outboundCallLegBean = (OutboundCallLegBean)getApplicationContext().getBean("outboundCallLegBean");
		dialogCollection = (DialogCollection)getApplicationContext().getBean("dialogCollection");
		dialogSipListener = (DialogJainSipListener)getApplicationContext().getBean("dialogSipListener");
	}

	// test that the SimpleSipStackListener delegates properly on receiving a request for a busy phone
	@Test
	public void testInboundDialogDelegation() throws Exception {
		// setup
		this.setOutboundCallTargetUsername("busy");

		// act
		assertTrue(this.getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), this.getRemoteSipAddress(), this.getRemoteSipProxy(), createHoldSessionDescription().toString(), "application", "sdp", null, null));

		// assert
		assertOutboundCallResponses(new int[] {Response.BUSY_HERE});
	}

	// test error message for requests which don't match any delegation rules SHOULD be NOT_FOUND
	// See section 8.2 of RFC 3261 - AMO 
	@Test
	public void testNoDelegationRulesErrorMessage() throws Exception {
		// setup
		this.setOutboundCallTargetUsername("nothing");

		// act
		assertTrue(this.getOutboundCall().initiateOutgoingCall(getRemoteSipAddress(), getRemoteSipProxy()));

		// assert
		assertTrue("No response", this.getOutboundCall().waitOutgoingCallResponse(5000));
		assertEquals(Response.NOT_FOUND, this.getOutboundCall().getLastReceivedResponse().getStatusCode());
	}

	// test error message for requests which aren't INVITEs and there is no existing dialoginfo
	// even if there is no router rule...as per section 12.2.2 of RFC 3261 - AMO
	@Test
	public void testNoDialogNotInviteErrorMessage() throws Exception {
		// setup
		this.setOutboundCallTargetUsername("nothing");
		
		SipPhone parent = this.getOutboundCall().getParent();
		
		String method = "OK";

		Request msg = createRequestByMethod(parent, method);

        // act
		SipTransaction transaction = parent.sendRequestWithTransaction(msg, false, null);

		// assert
		assertNotNull(transaction);
		EventObject response_event = parent.waitResponse(transaction, 5000);
		assertNotNull("No response", response_event);
		assertEquals(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST, ((ResponseEvent)response_event).getResponse().getStatusCode());
	}

	// test that a response from the stack for an OPTIONS message contains the headers we add according to the rfc spec.
	@Test
	public void testOptionsRequestReturnsOK() throws Exception {
		// setup
		this.setOutboundCallTargetUsername("nothing");
		
		SipPhone parent = this.getOutboundCall().getParent();
		
		String method = "OPTIONS";

		Request msg = createRequestByMethod(parent, method);

        // act
		SipTransaction transaction = parent.sendRequestWithTransaction(msg, false, null);

		// assert
		assertNotNull(transaction);
		EventObject response_event = parent.waitResponse(transaction, 5000);
		assertNotNull("No response", response_event);
		assertEquals(Response.OK, ((ResponseEvent)response_event).getResponse().getStatusCode());
		
		Response response = ((ResponseEvent)response_event).getResponse();
		// assert no Allow Header, but headers exist for Accept, Accept-Encoding, Accept-Language, Supported and that Content-Length is 0
		assertNull(response.getHeader(AllowHeader.NAME));
		assertEquals("application", ((AcceptHeader)response.getHeader(AcceptHeader.NAME)).getContentType());
		assertEquals("sdp", ((AcceptHeader)response.getHeader(AcceptHeader.NAME)).getContentSubType());
		assertEquals("gzip", ((AcceptEncodingHeader)response.getHeader(AcceptEncodingHeader.NAME)).getEncoding());
		assertEquals(Locale.ENGLISH, ((AcceptLanguageHeader)response.getHeader(AcceptLanguageHeader.NAME)).getAcceptLanguage());
		assertEquals(0, ((ContentLengthHeader)response.getHeader(ContentLengthHeader.NAME)).getContentLength());
		
		/*
		 *  Check the presence of Supported Header. Since we send the header as blank, and JAIN-SIP returns it as null on the receiving end, 
		 *  we want to ensure the header's actually present in the message 
		 */
		ListIterator<?> it = response.getHeaderNames();
		boolean found = false;
		while (it.hasNext()) {
			if (it.next().equals(SupportedHeader.NAME)) {
				found = true;
				break;
			}
		}
		assertTrue("Supported Header is missing", found);
	}
	

	private Request createRequestByMethod(SipPhone parent, String method)
			throws ParseException, InvalidArgumentException {
		AddressFactory addr_factory = parent.getParent().getAddressFactory();
        HeaderFactory hdr_factory = parent.getParent().getHeaderFactory();
        MessageFactory msg_factory = parent.getParent().getMessageFactory();
		javax.sip.address.URI request_uri = addr_factory.createURI(getRemoteSipAddress());        
        CallIdHeader callId = parent.getParent().getSipProvider().getNewCallId();
        CSeqHeader cseq = hdr_factory.createCSeqHeader(1L, method);
        Address to_address = addr_factory.createAddress(request_uri);
        ToHeader to_header = hdr_factory.createToHeader(to_address, null);
        String myTag = parent.generateNewTag();
        Address myAddress = parent.getAddress();
        FromHeader from_header = hdr_factory.createFromHeader(myAddress, myTag);
        MaxForwardsHeader max_forwards = hdr_factory.createMaxForwardsHeader(70);
        ArrayList<?> via_headers = parent.getViaHeaders();
        Request msg = msg_factory.createRequest(request_uri, method, callId, cseq, from_header, to_header, via_headers, max_forwards);
        msg.addHeader((ContactHeader)parent.getContactInfo().getContactHeader().clone());
        String viaNonProxyRoute = getRemoteSipProxy();
		int xport_offset = viaNonProxyRoute.indexOf('/');
        SipURI route_uri = addr_factory.createSipURI(null, viaNonProxyRoute.substring(0, xport_offset));
        route_uri.setTransportParam(viaNonProxyRoute.substring(xport_offset + 1));
        route_uri.setSecure(((SipURI)request_uri).isSecure());
        route_uri.setLrParam();
        Address route_address = addr_factory.createAddress(route_uri);
        msg.addHeader(hdr_factory.createRouteHeader(route_address));
		return msg;
	}

	// test non-invite requests
	@Test
	public void testRegisterMessageErrorMessage() throws Exception {
		// setup
		SipPhone newPhone = this.getSipStack().createSipPhone(this.getRemoteHost(), SipStack.PROTOCOL_UDP, this.getRemotePort(), getOutboundPhoneSipAddress());

		// assert
		assertFalse(newPhone.register(getOutboundPhoneSipAddress(), 5));
	}

	// test non-sip uris error message
	// test response messages


	// test that a response is processed correctly
	@Test
	public void testProcessOkResponse() throws Exception {
		// setup
		this.setOutboundCallTargetUsername("hangup-" + System.currentTimeMillis());
		assertTrue(this.getOutboundCall().initiateOutgoingCall(getRemoteSipAddress(), getRemoteSipProxy()));

		assertWeGetOKWithHoldSdp();

		this.getOutboundCall().sendInviteOkAck();

		// act
		// Hangup phone sends BYE after 1s

		// assert
		assertTrue(this.getOutboundCall().listenForDisconnect());
		assertTrue(this.getOutboundCall().waitForDisconnect(5000));
		assertTrue(this.getOutboundCall().respondToDisconnect());
	}

	// test that the listener handles timeout event & delegates event to appropriate dialog for a client transaction
	@Test
	public void testClientTransactionTimeout() throws Exception {
		// setup
		String dialogId = outboundCallLegBean.createCallLeg(URI.create("sip:me@somewhere.net"), URI.create("sip:127.0.0.1:43210"), 0);
		outboundCallLegBean.connectCallLeg(dialogId, AutoTerminateAction.False);

		CallIdHeader callIdHeader = EasyMock.createMock(CallIdHeader.class);
		EasyMock.expect(callIdHeader.getCallId()).andReturn(dialogId);
		EasyMock.replay(callIdHeader);

		Request request = EasyMock.createMock(Request.class);
		EasyMock.expect(request.getHeader(CallIdHeader.NAME)).andReturn(callIdHeader);
		EasyMock.replay(request);

		ClientTransaction clientTransaction = EasyMock.createMock(ClientTransaction.class);
		EasyMock.expect(clientTransaction.getRequest()).andReturn(request);
		EasyMock.replay(clientTransaction);

		TimeoutEvent timeoutEvent = EasyMock.createMock(TimeoutEvent.class);
		EasyMock.expect(timeoutEvent.getClientTransaction()).andReturn(clientTransaction).anyTimes();
		EasyMock.expect(timeoutEvent.getTimeout()).andReturn(Timeout.TRANSACTION);
		EasyMock.expect(timeoutEvent.isServerTransaction()).andReturn(false).anyTimes();
		EasyMock.replay(timeoutEvent);

		// act
		dialogSipListener.processTimeout(timeoutEvent);

		// assert
		assertEquals(DialogState.Terminated, dialogCollection.get(dialogId).getDialogState());
	}

	// test that the listener handles timeout event & delegates event to appropriate dialog for a server transaction
	@Test
	public void testServerTransactionTimeout() throws Exception {
		// setup
		this.setOutboundCallTargetUsername("happy");

		assertTrue(this.getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), this.getRemoteSipAddress(), this.getRemoteSipProxy(), createHoldSessionDescription().toString(), "application", "sdp", null, null));
		assertTrue("Didn't get an answer", this.getOutboundCall().waitForAnswer(1000));
		String callId = ((CallIdHeader)this.getOutboundCall().getLastReceivedResponse().getMessage().getHeader(CallIdHeader.NAME)).getCallId();

		CallIdHeader callIdHeader = EasyMock.createMock(CallIdHeader.class);
		EasyMock.expect(callIdHeader.getCallId()).andReturn(callId);
		EasyMock.replay(callIdHeader);

		Request request = EasyMock.createMock(Request.class);
		EasyMock.expect(request.getHeader(CallIdHeader.NAME)).andReturn(callIdHeader);
		EasyMock.replay(request);

		ServerTransaction serverTransaction = EasyMock.createMock(ServerTransaction.class);
		EasyMock.expect(serverTransaction.getRequest()).andReturn(request);
		EasyMock.replay(serverTransaction);

		TimeoutEvent timeoutEvent = EasyMock.createMock(TimeoutEvent.class);
		EasyMock.expect(timeoutEvent.getServerTransaction()).andReturn(serverTransaction).anyTimes();
		EasyMock.expect(timeoutEvent.getTimeout()).andReturn(Timeout.TRANSACTION);
		EasyMock.expect(timeoutEvent.isServerTransaction()).andReturn(true).times(2);
		EasyMock.replay(timeoutEvent);

		// act
		dialogSipListener.processTimeout(timeoutEvent);

		// assert
		assertEquals(DialogState.Terminated, dialogCollection.get(callId).getDialogState());
	}

	// test that a request sent to a terminated dialog is rejected
	@Test
	public void testRequestToTerminatedDialogRejected() throws Exception {
		// Setup
		String dialogId = outboundCallLegBean.createCallLeg(URI.create("sip:me@somewhere.net"), URI.create("sip:127.0.0.1:43210"), 0);
		DialogInfo dialogInfo = dialogCollection.get(dialogId);
		dialogInfo.setDialogState(DialogState.Terminated);
		dialogInfo.setRemoteContact("sip:someone@somewhere.com");
		dialogCollection.replace(dialogInfo);
		SimpleSipStack simpleSipStack = (SimpleSipStack)getApplicationContext().getBean("simpleSipStack");
		final Vector<String> callbacks = new Vector<String>();
		DialogJainSipListener sssl = new DialogJainSipListener(simpleSipStack, dialogCollection) {
			@Override
			protected void sendErrorResponse(Request request, ServerTransaction serverTransaction, int errorCode, String reason) {
				callbacks.add("one");
			}
		};
		sssl.setApplicationContext(getApplicationContext());
		DialogBeanHelper dialogBeanHelper = new DialogBeanHelper();
		dialogBeanHelper.setSimpleSipStack(simpleSipStack);

		// act
		Request request = dialogBeanHelper.createByeRequest(dialogInfo);
		RequestEvent re = new RequestEvent(this, null, null, request);
		sssl.processRequest(re);

		// assert
		assertEquals(1, callbacks.size());
	}
	
//	 test that we bin any duplicate requests
	@Test
	public void testDuplicateRequestThrownAwayTransactionExistsExceptionAbsorbed() throws Exception {
		// setup
		StackException e = new StackException("oops", new TransactionAlreadyExistsException());
		SimpleSipStack simpleSipStack = EasyMock.createMock(SimpleSipStack.class);
		EasyMock.expect(simpleSipStack.createNewServerTransaction(null)).andThrow(e);
		EasyMock.replay(simpleSipStack);
		
		RequestEvent requestEvent = EasyMock.createNiceMock(RequestEvent.class);
		EasyMock.expect(requestEvent.getRequest()).andReturn(null);
		EasyMock.replay(requestEvent);
		
		// act
		DialogJainSipListener djsl = new DialogJainSipListener(simpleSipStack, dialogCollection);
		djsl.processRequest(requestEvent);
	}
	
	// test that we null out dialog info transactions once we get a transaction terminated event
	@Test
	@Ignore
	public void dialogTransactionRemotedOnTerminatedEvent() throws Exception {
		// setup
		CallIdHeader callIdHeader = EasyMock.createMock(CallIdHeader.class);
		EasyMock.expect(callIdHeader.getCallId()).andReturn("somerandomid");
		EasyMock.replay(callIdHeader);
		
		Request request = EasyMock.createNiceMock(Request.class);
		EasyMock.expect(request.getHeader(CallIdHeader.NAME)).andReturn(callIdHeader);
		EasyMock.replay(request);
		
		ClientTransaction clientTransaction = EasyMock.createNiceMock(ClientTransaction.class);
		EasyMock.expect(clientTransaction.getRequest()).andStubReturn(request);
		EasyMock.replay(clientTransaction);
		
		DialogInfo dialogInfo = new DialogInfo("somerandomid", "me", "1.2.3.4");
		dialogInfo.setInviteClientTransaction(clientTransaction);		
		dialogCollection.add(dialogInfo);
				
		TransactionTerminatedEvent transactionTerminatedEvent = EasyMock.createMock(TransactionTerminatedEvent.class);
		EasyMock.expect(transactionTerminatedEvent.isServerTransaction()).andStubReturn(false);
		EasyMock.expect(transactionTerminatedEvent.getClientTransaction()).andStubReturn(clientTransaction);
		EasyMock.replay(transactionTerminatedEvent);
		
		// act
		DialogJainSipListener djsl = new DialogJainSipListener(null, dialogCollection);
		djsl.processTransactionTerminated(transactionTerminatedEvent);

		// assert
		dialogInfo = dialogCollection.get("somerandomid");
		assertNull(dialogInfo.getInviteClientTransaction());
	}
	
}
