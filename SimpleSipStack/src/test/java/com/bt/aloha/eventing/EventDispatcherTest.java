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

 	

 	
 	
 
package com.bt.aloha.eventing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import com.bt.aloha.call.event.AbstractCallEvent;
import com.bt.aloha.call.event.CallConnectedEvent;
import com.bt.aloha.eventing.EventDispatcher;
import com.bt.aloha.eventing.EventFilter;
import com.bt.aloha.eventing.UndefinedEventException;

public class EventDispatcherTest {
	private int niceEventCount = 0;
	private TaskExecutor taskExecutor;
	private EventDispatcher eventDispatcher;
	
	@Before
	public void before() {
		taskExecutor = new SimpleAsyncTaskExecutor();
		eventDispatcher = new EventDispatcher();
		eventDispatcher.setTaskExecutor(taskExecutor);
		niceEventCount = 0;
	}
	
	private class NiceEvent {
		public String getNice() {
			return "very niiiiice!!!";
		}
	}
	
	private class PatternFilteringEventListener extends NiceEventListener implements EventFilter {
		public boolean shouldDeliverEvent(Object event) {
			return event instanceof NiceEvent 
				&& ((NiceEvent)event).getNice().matches("^not very.*");
		}
	}
	
	private class HappyPatternFilteringEventListener extends NiceEventListener implements EventFilter {
		public boolean shouldDeliverEvent(Object event) {
			return event instanceof NiceEvent 
				&& ((NiceEvent)event).getNice().matches("^very.*");
		}
	}
	
	private class NiceEventListener {
		public void onNice(NiceEvent niceEvent) {
			niceEventCount++;				
		}
	}
	
