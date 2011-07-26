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

// TODO do we even need this?  It's nearly identically exposed in MediaCallBean
public interface MediaCallLegBean {
	String createMediaCallLeg(String targetCallLegId);
	String createMediaCallLeg(String targetCallLegId, int callAnswerTimeout);
    String playAnnouncement(String mediaCallLegId, String callLegId, String audioFileUri, boolean allowBarge, boolean clearBuffer);
    String playAnnouncement(String mediaCallLegId, String callLegId, String audioFileUri, boolean allowBarge, boolean clearBuffer, int iterations);
    String playAnnouncement(String mediaCallLegId, String callLegId, String audioFileUri, boolean allowBarge, boolean clearBuffer, int iterations, int intervalMs);
    String promptAndCollectDigits(String mediaCallLegId, String callLegId, DtmfCollectCommand dtmfCollectCommand);
	String promptAndRecord(String mediaCallLegId, String callLegId, PromptAndRecordCommand promptAndRecordCommand);
	String generateDtmfDigits(final String mediaDialogId, final String dialogId, final String digits);
	String generateDtmfDigits(final String mediaDialogId, final String dialogId, final String digits, int digitLengthMilliseconds);
	void cancelMediaCommand(String mediaCallLegId, String callLegId, String mediaCommandId);
	void addMediaCallLegListener(MediaCallLegListener listener);
	void removeMediaCallLegListener(MediaCallLegListener listener);
}
