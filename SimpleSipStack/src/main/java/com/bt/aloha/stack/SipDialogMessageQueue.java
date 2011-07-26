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

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A sending queue for every dialog.
 * The idea is that, for every request we send, that request gets added to a queue 
 * for the dialog it belongs to from within a concurrent block. When we then call 
 * sendMessage outside the concurrent block, we check whether or not there are higher 
 * priority messages that are being sent for that same dialog, and if so (or until timeout)
 * we hold. This is there to make message sending more deterministic.
 */
public class SipDialogMessageQueue  {
    private static final int PRIME = 31;
	private static final int THIRTY_TWO = 32;
    private static final Log LOG = LogFactory.getLog(SipDialogMessageQueue.class);
	private static final String FORCING_SEQ_NUM_D_TO_QUEUE_METHOD_S = "Forcing seq num %d to queue, method %s";
	private ScheduledExecutorService scheduledExecutorService;
	private SortedMap<QueuedSipMessageKey, QueuedSipMessageLatch> q = new TreeMap<QueuedSipMessageKey, QueuedSipMessageLatch>();
	private long lastSequenceNumberCreatedAt;
	private long lastSequenceNumber;
	private long queuedSipMessageBlockingInterval;

	protected SipDialogMessageQueue(long aSipMessageTimeToLive, ScheduledExecutorService aScheduledExecutorService) {
		queuedSipMessageBlockingInterval = aSipMessageTimeToLive;
		scheduledExecutorService = aScheduledExecutorService;
	}

	protected synchronized long enqueueRequest(long sequenceNumber, String requestMethod) {
		LOG.debug(String.format("enqueueRequest(%d,%s)", sequenceNumber, requestMethod));
		long nextSequenceNumber;
		if (q.size() == 0)
			nextSequenceNumber = sequenceNumber <= 0 ? 1 : sequenceNumber;
		else {
			if (sequenceNumber > lastSequenceNumber)
				nextSequenceNumber = sequenceNumber;
			else
				nextSequenceNumber = lastSequenceNumber + 1;
		}
		LOG.debug(String.format(FORCING_SEQ_NUM_D_TO_QUEUE_METHOD_S, nextSequenceNumber, requestMethod));
		QueuedSipMessageKey key = new QueuedSipMessageKey(nextSequenceNumber, requestMethod);
		q.put(key, new QueuedSipMessageLatch(nextSequenceNumber, requestMethod, queuedSipMessageBlockingInterval, scheduledExecutorService));
		lastSequenceNumber = nextSequenceNumber;
		lastSequenceNumberCreatedAt = System.currentTimeMillis();
		LOG.debug(dumpQueue());
		return nextSequenceNumber;
	}

	public synchronized void enqueueRequestForceSequenceNumber(long sequenceNumber, String requestMethod) {
		LOG.debug(String.format(FORCING_SEQ_NUM_D_TO_QUEUE_METHOD_S, sequenceNumber, requestMethod));
		QueuedSipMessageKey key = new QueuedSipMessageKey(sequenceNumber, requestMethod);
		q.put(key, new QueuedSipMessageLatch(sequenceNumber, requestMethod, queuedSipMessageBlockingInterval, scheduledExecutorService));
		if (lastSequenceNumber <= sequenceNumber) {
			lastSequenceNumber = sequenceNumber;
	        lastSequenceNumberCreatedAt = System.currentTimeMillis();
		}
		LOG.debug(dumpQueue());
	}

