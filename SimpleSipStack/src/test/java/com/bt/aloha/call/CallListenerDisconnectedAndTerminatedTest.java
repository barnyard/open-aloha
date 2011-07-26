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

 	

 	
 	
 
package com.bt.aloha.call;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.bt.aloha.call.event.AbstractCallEvent;
import com.bt.aloha.call.event.CallDisconnectedEvent;
import com.bt.aloha.call.event.CallTerminatedEvent;
import com.bt.aloha.call.state.CallLegCausingTermination;
import com.bt.aloha.call.state.CallTerminationCause;
import com.bt.aloha.dialog.state.TerminationMethod;
import com.bt.aloha.testing.SipUnitPhone;

public class CallListenerDisconnectedAndTerminatedTest extends CallListenerTestBase {
	// Just reuse joinCallLegs from ThirdPartyCallMessageFlow to join call legs.
	@Override
	protected String joinCallLegs() throws Exception {
		String callId = callBean.joinCallLegs(firstDialogId, secondDialogId);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		waitForCallSendTryingRingingOk(SipUnitPhone.SecondInbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());
		waitForAckAssertMediaDescription(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());
		return callId;
	}
	
	@Override
	public void testOnCallConnected() throws Exception {
		// do nothing - all call flows test this anyway
	}

	// Test that the disconnected event is raised when the first dialog disconnects
	@Test
	public void testOnCallDisconnectedEventCausedByFirstDialog() throws Exception {
		// act
		String callId = joinCallLegs();

		// wait for call connected event
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));

		// bye
		Thread.sleep(1000);

		getFirstSipCall().disconnect();
		
		assertTrue("No disconnected event", disconnectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
		List<AbstractCallEvent> events = filterCallEventsForCallId(eventVector, callId);
		
		// assert
		assertEquals("Expected 2 events", 2, events.size());
		assertTrue("Didn't get expected event", events.get(1) instanceof CallDisconnectedEvent);
		assertEquals("Got different call id", callId, ((CallDisconnectedEvent)events.get(1)).getCallId());
		assertEquals("Got unexpected TerminationCause", CallTerminationCause.RemotePartyHungUp, ((CallDisconnectedEvent)events.get(1)).getCallTerminationCause());
		assertEquals("Got unexpected call leg causing termination", CallLegCausingTermination.First, ((CallDisconnectedEvent)events.get(1)).getCallLegCausingTermination());
		assertTrue("Got a zero call duration", ((CallDisconnectedEvent)events.get(1)).getDuration() > 0);
	}	

	// Test that the disconnected event is raised when the second dialog disconnects
	@Test
	public void testOnCallDisconnectedEventCausedBySecondDialog() throws Exception {
		// act
		String callId = joinCallLegs();

		// wait for call connected event
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));

		// bye
		Thread.sleep(1000);
		
		getSecondSipCall().disconnect();
		
		assertTrue("No disconnected event", disconnectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
		List<AbstractCallEvent> events = filterCallEventsForCallId(eventVector, callId);
		
		// assert
		assertEquals("Expected 2 events", 2, events.size());
		assertTrue("Didn't get expected event", events.get(1) instanceof CallDisconnectedEvent);
		assertEquals("Got different call id", callId, ((CallDisconnectedEvent)events.get(1)).getCallId());
		assertEquals("Got unexpected TerminationCause", CallTerminationCause.RemotePartyHungUp, ((CallDisconnectedEvent)events.get(1)).getCallTerminationCause());
		assertEquals("Got unexpected call leg causing termination", CallLegCausingTermination.Second, ((CallDisconnectedEvent)events.get(1)).getCallLegCausingTermination());
		assertTrue("Got a zero call duration", ((CallDisconnectedEvent)events.get(1)).getDuration() > 0);
	}

	// Test that the disconnected event is raised only once if both dialogs disconnect in parallel
	@Test
	public void testOnCallDisconnectedEventNotRaisedTwice() throws Exception {
		// act
		String callId = joinCallLegs();

		// wait for call connected event
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));

		// bye from first
		Thread.sleep(1000);

		getFirstSipCall().disconnect();
		
		// bye from second
		getSecondSipCall().disconnect();
		assertTrue("No disconnected event", disconnectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
		List<AbstractCallEvent> events = filterCallEventsForCallId(eventVector, callId);
		
		// assert
		assertEquals("Exactly 1 connected & 1 disconnected event expected", 2, events.size());
		assertTrue("Wrong event type", events.get(1) instanceof CallDisconnectedEvent);
		assertEquals("Got unexpected TerminationCause", CallTerminationCause.RemotePartyHungUp, ((CallDisconnectedEvent)events.get(1)).getCallTerminationCause());
		assertTrue("Got a zero call duration", ((CallDisconnectedEvent)events.get(1)).getDuration() > 0);
	}

	// Test that the terminated event is raised when the first dialog is terminated
	@Test
	public void testOnCallTerminatedEventCausedByTerminatingFirstDialog() throws Exception {
		// setup
		String callId = joinCallLegs();

		// wait for call connected event
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));

		assertTrue(getFirstSipCall().listenForDisconnect());

		// act
		Thread.sleep(1000);
		
		terminateCallLeg(firstDialogId);
		
		waitForByeAndRespond(getFirstSipCall());
		assertTrue("No terminated event", terminatedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
		List<AbstractCallEvent> events = filterCallEventsForCallId(eventVector, callId);
		
		// assert
		assertEquals("Expected 2 events", 2, events.size());
		assertEquals("Got different call id", callId, ((CallTerminatedEvent)events.get(1)).getCallId());
		assertEquals("Got unexpected TerminationCause", CallTerminationCause.TerminatedByApplication, ((CallTerminatedEvent)events.get(1)).getCallTerminationCause());
		assertEquals("Got unexpected call leg causing termination", CallLegCausingTermination.Neither, ((CallTerminatedEvent)events.get(1)).getCallLegCausingTermination());
		assertTrue("Got a zero call duration", ((CallTerminatedEvent)events.get(1)).getDuration() > 0);
		assertEquals("Did not clear terminate flag", TerminationMethod.None, dialogCollection.get(firstDialogId).getTerminationMethod());
	}

	// Test that the terminated event is raised only once if both dialogs are termminated in parallel
	@Test
	public void testOnCallTerminatedEventNotRaisedTwice() throws Exception {
		// setup
		String callId = joinCallLegs();

		// wait for call connected event
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));

		assertTrue(getFirstSipCall().listenForDisconnect());
		assertTrue(getSecondSipCall().listenForDisconnect());

		// act
		Thread.sleep(1000);
		outboundCallLegBean.terminateCallLeg(firstDialogId);
		outboundCallLegBean.terminateCallLeg(secondDialogId);
		waitForByeAndRespond(getFirstSipCall());
		waitForByeAndRespond(getSecondSipCall());
		assertTrue("No terminated event", terminatedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
		List<AbstractCallEvent> events = filterCallEventsForCallId(eventVector, callId);
		
		// assert
		assertEquals("Exactly 1 connected & 1 terminated event expected", 2, events.size());
		assertTrue("Wrong event type", events.get(1) instanceof CallTerminatedEvent);
		assertEquals("Got unexpected TerminationCause", CallTerminationCause.TerminatedByApplication, ((CallTerminatedEvent)events.get(1)).getCallTerminationCause());
		assertTrue("Got a zero call duration", ((CallTerminatedEvent)events.get(1)).getDuration() > 0);
	}
}
