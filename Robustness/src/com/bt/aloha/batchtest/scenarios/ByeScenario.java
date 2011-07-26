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

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.call.event.CallConnectedEvent;
import com.bt.aloha.call.event.CallDisconnectedEvent;
import com.bt.aloha.call.event.CallTerminatedEvent;
import com.bt.aloha.callleg.AutoTerminateAction;

public class ByeScenario extends CreateCallTerminateCallScenario {

	private final Log log = LogFactory.getLog(this.getClass());
	private Object lock = new Object(); 
	private final static String CALL_TERMINATED_EVENT_RECEIVED = "Call terminated event recieved";
	private String testEndpoint2;

	@Override
	protected void startScenario(String scenarioId) throws Exception {
		String firstDialogId = outboundCallLegBean.createCallLeg(getFromAddressUri(), getTestEndpointUri());
		String secondDialogId = outboundCallLegBean.createCallLeg(getFromAddressUri(), URI.create(this.testEndpoint2));
        String callId = null;
		synchronized (lock) {
			callId = manager.selectNextCallBean(this).joinCallLegs(firstDialogId, secondDialogId, AutoTerminateAction.True);
		    callScenarioMap.put(callId, scenarioId);
        }
		log.info(String.format("call %s started for scenario %s", callId, scenarioId));
		updateScenario(scenarioId, SCENARIO_STARTED);
	}
	
	@Override
	public void onCallConnected(CallConnectedEvent arg0) {

		String callId = arg0.getCallId();		
        String scenarioId = null;
        synchronized (lock) {
            scenarioId = callScenarioMap.get(callId);
        }
		if (scenarioId != null) {
			updateScenario(scenarioId, "Call Connected, now waiting for phone to hang up");
		} else {
		    log.info(String.format("no scenario found for call %s", callId));
        }
	}

	@Override
	public void onCallDisconnected(CallDisconnectedEvent arg0) {
		String callId = arg0.getCallId();
        String scenarioId = null;
        synchronized (lock) {
            scenarioId = callScenarioMap.get(callId);
        }
        if (scenarioId != null) {
			updateScenario(scenarioId, CALL_DISCONNECTED_EVENT_RECEIVED);
			succeed(scenarioId);
        } else {
            log.info(String.format("no scenario found for call %s", callId));
        }
	}

	@Override
	public void onCallTerminated(CallTerminatedEvent arg0) {
		String callId = arg0.getCallId();
        String scenarioId = null;
        synchronized (lock) {
            scenarioId = callScenarioMap.get(callId);
        }
        if (scenarioId != null) {
			updateScenario(scenarioId, CALL_TERMINATED_EVENT_RECEIVED);
			fail(scenarioId, CALL_TERMINATED_EVENT_RECEIVED);
		} else {
            log.info(String.format("no scenario found for call %s", callId));
        }
	}

	public String getTestEndpoint2() {
		return testEndpoint2;
	}

	public void setTestEndpoint2(String testEndpoint2) {
		this.testEndpoint2 = testEndpoint2;
	}
	
}
