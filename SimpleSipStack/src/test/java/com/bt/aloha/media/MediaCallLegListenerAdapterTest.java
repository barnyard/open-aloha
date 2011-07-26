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

 	

 	
 	
 
package com.bt.aloha.media;

import static org.junit.Assert.assertTrue;

import org.easymock.classextension.EasyMock;
import org.junit.Test;

import com.bt.aloha.callleg.event.CallLegConnectedEvent;
import com.bt.aloha.dialog.event.AbstractDialogEvent;
import com.bt.aloha.dialog.event.DialogConnectedEvent;
import com.bt.aloha.media.MediaCallLegListener;
import com.bt.aloha.media.MediaCallLegListenerAdapter;
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
import com.bt.aloha.media.event.dialog.DialogAnnouncementCompletedEvent;
import com.bt.aloha.media.event.dialog.DialogAnnouncementFailedEvent;
import com.bt.aloha.media.event.dialog.DialogAnnouncementTerminatedEvent;
import com.bt.aloha.media.event.dialog.DialogDtmfGenerationCompletedEvent;
import com.bt.aloha.media.event.dialog.DialogDtmfGenerationFailedEvent;
import com.bt.aloha.media.event.dialog.DialogPromptAndCollectDigitsCompletedEvent;
import com.bt.aloha.media.event.dialog.DialogPromptAndCollectDigitsFailedEvent;
import com.bt.aloha.media.event.dialog.DialogPromptAndCollectDigitsTerminatedEvent;
import com.bt.aloha.media.event.dialog.DialogPromptAndRecordCompletedEvent;
import com.bt.aloha.media.event.dialog.DialogPromptAndRecordFailedEvent;
import com.bt.aloha.media.event.dialog.DialogPromptAndRecordTerminatedEvent;


public class MediaCallLegListenerAdapterTest {
	@Test
	public void testEventDelivery() {
		// setup
		MediaCallLegListener listener = EasyMock.createMock(MediaCallLegListener.class);
		listener.onCallLegAnnouncementCompleted(EasyMock.isA(CallLegAnnouncementCompletedEvent.class));
		listener.onCallLegAnnouncementFailed(EasyMock.isA(CallLegAnnouncementFailedEvent.class));
		listener.onCallLegAnnouncementTerminated(EasyMock.isA(CallLegAnnouncementTerminatedEvent.class));
		listener.onCallLegDtmfGenerationCompleted(EasyMock.isA(CallLegDtmfGenerationCompletedEvent.class));
		listener.onCallLegDtmfGenerationFailed(EasyMock.isA(CallLegDtmfGenerationFailedEvent.class));
		listener.onCallLegPromptAndCollectDigitsCompleted(EasyMock.isA(CallLegPromptAndCollectDigitsCompletedEvent.class));
		listener.onCallLegPromptAndCollectDigitsFailed(EasyMock.isA(CallLegPromptAndCollectDigitsFailedEvent.class));
		listener.onCallLegPromptAndCollectDigitsTerminated(EasyMock.isA(CallLegPromptAndCollectDigitsTerminatedEvent.class));
        listener.onCallLegPromptAndRecordCompleted(EasyMock.isA(CallLegPromptAndRecordCompletedEvent.class));
        listener.onCallLegPromptAndRecordFailed(EasyMock.isA(CallLegPromptAndRecordFailedEvent.class));
        listener.onCallLegPromptAndRecordTerminated(EasyMock.isA(CallLegPromptAndRecordTerminatedEvent.class));
		EasyMock.replay(listener);

		// act
		MediaCallLegListenerAdapter adapter = new MediaCallLegListenerAdapter(listener);
		adapter.onDialogAnnouncementCompleted(new DialogAnnouncementCompletedEvent("a", "b", "3", false));
		adapter.onDialogAnnouncementFailed(new DialogAnnouncementFailedEvent("a", "b"));
		adapter.onDialogAnnouncementTerminated(new DialogAnnouncementTerminatedEvent("a", "b"));
		adapter.onDialogDtmfGenerationCompleted(new DialogDtmfGenerationCompletedEvent("a", "b"));
		adapter.onDialogDtmfGenerationFailed(new DialogDtmfGenerationFailedEvent("a", "b"));
		adapter.onDialogPromptAndCollectDigitsCompleted(new DialogPromptAndCollectDigitsCompletedEvent("a", "b", "3", "d"));
		adapter.onDialogPromptAndCollectDigitsFailed(new DialogPromptAndCollectDigitsFailedEvent("a", "b", "3", "d"));
		adapter.onDialogPromptAndCollectDigitsTerminated(new DialogPromptAndCollectDigitsTerminatedEvent("a", "b", "3", "d"));
        adapter.onDialogPromptAndRecordCompleted(new DialogPromptAndRecordCompletedEvent("a", "b", "3", "d", "c"));
        adapter.onDialogPromptAndRecordFailed(new DialogPromptAndRecordFailedEvent("a", "b", "c"));
        adapter.onDialogPromptAndRecordTerminated(new DialogPromptAndRecordTerminatedEvent("a", "b", "c"));

		// assert
		EasyMock.verify(listener);
	}

