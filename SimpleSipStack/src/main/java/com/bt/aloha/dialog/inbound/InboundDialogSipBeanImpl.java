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
package com.bt.aloha.dialog.inbound;

import javax.sdp.MediaDescription;
import javax.sip.ServerTransaction;
import javax.sip.TransactionState;
import javax.sip.header.CSeqHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.dialog.DialogSipBeanBase;
import com.bt.aloha.dialog.event.DialogConnectedEvent;
import com.bt.aloha.dialog.event.DialogConnectionFailedEvent;
import com.bt.aloha.dialog.event.IncomingAction;
import com.bt.aloha.dialog.event.IncomingDialogEvent;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.ImmutableDialogInfo;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.dialog.state.TerminationCause;
import com.bt.aloha.dialog.state.TerminationMethod;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.util.ConcurrentUpdateBlock;

public class InboundDialogSipBeanImpl extends DialogSipBeanBase implements InboundDialogSipBean {
	private static final Log LOG = LogFactory.getLog(InboundDialogSipBeanImpl.class);

    public InboundDialogSipBeanImpl() {
        super();
    }

	public void addInboundDialogListener(InboundDialogSipListener listener) {
		addDialogSipListener(listener);
	}

	public void removeInboundDialogListener(InboundDialogSipListener listener) {
		removeDialogSipListener(listener);
	}
	
	@Override
	public void processRequest(Request request, ServerTransaction serverTransaction, final ImmutableDialogInfo dialogInfo) {
		String dialogId = dialogInfo.getId();
		if (request.getMethod().equals(Request.INVITE)
				&& (((CSeqHeader) request.getHeader(CSeqHeader.NAME)).getSeqNumber() == dialogInfo.getInitialInviteTransactionSequenceNumber())) {
			LOG.debug(String.format("Processing initial INVITE request for %s", dialogId));
			processInitialInvite(request, serverTransaction, dialogId);
		} else if (request.getMethod().equals(Request.ACK)
				&& (((CSeqHeader) request.getHeader(CSeqHeader.NAME)).getSeqNumber() == dialogInfo.getInitialInviteTransactionSequenceNumber())) {
			processInitialInviteAck(request, serverTransaction, dialogId);
		} else if (request.getMethod().equals(Request.CANCEL)) {
			processCancel(request, serverTransaction, dialogId);
		} else {
			super.processRequest(request, serverTransaction, dialogInfo);
		}
	}

	protected void endNonConfirmedDialog(ReadOnlyDialogInfo readOnlyDialogInfo, TerminationMethod previousTerminationMethod) {
		if (DialogState.Initiated.equals(readOnlyDialogInfo.getDialogState())) {
			// TODO: MED - can we map termination cause to sip respoonse?
			sendInitialErrorResponse(readOnlyDialogInfo.getId(), Response.TEMPORARILY_UNAVAILABLE);
		} else {
			LOG.debug(String.format("Dialog state %s and termination method %s for dialog %s mean terminateDialog is doing nothing", readOnlyDialogInfo.getDialogState(), previousTerminationMethod, readOnlyDialogInfo.getId()));
		}
	}

	protected void processInitialInvite(final Request request, final ServerTransaction serverTransaction, final String dialogId) {
		IncomingDialogEvent incomingDialogEvent = new IncomingDialogEvent(dialogId);
		incomingDialogEvent.setFromUri(((FromHeader) request.getHeader(FromHeader.NAME)).getAddress().getURI().toString());
		incomingDialogEvent.setToUri(((ToHeader) request.getHeader(ToHeader.NAME)).getAddress().getURI().toString());
		incomingDialogEvent.setRequestUri(request.getRequestURI().toString());

		int numEventsDelivered = getEventDispatcher().dispatchEvent(getDialogListeners(), incomingDialogEvent, false, 1);
		if (numEventsDelivered < 1) {
			LOG.info(String.format("No matching inbound handler found, sending Not Found response"));
			getDialogBeanHelper().sendResponse(request, serverTransaction, Response.NOT_FOUND);
			return;
		} else if (numEventsDelivered > 1) {
			LOG.warn(String.format("%d IncomingDialog events delivered - shouldn't happen!", numEventsDelivered));
		}

		if (incomingDialogEvent.getIncomingAction().equals(IncomingAction.Reject)) {
			LOG.info(String.format("Rejecting dialog %s", incomingDialogEvent.getId()));
			sendInitialErrorResponse(dialogId, incomingDialogEvent.getResponseCode().getSipResponseCode());
		} else if (incomingDialogEvent.getIncomingAction().equals(IncomingAction.PlaceOnHold)) {
			LOG.info(String.format("Putting dialog %s on hold", incomingDialogEvent.getId()));
			sendInitialOkResponse(dialogId);
		} else if (incomingDialogEvent.getIncomingAction().equals(IncomingAction.None)) {
			LOG.info(String.format("Doing nothing with dialog %s", incomingDialogEvent.getId()));
		} else {
			LOG.warn(String.format("Unknown inbound action for dialog %s", incomingDialogEvent.getId()));
		}
	}
	
