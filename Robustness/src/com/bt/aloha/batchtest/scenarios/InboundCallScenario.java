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
import java.util.Hashtable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.batchtest.BatchTestScenarioBase;
import com.bt.aloha.callleg.InboundCallLegListener;
import com.bt.aloha.callleg.OutboundCallLegBean;
import com.bt.aloha.callleg.OutboundCallLegListener;
import com.bt.aloha.callleg.event.CallLegAlertingEvent;
import com.bt.aloha.callleg.event.CallLegConnectedEvent;
import com.bt.aloha.callleg.event.CallLegConnectionFailedEvent;
import com.bt.aloha.callleg.event.CallLegDisconnectedEvent;
import com.bt.aloha.callleg.event.CallLegRefreshCompletedEvent;
import com.bt.aloha.callleg.event.CallLegTerminatedEvent;
import com.bt.aloha.callleg.event.CallLegTerminationFailedEvent;
import com.bt.aloha.callleg.event.IncomingCallLegEvent;
import com.bt.aloha.callleg.event.ReceivedCallLegRefreshEvent;
import com.bt.aloha.dialog.event.IncomingAction;
import com.bt.aloha.dialog.event.IncomingResponseCode;
/**
 * @author Robbie & Fab
 * So, this scenario is for testing load balancing the bottom end for inbound calls
 * We use a mock phone stack to send an INVITE to the load balancer stack which forwards to
 * one of the load balanced stacks.  That stack declines the call, and the scenario is passed
 * when the CallLegConnectionFailedEvent is recieved on the mock phone stack. 
 */
public class InboundCallScenario extends BatchTestScenarioBase implements InboundCallLegListener {
	private OutboundCallLegBean outboundCallLegBeanFromMockphoneContext;
//	private InboundCallLegBean inboundCallLegBean;
	private URI incomingUri;
	protected Hashtable<String, String> scenarioIdMap = new Hashtable<String, String>();
	private static Log log = LogFactory.getLog(InboundCallScenario.class);
	private Object theLock = new Object();
	
	private static class OutboundCallTrigger implements OutboundCallLegListener{

		private InboundCallScenario scenario;
		
		public OutboundCallTrigger(InboundCallScenario scenarioBase){
			this.scenario = scenarioBase;
		}
		
		public void onCallLegAlerting(CallLegAlertingEvent arg0) {
			String callLegId = arg0.getId();
			scenario.setUpdateScenario(callLegId, "OutboundCallTrigger.onCallLegAlerting received");
		}
		
		public void onCallLegConnected(CallLegConnectedEvent arg0) {
			String callLegId = arg0.getId();
			scenario.setUpdateScenario(callLegId, "OutboundCallTrigger.onCallLegConnected received");
		}

		public void onCallLegConnectionFailed(CallLegConnectionFailedEvent arg0) {
			String callLegId = arg0.getId();
			scenario.setSucceed(callLegId);
		}

		public void onCallLegDisconnected(CallLegDisconnectedEvent arg0) {
			String callLegId = arg0.getId();
			scenario.setUpdateScenario(callLegId, "OutboundCallTrigger.onCallLegDisconnected received");
		}

		public void onCallLegRefreshCompleted(CallLegRefreshCompletedEvent arg0) {
			String callLegId = arg0.getId();
			scenario.setUpdateScenario(callLegId, "OutboundCallTrigger.onCallLegRefreshCompleted received");
		}

		public void onCallLegTerminated(CallLegTerminatedEvent arg0) {
			String callLegId = arg0.getId();
			scenario.setUpdateScenario(callLegId, "OutboundCallTrigger.onCallLegTerminated received");
		}

		public void onCallLegTerminationFailed(CallLegTerminationFailedEvent arg0) {
			String callLegId = arg0.getId();
			scenario.setUpdateScenario(callLegId, "OutboundCallTrigger.onCallLegTerminationFailed received");
		}

		public void onReceivedCallLegRefresh(ReceivedCallLegRefreshEvent arg0) {
			String callLegId = arg0.getId();
			scenario.setUpdateScenario(callLegId, "OutboundCallTrigger.onReceivedCallLegRefresh received");
		}
	}

