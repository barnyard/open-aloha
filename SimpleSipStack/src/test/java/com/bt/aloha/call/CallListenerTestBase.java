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

import org.cafesip.sipunit.SipCall;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.call.event.AbstractCallEvent;
import com.bt.aloha.call.event.CallConnectedEvent;

public abstract class CallListenerTestBase extends CallBeanTestBase {	
	protected String firstDialogId;
	protected String secondDialogId;	

	@Before
	public void beforeCallListenerTestBase() throws Exception {
		firstDialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri());
		secondDialogId = outboundCallLegBean.createCallLeg(getInboundPhoneSipUri(), getSecondInboundPhoneSipUri());
	}

	@After
	public void after() {
		eventVector.clear();
	}

	protected abstract String joinCallLegs() throws Exception;

	protected SipCall getFirstSipCall() {
		return getInboundCall();
	}
	
	protected SipCall getSecondSipCall() {
		return getSecondInboundCall();
	}
	
	protected void terminateCallLeg(String callLegId) {
		outboundCallLegBean.terminateCallLeg(callLegId);
	}
	
    protected String printEvents() {
        StringBuffer result = new StringBuffer();
        String sep = "";
        for (Object o: this.eventVector) {
            result.append(sep + o.getClass().getSimpleName());
            sep = ": ";
        }
        return result.toString();
    }

	//Test that the connected event is raised for 3pc scenario after successful processing of reinvites
	@Test
	public void testOnCallConnected() throws Exception {
		// act
		String callId = joinCallLegs();

		// assert
		assertTrue("No connected event", connectedSemaphore.tryAcquire(5, TimeUnit.SECONDS));

		List<AbstractCallEvent> events = filterCallEventsForCallId(eventVector, callId);
		assertEquals("Expected 1 event", 1, events.size());
		assertEquals("Got call id", callId, ((CallConnectedEvent)events.get(0)).getCallId());
	}
}
