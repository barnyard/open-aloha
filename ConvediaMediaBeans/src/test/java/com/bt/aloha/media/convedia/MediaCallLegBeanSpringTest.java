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

 	

 	
 	
 
package com.bt.aloha.media.convedia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.call.CallListener;
import com.bt.aloha.call.event.CallConnectedEvent;
import com.bt.aloha.call.event.CallConnectionFailedEvent;
import com.bt.aloha.call.event.CallDisconnectedEvent;
import com.bt.aloha.call.event.CallTerminatedEvent;
import com.bt.aloha.call.event.CallTerminationFailedEvent;
import com.bt.aloha.callleg.OutboundCallLegListener;
import com.bt.aloha.callleg.event.CallLegAlertingEvent;
import com.bt.aloha.callleg.event.CallLegConnectedEvent;
import com.bt.aloha.callleg.event.CallLegConnectionFailedEvent;
import com.bt.aloha.callleg.event.CallLegDisconnectedEvent;
import com.bt.aloha.callleg.event.CallLegRefreshCompletedEvent;
import com.bt.aloha.callleg.event.CallLegTerminatedEvent;
import com.bt.aloha.callleg.event.CallLegTerminationFailedEvent;
import com.bt.aloha.callleg.event.ReceivedCallLegRefreshEvent;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.TerminationCause;
import com.bt.aloha.media.DtmfCollectCommand;
import com.bt.aloha.media.MediaCallLegListener;
import com.bt.aloha.media.MediaDialogInfo;
import com.bt.aloha.media.event.callleg.CallLegAnnouncementCompletedEvent;
import com.bt.aloha.media.event.callleg.CallLegAnnouncementFailedEvent;
import com.bt.aloha.media.event.callleg.CallLegAnnouncementTerminatedEvent;
import com.bt.aloha.media.event.callleg.CallLegDtmfGenerationCompletedEvent;
import com.bt.aloha.media.event.callleg.CallLegDtmfGenerationFailedEvent;
import com.bt.aloha.media.event.callleg.CallLegPromptAndCollectDigitsCompletedEvent;
import com.bt.aloha.media.event.callleg.CallLegPromptAndCollectDigitsFailedEvent;
import com.bt.aloha.media.event.callleg.CallLegPromptAndCollectDigitsTerminatedEvent;
import com.bt.aloha.media.event.callleg.CallLegPromptAndRecordCompletedEvent;
import com.bt.aloha.media.event.callleg.CallLegPromptAndRecordFailedEvent;
import com.bt.aloha.media.event.callleg.CallLegPromptAndRecordTerminatedEvent;
import com.bt.aloha.media.testing.mockphones.ConvediaMockphoneBean;
import com.bt.aloha.testing.SipUnitPhone;
import com.bt.aloha.util.HousekeeperAware;

public class MediaCallLegBeanSpringTest extends ConvediaMediaPerClassTestCase implements CallListener, OutboundCallLegListener, MediaCallLegListener{
	@Before
	public void before() throws Exception {
		callBean.addCallListener(this);
		mediaCallLegBean.addMediaCallLegListener(this);
	}

	@After
	public void after() {
		callBean.removeCallListener(this);
		mediaCallLegBean.removeMediaCallLegListener(this);
	}
	//test null callee dialog id throws exception
	@Test(expected=IllegalArgumentException.class)
	public void testDialogCreationFailsIfNullMediaServerAddress() throws Exception {
		// act
		mediaCallLegBean.createMediaCallLeg(null);
	}

	// test non-existent callee dialog id throws exception
	@Test(expected=IllegalArgumentException.class)
	public void testDialogCreationFailsIfNonExistentMediaServerAddress() throws Exception {
		// act
		mediaCallLegBean.createMediaCallLeg("i don't exist");
	}

	// test media server sip uri creation

