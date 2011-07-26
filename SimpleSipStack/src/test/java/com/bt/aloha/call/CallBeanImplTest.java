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

 	

 	
 	
 
/**
 * (c) British Telecommunications plc, 2007, All Rights Reserved
 */
package com.bt.aloha.call;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.sdp.MediaDescription;

import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.call.collections.CallCollectionImpl;
import com.bt.aloha.call.event.CallTerminationFailedEvent;
import com.bt.aloha.call.state.CallInfo;
import com.bt.aloha.call.state.CallLegCausingTermination;
import com.bt.aloha.call.state.CallLegConnectionState;
import com.bt.aloha.call.state.CallState;
import com.bt.aloha.call.state.CallTerminationCause;
import com.bt.aloha.call.state.MediaNegotiationState;
import com.bt.aloha.call.state.ReadOnlyCallInfo;
import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.callleg.InboundCallLegBean;
import com.bt.aloha.callleg.OutboundCallLegBean;
import com.bt.aloha.callleg.event.CallLegAlertingEvent;
import com.bt.aloha.callleg.event.CallLegConnectedEvent;
import com.bt.aloha.callleg.event.CallLegConnectionFailedEvent;
import com.bt.aloha.callleg.event.CallLegDisconnectedEvent;
import com.bt.aloha.callleg.event.CallLegRefreshCompletedEvent;
import com.bt.aloha.callleg.event.CallLegTerminatedEvent;
import com.bt.aloha.callleg.event.CallLegTerminationFailedEvent;
import com.bt.aloha.callleg.event.IncomingCallLegEvent;
import com.bt.aloha.callleg.event.ReceivedCallLegRefreshEvent;
import com.bt.aloha.collections.memory.InMemoryHousekeepingCollectionImpl;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.collections.DialogCollectionImpl;
import com.bt.aloha.dialog.event.DialogConnectedEvent;
import com.bt.aloha.dialog.event.IncomingDialogEvent;
import com.bt.aloha.dialog.event.ReceivedDialogRefreshEvent;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.ReinviteInProgress;
import com.bt.aloha.dialog.state.TerminationCause;
import com.bt.aloha.eventing.EventDispatcher;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.stack.SimpleSipStack;

public class CallBeanImplTest {
	private CallBeanImpl callBean;
	private OutboundCallLegBean outboundCallLegBean;
	private InboundCallLegBean inboundCallLegBean;
	private CallInfo callInfo;
	private DialogInfo dialogInfo1;
	private DialogInfo dialogInfo2;
	private EventDispatcher dispatcher;
	private DialogCollection dialogCollection;
    private CallCollection callCollection;
    private String beanName = "beanName";

	@Before
	public void initializeStubsAndMocks() throws Exception {
		SimpleSipStack sss = EasyMock.createNiceMock(SimpleSipStack.class);
		EasyMock.replay(sss);
		dispatcher = EasyMock.createNiceMock(EventDispatcher.class);
		EasyMock.replay(dispatcher);
		outboundCallLegBean = EasyMock.createNiceMock(OutboundCallLegBean.class);
		inboundCallLegBean = EasyMock.createNiceMock(InboundCallLegBean.class);
		MaxCallDurationScheduler maxCallDurationScheduler = EasyMock.createNiceMock(MaxCallDurationScheduler.class);
		EasyMock.replay(maxCallDurationScheduler);

		dialogInfo1 = new DialogInfo("id1", "dialogId1", "1.2.3.4");
		dialogInfo1.setRemoteParty("sip:a@127.0.0.1");
		dialogInfo2 = new DialogInfo("id2", "dialogId2", "1.2.3.4");
		dialogInfo2.setRemoteParty("sip:b@127.0.0.1");
		dialogCollection = new DialogCollectionImpl(new InMemoryHousekeepingCollectionImpl<DialogInfo>());
		dialogCollection.add(dialogInfo1);
		dialogCollection.add(dialogInfo2);

		callCollection = new CallCollectionImpl(new InMemoryHousekeepingCollectionImpl<CallInfo>());

		callBean = new CallBeanImpl();
		outboundCallLegBean.addOutboundCallLegListener(callBean);
		EasyMock.replay(outboundCallLegBean);
		callBean.setOutboundCallLegBean(outboundCallLegBean);
		callBean.setDialogCollection(dialogCollection);
		callBean.setCallCollection(callCollection);
		callBean.setEventDispatcher(dispatcher);
		callBean.setMaxCallDurationScheduler(maxCallDurationScheduler);

        String callId = callBean.generateCallId();
		callInfo = new CallInfo(beanName, callId, dialogInfo1.getId(), dialogInfo2.getId(), AutoTerminateAction.False, -1);
		callCollection.add(callInfo);
	}

	// This tests that onCallLegConnected sets the CallStatus of the call to Connected
	@Test
	public void onCallLegConnectedSetsCallStateToConnected() {
		// setup
		callInfo.setCallLegConnectionState(dialogInfo1.getId(), CallLegConnectionState.Completed);
		callInfo.setMediaNegotiationState(MediaNegotiationState.ProxiedOffer);
		callCollection.replace(callInfo);

		// act
		callBean.onCallLegConnected(new CallLegConnectedEvent(dialogInfo2.getId(), callInfo.getId(), SessionDescriptionHelper.generateHoldMediaDescription()));

		// assert
		assertEquals(CallState.Connected, callCollection.get(callInfo.getId()).getCallState());
	}

