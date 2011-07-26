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

 	

 	
 	
 
package com.bt.aloha.media.convedia.conference;

import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.task.TaskExecutor;

import com.bt.aloha.media.conference.collections.ConferenceCollection;
import com.bt.aloha.media.conference.state.ConferenceInfo;
import com.bt.aloha.media.conference.state.ConferenceState;
import com.bt.aloha.media.conference.state.ConferenceTerminationCause;

public class MaxConferenceDurationTermination {

	private static final long MINUTE_TO_MILLISECONDS = 60000;
	private ConferenceBean conferenceBean;
	private ConferenceCollection conferenceCollection;
	private MaxConferenceDurationScheduler maxConferenceDurationScheduler;
	private Log log = LogFactory.getLog(this.getClass());
	private TaskExecutor taskExecutor;

	public MaxConferenceDurationTermination() {}

	public void runTask() {
		log.debug("runTask()");
		this.taskExecutor.execute(new Runnable() {
			public void run() {
				initialize();
			}
		});
	}
	
    public void initialize() {
    	log.debug("initialize()");
    	//TODO: replace getAll()
	    ConcurrentMap<String, ConferenceInfo> allConferenceCalls = 	conferenceCollection.getAll();
	    log.debug(String.format("Initialising with %s conference calls", allConferenceCalls.size()));
	    for (ConferenceInfo conferenceInfo : allConferenceCalls.values())
	    	if (conferenceInfo.getConferenceState().equals(ConferenceState.Active) && conferenceInfo.getMaxDurationInMinutes() > 0)
	    		setTerminationTime(conferenceInfo);
	}

	private void setTerminationTime(ConferenceInfo conferenceInfo) {
		long timeToTerminate = conferenceInfo.getStartTime() + (conferenceInfo.getMaxDurationInMinutes() * MINUTE_TO_MILLISECONDS);
		log.debug(String.format("ConferenceId: %s, start time: %s, max duration: %s, time to terminate: %s, current time: %s",
				conferenceInfo.getId(), conferenceInfo.getStartTime(), conferenceInfo.getMaxDurationInMinutes() * MINUTE_TO_MILLISECONDS, timeToTerminate, System.currentTimeMillis()));
		if (timeToTerminate <= System.currentTimeMillis()){
			log.debug(String.format("Request termination on conferenceId: %s", conferenceInfo.getId()));
			try {
			    conferenceBean.endConference(conferenceInfo.getId(), ConferenceTerminationCause.MaximumDurationExceeded);
            } catch (Throwable t) {
                log.warn("Error ending conference", t);
            }
		} else {
			log.debug(String.format("Set termination time to %s on conferenceId %s", timeToTerminate, conferenceInfo.getId()));
            try {
                maxConferenceDurationScheduler.terminateConferenceAfterMaxDuration(conferenceInfo, conferenceBean);
            } catch (Throwable t) {
                log.warn("Error scheduling end of conference", t);
            }
		}
	}

	public void setConferenceCollection(ConferenceCollection aConferenceCollection){
		this.conferenceCollection = aConferenceCollection;
	}

	public void setConferenceBean(ConferenceBean aConferenceBean){
		this.conferenceBean = aConferenceBean;
	}

	public void setMaxConferenceDurationScheduler(MaxConferenceDurationScheduler aMaxConferenceDurationScheduler){
		this.maxConferenceDurationScheduler = aMaxConferenceDurationScheduler;
	}

	public void setTaskExecutor(TaskExecutor aTaskExecutor) {
		this.taskExecutor = aTaskExecutor;
	}
}