	// test getting address and port from sdp
	@Test
	public void testAddressPortSdp() throws Exception {
		//setup
		String eol = "\r\n";
		String sdp =
			"v=0" + eol  +
			"o=- 7114186 7114186 IN IP4 cmsmpccontrol" + eol +
			"s=media server session" + eol +
			"t=0 0" + eol +
			"m=audio 33870 RTP/AVP 0 101" + eol +
			"c=IN IP4 1.2.3.4" + eol +
			"a=rtpmap:0 PCMU/8000" + eol +
			"a=rtpmap:101 telephone-event/8000" + eol +
			"a=fmtp:101 0-15,36" + eol +
			"a=sendrecv";
		SessionDescription sessionDescription = SdpFactory.getInstance().createSessionDescription(sdp);

		//act
		String rtp = ((MediaCallLegBeanImpl)mediaCallLegBean).getRtpTargetFromSessionDescription(sessionDescription);

		// assert
		assertEquals("1.2.3.4:33870", rtp);
	}

	// test exception if no media description in sdp
	@Test(expected=SDPParseException.class)
	public void testExceptionNoMediaDescription() throws Exception {
		//setup
		String eol = "\r\n";
		String sdp =
			"v=0" + eol  +
			"o=- 7114186 7114186 IN IP4 cmsmpccontrol" + eol +
			"s=media server session" + eol +
			"t=0 0";
		SessionDescription sessionDescription = SdpFactory.getInstance().createSessionDescription(sdp);

		// act
		((MediaCallLegBeanImpl)mediaCallLegBean).getRtpTargetFromSessionDescription(sessionDescription);
	}

	// test exception if no media description in sdp
	@Test(expected=IllegalStateException.class)
	public void testExceptionNullSdp() throws Exception {
		((MediaCallLegBeanImpl)mediaCallLegBean).getRtpTargetFromSessionDescription(null);
	}

	// test announcement playback failure with null dialog
	@Test(expected=IllegalArgumentException.class)
	public void testPlayAnnouncementNullDialog() throws Exception {
		mediaCallLegBean.playAnnouncement(null, null, null, true, true);
	}

	private MediaDialogInfo createMediaDialogInfo() {
		MediaDialogInfo mediaDialogInfo = new MediaDialogInfo("mediaDialogId" + Math.random(), "mediaDialogBean", "127.0.0.1", "sip:you", "sip:me", null, 0);
		return mediaDialogInfo;
	}

	// test announcement playback failure due to invalid target dialog status
	@Test(expected=IllegalStateException.class)
	public void testPlayAnnouncementInvalidTargetDialogStatus() throws Exception {
		// setup
		String dialogId = outboundCallLegBean.createCallLeg(URI.create("sip:me"), URI.create("sip:you"));
		MediaDialogInfo mediaDialogInfo = createMediaDialogInfo();
		String mediaDialogId = mediaDialogInfo.getId();
		dialogCollection.add(mediaDialogInfo);

		dialogCollection.get(dialogId).setDialogState(DialogState.Terminated);
		dialogCollection.get(mediaDialogId).setDialogState(DialogState.Confirmed);

		// act
		mediaCallLegBean.playAnnouncement(mediaDialogId, dialogId, "", true, true);
	}

	// test announcement playback failure due to invalid media dialog status
	@Test(expected=IllegalStateException.class)
	public void testPlayAnnouncementInvalidMediaDialogStatus() throws Exception {
		// setup
		String dialogId = outboundCallLegBean.createCallLeg(URI.create("sip:me"), URI.create("sip:you"));
		MediaDialogInfo mediaDialogInfo = createMediaDialogInfo();
		String mediaDialogId = mediaDialogInfo.getId();
		dialogCollection.add(mediaDialogInfo);

		dialogCollection.get(dialogId).setDialogState(DialogState.Confirmed);
		dialogCollection.get(mediaDialogId).setDialogState(DialogState.Initiated);

		// act
		mediaCallLegBean.playAnnouncement(mediaDialogId, dialogId, "", true, true);
	}

