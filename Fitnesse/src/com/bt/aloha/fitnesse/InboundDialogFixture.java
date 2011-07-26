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

import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sip.ClientTransaction;
import javax.sip.TransactionUnavailableException;
import javax.sip.message.Request;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

import com.bt.aloha.call.CallBean;
import com.bt.aloha.call.CallBeanImpl;
import com.bt.aloha.call.CallListener;
import com.bt.aloha.call.event.AbstractCallEndedEvent;
import com.bt.aloha.call.event.CallConnectedEvent;
import com.bt.aloha.call.event.CallConnectionFailedEvent;
import com.bt.aloha.call.event.CallDisconnectedEvent;
import com.bt.aloha.call.event.CallTerminatedEvent;
import com.bt.aloha.call.event.CallTerminationFailedEvent;
import com.bt.aloha.call.state.CallLegCausingTermination;
import com.bt.aloha.call.state.CallTerminationCause;
import com.bt.aloha.callleg.InboundCallLegBean;
import com.bt.aloha.callleg.InboundCallLegBeanImpl;
import com.bt.aloha.callleg.InboundCallLegListener;
import com.bt.aloha.callleg.OutboundCallLegBean;
import com.bt.aloha.callleg.OutboundCallLegBeanImpl;
import com.bt.aloha.callleg.event.CallLegConnectedEvent;
import com.bt.aloha.callleg.event.IncomingCallLegEvent;
import com.bt.aloha.callleg.event.ReceivedCallLegRefreshEvent;
import com.bt.aloha.dialog.DialogConcurrentUpdateBlock;
import com.bt.aloha.dialog.event.IncomingAction;
import com.bt.aloha.dialog.event.IncomingResponseCode;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.eventing.EventFilter;
import com.bt.aloha.stack.SimpleSipStack;
import com.bt.aloha.stack.StackException;
import com.bt.aloha.testing.CallLegListenerStubBase;
import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;

/*
 * To create inbound dialogs into our stack, we initiate two stacks. One stack does an outbound call into the
 * stack under test, and this simulates an inbound scenario.
 * This is the reason InboundDialogFixture inherits from the OutboundDialogFixture - not because it is-a OutboundDialogFixture
 * in the true sense of OOP, but to reuse the functionality for outbound dialogs.
 */
public class InboundDialogFixture extends OutboundDialogFixture implements CallListener {
	public class InboundApplicationContextBeans {
		private CallBean callBean;
		private OutboundCallLegBean outboundCallLegBean;
		private InboundCallLegBean inboundCallLegBean;
		
		public InboundCallLegBean getInboundCallLegBean() {
			return inboundCallLegBean;
		}
	}
	
    private static final String LOCALHOST = "127.0.0.1";
    protected InboundApplicationContextBeans inboundApplicationContextBeans = new InboundApplicationContextBeans();
	
    private ForwardingInboundCallLegListener forwardingInboundListener = new ForwardingInboundCallLegListener();

	protected Vector<String> callIds = new Vector<String>();

	private Semaphore callConnectedSemaphore = new Semaphore(0);
	private Semaphore callConnectionFailedSemaphore = new Semaphore(0);
	protected Semaphore callTerminatedSemaphore = new Semaphore(0);
	private Semaphore callDisconnectedSemaphore = new Semaphore(0);
	private Semaphore forwardConnectedSemaphore = new Semaphore(0);

	protected Vector<String> callConnectedEvents = new Vector<String>();
	protected Hashtable<String, CallConnectionFailedEvent> callConnectionFailedEvents = new Hashtable<String, CallConnectionFailedEvent>();
	protected Hashtable<String, AbstractCallEndedEvent> callTerminatedEvents = new Hashtable<String, AbstractCallEndedEvent>();
	protected Hashtable<String, CallDisconnectedEvent> callDisconnectedEvents = new Hashtable<String, CallDisconnectedEvent>();
	private String forwardDialogId;
	private String forwardDialogUri;


	public InboundDialogFixture() {
		super();
		ApplicationContext inboundApplicationContext  = FixtureApplicationContexts.getInstance().startInboundApplicationContext();
		inboundApplicationContextBeans.outboundCallLegBean = (OutboundCallLegBean) inboundApplicationContext.getBean("outboundCallLegBean");
		inboundApplicationContextBeans.callBean = (CallBean) inboundApplicationContext.getBean("callBean");
		inboundApplicationContextBeans.inboundCallLegBean = (InboundCallLegBean) inboundApplicationContext.getBean("inboundCallLegBean");

		List<InboundCallLegListener> l = new ArrayList<InboundCallLegListener>();
		l.add((CallBeanImpl)inboundApplicationContextBeans.callBean);
		l.add(new DecliningInboundCallLegListener());
		l.add(new HoldingInboundCallLegListener());
		l.add(forwardingInboundListener);

		((InboundCallLegBeanImpl)inboundApplicationContextBeans.inboundCallLegBean).setInboundCallLegListeners(l);

		List<CallListener> callListeners = ((CallBeanImpl)inboundApplicationContextBeans.callBean).getCallListeners();
		if (!callListeners.contains(this))
			callListeners.add(this);
	}

