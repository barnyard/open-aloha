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
import java.util.concurrent.CountDownLatch;

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
import com.bt.aloha.callleg.AutoTerminateAction;


/**
 * Create 2 dialogs.
 * Join dialogs 1 & 2.
 * Wait for CallConnectedEvent.
 * Create dialog 3 & join dialogs 2 & 3.
 * Wait for 1 CallConnectedEvent & 1 CallTerminatedEvent.
 * Terminate the second call.
 * Wait for 1 CallTerminatedEvent.
 */
public class TwoCallsSharingCallLegScenario extends BatchTestScenarioBase implements CallListener, Resetable {
    private final Log log = LogFactory.getLog(this.getClass());
	private Hashtable<String, ScenarioData> callScenarioMap = new Hashtable<String, ScenarioData>();
	private Hashtable<String, AbstractCallEvent> callEventMap = new Hashtable<String, AbstractCallEvent>();
    private Hashtable<String, CountDownLatch> latchMap = new Hashtable<String, CountDownLatch>();

    public class ScenarioData {
        private String scenarioId;
        private String firstCallId;
        private String secondCallId;

        public ScenarioData(String scenarioId, String firstCallId) {
            this.scenarioId = scenarioId;
            this.firstCallId = firstCallId;
        }
        public String getFirstCallId() {
            return firstCallId;
        }
        public void setFirstCallId(String firstCallId) {
            this.firstCallId = firstCallId;
        }
        public String getScenarioId() {
            return scenarioId;
        }
        public void setScenarioId(String scenarioId) {
            this.scenarioId = scenarioId;
        }
        public String getSecondCallId() {
            return secondCallId;
        }
        public void setSecondCallId(String secondCallId) {
            this.secondCallId = secondCallId;
        }

        @Override
        public String toString() {
            return String.format("%s: scenarioId: %s, 1st callId: %s, 2nd callId: %s", this.getClass().getSimpleName(), this.scenarioId, this.firstCallId, this.secondCallId);
        }
    }

	@Override
	protected void startScenario(String scenarioId) throws Exception {
	    updateScenario(scenarioId, SCENARIO_STARTED);

        String firstDialogId = outboundCallLegBean.createCallLeg(getFromAddressUri(), getTestEndpointUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getFromAddressUri(), getTestEndpointUri());

        latchMap.put(scenarioId, new CountDownLatch(1));
		String callId = callBean.joinCallLegs(firstDialogId, secondDialogId, AutoTerminateAction.True);
		callScenarioMap.put(callId, new ScenarioData(scenarioId, callId));
        latchMap.get(scenarioId).countDown();
        updateScenario(scenarioId, "1st call initiated: " + callId);
	}

	

	public void onCallConnected(CallConnectedEvent callConnectedEvent) {
		String eventCallId = callConnectedEvent.getCallId();
        ScenarioData scenarioData = waitForScenarioData(eventCallId, callScenarioMap);
        if (scenarioData == null) {
            return;
        }
        log.info(scenarioData.toString());
        String scenarioId = scenarioData.getScenarioId();
        try {
            latchMap.get(scenarioId).await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        callEventMap.put(eventCallId, callConnectedEvent);

        if (eventCallId.equals(scenarioData.getFirstCallId())) {
			updateScenario(scenarioId, "Call Connected Event received for 1st call");
			String secondDialogId = callCollection.get(eventCallId).getSecondDialogId();
			String thirdDialogId = outboundCallLegBean.createCallLeg(getFromAddressUri(), getTestEndpointUri());
            latchMap.put(scenarioId, new CountDownLatch(1));
            String callId2 = callBean.joinCallLegs(secondDialogId, thirdDialogId);
            scenarioData.setSecondCallId(callId2);
            callScenarioMap.put(callId2, scenarioData);
            latchMap.get(scenarioId).countDown();
            updateScenario(scenarioId, "2nd call initiated: " + callId2);
		}

        if (eventCallId.equals(scenarioData.getSecondCallId())) {
			updateScenario(scenarioId, "Call Connected Event received for 2nd call, terminating");
			callBean.terminateCall(eventCallId);
		}
	}

	public void onCallConnectionFailed(CallConnectionFailedEvent arg0) {
		String eventCallId = arg0.getCallId();
		ScenarioData scenarioData = waitForScenarioData(eventCallId, callScenarioMap);
        if (scenarioData == null) {
            return;
        }
        String scenarioId = scenarioData.getScenarioId();
        try {
            latchMap.get(scenarioId).await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
		callEventMap.put(eventCallId, arg0);
        if (callScenarioMap.containsKey(eventCallId)) {
			updateScenario(scenarioId, "Call Connection Failed Event received");
            fail(scenarioId, "Call connection failed for call ID " + eventCallId);
        }
	}

	public void onCallDisconnected(CallDisconnectedEvent arg0) {
		String eventCallId = arg0.getCallId();
		ScenarioData scenarioData = waitForScenarioData(eventCallId, callScenarioMap);
        if (scenarioData == null) {
            return;
        }
        String scenarioId = scenarioData.getScenarioId();
        try {
            latchMap.get(scenarioId).await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
		callEventMap.put(eventCallId, arg0);
        if (callScenarioMap.containsKey(eventCallId)) {
			updateScenario(scenarioId, "Call Disconnected Event received");
            fail(scenarioId, "Call disconnected for call ID " + eventCallId);
        }
	}

	public void onCallTerminated(CallTerminatedEvent callTerminatedEvent) {
        String eventCallId = callTerminatedEvent.getCallId();
        ScenarioData scenarioData = waitForScenarioData(eventCallId, callScenarioMap);
        if (scenarioData == null) {
            return;
        }
        String scenarioId = scenarioData.getScenarioId();
        try {
            latchMap.get(scenarioId).await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
		callEventMap.put(eventCallId, callTerminatedEvent);
		String firstCallId = scenarioData.getFirstCallId();
		String secondCallId = scenarioData.getSecondCallId();

		String message = null;
		if (eventCallId.equals(firstCallId))
			message = String.format("Call Terminated Event received for %s, 1st call", firstCallId);
		else if (eventCallId.equals(secondCallId))
			message = String.format("Call Terminated Event received for %s, 2nd call", secondCallId);
		updateScenario(scenarioId, message);

        if (firstCallId == null) return;
        if (secondCallId == null) return;

        if (callEventMap.get(secondCallId) != null && callEventMap.get(secondCallId) instanceof CallTerminatedEvent
		 && callEventMap.get(firstCallId)  != null && callEventMap.get(firstCallId)  instanceof CallTerminatedEvent) {
			updateScenario(scenarioId, "Both calls terminated");
			succeed(scenarioId);
		}
	}

	public void onCallTerminationFailed(CallTerminationFailedEvent arg0) {
		String eventCallId = arg0.getCallId();
		ScenarioData scenarioData = waitForScenarioData(eventCallId, callScenarioMap);
        if (scenarioData == null) {
            return;
        }
        String scenarioId = scenarioData.getScenarioId();
        try {
            latchMap.get(scenarioId).await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
		callEventMap.put(eventCallId, arg0);
        if (callScenarioMap.containsKey(eventCallId)) {
			updateScenario(scenarioId, "Call Termination Failed Event received");
            fail(scenarioId, "Call termination failed for call ID " + eventCallId);
        }
	}

	public void reset() {
		callScenarioMap.clear();
		callEventMap.clear();
        latchMap.clear();
	}
}
