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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.media.conference.state.ConferenceTerminationCause;

public class TerminateConferenceTask implements Runnable {
	private Log log = LogFactory.getLog(TerminateConferenceTask.class);
	private String conferenceId;
	private ConferenceBean conferenceBean;

	public TerminateConferenceTask (String aConferenceId, ConferenceBean aConferenceBean) {
		this.conferenceId = aConferenceId;
		this.conferenceBean  = aConferenceBean;
	}

	public void run() {
		log.debug(String.format("Terminating scheduled conference: %s", conferenceId));
		try {
			this.conferenceBean.endConference(this.conferenceId, ConferenceTerminationCause.MaximumDurationExceeded);
		} catch (Throwable t) {
			log.warn(String.format("Unable to terminate scheduled conference %s", conferenceId), t);
		}
	}
}
