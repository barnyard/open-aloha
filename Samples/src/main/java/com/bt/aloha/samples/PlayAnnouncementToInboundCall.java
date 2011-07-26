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

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.aloha.call.CallBean;
import com.bt.aloha.call.CallListener;
import com.bt.aloha.call.event.CallConnectedEvent;
import com.bt.aloha.call.event.CallConnectionFailedEvent;
import com.bt.aloha.call.event.CallDisconnectedEvent;
import com.bt.aloha.call.event.CallTerminatedEvent;
import com.bt.aloha.call.event.CallTerminationFailedEvent;
import com.bt.aloha.callleg.InboundCallLegBean;
import com.bt.aloha.callleg.InboundCallLegListener;
import com.bt.aloha.callleg.event.CallLegConnectedEvent;
import com.bt.aloha.callleg.event.CallLegConnectionFailedEvent;
import com.bt.aloha.callleg.event.CallLegDisconnectedEvent;
import com.bt.aloha.callleg.event.CallLegRefreshCompletedEvent;
import com.bt.aloha.callleg.event.CallLegTerminatedEvent;
import com.bt.aloha.callleg.event.CallLegTerminationFailedEvent;
import com.bt.aloha.callleg.event.IncomingCallLegEvent;
import com.bt.aloha.callleg.event.ReceivedCallLegRefreshEvent;
import com.bt.aloha.dialog.event.IncomingAction;
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
 * play an announcement to an inbound call leg and then terminate the leg
 */
public class PlayAnnouncementToInboundCall implements InboundCallLegListener, CallListener, MediaCallListener {

    private ClassPathXmlApplicationContext applicationContext;
    private MediaCallBean mediaCallBean;
    private String mediaFile;
    private String commandId;
    private String incomingDialogId;
    private String mediaCallId;

    public PlayAnnouncementToInboundCall(String mediaFile) {
        this.mediaFile = mediaFile;
    }

    public void listenToIncomingCall() throws Exception {

        // load Spring
        applicationContext = new ClassPathXmlApplicationContext("com/bt/aloha/samples/Inbound.ApplicationContext.xml");

        // get required beans
        mediaCallBean = (MediaCallBean)applicationContext.getBean("mediaCallBean");
        CallBean callBean = (CallBean)applicationContext.getBean("callBean");
        InboundCallLegBean inboundCallLegBean = (InboundCallLegBean)applicationContext.getBean("inboundCallLegBean");

        // make this class a listener to call events
        callBean.addCallListener(this);
        mediaCallBean.addMediaCallListener(this);
        inboundCallLegBean.addInboundCallLegListener(this);
        System.out.println("listening for incoming calls...");
    }

    public void onCallConnected(CallConnectedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());

        // play the announcement and wait for the CallAnnouncementCompletedEvent
        if (arg0.getCallId().equals(this.mediaCallId)) {
            this.commandId = mediaCallBean.playAnnouncement(this.mediaCallId, this.mediaFile);
            System.out.println("Command Id: " + this.commandId);
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
        if (arg0.getCallId().equals(this.mediaCallId))
            applicationContext.destroy();
    }

    public void onCallTerminationFailed(CallTerminationFailedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        applicationContext.destroy();
    }

    public void onCallAnnouncementCompleted(CallAnnouncementCompletedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());

        // terminate the call and wait for the CallTerminatedEvent
        if (arg0.getMediaCommandId().equals(this.commandId))
            this.mediaCallBean.terminateMediaCall(this.mediaCallId);
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

    public void onIncomingCallLeg(IncomingCallLegEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        // capture the call leg Id
        this.incomingDialogId = arg0.getId();

        // indicate that I'll handle the call
        arg0.setIncomingCallAction(IncomingAction.None);

        // create a media call and wait for the CallConnectedEvent
        this.mediaCallId = this.mediaCallBean.createMediaCall(this.incomingDialogId);
        System.out.println("Media Call Id: " + this.mediaCallId);
    }

    public void onCallLegConnected(CallLegConnectedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
    }

    public void onCallLegConnectionFailed(CallLegConnectionFailedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        applicationContext.destroy();
    }

    public void onCallLegDisconnected(CallLegDisconnectedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        applicationContext.destroy();
    }

    public void onCallLegRefreshCompleted(CallLegRefreshCompletedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
    }

    public void onCallLegTerminated(CallLegTerminatedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        applicationContext.destroy();
    }

    public void onCallLegTerminationFailed(CallLegTerminationFailedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        applicationContext.destroy();
    }

    public void onReceivedCallLegRefresh(ReceivedCallLegRefreshEvent arg0) {
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
            new PlayAnnouncementToInboundCall("/provisioned/behave.wav").listenToIncomingCall();
    }
}