	// This tests that onCallLegConnected of a dialog with a different call id does not affect CallStatus
	@Test
	public void onCallLegConnectedDifferentCallIdDoesNotChangeCallState() {
		// setup
		callCollection.replace(callInfo);

		// act
		callBean.onCallLegRefreshCompleted(new CallLegRefreshCompletedEvent(dialogInfo2.getId(), "SomeRandomCallId", SessionDescriptionHelper.generateHoldMediaDescription()));

		// assert
		assertEquals(CallState.Connecting, callCollection.get(callInfo.getId()).getCallState());
	}

	// This tests that onCallLegConnected sets the start time
	@Test
	public void onCallLegConnectedSetsStartTime() {
		// setup
		callInfo.setCallLegConnectionState(dialogInfo1.getId(), CallLegConnectionState.Completed);
		callInfo.setMediaNegotiationState(MediaNegotiationState.ProxiedOffer);
		callCollection.replace(callInfo);
		assertEquals(CallInfo.TIME_NOT_SET, callCollection.get(callInfo.getId()).getStartTime());

		// act
		callBean.onCallLegConnected(new CallLegConnectedEvent(dialogInfo2.getId(), callInfo.getId(), SessionDescriptionHelper.generateHoldMediaDescription()));

		// assert
		assertTrue(callCollection.get(callInfo.getId()).getStartTime() > 0);
	}

	// This tests that onCallLegConnected of a dialog with a different call id does not affect the start time
	@Test
	public void onCallLegConnectedDifferentCallIdDoesNotChangeStartTime() {
		// setup
		callCollection.replace(callInfo);
		assertEquals(CallInfo.TIME_NOT_SET, callCollection.get(callInfo.getId()).getStartTime());

		// act
		callBean.onCallLegRefreshCompleted(new CallLegRefreshCompletedEvent(dialogInfo2.getId(), "SomeRandomCallId", SessionDescriptionHelper.generateHoldMediaDescription()));

		// assert
		assertEquals(CallInfo.TIME_NOT_SET, callCollection.get(callInfo.getId()).getStartTime());
	}

	/**
	 * This tests that onConnectionFailed sets the CallStatus of the call to Terminated
	 *
	 */
	@Test
	public void onCallConnectionFailedSetsCallStateToTerminated() {
		// setup
		dialogInfo1.setDialogState(DialogState.Terminated);
		dialogInfo2.setDialogState(DialogState.Confirmed);

		// act
		callBean.onCallLegConnectionFailed(new CallLegConnectionFailedEvent(dialogInfo2.getId(), TerminationCause.TerminatedByServer));

		// assert
		assertEquals(CallState.Terminated, callCollection.get(callInfo.getId()).getCallState());
	}

	/**
	 * This tests that onTerminated sets the CallStatus of the call to Terminated
	 */
	@Test
	public void onCallTerminatedSetsCallStateToTerminated() {
		// setup
		dialogInfo1.setDialogState(DialogState.Confirmed);
		dialogInfo2.setDialogState(DialogState.Confirmed);

		// act
		callBean.onCallLegTerminated(new CallLegTerminatedEvent(dialogInfo2.getId(), TerminationCause.TerminatedByServer));

		// assert
		assertEquals(CallState.Terminated, callCollection.get(callInfo.getId()).getCallState());
	}

	/**
	 * This tests that onTerminated sets the CallStatus of the call to Terminated
	 */
	@Test
	public void onCallDisconnectedSetsCallStateToTerminated() {
		// setup
		dialogInfo1.setDialogState(DialogState.Confirmed);
		dialogInfo2.setDialogState(DialogState.Confirmed);

		// act
		callBean.onCallLegDisconnected(new CallLegDisconnectedEvent(dialogInfo2.getId(), TerminationCause.TerminatedByServer));

		// assert
		assertEquals(CallState.Terminated, callCollection.get(callInfo.getId()).getCallState());
	}

	/**
	 * This tests that onConnectionFailed wont fire the onCallTerminated event
	 * again if the call was already terminated
	 *
	 */
	@Test
	public void onCallConnectionFailedDoesntFireIfAlreadyTerminated() {
		// setup
		dialogInfo1.setDialogState(DialogState.Terminated);
		dialogInfo2.setDialogState(DialogState.Confirmed);
		dispatcher = EasyMock.createMock(EventDispatcher.class);
		callBean.setEventDispatcher(dispatcher);
		EasyMock.replay(dispatcher);
		callInfo.setCallState(CallState.Terminated);
        callCollection.replace(callInfo);

		// act
		callBean.onCallLegConnectionFailed(new CallLegConnectionFailedEvent(dialogInfo2.getId(), TerminationCause.TerminatedByServer));

		// assert
		assertEquals(CallState.Terminated, callCollection.get(callInfo.getId()).getCallState());
		EasyMock.verify(dispatcher);
	}

	/**
	 * This tests that onTerminated wont fire the onCallTerminated event
	 * again if the call was already terminated
	 *
	 */
	@Test
	public void onTerminatedDoesntFireIfAlreadyTerminated() {
		// setup
		dialogInfo1.setDialogState(DialogState.Terminated);
		dialogInfo2.setDialogState(DialogState.Confirmed);
		dispatcher = EasyMock.createMock(EventDispatcher.class);
		callBean.setEventDispatcher(dispatcher);
		EasyMock.replay(dispatcher);
		callInfo.setCallState(CallState.Terminated);
        callCollection.replace(callInfo);

		// act
		callBean.onCallLegTerminated(new CallLegTerminatedEvent(dialogInfo2.getId(), TerminationCause.TerminatedByServer));

		// assert
		assertEquals(CallState.Terminated, callCollection.get(callInfo.getId()).getCallState());
		EasyMock.verify(dispatcher);
	}

