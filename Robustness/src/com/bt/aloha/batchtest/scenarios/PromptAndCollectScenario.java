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

import com.bt.aloha.call.event.AbstractCallEvent;
import com.bt.aloha.call.event.CallConnectedEvent;
import com.bt.aloha.call.event.CallTerminatedEvent;
import com.bt.aloha.media.DtmfCollectCommand;
import com.bt.aloha.media.event.call.CallPromptAndCollectDigitsCompletedEvent;
import com.bt.aloha.media.event.call.CallPromptAndCollectDigitsFailedEvent;


/**
 * Create 1 dialog.
 * Create media call with dialog.
 * Wait for CallConnectedEvent.
 * Prompt and collect.
 * Wait for AnnouncementCompletedEvent.
 * Wait for PromptAndCollectDigitsEvent.
 * Terminate call.
 * Wait for CallTerminatedEvent.
 */
public class PromptAndCollectScenario extends BasicPromptScenario{
    private final Log log = LogFactory.getLog(this.getClass());
	private Hashtable<String,String> callScenarioMap = new Hashtable<String,String>();
    private Object lock = new Object();


	@Override
	protected void startScenario(String scenarioId) throws Exception {
		String firstDialogId = outboundCallLegBean.createCallLeg(getFromAddressUri(), getPromptEndpointUri());
        String mediaCallId = null;
        synchronized (lock) {
            mediaCallId = mediaCallBean.createMediaCall(firstDialogId);
            callScenarioMap.put(mediaCallId, scenarioId);
        }
        log.info(String.format("media call %s started for scenario %s", mediaCallId, scenarioId));
		updateScenario(scenarioId, SCENARIO_STARTED);
	}

	public void onCallConnected(CallConnectedEvent arg0) {
        String scenarioId = processEvent(arg0);
		if (scenarioId != null) {
            DtmfCollectCommand params = new DtmfCollectCommand(getAudioFileUri(), true, true, 200, 5, 5, 1);
            mediaCallBean.promptAndCollectDigits(arg0.getCallId(), params);
        }
	}

	public void onCallTerminated(CallTerminatedEvent arg0) {
		String scenarioId = processEvent(arg0);
        if (scenarioId != null) {
			succeed(scenarioId);
		}
	}

    public void onCallPromptAndCollectDigitsCompleted(CallPromptAndCollectDigitsCompletedEvent arg0) {
		String mediaCallId = arg0.getCallId();
        String scenarioId = null;
        synchronized (lock) {
            scenarioId = callScenarioMap.get(mediaCallId);
        }
        if (scenarioId != null){
			updateScenario(scenarioId, "Call Prompt & Collect Digits Completed event received");
        	if(!"1".equals(arg0.getDigits())){
        		fail(scenarioId, "PromptAndCollect digit expected 1, but got" + arg0.getDigits());
        	} else {
    			updateScenario(scenarioId, "PromptAndCollect terminating call");
        		mediaCallBean.terminateMediaCall(mediaCallId);
        	}
        }
    }

	public void reset() {
		callScenarioMap.clear();
	}
	@Override
	protected String processEvent(AbstractCallEvent arg0) {
		String mediaCallId = arg0.getCallId();
        String scenarioId = null;
        synchronized (lock) {
            scenarioId = callScenarioMap.get(mediaCallId);
        }
        if (scenarioId != null) {
			updateScenario(scenarioId, String.format("%s received", arg0.getClass().getSimpleName()));
        }
		return scenarioId;
	}

    @Override
    public void onCallPromptAndCollectDigitsFailed(CallPromptAndCollectDigitsFailedEvent arg0) {
        String scenarioId = processEvent(arg0);
        if (null == scenarioId) return;
        updateScenario(scenarioId, String.format("PromptAndCollectDigitsFailed %s, %s - terminating call", arg0.getDtmfResult(), arg0.getDigits()));
    }
}
