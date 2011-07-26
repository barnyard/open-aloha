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
 * Terminate conference.
 * Wait for 2 ParticipantTerminatedEvents & ConferenceEndedEvent.
 */
public class BasicConferenceScenario extends BatchTestScenarioBase implements ConferenceListener, Resetable {
    private final Log log = LogFactory.getLog(this.getClass());
	private Hashtable<String, String[]> conferenceScenarioMap = new Hashtable<String, String[]>();
	private Hashtable<String, AbstractConferenceEvent> conferenceEventMap = new Hashtable<String, AbstractConferenceEvent>();
    private Object lock = new Object();

	@Override
	protected void startScenario(String scenarioId) throws Exception {
		synchronized (lock) {
            String conferenceId = manager.selectNextConferenceBean(this).createConference();
    		updateScenario(scenarioId, SCENARIO_STARTED);
            log.info(String.format("Conference %s created for scenario %s", conferenceId, scenarioId));
            String firstParticipantId = manager.selectNextConferenceBean(this).createParticipantCallLeg(conferenceId, getTestEndpointUri());
            log.debug(String.format("First participant in conference %s is %s", conferenceId, firstParticipantId));
    		conferenceScenarioMap.put(conferenceId, new String[] {scenarioId, firstParticipantId, null});
    		manager.selectNextConferenceBean(this).inviteParticipant(conferenceId, firstParticipantId);
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
        String[] participants = null;
        synchronized (lock) {
            participants = conferenceScenarioMap.get(conferenceId);
        }
		conferenceEventMap.put(conferenceId, conferenceEndedEvent);
		if (participants != null)
			updateScenario(participants[0], "Conference Ended event received");
		if (participants != null && participants[1] == null && participants[2] == null)
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
			String secondParticipantId = manager.selectNextConferenceBean(this).createParticipantCallLeg(participantConnectedEvent.getConferenceId(), getTestEndpointUri());
			participants[2] = secondParticipantId;
			manager.selectNextConferenceBean(this).inviteParticipant(participantConnectedEvent.getConferenceId(), secondParticipantId);
		} else if (participantConnectedEvent.getDialogId().equals(participants[2])) {
			updateScenario(participants[0], "Second participant connected, ending conference");
			manager.selectNextConferenceBean(this).endConference(participantConnectedEvent.getConferenceId());
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
        String conferenceId = participantTerminatedEvent.getConferenceId();
        String[] participants = null;
        synchronized (lock) {
            participants = conferenceScenarioMap.get(conferenceId);
        }
        if (participants == null) return;
		if (participantTerminatedEvent.getDialogId().equals(participants[1])){
			updateScenario(participants[0], "First participant terminated");
			participants[1] = null;
        }
		else if (participantTerminatedEvent.getDialogId().equals(participants[2])) {
			updateScenario(participants[0], "Second participant terminated");
			participants[2] = null;
        }
		if (participants[1] == null && participants[2] == null && conferenceEventMap.get(participantTerminatedEvent.getConferenceId()) instanceof ConferenceEndedEvent) {
			updateScenario(participants[0], "Both participants terminated");
			succeed(participants[0]);
		}
	}

	public void reset() {
		conferenceScenarioMap.clear();
		conferenceEventMap.clear();
	}
}
