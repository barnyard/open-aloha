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
package com.bt.aloha.call.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ConcurrentMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.call.state.CallInfo;
import com.bt.aloha.call.state.CallState;
import com.bt.aloha.call.state.ReadOnlyCallInfo;
import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.collections.database.DatabaseInfoCollectionImpl;
import com.bt.aloha.util.ConcurrentUpdateException;
import com.bt.aloha.util.Housekeeper;

public class CallCollectionTest {

    private CallCollection callCollection;
    private CallInfo callInfo1;
    private CallInfo callInfo2;
    private ReadOnlyCallInfo callInfo;
	private ClassPathXmlApplicationContext applicationContext;
	private String beanName = "callBean";

    @Before
    public void setUp() throws Exception {
    	applicationContext = new ClassPathXmlApplicationContext("applicationContextTest.xml");
    	this.callCollection = (CallCollection)applicationContext.getBean("callCollection");
        callInfo1 = new CallInfo(beanName, "c1", "TIM", "JANE", AutoTerminateAction.False, -1);
        callInfo2 = new CallInfo(beanName, "c2", "TIM", "JANE", AutoTerminateAction.False, -1);
    }

    @After
    public void after() {
    	this.callCollection.destroy();
    	this.applicationContext.destroy();
    }

    // Keep Emma happy until we do something serious with destroy()
    @Test
    public void testDestroy() throws Exception {
        callCollection.destroy();
    }

    @Test
    public void testHouseKeeper() throws Exception {
        //setup
        CallInfo ci1 = new CallInfo(beanName, "c1", "d1", "d2", AutoTerminateAction.False, 0);
        ci1.setCallState(CallState.Terminated);
        CallInfo ci2 = new CallInfo(beanName, "c2", "d3", "d4", AutoTerminateAction.False, 0);
        ci2.setCallState(CallState.Terminated);
        CallInfo ci3 = new CallInfo(beanName, "c3", "d5", "d6", AutoTerminateAction.False, 0);
        ci3.setCallState(CallState.Connecting);
        this.callCollection.add(ci1);
        this.callCollection.add(ci2);
        this.callCollection.add(ci3);

        assertEquals(3, this.callCollection.size());

        this.callCollection.setMaxTimeToLive(1000);

        //act
        //wait for housekeeper to remove the old dialogInfos
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }

        ((Housekeeper)this.callCollection).housekeep();

        //assert
        assertEquals(1, this.callCollection.size());