    // test prompt and collect digits failure with null dialog
    @Test(expected=IllegalArgumentException.class)
    public void testPromptAndCollectDigitsNullDialog() throws Exception {
        mediaCallLegBean.promptAndCollectDigits(null, null, null);
    }

    // test prompt and collect digits failure due to invalid target dialog status
    @Test(expected=IllegalStateException.class)
    public void testPromptAndCollectDigitsInvalidTargetDialogStatus() throws Exception {
        // setup
        String dialogId = outboundCallLegBean.createCallLeg(URI.create("sip:me"), URI.create("sip:you"));
        MediaDialogInfo mediaDialogInfo = createMediaDialogInfo();
        String mediaDialogId = mediaDialogInfo.getId();
        dialogCollection.add(mediaDialogInfo);

        dialogCollection.get(dialogId).setDialogState(DialogState.Terminated);
        dialogCollection.get(mediaDialogId).setDialogState(DialogState.Confirmed);

        // act
        mediaCallLegBean.promptAndCollectDigits(mediaDialogId, dialogId, null);
    }

    // test prompt and collect digits failure due to invalid media dialog status
    @Test(expected=IllegalStateException.class)
    public void testPromptAndCollectDigitsInvalidMediaDialogStatus() throws Exception {
        // setup
        String dialogId = outboundCallLegBean.createCallLeg(URI.create("sip:me"), URI.create("sip:you"));
        MediaDialogInfo mediaDialogInfo = createMediaDialogInfo();
        String mediaDialogId = mediaDialogInfo.getId();
        dialogCollection.add(mediaDialogInfo);

        dialogCollection.get(dialogId).setDialogState(DialogState.Confirmed);
        dialogCollection.get(mediaDialogId).setDialogState(DialogState.Initiated);

        // act
        mediaCallLegBean.promptAndCollectDigits(mediaDialogId, dialogId, null);
    }

	// test sip message flow for playing an announcement
	@Test
	public void testPlayAnnouncementSipMessageFlow() throws Exception {
		// setup
		MediaCallLegBeanImpl convediaMediaDialogBean = (MediaCallLegBeanImpl)mediaCallLegBean;
		String mediaDialogId = convediaMediaDialogBean.createMediaDialogLeg(convediaMediaDialogBean.getMediaServerSipUri(), 0);
		String dialogId = outboundCallLegBean.createCallLeg(URI.create("sip:@my.com"), getInboundPhoneSipUri());
		callBean.joinCallLegs(dialogId, mediaDialogId);

        // invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());

        waitForCallEvent(CallConnectedEvent.class);

		// act
		String commandId = convediaMediaDialogBean.playAnnouncement(mediaDialogId, dialogId, "abc.wav", true, true);
		waitForCallEvent(CallLegAnnouncementCompletedEvent.class);
		Thread.sleep(200);	// to give inbound call leg chance to die - it shouldn't

		// assert
		assertTrue("No DtmfGenerationCompletedEvent event", eventVector.get(1) instanceof CallLegAnnouncementCompletedEvent);
		assertEquals(commandId, ((CallLegAnnouncementCompletedEvent)eventVector.get(1)).getMediaCommandId());
		assertEquals(mediaDialogId, ((CallLegAnnouncementCompletedEvent)eventVector.get(1)).getId());

		assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
	}

