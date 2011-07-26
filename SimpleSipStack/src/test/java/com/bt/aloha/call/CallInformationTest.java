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

 	

 	
 	
 
package com.bt.aloha.call;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;

import org.junit.Test;

import com.bt.aloha.call.CallInformation;
import com.bt.aloha.call.state.CallLegCausingTermination;
import com.bt.aloha.call.state.CallState;
import com.bt.aloha.call.state.CallTerminationCause;

public class CallInformationTest {

	//Test that the start time conversion between millis and Calendar is correct
	@Test
	public void testStartTime() throws Exception {
		//setup
		long t1 = Calendar.getInstance().getTimeInMillis();
		CallInformation info = new CallInformation(CallState.Connected, t1, 0, CallTerminationCause.RemotePartyBusy, CallLegCausingTermination.First, "ID1", "ID2");
		//act
		Calendar c = info.getStartTime();
		//assert
		assertEquals(t1, c.getTimeInMillis());
		assertEquals(CallTerminationCause.RemotePartyBusy, info.getCallTerminationCause());
		assertEquals(CallLegCausingTermination.First, info.getCallLegCausingTermination());
		assertEquals("ID1", info.getFirstCallLegId());
		assertEquals("ID2", info.getSecondCallLegId());
	}
}
