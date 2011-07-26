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
package com.bt.aloha.stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.SipException;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TransactionState;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Request;

import org.easymock.classextension.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.bt.aloha.stack.NextHopRouter;
import com.bt.aloha.stack.SimpleSipStack;
import com.bt.aloha.stack.SipStackMessageQueueCollection;
import com.bt.aloha.stack.StackException;

public class SimpleSipStackTest  {
	private String defaultIpPattern = "127.0.0.1";
	private int defaultPort = 5666;
	private String defaultTransport = "udp";
	private String stackName = "SimpleSipStackTest";
	private SimpleSipStack simpleSipStack;

	public SimpleSipStackTest() {
	}

	// NIST stack currently can't instantiate a javax.sip.address.Router impl through a default constructor
	public SimpleSipStackTest(SipStack sipStack, String defaultRoute) {
	}

	@Before
	public void before() {
		simpleSipStack = new SimpleSipStack();
		simpleSipStack.setIpAddress(defaultIpPattern);
		simpleSipStack.setPort(defaultPort);
		simpleSipStack.setTransport(defaultTransport);
		simpleSipStack.setStackName(stackName);
		
	}

	@After
	public void after() {
		System.clearProperty(NextHopRouter.NEXT_HOP + "_" + stackName);
		if(simpleSipStack != null) {
			simpleSipStack.destroy();
			simpleSipStack = null;
		}
	}

	@Test
	public void testSipRouterClass() throws Exception {
		// setup

		// act
		Properties props = new Properties();
		props.setProperty("javax.sip.ROUTER_PATH", NextHopRouter.class.getName());
		simpleSipStack.setJainStackProperties(props);
		simpleSipStack.init();

		// assert
		assertEquals("Wrong router class", NextHopRouter.class.getName(), simpleSipStack.getSipStack().getRouter().getClass().getName());
	}
	
	// test that setting the ContactAddress works OK
	@Test
	public void testSetContactAddress() throws Exception {
		// setup
		
		// act
		simpleSipStack.setContactAddress("123.123.123.123:1234");
		
		//assert
		assertEquals("123.123.123.123:1234", simpleSipStack.getContactAddress());
	}

	// test that an empty String doesn't result in the ContactAddress being set in any way
	@Test
	public void testSetContactAddressEmptyString() throws Exception {
		// setup
		
		// act
		simpleSipStack.setContactAddress("");
		
		//assert
		assertEquals(simpleSipStack.getIpAddress() + ":" + simpleSipStack.getPort(), simpleSipStack.getContactAddress());
	}

	// test that a null doesn't result in the ContactAddress being set in any way
	@Test
	public void testSetContactAddressNull() throws Exception {
		// setup
		
		// act
		simpleSipStack.setContactAddress(null);
		
		//assert
		assertEquals(simpleSipStack.getIpAddress() + ":" + simpleSipStack.getPort(), simpleSipStack.getContactAddress());
	}
	
	// test that RE-setting the contact Address with null results in the
	// contact address being set to the address of the stack 
	@Test
	public void testSetContactAddressNullResetContactAddress() throws Exception {
		// setup
		simpleSipStack.setContactAddress("123.123.123.123:1234");
		assertEquals("123.123.123.123:1234", simpleSipStack.getContactAddress());
		
		// act
		simpleSipStack.setContactAddress(null);
		
		//assert
		assertEquals(simpleSipStack.getIpAddress() + ":" + simpleSipStack.getPort(), simpleSipStack.getContactAddress());
	}

	// test that RE-setting the contact Address with empty string results in the
	// contact address being set to the address of the stack 
	@Test
	public void testSetContactAddressEmptyStringResetContactAddress() throws Exception {
		// setup
		simpleSipStack.setContactAddress("123.123.123.123:1234");
		assertEquals("123.123.123.123:1234", simpleSipStack.getContactAddress());
		
		// act
		simpleSipStack.setContactAddress("");
		
		//assert
		assertEquals(simpleSipStack.getIpAddress() + ":" + simpleSipStack.getPort(), simpleSipStack.getContactAddress());
	}

