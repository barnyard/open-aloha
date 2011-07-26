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

 	

 	
 	
 
package com.bt.aloha.media.conference.event;

import com.bt.aloha.media.conference.state.ConferenceTerminationCause;

/**
 * Event fired when a conference ends. This occurs either when the last participant is disconnected, or when
 * ConferenceBean.endConference is called.
 */
public class ConferenceEndedEvent extends AbstractConferenceEvent {
	private ConferenceTerminationCause conferenceTerminationCause;
	private int duration;

	/**
     * Constructor
     * @param aConfId the conference ID
     * @param aConferenceTerminationCause the termination cause
     * @param aDuration the conference duration in seconds
	 */
    public ConferenceEndedEvent(String aConfId, ConferenceTerminationCause aConferenceTerminationCause, int aDuration) {
		super(aConfId);
		conferenceTerminationCause = aConferenceTerminationCause;
		duration = aDuration;
	}

	/**
     * Return the ConferenceTerminationCause
     * @return the ConferenceTerminationCause
	 */
    public ConferenceTerminationCause getConferenceTerminationCause() {
		return conferenceTerminationCause;
	}

	/**
     * return the conference duration in seconds
     * @return the conference duration in seconds
	 */
    public int getDuration() {
		return duration;
	}
}
