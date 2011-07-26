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
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.sip.message.Request;

import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.stack.QueuedSipMessageKey;
import com.bt.aloha.stack.QueuedSipMessageLatch;
import com.bt.aloha.stack.SipDialogMessageQueue;
import com.bt.aloha.stack.StackException;

public class SipDialogMessageQueueTest {
	private static final int BLOCKING_INTERVAL = 50;
	private SipDialogMessageQueue sipDialogMessageQueue;
	private StopWatch blockedFor;


	@Before
	public void setUp() throws Exception {
		sipDialogMessageQueue = new SipDialogMessageQueue(50, new ScheduledThreadPoolExecutor(10));
		blockedFor = new StopWatch();
	}

	// test adding next sequence number works
	@Test
	public void testAddNextSequenceNumber() throws Exception {
		// act
		long nextSequenceNumber = sipDialogMessageQueue.enqueueRequest(3, Request.ACK);

		// assert
		assertEquals(3, nextSequenceNumber);
	}

	// test trying to add negative sequence number returns sequence number of 1
	@Test
	public void testAddNegativeSequenceNumber() throws Exception {
		// act
		long nextSequenceNumber = sipDialogMessageQueue.enqueueRequest(-3, Request.ACK);

		// assert
		assertEquals(1, nextSequenceNumber);
	}

	// test adding 0 sequence number works
	@Test
	public void testAdd0SequenceNumber() throws Exception {
		// act
		long nextSequenceNumber = sipDialogMessageQueue.enqueueRequest(0, Request.ACK);

		// assert
		assertEquals(1, nextSequenceNumber);
	}


	// test trying to add negative sequence number returns next sequence number
	@Test
	public void testAddNegativeSequenceNumberToExistingListReturnsNextNumber() throws Exception {
		// setup
		sipDialogMessageQueue.enqueueRequest(3, Request.ACK);

		// act
		long nextSequenceNumber = sipDialogMessageQueue.enqueueRequest(-3, Request.ACK);

		// assert
		assertEquals(4, nextSequenceNumber);
	}

	// test that if sequence number already exists, provide next available number
	@Test
	public void testReturnNextAvailableNumberIfNumberTaken() throws Exception {
		// setup
		sipDialogMessageQueue.enqueueRequest(3, Request.ACK);

		// act
		long nextSequenceNumber = sipDialogMessageQueue.enqueueRequest(3, Request.ACK);

		// assert
		assertEquals(4, nextSequenceNumber);
	}

	// test that if higher sequence number exists, provide next available number
	@Test
	public void testReturnNextAvailableNumberIfHigherNumberTaken() throws Exception {
		// setup
		sipDialogMessageQueue.enqueueRequest(5, Request.ACK);

		// act
		long nextSequenceNumber = sipDialogMessageQueue.enqueueRequest(3, Request.ACK);

		// assert
		assertEquals(6, nextSequenceNumber);
	}

	// test that if the requested seq num is > 1 higher than current, the requested number is assigned
	@Test
	public void testReturnNextAvailableNumberIf() throws Exception {
		// setup
		sipDialogMessageQueue.enqueueRequest(3, Request.ACK);

		// act
		long nextSequenceNumber = sipDialogMessageQueue.enqueueRequest(5, Request.ACK);

		// assert
		assertEquals(5, nextSequenceNumber);
	}

	// test that adding a sequence number to the head of an empty queue adds it to the head of the queue
	@Test
	public void testAddSequenceNumberForceWithEmptyQueue() throws Exception {
		// act
		sipDialogMessageQueue.enqueueRequestForceSequenceNumber(2, Request.ACK);

		// assert
		assertEquals(2, sipDialogMessageQueue.getQueue().firstKey().getSequenceNumber());
        assertTrue(sipDialogMessageQueue.getLastSequenceNumberCreatedAt() > 0);
	}

    // test that adding a sequence number to the tail of a queue updates the lastSequenceNumberCreatedAt
    @Test
    public void testAddSequenceNumberForceWithNonEmptyQueue() throws Exception {
        // setup
        sipDialogMessageQueue.enqueueRequest(1, Request.ACK);
        long lastSequenceNumberCreatedAt = sipDialogMessageQueue.getLastSequenceNumberCreatedAt();

        // act
        Thread.sleep(50);
        sipDialogMessageQueue.enqueueRequestForceSequenceNumber(2, Request.ACK);

        // assert
        assertEquals(2, sipDialogMessageQueue.getQueue().size());
        assertEquals(1, sipDialogMessageQueue.getQueue().firstKey().getSequenceNumber());
        assertTrue(sipDialogMessageQueue.getLastSequenceNumberCreatedAt() > lastSequenceNumberCreatedAt);
    }

