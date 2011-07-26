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
 * (c) British Telecommunications plc, 2008, All Rights Reserved
 */
package com.bt.aloha.testing.mockphones;

import static org.junit.Assert.assertTrue;

import javax.sip.message.Response;

import org.cafesip.sipunit.Credential;
import org.junit.Test;

import com.bt.aloha.testing.SimpleSipStackPerClassTestCase;

public class AuthenticatingDialogBeanTest extends SimpleSipStackPerClassTestCase {
	private static final String REALM = "bt.com";
	private static final String BADPASSWORD = "Barney";
	private static final String PASSWORD = "Wilma";
	private static final String USERNAME = "Fred";

	// test that the SimpleSipStackListener delegates properly on receiving a request for an authenticating phone
	@Test
	public void testInboundDialogDelegation() throws Exception {
		// setup
		this.setOutboundCallTargetUsername("authenticating");
		String remoteSipAddress = this.getRemoteSipAddress();
		// act
		Credential c = new Credential();
		c.setUser(USERNAME);
		c.setPassword(PASSWORD);
		c.setRealm(REALM);
		this.getOutboundCall().getParent().addUpdateCredential(c);
		// assert
		assertTrue(this.getOutboundCall().initiateOutgoingCall(remoteSipAddress, this.getRemoteSipProxy()));
		assertOutboundCallResponses(new int[] {Response.PROXY_AUTHENTICATION_REQUIRED, Response.RINGING,Response.OK});
		assertTrue("Not sent ACK", this.getOutboundCall().sendInviteOkAck());
		assertTrue(getOutboundCall().disconnect());
	}
	
	// test that the mockphone can deal with www-authenticate scenario.
	@Test
	public void testInboundDialogDelegationWWWAuthenicate() throws Exception {
		// setup
		this.setOutboundCallTargetUsername("authenticating401");
		String remoteSipAddress = this.getRemoteSipAddress();
		// act
		Credential c = new Credential();
		c.setUser(USERNAME);
		c.setPassword(PASSWORD);
		c.setRealm(REALM);
		this.getOutboundCall().getParent().addUpdateCredential(c);
		// assert
		assertTrue(this.getOutboundCall().initiateOutgoingCall(remoteSipAddress, this.getRemoteSipProxy()));
		assertOutboundCallResponses(new int[] {Response.UNAUTHORIZED, Response.RINGING,Response.OK});
		assertTrue("Not sent ACK", this.getOutboundCall().sendInviteOkAck());
		assertTrue(getOutboundCall().disconnect());
	}
	
	// test that authenticating mock phone rejects incorrect passwords
	@Test
	public void testInboundDialogDelegationWithBadPassword() throws Exception {
		// setup
		this.setOutboundCallTargetUsername("authenticating");
		String remoteSipAddress = this.getRemoteSipAddress();
		// act
		Credential c = new Credential();
		c.setUser(USERNAME);
		c.setPassword(BADPASSWORD);
		c.setRealm(REALM);
		this.getOutboundCall().getParent().addUpdateCredential(c);
		// assert
    	assertTrue(this.getOutboundCall().initiateOutgoingCall(remoteSipAddress, this.getRemoteSipProxy()));
		assertOutboundCallResponses(new int[] {Response.PROXY_AUTHENTICATION_REQUIRED, Response.FORBIDDEN});
	}
	
	// test that authenticating mock phone rejects incorrect WWW-Authentication passwords
	@Test
	public void testInboundDialogDelegationWWWAuthenicateWithBadPassword() throws Exception {
		// setup
		this.setOutboundCallTargetUsername("authenticating401");
		String remoteSipAddress = this.getRemoteSipAddress();
		// act
		Credential c = new Credential();
		c.setUser(USERNAME);
		c.setPassword(BADPASSWORD);
		c.setRealm(REALM);
		this.getOutboundCall().getParent().addUpdateCredential(c);
		// assert
    	assertTrue(this.getOutboundCall().initiateOutgoingCall(remoteSipAddress, this.getRemoteSipProxy()));
		assertOutboundCallResponses(new int[] {Response.UNAUTHORIZED, Response.FORBIDDEN});
	
	}
}
