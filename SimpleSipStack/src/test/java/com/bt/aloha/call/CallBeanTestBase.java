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

 	

 	
 	
 
package com.bt.aloha.call;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;

import javax.sip.message.Response;

import org.junit.Before;

import com.bt.aloha.call.CallBean;
import com.bt.aloha.call.CallBeanImpl;
import com.bt.aloha.call.CallListener;
import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.call.event.AbstractCallEvent;
import com.bt.aloha.call.event.CallConnectedEvent;
import com.bt.aloha.call.event.CallConnectionFailedEvent;
import com.bt.aloha.call.event.CallDisconnectedEvent;
import com.bt.aloha.call.event.CallTerminatedEvent;
import com.bt.aloha.call.event.CallTerminationFailedEvent;
import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.callleg.InboundCallLegBean;
import com.bt.aloha.callleg.InboundCallLegBeanImpl;
import com.bt.aloha.callleg.InboundCallLegListener;
import com.bt.aloha.callleg.OutboundCallLegBean;
import com.bt.aloha.callleg.OutboundCallLegBeanImpl;
import com.bt.aloha.callleg.OutboundCallLegListener;
import com.bt.aloha.callleg.event.CallLegConnectedEvent;
import com.bt.aloha.callleg.event.IncomingCallLegEvent;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.event.IncomingAction;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.eventing.EventFilter;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.stack.SimpleSipStack;
import com.bt.aloha.testing.InboundCallLegListenerStubBase;
import com.bt.aloha.testing.SimpleSipStackPerClassTestCase;
import com.bt.aloha.testing.SipUnitPhone;
import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;

public abstract class CallBeanTestBase extends SimpleSipStackPerClassTestCase implements CallListener {
	protected static final String INBOUND_CALL_SETUP_USING_CALL_BEAN = "inboundcallsetupusingcallbean";
	protected static final String INBOUND_CALL_SETUP_USING_HOLD = "inboundcallsetupusinghold";
	protected List<AbstractCallEvent> eventVector = new Vector<AbstractCallEvent>();
	protected Semaphore connectedSemaphore = new Semaphore(0);
	protected Semaphore connectionFailedSemaphore = new Semaphore(0);
	protected Semaphore disconnectedSemaphore = new Semaphore(0);
	protected Semaphore terminatedSemaphore = new Semaphore(0);
	protected SimpleSipStack simpleSipStack;
	protected OutboundCallLegBean outboundCallLegBean;
	protected InboundCallLegBean inboundCallLegBean;
	protected CallBean callBean;
	protected CallCollection callCollection;
	protected DialogCollection dialogCollection;
	public InboundCallSetupUsingHold inboundCallSetupUsingHold;
    public InboundCallSetupUsingCallBean inboundCallSetupUsingCallBean;
	
	@Before
	public void beforeCallBeanTestBase() { 
		simpleSipStack = (SimpleSipStack)getApplicationContext().getBean("simpleSipStack");
		outboundCallLegBean = (OutboundCallLegBean)getApplicationContext().getBean("outboundCallLegBean");
		inboundCallLegBean = (InboundCallLegBean)getApplicationContext().getBean("inboundCallLegBean");
		callBean = (CallBean)getApplicationContext().getBean("callBean");
		dialogCollection = (DialogCollection)getApplicationContext().getBean("dialogCollection");
		callCollection = (CallCollection)getApplicationContext().getBean("callCollection");
		dialogCollection = (DialogCollection)getApplicationContext().getBean("dialogCollection");
		
        inboundCallSetupUsingHold = new InboundCallSetupUsingHold();
        inboundCallSetupUsingHold.setOutboundCallLegBean(outboundCallLegBean);
        inboundCallSetupUsingHold.setCallBean(callBean);
        inboundCallSetupUsingHold.setCallCollection(callCollection);
        inboundCallSetupUsingHold.setOutboundCallLegEndpoint(getInboundPhoneSipAddress());

        inboundCallSetupUsingCallBean = new InboundCallSetupUsingCallBean();
        inboundCallSetupUsingCallBean.setCallBean(callBean);
        inboundCallSetupUsingCallBean.setOutboundCallLegEndpoint(getInboundPhoneSipAddress());
        
        List<InboundCallLegListener> inboundCallLegListeners = new ArrayList<InboundCallLegListener>();
        inboundCallLegListeners.add(inboundCallSetupUsingHold);
        inboundCallLegListeners.add(inboundCallSetupUsingCallBean);
        inboundCallLegListeners.add((CallBeanImpl)callBean);
        ((InboundCallLegBeanImpl)inboundCallLegBean).setInboundCallLegListeners(inboundCallLegListeners);
        
        List<OutboundCallLegListener> outboundCallLegListeners = new ArrayList<OutboundCallLegListener>();
        outboundCallLegListeners.add((CallBeanImpl)callBean);
        ((OutboundCallLegBeanImpl)outboundCallLegBean).setOutboundCallLegListeners(outboundCallLegListeners);
        
        List<CallListener> callListeners = new ArrayList<CallListener>();
		callListeners.add(this);
		((CallBeanImpl)callBean).setCallListeners(callListeners);
	}
	
