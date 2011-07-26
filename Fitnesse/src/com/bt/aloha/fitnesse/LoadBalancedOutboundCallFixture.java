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
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

import com.bt.aloha.call.CallBean;
import com.bt.aloha.call.CallBeanImpl;
import com.bt.aloha.call.CallListener;
import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.callleg.OutboundCallLegBean;
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
import com.bt.aloha.collections.CallCollectionHashtableImpl;
import com.bt.aloha.collections.DialogCollectionHashtableImpl;
import com.bt.aloha.dialog.DialogJainSipListener;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.fitnesse.siploadbalancer.RoundRobinHostManager;
import com.bt.aloha.stack.SimpleSipStack;

public class LoadBalancedOutboundCallFixture extends OutboundCallFixture implements OutboundCallLegListener {
	private Log log = LogFactory.getLog(this.getClass());

	private ApplicationContext applicationContext = FixtureApplicationContexts.getInstance().startApplicationContext();
	private ApplicationContext secondApplicationContext = FixtureApplicationContexts.getInstance().startSecondApplicationContext();
	private ApplicationContext sipLoadBalancerApplicationContext = FixtureApplicationContexts.getInstance().startSipLoadBalancerApplicationContext();

    protected OutboundCallLegBean secondOutboundCallLegBean;
	protected CallBean secondCallBean;
	protected DialogJainSipListener secondDialogSipListener;
	protected CallCollection secondCallCollection = new CallCollectionHashtableImpl();
	protected DialogCollection secondDialogCollection = new DialogCollectionHashtableImpl();
	
	private Semaphore callAlertingSemaphore = new Semaphore(0);
	protected Vector<String> callAlertingEvents = new Vector<String>();


	public LoadBalancedOutboundCallFixture() {
		super();
		secondCallBean = (CallBean)secondApplicationContext.getBean("callBean");
		secondOutboundCallLegBean = (OutboundCallLegBean)secondApplicationContext.getBean("outboundCallLegBean");
		secondDialogSipListener = (DialogJainSipListener)secondApplicationContext.getBean("dialogSipListener");
		List<CallListener> callListeners = new ArrayList<CallListener>();
		callListeners.add(this);
		((CallBeanImpl)secondCallBean).setCallListeners(callListeners);

		((CallBeanImpl)callBean).setCallCollection(secondCallCollection);
		((CallBeanImpl)callBean).setDialogCollection(secondDialogCollection);
		((OutboundCallLegBeanImpl)outboundCallLegBean).setDialogCollection(secondDialogCollection);

		((CallBeanImpl)secondCallBean).setCallCollection(secondCallCollection);
		((CallBeanImpl)secondCallBean).setDialogCollection(secondDialogCollection);
		((OutboundCallLegBeanImpl)secondOutboundCallLegBean).setDialogCollection(secondDialogCollection);

		dialogSipListener.setDialogCollection(secondDialogCollection);
		secondDialogSipListener.setDialogCollection(secondDialogCollection);
		
		if (!((OutboundCallLegBeanImpl)outboundCallLegBean).getDialogListeners().contains(this))
			((OutboundCallLegBeanImpl)outboundCallLegBean).addOutboundCallLegListener(this);
	}
	
	@Override
	protected CallCollection getCallCollection() {
		return secondCallCollection;
	}

	@Override
	protected DialogCollection getDialogCollection() {
		return secondDialogCollection;
	}

	public void setLoadBalancerContactAddress() {
		SimpleSipStack first  = (SimpleSipStack)applicationContext.getBean("simpleSipStack");
		SimpleSipStack second = (SimpleSipStack)secondApplicationContext.getBean("simpleSipStack");
		SimpleSipStack loadBalancerStack = (SimpleSipStack)sipLoadBalancerApplicationContext.getBean("simpleSipStack");

		String contactAddress = loadBalancerStack.getIpAddress() + ":" + loadBalancerStack.getPort();

		first.setContactAddress(contactAddress);
		second.setContactAddress(contactAddress);
	}

	public void unsetLoadBalancerContactAddress() {
		SimpleSipStack first  = (SimpleSipStack)applicationContext.getBean("simpleSipStack");
		SimpleSipStack second = (SimpleSipStack)secondApplicationContext.getBean("simpleSipStack");

		first.setContactAddress(null);
		second.setContactAddress(null);
	}

	public void currentSipLoadBalancerHostIndex(int hostIndex) {
		RoundRobinHostManager.getInstance().setCurrentHostIndex(hostIndex);
	}

	public void terminateCallSecondContext() {
		secondCallBean.terminateCall(callIds.get(activeCall-1));
	}

	public String waitForCallLegAlertingEvent() throws Exception {
		String targetId = getActiveCallId();
		if (callAlertingSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS)) {
			if (callAlertingEvents.contains(targetId)) {
				return "OK";
			} else {
				return callAlertingEvents.toString();
			}
		}
		return "No event";
	}

	public void onCallLegAlerting(CallLegAlertingEvent alertingEvent) {
		if (latch != null) {
			try {
				latch.await();
			} catch (InterruptedException e) {
				log.warn(e);
			}
			log.info(String.format("onCallLegAlerting(%s)", alertingEvent.getId()));
			String callId = getCallCollection().getCurrentCallForCallLeg(alertingEvent.getId()).getId();
			if (this.callIds.contains(callId)) {
				callAlertingEvents.add(callId);
				callAlertingSemaphore.release();
			}
		}
	}

	public void onCallLegConnected(CallLegConnectedEvent connectedEvent) {
	}

	public void onCallLegConnectionFailed(CallLegConnectionFailedEvent connectionFailedEvent) {
	}

	public void onCallLegDisconnected(CallLegDisconnectedEvent disconnectedEvent) {
	}

	public void onCallLegRefreshCompleted(CallLegRefreshCompletedEvent callLegConnectedEvent) {
	}

	public void onCallLegTerminated(CallLegTerminatedEvent terminatedEvent) {
	}

	public void onCallLegTerminationFailed(CallLegTerminationFailedEvent terminationFailedEvent) {
	}

	public void onReceivedCallLegRefresh(ReceivedCallLegRefreshEvent receivedCallLegRefreshEvent) {
	}
}
