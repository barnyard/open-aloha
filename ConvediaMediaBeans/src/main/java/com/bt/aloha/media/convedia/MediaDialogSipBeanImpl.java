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
package com.bt.aloha.media.convedia;

import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.header.ContentTypeHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.dialog.outbound.OutboundDialogSipBeanImpl;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.ImmutableDialogInfo;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.media.MediaDialogSipBean;
import com.bt.aloha.media.MediaDialogSipBeanListener;
import com.bt.aloha.media.convedia.msml.MsmlParseException;
import com.bt.aloha.media.convedia.msml.MsmlRequestParser;
import com.bt.aloha.media.convedia.msml.MsmlResponseParser;
import com.bt.aloha.media.convedia.msml.model.MsmlAnnouncementResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlDtmfGenerationResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndCollectDigitsAnnouncementResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndCollectDigitsCollectedResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndRecordAnnouncementResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndRecordRecordedResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlResponse;
import com.bt.aloha.media.event.dialog.AbstractMediaDialogCommandEvent;
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

public class MediaDialogSipBeanImpl extends OutboundDialogSipBeanImpl implements MediaDialogSipBean {
	private static final String IGNORING_ANNOUNCEMENT_COMPLETED_EVENT_FOR_PROMPT_COLLECT_COMMAND_S = "Ignoring announcement completed event for prompt & collect command %s";
    private static final String PROMPT_COLLECT_PLAY_END_S_PLAY_AMT_S = "Prompt & collect playEnd: %s, playAmt: %s";
    private static final Log LOG = LogFactory.getLog(MediaDialogSipBean.class);
    private static final String PLAY_COMPLETE = "play.complete";
    private static final String PLAY_TERMINATED = "play.terminated";

    public MediaDialogSipBeanImpl() {
    	super();
    }

	public void addMediaDialogSipBeanListener(MediaDialogSipBeanListener listener) {
		this.addDialogSipListener(listener);
	}

	public void removeMediaDialogSipBeanListener(MediaDialogSipBeanListener listener) {
		this.removeDialogSipListener(listener);
	}

