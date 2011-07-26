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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.call.CallBean;
import com.bt.aloha.call.CallInformation;
import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.call.state.CallTerminationCause;
import com.bt.aloha.call.state.ImmutableCallInfo;
import com.bt.aloha.call.state.ReadOnlyCallInfo;
import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.callleg.OutboundCallLegListener;
import com.bt.aloha.callleg.event.CallLegAlertingEvent;
import com.bt.aloha.callleg.event.CallLegConnectedEvent;
import com.bt.aloha.callleg.event.CallLegConnectionFailedEvent;
import com.bt.aloha.callleg.event.CallLegDisconnectedEvent;
import com.bt.aloha.callleg.event.CallLegRefreshCompletedEvent;
import com.bt.aloha.callleg.event.CallLegTerminatedEvent;
import com.bt.aloha.callleg.event.CallLegTerminationFailedEvent;
import com.bt.aloha.callleg.event.ReceivedCallLegRefreshEvent;
import com.bt.aloha.media.DtmfCollectCommand;
import com.bt.aloha.media.MediaCallBean;
import com.bt.aloha.media.MediaCallLegBean;
import com.bt.aloha.media.MediaCallLegListener;
import com.bt.aloha.media.MediaCallListener;
import com.bt.aloha.media.PromptAndRecordCommand;
import com.bt.aloha.media.convedia.msml.model.MsmlAnnouncementRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlDtmfGenerationRequest;
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
import com.bt.aloha.stack.SimpleSipBeanBase;
import com.bt.aloha.util.HousekeeperAware;

public class MediaCallBeanImpl extends SimpleSipBeanBase implements MediaCallBean, MediaCallLegListener, OutboundCallLegListener, HousekeeperAware {
    public static final String DTMF_GENERATION_DIGITS_CANNOT_BE_NULL = "DTMF Generation digits cannot be null";
    public static final String CALL_ID_CANNOT_BE_NULL = "CallId cannot be null";
    private static final int ONE_THOUSAND = 1000;
	private static final String CANNOT_ADD_A_NULL_LISTENER = "Cannot add a null listener";
	private Log log = LogFactory.getLog(this.getClass());
	private CallBean callBean;
	private MediaCallLegBean mediaCallLegBean;
	private List<MediaCallListener> mediaCallListeners = new ArrayList<MediaCallListener>();
	private CallCollection callCollection;

    public MediaCallBeanImpl() {
    }

	public void setMediaCallLegBean(MediaCallLegBean theMediaDialogBean) {
		if (this.mediaCallLegBean != null)
			this.mediaCallLegBean.removeMediaCallLegListener(this);
		this.mediaCallLegBean = theMediaDialogBean;
		this.mediaCallLegBean.addMediaCallLegListener(this);
	}

	public void addMediaCallListener(MediaCallListener aMediaCallListener){
		if (aMediaCallListener == null)
			throw new IllegalArgumentException(CANNOT_ADD_A_NULL_LISTENER);
	    this.mediaCallListeners.add(aMediaCallListener);
    }

	public void removeMediaCallListener(MediaCallListener aMediaCallListener) {
		if (aMediaCallListener == null)
			throw new IllegalArgumentException(CANNOT_ADD_A_NULL_LISTENER);
		this.mediaCallListeners.remove(aMediaCallListener);
	}

	public void setMediaCallListeners(List<MediaCallListener> theMediaListeners) {
		this.mediaCallListeners = theMediaListeners;
	}

	public List<MediaCallListener> getMediaCallListeners() {
		return mediaCallListeners;
	}

	public void setCallBean(CallBean theCallBean) {
		this.callBean = theCallBean;
	}

	public void setCallCollection(CallCollection aCallCollection) {
		this.callCollection = aCallCollection;
	}

    public String createMediaCall(String dialogId) {
		return createMediaCall(dialogId, AutoTerminateAction.Unchanged);
	}

	public String createMediaCall(String dialogId, AutoTerminateAction autoTerminateCallLeg) {
		log.debug(String.format("Creating media call with dialog %s and dialog autotermination set to %b", dialogId, autoTerminateCallLeg));
		if (dialogId == null)
			throw new IllegalArgumentException("DialogId cannot be null");

		String mediaDialogId = mediaCallLegBean.createMediaCallLeg(dialogId);
		return callBean.joinCallLegs(dialogId, mediaDialogId, autoTerminateCallLeg);
	}
	
