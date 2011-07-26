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
import com.bt.aloha.call.CallListener;
import com.bt.aloha.call.event.AbstractCallEvent;
import com.bt.aloha.call.event.CallConnectedEvent;
import com.bt.aloha.call.event.CallConnectionFailedEvent;
import com.bt.aloha.call.event.CallDisconnectedEvent;
import com.bt.aloha.call.event.CallTerminatedEvent;
import com.bt.aloha.call.event.CallTerminationFailedEvent;
import com.bt.aloha.media.MediaCallListener;
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


/**
 * Create 2 dialogs.
 * Join dialogs 1 & 2.
 * Wait for CallConnectedEvent.
 * Create dialog 3 & join dialogs 2 & 3.
 * Create media call with dialog 1.
 * Wait for 2 CallConnectedEvents & 1 CallTerminatedEvent.
 * Play announcement to media call.
 * Wait for AnnouncementCompletedEvent.
 * Terminate both existing calls.
 * Wait for 2 CallTerminatedEvents.
 */
public class BasicCallAndMediaCallSharedDialogScenario extends BatchTestScenarioBase implements CallListener, MediaCallListener, Resetable {
	private final Log log = LogFactory.getLog(this.getClass());
	private Hashtable<String, ScenarioData> callScenarioMap = new Hashtable<String, ScenarioData>();
	private Hashtable<String, AbstractCallEvent> callConnectedMap = new Hashtable<String, AbstractCallEvent>();
    private Hashtable<String, AbstractCallEvent> callTerminatedMap = new Hashtable<String, AbstractCallEvent>();
    private Hashtable<String, AbstractCallEvent> announcementCompletedMap = new Hashtable<String, AbstractCallEvent>();
    private Object lock = new Object();

    class ScenarioData {
        private String scenarioId;
        private String firstCallId;
        private String secondCallId;
        private String mediaCallId;

        public ScenarioData(String aScenarioId, String aFirstCallId) {
            this.scenarioId = aScenarioId;
            this.firstCallId = aFirstCallId;
        }

        public ScenarioData(ScenarioData newScenarioData){
            this.scenarioId = newScenarioData.getScenarioId();
            this.firstCallId = newScenarioData.getFirstCallId();
            this.secondCallId = newScenarioData.getSecondCallId();
            this.mediaCallId = newScenarioData.getMediaCallId();
        }

        private String getMediaCallId() {
            return this.mediaCallId;
        }

        public String getScenarioId() {
            return scenarioId;
        }

        public String getSecondCallId() {
            return secondCallId;
        }

        public void setMediaCallId(String mediaCallId) {
            this.mediaCallId = mediaCallId;
        }

        public void setSecondCallId(String secondCallId) {
            this.secondCallId = secondCallId;
        }

        public String getFirstCallId() {
            return this.firstCallId;
        }

        public boolean allCallsMade() {
            return firstCallId != null && secondCallId != null && mediaCallId != null;
        }
    }

	@Override
	protected void startScenario(String scenarioId) throws Exception {
		String firstDialogId = outboundCallLegBean.createCallLeg(getFromAddressUri(), getTestEndpointUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getFromAddressUri(), getTestEndpointUri());

        String callId = null;
        synchronized (lock) {
            callId = callBean.joinCallLegs(firstDialogId, secondDialogId);
            callScenarioMap.put(callId, new ScenarioData(scenarioId, callId));
        }
        log.info(String.format("call %s started for scenario %s", callId, scenarioId));
		updateScenario(scenarioId, SCENARIO_STARTED);
	}

	public void onCallConnected(CallConnectedEvent arg0) {
		String callId = arg0.getCallId();
        callConnectedMap.put(callId, arg0);
        log.debug(String.format("call %s connectedEvent, callConnectedMap.size()=%d", callId, callConnectedMap.size()));
        ScenarioData scenarioData = null;
        synchronized (lock) {
            scenarioData = callScenarioMap.get(callId);
        }
        if (scenarioData == null) return;

		if (scenarioData.getFirstCallId() != null
            && scenarioData.getFirstCallId().equals(callId)) { // call #1 connected
			updateScenario(scenarioData.getScenarioId(), "Call Connected Event received for basic call");
			String firstDialogId = callCollection.get(callId).getFirstDialogId();
			String secondDialogId = callCollection.get(callId).getSecondDialogId();
			String thirdDialogId = outboundCallLegBean.createCallLeg(getFromAddressUri(), getTestEndpointUri());
			String callId2 = null;
            String mediaCallId = null;

            synchronized (lock) {
                callId2 = callBean.joinCallLegs(secondDialogId, thirdDialogId);
                log.info(String.format("call %s started for scenario %s", callId2, scenarioData.getScenarioId()));
                mediaCallId = mediaCallBean.createMediaCall(firstDialogId);
                log.info(String.format("media call %s started for scenario %s", mediaCallId, scenarioData.getScenarioId()));
                scenarioData.setSecondCallId(callId2);
                scenarioData.setMediaCallId(mediaCallId);
    			callScenarioMap.put(callId2, new ScenarioData(scenarioData));
    			callScenarioMap.put(mediaCallId, new ScenarioData(scenarioData));
            }

		} else if (callId.equals(scenarioData.getMediaCallId())){ // media call connected
			updateScenario(scenarioData.getScenarioId(), "Call Connected Event received for media call");
			log.info("Media call connected, playing announcement");
			mediaCallBean.playAnnouncement(callId, getAudioFileUri());
		} else if (callId.equals(scenarioData.getSecondCallId())) { // 2nd basic call connected
			log.info(String.format("2nd basic call connected: %s", scenarioData.getSecondCallId()));
			updateScenario(scenarioData.getScenarioId(), "Call Connected Event received for second basic call");
			callBean.terminateCall(scenarioData.getSecondCallId());
		}
	}

