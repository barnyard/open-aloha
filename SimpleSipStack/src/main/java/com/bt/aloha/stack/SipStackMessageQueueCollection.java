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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.util.Housekeeper;

/**
 * A collection of SipDialogMessageQueues 
 */
public class SipStackMessageQueueCollection implements Housekeeper {
	private static final String QUEUED_SIP_MESSAGE_RETRY_INTERVAL_IS_ZERO_BYPASSING_QUEUE = "Queued SIP message retry interval is zero - bypassing queue";
    private static final int FIFTY = 50;
	private static final int TEN_THOUSAND = 10000;
	private static final String SIP_CALL_ID_NOT_IN_SIP_STACK_MESSAGE_QUEUE = "Sip call id %s(%s/%d) not in SipStackMessageQueue";
	private static final String SIP_CALL_ID_IS_NULL = "Sip call id is null";
	private static final Log LOG = LogFactory.getLog(SipStackMessageQueueCollection.class);
	private ScheduledExecutorService scheduledExecutorService;
	private ConcurrentHashMap<String, SipDialogMessageQueue> stackMessageQueueMap = new ConcurrentHashMap<String, SipDialogMessageQueue>();
	private long housekeepTimeToLive = TEN_THOUSAND;
	private long queuedSipMessageBlockingInterval = FIFTY;

	public SipStackMessageQueueCollection() {
		super();
	}

	public void setScheduledExecutorService(ScheduledExecutorService aScheduledExecutorService) {
		this.scheduledExecutorService = aScheduledExecutorService;
	}

	protected long enqueueRequest(String sipCallId, long sipSequenceNumber, String method) {
		if (queuedSipMessageBlockingInterval == 0) {
			LOG.debug("Queued SIP message blocking interval is zero - bypassing queue");
			return sipSequenceNumber;
		}

		if (sipCallId == null)
			throw new IllegalArgumentException(SIP_CALL_ID_IS_NULL);

		SipDialogMessageQueue sipDialogMessageQueue = new SipDialogMessageQueue(queuedSipMessageBlockingInterval, scheduledExecutorService);
		SipDialogMessageQueue previousSipDialogMessageQueue = stackMessageQueueMap.putIfAbsent(sipCallId, sipDialogMessageQueue);
		if (previousSipDialogMessageQueue != null)
			sipDialogMessageQueue = previousSipDialogMessageQueue;
		return sipDialogMessageQueue.enqueueRequest(sipSequenceNumber, method);
	}

	protected void enqueueRequestForceSequenceNumber(String sipCallId, long sipSequenceNumber, String method) {
		if (queuedSipMessageBlockingInterval == 0) {
			LOG.debug(QUEUED_SIP_MESSAGE_RETRY_INTERVAL_IS_ZERO_BYPASSING_QUEUE);
			return;
		}

		if (sipCallId == null)
			throw new IllegalArgumentException(SIP_CALL_ID_IS_NULL);

		LOG.debug(String.format("Forcing sequence number %d, method %s in queue for sip call id %s", sipSequenceNumber, method, sipCallId));
		SipDialogMessageQueue sipDialogMessageQueue = new SipDialogMessageQueue(queuedSipMessageBlockingInterval, scheduledExecutorService);
		SipDialogMessageQueue previousSipDialogMessageQueue = stackMessageQueueMap.putIfAbsent(sipCallId, sipDialogMessageQueue);
		if (previousSipDialogMessageQueue != null)
			sipDialogMessageQueue = previousSipDialogMessageQueue;
		sipDialogMessageQueue.enqueueRequestForceSequenceNumber(sipSequenceNumber, method);
	}

	protected void blockUntilCanSendRequest(String sipCallId, long sipSequenceNumber, String method) {
		if (queuedSipMessageBlockingInterval == 0) {
			LOG.debug(QUEUED_SIP_MESSAGE_RETRY_INTERVAL_IS_ZERO_BYPASSING_QUEUE);
			return;
		}
		if (sipCallId == null)
			throw new IllegalArgumentException(SIP_CALL_ID_IS_NULL);

		LOG.debug(String.format("Retrieving sequence number %d, method %s from queue for sip call id %s", sipSequenceNumber, method, sipCallId));
		SipDialogMessageQueue sipDialogMessageQueue = stackMessageQueueMap.get(sipCallId);

		if (sipDialogMessageQueue == null)
			throw new StackException(String.format(SIP_CALL_ID_NOT_IN_SIP_STACK_MESSAGE_QUEUE, sipCallId, method, sipSequenceNumber));

    	LOG.debug(String.format("Going to send message %d for dialog %s", sipSequenceNumber, sipCallId));
		sipDialogMessageQueue.blockUntilCanSend(sipSequenceNumber, method);
	}

	protected void dequeueRequest(String sipCallId, long sequenceNumber, String method) {
		if (queuedSipMessageBlockingInterval == 0)
			return;
		if (sipCallId == null)
			throw new IllegalArgumentException(SIP_CALL_ID_IS_NULL);

		SipDialogMessageQueue sipDialogMessageQueue = stackMessageQueueMap.get(sipCallId);
		if (sipDialogMessageQueue == null)
			throw new StackException(String.format(SIP_CALL_ID_NOT_IN_SIP_STACK_MESSAGE_QUEUE, sipCallId, method, sequenceNumber));

		sipDialogMessageQueue.dequeueRequest(sequenceNumber, method);
	}

	public void housekeep() {
		LOG.debug("SipStackMessageQueue Housekeeping");
		for (String sipCallId : stackMessageQueueMap.keySet()) {
			SipDialogMessageQueue sipDialogMessageQueue = stackMessageQueueMap.get(sipCallId);
			if (sipDialogMessageQueue != null && System.currentTimeMillis() - sipDialogMessageQueue.getLastSequenceNumberCreatedAt() > housekeepTimeToLive) {
				LOG.debug(String.format("SipStackMessageQueue Housekeeping: removing SipDialogMessageQueue for sipCallId %s", sipCallId));
				boolean removed = stackMessageQueueMap.remove(sipCallId, sipDialogMessageQueue);
				LOG.debug(String.format("SipStackMessageQueue Housekeeping: removed = %s for sipCallId %s", removed, sipCallId));
			}
		}
	}

	public void setMaxTimeToLive(long aMaxTimeToLive) {
		housekeepTimeToLive = aMaxTimeToLive;
	}

	public long getQueuedSipMessageBlockingInterval() {
		return queuedSipMessageBlockingInterval;
	}

	public void setQueuedSipMessageBlockingInterval(long aQueuedSipMessageRetryInterval) {
		this.queuedSipMessageBlockingInterval = aQueuedSipMessageRetryInterval;
	}

	protected void setStackMessageQueueMap(ConcurrentHashMap<String, SipDialogMessageQueue> aStackMessageQueueMap) {
		this.stackMessageQueueMap = aStackMessageQueueMap;
	}

	protected ConcurrentHashMap<String, SipDialogMessageQueue> getStackMessageQueueMap() {
		return stackMessageQueueMap;
	}

	public int size() {
		return stackMessageQueueMap.size();
	}
}
