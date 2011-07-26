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

import com.bt.aloha.media.event.call.CallAnnouncementCompletedEvent;
import com.bt.aloha.media.event.call.CallAnnouncementFailedEvent;
import com.bt.aloha.media.event.call.CallAnnouncementTerminatedEvent;
import com.bt.aloha.media.event.call.CallDtmfGenerationCompletedEvent;
import com.bt.aloha.media.event.call.CallDtmfGenerationFailedEvent;
import com.bt.aloha.media.event.call.CallPromptAndCollectDigitsCompletedEvent;
import com.bt.aloha.media.event.call.CallPromptAndCollectDigitsFailedEvent;
import com.bt.aloha.media.event.call.CallPromptAndCollectDigitsTerminatedEvent;
import com.bt.aloha.media.event.call.CallPromptAndRecordCompletedEvent;
import com.bt.aloha.media.event.call.CallPromptAndRecordFailedEvent;
import com.bt.aloha.media.event.call.CallPromptAndRecordTerminatedEvent;

/**
 * To listen to media call events you must implement this interface and then add class to the MediaCallBean listener list.
 */
public interface MediaCallListener {
    /**
     * Event fired when an announcement is completed
     * @param announcementCompletedEvent details of the event
     */
	void onCallAnnouncementCompleted(CallAnnouncementCompletedEvent announcementCompletedEvent);

    /**
     * Event fired when an announcement fails to complete
     * @param announcementFailedEvent details of the event
     */
	void onCallAnnouncementFailed(CallAnnouncementFailedEvent announcementFailedEvent);

    /**
     * Event fired when an announcement is terminated
     * @param announcementTerminatedEvent details of the event
     */
	void onCallAnnouncementTerminated(CallAnnouncementTerminatedEvent announcementTerminatedEvent);

    /**
     * Event fired when a prompt and collect is completed
     * @param dtmfCollectDigitsCompletedEvent details of the event
     */
    void onCallPromptAndCollectDigitsCompleted(CallPromptAndCollectDigitsCompletedEvent dtmfCollectDigitsCompletedEvent);

    /**
     * Event fired when a prompt and collect fails to complete
     * @param dtmfCollectDigitsFailedEvent details of the event
     */
    void onCallPromptAndCollectDigitsFailed(CallPromptAndCollectDigitsFailedEvent dtmfCollectDigitsFailedEvent);

    /**
     * Event fired when a prompt and collect is terminated
     * @param dtmfCollectDigitsTerminatedEvent details of the event
     */
    void onCallPromptAndCollectDigitsTerminated(CallPromptAndCollectDigitsTerminatedEvent dtmfCollectDigitsTerminatedEvent);

    /**
     * Event fired when a prompt and record is completed
     * @param callPromptAndRecordCompletedEvent details of the event
     */
    void onCallPromptAndRecordCompleted(CallPromptAndRecordCompletedEvent callPromptAndRecordCompletedEvent);

    /**
     * Event fired when a prompt and record fails to complete
     * @param callPromptAndRecordFailedEvent details of the event
     */
    void onCallPromptAndRecordFailed(CallPromptAndRecordFailedEvent callPromptAndRecordFailedEvent);

    /**
     * Event fired when a prompt and record is terminated
     * @param callPromptAndRecordTerminatedEvent details of the event
     */
    void onCallPromptAndRecordTerminated(CallPromptAndRecordTerminatedEvent callPromptAndRecordTerminatedEvent);

    /**
     * Event fired when a dtmf generation is completed
     * @param dtmfGenerationCompletedCompletedEvent details of the event
     */
    void onCallDtmfGenerationCompleted(CallDtmfGenerationCompletedEvent dtmfGenerationCompletedCompletedEvent);

    /**
     * Event fired when a dtmf generation fails to complete
     * @param dtmfGenerationFailedEvent details of the event
     */
    void onCallDtmfGenerationFailed(CallDtmfGenerationFailedEvent dtmfGenerationFailedEvent);
}
