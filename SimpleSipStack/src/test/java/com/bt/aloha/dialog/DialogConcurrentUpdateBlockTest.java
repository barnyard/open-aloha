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

 	

 	
 	
 
package com.bt.aloha.dialog;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sip.message.Request;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.collections.memory.InMemoryHousekeepingCollectionImpl;
import com.bt.aloha.dialog.DialogBeanHelper;
import com.bt.aloha.dialog.DialogConcurrentUpdateBlock;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.collections.DialogCollectionImpl;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.util.ConcurrentUpdateException;
import com.bt.aloha.util.ConcurrentUpdateManager;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;


public class DialogConcurrentUpdateBlockTest {
	private final Log log = LogFactory.getLog(this.getClass());
	private final DialogCollection dialogCollection = new DialogCollectionImpl(new InMemoryHousekeepingCollectionImpl<DialogInfo>());;
	private final ConcurrentUpdateManager concurrentUpdateManager = new ConcurrentUpdateManagerImpl();
	private final DialogBeanHelper dialogBeanHelper;
	private long lastSequenceNumber = 0;
	private ArrayList<Long> releasedSequenceNumbers = new ArrayList<Long>();
	
	public DialogConcurrentUpdateBlockTest() {
		dialogBeanHelper = new DialogBeanHelper() {
			@Override
			public long enqueueRequestGetSequenceNumber(String sipCallId, long sequenceNumber, String method) {
				if (sequenceNumber > lastSequenceNumber)
					lastSequenceNumber = sequenceNumber;
				else
					lastSequenceNumber += 1;
				
				return lastSequenceNumber;
			}
			
			@Override
			protected void dequeueRequest(String sipCallId, long sequenceNumber, String requestMethod) {
				releasedSequenceNumbers.add(sequenceNumber);
			}
			
			@Override
			public void enqueueRequestForceSequenceNumber(String sipCallId, long sequenceNumber, String requestMethod) {
				if (lastSequenceNumber < sequenceNumber)
					lastSequenceNumber = sequenceNumber;
			}
		};
	}
	
	@Before
	public void before() {
		DialogInfo dialogInfo = new DialogInfo("id", "ho", "hum");
		dialogInfo.setSequenceNumber(3);
		dialogCollection.add(dialogInfo);
	}
		
	// test use of base class helper method for setting seq num from within a concurrent dialog block
	@Test
	public void testSequenceNumberSetAndDialogInfoUpdated() {
		// setup
		DialogConcurrentUpdateBlock dialogConcurrentUpdateBlock = new DialogConcurrentUpdateBlock(dialogBeanHelper) {
			public void execute() {
				DialogInfo dialogInfo = dialogCollection.get("id");
				dialogInfo.setApplicationData("nosorog");
				assignSequenceNumber(dialogInfo, Request.INVITE);
				dialogCollection.replace(dialogInfo);
			}
			
			public String getResourceId() {
				return "id";
			}
		};
		
		// act
		concurrentUpdateManager.executeConcurrentUpdate(dialogConcurrentUpdateBlock);
		
		// assert
		assertEquals("nosorog", dialogCollection.get("id").getApplicationData());
		assertEquals(4, dialogCollection.get("id").getSequenceNumber());
		assertEquals(4, lastSequenceNumber);
	}
	
	private class CompetingWriter implements Runnable {
		CountDownLatch latchToWait;
		CountDownLatch latchToOpen;
		
		public CompetingWriter(CountDownLatch latchToWait, CountDownLatch latchToOpen) {
			this.latchToWait = latchToWait;
			this.latchToOpen = latchToOpen;
		}
		
		public void run() {
			log.debug("Waiting for first writer to read");
			try {
				latchToWait.await();
			} catch (InterruptedException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
			DialogInfo di = dialogCollection.get("id");
			dialogCollection.replace(di);
			log.debug("Writer " + Thread.currentThread().getName() + " replaced");
			latchToOpen.countDown();
		}			
	};
	
