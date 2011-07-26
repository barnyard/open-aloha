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
import com.bt.aloha.media.DtmfCollectCommand;
import com.bt.aloha.media.MediaCallBean;
import com.bt.aloha.media.MediaCallListener;
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
 * prompt and collect to a call leg and then terminate the leg
 */
public class PromptAndCollect implements CallListener, MediaCallListener {

    private ClassPathXmlApplicationContext applicationContext;
    private MediaCallBean mediaCallBean;
    private String callId;
    private String mediaFile;
    private String commandId;

    public PromptAndCollect(String mediaFile) {
        this.mediaFile = mediaFile;
    }

    public void promptAndCollect(String callee) throws Exception {

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

        // prompt and collect and wait for the PromptAncCollectDigitsCompletedEvent
        if (arg0.getCallId().equals(this.callId)) {
            DtmfCollectCommand params = new DtmfCollectCommand(this.mediaFile, true, true, 20, 5, 5, 1);
            this.commandId = mediaCallBean.promptAndCollectDigits(this.callId, params);
            System.out.println(this.commandId);
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
        applicationContext.destroy();
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

        if (arg0.getMediaCommandId().equals(this.commandId)) {
            System.out.println("Digits collected: " + arg0.getDigits());

            // terminate the call and wait for the CallTerminatedEvent
            this.mediaCallBean.terminateMediaCall(this.callId);
        }
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
    }

    public void onCallPromptAndRecordFailed(CallPromptAndRecordFailedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
    }

    public void onCallPromptAndRecordTerminated(CallPromptAndRecordTerminatedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
    }

    public static void main(String[] args) throws Exception {
        new PromptAndCollect("file://mnt/172.25.19.54/uros/clips/prompt123.wav").promptAndCollect("sip:adrian@132.146.185.199");
    }
}
