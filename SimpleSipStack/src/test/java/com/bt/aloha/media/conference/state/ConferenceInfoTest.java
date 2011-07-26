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

 	

 	
 	
 
package com.bt.aloha.media.conference.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;

import org.junit.Test;

import com.bt.aloha.media.conference.state.ConferenceInfo;
import com.bt.aloha.media.conference.state.ConferenceInformation;
import com.bt.aloha.media.conference.state.ConferenceState;
import com.bt.aloha.media.conference.state.ParticipantState;


public class ConferenceInfoTest {

	private String mediaServerAddress = "127.0.0.1:5060";;
	private String beanName = "conferenceBean";

	//Test simple construction of the ConferenceInfo
	@Test
	public void testConstructor() throws Exception {
		//setup
		//act
		ConferenceInfo info = new ConferenceInfo(beanName, mediaServerAddress);

		//assert
		assertEquals(32, info.getId().length());
		assertEquals(String.format("sip:conf=%s@%s", info.getId(), mediaServerAddress), info.getConferenceSipUri());
		assertNotNull(info.getVersionId());
		assertNotNull(info.getCreateTime());
		assertNotSame(ConferenceInfo.TIME_NOT_SET, info.getStartTime());
		assertEquals(ConferenceInfo.TIME_NOT_SET, info.getEndTime());
		assertEquals(ConferenceState.Initial, info.getConferenceState());
		assertEquals(0, info.getNumberOfActiveParticipants());
		assertFalse(info.containsParticipant("callId"));
		assertNull(info.getParticipantState("abc"));
		assertEquals(ConferenceInfo.DEFAULT_MAX_NUMBER_OF_PARTICIPANTS, info.getMaxNumberOfParticipants());
		assertEquals(ConferenceInfo.DEFAULT_MAX_DURATION_IN_MINUTES, info.getMaxDurationInMinutes());
	}

	//Test full construction of the ConferenceInfo
	@Test
	public void testConstructorFull() throws Exception {
		//setup
		//act
		ConferenceInfo info = new ConferenceInfo(beanName, mediaServerAddress, 5, 6);

		//assert
		assertEquals(32, info.getId().length());
		assertEquals(String.format("sip:conf=%s@%s", info.getId(), mediaServerAddress), info.getConferenceSipUri());
		assertNotNull(info.getVersionId());
		assertNotNull(info.getCreateTime());
		assertNotSame(ConferenceInfo.TIME_NOT_SET, info.getStartTime());
		assertEquals(ConferenceInfo.TIME_NOT_SET, info.getEndTime());
		assertEquals(ConferenceState.Initial, info.getConferenceState());
		assertEquals(0, info.getNumberOfActiveParticipants());
		assertFalse(info.containsParticipant("callId"));
		assertNull(info.getParticipantState("abc"));
		assertEquals(5, info.getMaxNumberOfParticipants());
		assertEquals(6, info.getMaxDurationInMinutes());
	}

	//Test full construction of the ConferenceInfo with an invalid max number of participants (1)
	@Test(expected=IllegalArgumentException.class)
	public void testConstructorMaxNumberOfParticipantsIsOne() throws Exception {
		//setup
		//act
		new ConferenceInfo(beanName, mediaServerAddress, 1, 6);
	}

	//Test full construction of the ConferenceInfo with an invalid max number of participants (negative)
	@Test(expected=IllegalArgumentException.class)
	public void testConstructorMaxNumberOfParticipantsIsNegative() throws Exception {
		//setup
		//act
		new ConferenceInfo(beanName, mediaServerAddress, -1, 6);
	}
	
	//Test full construction of the ConferenceInfo with max number of participants as 0 means unlimited number of participants
	@Test
	public void testMaxNumberOfParticipantsIsZero() throws Exception {
		//setup
		ConferenceInfo conferenceInfo = new ConferenceInfo(beanName, mediaServerAddress);
		assertFalse(conferenceInfo.isMaxNumberOfParticipants());

		// Add a large number of participants and ensure its allowed
		for (int i=0; i<1000; i++) {
			//act
			conferenceInfo.addParticipant("participant "+i);
			//assert
			assertFalse(conferenceInfo.isMaxNumberOfParticipants());
		}
	}
	
	//Test full construction of the ConferenceInfo with too small max number of participants
	@Test(expected=IllegalArgumentException.class)
	public void testConstructorTooSmallMaxDuration() throws Exception {
		//setup
		//act
		new ConferenceInfo(beanName, mediaServerAddress, 5, -1);
	}

	// Test clone method
	@Test
	public void testClone() throws Exception {
		//setup
		ConferenceInfo info = new ConferenceInfo(beanName, mediaServerAddress);
		info.addParticipant("callId1");
        info.addParticipant("callId2");

		//act
		ConferenceInfo clonedInfo = info.cloneObject();

		//assert
		assertNotSame(info, clonedInfo);
		assertNotSame(info.getParticipants(), clonedInfo.getParticipants());
		assertTrue(clonedInfo.getParticipants().containsKey("callId1"));
		assertTrue(clonedInfo.getParticipants().containsKey("callId2"));

		clonedInfo.addParticipant("callId3");
		assertFalse(info.getParticipants().containsKey("callId3"));
	}

