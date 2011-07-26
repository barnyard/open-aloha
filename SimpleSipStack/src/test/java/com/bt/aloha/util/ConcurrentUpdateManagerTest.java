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

 	

 	
 	
 
package com.bt.aloha.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.collections.memory.InMemoryHousekeepingCollectionImpl;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.collections.DialogCollectionImpl;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateException;
import com.bt.aloha.util.ConcurrentUpdateManager;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;
import com.bt.aloha.util.ConflictAwareConcurrentUpdateBlock;


public class ConcurrentUpdateManagerTest {
	private final Log log = LogFactory.getLog(this.getClass());
	private ConcurrentUpdateManager concurrentUpdateManager = new ConcurrentUpdateManagerImpl();
	private final Object monitor = new Object[0];
	private DialogCollection dialogCollection;
	private DialogInfo dialogInfo;
	private String dialogId;
	private Vector<String> repeatCounter;

	@Before
	public void before() {
		concurrentUpdateManager = new ConcurrentUpdateManagerImpl();
		dialogCollection = new DialogCollectionImpl(new InMemoryHousekeepingCollectionImpl<DialogInfo>());
		dialogInfo = new DialogInfo("id", "whatever", "1.2.3.4");
		dialogId = dialogInfo.getId();
		dialogCollection.add(dialogInfo);
		repeatCounter = new Vector<String>();
	}

	// test basic concurrent update - given a dialog id, an update block should be executed
	@Test
	public void testConcurrentUpdateManagerOneUpdateSucceeds() throws Exception {
		// setup
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo myDialogInfo = dialogCollection.get(dialogId);
				myDialogInfo.setDialogState(DialogState.Terminated);
				dialogCollection.replace(myDialogInfo);
			}