    @Override
	public void processInfo(final Request request, final ServerTransaction serverTransaction, String dialogId) {
		ReadOnlyDialogInfo dialogInfo = getDialogCollection().get(dialogId);
		DialogState dialogState = dialogInfo.getDialogState();
		if (!dialogState.equals(DialogState.Confirmed)) {
			sendBadRequestResponse(request, serverTransaction, dialogInfo, String.format("INFO received for non-connected call: %s - status is %s", dialogId, dialogState));
			return;
		}

		ContentTypeHeader contentTypeHeader = (ContentTypeHeader)request.getHeader(ContentTypeHeader.NAME);
		if(contentTypeHeader == null) {
			sendBadRequestResponse(request, serverTransaction, dialogInfo, String.format("INFO request with no content type received: %s", dialogId));
			return;
		}

		String contentType = String.format("%s/%s", contentTypeHeader.getContentType(), contentTypeHeader.getContentSubType());
		if(contentType == null || !contentType.equals("application/msml+xml")) {
			sendBadRequestResponse(request, serverTransaction, dialogInfo, String.format("INFO request with unexpected content type of %s received: %s", contentType, dialogId));
			return;
		}

		byte[] rawContent = request.getRawContent();
		if(rawContent == null) {
			sendBadRequestResponse(request, serverTransaction, dialogInfo, String.format("INFO request with no content received: %s", dialogId));
			return;
		}

		MsmlResponseParser msmlResponseParser = new MsmlResponseParser();
		MsmlResponse response = msmlResponseParser.parse(new String(rawContent));

		LOG.info("Msml content:" + new String(rawContent));
		// Do stuff with the content
		try {
			LOG.debug(String.format("Received INFO request with media event %s", response.getClass().getName()));

			String commandId = response.getCommandId();
			if (response instanceof MsmlPromptAndCollectDigitsAnnouncementResponse) {
				String playEnd = ((MsmlPromptAndCollectDigitsAnnouncementResponse)response).getPlayEnd();
				String duration = ((MsmlPromptAndCollectDigitsAnnouncementResponse)response).getPlayAmount();
	            LOG.debug(String.format(PROMPT_COLLECT_PLAY_END_S_PLAY_AMT_S, playEnd, duration));

	            // Could be play.complete or play.complete.barged
				if (playEnd != null && playEnd.startsWith(PLAY_COMPLETE)) {
					LOG.debug(String.format(IGNORING_ANNOUNCEMENT_COMPLETED_EVENT_FOR_PROMPT_COLLECT_COMMAND_S, commandId));
				} else if (playEnd != null && playEnd.startsWith(PLAY_TERMINATED)) {
					DialogPromptAndCollectDigitsTerminatedEvent event = new DialogPromptAndCollectDigitsTerminatedEvent(dialogInfo.getId(), commandId, "", playEnd);
	            	getEventDispatcher().dispatchEvent(getDialogListeners(), event);
				} else {
					DialogPromptAndCollectDigitsFailedEvent event = new DialogPromptAndCollectDigitsFailedEvent(dialogInfo.getId(), commandId, "", playEnd);
	            	getEventDispatcher().dispatchEvent(getDialogListeners(), event);
				}

            } else if (response instanceof MsmlPromptAndRecordAnnouncementResponse) {
                String playEnd = ((MsmlPromptAndRecordAnnouncementResponse)response).getPlayEnd();
                String duration = ((MsmlPromptAndRecordAnnouncementResponse)response).getPlayAmount();
                LOG.debug(String.format(PROMPT_COLLECT_PLAY_END_S_PLAY_AMT_S, playEnd, duration));

                // Could be play.complete or play.complete.barged
                if (playEnd != null && playEnd.startsWith(PLAY_COMPLETE)) {
                    LOG.debug(String.format(IGNORING_ANNOUNCEMENT_COMPLETED_EVENT_FOR_PROMPT_COLLECT_COMMAND_S, commandId));
                } else if (playEnd != null && playEnd.startsWith(PLAY_TERMINATED)) {
                    DialogPromptAndRecordTerminatedEvent event = new DialogPromptAndRecordTerminatedEvent(dialogInfo.getId(), commandId, playEnd);
                    getEventDispatcher().dispatchEvent(getDialogListeners(), event);
                } else {
                    DialogPromptAndRecordFailedEvent event = new DialogPromptAndRecordFailedEvent(dialogInfo.getId(), commandId, playEnd);
                    getEventDispatcher().dispatchEvent(getDialogListeners(), event);
                }
			} else if (response instanceof MsmlAnnouncementResponse) {
				/**
				 * Note that MsmlAnnouncementResponse comes AFTER MsmlPromptAndCollectDigitsAnnouncementResponse, as the latter extends the former
				 */
				String playEnd = ((MsmlAnnouncementResponse)response).getPlayEnd();
				String duration = ((MsmlAnnouncementResponse)response).getPlayAmount();
                LOG.debug(String.format("playEnd: %s, playAmt: %s", playEnd, duration));

				AbstractMediaDialogCommandEvent event;
				// Could be play.complete or play.complete.barged
				if (playEnd != null && (playEnd.startsWith(PLAY_COMPLETE) || playEnd.startsWith(PLAY_TERMINATED))) {
                    boolean barged = playEnd.indexOf("barged") > -1;
                    LOG.debug("barged: " + barged);
                    if (playEnd.startsWith(PLAY_COMPLETE))
                    	event = new DialogAnnouncementCompletedEvent(dialogInfo.getId(), commandId, duration, barged);
                    else
                    	event = new DialogAnnouncementTerminatedEvent(dialogInfo.getId(), commandId);
				} else {
					event = new DialogAnnouncementFailedEvent(dialogInfo.getId(), commandId);
				}
				getEventDispatcher().dispatchEvent(getDialogListeners(), event);
            } else if (response instanceof MsmlPromptAndRecordRecordedResponse) {
                String recordEnd = ((MsmlPromptAndRecordRecordedResponse)response).getRecordEnd();
                String recordId = ((MsmlPromptAndRecordRecordedResponse)response).getRecordId();
                String recordLen = ((MsmlPromptAndRecordRecordedResponse)response).getRecordLen();
                AbstractMediaDialogCommandEvent event;
                if (recordEnd.startsWith("record.complete.")) {
                    event = new DialogPromptAndRecordCompletedEvent(dialogInfo.getId(), commandId, recordEnd, recordId, recordLen);
                } else if ("record.terminated".equals(recordEnd)) {
                    event = new DialogPromptAndRecordTerminatedEvent(dialogInfo.getId(), commandId, recordEnd);
                } else {
                    event = new DialogPromptAndRecordFailedEvent(dialogInfo.getId(), commandId, recordEnd);
                }
                getEventDispatcher().dispatchEvent(getDialogListeners(), event);
			} else if (response instanceof MsmlPromptAndCollectDigitsCollectedResponse) {
				String digits = ((MsmlPromptAndCollectDigitsCollectedResponse)response).getDtmfDigits();
				String dtmfResult = ((MsmlPromptAndCollectDigitsCollectedResponse)response).getDtmfEnd();
                LOG.debug(String.format("digits: %s dtmf.end: %s", digits, dtmfResult));
				AbstractMediaDialogCommandEvent event;
                if ("dtmf.match".equals(dtmfResult)) {
                	event = new DialogPromptAndCollectDigitsCompletedEvent(dialogInfo.getId(), commandId, digits, dtmfResult);
                } else if ("dtmf.terminated".equals(dtmfResult)) {
                	event = new DialogPromptAndCollectDigitsTerminatedEvent(dialogInfo.getId(), commandId, digits, dtmfResult);
                }
                else {
                	event = new DialogPromptAndCollectDigitsFailedEvent(dialogInfo.getId(), commandId, digits, dtmfResult);
                }
            	getEventDispatcher().dispatchEvent(getDialogListeners(), event);
			} else if (response instanceof MsmlDtmfGenerationResponse) {
				String dtmfGenResult = ((MsmlDtmfGenerationResponse)response).getDtmfGenEnd();
                LOG.debug(String.format("dtmfgen: %s ", dtmfGenResult));
				AbstractMediaDialogCommandEvent event;
                if ("dtmfgen.complete".equals(dtmfGenResult)) {
                	event = new DialogDtmfGenerationCompletedEvent(dialogInfo.getId(), commandId);
                } else {
                	event = new DialogDtmfGenerationFailedEvent(dialogInfo.getId(), commandId);
                }
            	getEventDispatcher().dispatchEvent(getDialogListeners(), event);
			}

			LOG.info(String.format("Sending OK response to INFO request: %s", dialogId));
			getDialogBeanHelper().sendResponse(request, serverTransaction, Response.OK);
		} catch (MsmlParseException e) {
			LOG.info(String.format("Unable to parse info request: %s", dialogId));
			getDialogBeanHelper().sendResponse(request, serverTransaction, Response.BAD_REQUEST);
		}
	}

	@Override
	public void processInfoResponse(final ResponseEvent re, final String dialogId) {
        if (re.getResponse().getStatusCode() == Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST){
            super.processInfoResponse(re, dialogId);
            return;
        }
        if (re.getResponse().getStatusCode() != Response.OK) {
        	String commandId;
        	try {
        		String content = new String(re.getClientTransaction().getRequest().getRawContent());
        		MsmlRequestParser msmlRequestParser = new MsmlRequestParser();
        		MsmlRequest req = msmlRequestParser.parse(content);
        		commandId = req.getCommandId();
        	} catch(Exception e) {
        		throw new MsmlParseException(String.format("Unable to process failure response to INFO for %s", dialogId), e);
        	}

			final DialogAnnouncementFailedEvent announcementFailedEvent = new DialogAnnouncementFailedEvent(dialogId, commandId);
			getEventDispatcher().dispatchEvent(getDialogListeners(), announcementFailedEvent);
        }
	}

	protected void sendBadRequestResponse(Request request, ServerTransaction serverTransaction, ImmutableDialogInfo dialogInfo, String logMessage) {
		LOG.info(logMessage);
		getDialogBeanHelper().sendResponse(request, serverTransaction, Response.BAD_REQUEST);
		return;
	}
}
