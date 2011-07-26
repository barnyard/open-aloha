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

 	

 	
 	
 
package com.bt.aloha.fitnesse;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationContext;

import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.callleg.OutboundCallLegBeanImpl;
import com.bt.aloha.callleg.OutboundCallLegListener;
import com.bt.aloha.callleg.event.CallLegAlertingEvent;
import com.bt.aloha.callleg.event.CallLegConnectedEvent;
import com.bt.aloha.callleg.event.CallLegConnectionFailedEvent;
import com.bt.aloha.callleg.event.CallLegDisconnectedEvent;
import com.bt.aloha.callleg.event.CallLegRefreshCompletedEvent;
import com.bt.aloha.callleg.event.CallLegTerminatedEvent;
import com.bt.aloha.callleg.event.CallLegTerminationFailedEvent;
import com.bt.aloha.callleg.event.ReceivedCallLegRefreshEvent;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.stack.SessionDescriptionHelper;

public class OutboundDialogFixture extends SimpleSipStackBaseFixture implements
		OutboundCallLegListener {
	private List<String> alertingEvents = new Vector<String>();
	protected List<String> connectedEvents = new Vector<String>();
	private List<String> connectionFailedEvents = new Vector<String>();
	private List<String> terminatedEvents = new Vector<String>();
	private List<String> disconnectedEvents = new Vector<String>();
	private List<String> terminationFailedEvents = new Vector<String>();
	private List<String> callLegConnectedEvents = new Vector<String>();

	private Semaphore connectedSemaphore = new Semaphore(0);
	private Semaphore alertingSemaphore = new Semaphore(0);
	private Semaphore connectionFailedSemaphore = new Semaphore(0);
	private Semaphore terminatedSemaphore = new Semaphore(0);
	private Semaphore disconnectedSemaphore = new Semaphore(0);
	private Semaphore terminationFailedSemaphore = new Semaphore(0);
	private Semaphore callLegConnectedSemaphore = new Semaphore(0);
	
	private int lastDialogCollectionSize;

	public OutboundDialogFixture(ApplicationContext applicationContext) {
		super(applicationContext);
		List<OutboundCallLegListener> listeners = new ArrayList<OutboundCallLegListener>();
		listeners.add(callBean);
		listeners.add(this);
		((OutboundCallLegBeanImpl) outboundCallLegBean)
				.setOutboundCallLegListeners(listeners);
	}

	public OutboundDialogFixture() {
		this(FixtureApplicationContexts.getInstance().startApplicationContext());
	}

	public void ipAddressPattern(String ipAddressPattern) {
		this.ipAddressPattern = ipAddressPattern;
	}

	public String connectFirstDialog() {
		outboundCallLegBean.connectCallLeg(firstDialogId);
		return "OK";
	}

	public String connectSecondDialog() {
		outboundCallLegBean.connectCallLeg(secondDialogId);
		return "OK";
	}

	public String reinviteFirstDialog() {
		outboundCallLegBean.reinviteCallLeg(firstDialogId,
				SessionDescriptionHelper.generateHoldMediaDescription(),
				AutoTerminateAction.False, null);
		return "OK";
	}

	public void terminateFirstDialog() {
		outboundCallLegBean.terminateCallLeg(firstDialogId);
	}

	public String firstDialogStatus() {
		ReadOnlyDialogInfo dialogInfo = getDialogCollection()
				.get(firstDialogId);
		return dialogInfo.getDialogState().toString();
	}

	public String firstDialogTerminationCause() {
		ReadOnlyDialogInfo dialogInfo = getDialogCollection()
				.get(firstDialogId);
		return dialogInfo.getTerminationCause().toString();
	}

	public String secondDialogStatus() {
		ReadOnlyDialogInfo dialogInfo = getDialogCollection().get(
				secondDialogId);
		return dialogInfo.getDialogState().toString();
	}

	public void dialogMaxTTL(int val) {
		getDialogCollection().setMaxTimeToLive(val);
	}

	public void housekeepDialogCollection() {
		getDialogCollection().housekeep();
	}

	public void storeDialogCollectionSize() {
		lastDialogCollectionSize = getDialogCollection().size();
	}

	public int dialogCollectionSize() {
		return getDialogCollection().size();
	}

	public int dialogCollectionSizeDelta() {
		return getDialogCollection().size() - lastDialogCollectionSize;
	}

	public String cleanDialogCollection() {
		ConcurrentMap<String, DialogInfo> dialogs = getDialogCollection()
				.getAll();
		for (String dialogId : dialogs.keySet()) {
			getDialogCollection().remove(dialogId);
		}
		if (getDialogCollection().size() == 0)
			return "OK";
		else
			return "Failed";
	}

	// ///////////////////////////////// Events/WaitFor

	public String waitForAlertingEvent() throws Exception {
		if (alertingSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS))
			return alertingEvents.contains(firstDialogId) ? "OK"
					: alertingEvents.toString();
		return "No event";
	}

	public String waitForConnectedEvent() throws Exception {
		if (connectedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS))
			return connectedEvents.contains(firstDialogId) ? "OK"
					: connectedEvents.toString();
		return "No event";
	}

	public String waitForConnectionFailedEvent() throws Exception {
		if (connectionFailedSemaphore.tryAcquire(waitTimeoutSeconds,
				TimeUnit.SECONDS))
			return connectionFailedEvents.contains(firstDialogId) ? "OK"
					: connectionFailedEvents.toString();
		return "No event";
	}

	public String waitForTerminatedEvent() throws Exception {
		if (terminatedSemaphore
				.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS))
			return terminatedEvents.contains(firstDialogId) ? "OK"
					: terminatedEvents.toString();
		return "No event";
	}

	public String waitForTerminationFailedEvent() throws Exception {
		if (terminationFailedSemaphore.tryAcquire(waitTimeoutSeconds,
				TimeUnit.SECONDS))
			return terminationFailedEvents.contains(firstDialogId) ? "OK"
					: terminationFailedEvents.toString();
		return "No event";
	}

	public String waitForDisconnectedEvent() throws Exception {
		if (disconnectedSemaphore.tryAcquire(waitTimeoutSeconds,
				TimeUnit.SECONDS))
			return disconnectedEvents.contains(firstDialogId) ? "OK"
					: disconnectedEvents.toString();
		return "No event";
	}

	public String waitForCallLegConnectedEvent() throws Exception {
		if (callLegConnectedSemaphore.tryAcquire(waitTimeoutSeconds,
				TimeUnit.SECONDS))
			return callLegConnectedEvents.contains(firstDialogId) ? "OK"
					: callLegConnectedEvents.toString();
		return "No event";
	}

	public void onCallLegAlerting(CallLegAlertingEvent alertingEvent) {
		String dialogId = alertingEvent.getId();
		if (dialogId.equals(firstDialogId)) {
			this.alertingEvents.add(alertingEvent.getId());
			this.alertingSemaphore.release();
		}
	}

	public void onCallLegConnected(CallLegConnectedEvent connectedEvent) {
		String dialogId = connectedEvent.getId();
		if (dialogId.equals(firstDialogId)) {
			this.connectedEvents.add(connectedEvent.getId());
			connectedSemaphore.release();
		}
	}

	public void onCallLegConnectionFailed(
			CallLegConnectionFailedEvent connectionFailedEvent) {
		String dialogId = connectionFailedEvent.getId();
		if (dialogId.equals(firstDialogId)) {
			this.connectionFailedEvents.add(connectionFailedEvent.getId());
			connectionFailedSemaphore.release();
		}
	}

	public void onCallLegTerminated(CallLegTerminatedEvent terminatedEvent) {
		String dialogId = terminatedEvent.getId();
		if (dialogId.equals(firstDialogId)) {
			this.terminatedEvents.add(terminatedEvent.getId());
			terminatedSemaphore.release();
		}
	}

	public void onCallLegDisconnected(CallLegDisconnectedEvent disconnectedEvent) {
		String dialogId = disconnectedEvent.getId();
		if (dialogId.equals(firstDialogId)) {
			this.disconnectedEvents.add(disconnectedEvent.getId());
			disconnectedSemaphore.release();
		}
	}

	public void onCallLegTerminationFailed(
			CallLegTerminationFailedEvent terminationFailedEvent) {
		String dialogId = terminationFailedEvent.getId();
		if (dialogId.equals(firstDialogId)) {
			this.terminationFailedEvents.add(terminationFailedEvent.getId());
			terminationFailedSemaphore.release();
		}
	}

	public void onCallLegRefreshCompleted(
			CallLegRefreshCompletedEvent callLegConnectedEvent) {
		String dialogId = callLegConnectedEvent.getId();
		if (dialogId.equals(firstDialogId)) {
			this.callLegConnectedEvents.add(callLegConnectedEvent.getId());
			callLegConnectedSemaphore.release();
		}
	}

	public void onReceivedCallLegRefresh(
			ReceivedCallLegRefreshEvent receivedCallLegRefreshEvent) {
	}
}
