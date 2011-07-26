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

import com.bt.aloha.media.conference.event.ConferenceActiveEvent;
import com.bt.aloha.media.conference.event.ConferenceEndedEvent;
import com.bt.aloha.media.conference.event.ParticipantConnectedEvent;
import com.bt.aloha.media.conference.event.ParticipantDisconnectedEvent;
import com.bt.aloha.media.conference.event.ParticipantFailedEvent;
import com.bt.aloha.media.conference.event.ParticipantTerminatedEvent;

/**
 * To listen to conference events you must implement this interface and then add class to the ConferenceBean listener list.
 */
public interface ConferenceListener {
    /**
     * Event fired when a conference becomes active
     * @param conferenceActiveEvent details of the event
     */
    void onConferenceActive(ConferenceActiveEvent conferenceActiveEvent);

	/**
     * Event fired when a conference ends
     * @param conferenceEndedEvent details of the event
	 */
    void onConferenceEnded(ConferenceEndedEvent conferenceEndedEvent);

    /**
     * Event fired when a conference participant is connected
     * @param participantConnectedEvent details of the event
     */
    void onParticipantConnected(ParticipantConnectedEvent participantConnectedEvent);

    /**
     * Event fired when a conference participant is terminated
     * @param participantTerminatedEvent details of the event
     */
    void onParticipantTerminated(ParticipantTerminatedEvent participantTerminatedEvent);

    /**
     * Event fired when a conference participant is disconnected
     * @param participantDisconnectedEvent details of the event
     */
    void onParticipantDisconnected(ParticipantDisconnectedEvent participantDisconnectedEvent);

    /**
     * Event fired when a conference participant fails to connect
     * @param participantFailedEvent details of the event
     */
    void onParticipantFailed(ParticipantFailedEvent participantFailedEvent);
}