	public String playAnnouncement(String callId, String audioFileUri) {
		return playAnnouncement(callId, audioFileUri, MsmlAnnouncementRequest.DEFAULT_ITERATIONS);
	}
	
	public String playAnnouncement(String callId, String audioFileUri, int iterations) {
		return playAnnouncement(callId, audioFileUri, iterations, MsmlAnnouncementRequest.DEFAULT_INTERVAL);
	}

	public String playAnnouncement(String callId, String audioFileUri, int iterations, int interval) {
		return playAnnouncement(callId, audioFileUri, iterations, interval, false);
	}

	public String playAnnouncement(String callId, String audioFileUri, int iterations, int interval, boolean barge) {
		log.debug(String.format("Playing media to call %s with audio file located in %s", callId, audioFileUri));
		if (callId == null)
			throw new IllegalArgumentException(CALL_ID_CANNOT_BE_NULL);
		if (audioFileUri == null)
			throw new IllegalArgumentException("Audio file uri cannot be null");

		ImmutableCallInfo callInfo = callCollection.get(callId);
		return mediaCallLegBean.playAnnouncement(callInfo.getSecondDialogId(), callInfo.getFirstDialogId(), audioFileUri, barge, true, iterations, interval);
	}

	public String promptAndCollectDigits(String callId, DtmfCollectCommand dtmfCollectCommand) {
		if (callId == null)
			throw new IllegalArgumentException(CALL_ID_CANNOT_BE_NULL);
		if (dtmfCollectCommand == null)
			throw new IllegalArgumentException("DTMF Collect Command cannot be null");
		log.debug(String.format("prompting and collecting digits to call %s with %s", callId, dtmfCollectCommand.toString()));

		ImmutableCallInfo callInfo = callCollection.get(callId);
		return mediaCallLegBean.promptAndCollectDigits(callInfo.getSecondDialogId(), callInfo.getFirstDialogId(), dtmfCollectCommand);
	}

    public String promptAndRecord(String mediaCallId, PromptAndRecordCommand promptAndRecordCommand) {
        if (mediaCallId == null)
            throw new IllegalArgumentException(CALL_ID_CANNOT_BE_NULL);
        if (promptAndRecordCommand == null)
            throw new IllegalArgumentException("Prompt and Record Command cannot be null");

        ImmutableCallInfo callInfo = callCollection.get(mediaCallId);
        return mediaCallLegBean.promptAndRecord(callInfo.getSecondDialogId(), callInfo.getFirstDialogId(), promptAndRecordCommand);
    }

    public String generateDtmfDigits(String callId, String digits) {
    	return generateDtmfDigits(callId, digits, MsmlDtmfGenerationRequest.DEFAULT_DIGIT_LENGTH_MILLIS);
    }
    
    public String generateDtmfDigits(String callId, String digits, int digitLengthMilliseconds) {
		if (callId == null)
			throw new IllegalArgumentException(CALL_ID_CANNOT_BE_NULL);
		if (digits == null)
			throw new IllegalArgumentException(DTMF_GENERATION_DIGITS_CANNOT_BE_NULL);
		if (digitLengthMilliseconds < MsmlDtmfGenerationRequest.MINIMUM_DIGIT_LENGTH || digitLengthMilliseconds > MsmlDtmfGenerationRequest.MAXIMUM_DIGIT_LENGTH)
			throw new IllegalArgumentException(String.format("digit length must be between %d and %d", MsmlDtmfGenerationRequest.MINIMUM_DIGIT_LENGTH, MsmlDtmfGenerationRequest.MAXIMUM_DIGIT_LENGTH));
		log.debug(String.format("Generating DTMF digits %s in call %s", digits, callId));

		ImmutableCallInfo callInfo = callCollection.get(callId);
		return mediaCallLegBean.generateDtmfDigits(callInfo.getSecondDialogId(), callInfo.getFirstDialogId(), digits, digitLengthMilliseconds);
	}

	public void cancelMediaCommand(String callId, String mediaCommandId) {
		if (callId == null)
			throw new IllegalArgumentException(CALL_ID_CANNOT_BE_NULL);
		if (mediaCommandId == null)
			throw new IllegalArgumentException("Media Command ID cannot be null");
		log.debug(String.format("Cancelling media command id %s in call %s", mediaCommandId, callId));

		ImmutableCallInfo callInfo = callCollection.get(callId);
		mediaCallLegBean.cancelMediaCommand(callInfo.getSecondDialogId(), callInfo.getFirstDialogId(), mediaCommandId);
	}

