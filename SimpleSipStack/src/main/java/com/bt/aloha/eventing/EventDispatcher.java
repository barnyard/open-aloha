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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;
import org.springframework.core.task.TaskExecutor;

public class EventDispatcher {
	private static final String LOCALE = "en";
	private static final String EVENT_SUFFIX = "event";
	private Log log = LogFactory.getLog(this.getClass());
	private TaskExecutor taskExecutor;

	public EventDispatcher() {
	}

	public TaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}

	public void setTaskExecutor(TaskExecutor aTaskExecutor) {
		this.taskExecutor = aTaskExecutor;
	}

	protected List<?> filterListeners(final List<?> rawListenerList, Object event) {
		if(rawListenerList == null || rawListenerList.size() == 0) {
			log.debug(String.format("NO listeners found for %s", event.getClass().getSimpleName()));
			return new ArrayList<Object>();
		}
		
		log.debug(String.format("Filtering %d listeners for event %s", rawListenerList.size(), event.getClass().getSimpleName()));
		List<Object> matchingListeners = new ArrayList<Object>();
		for (int i = 0; i < rawListenerList.size(); i++) {
			Object listener = null;
			try {
				listener = rawListenerList.get(i);
			} catch (IndexOutOfBoundsException e) {
				log.debug(String.format("%s: size changed whilst processing listener list", e.getClass().getSimpleName()));
				continue;
			}
			if(listener instanceof EventFilter) {
				EventFilter filteringListener = (EventFilter)listener;
				if(filteringListener.shouldDeliverEvent(event)) {
					matchingListeners.add(listener);
				} else {
					log.info(String.format("Listener %s does NOT want event %s", filteringListener.getClass().getSimpleName(), event.getClass().getSimpleName()));
				}
			} else {
				matchingListeners.add(listener);
			}
		}
		log.debug(String.format("Returning %d listeners found for %s", matchingListeners.size(), event.getClass().getSimpleName()));
		return matchingListeners;
	}

	public int dispatchEvent(List<?> rawListenerList, final Object event) {
		return dispatchEvent(rawListenerList, event, true, null);
	}

	public int dispatchEvent(List<?> rawListenerList, final Object event, boolean invokeAsynchronously, Integer maxNumberOfListenersToInvoke) {
		if(event == null)
			throw new UndefinedEventException("Cannot dispatch null event");

		List<?> filteredListenerArray = filterListeners(rawListenerList, event);
		final Locale locale = new Locale(LOCALE);
		String eventName = event.getClass().getSimpleName().toLowerCase(locale);
		if (eventName.toLowerCase(locale).endsWith(EVENT_SUFFIX))
			eventName = eventName.substring(0, eventName.length() - EVENT_SUFFIX.length());

		int numberOfListenersInvoked = 0;
		for (final Object listener: filteredListenerArray) {
			if(maxNumberOfListenersToInvoke != null && numberOfListenersInvoked >= maxNumberOfListenersToInvoke.intValue()) {
				log.debug(String.format("Already invoked max number (%d) of listeners for event %s", maxNumberOfListenersToInvoke.intValue(), eventName));
				break;
			}

			if(deliverEventToListener(listener, eventName, event, invokeAsynchronously)) {
				numberOfListenersInvoked++;
			} else {
				log.warn(String.format("Unable to deliver event %s to listener %s - check that the listeners have method names corresponding to event names", eventName, listener.getClass().getName()));
			}
		}
		return numberOfListenersInvoked;
	}

	private boolean deliverEventToListener(final Object listener, final String eventName, final Object event, boolean invokeAsynchronously) {
		for (final Method method : listener.getClass().getMethods()) {
			final Locale locale = new Locale(LOCALE);
			if (method.getName().toLowerCase(locale).equals("on" + eventName)) {
				if(invokeAsynchronously) {
                    Runnable runnable = new EventDispatcherRunnable(method, event, listener, (String)MDC.get("stackname"));
					taskExecutor.execute(runnable);
				} else {
					log.debug(String.format("Delivering synchronous event %s to %s", event.getClass().getSimpleName(), listener.getClass().getName()));
					try {
						method.invoke(listener, new Object[] {event});
					} catch(Throwable t) {
						log.error(String.format("Exception whilst processing event %s", event.getClass().getSimpleName()), t);
					}
				}
				return true;
			}
		}
		return false;
	}
}
