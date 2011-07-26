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
package com.bt.sdk.callcontrol.demo.standup;

import java.net.URI;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.sdk.callcontrol.sip.call.CallListener;
import com.bt.sdk.callcontrol.sip.call.event.CallConnectedEvent;
import com.bt.sdk.callcontrol.sip.call.event.CallConnectionFailedEvent;
import com.bt.sdk.callcontrol.sip.call.event.CallDisconnectedEvent;
import com.bt.sdk.callcontrol.sip.call.event.CallTerminatedEvent;
import com.bt.sdk.callcontrol.sip.callleg.AutoTerminateAction;
import com.bt.sdk.callcontrol.sip.callleg.OutboundCallLegBean;
import com.bt.sdk.callcontrol.sip.callleg.event.CallLegConnectedEvent;
import com.bt.sdk.callcontrol.sip.callleg.event.CallLegConnectionFailedEvent;
import com.bt.sdk.callcontrol.sip.callleg.event.CallLegDisconnectedEvent;
import com.bt.sdk.callcontrol.sip.callleg.event.CallLegRefreshCompletedEvent;
import com.bt.sdk.callcontrol.sip.callleg.event.CallLegTerminatedEvent;
import com.bt.sdk.callcontrol.sip.callleg.event.CallLegTerminationFailedEvent;
import com.bt.sdk.callcontrol.sip.callleg.event.IncomingCallLegEvent;
import com.bt.sdk.callcontrol.sip.callleg.event.ReceivedCallLegRefreshEvent;
import com.bt.sdk.callcontrol.sip.dialog.event.IncomingAction;
import com.bt.sdk.callcontrol.sip.media.DtmfCollectCommand;
import com.bt.sdk.callcontrol.sip.media.MediaCallBean;
import com.bt.sdk.callcontrol.sip.media.MediaCallListener;
import com.bt.sdk.callcontrol.sip.media.conference.event.ConferenceActiveEvent;
import com.bt.sdk.callcontrol.sip.media.conference.event.ConferenceEndedEvent;
import com.bt.sdk.callcontrol.sip.media.conference.event.ParticipantConnectedEvent;
import com.bt.sdk.callcontrol.sip.media.conference.event.ParticipantDisconnectedEvent;
import com.bt.sdk.callcontrol.sip.media.conference.event.ParticipantFailedEvent;
import com.bt.sdk.callcontrol.sip.media.conference.event.ParticipantTerminatedEvent;
import com.bt.sdk.callcontrol.sip.media.convedia.conference.ConferenceBean;
import com.bt.sdk.callcontrol.sip.media.convedia.conference.ConferenceListener;
import com.bt.sdk.callcontrol.sip.media.event.call.CallAnnouncementCompletedEvent;
import com.bt.sdk.callcontrol.sip.media.event.call.CallAnnouncementFailedEvent;
import com.bt.sdk.callcontrol.sip.media.event.call.CallAnnouncementTerminatedEvent;
import com.bt.sdk.callcontrol.sip.media.event.call.CallDtmfGenerationCompletedEvent;
import com.bt.sdk.callcontrol.sip.media.event.call.CallDtmfGenerationFailedEvent;
import com.bt.sdk.callcontrol.sip.media.event.call.CallPromptAndCollectDigitsCompletedEvent;
import com.bt.sdk.callcontrol.sip.media.event.call.CallPromptAndCollectDigitsFailedEvent;
import com.bt.sdk.callcontrol.sip.media.event.call.CallPromptAndCollectDigitsTerminatedEvent;
import com.bt.sdk.callcontrol.sip.media.event.call.CallPromptAndRecordCompletedEvent;
import com.bt.sdk.callcontrol.sip.media.event.call.CallPromptAndRecordFailedEvent;
import com.bt.sdk.callcontrol.sip.media.event.call.CallPromptAndRecordTerminatedEvent;

public class StandupBeanImpl implements ConferenceListener, MediaCallListener, CallListener {
    private static final String MEDIA_SERVER_MOUNT_DIR = "file://mnt/172.25.58.146/audio/standupapp/";
	private static final long EVENT_WAIT_TIMEOUT = 30;
	private static final String ANNOUNCEMENT_WELCOME = MEDIA_SERVER_MOUNT_DIR + "welcome.wav";
	private static final String PROMPT_FILE = MEDIA_SERVER_MOUNT_DIR + "pressone.wav";
	private static final String ANNOUNCEMENT_BYE = MEDIA_SERVER_MOUNT_DIR + "bye.wav";
	private static final String ANNOUNCEMENT_MAKE_INBOUND_CALL = MEDIA_SERVER_MOUNT_DIR + "inbound2.wav";

