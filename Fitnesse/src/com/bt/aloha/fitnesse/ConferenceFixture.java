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

import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

import com.bt.aloha.call.state.ReadOnlyCallInfo;
import com.bt.aloha.media.conference.collections.ConferenceCollection;
import com.bt.aloha.media.conference.event.ConferenceActiveEvent;
import com.bt.aloha.media.conference.event.ConferenceEndedEvent;
import com.bt.aloha.media.conference.event.ParticipantConnectedEvent;
import com.bt.aloha.media.conference.event.ParticipantDisconnectedEvent;
import com.bt.aloha.media.conference.event.ParticipantFailedEvent;
import com.bt.aloha.media.conference.event.ParticipantTerminatedEvent;
import com.bt.aloha.media.conference.state.ConferenceInfo;
import com.bt.aloha.media.conference.state.ConferenceInformation;
import com.bt.aloha.media.conference.state.ConferenceTerminationCause;
import com.bt.aloha.media.convedia.conference.ConferenceBean;
import com.bt.aloha.media.convedia.conference.ConferenceBeanImpl;
import com.bt.aloha.media.convedia.conference.ConferenceListener;

public class ConferenceFixture extends MediaCallFixture implements ConferenceListener {
	private ConferenceBean conferenceBean;
    private Semaphore conferenceActiveSemaphore = new Semaphore(0);
    private Semaphore conferenceEndedSemaphore = new Semaphore(0);
    private Semaphore participantConnectedSemaphore = new Semaphore(0);
    private Semaphore participantTerminatedSemaphore = new Semaphore(0);
    private Semaphore participantDisconnectedSemaphore = new Semaphore(0);
    private Semaphore participantFailedSemaphore = new Semaphore(0);
    private List<String> conferenceActiveEvents = new Vector<String>();
    private Hashtable<String, ConferenceEndedEvent> conferenceEndedEvents = new Hashtable<String, ConferenceEndedEvent>();
    private List<String> participantConnectedEvents = new Vector<String>();
    private List<String> participantTerminatedEvents = new Vector<String>();
    private List<String> participantDisconnectedEvents = new Vector<String>();
    private List<String> participantFailedEvents = new Vector<String>();
    private List<String> ourDialogIds = new Vector<String>();
    private String confId;
    private int activeParticipant;
	private ConferenceCollection conferenceCollection;
	private int maxNumberOfParticipants = 10;
	private long maxDurationInMinutes = 0;
	private int lastConferenceCollectionSize;
	private ApplicationContext applicationContext;
	private Log log = LogFactory.getLog(this.getClass());

    public ConferenceFixture(ApplicationContext appContext){
    	super();
    	applicationContext = appContext;
    	
    	conferenceBean = (ConferenceBean)applicationContext.getBean("conferenceBean");
    	conferenceCollection = (ConferenceCollection)applicationContext.getBean("conferenceCollection");
    	List<ConferenceListener> conferenceListeners = new ArrayList<ConferenceListener>();
    	conferenceListeners.add(this);
    	((ConferenceBeanImpl)conferenceBean).setConferenceListeners(conferenceListeners);
    }
    
    public ConferenceFixture() {
    	this(FixtureApplicationContexts.getInstance().startMediaApplicationContext());
    }
    
    public boolean conferenceIdSet() {
    	return getConfId() != null && !getConfId().equals("");
    }

    public void conferenceId(String val) {
    	setConfId(val);
    }

    public void activeParticipant(int part) {
        this.activeParticipant = part;
    }

    public void setMaxNumberParticipants(int val) {
    	this.maxNumberOfParticipants = val;
    }

    public void setMaxDurationInMinutes(long val) {
    	this.maxDurationInMinutes  = val;
    }

    public int maxNumberParticipants() {
    	return conferenceCollection.get(getConfId()).getMaxNumberOfParticipants();
    }

    public long maxDurationInMinutes() {
    	return conferenceCollection.get(getConfId()).getMaxDurationInMinutes();
    }

    public String createConference() {
    	try {
	    	setConfId(conferenceBean.createConference(maxNumberOfParticipants, maxDurationInMinutes));
	    	return getConfId();
    	} catch (Exception e) {
    		return e.getClass().getSimpleName();
    	}
    }

    public String endConference() {
    	try {
    		conferenceBean.endConference(getConfId());
    		return "OK";
    	} catch (Exception e) {
    		return "Exception:" + e.getClass().getSimpleName();
    	}
    }

    public String createDialog() {
    	try {
    		switch (activeParticipant) {
		        case 1:
		            firstDialogId = conferenceBean.createParticipantCallLeg(getConfId(), URI.create(firstPhoneUri));
		            ourDialogIds.add(0, firstDialogId);
		            break;
		        case 2:
		            secondDialogId = conferenceBean.createParticipantCallLeg(getConfId(), URI.create(secondPhoneUri));
		            ourDialogIds.add(1, secondDialogId);
		            break;
		        case 3:
		            thirdDialogId = conferenceBean.createParticipantCallLeg(getConfId(), URI.create(thirdPhoneUri));
		            ourDialogIds.add(2, thirdDialogId);
		            break;
	        }
    		return "OK";
		} catch (Exception e) {
			return "Exception:" + e.getClass().getSimpleName();
		}
    }

