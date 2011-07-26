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

 	

 	
 	
 
package com.bt.aloha.samples;

import java.net.URI;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.aloha.media.conference.event.ConferenceActiveEvent;
import com.bt.aloha.media.conference.event.ConferenceEndedEvent;
import com.bt.aloha.media.conference.event.ParticipantConnectedEvent;
import com.bt.aloha.media.conference.event.ParticipantDisconnectedEvent;
import com.bt.aloha.media.conference.event.ParticipantFailedEvent;
import com.bt.aloha.media.conference.event.ParticipantTerminatedEvent;
import com.bt.aloha.media.convedia.conference.ConferenceBean;
import com.bt.aloha.media.convedia.conference.ConferenceListener;

/**
 * ConferenceCall with the addition of being a "listener" to events related to calls
 */
public class ConferenceCall implements ConferenceListener {

    private ClassPathXmlApplicationContext applicationContext;
    private String conferenceId;
    private String[] callees;

    public void makeCall(String[] callees) throws Exception {
        this.callees = callees;

        // load Spring
        applicationContext = new ClassPathXmlApplicationContext("com/bt/aloha/samples/ConferenceCall.ApplicationContext.xml");

        // get required beans
        ConferenceBean conferenceBean = (ConferenceBean) applicationContext.getBean("conferenceBean");

        // make this class a listener to call events
        conferenceBean.addConferenceListener(this);

        // create the conference
        this.conferenceId = conferenceBean.createConference();
        System.out.println("conferenceId: " + this.conferenceId);

        for (String callee : this.callees) {
            // create call legs and invite to conference
            String callLegId = conferenceBean.createParticipantCallLeg(this.conferenceId, URI.create(callee));
            System.out.println("Call Leg Id: " + callLegId);
            conferenceBean.inviteParticipant(this.conferenceId, callLegId);
        }
    }

    public void onConferenceActive(ConferenceActiveEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
    }

    public void onConferenceEnded(ConferenceEndedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        if (arg0.getConferenceId().equals(this.conferenceId))
            this.applicationContext.destroy();
    }

    public void onParticipantConnected(ParticipantConnectedEvent arg0) {
        if (arg0.getConferenceId().equals(this.conferenceId)) {
            System.out.println(String.format("%s participant id: %s",
                    arg0.getClass().getSimpleName(),
                    arg0.getDialogId()));
        }
    }

    public void onParticipantDisconnected(ParticipantDisconnectedEvent arg0) {
        if (arg0.getConferenceId().equals(this.conferenceId)) {
            System.out.println(String.format("%s participant id: %s",
                    arg0.getClass().getSimpleName(),
                    arg0.getDialogId()));
        }
    }

    public void onParticipantFailed(ParticipantFailedEvent arg0) {
        System.out.println(String.format("%s: conference Id: %s, participant Id: %s",
                arg0.getClass().getSimpleName(),
                arg0.getConferenceId(),
                arg0.getDialogId()));
        this.applicationContext.destroy();
    }

    public void onParticipantTerminated(ParticipantTerminatedEvent arg0) {
        if (arg0.getConferenceId().equals(this.conferenceId)) {
            System.out.println(String.format("%s participant id: %s",
                    arg0.getClass().getSimpleName(),
                    arg0.getDialogId()));
        }
    }

    public static void main(String[] args) throws Exception {
        new ConferenceCall().makeCall(new String[] { "sip:01442208294@10.238.67.22",
                "sip:07917024142@10.238.67.22" });
    }
}
