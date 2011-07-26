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
 * Create 1 dialog.
 * Create media call with dialog.
 * Wait for CallConnectedEvent.
 * Play announcement to call.
 * Wait for AnnouncementCompletedEvent.
 * Terminate call.
 * Wait for CallTerminatedEvent.
 */
public class AnnouncementCallScenario extends BatchTestScenarioBase implements CallListener, MediaCallListener, Resetable {
    private final Log log = LogFactory.getLog(this.getClass());
	private Hashtable<String,String> callScenarioMap = new Hashtable<String,String>();
    private Object lock = new Object();

	@Override
	protected void startScenario(String scenarioId) throws Exception {
		String firstDialogId = outboundCallLegBean.createCallLeg(getFromAddressUri(), getTestEndpointUri());

        String mediaCallId = null;
        synchronized (lock) {
            mediaCallId = mediaCallBean.createMediaCall(firstDialogId);
            callScenarioMap.put(mediaCallId, scenarioId);
        }
        log.info(String.format("media call %s started for scenario %s", mediaCallId, scenarioId));
		updateScenario(scenarioId, SCENARIO_STARTED);
	}

	public void onCallConnected(CallConnectedEvent arg0) {
		String mediaCallId = arg0.getCallId();
        String scenarioId = null;
        synchronized (lock) {
            scenarioId = callScenarioMap.get(mediaCallId);
        }
		if (scenarioId != null) {
			updateScenario(scenarioId, "Call Connected event received, playing announcement");
			mediaCallBean.playAnnouncement(mediaCallId, getAudioFileUri());
        }
	}

	public void onCallConnectionFailed(CallConnectionFailedEvent arg0) {
		String mediaCallId = arg0.getCallId();
        String scenarioId = null;
        synchronized (lock) {
            scenarioId = callScenarioMap.get(mediaCallId);
        }
        if (scenarioId != null)
			updateScenario(scenarioId, "Call Connection Failed event received");
	}

	public void onCallDisconnected(CallDisconnectedEvent arg0) {
		String mediaCallId = arg0.getCallId();
        String scenarioId = null;
        synchronized (lock) {
            scenarioId = callScenarioMap.get(mediaCallId);
        }
        if (scenarioId != null)
            updateScenario(scenarioId, "Call Disconnected event received");
	}

	public void onCallTerminated(CallTerminatedEvent arg0) {
		String mediaCallId = arg0.getCallId();
        String scenarioId = null;
        synchronized (lock) {
            scenarioId = callScenarioMap.get(mediaCallId);
        }
        if (scenarioId != null) {
			updateScenario(scenarioId, "Call Terminated event received");
			succeed(scenarioId);
		}
	}

	public void onCallTerminationFailed(CallTerminationFailedEvent arg0) {
		String mediaCallId = arg0.getCallId();
        String scenarioId = null;
        synchronized (lock) {
            scenarioId = callScenarioMap.get(mediaCallId);
        }
        if (scenarioId != null)
            updateScenario(scenarioId, "Call Termination Failed event received");
	}

	public void onCallAnnouncementCompleted(CallAnnouncementCompletedEvent arg0) {
		String mediaCallId = arg0.getCallId();
        String scenarioId = null;
        synchronized (lock) {
            scenarioId = callScenarioMap.get(mediaCallId);
        }
        if (scenarioId != null) {
			updateScenario(scenarioId, "Call Announcement Completed event received, terminating media call");
			mediaCallBean.terminateMediaCall(mediaCallId);
        }
	}

	public void onCallAnnouncementFailed(CallAnnouncementFailedEvent arg0) {
		String mediaCallId = arg0.getCallId();
        String scenarioId = null;
        synchronized (lock) {
            scenarioId = callScenarioMap.get(mediaCallId);
        }
        if (scenarioId != null)
			updateScenario(scenarioId, "Call Announcement Failed event received");
	}

    public void onCallPromptAndCollectDigitsCompleted(CallPromptAndCollectDigitsCompletedEvent arg0) {
		String mediaCallId = arg0.getCallId();
        String scenarioId = null;
        synchronized (lock) {
            scenarioId = callScenarioMap.get(mediaCallId);
        }
        if (scenarioId != null)
			updateScenario(scenarioId, "Call Prompt & Collect Digits Completed event received");
    }

    public void onCallPromptAndCollectDigitsFailed(CallPromptAndCollectDigitsFailedEvent arg0) {
		String mediaCallId = arg0.getCallId();
        String scenarioId = null;
        synchronized (lock) {
            scenarioId = callScenarioMap.get(mediaCallId);
        }
        if (scenarioId != null)
			updateScenario(scenarioId, "Call Prompt & Collect Digits Failed event received");
    }

	public void onCallDtmfGenerationCompleted(CallDtmfGenerationCompletedEvent arg0) {
		String mediaCallId = arg0.getCallId();
        String scenarioId = null;
        synchronized (lock) {
            scenarioId = callScenarioMap.get(mediaCallId);
        }
        if (scenarioId != null)
			updateScenario(scenarioId, "Call Dtmf Generation Completed event received");
	}

	public void onCallDtmfGenerationFailed(CallDtmfGenerationFailedEvent arg0) {
		String mediaCallId = arg0.getCallId();
        String scenarioId = null;
        synchronized (lock) {
            scenarioId = callScenarioMap.get(mediaCallId);
        }
        if (scenarioId != null)
			updateScenario(scenarioId, "Call Dtmf Generation Failed event received");
	}

	public void onCallAnnouncementTerminated(CallAnnouncementTerminatedEvent arg0) {
		String mediaCallId = arg0.getCallId();
        String scenarioId = null;
        synchronized (lock) {
            scenarioId = callScenarioMap.get(mediaCallId);
        }
        if (scenarioId != null)
			updateScenario(scenarioId, "Call Announcement Terminated event received");
	}

	public void onCallPromptAndCollectDigitsTerminated(CallPromptAndCollectDigitsTerminatedEvent arg0) {
		String mediaCallId = arg0.getCallId();
        String scenarioId = null;
        synchronized (lock) {
            scenarioId = callScenarioMap.get(mediaCallId);
        }
        if (scenarioId != null)
			updateScenario(scenarioId, "Call Prompt & Collect Digits Terminated event received");
	}

	public void reset() {
		callScenarioMap.clear();
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
