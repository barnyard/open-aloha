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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.easymock.classextension.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.call.CallBean;
import com.bt.aloha.call.CallListener;
import com.bt.aloha.call.event.CallConnectedEvent;
import com.bt.aloha.call.event.CallConnectionFailedEvent;
import com.bt.aloha.call.event.CallDisconnectedEvent;
import com.bt.aloha.call.event.CallTerminatedEvent;
import com.bt.aloha.call.event.CallTerminationFailedEvent;
import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.media.DtmfCollectCommand;
import com.bt.aloha.media.MediaCallListener;
import com.bt.aloha.media.MediaDialogInfo;
import com.bt.aloha.media.PromptAndRecordCommand;
import com.bt.aloha.media.event.call.AbstractMediaCallCommandEvent;
import com.bt.aloha.media.event.call.CallAnnouncementCompletedEvent;
import com.bt.aloha.media.event.call.CallAnnouncementFailedEvent;
import com.bt.aloha.media.event.call.CallAnnouncementTerminatedEvent;
import com.bt.aloha.media.event.call.CallDtmfGenerationCompletedEvent;
import com.bt.aloha.media.event.call.CallDtmfGenerationFailedEvent;
import com.bt.aloha.media.event.call.CallPromptAndCollectDigitsCompletedEvent;
import com.bt.aloha.media.event.call.CallPromptAndCollectDigitsFailedEvent;
import com.bt.aloha.media.event.call.CallPromptAndCollectDigitsTerminatedEvent;
import com.bt.aloha.media.event.call.CallPromptAndRecordCompletedEvent;
import com.bt.aloha.media.event.call.CallPromptAndRecordFailedEvent;
import com.bt.aloha.media.event.call.CallPromptAndRecordTerminatedEvent;
import com.bt.aloha.media.testing.mockphones.ConvediaMockphoneBean;
import com.bt.aloha.testing.SipUnitPhone;

public class MediaCallBeanSpringTest extends ConvediaMediaPerClassTestCase implements CallListener, MediaCallListener {
	private String dialogId;
	private String audioFileUri;

	@Before
	public void before() throws Exception {
		audioFileUri = "http://localhost/nothing.wav";
		dialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());

