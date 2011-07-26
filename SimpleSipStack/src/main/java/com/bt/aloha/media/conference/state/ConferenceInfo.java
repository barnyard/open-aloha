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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.state.StateInfoBase;
import com.bt.aloha.util.MessageDigestHelper;

public class ConferenceInfo extends StateInfoBase<ConferenceInfo> implements ReadOnlyConferenceInfo {
	public static final int DEFAULT_MAX_NUMBER_OF_PARTICIPANTS = 0;
	public static final long DEFAULT_MAX_DURATION_IN_MINUTES = 0;
	private static final String FUTURE = "future";
	private static final long serialVersionUID = -8417423038531670210L;
	private static Log log = LogFactory.getLog(ConferenceInfo.class);
    private String mediaServerAddress;
	private Map<String,ParticipantState> participants;
	private ConferenceState state;
	private ConferenceTerminationCause conferenceTerminationCause;
	private int maxNumberOfParticipants;
	private long maxDurationInMinutes;
	private transient ScheduledFuture<?> future;
	private transient boolean flipFlopForSimulatingConcurrencyFailures;

	public ConferenceInfo(String creatingBeanName, String aMediaServerAddress) {
		this(creatingBeanName, aMediaServerAddress, DEFAULT_MAX_NUMBER_OF_PARTICIPANTS, DEFAULT_MAX_DURATION_IN_MINUTES);
	}

	public ConferenceInfo(String creatingBeanName, String aMediaServerAddress, int aMaxNumberOfParticipants, long aMaxDurationInMinutes) {
        super(creatingBeanName);
        if (aMaxNumberOfParticipants < 0 || aMaxNumberOfParticipants == 1) {
        	throw new IllegalArgumentException("A conference's MaxNumberOfParticipants must be 2 or more.");
        }
        if (aMaxDurationInMinutes < 0) {
        	throw new IllegalArgumentException("A conference's MaxDurationInMinutes must be 0 or more.");
        }
		setId(MessageDigestHelper.generateDigest());
		this.mediaServerAddress = aMediaServerAddress;
		this.participants = new HashMap<String,ParticipantState>();
		this.state = ConferenceState.Initial;
		this.maxNumberOfParticipants = aMaxNumberOfParticipants;
		this.maxDurationInMinutes = aMaxDurationInMinutes;
		flipFlopForSimulatingConcurrencyFailures = true;
		log.debug(String.format("Created ConferenceInfo %s", getConferenceSipUri()));
	}

	@SuppressWarnings("unchecked")
	@Override
    public ConferenceInfo cloneObject() {
        ConferenceInfo clonedInfo = (ConferenceInfo)super.cloneObject();

        clonedInfo.participants = (Map<String, ParticipantState>)((HashMap<String, ParticipantState>)this.participants).clone();
        return clonedInfo;
    }

	public String getConferenceSipUri() {
		return String.format("sip:conf=%s@%s", getId(), mediaServerAddress);
	}

	public Map<String, ParticipantState> getParticipants() {
		return participants;
	}

	public ParticipantState getParticipantState(String callId) {
		return participants.get(callId);
	}

	public void addParticipant(String callId) {
        participants.put(callId, ParticipantState.Connecting);
	}

	public boolean containsParticipant(String callId) {
        return participants.containsKey(callId);
	}

    public int getNumberOfParticipants(){
        return this.participants.size();
    }

	public int getNumberOfActiveParticipants() {
        int result = 0;
        for (ParticipantState participantState: participants.values()) {
            if (participantState.equals(ParticipantState.Connecting) || participantState.equals(ParticipantState.Connected) || participantState.equals(ParticipantState.Terminating))
                result++;
        }
		return result;
	}

	public boolean updateConferenceState(ConferenceState newState) {
    	if (this.state.ordinal() < newState.ordinal()) {
        	log.debug(String.format("Updating state of conference %s from %s to %s", this.getId(), this.state, newState));
        	this.state = newState;
        	if (newState.equals(ConferenceState.Active))
        		return setStartTime(Calendar.getInstance().getTimeInMillis());
        	if (newState.equals(ConferenceState.Ended))
        		return setEndTime(Calendar.getInstance().getTimeInMillis());
        	return true;
    	}
       	log.debug(String.format("Previous state of conference %s, not updating (%s to %s)", this.getId(), this.state, newState));
        return false;
	}

	public ConferenceState getConferenceState() {
		return state;
	}

    public boolean updateParticipantState(String callId, ParticipantState newState) {
        ParticipantState oldState = this.participants.get(callId);
        if (null == oldState) return false;
        if (oldState.ordinal() < newState.ordinal()) {
        	log.debug(String.format("Updating state of participant %s from %s to %s", callId, oldState, newState));
        	participants.put(callId, newState);
        	return true;
        }
    	log.debug(String.format("Previous state of participant %s, not updating (%s to %s)", callId, oldState, newState));
    	return false;
    }

	public ConferenceInformation getConferenceInformation() {
		return new ConferenceInformation(getId(), getConferenceState(), getCreateTime(), getStartTime(), getEndTime(), getDuration(), getConferenceTerminationCause(), getNumberOfActiveParticipants(), getNumberOfParticipants(), participants);
	}

	public ConferenceTerminationCause getConferenceTerminationCause() {
		return conferenceTerminationCause;
	}

	public boolean setConferenceTerminationCause(ConferenceTerminationCause cause) {
		if (this.conferenceTerminationCause == null) {
			this.conferenceTerminationCause = cause;
			return true;
		}
		log.debug(String.format("Attempt to set conference termination cause again for conf %s (%s to %s)", getId(), this.conferenceTerminationCause, cause));
		return false;
	}

	@Override
    public boolean isDead() {
        return this.state.equals(ConferenceState.Ended);
    }

	public int getMaxNumberOfParticipants() {
		return maxNumberOfParticipants;
	}

	public boolean isMaxNumberOfParticipants() {
		if (maxNumberOfParticipants == 0)
			return false;
		return getNumberOfActiveParticipants() == getMaxNumberOfParticipants();
	}

	public long getMaxDurationInMinutes() {
		return maxDurationInMinutes;
	}

	public ScheduledFuture<?> getFuture() {
		return future;
	}

	public void setFuture(ScheduledFuture<?> aFuture) {
		this.future = aFuture;
	}

    @Override
    public Map<String, Object> getTransients() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(FUTURE, this.getFuture());
        return result;
    }

    @Override
    public void setTransients(Map<String, Object> m) {
        if (m.containsKey(FUTURE)) this.setFuture((ScheduledFuture<?>)m.get(FUTURE));

    }

	public boolean isFlipFlopForSimulatingConcurrencyFailures() {
		return flipFlopForSimulatingConcurrencyFailures;
	}

	public void setFlipFlopForSimulatingConcurrencyFailures(boolean aFlipFlopForSimulatingConcurrencyFailures) {
		this.flipFlopForSimulatingConcurrencyFailures = aFlipFlopForSimulatingConcurrencyFailures;
	}

	public String getMediaServerAddress() {
		return mediaServerAddress;
	}
}