	private Log log = LogFactory.getLog(this.getClass());

	private URI fromURI = URI.create("sip:standup@sdk.bt.com");
	private ConferenceBean conferenceBean;
	private OutboundCallLegBean outboundCallLegBean;
	private MediaCallBean mediaCallBean;

	private String conferenceId;
	private String inboundParticipantId;
	private Hashtable<String, String> callsAndParticipants = new Hashtable<String, String>();

    private Semaphore participantConnectedSemaphore = new Semaphore(0);
    private List<String> participantConnectedEvents = new Vector<String>();

    private Semaphore callConnectedSemaphore = new Semaphore(0);
    private List<String> callConnectedEvents = new Vector<String>();

    private Semaphore callAnnouncementCompletedSemaphore = new Semaphore(0);
    private List<String> callAnnouncementCompletedEvents = new Vector<String>();

    private ClassPathXmlApplicationContext applicationContext;

	public StandupBeanImpl() {
	}

	////////////////// LOGIC /////////////////////

	public void runStandup(List<URI> participants) {
		print(String.format("Trying to run standup with %d participants", participants.size()));
		final String confId = conferenceBean.createConference();
		conferenceId = confId;
		URI inboundParticipant = getInboundParticipantAtRandom(participants);
		for (final URI participant : participants) {
			final String participantDialogId = outboundCallLegBean
					.createCallLeg(fromURI, participant);

			Runnable participantThread = null;
			if (participant.equals(inboundParticipant)) {
				participantThread = new Runnable() {
					public void run() {
						try {
							processParticipantInformInbound(confId, participantDialogId);
						} catch (Exception e) {
							log.error(String.format(
									"Error when processing participant %s: %s",
									participant.toString(), e.getMessage()));
						}
					}
				};
			} else {
				participantThread = new Runnable() {
					public void run() {
						try {
							processParticipant(confId, participantDialogId);
						} catch (Exception e) {
							log.error(String.format(
									"Error when processing participant %s: %s",
									participant.toString(), e.getMessage()));
						}
					}
				};
			}
			new Thread(participantThread).start();
		}
	}

	private URI getInboundParticipantAtRandom(List<URI> participants) {
		int random = ((int)(100.0 * Math.random())) % participants.size();
		return participants.get(random);
	}

	private void processParticipantInformInbound(String confId, String participantDialogId) throws Exception {
		print(String.format("Processing participant %s and making him call back", participantDialogId));
		String mediaCallId = callParticipant(participantDialogId);
		playAnnouncement(mediaCallId, ANNOUNCEMENT_MAKE_INBOUND_CALL);
		mediaCallBean.terminateMediaCall(mediaCallId);
	}

	private void processParticipant(String confId, String participantDialogId) throws Exception {
		print(String.format("Processing participant %s", participantDialogId));
		String mediaCallId = callParticipant(participantDialogId);
		callsAndParticipants.put(mediaCallId, participantDialogId);
		playAnnouncement(mediaCallId, ANNOUNCEMENT_WELCOME);

		print(String.format("Prompt&Collect using prompt %s for media call %s", PROMPT_FILE, mediaCallId));
        DtmfCollectCommand dtmfCollectCommand = new DtmfCollectCommand(PROMPT_FILE, true, true, 20, 5, 5, 1);
		mediaCallBean.promptAndCollectDigits(mediaCallId, dtmfCollectCommand);
	}

	private String callParticipant(String participantDialogId) throws Exception {
		print(String.format("Calling participant %s", participantDialogId));
        String mediaCallId = mediaCallBean.createMediaCall(participantDialogId, AutoTerminateAction.False);
        waitForCallConnectedEvent(mediaCallId);
        return mediaCallId;
	}

	private void playAnnouncement(String mediaCallId, String announcementFile) throws Exception {
		print(String.format("Playing announcement %s to media call %s", announcementFile, mediaCallId));
		mediaCallBean.playAnnouncement(mediaCallId, announcementFile);
		waitForCallAnnouncementCompletedEvent(mediaCallId);
	}