	/**
	 * Blocks until message with the specified sequence number can be sent. Returns true if send was delayed.
	 * @param sequenceNumber
	 */
	protected void blockUntilCanSend(long sequenceNumber, String requestMethod) {
		QueuedSipMessageKey keyToSend = new QueuedSipMessageKey(sequenceNumber, requestMethod);
		QueuedSipMessageKey currentLiveKey = null;
		synchronized (this) {
            if (! q.containsKey(keyToSend))
                throw new StackException(String.format("Nothing in queue for message %d, method %s", sequenceNumber, requestMethod));
            currentLiveKey = getFirstLiveKey();
		}
		// for each message before us, wait, then remove it
		while (currentLiveKey != null && currentLiveKey.compareTo(keyToSend) < 0) {
			QueuedSipMessageLatch latch = q.get(currentLiveKey);
			try {
				if (latch.await(queuedSipMessageBlockingInterval, TimeUnit.MILLISECONDS))
					LOG.debug(String.format("Thread allowed to send message w/ seq number %d, method %s, without waiting for full interval", sequenceNumber, requestMethod));
				else
					LOG.debug(String.format("Thread allowed to send message w/ seq number %d, method %s, after waiting for full interval", sequenceNumber, requestMethod));
			} catch (InterruptedException e) {
				LOG.warn("Thread interrupted while waiting to send message", e);
				// TODO: do we have to do something to current thread?
			}
			latch.completed();
			synchronized (this) {
			    currentLiveKey = getFirstLiveKey();
			}
		}
	}

	private synchronized QueuedSipMessageKey getFirstLiveKey() {
		for (QueuedSipMessageKey queuedSipMessageKey: q.keySet()) {
			QueuedSipMessageLatch currentMessageLatch = q.get(queuedSipMessageKey);
			if (currentMessageLatch != null && currentMessageLatch.isAlive())
				return queuedSipMessageKey;
		}
  		return null;
	}

	protected synchronized void dequeueRequest(long sequenceNumber, String requestMethod) {
		// we've sent that sequence number...current can release its latch and be removed
		LOG.debug(String.format("Deactivating seq number %d, method %s", sequenceNumber, requestMethod));
		QueuedSipMessageKey key = new QueuedSipMessageKey(sequenceNumber, requestMethod);
		QueuedSipMessageLatch message = q.get(key);
		if (message != null) {
			message.completed();
		}
	}

	protected synchronized long getQueuedSipMessageBlockingInterval() {
		return queuedSipMessageBlockingInterval;
	}

	protected synchronized void setQueuedSipMessageBlockingInterval(long aQueuedSipMessageBlockingInterval) {
		this.queuedSipMessageBlockingInterval = aQueuedSipMessageBlockingInterval;
	}

	protected synchronized long getLastSequenceNumber() {
		return this.lastSequenceNumber;
	}

	protected synchronized long getLastSequenceNumberCreatedAt() {
		return this.lastSequenceNumberCreatedAt;
	}

	@Override
	public synchronized int hashCode() {
		int result = 1;
		result = PRIME * result + (int) (lastSequenceNumber ^ (lastSequenceNumber >>> THIRTY_TWO));
		result = PRIME * result + (int) (lastSequenceNumberCreatedAt ^ (lastSequenceNumberCreatedAt >>> THIRTY_TWO));
		return result;
	}

	//TODO: review this code
	@Override
	public synchronized boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final SipDialogMessageQueue other = (SipDialogMessageQueue) obj;
		if (getLastSequenceNumber() != other.getLastSequenceNumber())
			return false;
		if (getLastSequenceNumberCreatedAt() != other.getLastSequenceNumberCreatedAt())
			return false;
		return true;
	}

	protected synchronized SortedMap<QueuedSipMessageKey, QueuedSipMessageLatch> getQueue() {
		return q;
	}

	public synchronized void setLastSequenceNumberCreatedAt(long time) {
		this.lastSequenceNumberCreatedAt = time;
	}

	public String dumpQueue() {
		StringBuffer buff = new StringBuffer(String.format("Message Queue: size %d\n", q.size()));
		for (QueuedSipMessageKey k : q.keySet()) {
			QueuedSipMessageLatch m = q.get(k);
			buff.append(String.format("Seqnum: %d, method: %s, isAlive: %s\n", k.getSequenceNumber(), k.getMethod(), m.isAlive()));
		}
		return buff.toString();
	}
}
