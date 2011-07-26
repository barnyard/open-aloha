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

import com.bt.aloha.call.event.AbstractCallEvent;
import com.bt.aloha.call.event.CallConnectedEvent;
import com.bt.aloha.call.event.CallTerminatedEvent;
import com.bt.aloha.media.PromptAndRecordCommand;
import com.bt.aloha.media.event.call.CallAnnouncementCompletedEvent;
import com.bt.aloha.media.event.call.CallPromptAndRecordCompletedEvent;


/**
 * Create 1 dialog.
 * Create media call with dialog.
 * Wait for CallConnectedEvent.
 * Prompt and record.
 * Wait for AnnouncementCompletedEvent.
 * Wait for PromptAndRecordCompletedEvent.
 * Play the recording via playAnnouncement
 * Wait for AnnouncementCompletedEvent.
 * Terminate call.
 * Wait for CallTerminatedEvent.
 */
public class PromptAndRecordScenario extends BasicPromptScenario{
    //private static final String BASE_PATH = "file://mnt/172.25.19.54/uros/clips";
//    private static final String BASE_PATH = "file://mnt/172.25.58.146/audio/robustness";
    private Hashtable<String,ScenarioData> callScenarioMap = new Hashtable<String,ScenarioData>();
    private Object lock = new Object();

    private String basePath;
    
    private static class ScenarioData {
    	String scenarioId;
    	String recordCommandId;
    	String announcementCommandId;
		public ScenarioData(String scenarioId) {
			this.scenarioId = scenarioId;
		}
    }

	@Override
	protected void startScenario(String scenarioId) throws Exception {
		String firstDialogId = outboundCallLegBean.createCallLeg(getFromAddressUri(), getPromptEndpointUri());
        String mediaCallId = null;
        synchronized (lock) {
            mediaCallId = mediaCallBean.createMediaCall(firstDialogId);
            callScenarioMap.put(mediaCallId, new ScenarioData(scenarioId));
        }
        log.info(String.format("media call %s started for scenario %s", mediaCallId, scenarioId));
		updateScenario(scenarioId, SCENARIO_STARTED);
	}

	public void onCallConnected(CallConnectedEvent arg0) {
		ScenarioData scenarioData = processEvent(arg0);
		if (null == scenarioData) return;
		updateScenario(scenarioData.scenarioId, "Call Connected event received, promptAndRecording");
		String outputFilename = generateOutputFilename(scenarioData.scenarioId);
        PromptAndRecordCommand command =
                new PromptAndRecordCommand(getAudioFileUri(), true, outputFilename, false, "audio/wav", 5, 200, 1, null);
        scenarioData.recordCommandId = mediaCallBean.promptAndRecord(arg0.getCallId(), command);
	}

	private String generateOutputFilename(String scenarioId) {
		if(null==getBasePath())
			throw new IllegalStateException("No base path set where to store the recorded clip");
		return getBasePath() + "/" + scenarioId.replaceAll(":", "_") + ".wav";
	}

	protected ScenarioData processEvent(AbstractCallEvent arg0) {
		String callId = arg0.getCallId();
		ScenarioData scenarioData = null;
		synchronized (lock) {
			scenarioData = callScenarioMap.get(callId);
		}
		if (scenarioData != null){
			updateScenario(scenarioData.scenarioId, String.format("%s received", arg0.getClass().getSimpleName()));
		}
		return scenarioData;
	}

	public void onCallTerminated(CallTerminatedEvent arg0) {
		ScenarioData scenarioData = processEvent(arg0);
		if (null != scenarioData)
			succeed(scenarioData.scenarioId);
	}

	public void onCallAnnouncementCompleted(CallAnnouncementCompletedEvent arg0) {
		ScenarioData scenarioData = processEvent(arg0);
		if (null == scenarioData) return;
		if (arg0.getMediaCommandId().equals(scenarioData.announcementCommandId)) {
			updateScenario(scenarioData.scenarioId, "playback of recording completed - terminating call");
    		mediaCallBean.terminateMediaCall(arg0.getCallId());
		}
		if (arg0.getMediaCommandId().equals(scenarioData.recordCommandId)) {
			updateScenario(scenarioData.scenarioId, "recording announcement completed");
		}
	}

    public void onCallPromptAndRecordCompleted(CallPromptAndRecordCompletedEvent arg0) {
		ScenarioData scenarioData = processEvent(arg0);
		if (null == scenarioData) return;
		if (arg0.getMediaCommandId().equals(scenarioData.recordCommandId)) {
    		updateScenario(scenarioData.scenarioId, "Prompt & Record Completed event received, replaying recording");
    		sleep(1000);
    		scenarioData.announcementCommandId = mediaCallBean.playAnnouncement(arg0.getCallId(), this.generateOutputFilename(scenarioData.scenarioId));
    	}
    }

    public void reset() {
    	callScenarioMap.clear();
    }

	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}
}
