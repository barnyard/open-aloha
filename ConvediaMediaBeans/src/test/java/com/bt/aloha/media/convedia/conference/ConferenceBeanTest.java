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
package com.bt.aloha.media.convedia.conference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.call.CallBean;
import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.call.state.CallInfo;
import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.media.conference.collections.ConferenceCollection;
import com.bt.aloha.media.conference.event.ConferenceActiveEvent;
import com.bt.aloha.media.conference.event.ConferenceEndedEvent;
import com.bt.aloha.media.conference.event.ParticipantConnectedEvent;
import com.bt.aloha.media.conference.event.ParticipantDisconnectedEvent;
import com.bt.aloha.media.conference.event.ParticipantFailedEvent;
import com.bt.aloha.media.conference.event.ParticipantTerminatedEvent;
import com.bt.aloha.media.conference.state.ConferenceInfo;
import com.bt.aloha.media.conference.state.ConferenceState;
import com.bt.aloha.media.conference.state.ParticipantState;


public class ConferenceBeanTest {
	private CallCollection callCollection;
	private ConferenceBeanImpl conferenceBean;
	private CallBean callBean;
	private ConferenceListener dummyConferenceListener;

	@Before
	public void before() {
		callCollection = EasyMock.createNiceMock(CallCollection.class);
		callBean = EasyMock.createNiceMock(CallBean.class);
		conferenceBean = new ConferenceBeanImpl();
		conferenceBean.setCallBean(callBean);
		conferenceBean.setCallCollection(callCollection);
		dummyConferenceListener = new ConferenceListener() {
			public void onConferenceActive(ConferenceActiveEvent conferenceActiveEvent) {}
			public void onConferenceEnded(ConferenceEndedEvent conferenceEndedEvent) {}
			public void onParticipantConnected(ParticipantConnectedEvent participantConnectedEvent) {}
			public void onParticipantDisconnected(ParticipantDisconnectedEvent participantDisconnectedEvent) {}
			public void onParticipantFailed(ParticipantFailedEvent participantFailedEvent) {}
			public void onParticipantTerminated(ParticipantTerminatedEvent participantTerminatedEvent) {}
		};
	}

	/**
	 * Tests that we add the conference bean as a listener to the call bean when set
	 */
	@Test
	public void addConferenceBeanAsListenerWhenSettingCallBean() {
		// setup
		ConferenceBeanImpl conferenceBean = new ConferenceBeanImpl();
		CallBean callBean = EasyMock.createMock(CallBean.class);
		callBean.addCallListener(conferenceBean);
		EasyMock.replay(callBean);

		// act
		conferenceBean.setCallBean(callBean);

		// assert
		EasyMock.verify(callBean);
	}

	/**
	 * Tests that we remove the conference bean when we reset the call bean
	 */
	@Test
	public void removeConferenceBeanAsListenerWhenResetingCallBean() {
		// setup
		ConferenceBeanImpl conferenceBean = new ConferenceBeanImpl();
		CallBean callBean = EasyMock.createMock(CallBean.class);
		callBean.addCallListener(conferenceBean);
		callBean.removeCallListener(conferenceBean);
		EasyMock.replay(callBean);
		CallBean anotherCallBean = EasyMock.createMock(CallBean.class);
		anotherCallBean.addCallListener(conferenceBean);
		EasyMock.replay(anotherCallBean);
		conferenceBean.setCallBean(callBean);

		// act
		conferenceBean.setCallBean(anotherCallBean);

		// assert
		EasyMock.verify(callBean);
		EasyMock.verify(anotherCallBean);
	}

    // test that we call add listeners one at a time to an existing conferencebean that has no listeners
    @Test
    public void testAddListenerToNewBean() {
        // act
        conferenceBean.addConferenceListener(dummyConferenceListener);

        // assert
        assertEquals(1, conferenceBean.getConferenceListeners().size());

        // act
        conferenceBean.addConferenceListener(dummyConferenceListener);

        // assert
        assertEquals(2, conferenceBean.getConferenceListeners().size());
    }

    // test that we call add listeners one at a time to an existing conferencebean that already has
    // som listeners
    @Test
    public void testAddListenerToBeanWithExistingArray() {
        // setup
        List<ConferenceListener> conferenceListenerList = new ArrayList<ConferenceListener>();
        conferenceListenerList.add(dummyConferenceListener);
        conferenceListenerList.add(dummyConferenceListener);

        conferenceBean.setConferenceListeners(conferenceListenerList);

        // act
        conferenceBean.addConferenceListener(dummyConferenceListener);

        // assert
        assertEquals(3, conferenceBean.getConferenceListeners().size());

        // act
        conferenceBean.addConferenceListener(dummyConferenceListener);

        // assert
        assertEquals(4, conferenceBean.getConferenceListeners().size());
    }

	/**
	 * make sure we can add conference listeners
	 */
	@Test
	public void addConferenceListener() {
		// setup
		ConferenceListener listener = EasyMock.createMock(ConferenceListener.class);

		// act
		conferenceBean.addConferenceListener(listener);

		// assert
		assertEquals(1, conferenceBean.getConferenceListeners().size());
		assertSame(listener, conferenceBean.getConferenceListeners().get(0));
	}

