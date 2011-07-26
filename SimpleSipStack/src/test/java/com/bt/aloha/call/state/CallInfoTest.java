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
package com.bt.aloha.call.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.call.state.CallInfo;
import com.bt.aloha.call.state.CallLegCausingTermination;
import com.bt.aloha.call.state.CallLegConnectionState;
import com.bt.aloha.call.state.CallState;
import com.bt.aloha.call.state.CallTerminationCause;
import com.bt.aloha.call.state.ReadOnlyCallInfo;
import com.bt.aloha.callleg.AutoTerminateAction;

public class CallInfoTest {
	private CallInfo callInfo;

	@Before
	public void before() {
		callInfo = new CallInfo("callBean", Double.toString(Math.random()), "oneId", "twoId", AutoTerminateAction.False, -1);
	}

	@Test
	public void initialStateIsConnectng() {
		// assert
		assertEquals(CallState.Connecting, callInfo.getCallState());
	}

	@Test
	public void connectingToConnected() {
		// act
		CallState previousState = callInfo.setCallState(CallState.Connected);

		// assert
		assertEquals(CallState.Connecting, previousState);
		assertEquals(CallState.Connected, callInfo.getCallState());
	}

	@Test
	public void connectedToTerminated() {
		// setup
		callInfo.setCallState(CallState.Connected);

		// act
		CallState previousState = callInfo.setCallState(CallState.Terminated);

		// assert
		assertEquals(CallState.Connected, previousState);
		assertEquals(CallState.Terminated, callInfo.getCallState());
	}

	@Test
	public void connectingToTerminated() {
		// act
		CallState previousState = callInfo.setCallState(CallState.Terminated);

		// assert
		assertEquals(CallState.Connecting, previousState);
		assertEquals(CallState.Terminated, callInfo.getCallState());
	}

	@Test
	public void connectedToConnectingFails() {
		// setup
		callInfo.setCallState(CallState.Connected);

		// act
		CallState previousState = callInfo.setCallState(CallState.Connecting);

		// assert
		assertNull(previousState);
		assertEquals(CallState.Connected, callInfo.getCallState());
	}

	@Test
	public void terminatedToConnectingFails() {
		// setup
		callInfo.setCallState(CallState.Terminated);

		// act
		CallState previousState = callInfo.setCallState(CallState.Connecting);

		// assert
		assertNull(previousState);
		assertEquals(CallState.Terminated, callInfo.getCallState());
	}

	@Test
	public void terminatedToConnectedFails() {
		// setup
		callInfo.setCallState(CallState.Terminated);

		// act
		CallState previousState = callInfo.setCallState(CallState.Connected);

		// assert
		assertNull(previousState);
		assertEquals(CallState.Terminated, callInfo.getCallState());
	}

	//Test that we can set call state to Terminated and termination cause when there is no cause yet
	@Test
	public void testSetTerminationCauseWorksWhenNoCause() throws Exception {
		//setup
		//act/assert
		assertTrue(callInfo.setCallTerminationCause(CallTerminationCause.TerminatedByApplication, CallLegCausingTermination.Neither));
	}

	//Test that we don't set termination cause when there is cause already
	@Test
	public void testSetCallTerminatedWithTerminationCauseWorksWhenThereIsCause() throws Exception {
		//setup
		callInfo.setCallTerminationCause(CallTerminationCause.RemotePartyHungUp, CallLegCausingTermination.Second);

		//act/assert
		assertFalse(callInfo.setCallTerminationCause(CallTerminationCause.TerminatedByApplication, CallLegCausingTermination.Neither));
	}

    @Test
    public void testClone() throws Exception {
        // setup
        CallInfo c1 = callInfo;

        // act
        CallInfo c2 = c1.cloneObject();

        // assert
        assertEquals(c1.getId(), c2.getId());
        assertEquals(c1.getCallState(), c2.getCallState());
        assertEquals(c1.getCreateTime(), c2.getCreateTime());
        assertEquals(c1.getFirstDialogId(), c2.getFirstDialogId());
        assertEquals(c1.getFuture(), c2.getFuture());
        assertEquals(c1.getLastUsedTime(), c2.getLastUsedTime());
        assertEquals(c1.getVersionId(), c2.getVersionId());
        assertEquals(c1.getMaxDurationInMinutes(), c2.getMaxDurationInMinutes());
        assertEquals(c1.getSecondDialogId(), c2.getSecondDialogId());
    }

    //Test that we can set termination cause just once
    @Test
    public void testSetTerminationCausePossibleJustOnce() throws Exception {
    	//setup/act/assert
    	assertTrue(callInfo.setCallTerminationCause(CallTerminationCause.TerminatedByApplication, CallLegCausingTermination.First));
    	assertEquals(CallTerminationCause.TerminatedByApplication, callInfo.getCallTerminationCause());
    	assertFalse(callInfo.setCallTerminationCause(CallTerminationCause.RemotePartyHungUp, CallLegCausingTermination.Second));
    	assertEquals(CallTerminationCause.TerminatedByApplication, callInfo.getCallTerminationCause());
    	assertEquals(CallLegCausingTermination.First, callInfo.getCallLegCausingTermination());
    }
    //Test for termination cause - that it returns what was set
	@Test
	public void testGetTerminationCauseReturnsRightValue() throws Exception {
		//setup
		callInfo.setCallTerminationCause(CallTerminationCause.TerminatedByApplication, CallLegCausingTermination.Second);
		//act
		CallTerminationCause cause = callInfo.getCallTerminationCause();
		CallLegCausingTermination culprit = callInfo.getCallLegCausingTermination();
		//assert
		assertEquals(CallTerminationCause.TerminatedByApplication, cause);
		assertEquals(CallLegCausingTermination.Second, culprit);
	}

