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

import com.bt.aloha.call.CallInformation;
import com.bt.aloha.callleg.AutoTerminateAction;

/**
 * Spring Bean to create and manage media calls.
 */
public interface MediaCallBean {
    /**
     * create a media call
     * @param callLegId the call leg ID
     * @return the media call ID
     */
	String createMediaCall(String callLegId);

    /**
     * create a media call specifying whether to auto-terminate the dialog after the call is ended
     * @param callLegId the call leg ID
     * @param autoTerminateCallLegs whether to auto-terminate the dialog after the call is ended
     * @return the media call ID
     */
	String createMediaCall(String callLegId, AutoTerminateAction autoTerminateCallLegs);

    /**
     * Play an announcement
     * @param mediaCallId the media call ID
     * @param audioFileUri the URI of the media file
     * @return a command ID
     */
	String playAnnouncement(String mediaCallId, String audioFileUri);

    /**
     * Play an announcement multiple times
     * @param mediaCallId the media call ID
     * @param audioFileUri the URI of the media file
     * @param iterations the number of times to play the announcement (-1 = infinite)
     * @return a command ID
     */
	String playAnnouncement(String mediaCallId, String audioFileUri, int iterations);

    /**
     * Play an announcement multiple times
     * @param mediaCallId the media call ID
     * @param audioFileUri the URI of the media file
     * @param iterations the number of times to play the announcement (-1 = infinite)
     * @param intervalMs the interval in milliseconds between plays
     * @return a command ID
     */
	String playAnnouncement(String mediaCallId, String audioFileUri, int iterations, int intervalMs);

    /**
     * Play an announcement multiple times
     * @param mediaCallId the media call ID
     * @param audioFileUri the URI of the media file
     * @param iterations the number of times to play the announcement (-1 = infinite)
     * @param intervalMs the interval in milliseconds between plays
     * @param barge whether barging of an announcement is allowed
     * @return a command ID
     */
	String playAnnouncement(String mediaCallId, String audioFileUri, int iterations, int intervalMs, boolean barge);

    /**
     * Prompt and collect digits
     * @param mediaCallId the media call ID
     * @param dtmfCollectCommand details of the prompt and collect command
     * @return the command ID
     */
	String promptAndCollectDigits(String mediaCallId, DtmfCollectCommand dtmfCollectCommand);

    /**
     * Prompt and record
     * @param mediaCallId the media call ID
     * @param promptAndRecordCommand details of the prompt and record command
     * @return the command ID
     */
    String promptAndRecord(String mediaCallId, PromptAndRecordCommand promptAndRecordCommand);

	/**
	 * Generate digits to send
	 * @param mediaCallId the media call ID
	 * @param digits the digits to send
	 * @return the command ID
	 */
	String generateDtmfDigits(String mediaCallId, String digits);

	/**
	 * Generate digits to send
	 * @param mediaCallId the media call ID
	 * @param digits the digits to send
	 * @param digitLengthMilliseconds the length of the DTMF tones in milli-seconds
	 * @return the command ID
	 */
	String generateDtmfDigits(String mediaCallId, String digits, int digitLengthMilliseconds);

    /**
     * Cancel a media command
     * @param mediaCallId the media call ID
     * @param mediaCommandId the command ID to cancel
     */
	void cancelMediaCommand(String mediaCallId, String mediaCommandId);

    /**
     * Terminate a media call
     * @param mediaCallId the media call ID
     */
	void terminateMediaCall(String mediaCallId);

	/**
     * Get information for a media call
     * @param mediaCallId the media call ID
     */
	CallInformation getCallInformation(String mediaCallId);

	/**
     * Add a media call listener to the MediaCallBean
     * @param aCallListener the MediaCallListener to add
	 */
    void addMediaCallListener(MediaCallListener aCallListener);

    /**
     * Remove a media call listener from the MediaCallBean
     * @param aCallListener the MediaCallListener to remove
     */
	void removeMediaCallListener(MediaCallListener aCallListener);
}