	@Test
	public void testConstructor() {
        // for Emma
		simpleSipStack.setSleepIntervalBeforeSending(100);
        simpleSipStack.setSipDebugLog("aSipDebugLog");
        simpleSipStack.setSipServerLog("aSipServerLog");
        simpleSipStack.setSipTraceLevel("aSipTraceLevel");

		assertEquals(defaultPort, simpleSipStack.getPort());
		assertEquals(defaultTransport, simpleSipStack.getTransport());
        assertEquals(100, simpleSipStack.getSleepIntervalBeforeSending());
	}

	@Ignore
	@Test
	public void testNullConstructorParams() {
		try {
			simpleSipStack = new SimpleSipStack();
			fail("Should have gotten an exception");
		} catch (StackException e) {
			assertNotNull(e);
		}

		try {
			simpleSipStack = new SimpleSipStack();
			fail("Should have gotten an exception");
		} catch (StackException e) {
			assertNotNull(e);
		}

		try {
			simpleSipStack = new SimpleSipStack();
			fail("Should have gotten an exception");
		} catch (StackException e) {
			assertNotNull(e);
		}

		try {
			simpleSipStack = new SimpleSipStack();
			fail("Should have gotten an exception");
		} catch (StackException e) {
			assertNotNull(e);
		}
	}

	@Test
	public void testInitDestroyHappyPath() {
		simpleSipStack.init();
		assertNotNull(simpleSipStack.getSipStack());

		simpleSipStack.destroy();
		assertNull(simpleSipStack.getSipStack());
	}

	@Test
	public void testDestroy() throws Exception {
		// Setup
		SipStack stack = EasyMock.createMock(SipStack.class);
		simpleSipStack.setSipStack(stack);

		stack.stop();
		EasyMock.replay(stack);

		// Act
		simpleSipStack.destroy();

		// Assert
		assertNull(simpleSipStack.getSipStack());
		EasyMock.verify(stack);

	}

	@Test
	public void testInitStackListenerException() throws Exception {
		//setup
		SipStack stack = EasyMock.createMock(SipStack.class);

		String ip = "1.2.3.4";
		simpleSipStack.setStackName(stackName+"2");

		// EasyMock script
		EasyMock.expect(stack.createListeningPoint(ip, this.defaultPort, this.defaultTransport)).andThrow(new RuntimeException());
		EasyMock.replay(stack);

		//act
		try {
			simpleSipStack.initStackListener(stack, ip, this.defaultPort, this.defaultTransport);
			fail("Exception should have been thrown");
		} catch (StackException e) {
			assertEquals("Failed to add listening point", e.getMessage());
		}

		//verify
		EasyMock.verify(stack);

	}

	@Test
	public void testInitStackListener() throws Exception {
		// Setup
		SipStack stack = EasyMock.createMock(SipStack.class);
		ListeningPoint lp = EasyMock.createMock(ListeningPoint.class);
		SipProvider sipProvider = EasyMock.createMock(SipProvider.class);
		SipListener sipListener = EasyMock.createMock(SipListener.class);

		String ip = "1.2.3.4";
		simpleSipStack.setSipListener(sipListener);

		// EasyMock script
		EasyMock.expect(stack.createListeningPoint(ip, this.defaultPort, this.defaultTransport)).andReturn(lp);
		EasyMock.expect(stack.createSipProvider(lp)).andReturn(sipProvider);
		sipProvider.addSipListener(sipListener);
		stack.start();

		EasyMock.replay(stack, lp, sipProvider, sipListener);

		// Act
		simpleSipStack.initStackListener(stack, ip, this.defaultPort, this.defaultTransport);

		// Verify
		EasyMock.verify(stack, lp, sipProvider, sipListener);

	}