	//Test that termination cause doesn't have default value
	@Test
	public void testGetTerminationDoesntHaveDefaultValue() throws Exception {
		//setup
		ReadOnlyCallInfo info = new CallInfo("callBean", Double.toString(Math.random()), "oneId", "twoId", AutoTerminateAction.False, -1);
		//act
		CallTerminationCause cause = info.getCallTerminationCause();
		//assert
		assertNull(cause);
	}

    // test for when both times are set
	@Test
    public void testGetDurationBothTimesSet() throws Exception{
        long then = Calendar.getInstance().getTimeInMillis();
        Thread.sleep(1100);
        long now = Calendar.getInstance().getTimeInMillis();
        callInfo.setStartTime(then);

        callInfo.setEndTime(now);
        assertEquals(1, callInfo.getDuration());
    }

	// Test that we can set start time just once
	@Test
	public void testSetStartTimePossibleJustOnce() throws Exception {
		//setup/act/assert
		long t1 = Calendar.getInstance().getTimeInMillis();
		assertTrue(callInfo.setStartTime(t1));
		assertEquals(t1, callInfo.getStartTime());
		assertFalse(callInfo.setStartTime(Calendar.getInstance().getTimeInMillis()));
		assertEquals(t1, callInfo.getStartTime());
	}

	// Test that we can set end time just once
	@Test
	public void testSetEndTimePossibleJustOnce() throws Exception {
		//setup/act/assert
		assertTrue(callInfo.setEndTime(Calendar.getInstance().getTimeInMillis()));
		assertFalse(callInfo.setEndTime(Calendar.getInstance().getTimeInMillis()));
	}

    @Test
    public void testGetTransients() {
        // setup
        callInfo.setFuture(EasyMock.createNiceMock(ScheduledFuture.class));

        //act
        Map<?, ?> result = callInfo.getTransients();

        //assert
        assertEquals(1, result.size());
        assertTrue(result.containsKey("future"));
        assertTrue(result.get("future") instanceof ScheduledFuture);
    }

    @Test
    public void testSetTransients() {
        // setup
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("future", EasyMock.createNiceMock(ScheduledFuture.class));

        //act
        callInfo.setTransients(input);

        //assert
        assertNotNull(callInfo.getFuture());
        assertTrue(callInfo.getFuture() instanceof ScheduledFuture);
    }

    //
	@Test
	public void testSetCreatingBeanName() throws Exception {
		//setup
		//act
		//assert
		assertEquals("callBean", callInfo.getSimpleSipBeanId());
	}
	
	public void testCallLegConnectionStateGetter() throws Exception {
		// setup
		ReadOnlyCallInfo callInfo = new CallInfo("beano", "1", "2", "3", AutoTerminateAction.False, 100L);
		
		// assert
		assertEquals(CallLegConnectionState.Pending, callInfo.getCallLegConnectionState("2"));
		assertEquals(CallLegConnectionState.Pending, callInfo.getCallLegConnectionState("3"));		
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCallLegConnectionStateGetterBarfsOnBadId() throws Exception {
		// act
		callInfo.getCallLegConnectionState("wrong");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCallLegConnectionStateGetterBarfsOnNull() throws Exception {
		// act
		callInfo.getCallLegConnectionState(null);
	}

	public void testCallLegConnectionStateSetter() throws Exception {
		// setup
		CallInfo callInfoOne = new CallInfo("beano", "1", "2", "3", AutoTerminateAction.False, 100L);
		CallInfo callInfoTwo = new CallInfo("beano", "1", "2", "3", AutoTerminateAction.False, 100L);
		
		// act
		callInfoOne.setCallLegConnectionState("2", CallLegConnectionState.Completed);
		callInfoTwo.setCallLegConnectionState("3", CallLegConnectionState.Completed);
		
		// assert
		assertEquals(CallLegConnectionState.Completed, callInfo.getCallLegConnectionState("2"));
		assertEquals(CallLegConnectionState.Completed, callInfo.getCallLegConnectionState("3"));		
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCallLegConnectionStateSetterBarfsOnBadId() throws Exception {
		// act
		callInfo.setCallLegConnectionState("wrong", CallLegConnectionState.InProgress);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCallLegConnectionStateSetterBarfsOnNullId() throws Exception {
		// act
		callInfo.setCallLegConnectionState(null, CallLegConnectionState.InProgress);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCallLegConnectionStateSetterBarfsOnNullState() throws Exception {
		// act
		callInfo.setCallLegConnectionState("oneId", null);
	}
	
	@Test
	public void testBothCallLegsConnected() throws Exception {
		// setup
		callInfo.setCallLegConnectionState("oneId", CallLegConnectionState.Completed);
		callInfo.setCallLegConnectionState("twoId", CallLegConnectionState.Completed);
		
		// assert
		assertTrue(callInfo.areBothCallLegsConnected());
	}

	@Test
	public void testBothCallLegsNotConnected() throws Exception {
		// setup
		callInfo.setCallLegConnectionState("oneId", CallLegConnectionState.Completed);
		callInfo.setCallLegConnectionState("twoId", CallLegConnectionState.InProgress);
		
		// assert
		assertFalse(callInfo.areBothCallLegsConnected());
	}
}
