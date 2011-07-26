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

 	

 	
 	
 
package com.bt.aloha.media.convedia.conference;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bt.aloha.collections.memory.InMemoryHousekeepingCollectionImpl;
import com.bt.aloha.media.conference.collections.ConferenceCollection;
import com.bt.aloha.media.conference.collections.ConferenceCollectionImpl;
import com.bt.aloha.media.conference.state.ConferenceInfo;
import com.bt.aloha.media.conference.state.ConferenceState;
import com.bt.aloha.media.conference.state.ConferenceTerminationCause;

public class MaxConferenceDurationTerminationTest {

	private final static int FIVE_MINUTES_IN_MILLISECONDS = 300000;
	private final static int TWO_MINUTES = 2;
	private ConferenceInfo conferenceInfoInPast;
	private ConferenceInfo conferenceInfoInFuture;
	private ConferenceInfo conferenceInfoNoMaxDuration;
	private ConferenceCollection conferenceCollection;
	private MaxConferenceDurationTermination maxConferenceDurationTermination;
	private Log log = LogFactory.getLog(this.getClass());
    private boolean endConferenceCalled = false;
    private boolean terminateCalled = false;

	@Before
	public void setup(){
		maxConferenceDurationTermination = new MaxConferenceDurationTermination();
		conferenceCollection = new ConferenceCollectionImpl(new InMemoryHousekeepingCollectionImpl<ConferenceInfo>());
		conferenceInfoInPast = new ConferenceInfo("conferenceBean", "a:1", 5, TWO_MINUTES);
		long startTime = System.currentTimeMillis() - FIVE_MINUTES_IN_MILLISECONDS;
		log.debug(String.format("TEST: start time: %s", startTime));
		if (!conferenceInfoInPast.setStartTime(startTime))
			throw new RuntimeException("couldn't set time in the past for some reason");
		conferenceInfoInPast.updateConferenceState(ConferenceState.Active);

		conferenceInfoInFuture = new ConferenceInfo("conferenceBean", "a:1", 5, TWO_MINUTES);
		conferenceInfoInFuture.updateConferenceState(ConferenceState.Active);
		
		conferenceInfoNoMaxDuration = new ConferenceInfo("conferenceBean", "a:1", 5, 0);
		conferenceInfoNoMaxDuration.updateConferenceState(ConferenceState.Active);
		
		endConferenceCalled = false;
	    terminateCalled = false;
	}

