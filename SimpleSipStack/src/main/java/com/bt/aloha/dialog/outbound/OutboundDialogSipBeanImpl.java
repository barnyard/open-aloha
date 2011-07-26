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
package com.bt.aloha.dialog.outbound;


import javax.sdp.MediaDescription;
import javax.sip.ResponseEvent;
import javax.sip.header.CSeqHeader;
import javax.sip.header.RecordRouteHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.dialog.DialogConcurrentUpdateBlock;
import com.bt.aloha.dialog.DialogSipBeanBase;
import com.bt.aloha.dialog.event.AbstractDialogEvent;
import com.bt.aloha.dialog.event.DialogAlertingEvent;
import com.bt.aloha.dialog.event.DialogConnectedEvent;
import com.bt.aloha.dialog.event.DialogConnectionFailedEvent;
import com.bt.aloha.dialog.event.DialogRefreshCompletedEvent;
import com.bt.aloha.dialog.event.ReceivedDialogRefreshEvent;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.ImmutableDialogInfo;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.dialog.state.TerminationMethod;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.util.ConcurrentUpdateBlock;

public class OutboundDialogSipBeanImpl extends DialogSipBeanBase implements OutboundDialogSipBean {
	private static final Log LOG = LogFactory.getLog(OutboundDialogSipBeanImpl.class);
	private ErrorResponseToTerminationCauseMapper errorResponseToTerminationCauseMapper;

    public OutboundDialogSipBeanImpl() {
        super();
        errorResponseToTerminationCauseMapper = new ErrorResponseToTerminationCauseMapper();
    }

	public void addOutboundDialogListener(OutboundDialogSipListener listener) {
		addDialogSipListener(listener);
	}

	public void removeOutboundDialogListener(OutboundDialogSipListener listener) {
		removeDialogSipListener(listener);
	}

	public void processResponse(ResponseEvent responseEvent, final ImmutableDialogInfo dialogInfo) {
		Response response = responseEvent.getResponse();
		String responseMethod = ((CSeqHeader) response.getHeader(CSeqHeader.NAME)).getMethod();
		if (responseMethod.equals(Request.INVITE)
				&& ((CSeqHeader) responseEvent.getResponse().getHeader(CSeqHeader.NAME)).getSeqNumber() == dialogInfo.getInitialInviteTransactionSequenceNumber()) {
			processInitialInviteResponse(responseEvent, dialogInfo.getId());
		} else if (responseMethod.equals(Request.CANCEL)) {
			processCancelResponse(responseEvent, dialogInfo.getId());
		} else {
			super.processResponse(responseEvent, dialogInfo);
		}
	}

	protected void processCancelResponse(final ResponseEvent re, final String dialogId) {
		LOG.debug(String.format("Default processing for CANCEL response for %s", dialogId));
		if (re.getResponse().getStatusCode() != Response.OK)
			LOG.warn(String.format("Dialog %s responded to CANCEL with status code %d", dialogId, re.getResponse().getStatusCode()));
	}

	protected void processInitialInviteResponse(final ResponseEvent re, final String dialogId) {
		LOG.debug(String.format("Processing initial INVITE response for %s", dialogId));
		if (re.getResponse().getStatusCode() >= Response.BAD_REQUEST) {
			if ( (re.getResponse().getStatusCode() == Response.PROXY_AUTHENTICATION_REQUIRED || re.getResponse().getStatusCode() == Response.UNAUTHORIZED) 
					&& ((CSeqHeader) re.getResponse().getHeader(CSeqHeader.NAME)).getSeqNumber() < 2) { // to stop looping if server keeps responding with 401/407
				processInitialInviteUnauthorised(re, dialogId);
			}
			else
				processInitialInviteErrorResponse(re, dialogId);
		} else if (re.getResponse().getStatusCode() < Response.OK) {
			processInitialInviteProvisionalResponse(re, dialogId);
		} else if (re.getResponse().getStatusCode() == Response.OK) {
			processInitialInviteOkResponse(re, dialogId);
		}
	}

	private void processInitialInviteUnauthorised(ResponseEvent re,	String dialogId) {
		// Send Invite with digest response.
		sendAuthorisationReInvite(dialogId, re.getResponse());
	}
	
