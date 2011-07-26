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

 	

 	
 	
 
package com.bt.aloha.batchtest.v2.scenarios;

import java.net.URI;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.aloha.batchtest.v2.BaseScenario;
import com.bt.aloha.batchtest.v2.ResultLogger;
import com.bt.aloha.batchtest.v2.ScenarioRunResult;
import com.bt.aloha.batchtest.v2.ScenarioRunResultListenerImpl;
import com.bt.aloha.batchtest.v2.StackManagerSyncronizationSemaphore;
import com.bt.aloha.batchtest.v2.StackManagerSyncronizationSemaphoreImpl;
import com.bt.aloha.callleg.OutboundCallLegBean;
import com.bt.aloha.callleg.OutboundCallLegListener;
import com.bt.aloha.callleg.event.AbstractCallLegEvent;
import com.bt.aloha.callleg.event.CallLegAlertingEvent;
import com.bt.aloha.callleg.event.CallLegConnectedEvent;
import com.bt.aloha.callleg.event.CallLegConnectionFailedEvent;
import com.bt.aloha.callleg.event.CallLegDisconnectedEvent;
import com.bt.aloha.callleg.event.CallLegRefreshCompletedEvent;
import com.bt.aloha.callleg.event.CallLegTerminatedEvent;
import com.bt.aloha.callleg.event.CallLegTerminationFailedEvent;
import com.bt.aloha.callleg.event.ReceivedCallLegRefreshEvent;
import com.bt.aloha.dialog.state.TerminationCause;

/**
 * This scenario assumes that busy mockphone is always being called
 */
public class InboundCallScenario extends BaseScenario implements OutboundCallLegListener {
	private static Log LOG = LogFactory.getLog(InboundCallScenario.class);
	private ApplicationContext applicationContext;
	private OutboundCallLegBean outboundCallLegBean;
	private Map<String, Semaphore> callLegIdSemaphores = new Hashtable<String, Semaphore>();
	private Map<String, String> callLegIdScenarioIds = new Hashtable<String, String>();
	private Object lock = new Object();

	public void run(String scenarioId) {
		LOG.debug("starting scenario: " + scenarioId);
		acquireSyncSemaphore();
		String callLegId;
		synchronized (lock) {
			callLegId = this.outboundCallLegBean.createCallLeg(URI.create("sip:inboundcallscenario@robustness.com"), URI.create(getTestEndpoint()));
			callLegIdSemaphores.put(callLegId, new Semaphore(0));
			callLegIdScenarioIds.put(callLegId, scenarioId);
		}
		ScenarioRunResult scenarioRunResult = new ScenarioRunResult(scenarioId, this.getName());
		scenarioRunResult.setMessage("Just started");
		connectCallLegAndWaitForCallLegConnectionFailedEvent(callLegId);
	}

	private void connectCallLegAndWaitForCallLegConnectionFailedEvent(String callLegId) {
		ScenarioRunResult scenarioRunResult = new ScenarioRunResult(callLegIdScenarioIds.get(callLegId), this.getName());
		try {
			this.outboundCallLegBean.connectCallLeg(callLegId);
			if (callLegIdSemaphores.get(callLegId).tryAcquire(20, TimeUnit.SECONDS)) {
				callLegIdSemaphores.remove(callLegId);
				scenarioRunResult.setResult(true, "OK");
			} else {
				scenarioRunResult.setMessage("timed out waiting for CallLegConnectionFailedEvent");
			}
		} catch (InterruptedException e) {
			scenarioRunResult.setResult(false, e.getMessage());
		} finally {
			publishResultOnScenarioComplete(scenarioRunResult);
		}
	}

	public void setup() {
	}

	public void teardown() {
	}

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		this.outboundCallLegBean = (OutboundCallLegBean)this.applicationContext.getBean("outboundCallLegBean");
		this.outboundCallLegBean.addOutboundCallLegListener(this);
	}

	private void unexpectedEvent(AbstractCallLegEvent arg0) {
		unexpectedEvent(arg0, null);
	}

	private void unexpectedEvent(AbstractCallLegEvent arg0, String message) {
		if (callLegIdSemaphores.containsKey(arg0.getId())) {
			String scenarioId = callLegIdScenarioIds.get(arg0.getId());
			ScenarioRunResult scenarioRunResult = new ScenarioRunResult(scenarioId, this.getName());
			String m = String.format("Unexpected Event: %s for %s", message == null ? arg0.getClass().getSimpleName() : message, arg0.getId());
			scenarioRunResult.setMessage(m);
			publishResultOnScenarioComplete(scenarioRunResult);
		}
	}

	public void onCallLegAlerting(CallLegAlertingEvent arg0) {
	}

	public void onCallLegConnected(CallLegConnectedEvent arg0) {
		unexpectedEvent(arg0);
	}

	public void onCallLegConnectionFailed(CallLegConnectionFailedEvent arg0) {
		String callLegId = arg0.getId();
		if (callLegIdSemaphores.containsKey(callLegId)) {
			if (arg0.getTerminationCause().equals(TerminationCause.RemotePartyBusy)) {
				callLegIdSemaphores.get(callLegId).release();
			} else {
				unexpectedEvent(arg0, arg0.getTerminationCause().toString());
			}
		}
	}

	public void onCallLegDisconnected(CallLegDisconnectedEvent arg0) {
		unexpectedEvent(arg0);
	}

	public void onCallLegRefreshCompleted(CallLegRefreshCompletedEvent arg0) {
		unexpectedEvent(arg0);
	}

	public void onCallLegTerminated(CallLegTerminatedEvent arg0) {
		unexpectedEvent(arg0);
	}

	public void onCallLegTerminationFailed(CallLegTerminationFailedEvent arg0) {
		unexpectedEvent(arg0);
	}

	public void onReceivedCallLegRefresh(ReceivedCallLegRefreshEvent arg0) {
		unexpectedEvent(arg0);
	}

	public static void main(String[] args) {
		ClassPathXmlApplicationContext applicationContext = null;
		try {
			InboundCallScenario scenario = new InboundCallScenario();
			ScenarioRunResultListenerImpl listener = new ScenarioRunResultListenerImpl();
			scenario.setScenarioRunResultListener(listener );
			applicationContext = new ClassPathXmlApplicationContext(new String[] {
	  				"core-ctx.xml",
	  				"memory-collections-ctx.xml",
					"com/bt/aloha/batchtest/v2/scenarios/inboundCallScenarioApplicationContext.xml"
					});
			scenario.setApplicationContext(applicationContext);
			scenario.setTestEndpoint("sip:inbound@172.25.58.154:7072");
			StackManagerSyncronizationSemaphore s = new StackManagerSyncronizationSemaphoreImpl();
			s.initialize(1, 1);
			scenario.setStackManagerSyncronizationSemaphore(s);
			scenario.run("fred");
			ResultLogger logger = new ResultLogger();
			logger.setScenarioResults(listener.getResults());
			logger.logResultEntries();
			logger.logResultsSummary();

		} finally {
			if (null != applicationContext) applicationContext.destroy();
		}
	}
}
