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

import static org.junit.Assert.assertTrue;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.call.state.CallInfo;
import com.bt.aloha.callleg.AutoTerminateAction;

public class ScheduledExecutorServiceMaxCallDurationSchedulerTest {
    private CallInfo callInfo;
    private long delay;
    private ScheduledFuture<?> future;

    class MyScheduledExecutorService extends ScheduledThreadPoolExecutor {

        public MyScheduledExecutorService(int arg0) {
            super(arg0);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable aCommand, long aDelay, TimeUnit unit) {
            delay = aDelay;
            return future;
        }
    }

    @Before
    public void before() {
        future = EasyMock.createMock(ScheduledFuture.class);
        callInfo = new CallInfo("callBean", Double.toString(Math.random()), "id1", "id2", AutoTerminateAction.False, 5);
        delay = -1;
    }

    /**
     * Tests that call info is scheduled correctly
     */
    @Test
    public void scheduleCallForTermination() {
        // setup
        ScheduledExecutorServiceMaxCallDurationScheduler scheduler = new ScheduledExecutorServiceMaxCallDurationScheduler();
        callInfo.setStartTime(System.currentTimeMillis());
        scheduler.setScheduledExecutorService(new MyScheduledExecutorService(1));

        // act
        scheduler.terminateCallAfterMaxDuration(callInfo, null);

        // assert
        System.out.println(delay);
        assertDelay(300000, 100);
    }

    // test that a termination is scheduled correctly after a restart
    @Test
    public void scheduleCallForTerminationAfterRestart() {
        // setup
        callInfo.setStartTime(System.currentTimeMillis() - (60 * 1000));
        ScheduledExecutorServiceMaxCallDurationScheduler scheduler = new ScheduledExecutorServiceMaxCallDurationScheduler();
        scheduler.setScheduledExecutorService(new MyScheduledExecutorService(1));

        // act
        scheduler.terminateCallAfterMaxDuration(callInfo, null);

        // assert
        System.out.println(delay);
        assertDelay(240000, 100);
    }

    /**
     * Tests that cancel terminate task is cancelled correctly
     */
    @Test
    public void cancelScheduledCallTermination() {
        // setup
        callInfo.setStartTime(System.currentTimeMillis());
        ScheduledExecutorServiceMaxCallDurationScheduler scheduler = new ScheduledExecutorServiceMaxCallDurationScheduler();
        scheduler.setScheduledExecutorService(new MyScheduledExecutorService(1));

        EasyMock.expect(future.cancel(false)).andReturn(true);
        EasyMock.replay(future);
        scheduler.terminateCallAfterMaxDuration(callInfo, null);

        // act
        scheduler.cancelTerminateCall(callInfo);

        // assert
        EasyMock.verify(future);
        System.out.println(delay);
        assertDelay(300000, 100);
    }

    /**
     * Tests that cancel terminate task doesnt throw exceptions even if it
     * wasn't scheduled
     */
    @Test
    public void cancelScheduledCallTerminationShouldntBarfWhenItWasntScheduled() {
        // setup
        ScheduledExecutorServiceMaxCallDurationScheduler scheduler = new ScheduledExecutorServiceMaxCallDurationScheduler();
        ScheduledFuture<?> future = EasyMock.createMock(ScheduledFuture.class);
        EasyMock.replay(future);
        ScheduledExecutorService executorService = EasyMock.createMock(ScheduledExecutorService.class);
        EasyMock.replay(executorService);
        scheduler.setScheduledExecutorService(executorService);

        // act
        scheduler.cancelTerminateCall(callInfo);

        // assert
        EasyMock.verify(future);
        EasyMock.verify(executorService);
    }

    // Tests that scheduling a call termination task cancels any previous timers
    // before scheduling a new one
    @Test
    public void scheduleCallTerminationShouldCancelEarlierSchedulerIfExistsBeforeRescheduling() {
        // setup
        ScheduledExecutorServiceMaxCallDurationScheduler scheduler = new ScheduledExecutorServiceMaxCallDurationScheduler();
        scheduler.setScheduledExecutorService(new MyScheduledExecutorService(1));
        EasyMock.expect(future.cancel(false)).andReturn(false);
        EasyMock.replay(future);
        scheduler.terminateCallAfterMaxDuration(callInfo, null);

        // act
        scheduler.terminateCallAfterMaxDuration(callInfo, null);

        EasyMock.verify(future);
    }

    private void assertDelay(int expected, int wobbleFactor) {
        assertTrue((expected - wobbleFactor) < delay);
        assertTrue((expected + wobbleFactor) > delay);
    }
}