	/**
	 * This tests that onDisconnected wont fire the onCallTerminated event
	 * again if the call was already terminated
	 *
	 */
	@Test
	public void onDisconnectedDoesntFireIfAlreadyTerminated() {
		// setup
		dialogInfo1.setDialogState(DialogState.Terminated);
		dialogInfo2.setDialogState(DialogState.Confirmed);
		dispatcher = EasyMock.createMock(EventDispatcher.class);
		callBean.setEventDispatcher(dispatcher);
		EasyMock.replay(dispatcher);
		callInfo.setCallState(CallState.Terminated);
        callCollection.replace(callInfo);

		// act
		callBean.onCallLegDisconnected(new CallLegDisconnectedEvent(dialogInfo2.getId(), TerminationCause.TerminatedByServer));

		// assert
		assertEquals(CallState.Terminated, callCollection.get(callInfo.getId()).getCallState());
		EasyMock.verify(dispatcher);
	}

	/**
	 * This tests that we add the call info to the max call duration scheduler
	 */
	@Test
	public void scheduleCallInfoForMaxCallDuration() {
		// setup
		callCollection = new CallCollectionImpl(new InMemoryHousekeepingCollectionImpl<CallInfo>());
		callInfo = new CallInfo(beanName, callBean.generateCallId(), dialogInfo1.getId(), dialogInfo2.getId(), AutoTerminateAction.False, 5);
		callInfo.setCallLegConnectionState(dialogInfo1.getId(), CallLegConnectionState.Completed);
		callInfo.setMediaNegotiationState(MediaNegotiationState.ProxiedOffer);
		callCollection.add(callInfo);
		callBean.setCallCollection(callCollection);
		MaxCallDurationScheduler scheduler = EasyMock.createMock(MaxCallDurationScheduler.class);
		scheduler.terminateCallAfterMaxDuration(EasyMock.isA(CallInfo.class), EasyMock.isA(CallBean.class));
		EasyMock.replay(scheduler);
		callBean.setMaxCallDurationScheduler(scheduler);

		// act
		callBean.onCallLegConnected(new CallLegConnectedEvent(dialogInfo2.getId(), callInfo.getId(), SessionDescriptionHelper.generateHoldMediaDescription()));

		// assert
		EasyMock.verify(scheduler);
	}

	/**
	 * This tests that we don't add the call info to the max call duration scheduler
	 * if we don't pass it in.
	 */
	@Test
	public void doNotScheduleCallInfoForMaxCallDuration() {
		// setup
		MaxCallDurationScheduler scheduler = EasyMock.createMock(MaxCallDurationScheduler.class);
		EasyMock.replay(scheduler);
		callBean.setMaxCallDurationScheduler(scheduler);
		dialogInfo1.setDialogState(DialogState.Confirmed);
		dialogInfo2.setDialogState(DialogState.Confirmed);

		// act
		callBean.onCallLegConnected(new CallLegConnectedEvent(dialogInfo2.getId(), null, null));

		// assert
		EasyMock.verify(scheduler);
	}

	/**
	 * This tests that we de-schedule a max call duration call if it terminates before the
	 * max duration
	 */
	@Test
	public void deScheduleCallIfTerminatesEarlier() {
		callCollection = new CallCollectionImpl(new InMemoryHousekeepingCollectionImpl<CallInfo>());
		callInfo = new CallInfo(beanName, callBean.generateCallId(), dialogInfo1.getId(), dialogInfo2.getId(), AutoTerminateAction.False, 5);
		callInfo.setCallLegConnectionState(dialogInfo1.getId(), CallLegConnectionState.Completed);
		callInfo.setMediaNegotiationState(MediaNegotiationState.ProxiedOffer);
		callCollection.add(callInfo);
		callBean.setCallCollection(callCollection);
		MaxCallDurationScheduler scheduler = EasyMock.createMock(MaxCallDurationScheduler.class);
		scheduler.terminateCallAfterMaxDuration(EasyMock.isA(CallInfo.class), EasyMock.isA(CallBean.class));
		scheduler.cancelTerminateCall(EasyMock.isA(CallInfo.class));
		EasyMock.replay(scheduler);
		callBean.setMaxCallDurationScheduler(scheduler);
		callBean.onCallLegConnected(new CallLegConnectedEvent(dialogInfo2.getId(), callInfo.getId(), SessionDescriptionHelper.generateHoldMediaDescription()));

		// act
		callBean.onCallLegTerminated(new CallLegTerminatedEvent(dialogInfo2.getId(), TerminationCause.TerminatedByServer));

		// assert
		EasyMock.verify(scheduler);
	}

	/**
	 * This tests that we don't call de-schedule if the call doesn't have a max duration
	 */
	@Test
	public void dontDeScheduleCallsThatDontHaveMaxDuration() {
		callCollection.replace(callInfo);
		callBean.setCallCollection(callCollection);
		MaxCallDurationScheduler scheduler = EasyMock.createMock(MaxCallDurationScheduler.class);
		EasyMock.replay(scheduler);
		callBean.setMaxCallDurationScheduler(scheduler);
		callBean.onCallLegRefreshCompleted(new CallLegRefreshCompletedEvent(dialogInfo2.getId(), callInfo.getId(), SessionDescriptionHelper.generateHoldMediaDescription()));

		// actt
		callBean.onCallLegTerminated(new CallLegTerminatedEvent(dialogInfo2.getId(), TerminationCause.TerminatedByServer));

		// assert
		EasyMock.verify(scheduler);

	}

