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

 	

 	
 	
 
package com.bt.aloha.media.testing.mockphones;

import java.text.ParseException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sip.ServerTransaction;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.dialog.DialogConcurrentUpdateBlock;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.media.convedia.SDPParseException;
import com.bt.aloha.media.convedia.msml.MsmlRequestParser;
import com.bt.aloha.media.convedia.msml.model.MsmlAnnouncementRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlAnnouncementResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlApplicationEventType;
import com.bt.aloha.media.convedia.msml.model.MsmlCancelMediaRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlDtmfGenerationRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlDtmfGenerationResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndCollectDigitsAnnouncementResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndCollectDigitsCollectedResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndCollectDigitsRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndRecordAnnouncementResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndRecordRecordedResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndRecordRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlRequest;
import com.bt.aloha.stack.SimpleSipStack;
import com.bt.aloha.testing.mockphones.HangUpDialogBean;
import com.bt.aloha.util.ConcurrentUpdateBlock;

public class ConvediaMockphoneBean extends HangUpDialogBean {
	public static final int DEFAULT_ANNOUNCEMENT_DURATION_PERIOD = 500;
	public static final String DTMF_VALUE_SEPARATOR = "_";
	public static final String ANNOUNCEMENT_DURATION_PERIOD_PROPERTY_KEY = "announcement.duration";
	public static final String BAD_REQUEST_STRING_PROPERTY_KEY = "BadRequest";
    public static final String PLAY_FAILED_STRING_PROPERTY_KEY = "PlayFailed";
    public static final String PLAY_TERMINATED_STRING_PROPERTY_KEY = "PlayFailedTerminated";
	public static final String FILE_NOT_FOUND_STRING_PROPERTY_KEY = "FileNotFound";
	public static final String BARGED_STRING_PROPERTY_KEY = "Barged";
	public static final String DTMFGEN_FAILURE_DIGIT_SEQUENCE = "666";
	public static final String DTMFGEN_TERMINATION_DIGIT_SEQUENCE = "13";
	public static final String DTMF_VALUE_STRING_PROPERTY_KEY = "DtmfValue";
    public static final String DTMF_NO_INPUT_STRING_PROPERTY_KEY = "DtmfFailedNoInput";
    public static final String DTMF_NO_MATCH_STRING_PROPERTY_KEY = "DtmfFailedNoMatch";
    public static final String DTMF_TERMINATED_STRING_PROPERTY_KEY = "DtmfFailedTerminated";
    public static final String DTMF_PLAY_TERMINATED_STRING_PROPERTY_KEY = "DtmfFailedPlayTerminated";
    public static final String DTMF_UNDEFINED_STRING_PROPERTY_KEY = "DtmfFailedUndefined";
	private static final String DTMFGEN_FAILED = "dtmfgen.failed";
	private static final String PLAY_TERMINATED = "play.terminated";
	private static final String ONE_HUNDRED_MS = "100ms";
	private static final int CONVEDIA_MOCKPHONE_PORT = 10005;
	private static final int FIVE_HUNDRED = 500;
	private static final int TWENTY = 20;
	private static final String LOCALHOST = "127.0.0.1";
	private static final MediaDescription OFFER_MEDIA_DESCRIPTION;

	private Log log = LogFactory.getLog(this.getClass());

	static {
		try {
			OFFER_MEDIA_DESCRIPTION = SdpFactory.getInstance().createMediaDescription("audio", CONVEDIA_MOCKPHONE_PORT, 0, "RTP/AVP", new String[] {"5"});
			Connection conn = SdpFactory.getInstance().createConnection(LOCALHOST);
			OFFER_MEDIA_DESCRIPTION.setConnection(conn);
			OFFER_MEDIA_DESCRIPTION.setAttribute("rtpmap", "5 PCMU/8000");
		} catch(SdpException e) {
			throw new SDPParseException(e.getMessage(), e);
		}
	}

	public static MediaDescription getOfferMediaDescription() {
		return OFFER_MEDIA_DESCRIPTION;
	}

	public ConvediaMockphoneBean(SimpleSipStack simpleSipStack)	throws Exception {
		super();
	}

	@Override
	protected MediaDescription getInitialInviteOkResponseMediaDescription(MediaDescription aOfferMediaDescription) {
		return OFFER_MEDIA_DESCRIPTION;
	};

	@Override
	protected MediaDescription getReinviteOkResponseMediaDescription(MediaDescription aOfferMediaDescription) {
		return OFFER_MEDIA_DESCRIPTION;
    }