			public String getResourceId() {
				return dialogId;
			}
		};

		// act
		concurrentUpdateManager.executeConcurrentUpdate(concurrentUpdateBlock);

		// assert
		assertEquals(DialogState.Terminated, dialogCollection.get(dialogId).getDialogState());
	}

	// test concurrent update retry - given a block where an update is 'overtaken' by a more recent one,
	// the original update should be executed again until it successds or until too many retries force it to fail
	@Test
	public void testConcurrentUpdateManagerUpdateOvertakenByAnother() throws Exception {
		// setup
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				repeatCounter.add("one");
				DialogInfo myDialogInfo = dialogCollection.get(dialogId);
				myDialogInfo.setDialogState(DialogState.Terminated);

				if(repeatCounter.size() == 1)
					try {
						log.debug(System.currentTimeMillis() + " Block about to wait...");
						monitor.wait(5000);
						log.debug(System.currentTimeMillis() + " Block returned from wait");
					} catch(InterruptedException e) {
						System.err.println(e);
					}

				log.debug("Block replacing dialog");
				dialogCollection.replace(myDialogInfo);
			}

			public String getResourceId() {
				return dialogId;
			}
		};

		Runnable competingWriter = new Runnable() {
			public void run() {
				log.debug(System.currentTimeMillis() + " Competing writer waiting for lock...");
				synchronized (monitor) {
					log.debug(System.currentTimeMillis() + " Competing writer got lock");
					DialogInfo myDialogInfo = dialogCollection.get(dialogId);
					myDialogInfo.setDialogState(DialogState.Confirmed);
					dialogCollection.replace(myDialogInfo);
					monitor.notify();
					log.debug(System.currentTimeMillis() + " Competing writer notified...");
				}
			}
		};

		// act
		synchronized (monitor) {
			new Thread(competingWriter).start();
			concurrentUpdateManager.executeConcurrentUpdate(concurrentUpdateBlock);
		}

		// assert
		assertEquals(DialogState.Terminated, dialogCollection.get(dialogId).getDialogState());
		assertEquals("Expected exactly two invocations of the concurrent block", 2, repeatCounter.size());
	}

	// test concurrent update failure - given a block where an update is 'overtaken' by a more recent one,
	// the original update is retried a fixed number of times and eventually fails
	@Test(expected=ConcurrentUpdateException.class)
	public void testConcurrentUpdateManagerUpdateOvertakenMaxNumberOfRetriesReached() throws Exception {
		// setup
		concurrentUpdateManager = new ConcurrentUpdateManagerImpl(0);
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				repeatCounter.add("one");
				DialogInfo myDialogInfo = dialogCollection.get(dialogId);
				myDialogInfo.setDialogState(DialogState.Terminated);

				if(repeatCounter.size() == 1)
					try {
						log.debug(System.currentTimeMillis() + " Block about to wait...");
						monitor.wait(5000);
						log.debug(System.currentTimeMillis() + " Block returned from wait");
					} catch(InterruptedException e) {
						System.err.println(e);
					}

				dialogCollection.replace(myDialogInfo);
			}

			public String getResourceId() {
				return dialogId;
			}
		};

		Runnable competingWriter = new Runnable() {
			public void run() {
				log.debug(System.currentTimeMillis() + " Competing writer waiting for lock...");
				synchronized (monitor) {
					log.debug(System.currentTimeMillis() + " Competing writer got lock");
					DialogInfo myDialogInfo = dialogCollection.get(dialogId);
					myDialogInfo.setDialogState(DialogState.Confirmed);
					dialogCollection.replace(myDialogInfo);
					monitor.notify();
					log.debug(System.currentTimeMillis() + " Competing writer notified...");
				}
			}
		};

		// act
		synchronized (monitor) {
			new Thread(competingWriter).start();
			concurrentUpdateManager.executeConcurrentUpdate(concurrentUpdateBlock);
		}
	}

	// Test that nested exceptions are handled correctly
	@Test
	public void testNestedExceptions() throws Exception {
		//setup			
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				repeatCounter.add("one");
				ConcurrentUpdateBlock innerBlock = new ConcurrentUpdateBlock() {
					public void execute() {
						throw new ConcurrentUpdateException("nestedId", "message");
					}
					public String getResourceId() {
						return "nestedId";
					}
				};
				new ConcurrentUpdateManagerImpl(1).executeConcurrentUpdate(innerBlock);
			}

			public String getResourceId() {
				return dialogId;
			}
		};

		//act
		try {
			concurrentUpdateManager.executeConcurrentUpdate(concurrentUpdateBlock);
			fail("Expected exception");
		} catch (ConcurrentUpdateException e) {
			//assert
			assertEquals(1, repeatCounter.size());
		}


	}
	
	/**
	 * Tests you must call get in the concurrent modification block else you'll get concurrentupdateexception always
	 */
	@Test(expected=ConcurrentUpdateException.class)
	public void testMustCallGetWithinConcurrentModificationBlock() throws Exception {
		// setup
		concurrentUpdateManager = new ConcurrentUpdateManagerImpl(0);
		final DialogInfo myDialogInfo = dialogCollection.get(dialogId);
		DialogInfo changedInfo = dialogCollection.get(dialogId);
		changedInfo.setDialogState(DialogState.Confirmed);
		dialogCollection.replace(changedInfo);
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				myDialogInfo.setDialogState(DialogState.Terminated);
				dialogCollection.replace(myDialogInfo);
			}

			public String getResourceId() {
				return dialogId;
			}
		};
		
		// act
		concurrentUpdateManager.executeConcurrentUpdate(concurrentUpdateBlock);
	}
	
	// test that any concurrent block implementing ConcurrentUpdateConflictAware gets
	// notified when an update fails
	@Test
	public void testConcurrentUpdateConflictAwawreGetsCalled() throws Exception {
		// setup
		final CountDownLatch firstWriterRead = new CountDownLatch(1);
		final CountDownLatch secondWriterWrote = new CountDownLatch(1);
		final AtomicInteger failuresCounter = new AtomicInteger();
		
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConflictAwareConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo di = dialogCollection.get(dialogId);
				log.debug("First writer read");
				firstWriterRead.countDown();
				log.debug("Waiting for second writer to write");
				try {
					secondWriterWrote.await();
				} catch (InterruptedException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
				dialogCollection.replace(di);
				log.debug("First writer replaced");
			}

			public String getResourceId() {
				return dialogId;
			}

			public void onConcurrentUpdateConflict() {
				failuresCounter.incrementAndGet();
			}			
		};
		
		Runnable competingWriter = new Runnable() {
			public void run() {
				log.debug("Waiting for first writer to read");
				try {
					firstWriterRead.await();
				} catch (InterruptedException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
				DialogInfo di = dialogCollection.get(dialogId);
				dialogCollection.replace(di);
				log.debug("Second writer replaced");
				secondWriterWrote.countDown();
			}			
		};
		
		// act
		new Thread(competingWriter).start();
		concurrentUpdateManager.executeConcurrentUpdate(concurrentUpdateBlock);
		
		// assert
		assertEquals(1, failuresCounter.get());
	}
	
	// test that if the ConcurrentUpdateConflictAware implementation throws an exception, 
	// that exception is absorbed by the update manager
	@Test
	public void testConcurrentUpdateConflictAwawreExceptionAbsorbed() throws Exception {
		// setup
		final CountDownLatch firstWriterRead = new CountDownLatch(1);
		final CountDownLatch secondWriterWrote = new CountDownLatch(1);
		final AtomicInteger failuresCounter = new AtomicInteger();
		
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConflictAwareConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo di = dialogCollection.get(dialogId);
				log.debug("First writer read");
				firstWriterRead.countDown();
				
				di.setApplicationData("first");
				log.debug("Waiting for second writer to write");
				try {
					secondWriterWrote.await();
				} catch (InterruptedException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
				dialogCollection.replace(di);
				log.debug("First writer replaced");
			}

			public String getResourceId() {
				return dialogId;
			}

			public void onConcurrentUpdateConflict() {
				failuresCounter.incrementAndGet();
				throw new RuntimeException("bugger off");
			}			
		};
		
		Runnable competingWriter = new Runnable() {
			public void run() {
				log.debug("Waiting for first writer to read");
				try {
					firstWriterRead.await();
				} catch (InterruptedException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
				DialogInfo di = dialogCollection.get(dialogId);
				di.setApplicationData("second");
				dialogCollection.replace(di);
				log.debug("Second writer replaced");
				secondWriterWrote.countDown();
			}			
		};
		
		// act
		new Thread(competingWriter).start();
		concurrentUpdateManager.executeConcurrentUpdate(concurrentUpdateBlock);
		
		// assert
		assertEquals(1, failuresCounter.get());
		assertEquals("first", dialogCollection.get(dialogId).getApplicationData());
	}
}
