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

 	

 	
 	
 
package com.bt.aloha.media.convedia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.sip.ResponseEvent;
import javax.sip.SipFactory;
import javax.sip.message.Response;

import org.easymock.classextension.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.callleg.event.AbstractCallLegEvent;
import com.bt.aloha.callleg.event.CallLegConnectedEvent;
import com.bt.aloha.callleg.event.CallLegConnectionFailedEvent;
import com.bt.aloha.callleg.event.CallLegDisconnectedEvent;
import com.bt.aloha.callleg.event.CallLegRefreshCompletedEvent;
import com.bt.aloha.callleg.event.CallLegTerminatedEvent;
import com.bt.aloha.callleg.event.CallLegTerminationFailedEvent;
import com.bt.aloha.callleg.event.ReceivedCallLegRefreshEvent;
import com.bt.aloha.media.MediaCallLegListener;
import com.bt.aloha.media.event.callleg.CallLegAnnouncementCompletedEvent;
import com.bt.aloha.media.event.callleg.CallLegAnnouncementFailedEvent;
import com.bt.aloha.media.event.callleg.CallLegAnnouncementTerminatedEvent;
import com.bt.aloha.media.event.callleg.CallLegDtmfGenerationCompletedEvent;
import com.bt.aloha.media.event.callleg.CallLegDtmfGenerationFailedEvent;
import com.bt.aloha.media.event.callleg.CallLegPromptAndCollectDigitsCompletedEvent;
import com.bt.aloha.media.event.callleg.CallLegPromptAndCollectDigitsFailedEvent;
import com.bt.aloha.media.event.callleg.CallLegPromptAndCollectDigitsTerminatedEvent;
import com.bt.aloha.media.event.callleg.CallLegPromptAndRecordCompletedEvent;
import com.bt.aloha.media.event.callleg.CallLegPromptAndRecordFailedEvent;
import com.bt.aloha.media.event.callleg.CallLegPromptAndRecordTerminatedEvent;

public class MediaDialogSipBeanImplTest extends ConvediaMediaPerClassTestCase implements MediaCallLegListener {

	private List<AbstractCallLegEvent> receivedCallLegEvents;
    private Semaphore receivedCallLegDisconnectedSemaphore;

	@Before
	public void setup(){
		receivedCallLegEvents = new Vector<AbstractCallLegEvent>();
        receivedCallLegDisconnectedSemaphore = new Semaphore(0);
        mediaCallLegBean = (MediaCallLegBeanImpl)getApplicationContext().getBean("mediaCallLegBean");
        mediaCallLegBean.addMediaCallLegListener(this);
	}

    @After
    public void after() {
        mediaCallLegBean.removeMediaCallLegListener(this);
    }

	@Test
	public void testInfoOKTriggersNoEvent() throws Exception {
		// setup
		String callLegId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
		String mediaCallLegId = mediaCallLegBean.createMediaCallLeg(callLegId);

		Response response = SipFactory.getInstance().createMessageFactory().createResponse(createInfoOkResponseString(mediaCallLegId));
		ResponseEvent responseEvent = EasyMock.createNiceMock(ResponseEvent.class);
        EasyMock.expect(responseEvent.getResponse()).andStubReturn(response);
		EasyMock.replay(responseEvent);

		// act
		((MediaCallLegBeanImpl)mediaCallLegBean).processResponse(responseEvent, dialogCollection.get(mediaCallLegId));
		// assert
		assertEquals(0, receivedCallLegEvents.size());
	}

    @Test
	public void testInfo481TriggersDialogDisconnectedEvent() throws Exception {
		// setup
		final String dialogId = outboundCallLegBean.createCallLeg(getSecondInboundPhoneSipUri(), getInboundPhoneSipUri(), 0);
		Response response = SipFactory.getInstance().createMessageFactory().createResponse(createInfo481ResponseString(dialogId));
		ResponseEvent responseEvent = EasyMock.createNiceMock(ResponseEvent.class);
        EasyMock.expect(responseEvent.getResponse()).andStubReturn(response);
		EasyMock.replay(responseEvent);
		// act
		((MediaCallLegBeanImpl)mediaCallLegBean).processResponse(responseEvent, dialogCollection.get(dialogId));

		// assert
        receivedCallLegDisconnectedSemaphore.tryAcquire(10, TimeUnit.SECONDS);
		assertEquals(1, receivedCallLegEvents.size());
		assertTrue(receivedCallLegEvents.get(0) instanceof CallLegDisconnectedEvent);
	}

    public void onCallLegAnnouncementCompleted(CallLegAnnouncementCompletedEvent arg0) {
        this.receivedCallLegEvents.add(arg0);
    }

    public void onCallLegAnnouncementFailed(CallLegAnnouncementFailedEvent arg0) {
        this.receivedCallLegEvents.add(arg0);
    }

    public void onCallLegAnnouncementTerminated(CallLegAnnouncementTerminatedEvent arg0) {
        this.receivedCallLegEvents.add(arg0);
    }

    public void onCallLegDtmfGenerationCompleted(CallLegDtmfGenerationCompletedEvent arg0) {
        this.receivedCallLegEvents.add(arg0);
    }

    public void onCallLegDtmfGenerationFailed(CallLegDtmfGenerationFailedEvent arg0) {
        this.receivedCallLegEvents.add(arg0);
    }

    public void onCallLegPromptAndCollectDigitsCompleted(CallLegPromptAndCollectDigitsCompletedEvent arg0) {
        this.receivedCallLegEvents.add(arg0);
    }

    public void onCallLegPromptAndCollectDigitsFailed(CallLegPromptAndCollectDigitsFailedEvent arg0) {
        this.receivedCallLegEvents.add(arg0);
    }

    public void onCallLegPromptAndCollectDigitsTerminated(CallLegPromptAndCollectDigitsTerminatedEvent arg0) {
        this.receivedCallLegEvents.add(arg0);
    }

    public void onCallLegPromptAndRecordCompleted(CallLegPromptAndRecordCompletedEvent arg0) {
        this.receivedCallLegEvents.add(arg0);
    }

    public void onCallLegPromptAndRecordFailed(CallLegPromptAndRecordFailedEvent arg0) {
        this.receivedCallLegEvents.add(arg0);
    }

    public void onCallLegPromptAndRecordTerminated(CallLegPromptAndRecordTerminatedEvent arg0) {
        this.receivedCallLegEvents.add(arg0);
    }

    public void onCallLegConnected(CallLegConnectedEvent arg0) {
        this.receivedCallLegEvents.add(arg0);
    }

    public void onCallLegConnectionFailed(CallLegConnectionFailedEvent arg0) {
        this.receivedCallLegEvents.add(arg0);
    }

    public void onCallLegDisconnected(CallLegDisconnectedEvent arg0) {
        this.receivedCallLegEvents.add(arg0);
        receivedCallLegDisconnectedSemaphore.release();
    }

    public void onCallLegRefreshCompleted(CallLegRefreshCompletedEvent arg0) {
        this.receivedCallLegEvents.add(arg0);
    }

    public void onCallLegTerminated(CallLegTerminatedEvent arg0) {
        this.receivedCallLegEvents.add(arg0);
    }

    public void onCallLegTerminationFailed(CallLegTerminationFailedEvent arg0) {
        this.receivedCallLegEvents.add(arg0);
    }

    public void onReceivedCallLegRefresh(ReceivedCallLegRefreshEvent arg0) {
        this.receivedCallLegEvents.add(arg0);
    }
}
