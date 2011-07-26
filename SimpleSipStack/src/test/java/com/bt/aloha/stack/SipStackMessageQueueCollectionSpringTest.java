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

 	

 	
 	
 
package com.bt.aloha.stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sip.header.CSeqHeader;
import javax.sip.message.Request;

import org.cafesip.sipunit.SipRequest;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.callleg.OutboundCallLegBean;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.stack.SimpleSipStack;
import com.bt.aloha.stack.SipStackMessageQueueCollection;
import com.bt.aloha.testing.SimpleSipStackPerClassTestCase;
import com.bt.aloha.testing.SipUnitPhone;



public class SipStackMessageQueueCollectionSpringTest extends SimpleSipStackPerClassTestCase {
	private SimpleSipStack simpleSipStack;
	private OutboundCallLegBean outboundCallLegBean;
	private DialogCollection dialogCollection;

	@Before
	public void before() {
		simpleSipStack = (SimpleSipStack)applicationContext.getBean("simpleSipStack");
		outboundCallLegBean = (OutboundCallLegBean)applicationContext.getBean("outboundCallLegBean");
		dialogCollection = (DialogCollection)applicationContext.getBean("dialogCollection");
	}

	// test that out-of-order sip messages arrive in order
	@SuppressWarnings("unchecked")
	@Test
	public void testOutOfOrderSipMessagesArriveInOrder() throws Exception {
		// setup
		SipStackMessageQueueCollection queueCollection = new SipStackMessageQueueCollection();
		queueCollection.setScheduledExecutorService(new ScheduledThreadPoolExecutor(10));
		queueCollection.setQueuedSipMessageBlockingInterval(100000);
		queueCollection.setMaxTimeToLive(100000);
		simpleSipStack.setSipStackMessageQueueCollection(queueCollection);
		getInboundCall().listenForIncomingCall();
		final String dialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		outboundCallLegBean.connectCallLeg(dialogId);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		getInboundCall().listenForReinvite();
		getInboundCall().listenForDisconnect();
		final CountDownLatch latch = new CountDownLatch(1);

		DialogInfo dialogInfo = dialogCollection.get(dialogId);
		long inviteSequenceNumber = simpleSipStack.enqueueRequestAssignSequenceNumber(dialogId, 3, Request.INVITE);
		long byeSequenceNumber = simpleSipStack.enqueueRequestAssignSequenceNumber(dialogId, 4, Request.BYE);

		final Request inviteRequest = simpleSipStack.createRequest(getInboundPhoneSipAddress(), Request.INVITE, dialogId, inviteSequenceNumber, dialogInfo.getLocalParty(), dialogInfo.getLocalTag(), dialogInfo.getRemoteParty(), dialogInfo.getRemoteTag(), null, dialogInfo.getRouteList(), null);
		final Request byeRequest = simpleSipStack.createRequest(getInboundPhoneSipAddress(), Request.BYE, dialogId, byeSequenceNumber, dialogInfo.getLocalParty(), dialogInfo.getLocalTag(), dialogInfo.getRemoteParty(), dialogInfo.getRemoteTag(), null, dialogInfo.getRouteList(), null);

		Runnable r1 = new Runnable() {
			public void run() {
				simpleSipStack.sendRequest(byeRequest);
				latch.countDown();
			}
		};
		Runnable r2 = new Runnable() {
			public void run() {
				try {
					latch.await(20, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				simpleSipStack.sendRequest(inviteRequest);
			}
		};

		// act
		new Thread(r1).start();
		new Thread(r2).start();

		// assert
		assertNotNull("No reinvite", getInboundCall().waitForReinvite(1000));
		assertTrue("No BYE", getInboundCall().waitForDisconnect(1000));
		List<SipRequest> receivedRequests = getInboundCall().getAllReceivedRequests();
		assertEquals(4, receivedRequests.size());
		assertEquals(Request.INVITE, ((Request)receivedRequests.get(2).getMessage()).getMethod());
		assertEquals(3, ((CSeqHeader)receivedRequests.get(2).getMessage().getHeader(CSeqHeader.NAME)).getSeqNumber());
		assertEquals(Request.BYE, ((Request)receivedRequests.get(3).getMessage()).getMethod());
		assertEquals(4, ((CSeqHeader)receivedRequests.get(3).getMessage().getHeader(CSeqHeader.NAME)).getSeqNumber());
	}
}