	/**
	 * make sure we can remove conference listeners
	 */
	@Test
	public void removeConferenceListener() {
		// setup
		ConferenceListener listener = EasyMock.createMock(ConferenceListener.class);
		conferenceBean.addConferenceListener(listener);

		// act
		conferenceBean.removeConferenceListener(listener);

		// assert
		assertEquals(0, conferenceBean.getConferenceListeners().size());
	}

	// test that you cannot add a null listener
	@Test(expected=IllegalArgumentException.class)
	public void addNullListener() {
		// act
		conferenceBean.addConferenceListener(null);

		// assert - exception
	}

	// test that you cannot remove a null listener
	@Test(expected=IllegalArgumentException.class)
	public void removeNullListener() {
		// act
		conferenceBean.removeConferenceListener(null);

		// assert - exception
	}
	
	// test that we can't send in a null sip uri
	@Test(expected=IllegalArgumentException.class)
	public void nullSipUri() {
		// setup
		ConferenceCollection confCollection = EasyMock.createNiceMock(ConferenceCollection.class);
		EasyMock.expect(confCollection.get("123")).andReturn(new ConferenceInfo("a", "a"));
		EasyMock.replay(confCollection);
		conferenceBean.setConferenceCollection(confCollection);

		// act
		conferenceBean.createParticipantCallLeg("123", null);
	}
	
	// test that we don't throw an exception when terminating an already terminated conference
	@Test
	public void terminateAlreadyTerminatedConference() {
		// setup
		ConferenceCollection confCollection = EasyMock.createMock(ConferenceCollection.class);
		ConferenceInfo info = new ConferenceInfo("bean", "123.123.123.123");
		info.updateConferenceState(ConferenceState.Ended);
		EasyMock.expect(confCollection.get("123")).andReturn(info);
		EasyMock.replay(confCollection);
		conferenceBean.setConferenceCollection(confCollection);
		
		// act
		conferenceBean.endConference("123");
		
		// assert
		// no exception
	}

	// test that we don't throw an exception when terminating an already ending conference
	@Test
	public void terminateAlreadyEndingConference() {
		// setup
		ConferenceCollection confCollection = EasyMock.createMock(ConferenceCollection.class);
		ConferenceInfo info = new ConferenceInfo("bean", "123.123.123.123");
		info.updateConferenceState(ConferenceState.Ending);
		EasyMock.expect(confCollection.get("123")).andReturn(info);
		EasyMock.replay(confCollection);
		conferenceBean.setConferenceCollection(confCollection);
		
		// act
		conferenceBean.endConference("123");
		
		// assert
		// no exception
	}

	// test that terminating a particpant not in a call doesn't throw an exception
	@Test
	public void teminateNoCurrentCallForParticipant() {
		// setup
		ConferenceInfo info = new ConferenceInfo("bean", "123.123.123.123");
		info.updateConferenceState(ConferenceState.Active);
		EasyMock.expect(callCollection.getCurrentCallForCallLeg("callLegId")).andReturn(null);
		
		// act
		conferenceBean.terminateParticipant("123", "callLegId");
		
		// assert
		// no exception
	}

	// test that terminating a particpant not in a conference doesn't throw exception
	@Test
	public void teminateNonExistantParticipant() {
		// setup
		ConferenceCollection confCollection = EasyMock.createMock(ConferenceCollection.class);
		ConferenceInfo info = new ConferenceInfo("bean", "123.123.123.123");
		info.updateConferenceState(ConferenceState.Active);
		CallInfo callInfo = new CallInfo("bean", "callId", "dialog1", "dialog2", AutoTerminateAction.False, 100);
		EasyMock.expect(callCollection.getCurrentCallForCallLeg("dialog1")).andReturn(callInfo);
		EasyMock.expect(confCollection.get("123")).andReturn(info);
		EasyMock.replay(confCollection);
		EasyMock.replay(callCollection);
		conferenceBean.setConferenceCollection(confCollection);
		
		// act
		conferenceBean.terminateParticipant("123", "dialog1");
		
		// assert
		// no exception
	}

	// test that terminating an already terminated particpant doesn't throw exception
	@Test
	public void teminateAlreadyTerminatedParticipant() {
		// setup
		ConferenceCollection confCollection = EasyMock.createMock(ConferenceCollection.class);
		ConferenceInfo info = new ConferenceInfo("bean", "123.123.123.123");
		info.updateConferenceState(ConferenceState.Active);
		info.addParticipant("callId");
		info.updateParticipantState("callId", ParticipantState.Terminating);
		CallInfo callInfo = new CallInfo("bean", "callId", "dialog1", "dialog2", AutoTerminateAction.False, 100);
		EasyMock.expect(callCollection.getCurrentCallForCallLeg("dialog1")).andReturn(callInfo);
		EasyMock.expect(confCollection.get("123")).andReturn(info);
		EasyMock.replay(confCollection);
		EasyMock.replay(callCollection);
		conferenceBean.setConferenceCollection(confCollection);
		
		// act
		conferenceBean.terminateParticipant("123", "dialog1");
		
		// assert
		// no exception
	}
}
