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
package com.bt.aloha.callleg;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.callleg.event.AbstractCallLegEvent;
import com.bt.aloha.callleg.event.CallLegConnectedEvent;
import com.bt.aloha.callleg.event.CallLegConnectionFailedEvent;
import com.bt.aloha.callleg.event.CallLegDisconnectedEvent;
import com.bt.aloha.callleg.event.CallLegRefreshCompletedEvent;
import com.bt.aloha.callleg.event.CallLegTerminatedEvent;
import com.bt.aloha.callleg.event.CallLegTerminationFailedEvent;
import com.bt.aloha.callleg.event.ReceivedCallLegRefreshEvent;
import com.bt.aloha.dialog.DialogSipListener;
import com.bt.aloha.dialog.event.AbstractDialogEvent;
import com.bt.aloha.dialog.event.DialogConnectedEvent;
import com.bt.aloha.dialog.event.DialogConnectionFailedEvent;
import com.bt.aloha.dialog.event.DialogDisconnectedEvent;
import com.bt.aloha.dialog.event.DialogRefreshCompletedEvent;
import com.bt.aloha.dialog.event.DialogTerminatedEvent;
import com.bt.aloha.dialog.event.DialogTerminationFailedEvent;
import com.bt.aloha.dialog.event.ReceivedDialogRefreshEvent;
import com.bt.aloha.eventing.EventFilter;

public class CallLegListenerAdapter implements DialogSipListener, EventFilter {
	private static final String DELIVERING_S_EVENT_TO_S = "Delivering %s event to %s...";
    private static final String DELIVERED_S_EVENT_TO_S = "...delivered %s event to %s";
    private static final Log LOG = LogFactory.getLog(CallLegListenerAdapter.class);
	protected CallLegListener listener;

	protected CallLegListenerAdapter(CallLegListener aListener) {
		if (aListener == null)
			throw new IllegalArgumentException("Cannot use a null listener");
		this.listener = aListener;
	}

	public void onDialogConnected(DialogConnectedEvent connectedEvent) {
		LOG.debug(String.format(DELIVERING_S_EVENT_TO_S, CallLegConnectedEvent.class.getSimpleName(), this.listener.getClass().toString()));
		this.listener.onCallLegConnected(new CallLegConnectedEvent(connectedEvent));
        LOG.debug(String.format(DELIVERED_S_EVENT_TO_S, CallLegConnectedEvent.class.getSimpleName(), this.listener.getClass().toString()));
	}

	public void onDialogConnectionFailed(DialogConnectionFailedEvent connectionFailedEvent) {
		LOG.debug(String.format(DELIVERING_S_EVENT_TO_S, CallLegConnectionFailedEvent.class.getSimpleName(), this.listener.getClass().toString()));
		this.listener.onCallLegConnectionFailed(new CallLegConnectionFailedEvent(connectionFailedEvent));
        LOG.debug(String.format(DELIVERED_S_EVENT_TO_S, CallLegConnectionFailedEvent.class.getSimpleName(), this.listener.getClass().toString()));
	}

	public void onDialogDisconnected(DialogDisconnectedEvent disconnectedEvent) {
		LOG.debug(String.format(DELIVERING_S_EVENT_TO_S, CallLegDisconnectedEvent.class.getSimpleName(), this.listener.getClass().toString()));
		this.listener.onCallLegDisconnected(new CallLegDisconnectedEvent(disconnectedEvent));
        LOG.debug(String.format(DELIVERED_S_EVENT_TO_S, CallLegDisconnectedEvent.class.getSimpleName(), this.listener.getClass().toString()));
	}

	public void onDialogRefreshCompleted(DialogRefreshCompletedEvent callLegConnectedEvent) {
		LOG.debug(String.format(DELIVERING_S_EVENT_TO_S, CallLegRefreshCompletedEvent.class.getSimpleName(), this.listener.getClass().toString()));
		this.listener.onCallLegRefreshCompleted(new CallLegRefreshCompletedEvent(callLegConnectedEvent));
        LOG.debug(String.format(DELIVERED_S_EVENT_TO_S, CallLegRefreshCompletedEvent.class.getSimpleName(), this.listener.getClass().toString()));
	}