    // test that adding a sequence number with the same seq number as the last existing message on the queue to the tail of a queue updates the lastSequenceNumberCreatedAt
    @Test
    public void testAddSequenceNumberForceWithNonEmptyQueueAndSameSequenceNumberAsCurrentlyInTail() throws Exception {
        // setup
        sipDialogMessageQueue.enqueueRequest(2, Request.INVITE);
        long lastSequenceNumberCreatedAt = sipDialogMessageQueue.getLastSequenceNumberCreatedAt();

        // act
        Thread.sleep(50);
        sipDialogMessageQueue.enqueueRequestForceSequenceNumber(2, Request.ACK);

        // assert
        assertEquals(2, sipDialogMessageQueue.getQueue().size());
        assertTrue(sipDialogMessageQueue.getLastSequenceNumberCreatedAt() > lastSequenceNumberCreatedAt);
    }

	// test that adding a sequence number to the head of the queue adds it to the head of the queue
	@Test
	public void testAddSequenceNumberForce() throws Exception {
		// setup
		sipDialogMessageQueue.enqueueRequest(3, Request.ACK);
		sipDialogMessageQueue.enqueueRequest(4, Request.ACK);
		long lastSequenceNumberCreatedAt = sipDialogMessageQueue.getLastSequenceNumberCreatedAt();

		// act
		sipDialogMessageQueue.enqueueRequestForceSequenceNumber(2, Request.ACK);

		// assert
		assertEquals(3, sipDialogMessageQueue.getQueue().size());
		assertEquals(2, sipDialogMessageQueue.getQueue().firstKey().getSequenceNumber());
		assertEquals(lastSequenceNumberCreatedAt, sipDialogMessageQueue.getLastSequenceNumberCreatedAt());
	}

	// Testing that forced ACK overtakes INVITE with greater seq number
	@Test
	public void testAddSequenceNumberForceAckAfterInvite() throws Exception {
		// setup
		sipDialogMessageQueue.enqueueRequest(3, Request.INVITE);
        long lastSequenceNumberCreatedAt = sipDialogMessageQueue.getLastSequenceNumberCreatedAt();

		// act
		sipDialogMessageQueue.enqueueRequestForceSequenceNumber(2, Request.ACK);

		// assert
		assertEquals(2, sipDialogMessageQueue.getQueue().size());
		assertEquals(Request.ACK, sipDialogMessageQueue.getQueue().firstKey().getMethod());
        assertEquals(lastSequenceNumberCreatedAt, sipDialogMessageQueue.getLastSequenceNumberCreatedAt());
	}

	//Testing that forced ACK doesn't overtake existing INVITE with the same seq number
	@Test
	public void testAddSequenceNumberForceAckWithTheSameSeqNum() throws Exception {
		// setup
		sipDialogMessageQueue.enqueueRequest(3, Request.INVITE);

		// act
		sipDialogMessageQueue.enqueueRequestForceSequenceNumber(3, Request.ACK);

		// assert
		assertEquals(2, sipDialogMessageQueue.getQueue().size());
		assertEquals(Request.INVITE, sipDialogMessageQueue.getQueue().firstKey().getMethod());
	}


	// test that isNextSequenceNumber returns true if expected
	// test that blockUntilCanSend doesn't blocks when next seqnum == our seqnum
	@Test
	public void testBlockUntilCanSendWhenCanSendImmediately() throws Exception {
		// setup
		sipDialogMessageQueue.enqueueRequest(5, Request.ACK);
		sipDialogMessageQueue.setQueuedSipMessageBlockingInterval(1000);

		// act
		blockedFor.start();
		sipDialogMessageQueue.blockUntilCanSend(5, Request.ACK);
		blockedFor.stop();

		// assert
		assertTrue("apparently we blocked", blockedFor.shorterThan(1000));
	}

	// test that blockUntilCanSend throws exception when we don't have a request with that seqnum
	@Test(expected=StackException.class)
	public void testBlockUntilCanSendThrowsExceptionWhenWrongMethod() throws Exception {
		// setup
		sipDialogMessageQueue.enqueueRequest(5, Request.INVITE);
		sipDialogMessageQueue.setQueuedSipMessageBlockingInterval(1000);

		// act
		sipDialogMessageQueue.blockUntilCanSend(5, Request.ACK);
	}

	// test that blockUntilCanSend blocks for blocking interval before allowing to send
	@Test
	public void testBlockUntilCanSendBlocksWhenCannotSend() throws Exception {
		//setup
		sipDialogMessageQueue.setQueuedSipMessageBlockingInterval(200);
		sipDialogMessageQueue.enqueueRequest(4, Request.ACK);
		sipDialogMessageQueue.enqueueRequest(5, Request.ACK);

		// act
		blockedFor.start();
		sipDialogMessageQueue.blockUntilCanSend(5, Request.ACK);
		blockedFor.stop();

		// assert
		assertTrue( blockedFor.longerThan(BLOCKING_INTERVAL));
	}

