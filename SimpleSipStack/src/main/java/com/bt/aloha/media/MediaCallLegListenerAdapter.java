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
package com.bt.aloha.media;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.callleg.CallLegListenerAdapter;
import com.bt.aloha.callleg.event.AbstractCallLegEvent;
import com.bt.aloha.dialog.event.AbstractDialogEvent;
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

public class MediaCallLegListenerAdapter extends CallLegListenerAdapter implements MediaDialogSipBeanListener {
	private static final String DELIVERING_S_EVENT_TO_S = "Delivering %s event to %s...";
    private static final String DELIVERED_S_EVENT_TO_S = "...delivered %s event to %s";
    private static final Log LOG = LogFactory.getLog(MediaCallLegListenerAdapter.class);

	public MediaCallLegListenerAdapter(MediaCallLegListener listener) {
		super(listener);
	}

	public void onDialogAnnouncementCompleted(DialogAnnouncementCompletedEvent announcementCompletedEvent) {
		LOG.debug(String.format(DELIVERING_S_EVENT_TO_S, announcementCompletedEvent.getClass().toString(), this.listener.getClass().toString()));
		((MediaCallLegListener)this.listener).onCallLegAnnouncementCompleted(new CallLegAnnouncementCompletedEvent(announcementCompletedEvent));
        LOG.debug(String.format(DELIVERED_S_EVENT_TO_S, announcementCompletedEvent.getClass().toString(), this.listener.getClass().toString()));
	}

	public void onDialogAnnouncementFailed(DialogAnnouncementFailedEvent announcementFailedEvent) {
		LOG.debug(String.format(DELIVERING_S_EVENT_TO_S, announcementFailedEvent.getClass().toString(), this.listener.getClass().toString()));
		((MediaCallLegListener)this.listener).onCallLegAnnouncementFailed(new CallLegAnnouncementFailedEvent(announcementFailedEvent));
        LOG.debug(String.format(DELIVERED_S_EVENT_TO_S, announcementFailedEvent.getClass().toString(), this.listener.getClass().toString()));
	}

	public void onDialogAnnouncementTerminated(DialogAnnouncementTerminatedEvent announcementTerminatedEvent) {
		LOG.debug(String.format(DELIVERING_S_EVENT_TO_S, announcementTerminatedEvent.getClass().toString(), this.listener.getClass().toString()));
		((MediaCallLegListener)this.listener).onCallLegAnnouncementTerminated(new CallLegAnnouncementTerminatedEvent(announcementTerminatedEvent));
        LOG.debug(String.format(DELIVERED_S_EVENT_TO_S, announcementTerminatedEvent.getClass().toString(), this.listener.getClass().toString()));
	}

	public void onDialogDtmfGenerationCompleted(DialogDtmfGenerationCompletedEvent dtmfGenerationCompletedCompletedEvent) {
		LOG.debug(String.format(DELIVERING_S_EVENT_TO_S, dtmfGenerationCompletedCompletedEvent.getClass().toString(), this.listener.getClass().toString()));
		((MediaCallLegListener)this.listener).onCallLegDtmfGenerationCompleted(new CallLegDtmfGenerationCompletedEvent(dtmfGenerationCompletedCompletedEvent));
        LOG.debug(String.format(DELIVERED_S_EVENT_TO_S, dtmfGenerationCompletedCompletedEvent.getClass().toString(), this.listener.getClass().toString()));
	}

	public void onDialogDtmfGenerationFailed(DialogDtmfGenerationFailedEvent dtmfGenerationFailedEvent) {
		LOG.debug(String.format(DELIVERING_S_EVENT_TO_S, dtmfGenerationFailedEvent.getClass().toString(), this.listener.getClass().toString()));
		((MediaCallLegListener)this.listener).onCallLegDtmfGenerationFailed(new CallLegDtmfGenerationFailedEvent(dtmfGenerationFailedEvent));
        LOG.debug(String.format(DELIVERED_S_EVENT_TO_S, dtmfGenerationFailedEvent.getClass().toString(), this.listener.getClass().toString()));
	}

	public void onDialogPromptAndCollectDigitsCompleted(DialogPromptAndCollectDigitsCompletedEvent dtmfCollectDigitsCompletedEvent) {
		LOG.debug(String.format(DELIVERING_S_EVENT_TO_S, dtmfCollectDigitsCompletedEvent.getClass().toString(), this.listener.getClass().toString()));
		((MediaCallLegListener)this.listener).onCallLegPromptAndCollectDigitsCompleted(new CallLegPromptAndCollectDigitsCompletedEvent(dtmfCollectDigitsCompletedEvent));
        LOG.debug(String.format(DELIVERED_S_EVENT_TO_S, dtmfCollectDigitsCompletedEvent.getClass().toString(), this.listener.getClass().toString()));
	}

	public void onDialogPromptAndCollectDigitsFailed(DialogPromptAndCollectDigitsFailedEvent dtmfCollectDigitsFailedEvent) {
		LOG.debug(String.format(DELIVERING_S_EVENT_TO_S, dtmfCollectDigitsFailedEvent.getClass().toString(), this.listener.getClass().toString()));
		((MediaCallLegListener)this.listener).onCallLegPromptAndCollectDigitsFailed(new CallLegPromptAndCollectDigitsFailedEvent(dtmfCollectDigitsFailedEvent));
        LOG.debug(String.format(DELIVERED_S_EVENT_TO_S, dtmfCollectDigitsFailedEvent.getClass().toString(), this.listener.getClass().toString()));
	}

