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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Vector;
import java.util.concurrent.ScheduledFuture;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sip.ServerTransaction;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.cafesip.sipunit.SipTransaction;
import org.easymock.classextension.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.dialog.DialogBeanHelper;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.TerminationMethod;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.testing.SimpleSipStackPerClassTestCase;
import com.bt.aloha.testing.mockphones.HangUpDialogBean;
import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;

public class HangUpDialogBeanTest extends SimpleSipStackPerClassTestCase {
	private HangUpDialogBean hangUpDialogBean;
	private DialogCollection dialogCollection;

	@Before
	public void before() {
    	hangUpDialogBean = (HangUpDialogBean)applicationContext.getBean("hangUpMockphoneBean");
    	DialogInfo dialogInfo = new DialogInfo("id", "bollocks", "1.2.3.4");
    	dialogInfo.setDialogState(DialogState.Confirmed);
    	dialogCollection = (DialogCollection)applicationContext.getBean("dialogCollection");
		dialogCollection.add(dialogInfo);
	}

	@After
	public void after() {
		dialogCollection.remove("id");
	}

	// test that the SimpleSipStackListener delegates properly on receiving a request for a busy phone
	@Test
	public void testInboundDialogDelegation() throws Exception {
		// setup
		this.setOutboundCallTargetUsername("hangup");

		// act
		assertTrue(this.getOutboundCall().initiateOutgoingCall(this.getRemoteSipAddress(), this.getRemoteSipProxy()));

		// assert
		assertOutboundCallResponses(new int[] {Response.RINGING, Response.OK});

		assertTrue("Not sent ACK", this.getOutboundCall().sendInviteOkAck());
		assertEquals(1, hangUpDialogBean.getTimers().size());

		this.getOutboundCall().listenForDisconnect();
		assertTrue(this.getOutboundCall().waitForDisconnect(8000));
		this.getOutboundCall().respondToDisconnect();
		Thread.sleep(500);
		assertEquals(0, hangUpDialogBean.getTimers().size());
	}

    // test that when a bye request is processed, the timer to hang up the mockphone is also killed off
    @Test
    public void testByeRequestKillsOffTimerThread() throws Exception {
    	// setup
    	this.setOutboundCallTargetUsername("hangup");

    	// act
    	assertTrue(this.getOutboundCall().initiateOutgoingCall(this.getRemoteSipAddress(), this.getRemoteSipProxy()));

    	// assert
    	assertOutboundCallResponses(new int[] {Response.RINGING, Response.OK});

    	assertTrue("Not sent ACK", this.getOutboundCall().sendInviteOkAck());
    	assertEquals(1, hangUpDialogBean.getTimers().size());

    	String callId = ((CallIdHeader)getOutboundCall().getLastReceivedResponse().getMessage().getHeader(CallIdHeader.NAME)).getCallId();
		ScheduledFuture<?> future = hangUpDialogBean.getTimers().get(callId);
    	future.cancel(true);
    	future = EasyMock.createNiceMock(ScheduledFuture.class);
    	EasyMock.expect(future.cancel(true)).andReturn(true);
    	EasyMock.replay(future);
    	hangUpDialogBean.getTimers().put(callId, future);

    	assertTrue(getOutboundCall().disconnect());
    	Thread.sleep(500);
    	assertEquals(0, hangUpDialogBean.getTimers().size());
    	EasyMock.verify(future);
	}

    @Test
	public void testProcessReinvite() throws Exception {
		// setup
		this.setOutboundCallTargetUsername("hangup2");
		this.getOutboundCall().listenForDisconnect();

		// act
		assertTrue(this.getOutboundCall().initiateOutgoingCall(this.getRemoteSipAddress(), this.getRemoteSipProxy()));

		// assert
		assertOutboundCallResponses(new int[] {Response.RINGING, Response.OK});
		this.getOutboundCall().sendInviteOkAck();
		Thread.sleep(100);
		SessionDescriptionHelper.setMediaDescription(getOutboundPhoneSdp(), getOutboundPhoneMediaDescription());
		SipTransaction trans = this.getOutboundCall().sendReinvite(null, null, getOutboundPhoneSdp().toString(), "application", "sdp");

		// assert
		assertNotNull(trans);
		waitForReinviteOKResponseAndAssertMediaDescription(getOutboundPhone(), trans, getOutboundPhoneHoldMediaDescription());
        assertTrue(this.getOutboundCall().sendReinviteOkAck(trans));

		assertTrue(this.getOutboundCall().waitForDisconnect(8000));
		this.getOutboundCall().respondToDisconnect();
	}

	@Test
	public void testProcessEmptyReinvite() throws Exception {
		// setup
		this.setOutboundCallTargetUsername("hangup3");
		this.getOutboundCall().listenForDisconnect();

		// act
		assertTrue(this.getOutboundCall().initiateOutgoingCall(this.getRemoteSipAddress(), this.getRemoteSipProxy()));

		// assert
		assertOutboundCallResponses(new int[] {Response.RINGING, Response.OK});
		this.getOutboundCall().sendInviteOkAck();
		Thread.sleep(100);
		SessionDescriptionHelper.setMediaDescription(getOutboundPhoneSdp(), getOutboundPhoneMediaDescription());
		SipTransaction trans = this.getOutboundCall().sendReinvite(null, null, (String)null, null, null);

		// assert
		assertNotNull(trans);
		waitForReinviteOKResponseAndAssertMediaDescription(getOutboundPhone(), trans, getOutboundPhoneHoldMediaDescription());
        assertTrue(this.getOutboundCall().sendReinviteOkAck(trans));

		assertTrue(this.getOutboundCall().waitForDisconnect(8000));
		this.getOutboundCall().respondToDisconnect();
	}