    private ScenarioData processEvent(AbstractCallEvent arg0){
        String callId = arg0.getCallId();
        ScenarioData scenarioData = null;
        synchronized (lock) {
            scenarioData = callScenarioMap.get(callId);
        }
        if (scenarioData != null)
            updateScenario(scenarioData.getScenarioId(), arg0.getClass().getSimpleName() + " received");
        return scenarioData;
    }

    private void processFailedEvent(AbstractCallEvent arg0){
        ScenarioData scenarioData = processEvent(arg0);
        if (scenarioData == null) return;
        fail(scenarioData.getScenarioId(), arg0.getClass().getSimpleName() + " for call ID " + arg0.getCallId());
    }

	public void onCallConnectionFailed(CallConnectionFailedEvent arg0) {
        processFailedEvent(arg0);
	}

	public void onCallDisconnected(CallDisconnectedEvent arg0) {
        processFailedEvent(arg0);
	}

	public void onCallTerminated(CallTerminatedEvent arg0) {
		String callId = arg0.getCallId();
        callTerminatedMap.put(callId, arg0);
        log.debug(String.format("call %s terminatedEvent, callTerminatedMap.size()=%d", callId, callTerminatedMap.size()));
        ScenarioData scenarioData = null;
        synchronized (lock) {
            scenarioData = callScenarioMap.get(callId);
        }
        if (scenarioData == null) return;
		String message = null;
		if (callId.equals(scenarioData.getFirstCallId()))
			message = String.format("Call Terminated Event received for %s, 1st basic call", callId);
		else if (callId.equals(scenarioData.getSecondCallId()))
			message = String.format("Call Terminated Event received for %s, 2nd basic call", callId);
		else if (callId.equals(scenarioData.getMediaCallId()))
			message = String.format("Call Terminated Event received for %s, media call", callId);
		log.info(message);
		updateScenario(scenarioData.getScenarioId(), message);

		if (scenarioData.allCallsMade() && allCallsTerminated(scenarioData)) {
			updateScenario(scenarioData.getScenarioId(), "All calls terminated");
			succeed(scenarioData.getScenarioId());
		}
	}

	public void onCallTerminationFailed(CallTerminationFailedEvent arg0) {
        processFailedEvent(arg0);
	}

    private boolean allCallsTerminated(ScenarioData scenarioData) {
        return callTerminatedMap.containsKey(scenarioData.getFirstCallId())
            && callTerminatedMap.containsKey(scenarioData.getSecondCallId())
            && callTerminatedMap.containsKey(scenarioData.getMediaCallId());
    }

	public void onCallAnnouncementCompleted(CallAnnouncementCompletedEvent arg0) {
		String callId = arg0.getCallId();
        announcementCompletedMap.put(callId, arg0);
        log.debug(String.format("call %s announcementCompleted, announcementCompletedMap.size()=%d", callId, announcementCompletedMap.size()));
        ScenarioData scenarioData = null;
        synchronized (lock) {
            scenarioData = callScenarioMap.get(callId);
        }
        if (scenarioData == null) return;
		updateScenario(scenarioData.getScenarioId(), "Announcement completed for media call");
		if (callId.equals(scenarioData.getMediaCallId()) // media call
                && announcementCompletedMap.containsKey(scenarioData.getMediaCallId()) ) {
			mediaCallBean.terminateMediaCall(scenarioData.getMediaCallId());
		}
	}

	public void onCallAnnouncementFailed(CallAnnouncementFailedEvent arg0) {
        processFailedEvent(arg0);
	}

	public void onCallPromptAndCollectDigitsCompleted(CallPromptAndCollectDigitsCompletedEvent arg0) {
        processEvent(arg0);
	}

	public void onCallPromptAndCollectDigitsFailed(CallPromptAndCollectDigitsFailedEvent arg0) {
        processEvent(arg0);
	}

	public void onCallDtmfGenerationCompleted(CallDtmfGenerationCompletedEvent arg0) {
        processEvent(arg0);
	}

	public void onCallDtmfGenerationFailed(CallDtmfGenerationFailedEvent arg0) {
        processEvent(arg0);
	}

	public void onCallAnnouncementTerminated(CallAnnouncementTerminatedEvent arg0) {
        processEvent(arg0);
	}

	public void onCallPromptAndCollectDigitsTerminated(CallPromptAndCollectDigitsTerminatedEvent arg0) {
        processEvent(arg0);
	}

	public void reset() {
		callScenarioMap.clear();
		callConnectedMap.clear();
	    callTerminatedMap.clear();
	    announcementCompletedMap.clear();
	}

    public void onCallPromptAndRecordCompleted(CallPromptAndRecordCompletedEvent callPromptAndRecordCompletedEvent) {
        // TODO Auto-generated method stub
    }

    public void onCallPromptAndRecordFailed(CallPromptAndRecordFailedEvent callPromptAndRecordFailedEvent) {
        // TODO Auto-generated method stub
    }

    public void onCallPromptAndRecordTerminated(CallPromptAndRecordTerminatedEvent callPromptAndRecordTerminatedEvent) {
        // TODO Auto-generated method stub
    }
}
