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

import java.util.concurrent.CountDownLatch;

public class MediaCallPersistencyFixture extends CallPersistencyFixtureBase {
	private String firstPhoneUri;
    private MediaCallFixture mediaCallFixture;
    private String audioFileUri;
    private String mediaCommandId;

	static{
		// make sure to load this app ctx
		FixtureApplicationContexts.getInstance().startMockphonesApplicationContext();
	}

	public MediaCallPersistencyFixture() {
	}

    public void audioFileUri(String aAudioFileUri) {
        this.audioFileUri = aAudioFileUri;
    }

    public String createFirstDialog() {
        mediaCallFixture = new MediaCallFixture(applicationContext);
        mediaCallFixture.ipAddressPattern(getIpAddressPattern());
        mediaCallFixture.firstPhoneUri(firstPhoneUri);
        mediaCallFixture.waitTimeoutSeconds(getWaitTimeoutSeconds());
        mediaCallFixture.audioFileUri(this.audioFileUri);
        mediaCallFixture.createFirstDialog();
        mediaCallFixture.latch = new CountDownLatch(0);
        return "OK";
    }

    public String createMediaCall() {
        String result = mediaCallFixture.createMediaCall();
        setCallId(mediaCallFixture.getMediaCallId());
        return result;
    }

    public String terminateMediaCall() {
        if (getCallId() == null)
            return "mediaCallId is null - maybe the call hasn't been initiated!";
        try {
            mediaCallFixture.terminateCall();
        } catch (Exception e) {
            e.printStackTrace();
            return "call not terminated: " + e.getMessage();
        }
        return "OK";
    }

    public String waitForMediaCallTerminatedEventWithTerminatedByApplication() throws Exception {
        return mediaCallFixture.waitForCallTerminatedEventWithTerminatedByApplication();
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
        mediaCallFixture = new MediaCallFixture(applicationContext);
        mediaCallFixture.ipAddressPattern(getIpAddressPattern());
        mediaCallFixture.firstPhoneUri(firstPhoneUri);
        mediaCallFixture.waitTimeoutSeconds(getWaitTimeoutSeconds());
        mediaCallFixture.audioFileUri(this.audioFileUri);
        mediaCallFixture.setMediaCallId(getCallId());
        mediaCallFixture.latch = new CountDownLatch(0);
        if (null != getCallId()) {
            mediaCallFixture.callIds.add(getCallId());
            System.out.println("################################# " + mediaCallFixture.callIds.size());
        }
        if (null != this.mediaCommandId)
            mediaCallFixture.commandIds.add(this.mediaCommandId);
        return "OK";
	}


    public String waitForMediaCallConnectedEvent() {
        try {
            return mediaCallFixture.waitForCallConnectedEvent();
        } catch (Exception e) {
            return "Exception: " + e.getMessage();
        }
    }

    public String playAnnouncement() {
        this.mediaCommandId = mediaCallFixture.playAnnouncement();
        return this.mediaCommandId;
    }

    public String waitForAnnouncementCompletedEvent() {
        try {
            return mediaCallFixture.waitForAnnouncementCompletedEvent();
        } catch (Exception e) {
            return "Exception: " + e.getMessage();
        }
    }

	public void setFirstPhoneUri(String firstPhoneUri) {
		this.firstPhoneUri = firstPhoneUri;
	}

}
