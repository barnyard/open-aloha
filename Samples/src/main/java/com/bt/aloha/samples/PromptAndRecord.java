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

import com.bt.aloha.call.CallBean;
import com.bt.aloha.call.CallListener;
import com.bt.aloha.call.event.CallConnectedEvent;
import com.bt.aloha.call.event.CallConnectionFailedEvent;
import com.bt.aloha.call.event.CallDisconnectedEvent;
import com.bt.aloha.call.event.CallTerminatedEvent;
import com.bt.aloha.call.event.CallTerminationFailedEvent;
import com.bt.aloha.callleg.OutboundCallLegBean;
import com.bt.aloha.media.MediaCallBean;
import com.bt.aloha.media.MediaCallListener;
import com.bt.aloha.media.PromptAndRecordCommand;
import com.bt.aloha.media.event.call.CallAnnouncementCompletedEvent;
import com.bt.aloha.media.event.call.CallAnnouncementFailedEvent;
import com.bt.aloha.media.event.call.CallAnnouncementTerminatedEvent;
import com.bt.aloha.media.event.call.CallDtmfGenerationCompletedEvent;
import com.bt.aloha.media.event.call.CallDtmfGenerationFailedEvent;
import com.bt.aloha.media.event.call.CallPromptAndCollectDigitsCompletedEvent;
import com.bt.aloha.media.event.call.CallPromptAndCollectDigitsFailedEvent;
import com.bt.aloha.media.event.call.CallPromptAndCollectDigitsTerminatedEvent;
import com.bt.aloha.media.event.call.CallPromptAndRecordCompletedEvent;
import com.bt.aloha.media.event.call.CallPromptAndRecordFailedEvent;
import com.bt.aloha.media.event.call.CallPromptAndRecordTerminatedEvent;

/**
 * prompt and record and then play back the recording
 */
public class PromptAndRecord implements CallListener, MediaCallListener {

    private static final String TEMP_CLIP_FILE = "file://mnt/172.25.58.146/audio/robustness/temp.wav";
    private ClassPathXmlApplicationContext applicationContext;
    private MediaCallBean mediaCallBean;
    private String callId;
    private String mediaFile;
    private String commandId1;
    private String commandId2;

    public PromptAndRecord(String mediaFile) {
        this.mediaFile = mediaFile;
    }

    public void promptAndRecord(String callee) throws Exception {

        // load Spring
        applicationContext = new ClassPathXmlApplicationContext("com/bt/aloha/samples/MediaCall.ApplicationContext.xml");

        // get required beans
        OutboundCallLegBean outboundCallLegBean = (OutboundCallLegBean)applicationContext.getBean("outboundCallLegBean");
        mediaCallBean = (MediaCallBean)applicationContext.getBean("mediaCallBean");
        CallBean callBean = (CallBean)applicationContext.getBean("callBean");

        // make this class a listener to call events
        callBean.addCallListener(this);
        mediaCallBean.addMediaCallListener(this);

        // create the call leg
        String callLegId = outboundCallLegBean.createCallLeg(URI.create("sip:media@bt.com"), URI.create(callee));

        // create the media call and wait for the CallConnectedEvent
        this.callId = mediaCallBean.createMediaCall(callLegId);
        System.out.println(this.callId);
    }

    public void onCallConnected(CallConnectedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());

        // prompt and record and wait for the PromptAndRecordCompletedEvent
        if (arg0.getCallId().equals(this.callId)) {
            PromptAndRecordCommand command =
                new PromptAndRecordCommand(this.mediaFile, true, TEMP_CLIP_FILE, false, "audio/wav", 10, 5, 2, '#');
            this.commandId1 = mediaCallBean.promptAndRecord(this.callId, command);
            System.out.println(this.commandId1);
        }
    }

    public void onCallConnectionFailed(CallConnectionFailedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        applicationContext.destroy();
    }

    public void onCallDisconnected(CallDisconnectedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        applicationContext.destroy();
    }

    public void onCallTerminated(CallTerminatedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        applicationContext.destroy();
    }

    public void onCallTerminationFailed(CallTerminationFailedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        applicationContext.destroy();
    }

    public void onCallAnnouncementCompleted(CallAnnouncementCompletedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        if (arg0.getMediaCommandId().equals(this.commandId2)) {
            this.mediaCallBean.terminateMediaCall(this.callId);
        }
    }

    public void onCallAnnouncementFailed(CallAnnouncementFailedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        applicationContext.destroy();
    }

    public void onCallAnnouncementTerminated(CallAnnouncementTerminatedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        applicationContext.destroy();
    }

    public void onCallDtmfGenerationCompleted(CallDtmfGenerationCompletedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        applicationContext.destroy();
    }

    public void onCallDtmfGenerationFailed(CallDtmfGenerationFailedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        applicationContext.destroy();
    }

    public void onCallPromptAndCollectDigitsCompleted(CallPromptAndCollectDigitsCompletedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
    }

    public void onCallPromptAndCollectDigitsFailed(CallPromptAndCollectDigitsFailedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        applicationContext.destroy();
    }

    public void onCallPromptAndCollectDigitsTerminated(CallPromptAndCollectDigitsTerminatedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        applicationContext.destroy();
    }

    public void onCallPromptAndRecordCompleted(CallPromptAndRecordCompletedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        if (arg0.getMediaCommandId().equals(this.commandId1)) {
            System.out.println("Recording result: " + arg0.getRecordResult());
            System.out.println("Recording time: " + arg0.getRecordingLengthMillis());

            // now play back the recoding
            this.commandId2 = this.mediaCallBean.playAnnouncement(this.callId, TEMP_CLIP_FILE);
            System.out.println(this.commandId2);
        }
    }

    public void onCallPromptAndRecordFailed(CallPromptAndRecordFailedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
    }

    public void onCallPromptAndRecordTerminated(CallPromptAndRecordTerminatedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
    }

    public static void main(String[] args) throws Exception {
        new PromptAndRecord("file://mnt/172.25.58.146/audio/robustness/RecordYourMessage.wav").promptAndRecord("sip:XXXXXXX@132.146.185.199");
    }
}

