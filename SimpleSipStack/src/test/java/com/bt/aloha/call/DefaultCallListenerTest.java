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
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.bt.aloha.call.event.AbstractCallEvent;
import com.bt.aloha.call.event.CallConnectedEvent;
import com.bt.aloha.call.event.CallConnectionFailedEvent;
import com.bt.aloha.call.state.CallTerminationCause;
import com.bt.aloha.testing.SipUnitPhone;

public class DefaultCallListenerTest extends CallListenerTestBase {
	@Override
	protected String joinCallLegs() throws Exception {
		outboundCallLegBean.connectCallLeg(firstDialogId);
		String callId = callBean.joinCallLegs(firstDialogId, secondDialogId);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.SecondInbound);
		waitForEmptyReinviteRespondOk(SipUnitPhone.Inbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());
		return callId;
	}
	
	// test that the connected event is not raised after successful negotiation of only 1 reinvite
	@Test
	public void testCallNotConnectedAfterOneReInvite() throws Exception {
		// act
		outboundCallLegBean.connectCallLeg(firstDialogId);
		callBean.joinCallLegs(firstDialogId, secondDialogId);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.SecondInbound);
		waitForEmptyReinviteRespondOk(SipUnitPhone.Inbound);		

		// assert
		assertFalse("Unexpected event: " + printEvents(), connectedSemaphore.tryAcquire(300, TimeUnit.MILLISECONDS));
	}
	
	//Test that the connected event is not immediately raised when two connected dialogs are joined
	@Test
	public void testCallNotConnectedTwoConnectedDialogs() throws Exception {
		// setup
		outboundCallLegBean.connectCallLeg(firstDialogId);
		outboundCallLegBean.connectCallLeg(secondDialogId);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.SecondInbound);

		// act
		callBean.joinCallLegs(firstDialogId, secondDialogId);

		// assert
		assertFalse("Unexpected event: " + printEvents(), connectedSemaphore.tryAcquire(300, TimeUnit.MILLISECONDS));
	}

	//Test that the connected event is raised when two connected dialogs are joined and reinvites are complete
	@Test
	public void testOnCallConnectedTwoConnectedDialogsReinvited() throws Exception {
		// setup
		outboundCallLegBean.connectCallLeg(firstDialogId);
		outboundCallLegBean.connectCallLeg(secondDialogId);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.Inbound);
		waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone.SecondInbound);

		// act
		String callId = callBean.joinCallLegs(firstDialogId, secondDialogId);
		waitForEmptyReinviteRespondOk(SipUnitPhone.Inbound);
		waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone.SecondInbound, getInboundPhoneMediaDescription());
		waitForReinviteAckAssertMediaDescription(SipUnitPhone.Inbound, getSecondInboundPhoneMediaDescription());

		// assert
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
		List<AbstractCallEvent> events = filterCallEventsForCallId(eventVector, callId);
		assertEquals("Expected 1 event", 1, events.size());
		assertEquals("Got different call id", callId, ((CallConnectedEvent)events.get(0)).getCallId());
	}
	
//	 Test that the connection failed event is raised only once if both dialogs fail in parallel
	@Test
	public void testOnCallConnectionFailedEventNotRaisedTwice() throws Exception {
		// setup
		outboundCallLegBean.connectCallLeg(firstDialogId);
		outboundCallLegBean.connectCallLeg(secondDialogId);

		// act
		String callId = callBean.joinCallLegs(firstDialogId, secondDialogId);

		waitForCallSendTryingBusyHere(getInboundCall());
		waitForCallSendTryingBusyHere(getSecondInboundCall());

		// assert
		assertTrue("No connection failed event", connectionFailedSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS));
		List<AbstractCallEvent> events = filterCallEventsForCallId(eventVector, callId);
		assertEquals("Expected 1 event", 1, events.size());
		assertTrue("Wrong event type", events.get(0) instanceof CallConnectionFailedEvent);
		assertEquals("Got unexpected TerminationCause", CallTerminationCause.RemotePartyBusy, ((CallConnectionFailedEvent)events.get(0)).getCallTerminationCause());
		assertEquals("Got a non-zero call duration", 0, ((CallConnectionFailedEvent)events.get(0)).getDuration());
	}
}
