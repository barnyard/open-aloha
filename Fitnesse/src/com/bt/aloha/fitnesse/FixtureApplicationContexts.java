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

 	

 	
 	
 
package com.bt.aloha.fitnesse;

import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.aloha.call.ScheduledExecutorServiceMaxCallDurationScheduler;
import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.call.state.CallInfo;
import com.bt.aloha.media.conference.collections.ConferenceCollection;
import com.bt.aloha.media.conference.state.ConferenceInfo;
import com.bt.aloha.media.convedia.conference.ScheduledExecutorServiceMaxConferenceDurationScheduler;

public class FixtureApplicationContexts {
	private ClassPathXmlApplicationContext applicationContext = null;
	private ClassPathXmlApplicationContext mockphonesapplicationContext = null;
	private ClassPathXmlApplicationContext mediaApplicationContext = null;
	private ClassPathXmlApplicationContext secondApplicationContext = null;
	private ClassPathXmlApplicationContext sipLoadBalancerApplicationContext = null;
	private ClassPathXmlApplicationContext inboundApplicationContext = null;
	private ClassPathXmlApplicationContext persistencyApplicationContext = null;
	private static final Log log = LogFactory.getLog(FixtureApplicationContexts.class);
	private static final FixtureApplicationContexts instance = new FixtureApplicationContexts();

	private FixtureApplicationContexts(){

	}

	public static FixtureApplicationContexts getInstance(){
		return instance;
	}

    public void destroyPersistencyApplicationContext(){
        if (null != persistencyApplicationContext)
            persistencyApplicationContext.destroy();
    }

	public ClassPathXmlApplicationContext startApplicationContext() {
		return startApplicationContext(false);
	}

	public ClassPathXmlApplicationContext startApplicationContext(
			boolean destroyFirst) {
		applicationContext = startCustomApplicationContext(applicationContext,
				"testApplicationContext.xml", destroyFirst, 0);
		return applicationContext;
	}

	public ClassPathXmlApplicationContext startMockphonesApplicationContext(){
		return startMockphonesApplicationContext(false);
	}

	public ClassPathXmlApplicationContext startMockphonesApplicationContext(
			boolean destroyFirst) {
		mockphonesapplicationContext = startCustomApplicationContext(
				mockphonesapplicationContext,
				"mockphonesApplicationContext.xml", destroyFirst, 0);
		return mockphonesapplicationContext;
	}

	public ClassPathXmlApplicationContext startMediaApplicationContext(){
		return startMediaApplicationContext(false);
	}

	public ClassPathXmlApplicationContext startMediaApplicationContext(
			boolean destroyFirst) {
		mediaApplicationContext = startCustomApplicationContext(
				mediaApplicationContext, "mediaApplicationContext.xml",
				destroyFirst, 0);
		return mediaApplicationContext;
	}

	public ClassPathXmlApplicationContext startSecondApplicationContext(){
		return startSecondApplicationContext(false);
	}

	public ClassPathXmlApplicationContext startSecondApplicationContext(
			boolean destroyFirst) {
		secondApplicationContext = startCustomApplicationContext(
				secondApplicationContext, "secondTestApplicationContext.xml",
				destroyFirst, 0);
		return secondApplicationContext;
	}

	public ClassPathXmlApplicationContext startSipLoadBalancerApplicationContext(){
		return startSipLoadBalancerApplicationContext(false);
	}

	public ClassPathXmlApplicationContext startSipLoadBalancerApplicationContext(
			boolean destroyFirst) {
		sipLoadBalancerApplicationContext = startCustomApplicationContext(
				sipLoadBalancerApplicationContext,
				"sipLoadBalancerApplicationContext.xml", destroyFirst, 0);
		return sipLoadBalancerApplicationContext;
	}

	public ClassPathXmlApplicationContext startInboundApplicationContext(){
		return startInboundApplicationContext(false);
	}

	public ClassPathXmlApplicationContext startInboundApplicationContext(
			boolean destroyFirst) {
		inboundApplicationContext = startCustomApplicationContext(
				inboundApplicationContext, "inboundTestApplicationContext.xml",
				destroyFirst, 0);
		return inboundApplicationContext;
	}

	public ClassPathXmlApplicationContext startPersistencyApplicationContext(){
		return startPersistencyApplicationContext(false, 0);
	}

	public ClassPathXmlApplicationContext startPersistencyApplicationContext(
			boolean destroyFirst, int restartAfter) {
		persistencyApplicationContext = startCustomApplicationContext(
				persistencyApplicationContext,
				"persistencyApplicationContext.xml", destroyFirst, restartAfter);
		return persistencyApplicationContext;
	}

	public ClassPathXmlApplicationContext startCustomApplicationContext(
			ClassPathXmlApplicationContext applicationContext,
			String resourceName, boolean destroyFirst, int restartAfter) {
		if (applicationContext == null){
			String distroyMessage = (destroyFirst ? "(ignoring 'distroyFirst' flag)":"");
			log.debug(String.format("creating new application context from '%s' %s", resourceName, distroyMessage));
			return new ClassPathXmlApplicationContext(resourceName);
		}
		if (destroyFirst) {
			log.debug(String.format("distroying and re-creating new application context from '%s'", resourceName));
			cancelTerminationTimers(applicationContext);
			applicationContext.destroy();
			try {
				Thread.sleep(restartAfter);
			} catch (InterruptedException e) {
				log.warn("Unable to sleep while jvm is down, interrupted");
			}
			ClassPathXmlApplicationContext result = new ClassPathXmlApplicationContext(resourceName);
			log.debug("returning");
			return result;
		}
		log.debug(String.format("using an already created application context from '%s'", resourceName));
		return applicationContext;
	}

	private void cancelTerminationTimers(ClassPathXmlApplicationContext applicationContext) {
		CallCollection callCollection = (CallCollection)applicationContext.getBean("callCollection");
		ConferenceCollection conferenceCollection = (ConferenceCollection)applicationContext.getBean("conferenceCollection");
		ScheduledExecutorServiceMaxCallDurationScheduler callTerminator = (ScheduledExecutorServiceMaxCallDurationScheduler)applicationContext.getBean("maxCallDurationScheduler");
		ScheduledExecutorServiceMaxConferenceDurationScheduler conferenceTerminator = (ScheduledExecutorServiceMaxConferenceDurationScheduler)applicationContext.getBean("maxConferenceDurationScheduler");
		
		ConcurrentMap<String, CallInfo> calls = callCollection.getAll();
		for (CallInfo callInfo : calls.values())
			callTerminator.cancelTerminateCall(callInfo);
		ConcurrentMap<String, ConferenceInfo> conferences = conferenceCollection.getAll();
		for (ConferenceInfo conferenceInfo : conferences.values())
			conferenceTerminator.cancelTerminateConference(conferenceInfo);
	}
}