	public void onCallLegAnnouncementCompleted(CallLegAnnouncementCompletedEvent event) {
		ReadOnlyCallInfo callInfo = callCollection.getCurrentCallForCallLeg(event.getId());
		final CallAnnouncementCompletedEvent announcementCompletedEvent = new CallAnnouncementCompletedEvent(callInfo.getId(), event.getMediaCommandId(), event.getDuration(), event.getBarged());
		getEventDispatcher().dispatchEvent(mediaCallListeners, announcementCompletedEvent);
	}

	public void onCallLegAnnouncementFailed(CallLegAnnouncementFailedEvent event) {
		ReadOnlyCallInfo callInfo = callCollection.getCurrentCallForCallLeg(event.getId());
		final CallAnnouncementFailedEvent announcementFailedEvent = new CallAnnouncementFailedEvent(callInfo.getId(), event.getMediaCommandId());
		getEventDispatcher().dispatchEvent(mediaCallListeners, announcementFailedEvent);
	}

	public void onCallLegPromptAndCollectDigitsCompleted(CallLegPromptAndCollectDigitsCompletedEvent event) {
		ReadOnlyCallInfo callInfo = callCollection.getCurrentCallForCallLeg(event.getId());
		final CallPromptAndCollectDigitsCompletedEvent dtmfCollectDigitsCompletedEvent = new CallPromptAndCollectDigitsCompletedEvent(callInfo.getId(), event.getMediaCommandId(), event.getDigits(), event.getDtmfResult());
		getEventDispatcher().dispatchEvent(mediaCallListeners, dtmfCollectDigitsCompletedEvent);
	}

    public void onCallLegPromptAndCollectDigitsFailed(CallLegPromptAndCollectDigitsFailedEvent event) {
		ReadOnlyCallInfo callInfo = callCollection.getCurrentCallForCallLeg(event.getId());
        final CallPromptAndCollectDigitsFailedEvent dtmfCollectDigitsFailedEvent = new CallPromptAndCollectDigitsFailedEvent(callInfo.getId(), event.getMediaCommandId(), event.getDigits(), event.getDtmfResult());
        getEventDispatcher().dispatchEvent(mediaCallListeners, dtmfCollectDigitsFailedEvent);
    }

    public void onCallLegPromptAndRecordCompleted(CallLegPromptAndRecordCompletedEvent event) {
        ReadOnlyCallInfo callInfo = callCollection.getCurrentCallForCallLeg(event.getId());
        final CallPromptAndRecordCompletedEvent callPromptAndRecordCompletedEvent = new CallPromptAndRecordCompletedEvent(callInfo.getId(), event.getMediaCommandId(), event.getRecordId(), event.getRecordEnd(), getRecordLenFromString(event.getRecordLen()));
        getEventDispatcher().dispatchEvent(mediaCallListeners, callPromptAndRecordCompletedEvent);
    }

    private int getRecordLenFromString(String in) {
        if (in.endsWith("ms"))
            return Integer.parseInt(in.substring(0, in.length() - 2));
        if (in.endsWith("s"))
            return Integer.parseInt(in.substring(0, in.length() - 1)) * ONE_THOUSAND;
        return  Integer.parseInt(in);
    }

    public void onCallLegPromptAndRecordFailed(CallLegPromptAndRecordFailedEvent event) {
        ReadOnlyCallInfo callInfo = callCollection.getCurrentCallForCallLeg(event.getId());
        final CallPromptAndRecordFailedEvent callPromptAndRecordFailedEvent = new CallPromptAndRecordFailedEvent(callInfo.getId(), event.getMediaCommandId(), event.getRecordEnd());
        getEventDispatcher().dispatchEvent(mediaCallListeners, callPromptAndRecordFailedEvent);
    }

    public void onCallLegPromptAndRecordTerminated(CallLegPromptAndRecordTerminatedEvent event) {
        ReadOnlyCallInfo callInfo = callCollection.getCurrentCallForCallLeg(event.getId());
        final CallPromptAndRecordTerminatedEvent callPromptAndRecordTerminatedEvent = new CallPromptAndRecordTerminatedEvent(callInfo.getId(), event.getMediaCommandId());
        getEventDispatcher().dispatchEvent(mediaCallListeners, callPromptAndRecordTerminatedEvent);
    }