	@Test
	public void testMapper() throws Exception {
		// setup
		MediaCallLegListenerAdapter adapter = new MediaCallLegListenerAdapter(EasyMock.createNiceMock(MediaCallLegListener.class));

		// act and assert
		assertTrue(adapter.mapDialogEventToCallLegEvent(EasyMock.createMock(DialogAnnouncementCompletedEvent.class)) instanceof CallLegAnnouncementCompletedEvent);
		assertTrue(adapter.mapDialogEventToCallLegEvent(EasyMock.createMock(DialogAnnouncementFailedEvent.class)) instanceof CallLegAnnouncementFailedEvent);
		assertTrue(adapter.mapDialogEventToCallLegEvent(EasyMock.createMock(DialogAnnouncementTerminatedEvent.class)) instanceof CallLegAnnouncementTerminatedEvent);
		assertTrue(adapter.mapDialogEventToCallLegEvent(EasyMock.createMock(DialogDtmfGenerationCompletedEvent.class)) instanceof CallLegDtmfGenerationCompletedEvent);
		assertTrue(adapter.mapDialogEventToCallLegEvent(EasyMock.createMock(DialogDtmfGenerationFailedEvent.class)) instanceof CallLegDtmfGenerationFailedEvent);
		assertTrue(adapter.mapDialogEventToCallLegEvent(EasyMock.createMock(DialogPromptAndCollectDigitsCompletedEvent.class)) instanceof CallLegPromptAndCollectDigitsCompletedEvent);
		assertTrue(adapter.mapDialogEventToCallLegEvent(EasyMock.createMock(DialogPromptAndCollectDigitsFailedEvent.class)) instanceof CallLegPromptAndCollectDigitsFailedEvent);
		assertTrue(adapter.mapDialogEventToCallLegEvent(EasyMock.createMock(DialogPromptAndCollectDigitsTerminatedEvent.class)) instanceof CallLegPromptAndCollectDigitsTerminatedEvent);

        assertTrue(adapter.mapDialogEventToCallLegEvent(EasyMock.createMock(DialogPromptAndRecordCompletedEvent.class)) instanceof CallLegPromptAndRecordCompletedEvent);
        assertTrue(adapter.mapDialogEventToCallLegEvent(EasyMock.createMock(DialogPromptAndRecordFailedEvent.class)) instanceof CallLegPromptAndRecordFailedEvent);
        assertTrue(adapter.mapDialogEventToCallLegEvent(EasyMock.createMock(DialogPromptAndRecordTerminatedEvent.class)) instanceof CallLegPromptAndRecordTerminatedEvent);

        //test that superclass is called
        assertTrue(adapter.mapDialogEventToCallLegEvent(EasyMock.createMock(DialogConnectedEvent.class)) instanceof CallLegConnectedEvent);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testMapperBadObject() throws Exception {
		// setup
		MediaCallLegListenerAdapter adapter = new MediaCallLegListenerAdapter(null);

		// act and assert
		adapter.mapDialogEventToCallLegEvent(new AbstractDialogEvent(null) {});
	}
}