	@Override
	public void processInfo(final Request request, final ServerTransaction serverTransaction, final String dialogId) {
		final String msmlContent = new String(request.getRawContent());
		MsmlRequestParser msmlRequestParser = new MsmlRequestParser();
		MsmlRequest req = msmlRequestParser.parse(msmlContent);
		final String commandId = req.getCommandId();

		if (req instanceof MsmlAnnouncementRequest)
			processAnnouncementRequest(dialogId, request, serverTransaction, commandId, req);
		else if (req instanceof MsmlPromptAndCollectDigitsRequest)
			processDtmfCollectionRequest(dialogId, request, serverTransaction, commandId, req);
		else if (req instanceof MsmlDtmfGenerationRequest)
			processDtmfGenerationRequest(dialogId, commandId, (MsmlDtmfGenerationRequest)req);
        else if (req instanceof MsmlPromptAndRecordRequest)
            processPromptAndRecordRequest(dialogId, request, serverTransaction, commandId, req);
		else if (req instanceof MsmlCancelMediaRequest)
			processMsmlCancelMediaRequest(dialogId, commandId);
		else
			throw new RuntimeException("Unknown request received: " + commandId);
	}

    private void processMsmlCancelMediaRequest(final String dialogId, final String commandId) {
		final AtomicBoolean needToSendDtmfTerminatedResponse = new AtomicBoolean(false);
		final DialogInfo dialogInfo = getDialogCollection().get(dialogId);
		String originalAudioFileUri = dialogInfo.getApplicationData();
		String responseContent = null;
		if (originalAudioFileUri.indexOf(PLAY_TERMINATED_STRING_PROPERTY_KEY) > -1) {
			responseContent = new MsmlAnnouncementResponse(commandId, ONE_HUNDRED_MS, PLAY_TERMINATED).getXml();
		} else if (originalAudioFileUri.indexOf(DTMF_PLAY_TERMINATED_STRING_PROPERTY_KEY) > -1) {
			responseContent = new MsmlPromptAndCollectDigitsAnnouncementResponse(commandId, ONE_HUNDRED_MS, PLAY_TERMINATED).getXml();
		} else if (originalAudioFileUri.indexOf(DTMF_TERMINATED_STRING_PROPERTY_KEY) > -1) {
			responseContent = new MsmlPromptAndCollectDigitsAnnouncementResponse(commandId, ONE_HUNDRED_MS, "play.complete").getXml();
			needToSendDtmfTerminatedResponse.set(true);
		}
		sendMediaResponse(dialogId, commandId, responseContent);

		if (needToSendDtmfTerminatedResponse.get()) {
			responseContent = new MsmlPromptAndCollectDigitsCollectedResponse(commandId, ONE_HUNDRED_MS, "dtmf.terminated").getXml();
			sendMediaResponse(dialogId, commandId, responseContent);
		}
	}

	private boolean isBadRequestSendResponse(final Request request, final ServerTransaction serverTransaction, final String audioFileUri) {
		if (audioFileUri.indexOf(BAD_REQUEST_STRING_PROPERTY_KEY) > -1) {
			getDialogBeanHelper().sendResponse(request, serverTransaction, Response.BAD_REQUEST);
			return true;
		}
		return false;
	}