	public void onDialogPromptAndCollectDigitsTerminated(DialogPromptAndCollectDigitsTerminatedEvent dtmfCollectDigitsTerminatedEvent) {
		LOG.debug(String.format(DELIVERING_S_EVENT_TO_S, dtmfCollectDigitsTerminatedEvent.getClass().toString(), this.listener.getClass().toString()));
		((MediaCallLegListener)this.listener).onCallLegPromptAndCollectDigitsTerminated(new CallLegPromptAndCollectDigitsTerminatedEvent(dtmfCollectDigitsTerminatedEvent));
        LOG.debug(String.format(DELIVERED_S_EVENT_TO_S, dtmfCollectDigitsTerminatedEvent.getClass().toString(), this.listener.getClass().toString()));
	}

    public void onDialogPromptAndRecordCompleted(DialogPromptAndRecordCompletedEvent dialogPromptAndRecordCompletedEvent) {
        LOG.debug(String.format(DELIVERING_S_EVENT_TO_S, dialogPromptAndRecordCompletedEvent.getClass().toString(), this.listener.getClass().toString()));
        ((MediaCallLegListener)this.listener).onCallLegPromptAndRecordCompleted(new CallLegPromptAndRecordCompletedEvent(dialogPromptAndRecordCompletedEvent));
        LOG.debug(String.format(DELIVERED_S_EVENT_TO_S, dialogPromptAndRecordCompletedEvent.getClass().toString(), this.listener.getClass().toString()));
    }

    public void onDialogPromptAndRecordFailed(DialogPromptAndRecordFailedEvent dialogPromptAndRecordFailedEvent) {
        LOG.debug(String.format(DELIVERING_S_EVENT_TO_S, dialogPromptAndRecordFailedEvent.getClass().toString(), this.listener.getClass().toString()));
        ((MediaCallLegListener)this.listener).onCallLegPromptAndRecordFailed(new CallLegPromptAndRecordFailedEvent(dialogPromptAndRecordFailedEvent));
        LOG.debug(String.format(DELIVERED_S_EVENT_TO_S, dialogPromptAndRecordFailedEvent.getClass().toString(), this.listener.getClass().toString()));
    }

    public void onDialogPromptAndRecordTerminated(DialogPromptAndRecordTerminatedEvent dialogPromptAndRecordTerminatedEvent) {
        LOG.debug(String.format(DELIVERING_S_EVENT_TO_S, dialogPromptAndRecordTerminatedEvent.getClass().toString(), this.listener.getClass().toString()));
        ((MediaCallLegListener)this.listener).onCallLegPromptAndRecordTerminated(new CallLegPromptAndRecordTerminatedEvent(dialogPromptAndRecordTerminatedEvent));
        LOG.debug(String.format(DELIVERED_S_EVENT_TO_S, dialogPromptAndRecordTerminatedEvent.getClass().toString(), this.listener.getClass().toString()));
    }

	@Override
	protected AbstractCallLegEvent mapDialogEventToCallLegEvent(AbstractDialogEvent event) {
		if (event instanceof DialogAnnouncementCompletedEvent)
			return new CallLegAnnouncementCompletedEvent((DialogAnnouncementCompletedEvent)event);
		if (event instanceof DialogAnnouncementFailedEvent)
			return new CallLegAnnouncementFailedEvent((DialogAnnouncementFailedEvent)event);
		if (event instanceof DialogAnnouncementTerminatedEvent)
			return new CallLegAnnouncementTerminatedEvent((DialogAnnouncementTerminatedEvent)event);
		if (event instanceof DialogDtmfGenerationCompletedEvent)
			return new CallLegDtmfGenerationCompletedEvent((DialogDtmfGenerationCompletedEvent)event);
		if (event instanceof DialogDtmfGenerationFailedEvent)
			return new CallLegDtmfGenerationFailedEvent((DialogDtmfGenerationFailedEvent)event);
		if (event instanceof DialogPromptAndCollectDigitsCompletedEvent)
			return new CallLegPromptAndCollectDigitsCompletedEvent((DialogPromptAndCollectDigitsCompletedEvent)event);
		if (event instanceof DialogPromptAndCollectDigitsFailedEvent)
			return new CallLegPromptAndCollectDigitsFailedEvent((DialogPromptAndCollectDigitsFailedEvent)event);
		if (event instanceof DialogPromptAndCollectDigitsTerminatedEvent)
			return new CallLegPromptAndCollectDigitsTerminatedEvent((DialogPromptAndCollectDigitsTerminatedEvent)event);
        if (event instanceof DialogPromptAndRecordCompletedEvent)
            return new CallLegPromptAndRecordCompletedEvent((DialogPromptAndRecordCompletedEvent)event);
        if (event instanceof DialogPromptAndRecordFailedEvent)
            return new CallLegPromptAndRecordFailedEvent((DialogPromptAndRecordFailedEvent)event);
        if (event instanceof DialogPromptAndRecordTerminatedEvent)
            return new CallLegPromptAndRecordTerminatedEvent((DialogPromptAndRecordTerminatedEvent)event);
		return super.mapDialogEventToCallLegEvent(event);
	}


}