	protected void processInitialInviteAck(final Request request, final ServerTransaction serverTransaction, final String dialogId) {
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				if (dialogInfo.setDialogState(DialogState.Confirmed) != null) {
					LOG.info(String.format(DIALOG_S_CONNECTED, dialogId));
					String remoteSdp = getDialogBeanHelper().getRemoteSdpFromRequest(request);
					MediaDescription answerMediaDescription = null;
					if (remoteSdp != null) {
						LOG.debug(String.format(FOUND_SDP_IN_ACK_FOR_DIALOG_S_S, dialogId, remoteSdp));
						answerMediaDescription = getDialogBeanHelper().getActiveMediaDescriptionFromMessageBody(remoteSdp);
						SessionDescriptionHelper.updateDynamicMediaPayloadMappings(answerMediaDescription, dialogInfo.getDynamicMediaPayloadTypeMap());
					}

					getDialogCollection().replace(dialogInfo);
					final DialogConnectedEvent connectedEvent = new DialogConnectedEvent(dialogId, dialogInfo.getApplicationData(), answerMediaDescription);
					getEventDispatcher().dispatchEvent(getDialogListeners(), connectedEvent);
				}
			}

			public String getResourceId() {
				return dialogId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}
	
	protected void processCancel(final Request request, final ServerTransaction serverTransaction, final String dialogId) {
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				dialogInfo.setTerminationCause(TerminationCause.RemotePartyHungUp);
				if (dialogInfo.getInviteServerTransaction() != null) {
					ServerTransaction serverTransaction = dialogInfo.getInviteServerTransaction();
					dialogInfo.setInviteServerTransaction(null);
					if (serverTransaction.getState().getValue() < TransactionState._COMPLETED) {
						if (dialogInfo.setDialogState(DialogState.Terminated) != null) {							
							getDialogCollection().replace(dialogInfo);

							LOG.info("sending Request Terminated response...");
							getDialogBeanHelper().sendResponse(serverTransaction.getRequest(), serverTransaction, Response.REQUEST_TERMINATED);

							final DialogConnectionFailedEvent connectionFailedEvent = new DialogConnectionFailedEvent(dialogInfo.getId(), dialogInfo.getTerminationCause());
							getEventDispatcher().dispatchEvent(getDialogListeners(), connectionFailedEvent);
						}
					} else {
						LOG.info(String.format("Can't cancel dialog %s, transaction state is COMPLETED", dialogInfo.getId()));
					}
				} else {
					LOG.warn(String.format("Got CANCEL for dialog %s but no invite transaction found", dialogInfo.getId()));
				}
				LOG.debug(String.format("Responding with OK to CANCEL request for %s", dialogInfo.getId()));
				getDialogBeanHelper().sendResponse(request, serverTransaction, Response.OK);
			}

