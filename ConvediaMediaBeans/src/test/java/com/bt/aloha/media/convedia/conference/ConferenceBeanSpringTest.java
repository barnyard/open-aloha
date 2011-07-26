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

package com.bt.aloha.media.convedia.conference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.sip.message.Response;

import org.cafesip.sipunit.SipCall;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.call.event.CallConnectedEvent;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.media.MediaDialogInfo;
import com.bt.aloha.media.conference.collections.ConferenceCollection;
import com.bt.aloha.media.conference.event.ConferenceActiveEvent;
import com.bt.aloha.media.conference.event.ConferenceEndedEvent;
import com.bt.aloha.media.conference.event.ParticipantConnectedEvent;
import com.bt.aloha.media.conference.event.ParticipantDisconnectedEvent;
import com.bt.aloha.media.conference.event.ParticipantFailedEvent;
import com.bt.aloha.media.conference.event.ParticipantTerminatedEvent;
import com.bt.aloha.media.conference.state.ConferenceInfo;
import com.bt.aloha.media.conference.state.ConferenceInformation;
import com.bt.aloha.media.conference.state.ConferenceState;
import com.bt.aloha.media.conference.state.ConferenceTerminationCause;
import com.bt.aloha.media.conference.state.ParticipantState;
import com.bt.aloha.media.convedia.MediaServerAddressFactory;
import com.bt.aloha.media.testing.ConvediaMediaBeansPerMethodTestCase;
import com.bt.aloha.media.testing.mockphones.ConvediaMockphoneBean;
import com.bt.aloha.testing.SipUnitPhone;
import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;

public class ConferenceBeanSpringTest extends ConvediaMediaBeansPerMethodTestCase implements ConferenceListener {

    private ConferenceBean conferenceBean;
    private ConferenceCollection conferenceCollection;
    private List<ConferenceActiveEvent> conferenceActiveEventVector;
    private List<ConferenceEndedEvent> conferenceEndedEventVector;
    private List<ParticipantConnectedEvent> participantConnectedEventVector;
    private List<ParticipantTerminatedEvent> participantTerminatedEventVector;
    private List<ParticipantDisconnectedEvent> participantDisconnectedEventVector;
    private List<ParticipantFailedEvent> participantFailedEventVector;
    private Semaphore conferenceActiveEventVectorSemaphore;
    private Semaphore conferenceEndedEventVectorSemaphore;
    private Semaphore participantConnectedEventVectorSemaphore;
    private Semaphore participantTerminatedEventVectorSemaphore;
    private Semaphore participantDisconnectedEventVectorSemaphore;
    private Semaphore participantFailedEventVectorSemaphore;
    private CallCollection callCollection;
    private static ClassPathXmlApplicationContext mockphonesApplicationContext;
    private DialogCollection dialogCollection;
    private boolean scheduled;

    @Before
    public void before() throws Exception {
        mockphonesApplicationContext = new ClassPathXmlApplicationContext("testMockphoneApplicationContext.xml");
        conferenceCollection = (ConferenceCollection) getApplicationContext().getBean("conferenceCollection");
        conferenceBean = (ConferenceBean) getApplicationContext().getBean("conferenceBean");
        callCollection = (CallCollection) getApplicationContext().getBean("callCollection");
        dialogCollection = (DialogCollection) getApplicationContext().getBean("dialogCollection");

        List<ConferenceListener> conferenceListeners = new ArrayList<ConferenceListener>();
        conferenceListeners.add(this);
        ((ConferenceBeanImpl) conferenceBean).setConferenceListeners(conferenceListeners);
        conferenceActiveEventVector = new Vector<ConferenceActiveEvent>();
        conferenceEndedEventVector = new Vector<ConferenceEndedEvent>();
        participantConnectedEventVector = new Vector<ParticipantConnectedEvent>();
        participantTerminatedEventVector = new Vector<ParticipantTerminatedEvent>();
        participantDisconnectedEventVector = new Vector<ParticipantDisconnectedEvent>();
        participantFailedEventVector = new Vector<ParticipantFailedEvent>();
        conferenceActiveEventVectorSemaphore = new Semaphore(0);
        conferenceEndedEventVectorSemaphore = new Semaphore(0);
        participantConnectedEventVectorSemaphore = new Semaphore(0);
        participantTerminatedEventVectorSemaphore = new Semaphore(0);
        participantDisconnectedEventVectorSemaphore = new Semaphore(0);
        participantFailedEventVectorSemaphore = new Semaphore(0);

        scheduled = false;
    }

    @After
    public void after() {
        if (null != mockphonesApplicationContext) {
            mockphonesApplicationContext.destroy();
        }
    }

    // Test creating conference dialog
    @Test
    public void testCreateConferenceDialog() throws Exception {
        // setup
        String confId = conferenceBean.createConference();

        // act
        String dialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());

