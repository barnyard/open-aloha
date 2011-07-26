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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.sip.message.Request;

import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.stack.QueuedSipMessageKey;
import com.bt.aloha.stack.QueuedSipMessageLatch;
import com.bt.aloha.stack.SipDialogMessageQueue;
import com.bt.aloha.stack.SipStackMessageQueueCollection;
import com.bt.aloha.stack.StackException;

public class SipStackMessageQueueCollectionTest {
	private SipStackMessageQueueCollection sipStackMessageQueueCollection;

	@Before
	public void setUp() {
		sipStackMessageQueueCollection = new SipStackMessageQueueCollection();
		sipStackMessageQueueCollection.setScheduledExecutorService(new ScheduledThreadPoolExecutor(10));
	}

	// test that we get IllegalArgumentException while trying to add with null sipCallId
	@Test(expected=IllegalArgumentException.class)
	public void testAddWithNullCallId() throws Exception {
		// act
		sipStackMessageQueueCollection.enqueueRequest(null, 0, Request.ACK);
	}

	// test that adding works
	@Test
	public void testAddToSipMessageQueue() throws Exception {
		// act
		sipStackMessageQueueCollection.enqueueRequest("callId", 10, Request.ACK);

		// assert
		assertNotNull(sipStackMessageQueueCollection.getStackMessageQueueMap());
		assertEquals(1, sipStackMessageQueueCollection.getStackMessageQueueMap().size());
		assertTrue(sipStackMessageQueueCollection.getStackMessageQueueMap().containsKey("callId"));
	}

	// test that adding with same callid still has only 1 element in hashmap
	@Test
	public void testAddSameCallIdToSipMessageQueue() throws Exception {
		// setup
		sipStackMessageQueueCollection.enqueueRequest("callId", 10, Request.ACK);

		// act
		sipStackMessageQueueCollection.enqueueRequest("callId", 10, Request.ACK);

		// assert
		assertNotNull(sipStackMessageQueueCollection.getStackMessageQueueMap());
		assertEquals(1, sipStackMessageQueueCollection.getStackMessageQueueMap().size());
		assertTrue(sipStackMessageQueueCollection.getStackMessageQueueMap().containsKey("callId"));
	}

	// test that adding with different callid means 2 elements in hashmap
	@Test
	public void testAddDifferentCallIdsToSipMessageQueue() throws Exception {
		// setup
		sipStackMessageQueueCollection.enqueueRequest("callId", 10, Request.ACK);

		// act
		sipStackMessageQueueCollection.enqueueRequest("callId2", 10, Request.ACK);

		// assert
		assertNotNull(sipStackMessageQueueCollection.getStackMessageQueueMap());
		assertEquals(2, sipStackMessageQueueCollection.getStackMessageQueueMap().size());
		assertTrue(sipStackMessageQueueCollection.getStackMessageQueueMap().containsKey("callId"));
		assertTrue(sipStackMessageQueueCollection.getStackMessageQueueMap().containsKey("callId2"));
	}

	// test that we get IllegalArgumentException while checking isNextSipMessageInQueue
	@Test(expected=IllegalArgumentException.class)
	public void testIsNextSipMessageInQueueWithNullCallId() throws Exception {
		// act
		sipStackMessageQueueCollection.enqueueRequest(null, 0, Request.ACK);
	}

	// test that isNextSipMessageInQueue throws StackException if callId is not present in hashmap
	@Test(expected=StackException.class)
	public void testIsNextSipMessageInQueueUnknownCallId() throws Exception {
		// act
		sipStackMessageQueueCollection.blockUntilCanSendRequest("unknown", 2, Request.ACK);
	}

	// test that if queuedSipMessageRetryInterval is set to 0, the method should return immediately
	@SuppressWarnings("unchecked")
	@Test
	public void testQueuedSipMessageRetryIntervalIsZero() throws Exception {
		// setup
		ConcurrentHashMap<String, SipDialogMessageQueue> stackMessageQueue = EasyMock.createMock(ConcurrentHashMap.class);
		EasyMock.replay(stackMessageQueue);
		sipStackMessageQueueCollection.setQueuedSipMessageBlockingInterval(0);
		sipStackMessageQueueCollection.setStackMessageQueueMap(stackMessageQueue);

		// act
		sipStackMessageQueueCollection.blockUntilCanSendRequest("callId", 3, Request.ACK);

		// assert
		EasyMock.verify(stackMessageQueue);
	}