			public String getResourceId() {
				return dialogId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	protected boolean sendInitialOkResponse(final String dialogId) {
		return sendInitialOkResponse(dialogId, null, null);
	}

	// TODO: Write unit test to ensure we set application data in dialogInfo
	protected boolean sendInitialOkResponse(final String dialogId, final MediaDescription mediaDescription, final String applicationData) {
		class MyConcurrentUpdateBlock implements ConcurrentUpdateBlock {
			private boolean responseSent;
			
			MyConcurrentUpdateBlock() {
			}

			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				dialogInfo.setApplicationData(applicationData);
				if (dialogInfo.setDialogState(DialogState.Early) != null) {
					MediaDescription newMediaDescription = mediaDescription;
					if(newMediaDescription == null) {
						if(dialogInfo.isSdpInInitialInvite()) {
							newMediaDescription = SessionDescriptionHelper.generateHoldMediaDescription(dialogInfo.getRemoteOfferMediaDescription());
						} else {
							newMediaDescription = SessionDescriptionHelper.generateHoldMediaDescription();
						}
					}
					
					SessionDescriptionHelper.setMediaDescription(dialogInfo.getSessionDescription(), newMediaDescription, dialogInfo.getDynamicMediaPayloadTypeMap());
					
					if(!dialogInfo.isSdpInInitialInvite())
						dialogInfo.setRemoteOfferMediaDescription(newMediaDescription);
					
					String sipUserName = dialogInfo.getSipUserName();
					if (sipUserName == null)
						sipUserName = "";
					
					ServerTransaction inviteServerTransaction = dialogInfo.getInviteServerTransaction(); 
					dialogInfo.setInviteServerTransaction(null);
					getDialogCollection().replace(dialogInfo);

					getDialogBeanHelper().sendInviteOkResponse(inviteServerTransaction.getRequest(), inviteServerTransaction,
							dialogInfo.getLocalTag(), sipUserName, dialogInfo.getSessionDescription());
					responseSent = true;
				}
			}

			public String getResourceId() {
				return dialogId;
			}
		}
		MyConcurrentUpdateBlock myConcurrentUpdateBlock = new MyConcurrentUpdateBlock();
		getConcurrentUpdateManager().executeConcurrentUpdate(myConcurrentUpdateBlock);
		return myConcurrentUpdateBlock.responseSent;
	}

	protected boolean sendInitialErrorResponse(final String dialogId, final int statusCode) {
		class MyConcurrentUpdateBlock implements ConcurrentUpdateBlock {
			private boolean responseSent;

			MyConcurrentUpdateBlock() {
			}

			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				
				ServerTransaction serverTransaction = dialogInfo.getInviteServerTransaction();
				dialogInfo.setInviteServerTransaction(null);
				// TODO: HIGH set termination cause - could be rejected if rejected by app, or a failure due to outbound leg failing
				if (dialogInfo.setDialogState(DialogState.Terminated) != null) {
					getDialogCollection().replace(dialogInfo);

					getDialogBeanHelper().sendResponse(serverTransaction.getRequest(), serverTransaction,statusCode);
					responseSent = true;
				}
			}

			public String getResourceId() {
				return dialogId;
			}
		}
		MyConcurrentUpdateBlock myConcurrentUpdateBlock = new MyConcurrentUpdateBlock();
		getConcurrentUpdateManager().executeConcurrentUpdate(myConcurrentUpdateBlock);
		return myConcurrentUpdateBlock.responseSent;
	}

	protected boolean sendRingingResponse(final String dialogId) {
		class MyConcurrentUpdateBlock implements ConcurrentUpdateBlock {
			private boolean responseSent;

			MyConcurrentUpdateBlock() {
			}

			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				ServerTransaction serverTransaction = dialogInfo.getInviteServerTransaction();
				if (DialogState.Early.ordinal() > dialogInfo.getDialogState().ordinal()) {
					getDialogCollection().replace(dialogInfo);

					getDialogBeanHelper().sendResponse(serverTransaction.getRequest(), serverTransaction, Response.RINGING);
					responseSent = true;
				} else {
					LOG.debug(String.format("Not sending RINGING response for dialog %s, already reached Early", dialogId));
				}
			}

			public String getResourceId() {
				return dialogId;
			}
		}
		MyConcurrentUpdateBlock myConcurrentUpdateBlock = new MyConcurrentUpdateBlock();
		getConcurrentUpdateManager().executeConcurrentUpdate(myConcurrentUpdateBlock);
		return myConcurrentUpdateBlock.responseSent;
	}
}