        // assert
        assertNotNull(dialogId);
    }

    // Test creating conference dialog for non-existing conference
    @Test(expected = IllegalArgumentException.class)
    public void testCreateConferenceDialogForNonExistingConference() throws Exception {
        // setup
        String confId = "xyz";

        // act
        conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());
    }

    // this test, although not strictly at home here, tests that the media
    // server address is stored in the
    // conference info and would get retrieved for use when connecting
    // participants.
    @Test
    public void testCreateConferenceWithRoundRobinMediaServerAddresses() {
        // setup
        MediaServerAddressFactory mediaServerAddressFactory = new MediaServerAddressFactory();
        mediaServerAddressFactory.setMediaServerAddresses("1.1.1.1:1,2.2.2.2:2");
        ((ConferenceBeanImpl) conferenceBean).setMediaServerAddressFactory(mediaServerAddressFactory);

        // act
        String confId1 = conferenceBean.createConference();
        String confId2 = conferenceBean.createConference();
        String confId3 = conferenceBean.createConference();

        // assert
        assertTrue(conferenceCollection.get(confId1).getConferenceSipUri().endsWith("1.1.1.1:1"));
        assertTrue(conferenceCollection.get(confId2).getConferenceSipUri().endsWith("2.2.2.2:2"));
        assertTrue(conferenceCollection.get(confId3).getConferenceSipUri().endsWith("1.1.1.1:1"));
    }

    // test that we create the conference dialog as a Conference Dialog Info
    // when we create conference call leg
    @Test
    public void inviteParticipantCreatesMediaDialogInfo() {
        // act
        String dialogId = ((ConferenceBeanImpl) conferenceBean).createConferenceCallLeg(URI
                .create("sip:someone@somewhere.com"), URI.create("sip:conference@mediaserver.com"));

        // assert
        assertTrue("DialogInfo is not a MediaDialogInfo", dialogCollection.get(dialogId) instanceof MediaDialogInfo);
    }

    // test that create conference allocates autogenerated conference id
    // and adds the new conferenceInfo to conferenceCollection
    @Test
    public void testCreateConferenceWithDefaultMaxMNumberOfParticipants() throws Exception {
        // setup
        int sizeBefore = conferenceCollection.size();

        // act
        String confId = conferenceBean.createConference();

        // assert
        assertNotNull(confId);
        assertEquals(32, confId.length());
        assertEquals(1, conferenceCollection.size() - sizeBefore);
        assertNotNull(conferenceCollection.get(confId));
        assertTrue(conferenceCollection.get(confId).getConferenceSipUri().contains(confId));
        assertEquals(ConferenceState.Initial, conferenceCollection.get(confId).getConferenceState());
        Thread.sleep(10);
        assertTrue(conferenceCollection.get(confId).getCreateTime() < Calendar.getInstance().getTimeInMillis());
        assertEquals(ConferenceInfo.TIME_NOT_SET, conferenceCollection.get(confId).getStartTime());
        assertEquals(ConferenceInfo.TIME_NOT_SET, conferenceCollection.get(confId).getEndTime());
        assertEquals(ConferenceInfo.DEFAULT_MAX_NUMBER_OF_PARTICIPANTS, conferenceCollection.get(confId)
                .getMaxNumberOfParticipants());
        assertEquals(ConferenceInfo.DEFAULT_MAX_DURATION_IN_MINUTES, conferenceCollection.get(confId)
                .getMaxDurationInMinutes());
    }

    // test that create conference with max number of participants stores that
    // information
    // correctly.
    @Test
    public void testCreateConferenceWithMaxNumberOfParticipantsAndMaxDuration() throws Exception {
        // setup
        int sizeBefore = conferenceCollection.size();

        // act
        String confId = conferenceBean.createConference(16, 30);

        // assert
        assertNotNull(confId);
        assertEquals(32, confId.length());
        assertEquals(1, conferenceCollection.size() - sizeBefore);
        assertNotNull(conferenceCollection.get(confId));
        assertTrue(conferenceCollection.get(confId).getConferenceSipUri().contains(confId));
        assertEquals(ConferenceState.Initial, conferenceCollection.get(confId).getConferenceState());
        Thread.sleep(10);
        assertTrue(conferenceCollection.get(confId).getCreateTime() < Calendar.getInstance().getTimeInMillis());
        assertEquals(ConferenceInfo.TIME_NOT_SET, conferenceCollection.get(confId).getStartTime());
        assertEquals(ConferenceInfo.TIME_NOT_SET, conferenceCollection.get(confId).getEndTime());
        assertEquals(16, conferenceCollection.get(confId).getMaxNumberOfParticipants());
    }

    // test that create conference with illegal number of max number of
    // participants
    // throws illegal argument exception
    @Test(expected = IllegalArgumentException.class)
    public void testCreateConferenceWithIllegalMaxNumberOfParticipants() throws Exception {
        // act
        conferenceBean.createConference(1, 30);
    }

    // test that create conference with illegal max duration
    // throws illegal argument exception
    @Test(expected = IllegalArgumentException.class)
    public void testCreateConferenceWithIllegalMaxDuration() throws Exception {
        // act
        conferenceBean.createConference(10, -1);
    }

    // test that create conference with 0 minutes as max duration
    // is possible and means no max duration
    @Test
    public void testCreateConferenceWithZeroAsMaxConferenceDuration() throws Exception {
        // act
        conferenceBean.createConference(10, 0);
    }

    // simple single party conference - no one to talk to!
    @Test
    public void testInviteParticipant() throws Exception {
        // setup
        String confId = conferenceBean.createConference();

        // create first dialog
        String dialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());

        // act
        conferenceBean.inviteParticipant(confId, dialogId);

        this.answerConferenceLeg(SipUnitPhone.Inbound);

        waitForConferenceActiveEvent(confId);
        waitForParticipantConnectedEvent(confId, dialogId);

        assertEquals("conference state not Active", ConferenceState.Active, conferenceCollection.get(confId)
                .getConferenceState());
        assertEquals(1, conferenceCollection.get(confId).getNumberOfActiveParticipants());
        assertTrue(conferenceCollection.get(confId).getCreateTime() > 0);
        assertTrue(conferenceCollection.get(confId).getStartTime() > 0);
        assertEquals(ConferenceInfo.TIME_NOT_SET, conferenceCollection.get(confId).getEndTime());
        assertTrue(conferenceCollection.get(confId).containsParticipant(
                callCollection.getCurrentCallForCallLeg(dialogId).getId()));
    }

    // simple two party conference
    @Test
    public void testInviteParticipants() throws Exception {
        // setup
        String confId = conferenceBean.createConference();

        // create first dialog
        String dialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());

        // act
        conferenceBean.inviteParticipant(confId, dialogId);

        this.answerConferenceLeg(SipUnitPhone.Inbound);

        waitForConferenceActiveEvent(confId);
        waitForParticipantConnectedEvent(confId, dialogId);

        assertEquals("conference state not Active", ConferenceState.Active, conferenceCollection.get(confId)
                .getConferenceState());
        assertEquals(1, conferenceCollection.get(confId).getNumberOfActiveParticipants());
        assertTrue(conferenceCollection.get(confId).containsParticipant(
                callCollection.getCurrentCallForCallLeg(dialogId).getId()));

        // **** second participant *****
        String secondDialogId = conferenceBean.createParticipantCallLeg(confId, getSecondInboundPhoneSipUri());

        // act
        conferenceBean.inviteParticipant(confId, secondDialogId);

        this.answerConferenceLeg(SipUnitPhone.SecondInbound);

        waitForParticipantConnectedEvent(confId, secondDialogId);

        assertEquals("conference state not Active", ConferenceState.Active, conferenceCollection.get(confId)
                .getConferenceState());
        assertEquals(2, conferenceCollection.get(confId).getNumberOfActiveParticipants());
        assertTrue(conferenceCollection.get(confId).containsParticipant(
                callCollection.getCurrentCallForCallLeg(secondDialogId).getId()));
    }

    // Test adding participant to a conference which has currently max number of
    // participants
    @Test(expected = IllegalStateException.class)
    public void testInviteParticipantToFullConference() throws Exception {
        // setup
        String confId = conferenceBean.createConference(2, 0);
        String firstDialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());
        String secondDialogId = conferenceBean.createParticipantCallLeg(confId, getSecondInboundPhoneSipUri());
        String thirdDialogId = conferenceBean.createParticipantCallLeg(confId, getThirdInboundPhoneSipUri());

        conferenceBean.inviteParticipant(confId, firstDialogId);
        conferenceBean.inviteParticipant(confId, secondDialogId);

        // act
        conferenceBean.inviteParticipant(confId, thirdDialogId);
    }

    // Test adding participant to a conference with one non-active participant.
    // It should be possible and no exception should be thrown.
    @Test
    public void testInviteParticipantToConferenceWithOneNonActiveParticipant() throws Exception {
        // setup
        String confId = conferenceBean.createConference(2, 0);

        // create first dialog
        String dialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());

        conferenceBean.inviteParticipant(confId, dialogId);

        this.answerConferenceLeg(SipUnitPhone.Inbound);

        waitForConferenceActiveEvent(confId);
        waitForParticipantConnectedEvent(confId, dialogId);

        assertEquals("conference state not Active", ConferenceState.Active, conferenceCollection.get(confId)
                .getConferenceState());
        assertEquals(1, conferenceCollection.get(confId).getNumberOfActiveParticipants());
        assertTrue(conferenceCollection.get(confId).containsParticipant(
                callCollection.getCurrentCallForCallLeg(dialogId).getId()));

        // **** second participant *****
        String secondDialogId = conferenceBean.createParticipantCallLeg(confId, getSecondInboundPhoneSipUri());

        conferenceBean.inviteParticipant(confId, secondDialogId);

        this.answerConferenceLeg(SipUnitPhone.SecondInbound);

        waitForParticipantConnectedEvent(confId, secondDialogId);

        assertEquals("conference state not Active", ConferenceState.Active, conferenceCollection.get(confId)
                .getConferenceState());
        assertEquals(2, conferenceCollection.get(confId).getNumberOfActiveParticipants());
        assertTrue(conferenceCollection.get(confId).containsParticipant(
                callCollection.getCurrentCallForCallLeg(secondDialogId).getId()));

        // terminate 1st participant
        conferenceBean.terminateParticipant(confId, dialogId);

        // assert
        assertTrue("No disconnect for participant dialog", getInboundCall().waitForDisconnect(5000));
        assertTrue("Participant dialog failed to respond to disconnect", getInboundCall().respondToDisconnect());

        waitForParticipantTerminatedEvent(confId, dialogId);
        assertEquals(1, conferenceCollection.get(confId).getNumberOfActiveParticipants());

        assertParticipantState(ParticipantState.Terminated, confId, dialogId);

        // act
        dialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());
        conferenceBean.inviteParticipant(confId, dialogId);

        this.answerConferenceLeg(SipUnitPhone.Inbound);
        waitForParticipantConnectedEvent(confId, dialogId);
        assertEquals("conference state not Active", ConferenceState.Active, conferenceCollection.get(confId)
                .getConferenceState());
        assertEquals(2, conferenceCollection.get(confId).getNumberOfActiveParticipants());
        assertTrue(conferenceCollection.get(confId).containsParticipant(
                callCollection.getCurrentCallForCallLeg(dialogId).getId()));
    }

    // Invite a non-existing participant to a conference
    @Test(expected = IllegalArgumentException.class)
    public void testInviteParticipantNonExisting() throws Exception {
        // setup
        String confId = conferenceBean.createConference();

        // act
        conferenceBean.inviteParticipant(confId, "xyz");
    }

    // test terminating a participant from a single participant conference
    @Test
    public void testTerminateParticipant() throws Exception {
        // setup
        String confId = conferenceBean.createConference();

        // create first dialog
        String dialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());

        // act
        conferenceBean.inviteParticipant(confId, dialogId);

        this.answerConferenceLeg(SipUnitPhone.Inbound);

        waitForConferenceActiveEvent(confId);
        waitForParticipantConnectedEvent(confId, dialogId);

        assertEquals("conference state not Active", ConferenceState.Active, conferenceCollection.get(confId)
                .getConferenceState());
        assertEquals(1, conferenceCollection.get(confId).getNumberOfActiveParticipants());
        assertTrue(conferenceCollection.get(confId).containsParticipant(
                callCollection.getCurrentCallForCallLeg(dialogId).getId()));

        // terminate
        Thread.sleep(1000);
        conferenceBean.terminateParticipant(confId, dialogId);

        // assert
        assertTrue("No disconnect for participant dialog", getInboundCall().waitForDisconnect(5000));
        assertTrue("Participant dialog failed to respond to disconnect", getInboundCall().respondToDisconnect());

        waitForParticipantTerminatedEvent(confId, dialogId);
        assertEquals(0, conferenceCollection.get(confId).getNumberOfActiveParticipants());

        assertParticipantState(ParticipantState.Terminated, confId, dialogId);

        waitForConferenceEndedEventWithTerminationCause(confId, ConferenceTerminationCause.LastParticipantTerminated,
                false);
        assertEquals(ConferenceState.Ended, conferenceCollection.get(confId).getConferenceState());
        assertEquals(ConferenceTerminationCause.LastParticipantTerminated, conferenceCollection.get(confId)
                .getConferenceTerminationCause());
    }

    // test terminating 2 participants from conference
    @Test
    public void testTerminateParticipants() throws Exception {
        // setup
        String confId = conferenceBean.createConference();

        // create first dialog
        String dialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());

        // act
        conferenceBean.inviteParticipant(confId, dialogId);

        this.answerConferenceLeg(SipUnitPhone.Inbound);

        waitForConferenceActiveEvent(confId);
        waitForParticipantConnectedEvent(confId, dialogId);

        assertEquals("conference state not Active", ConferenceState.Active, conferenceCollection.get(confId)
                .getConferenceState());
        assertEquals(1, conferenceCollection.get(confId).getNumberOfActiveParticipants());
        assertTrue(conferenceCollection.get(confId).containsParticipant(
                callCollection.getCurrentCallForCallLeg(dialogId).getId()));

        // invite 2nd participant
        String secondDialogId = conferenceBean.createParticipantCallLeg(confId, getSecondInboundPhoneSipUri());
        conferenceBean.inviteParticipant(confId, secondDialogId);
        this.answerConferenceLeg(SipUnitPhone.SecondInbound);

        waitForParticipantConnectedEvent(confId, secondDialogId);
        assertEquals("conference state not Active", ConferenceState.Active, conferenceCollection.get(confId)
                .getConferenceState());
        assertEquals(2, conferenceCollection.get(confId).getNumberOfActiveParticipants());
        assertTrue(conferenceCollection.get(confId).containsParticipant(
                callCollection.getCurrentCallForCallLeg(secondDialogId).getId()));

        // terminate 1st participant
        Thread.sleep(1000);
        conferenceBean.terminateParticipant(confId, dialogId);

        // assert
        assertTrue("No disconnect for participant dialog", getInboundCall().waitForDisconnect(5000));
        assertTrue("Participant dialog failed to respond to disconnect", getInboundCall().respondToDisconnect());

        waitForParticipantTerminatedEvent(confId, dialogId);
        assertEquals(1, conferenceCollection.get(confId).getNumberOfActiveParticipants());
        assertParticipantState(ParticipantState.Terminated, confId, dialogId);

        assertEquals(ConferenceState.Active, conferenceCollection.get(confId).getConferenceState());

        // terminate 2nd participant
        conferenceBean.terminateParticipant(confId, secondDialogId);

        assertTrue("No disconnect for participant dialog", getSecondInboundCall().waitForDisconnect(5000));
        assertTrue("Participant dialog failed to respond to disconnect", getSecondInboundCall().respondToDisconnect());

        waitForParticipantTerminatedEvent(confId, secondDialogId);
        assertEquals(0, conferenceCollection.get(confId).getNumberOfActiveParticipants());

        assertParticipantState(ParticipantState.Terminated, confId, secondDialogId);

        waitForConferenceEndedEventWithTerminationCause(confId, ConferenceTerminationCause.LastParticipantTerminated,
                false);
        assertEquals(ConferenceState.Ended, conferenceCollection.get(confId).getConferenceState());
        assertEquals(ConferenceTerminationCause.LastParticipantTerminated, conferenceCollection.get(confId)
                .getConferenceTerminationCause());
    }

    // test terminating participant in non-existing conference
    @Test(expected = IllegalArgumentException.class)
    public void testTerminateParticipantInNonExistingConference() throws Exception {
        // setup
        String confId = conferenceBean.createConference();

        // create first dialog
        String dialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());

        conferenceBean.inviteParticipant(confId, dialogId);

        this.answerConferenceLeg(SipUnitPhone.Inbound);

        waitForConferenceActiveEvent(confId);
        waitForParticipantConnectedEvent(confId, dialogId);

        assertEquals("conference state not Active", ConferenceState.Active, conferenceCollection.get(confId)
                .getConferenceState());
        assertEquals(1, conferenceCollection.get(confId).getNumberOfActiveParticipants());
        assertTrue(conferenceCollection.get(confId).containsParticipant(
                callCollection.getCurrentCallForCallLeg(dialogId).getId()));

        // act
        conferenceBean.terminateParticipant("xyz", dialogId);
    }

    // test diconnecting a participant from a single participant conference
    @Test
    public void testDisconnectedParticipant() throws Exception {
        // setup
        String confId = conferenceBean.createConference();

        // create first dialog
        String dialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());

        // act
        conferenceBean.inviteParticipant(confId, dialogId);

        this.answerConferenceLeg(SipUnitPhone.Inbound);

        waitForConferenceActiveEvent(confId);
        waitForParticipantConnectedEvent(confId, dialogId);

        assertEquals("conference state not Active", ConferenceState.Active, conferenceCollection.get(confId)
                .getConferenceState());
        assertEquals(1, conferenceCollection.get(confId).getNumberOfActiveParticipants());
        assertTrue(conferenceCollection.get(confId).containsParticipant(
                callCollection.getCurrentCallForCallLeg(dialogId).getId()));

        // Disconnect the participant
        Thread.sleep(1000);
        getInboundCall().disconnect();

        // assert

        waitForParticipantDisconnectedEvent(confId, dialogId);
        assertEquals(0, conferenceCollection.get(confId).getNumberOfActiveParticipants());

        assertParticipantState(ParticipantState.Disconnected, confId, dialogId);

        waitForConferenceEndedEventWithTerminationCause(confId, ConferenceTerminationCause.LastParticipantDisconnected,
                false);
        assertEquals(ConferenceState.Ended, conferenceCollection.get(confId).getConferenceState());
        assertEquals(ConferenceTerminationCause.LastParticipantDisconnected, conferenceCollection.get(confId)
                .getConferenceTerminationCause());
    }

    // test 2 participants disconnected from conference
    @Test
    public void testDisconnectedParticipants() throws Exception {
        // setup
        String confId = conferenceBean.createConference();

        // create first dialog
        String dialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());

        // act
        conferenceBean.inviteParticipant(confId, dialogId);

        this.answerConferenceLeg(SipUnitPhone.Inbound);

        waitForConferenceActiveEvent(confId);
        waitForParticipantConnectedEvent(confId, dialogId);

        assertEquals("conference state not Active", ConferenceState.Active, conferenceCollection.get(confId)
                .getConferenceState());
        assertEquals(1, conferenceCollection.get(confId).getNumberOfActiveParticipants());
        assertTrue(conferenceCollection.get(confId).containsParticipant(
                callCollection.getCurrentCallForCallLeg(dialogId).getId()));

        // invite 2nd participant
        String secondDialogId = conferenceBean.createParticipantCallLeg(confId, getSecondInboundPhoneSipUri());
        conferenceBean.inviteParticipant(confId, secondDialogId);
        this.answerConferenceLeg(SipUnitPhone.SecondInbound);

        waitForParticipantConnectedEvent(confId, secondDialogId);
        assertEquals("conference state not Active", ConferenceState.Active, conferenceCollection.get(confId)
                .getConferenceState());
        assertEquals(2, conferenceCollection.get(confId).getNumberOfActiveParticipants());
        assertTrue(conferenceCollection.get(confId).containsParticipant(
                callCollection.getCurrentCallForCallLeg(secondDialogId).getId()));

        // terminate 1st participant
        Thread.sleep(1000);
        getInboundCall().disconnect();

        // assert
        waitForParticipantDisconnectedEvent(confId, dialogId);
        assertEquals(1, conferenceCollection.get(confId).getNumberOfActiveParticipants());

        assertParticipantState(ParticipantState.Disconnected, confId, dialogId);
        assertEquals(ConferenceState.Active, conferenceCollection.get(confId).getConferenceState());

        // terminate 2nd participant
        getSecondInboundCall().disconnect();

        waitForParticipantDisconnectedEvent(confId, secondDialogId);
        assertEquals(0, conferenceCollection.get(confId).getNumberOfActiveParticipants());

        assertParticipantState(ParticipantState.Disconnected, confId, secondDialogId);

        waitForConferenceEndedEventWithTerminationCause(confId, ConferenceTerminationCause.LastParticipantDisconnected,
                false);
        assertEquals(ConferenceState.Ended, conferenceCollection.get(confId).getConferenceState());
        assertEquals(ConferenceTerminationCause.LastParticipantDisconnected, conferenceCollection.get(confId)
                .getConferenceTerminationCause());
    }

    // Test ending a single participant conference
    @Test
    public void testEndConferenceWithTerminationCause() throws Exception {
        // setup
        String confId = conferenceBean.createConference();

        // create first dialog
        String dialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());
        assertEquals(ConferenceState.Initial, conferenceCollection.get(confId).getConferenceState());
        assertEquals(ConferenceInfo.TIME_NOT_SET, conferenceCollection.get(confId).getEndTime());

        // act
        conferenceBean.inviteParticipant(confId, dialogId);

        this.answerConferenceLeg(SipUnitPhone.Inbound);

        waitForConferenceActiveEvent(confId);
        waitForParticipantConnectedEvent(confId, dialogId);

        assertEquals("conference state not Active", ConferenceState.Active, conferenceCollection.get(confId)
                .getConferenceState());
        assertEquals(1, conferenceCollection.get(confId).getNumberOfActiveParticipants());
        assertTrue(conferenceCollection.get(confId).containsParticipant(
                callCollection.getCurrentCallForCallLeg(dialogId).getId()));

        // end conference
        Thread.sleep(1000);
        conferenceBean.endConference(confId, ConferenceTerminationCause.MaximumDurationExceeded);

        // assertEquals(ConferenceState.Ending,
        // conferenceCollection.get(confId).getConferenceState());
        assertTrue(conferenceCollection.get(confId).getConferenceState().ordinal() >= ConferenceState.Ending.ordinal());

        // assert
        assertTrue("No disconnect for participant dialog", getInboundCall().waitForDisconnect(5000));
        assertTrue("Participant dialog failed to respond to disconnect", getInboundCall().respondToDisconnect());

        waitForParticipantTerminatedEvent(confId, dialogId);
        assertEquals(0, conferenceCollection.get(confId).getNumberOfActiveParticipants());

        assertParticipantState(ParticipantState.Terminated, confId, dialogId);

        waitForConferenceEndedEventWithTerminationCause(confId, ConferenceTerminationCause.MaximumDurationExceeded,
                false);
        assertEquals(ConferenceState.Ended, conferenceCollection.get(confId).getConferenceState());
        assertEquals(ConferenceTerminationCause.MaximumDurationExceeded, conferenceCollection.get(confId)
                .getConferenceTerminationCause());
        assertTrue(conferenceCollection.get(confId).getEndTime() > 0);
    }

    // Test ending a single participant conference
    @Test
    public void testEndConference() throws Exception {
        // setup
        String confId = conferenceBean.createConference();

        // create first dialog
        String dialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());
        assertEquals(ConferenceState.Initial, conferenceCollection.get(confId).getConferenceState());
        assertEquals(ConferenceInfo.TIME_NOT_SET, conferenceCollection.get(confId).getEndTime());

        // act
        conferenceBean.inviteParticipant(confId, dialogId);

        this.answerConferenceLeg(SipUnitPhone.Inbound);

        waitForConferenceActiveEvent(confId);
        waitForParticipantConnectedEvent(confId, dialogId);

        assertEquals("conference state not Active", ConferenceState.Active, conferenceCollection.get(confId)
                .getConferenceState());
        assertEquals(1, conferenceCollection.get(confId).getNumberOfActiveParticipants());
        assertTrue(conferenceCollection.get(confId).containsParticipant(
                callCollection.getCurrentCallForCallLeg(dialogId).getId()));

        // end conference
        Thread.sleep(1000);
        conferenceBean.endConference(confId);
        assertTrue(conferenceCollection.get(confId).getConferenceState().ordinal() >= ConferenceState.Ending.ordinal());
        // assertEquals(ConferenceState.Ending,
        // conferenceCollection.get(confId).getConferenceState());

        // assert
        assertTrue("No disconnect for participant dialog", getInboundCall().waitForDisconnect(5000));
        assertTrue("Participant dialog failed to respond to disconnect", getInboundCall().respondToDisconnect());

        waitForParticipantTerminatedEvent(confId, dialogId);
        assertEquals(0, conferenceCollection.get(confId).getNumberOfActiveParticipants());

        assertParticipantState(ParticipantState.Terminated, confId, dialogId);

        waitForConferenceEndedEventWithTerminationCause(confId, ConferenceTerminationCause.EndedByApplication, false);
        assertEquals(ConferenceState.Ended, conferenceCollection.get(confId).getConferenceState());
        assertEquals(ConferenceTerminationCause.EndedByApplication, conferenceCollection.get(confId)
                .getConferenceTerminationCause());
        assertTrue(conferenceCollection.get(confId).getEndTime() > 0);
    }

    // Ending conference with 2 participants
    @Test
    public void testEndConferenceWith2Participants() throws Exception {
        // setup
        String confId = conferenceBean.createConference();

        // create first dialog
        String dialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());
        assertEquals(ConferenceInfo.TIME_NOT_SET, conferenceCollection.get(confId).getEndTime());

        // act
        conferenceBean.inviteParticipant(confId, dialogId);

        this.answerConferenceLeg(SipUnitPhone.Inbound);

        waitForConferenceActiveEvent(confId);
        waitForParticipantConnectedEvent(confId, dialogId);

        assertEquals("conference state not Active", ConferenceState.Active, conferenceCollection.get(confId)
                .getConferenceState());
        assertEquals(1, conferenceCollection.get(confId).getNumberOfActiveParticipants());
        assertTrue(conferenceCollection.get(confId).containsParticipant(
                callCollection.getCurrentCallForCallLeg(dialogId).getId()));

        // invite 2nd participant
        String secondDialogId = conferenceBean.createParticipantCallLeg(confId, getSecondInboundPhoneSipUri());
        conferenceBean.inviteParticipant(confId, secondDialogId);
        this.answerConferenceLeg(SipUnitPhone.SecondInbound);

        waitForParticipantConnectedEvent(confId, secondDialogId);
        assertEquals("conference state not Active", ConferenceState.Active, conferenceCollection.get(confId)
                .getConferenceState());
        assertEquals(2, conferenceCollection.get(confId).getNumberOfActiveParticipants());
        assertTrue(conferenceCollection.get(confId).containsParticipant(
                callCollection.getCurrentCallForCallLeg(secondDialogId).getId()));

        // end the conference
        Thread.sleep(1000);
        conferenceBean.endConference(confId);
        // assertEquals(ConferenceState.Ending,
        // conferenceCollection.get(confId).getConferenceState());
        assertTrue(conferenceCollection.get(confId).getConferenceState().ordinal() >= ConferenceState.Ending.ordinal());

        // assert 1st participant
        assertTrue("No disconnect for participant dialog", getInboundCall().waitForDisconnect(5000));
        assertTrue("Participant dialog failed to respond to disconnect", getInboundCall().respondToDisconnect());

        String lastDialogId = waitForParticipantTerminatedEvent(confId, new String[] { dialogId, secondDialogId });
        assertParticipantState(ParticipantState.Terminated, confId, lastDialogId);

        // assert 2nd participant
        assertTrue("No disconnect for participant dialog", getSecondInboundCall().waitForDisconnect(5000));
        assertTrue("Participant dialog failed to respond to disconnect", getSecondInboundCall().respondToDisconnect());

        lastDialogId = waitForParticipantTerminatedEvent(confId, new String[] { dialogId, secondDialogId });
        assertParticipantState(ParticipantState.Terminated, confId, lastDialogId);
        assertEquals(0, conferenceCollection.get(confId).getNumberOfActiveParticipants());

        // assert conference
        waitForConferenceEndedEventWithTerminationCause(confId, ConferenceTerminationCause.EndedByApplication, false);
        assertEquals(ConferenceState.Ended, conferenceCollection.get(confId).getConferenceState());
        assertEquals(ConferenceTerminationCause.EndedByApplication, conferenceCollection.get(confId)
                .getConferenceTerminationCause());
        assertTrue(conferenceCollection.get(confId).getEndTime() > 0);
    }

    // Invite a BUSY participant and check that the conference stays initial
    @Test
    public void testInviteBusyParticipantConfStaysInitial() throws Exception {
        // setup
        String confId = conferenceBean.createConference();

        // create first dialog
        String dialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());

        // act
        conferenceBean.inviteParticipant(confId, dialogId);

        this.busyConferenceLeg(getInboundCall(), getInboundPhoneSipUri());

        waitForParticipantFailedEvent(confId, dialogId);

        // assert
        assertEquals("conference state not Initial", ConferenceState.Initial, conferenceCollection.get(confId)
                .getConferenceState());
        assertEquals(0, conferenceCollection.get(confId).getNumberOfActiveParticipants());
        conferenceBean.endConference(confId);
    }

    // Invite a BUSY participant and then end the conference
    @Test
    public void testInviteBusyParticipantAndEndConference() throws Exception {
        // setup
        String confId = conferenceBean.createConference();

        // create first dialog
        String dialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());

        // act
        conferenceBean.inviteParticipant(confId, dialogId);

        this.busyConferenceLeg(getInboundCall(), getInboundPhoneSipUri());

        waitForParticipantFailedEvent(confId, dialogId);

        assertEquals("conference state not Initial", ConferenceState.Initial, conferenceCollection.get(confId)
                .getConferenceState());
        assertEquals(0, conferenceCollection.get(confId).getNumberOfActiveParticipants());

        // end the conference
        conferenceBean.endConference(confId);

        // assert conference
        waitForConferenceEndedEventWithTerminationCause(confId, ConferenceTerminationCause.EndedByApplication, true);
        assertEquals(ConferenceState.Ended, conferenceCollection.get(confId).getConferenceState());
        assertEquals(ConferenceTerminationCause.EndedByApplication, conferenceCollection.get(confId)
                .getConferenceTerminationCause());
    }

    @Test
    public void testGetConferenceInformation() throws Exception {
        // setup

        String confId = conferenceBean.createConference();

        // act
        ConferenceInformation confInformation = conferenceBean.getConferenceInformation(confId);

        // assert
        assertEquals(confId, confInformation.getId());
        assertEquals(ConferenceState.Initial, confInformation.getConferenceState());
        assertEquals(0, confInformation.getNumberOfActiveParticipants());
        assertEquals(0, confInformation.getNumberOfParticipants());
        Thread.sleep(50);
        assertTrue(confInformation.getCreateTime().getTimeInMillis() < Calendar.getInstance().getTimeInMillis());
        assertEquals(ConferenceInfo.TIME_NOT_SET, confInformation.getStartTime().getTimeInMillis());
        assertEquals(ConferenceInfo.TIME_NOT_SET, confInformation.getEndTime().getTimeInMillis());
    }

    // Test invite participant to ended conference
    @Test(expected = IllegalStateException.class)
    public void testInviteParticipantToEndedConference() throws Exception {
        // setup
        final String confId = conferenceBean.createConference();

        ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
            public void execute() {
                ConferenceInfo ci = conferenceCollection.get(confId);
                ci.updateConferenceState(ConferenceState.Ended);
                conferenceCollection.replace(ci);
            }

            public String getResourceId() {
                return confId;
            }

        };
        new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);

        // act
        // create dialog
        String secondDialogId = conferenceBean.createParticipantCallLeg(confId, getSecondInboundPhoneSipUri());
        conferenceBean.inviteParticipant(confId, secondDialogId);
    }

    // Test invite participant to non-existing conference
    @Test(expected = IllegalArgumentException.class)
    public void testInviteParticipantToNonExistingConference() throws Exception {
        // setup
        String confId = "xyz";
        String dialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());

        // act
        conferenceBean.inviteParticipant(confId, dialogId);
    }

    // test that conference without any participants can be ended
    @Test
    public void testEndConferenceWithoutParticipants() throws Exception {
        // setup
        String confId = conferenceBean.createConference();

        // act
        conferenceBean.endConference(confId);

        // assert
        waitForConferenceEndedEventWithTerminationCause(confId, ConferenceTerminationCause.EndedByApplication, true);
        assertEquals(ConferenceState.Ended, conferenceCollection.get(confId).getConferenceState());
        assertEquals(ConferenceTerminationCause.EndedByApplication, conferenceCollection.get(confId)
                .getConferenceTerminationCause());
        assertTrue(conferenceCollection.get(confId).getEndTime() > 0);
    }

    // Test end non-existing conference
    @Test(expected = IllegalArgumentException.class)
    public void testEndConferenceNonExisting() throws Exception {
        // act
        conferenceBean.endConference("xyz");
    }

    // tests that when onCallConnected is fired for a call that the conference
    // bean doesn't know about,
    // it won't throw an NPE
    @Test
    public void testCallConnectedNotRelatedToConference() {
        // act
        ((ConferenceBeanImpl) conferenceBean).onCallConnected(new CallConnectedEvent("aNewId"));

        // assert
        // no NPE should be thrown
    }

    private void createStubMaxConferenceDurationScheduler() {
        MaxConferenceDurationScheduler scheduler = new MaxConferenceDurationScheduler() {
            public void cancelTerminateConference(ConferenceInfo conferenceInfo) {
                scheduled = false;
            }

            public void terminateConferenceAfterMaxDuration(ConferenceInfo conferenceInfo, ConferenceBean conferenceBean) {
                conferenceInfo.setFuture(EasyMock.createNiceMock(ScheduledFuture.class));
                scheduled = true;
            }
        };
        ((ConferenceBeanImpl) conferenceBean).setMaxConferenceDurationScheduler(scheduler);
    }

    // Test that max conference duration scheduler is activated upon conference
    // activation
    @Test
    public void testMaxConferenceDurationSchedulerIsScheduledWhenConferenceBecomeActive() throws Exception {
        // setup
        String confId = conferenceBean.createConference(ConferenceInfo.DEFAULT_MAX_NUMBER_OF_PARTICIPANTS, 5);

        // create first dialog
        String dialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());

        createStubMaxConferenceDurationScheduler();

        // act
        conferenceBean.inviteParticipant(confId, dialogId);

        this.answerConferenceLeg(SipUnitPhone.Inbound);

        waitForConferenceActiveEvent(confId);
        waitForParticipantConnectedEvent(confId, dialogId);

        assertNotNull(conferenceCollection.get(confId).getFuture());
    }

    // Test that max conference duration scheduler is not activated upon attempt
    // to invite busy participant
    @Test
    public void testMaxConferenceDurationSchedulerIsNotScheduledWhenParticipantBusy() throws Exception {
        // setup
        String confId = conferenceBean.createConference(ConferenceInfo.DEFAULT_MAX_NUMBER_OF_PARTICIPANTS, 5);

        // create first dialog
        String dialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());

        // a bit of easymock
        MaxConferenceDurationScheduler scheduler = EasyMock.createMock(MaxConferenceDurationScheduler.class);
        EasyMock.replay(scheduler);
        ((ConferenceBeanImpl) conferenceBean).setMaxConferenceDurationScheduler(scheduler);

        // act
        conferenceBean.inviteParticipant(confId, dialogId);

        this.busyConferenceLeg(getInboundCall(), getInboundPhoneSipUri());

        waitForParticipantFailedEvent(confId, dialogId);

        // assert
        EasyMock.verify(scheduler);
    }

    // Test that max conference duration scheduler is not activated when max
    // duration is set to zero == infinitive
    @Test
    public void testMaxConferenceDurationSchedulerIsNotScheduledWhenMaxDurationIsZero() throws Exception {
        // setup
        String confId = conferenceBean.createConference(ConferenceInfo.DEFAULT_MAX_NUMBER_OF_PARTICIPANTS, 0);

        // create first dialog
        String dialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());

        // a bit of easymock
        MaxConferenceDurationScheduler scheduler = EasyMock.createMock(MaxConferenceDurationScheduler.class);
        EasyMock.replay(scheduler);
        ((ConferenceBeanImpl) conferenceBean).setMaxConferenceDurationScheduler(scheduler);

        // act
        conferenceBean.inviteParticipant(confId, dialogId);

        this.answerConferenceLeg(SipUnitPhone.Inbound);

        waitForConferenceActiveEvent(confId);
        waitForParticipantConnectedEvent(confId, dialogId);

        EasyMock.verify(scheduler);
    }

    // This tests that we deschedule a max conference duration scheduler if it
    // ends before the max duration
    @Test
    public void testMaxConferenceDurationSchedulerIsDescheduledWhenConferenceEnded() throws Exception {
        // setup
        String confId = conferenceBean.createConference(ConferenceInfo.DEFAULT_MAX_NUMBER_OF_PARTICIPANTS, 5);

        // create first dialog
        String dialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());

        createStubMaxConferenceDurationScheduler();

        conferenceBean.inviteParticipant(confId, dialogId);

        this.answerConferenceLeg(SipUnitPhone.Inbound);

        waitForConferenceActiveEvent(confId);
        waitForParticipantConnectedEvent(confId, dialogId);
        assertNotNull(conferenceCollection.get(confId).getFuture());

        // act
        Thread.sleep(1000);
        conferenceBean.endConference(confId);

        waitForParticipantTerminatedEvent(confId, dialogId);
        waitForConferenceEndedEventWithTerminationCause(confId, ConferenceTerminationCause.EndedByApplication, false);

        // assert
        assertFalse(scheduled);
    }

    // This tests that we deschedule a max conference duration scheduler when
    // last participant is terminated
    @Test
    public void testMaxConferenceDurationSchedulerIsDescheduledWhenLastParticipantTerminated() throws Exception {
        // setup
        String confId = conferenceBean.createConference(ConferenceInfo.DEFAULT_MAX_NUMBER_OF_PARTICIPANTS, 5);

        // create first dialog
        String dialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());

        createStubMaxConferenceDurationScheduler();

        conferenceBean.inviteParticipant(confId, dialogId);

        this.answerConferenceLeg(SipUnitPhone.Inbound);

        waitForConferenceActiveEvent(confId);
        waitForParticipantConnectedEvent(confId, dialogId);
        assertNotNull(conferenceCollection.get(confId).getFuture());

        // act
        Thread.sleep(1000);
        conferenceBean.terminateParticipant(confId, dialogId);

        waitForParticipantTerminatedEvent(confId, dialogId);
        waitForConferenceEndedEventWithTerminationCause(confId, ConferenceTerminationCause.LastParticipantTerminated,
                false);

        // assert
        assertFalse(scheduled);
    }

    // This tests that we deschedule a max conference duration scheduler when
    // last participant disconnects
    @Test
    public void testMaxConferenceDurationSchedulerIsDescheduledWhenLastParticipantDisconnected() throws Exception {
        // setup
        String confId = conferenceBean.createConference(ConferenceInfo.DEFAULT_MAX_NUMBER_OF_PARTICIPANTS, 5);

        // create first dialog
        String dialogId = conferenceBean.createParticipantCallLeg(confId, getInboundPhoneSipUri());

        createStubMaxConferenceDurationScheduler();

        conferenceBean.inviteParticipant(confId, dialogId);

        this.answerConferenceLeg(SipUnitPhone.Inbound);

        waitForConferenceActiveEvent(confId);
        waitForParticipantConnectedEvent(confId, dialogId);
        assertNotNull(conferenceCollection.get(confId).getFuture());

        // act
        getInboundCall().disconnect();

        waitForParticipantDisconnectedEvent(confId, dialogId);
        waitForConferenceEndedEventWithTerminationCause(confId, ConferenceTerminationCause.LastParticipantDisconnected,
                true);

        // assert
        assertFalse(scheduled);
    }

    // Testing setter for default max number of participants and that it is used
    // when created new conference
    @Test
    public void testSettingDefaultMaxNumberOfParticipants() throws Exception {
        // setup
        ((ConferenceBeanImpl) conferenceBean).setDefaultMaxNumberOfParticipants(5);
        // act
        String confId = conferenceBean.createConference();

        // assert
        assertEquals(5, conferenceCollection.get(confId).getMaxNumberOfParticipants());

        // reset the default
        ((ConferenceBeanImpl) conferenceBean)
                .setDefaultMaxNumberOfParticipants(ConferenceInfo.DEFAULT_MAX_NUMBER_OF_PARTICIPANTS);
    }

    // Testing that we cannot set default max number of participants to number
    // smaller than 2
    @Test(expected = IllegalArgumentException.class)
    public void testSettingDefaultMaxNumberOfParticipantsTooSmallNumber() throws Exception {
        // setup/act/assert
        ((ConferenceBeanImpl) conferenceBean).setDefaultMaxNumberOfParticipants(1);
    }

    // Testing setter for default max conf duration and that it is used when
    // created new conference
    @Test
    public void testSettingDefaultMaxConfDuration() throws Exception {
        // setup
        ((ConferenceBeanImpl) conferenceBean).setDefaultMaxDurationInMinutes(15);
        // act
        String confId = conferenceBean.createConference();

        // assert
        assertEquals(15, conferenceCollection.get(confId).getMaxDurationInMinutes());

        // reset the default
        ((ConferenceBeanImpl) conferenceBean)
                .setDefaultMaxDurationInMinutes(ConferenceInfo.DEFAULT_MAX_DURATION_IN_MINUTES);
    }

    // Testing that we cannot set default max conference duration to negative
    // number
    @Test(expected = IllegalArgumentException.class)
    public void testSettingDefaultMaxConfDurationNegative() throws Exception {
        // setup/act/assert
        ((ConferenceBeanImpl) conferenceBean).setDefaultMaxNumberOfParticipants(-1);
    }

    private void busyConferenceLeg(SipCall participantCall, URI participantUri) {
        assertTrue("Timed out waiting for incoming call", participantCall.waitForIncomingCall(5000));
        assertEquals(participantUri.toString(), participantCall.getLastReceivedRequest().getRequestURI());
        assertTrue(participantCall.listenForReinvite());

        participantCall.sendIncomingCallResponse(Response.BUSY_HERE, "Busy", 0);
    }

    private void answerConferenceLeg(SipUnitPhone sipUnitPhone) throws Exception {
        waitForCallSendTryingRingingOk(sipUnitPhone);
        waitForAckAssertMediaDescription(sipUnitPhone, ConvediaMockphoneBean.getOfferMediaDescription());
    }

    public void onConferenceActive(ConferenceActiveEvent conferenceActiveEvent) {
        conferenceActiveEventVector.add(conferenceActiveEvent);
        this.conferenceActiveEventVectorSemaphore.release();
    }

    public void onConferenceEnded(ConferenceEndedEvent conferenceEndedEvent) {
        conferenceEndedEventVector.add(conferenceEndedEvent);
        this.conferenceEndedEventVectorSemaphore.release();
    }

    public void onParticipantConnected(ParticipantConnectedEvent participantConnectedEvent) {
        participantConnectedEventVector.add(participantConnectedEvent);
        this.participantConnectedEventVectorSemaphore.release();
    }

    public void onParticipantTerminated(ParticipantTerminatedEvent participantTerminatedEvent) {
        participantTerminatedEventVector.add(participantTerminatedEvent);
        this.participantTerminatedEventVectorSemaphore.release();
    }

    public void onParticipantDisconnected(ParticipantDisconnectedEvent participantDisconnectedEvent) {
        participantDisconnectedEventVector.add(participantDisconnectedEvent);
        this.participantDisconnectedEventVectorSemaphore.release();
    }

    public void onParticipantFailed(ParticipantFailedEvent participantFailedEvent) {
        participantFailedEventVector.add(participantFailedEvent);
        this.participantFailedEventVectorSemaphore.release();
    }

    private void waitForConferenceActiveEvent(String confId) throws InterruptedException {
        assertTrue(this.conferenceActiveEventVectorSemaphore.tryAcquire(10, TimeUnit.SECONDS));
        assertTrue("No ConferenceActiveEvent", conferenceActiveEventVector.size() == 1);
        assertEquals("No matching conference Id", confId, conferenceActiveEventVector.get(0).getConferenceId());
    }

    private void waitForConferenceEndedEventWithTerminationCause(String confId,
            ConferenceTerminationCause conferenceTerminationCause, boolean zeroDuration) throws InterruptedException {
        assertTrue(this.conferenceEndedEventVectorSemaphore.tryAcquire(10, TimeUnit.SECONDS));
        assertTrue("No ConferenceEndedEvent", conferenceEndedEventVector.size() > 0);
        ConferenceEndedEvent event = findConferenceId(confId);
        assertEquals("Unexpected conferenceTerminationCause", conferenceTerminationCause, event
                .getConferenceTerminationCause());
        if (!zeroDuration)
            assertTrue("Conference duration zero", event.getDuration() > 0);
        else
            assertEquals("Conference duration non-zero", 0, event.getDuration());
    }

    private ConferenceEndedEvent findConferenceId(String id) {
        for (ConferenceEndedEvent event : conferenceEndedEventVector) {
            if (event.getConferenceId().equals(id))
                return event;
        }
        fail("No matching conference Id");
        return null;
    }

    private void waitForParticipantConnectedEvent(String confId, String dialogId) throws InterruptedException {
        assertTrue(this.participantConnectedEventVectorSemaphore.tryAcquire(10, TimeUnit.SECONDS));
        for (ParticipantConnectedEvent event : this.participantConnectedEventVector) {
            if (event.getConferenceId().equals(confId) && event.getDialogId().equals(dialogId))
                return;
        }
        fail("No matching ParticipantConnectedEvent");
    }

    private void waitForParticipantTerminatedEvent(String confId, String dialogId) throws InterruptedException {
        waitForParticipantTerminatedEvent(confId, new String[] { dialogId });
    }

    private String waitForParticipantTerminatedEvent(String confId, String[] dialogIds) throws InterruptedException {
        assertTrue(this.participantTerminatedEventVectorSemaphore.tryAcquire(10, TimeUnit.SECONDS));
        for (ParticipantTerminatedEvent event : this.participantTerminatedEventVector) {
            for (int i = 0; i < dialogIds.length; i++) {
                if (event.getConferenceId().equals(confId) && event.getDialogId().equals(dialogIds[i]))
                    return dialogIds[i];
            }
        }
        fail("No matching ParticipantTerminatedEvent");
        return null;
    }

    private void waitForParticipantDisconnectedEvent(String confId, String dialogId) throws InterruptedException {
        assertTrue("No matching ParticipantDisconnectedEvent", this.participantDisconnectedEventVectorSemaphore
                .tryAcquire(10, TimeUnit.SECONDS));
        for (ParticipantDisconnectedEvent event : this.participantDisconnectedEventVector) {
            if (event.getConferenceId().equals(confId) && event.getDialogId().equals(dialogId))
                return;
        }
        fail("No matching ParticipantDisconnectedEvent");
    }

    private void waitForParticipantFailedEvent(String confId, String dialogId) throws InterruptedException {
        assertTrue("No matching ParticipantFailedEvent", this.participantFailedEventVectorSemaphore.tryAcquire(10,
                TimeUnit.SECONDS));
        for (ParticipantFailedEvent event : this.participantFailedEventVector) {
            if (event.getConferenceId().equals(confId) && event.getDialogId().equals(dialogId))
                return;
        }
        fail("No matching ParticipantFailedEvent");
    }

    private void assertParticipantState(ParticipantState expected, String confId, String dialogId) {
        String callId = callCollection.getCurrentCallForCallLeg(dialogId).getId();
        assertEquals(expected, conferenceCollection.get(confId).getParticipants().get(callId));
    }
}