        //post-test cleaning
        this.callCollection.destroy();
    }

    // test that the housekeeping doesn't retain any locks on the collection objects
    @Test
    public void testHouseKeeperReleasesLockAfterProcessing() throws Exception {
        //setup
        final CallInfo ci1 = new CallInfo(beanName, "c1", "d1", "d2", AutoTerminateAction.False, 0);
        this.callCollection.add(ci1);
        this.callInfo = null;

        assertEquals(1, this.callCollection.size());

        this.callCollection.setMaxTimeToLive(5000);

        //act
        //wait for housekeeper to remove the old dialogInfos
        Thread.sleep(1000);

        ((Housekeeper)this.callCollection).housekeep();

        //assert
        assertEquals(1, this.callCollection.size());

        Runnable runnable = new Runnable() {
            public void run() {
                ReadOnlyCallInfo ci = callCollection.get(ci1.getId());
                setCallInfo(ci);
            }
        };

        new Thread(runnable).start();

        Thread.sleep(500);

        assertNotNull(this.callInfo);

        //post-test cleaning
        this.callCollection.destroy();
    }

    private void setCallInfo(ReadOnlyCallInfo ci) {
        this.callInfo = ci;
    }

    // test that locks get released even if checking a CallInfo throws an exception
    @Test
    public void testHouseKeeperReleasesLockAfterException() throws Exception {
        if (this.applicationContext.getBean("callCollectionBacker") instanceof DatabaseInfoCollectionImpl)
            return;
        //setup
        final CallInfo ci1 = new CallInfo(beanName, "c1", "d1", "d2", AutoTerminateAction.False, 0) {
 			private static final long serialVersionUID = 5363796903727193179L;

			@Override
            public long getLastUsedTime() {
                throw new RuntimeException("shit happens");
            }
        };
        this.callCollection.add(ci1);
        this.callInfo = null;

        assertEquals(1, this.callCollection.size());

        this.callCollection.setMaxTimeToLive(5000);

        //act
        //wait for housekeeper to remove the old dialogInfos
        Thread.sleep(1000);

        ((Housekeeper)this.callCollection).housekeep();

        //assert
        assertEquals(1, this.callCollection.size());

        Runnable runnable = new Runnable() {
            public void run() {
                ReadOnlyCallInfo ci = callCollection.get(ci1.getId());
                setCallInfo(ci);
            }
        };

        new Thread(runnable).start();

        Thread.sleep(500);

        assertNotNull(this.callInfo);

        //post-test cleaning
        this.callCollection.destroy();
    }


	@Test
    public void testEmptyCollection() throws Exception {
		assertNull(callCollection.getCurrentCallForCallLeg("BOB"));
	}

    @Test
	public void testSingleExisting() throws Exception {
		callCollection.add(callInfo1);
		assertEquals(callInfo1.getId(), callCollection.getCurrentCallForCallLeg("TIM").getId());
        assertEquals(callInfo1.getId(), callCollection.getCurrentCallForCallLeg("JANE").getId());
	}

    @Test
	public void testSingleNonExisting() throws Exception {
		callCollection.add(callInfo1);
		assertNull(callCollection.getCurrentCallForCallLeg("BOB"));
	}

    @Test
	public void testMultipleExisting() throws Exception {
		Thread.sleep(50);
		CallInfo callInfo2 = new CallInfo(beanName, "c2", "JANE", "BERT", AutoTerminateAction.False, -1);
		Thread.sleep(50);
		CallInfo callInfo3 = new CallInfo(beanName, "c3", "PAT", "JESS", AutoTerminateAction.False, -1);
		callCollection.add(callInfo1);
		callCollection.add(callInfo2);
		callCollection.add(callInfo3);
		assertEquals(callInfo1.getId(), callCollection.getCurrentCallForCallLeg("TIM").getId());
        assertEquals(callInfo2.getId(), callCollection.getCurrentCallForCallLeg("JANE").getId());
        assertEquals(callInfo2.getId(), callCollection.getCurrentCallForCallLeg("BERT").getId());
        assertEquals(callInfo3.getId(), callCollection.getCurrentCallForCallLeg("PAT").getId());
        assertEquals(callInfo3.getId(), callCollection.getCurrentCallForCallLeg("JESS").getId());
	}

    @Test
	public void testMultipleNonExisting() throws Exception {
		CallInfo callInfo2 = new CallInfo(beanName, "c2", "JANE", "BERT", AutoTerminateAction.False, -1);
		CallInfo callInfo3 = new CallInfo(beanName, "c3", "PAT", "JESS", AutoTerminateAction.False, -1);
		callCollection.add(callInfo1);
		callCollection.add(callInfo2);
		callCollection.add(callInfo3);
		assertNull(callCollection.getCurrentCallForCallLeg("BOB"));
	}

    @Test
	public void testMultipleReturnsMostRecent() throws Exception {
    	Thread.sleep(50);
    	CallInfo callInfo2 = new CallInfo(beanName, "c2", "JANE", "BERT", AutoTerminateAction.False, -1);
		Thread.sleep(50);
		CallInfo callInfo3 = new CallInfo(beanName, "c3", "PAT", "JESS", AutoTerminateAction.False, -1);
		callCollection.add(callInfo1);
		callCollection.add(callInfo2);
		callCollection.add(callInfo3);
		assertEquals(callInfo2.getId(), callCollection.getCurrentCallForCallLeg("JANE").getId());
	}

    @Test
	public void testMultipleIgnoresSpecifiedCall() throws Exception {
    	// setup
		CallInfo callInfo2 = new CallInfo(beanName, "c2", "JANE", "BERT", AutoTerminateAction.False, -1);
		Thread.sleep(50);
		CallInfo callInfo3 = new CallInfo(beanName, "c3", "JANE", "JESS", AutoTerminateAction.False, -1);
		callCollection.add(callInfo2);
		callCollection.add(callInfo3);

		// act
		String callId = callCollection.getCurrentCallForCallLeg("JANE", callInfo3.getId()).getId();

		// assert
		assertEquals(callInfo2.getId(), callId);
	}

    @Test
    public void testGetCallReturnsNewObjectButSameCallId() throws Exception {
        // setup
        callCollection.add(callInfo1);

        String callId = callInfo1.getId();

        // act
        ReadOnlyCallInfo callInfo2 = callCollection.get(callId);

        // assert
        assertNotSame(callInfo1, callInfo2);
        assertEquals(callId, callInfo2.getId());
    }

    @Test
    public void testAddCallCreatesNewInstance() throws Exception {
        // setup
        callInfo1.setCallState(CallState.Connecting);
        callCollection.add(callInfo1);

        // act
        callInfo1.setCallState(CallState.Terminated);

        // assert
        assertEquals(CallState.Connecting, callCollection.get(callInfo1.getId()).getCallState());
    }

    @Test
    public void testReplaceCall() throws Exception {
        // setup
        callInfo1.setCallState(CallState.Connecting);
        callCollection.add(callInfo1);
        callInfo1.setCallState(CallState.Terminated);

        // act
        callCollection.replace(callInfo1);

        // assert
        assertEquals(CallState.Terminated, callCollection.get(callInfo1.getId()).getCallState());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testReplaceCallNullCall() throws Exception {
        // act
        callCollection.replace(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testReplaceCallUnknownCall() throws Exception {
        // act
        callCollection.replace(new CallInfo(beanName, Double.toString(Math.random()), "d1", "d2", AutoTerminateAction.False, 123));
    }

    @Test(expected=ConcurrentUpdateException.class)
    public void testReplaceCallAlreadyModifiedCall() throws Exception {
        // setup
        CallInfo callInfo2 = callInfo1.cloneObject();
        callInfo1.setCallState(CallState.Connecting);
        callCollection.add(callInfo1);
        Thread.sleep(10);
        callCollection.replace(callInfo1);
        Thread.sleep(10);

        // act
        callCollection.replace(callInfo2);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testGetCallNullArgument() throws Exception {
        // act
        callCollection.get(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testAddCallExistingCall() throws Exception {
        // setup
        callCollection.add(callInfo1);

        // act
        callCollection.add(callInfo1);
    }

    @Test
    public void testGetAllConnectedCallsWithMaxDuration() {
        // setup
        callCollection.add(callInfo1);

        CallInfo callInfo2 = new CallInfo(beanName, "c2", "JANE", "BERT", AutoTerminateAction.False, 23);
        callInfo2.setCallState(CallState.Connected);

        callCollection.add(callInfo2);

        CallInfo callInfo3 = new CallInfo(beanName, "c3", "PAT", "JESS", AutoTerminateAction.False, -1);
        callInfo3.setCallState(CallState.Connected);
        callCollection.add(callInfo3);

        CallInfo callInfo4 = new CallInfo(beanName, "c4", "PAT", "JESS", AutoTerminateAction.False, 34);
        callInfo4.setCallState(CallState.Connecting);
        callCollection.add(callInfo4);

        assertEquals(4, callCollection.size());

        // act
        ConcurrentMap<String, CallInfo> result = callCollection.getAllConnectedCallsWithMaxDuration();

        // assert
        assertEquals(1, result.size());
        assertTrue(result.containsKey("c2"));
        assertEquals(4, callCollection.size()); // make sure we haven't affected the collection
    }

    // tests that the number of connecting calls is correctly returned
    @Test
    public void testGetNumberOfConnectingCalls(){
    	callInfo1.setCallState(CallState.Connecting);
    	callInfo2.setCallState(CallState.Terminated);
        callCollection.add(callInfo1);
        callCollection.add(callInfo2);
        assertEquals(1, callCollection.getNumberOfConnectingCalls());
    }
}