	// test sip message flow for playing an announcement with iterations
	@Test
	public void testPlayAnnouncementIterationsSipMessageFlow() throws Exception {
		// setup
		MediaCallLegBeanImpl convediaMediaDialogBean = (MediaCallLegBeanImpl)mediaCallLegBean;
		String mediaDialogId = convediaMediaDialogBean.createMediaDialogLeg(convediaMediaDialogBean.getMediaServerSipUri(), 0);
		String dialogId = outboundCallLegBean.createCallLeg(URI.create("sip:@my.com"), getInboundPhoneSipUri());
		callBean.joinCallLegs(dialogId, mediaDialogId);

		// invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());

		waitForCallEvent(CallConnectedEvent.class);

		// act
		String commandId = convediaMediaDialogBean.playAnnouncement(mediaDialogId, dialogId, "abc.wav", true, true, 2);
		waitForCallEvent(CallLegAnnouncementCompletedEvent.class);
		Thread.sleep(200);	// to give inbound call leg chance to die - it shouldn't

		// assert
		assertTrue("No DtmfGenerationCompletedEvent event", eventVector.get(1) instanceof CallLegAnnouncementCompletedEvent);
		assertEquals(commandId, ((CallLegAnnouncementCompletedEvent)eventVector.get(1)).getMediaCommandId());
		assertEquals(mediaDialogId, ((CallLegAnnouncementCompletedEvent)eventVector.get(1)).getId());

		assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
	}

	// test sip message flow for playing an announcement with iterations and interval
	@Test
	public void testPlayAnnouncementIterationsWithIntervalSipMessageFlow() throws Exception {
		// setup
		MediaCallLegBeanImpl convediaMediaDialogBean = (MediaCallLegBeanImpl)mediaCallLegBean;
		String mediaDialogId = convediaMediaDialogBean.createMediaDialogLeg(convediaMediaDialogBean.getMediaServerSipUri(), 0);
		String dialogId = outboundCallLegBean.createCallLeg(URI.create("sip:@my.com"), getInboundPhoneSipUri());
		callBean.joinCallLegs(dialogId, mediaDialogId);

		// invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());

		waitForCallEvent(CallConnectedEvent.class);

		// act
		String commandId = convediaMediaDialogBean.playAnnouncement(mediaDialogId, dialogId, "abc.wav", true, true, 2, 750);
		waitForCallEvent(CallLegAnnouncementCompletedEvent.class);
		Thread.sleep(200);	// to give inbound call leg chance to die - it shouldn't

		// assert
		assertTrue("No DtmfGenerationCompletedEvent event", eventVector.get(1) instanceof CallLegAnnouncementCompletedEvent);
		assertEquals(commandId, ((CallLegAnnouncementCompletedEvent)eventVector.get(1)).getMediaCommandId());
		assertEquals(mediaDialogId, ((CallLegAnnouncementCompletedEvent)eventVector.get(1)).getId());

		assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
	}

	// test sip message flow for DTMF generation
	@Test
	public void testDtmfGenerationSipMessageFlow() throws Exception {
		// setup
		MediaCallLegBeanImpl convediaMediaDialogBean = (MediaCallLegBeanImpl)mediaCallLegBean;
		String mediaDialogId = convediaMediaDialogBean.createMediaDialogLeg(convediaMediaDialogBean.getMediaServerSipUri(), 0);
		String dialogId = outboundCallLegBean.createCallLeg(URI.create("sip:@my.com"), getInboundPhoneSipUri());
		callBean.joinCallLegs(dialogId, mediaDialogId);

		// invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());

		waitForCallEvent(CallConnectedEvent.class);

		// act
		String commandId = convediaMediaDialogBean.generateDtmfDigits(mediaDialogId, dialogId, "123");
		waitForCallEvent(CallLegDtmfGenerationCompletedEvent.class);
		Thread.sleep(200);	// to give inbound call leg chance to die - it shouldn't

		// assert
		assertTrue("No DtmfGenerationCompletedEvent event", eventVector.get(1) instanceof CallLegDtmfGenerationCompletedEvent);
		assertEquals(commandId, ((CallLegDtmfGenerationCompletedEvent)eventVector.get(1)).getMediaCommandId());
		assertEquals(mediaDialogId, ((CallLegDtmfGenerationCompletedEvent)eventVector.get(1)).getId());

		assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
	}