	public void onDialogTerminated(DialogTerminatedEvent terminatedEvent) {
		LOG.debug(String.format(DELIVERING_S_EVENT_TO_S, CallLegTerminatedEvent.class.getSimpleName(), this.listener.getClass().toString()));
		this.listener.onCallLegTerminated(new CallLegTerminatedEvent(terminatedEvent));
        LOG.debug(String.format(DELIVERED_S_EVENT_TO_S, CallLegTerminatedEvent.class.getSimpleName(), this.listener.getClass().toString()));
	}

	public void onDialogTerminationFailed(DialogTerminationFailedEvent terminationFailedEvent) {
		LOG.debug(String.format(DELIVERING_S_EVENT_TO_S, CallLegTerminationFailedEvent.class.getSimpleName(), this.listener.getClass().toString()));
		this.listener.onCallLegTerminationFailed(new CallLegTerminationFailedEvent(terminationFailedEvent));
        LOG.debug(String.format(DELIVERED_S_EVENT_TO_S, CallLegTerminationFailedEvent.class.getSimpleName(), this.listener.getClass().toString()));
	}

	public void onReceivedDialogRefresh(ReceivedDialogRefreshEvent receivedDialogRefreshEvent) {
		LOG.debug(String.format(DELIVERING_S_EVENT_TO_S, ReceivedCallLegRefreshEvent.class.getSimpleName(), this.listener.getClass().toString()));
		this.listener.onReceivedCallLegRefresh(new ReceivedCallLegRefreshEvent(receivedDialogRefreshEvent));
        LOG.debug(String.format(DELIVERED_S_EVENT_TO_S, ReceivedCallLegRefreshEvent.class.getSimpleName(), this.listener.getClass().toString()));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CallLegListenerAdapter)
			return this.listener.equals(((CallLegListenerAdapter)obj).listener);
		else
			return this.listener.equals(obj);
	}

	@Override
	public int hashCode() {
		return this.listener.hashCode();
	}

	public boolean shouldDeliverEvent(Object event) {
		LOG.debug(String.format("%s.shouldDeliverEvent(%s)?", this.listener.getClass().toString(), event.getClass().toString()));
		if (this.listener instanceof EventFilter) {
			EventFilter eventFilter = (EventFilter) this.listener;
			if (event instanceof AbstractDialogEvent) {
				return eventFilter.shouldDeliverEvent(mapDialogEventToCallLegEvent((AbstractDialogEvent)event));
			}
		}
		return true;
	}

	protected AbstractCallLegEvent mapDialogEventToCallLegEvent(AbstractDialogEvent event) {
		if (event instanceof DialogConnectedEvent)
			return new CallLegConnectedEvent((DialogConnectedEvent)event);
		if (event instanceof DialogConnectionFailedEvent)
			return new CallLegConnectionFailedEvent((DialogConnectionFailedEvent)event);
		if (event instanceof DialogDisconnectedEvent)
			return new CallLegDisconnectedEvent((DialogDisconnectedEvent)event);
		if (event instanceof DialogRefreshCompletedEvent)
			return new CallLegRefreshCompletedEvent((DialogRefreshCompletedEvent)event);
		if (event instanceof DialogTerminatedEvent)
			return new CallLegTerminatedEvent((DialogTerminatedEvent)event);
		if (event instanceof DialogTerminationFailedEvent)
			return new CallLegTerminationFailedEvent((DialogTerminationFailedEvent)event);
		if (event instanceof ReceivedDialogRefreshEvent)
			return new ReceivedCallLegRefreshEvent((ReceivedDialogRefreshEvent)event);
		throw new IllegalArgumentException(String.format("The dialog event %s cannot be converted to a call leg event!", event.getClass().toString()));
	}
}
