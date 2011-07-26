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
package com.bt.aloha.media.convedia;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

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


public class MediaCallLegBeanTest {
	private MediaCallLegListener listener;

	@Before
	public void before() {
		listener = new MediaCallLegListener() {
			public void onCallLegAnnouncementCompleted(CallLegAnnouncementCompletedEvent announcementCompletedEvent) {}
			public void onCallLegAnnouncementFailed(CallLegAnnouncementFailedEvent announcementFailedEvent) {}
			public void onCallLegAnnouncementTerminated(CallLegAnnouncementTerminatedEvent announcementTerminatedEvent) {}
			public void onCallLegDtmfGenerationCompleted(CallLegDtmfGenerationCompletedEvent dtmfGenerationCompletedCompletedEvent) {}
			public void onCallLegDtmfGenerationFailed(CallLegDtmfGenerationFailedEvent dtmfGenerationFailedEvent) {}
			public void onCallLegPromptAndCollectDigitsCompleted(CallLegPromptAndCollectDigitsCompletedEvent dtmfCollectDigitsCompletedEvent) {}
			public void onCallLegPromptAndCollectDigitsFailed(CallLegPromptAndCollectDigitsFailedEvent dtmfCollectDigitsFailedEvent) {}
			public void onCallLegPromptAndCollectDigitsTerminated(CallLegPromptAndCollectDigitsTerminatedEvent dtmfCollectDigitsTerminatedEvent) {}
			public void onCallLegConnected(CallLegConnectedEvent connectedEvent) {}
			public void onCallLegConnectionFailed(CallLegConnectionFailedEvent connectionFailedEvent) {}
			public void onCallLegDisconnected(CallLegDisconnectedEvent disconnectedEvent) {}
			public void onCallLegRefreshCompleted(CallLegRefreshCompletedEvent callLegConnectedEvent) {}
			public void onCallLegTerminated(CallLegTerminatedEvent terminatedEvent) {}
			public void onCallLegTerminationFailed(CallLegTerminationFailedEvent terminationFailedEvent) {}
			public void onReceivedCallLegRefresh(ReceivedCallLegRefreshEvent receivedCallLegRefreshEvent) {}
            public void onCallLegPromptAndRecordCompleted(CallLegPromptAndRecordCompletedEvent dtmfCollectDigitsCompletedEvent) {}
            public void onCallLegPromptAndRecordFailed(CallLegPromptAndRecordFailedEvent dtmfCollectDigitsFailedEvent) {}
            public void onCallLegPromptAndRecordTerminated(CallLegPromptAndRecordTerminatedEvent dtmfCollectDigitsTerminatedEvent) {}
		};
	}

	/**
	 * Tests that we can add the listener
	 */
	@Test
	public void addListener() {
		// setup
		MediaCallLegBeanImpl bean = new MediaCallLegBeanImpl();

		// act
		bean.addMediaCallLegListener(listener);

		// assert
		assertEquals(1, bean.getDialogListeners().size());
		assertEquals(bean.getDialogListeners().get(0), listener);
	}

	/**
	 * Tests that we can remove the listener
	 */
	@Test
	public void removeListener() {
		MediaCallLegBeanImpl bean = new MediaCallLegBeanImpl();
		bean.addMediaCallLegListener(listener);

		// act
		bean.removeMediaCallLegListener(listener);

		// assert
		assertEquals(0, bean.getDialogListeners().size());
	}
}
