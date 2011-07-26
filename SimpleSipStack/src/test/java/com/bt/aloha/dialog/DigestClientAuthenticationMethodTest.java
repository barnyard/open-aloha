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
package com.bt.aloha.dialog;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.dialog.DigestClientAuthenticationMethod;
import com.bt.aloha.stack.StackException;

public class DigestClientAuthenticationMethodTest {

	private DigestClientAuthenticationMethod digestClientAuthenticationMethod;
	private String realm;
	private String nonce;
	private String password;
	private String username;
	private String uri;
	private String cnonce;
	private String method;
	private String algorithm;
	
	@Before
	public void before() {
		digestClientAuthenticationMethod = new DigestClientAuthenticationMethod();
		realm = "172.25.58.151";
		nonce = "47a0a83241311fd221ea7e6ebe0f113ac9bcfc79";
		password = "secret";
		username = "adrian";
		uri = "sip:" + realm;
		cnonce = null;
		method = "REGISTER";
		algorithm = "MD5";
	}
	
	// happy path - difficult to genuinely assert the result, as we'd be actually testing the JDKs MessageDigest class
	@Test
	public void testGenerateResponse() {
		// setup

		// act
		String result = digestClientAuthenticationMethod.generateResponse(realm, username, uri, nonce, password, method, cnonce, algorithm);
		
		// assert
		assertEquals("60187da6635fe15d3b8863bafb24743a", result);
	}
	
	@Test(expected = StackException.class)
	public void testInvalidAlgorithm() {
		// setup
		String algorithm = "SecretSquirrel";
		
		// act
		digestClientAuthenticationMethod.generateResponse(realm, username, uri, nonce, password, method, cnonce, algorithm);
	}
}
