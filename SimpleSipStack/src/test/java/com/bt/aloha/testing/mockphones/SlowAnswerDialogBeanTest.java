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
package com.bt.aloha.testing.mockphones;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import gov.nist.javax.sip.address.SipUri;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;

import javax.sip.ClientTransaction;
import javax.sip.TransactionState;
import javax.sip.address.Address;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.testing.SimpleSipStackPerClassTestCase;
import com.bt.aloha.testing.mockphones.SlowAnswerDialogBean;

public class SlowAnswerDialogBeanTest extends SimpleSipStackPerClassTestCase {
	private Random random = new Random((new Date()).getTime());
	private SlowAnswerDialogBean slowAnswerDialogBean;

	@Before
	public void before() {
    	slowAnswerDialogBean = (SlowAnswerDialogBean)applicationContext.getBean("slowAnswerMockphoneBean");
	}

	// test that the SimpleSipStackListener delegates properly on receiving a request for a slow answer phone
	@Test
	public void testSlowAnswerPhoneBean() throws Exception {
		// setup
		this.setOutboundCallTargetUsername("slowanswer");

		// act
		assertTrue(this.getOutboundCall().initiateOutgoingCall(this.getRemoteSipAddress(), this.getRemoteSipProxy()));

		// assert
		long timeBeforeAnswer = System.currentTimeMillis();
		assertTrue("No response", this.getOutboundCall().waitForAnswer(5000));
		assertTrue("Slow answer phone answered in less than 2 sec", System.currentTimeMillis() - 2000 > timeBeforeAnswer);
		assertEquals(Response.OK, this.getOutboundCall().getLastReceivedResponse().getStatusCode());

		assertTrue(getOutboundCall().sendInviteOkAck());
		assertEquals(1, slowAnswerDialogBean.getTimers().size());

		//listen for BYE
		this.getOutboundCall().listenForDisconnect();
		assertTrue(this.getOutboundCall().waitForDisconnect(8000));
		this.getOutboundCall().respondToDisconnect();
		Thread.sleep(500);
		assertEquals(0, slowAnswerDialogBean.getTimers().size());
	}

    // test that when a bye request is processed, the timer to hang up the mockphone is also killed off
    @Test
    public void testByeRequestKillsOffTimerThread() throws Exception {
    	// setup
    	this.setOutboundCallTargetUsername("slowanswer");

    	// act
    	assertTrue(this.getOutboundCall().initiateOutgoingCall(this.getRemoteSipAddress(), this.getRemoteSipProxy()));

    	// assert
    	assertOutboundCallResponses(new int[] {Response.RINGING, Response.OK});

    	assertTrue("Not sent ACK", this.getOutboundCall().sendInviteOkAck());
    	assertEquals(1, slowAnswerDialogBean.getTimers().size());

    	String callId = ((CallIdHeader)getOutboundCall().getLastReceivedResponse().getMessage().getHeader(CallIdHeader.NAME)).getCallId();
		ScheduledFuture<?> future = slowAnswerDialogBean.getTimers().get(callId);
    	future.cancel(true);
    	future = EasyMock.createNiceMock(ScheduledFuture.class);
    	EasyMock.expect(future.cancel(true)).andReturn(true);
    	EasyMock.replay(future);
    	slowAnswerDialogBean.getTimers().put(callId, future);

    	assertTrue(getOutboundCall().disconnect());
    	Thread.sleep(500);
    	assertEquals(0, slowAnswerDialogBean.getTimers().size());
    	EasyMock.verify(future);

	}

	// test that a CANCEL sent for an INVITE before we get back a OK is processed correctly
	@Test
	@SuppressWarnings("unchecked")
	public void testCancelResponse() throws Exception {
		// setup
		this.setOutboundCallTargetUsername("slowanswer2");

		CallIdHeader callIdHeader = getSipStack().getSipProvider().getNewCallId();

		CSeqHeader cseqHeader = getSipStack().getHeaderFactory().createCSeqHeader((long)1, Request.INVITE);

		Address toAddress = getSipStack().getAddressFactory().createAddress(getSipStack().getAddressFactory().createURI(getRemoteSipAddress()));
		ToHeader toHeader = getSipStack().getHeaderFactory().createToHeader(toAddress, null);

		Address fromAddress = getSipStack().getAddressFactory().createAddress(getOutboundPhoneSipAddress());
		FromHeader fromHeader = getSipStack().getHeaderFactory().createFromHeader(fromAddress, Integer.toString(this.random.nextInt(Integer.MAX_VALUE)));

		Address contactAddress = getSipStack().getAddressFactory().createAddress(getOutboundPhoneSipAddress());
		ContactHeader contactHeader = getSipStack().getHeaderFactory().createContactHeader(contactAddress);

		MaxForwardsHeader maxForwardsHeader = getSipStack().getHeaderFactory().createMaxForwardsHeader(5);
		List<ViaHeader> viaHeaders = getOutboundPhone().getViaHeaders();

		SipUri requestURI = (SipUri)getSipStack().getAddressFactory().createAddress("sip:" + getRemoteUser() + "@" + getRemoteHost()).getURI();
		Request request = getSipStack().getMessageFactory().createRequest(requestURI, Request.INVITE,
                callIdHeader, cseqHeader, fromHeader, toHeader, viaHeaders, maxForwardsHeader);

		Address routeAddress = getSipStack().getAddressFactory().createAddress(getRemoteSipAddress() + "/udp");
		request.addHeader(getSipStack().getHeaderFactory().createRouteHeader(routeAddress));
		request.addHeader(contactHeader);

		// act
		ClientTransaction inviteTrans = getSipStack().getSipProvider().getNewClientTransaction(request);
		inviteTrans.sendRequest();
		assertEquals(TransactionState.CALLING, inviteTrans.getState());
		Thread.sleep(500);
		assertEquals(TransactionState.PROCEEDING, inviteTrans.getState());

		Request cancelRequest = inviteTrans.createCancel();
		ClientTransaction cancelTrans = getSipStack().getSipProvider().getNewClientTransaction(cancelRequest);
		cancelTrans.sendRequest();

		// assert
		assertEquals(TransactionState.TRYING, cancelTrans.getState());
		Thread.sleep(500);
		assertEquals(TransactionState.COMPLETED, cancelTrans.getState());
	}
}
