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
package com.bt.aloha.media.conference.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ConcurrentMap;

import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.collections.memory.InMemoryHousekeepingCollectionImpl;
import com.bt.aloha.media.conference.collections.ConferenceCollection;
import com.bt.aloha.media.conference.collections.ConferenceCollectionImpl;
import com.bt.aloha.media.conference.state.ConferenceInfo;
import com.bt.aloha.media.conference.state.ConferenceState;
import com.bt.aloha.util.ConcurrentUpdateException;
import com.bt.aloha.util.Housekeeper;

public class ConferenceCollectionTest {

    private ConferenceCollection conferenceCollection;
    private ConferenceInfo confInfo;
    private String beanName = "conferenceBean";

    @Before
    public void setUp() {
        this.conferenceCollection = new ConferenceCollectionImpl(new InMemoryHousekeepingCollectionImpl<ConferenceInfo>());
        confInfo = new ConferenceInfo(beanName, "a:1");
    }

    // Keep Emma happy until we do something serious with destroy()
    @Test
    public void testDestroy() throws Exception {
        conferenceCollection.destroy();
    }

    @Test
    public void testHouseKeeper() throws Exception {
        //setup
    	ConferenceInfo ci1 = new ConferenceInfo(beanName, "a1:1");
    	ConferenceInfo ci2 = new ConferenceInfo(beanName, "a2:2");

    	ci1.updateConferenceState(ConferenceState.Ended);
    	ci2.updateConferenceState(ConferenceState.Ended);

    	this.conferenceCollection.add(ci1);
        this.conferenceCollection.add(ci2);

        assertEquals(2, this.conferenceCollection.size());

        this.conferenceCollection.setMaxTimeToLive(2000);

        //act
        //wait for housekeeper to remove the old dialogInfos
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }

        ((Housekeeper)this.conferenceCollection).housekeep();

        //assert
        assertEquals(0, this.conferenceCollection.size());

