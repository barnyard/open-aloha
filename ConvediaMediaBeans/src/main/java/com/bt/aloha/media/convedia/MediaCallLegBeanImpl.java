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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SessionDescription;
import javax.sip.message.Request;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.callleg.CallLegHelper;
import com.bt.aloha.dialog.DialogConcurrentUpdateBlock;
import com.bt.aloha.dialog.DialogSipListener;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.ImmutableDialogInfo;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.dialog.state.TerminationCause;
import com.bt.aloha.dialog.state.TerminationMethod;
import com.bt.aloha.media.DtmfCollectCommand;
import com.bt.aloha.media.MediaCallLegBean;
import com.bt.aloha.media.MediaCallLegListener;
import com.bt.aloha.media.MediaCallLegListenerAdapter;
import com.bt.aloha.media.MediaDialogInfo;
import com.bt.aloha.media.PromptAndRecordCommand;
import com.bt.aloha.media.convedia.msml.model.MsmlAnnouncementRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlCancelMediaRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlDtmfGenerationRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndCollectDigitsRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndRecordRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlRequest;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateManager;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;
import com.bt.aloha.util.HousekeeperAware;

public class MediaCallLegBeanImpl extends MediaDialogSipBeanImpl implements MediaCallLegBean, HousekeeperAware {
    public static final String GENERATE_INVALID_DIALOG_MESSAGE = "Unable to generate digits: invalid dialog id specified";
    private static final String UNABLE_TO_PROMPT_AND_COLLECT_DIGITS_MEDIA_DIALOG_NOT_IN_CONNECTED_STATE = "Unable to prompt and collect digits: media dialog not in connected state";
    private static final String UNABLE_TO_PROMPT_AND_RECORD_INVALID_DIALOG_ID_SPECIFIED = "Unable to prompt and record: invalid dialog id specified";
    private static final String CONVEDIA_MSML_USER_ID = "msml";
    private static final String ANNOUNCEMENT_INVALID_DIALOG_MESSAGE = "Unable to play announcement: invalid dialog id specified";
    private static final String PROMPT_INVALID_DIALOG_MESSAGE = "Unable to prompt and collect digits: invalid dialog id specified";
    private ConcurrentUpdateManager concurrentUpdateManager = new ConcurrentUpdateManagerImpl();
    private Log log = LogFactory.getLog(this.getClass());
    private MediaServerAddressFactory mediaServerAddressFactory;

    private HousekeeperCallLegHelper housekeeperCallLegHelper;
    
    public MediaCallLegBeanImpl() {
        super();
    }

    public void setMediaServerAddressFactory(MediaServerAddressFactory theMediaServerAddressFactory) {
        this.mediaServerAddressFactory = theMediaServerAddressFactory;
    }

    void setHousekeeperCallLegHelper(HousekeeperCallLegHelper aHousekeeperCallLegHelper){
    	this.housekeeperCallLegHelper = aHousekeeperCallLegHelper;
    }
    
    public void setMediaCallLegListeners(List<MediaCallLegListener> listeners) {
        List<DialogSipListener> dialogSipListeners = new ArrayList<DialogSipListener>();
        for (MediaCallLegListener listener : listeners) {
            dialogSipListeners.add(new MediaCallLegListenerAdapter(listener));
        }
        this.setDialogSipListeners(dialogSipListeners);
    }

    public String createMediaCallLeg(String dialogId) {
        return createMediaCallLeg(dialogId, 0);
    }

    public String createMediaCallLeg(final String targetDialogId, final int callAnswerTimeout) {
        log.debug(String.format("Creating new media dialog for %s with call answer timeout %d", targetDialogId, callAnswerTimeout));
        if(targetDialogId == null)
            throw new IllegalArgumentException("Callee dialog must not be empty");
        ImmutableDialogInfo targetDialogInfo = getDialogCollection().get(targetDialogId);
        if(targetDialogInfo == null)
            throw new IllegalArgumentException(String.format("Failed to create media dialog: could not find target dialog %s", targetDialogId));

        String targetSipUri = getDialogCollection().get(targetDialogId).getRemoteParty().getURI().toString();
        return createMediaDialogLeg(targetSipUri, callAnswerTimeout);
    }

    // TODO should be protected/private?
    public String createMediaDialogLeg(String targetSipUri, int callAnswerTimeout) {
        String dialogId = getSimpleSipStack().getSipProvider().getNewCallId().getCallId();
        MediaDialogInfo mediaDialogInfo = new MediaDialogInfo(dialogId, getBeanName(), getSimpleSipStack().getIpAddress(), targetSipUri, getMediaServerSipUri(), getSimpleSipStack().generateNewTag(), callAnswerTimeout);
        getDialogCollection().add(mediaDialogInfo);
        return mediaDialogInfo.getId();
    }

