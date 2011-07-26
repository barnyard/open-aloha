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
package com.bt.sdk.callcontrol.demo.problem;

import java.net.URI;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.sdk.callcontrol.sip.call.CallBean;
import com.bt.sdk.callcontrol.sip.call.CallListener;
import com.bt.sdk.callcontrol.sip.call.event.CallConnectedEvent;
import com.bt.sdk.callcontrol.sip.call.event.CallConnectionFailedEvent;
import com.bt.sdk.callcontrol.sip.call.event.CallDisconnectedEvent;
import com.bt.sdk.callcontrol.sip.call.event.CallTerminatedEvent;
import com.bt.sdk.callcontrol.sip.callleg.InboundCallLegListener;
import com.bt.sdk.callcontrol.sip.callleg.OutboundCallLegBean;
import com.bt.sdk.callcontrol.sip.callleg.event.CallLegConnectedEvent;
import com.bt.sdk.callcontrol.sip.callleg.event.CallLegConnectionFailedEvent;
import com.bt.sdk.callcontrol.sip.callleg.event.CallLegDisconnectedEvent;
import com.bt.sdk.callcontrol.sip.callleg.event.CallLegRefreshCompletedEvent;
import com.bt.sdk.callcontrol.sip.callleg.event.CallLegTerminatedEvent;
import com.bt.sdk.callcontrol.sip.callleg.event.CallLegTerminationFailedEvent;
import com.bt.sdk.callcontrol.sip.callleg.event.IncomingCallLegEvent;
import com.bt.sdk.callcontrol.sip.callleg.event.ReceivedCallLegRefreshEvent;
import com.bt.sdk.callcontrol.sip.dialog.event.IncomingAction;

public class ProblemBeanImpl implements CallListener, InboundCallLegListener {
	private Log log = LogFactory.getLog(this.getClass());

	private URI toURI = URI.create("sip:07918029610@10.238.67.22");
	private OutboundCallLegBean outboundCallLegBean;
    private CallBean callBean;

	public ProblemBeanImpl() {
	}

    public void onIncomingCallLeg(IncomingCallLegEvent arg0) {
        String incomingDialogId = arg0.getId();
        String incomingUri = arg0.getFromUri();
        arg0.setIncomingCallAction(IncomingAction.None);

        String outboundCallLegId = outboundCallLegBean.createCallLeg(URI.create(incomingUri), toURI);
        String callId = callBean.joinCallLegs(incomingDialogId, outboundCallLegId);
        print(callId);
    }

	public void onCallConnected(CallConnectedEvent arg0) {
        print(arg0.getClass().getSimpleName());
	}

	public void onCallConnectionFailed(CallConnectionFailedEvent arg0) {
        print(arg0.getClass().getSimpleName());
	}

	public void onCallDisconnected(CallDisconnectedEvent arg0) {
        print(arg0.getClass().getSimpleName());
	}

	public void onCallTerminated(CallTerminatedEvent arg0) {
        print(arg0.getClass().getSimpleName());
	}

	public void setCallBean(CallBean callBean) {
		this.callBean = callBean;
	}

    public void setOutboundCallLegBean(OutboundCallLegBean outboundCallLegBean) {
        this.outboundCallLegBean = outboundCallLegBean;
    }

	private void print(String message) {
		log.debug("===================================================================");
		log.debug(message);
		log.debug("===================================================================");
	}

	public void onCallLegConnected(CallLegConnectedEvent arg0) {
        print(arg0.getClass().getSimpleName());
	}

	public void onCallLegConnectionFailed(CallLegConnectionFailedEvent arg0) {
        print(arg0.getClass().getSimpleName());
	}

	public void onCallLegDisconnected(CallLegDisconnectedEvent arg0) {
        print(arg0.getClass().getSimpleName());
	}

	public void onCallLegRefreshCompleted(CallLegRefreshCompletedEvent arg0) {
        print(arg0.getClass().getSimpleName());
	}

	public void onCallLegTerminated(CallLegTerminatedEvent arg0) {
        print(arg0.getClass().getSimpleName());
	}

	public void onCallLegTerminationFailed(CallLegTerminationFailedEvent arg0) {
        print(arg0.getClass().getSimpleName());
	}

	public void onReceivedCallLegRefresh(ReceivedCallLegRefreshEvent arg0) {
        print(arg0.getClass().getSimpleName());
	}
}