	public Hashtable<String,String> getScenarioIdMap(){
		return scenarioIdMap;
	}
	
	public void setSucceed(String id){
		synchronized (theLock) {
			String scenarioId = scenarioIdMap.get(id);
			if(scenarioId!=null){
				updateScenario(scenarioId, "setting for success");
				succeed(scenarioId);
			}
			else{
				log.info(String.format("cannot setSucceded - no scenario found for call leg %s", id));
			}
		}
	}

	public void setUpdateScenario(String id, String m){
		synchronized (theLock) {
			String scenarioId = scenarioIdMap.get(id);
			if(scenarioId!=null){
				updateScenario(scenarioId, m);
			}
			else{
				log.info(String.format("cannot updateScenario - no scenario found for call leg %s", id));
			}
		}
	}

	private OutboundCallTrigger trigger = new OutboundCallTrigger(this);
	private boolean listenerAdded = false;
	
	@Override
	protected void startScenario(String id) throws Exception {
		if(!listenerAdded){
			outboundCallLegBeanFromMockphoneContext.addOutboundCallLegListener(trigger);
			listenerAdded = true;
		}
		String callLegId = null;
		synchronized (theLock) {
			callLegId = outboundCallLegBeanFromMockphoneContext.createCallLeg(URI.create("sip:random"), incomingUri);
			scenarioIdMap.put(callLegId, id);
			log.debug("Created call leg, id = " + callLegId);
			log.debug("call leg connecting");
			outboundCallLegBeanFromMockphoneContext.connectCallLeg(callLegId);
		}
		log.info(String.format("Started scenario %s for call leg id %s", id, callLegId));
	}

	public void onIncomingCallLeg(IncomingCallLegEvent arg0) {
		log.debug("incoming call event recieved: " + arg0.toString());
		arg0.setIncomingCallAction(IncomingAction.Reject);
		arg0.setResponseCode(IncomingResponseCode.Decline);
	}

	public void setOutboundCallLegBeanFromMockphoneContext(OutboundCallLegBean outboundCallLegBeanFromMockphoneContext) {
		this.outboundCallLegBeanFromMockphoneContext = outboundCallLegBeanFromMockphoneContext;
	}

//	public void setInboundCallLegBean(InboundCallLegBean inboundCallLegBean) {
//		this.inboundCallLegBean = inboundCallLegBean;
//	}
//
	public void setIncomingUri(String incomingUri) {
		this.incomingUri = URI.create(incomingUri);
	}

	public void onCallLegConnected(CallLegConnectedEvent arg0) {
		String callLegId = arg0.getId();
		setUpdateScenario(callLegId, "InboundCallScenario.onReceivedCallLegRefresh received");
	}
	public void onCallLegConnectionFailed(CallLegConnectionFailedEvent arg0) {
		String callLegId = arg0.getId();
		setUpdateScenario(callLegId, "InboundCallScenario.onCallLegConnectionFailed received");
	}
	public void onCallLegDisconnected(CallLegDisconnectedEvent arg0) {
		String callLegId = arg0.getId();
		setUpdateScenario(callLegId, "InboundCallScenario.onCallLegDisconnected received");
	}
	public void onCallLegRefreshCompleted(CallLegRefreshCompletedEvent arg0) {
		String callLegId = arg0.getId();
		setUpdateScenario(callLegId, "InboundCallScenario.onCallLegRefreshCompleted received");
	}
	public void onCallLegTerminated(CallLegTerminatedEvent arg0) {
		String callLegId = arg0.getId();
		setUpdateScenario(callLegId, "InboundCallScenario.onCallLegTerminated received");
	}
	public void onCallLegTerminationFailed(CallLegTerminationFailedEvent arg0) {
		String callLegId = arg0.getId();
		setUpdateScenario(callLegId, "InboundCallScenario.onCallLegTerminationFailed received");
	}
	public void onReceivedCallLegRefresh(ReceivedCallLegRefreshEvent arg0) {
		String callLegId = arg0.getId();
		setUpdateScenario(callLegId, "InboundCallScenario.onReceivedCallLegRefresh received");
	}
}