	// test sip message flow for DTMF generation with generation failure result
	@Test
	public void testDtmfGenerationSipMessageFlowGenerationFailure() throws Exception {
		// setup
		MediaCallLegBeanImpl convediaMediaDialogBean = (MediaCallLegBeanImpl)mediaCallLegBean;
		String mediaDialogId = convediaMediaDialogBean.createMediaDialogLeg(convediaMediaDialogBean.getMediaServerSipUri(), 0);
		String dialogId = outboundCallLegBean.createCallLeg(URI.create("sip:@my.com"), getInboundPhoneSipUri());
		callBean.joinCallLegs(dialogId, mediaDialogId);

        // invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
        waitForCallEvent(CallConnectedEvent.class);

		// act
		String commandId = convediaMediaDialogBean.generateDtmfDigits(mediaDialogId, dialogId, "666");
		waitForCallEvent(CallLegDtmfGenerationFailedEvent.class);
		Thread.sleep(200);	// to give inbound call leg chance to die - it shouldn't

		// assert
		assertTrue("No DialogDtmfGenerationFailedEvent event", eventVector.get(1) instanceof CallLegDtmfGenerationFailedEvent);
		assertEquals(commandId, ((CallLegDtmfGenerationFailedEvent)eventVector.get(1)).getMediaCommandId());
		assertEquals(mediaDialogId, ((CallLegDtmfGenerationFailedEvent)eventVector.get(1)).getId());

		assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
	}

	@Test
	public void testDtmfGenerationSipMessageFlowGenerationTerminated() throws Exception {
		// setup
		MediaCallLegBeanImpl convediaMediaDialogBean = (MediaCallLegBeanImpl)mediaCallLegBean;
		String mediaDialogId = convediaMediaDialogBean.createMediaDialogLeg(convediaMediaDialogBean.getMediaServerSipUri(), 0);
		String dialogId = outboundCallLegBean.createCallLeg(URI.create("sip:@my.com"), getInboundPhoneSipUri());
		callBean.joinCallLegs(dialogId, mediaDialogId);

        // invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
        waitForCallEvent(CallConnectedEvent.class);

		// act
		String commandId = convediaMediaDialogBean.generateDtmfDigits(mediaDialogId, dialogId, "13");
		waitForCallEvent(CallLegDtmfGenerationFailedEvent.class);
		Thread.sleep(200);	// to give inbound call leg chance to die - it shouldn't

		// assert
		assertTrue("No CallLegDtmfGenerationFailedEvent event", eventVector.get(1) instanceof CallLegDtmfGenerationFailedEvent);
		assertEquals(commandId, ((CallLegDtmfGenerationFailedEvent)eventVector.get(1)).getMediaCommandId());
		assertEquals(mediaDialogId, ((CallLegDtmfGenerationFailedEvent)eventVector.get(1)).getId());

		assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
	}

	// test sip message flow for cancelling media command
	@Test
	public void testCancelAnnouncementSipMessageFlow() throws Exception {
		// setup
        String audioFileUri = "http://PlayFailedTerminated.wav";

        MediaCallLegBeanImpl convediaMediaDialogBean = (MediaCallLegBeanImpl)mediaCallLegBean;
		String mediaDialogId = convediaMediaDialogBean.createMediaDialogLeg(convediaMediaDialogBean.getMediaServerSipUri(), 0);
		String dialogId = outboundCallLegBean.createCallLeg(URI.create("sip:@my.com"), getInboundPhoneSipUri());
		callBean.joinCallLegs(dialogId, mediaDialogId);

        // invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
        waitForCallEvent(CallConnectedEvent.class);
        String mediaCommandId = mediaCallLegBean.playAnnouncement(mediaDialogId, dialogId, audioFileUri, true, true);
        Thread.sleep(100);

		// act
		convediaMediaDialogBean.cancelMediaCommand(mediaDialogId, dialogId, mediaCommandId);

		// assert
        waitForCallEvent(CallLegAnnouncementTerminatedEvent.class);
	}