	// test that blockUntilCanSend doesn't block if there is no messages in the queue
	@Test(expected=StackException.class)
	public void testBlockUntilCanSendDoesntBlockWhenEmptyQueue() throws Exception {
		// act
		sipDialogMessageQueue.blockUntilCanSend(5, Request.ACK);
	}

    // test that we don't block if checking a lower sequence number than the current one - (allow sending of old ones)
	@Test
	public void testBlockUntilCanSendDoesntBlockIfLowerSequenceNumber() throws Exception {
		// setup
        sipDialogMessageQueue.enqueueRequest(3, Request.ACK);
		sipDialogMessageQueue.enqueueRequest(4, Request.ACK);

		// act
		blockedFor.start();
		sipDialogMessageQueue.blockUntilCanSend(3, Request.ACK);
		blockedFor.stop();

		// assert
		assertTrue("apparently we blocked", blockedFor.shorterThan(BLOCKING_INTERVAL));
	}

	// test that we don't block if number is higher but lower number expired
	@Test
	public void testBlockUntilCanSendDoesntBlockIfLowerNumberExpired() throws Exception {
		// setup
		sipDialogMessageQueue.enqueueRequest(4, Request.ACK);
        sipDialogMessageQueue.enqueueRequest(5, Request.ACK);
		sipDialogMessageQueue.blockUntilCanSend(5, Request.ACK);
		sipDialogMessageQueue.enqueueRequest(6, Request.ACK);

		// act
		blockedFor.start();
		sipDialogMessageQueue.blockUntilCanSend(6, Request.ACK);
		blockedFor.stop();

		// assert
		assertTrue("apparently we blocked", blockedFor.shorterThan(BLOCKING_INTERVAL));
	}

	@Test(expected=StackException.class)
	public void testBlockUntilCanSendExceptionWhenSequenceNotInQueue() {
        // setup
        sipDialogMessageQueue.enqueueRequest(4, Request.ACK);
        sipDialogMessageQueue.dequeueRequest(4, Request.ACK);

        // act
        sipDialogMessageQueue.blockUntilCanSend(5, Request.ACK);
    }

	// test that notifying sequence number unblocks waiting threads
	@Test
	public void testNotifyUnblocksWaitingThread() throws Exception {
		// setup
		sipDialogMessageQueue.setQueuedSipMessageBlockingInterval(100);
		sipDialogMessageQueue.enqueueRequest(4, Request.ACK);
		sipDialogMessageQueue.enqueueRequest(5, Request.ACK);
		final Semaphore semaphore = new Semaphore(0);
		Runnable runnable = new Runnable() {
			public void run() {
				blockedFor.start();
				sipDialogMessageQueue.blockUntilCanSend(5, Request.ACK);
				blockedFor.stop();
				semaphore.release();
			}
		};
		new Thread(runnable).start();
		Thread.sleep(5);

		// act
		sipDialogMessageQueue.dequeueRequest(4, Request.ACK);

		// assert
		assertTrue(semaphore.tryAcquire(1, TimeUnit.SECONDS));
		assertFalse(blockedFor.longerThan(sipDialogMessageQueue.getQueuedSipMessageBlockingInterval()));
	}

	// test that notifying on a sequence number not at the head counts down that latch
	@Test
	public void testNotifyNonHeadOfQueueHasNoEffect() throws Exception {
		// setup
		sipDialogMessageQueue.setQueuedSipMessageBlockingInterval(100);
		sipDialogMessageQueue.enqueueRequest(4, Request.ACK);
		sipDialogMessageQueue.enqueueRequest(5, Request.ACK);
		QueuedSipMessageLatch message5 = sipDialogMessageQueue.getQueue().get(new QueuedSipMessageKey(5L, Request.ACK));

		// act
		sipDialogMessageQueue.dequeueRequest(5, Request.ACK);

		// assert
		assertEquals(1, sipDialogMessageQueue.getQueue().get(new QueuedSipMessageKey(4L, Request.ACK)).getCount());
//		assertFalse(sipDialogMessageQueue.getQueue().containsKey(5L));
        assertTrue(sipDialogMessageQueue.getQueue().containsKey(new QueuedSipMessageKey(5L, Request.ACK)));
		assertEquals(0, message5.getCount());
	}

	private class StopWatch {
		private long before;
		private long after;

		public StopWatch() {
			reset();
		}
		public void stop() {
			after = System.currentTimeMillis();
		}
		public void start() {
			before = System.currentTimeMillis();
		}
		public void reset() {
			before = 0;
			after = 0;
		}
		public long duration() {
			return after - before;
		}

		public boolean longerThan(long aDuration) {
			return duration() >= aDuration;
		}

		public boolean shorterThan(long aDuration) {
			return duration() <= aDuration;
		}
	}
}
