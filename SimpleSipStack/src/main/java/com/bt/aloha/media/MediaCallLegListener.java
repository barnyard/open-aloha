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

import com.bt.aloha.callleg.CallLegListener;
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


public interface MediaCallLegListener extends CallLegListener {
	void onCallLegAnnouncementCompleted(CallLegAnnouncementCompletedEvent announcementCompletedEvent);
	void onCallLegAnnouncementFailed(CallLegAnnouncementFailedEvent announcementFailedEvent);
	void onCallLegAnnouncementTerminated(CallLegAnnouncementTerminatedEvent announcementTerminatedEvent);
	void onCallLegPromptAndCollectDigitsCompleted(CallLegPromptAndCollectDigitsCompletedEvent dtmfCollectDigitsCompletedEvent);
    void onCallLegPromptAndCollectDigitsFailed(CallLegPromptAndCollectDigitsFailedEvent dtmfCollectDigitsFailedEvent);
    void onCallLegPromptAndCollectDigitsTerminated(CallLegPromptAndCollectDigitsTerminatedEvent dtmfCollectDigitsTerminatedEvent);
    void onCallLegPromptAndRecordCompleted(CallLegPromptAndRecordCompletedEvent callLegPromptAndRecordCompletedEvent);
    void onCallLegPromptAndRecordFailed(CallLegPromptAndRecordFailedEvent callLegPromptAndRecordFailedEvent);
    void onCallLegPromptAndRecordTerminated(CallLegPromptAndRecordTerminatedEvent callLegPromptAndRecordTerminatedEvent);
    void onCallLegDtmfGenerationCompleted(CallLegDtmfGenerationCompletedEvent dtmfGenerationCompletedCompletedEvent);
    void onCallLegDtmfGenerationFailed(CallLegDtmfGenerationFailedEvent dtmfGenerationFailedEvent);
}
