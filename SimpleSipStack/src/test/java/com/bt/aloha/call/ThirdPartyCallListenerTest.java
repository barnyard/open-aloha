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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.bt.aloha.call.event.AbstractCallEvent;
import com.bt.aloha.call.event.CallConnectedEvent;
import com.bt.aloha.call.event.CallConnectionFailedEvent;
import com.bt.aloha.call.state.CallLegCausingTermination;
import com.bt.aloha.call.state.CallTerminationCause;
import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.testing.SipUnitPhone;

public class ThirdPartyCallListenerTest extends CallListenerTestBase {
	@Override
	protected String joinCallLegs() throws Exception {
		String callId = callBean.joinCallLegs(firstDialogId, secondDialogId);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		waitForCallSendTryingRingingOk(SipUnitPhone.SecondInbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());
		waitForAckAssertMediaDescription(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());
		return callId;
	}
	
//	 test that the connected event is not raised after successful negotiation of initial invites
	@Test
	public void testCallNotConnectedAfterInitialInvites() throws Exception {
		// act
		callBean.joinCallLegs(firstDialogId, secondDialogId);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		waitForCallSendTryingRingingOk(SipUnitPhone.SecondInbound);

		// assert
		assertFalse("Unexpected event", connectedSemaphore.tryAcquire(300, TimeUnit.MILLISECONDS));
	}
	
	// test that the connected event is not raised after successful negotiation of only 1 reinvite
	@Test
	public void testCallNotConnectedAfterOneReInvite() throws Exception {
		// act
		callBean.joinCallLegs(firstDialogId, secondDialogId);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		waitForCallSendTryingRingingOk(SipUnitPhone.SecondInbound);
		assertNotNull(getInboundCall().waitForReinvite(5000));		

		assertFalse("Unexpected event", connectedSemaphore.tryAcquire(300, TimeUnit.MILLISECONDS));
	}
	
	//Test that the connected event is raised for a connected call when maxDisconnected not set
	@Test
	public void testOnCallConnectedWithDelay() throws Exception {
		// act
		String callId = callBean.joinCallLegs(firstDialogId, secondDialogId, AutoTerminateAction.False, 1);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		waitForCallSendTryingRingingOk(SipUnitPhone.SecondInbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());
		waitForAckAssertMediaDescription(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());
		
		// assert
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
		List<AbstractCallEvent> events = filterCallEventsForCallId(eventVector, callId);
		assertEquals("Expected 1 event", 1, events.size());
		assertEquals("Got different call id", callId, ((CallConnectedEvent)events.get(0)).getCallId());
	}

	// Test that the connection failed event is raised when the first dialog fails to connect
	@Test
	public void testOnCallConnectionFailedEventCausedByFirstDialog() throws Exception {
		// act
		String callId = callBean.joinCallLegs(firstDialogId, secondDialogId);
		waitForCallSendTryingBusyHere(getInboundCall());

		// assert
		assertTrue("No connection failed event", connectionFailedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
		List<AbstractCallEvent> events = filterCallEventsForCallId(eventVector, callId);
		assertEquals("Expected 1 event", 1, events.size());
		assertEquals("Got different call id", callId, ((CallConnectionFailedEvent)events.get(0)).getCallId());
		assertEquals("Got unexpected TerminationCause", CallTerminationCause.RemotePartyBusy, ((CallConnectionFailedEvent)events.get(0)).getCallTerminationCause());
		assertEquals("Got unexpected call leg causing termination", CallLegCausingTermination.First, ((CallConnectionFailedEvent)events.get(0)).getCallLegCausingTermination());
		assertEquals("Got a non-zero call duration", 0, ((CallConnectionFailedEvent)events.get(0)).getDuration());
	}

	// Test that the connection failed event is raised when the second dialog fails to connect
	@Test
	public void testOnCallConnectionFailedEventCausedBySecondDialog() throws Exception {
		// act
		String callId = callBean.joinCallLegs(firstDialogId, secondDialogId);

		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		waitForCallSendTryingBusyHere(getSecondInboundCall());

		// assert
		assertTrue("No connection failed event", connectionFailedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
		List<AbstractCallEvent> events = filterCallEventsForCallId(eventVector, callId);
		assertEquals("Expected 1 event", 1, events.size());
		assertEquals("Got different call id", callId, ((CallConnectionFailedEvent)events.get(0)).getCallId());
		assertEquals("Got unexpected TerminationCause", CallTerminationCause.RemotePartyBusy, ((CallConnectionFailedEvent)events.get(0)).getCallTerminationCause());
		assertEquals("Got unexpected call leg causing termination", CallLegCausingTermination.Second, ((CallConnectionFailedEvent)events.get(0)).getCallLegCausingTermination());
		assertEquals("Got a non-zero call duration", 0, ((CallConnectionFailedEvent)events.get(0)).getDuration());
	}
}