	// test sip message flow for cancelling media command
	@Test
	public void testCancelPromptInPromptAndCollectDigitsSipMessageFlow() throws Exception {
		// setup
        String audioFileUri = "http://DtmfFailedPlayTerminated.wav";

        MediaCallLegBeanImpl convediaMediaDialogBean = (MediaCallLegBeanImpl)mediaCallLegBean;
		String mediaDialogId = convediaMediaDialogBean.createMediaDialogLeg(convediaMediaDialogBean.getMediaServerSipUri(), 0);
		String dialogId = outboundCallLegBean.createCallLeg(URI.create("sip:@my.com"), getInboundPhoneSipUri());
		callBean.joinCallLegs(dialogId, mediaDialogId);

        // invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
        waitForCallEvent(CallConnectedEvent.class);
		DtmfCollectCommand dtmfCollectCommand =  new DtmfCollectCommand(audioFileUri, true, true, 12, 34, 56, 44);
		String mediaCommandId = convediaMediaDialogBean.promptAndCollectDigits(mediaDialogId, dialogId, dtmfCollectCommand);
        Thread.sleep(100);

		// act
		convediaMediaDialogBean.cancelMediaCommand(mediaDialogId, dialogId, mediaCommandId);

		// assert
        waitForCallEvent(CallLegPromptAndCollectDigitsTerminatedEvent.class);
	}

	// test sip message flow for cancelling media command
	@Test
	public void testCancelDtmfInPromptAndCollectDigitsSipMessageFlow() throws Exception {
		// setup
        String audioFileUri = "http://DtmfFailedTerminated.wav";

        MediaCallLegBeanImpl convediaMediaCallLegBean = (MediaCallLegBeanImpl)mediaCallLegBean;
		String mediaDialogId = convediaMediaCallLegBean.createMediaDialogLeg(convediaMediaCallLegBean.getMediaServerSipUri(), 0);
		String dialogId = outboundCallLegBean.createCallLeg(URI.create("sip:@my.com"), getInboundPhoneSipUri());
		callBean.joinCallLegs(dialogId, mediaDialogId);

        // invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
        waitForCallEvent(CallConnectedEvent.class);
		DtmfCollectCommand dtmfCollectCommand =  new DtmfCollectCommand(audioFileUri, true, true, 12, 34, 56, 44);
		String mediaCommandId = convediaMediaCallLegBean.promptAndCollectDigits(mediaDialogId, dialogId, dtmfCollectCommand);
        Thread.sleep(100);

		// act
		convediaMediaCallLegBean.cancelMediaCommand(mediaDialogId, dialogId, mediaCommandId);

		// assert
        waitForCallEvent(CallLegPromptAndCollectDigitsTerminatedEvent.class);
	}
	
	private static class MockHousekeeperCallLegHelper extends MediaCallLegBeanImpl.HousekeeperCallLegHelper
	{
		boolean called = false;
		String calledDialogId;
		TerminationCause calledTerminationCause;
		
		public MockHousekeeperCallLegHelper(){
			super(null, null);
		}
		@Override
		public void terminateCallLeg(String dialogId,
				TerminationCause dialogTerminationCause) {
			called = true;
			calledDialogId = dialogId;
			calledTerminationCause = dialogTerminationCause;
		}
		
	};	
	
	// Test that the class implements HousekeeperAware and that killHousekeeperCandidate is correctly invoked
	@Test
	public void testKillHousekeeperCandidateThroughInterface(){
		
		// setup
		MediaCallLegBeanImpl callLegBean = (MediaCallLegBeanImpl)mediaCallLegBean;

		MockHousekeeperCallLegHelper mockHousekeeperCallLegHelper = new MockHousekeeperCallLegHelper();
		
		String dialogId = callLegBean.createMediaDialogLeg(callLegBean.getMediaServerSipUri(), 0);
		callLegBean.setHousekeeperCallLegHelper(mockHousekeeperCallLegHelper);
		//act
		HousekeeperAware housekeeperAware = (HousekeeperAware)callLegBean;
		housekeeperAware.killHousekeeperCandidate(dialogId);		
		// assert
		assertTrue(mockHousekeeperCallLegHelper.called);
		assertEquals(dialogId, mockHousekeeperCallLegHelper.calledDialogId);
		assertEquals(TerminationCause.TerminatedByServer, mockHousekeeperCallLegHelper.calledTerminationCause);
		
	}

