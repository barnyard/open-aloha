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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

import com.bt.aloha.call.CallBeanImpl;
import com.bt.aloha.call.CallInformation;
import com.bt.aloha.call.CallListener;
import com.bt.aloha.call.event.AbstractCallEndedEvent;
import com.bt.aloha.call.event.CallConnectedEvent;
import com.bt.aloha.call.event.CallConnectionFailedEvent;
import com.bt.aloha.call.event.CallDisconnectedEvent;
import com.bt.aloha.call.event.CallTerminatedEvent;
import com.bt.aloha.call.event.CallTerminationFailedEvent;
import com.bt.aloha.call.state.CallInfo;
import com.bt.aloha.call.state.CallLegCausingTermination;
import com.bt.aloha.call.state.CallTerminationCause;
import com.bt.aloha.call.state.ImmutableCallInfo;
import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.callleg.CallLegInformation;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.TerminationCause;

public class OutboundCallFixture extends SimpleSipStackBaseFixture implements CallListener {
	private Log log = LogFactory.getLog(this.getClass());

	protected Vector<String> callIds = new Vector<String>();
	protected int activeCall = 1;

	private Semaphore callConnectedSemaphore = new Semaphore(0);
	private Semaphore callConnectionFailedSemaphore = new Semaphore(0);
	private Semaphore callTerminatedSemaphore = new Semaphore(0);
	private Semaphore callDisconnectedSemaphore = new Semaphore(0);

	protected CountDownLatch latch;

	protected Vector<String> callConnectedEvents = new Vector<String>();
	protected Hashtable<String, AbstractCallEndedEvent> callConnectionFailedEvents = new Hashtable<String, AbstractCallEndedEvent>();
	protected Hashtable<String, AbstractCallEndedEvent> callTerminatedEvents = new Hashtable<String, AbstractCallEndedEvent>();
	protected Hashtable<String, AbstractCallEndedEvent> callDisconnectedEvents = new Hashtable<String, AbstractCallEndedEvent>();
	private int lastCallCollectionSize;

	public OutboundCallFixture(ApplicationContext appCtx) {
		super(appCtx);
		setCallBeanListeners();
	}

	public void setCallBeanListeners() {
		List<CallListener> callListeners = new ArrayList<CallListener>();
		callListeners.add(this);
		((CallBeanImpl) callBean).setCallListeners(callListeners);
	}

	public OutboundCallFixture() {
		this(FixtureApplicationContexts.getInstance().startApplicationContext());
	}

	public String getActiveCallId() {
		return callIds.get(activeCall - 1);
	}

	public void setActiveCall(int activeCall) {
		this.activeCall = activeCall;
	}

	public String joinDialogsOneAndTwo() {
		latch = new CountDownLatch(1);
		String id = callBean.joinCallLegs(firstDialogId, secondDialogId);
		log.info("call ID: " + id);
		callIds.add(id);
		latch.countDown();
		return "OK";
	}

	public String joinDialogsOneAndTwoWithAutoTerminateDialogs() {
		latch = new CountDownLatch(1);
		callIds.add(callBean.joinCallLegs(firstDialogId, secondDialogId,
				AutoTerminateAction.True));
		latch.countDown();
		return "OK";
	}

	public String joinDialogsOneAndTwoWithOneMinuteDuration() {
		latch = new CountDownLatch(1);
		callIds.add(callBean.joinCallLegs(firstDialogId, secondDialogId,
				AutoTerminateAction.True, 1));
		latch.countDown();
		return "OK";
	}

	public String joinDialogsTwoAndThree() {
		latch = new CountDownLatch(1);
		callIds.add(callBean.joinCallLegs(secondDialogId, thirdDialogId));
		latch.countDown();
		return "OK";
	}

	public void terminateCall() {
		callBean.terminateCall(getActiveCallId());
	}

	public String callStatus() {
		CallInformation callInformation = callBean.getCallInformation(callIds
				.get(activeCall - 1));
		return callInformation.getCallState().toString();
	}