    public String getMediaServerSipUri() {
        return String.format("sip:%s@%s", CONVEDIA_MSML_USER_ID, mediaServerAddressFactory.getAddress());
    }

    public String playAnnouncement(final String mediaDialogId, final String dialogId, final String audioFileUri, final boolean allowBarge, final boolean clearBuffer) {
    	return playAnnouncement(mediaDialogId, dialogId, audioFileUri, allowBarge, clearBuffer, MsmlAnnouncementRequest.DEFAULT_ITERATIONS, MsmlAnnouncementRequest.DEFAULT_INTERVAL);
    }

    public String playAnnouncement(final String mediaDialogId, final String dialogId, final String audioFileUri, final boolean allowBarge, final boolean clearBuffer, int iterations) {
    	return playAnnouncement(mediaDialogId, dialogId, audioFileUri, allowBarge, clearBuffer, iterations, MsmlAnnouncementRequest.DEFAULT_INTERVAL);
    }

    public String playAnnouncement(final String mediaDialogId, final String dialogId, final String audioFileUri, final boolean allowBarge, final boolean clearBuffer, int iterations, int interval) {
        ReadOnlyDialogInfo dialogInfo = getDialogCollection().get(dialogId);

        if(dialogInfo == null)
            throw new IllegalArgumentException(ANNOUNCEMENT_INVALID_DIALOG_MESSAGE);
        if(!DialogState.Confirmed.equals(dialogInfo.getDialogState()))
            throw new IllegalStateException("Unable to play announcement: target dialog not in connected state");

        String targetAddress = getRtpTargetFromSessionDescription(dialogInfo.getSessionDescription());
        final MsmlAnnouncementRequest req = new MsmlAnnouncementRequest(targetAddress, audioFileUri, allowBarge, clearBuffer, iterations, interval);

        ConcurrentUpdateBlock concurrentUpdateBlock = new DialogConcurrentUpdateBlock(getDialogBeanHelper()) {
            public void execute() {
                DialogInfo mediaDialogInfo = getDialogCollection().get(mediaDialogId);
                if(mediaDialogInfo == null)
                    throw new IllegalArgumentException(ANNOUNCEMENT_INVALID_DIALOG_MESSAGE);
                if(!DialogState.Confirmed.equals(mediaDialogInfo.getDialogState()))
                    throw new IllegalStateException("Unable to play announcement: media dialog not in connected state");
                assignSequenceNumber(mediaDialogInfo, Request.INFO);
                replaceDialogInfoAndSendMediaRequest(mediaDialogInfo, req);
            }
            public String getResourceId() {
                return mediaDialogId;
            }
        };
        concurrentUpdateManager.executeConcurrentUpdate(concurrentUpdateBlock);
        return req.getCommandId();
    }

    public void replaceDialogInfoAndSendMediaRequest(DialogInfo mediaDialogInfo, MsmlRequest req) {
        Request request = getDialogBeanHelper().createInfoRequest(mediaDialogInfo);
        if (req != null && req.getXml() != null) {
            try {
                request.setContent(req.getXml(), getSimpleSipStack().getHeaderFactory().createContentTypeHeader("application", "msml+xml"));
            } catch (ParseException e) {
                log.debug(String.format("Error setting content of INFO request message for dialog %s", mediaDialogInfo.getId()));
            }
        }
        replaceDialogIfCanSendRequest(request.getMethod(), mediaDialogInfo.getDialogState(), mediaDialogInfo.getTerminationMethod(), mediaDialogInfo);
        getDialogBeanHelper().sendRequest(request);
    }

