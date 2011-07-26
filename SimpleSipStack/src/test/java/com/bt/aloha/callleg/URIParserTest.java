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

 	

 	
 	
 
package com.bt.aloha.callleg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.net.URI;

import org.junit.Test;

import com.bt.aloha.callleg.URIParameters;
import com.bt.aloha.callleg.URIParser;

public class URIParserTest {

	// Test happy path URI Parsing.
	@Test
	public void testParseURI() {
		//Setup
		URIParser uriParser = new URIParser();
		String expected = "sip:fred.bloggs@bt.com";
		URI uri = URI.create("sip:fred.bloggs@bt.com;username=fred;password=pass123");
		//Act
		URIParameters result = uriParser.parseURI(uri);
		//Assert
		assertEquals("fred",result.getUsername());
		assertEquals("pass123",result.getPassword());
		assertEquals(expected, result.getStrippedURI().toString());
		
	}
	
	
	// Test happy path URI Parsing - reversed params.
	@Test
	public void testParseURIReversedParams() {
		//Setup
		URIParser uriParser = new URIParser();
		String expected = "sip:fred.bloggs@bt.com";
		URI uri = URI.create("sip:fred.bloggs@bt.com;password=pass123;username=fred");
		//Act
		URIParameters result = uriParser.parseURI(uri);
		//Assert
		assertEquals("fred",result.getUsername());
		assertEquals("pass123",result.getPassword());
		assertEquals(expected, result.getStrippedURI().toString());
	}
	
	// Test if URIParser returns null if no parameters on URI
	@Test
	public void testParseURINoParams() {
		//Setup
		URIParser uriParser = new URIParser();
		String expected = "sip:fred.bloggs@bt.com";
		URI uri = URI.create("sip:fred.bloggs@bt.com");
		//Act
		URIParameters result = uriParser.parseURI(uri);
		//Assert
		assertNull(result.getUsername());
		assertNull(result.getPassword());
		assertEquals(expected, result.getStrippedURI().toString());
	}	

	
	// Test if URIParser returns null if URI Params are empty username
	@Test
	public void testParseURIEmptyParamsUser() {
		//Setup
		URIParser uriParser = new URIParser();
		String expected = "sip:fred.bloggs@bt.com";
		URI uri = URI.create("sip:fred.bloggs@bt.com;username=;password=pass123");
		//Act
		URIParameters result = uriParser.parseURI(uri);
		//Assert
		assertNull(result.getUsername());
		assertEquals("pass123",result.getPassword());
		assertEquals(expected, result.getStrippedURI().toString());
	}
	
	// Test if URIParser returns null if URI Params are empty password
	@Test
	public void testParseURIEmptyParamsPassword() {
		//Setup
		URIParser uriParser = new URIParser();
		String expected = "sip:fred.bloggs@bt.com";
		URI uri = URI.create("sip:fred.bloggs@bt.com;username=fred;password=");
		//Act
		URIParameters result = uriParser.parseURI(uri);
		//Assert
		assertNull(result.getPassword());
		assertEquals("fred",result.getUsername());
		assertEquals(expected, result.getStrippedURI().toString());
	}
	
	// Test if URIParser returns null if URI is Malformed
	@Test
	public void testParseURIMalformedURI() {
		//Setup
		URIParser uriParser = new URIParser();
		String expected = "sip:frusername=bobed.bloggs@bt.com";
		URI uri = URI.create("sip:frusername=bobed.bloggs@bt.com;username=fred;password=");
		//Act
		URIParameters result = uriParser.parseURI(uri);
		//Assert
		assertNull(result.getPassword());
		assertEquals("fred",result.getUsername());
		assertEquals(expected, result.getStrippedURI().toString());
	}
	
	// Test if parameter names are overrideable
	@Test
	public void testParameterOverride() {
		//Setup
		URIParser uriParser = new URIParser();
		uriParser.setUsernameParameterName("user");
		uriParser.setPasswordParameterName("pwd");
		String expected = "sip:fred.bloggs@bt.com";
		URI uri = URI.create("sip:fred.bloggs@bt.com;user=fred;pwd=pass123");
		//Act
		URIParameters result = uriParser.parseURI(uri);
		//Assert
		assertEquals("fred",result.getUsername());
		assertEquals("pass123",result.getPassword());
		assertEquals(expected, result.getStrippedURI().toString());
	}
	
	// Test if username and password parameters are removed from Request URI
	@Test
	public void testParameterremoved() {
		//Setup
		URIParser uriParser = new URIParser();
		uriParser.setRemoveUserAndPasswordParameters(true);
		String expectedURI = "sip:fred.bloggs@bt.com;someotherparam=foo";
		URI uri = URI.create("sip:fred.bloggs@bt.com;username=fred;password=pass123;someotherparam=foo");
		//Act
		URIParameters result = uriParser.parseURI(uri);
		//Assert
		assertEquals(expectedURI,result.getStrippedURI().toString());
	}
	
	// Test if username and password parameters are removed from Request URI different order
	@Test
	public void testParameterremovedReorders() {
		//Setup
		URIParser uriParser = new URIParser();
		uriParser.setRemoveUserAndPasswordParameters(true);
		String expectedURI = "sip:fred.bloggs@bt.com;someparam=foo;someotherparam=bar";
		URI uri = URI.create("sip:fred.bloggs@bt.com;someparam=foo;username=fred;password=pass123;someotherparam=bar");
		//Act
		URIParameters result = uriParser.parseURI(uri);
		//Assert
		assertEquals(expectedURI,result.getStrippedURI().toString());
	}
	
	// Test if username and password parameters are removed from Request URI when the parameter names are changed
	@Test
	public void testParameterremovedParamsChanged() {
		//Setup
		URIParser uriParser = new URIParser();
		uriParser.setRemoveUserAndPasswordParameters(true);
		uriParser.setUsernameParameterName("user");
		uriParser.setPasswordParameterName("pwd");
		String expectedURI = "sip:fred.bloggs@bt.com;someotherparam=foo";
		URI uri = URI.create("sip:fred.bloggs@bt.com;user=fred;pwd=pass123;someotherparam=foo");
		//Act
		URIParameters result = uriParser.parseURI(uri);
		//Assert
		assertEquals(expectedURI,result.getStrippedURI().toString());
	}
	
	
	// Test if username and password parameters are not removed from Request URI when the setRemoveUserAndPasswordParameters is set to false
	@Test
	public void testParameterAreNotRemovedIfConfigParamSetToFalse() {
		//Setup
		URIParser uriParser = new URIParser();
		uriParser.setRemoveUserAndPasswordParameters(false);
		String expectedURI = "sip:fred.bloggs@bt.com;someparam=foo;username=fred;password=pass123;someotherparam=bar";
		URI uri = URI.create("sip:fred.bloggs@bt.com;someparam=foo;username=fred;password=pass123;someotherparam=bar");
		//Act
		URIParameters result = uriParser.parseURI(uri);
		//Assert
		assertEquals(expectedURI,result.getStrippedURI().toString());
	}
}
