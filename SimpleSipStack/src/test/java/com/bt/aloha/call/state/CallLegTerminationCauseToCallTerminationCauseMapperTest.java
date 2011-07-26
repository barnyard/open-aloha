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

 	

 	
 	
 
package com.bt.aloha.call.state;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.bt.aloha.call.state.CallLegTerminationCauseToCallTerminationCauseMapper;
import com.bt.aloha.call.state.CallTerminationCause;
import com.bt.aloha.dialog.state.TerminationCause;

public class CallLegTerminationCauseToCallTerminationCauseMapperTest {
	private CallLegTerminationCauseToCallTerminationCauseMapper callLegTerminationCauseToCallTerminationCauseMapper = new CallLegTerminationCauseToCallTerminationCauseMapper();

	@Test
	public void testCallLegHangup() throws Exception {
		// act
		CallTerminationCause cause = callLegTerminationCauseToCallTerminationCauseMapper.map(TerminationCause.RemotePartyHungUp);
		
		// assert
		assertEquals(CallTerminationCause.RemotePartyHungUp, cause);
	}
	
	@Test
	public void testCallLegAnswerTimeout() throws Exception {
		// act
		CallTerminationCause cause = callLegTerminationCauseToCallTerminationCauseMapper.map(TerminationCause.CallAnswerTimeout);
		
		// assert
		assertEquals(CallTerminationCause.CallAnswerTimeout, cause);
	}
	
	@Test
	public void testCallLegUnavailable() throws Exception {
		// act
		CallTerminationCause cause = callLegTerminationCauseToCallTerminationCauseMapper.map(TerminationCause.RemotePartyUnavailable);
		
		// assert
		assertEquals(CallTerminationCause.RemotePartyUnavailable, cause);
	}
	
	@Test
	public void testServiceUnavailable() throws Exception {
		// act
		CallTerminationCause cause = callLegTerminationCauseToCallTerminationCauseMapper.map(TerminationCause.ServiceUnavailable);
		
		// assert
		assertEquals(CallTerminationCause.ServiceUnavailable, cause);
	}
	
	@Test
	public void testCallLegBusy() throws Exception {
		// act
		CallTerminationCause cause = callLegTerminationCauseToCallTerminationCauseMapper.map(TerminationCause.RemotePartyBusy);
		
		// assert
		assertEquals(CallTerminationCause.RemotePartyBusy, cause);
	}
	
	@Test
	public void testCallLegAutoTerminated() throws Exception {
		// act
		CallTerminationCause cause = callLegTerminationCauseToCallTerminationCauseMapper.map(TerminationCause.AutoTerminated);
		
		// assert
		assertEquals(CallTerminationCause.CallLegDetached, cause);
	}
	
	@Test
	public void testCallLegUnknown() throws Exception {
		// act
		CallTerminationCause cause = callLegTerminationCauseToCallTerminationCauseMapper.map(TerminationCause.RemotePartyUnknown);
		
		// assert
		assertEquals(CallTerminationCause.RemotePartyUnknown, cause);
	}
	
	@Test
	public void testCallLegSipSessionError() throws Exception {
		// act
		CallTerminationCause cause = callLegTerminationCauseToCallTerminationCauseMapper.map(TerminationCause.SipSessionError);
		
		// assert
		assertEquals(CallTerminationCause.SipSessionError, cause);
	}
	
	@Test
	public void testCallLegTerminatedByApp() throws Exception {
		// act
		CallTerminationCause cause = callLegTerminationCauseToCallTerminationCauseMapper.map(TerminationCause.TerminatedByServer);
		
		// assert
		assertEquals(CallTerminationCause.TerminatedByApplication, cause);
	}
	
	@Test
	public void testCallLegForbidden() throws Exception {
		// act
		CallTerminationCause cause = callLegTerminationCauseToCallTerminationCauseMapper.map(TerminationCause.Forbidden);
		
		// assert
		assertEquals(CallTerminationCause.RemotePartyForbidden, cause);
	}
}