	/**
	 * This tests that we don't terminate the dialogs if the call is already terminated
	 */
	@Test
	public void terminateCalldoesNotTerminateDialogsIfCallAlreadyTerminated() {
		// setup
		callInfo.setCallState(CallState.Terminated);
        callCollection.replace(callInfo);
		outboundCallLegBean = EasyMock.createMock(OutboundCallLegBean.class);
		outboundCallLegBean.addOutboundCallLegListener(callBean);
		EasyMock.replay(outboundCallLegBean);
		callBean.setOutboundCallLegBean(outboundCallLegBean);

		//act
		callBean.terminateCall(callInfo.getId());

		// assert
		EasyMock.verify(outboundCallLegBean);
	}


	/**
	 * Don't throw exceptions if callInfo is null from a dialog event
	 */
	@Test
	public void noExceptionsForNullCallInfo() {
		// setup
		dialogInfo1.setDialogState(DialogState.Confirmed);
		dialogInfo2.setDialogState(DialogState.Confirmed);

		// act
		callBean.onCallLegTerminated(new CallLegTerminatedEvent("bad id", TerminationCause.TerminatedByServer));
		callBean.onCallLegDisconnected(new CallLegDisconnectedEvent("bad id", TerminationCause.TerminatedByServer));
		callBean.onCallLegAlerting(new CallLegAlertingEvent("bad id"));
		callBean.onCallLegConnected(new CallLegConnectedEvent("bad id", null, null));
		callBean.onCallLegConnectionFailed(new CallLegConnectionFailedEvent("bad id", TerminationCause.TerminatedByServer));
		callBean.onCallLegTerminationFailed(new CallLegTerminationFailedEvent("bad id", TerminationCause.SipSessionError));

		// assert
		// no exception should be thrown
	}

	/**
	 * Make sure call bean adds itself to the outbound dialog bean list of dialog listeners
	 */
	@Test
	public void callBeanIsListenerOfOutboundDialogBean() {
		// assert
		EasyMock.verify(outboundCallLegBean);
	}

	/**
	 * Make sure we remove ourselves from the list of listeners when we set another dialog bean
	 */
	@Test
	public void removeListenerBeforeResettingOutboundListener() {
		// setup
		outboundCallLegBean = EasyMock.createMock(OutboundCallLegBean.class);
		outboundCallLegBean.addOutboundCallLegListener(callBean);
		outboundCallLegBean.removeOutboundCallLegListener(callBean);
		EasyMock.replay(outboundCallLegBean);
		callBean.setOutboundCallLegBean(outboundCallLegBean);
		OutboundCallLegBean anotherOutboundDialogBean = EasyMock.createMock(OutboundCallLegBean.class);
		anotherOutboundDialogBean.addOutboundCallLegListener(callBean);
		EasyMock.replay(anotherOutboundDialogBean);

		// act
		callBean.setOutboundCallLegBean(anotherOutboundDialogBean);

		// assert
		EasyMock.verify(outboundCallLegBean);
		EasyMock.verify(anotherOutboundDialogBean);
	}

	@Test
	public void testCallIdGeneration() throws Exception {
		// act
		String callId = ((CallBeanImpl)callBean).generateCallId();

		// assert
		assertNotNull(callId);
	}

	/**
	 * make sure we can add call listeners
	 */
	@Test
	public void addCallListener() {
		// setup
		CallListener listener = EasyMock.createMock(CallListener.class);

		// act
		callBean.addCallListener(listener);

		// assert
		assertEquals(1, callBean.getCallListeners().size());
		assertSame(listener, callBean.getCallListeners().get(0));
	}

	/**
	 * make sure we can remove call listeners
	 */
	@Test
	public void removeCallListener() {
		// setup
		CallListener listener = EasyMock.createMock(CallListener.class);
		callBean.addCallListener(listener);

		// act
		callBean.removeCallListener(listener);

		// assert
		assertEquals(0, callBean.getCallListeners().size());
	}

    // Test that setting inbound dialog register the call bean as the listener for inbound events
    @Test
    public void testSetInboundDialogBeanAddsCallBeanAsListener() {
    	// setup
    	inboundCallLegBean = EasyMock.createMock(InboundCallLegBean.class);
    	inboundCallLegBean.addInboundCallLegListener(callBean);
    	EasyMock.replay(inboundCallLegBean);

    	// act
    	callBean.setInboundCallLegBean(inboundCallLegBean);

    	// assert
    	EasyMock.verify(inboundCallLegBean);
    }

    /**
     * Tests that we remove the call bean as a listener before we reset the inbound dialog bean
     */
    @Test
    public void removeCallBeanWhenWeResetInboundDialogBean() {
    	// setup
    	inboundCallLegBean = EasyMock.createMock(InboundCallLegBean.class);
    	inboundCallLegBean.addInboundCallLegListener(callBean);
    	inboundCallLegBean.removeInboundCallLegListener(callBean);
    	EasyMock.replay(inboundCallLegBean);
    	InboundCallLegBean anotherInbound = EasyMock.createMock(InboundCallLegBean.class);
    	anotherInbound.addInboundCallLegListener(callBean);
    	EasyMock.replay(anotherInbound);
    	callBean.setInboundCallLegBean(inboundCallLegBean);

    	// act
    	callBean.setInboundCallLegBean(anotherInbound);

    	// assert
    	EasyMock.verify(inboundCallLegBean);
    	EasyMock.verify(anotherInbound);
    }

    // make sure we filter out the onIncomingDialog even from call bean
    @Test
    public void filterOutIncomingDialogEvent() {
    	// assert
    	assertTrue(callBean.shouldDeliverEvent(new CallLegConnectedEvent(new DialogConnectedEvent("anid", null, null))));
    	assertFalse(callBean.shouldDeliverEvent(new IncomingCallLegEvent(new IncomingDialogEvent("anid"))));
    }