    private String createSessionDescriptionWithAudioAndVideoMedia() throws SdpException {
        SessionDescription sd = SessionDescriptionHelper.createSessionDescription("127.0.0.1", "SimpleSipStack");

        MediaDescription md1 = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"0"});
        md1.setAttribute("rtpmap", "0 PCMU/8000");
        MediaDescription md2 = SdpFactory.getInstance().createMediaDescription("video", 5678, 0, "RTP/AVP", new String[] {"0"});
        md2.setAttribute("rtpmap", "0 PCMU/8000");

        Vector<MediaDescription> mediaDescriptions = new Vector<MediaDescription>();
        mediaDescriptions.add(md1);
        mediaDescriptions.add(md2);
        sd.setMediaDescriptions(mediaDescriptions);

        return sd.toString();
    }

    // test that we return error if initial invite contains an active video media description
    @Test
    public void testProcessInitialInviteWithVideoMediaCodec() throws Exception {
        // setup
        this.setOutboundCallTargetUsername("hangup");

        // act
        assertTrue(getOutboundCall().initiateOutgoingCall(getRemoteSipAddress(), getRemoteSipAddress(), getRemoteSipProxy(),
                createSessionDescriptionWithAudioAndVideoMedia(), "application", "sdp", null, null));

        // assert
        assertOutboundCallResponses(new int[] {Response.NOT_ACCEPTABLE_HERE});
    }

    // test that hangup bean responds with video codec when called with "video" in the uri
    @Test
    public void testProcessInitialInviteWithVideo() {
        // setup
        this.setOutboundCallTargetUsername("video");

        // act
        assertTrue(this.getOutboundCall().initiateOutgoingCall(this.getRemoteSipAddress(), this.getRemoteSipProxy()));

        // assert
        assertOutboundCallResponses(new int[] {Response.RINGING, Response.OK});

        // no assert we get a video Media Description in the OK's SDP
        String sdp = new String(this.getOutboundCall().getLastReceivedResponse().getRawContent());
        assertTrue("no video media description in SDP", sdp.contains("m=video"));

        assertTrue("Not sent ACK", this.getOutboundCall().sendInviteOkAck());
        assertTrue(this.getOutboundCall().disconnect());
    }

    // test that hangup bean responds to empty INFO message with OK when its state is connected
    @Test
    public void testEmptyInfoResponseIs200WhenConnected() throws Exception {
    	// setup
    	Request request = EasyMock.createNiceMock(Request.class);
    	EasyMock.replay(request);
    	ServerTransaction serverTransaction = EasyMock.createNiceMock(ServerTransaction.class);
    	EasyMock.replay(serverTransaction);
    	DialogBeanHelper dialogBeanHelper = EasyMock.createNiceMock(DialogBeanHelper.class);
    	dialogBeanHelper.sendResponse(request, serverTransaction, Response.OK);
    	EasyMock.replay(dialogBeanHelper);
    	hangUpDialogBean.setDialogBeanHelper(dialogBeanHelper);

    	// act
    	hangUpDialogBean.processInfo(request, serverTransaction, "id");

    	// assert
    	EasyMock.verify(dialogBeanHelper);
	}

    // test that hangup bean responds to empty INFO message with 481 when its state is "terminating"
    @Test
    public void testEmptyInfoResponseIs481WhenTerminating() throws Exception {
    	// setup
    	Request request = EasyMock.createNiceMock(Request.class);
    	EasyMock.replay(request);
    	ServerTransaction serverTransaction = EasyMock.createNiceMock(ServerTransaction.class);
    	EasyMock.replay(serverTransaction);
    	DialogBeanHelper dialogBeanHelper = EasyMock.createNiceMock(DialogBeanHelper.class);
    	dialogBeanHelper.sendResponse(request, serverTransaction, Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
    	EasyMock.replay(dialogBeanHelper);
		hangUpDialogBean.setDialogBeanHelper(dialogBeanHelper);
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = dialogCollection.get("id");
				dialogInfo.setTerminationMethod(TerminationMethod.Terminate);
				dialogCollection.replace(dialogInfo);
			}

			public String getResourceId() {
				return "id";
			}
		};
		new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

    	// act
    	hangUpDialogBean.processInfo(request, serverTransaction, "id");

    	// assert
    	EasyMock.verify(dialogBeanHelper);
	}

    // test that hangup bean responds to empty INFO message with 481 when its state is terminated
    @Test
    public void testEmptyInfoResponseIs481WhenTerminated() throws Exception {
    	// setup
    	Request request = EasyMock.createNiceMock(Request.class);
    	EasyMock.replay(request);
    	ServerTransaction serverTransaction = EasyMock.createNiceMock(ServerTransaction.class);
    	EasyMock.replay(serverTransaction);
    	DialogBeanHelper dialogBeanHelper = EasyMock.createNiceMock(DialogBeanHelper.class);
    	dialogBeanHelper.sendResponse(request, serverTransaction, Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
    	EasyMock.replay(dialogBeanHelper);
		hangUpDialogBean.setDialogBeanHelper(dialogBeanHelper);
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = dialogCollection.get("id");
				dialogInfo.setDialogState(DialogState.Terminated);
				dialogCollection.replace(dialogInfo);
			}

			public String getResourceId() {
				return "id";
			}
		};
		new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

    	// act
    	hangUpDialogBean.processInfo(request, serverTransaction, "id");

    	// assert
    	EasyMock.verify(dialogBeanHelper);
	}
}