	static public class InboundCallSetupUsingHold extends InboundCallLegListenerStubBase implements EventFilter {
		private OutboundCallLegBean outboundCallLegBean;
		private String outboundCallLegEndpoint;
		private CallBean callBean;
		private CallCollection callCollection;
		private AutoTerminateAction autoTerminate = AutoTerminateAction.False;
		protected String inboundDialogId;
		protected String callId;
		private boolean reverseDialogOrder = false;

		public InboundCallSetupUsingHold() {
	        this.callBean = null;
	        this.outboundCallLegBean = null;
	        this.callCollection = null;
	    }

		public void setAutoTerminate(AutoTerminateAction aAutoTerminate) {
			this.autoTerminate = aAutoTerminate;
		}

	    public void setOutboundCallLegEndpoint(String aOutboundCallLegEndpoint) {
			this.outboundCallLegEndpoint = aOutboundCallLegEndpoint;
		}

		public void onIncomingCallLeg(IncomingCallLegEvent e) {
			inboundDialogId = e.getId();
			e.setIncomingCallAction(IncomingAction.PlaceOnHold);
		}

		public void setCallBean(CallBean aCallBean) {
			this.callBean = aCallBean;
		}

		public void setOutboundCallLegBean(OutboundCallLegBean anOutboundDialogBean) {
			this.outboundCallLegBean = anOutboundDialogBean;
		}

		public void setCallCollection(CallCollection callCollection) {
			this.callCollection = callCollection;
		}

		public boolean shouldDeliverEvent(Object event) {
			return event instanceof CallLegConnectedEvent || (event instanceof IncomingCallLegEvent
				&& ((IncomingCallLegEvent)event).getToUri().contains(INBOUND_CALL_SETUP_USING_HOLD));
		}
		
		@Override
		public void onCallLegConnected(CallLegConnectedEvent connectedEvent) {
			if (connectedEvent.getId().equals(inboundDialogId) && callCollection.getCurrentCallForCallLeg(inboundDialogId) == null) {
				String outboundDialogId = null;
					outboundDialogId = outboundCallLegBean.createCallLeg(URI.create("sip:whatever"), URI.create(this.outboundCallLegEndpoint));
				if(!reverseDialogOrder)
					callId = callBean.joinCallLegs(inboundDialogId, outboundDialogId, autoTerminate);
				else
					callId = callBean.joinCallLegs(outboundDialogId, inboundDialogId, autoTerminate);
			}
		}
		
		public void reverseDialogOrder() {
			this.reverseDialogOrder = true;
		}
	}

	// Uses a 'late' OK response to inbound dialog to pass the media received from the outboudn dialog, thus eliminating
	// the need for hold SDP, and potentially a reinvite to the inbound dialog.
	public class InboundCallSetupUsingCallBean extends InboundCallLegListenerStubBase implements EventFilter {
		private String outboundCallLegEndpoint;
		private CallBean callBean;
		protected String outboundDialogId;
		protected String inboundDialogId;
		protected String callId;
		private boolean reverseDialogOrder = false;

		public InboundCallSetupUsingCallBean() {
	        this.callBean = null;
	    }

		public boolean shouldDeliverEvent(Object event) {
			return event instanceof IncomingCallLegEvent
				&& ((IncomingCallLegEvent)event).getToUri().contains(INBOUND_CALL_SETUP_USING_CALL_BEAN);
		}

		public void setOutboundCallLegEndpoint(String aOutboundCallLegEndpoint) {
			this.outboundCallLegEndpoint = aOutboundCallLegEndpoint;
		}

