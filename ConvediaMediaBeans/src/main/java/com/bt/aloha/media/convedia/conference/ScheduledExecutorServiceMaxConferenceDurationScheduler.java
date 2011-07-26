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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.media.conference.state.ConferenceInfo;

public class ScheduledExecutorServiceMaxConferenceDurationScheduler implements MaxConferenceDurationScheduler {
	private static final int NUMBER_OF_SECONDS_IN_ONE_MINUTE = 60;
	private Log log = LogFactory.getLog(this.getClass());
	private ScheduledExecutorService executorService;

	public ScheduledExecutorServiceMaxConferenceDurationScheduler() {
		executorService = null;
	}

	public void terminateConferenceAfterMaxDuration(ConferenceInfo conferenceInfo, ConferenceBean conferenceBean) {
		cancelTerminateConference(conferenceInfo);
		log.debug(String.format("Scheduling conference %s to be terminated after %s min", conferenceInfo.getId(), conferenceInfo.getMaxDurationInMinutes()));
		ScheduledFuture<?> future = this.executorService.schedule(new TerminateConferenceTask(conferenceInfo.getId(), conferenceBean), conferenceInfo.getMaxDurationInMinutes() * NUMBER_OF_SECONDS_IN_ONE_MINUTE, TimeUnit.SECONDS);
		conferenceInfo.setFuture(future);
	}

	public void setScheduledExecutorService(ScheduledExecutorService theExecutorService) {
		this.executorService = theExecutorService;
	}

	public void cancelTerminateConference(ConferenceInfo conferenceInfo) {
		log.debug(String.format("Canceling terminating scheduler for conference %s", conferenceInfo.getId()));
		ScheduledFuture<?> future = conferenceInfo.getFuture();
		if (future != null)
			log.debug(String.format("Cancel succeeded: %s", future.cancel(false)));
	}
}
