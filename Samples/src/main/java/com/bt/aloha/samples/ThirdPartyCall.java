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

/**
 * ThirdPartyCall with the addition of being a "listener" to events related to calls
 */
public class ThirdPartyCall implements CallListener {

    private ClassPathXmlApplicationContext applicationContext;
    private String callId;

    public void makeCall(String caller, String callee) throws Exception {
        // load Spring
        applicationContext = new ClassPathXmlApplicationContext("com/bt/aloha/samples/ThirdPartyCall.ApplicationContext.xml");

        // get required beans
        OutboundCallLegBean outboundCallLegBean = (OutboundCallLegBean)applicationContext.getBean("outboundCallLegBean");
        CallBean callBean = (CallBean)applicationContext.getBean("callBean");

        // make this class a listener to call events
        callBean.addCallListener(this);

        // create two call legs
        String callLegId1 = outboundCallLegBean.createCallLeg(URI.create(callee), URI.create(caller));
        String callLegId2 = outboundCallLegBean.createCallLeg(URI.create(caller), URI.create(callee));

        // join the call legs
        this.callId = callBean.joinCallLegs(callLegId1, callLegId2);
        System.out.println(this.callId);
    }

    public void onCallConnected(CallConnectedEvent arg0) {
        System.out.println(arg0.getClass().getSimpleName());
        if (arg0.getCallId().equals(this.callId))
            applicationContext.destroy();
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

    public static void main(String[] args) throws Exception {
        new ThirdPartyCall().makeCall("sip:XXXXXXXXXXX@10.238.67.22", "sip:XXXXXXXXXXX@10.238.67.22");
    }
}