    // check if the termination cause is set correctly when we receive terminatedEvent for first participant
	@Test
	public void testTerminationCauseWhenFirstParticipantTerminated() throws Exception {
		// setup
		dialogInfo1.setDialogState(DialogState.Confirmed);
		dialogInfo2.setDialogState(DialogState.Confirmed);

		// act
		callBean.onCallLegTerminated(new CallLegTerminatedEvent(dialogInfo1.getId(), TerminationCause.TerminatedByServer));

		// assert
		assertEquals(CallState.Terminated, callCollection.get(callInfo.getId()).getCallState());
        assertEquals(CallTerminationCause.TerminatedByApplication, callCollection.get(callInfo.getId()).getCallTerminationCause());
		assertEquals(CallLegCausingTermination.Neither, callCollection.get(callInfo.getId()).getCallLegCausingTermination());
	}

	// check if the termination cause is set correctly when we receive terminatedEvent for second participant
	@Test
	public void testTerminationCauseWhenSecondParticipantTerminated() throws Exception {
		// setup
		dialogInfo1.setDialogState(DialogState.Confirmed);
		dialogInfo2.setDialogState(DialogState.Confirmed);

		// act
		callBean.onCallLegTerminated(new CallLegTerminatedEvent(dialogInfo2.getId(), TerminationCause.TerminatedByServer));

		// assert
        assertEquals(CallState.Terminated, callCollection.get(callInfo.getId()).getCallState());
		assertEquals(CallTerminationCause.TerminatedByApplication, callCollection.get(callInfo.getId()).getCallTerminationCause());
		assertEquals(CallLegCausingTermination.Neither, callCollection.get(callInfo.getId()).getCallLegCausingTermination());
	}

    // check if the termination cause is set correctly when we receive disconnectedEvent for first participant
	@Test
	public void testTerminationCauseWhenFirstParticipantDisconnected() throws Exception {
		// setup
		dialogInfo1.setDialogState(DialogState.Confirmed);
		dialogInfo2.setDialogState(DialogState.Confirmed);

		// act
		callBean.onCallLegDisconnected(new CallLegDisconnectedEvent(dialogInfo1.getId(), TerminationCause.RemotePartyHungUp));

		// assert
		assertEquals(CallTerminationCause.RemotePartyHungUp, callCollection.get(callInfo.getId()).getCallTerminationCause());
		assertEquals(CallLegCausingTermination.First, callCollection.get(callInfo.getId()).getCallLegCausingTermination());
	}

	// check if the termination cause is set correctly when we receive disconnectedEvent for second participant
	@Test
	public void testTerminationCauseWhenSecondParticipantDisconnected() throws Exception {
		// setup
		dialogInfo1.setDialogState(DialogState.Confirmed);
		dialogInfo2.setDialogState(DialogState.Confirmed);

		// act
		callBean.onCallLegDisconnected(new CallLegDisconnectedEvent(dialogInfo2.getId(), TerminationCause.RemotePartyHungUp));

		// assert
		assertEquals(CallTerminationCause.RemotePartyHungUp, callCollection.get(callInfo.getId()).getCallTerminationCause());
		assertEquals(CallLegCausingTermination.Second, callCollection.get(callInfo.getId()).getCallLegCausingTermination());
	}

    // check if the termination cause is set correctly when we receive connectionFailedEvent for first participant
	@Test
	public void testTerminationCauseWhenFirstParticipantFailsToConnect() throws Exception {
		// setup
		dialogInfo1.setDialogState(DialogState.Early);
		dialogInfo2.setDialogState(DialogState.Early);

		// act
		callBean.onCallLegConnectionFailed(new CallLegConnectionFailedEvent(dialogInfo1.getId(), TerminationCause.RemotePartyBusy));

		// assert
		assertEquals(CallTerminationCause.RemotePartyBusy, callCollection.get(callInfo.getId()).getCallTerminationCause());
		assertEquals(CallLegCausingTermination.First, callCollection.get(callInfo.getId()).getCallLegCausingTermination());
	}

	// check if the termination cause is set correctly when we receive connectionFailedEvent for second participant
	@Test
	public void testTerminationCauseWhenSecondParticipantFailsToConnect() throws Exception {
		// setup
		dialogInfo1.setDialogState(DialogState.Early);
		dialogInfo2.setDialogState(DialogState.Early);

		// act
		callBean.onCallLegConnectionFailed(new CallLegConnectionFailedEvent(dialogInfo2.getId(), TerminationCause.RemotePartyBusy));

		// assert
		assertEquals(CallTerminationCause.RemotePartyBusy, callCollection.get(callInfo.getId()).getCallTerminationCause());
		assertEquals(CallLegCausingTermination.Second, callCollection.get(callInfo.getId()).getCallLegCausingTermination());
	}

	/**
	 * Tests that when max duration exceeded is a termination cause that the dialogs have terminatedbyserver as their reason
	 */
	@Test
	public void testTerminationCauseWhenTerminatedByMaxDurationExceeded() {
		// setup
		dialogInfo1.setDialogState(DialogState.Confirmed);
		dialogInfo2.setDialogState(DialogState.Confirmed);
		EasyMock.reset(outboundCallLegBean);
		outboundCallLegBean.terminateCallLeg(dialogInfo1.getId(), TerminationCause.TerminatedByServer);
		outboundCallLegBean.terminateCallLeg(dialogInfo2.getId(), TerminationCause.TerminatedByServer);
		EasyMock.replay(outboundCallLegBean);

		// act
		callBean.terminateCall(callInfo.getId(), CallTerminationCause.MaximumCallDurationExceeded);

		// assert
		ReadOnlyCallInfo info = callCollection.get(callInfo.getId());
		assertEquals(CallTerminationCause.MaximumCallDurationExceeded, info.getCallTerminationCause());
		EasyMock.verify(outboundCallLegBean);
	}