	////////////////// WAITS /////////////////////

    // Conference

	public void waitForParticipantConnectedEvent(String targetDialogId) throws Exception {
        while (participantConnectedSemaphore.tryAcquire(EVENT_WAIT_TIMEOUT, TimeUnit.SECONDS))
            if (participantConnectedEvents.contains(targetDialogId)) {
            	participantConnectedEvents.remove(targetDialogId);
            	return;
            } else
            	participantConnectedSemaphore.release();
       	throw new IllegalStateException(String.format("No participantConnectedEvent for dialog %s", targetDialogId));
    }

	// Calls

    private void waitForCallConnectedEvent(String targetCallId) throws Exception {
    	while (callConnectedSemaphore.tryAcquire(EVENT_WAIT_TIMEOUT, TimeUnit.SECONDS))
    		if (callConnectedEvents.contains(targetCallId)) {
    			callConnectedEvents.remove(targetCallId);
    			return;
            } else
            	callConnectedSemaphore.release();
   		throw new IllegalStateException(String.format("No callConnectedEvent for call %s", targetCallId));
    }

    // Media calls

    private void waitForCallAnnouncementCompletedEvent(String targetCallId) throws Exception {
    	while (callAnnouncementCompletedSemaphore.tryAcquire(EVENT_WAIT_TIMEOUT, TimeUnit.SECONDS))
    		if (callAnnouncementCompletedEvents.contains(targetCallId)) {
    			callAnnouncementCompletedEvents.remove(targetCallId);
    			return;
            } else
            	callAnnouncementCompletedSemaphore.release();
    	throw new IllegalStateException(String.format("No callAnnouncementCompletedEvent for call %s", targetCallId));
    }

	////////////////// EVENTS /////////////////////

    // ConferenceListener

	public void onConferenceActive(ConferenceActiveEvent event) {
		log.debug("********************* ConferenceListener: onConferenceActive event received");
	}

	public void onConferenceEnded(ConferenceEndedEvent event) {
		log.debug("********************* ConferenceListener: onConferenceEnded event received");
		log.debug("********************* DESTROYING APPLICATION CONTEXT");
		applicationContext.destroy();
	}

	public void onParticipantConnected(ParticipantConnectedEvent event) {
        String dialogId = event.getDialogId();
        log.debug("********************* ConferenceListener: onParticipantConnected event received, dialog " + dialogId);
        participantConnectedEvents.add(dialogId);
        participantConnectedSemaphore.release();
	}

	public void onParticipantDisconnected(ParticipantDisconnectedEvent event) {
		log.debug("********************* ConferenceListener: onParticipantDisconnected event received");
	}

	public void onParticipantFailed(ParticipantFailedEvent event) {
		log.debug("********************* ConferenceListener: onParticipantFailed event received");
	}

	public void onParticipantTerminated(ParticipantTerminatedEvent event) {
		log.debug("********************* ConferenceListener: onParticipantTerminated event received");
	}

	// MediaCallListener

	public void onCallAnnouncementCompleted(CallAnnouncementCompletedEvent event) {
		String callId = event.getCallId();
		log.debug("********************* MediaCallListener: onCallAnnouncementCompleted event received, call " + callId);
		if (callId.equals(inboundParticipantId)) {
			print(String.format("Prompting and collecting to inbound media call %s", callId));
	        DtmfCollectCommand dtmfCollectCommand = new DtmfCollectCommand(PROMPT_FILE, true, true, 20, 5, 5, 1);
	        mediaCallBean.promptAndCollectDigits(callId, dtmfCollectCommand);
		} else {
			callAnnouncementCompletedEvents.add(callId);
			callAnnouncementCompletedSemaphore.release();
		}
	}

	public void onCallAnnouncementFailed(CallAnnouncementFailedEvent event) {
		log.debug("********************* MediaCallListener: onCallAnnouncementFailed event received");
	}

	public void onCallAnnouncementTerminated(CallAnnouncementTerminatedEvent event) {
		log.debug("********************* MediaCallListener: onCallAnnouncementTerminated event received");
	}

	public void onCallDtmfGenerationCompleted(CallDtmfGenerationCompletedEvent event) {
		log.debug("********************* MediaCallListener: onCallDtmfGenerationCompleted event received");
	}