    public String promptAndCollectDigits(final String mediaDialogId, final String dialogId, final DtmfCollectCommand dtmfCollectCommand) {
        ReadOnlyDialogInfo dialogInfo = getDialogCollection().get(dialogId);

        if(dialogInfo == null)
            throw new IllegalArgumentException(PROMPT_INVALID_DIALOG_MESSAGE);
        if(!DialogState.Confirmed.equals(dialogInfo.getDialogState()))
            throw new IllegalStateException("Unable to prompt and collect digits: target dialog not in connected state");

        String targetAddress = getRtpTargetFromSessionDescription(dialogInfo.getSessionDescription());
        final MsmlPromptAndCollectDigitsRequest req = new MsmlPromptAndCollectDigitsRequest(targetAddress, dtmfCollectCommand);

        ConcurrentUpdateBlock concurrentUpdateBlock = new DialogConcurrentUpdateBlock(getDialogBeanHelper()) {
            public void execute() {
                DialogInfo mediaDialogInfo = getDialogCollection().get(mediaDialogId);
                if(mediaDialogInfo == null)
                    throw new IllegalArgumentException(PROMPT_INVALID_DIALOG_MESSAGE);
                if(!DialogState.Confirmed.equals(mediaDialogInfo.getDialogState()))
                    throw new IllegalStateException(UNABLE_TO_PROMPT_AND_COLLECT_DIGITS_MEDIA_DIALOG_NOT_IN_CONNECTED_STATE);
                assignSequenceNumber(mediaDialogInfo, Request.INFO);
                replaceDialogInfoAndSendMediaRequest(mediaDialogInfo, req);
            }
            public String getResourceId() {
            	return mediaDialogId;
            }
        };
        concurrentUpdateManager.executeConcurrentUpdate(concurrentUpdateBlock);
        return req.getCommandId();
    }

    public String promptAndRecord(final String mediaDialogId, final String dialogId, final PromptAndRecordCommand promptAndRecordCommand) {
        ReadOnlyDialogInfo dialogInfo = getDialogCollection().get(dialogId);

        if (dialogInfo == null)
            throw new IllegalArgumentException(UNABLE_TO_PROMPT_AND_RECORD_INVALID_DIALOG_ID_SPECIFIED);
        if (!DialogState.Confirmed.equals(dialogInfo.getDialogState()))
            throw new IllegalStateException("Unable to prompt and record: target dialog not in connected state");

        String targetAddress = getRtpTargetFromSessionDescription(dialogInfo.getSessionDescription());
        final MsmlPromptAndRecordRequest req = new MsmlPromptAndRecordRequest(targetAddress, promptAndRecordCommand);

        ConcurrentUpdateBlock concurrentUpdateBlock = new DialogConcurrentUpdateBlock(getDialogBeanHelper()) {
            public void execute() {
                DialogInfo mediaDialogInfo = getDialogCollection().get(mediaDialogId);
                if (mediaDialogInfo == null)
                    throw new IllegalArgumentException(UNABLE_TO_PROMPT_AND_RECORD_INVALID_DIALOG_ID_SPECIFIED);
                if (!DialogState.Confirmed.equals(mediaDialogInfo.getDialogState()))
                    throw new IllegalStateException(UNABLE_TO_PROMPT_AND_COLLECT_DIGITS_MEDIA_DIALOG_NOT_IN_CONNECTED_STATE);
                assignSequenceNumber(mediaDialogInfo, Request.INFO);
                replaceDialogInfoAndSendMediaRequest(mediaDialogInfo, req);
            }
            public String getResourceId() {
                return mediaDialogId;
            }
        };
        concurrentUpdateManager.executeConcurrentUpdate(concurrentUpdateBlock);
        return req.getCommandId();
    }

    public String generateDtmfDigits(final String mediaDialogId, final String dialogId, final String digits) {
    	return generateDtmfDigits(mediaDialogId, dialogId, digits, MsmlDtmfGenerationRequest.DEFAULT_DIGIT_LENGTH_MILLIS);
    }
    
    public String generateDtmfDigits(final String mediaDialogId, final String dialogId, final String digits, int digitLengthMilliseconds) {
        ReadOnlyDialogInfo dialogInfo = getDialogCollection().get(dialogId);

        if(dialogInfo == null)
            throw new IllegalArgumentException(GENERATE_INVALID_DIALOG_MESSAGE);
        if(!DialogState.Confirmed.equals(dialogInfo.getDialogState()))
            throw new IllegalStateException("Unable to generate digits: target dialog not in connected state");

        String targetAddress = getRtpTargetFromSessionDescription(dialogInfo.getSessionDescription());
        final MsmlDtmfGenerationRequest req = new MsmlDtmfGenerationRequest(targetAddress, digits, digitLengthMilliseconds);

        ConcurrentUpdateBlock concurrentUpdateBlock = new DialogConcurrentUpdateBlock(getDialogBeanHelper()) {
            public void execute() {
                DialogInfo mediaDialogInfo = getDialogCollection().get(mediaDialogId);
                if(mediaDialogInfo == null)
                    throw new IllegalArgumentException(GENERATE_INVALID_DIALOG_MESSAGE);
                if(!DialogState.Confirmed.equals(mediaDialogInfo.getDialogState()))
                    throw new IllegalStateException("Unable to generate digits: media dialog not in connected state");

                assignSequenceNumber(mediaDialogInfo, Request.INFO);
                replaceDialogInfoAndSendMediaRequest(mediaDialogInfo, req);
            }
            public String getResourceId() {
                return mediaDialogId;
            }
        };
        concurrentUpdateManager.executeConcurrentUpdate(concurrentUpdateBlock);
        return req.getCommandId();
    }