    public boolean dialogNotNull() {
    	return ourDialogIds.get(activeParticipant-1) != null && !ourDialogIds.get(activeParticipant-1).equals("");
    }

    public String inviteParticipant() {
    	try {
    		conferenceBean.inviteParticipant(getConfId(), ourDialogIds.get(activeParticipant-1));
    		return "OK";
    	} catch (Exception e) {
    		return "Exception:" + e.getClass().getSimpleName();
    	}
    }

    public String terminateParticipant() {
    	try {
    		conferenceBean.terminateParticipant(getConfId(), ourDialogIds.get(activeParticipant-1));
    		return "OK";
    	} catch (Exception e) {
    		return "Exception:" + e.getClass().getSimpleName();
    	}
    }

    public String conferenceState () {
    	return conferenceBean.getConferenceInformation(getConfId()).getConferenceState().name();
    }

    public int numberOfActiveParticipants () {
    	return conferenceBean.getConferenceInformation(getConfId()).getNumberOfActiveParticipants();
    }

    public int numberOfParticipants () {
    	return conferenceBean.getConferenceInformation(getConfId()).getNumberOfParticipants();
    }

    public String conferenceTerminationCause() {
    	ConferenceInformation callInformation = conferenceBean.getConferenceInformation(getConfId());
    	return callInformation.getConferenceTerminationCause().toString();
    }

    public boolean createTimeNotNull () {
    	return conferenceBean.getConferenceInformation(getConfId()).getCreateTime().getTimeInMillis() >= 0;
    }

    public boolean startTimeNotNull () {
    	return conferenceBean.getConferenceInformation(getConfId()).getStartTime().getTimeInMillis() >= 0;
    }

    public boolean endTimeNotNull () {
    	return conferenceBean.getConferenceInformation(getConfId()).getEndTime().getTimeInMillis() >= 0;
    }

    public boolean durationGreaterThanZero() {
    	return conferenceBean.getConferenceInformation(getConfId()).getDuration() > 0;
    }

    public int duration() {
    	return conferenceBean.getConferenceInformation(getConfId()).getDuration();
    }

    public String participantState() {
    	String dialogId = ourDialogIds.get(activeParticipant-1);
    	ReadOnlyCallInfo callInfo = callCollection.getCurrentCallForCallLeg(dialogId);
    	return conferenceBean.getConferenceInformation(getConfId()).getParticipantState(callInfo.getId()).name();
    }

    public void conferenceMaxTTL(int val) {
		conferenceCollection.setMaxTimeToLive(val);
	}

    public void housekeepConferenceCollection() {
		conferenceCollection.housekeep();
	}

	public void storeConferenceCollectionSize() {
		lastConferenceCollectionSize = conferenceCollection.size();
	}

	public int conferenceCollectionSize() {
		return conferenceCollection.size();
	}

	public int conferenceCollectionSizeDelta() {
		return conferenceCollection.size() - lastConferenceCollectionSize;
	}

	public String cleanConferenceCollection() {
		ConcurrentMap<String, ConferenceInfo> conferences = conferenceCollection.getAll();
		for (String conferenceId: conferences.keySet()) {
			conferenceCollection.remove(conferenceId);
		}
		if (conferenceCollection.size() == 0)
			return "OK";
		else
			return "Failed";
	}

	///////////////////////////////////////////