	@Test
	public void testInitSipStackException() {
		// Setup
		simpleSipStack = new SimpleSipStack()  {
			@Override
			protected SipStack createSipStack() throws PeerUnavailableException {
				throw new PeerUnavailableException("...");
			}
		};

		// Act
		try {
			simpleSipStack.init();
			fail("Exception not thrown");
		} catch(StackException e) {
			// Assert
			assertNotNull(e);
		}
	}

	@Test
	public void testGenerateNewTag() {
		int enteries = 100;
		Map<String, String> map = new HashMap<String, String>();
		for (int index = 0; index < enteries; index++) {
			String newTag = simpleSipStack.generateNewTag();
			map.put(newTag, null);
		}
		assertEquals(enteries, map.size());
	}

	@Test
	public void testGetPort() {
		// Setup

		// Act
		int port = simpleSipStack.getPort();

		// Asset
		assertEquals(port, this.defaultPort);
	}

	@Test
	public void testGetTransport() {
		// Setup

		// Act
		String transport = simpleSipStack.getTransport();

		// Asset
		assertEquals(transport, this.defaultTransport);
	}

	@Test
	public void testNonNullContactAddress() {
		// Setup
		String contactAddress = "local";

		// Act
		simpleSipStack.setContactAddress(contactAddress);

		// Asset
		assertEquals(contactAddress, simpleSipStack.getContactAddress());
	}

	@Test
	public void testAreFactoriesSet() {
		// Setup

		// Assert
		assertNull(simpleSipStack.getAddressFactory());
		assertNull(simpleSipStack.getHeaderFactory());
		assertNull(simpleSipStack.getMessageFactory());

		// Act
		simpleSipStack.init();

		// Asset
		assertNotNull(simpleSipStack.getAddressFactory());
		assertNotNull(simpleSipStack.getHeaderFactory());
		assertNotNull(simpleSipStack.getMessageFactory());

		// Tidy up
		simpleSipStack.destroy();
	}

	// Test that setting next hop routes sets System Property
	@Test
	public void testSetNextHopRoutesSetsSystemProperty() throws Exception {
		// setup

		// act
		simpleSipStack.setNextHopRoutes("1.1.1.1=2.2.2.2");

		// assert
		assertEquals("1.1.1.1=2.2.2.2", System.getProperty(NextHopRouter.NEXT_HOP + "_" + stackName));
	}

	// Test setting empty next hop routes does not set system property
	@Test
	public void testSetEmptyNextHopRoutesDoesNotSetSystemProperty() throws Exception {
		// setup

		// act
		simpleSipStack.setNextHopRoutes("");

		// assert
		assertNull(System.getProperty(NextHopRouter.NEXT_HOP + "_" + stackName));
	}

	// Test setting next hop routes with spaces does not set system property
	@Test
	public void testSetNextHopRoutesWithSpacesDoesNotSetSystemProperty() throws Exception {
		// setup

		// act
		simpleSipStack.setNextHopRoutes("      ");

		// assert
		assertNull(System.getProperty(NextHopRouter.NEXT_HOP + "_" + stackName));
	}

	// Test setting next hop routes to a value with a missing equal sign throws an IllegalArgumentException
	@Test(expected=IllegalArgumentException.class)
	public void testSetNextHopWithMissingEqualsThrowsIllegalArgumentException() throws Exception {
		// setup

		// act
		simpleSipStack.setNextHopRoutes("1.1.1.1");
	}