	public void onCallConnected(CallConnectedEvent callConnectedEvent) {
		eventVector.add(callConnectedEvent);
		semaphore.release();
	}

	public void onCallConnectionFailed(CallConnectionFailedEvent callConectionFailedEvent) {
	}

	public void onCallDisconnected(CallDisconnectedEvent callDisconnectedEvent) {
	}

	public void onCallTerminated(CallTerminatedEvent callTerminatedEvent) {
	}
	
	public void onCallTerminationFailed(CallTerminationFailedEvent callTerminationFailedEvent) {
	}

	public void onCallLegAnnouncementCompleted(CallLegAnnouncementCompletedEvent announcementCompletedEvent) {
		eventVector.add(announcementCompletedEvent);
		semaphore.release();
	}

	public void onCallLegAnnouncementFailed(CallLegAnnouncementFailedEvent announcementFailedEvent) {
	}

	public void onCallLegAnnouncementTerminated(CallLegAnnouncementTerminatedEvent announcementTerminatedEvent) {
		eventVector.add(announcementTerminatedEvent);
		semaphore.release();
	}

	public void onCallLegPromptAndCollectDigitsCompleted(CallLegPromptAndCollectDigitsCompletedEvent dtmfCollectDigitsCompletedEvent) {
	}

	public void onCallLegPromptAndCollectDigitsFailed(CallLegPromptAndCollectDigitsFailedEvent dtmfCollectDigitsFailedEvent) {
	}

	public void onCallLegPromptAndCollectDigitsTerminated(CallLegPromptAndCollectDigitsTerminatedEvent dtmfCollectDigitsTerminatedEvent) {
		eventVector.add(dtmfCollectDigitsTerminatedEvent);
		semaphore.release();
	}

	public void onCallLegDtmfGenerationCompleted(CallLegDtmfGenerationCompletedEvent dtmfGenerationCompletedEvent) {
		eventVector.add(dtmfGenerationCompletedEvent);
		semaphore.release();
	}

    public void onCallLegDtmfGenerationFailed(CallLegDtmfGenerationFailedEvent dtmfGenerationFailedEvent) {
		eventVector.add(dtmfGenerationFailedEvent);
		semaphore.release();
    }

	public void onCallLegConnected(CallLegConnectedEvent connectedEvent) {
	}

	public void onCallLegConnectionFailed(CallLegConnectionFailedEvent connectionFailedEvent) {
	}

	public void onCallLegDisconnected(CallLegDisconnectedEvent disconnectedEvent) {
	}

	public void onCallLegTerminated(CallLegTerminatedEvent terminatedEvent) {
	}

	public void onCallLegTerminationFailed(CallLegTerminationFailedEvent terminationFailedEvent) {
	}

	public void onCallLegAlerting(CallLegAlertingEvent alertingEvent) {
	}

	public void onCallLegRefreshCompleted(CallLegRefreshCompletedEvent callLegConnectedEvent) {
	}

	public void onReceivedCallLegRefresh(ReceivedCallLegRefreshEvent receivedCallLegRefreshEvent) {
	}

    public void onCallLegPromptAndRecordCompleted(CallLegPromptAndRecordCompletedEvent dtmfCollectDigitsCompletedEvent) {
    }

    public void onCallLegPromptAndRecordFailed(CallLegPromptAndRecordFailedEvent dtmfCollectDigitsFailedEvent) {
    }

    public void onCallLegPromptAndRecordTerminated(CallLegPromptAndRecordTerminatedEvent dtmfCollectDigitsTerminatedEvent) {
    }
}
