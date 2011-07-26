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

import java.net.URI;

import com.bt.aloha.media.conference.state.ConferenceInformation;
import com.bt.aloha.media.conference.state.ConferenceTerminationCause;

/**
 * Spring Bean to create and manage conferences.
 */
public interface ConferenceBean {
    /**
     * Create a conference
     * @return a conference ID
     */
	String createConference();

	/**
     * Create a conference
     * @param maxNumberOfParticipants the maximum number of participants
     * @param maxDurationInMinutes the maximum duration in minutes
     * @return the conference ID
	 */
    String createConference(int maxNumberOfParticipants, long maxDurationInMinutes);

    /**
     * Invite a participant
     * @param conferenceId the conference ID
     * @param callLegId the call leg ID
     */
	void inviteParticipant(String conferenceId, String callLegId);

	/**
     * Terminate a participant
     * @param conferenceId the conference ID
     * @param callLegId the call leg ID
	 */
    void terminateParticipant(String conferenceId, String callLegId);

    /**
     * End a conference
     * @param conferenceId the conference ID
     */
	void endConference(String conferenceId);

    /**
     * End a conference providing a termination cause
     * @param conferenceId the conference ID
     * @param terminationCause the termination cause
     */
	void endConference(String conferenceId, ConferenceTerminationCause terminationCause);

    /**
     * Get information about a conference
     * @param conferenceId the conference ID
     * @return the conference information
     */
    ConferenceInformation getConferenceInformation(String conferenceId);

    /**
     * Create a participant call leg
     * @param conferenceId the conference ID
     * @param sipUri the URI of the participant
     * @return the call leg ID
     */
    String createParticipantCallLeg(String conferenceId, URI sipUri);

    /**
     * Add a conference listener
     * @param aCallListener the listener
     */
	void addConferenceListener(ConferenceListener aCallListener);

    /**
     * Remove a conference listener
     * @param listener the listener to remove
     */
	void removeConferenceListener(ConferenceListener listener);
}
