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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bt.aloha.call.CallBean;
import com.bt.aloha.call.CallBeanImpl;
import com.bt.aloha.call.CallInformation;
import com.bt.aloha.call.CallListener;
import com.bt.aloha.call.MaxCallDurationScheduler;
import com.bt.aloha.call.MaxCallDurationTermination;
import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.call.collections.CallCollectionImpl;
import com.bt.aloha.call.state.CallInfo;
import com.bt.aloha.call.state.CallState;
import com.bt.aloha.call.state.CallTerminationCause;
import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.callleg.event.CallLegAlertingEvent;
import com.bt.aloha.callleg.event.CallLegConnectedEvent;
import com.bt.aloha.callleg.event.CallLegConnectionFailedEvent;
import com.bt.aloha.callleg.event.CallLegDisconnectedEvent;
import com.bt.aloha.callleg.event.CallLegRefreshCompletedEvent;
import com.bt.aloha.callleg.event.CallLegTerminatedEvent;
import com.bt.aloha.callleg.event.CallLegTerminationFailedEvent;
import com.bt.aloha.callleg.event.ReceivedCallLegRefreshEvent;
import com.bt.aloha.collections.memory.InMemoryHousekeepingCollectionImpl;

public class MaxCallDurationTerminationTest {

	private MaxCallDurationTermination maxCallDurationTermination;
	private CallCollection callCollection;
	private CallInfo callInfo;
	private MaxCallDurationScheduler maxCallDurationScheduler;
	private final static int FIVE_MINUTES_IN_MILLISECONDS = 300000;
	private final static String CALL_ID_SET_TO_TERMINATE_IN_PAST = "CALL_ID_SET_TO_TERMINATE_IN_PAST";
	private Log log = LogFactory.getLog(this.getClass());
	private boolean terminateCallCalled = false;
	private CallBean callBean;
	private boolean terminateWithReason;

	@Before
	public void setup(){
		maxCallDurationTermination = new MaxCallDurationTermination();
		maxCallDurationTermination.setRunOnStartup("true");
		callCollection = new CallCollectionImpl(new InMemoryHousekeepingCollectionImpl<CallInfo>());
		maxCallDurationScheduler = EasyMock.createMock(MaxCallDurationScheduler.class);
		maxCallDurationTermination.setMaxCallDurationScheduler(maxCallDurationScheduler);
		maxCallDurationTermination.setCallCollection(callCollection);
		createCallBeanStub();
	}
	
	private void createCallBeanStub() {
		terminateWithReason = false;
		callBean = new CallBean() {
			public void addCallListener(CallListener callListener) {
				throw new RuntimeException();
			}

			public CallInformation getCallInformation(String callId) {
				throw new RuntimeException();
			}

			public String joinCallLegs(String firstDialogId, String secondDialogId) {
				throw new RuntimeException();
			}

			public String joinCallLegs(String firstDialogId, String secondDialogId, AutoTerminateAction autoTerminateCallLegs) {
				throw new RuntimeException();
			}

			public String joinCallLegs(String firstDialogId, String secondDialogId, AutoTerminateAction autoTerminationCallLegs, long durationInMinutes) {
				throw new RuntimeException();
			}

			public void removeCallListener(CallListener callListener) {
				throw new RuntimeException();
			}

			public void terminateCall(String callId) {
				throw new RuntimeException();
			}

			public void terminateCall(String callId, CallTerminationCause callTerminationCause) {
				if (callId.equals(CALL_ID_SET_TO_TERMINATE_IN_PAST) && callTerminationCause.equals(CallTerminationCause.MaximumCallDurationExceeded))
					terminateWithReason = true;
				else throw new RuntimeException();
			}

			public void onCallLegAlerting(CallLegAlertingEvent alertingEvent) {
				throw new RuntimeException();
			}

			public void onCallLegConnected(CallLegConnectedEvent connectedEvent) {
				throw new RuntimeException();
			}

			public void onCallLegConnectionFailed(CallLegConnectionFailedEvent connectionFailedEvent) {
				throw new RuntimeException();
			}

			public void onCallLegDisconnected(CallLegDisconnectedEvent disconnectedEvent) {
				throw new RuntimeException();
			}

			public void onCallLegRefreshCompleted(CallLegRefreshCompletedEvent callLegConnectedEvent) {
				throw new RuntimeException();
			}

			public void onCallLegTerminated(CallLegTerminatedEvent terminatedEvent) {
				throw new RuntimeException();
			}

			public void onCallLegTerminationFailed(CallLegTerminationFailedEvent terminationFailedEvent) {
				throw new RuntimeException();
			}

			public void onReceivedCallLegRefresh(ReceivedCallLegRefreshEvent receivedCallLegRefreshEvent) {
				throw new RuntimeException();
			}
		};
	}
	