    // check if the end time is set when we receive terminatedEvent for first participant
	@Test
	public void testEndTimeSetWhenFirstParticipantTerminated() throws Exception {
		// setup
		dialogInfo1.setDialogState(DialogState.Confirmed);
		dialogInfo2.setDialogState(DialogState.Confirmed);
		assertEquals(CallInfo.TIME_NOT_SET, callCollection.get(callInfo.getId()).getEndTime());

		// act
		callBean.onCallLegTerminated(new CallLegTerminatedEvent(dialogInfo1.getId(), TerminationCause.TerminatedByServer));

		// assert
		assertTrue(callCollection.get(callInfo.getId()).getEndTime() > 0 );
	}

	// check if the end time is set when we receive terminatedEvent for second participant
	@Test
	public void testEndTimeSetWhenSecondParticipantTerminated() throws Exception {
		// setup
		dialogInfo1.setDialogState(DialogState.Confirmed);
		dialogInfo2.setDialogState(DialogState.Confirmed);
		assertEquals(CallInfo.TIME_NOT_SET, callCollection.get(callInfo.getId()).getEndTime());

		// act
		callBean.onCallLegTerminated(new CallLegTerminatedEvent(dialogInfo2.getId(), TerminationCause.TerminatedByServer));

		// assert
		assertTrue(callCollection.get(callInfo.getId()).getEndTime() > 0 );
	}

    // check if the end time is set when we receive disconnectedEvent for first participant
	@Test
	public void testEndTimeSetWhenFirstParticipantDisconnected() throws Exception {
		// setup
		dialogInfo1.setDialogState(DialogState.Confirmed);
		dialogInfo2.setDialogState(DialogState.Confirmed);
		assertEquals(CallInfo.TIME_NOT_SET, callCollection.get(callInfo.getId()).getEndTime());

		// act
		callBean.onCallLegDisconnected(new CallLegDisconnectedEvent(dialogInfo1.getId(), TerminationCause.TerminatedByServer));

		// assert
		assertTrue(callCollection.get(callInfo.getId()).getEndTime() > 0 );
	}

	// check if the end time is set when we receive disconnectedEvent for second participant
	@Test
	public void testEndTimeSetWhenSecondParticipantDisconnected() throws Exception {
		// setup
		dialogInfo1.setDialogState(DialogState.Confirmed);
		dialogInfo2.setDialogState(DialogState.Confirmed);
		assertEquals(CallInfo.TIME_NOT_SET, callCollection.get(callInfo.getId()).getEndTime());

		// act
		callBean.onCallLegDisconnected(new CallLegDisconnectedEvent(dialogInfo2.getId(), TerminationCause.TerminatedByServer));

		// assert
		assertTrue(callCollection.get(callInfo.getId()).getEndTime() > 0 );
	}

    // check if the end time is set when we receive connectionFailedEvent for first participant
	@Test
	public void testEndTimeSetWhenFirstParticipantFailsToConnect() throws Exception {
		// setup
		dialogInfo1.setDialogState(DialogState.Early);
		dialogInfo2.setDialogState(DialogState.Early);
		assertEquals(CallInfo.TIME_NOT_SET, callCollection.get(callInfo.getId()).getEndTime());

		// act
		callBean.onCallLegConnectionFailed(new CallLegConnectionFailedEvent(dialogInfo1.getId(), TerminationCause.TerminatedByServer));

		// assert
		assertTrue(callCollection.get(callInfo.getId()).getEndTime() > 0 );
	}

	// check if the end time is set when we receive connectionFailedEvent for second participant
	@Test
	public void testEndTimeSetWhenSecondParticipantFailsToConnect() throws Exception {
		// setup
		dialogInfo1.setDialogState(DialogState.Early);
		dialogInfo2.setDialogState(DialogState.Early);
		assertEquals(CallInfo.TIME_NOT_SET, callCollection.get(callInfo.getId()).getEndTime());

		// act
		callBean.onCallLegConnectionFailed(new CallLegConnectionFailedEvent(dialogInfo2.getId(), TerminationCause.TerminatedByServer));

		// assert
		assertTrue(callCollection.get(callInfo.getId()).getEndTime() > 0 );
	}

	// test that you cannot add a null listener
	@Test(expected=IllegalArgumentException.class)
	public void addNullListener() {
		// act
		callBean.addCallListener(null);

		// assert - exception
	}

	// test that you cannot remove a null listener
	@Test(expected=IllegalArgumentException.class)
	public void removeNullListener() {
		// act
		callBean.removeCallListener(null);

		// assert - exception
	}