    public void cancelMediaCommand(final String mediaDialogId, final String dialogId, final String mediaCommandId) {
        ReadOnlyDialogInfo dialogInfo = getDialogCollection().get(dialogId);

        if(dialogInfo == null)
            throw new IllegalArgumentException(GENERATE_INVALID_DIALOG_MESSAGE);
        if(!DialogState.Confirmed.equals(dialogInfo.getDialogState()))
            throw new IllegalStateException("Unable to cancel media: target dialog not in connected state");

        String targetAddress = getRtpTargetFromSessionDescription(dialogInfo.getSessionDescription());
        final MsmlCancelMediaRequest req = new MsmlCancelMediaRequest(mediaCommandId, targetAddress);

        ConcurrentUpdateBlock concurrentUpdateBlock = new DialogConcurrentUpdateBlock(getDialogBeanHelper()) {
            public void execute() {
                DialogInfo mediaDialogInfo = getDialogCollection().get(mediaDialogId);
                if(mediaDialogInfo == null)
                    throw new IllegalArgumentException(GENERATE_INVALID_DIALOG_MESSAGE);
                if(!DialogState.Confirmed.equals(mediaDialogInfo.getDialogState()))
                    throw new IllegalStateException("Unable to cancel media: media dialog not in connected state");

                assignSequenceNumber(mediaDialogInfo, Request.INFO);
                replaceDialogInfoAndSendMediaRequest(mediaDialogInfo, req);
            }
            public String getResourceId() {
                return mediaDialogId;
            }
        };
        concurrentUpdateManager.executeConcurrentUpdate(concurrentUpdateBlock);
    }

    public String getRtpTargetFromSessionDescription(SessionDescription sessionDescription) {
        if (sessionDescription == null)
            throw new IllegalStateException("SDP not set, media command can't proceed");

        MediaDescription activeMediaDescription = SessionDescriptionHelper.getActiveMediaDescription(sessionDescription);
        if (activeMediaDescription == null)
            throw new SDPParseException("No media description in SDP");

        try {
            int port = activeMediaDescription.getMedia().getMediaPort();
            String address;
            if (activeMediaDescription.getConnection() != null)
                address = activeMediaDescription.getConnection().getAddress();
            else
                address = sessionDescription.getConnection().getAddress();

            return address + ":" + port;
        } catch (SdpException e) {
            throw new SDPParseException("Error extracting rtp target from SDP", e);
        }
    }

    public void addMediaCallLegListener(MediaCallLegListener listener) {
        this.addMediaDialogSipBeanListener(new MediaCallLegListenerAdapter(listener));
    }

    public void removeMediaCallLegListener(MediaCallLegListener listener) {
        this.removeMediaDialogSipBeanListener(new MediaCallLegListenerAdapter(listener));
    }

	public void killHousekeeperCandidate(String infoId) {
		if(housekeeperCallLegHelper==null)
			housekeeperCallLegHelper = new HousekeeperCallLegHelper(getConcurrentUpdateManager(), getDialogCollection());
		housekeeperCallLegHelper.terminateCallLeg(infoId, TerminationCause.TerminatedByServer);
	}
	
   public static class HousekeeperCallLegHelper extends CallLegHelper {
  
	   private ConcurrentUpdateManager concurrentUpdateManager;
	   private DialogCollection dialogCollection;
	   public HousekeeperCallLegHelper(ConcurrentUpdateManager aConcurrentUpdateManager, DialogCollection aDialogCollection){
		   this.dialogCollection = aDialogCollection;
		   this.concurrentUpdateManager = aConcurrentUpdateManager;
	   }
		@Override
		protected void acceptReceivedMediaOffer(String dialogId,
				MediaDescription mediaDescription, boolean offerInOkResponse,
				boolean initialInviteTransactionCompleted) {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void endNonConfirmedDialog(ReadOnlyDialogInfo dialogInfo,
				TerminationMethod previousTerminationMethod) {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected ConcurrentUpdateManager getConcurrentUpdateManager() {
			return concurrentUpdateManager;
		}

		@Override
		protected DialogCollection getDialogCollection() {
			return dialogCollection;
		}
    }
}