	private MyClientTransaction createClientTransaction(String callId, long seqNumber, String method) {
		CallIdHeader callIdHeader = EasyMock.createMock(CallIdHeader.class);
		EasyMock.expect(callIdHeader.getCallId()).andStubReturn(callId);
		EasyMock.replay(callIdHeader);
		CSeqHeader cSeqHeader = EasyMock.createMock(CSeqHeader.class);
		EasyMock.expect(cSeqHeader.getSeqNumber()).andStubReturn(seqNumber);
		EasyMock.replay(cSeqHeader);
		Request request = EasyMock.createNiceMock(Request.class);
		EasyMock.expect(request.getHeader(CallIdHeader.NAME)).andStubReturn(callIdHeader);
		EasyMock.expect(request.getHeader(CSeqHeader.NAME)).andStubReturn(cSeqHeader);
		EasyMock.expect(request.getMethod()).andReturn(method);
		EasyMock.replay(request);
		MyClientTransaction clientTransaction = new MyClientTransaction(request);
		return clientTransaction;
	}

	// test that when two calls are made to sendRequest using the same dialogId and out-of-order sequence numbers, the lower sequence number goes out first
	@Test
	public void testSipMessagesSendsLowerSequenceNumberFirst() throws Exception {
		// setup
		SipStackMessageQueueCollection sipMessageQueueCollection = new SipStackMessageQueueCollection();
		sipMessageQueueCollection.setScheduledExecutorService(new ScheduledThreadPoolExecutor(10));
		sipMessageQueueCollection.setMaxTimeToLive(10000);
		sipMessageQueueCollection.setQueuedSipMessageBlockingInterval(500);
		simpleSipStack.setSipStackMessageQueueCollection(sipMessageQueueCollection);
		final MyClientTransaction clientTransaction1 = createClientTransaction("callId", simpleSipStack.enqueueRequestAssignSequenceNumber("callId", 3, Request.ACK), Request.ACK);
		final MyClientTransaction clientTransaction2 = createClientTransaction("callId", simpleSipStack.enqueueRequestAssignSequenceNumber("callId", 4, Request.ACK), Request.ACK);
		Runnable r1 = new Runnable() {
			public void run() {
				simpleSipStack.sendRequest(clientTransaction2);
			}
		};
		Runnable r2 = new Runnable() {
			public void run() {
				simpleSipStack.sendRequest(clientTransaction1);
			}
		};

		// act
		new Thread(r1).start();
		Thread.sleep(10);
		new Thread(r2).start();

		Thread.sleep(500);

		// assert
		assertTrue(clientTransaction1.sendRequestTime > -1);
		assertTrue(clientTransaction2.sendRequestTime > -1);
		assertTrue(clientTransaction1.sendRequestTime <= clientTransaction2.sendRequestTime);
	}

	@Test
	@Ignore	// until we know if this will work or not
	public void testSleepBeforeSendForRequests() {
		// setup
		simpleSipStack.setSleepIntervalBeforeSending(200);
		long timeBefore = System.currentTimeMillis();

		// act
		try {
			simpleSipStack.sendRequest((ClientTransaction)null);
			fail("Should throw exception?");
		} catch(StackException e) {
			// assert
			assertTrue("sleep didn't happen", System.currentTimeMillis() > timeBefore + 200);
		}
	}

	public class MyClientTransaction implements ClientTransaction {
		private static final long serialVersionUID = 1L;
		private Request request;
		private long sendRequestTime;

		public MyClientTransaction(Request aRequest) {
			request = aRequest;
			sendRequestTime = -1;
		}

		public Request createAck() throws SipException {
			return null;
		}

		public Request createCancel() throws SipException {
			return null;
		}

		public void sendRequest() throws SipException {
			sendRequestTime = System.currentTimeMillis();
		}

		public Object getApplicationData() {
			return null;
		}

		public String getBranchId() {
			return null;
		}

		public Dialog getDialog() {
			return null;
		}

		public Request getRequest() {
			return request;
		}

		public int getRetransmitTimer() throws UnsupportedOperationException {
			return 0;
		}

		public TransactionState getState() {
			return null;
		}

		public void setApplicationData(Object arg0) {
		}

		public void setRetransmitTimer(int arg0) throws UnsupportedOperationException {
		}

		public void terminate() throws ObjectInUseException {
		}
	}
}