	// test that a call bean originated dialog refresh event for a terminated call is thrown away
	@Test
	public void receivedCallBeanOriginatedRefreshEventForTerminatedCallThrownAway() {
		// setup
		MediaDescription mockMediaDescription = SessionDescriptionHelper.generateHoldMediaDescription();

		ReceivedDialogRefreshEvent dialogRefreshEvent = new ReceivedDialogRefreshEvent("dialog123", mockMediaDescription, "sip:remote", "callId", true);
		ReceivedCallLegRefreshEvent e = new ReceivedCallLegRefreshEvent(dialogRefreshEvent);

		OutboundCallLegBean outboundCallLegBean = EasyMock.createNiceMock(OutboundCallLegBean.class);
		outboundCallLegBean.addOutboundCallLegListener(callBean);
		outboundCallLegBean.acceptReceivedMediaOffer(EasyMock.eq("dialog123"), EasyMock.isA(MediaDescription.class), EasyMock.eq(false), EasyMock.eq(true));
		EasyMock.replay(outboundCallLegBean);
		callBean.setOutboundCallLegBean(outboundCallLegBean);

		DialogInfo di = new DialogInfo("dialog123", "me", "1.2.3.4");
		di.setSessionDescription(SessionDescriptionHelper.createSessionDescription("127.0.0.1", "test"));
		di.setReinviteInProgess(ReinviteInProgress.SendingReinviteWithoutSessionDescription);
		dialogCollection.add(di);

		CallInfo ci = new CallInfo("me", "callId", "dialog123", "dialog456", AutoTerminateAction.False, 0);
		ci.setCallState(CallState.Terminated);
		ci.setCallLegConnectionState("dialog123", CallLegConnectionState.Completed);
		callCollection.add(ci);

		// act
		callBean.onReceivedCallLegRefresh(e);

		// assert
		EasyMock.verify(outboundCallLegBean);
	}

	// test that a received dialog refresh event for a terminated call is thrown away
	@Test
	public void receivedReinviteRefreshEventForTerminatedCallThrownAway() {
		// setup
		MediaDescription mockMediaDescription = SessionDescriptionHelper.generateHoldMediaDescription();

		ReceivedDialogRefreshEvent dialogRefreshEvent = new ReceivedDialogRefreshEvent("dialog123", mockMediaDescription, "sip:remote", null, false);
		ReceivedCallLegRefreshEvent e = new ReceivedCallLegRefreshEvent(dialogRefreshEvent);

		OutboundCallLegBean outboundCallLegBean = EasyMock.createNiceMock(OutboundCallLegBean.class);
		outboundCallLegBean.addOutboundCallLegListener(callBean);
		outboundCallLegBean.acceptReceivedMediaOffer(EasyMock.eq("dialog123"), EasyMock.isA(MediaDescription.class), EasyMock.eq(false), EasyMock.eq(true));
		EasyMock.replay(outboundCallLegBean);
		callBean.setOutboundCallLegBean(outboundCallLegBean);

		DialogInfo di = new DialogInfo("dialog123", "me", "1.2.3.4");
		di.setSessionDescription(SessionDescriptionHelper.createSessionDescription("127.0.0.1", "test"));
		di.setReinviteInProgess(ReinviteInProgress.ReceivedReinvite);
		dialogCollection.add(di);

		CallInfo ci = new CallInfo("me", "callId", "dialog123", "dialog456", AutoTerminateAction.False, 0);
		ci.setCallState(CallState.Terminated);
		ci.setCallLegConnectionState("dialog123", CallLegConnectionState.Completed);
		callCollection.add(ci);

		// act
		callBean.onReceivedCallLegRefresh(e);

		// assert
		EasyMock.verify(outboundCallLegBean);
	}

	private DialogInfo createOutboundDialogInfoMock(DialogState dialogState) {
		DialogInfo outboundDialogInfo = EasyMock.createMock(DialogInfo.class);
		EasyMock.expect(outboundDialogInfo.isInbound()).andReturn(false);
		EasyMock.expect(outboundDialogInfo.getDialogState()).andReturn(dialogState);
		EasyMock.replay(outboundDialogInfo);
		return outboundDialogInfo;
	}

	private DialogInfo createInboundDialogInfoMock(DialogState dialogState) {
		DialogInfo inboundDialogInfo = EasyMock.createMock(DialogInfo.class);
		EasyMock.expect(inboundDialogInfo.isInbound()).andReturn(true);
		EasyMock.expect(inboundDialogInfo.getDialogState()).andReturn(dialogState);
		EasyMock.replay(inboundDialogInfo);
		return inboundDialogInfo;
	}

	@Test
	public void testDialogInfoToCallConnectionStatusMappnig() {
		// setup
		CallBeanImpl callBeanImpl = new CallBeanImpl();

		// assert
		assertEquals(CallLegConnectionState.Pending, callBeanImpl.mapDialogInfoStateToCallConnectionState(createOutboundDialogInfoMock(DialogState.Created)));
		assertEquals(CallLegConnectionState.InProgress, callBeanImpl.mapDialogInfoStateToCallConnectionState(createOutboundDialogInfoMock(DialogState.Initiated)));
		assertEquals(CallLegConnectionState.InProgress, callBeanImpl.mapDialogInfoStateToCallConnectionState(createOutboundDialogInfoMock(DialogState.Early)));
		assertEquals(CallLegConnectionState.Completed, callBeanImpl.mapDialogInfoStateToCallConnectionState(createOutboundDialogInfoMock(DialogState.Confirmed)));
		assertEquals(CallLegConnectionState.Completed, callBeanImpl.mapDialogInfoStateToCallConnectionState(createOutboundDialogInfoMock(DialogState.Terminated)));

		assertEquals(CallLegConnectionState.Pending, callBeanImpl.mapDialogInfoStateToCallConnectionState(createInboundDialogInfoMock(DialogState.Created)));
		assertEquals(CallLegConnectionState.Pending, callBeanImpl.mapDialogInfoStateToCallConnectionState(createInboundDialogInfoMock(DialogState.Initiated)));
		assertEquals(CallLegConnectionState.InProgress, callBeanImpl.mapDialogInfoStateToCallConnectionState(createInboundDialogInfoMock(DialogState.Early)));
		assertEquals(CallLegConnectionState.Completed, callBeanImpl.mapDialogInfoStateToCallConnectionState(createInboundDialogInfoMock(DialogState.Confirmed)));
		assertEquals(CallLegConnectionState.Completed, callBeanImpl.mapDialogInfoStateToCallConnectionState(createInboundDialogInfoMock(DialogState.Terminated)));
	}

