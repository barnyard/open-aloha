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

 	

 	
 	
 
package com.bt.aloha.media.conference.state;

import java.util.Calendar;
import java.util.Map;

/**
 * Information about a conference
 */
/**
 * @author 600559687
 *
 */
public class ConferenceInformation {
	private String id;
	private ConferenceState state;
	private Calendar createTime;
	private Calendar startTime;
	private Calendar endTime;
	private int duration;
	private ConferenceTerminationCause terminationCause;
	private int numOfActiveParticipants;
	private int numOfParticipants;
	private Map<String,ParticipantState> participantStates;

	/**
     * Constructor
	 */
	/**
	 * @param aId
	 * @param aState the conference state
	 * @param aCreateTime the conference create time
	 * @param aStartTime the conference start time
	 * @param aEndTime the conference end time
	 * @param aDuration the conference duration
	 * @param aTerminationCause the conference termination cause
	 * @param aNumOfActiveParticipants number of active participants
	 * @param aNumOfParticipants number of participants
	 * @param aParticipantStates participant states
	 */
	public ConferenceInformation(String aId, ConferenceState aState, long aCreateTime, long aStartTime, long aEndTime,
			int aDuration, ConferenceTerminationCause aTerminationCause, int aNumOfActiveParticipants, int aNumOfParticipants, Map<String,ParticipantState> aParticipantStates) {
		this.id = aId;
		this.state = aState;
		this.createTime = Calendar.getInstance();
		this.createTime.setTimeInMillis(aCreateTime);
		this.startTime = Calendar.getInstance();
		this.startTime.setTimeInMillis(aStartTime);
		this.endTime = Calendar.getInstance();
		this.endTime.setTimeInMillis(aEndTime);
		this.duration = aDuration;
		this.terminationCause = aTerminationCause;
		this.numOfActiveParticipants = aNumOfActiveParticipants;
		this.numOfParticipants = aNumOfParticipants;
		this.participantStates = aParticipantStates;
	}

	/**
	 * Get the conference identifier
	 * @return the conference identifier
	 */
	public String getId() {
		return id;
	}

	/**
     * Get the conference state
     * @return the conference state
	 */
    public ConferenceState getConferenceState() {
		return state;
	}

	/**
     * Get the conference start time
     * @return the conference start time
	 */
    public Calendar getStartTime() {
		return startTime;
	}

    /**
     * Get the conference duration in seconds
     * @return the conference duration in seconds
     */
	public int getDuration() {
		return duration;
	}

    /**
     * Get the conference termination cause
     * @return the conference termination cause
     */
	public ConferenceTerminationCause getConferenceTerminationCause() {
		return terminationCause;
	}

	/**
	 * Get number of active participants
	 * @return the number of active participants
	 */
	public int getNumberOfActiveParticipants() {
		return numOfActiveParticipants;
	}

	/**
	 * Get number of participants
	 * @return the number of participants
	 */
	public int getNumberOfParticipants() {
		return numOfParticipants;
	}

	/**
	 * Get create time
	 * @return the create time
	 */
	public Calendar getCreateTime() {
		return createTime;
	}

	/**
	 * Get the conference end time
	 * @return the end time
	 */
	public Calendar getEndTime() {
		return endTime;
	}

	/**
	 * Get a state of participant
	 * @param callId call identifier for the participant
	 * @return the participant state
	 */
	public ParticipantState getParticipantState(String callId) {
		return participantStates.get(callId);
	}
}