	private void processInitialInviteProvisionalResponse(final ResponseEvent responseEvent, final String dialogId) {
		LOG.debug(String.format("Processing PROVISIONAL response for outbound dialog %s", dialogId));
		ConcurrentUpdateBlock concurrentUpdateBlock = new DialogConcurrentUpdateBlock(getDialogBeanHelper()) {
			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				if (dialogInfo.setDialogState(DialogState.Early) != null) {

					boolean releaseCancel = TerminationMethod.Cancel.ordinal() <= dialogInfo.getTerminationMethod().ordinal();
					if (releaseCancel) {
						forceSequenceNumber(dialogInfo.getSipCallId(), dialogInfo.getSequenceNumber(), Request.CANCEL);
					}
					
					getDialogCollection().replace(dialogInfo);

					if (releaseCancel) {
						LOG.debug(String.format("Releasing a pending CANCEL for outbound dialog %s", dialogId));
						Request cancelRequest = getDialogBeanHelper().createCancelRequest(dialogInfo);
						getDialogBeanHelper().sendRequest(cancelRequest);
					}
				}

				if (dialogInfo.getDialogState().ordinal() < DialogState.Confirmed.ordinal() && responseEvent.getResponse().getStatusCode() == Response.RINGING) {
					final DialogAlertingEvent alertingEvent = new DialogAlertingEvent(dialogId);
					getEventDispatcher().dispatchEvent(getDialogListeners(), alertingEvent);
				}
			}

			public String getResourceId() {
				return dialogId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	private void processInitialInviteErrorResponse(final ResponseEvent responseEvent, final String dialogId) {
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				LOG.info(String.format("Got error response %d for dialog %s, terminating", responseEvent.getResponse().getStatusCode(), dialogInfo.getId()));
				dialogInfo.setTerminationMethod(TerminationMethod.None);
				dialogInfo.setTerminationCause(errorResponseToTerminationCauseMapper.map(responseEvent.getResponse().getStatusCode()));
				dialogInfo.setInviteClientTransaction(null);
				if (dialogInfo.setDialogState(DialogState.Terminated) != null) {
					getDialogCollection().replace(dialogInfo);

					final DialogConnectionFailedEvent connectionFailedEvent = new DialogConnectionFailedEvent(dialogInfo.getId(), dialogInfo.getTerminationCause());
					getEventDispatcher().dispatchEvent(getDialogListeners(), connectionFailedEvent);
				}
			}

			public String getResourceId() {
				return dialogId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	private void processInitialInviteOkResponse(final ResponseEvent responseEvent, final String dialogId) {
		ConcurrentUpdateBlock concurrentUpdateBlock = new DialogConcurrentUpdateBlock(getDialogBeanHelper()) {
			public void execute() {
				LOG.debug(String.format(PROCESSING_OK_RESPONSE_FOR_OUTBOUND_DIALOG_S, dialogId));
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);

				if (handleInviteOkResponseIfResent(responseEvent, dialogInfo)) {
					LOG.debug(String.format("Handled resent OK response for dialog %s", dialogId));
					return;
				}

				if (dialogInfo.setDialogState(DialogState.Confirmed) != null) {
					LOG.info(String.format("Dialog %s (with app data %s) connected", dialogInfo.getId(), dialogInfo.getApplicationData()));

					updateDialogInfoFromInviteOkResponse(dialogInfo, responseEvent.getResponse());
					dialogInfo.setRouteList(responseEvent.getResponse().getHeaders(RecordRouteHeader.NAME), true);
					dialogInfo.setRemoteTag(((ToHeader) responseEvent.getResponse().getHeader(ToHeader.NAME)).getTag());
					dialogInfo.setInviteClientTransaction(null);

					boolean shouldSendAck = dialogInfo.isSdpInInitialInvite() || dialogInfo.isAutomaticallyPlaceOnHold();
					Request ackRequest = null;
					if (shouldSendAck) {
						forceSequenceNumber(dialogInfo.getSipCallId(), dialogInfo.getLastReceivedOkSequenceNumber(), Request.ACK);
						ackRequest = getDialogBeanHelper().createInviteOkAckRequest(dialogInfo, responseEvent);
						if (!dialogInfo.isSdpInInitialInvite()) {
							LOG.debug(String.format("SDP not in initial invite for dialog %s, adding it to ACK content", dialogInfo.getId()));
							MediaDescription holdMediaDescription = SessionDescriptionHelper.generateHoldMediaDescription(dialogInfo.getRemoteOfferMediaDescription());
							addContentToAckRequest(ackRequest, dialogInfo, holdMediaDescription);
						} else {
							dialogInfo.setLastAckRequest(ackRequest);
						}
					}

					if (dialogInfo.getTerminationMethod().equals(TerminationMethod.Cancel)) {
						dialogInfo.setTerminationMethod(TerminationMethod.None);
						dialogInfo.setTerminationCause(null);
					}
					getDialogCollection().replace(dialogInfo);

					final AbstractDialogEvent event;
					if (shouldSendAck) {
						getDialogBeanHelper().sendRequest(ackRequest);
						LOG.debug(String.format("Raising dialog connected event for dialog %s with app data %s", dialogId, dialogInfo.getApplicationData()));
						MediaDescription mediaDescriptionForEvent;
						if (dialogInfo.isSdpInInitialInvite())
							mediaDescriptionForEvent = getDialogBeanHelper().getActiveMediaDescriptionFromMessageBody(new String(responseEvent.getResponse().getRawContent()));
						else
							mediaDescriptionForEvent = dialogInfo.getRemoteOfferMediaDescription();

						event = new DialogConnectedEvent(dialogInfo.getId(), dialogInfo.getApplicationData(), mediaDescriptionForEvent);
					} else {
						if (dialogInfo.isSdpInInitialInvite()) {
							MediaDescription negotiatedMediaDescription = getDialogBeanHelper().getActiveMediaDescriptionFromMessageBody(new String(responseEvent.getResponse().getRawContent()));
							event = new DialogRefreshCompletedEvent(dialogInfo.getId(), dialogInfo.getApplicationData(), negotiatedMediaDescription);
						} else {
							event = new ReceivedDialogRefreshEvent(dialogInfo.getId(), dialogInfo.getRemoteOfferMediaDescription(), dialogInfo.getRemoteContact().getURI().toString(),  dialogInfo.getApplicationData(), true);
						}
					}
					getEventDispatcher().dispatchEvent(getDialogListeners(), event);

					// send out any pending reinvites that exist
					if (shouldSendAck && dialogInfo.getPendingReinvite() != null) {
						LOG.debug(String.format("Sending out queued reinvite for dialog %s", dialogId));
						reinviteDialog(dialogInfo.getId(), dialogInfo.getPendingReinvite().getMediaDescription(), dialogInfo.getPendingReinvite().getAutoTerminate(), dialogInfo.getPendingReinvite().getApplicationData());
					}

					if (dialogInfo.getTerminationMethod().equals(TerminationMethod.Terminate)) {
						// TODO: LOW: use own injected task executor!
						getEventDispatcher().getTaskExecutor().execute(new DialogTerminationTask(dialogId));
					}
				}
			}

			public String getResourceId() {
				return dialogId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	protected void sendInitialInviteAck(final String dialogId, final MediaDescription mediaDescription) {
		ConcurrentUpdateBlock concurrentUpdateBlock = new DialogConcurrentUpdateBlock(getDialogBeanHelper()) {
			public void execute() {
				LOG.debug(String.format("Sending INITIAL Invite ACK for dialog %s", dialogId));
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);

				forceSequenceNumber(dialogInfo.getSipCallId(), dialogInfo.getSequenceNumber(), Request.ACK);
				Request ackRequest = getDialogBeanHelper().createAckRequest(dialogInfo);
				if (!dialogInfo.isSdpInInitialInvite()) {
					LOG.debug(String.format("SDP not in initial invite for dialog %s, adding media description to ACK content", dialogInfo.getId()));
					addContentToAckRequest(ackRequest, dialogInfo, mediaDescription);
				}

				getDialogCollection().replace(dialogInfo);

				getDialogBeanHelper().sendRequest(ackRequest);

				if (!dialogInfo.isSdpInInitialInvite()) {
					DialogConnectedEvent event = new DialogConnectedEvent(dialogInfo.getId(), dialogInfo.getApplicationData(), dialogInfo.isSdpInInitialInvite() ? null : mediaDescription);
					getEventDispatcher().dispatchEvent(getDialogListeners(), event);
				}

				// send out any pending reinvites that exist
				if (dialogInfo.getPendingReinvite() != null)
					reinviteDialog(dialogInfo.getId(), dialogInfo.getPendingReinvite().getMediaDescription(), dialogInfo.getPendingReinvite().getAutoTerminate(), dialogInfo.getPendingReinvite().getApplicationData());
			}

			public String getResourceId() {
				return dialogId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}
	


	protected void endNonConfirmedDialog(ReadOnlyDialogInfo readOnlyDialogInfo, TerminationMethod previousTerminationMethod) {
		if (DialogState.Early.equals(readOnlyDialogInfo.getDialogState()) && !TerminationMethod.Cancel.equals(previousTerminationMethod)) {
			LOG.debug(String.format("Terminating Early dialog %s by sending CANCEL", readOnlyDialogInfo.getId()));
			CSeqHeader requestCSeqHeader = (CSeqHeader)((DialogInfo)readOnlyDialogInfo).getInviteClientTransaction().getRequest().getHeader(CSeqHeader.NAME);
			getDialogBeanHelper().enqueueRequestForceSequenceNumber(readOnlyDialogInfo.getId(), requestCSeqHeader.getSeqNumber(), Request.CANCEL);
			Request cancelRequest = getDialogBeanHelper().createCancelRequest(readOnlyDialogInfo);
			getDialogBeanHelper().sendRequest(cancelRequest);
		} else {
			LOG.debug(String.format("Dialog state %s and previous termination method %s for dialog %s mean terminateDialog is doing nothing",
					readOnlyDialogInfo.getDialogState(), previousTerminationMethod, readOnlyDialogInfo.getId()));
		}
	}
}