	/**
	 * This tests that onTerminationFailed sets the CallStatus of the call to Terminated and fires off a CallTerminationFailedEvent
	 */
	@Test
	public void onCallTerminationFailedSetsCallStateToTerminated() {
		// setup
		dialogInfo1.setDialogState(DialogState.Confirmed);
		dialogInfo2.setDialogState(DialogState.Confirmed);
		dispatcher = EasyMock.createMock(EventDispatcher.class);
		EasyMock.expect(dispatcher.dispatchEvent(EasyMock.isA(List.class), EasyMock.isA(CallTerminationFailedEvent.class))).andReturn(0);
		EasyMock.replay(dispatcher);
		callBean.setEventDispatcher(dispatcher);

		// act
		callBean.onCallLegTerminationFailed(new CallLegTerminationFailedEvent(dialogInfo2.getId(), TerminationCause.SipSessionError));

		// assert
		assertEquals(CallState.Terminated, callCollection.get(callInfo.getId()).getCallState());
		EasyMock.verify(dispatcher);
	}

	/**
	 * This tests that onTerminationFailed wont fire the onCallTerminationFailed event
	 * if the call was already terminated
	 */
	@Test
	public void onTerminationFailedDoesntFireIfAlreadyTerminated() {
		// setup
		dialogInfo1.setDialogState(DialogState.Terminated);
		dialogInfo2.setDialogState(DialogState.Confirmed);
		dispatcher = EasyMock.createMock(EventDispatcher.class);
		callBean.setEventDispatcher(dispatcher);
		EasyMock.replay(dispatcher);
		callInfo.setCallState(CallState.Terminated);
        callCollection.replace(callInfo);

		// act
		callBean.onCallLegTerminationFailed(new CallLegTerminationFailedEvent(dialogInfo2.getId(), TerminationCause.SipSessionError));

		// assert
		assertEquals(CallState.Terminated, callCollection.get(callInfo.getId()).getCallState());
		EasyMock.verify(dispatcher);
	}

    // check if the termination cause is set correctly when we receive terminationFailedEvent for first participant
	@Test
	public void testTerminationCauseWhenFirstParticipantTerminationFailed() throws Exception {
		// setup
		dialogInfo1.setDialogState(DialogState.Confirmed);
		dialogInfo2.setDialogState(DialogState.Confirmed);

		// act
		callBean.onCallLegTerminationFailed(new CallLegTerminationFailedEvent(dialogInfo1.getId(), TerminationCause.SipSessionError));

		// assert
		assertEquals(CallState.Terminated, callCollection.get(callInfo.getId()).getCallState());
        assertEquals(CallTerminationCause.SipSessionError, callCollection.get(callInfo.getId()).getCallTerminationCause());
		assertEquals(CallLegCausingTermination.First, callCollection.get(callInfo.getId()).getCallLegCausingTermination());
	}

    // check if the termination cause is set correctly when we receive terminationFailedEvent for second participant
	@Test
	public void testTerminationCauseWhenSecondParticipantTerminationFailed() throws Exception {
		// setup
		dialogInfo1.setDialogState(DialogState.Confirmed);
		dialogInfo2.setDialogState(DialogState.Confirmed);

		// act
		callBean.onCallLegTerminationFailed(new CallLegTerminationFailedEvent(dialogInfo2.getId(), TerminationCause.SipSessionError));

		// assert
        assertEquals(CallState.Terminated, callCollection.get(callInfo.getId()).getCallState());
		assertEquals(CallTerminationCause.SipSessionError, callCollection.get(callInfo.getId()).getCallTerminationCause());
		assertEquals(CallLegCausingTermination.Second, callCollection.get(callInfo.getId()).getCallLegCausingTermination());
	}

    // check if the end time is set when we receive terminationFailedEvent for first participant
	@Test
	public void testEndTimeSetWhenFirstParticipantTerminationFailed() throws Exception {
		// setup
		dialogInfo1.setDialogState(DialogState.Confirmed);
		dialogInfo2.setDialogState(DialogState.Confirmed);
		assertEquals(CallInfo.TIME_NOT_SET, callCollection.get(callInfo.getId()).getEndTime());

		// act
		callBean.onCallLegTerminationFailed(new CallLegTerminationFailedEvent(dialogInfo1.getId(), TerminationCause.SipSessionError));

		// assert
		assertTrue(callCollection.get(callInfo.getId()).getEndTime() > 0 );
	}

	// check if the end time is set when we receive terminationFailedEvent for second participant
	@Test
	public void testEndTimeSetWhenSecondParticipantTerminationFailed() throws Exception {
		// setup
		dialogInfo1.setDialogState(DialogState.Confirmed);
		dialogInfo2.setDialogState(DialogState.Confirmed);
		assertEquals(CallInfo.TIME_NOT_SET, callCollection.get(callInfo.getId()).getEndTime());

		// act
		callBean.onCallLegTerminationFailed(new CallLegTerminationFailedEvent(dialogInfo2.getId(), TerminationCause.SipSessionError));

		// assert
		assertTrue(callCollection.get(callInfo.getId()).getEndTime() > 0 );
	}
}