	// test that, on failed updates, the reserved sequence number gets released, and update proceeds as normal
	@Test
	public void testSequenceNumberReleasedOnFailedUpdate() {
		// setup
		final CountDownLatch firstWriterRead = new CountDownLatch(1);
		final CountDownLatch secondWriterWrote = new CountDownLatch(1);
		
		DialogConcurrentUpdateBlock dialogConcurrentUpdateBlock = new DialogConcurrentUpdateBlock(dialogBeanHelper) {
			public void execute() {
				DialogInfo dialogInfo = dialogCollection.get("id");
				firstWriterRead.countDown();
				assignSequenceNumber(dialogInfo, Request.INVITE);
				log.debug("Waiting for second writer to write");
				try {
					secondWriterWrote.await();
				} catch (InterruptedException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
				dialogCollection.replace(dialogInfo);
			}
			
			public String getResourceId() {
				return "id";
			}
		};
		
		// act
		new Thread(new CompetingWriter(firstWriterRead, secondWriterWrote)).start();
		concurrentUpdateManager.executeConcurrentUpdate(dialogConcurrentUpdateBlock);
		
		// assert
		assertEquals(5, dialogCollection.get("id").getSequenceNumber());
		assertEquals(1, releasedSequenceNumbers.size());
		assertEquals(4, releasedSequenceNumbers.get(0));
		assertEquals(5, lastSequenceNumber);
	}
	
	// test use of base class helper method for forcing a seq num into the queue from within a concurrent dialog block
	@Test
	public void testSequenceNumberForcedDialogInfoUpdated() {
		// setup
		DialogConcurrentUpdateBlock dialogConcurrentUpdateBlock = new DialogConcurrentUpdateBlock(dialogBeanHelper) {
			public void execute() {
				DialogInfo dialogInfo = dialogCollection.get("id");
				dialogInfo.setApplicationData("nosorog");
				forceSequenceNumber(dialogInfo.getId(), 4L, Request.INVITE);
				dialogCollection.replace(dialogInfo);
			}
			
			public String getResourceId() {
				return "id";
			}
		};
		
		// act
		concurrentUpdateManager.executeConcurrentUpdate(dialogConcurrentUpdateBlock);
		
		// assert
		assertEquals("nosorog", dialogCollection.get("id").getApplicationData());
		assertEquals(3, dialogCollection.get("id").getSequenceNumber());
		assertEquals(4, lastSequenceNumber);
	}
	
	// test that, on failed updates, the reserved sequence number gets released, and update proceeds as normal
	@Test
	public void testSequenceNumberReleasedOnFailedForcedUpdate() {
		// setup
		final CountDownLatch firstWriterRead = new CountDownLatch(1);
		final CountDownLatch secondWriterWrote = new CountDownLatch(1);
		
		DialogConcurrentUpdateBlock dialogConcurrentUpdateBlock = new DialogConcurrentUpdateBlock(dialogBeanHelper) {
			public void execute() {
				DialogInfo dialogInfo = dialogCollection.get("id");
				firstWriterRead.countDown();
				forceSequenceNumber(dialogInfo.getId(), 4L, Request.INVITE);
				log.debug("Waiting for second writer to write");
				try {
					secondWriterWrote.await();
				} catch (InterruptedException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
				dialogCollection.replace(dialogInfo);
			}
			
			public String getResourceId() {
				return "id";
			}
		};
		
		// act
		new Thread(new CompetingWriter(firstWriterRead, secondWriterWrote)).start();
		concurrentUpdateManager.executeConcurrentUpdate(dialogConcurrentUpdateBlock);
		
		// assert
		assertEquals(3, dialogCollection.get("id").getSequenceNumber());
		assertEquals(1, releasedSequenceNumbers.size());
		assertEquals(4, releasedSequenceNumbers.get(0));
		assertEquals(4, lastSequenceNumber);
	}
	
	// test that, on failed updates, the reserved sequence number gets released, and update proceeds as normal
	@Test
	public void testSequenceNumberReleasedOnMultipleFailedUpdates() throws InterruptedException {
		// setup
		final DialogCollection dodgyCollection = new DialogCollectionImpl(new InMemoryHousekeepingCollectionImpl<DialogInfo>()) {
			AtomicInteger count = new AtomicInteger(0);
			@Override
			public void replace(DialogInfo dialogInfo) {
				int currentIteration = count.incrementAndGet();
				log.debug("iteration " + currentIteration);
				if (currentIteration < 4)
					throw new ConcurrentUpdateException("id", "ho");
				else
					super.replace(dialogInfo);
			}
		};
		dodgyCollection.add(dialogCollection.get("id"));
		
		DialogConcurrentUpdateBlock dialogConcurrentUpdateBlock = new DialogConcurrentUpdateBlock(dialogBeanHelper) {
			public void execute() {
				DialogInfo dialogInfo = dialogCollection.get("id");
				assignSequenceNumber(dialogInfo, Request.INVITE);
				dodgyCollection.replace(dialogInfo);
			}
			
			public String getResourceId() {
				return "id";
			}
		};
		
		// act
		concurrentUpdateManager.executeConcurrentUpdate(dialogConcurrentUpdateBlock);
		
		// assert
		assertEquals(7, dodgyCollection.get("id").getSequenceNumber());
		assertEquals(3, releasedSequenceNumbers.size());
		assertEquals(4, releasedSequenceNumbers.get(0));
		assertEquals(5, releasedSequenceNumbers.get(1));
		assertEquals(6, releasedSequenceNumbers.get(2));
		assertEquals(7, lastSequenceNumber);
	}

}