        //post-test cleaning
        this.conferenceCollection.destroy();
    }

    // Test getCurrentConferenceForCall()
	@Test
	public void testGetCurrentConferenceForCall() throws Exception {
		//setup
		ConferenceInfo conferenceInfo = new ConferenceInfo(beanName, "m1:1");
		conferenceInfo.addParticipant("callId1");
		conferenceInfo.addParticipant("callId2");
		conferenceCollection.add(conferenceInfo);

		ConferenceInfo conferenceInfo2 = new ConferenceInfo(beanName, "m2:2");
		conferenceInfo2.addParticipant("callId3");
		conferenceInfo2.addParticipant("callId4");
		conferenceCollection.add(conferenceInfo2);
		//act
		ConferenceInfo currentConferenceInfo = conferenceCollection.getCurrentConferenceForCall("callId4");

		//assert
		assertEquals(conferenceInfo2.getId(), currentConferenceInfo.getId());
	}

	// Test getCurrentConferenceForCall()
	@Test
	public void testGetCurrentConferenceForCallNoConference() throws Exception {
		//setup
		ConferenceInfo conferenceInfo = new ConferenceInfo(beanName, "m1:1");
		conferenceInfo.addParticipant("callId1");
		conferenceInfo.addParticipant("callId2");
		conferenceCollection.add(conferenceInfo);

		//act/assert
		assertNull(conferenceCollection.getCurrentConferenceForCall("callId4"));
	}

    @Test
	public void testSingleExisting() throws Exception {
		conferenceCollection.add(confInfo);
		assertEquals(confInfo.getConferenceSipUri(), conferenceCollection.get(confInfo.getId()).getConferenceSipUri());
	}

    @Test
	public void testSingleNonExisting() throws Exception {
		conferenceCollection.add(confInfo);
		assertNull(conferenceCollection.get("unknown"));
	}

    @Test
	public void testMultipleExisting() throws Exception {
		Thread.sleep(100);
		ConferenceInfo confInfo2 = new ConferenceInfo(beanName, "a2:2");
		Thread.sleep(100);
		ConferenceInfo confInfo3 = new ConferenceInfo(beanName, "a3:3");
		conferenceCollection.add(confInfo);
		conferenceCollection.add(confInfo2);
		conferenceCollection.add(confInfo3);
		assertEquals(confInfo.getConferenceSipUri(), conferenceCollection.get(confInfo.getId()).getConferenceSipUri());
        assertEquals(confInfo2.getConferenceSipUri(), conferenceCollection.get(confInfo2.getId()).getConferenceSipUri());
        assertEquals(confInfo3.getConferenceSipUri(), conferenceCollection.get(confInfo3.getId()).getConferenceSipUri());
	}

    @Test
	public void testMultipleNonExisting() throws Exception {
		ConferenceInfo confInfo2 = new ConferenceInfo(beanName, "a2:2");
		Thread.sleep(100);
		ConferenceInfo confInfo3 = new ConferenceInfo(beanName, "a3:3");
		conferenceCollection.add(confInfo);
		conferenceCollection.add(confInfo2);
		conferenceCollection.add(confInfo3);
		assertNull(conferenceCollection.get("qcon"));
	}

    @Test
    public void testgetConferenceReturnsNewObjectButSameConferenceId() throws Exception {
        // setup
        conferenceCollection.add(confInfo);

        String conferenceId = confInfo.getId();

        // act
        ConferenceInfo confInfo2 = conferenceCollection.get(conferenceId);

        // assert
        assertNotSame(confInfo, confInfo2);
        assertEquals(conferenceId, confInfo2.getId());
    }

    @Test
    public void testaddConferenceCreatesNewInstance() throws Exception {
        // setup
        confInfo.updateConferenceState(ConferenceState.Active);
        conferenceCollection.add(confInfo);

        // act
        confInfo.updateConferenceState(ConferenceState.Ended);

        // assert
        assertEquals(ConferenceState.Active, conferenceCollection.get(confInfo.getId()).getConferenceState());
    }

    @Test
    public void testReplaceInfo() throws Exception {
        // setup
        confInfo.updateConferenceState(ConferenceState.Active);
        conferenceCollection.add(confInfo);
        confInfo.updateConferenceState(ConferenceState.Ended);

        // act
        conferenceCollection.replace(confInfo);

        // assert
        assertEquals(ConferenceState.Ended, conferenceCollection.get(confInfo.getId()).getConferenceState());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testreplaceConferenceNullInfo() throws Exception {
        // act
        conferenceCollection.replace(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testreplaceConferenceUnknownInfo() throws Exception {
        // act
        conferenceCollection.replace(new ConferenceInfo(beanName, "a99:99"));
    }

    @Test(expected=ConcurrentUpdateException.class)
    public void testreplaceConferenceAlreadyModifiedInfo() throws Exception {
        // setup
    	ConferenceInfo conferenceInfo2 = confInfo.cloneObject();
        confInfo.updateConferenceState(ConferenceState.Active);
        conferenceCollection.add(confInfo);
        Thread.sleep(10);
        conferenceCollection.replace(confInfo);
        Thread.sleep(10);

        // act
        conferenceCollection.replace(conferenceInfo2);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testgetConferenceNullArgument() throws Exception {
        // act
        conferenceCollection.get(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testaddConferenceExistingCall() throws Exception {
        // setup
        conferenceCollection.add(confInfo);

        // act
        conferenceCollection.add(confInfo);
    }

    //Test removing an object from collection
	@Test
	public void testRemoveConference() throws Exception {
		//setup
		conferenceCollection.add(confInfo);
		int sizeBefore = conferenceCollection.size();

		//act
		conferenceCollection.remove(confInfo.getId());

		//assert
		assertEquals(sizeBefore - 1, conferenceCollection.size());
	}

	//Test getAll method returns all objects
	@Test
	public void testGetAll() throws Exception {
		//setup
    	ConferenceInfo confInfo2 = new ConferenceInfo(beanName, "a2:2");
		conferenceCollection.add(confInfo);
		conferenceCollection.add(confInfo2);

		//act
		ConcurrentMap<String, ConferenceInfo> all = conferenceCollection.getAll();

		//assert
		assertEquals(2, all.size());
		assertTrue(all.keySet().contains(confInfo.getId()));
		assertTrue(all.keySet().contains(confInfo2.getId()));
	}
}