	// test that if runOnStartup is set to false, initialize does nothing
	@Test
	public void testInitializeWithRunOnStartupSetToFalse() throws Exception {
		// setup
		maxCallDurationTermination.setRunOnStartup("false");
		callInfo = new CallInfo("test", CALL_ID_SET_TO_TERMINATE_IN_PAST, "1", "2", AutoTerminateAction.False, 1);
		if (!callInfo.setStartTime(System.currentTimeMillis() - FIVE_MINUTES_IN_MILLISECONDS))
			throw new RuntimeException("Can't set time in the past for some reason");
		callInfo.setCallState(CallState.Connected);
		callCollection.add(callInfo);
		maxCallDurationTermination.setCallBean(callBean);
		
		// act
		maxCallDurationTermination.initialize();
		
		// assert
		assertFalse(terminateWithReason);
	}

	// Test the initialise method, ensure that the call "12345" gets a termination request as the time scheduled
	// to be terminated should be in the past
	@Test
	public void testInitializeWithOneTermination() {
		// setup
		ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
		te.initialize();
		maxCallDurationTermination.setTaskExecutor(te);
		callInfo = new CallInfo("test", CALL_ID_SET_TO_TERMINATE_IN_PAST, "1", "2", AutoTerminateAction.False, 1);
		if (!callInfo.setStartTime(System.currentTimeMillis() - FIVE_MINUTES_IN_MILLISECONDS))
			throw new RuntimeException("Can't set time in the past for some reason");
		callInfo.setCallState(CallState.Connected);
		callCollection.add(callInfo);
		maxCallDurationTermination.setCallBean(callBean);
		
		// act
		maxCallDurationTermination.runTask();
		try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); } // allow time for thread to run
		
		// assert
		assertTrue(terminateWithReason);
	}

	// Test the initialise method, ensure that the call "5678" gets NO termination request as the time scheduled
	// to be terminated should be in the past
	@Test
	public void testInitializeWithoutTermination() {
		// setup
		callInfo = new CallInfo("test", "5678", "1", "2", AutoTerminateAction.False, 5);
		callInfo.setCallState(CallState.Connected);
		callCollection.add(callInfo);

		maxCallDurationScheduler = new MaxCallDurationScheduler(){
			public void terminateCallAfterMaxDuration(CallInfo aCallInfo, CallBean aCallBean){
				log.debug(String.format("TEST DEBUG: Expecting callId = %s, got callId = %s", "5678", aCallInfo.getId()));
				if (aCallInfo.getId() != "5678")
					throw new RuntimeException("Unexpected call id");
			}
			public void cancelTerminateCall(CallInfo callInfo) {
			}
		};

		maxCallDurationScheduler.terminateCallAfterMaxDuration(callInfo, callBean);
		maxCallDurationTermination.setCallBean(callBean);
		maxCallDurationTermination.setMaxCallDurationScheduler(maxCallDurationScheduler);
		
		// act
		maxCallDurationTermination.initialize();
		
		// assert
		assertFalse(terminateWithReason);
	}

	// when a call has no max duration set, treat as infinite and do not terminate
	@Test
	public void testInitializeWithOneCallWithNoLimit() {
		// setup
		callInfo = new CallInfo("test", "5678", "1", "2", AutoTerminateAction.False, 0);
		callInfo.setCallState(CallState.Connected);
		callCollection.add(callInfo);

		maxCallDurationScheduler = new MaxCallDurationScheduler(){
			public void terminateCallAfterMaxDuration(CallInfo aCallInfo, CallBean aCallBean){
				log.debug(String.format("TEST DEBUG: Expecting callId = %s, got callId = %s", "5678", aCallInfo.getId()));
				if (aCallInfo.getId() != "5678")
					throw new RuntimeException("Unexpected call id");
			}
			public void cancelTerminateCall(CallInfo callInfo) {
			}
		};

		maxCallDurationTermination.setCallBean(callBean);
		maxCallDurationTermination.setMaxCallDurationScheduler(maxCallDurationScheduler);
		
		// act
		maxCallDurationTermination.initialize();
		
		// assert
		assertFalse(terminateWithReason);
	}

    // Test that we handle an exception thrown by the call bean
    @Test
    public void testInitializeCallBeanException() {
        // setup
        terminateCallCalled = false;
        callInfo = new CallInfo("test", CALL_ID_SET_TO_TERMINATE_IN_PAST, "1", "2", AutoTerminateAction.False, 1);
        if (!callInfo.setStartTime(System.currentTimeMillis() - FIVE_MINUTES_IN_MILLISECONDS))
            throw new RuntimeException("Can't set time in the past for some reason");
        callInfo.setCallState(CallState.Connected);
        callCollection.add(callInfo);

        CallBean callBean = new CallBeanImpl(){

            @Override
            public void terminateCall(String callId, CallTerminationCause callTerminationCause) {
                terminateCallCalled = true;
                throw new RuntimeException("unable to terminate call");
            }
        };
        maxCallDurationTermination.setCallBean(callBean);
        
        // act
        maxCallDurationTermination.initialize();
        
        // assert
        assertTrue(terminateCallCalled);
    }

    // Test that we handle an exception thrown by the termination scheduler
    @Test
    public void testInitializeSchedulerException() {
        // setup
        terminateCallCalled = false;
        callInfo = new CallInfo("test", "5678", "1", "2", AutoTerminateAction.False, 1);
        callInfo.setCallState(CallState.Connected);
        callCollection.add(callInfo);

        maxCallDurationScheduler = new MaxCallDurationScheduler(){
            public void terminateCallAfterMaxDuration(CallInfo aCallInfo, CallBean aCallBean){
                terminateCallCalled = true;
                throw new RuntimeException("Unexpected call id");
            }
            public void cancelTerminateCall(CallInfo callInfo) {
            }
        };

        maxCallDurationTermination.setCallBean(callBean);
        maxCallDurationTermination.setMaxCallDurationScheduler(maxCallDurationScheduler);

        // act
        maxCallDurationTermination.initialize();
        
        // assert
        assertTrue(terminateCallCalled);
		assertFalse(terminateWithReason);
    }
    
    // test re-scheduling a call with a termination time
    @Test
    public void testInitializeCallToBeTerminated() {
		// setup
		callInfo = new CallInfo("test", "qwe123", "1", "2", AutoTerminateAction.False, 10);
		if (!callInfo.setStartTime(System.currentTimeMillis() - FIVE_MINUTES_IN_MILLISECONDS))
			throw new RuntimeException("Can't set time in the past for some reason");
		callInfo.setCallState(CallState.Connected);
		callCollection.add(callInfo);
		
		maxCallDurationTermination.setCallBean(callBean);
		
		final List<String> schedulerCallIds = new ArrayList<String>();
		
		maxCallDurationScheduler = new MaxCallDurationScheduler(){
			public void terminateCallAfterMaxDuration(CallInfo aCallInfo, CallBean aCallBean){
				schedulerCallIds.add(aCallInfo.getId());
			}
			public void cancelTerminateCall(CallInfo callInfo) {
			}
		};
		maxCallDurationTermination.setMaxCallDurationScheduler(maxCallDurationScheduler);

		// act
		maxCallDurationTermination.initialize();
		
		// assert
		assertTrue(schedulerCallIds.contains("qwe123"));
		assertFalse(terminateWithReason);
    }
}