	public void forwardToPhoneUri(String val) {
		this.forwardDialogUri = getAddressAndPort(val);
		forwardingInboundListener.setForwardToPhoneUri(this.forwardDialogUri);
	}

	public String createForwardDialog() {
		forwardDialogId = outboundCallLegBean.createCallLeg(URI.create(secondPhoneUri), URI.create(forwardDialogUri), firstDialogCallAnswerTimeout);
		return "OK";

	}

	public String connectForwardDialog() {
		outboundCallLegBean.connectCallLeg(forwardDialogId);
		return "OK";
	}

    public String connectFirstDialogWithVideo() throws SdpException {
        final MediaDescription videoMediaDescription = SdpFactory.getInstance().createMediaDescription("video", 5678, 0, "RTP/AVP", new String[] {"0"});
        videoMediaDescription.setAttribute("rtpmap", "0 PCMU/8000");

        final MediaDescription audioMediaDescription = SdpFactory.getInstance().createMediaDescription("audio", 5678, 0, "RTP/AVP", new String[] {"0"});
        audioMediaDescription.setAttribute("rtpmap", "0 PCMU/8000");

        ConcurrentUpdateBlock concurrentUpdateBlock = new DialogConcurrentUpdateBlock(((OutboundCallLegBeanImpl)outboundCallLegBean).getDialogBeanHelper()) {

            @SuppressWarnings("unchecked")
            public void execute() {
                DialogInfo dialogInfo = getDialogCollection().get(firstDialogId);
                try {
                    Connection connection = SdpFactory.getInstance().createConnection(LOCALHOST);
                    dialogInfo.getSessionDescription().setConnection(connection);
                    Vector mediaDescriptions = dialogInfo.getSessionDescription().getMediaDescriptions(true);
                    mediaDescriptions.add(videoMediaDescription);
                    mediaDescriptions.add(audioMediaDescription);
                } catch (SdpException e) {
                    e.printStackTrace();
                }

                dialogInfo.setSdpInInitialInvite(true);
                dialogInfo.setAutomaticallyPlaceOnHold(false);
                dialogInfo.setApplicationData(null);
                forceSequenceNumber(dialogInfo.getSipCallId(), dialogInfo.getSequenceNumber(), Request.INVITE);
                dialogInfo.setAutoTerminate(false);

                Request request = ((OutboundCallLegBeanImpl)outboundCallLegBean).getDialogBeanHelper().createInitialInviteRequest(dialogInfo.getRemoteParty().getURI().toString(), dialogInfo);

                ClientTransaction clientTransaction;
                SimpleSipStack stack = (SimpleSipStack)applicationContext.getBean("simpleSipStack");
                try {
                    clientTransaction = stack.getSipProvider().getNewClientTransaction(request);
                } catch (TransactionUnavailableException e) {
                    throw new StackException(e.getMessage(), e);
                }

                dialogInfo.setDialogState(DialogState.Initiated);
                dialogInfo.setInviteClientTransaction(clientTransaction);

                getDialogCollection().replace(dialogInfo);

                stack.sendRequest(clientTransaction);
            }

            public String getResourceId() {
                return firstDialogId;
            }
        };
        new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

        return "OK";
    }

    // Event listener methods & event filtering
    public class DecliningInboundCallLegListener extends CallLegListenerStubBase implements InboundCallLegListener, EventFilter {
		public void onIncomingCallLeg(IncomingCallLegEvent e) {
			e.setIncomingCallAction(IncomingAction.Reject);
			e.setResponseCode(IncomingResponseCode.Decline);
		}
		public boolean shouldDeliverEvent(Object event) {
			return event instanceof IncomingCallLegEvent && ((IncomingCallLegEvent)event).getToUri().contains("decline");
		}
    }
	public class HoldingInboundCallLegListener extends CallLegListenerStubBase implements InboundCallLegListener, EventFilter {
		public void onIncomingCallLeg(IncomingCallLegEvent e) {
			e.setIncomingCallAction(IncomingAction.PlaceOnHold);
		}
		public boolean shouldDeliverEvent(Object event) {
			return event instanceof IncomingCallLegEvent && ((IncomingCallLegEvent)event).getToUri().contains("hold");
		}
	}
	public class ForwardingInboundCallLegListener extends CallLegListenerStubBase implements InboundCallLegListener, EventFilter {
		private Log log = LogFactory.getLog(this.getClass());
		private String forwardToPhoneUri;

		public void setForwardToPhoneUri(String uri) {
			log.debug("setting forwardToUri:" + uri);
			this.forwardToPhoneUri = uri;
			log.debug("set to:" + this.forwardToPhoneUri);
		}