	// create a conference that should have already been terminated and initalise the termination class
	// this should go through the collection and terminate any conferences that should have expired.
	@Test
	public void testInitializeWithTermination(){
		//setup
		conferenceCollection.add(conferenceInfoInPast);
		maxConferenceDurationTermination.setConferenceCollection(conferenceCollection);
		ConferenceBean conferenceBean = EasyMock.createMock(ConferenceBean.class);
		conferenceBean.endConference(conferenceInfoInPast.getId(), ConferenceTerminationCause.MaximumDurationExceeded);
		EasyMock.replay(conferenceBean);
		maxConferenceDurationTermination.setConferenceBean(conferenceBean);
		ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
		te.initialize();
		maxConferenceDurationTermination.setTaskExecutor(te);
		
		//act
		maxConferenceDurationTermination.runTask();
		try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); } // allow time for thread to run
		
		//assert
		EasyMock.verify(conferenceBean);
	}

	// create a conference that should be terminated in the future, assert that the call isn't removed
	// from the conference collection
	@Test
	public void testInitialiseWithoutTermination(){
		// setup
        terminateCalled = false;
		conferenceCollection.add(conferenceInfoInFuture);
		maxConferenceDurationTermination.setConferenceCollection(conferenceCollection);
		ConferenceBean conferenceBean = EasyMock.createMock(ConferenceBean.class);
		EasyMock.replay(conferenceBean);
		maxConferenceDurationTermination.setConferenceBean(conferenceBean);
		final String callId = conferenceInfoInFuture.getId();

		MaxConferenceDurationScheduler maxConferenceDurationScheduler = new MaxConferenceDurationScheduler(){
			public void cancelTerminateConference(ConferenceInfo conferenceInfo) {
			    // do nothing
			}

			public void terminateConferenceAfterMaxDuration(ConferenceInfo conferenceInfo, ConferenceBean conferenceBean) {
                terminateCalled = true;
			    if (conferenceInfo.getId() != callId)
			        throw new RuntimeException("unexpected call id");
			}
		};
		maxConferenceDurationTermination.setMaxConferenceDurationScheduler(maxConferenceDurationScheduler);
		
		//act
		maxConferenceDurationTermination.initialize();
		
		//assert
        EasyMock.verify(conferenceBean);
		assertTrue(terminateCalled);
	}

	// when a call has no max duration set, treat as infinite and do not terminate
	@Test
	public void testInitialiseWithNoLimit(){
		// setup
		conferenceCollection.add(conferenceInfoNoMaxDuration);
		maxConferenceDurationTermination.setConferenceCollection(conferenceCollection);
		ConferenceBean conferenceBean = new ConferenceBeanImpl(){
			@Override
			public void endConference(String conferenceId, ConferenceTerminationCause terminationCause) {
				endConferenceCalled = true;
			}
		};
		maxConferenceDurationTermination.setConferenceBean(conferenceBean);
		MaxConferenceDurationScheduler maxConferenceDurationScheduler = new MaxConferenceDurationScheduler(){
			public void cancelTerminateConference(ConferenceInfo conferenceInfo) {
			    // do nothing
			}

			public void terminateConferenceAfterMaxDuration(ConferenceInfo conferenceInfo, ConferenceBean conferenceBean) {
                terminateCalled = true;
			}
		};
		maxConferenceDurationTermination.setMaxConferenceDurationScheduler(maxConferenceDurationScheduler);

		// act
		maxConferenceDurationTermination.initialize();
		
		// assert
		assertFalse(terminateCalled);
		assertFalse(endConferenceCalled);
	}
	
    // ensure that we deal with any exceptions thrown by the conference bean
    @Test
    public void testInitializeConferenceBeanException(){
        //setup
        endConferenceCalled = false;
        conferenceCollection.add(conferenceInfoInPast);
        maxConferenceDurationTermination.setConferenceCollection(conferenceCollection);
        ConferenceBean conferenceBean = new ConferenceBeanImpl() {

            @Override
            public void endConference(String conferenceId, ConferenceTerminationCause terminationCause) {
                endConferenceCalled = true;
                throw new IllegalArgumentException("no call found");
            }

        };
        maxConferenceDurationTermination.setConferenceBean(conferenceBean);
        
        //act
        maxConferenceDurationTermination.initialize();
        
        //assert
        assertTrue(endConferenceCalled);
    }

    // ensure that we deal with any exceptions thrown by the termination scheduler
    @Test
    public void testInitialiseSchedulerException(){
        // setup
        terminateCalled = false;
        conferenceCollection.add(conferenceInfoInFuture);
        maxConferenceDurationTermination.setConferenceCollection(conferenceCollection);
        ConferenceBean conferenceBean = EasyMock.createMock(ConferenceBean.class);
        EasyMock.replay(conferenceBean);
        maxConferenceDurationTermination.setConferenceBean(conferenceBean);

        MaxConferenceDurationScheduler maxConferenceDurationScheduler = new MaxConferenceDurationScheduler(){
            public void cancelTerminateConference(ConferenceInfo conferenceInfo) {
                // do nothing
            }

            public void terminateConferenceAfterMaxDuration(ConferenceInfo conferenceInfo, ConferenceBean conferenceBean) {
                terminateCalled = true;
                throw new RuntimeException("unexpected call id");
            }
        };
        maxConferenceDurationTermination.setMaxConferenceDurationScheduler(maxConferenceDurationScheduler);

        //act
        maxConferenceDurationTermination.initialize();
        
        //assert
        EasyMock.verify(conferenceBean);
        assertTrue(terminateCalled);
    }
}