		public void onIncomingCallLeg(IncomingCallLegEvent e) {
			e.setIncomingCallAction(IncomingAction.None);

			inboundDialogId = e.getId();
			if(outboundDialogId == null)
                outboundDialogId = outboundCallLegBean.createCallLeg(URI.create(e.getFromUri()), URI.create(this.outboundCallLegEndpoint));

			if(!reverseDialogOrder)
				callId = callBean.joinCallLegs(inboundDialogId, outboundDialogId, AutoTerminateAction.False);
			else
				callId = callBean.joinCallLegs(outboundDialogId, inboundDialogId, AutoTerminateAction.False);
		}

		public void setCallBean(CallBean aCallBean) {
			this.callBean = aCallBean;
		}

		public void reverseDialogOrder() {
			this.reverseDialogOrder = true;
		}
	}
	
	protected void setupInboundToOutboundPhoneCallWithInitialHold() throws Exception {
		getOutboundCall().listenForReinvite();

		SessionDescriptionHelper.setMediaDescription(getOutboundPhoneSdp(), getOutboundPhoneMediaDescription());
		assertTrue(getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), getRemoteSipAddress(), getRemoteSipProxy(), getOutboundPhoneSdp().toString(), "application", "sdp", null, null));
		assertWeGetOKWithHoldSdp();
		getOutboundCall().sendInviteOkAck();

		// invite-trying-ringing-ok-ack
		waitForCallAssertMediaDescriptionSendTryingRingingOk(SipUnitPhone.Inbound, null);

		// reinvite
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Outbound, getInboundPhoneMediaDescription());
		waitForEmptyAck(SipUnitPhone.Outbound);
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, getOutboundPhoneMediaDescription());
	}
	
	protected void setupInboundToOutboundPhoneCallWithoutInitialHold() throws Exception {
		SessionDescriptionHelper.setMediaDescription(getOutboundPhoneSdp(), getOutboundPhoneMediaDescription());
		assertTrue(getOutboundCall().initiateOutgoingCall(getOutboundPhoneSipAddress(), getRemoteSipAddress(), getRemoteSipProxy(), getOutboundPhoneSdp().toString(), "application", "sdp", null, null));

		// invite-trying-ringing-ok-ack
		waitForCallAssertMediaDescriptionSendTryingRingingOk(SipUnitPhone.Inbound, getOutboundPhoneMediaDescription());
		waitForEmptyAck(SipUnitPhone.Inbound);

		// inbound call ok
		assertTrue("No inbound ok", getOutboundCall().waitForAnswer(5000));
		assertEquals(Response.OK, getOutboundCall().getLastReceivedResponse().getStatusCode());
		assertMediaDescriptionInSessionDescription(getInboundPhoneMediaDescription(), new String(getOutboundCall().getLastReceivedResponse().getRawContent()));
		assertTrue(getOutboundCall().sendInviteOkAck());
	}

	protected void makeCallLegAutomaton(final String callLegId) {
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo secondDialogInfo = dialogCollection.get(callLegId);
				secondDialogInfo.setAutomaton(true);
				dialogCollection.replace(secondDialogInfo);
			}

			public String getResourceId() {
				return callLegId;
			}			
		};
		new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);
	}
	
	protected void setupAutomataThirdPartyCall() throws Exception {
		waitForCallSendTryingRingingOk(SipUnitPhone.Inbound);
		waitForCallAssertMediaDescriptionSendTryingRingingOk(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());
		assertTrue(getSecondInboundCall().waitForAck(5000));
		waitForAckAssertMediaDescription(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());
	}
	
	public void onCallConnected(CallConnectedEvent callConnectedEvent) {
		this.eventVector.add(callConnectedEvent);
		connectedSemaphore.release();
	}

	public void onCallConnectionFailed(CallConnectionFailedEvent callConnectionFailedEvent) {
		eventVector.add(callConnectionFailedEvent);
		connectionFailedSemaphore.release();
	}

	public void onCallDisconnected(CallDisconnectedEvent callDisconnectedEvent) {
		this.eventVector.add(callDisconnectedEvent);
		disconnectedSemaphore.release();
	}

	public void onCallTerminated(CallTerminatedEvent callTerminatedEvent) {
		this.eventVector.add(callTerminatedEvent);
		terminatedSemaphore.release();
	}
	
	public void onCallTerminationFailed(CallTerminationFailedEvent callTerminationFailedEvent) {
	}
}
