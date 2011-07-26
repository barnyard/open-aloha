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

import java.util.Vector;

import com.bt.aloha.call.CallBean;
import com.bt.aloha.call.CallBeanImpl;
import com.bt.aloha.media.convedia.conference.ConferenceBeanImpl;

public class ConferenceCallPersistencyFixture extends CallPersistencyFixtureBase {
	private String firstPhoneUri;
	private String secondPhoneUri;
    private String conferenceId;
    private Vector<String> participants = new Vector<String>();
    private ConferenceFixture conferenceFixture;
    private long maxDuration = 0;

	static{
		// make sure to load this app ctx
		FixtureApplicationContexts.getInstance().startMockphonesApplicationContext();
	}

	public ConferenceCallPersistencyFixture() {
	}

    public void destroyPersistencyApplicationContext(){
        FixtureApplicationContexts.getInstance().destroyPersistencyApplicationContext();
    }

	public String startApplicationContext(){
		applicationContext = FixtureApplicationContexts.getInstance().startPersistencyApplicationContext();
		return "OK";
	}

	public String destroyAndStartApplicationContext(){
		applicationContext = FixtureApplicationContexts.getInstance().startPersistencyApplicationContext(true, getJvmDownTime());
        setupConferenceFixture();
        conferenceFixture.setConfId(conferenceId);
        conferenceFixture.setOurDialogIds(participants);
        return "OK";
	}

	private void setupConferenceFixture() {
		int activeParticipant = 0;
		if (conferenceFixture != null)
			activeParticipant = conferenceFixture.getActiveParticipant();
		conferenceFixture = new ConferenceFixture(applicationContext);
		conferenceFixture.activeParticipant(activeParticipant);
        conferenceFixture.ipAddressPattern(getIpAddressPattern());
        conferenceFixture.firstPhoneUri(firstPhoneUri);
        conferenceFixture.secondPhoneUri(secondPhoneUri);
        conferenceFixture.waitTimeoutSeconds(getWaitTimeoutSeconds());
        conferenceFixture.setMaxDurationInMinutes(maxDuration);
        
        ConferenceBeanImpl conferenceBean = (ConferenceBeanImpl)applicationContext.getBean("conferenceBean");
        CallBean callBean = (CallBean)applicationContext.getBean("callBean");
        
        if (!((CallBeanImpl)callBean).getCallListeners().contains(conferenceBean))
        	callBean.addCallListener(conferenceBean);
	}
	
	public String createConference(){
		setupConferenceFixture();
		conferenceId = conferenceFixture.createConference();
		return conferenceId;
	}
	
	public void setActiveParticipant(int part){
		conferenceFixture.activeParticipant(part);
	}
	
	public String inviteParticipant(){
		conferenceFixture.createDialog();
		conferenceFixture.inviteParticipant();
		participants = (Vector<String>)conferenceFixture.getOurDialogIds();
		return participants.lastElement();
	}
	
	public String waitForParticipantConnectedEvent() throws Exception {
		return conferenceFixture.waitForParticipantConnectedEvent();
	}
	
	public String waitForConferenceActiveEvent() throws Exception {
		return conferenceFixture.waitForConferenceActiveEvent();
	}
	
	public String endConference(){
		return conferenceFixture.endConference();
	}
	
	public String waitForParticipantTerminatedEvent() throws Exception{
		return conferenceFixture.waitForParticipantTerminatedEvent();
	}
	
	public String waitForParticipantDisconnectedEvent() throws Exception {
		return conferenceFixture.waitForParticipantDisconnectedEvent();
	}
	
	public String waitForConferenceEndedEventWithLastParticipantDisconnected() throws Exception {
		return conferenceFixture.waitForConferenceEndedEventWithLastParticipantDisconnected();
	}
	
	public String waitForConferenceEndedEventWithEndedByApplication() throws Exception{
		return conferenceFixture.waitForConferenceEndedEventWithEndedByApplication();
	}
	
	public String waitForConferenceEndedEventWithMaxDurationExceeded() throws Exception {
		return conferenceFixture.waitForConferenceEndedEventWithMaxDurationExceeded();
	}

	public void setFirstPhoneUri(String firstPhoneUri) {
		this.firstPhoneUri = firstPhoneUri;
	}
	
	public String createConferenceWithOneMinuteDuration(){
		maxDuration = 1;
		return createConference();
	}

	public int numberOfActiveParticipants(){
		return conferenceFixture.numberOfActiveParticipants();
	}
	
	@Override
	public void setWaitTimeoutSeconds(int waitTimeoutSeconds) {
		super.setWaitTimeoutSeconds(waitTimeoutSeconds);
		if (conferenceFixture != null)
			conferenceFixture.waitTimeoutSeconds(waitTimeoutSeconds);
	}

	public String getSecondPhoneUri() {
		return secondPhoneUri;
	}

	public void setSecondPhoneUri(String secondPhoneUri) {
		this.secondPhoneUri = secondPhoneUri;
	}
}
