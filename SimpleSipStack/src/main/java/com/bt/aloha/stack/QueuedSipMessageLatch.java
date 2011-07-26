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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class QueuedSipMessageLatch extends CountDownLatch {
	private static final Log LOG = LogFactory.getLog(QueuedSipMessageLatch.class);
	private final Long sequenceNumber;
	private final String requestMethod;
	private ScheduledFuture<?> future;

	public QueuedSipMessageLatch(long aSequenceNumber, String aRequestMethod, long queuedSipMessageBlockingInterval, ScheduledExecutorService scheduledExecutorService) {
		super(1);
		sequenceNumber = aSequenceNumber;
		requestMethod = aRequestMethod;
	
		future = scheduledExecutorService.schedule(new Runnable() {
			public void run() {
				LOG.debug(String.format("Expiring queued message %d, method %s", sequenceNumber, requestMethod));
				countDown();
			}
		}, queuedSipMessageBlockingInterval, TimeUnit.MILLISECONDS);
	}

	public void completed() {
		countDown();
		future.cancel(true);
	}

	public boolean isAlive() {
        return getCount() > 0;
    }
}