    public String waitForParticipantConnectedEvent() throws Exception {
        String targetDialogId = ourDialogIds.get(activeParticipant-1);
        if (participantConnectedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS)) {
            if (participantConnectedEvents.contains(targetDialogId)) {
                return "OK";
            } else {
                return participantConnectedEvents.toString();
            }
        }
        return "No event";
    }

    public String waitForParticipantTerminatedEvent() throws Exception {
        if (participantTerminatedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS)) {
	        for (String dialogId : ourDialogIds)
	    		if (participantTerminatedEvents.contains(dialogId))
	    			return "OK";
            return participantTerminatedEvents.toString();
        }
        return "No event";
    }

    public String waitForParticipantDisconnectedEvent() throws Exception {
    	String targetDialogId = ourDialogIds.get(activeParticipant-1);
    	if (participantDisconnectedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS)) {
    		if (participantDisconnectedEvents.contains(targetDialogId)) {
    			return "OK";
    		} else {
    			return participantDisconnectedEvents.toString();
    		}
    	}
    	return "No event";
    }

    public String waitForParticipantFailedEvent() throws Exception {
    	String targetDialogId = ourDialogIds.get(activeParticipant-1);
    	if (participantFailedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS)) {
    		if (participantFailedEvents.contains(targetDialogId)) {
    			return "OK";
    		} else {
    			return participantFailedEvents.toString();
    		}
    	}
    	return "No event";
    }

    public String waitForConferenceActiveEvent() throws Exception {
        if (conferenceActiveSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS)) {
            if (conferenceActiveEvents.contains(getConfId())) {
                return "OK";
            } else {
                return conferenceActiveEvents.toString();
            }
        }
        return "No event";
    }

    private String waitForConferenceEndedEvent(ConferenceTerminationCause conferenceTerminationCause, boolean zeroDuration) throws Exception {
        if (conferenceEndedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS)) {
        	ConferenceEndedEvent conferenceEndedEvent = conferenceEndedEvents.get(getConfId());
        	if (conferenceEndedEvent != null && conferenceEndedEvent.getConferenceTerminationCause().equals(conferenceTerminationCause)) {
        		if ((zeroDuration && conferenceEndedEvent.getDuration() == 0) || (!zeroDuration && conferenceEndedEvent.getDuration() > 0))
        			return "OK";
    			return String.format("Conference duration: %d seconds", conferenceEndedEvent.getDuration());
        	} else if (conferenceEndedEvent != null)
        		return conferenceEndedEvent.getConferenceTerminationCause().toString();
        	else return conferenceEndedEvents.keySet().toString();
        }
        return "No event";
    }

    public String waitForConferenceEndedEventWithLastParticipantDisconnected() throws Exception {
    	return waitForConferenceEndedEvent(ConferenceTerminationCause.LastParticipantDisconnected, false);
    }

    public String waitForConferenceEndedEventWithLastParticipantTerminated() throws Exception {
    	return waitForConferenceEndedEvent(ConferenceTerminationCause.LastParticipantTerminated, false);
    }

    public String waitForConferenceEndedEventWithEndedByApplication() throws Exception {
    	return waitForConferenceEndedEvent(ConferenceTerminationCause.EndedByApplication, !durationGreaterThanZero());
    }

    public String waitForConferenceEndedEventWithHousekept() throws Exception {
    	return waitForConferenceEndedEvent(ConferenceTerminationCause.Housekept, !durationGreaterThanZero());
    }

    public String waitForConferenceEndedEventWithMaxDurationExceeded() throws Exception {
    	return waitForConferenceEndedEvent(ConferenceTerminationCause.MaximumDurationExceeded, !durationGreaterThanZero());
    }

    public void onConferenceActive(ConferenceActiveEvent conferenceActiveEvent) {
        String eventConfId = conferenceActiveEvent.getConferenceId();
        if (eventConfId.equals(getConfId())) {
            this.conferenceActiveEvents.add(eventConfId);
            this.conferenceActiveSemaphore.release();
        }
    }

    public void onConferenceEnded(ConferenceEndedEvent conferenceEndedEvent) {
    	log.debug(String.format("Conference Id = %s, expected id = %s, termination cause = %s", conferenceEndedEvent.getConferenceId(), getConfId(), conferenceEndedEvent.getConferenceTerminationCause()));
        String eventConfId = conferenceEndedEvent.getConferenceId();
        if (eventConfId.equals(getConfId())) {
            this.conferenceEndedEvents.put(eventConfId, conferenceEndedEvent);
            this.conferenceEndedSemaphore.release();
        }
    }

    public void onParticipantConnected(ParticipantConnectedEvent participantConnectedEvent) {
        String dialogId = participantConnectedEvent.getDialogId();
        if (ourDialogIds.contains(dialogId)) {
            this.participantConnectedEvents.add(dialogId);
            this.participantConnectedSemaphore.release();
        }
    }

    public void onParticipantTerminated(ParticipantTerminatedEvent participantTerminatedEvent) {
    	log.debug(String.format("Participant %s terminated", participantTerminatedEvent.getDialogId()));
        String dialogId = participantTerminatedEvent.getDialogId();
        if (ourDialogIds.contains(dialogId)) {
            this.participantTerminatedEvents.add(dialogId);
            this.participantTerminatedSemaphore.release();
        }
    }

	public void onParticipantDisconnected(ParticipantDisconnectedEvent participantDisconnectedEvent) {
        String dialogId = participantDisconnectedEvent.getDialogId();
        if (ourDialogIds.contains(dialogId)) {
            this.participantDisconnectedEvents.add(dialogId);
            this.participantDisconnectedSemaphore.release();
        }
	}

	public void onParticipantFailed(ParticipantFailedEvent participantFailedEvent) {
		String dialogId = participantFailedEvent.getDialogId();
		if (ourDialogIds.contains(dialogId)) {
			this.participantFailedEvents.add(dialogId);
			this.participantFailedSemaphore.release();
		}
	}

	public void setConfId(String confId) {
		this.confId = confId;
	}

	private String getConfId() {
		return confId;
	}

	public void setOurDialogIds(List<String> ourDialogIds) {
		this.ourDialogIds = ourDialogIds;
	}

	public List<String> getOurDialogIds() {
		return ourDialogIds;
	}

	public int getActiveParticipant() {
		return activeParticipant;
	}
}
