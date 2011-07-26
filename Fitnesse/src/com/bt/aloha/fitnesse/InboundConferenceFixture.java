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
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.callleg.InboundCallLegListener;
import com.bt.aloha.callleg.event.IncomingCallLegEvent;
import com.bt.aloha.dialog.event.IncomingAction;
import com.bt.aloha.eventing.EventFilter;
import com.bt.aloha.media.conference.event.ConferenceActiveEvent;
import com.bt.aloha.media.conference.event.ConferenceEndedEvent;
import com.bt.aloha.media.conference.event.ParticipantConnectedEvent;
import com.bt.aloha.media.conference.event.ParticipantDisconnectedEvent;
import com.bt.aloha.media.conference.event.ParticipantFailedEvent;
import com.bt.aloha.media.conference.event.ParticipantTerminatedEvent;
import com.bt.aloha.media.conference.state.ConferenceTerminationCause;
import com.bt.aloha.media.convedia.conference.ConferenceBean;
import com.bt.aloha.media.convedia.conference.ConferenceBeanImpl;
import com.bt.aloha.media.convedia.conference.ConferenceListener;
import com.bt.aloha.testing.CallLegListenerStubBase;

public class InboundConferenceFixture extends InboundDialogFixture implements ConferenceListener {
    private ConferenceBean conferenceBean;
    private String conferenceId;
    private int activeParticipant;
	private List<String> participantConnectedEvents = new Vector<String>();
	private Semaphore participantConnectedSemaphore = new Semaphore(0);
	private String conferenceActiveEventId;
	private Semaphore conferenceActiveSemaphore = new Semaphore(0);
	private ConferenceEndedEvent conferenceEndedEvent;
	private Semaphore conferenceEndedSemaphore = new Semaphore(0);

    public InboundConferenceFixture() {
        super();
        conferenceBean = (ConferenceBean)FixtureApplicationContexts.getInstance()
        	.startInboundApplicationContext().getBean("conferenceBean");
        List<ConferenceListener> list = new ArrayList<ConferenceListener>();
        list.add(this);
        ((ConferenceBeanImpl)conferenceBean).setConferenceListeners(list);

        inboundApplicationContextBeans.getInboundCallLegBean().addInboundCallLegListener(new InboundConferenceCallLegListener());
    }

	public void activeParticipant(int participant) {
		this.activeParticipant = participant;
	}

    public class InboundConferenceCallLegListener extends CallLegListenerStubBase implements InboundCallLegListener, EventFilter {
        private Log log = LogFactory.getLog(this.getClass());

        public void onIncomingCallLeg(IncomingCallLegEvent e) {
            e.setIncomingCallAction(IncomingAction.None);
            String incomingDialogId = e.getId();
            
            log.debug("Got onIncomingDialog in the InboundConferenceCallLegListener, adding to conference");
            conferenceBean.inviteParticipant(conferenceId, incomingDialogId);
        }

        public boolean shouldDeliverEvent(Object event) {
            return event instanceof IncomingCallLegEvent
               && ((IncomingCallLegEvent) event).getToUri().contains("conference");
        }
    }
    
    public String createConference() {
    	conferenceId = conferenceBean.createConference();
    	return conferenceId;
    }

    public String createDialog() {
    	try {
    		switch (activeParticipant) {
		        case 1:
		            firstDialogId = conferenceBean.createParticipantCallLeg(conferenceId, URI.create(firstPhoneUri));
		            break;
		        case 2:
		            secondDialogId = conferenceBean.createParticipantCallLeg(conferenceId, URI.create(secondPhoneUri));
		            break;
		        case 3:
		            thirdDialogId = conferenceBean.createParticipantCallLeg(conferenceId, URI.create(thirdPhoneUri));
		            break;
	        }
    		return "OK";
		} catch (Exception e) {
			return "Exception:" + e.getClass().getSimpleName();
		}
    }

    private String getDialogId() {
    	switch (activeParticipant) {
	    	case 1:
	    		return firstDialogId;
	    	case 2:
	    		return secondDialogId;
	    	case 3:
	    		return thirdDialogId;
	    	default:
	    		return null;
    	}
    }
    
    public String inviteParticipant() {
    	try {
    		conferenceBean.inviteParticipant(conferenceId, getDialogId());
    		return "OK";
    	} catch (Exception e) {
    		return "Exception:" + e.getClass().getSimpleName();
    	}
    }

    public int numberOfActiveParticipants () {
    	return conferenceBean.getConferenceInformation(conferenceId).getNumberOfActiveParticipants();
    }

    public String endConference() {
		conferenceBean.endConference(conferenceId);
		return "OK";
    }

    public String waitForParticipantConnectedEvent() throws Exception {
	    String targetDialogId = getDialogId();
	    if (participantConnectedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS)) {
	        if (participantConnectedEvents.contains(targetDialogId))
	            return "OK";
            return participantConnectedEvents.toString();
	    }
	    return "No event";
    }

    public String waitForConferenceActiveEvent() throws Exception {
	    if (conferenceActiveSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS)) {
	        if (conferenceActiveEventId.equals(conferenceId))
	            return "OK";
            return conferenceActiveEventId;
	    }
	    return "No event";
    }

    public String waitForConferenceEndedEventWithEndedByApplication() throws Exception {
    	return waitForConferenceEndedEvent(ConferenceTerminationCause.EndedByApplication);
    }

    private String waitForConferenceEndedEvent(ConferenceTerminationCause conferenceTerminationCause) throws Exception {
	    if (conferenceEndedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS)) {
	        if (conferenceEndedEvent.getConferenceId().equals(conferenceId) && conferenceEndedEvent.getConferenceTerminationCause().equals(conferenceTerminationCause))
	            return "OK";
            return conferenceEndedEvent.getConferenceId() + conferenceEndedEvent.getConferenceTerminationCause();
	    }
	    return "No event";
    }

	public void onConferenceActive(ConferenceActiveEvent conferenceActiveEvent) {
		conferenceActiveEventId = conferenceActiveEvent.getConferenceId();
		conferenceActiveSemaphore.release();
	}

	public void onConferenceEnded(ConferenceEndedEvent aConferenceEndedEvent) {
		conferenceEndedEvent = aConferenceEndedEvent;
		conferenceEndedSemaphore.release();
	}

	public void onParticipantConnected(ParticipantConnectedEvent participantConnectedEvent) {
		participantConnectedEvents.add(participantConnectedEvent.getDialogId());
		participantConnectedSemaphore.release();
	}

	public void onParticipantDisconnected(ParticipantDisconnectedEvent participantDisconnectedEvent) {
	}

	public void onParticipantFailed(ParticipantFailedEvent participantFailedEvent) {
	}

	public void onParticipantTerminated(ParticipantTerminatedEvent participantTerminatedEvent) {
	}
}
