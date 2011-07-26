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
 * Setup a ThirdPartyCall, and after 5 seconds play an announcment to one of the call legs and connect it to another number
 */
public class DynamicCallLegs implements CallListener, MediaCallListener {

    private ClassPathXmlApplicationContext applicationContext;
    private String callId;
    private String mediaFile;
    private MediaCallBean mediaCallBean;
    private String callLegId2;
    private String mediaCallId;
    private String commandId;
    private String transferTo;
    private OutboundCallLegBean outboundCallLegBean;
    private String callee;
    private CallBean callBean;
    private Object transferCallId;

    public DynamicCallLegs(String mediaFile, String transferTo) {
        this.mediaFile = mediaFile;
        this.transferTo = transferTo;
    }

    public void makeCall(String caller, String callee) throws Exception {
        this.callee = callee;

        // load Spring
        this.applicationContext = new ClassPathXmlApplicationContext("com/bt/aloha/samples/MediaCall.ApplicationContext.xml");

        // get required beans
        this.outboundCallLegBean = (OutboundCallLegBean)applicationContext.getBean("outboundCallLegBean");
        this.mediaCallBean = (MediaCallBean)applicationContext.getBean("mediaCallBean");
        this.callBean = (CallBean)applicationContext.getBean("callBean");

        // make this class a listener to call events
        this.callBean.addCallListener(this);
        this.mediaCallBean.addMediaCallListener(this);

        // create two call legs
        String callLegId1 = this.outboundCallLegBean.createCallLeg(URI.create(callee), URI.create(caller));
        this.callLegId2 = this.outboundCallLegBean.createCallLeg(URI.create(caller), URI.create(callee));

        // join the call legs
        System.out.println(String.format("connecting %s and %s", caller, callee));
        this.callId = this.callBean.joinCallLegs(callLegId1, callLegId2);
        System.out.println(this.callId);
    }

    public void onCallConnected(CallConnectedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        if (arg0.getCallId().equals(this.callId)) {
            // sleep for 5 seconds
            System.out.println("waiting 5 seconds");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // create media call
            this.mediaCallId = this.mediaCallBean.createMediaCall(this.callLegId2);
            System.out.println("created media call: " + this.mediaCallId);
        }
        if (arg0.getCallId().equals(this.mediaCallId)) {
            // play announcement and wait for completion event
            System.out.println("playing announcement to " + this.callee);
            this.commandId = this.mediaCallBean.playAnnouncement(this.mediaCallId, this.mediaFile);
            System.out.println("command Id: " + this.commandId);
        }
        if (arg0.getCallId().equals(this.transferCallId)) {
            applicationContext.destroy();
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
        if (arg0.getCallId().equals(this.transferCallId))
            applicationContext.destroy();
    }
    
    public void onCallTerminationFailed(CallTerminationFailedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        applicationContext.destroy();
    }

    public void onCallAnnouncementCompleted(CallAnnouncementCompletedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        if (arg0.getMediaCommandId().equals(this.commandId)) {
            System.out.println(String.format("transfering %s to %s", this.callee, this.transferTo));
            String callLegId3 = this.outboundCallLegBean.createCallLeg(URI.create(this.callee), URI.create(this.transferTo));
            // join the call legs
            this.transferCallId = this.callBean.joinCallLegs(callLegId2, callLegId3);
        }
    }

    public void onCallAnnouncementFailed(CallAnnouncementFailedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
    }

    public void onCallAnnouncementTerminated(CallAnnouncementTerminatedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
    }

    public void onCallDtmfGenerationCompleted(CallDtmfGenerationCompletedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
    }

    public void onCallDtmfGenerationFailed(CallDtmfGenerationFailedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
    }

    public void onCallPromptAndCollectDigitsCompleted(CallPromptAndCollectDigitsCompletedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
    }

    public void onCallPromptAndCollectDigitsFailed(CallPromptAndCollectDigitsFailedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
    }

    public void onCallPromptAndCollectDigitsTerminated(CallPromptAndCollectDigitsTerminatedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
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
        new DynamicCallLegs("/provisioned/behave.wav", "sip:XXXXXXXXXXXX@10.238.67.22").makeCall("sip:XXXXXXXXXXX@10.238.67.22", "sip:XXXXXXXXXXX@10.238.67.22");
    }
}
