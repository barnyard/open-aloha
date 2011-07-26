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
package com.bt.aloha.media.convedia.conference;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.media.conference.state.ConferenceInfo;



public class ScheduledExecutorServiceMaxConferenceDurationSchedulerTest {
	private static final String UNCHECKED = "unchecked";
	
	private ConferenceInfo conferenceInfo;

	@Before
	public void before() {
		conferenceInfo = new ConferenceInfo("conferenceBean", "a:1", 5, 5);
	}

	/**
	 * Tests that conference info is scheduled correctly
	 */
	@SuppressWarnings(UNCHECKED)
	@Test
	public void scheduleConferenceForTermination() {
		// setup
		ScheduledExecutorServiceMaxConferenceDurationScheduler scheduler = new ScheduledExecutorServiceMaxConferenceDurationScheduler();
		ScheduledFuture future = EasyMock.createMock(ScheduledFuture.class);
		EasyMock.replay(future);
		ScheduledExecutorService executorService = EasyMock.createMock(ScheduledExecutorService.class);
		EasyMock.expect(executorService.schedule(EasyMock.isA(TerminateConferenceTask.class), EasyMock.eq((long)300), EasyMock.eq(TimeUnit.SECONDS))).andReturn(future);
		EasyMock.replay(executorService);
		scheduler.setScheduledExecutorService(executorService);

		// act
		scheduler.terminateConferenceAfterMaxDuration(conferenceInfo, null);

		// assert
		EasyMock.verify(future);
		EasyMock.verify(executorService);
	}

	/**
	 * Tests that cancel terminate task is cancelled correctly
	 */
	@SuppressWarnings(UNCHECKED)
	@Test
	public void cancelScheduledConferenceTermination() {
		// setup
		ScheduledExecutorServiceMaxConferenceDurationScheduler scheduler = new ScheduledExecutorServiceMaxConferenceDurationScheduler();
		ScheduledFuture future = EasyMock.createMock(ScheduledFuture.class);
		EasyMock.expect(future.cancel(false)).andReturn(true);
		EasyMock.replay(future);
		ScheduledExecutorService executorService = EasyMock.createMock(ScheduledExecutorService.class);
		EasyMock.expect(executorService.schedule(EasyMock.isA(TerminateConferenceTask.class), EasyMock.eq((long)300), EasyMock.eq(TimeUnit.SECONDS))).andReturn(future);
		EasyMock.replay(executorService);
		scheduler.setScheduledExecutorService(executorService);
		scheduler.terminateConferenceAfterMaxDuration(conferenceInfo, null);

		// act
		scheduler.cancelTerminateConference(conferenceInfo);

		// assert
		EasyMock.verify(future);
		EasyMock.verify(executorService);

	}

	/**
	 * Tests that cancel terminate task doesnt throw exceptions even if it wasn't scheduled
	 */
	@Test
	public void cancelScheduledConferenceTerminationShouldntBarfWhenItWasntScheduled() {
		// setup
		ScheduledExecutorServiceMaxConferenceDurationScheduler scheduler = new ScheduledExecutorServiceMaxConferenceDurationScheduler();
		ScheduledFuture<?> future = EasyMock.createMock(ScheduledFuture.class);
		EasyMock.replay(future);
		ScheduledExecutorService executorService = EasyMock.createMock(ScheduledExecutorService.class);
		EasyMock.replay(executorService);
		scheduler.setScheduledExecutorService(executorService);

		// act
		scheduler.cancelTerminateConference(conferenceInfo);

		// assert
		EasyMock.verify(future);
		EasyMock.verify(executorService);
	}

//	Tests that scheduling a conference termination task cancels any previous timers before scheduling a new one
	@SuppressWarnings(UNCHECKED)
	@Test
	public void scheduleConferenceTerminationShouldCancelEarlierSchedulerIfExistsBeforeRescheduling() {
		// setup
		ScheduledExecutorServiceMaxConferenceDurationScheduler scheduler = new ScheduledExecutorServiceMaxConferenceDurationScheduler();
		ScheduledFuture future = EasyMock.createMock(ScheduledFuture.class);
		EasyMock.expect(future.cancel(false)).andReturn(false);
		EasyMock.replay(future);
		ScheduledExecutorService executorService = EasyMock.createMock(ScheduledExecutorService.class);
		EasyMock.expect(executorService.schedule(EasyMock.isA(TerminateConferenceTask.class), EasyMock.eq((long)300), EasyMock.eq(TimeUnit.SECONDS))).andReturn(future).times(2);
		EasyMock.replay(executorService);
		scheduler.setScheduledExecutorService(executorService);
		scheduler.terminateConferenceAfterMaxDuration(conferenceInfo, null);

		// act
		scheduler.terminateConferenceAfterMaxDuration(conferenceInfo, null);

		// assert
		EasyMock.verify(future);
		EasyMock.verify(executorService);
	}
}