	public void onCallDtmfGenerationFailed(CallDtmfGenerationFailedEvent event) {
		log.debug("********************* MediaCallListener: onCallDtmfGenerationFailed event received");
	}

	public void onCallPromptAndCollectDigitsCompleted(CallPromptAndCollectDigitsCompletedEvent event) {
		String mediaCommandId = event.getMediaCommandId();
		log.debug("********************* MediaCallListener: onCallPromptAndCollectDigitsCompleted event received, mediaCommandId " + mediaCommandId + " digits " + event.getDigits());
		if (event.getDigits().equals("1"))
			conferenceBean.inviteParticipant(conferenceId, callsAndParticipants.get(event.getCallId()));
		else
			mediaCallBean.playAnnouncement(event.getCallId(), ANNOUNCEMENT_BYE);
	}

	public void onCallPromptAndCollectDigitsFailed(CallPromptAndCollectDigitsFailedEvent event) {
		log.debug("********************* MediaCallListener: onCallPromptAndCollectDigitsFailed event received");
	}

	public void onCallPromptAndCollectDigitsTerminated(CallPromptAndCollectDigitsTerminatedEvent event) {
		log.debug("********************* MediaCallListener: onCallPromptAndCollectDigitsTerminated event received");
	}

	// CallListener

	public void onCallConnected(CallConnectedEvent event) {
		String callId = event.getCallId();
		log.debug("********************* CallListener: onCallConnected event received, call " + callId);
		if (callId.equals(inboundParticipantId)) {
			log.info("Playing welcome announcement to inbound participant");
			mediaCallBean.playAnnouncement(inboundParticipantId, ANNOUNCEMENT_WELCOME);
		} else {
	        callConnectedEvents.add(callId);
	        callConnectedSemaphore.release();
		}
	}

	public void onCallConnectionFailed(CallConnectionFailedEvent event) {
		log.debug("********************* CallListener: onCallConnectionFailed event received");
	}

	public void onCallDisconnected(CallDisconnectedEvent event) {
		log.debug("********************* CallListener: onCallDisconnected event received");
	}

	public void onCallTerminated(CallTerminatedEvent event) {
		log.debug("********************* CallListener: onCallTerminated event received");
	}

	////////////////// SETTERS /////////////////////

	public void setConferenceBean(ConferenceBean conferenceBean) {
		this.conferenceBean = conferenceBean;
	}

	public void setOutboundCallLegBean(OutboundCallLegBean outboundCallLegBean) {
		this.outboundCallLegBean = outboundCallLegBean;
	}

	public void setMediaCallBean(MediaCallBean mediaCallBean) {
		this.mediaCallBean = mediaCallBean;
	}

	////////////////// HELPERS /////////////////////

	private void print(String message) {
		log.debug("===================================================================");
		log.debug(message);
		log.debug("===================================================================");
	}

	public void setApplicationContext(ClassPathXmlApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public void onIncomingCallLeg(IncomingCallLegEvent arg0) {
		arg0.setIncomingCallAction(IncomingAction.None);
		inboundParticipantId = mediaCallBean.createMediaCall(arg0.getId());
		callsAndParticipants.put(inboundParticipantId, arg0.getId());
	}

	public void onCallLegConnected(CallLegConnectedEvent arg0) {
	}

	public void onCallLegConnectionFailed(CallLegConnectionFailedEvent arg0) {
	}

	public void onCallLegDisconnected(CallLegDisconnectedEvent arg0) {
	}

	public void onCallLegRefreshCompleted(CallLegRefreshCompletedEvent arg0) {
	}

	public void onCallLegTerminated(CallLegTerminatedEvent arg0) {
	}

	public void onCallLegTerminationFailed(CallLegTerminationFailedEvent arg0) {
	}

	public void onReceivedCallLegRefresh(ReceivedCallLegRefreshEvent arg0) {
	}

    public void onCallPromptAndRecordCompleted(CallPromptAndRecordCompletedEvent callPromptAndRecordCompletedEvent) {
    }

    public void onCallPromptAndRecordFailed(CallPromptAndRecordFailedEvent callPromptAndRecordFailedEvent) {
    }

    public void onCallPromptAndRecordTerminated(CallPromptAndRecordTerminatedEvent callPromptAndRecordTerminatedEvent) {
    }
}