	// test happy event delivery
	@Test
	public void testHappyEventDelivery() { 
		// setup
		List<Object> listenerList = new ArrayList<Object>();
		listenerList.add(new NiceEventListener());

		// act
		eventDispatcher.dispatchEvent(listenerList, new NiceEvent());		
		try { 
			Thread.sleep(200);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		
		// assert
		assertEquals("Didn't get expected event", 1, niceEventCount);
	}
	
	// test two happy event deliveries (i.e. event delivered to two listeners)
	@Test
	public void testEventDeliveredToTwoListeners() { 
		// setup
		List<Object> listenerList = new ArrayList<Object>();
		listenerList.add(new NiceEventListener());
		listenerList.add(new NiceEventListener());

		// act
		eventDispatcher.dispatchEvent(listenerList,	new NiceEvent());		
		try { 
			Thread.sleep(400);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		
		// assert
		assertEquals("Didn't get expected events", 2, niceEventCount);
	}
	
	// test that event of the wrong type isn't delivered
	@Test
	public void testEventOfWrongTypeNotDelivered() { 
		// setup
		List<Object> listenerList = new ArrayList<Object>();
		listenerList.add(new NiceEventListener());

		// act
		eventDispatcher.dispatchEvent(listenerList, new Object());		
		try { 
			Thread.sleep(200);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		
		// assert
		assertEquals("Got unexpected event", 0, niceEventCount);
	}
	
	// test delivery of event without event suffix
	@Test
	public void testEventDeliveryWithoutEventSuffix() {
		class Nice extends NiceEvent {}
		// setup
		List<Object> listenerList = new ArrayList<Object>();
		listenerList.add(new NiceEventListener());

		// act
		eventDispatcher.dispatchEvent(listenerList, new Nice());		
		try { 
			Thread.sleep(200);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		
		// assert
		assertEquals("Didn't get expected event", 1, niceEventCount);
	}
	
	// test that an exception thrown whilst processing an event is absorbed
	@Test
	public void testExceptionAbsorbedOnEventDelivery() {
		class BadListener extends NiceEventListener {
			@Override public void onNice(NiceEvent niceEvent) {
				throw new RuntimeException("oops");
			}
		}
		
		// setup
		List<Object> listenerList = new ArrayList<Object>();
		listenerList.add(new BadListener());

		// act
		eventDispatcher.dispatchEvent(listenerList, new NiceEvent());
		
		// assert
		// NO exception should be thrown
	}
	
	// test that empty listener array is ignored
	@Test
	public void testEmptyListenerArray() {
		// setup
		List<Object> listenerList = new ArrayList<Object>();

		// act
		eventDispatcher.dispatchEvent(listenerList, new NiceEvent());
		
		// assert
		assertEquals(0, niceEventCount);
	}
	
	// test that null event results in an exception
	@Test(expected = UndefinedEventException.class)
	public void testNullEventObjectExceptionThrown() {
		// setup
		List<Object> listenerList = new ArrayList<Object>();

		// act
		eventDispatcher.dispatchEvent(listenerList, null);		
	}
	
	// test that filtering by pattern allows non-matching events through 
	@Test
	public void testEventPatternHappy() { 
		// setup
		List<Object> listenerList = new ArrayList<Object>();
		listenerList.add(new HappyPatternFilteringEventListener());

		// act
		eventDispatcher.dispatchEvent(listenerList, new NiceEvent());		
		try { 
			Thread.sleep(200);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		
		// assert
		assertEquals("Expected 1 event", 1, niceEventCount);
	}
	
	// test that filtering by pattern works
	@Test
	public void testEventPatternFilteringWorks() { 
		// setup
		List<Object> listenerList = new ArrayList<Object>();
		listenerList.add(new PatternFilteringEventListener());

		// act
		eventDispatcher.dispatchEvent(listenerList, new NiceEvent());		
		try { 
			Thread.sleep(200);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		
		// assert
		assertEquals("Got unexpected event", 0, niceEventCount);
	}
	
	// test synchronous event delivery - event should be delivered even when we don't have a taskexecutor
	@Test
	public void testSynchronousEventDelivery() { 
		// setup
		eventDispatcher.setTaskExecutor(null);
		
		List<Object> listenerList = new ArrayList<Object>();
		listenerList.add(new NiceEventListener());

		// act
		eventDispatcher.dispatchEvent(listenerList, new NiceEvent(), false, null);		
		
		// assert
		assertEquals("Didn't get expected event", 1, niceEventCount);
	}
	
	// test two events only one delivered if limit set 
	@Test
	public void testTwoEventsOnlyOneDeliveredIfMaxNumberOfListenersOne() { 
		// setup
		List<Object> listenerList = new ArrayList<Object>();
		listenerList.add(new NiceEventListener());
		listenerList.add(new NiceEventListener());

		// act
		eventDispatcher.dispatchEvent(listenerList,	new NiceEvent(), true, 1);		
		try { 
			Thread.sleep(200);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		
		// assert
		assertEquals("Didn't get expected events", 1, niceEventCount);
	}
	
	class MyEventFilter implements EventFilter {
		public boolean shouldDeliverEvent(Object event) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return true;
		}
	}
	
	// Test that altering the listener list whilst the event dispatcher is filtering the list does cause
	// a ConcurrentModificationException
	@Test
	public void testFilterListenersWhilstUpdatingRawList() throws Exception {
		//setup
		final List<EventFilter> rawListenerList = new ArrayList<EventFilter>();
		for (int i = 0; i < 100; i++) {
			EventFilter eventFilter = new MyEventFilter(); 
			rawListenerList.add(eventFilter);
		}
		final AbstractCallEvent event = new CallConnectedEvent("aCallId");
		final Semaphore flag = new Semaphore(0);
		
		//act
		Runnable r = new Runnable(){
			public void run() {
					eventDispatcher.filterListeners(rawListenerList, event);
					flag.release();
			}
		};
		Thread t = new Thread(r);
		t.start();
		
		for (int i = 10; i < 40; i++) {
			try {
				Thread.sleep(4);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			rawListenerList.remove(i);
		}
		
		//assert
		assertTrue(flag.tryAcquire(2, TimeUnit.SECONDS));
	}
	
	
	// Ensure that IndexOutOfBoundsException in filterListeners is dealt with correctly.
	@Test
	public void testFilterListenersIndexOutOfBoundsException() {
		//setup
		final List<EventFilter> rawListenerList = new ArrayList<EventFilter>() {
			private static final long serialVersionUID = -4010930174700704385L;

			@Override
			public EventFilter get(int index) {
				if (index == 1)
					throw new IndexOutOfBoundsException("doh");
				return super.get(index);
			}
		};
		EventFilter eventFilter = new MyEventFilter(); 
		rawListenerList.add(eventFilter);
		rawListenerList.add(eventFilter);
		AbstractCallEvent event = new CallConnectedEvent("aCallId");
		
		//act
		List<?> result = eventDispatcher.filterListeners(rawListenerList, event);
		
		//assert
		assertEquals(1, result.size());
	}
}
