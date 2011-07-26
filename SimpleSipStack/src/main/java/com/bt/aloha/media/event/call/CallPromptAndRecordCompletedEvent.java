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

 	

 	
 	
 
package com.bt.aloha.media.event.call;

/**
 * Event fired when an Prompt and Record has completed
 */
public class CallPromptAndRecordCompletedEvent extends AbstractMediaCallCommandEvent {
    private String audioFileUri;
    private int recordingLengthMillis;
    private String recordResult;

    public CallPromptAndRecordCompletedEvent(String aCallId, String aMediaCommandId, String anAudioFileUri, String aRecordResult, int aRecordingLengthMillis) {
        super(aCallId, aMediaCommandId);
        this.audioFileUri = anAudioFileUri;
        this.recordingLengthMillis = aRecordingLengthMillis;
        this.recordResult = aRecordResult;
    }

    public String getAudioFileUri() {
        return audioFileUri;
    }

    public int getRecordingLengthMillis() {
        return recordingLengthMillis;
    }

    public Object getRecordResult() {
        return recordResult;
    }
}
