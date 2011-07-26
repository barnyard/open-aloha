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

 	

 	
 	
 
package com.bt.aloha.batchtest.scenarios;

import java.util.Hashtable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.batchtest.BatchTestScenarioBase;
import com.bt.aloha.batchtest.Resetable;
import com.bt.aloha.media.conference.event.AbstractConferenceEvent;
import com.bt.aloha.media.conference.event.ConferenceActiveEvent;
import com.bt.aloha.media.conference.event.ConferenceEndedEvent;
import com.bt.aloha.media.conference.event.ParticipantConnectedEvent;
import com.bt.aloha.media.conference.event.ParticipantDisconnectedEvent;
import com.bt.aloha.media.conference.event.ParticipantFailedEvent;
import com.bt.aloha.media.conference.event.ParticipantTerminatedEvent;
import com.bt.aloha.media.convedia.conference.ConferenceListener;

/**
 * Create Conference.
 * Create 1 dialog.
 * Invite participant into conference.
 * Wait for ParticipantConnectedEvent & ConferenceActiveEvent.
 * Create 2nd dialog and invite 2nd participant.
 * Wait for ParticipantConnectedEvent.
 * Disconnect 1st participant.
 * Wait for ParticipantTerminatedEvent.
 * Disconnect 2nd participant.
 * Wait for ParticipantTerminatedEvent & ConferenceEndedEvent.
 */
public class ConferenceScenarioTerminateParticipants extends BatchTestScenarioBase implements ConferenceListener, Resetable {
    private final Log log = LogFactory.getLog(this.getClass());
	private Hashtable<String, String[]> conferenceScenarioMap = new Hashtable<String, String[]>();
	private Hashtable<String, AbstractConferenceEvent> conferenceEventMap = new Hashtable<String, AbstractConferenceEvent>();
    private Object lock = new Object();

	@Override
	protected void startScenario(String scenarioId) throws Exception {
        synchronized (lock) {
    		String conferenceId = conferenceBean.createConference();
    		updateScenario(scenarioId, SCENARIO_STARTED);
            log.info(String.format("Conference %s created for scenario %s", conferenceId, scenarioId));
            String firstParticipantId = conferenceBean.createParticipantCallLeg(conferenceId, getTestEndpointUri());
            log.debug(String.format("First participant in conference %s is %s", conferenceId, firstParticipantId));
    		conferenceScenarioMap.put(conferenceId, new String[] {scenarioId, firstParticipantId, null});
    		conferenceBean.inviteParticipant(conferenceId, firstParticipantId);
    		updateScenario(scenarioId, "First participant invited");
        }
	}

	public void onConferenceActive(ConferenceActiveEvent conferenceActiveEvent) {
		String conferenceId = conferenceActiveEvent.getConferenceId();
		conferenceEventMap.put(conferenceId, conferenceActiveEvent);
        String[] participantIds = null;
        synchronized (lock) {
            participantIds = conferenceScenarioMap.get(conferenceId);
        }
		if (participantIds != null)
			updateScenario(participantIds[0], "Conference Active event received");
	}

	public void onConferenceEnded(ConferenceEndedEvent conferenceEndedEvent) {
        String conferenceId = conferenceEndedEvent.getConferenceId();
		conferenceEventMap.put(conferenceId, conferenceEndedEvent);
        String[] participants = null;
        synchronized (lock) {
            participants = conferenceScenarioMap.get(conferenceId);
        }
        if (participants == null) return;
        updateScenario(participants[0], "Conference Ended event received");
		if (participants[1] == null && participants[2] == null)
			succeed(participants[0]);
	}

	public void onParticipantConnected(ParticipantConnectedEvent participantConnectedEvent) {
		log.debug(String.format("Participant %s connected in conference %s", participantConnectedEvent.getDialogId(), participantConnectedEvent.getConferenceId()));
        String conferenceId = participantConnectedEvent.getConferenceId();
        String[] participants = null;
        synchronized (lock) {
            participants = conferenceScenarioMap.get(conferenceId);
        }
        if (participants == null) return;
		if (participantConnectedEvent.getDialogId().equals(participants[1])) {
			updateScenario(participants[0], "First participant connected, inviting second participant");
			String secondParticipantId = conferenceBean.createParticipantCallLeg(conferenceId, getTestEndpointUri());
			participants[2] = secondParticipantId;
			conferenceBean.inviteParticipant(conferenceId, secondParticipantId);
		} else if (participantConnectedEvent.getDialogId().equals(participants[2])) {
			updateScenario(participants[0], "Second participant connected, terminating both participants");
			conferenceBean.terminateParticipant(conferenceId, participants[1]);
			conferenceBean.terminateParticipant(conferenceId, participants[2]);
		}
	}

	public void onParticipantDisconnected(ParticipantDisconnectedEvent participantDisconnectedEvent) {
        String conferenceId = participantDisconnectedEvent.getConferenceId();
        String[] participants = null;
        synchronized (lock) {
            participants = conferenceScenarioMap.get(conferenceId);
        }
		if (participants != null)
			updateScenario(participants[0], "Participant Disconnected event received");
	}

	public void onParticipantFailed(ParticipantFailedEvent participantFailedEvent) {
        String conferenceId = participantFailedEvent.getConferenceId();
        String[] participants = null;
        synchronized (lock) {
            participants = conferenceScenarioMap.get(conferenceId);
        }
		if (participants != null)
			updateScenario(participants[0], "Participant Failed event received");
	}

	public void onParticipantTerminated(ParticipantTerminatedEvent participantTerminatedEvent) {
		log.debug(String.format("Got participantTerminatedEvent for conf %s, dialog id %s", participantTerminatedEvent.getConferenceId(), participantTerminatedEvent.getDialogId()));
        String conferenceId = participantTerminatedEvent.getConferenceId();
        String[] participants = null;
        synchronized (lock) {
            participants = conferenceScenarioMap.get(conferenceId);
        }
        if (participants == null)
            return;
		if (participantTerminatedEvent.getDialogId().equals(participants[1])) {
			updateScenario(participants[0], "First participant terminated");
			participants[1] = null;
        }
		else if (participantTerminatedEvent.getDialogId().equals(participants[2])) {
			updateScenario(participants[0], "Second participant terminated");
			participants[2] = null;
        }

		if (participants[1] == null && participants[2] == null && conferenceEventMap.get(conferenceId) instanceof ConferenceEndedEvent) {
			updateScenario(participants[0], "Both participants terminated");
			succeed(participants[0]);
		}
	}

	public void reset() {
		conferenceEventMap.clear();
		conferenceScenarioMap.clear();
	}
}