		public void onIncomingCallLeg(IncomingCallLegEvent e) {
			e.setIncomingCallAction(IncomingAction.None);
			String incomingDialogId = e.getId();
			String incomingSipUri = e.getFromUri();
			if (incomingDialogId.equals(incomingDialogId)) {
				log.debug(String.format("Got onIncomingDialog in the ForwardingInboundListener, joining now with the %s", forwardToPhoneUri));
				String forwardDialogId;
                forwardDialogId = inboundApplicationContextBeans.outboundCallLegBean.createCallLeg(URI.create(incomingSipUri), URI.create(forwardToPhoneUri));
				String callId = inboundApplicationContextBeans.callBean.joinCallLegs(incomingDialogId, forwardDialogId);
				callIds.add(callId);
			} else {
				log.debug("Got onConnectedEvent in the ForwardingInboundListener: ignoring");
			}
		}
		public boolean shouldDeliverEvent(Object event) {
			return event instanceof IncomingCallLegEvent && ((IncomingCallLegEvent) event).getToUri().contains("forward");
		}
	}
	public String waitForCallConnectedEvent() throws Exception {
		if (callConnectedSemaphore.tryAcquire(waitTimeoutSeconds,
				TimeUnit.SECONDS)) {
			return "OK";
		}
		return "No event";
	}

	private String waitForCallConnectionFailedEvent(CallTerminationCause callTerminationCause, CallLegCausingTermination callLegCausingTermination) throws Exception {
        if (callConnectionFailedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS)) {
        	CallConnectionFailedEvent callConnectionFailedEvent = callConnectionFailedEvents.get(callIds.get(callIds.size()-1));
        	if (callConnectionFailedEvent != null && !callConnectionFailedEvent.getCallTerminationCause().equals(callTerminationCause)) {
        		if (callConnectionFailedEvent.getDuration() == 0)
        			return callConnectionFailedEvent.getCallTerminationCause().toString();
        		return String.format("Call duration: %d seconds", callConnectionFailedEvent.getDuration());
        	} else if (callConnectionFailedEvent != null && !callConnectionFailedEvent.getCallLegCausingTermination().equals(callLegCausingTermination))
        		return callConnectionFailedEvent.getCallLegCausingTermination().toString();
        	else if (callConnectionFailedEvent != null)
        		return "OK";
        	else return callConnectionFailedEvents.keySet().toString();
        }
        return "No event";
	}

	public String waitForCallConnectionFailedEventWithSecondRemotePartyBusy() throws Exception {
		return waitForCallConnectionFailedEvent(CallTerminationCause.RemotePartyBusy, CallLegCausingTermination.Second);
	}

	public String waitForCallTerminatedEvent() throws Exception {
		if (callTerminatedSemaphore.tryAcquire(waitTimeoutSeconds,
				TimeUnit.SECONDS)) {
			return "OK";
		}
		return "No event";
	}

	public String waitForCallDisconnectedEvent() throws Exception {
		if (callDisconnectedSemaphore.tryAcquire(waitTimeoutSeconds,
				TimeUnit.SECONDS)) {
			return "OK";
		}
		return "No event";
	}

	public String waitForForwardConnectedEvent() throws Exception {
		if (forwardConnectedSemaphore.tryAcquire(waitTimeoutSeconds,
				TimeUnit.SECONDS)) {
			return "OK";
		}
		return "No event";
	}

	public void onCallLegConnected(CallLegConnectedEvent e) {
        String dialogId = e.getId();
        if (dialogId.equals(forwardDialogId)) {
        	this.connectedEvents.add(dialogId);
            forwardConnectedSemaphore.release();
        } else {
        	super.onCallLegConnected(e);
        }
	}

	public void onCallConnected(CallConnectedEvent arg0) {
		this.callConnectedEvents.add(arg0.getCallId());
		callConnectedSemaphore.release();
	}

	public void onCallConnectionFailed(CallConnectionFailedEvent arg0) {
		this.callConnectionFailedEvents.put(arg0.getCallId(), arg0);
		callConnectionFailedSemaphore.release();
	}

	public void onCallDisconnected(CallDisconnectedEvent arg0) {
		this.callDisconnectedEvents.put(arg0.getCallId(), arg0);
		callDisconnectedSemaphore.release();
	}

	public void onCallTerminated(CallTerminatedEvent arg0) {
		this.callTerminatedEvents.put(arg0.getCallId(), arg0);
		callTerminatedSemaphore.release();
	}
	
	public void onCallTerminationFailed(CallTerminationFailedEvent callTerminationFailedEvent) {
	}

	@Override
	public void onReceivedCallLegRefresh(ReceivedCallLegRefreshEvent receivedCallLegRefreshEvent) {
        String dialogId = receivedCallLegRefreshEvent.getId();
        if (dialogId.equals(firstDialogId)) {
        	outboundCallLegBean.acceptReceivedMediaOffer(dialogId, receivedCallLegRefreshEvent.getMediaDescription(), false, true);
        }
	}
}