	public String callTerminationCause() {
		CallInformation callInformation = callBean.getCallInformation(callIds.get(activeCall - 1));
		CallTerminationCause cause = callInformation.getCallTerminationCause();
		return cause==null?null:cause.toString();
	}

	public String callLegCausingTermination() {
		CallInformation callInformation = callBean.getCallInformation(callIds.get(activeCall - 1));
		CallLegCausingTermination callLeg = callInformation.getCallLegCausingTermination();
		return callLeg==null?null:callLeg.toString();
	}
	
	public boolean firstCallLegIdNotNull() {
		return firstCallLegId() != null;
	}

	public String firstCallLegId() {
		CallInformation callInformation = callBean.getCallInformation(callIds.get(activeCall - 1));
		return callInformation.getFirstCallLegId();
	}

	public boolean secondCallLegIdNotNull() {
		return secondCallLegId() != null;
	}
	
	public boolean isFirstCallLegAMediaLeg() {
		CallLegInformation callLegInformation = outboundCallLegBean.getCallLegInformation(firstDialogId);
		return callLegInformation.isMediaCallLeg();
	}

	public boolean isSecondCallLegAMediaLeg() {
		CallLegInformation callLegInformation = outboundCallLegBean.getCallLegInformation(secondDialogId);
		return callLegInformation.isMediaCallLeg();
	}

	public String secondCallLegId() {
		CallInformation callInformation = callBean.getCallInformation(callIds.get(activeCall - 1));
		return callInformation.getSecondCallLegId();
	}

	public String dialogOneStatus() {
		ImmutableCallInfo callInfo = getCallCollection().get(getActiveCallId());
		return getDialogCollection().get(callInfo.getFirstDialogId()).getDialogState().toString();
	}

	public String dialogOneTerminationCause() {
		ImmutableCallInfo callInfo = getCallCollection().get(getActiveCallId());
		TerminationCause cause = getDialogCollection().get(callInfo.getFirstDialogId()).getTerminationCause();
		return cause == null ? null : cause.toString();
	}

	public String dialogTwoStatus() {
		ImmutableCallInfo callInfo = getCallCollection().get(getActiveCallId());
		DialogState dialogState = getDialogCollection().get(callInfo.getSecondDialogId()).getDialogState();
		return dialogState==null?null:dialogState.toString();
	}

	public String dialogTwoTerminationCause() {
		ImmutableCallInfo callInfo = getCallCollection().get(getActiveCallId());
		TerminationCause cause = getDialogCollection().get(callInfo.getSecondDialogId()).getTerminationCause();
		return cause==null?null:cause.toString();
	}

	public void callMaxTTL(int val) {
		getCallCollection().setMaxTimeToLive(val);
	}

	public void housekeepCallCollection() {
		getCallCollection().housekeep();
	}

	public void storeCallCollectionSize() {
		lastCallCollectionSize = getCallCollection().size();
	}

	public int callCollectionSize() {
		return getCallCollection().size();
	}

	public int callCollectionSizeDelta() {
		return getCallCollection().size() - lastCallCollectionSize;
	}

	public String cleanCallCollection() {
		ConcurrentMap<String, CallInfo> calls = getCallCollection().getAll();
		for (String callId : calls.keySet()) {
			getCallCollection().remove(callId);
		}
		if (getCallCollection().size() == 0)
			return "OK";
		else
			return "Failed";
	}

	// /////////////////////////////////////////