    public void onCallLegDtmfGenerationCompleted(CallLegDtmfGenerationCompletedEvent event) {
		ReadOnlyCallInfo callInfo = callCollection.getCurrentCallForCallLeg(event.getId());
		final CallDtmfGenerationCompletedEvent dtmfGenerationCompletedEvent = new CallDtmfGenerationCompletedEvent(callInfo.getId(), event.getMediaCommandId());
		getEventDispatcher().dispatchEvent(mediaCallListeners, dtmfGenerationCompletedEvent);
	}

    public void onCallLegDtmfGenerationFailed(CallLegDtmfGenerationFailedEvent event) {
		ReadOnlyCallInfo callInfo = callCollection.getCurrentCallForCallLeg(event.getId());
        final CallDtmfGenerationFailedEvent dtmfGenerationFailedEvent = new CallDtmfGenerationFailedEvent(callInfo.getId(), event.getMediaCommandId());
        getEventDispatcher().dispatchEvent(mediaCallListeners, dtmfGenerationFailedEvent);
    }

    public void onCallLegAnnouncementTerminated(CallLegAnnouncementTerminatedEvent event) {
		ReadOnlyCallInfo callInfo = callCollection.getCurrentCallForCallLeg(event.getId());
		final CallAnnouncementTerminatedEvent announcementTerminatedEvent = new CallAnnouncementTerminatedEvent(callInfo.getId(), event.getMediaCommandId());
		getEventDispatcher().dispatchEvent(mediaCallListeners, announcementTerminatedEvent);
    }

    public void onCallLegPromptAndCollectDigitsTerminated(CallLegPromptAndCollectDigitsTerminatedEvent event) {
		ReadOnlyCallInfo callInfo = callCollection.getCurrentCallForCallLeg(event.getId());
        final CallPromptAndCollectDigitsTerminatedEvent dtmfCollectDigitsTerminatedEvent = new CallPromptAndCollectDigitsTerminatedEvent(callInfo.getId(), event.getMediaCommandId(), event.getDigits(), event.getDtmfResult());
        getEventDispatcher().dispatchEvent(mediaCallListeners, dtmfCollectDigitsTerminatedEvent);
    }

    public void terminateMediaCall(String mediaCallId) {
		callBean.terminateCall(mediaCallId);
	}

    public void terminateMediaCall(String mediaCallId, CallTerminationCause callTerminationCause) {
    	callBean.terminateCall(mediaCallId, callTerminationCause);
    }

	public void onCallLegConnected(CallLegConnectedEvent connectedEvent) {
		callBean.onCallLegConnected(connectedEvent);
	}

	public void onCallLegConnectionFailed(CallLegConnectionFailedEvent connectionFailedEvent) {
		callBean.onCallLegConnectionFailed(connectionFailedEvent);
	}

	public void onCallLegDisconnected(CallLegDisconnectedEvent disconnectedEvent) {
		callBean.onCallLegDisconnected(disconnectedEvent);
	}

	public void onCallLegTerminated(CallLegTerminatedEvent terminatedEvent) {
		callBean.onCallLegTerminated(terminatedEvent);
	}

	public void onCallLegTerminationFailed(CallLegTerminationFailedEvent terminationFailedEvent) {
		callBean.onCallLegTerminationFailed(terminationFailedEvent);
	}

	public void onCallLegAlerting(CallLegAlertingEvent alertingEvent) {
		callBean.onCallLegAlerting(alertingEvent);
	}

	public void onCallLegRefreshCompleted(CallLegRefreshCompletedEvent callLegConnectedEvent) {
		callBean.onCallLegRefreshCompleted(callLegConnectedEvent);
	}

	public void onReceivedCallLegRefresh(ReceivedCallLegRefreshEvent receivedCallLegRefreshEvent) {
		callBean.onReceivedCallLegRefresh(receivedCallLegRefreshEvent);
	}

	public void killHousekeeperCandidate(String infoId) {
		terminateMediaCall(infoId, CallTerminationCause.Housekept);
	}

	public CallInformation getCallInformation(String callId) {
		return callBean.getCallInformation(callId);
	}
}
