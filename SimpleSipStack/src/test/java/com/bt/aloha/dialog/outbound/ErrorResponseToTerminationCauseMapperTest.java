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

 	

 	
 	
 
package com.bt.aloha.dialog.outbound;

import static org.junit.Assert.assertEquals;

import javax.sip.message.Response;

import org.junit.Test;

import com.bt.aloha.dialog.outbound.ErrorResponseToTerminationCauseMapper;
import com.bt.aloha.dialog.state.TerminationCause;


public class ErrorResponseToTerminationCauseMapperTest {
	private ErrorResponseToTerminationCauseMapper errorResponseToTerminationCauseMapper = new ErrorResponseToTerminationCauseMapper();
	
	@Test
	public void testServiceUnavailableErrorResponseMapping() throws Exception {
		// act
		TerminationCause cause = errorResponseToTerminationCauseMapper.map(Response.SERVICE_UNAVAILABLE);
		
		// assert
		assertEquals(TerminationCause.ServiceUnavailable, cause);
	}
	
	@Test
	public void testTempUnavailableResponseMapping() throws Exception {
		// act
		TerminationCause cause = errorResponseToTerminationCauseMapper.map(Response.TEMPORARILY_UNAVAILABLE);
		
		// assert
		assertEquals(TerminationCause.RemotePartyUnavailable, cause);
	}
	
	@Test
	public void testBusyResponseMapping() throws Exception {
		// act
		TerminationCause cause = errorResponseToTerminationCauseMapper.map(Response.BUSY_HERE);
		
		// assert
		assertEquals(TerminationCause.RemotePartyBusy, cause);
	}
	
	@Test
	public void testNotFoundErrorResponseMapping() throws Exception {
		// act
		TerminationCause cause = errorResponseToTerminationCauseMapper.map(Response.NOT_FOUND);
		
		// assert
		assertEquals(TerminationCause.RemotePartyUnknown, cause);
	}

	@Test
	public void testCallOrTransactionDoesNotExistErrorResponseMapping() throws Exception {
		// act
		TerminationCause cause = errorResponseToTerminationCauseMapper.map(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
		
		// assert
		assertEquals(TerminationCause.SipSessionError, cause);
	}

	@Test
	public void testBusyErrorResponseMapping() throws Exception {
		// act
		TerminationCause cause = errorResponseToTerminationCauseMapper.map(Response.BUSY_HERE);
		
		// assert
		assertEquals(TerminationCause.RemotePartyBusy, cause);
	}
	
	@Test
	public void testGeneralErrorResponseMapping() throws Exception {
		// act
		TerminationCause cause = errorResponseToTerminationCauseMapper.map(Response.METHOD_NOT_ALLOWED);
		
		// assert
		assertEquals(TerminationCause.SipSessionError, cause);
	}
	
	@Test
	public void testForbiddenResponseMapping() throws Exception {
		// act
		TerminationCause cause = errorResponseToTerminationCauseMapper.map(Response.FORBIDDEN);
		
		// assert
		assertEquals(TerminationCause.Forbidden, cause);
	}
	
	@Test
	public void testProxyAuthenticationRequiredResponseMapping() throws Exception {
		// act
		TerminationCause cause = errorResponseToTerminationCauseMapper.map(Response.PROXY_AUTHENTICATION_REQUIRED);
		
		// assert
		assertEquals(TerminationCause.Forbidden, cause);
	}
	
	@Test
	public void testUnauthorizedResponseMapping() throws Exception {
		// act
		TerminationCause cause = errorResponseToTerminationCauseMapper.map(Response.UNAUTHORIZED);
		
		// assert
		assertEquals(TerminationCause.Forbidden, cause);
	}
}