		callBean.addCallListener(this);
		mediaCallBean.addMediaCallListener(this);
	}

	@After
	public void after() {
		if (mediaCallBean != null)
			mediaCallBean.removeMediaCallListener(this);
		if (callBean != null)
			callBean.removeCallListener(this);
	}

	// test that media call creation creates a new media dialog leg
	@Test
	public void testMediaCallCreationCreatesMediaDialog() throws Exception {
		// act
		String mediaCallId = mediaCallBean.createMediaCall(dialogId);
		String mediaDialogId = callCollection.get(mediaCallId).getSecondDialogId();

		// assert
		assertEquals("First dialog should be the called party leg", dialogId, callCollection.get(mediaCallId).getFirstDialogId());
		assertTrue("Second dialog should be the media leg", dialogCollection.get(mediaDialogId) instanceof MediaDialogInfo);

		// so that we don't receive a CallConnectionFailedEvent in a future test
		// invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
		waitForCallEvent(CallConnectedEvent.class);
	}

	// test sip message flow for announcement playback
	@Test
	public void testPlayAnnouncement() throws Exception {
		// setup
		String mediaCallId = mediaCallBean.createMediaCall(dialogId);

		// invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
		waitForCallEvent(CallConnectedEvent.class);

		// act
		String commandId = mediaCallBean.playAnnouncement(mediaCallId, audioFileUri);
		waitForCallEvent(CallAnnouncementCompletedEvent.class);
		Thread.sleep(200);	// to give inbound call leg chance to die - it shouldn't

		// assert
		assertTrue("No AnnouncementCompleted event", eventVector.get(1) instanceof CallAnnouncementCompletedEvent);
		assertEquals("100ms", ((CallAnnouncementCompletedEvent)eventVector.get(1)).getDuration());
		assertEquals(commandId, ((CallAnnouncementCompletedEvent)eventVector.get(1)).getMediaCommandId());
        assertFalse(((CallAnnouncementCompletedEvent)eventVector.get(1)).getBarged());
		assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
	}

	// test sip message flow for announcement playback with iterations of the announcement
	@Test
	public void testPlayAnnouncementIterations() throws Exception {
		// setup
		String mediaCallId = mediaCallBean.createMediaCall(dialogId);

		// invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
		waitForCallEvent(CallConnectedEvent.class);

		// act
		String commandId = mediaCallBean.playAnnouncement(mediaCallId, audioFileUri, 2);
		waitForCallEvent(CallAnnouncementCompletedEvent.class);
		Thread.sleep(200);	// to give inbound call leg chance to die - it shouldn't

		// assert
		assertTrue("No AnnouncementCompleted event", eventVector.get(1) instanceof CallAnnouncementCompletedEvent);
		assertEquals("100ms", ((CallAnnouncementCompletedEvent)eventVector.get(1)).getDuration());
		assertEquals(commandId, ((CallAnnouncementCompletedEvent)eventVector.get(1)).getMediaCommandId());
		assertFalse(((CallAnnouncementCompletedEvent)eventVector.get(1)).getBarged());
		assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
	}

	// test sip message flow for announcement playback with iterations with interval of the announcement
	@Test
	public void testPlayAnnouncementIterationsWithInterval() throws Exception {
		// setup
		String mediaCallId = mediaCallBean.createMediaCall(dialogId);

		// invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
		waitForCallEvent(CallConnectedEvent.class);

		// act
		String commandId = mediaCallBean.playAnnouncement(mediaCallId, audioFileUri, 2, 500);
		waitForCallEvent(CallAnnouncementCompletedEvent.class);
		Thread.sleep(200);	// to give inbound call leg chance to die - it shouldn't

		// assert
		assertTrue("No AnnouncementCompleted event", eventVector.get(1) instanceof CallAnnouncementCompletedEvent);
		assertEquals("100ms", ((CallAnnouncementCompletedEvent)eventVector.get(1)).getDuration());
		assertEquals(commandId, ((CallAnnouncementCompletedEvent)eventVector.get(1)).getMediaCommandId());
		assertFalse(((CallAnnouncementCompletedEvent)eventVector.get(1)).getBarged());
		assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
	}

    // test sip message flow for announcement playback that was barged
    @Test
    public void testPlayAnnouncementBarged() throws Exception {
        // setup
        audioFileUri = "http://localhost/nothingAndBarged.wav";
        String mediaCallId = mediaCallBean.createMediaCall(dialogId);

        // invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
        waitForCallEvent(CallConnectedEvent.class);

        // act
        String commandId = mediaCallBean.playAnnouncement(mediaCallId, audioFileUri, 1, 0, true);
        waitForCallEvent(CallAnnouncementCompletedEvent.class);
        Thread.sleep(200);  // to give inbound call leg chance to die - it shouldn't

        // assert
        assertTrue("No AnnouncementCompleted event", eventVector.get(1) instanceof CallAnnouncementCompletedEvent);
        assertEquals("100ms", ((CallAnnouncementCompletedEvent)eventVector.get(1)).getDuration());
        assertEquals(commandId, ((CallAnnouncementCompletedEvent)eventVector.get(1)).getMediaCommandId());
        assertTrue(((CallAnnouncementCompletedEvent)eventVector.get(1)).getBarged());
        assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
    }

	// test that announcement failed event gets thrown when media server responds to invite with a "bad request" response
	@Test
	public void testBadRequestAnnouncementFailed() throws Exception {
		// setup
		String mediaCallId = mediaCallBean.createMediaCall(dialogId);
		audioFileUri = "http://BadRequest.wav";

		// invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
		waitForCallEvent(CallConnectedEvent.class);

		// act
		String announcementId = mediaCallBean.playAnnouncement(mediaCallId, audioFileUri);
		waitForCallEvent(CallAnnouncementFailedEvent.class);
		Thread.sleep(200);	// to give inbound call leg chance to die - it shouldn't

		// assert
		assertTrue("No AnnouncementFailed event", eventVector.get(1) instanceof CallAnnouncementFailedEvent);
		assertEquals(announcementId, ((AbstractMediaCallCommandEvent)eventVector.get(1)).getMediaCommandId());
		assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
	}

	// test that announcement failed event gets thrown when media server responds with INFO request containing msml without play.complete etc.
	@Test
	public void testEmptyMsmlAnnouncementFailed() throws Exception {
		// setup
		String mediaCallId = mediaCallBean.createMediaCall(dialogId);
		audioFileUri = "http://FileNotFound.wav";

		// invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
		waitForCallEvent(CallConnectedEvent.class);

		// act
		mediaCallBean.playAnnouncement(mediaCallId, audioFileUri);
		waitForCallEvent(CallAnnouncementFailedEvent.class);
		Thread.sleep(200);	// to give inbound call leg chance to die - it shouldn't

		// assert
		assertTrue("No AnnouncementFailed event", eventVector.get(1) instanceof CallAnnouncementFailedEvent);
		assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
	}

    // test that announcement failed event gets thrown when media server responds with INFO request containing msml play.failed.
    @Test
    public void testPlayFailedMsmlAnnouncementFailed() throws Exception {
        // setup
        String mediaCallId = mediaCallBean.createMediaCall(dialogId);
        audioFileUri = "http://PlayFailed.wav";

        // invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
        waitForCallEvent(CallConnectedEvent.class);

        // act
        mediaCallBean.playAnnouncement(mediaCallId, audioFileUri);
        waitForCallEvent(CallAnnouncementFailedEvent.class);
        Thread.sleep(200);  // to give inbound call leg chance to die - it shouldn't

        // assert
        assertTrue("No AnnouncementFailed event", eventVector.get(1) instanceof CallAnnouncementFailedEvent);
        assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
    }

	// test that the non-media dialog in a media call is auto terminated when the media dialog terminates
	@Test
	public void testNonMediaDialogAutoTermination() throws Exception {
		// setup
		String mediaCallId = mediaCallBean.createMediaCall(dialogId, AutoTerminateAction.True);
		String mediaDialogId = callCollection.get(mediaCallId).getSecondDialogId();

		// invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
		waitForCallEvent(CallConnectedEvent.class);

		Thread.sleep(100);

		// act
		outboundCallLegBean.terminateCallLeg(mediaDialogId);
		waitForCallEvent(CallTerminatedEvent.class);
		waitForByeAndRespond(getInboundCall());
		Thread.sleep(300);

		// assert
		assertEquals("Expected 2 events", 2, eventVector.size());
		assertTrue("No CallTerminated event", eventVector.get(1) instanceof CallTerminatedEvent);
		assertEquals("Calling party dialog should be terminated", DialogState.Terminated, dialogCollection.get(dialogId).getDialogState());
	}

	// test that the media dialog in a media call is auto terminated when the non-media dialog terminates, even if auto-terminate not set
	@Test
	public void testMediaDialogTerminationUponTerminationOfNonMediaDialog() throws Exception {
		// setup
		String mediaCallId = mediaCallBean.createMediaCall(dialogId, AutoTerminateAction.False);
		String mediaDialogId = callCollection.get(mediaCallId).getSecondDialogId();

		// invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
		waitForCallEvent(CallConnectedEvent.class);

		Thread.sleep(100);

		// act
		outboundCallLegBean.terminateCallLeg(dialogId);
		waitForByeAndRespond(getInboundCall());
		waitForCallEvent(CallTerminatedEvent.class);
// TODO: Please someone get rid of this...
		Thread.sleep(1000);

		// assert
		assertEquals("Expected 2 events", 2, eventVector.size());
		assertTrue("No CallTerminated event", eventVector.get(1) instanceof CallTerminatedEvent);
		assertEquals("Media dialog should be terminated", DialogState.Terminated, dialogCollection.get(mediaDialogId).getDialogState());
	}

	// test that terminate media call terminates a call
	@Test
	public void testTerminateMediaCall() throws Exception {
		// setup
		String mediaCallId = mediaCallBean.createMediaCall(dialogId);

		// invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
		waitForCallEvent(CallConnectedEvent.class);

		// act
		mediaCallBean.terminateMediaCall(mediaCallId);
		waitForCallEvent(CallTerminatedEvent.class);

		// assert
		assertTrue("No CallTerminated event", eventVector.get(1) instanceof CallTerminatedEvent);
	}

	// test that exception is thrown when passing null dialogId
	@Test (expected=IllegalArgumentException.class)
	public void testExceptionNullDialogId() throws Exception {
		// setup
		mediaCallBean.createMediaCall(null);
	}

	// test that exception is thrown when passing null media call id
	@Test (expected=IllegalArgumentException.class)
	public void testExceptionNullMediaCallId() throws Exception {
		// act
		mediaCallBean.playAnnouncement(null, audioFileUri);
	}

	// test that exception is thrown when passing null audio file uri
	@Test (expected=IllegalArgumentException.class)
	public void testExceptionNullAudioFileUri() throws Exception {
		// act
		mediaCallBean.playAnnouncement("mediaCallId", null);
	}

	// test sip message flow for DTMF prompt and collect
	@Test
	public void testDtmfPromptAndCollectDigits() throws Exception {
		// setup
		String mediaCallId = mediaCallBean.createMediaCall(dialogId);

		// invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
		waitForCallEvent(CallConnectedEvent.class);
		DtmfCollectCommand dtmfCollectCommand =  new DtmfCollectCommand("DtmfValue_1234", true, true, 12, 34, 56, 44);

		// act
		String commandId = mediaCallBean.promptAndCollectDigits(mediaCallId, dtmfCollectCommand);
		waitForCallEvent(CallPromptAndCollectDigitsCompletedEvent.class);
		Thread.sleep(200);	// to give inbound call leg chance to die - it shouldn't

		// assert
		assertTrue("No DtmfCollectDigitsCompletedEvent event", eventVector.get(1) instanceof CallPromptAndCollectDigitsCompletedEvent);
		assertEquals("1234", ((CallPromptAndCollectDigitsCompletedEvent)eventVector.get(1)).getDigits());
		assertEquals(commandId, ((CallPromptAndCollectDigitsCompletedEvent)eventVector.get(1)).getMediaCommandId());
		assertEquals("dtmf.match", ((CallPromptAndCollectDigitsCompletedEvent)eventVector.get(1)).getDtmfResult());

		assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
	}

    // test that exception is thrown for null call Id
    @Test(expected=IllegalArgumentException.class)
    public void testDtmfPromptAndCollectDigitsNullCall() throws Exception {
        // setup
        DtmfCollectCommand dtmfCollectCommand =  new DtmfCollectCommand("DtmfValue_1234", true, true, 12, 34, 56, 44);

        // act
        mediaCallBean.promptAndCollectDigits(null, dtmfCollectCommand);
    }

    // test that exception is thrown for null dtmfCollectCommant
    @Test(expected=IllegalArgumentException.class)
    public void testDtmfPromptAndCollectDigitsNullDtmfCollectCommand() throws Exception {
        // act
        mediaCallBean.promptAndCollectDigits("abc123", null);
    }

    // test sip message flow for DTMF prompt and collect without input
    @Test
    public void testDtmfPromptAndCollectDigitsNoInputFailure() throws Exception {
    	// setup
    	String mediaCallId = mediaCallBean.createMediaCall(dialogId);

    	// invite-trying-ringing-ok-ack-reinvite + callconnected-event
    	waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
    	waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
    	waitForCallEvent(CallConnectedEvent.class);
    	DtmfCollectCommand dtmfCollectCommand =  new DtmfCollectCommand("DtmfFailedNoInputDtmfValue_t", true, true, 12, 34, 56, 44);

    	// act
    	String commandId = mediaCallBean.promptAndCollectDigits(mediaCallId, dtmfCollectCommand);
    	waitForCallEvent(CallPromptAndCollectDigitsFailedEvent.class);
    	Thread.sleep(200);	// to give inbound call leg chance to die - it shouldn't

    	// assert
    	assertTrue("No DtmfCollectDigitsFailedEvent event", eventVector.get(1) instanceof CallPromptAndCollectDigitsFailedEvent);
    	assertEquals("t", ((CallPromptAndCollectDigitsFailedEvent)eventVector.get(1)).getDigits());
    	assertEquals(commandId, ((CallPromptAndCollectDigitsFailedEvent)eventVector.get(1)).getMediaCommandId());
    	assertEquals("dtmf.noinput", ((CallPromptAndCollectDigitsFailedEvent)eventVector.get(1)).getDtmfResult());

    	assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
    }

	// test sip message flow for DTMF prompt and collect without match
	@Test
	public void testDtmfPromptAndCollectDigitsNoMatchFailure() throws Exception {
		// setup
		String mediaCallId = mediaCallBean.createMediaCall(dialogId);

		// invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
		waitForCallEvent(CallConnectedEvent.class);
		DtmfCollectCommand dtmfCollectCommand =  new DtmfCollectCommand("DtmfFailedNoMatchDtmfValue_t", true, true, 12, 34, 56, 44);

		// act
		String commandId = mediaCallBean.promptAndCollectDigits(mediaCallId, dtmfCollectCommand);
		waitForCallEvent(CallPromptAndCollectDigitsFailedEvent.class);
		Thread.sleep(200);	// to give inbound call leg chance to die - it shouldn't

		// assert
		assertTrue("No DtmfCollectDigitsFailedEvent event", eventVector.get(1) instanceof CallPromptAndCollectDigitsFailedEvent);
		assertEquals("t", ((CallPromptAndCollectDigitsFailedEvent)eventVector.get(1)).getDigits());
		assertEquals(commandId, ((CallPromptAndCollectDigitsFailedEvent)eventVector.get(1)).getMediaCommandId());
		assertEquals("dtmf.nomatch", ((CallPromptAndCollectDigitsFailedEvent)eventVector.get(1)).getDtmfResult());

		assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
	}

	// test sip message flow for DTMF prompt and collect with undefined failure
	@Test
	public void testDtmfPromptAndCollectDigitsUndefinedFailure() throws Exception {
		// setup
		String mediaCallId = mediaCallBean.createMediaCall(dialogId);

		// invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
		waitForCallEvent(CallConnectedEvent.class);
		DtmfCollectCommand dtmfCollectCommand =  new DtmfCollectCommand("DtmfFailedUndefinedDtmfValue_t", true, true, 12, 34, 56, 44);

		// act
		String commandId = mediaCallBean.promptAndCollectDigits(mediaCallId, dtmfCollectCommand);
		waitForCallEvent(CallPromptAndCollectDigitsFailedEvent.class);
		Thread.sleep(200);	// to give inbound call leg chance to die - it shouldn't

		// assert
		assertTrue("No DtmfCollectDigitsFailedEvent event", eventVector.get(1) instanceof CallPromptAndCollectDigitsFailedEvent);
		assertEquals("t", ((CallPromptAndCollectDigitsFailedEvent)eventVector.get(1)).getDigits());
		assertEquals(commandId, ((CallPromptAndCollectDigitsFailedEvent)eventVector.get(1)).getMediaCommandId());
		assertEquals("undefined", ((CallPromptAndCollectDigitsFailedEvent)eventVector.get(1)).getDtmfResult());

		assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
	}

	// test that DTMF generation command rejects null call id
	@Test(expected=IllegalArgumentException.class)
	public void testDtmfGenerationNullCallId() throws Exception {
		// act
		mediaCallBean.generateDtmfDigits(null, "123");
	}

	// test that DTMF generation command rejects null digits
	@Test(expected=IllegalArgumentException.class)
	public void testDtmfGenerationNullDigits() throws Exception {
		// act
		mediaCallBean.generateDtmfDigits("123", null);
	}

	// test that DTMF generation command rejects invalid digit length (too low)
	@Test(expected=IllegalArgumentException.class)
	public void testDtmfGenerationDigitLengthTooLow() throws Exception {
		// act
		mediaCallBean.generateDtmfDigits("123", "123", 49);
	}

	// test that DTMF generation command rejects invalid digit length (too high)
	@Test(expected=IllegalArgumentException.class)
	public void testDtmfGenerationDigitLengthTooHigh() throws Exception {
		// act
		mediaCallBean.generateDtmfDigits("123", "123", 30001);
	}

	// test sip message flow for DTMF generation
	@Test
	public void testDtmfGenerationSipMessageFlow() throws Exception {
		// setup
		String mediaCallId = mediaCallBean.createMediaCall(dialogId);

        // invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
		waitForCallEvent(CallConnectedEvent.class);

		// act
		String commandId = mediaCallBean.generateDtmfDigits(mediaCallId, "123");
		waitForCallEvent(CallDtmfGenerationCompletedEvent.class);
		Thread.sleep(200);	// to give inbound call leg chance to die - it shouldn't

		// assert
		assertTrue("No DtmfGenerationCompletedEvent event", eventVector.get(1) instanceof CallDtmfGenerationCompletedEvent);
		assertEquals(commandId, ((CallDtmfGenerationCompletedEvent)eventVector.get(1)).getMediaCommandId());
		assertEquals(mediaCallId, ((CallDtmfGenerationCompletedEvent)eventVector.get(1)).getCallId());

		assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
	}

	// test sip message flow for DTMF generation failure
	@Test
	public void testDtmfGenerationFailureSipMessageFlow() throws Exception {
		// setup
		String mediaCallId = mediaCallBean.createMediaCall(dialogId);

        // invite-trying-ringing-ok-ack-reinvite + callconnected-event
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
		waitForCallEvent(CallConnectedEvent.class);

		// act
		String commandId = mediaCallBean.generateDtmfDigits(mediaCallId, "666");
		waitForCallEvent(CallDtmfGenerationFailedEvent.class);
		Thread.sleep(200);	// to give inbound call leg chance to die - it shouldn't

		// assert
		assertTrue("No CallDtmfGenerationFailedEvent event", eventVector.get(1) instanceof CallDtmfGenerationFailedEvent);
		assertEquals(commandId, ((CallDtmfGenerationFailedEvent)eventVector.get(1)).getMediaCommandId());
		assertEquals(mediaCallId, ((CallDtmfGenerationFailedEvent)eventVector.get(1)).getCallId());

		assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
	}

	// test sip message flow for cancelling media command
	@Test
	public void testCancelAnnouncementSipMessageFlow() throws Exception {
		// setup
        String audioFileUri = "http://PlayFailedTerminated.wav";
		String mediaCallId = mediaCallBean.createMediaCall(dialogId);

		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
        waitForCallEvent(CallConnectedEvent.class);

        // act
		String announcementId = mediaCallBean.playAnnouncement(mediaCallId, audioFileUri);
        Thread.sleep(100);

		// act
		mediaCallBean.cancelMediaCommand(mediaCallId, announcementId);

		// assert
        waitForCallEvent(CallAnnouncementTerminatedEvent.class);
	}

	// test sip message flow for cancelling media command
	@Test
	public void testCancelPromptInPromptAndCollectDigitsSipMessageFlow() throws Exception {
		// setup
        String audioFileUri = "http://DtmfFailedPlayTerminated.wav";
		String mediaCallId = mediaCallBean.createMediaCall(dialogId);

		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
        waitForCallEvent(CallConnectedEvent.class);
		DtmfCollectCommand dtmfCollectCommand =  new DtmfCollectCommand(audioFileUri, true, true, 12, 34, 56, 44);
		String mediaCommandId = mediaCallBean.promptAndCollectDigits(mediaCallId, dtmfCollectCommand);
        Thread.sleep(100);

		// act
		mediaCallBean.cancelMediaCommand(mediaCallId, mediaCommandId);

		// assert
        waitForCallEvent(CallPromptAndCollectDigitsTerminatedEvent.class);
	}

	// test sip message flow for cancelling media command
	@Test
	public void testCancelDtmfInPromptAndCollectDigitsSipMessageFlow() throws Exception {
		// setup
        String audioFileUri = "http://DtmfFailedTerminated.wav";
		String mediaCallId = mediaCallBean.createMediaCall(dialogId);

		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
        waitForCallEvent(CallConnectedEvent.class);
		DtmfCollectCommand dtmfCollectCommand =  new DtmfCollectCommand(audioFileUri, true, true, 12, 34, 56, 44);
		String mediaCommandId = mediaCallBean.promptAndCollectDigits(mediaCallId, dtmfCollectCommand);
        Thread.sleep(100);

		// act
		mediaCallBean.cancelMediaCommand(mediaCallId, mediaCommandId);

		// assert
        waitForCallEvent(CallPromptAndCollectDigitsTerminatedEvent.class);
	}

