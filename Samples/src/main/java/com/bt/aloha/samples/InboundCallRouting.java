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
import com.bt.aloha.callleg.InboundCallLegBean;
import com.bt.aloha.callleg.InboundCallLegListener;
import com.bt.aloha.callleg.OutboundCallLegBean;
import com.bt.aloha.callleg.event.CallLegConnectedEvent;
import com.bt.aloha.callleg.event.CallLegConnectionFailedEvent;
import com.bt.aloha.callleg.event.CallLegDisconnectedEvent;
import com.bt.aloha.callleg.event.CallLegRefreshCompletedEvent;
import com.bt.aloha.callleg.event.CallLegTerminatedEvent;
import com.bt.aloha.callleg.event.CallLegTerminationFailedEvent;
import com.bt.aloha.callleg.event.IncomingCallLegEvent;
import com.bt.aloha.callleg.event.ReceivedCallLegRefreshEvent;
import com.bt.aloha.dialog.event.IncomingAction;
import com.bt.aloha.dialog.event.IncomingResponseCode;

/**
 * decide whether to accept an incoming call based on the SIP URI
 */
public class InboundCallRouting implements InboundCallLegListener, CallListener {

    private ClassPathXmlApplicationContext applicationContext;
    private String callee;
    private OutboundCallLegBean outboundCallLegBean;
    private CallBean callBean;
    private String callId;

    public void listenToIncomingCall(String callee) throws Exception {
        this.callee = callee;

        // load Spring
        applicationContext = new ClassPathXmlApplicationContext("com/bt/aloha/samples/Inbound.ApplicationContext.xml");

        // get required beans
        this.outboundCallLegBean = (OutboundCallLegBean)applicationContext.getBean("outboundCallLegBean");
        this.callBean = (CallBean)applicationContext.getBean("callBean");
        InboundCallLegBean inboundCallLegBean = (InboundCallLegBean)applicationContext.getBean("inboundCallLegBean");

        // make this class a listener to call events
        callBean.addCallListener(this);
        inboundCallLegBean.addInboundCallLegListener(this);
        System.out.println("listening for incoming calls...");
    }

    public void onCallConnected(CallConnectedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
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
        if (arg0.getCallId().equals(this.callId))
            applicationContext.destroy();
    }
    
    public void onCallTerminationFailed(CallTerminationFailedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        applicationContext.destroy();
    }

    public void onIncomingCallLeg(IncomingCallLegEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        String caller = arg0.getFromUri();

        // reject telesales call
        if (caller.contains("telesales")) {
            arg0.setIncomingCallAction(IncomingAction.Reject);
            arg0.setResponseCode(IncomingResponseCode.Decline);
            return;
        }

        // capture the call leg Id
        String incomingDialogId = arg0.getId();

        // indicate that I'll handle the call
        arg0.setIncomingCallAction(IncomingAction.None);

        // create a call to the callee and wait for the CallConnectedEvent
        String callLegId = this.outboundCallLegBean.createCallLeg(URI.create(caller), URI.create(callee));
        this.callId = this.callBean.joinCallLegs(incomingDialogId, callLegId);
        System.out.println("Call Id: " + this.callId);
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
        // once connected, end the program
        applicationContext.destroy();
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

    public static void main(String[] args) throws Exception {
        new InboundCallRouting().listenToIncomingCall("sip:XXXXXXXXXXX@10.238.67.22");
    }
}