	// test that if queuedSipMessageRetryInterval is set to 0, the method should return the same sequence number
	@Test
	public void testAddWhenQueuedSipMessageRetryIntervalIsZero() throws Exception {
		// setup
		sipStackMessageQueueCollection.setQueuedSipMessageBlockingInterval(0);
		sipStackMessageQueueCollection.enqueueRequest("callId", 10, Request.ACK);

		// assert
		assertEquals(10, sipStackMessageQueueCollection.enqueueRequest("callId", 10, Request.ACK));
	}

	// test that if queuedSipMessageRetryInterval is set to 0, the method should return immediately
	@SuppressWarnings("unchecked")
	@Test
	public void testAddMessageWhenQueuedSipMessageRetryIntervalIsZero() throws Exception {
		// setup
		ConcurrentHashMap<String, SipDialogMessageQueue> stackMessageQueue = EasyMock.createMock(ConcurrentHashMap.class);
		EasyMock.replay(stackMessageQueue);
		sipStackMessageQueueCollection.setQueuedSipMessageBlockingInterval(0);

		// act
		sipStackMessageQueueCollection.enqueueRequestForceSequenceNumber("callId", 10, Request.ACK);

		// assert
		EasyMock.verify(stackMessageQueue);
	}

	// test that forcing ack message to queue adds it to the head of the queue and blocks other messages
	@Test
	public void testAddAckMessageAddsToHeadOfQueue() throws Exception {
		// setup
		sipStackMessageQueueCollection.enqueueRequest("callId", 10, Request.ACK);

		// act
		sipStackMessageQueueCollection.enqueueRequestForceSequenceNumber("callId", 9, Request.ACK);

		SipDialogMessageQueue queue = sipStackMessageQueueCollection.getStackMessageQueueMap().get("callId");
		SortedMap<QueuedSipMessageKey,QueuedSipMessageLatch> q = queue.getQueue();

		assertEquals(9L, q.firstKey().getSequenceNumber());
		assertTrue(q.get(new QueuedSipMessageKey(9L, Request.ACK)).isAlive());
		sipStackMessageQueueCollection.blockUntilCanSendRequest("callId", 10, Request.ACK);

		//assert
		assertFalse(q.get(new QueuedSipMessageKey(9L, Request.ACK)).isAlive());
	}

	// test that housekeep removes SipMessageQueueInfo which has expired
	@Test
	public void testHousekeep() throws Exception {
		// setup
		sipStackMessageQueueCollection.setMaxTimeToLive(1000);
		sipStackMessageQueueCollection.enqueueRequest("callId", 50, Request.ACK);
		sipStackMessageQueueCollection.enqueueRequest("callId2", 50, Request.ACK);
		sipStackMessageQueueCollection.getStackMessageQueueMap().get("callId").setLastSequenceNumberCreatedAt(System.currentTimeMillis()-10000);

		// act
		sipStackMessageQueueCollection.housekeep();

		// assert
		assertEquals(1, sipStackMessageQueueCollection.getStackMessageQueueMap().size());
		assertTrue(sipStackMessageQueueCollection.getStackMessageQueueMap().containsKey("callId2"));
	}

	// test that housekeep removes SipMessageQueueInfo which has expired
    @Test
    public void testHousekeepForForcedMessages() throws Exception {
        // setup
        sipStackMessageQueueCollection.setMaxTimeToLive(1000);
        sipStackMessageQueueCollection.enqueueRequestForceSequenceNumber("callId", 50, Request.ACK);
        sipStackMessageQueueCollection.enqueueRequestForceSequenceNumber("callId2", 50, Request.ACK);
        sipStackMessageQueueCollection.getStackMessageQueueMap().get("callId").setLastSequenceNumberCreatedAt(System.currentTimeMillis()-10000);

        // act
        sipStackMessageQueueCollection.housekeep();

        // assert
        assertEquals(1, sipStackMessageQueueCollection.getStackMessageQueueMap().size());
        assertTrue(sipStackMessageQueueCollection.getStackMessageQueueMap().containsKey("callId2"));
    }
}