//	 test that media call bean impl delegates call information retrieval to call bean
	@Test
	public void getCallInformationDelegatesToCallBean() {
		// setup
		CallBean callBean = EasyMock.createMock(CallBean.class);
		EasyMock.expect(callBean.getCallInformation("callId")).andReturn(null);
		EasyMock.replay(callBean);

		MediaCallBeanImpl mediaCallBean = new MediaCallBeanImpl();
		mediaCallBean.setCallBean(callBean);

		// act
		mediaCallBean.getCallInformation("callId");

		// assert
		EasyMock.verify(callBean);
	}

	// expecting exception when passing a null callId
    @Test(expected=IllegalArgumentException.class)
    public void testPromptAndRecordNullCallId() throws Exception {
        PromptAndRecordCommand promptAndRecordCommand = new PromptAndRecordCommand("uri", true, "dest", false, "format", 10, 1, 1, null);
        mediaCallBean.promptAndRecord(null, promptAndRecordCommand);
    }

    // expecting exception when passing a null promptAndRecordCommand
    @Test(expected=IllegalArgumentException.class)
    public void testPromptAndRecordNullPromptAndRecordCommand() throws Exception {
        mediaCallBean.promptAndRecord("123", null);
    }

    // test sip message flow for prompt and record
    @Test
    public void testPromptAndRecord() throws Exception {
        // setup
        String mediaCallId = mediaCallBean.createMediaCall(dialogId);

        // invite-trying-ringing-ok-ack-reinvite + callconnected-event
        waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
        waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
        waitForCallEvent(CallConnectedEvent.class);
        PromptAndRecordCommand promptAndRecordCommand = new PromptAndRecordCommand("file://PromptAndRecord_myFile.wav_1234", true, "file://dest", false, "audio/wav", 10, 1, 1, null);

        // act
        String commandId = mediaCallBean.promptAndRecord(mediaCallId, promptAndRecordCommand);
        waitForCallEvent(CallPromptAndRecordCompletedEvent.class);
        Thread.sleep(200);  // to give inbound call leg chance to die - it shouldn't

        // assert
        assertTrue("No CallCollectAndRecordCompletedEvent event", eventVector.get(1) instanceof CallPromptAndRecordCompletedEvent);
        assertEquals("myFile.wav", ((CallPromptAndRecordCompletedEvent)eventVector.get(1)).getAudioFileUri());
        assertEquals(commandId, ((CallPromptAndRecordCompletedEvent)eventVector.get(1)).getMediaCommandId());
        assertEquals("record.complete.maxlength", ((CallPromptAndRecordCompletedEvent)eventVector.get(1)).getRecordResult());
        assertEquals(1234, ((CallPromptAndRecordCompletedEvent)eventVector.get(1)).getRecordingLengthMillis());

        assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
    }

    // test sip message flow for prompt and record on failed call
    @Test
    public void testPromptAndRecordFailed() throws Exception {
        // setup
        String mediaCallId = mediaCallBean.createMediaCall(dialogId);

        // invite-trying-ringing-ok-ack-reinvite + callconnected-event
        waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
        waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
        waitForCallEvent(CallConnectedEvent.class);
        PromptAndRecordCommand promptAndRecordCommand = new PromptAndRecordCommand("file://PromptAndRecord_Failed_1234", true, "file://dest", false, "audio/wav", 10, 1, 1, null);

        // act
        String commandId = mediaCallBean.promptAndRecord(mediaCallId, promptAndRecordCommand);
        waitForCallEvent(CallPromptAndRecordFailedEvent.class);
        Thread.sleep(200);  // to give inbound call leg chance to die - it shouldn't

        // assert
        assertTrue("No CallCollectAndRecordFailedEvent event", eventVector.get(1) instanceof CallPromptAndRecordFailedEvent);
        assertEquals(commandId, ((CallPromptAndRecordFailedEvent)eventVector.get(1)).getMediaCommandId());
        assertEquals("record.failed", ((CallPromptAndRecordFailedEvent)eventVector.get(1)).getRecordResult());

        assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
    }

    // test sip message flow for prompt and record on failed call due to pre-speech timeout
    @Test
    public void testPromptAndRecordFailedPrespeech() throws Exception {
        // setup
        String mediaCallId = mediaCallBean.createMediaCall(dialogId);

        // invite-trying-ringing-ok-ack-reinvite + callconnected-event
        waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
        waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
        waitForCallEvent(CallConnectedEvent.class);
        PromptAndRecordCommand promptAndRecordCommand = new PromptAndRecordCommand("file://PromptAndRecord_FailedPrespeech_1234", true, "file://dest", false, "audio/wav", 10, 1, 1, null);

        // act
        String commandId = mediaCallBean.promptAndRecord(mediaCallId, promptAndRecordCommand);
        waitForCallEvent(CallPromptAndRecordFailedEvent.class);
        Thread.sleep(200);  // to give inbound call leg chance to die - it shouldn't

        // assert
        assertTrue("No CallCollectAndRecordFailedEvent event", eventVector.get(1) instanceof CallPromptAndRecordFailedEvent);
        assertEquals(commandId, ((CallPromptAndRecordFailedEvent)eventVector.get(1)).getMediaCommandId());
        assertEquals("record.failed.prespeech", ((CallPromptAndRecordFailedEvent)eventVector.get(1)).getRecordResult());

        assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
    }

    // test sip message flow for prompt and record on terminated call
    @Test
    public void testPromptAndRecordTerminated() throws Exception {
        // setup
        String mediaCallId = mediaCallBean.createMediaCall(dialogId);

        // invite-trying-ringing-ok-ack-reinvite + callconnected-event
        waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
        waitForAckAssertMediaDescription(SipUnitPhone.Inbound, ConvediaMockphoneBean.getOfferMediaDescription());
        waitForCallEvent(CallConnectedEvent.class);
        PromptAndRecordCommand promptAndRecordCommand = new PromptAndRecordCommand("file://PromptAndRecord_Terminated_1234", true, "file://dest", false, "audio/wav", 10, 1, 1, null);

        // act
        String commandId = mediaCallBean.promptAndRecord(mediaCallId, promptAndRecordCommand);
        waitForCallEvent(CallPromptAndRecordTerminatedEvent.class);
        Thread.sleep(200);  // to give inbound call leg chance to die - it shouldn't

        // assert
        assertTrue("No CallCollectAndRecordTerminatedEvent event", eventVector.get(1) instanceof CallPromptAndRecordTerminatedEvent);
        assertEquals(commandId, ((CallPromptAndRecordTerminatedEvent)eventVector.get(1)).getMediaCommandId());

        assertEquals("Calling party dialog shouldn't be terminated", DialogState.Confirmed, dialogCollection.get(dialogId).getDialogState());
    }

    ////// Listener methods //////

	public void onCallConnected(CallConnectedEvent arg0) {
		eventVector.add(arg0);
		semaphore.release();
	}

	public void onCallConnectionFailed(CallConnectionFailedEvent arg0) {
		eventVector.add(arg0);
		semaphore.release();
	}

	public void onCallDisconnected(CallDisconnectedEvent arg0) {
		eventVector.add(arg0);
		semaphore.release();
	}

	public void onCallTerminated(CallTerminatedEvent arg0) {
		eventVector.add(arg0);
		semaphore.release();
	}
	
	public void onCallTerminationFailed(CallTerminationFailedEvent callTerminationFailedEvent) {
	}

	public void onCallAnnouncementCompleted(CallAnnouncementCompletedEvent announcementCompletedEvent) {
		eventVector.add(announcementCompletedEvent);
		semaphore.release();
	}

	public void onCallAnnouncementFailed(CallAnnouncementFailedEvent announcementFailedEvent) {
		eventVector.add(announcementFailedEvent);
		semaphore.release();
	}

	public void onCallPromptAndCollectDigitsCompleted(CallPromptAndCollectDigitsCompletedEvent dtmfCollectDigitsCompletedEvent) {
		eventVector.add(dtmfCollectDigitsCompletedEvent);
		semaphore.release();
	}

    public void onCallPromptAndCollectDigitsFailed(CallPromptAndCollectDigitsFailedEvent dtmfCollectDigitsFailedEvent) {
		eventVector.add(dtmfCollectDigitsFailedEvent);
		semaphore.release();
    }

	public void onCallDtmfGenerationCompleted(CallDtmfGenerationCompletedEvent dtmfGenerationCompletedEvent) {
		eventVector.add(dtmfGenerationCompletedEvent);
		semaphore.release();
	}

    public void onCallDtmfGenerationFailed(CallDtmfGenerationFailedEvent dtmfGenerationFailedEvent) {
		eventVector.add(dtmfGenerationFailedEvent);
		semaphore.release();
    }

	public void onCallAnnouncementTerminated(CallAnnouncementTerminatedEvent announcementTerminatedEvent) {
		eventVector.add(announcementTerminatedEvent);
		semaphore.release();
	}

	public void onCallPromptAndCollectDigitsTerminated(CallPromptAndCollectDigitsTerminatedEvent dtmfCollectDigitsTerminatedEvent) {
		eventVector.add(dtmfCollectDigitsTerminatedEvent);
		semaphore.release();
	}

    public void onCallPromptAndRecordCompleted(CallPromptAndRecordCompletedEvent arg0) {
        eventVector.add(arg0);
        semaphore.release();
    }

    public void onCallPromptAndRecordFailed(CallPromptAndRecordFailedEvent arg0) {
        eventVector.add(arg0);
        semaphore.release();
    }

    public void onCallPromptAndRecordTerminated(CallPromptAndRecordTerminatedEvent arg0) {
        eventVector.add(arg0);
        semaphore.release();
    }
}

