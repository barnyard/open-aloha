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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.call.event.CallConnectedEvent;
import com.bt.aloha.media.MediaCallBean;
import com.bt.aloha.media.MediaCallListener;
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


public class InfoScenario extends CreateCallTerminateCallScenario implements MediaCallListener {
	private final Log log = LogFactory.getLog(this.getClass());
	
	@Override
	protected void startScenario(String scenarioId) throws Exception {
		String firstDialogId = outboundCallLegBean.createCallLeg(getFromAddressUri(), getTestEndpointUri());
		MediaCallBean mediaCallBean = manager.selectNextMediaCallBean(this);
        String mediaCallId = null;
		synchronized (lock) {
			mediaCallId = mediaCallBean.createMediaCall(firstDialogId);
		    callScenarioMap.put(mediaCallId, scenarioId);
        }
		log.info(String.format("media call %s started for scenario %s", mediaCallId, scenarioId));
		updateScenario(scenarioId, SCENARIO_STARTED);
	}
	
	@Override
	public void onCallConnected(CallConnectedEvent arg0) {
		String mediaCallId = arg0.getCallId();
        String scenarioId = null;
        synchronized (lock) {
            scenarioId = callScenarioMap.get(mediaCallId);
        }
		if (scenarioId != null) {
			updateScenario(scenarioId, "Call Connected, now playing announcement");
			manager.selectNextMediaCallBean(this).playAnnouncement(mediaCallId, getAudioFileUri());
		} else {
		    log.info(String.format("no scenario found for call %s", mediaCallId));
        }
	}

	public void onCallAnnouncementCompleted(CallAnnouncementCompletedEvent arg0) {
		String mediaCallId = arg0.getCallId();
        String scenarioId = null;
        synchronized (lock) {
            scenarioId = callScenarioMap.get(mediaCallId);
        }
		if (scenarioId != null) {
			updateScenario(scenarioId, "Announcement completed, now terminating call");
			manager.selectNextMediaCallBean(this).terminateMediaCall(mediaCallId);
		} else {
		    log.info(String.format("no scenario found for call %s", mediaCallId));
        }
	}

	private void handleFailedMediaEvent(AbstractMediaCallCommandEvent arg0, String message) {
		String mediaCallId = arg0.getCallId();
        String scenarioId = null;
        synchronized (lock) {
            scenarioId = callScenarioMap.get(mediaCallId);
        }
		if (scenarioId != null) {
			updateScenario(scenarioId, message);
			fail(scenarioId, message);
		} else {
		    log.info(String.format("no scenario found for call %s", mediaCallId));
        }
	}

	public void onCallAnnouncementFailed(CallAnnouncementFailedEvent arg0) {
		handleFailedMediaEvent(arg0, "Announcement failed event received");
	}
	
	public void onCallAnnouncementTerminated(CallAnnouncementTerminatedEvent arg0) {
		handleFailedMediaEvent(arg0, "Announcement terminated event received");
	}

	public void onCallDtmfGenerationCompleted(CallDtmfGenerationCompletedEvent arg0) {
		handleFailedMediaEvent(arg0, "Dtmf generation completed event received");
	}

	public void onCallDtmfGenerationFailed(CallDtmfGenerationFailedEvent arg0) {
		handleFailedMediaEvent(arg0, "Dtmf generation failed event received");
	}

	public void onCallPromptAndCollectDigitsCompleted(CallPromptAndCollectDigitsCompletedEvent arg0) {
		handleFailedMediaEvent(arg0, "P&C completed event received");
	}

	public void onCallPromptAndCollectDigitsFailed(CallPromptAndCollectDigitsFailedEvent arg0) {
		handleFailedMediaEvent(arg0, "P&C failed event received");
	}

	public void onCallPromptAndCollectDigitsTerminated(CallPromptAndCollectDigitsTerminatedEvent arg0) {
		handleFailedMediaEvent(arg0, "P&C terminated event received");
	}

	public void onCallPromptAndRecordCompleted(CallPromptAndRecordCompletedEvent arg0) {
		handleFailedMediaEvent(arg0, "P&R completed event received");
	}

	public void onCallPromptAndRecordFailed(CallPromptAndRecordFailedEvent arg0) {
		handleFailedMediaEvent(arg0, "P&R failed event received");
	}

	public void onCallPromptAndRecordTerminated(CallPromptAndRecordTerminatedEvent arg0) {
		handleFailedMediaEvent(arg0, "P&R terminated event received");
	}
}