	private void storeAudioFileUriForFutureCancel(final String dialogId, final String commandId, final String audioFileUri) {
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				log.debug(String.format("Storing audio file uri for command id %s for the cancel command to come later", commandId));
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				dialogInfo.setApplicationData(audioFileUri);
				getDialogCollection().replace(dialogInfo);
			}
			public String getResourceId() {
				return dialogId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	private void processAnnouncementRequest(final String dialogId, final Request request, final ServerTransaction serverTransaction, final String commandId, final MsmlRequest req) {
		final DialogInfo dialogInfo = getDialogCollection().get(dialogId);
		int timeUntilAnnouncementFinish = dialogInfo.getIntProperty(ANNOUNCEMENT_DURATION_PERIOD_PROPERTY_KEY, DEFAULT_ANNOUNCEMENT_DURATION_PERIOD);
		log.info("Announcement to finish after " + timeUntilAnnouncementFinish + " milliseconds");

		final String audioFileUri = ((MsmlAnnouncementRequest)req).getAudioFileUri();
		if (isBadRequestSendResponse(request, serverTransaction, audioFileUri))
			return;

		if (audioFileUri.indexOf(PLAY_TERMINATED_STRING_PROPERTY_KEY) > -1) {
			storeAudioFileUriForFutureCancel(dialogId, commandId, audioFileUri);
			return;
		}

		getDialogBeanHelper().sendResponse(request, serverTransaction, Response.OK);

		final StringBuffer playEndValue = getPlayEndValue(audioFileUri);
		final String responseContent = audioFileUri.indexOf(FILE_NOT_FOUND_STRING_PROPERTY_KEY) > -1 ?
				new MsmlEmptyEventResponse(commandId, MsmlApplicationEventType.PLAY_COMMAND_COMPLETE).getXml() :
					new MsmlAnnouncementResponse(commandId, ONE_HUNDRED_MS, playEndValue.toString()).getXml();

		getScheduledExecutorService().schedule(new Runnable() {
			public void run() {
				try {
					sendMediaResponse(dialogId, commandId, responseContent);
				} catch (Exception e) {
					log.info("Exception occured in ConvediaMockPhone: "	+ e.getMessage(),e);
				}
			}
		}, timeUntilAnnouncementFinish, TimeUnit.MILLISECONDS);
	}

	private void processPromptAndRecordRequest(final String dialogId, Request request, ServerTransaction serverTransaction, final String commandId, MsmlRequest req) {
        final DialogInfo dialogInfo = getDialogCollection().get(dialogId);
        int timeUntilAnnouncementFinish = dialogInfo.getIntProperty(ANNOUNCEMENT_DURATION_PERIOD_PROPERTY_KEY, DEFAULT_ANNOUNCEMENT_DURATION_PERIOD);
        log.info("Announcement to finish after " + timeUntilAnnouncementFinish + " milliseconds");
        final String audioFileUri = ((MsmlPromptAndRecordRequest)req).getPromptAndRecordCommand().getPromptFileUri();
        getDialogBeanHelper().sendResponse(request, serverTransaction, Response.OK);
        StringBuffer playEndValue = getPlayEndValue(audioFileUri);
        final String responseContent = new MsmlPromptAndRecordAnnouncementResponse(commandId, ONE_HUNDRED_MS, playEndValue.toString()).getXml();
		getScheduledExecutorService().schedule(new Runnable() {
            public void run() {
                try {
                    sendMediaResponse(dialogId, commandId, responseContent);
                    Thread.sleep(TWENTY);
                    sendRecordResponse(dialogId, commandId, audioFileUri);
                } catch (Exception e) {
                    log.info("Exception occured in ConvediaMockPhone: " + e.getMessage(),e);
                }
            }
		}, timeUntilAnnouncementFinish, TimeUnit.MILLISECONDS);
	}

	private void processDtmfCollectionRequest(final String dialogId, final Request request, final ServerTransaction serverTransaction, final String commandId, final MsmlRequest req) {
		final DialogInfo dialogInfo = getDialogCollection().get(dialogId);
		int timeUntilAnnouncementFinish = dialogInfo.getIntProperty(ANNOUNCEMENT_DURATION_PERIOD_PROPERTY_KEY, DEFAULT_ANNOUNCEMENT_DURATION_PERIOD);
		log.info("Announcement to finish after " + timeUntilAnnouncementFinish + " milliseconds");

		final String audioFileUri = ((MsmlPromptAndCollectDigitsRequest)req).getDtmfCollectCommand().getPromptFileUri();
		if (isBadRequestSendResponse(request, serverTransaction, audioFileUri))
			return;

		if (audioFileUri.indexOf(DTMF_TERMINATED_STRING_PROPERTY_KEY) > -1 || audioFileUri.indexOf(DTMF_PLAY_TERMINATED_STRING_PROPERTY_KEY) > -1) {
			storeAudioFileUriForFutureCancel(dialogId, commandId, audioFileUri);
			return;
		}

		getDialogBeanHelper().sendResponse(request, serverTransaction, Response.OK);

		StringBuffer playEndValue = getPlayEndValue(audioFileUri);
		final String responseContent = audioFileUri.indexOf(FILE_NOT_FOUND_STRING_PROPERTY_KEY) > -1 ?
				new MsmlEmptyEventResponse(commandId, MsmlApplicationEventType.DTMF_PLAY_COMMAND_COMPLETE).getXml() :
					new MsmlPromptAndCollectDigitsAnnouncementResponse(commandId, ONE_HUNDRED_MS, playEndValue.toString()).getXml();

		getScheduledExecutorService().schedule(new Runnable() {
			public void run() {
				try {
					sendMediaResponse(dialogId, commandId, responseContent);
					Thread.sleep(TWENTY);
					sendDtmfResponse(dialogId, commandId, audioFileUri);
				} catch (Exception e) {
					log.info("Exception occured in ConvediaMockPhone: "	+ e.getMessage(),e);
				}
			}
		}, timeUntilAnnouncementFinish, TimeUnit.MILLISECONDS);
	}

	private void processDtmfGenerationRequest(final String dialogId, final String commandId, MsmlDtmfGenerationRequest req) {
		log.info("Pretending to generate DTMF digits");
		try {
			Thread.sleep(FIVE_HUNDRED);
		} catch(InterruptedException e) {
			log.error(e.getMessage(),e);
		}

		String dtmfGenValue = req.getDigits().contains(DTMFGEN_FAILURE_DIGIT_SEQUENCE) || req.getDigits().contains(DTMFGEN_TERMINATION_DIGIT_SEQUENCE) ?
				DTMFGEN_FAILED : "dtmfgen.complete";
		final String content = new MsmlDtmfGenerationResponse(commandId, dtmfGenValue).getXml();
		sendMediaResponse(dialogId, commandId, content);
	}

	protected static String retrieveExpectedDtmfValue(String audioFileUri) {
		if (audioFileUri.indexOf(DTMF_VALUE_SEPARATOR) < 0) {
			return null;
		}
		return audioFileUri.split(DTMF_VALUE_SEPARATOR)[1];
	}

	protected Request createMediaCommandCompleteRequest(ReadOnlyDialogInfo dialogInfo,	String commandId, String content) {
		Request request = getDialogBeanHelper().createInfoRequest(dialogInfo);
		if (content != null)
			try {
				request.setContent(content, getSimpleSipStack().getHeaderFactory().createContentTypeHeader("application", "msml+xml"));
			} catch (ParseException e) {
				log.debug(String.format("Error setting content of INFO response message for dialog %s",	dialogInfo.getId()));
			}
		return request;
	}

	private StringBuffer getPlayEndValue(final String audioFileUri) {
		StringBuffer playEndValue = new StringBuffer("play.");
		if (audioFileUri.indexOf(PLAY_FAILED_STRING_PROPERTY_KEY) > -1)
			playEndValue.append("failed");
		else {
			playEndValue.append("complete");
			if (audioFileUri.indexOf(BARGED_STRING_PROPERTY_KEY) > -1)
				playEndValue.append(".barged");
		}
		return playEndValue;
	}

	private String getDtmfEndValue(final String audioFileUri) {
		String dtmfEndValue;
		if (audioFileUri.indexOf(DTMF_NO_INPUT_STRING_PROPERTY_KEY) > -1)
		    dtmfEndValue = "dtmf.noinput";
		else if (audioFileUri.indexOf(DTMF_NO_MATCH_STRING_PROPERTY_KEY) > -1)
			dtmfEndValue = "dtmf.nomatch";
		else if (audioFileUri.indexOf(DTMF_UNDEFINED_STRING_PROPERTY_KEY) > -1)
			dtmfEndValue = "undefined";
		else dtmfEndValue = "dtmf.match";

		return dtmfEndValue;
	}

	private void sendMediaResponse(final String dialogId, final String commandId, final String responseContent) {
		log.info("Notifying end of announcement");
		ConcurrentUpdateBlock concurrentUpdateBlock = new DialogConcurrentUpdateBlock(getDialogBeanHelper()) {
			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				assignSequenceNumber(dialogInfo, Request.INFO);
				getDialogCollection().replace(dialogInfo);

				Request playRequest = createMediaCommandCompleteRequest(dialogInfo, commandId, responseContent);
				getDialogBeanHelper().sendRequest(playRequest);
			}

			public String getResourceId() {
				return dialogId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	private void sendDtmfResponse(final String dialogId, final String commandId, final String audioFileUri) {
		String value = retrieveExpectedDtmfValue(audioFileUri);
		String dtmfEndValue = getDtmfEndValue(audioFileUri);

		final String dtmfResponseContent = new MsmlPromptAndCollectDigitsCollectedResponse(commandId, value, dtmfEndValue).getXml();
		sendMediaResponse(dialogId, commandId, dtmfResponseContent);
	}

    private String getRecordEndValue(String uri) {
    	StringBuffer result = new StringBuffer("record.");
        if (uri.contains("Failed")) {
            result.append("failed");
            if (uri.contains("Prespeech")) 
            	result.append(".prespeech");
        } else if (uri.contains("Terminated"))
            result.append("terminated");
        else
        	result.append("complete.maxlength");
        return result.toString();
    }

    private String getExpectedRecordLen(String uri) {
        int last = uri.lastIndexOf("_");
        return uri.substring(last+1);
    }

    private String getExpectedRecordId(String uri) {
        int first = uri.indexOf("_");
        int last = uri.lastIndexOf("_");
        if (first > -1 && last > -1 && first != last)
            return uri.substring(first+1, last);
        return uri.substring(first+1);
    }

    private void sendRecordResponse(final String dialogId, final String commandId, final String audioFileUri) {
        String recordId = getExpectedRecordId(audioFileUri);
        log.debug("recordId: " + recordId);
        String recordLen = getExpectedRecordLen(audioFileUri);
        log.debug("recordLen: " + recordLen);
        String recordEnd = getRecordEndValue(audioFileUri);
        log.debug("recordEnd: " + recordEnd);

        final String recordResponseContent = new MsmlPromptAndRecordRecordedResponse(commandId, recordId, recordLen, recordEnd).getXml();
        sendMediaResponse(dialogId, commandId, recordResponseContent);
    }
}