	public String waitForCallConnectedEvent() throws Exception {
		String targetId = getActiveCallId();
		if (callConnectedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS)) {
			if (callConnectedEvents.contains(targetId)) {
				return "OK";
			} else {
				return callConnectedEvents.toString();
			}
		}
		return "No event";
	}

	private String waitForCallConnectionFailedEvent(CallTerminationCause callTerminationCause, CallLegCausingTermination callLegCausingTermination)	throws Exception {
		String targetId = getActiveCallId();
		if (callConnectionFailedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS)) {
			return waitForCallEndedEvent(callConnectionFailedEvents, callTerminationCause, callLegCausingTermination, true, targetId);
		}
		return "No event";
	}

	public String waitForCallConnectionFailedEventWithFirstCallAnswerTimeout() throws Exception {
		return waitForCallConnectionFailedEvent(CallTerminationCause.CallAnswerTimeout,	CallLegCausingTermination.First);
	}

	public String waitForCallConnectionFailedEventWithSecondCallAnswerTimeout()	throws Exception {
		return waitForCallConnectionFailedEvent(CallTerminationCause.CallAnswerTimeout, CallLegCausingTermination.Second);
	}

	public String waitForCallConnectionFailedEventWithFirstSipSessionError() throws Exception {
		return waitForCallConnectionFailedEvent(CallTerminationCause.SipSessionError, CallLegCausingTermination.First);
	}

	public String waitForCallConnectionFailedEventWithFirstRemotePartyUnknown()	throws Exception {
		return waitForCallConnectionFailedEvent(CallTerminationCause.RemotePartyUnknown, CallLegCausingTermination.First);
	}

	public String waitForCallConnectionFailedEventWithFirstRemotePartyUnavailable()	throws Exception {
		return waitForCallConnectionFailedEvent(CallTerminationCause.RemotePartyUnavailable, CallLegCausingTermination.First);
	}

	public String waitForCallConnectionFailedEventWithFirstRemotePartyBusy() throws Exception {
		return waitForCallConnectionFailedEvent(CallTerminationCause.RemotePartyBusy, CallLegCausingTermination.First);
	}

	public String waitForCallConnectionFailedEventWithTerminatedByApplication() throws Exception {
		return waitForCallConnectionFailedEvent(CallTerminationCause.TerminatedByApplication, CallLegCausingTermination.Neither);
	}

	public String waitForCallConnectionFailedEventWithSecondCallLegForbidden() throws Exception {
		return waitForCallConnectionFailedEvent(CallTerminationCause.RemotePartyForbidden, CallLegCausingTermination.Second);
	}

	public String waitForCallConnectionFailedEventWithSecondServiceUnavailable() throws Exception {
		return waitForCallConnectionFailedEvent(CallTerminationCause.ServiceUnavailable, CallLegCausingTermination.Second);
	}

	private String waitForCallTerminatedEvent(CallTerminationCause callTerminationCause, CallLegCausingTermination callLegCausingTermination) throws Exception {
		String targetId = getActiveCallId();
		if (callTerminatedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS)) {
			return waitForCallEndedEvent(callTerminatedEvents, callTerminationCause, callLegCausingTermination, false, targetId);
		}
		return "No event";
	}

	private String waitForCallEndedEvent(Hashtable<String, AbstractCallEndedEvent> callEndedEvents,	CallTerminationCause callTerminationCause, CallLegCausingTermination callLegCausingTermination,	boolean zeroDuration, String targetId) {
		AbstractCallEndedEvent callEndedEvent = callEndedEvents.get(targetId);
		if (callEndedEvent != null && !callEndedEvent.getCallTerminationCause().equals(callTerminationCause)) {
			if ((zeroDuration && callEndedEvent.getDuration() == 0)	|| (!zeroDuration && callEndedEvent.getDuration() > 0))
				return callEndedEvent.getCallTerminationCause().toString();
			return String.format("Call duration: %d seconds", callEndedEvent.getDuration());
		} else if (callEndedEvent != null && !callEndedEvent.getCallLegCausingTermination().equals(callLegCausingTermination))
			return callEndedEvent.getCallLegCausingTermination().toString();
		else if (callEndedEvent != null)
			return "OK";
		else
			return callEndedEvents.keySet().toString();
	}

	public String waitForCallTerminatedEventWithTerminatedByApplication() throws Exception {
		return waitForCallTerminatedEvent(CallTerminationCause.TerminatedByApplication,	CallLegCausingTermination.Neither);
	}

	public String waitForCallTerminatedEventWithHousekept() throws Exception {
		return waitForCallTerminatedEvent(CallTerminationCause.Housekept, CallLegCausingTermination.Neither);
	}

	public String waitForCallTerminatedEventWithFirstCallLegDetached() throws Exception {
		return waitForCallTerminatedEvent(CallTerminationCause.CallLegDetached, CallLegCausingTermination.First);
	}

	public String waitForCallTerminatedEventWithSecondCallLegDetached()	throws Exception {
		return waitForCallTerminatedEvent(CallTerminationCause.CallLegDetached,	CallLegCausingTermination.Second);
	}

	public String waitForCallTerminatedEvent() throws Exception {
		return waitForCallTerminatedEvent(null, null);
	}

	private String waitForCallDisconnectedEvent(CallTerminationCause callTerminationCause,	CallLegCausingTermination callLegCausingTermination) throws Exception {
		String targetId = getActiveCallId();
		if (callDisconnectedSemaphore.tryAcquire(waitTimeoutSeconds,
				TimeUnit.SECONDS)) {
			return waitForCallEndedEvent(callDisconnectedEvents, callTerminationCause, callLegCausingTermination, false, targetId);
		}
		return "No event";
	}

	public String waitForCallTerminatedEventWithMaxCallDurationExceeded() throws Exception {
		return waitForCallTerminatedEvent(CallTerminationCause.MaximumCallDurationExceeded, CallLegCausingTermination.Neither);
	}

	public String waitForCallDisconnectedEventWithFirstRemotePartyHungUp() throws Exception {
		return waitForCallDisconnectedEvent(CallTerminationCause.RemotePartyHungUp, CallLegCausingTermination.First);
	}

	public String waitForCallDisconnectedEventWithSecondRemotePartyHungUp() throws Exception {
		return waitForCallDisconnectedEvent(CallTerminationCause.RemotePartyHungUp, CallLegCausingTermination.Second);
	}

	public void onCallConnected(CallConnectedEvent arg0) {
		if (latch != null) {
			try {
				latch.await();
			} catch (InterruptedException e) {
				log.warn(e);
			}
			if (this.callIds.contains(arg0.getCallId())) {
				this.callConnectedEvents.add(arg0.getCallId());
				callConnectedSemaphore.release();
			}
		}
	}

	public void onCallConnectionFailed(CallConnectionFailedEvent arg0) {
		if (latch != null) {
			try {
				latch.await();
			} catch (InterruptedException e) {
				log.warn(e);
			}
			log.info(String.format("onCallConnectionFailed(%s, %s, %s)", arg0
					.getCallId(), arg0.getCallTerminationCause(), arg0
					.getCallLegCausingTermination()));
			if (this.callIds.contains(arg0.getCallId())) {
				this.callConnectionFailedEvents.put(arg0.getCallId(), arg0);
				callConnectionFailedSemaphore.release();
			}
		}
	}

	public void onCallDisconnected(CallDisconnectedEvent arg0) {
		if (latch != null) {
			try {
				latch.await();
			} catch (InterruptedException e) {
				log.warn(e);
			}
			if (this.callIds.contains(arg0.getCallId())) {
				this.callDisconnectedEvents.put(arg0.getCallId(), arg0);
				callDisconnectedSemaphore.release();
			}
		}
	}

	public void onCallTerminated(CallTerminatedEvent arg0) {
		if (latch != null) {
			try {
				latch.await();
			} catch (InterruptedException e) {
				log.warn(e);
			}
			if (this.callIds.contains(arg0.getCallId())) {
				this.callTerminatedEvents.put(arg0.getCallId(), arg0);
				callTerminatedSemaphore.release();
			}
		}
	}
	
	public void onCallTerminationFailed(CallTerminationFailedEvent callTerminationFailedEvent) {
	}
}