	//Test of updating participant state
	@Test
	public void tesUpdateParticipantState() throws Exception {
		//setup
		ConferenceInfo info = new ConferenceInfo(beanName, mediaServerAddress);
		info.addParticipant("callId1");

		//act/assert
		assertFalse(info.updateParticipantState("callId1", ParticipantState.Connecting));
		assertTrue(info.updateParticipantState("callId1", ParticipantState.Connected));
		assertFalse(info.updateParticipantState("callId1", ParticipantState.Connecting));
		assertTrue(info.updateParticipantState("callId1", ParticipantState.Failed));
	}

	//Test of updating conference state
	@Test
	public void tesUpdateConferenceState() throws Exception {
		//setup
		ConferenceInfo info = new ConferenceInfo(beanName, mediaServerAddress);

		//act/assert
		assertFalse(info.updateConferenceState(ConferenceState.Initial));
		assertEquals(ConferenceInfo.TIME_NOT_SET, info.getStartTime());
		assertEquals(ConferenceInfo.TIME_NOT_SET, info.getEndTime());
		assertTrue(info.updateConferenceState(ConferenceState.Active));
		assertNotSame(ConferenceInfo.TIME_NOT_SET, info.getStartTime());
		assertEquals(ConferenceInfo.TIME_NOT_SET, info.getEndTime());
		assertFalse(info.updateConferenceState(ConferenceState.Active));
		assertFalse(info.updateConferenceState(ConferenceState.Initial));
		assertTrue(info.updateConferenceState(ConferenceState.Ending));
		assertNotSame(ConferenceInfo.TIME_NOT_SET, info.getStartTime());
		assertEquals(ConferenceInfo.TIME_NOT_SET, info.getEndTime());
		assertTrue(info.updateConferenceState(ConferenceState.Ended));
		assertNotSame(ConferenceInfo.TIME_NOT_SET, info.getStartTime());
		assertNotSame(ConferenceInfo.TIME_NOT_SET, info.getEndTime());
	}

	//Test of participant state getter
	@Test
	public void testGetParticipantState() throws Exception {
		//setup
		ConferenceInfo info = new ConferenceInfo(beanName, mediaServerAddress);
		info.addParticipant("callId1");

		//act
		assertEquals(ParticipantState.Connecting, info.getParticipantState("callId1"));
		assertNull(info.getParticipantState("abc"));
		//assert
	}

    // test returns 0 if conference hasn't started yet
	@Test
    public void testGetDurationNullStartTime() {
    	ConferenceInfo info = new ConferenceInfo(beanName, mediaServerAddress);
        info.setStartTime(ConferenceInfo.TIME_NOT_SET);
        assertEquals(0, info.getDuration());
    }

    // test for when conference hasn't ended yet
	@Test
    public void testGetDurationNullEndTime() throws Exception{
		ConferenceInfo info = new ConferenceInfo(beanName, mediaServerAddress);
        long now = Calendar.getInstance().getTimeInMillis();
        info.setStartTime(now);
        info.setEndTime(ConferenceInfo.TIME_NOT_SET);
        Thread.sleep(1100);
        int result = info.getDuration();
        assertTrue(result > 0);
    }

    // test for when both times are set
	@Test
    public void testGetDurationBothTimesSet() throws Exception{
    	ConferenceInfo info = new ConferenceInfo(beanName, mediaServerAddress);
        long then = Calendar.getInstance().getTimeInMillis();
        Thread.sleep(1100);
        long now = Calendar.getInstance().getTimeInMillis();
        info.setStartTime(then);
        info.setEndTime(now);
        assertEquals(1, info.getDuration());
    }

	//Test that getConferenceInformation returns right data
	@Test
	public void testGetConferenceInformation() throws Exception {
		//setup
		long t1 = Calendar.getInstance().getTimeInMillis() - 10;
		long t2 = Calendar.getInstance().getTimeInMillis();

		ConferenceInfo info = new ConferenceInfo(beanName, mediaServerAddress);
		info.setStartTime(t1);
		info.setEndTime(t2);

		//act
		ConferenceInformation information = info.getConferenceInformation();

		//assert
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(info.getStartTime());
		assertEquals(c, information.getStartTime());
		assertEquals(info.getDuration(), information.getDuration());
		assertEquals(info.getConferenceState(), information.getConferenceState());
		assertEquals(info.getConferenceTerminationCause(), information.getConferenceTerminationCause());
	}

	//
	@Test
	public void testSettingCretingBeanName() throws Exception {
		//setup
		//act
		ConferenceInfo info = new ConferenceInfo(beanName, mediaServerAddress);
		//assert
		assertEquals("conferenceBean", info.getSimpleSipBeanId());
	}
}
