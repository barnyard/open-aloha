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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.bt.aloha.media.conference.state.ConferenceInformation;
import com.bt.aloha.media.conference.state.ConferenceState;
import com.bt.aloha.media.conference.state.ConferenceTerminationCause;
import com.bt.aloha.media.conference.state.ParticipantState;

public class ConferenceInformationTest {
	//Test setting and retrieving information
	@Test
	public void testContructorAndGetters() throws Exception {
		//setup
		String id = "id";
		long startTime = Calendar.getInstance().getTimeInMillis();
		long createTime = Calendar.getInstance().getTimeInMillis() + 50;
		long endTime= Calendar.getInstance().getTimeInMillis() + 100;
		int duration = 123;
		int activeParticipants = 2;
		int participants = 3;
		Map<String, ParticipantState> participantStates = new HashMap<String, ParticipantState>();
		participantStates.put("abc", ParticipantState.Disconnected);

		//act
		ConferenceInformation info = new ConferenceInformation(id, ConferenceState.Ended, createTime, startTime, endTime, duration, ConferenceTerminationCause.EndedByApplication, activeParticipants, participants, participantStates);

		//assert
		assertEquals(id, info.getId());
		assertEquals(createTime, info.getCreateTime().getTimeInMillis());
		assertEquals(startTime, info.getStartTime().getTimeInMillis());
		assertEquals(endTime, info.getEndTime().getTimeInMillis());
		assertEquals(duration, info.getDuration());
		assertEquals(activeParticipants, info.getNumberOfActiveParticipants());
		assertEquals(participants, info.getNumberOfParticipants());
		assertEquals(ParticipantState.Disconnected, info.getParticipantState("abc"));
	}
}
