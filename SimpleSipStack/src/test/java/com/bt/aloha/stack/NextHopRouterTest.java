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

 	

 	
 	
 
package com.bt.aloha.stack;


import static org.junit.Assert.assertEquals;
import gov.nist.core.net.AddressResolver;
import gov.nist.javax.sip.SIPConstants;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.message.SIPRequest;

import javax.sip.address.Hop;
import javax.sip.address.SipURI;
import javax.sip.message.Request;

import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.stack.NextHopRouter;

public class NextHopRouterTest {
	private static String sipStackName = "testStack";
	private static String nextHopPropertyName = NextHopRouter.NEXT_HOP + "_" + sipStackName;
	NextHopRouter nextHopRouter;
	
	private class MyAddressResolver implements AddressResolver {
		public Hop resolveAddress(Hop arg0) {
			return arg0;
		}
	}
	
	@Before
	public void setUp() throws Exception {
		MyAddressResolver addressResolver = new MyAddressResolver();
		SipStackImpl sipStack = EasyMock.createMock(SipStackImpl.class);
		EasyMock.expect(sipStack.getStackName()).andStubReturn(sipStackName);
		EasyMock.expect(sipStack.getAddressResolver()).andStubReturn(addressResolver);
		EasyMock.replay(sipStack);
		nextHopRouter = new NextHopRouter(sipStack, null);
		System.setProperty(nextHopPropertyName, "1.2.3.4=4.3.2.1:5678;10.238.67.22=0.1.2.3:4567;12.*=a.b.c.d:1234");
	}
	
	private Request setUpMocks(String host, int port) {
		SipUri sipUri = EasyMock.createMock(SipUri.class);
		EasyMock.expect(sipUri.getHost()).andStubReturn(host);
		EasyMock.expect(sipUri.getPort()).andStubReturn(port);
		EasyMock.expect(sipUri.isSipURI()).andStubReturn(true);
		EasyMock.expect(sipUri.isSecure()).andStubReturn(false);
		EasyMock.expect(sipUri.getTransportParam()).andStubReturn(SIPConstants.UDP);
		EasyMock.expect(sipUri.getMAddrParam()).andStubReturn(null);
		EasyMock.replay(sipUri);
		
		SIPRequest request = new SIPRequest();
		request.setRequestURI(sipUri);
		return request;
	}
	
	@SuppressWarnings("deprecation")
	@Test(expected=UnsupportedOperationException.class)
	public void testGetNextHops() throws Exception {
		// act
		nextHopRouter.getNextHops(EasyMock.createNiceMock(Request.class));
	}

	@Test(expected=UnsupportedOperationException.class)
	public void testGetOutboundProxy() throws Exception {
		// act
		nextHopRouter.getOutboundProxy();
	}

	// Test that getNextHop returns expected value for first value in match-string
	@Test
	public void testGetNextHopReturnsExpectedValueForFirstValue() throws Exception {
		// setup
		Request request = setUpMocks("1.2.3.4", 5060);
		
		// act
		Hop hop = nextHopRouter.getNextHop(request);
		
		// assert
		assertEquals("4.3.2.1", hop.getHost());
		assertEquals(5678, hop.getPort());
	}

	// Test that getNextHop returns expected value for last value in match-string
	@Test
	public void testGetNextHopReturnsExpectedValueForLastValue() throws Exception {
		// setup
		Request request = setUpMocks("10.238.67.22", 5060);
		
		// act
		Hop hop = nextHopRouter.getNextHop(request);
		
		// assert
		assertEquals("0.1.2.3", hop.getHost());
		assertEquals(4567, hop.getPort());
	}
	
	// Test that getNextHop returns expected value for last value in match-string
	@Test
	public void testGetNextHopWithNoValuesSetReturnsRequestHostAndPort() throws Exception {
		// setup
		System.clearProperty(nextHopPropertyName);
		Request request = setUpMocks("10.238.67.22", 5060);
		
		// act
		Hop hop = nextHopRouter.getNextHop(request);
		
		// assert
		assertEquals("10.238.67.22", hop.getHost());
		assertEquals(5060, hop.getPort());
	}
	
	// Test that getNextHop returns expected value for last value in match-string
	@Test
	public void testGetNextHopWithUnspecifiedValueReturnsRequestHostAndPort() throws Exception {
		// setup
		Request request = setUpMocks("10.20.30.40", 5060);
		
		// act
		Hop hop = nextHopRouter.getNextHop(request);
		
		// assert
		assertEquals("10.20.30.40", hop.getHost());
		assertEquals(5060, hop.getPort());
	}
	
	// Test that getNextHop returns next hop port to be 5060 if nothing is specified
	@Test
	public void testGetNextHopWithUnspecifiedPortReturns5060AsPort() throws Exception {
		// setup
		System.setProperty(nextHopPropertyName, "1.2.3.4=5.6.7.8");
		Request request = setUpMocks("1.2.3.4", 5060);

		// act
		Hop hop = nextHopRouter.getNextHop(request);
		
		// assert
		assertEquals("5.6.7.8", hop.getHost());
		assertEquals(5060, hop.getPort());
	}
	
	// Test that getNextHop throws IllegalArgumentException if request contains non-sip uri
	@Test(expected=IllegalArgumentException.class)
	public void testGetNextHopNonSipUri() throws Exception {
		// setup
		SipURI sipUri = EasyMock.createMock(SipURI.class);
		EasyMock.expect(sipUri.isSipURI()).andStubReturn(false);
		EasyMock.replay(sipUri);
		Request request = EasyMock.createMock(Request.class);
		EasyMock.expect(request.getRequestURI()).andStubReturn(sipUri);
		EasyMock.replay(request);
		
		// act
		nextHopRouter.getNextHop(request);
	}
	
	// Test that regexes work
	@Test
	public void testRegexSpecificationWorks() throws Exception {
		// setup
		Request request = setUpMocks("123.123.123.123", 7890);
		
		// act
		Hop hop = nextHopRouter.getNextHop(request);
		
		// assert
		assertEquals("a.b.c.d", hop.getHost());
		assertEquals(1234, hop.getPort());
	}
}
