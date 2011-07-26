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

 	

 	
 	
 
package com.bt.aloha.batchtest;

import java.net.URI;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.call.CallBean;
import com.bt.aloha.call.CallBeanImpl;
import com.bt.aloha.call.CallListener;
import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.callleg.OutboundCallLegBean;
import com.bt.aloha.media.MediaCallBean;
import com.bt.aloha.media.MediaCallListener;
import com.bt.aloha.media.convedia.MediaCallBeanImpl;
import com.bt.aloha.media.convedia.conference.ConferenceBean;
import com.bt.aloha.media.convedia.conference.ConferenceBeanImpl;
import com.bt.aloha.media.convedia.conference.ConferenceListener;
import com.bt.aloha.util.MessageDigestHelper;

public abstract class BatchTestScenarioBase implements BatchTestScenario {
	private static final Log LOG = LogFactory.getLog(BatchTestScenarioBase.class);
	protected final static String SCENARIO_STARTED = "Start of scenario";
	
	private BatchTestScenarioResultListener listener;
	private Vector<String> scenariosToTerminate = new Vector<String>();
	private String testEndpoint;
	private String rejectTestEndpoint;
	private String callAnswerTimeoutEndpoint;
	private String fromAddress;
	private String audioFileUri;

	protected OutboundCallLegBean outboundCallLegBean;
	protected CallBean callBean;
	protected MediaCallBean mediaCallBean;
	protected ConferenceBean conferenceBean;
	protected CallCollection callCollection;

	protected MultistackApplicationContextManager manager;

	public void setOutboundCallLegBean(OutboundCallLegBean outboundCallLegBean) {
		this.outboundCallLegBean = outboundCallLegBean;
	}

	public void setCallBean(CallBean callBean) {
		this.callBean = callBean;
	}

	public void setMediaCallBean(MediaCallBean mediaCallBean) {
		this.mediaCallBean = mediaCallBean;
	}

	public void setConferenceBean(ConferenceBean conferenceBean) {
		this.conferenceBean = conferenceBean;
	}

	public void setCallCollection(CallCollection callCollection) {
		this.callCollection = callCollection;
	}

	public String getTestEndpoint() {
		return this.testEndpoint;
	}

    protected URI getTestEndpointUri() {
        return URI.create(this.testEndpoint);
    }

	public void setTestEndpoint(String endpoint) {
		this.testEndpoint = endpoint;
	}

	public URI getCallAnswerTimeoutEndpointUri() {
		return URI.create(callAnswerTimeoutEndpoint);
	}

	public String getCallAnswerTimeoutEndpoint() {
		return callAnswerTimeoutEndpoint;
	}

	public void setCallAnswerTimeoutEndpoint(String callAnswerTimeoutEndpoint) {
		this.callAnswerTimeoutEndpoint = callAnswerTimeoutEndpoint;
	}

	public String getFromAddress() {
		return fromAddress;
	}

    protected URI getFromAddressUri() {
        return URI.create(fromAddress);
    }

	public void setFromAddress(String fromAddress) {
		this.fromAddress = fromAddress;
	}

	public String getAudioFileUri() {
		return audioFileUri;
	}

	public void setAudioFileUri(String audioFileUri) {
		this.audioFileUri = audioFileUri;
	}

	public BatchTestScenarioResultListener getBatchTestScenarioResultListener() {
		return this.listener;
	}

	public void setBatchTestScenarioResultListener(BatchTestScenarioResultListener listener) {
		this.listener = listener;
	}

	public URI getBadAddressUri() {
		return URI.create("sip:1.2.3.4");
	}

	public void terminate(String scenarioId) {
		this.scenariosToTerminate.add(scenarioId);
	}

	protected abstract void startScenario(String id) throws Exception;

	protected void succeed(String id) {
		LOG.info("Run " + id + " succeeded!");
		this.listener.runCompleted(id, true);
	}

	protected void fail(String id, String message) {
		LOG.warn("Run " + id + " failed with the following message:\n" + message);
		this.listener.runCompleted(id, false);
	}
	
	protected void updateScenario(String id, String message) {
		LOG.info(String.format("Updating scenario %s with message %s", id, message));
		this.listener.updateRunStatus(id, message);
	}

	public String start(String beanName) throws Exception {
		String scenarioId = beanName + ":" +
            MessageDigestHelper.generateDigest(System.currentTimeMillis() + "batchtest" + Math.random());
//        this.listener.runCompleted(scenarioId, false);
        LOG.info("Starting scenario " + scenarioId + ", " + beanName);
		this.startScenario(scenarioId);
		return scenarioId;
	}

	protected URI getRejectTestEndpointUri() {
		return URI.create(rejectTestEndpoint);
	}

    public String getRejectTestEndpoint() {
        return rejectTestEndpoint;
    }

	public void setRejectTestEndpoint(String rejectTestEndpoint) {
		this.rejectTestEndpoint = rejectTestEndpoint;
	}
	
	protected static<T> T waitForScenarioData(String eventCallId, Map<String, T> map) {
	    for (int i = 0; i < 20; i++) {
	        T data = map.get(eventCallId);
	        if (data != null)
	            return data;
	        try {
	            Thread.sleep(1000);
	        } catch (InterruptedException e) {
	            LOG.error(e.getMessage(),e);
	        }
	    }
	    return null;
	}

	public void setApplicationContextManager(MultistackApplicationContextManager m){
		this.manager = m;
		if (manager.getApplicationContext2() == null) return;
		if (this instanceof CallListener && !((CallBeanImpl)manager.getApplicationContext2().getBean("callBean")).getCallListeners().contains(this)) {
			LOG.debug("adding " + this.getClass().getSimpleName() + " as a CallListener to ApplicationContext2");
			((CallBeanImpl)manager.getApplicationContext2().getBean("callBean")).addCallListener((CallListener)this);
		}
		if (this instanceof ConferenceListener && !((ConferenceBeanImpl)manager.getApplicationContext2().getBean("conferenceBean")).getConferenceListeners().contains(this)) {
			LOG.debug("adding " + this.getClass().getSimpleName() + " as a ConferenceListener to ApplicationContext2");
			((ConferenceBeanImpl)manager.getApplicationContext2().getBean("conferenceBean")).addConferenceListener((ConferenceListener)this);
		}
		if (this instanceof MediaCallListener && !((MediaCallBeanImpl)manager.getApplicationContext2().getBean("mediaCallBean")).getMediaCallListeners().contains(this)) {
			LOG.debug("adding " + this.getClass().getSimpleName() + " as a MediaCallListener to ApplicationContext2");
			((MediaCallBeanImpl)manager.getApplicationContext2().getBean("mediaCallBean")).addMediaCallListener((MediaCallListener)this);
		}
	}
}
