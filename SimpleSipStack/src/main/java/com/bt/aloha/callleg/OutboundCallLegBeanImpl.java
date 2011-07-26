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
package com.bt.aloha.callleg;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.sdp.MediaDescription;
import javax.sip.ClientTransaction;
import javax.sip.ResponseEvent;
import javax.sip.TransactionUnavailableException;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.dialog.DialogBeanHelper;
import com.bt.aloha.dialog.DialogConcurrentUpdateBlock;
import com.bt.aloha.dialog.DialogSipListener;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.event.DialogConnectionFailedEvent;
import com.bt.aloha.dialog.outbound.OutboundDialogSipBeanImpl;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.dialog.state.ReinviteInProgress;
import com.bt.aloha.dialog.state.TerminationCause;
import com.bt.aloha.dialog.state.TerminationMethod;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.stack.StackException;
import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateManager;
import com.bt.aloha.util.HousekeeperAware;

public class OutboundCallLegBeanImpl extends OutboundDialogSipBeanImpl implements OutboundCallLegBean, HousekeeperAware {
	private static final String URI_TO_FROM_SHOULD_NOT_BE_NULL = "URI to/from should not be null";
	private static final Log LOG = LogFactory.getLog(OutboundCallLegBeanImpl.class);
	private ScheduledExecutorService scheduledExecutorService = null;
	private URIParser uriParser = null;
		
	private CallLegHelper callLegHelper = new CallLegHelper() {
		@Override
		protected void endNonConfirmedDialog(ReadOnlyDialogInfo dialogInfo, TerminationMethod previousTerminationMethod) {
			OutboundCallLegBeanImpl.this.endNonConfirmedDialog(dialogInfo, previousTerminationMethod);
		}

		@Override
		protected ConcurrentUpdateManager getConcurrentUpdateManager() {
			return OutboundCallLegBeanImpl.this.getConcurrentUpdateManager();
		}

		@Override
		protected DialogCollection getDialogCollection() {
			return OutboundCallLegBeanImpl.this.getDialogCollection();
		}

		@Override
		protected void acceptReceivedMediaOffer(String dialogId, MediaDescription mediaDescription, boolean offerInOkResponse, boolean initialInviteTransactionCompleted) {
			LOG.debug(String.format("Accepting media offer for call leg %s, offer in ok response is %s, initial tx completed is %s", dialogId, offerInOkResponse, initialInviteTransactionCompleted));
			if (initialInviteTransactionCompleted) {
				if (offerInOkResponse)
					OutboundCallLegBeanImpl.this.sendReinviteAck(dialogId, mediaDescription);
				else
					OutboundCallLegBeanImpl.this.sendReinviteOkResponse(dialogId, mediaDescription);
			} else {
				OutboundCallLegBeanImpl.this.sendInitialInviteAck(dialogId, mediaDescription);
			}
		}
	};

	public OutboundCallLegBeanImpl() {
        super();
    }

   	@Override
	public DialogBeanHelper getDialogBeanHelper() {
   		return callLegHelper;
	}

   	public void setCallLegHelper(CallLegHelper aCallLegHelper) {
   		this.callLegHelper = aCallLegHelper;
   	}

	public void setScheduledExecutorService(ScheduledExecutorService aScheduledExecutorService) {
		this.scheduledExecutorService = aScheduledExecutorService;
	}
	
	public void setURIParser(URIParser aURIParser) {
		this.uriParser = aURIParser;
	}

	public String createCallLeg(URI from, URI to) {
		return createCallLeg(from, to, 0);
	}

	public String createCallLeg(URI from, URI to, int callAnswerTimeout) {
		if (null == to || null == from){
			throw new IllegalArgumentException(URI_TO_FROM_SHOULD_NOT_BE_NULL);
		}
		LOG.debug(String.format("Creating dialog from %s to %s", from, to));
		String dialogId = getSimpleSipStack().getSipProvider().getNewCallId().getCallId();
		URIParameters uriParameters = uriParser.parseURI(to);
		DialogInfo dialogInfo = new DialogInfo(dialogId, getBeanName(), getSimpleSipStack().getIpAddress(), from.toString(), uriParameters.getStrippedURI().toString(), getSimpleSipStack().generateNewTag(), callAnswerTimeout, false,false, uriParameters.getUsername(), uriParameters.getPassword());
		getDialogCollection().add(dialogInfo);
		return dialogInfo.getId();
	}

	public void connectCallLeg(String dialogId) {
		connectCallLeg(dialogId, AutoTerminateAction.Unchanged);
	}

	public void connectCallLeg(final String dialogId, final AutoTerminateAction autoTerminate) {
		connectCallLeg(dialogId, autoTerminate, null, null, true);
	}

	public void connectCallLeg(final String dialogId, final AutoTerminateAction autoTerminate, final String applicationData, final MediaDescription mediaDescription, final boolean shouldPlaceOnHold) {
		LOG.debug(String.format("Connecting dialog %s with autoTerminate set to %s, app data %s, should place on hold %s", dialogId, autoTerminate, applicationData, shouldPlaceOnHold));
        ConcurrentUpdateBlock concurrentUpdateBlock = new DialogConcurrentUpdateBlock(getDialogBeanHelper()) {
			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				DialogState originalDialogState = dialogInfo.getDialogState();
				TerminationMethod originalTerminationMethod = dialogInfo.getTerminationMethod();
				if (originalDialogState.ordinal() > DialogState.Initiated.ordinal()) {
					throw new IllegalStateException(String.format("Failed to connect dialog %s, current state is %s", dialogId, originalDialogState));
				} else if (DialogState.Initiated.equals(originalDialogState)) {
					LOG.warn(String.format("Dialog %s already Initiated, aborting connect dialog request", dialogId));
					return;
				} else if (TerminationMethod.None.ordinal() < originalTerminationMethod.ordinal()) {
					if(dialogInfo.setDialogState(DialogState.Terminated) != null) {
						LOG.info(String.format("Dialog %s being cancelled or terminated, aborting connect dialog request", dialogId));
						dialogInfo.setTerminationCause(TerminationCause.TerminatedByServer);
						getDialogCollection().replace(dialogInfo);
						DialogConnectionFailedEvent connectionFailedEvent = new DialogConnectionFailedEvent(dialogId, dialogInfo.getTerminationCause());
						getEventDispatcher().dispatchEvent(getDialogListeners(), connectionFailedEvent);
						return;
					}
				}

				dialogInfo.setSdpInInitialInvite(mediaDescription != null);
				dialogInfo.setAutomaticallyPlaceOnHold(shouldPlaceOnHold);
				dialogInfo.setApplicationData(applicationData);
				forceSequenceNumber(dialogInfo.getSipCallId(), dialogInfo.getSequenceNumber(), Request.INVITE);
				if (mediaDescription != null) {
					SessionDescriptionHelper.setMediaDescription(dialogInfo.getSessionDescription(), mediaDescription);
				}

				if (autoTerminate != AutoTerminateAction.Unchanged)
					dialogInfo.setAutoTerminate(autoTerminate.getBoolean());

				Request request = getDialogBeanHelper().createInitialInviteRequest(dialogInfo.getRemoteParty().getURI().toString(), dialogInfo);
				LOG.debug(String.format("Created INVITE request for dialog %s", dialogId));

				ClientTransaction clientTransaction;
				try {
					clientTransaction = getSimpleSipStack().getSipProvider().getNewClientTransaction(request);
				} catch (TransactionUnavailableException e) {
					throw new StackException(e.getMessage(), e);
				}
				LOG.debug(String.format("Created client transaction for dialog %s", dialogId));

                dialogInfo.setDialogState(DialogState.Initiated);
                dialogInfo.setInviteClientTransaction(clientTransaction);

                replaceDialogIfCanSendRequest(Request.INVITE, originalDialogState, dialogInfo.getTerminationMethod(), dialogInfo);

                LOG.debug(String.format("Sending INVITE request for dialog %s with app data %s", dialogId, applicationData));
                getSimpleSipStack().sendRequest(clientTransaction);
			}
			public String getResourceId() {
				return dialogId;
			}
        };
        getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);

        // Answer timeout handling
        ReadOnlyDialogInfo dialogInfo = getDialogCollection().get(dialogId);
		if (dialogInfo.getCallAnswerTimeout() > 0) {
			LOG.debug(String.format("Starting answer timeout timer with timeout %d", dialogInfo.getCallAnswerTimeout()));
			scheduledExecutorService.schedule(new Runnable() {
				public void run() {
					ReadOnlyDialogInfo dialogInfo = getDialogCollection().get(dialogId);
					if (dialogInfo.getDialogState().ordinal() <= DialogState.Early.ordinal()) {
						cancelCallLeg(dialogId, TerminationCause.CallAnswerTimeout);
					}
				}
			}, dialogInfo.getCallAnswerTimeout(), TimeUnit.MILLISECONDS);
		}
	}

	public void reinviteCallLeg(String dialogId, MediaDescription offerMediaDescription, AutoTerminateAction autoTerminate, String applicationData) {
		super.reinviteDialog(dialogId, offerMediaDescription, autoTerminate.getBoolean(), applicationData);
	}

	public void cancelCallLeg(final String dialogId) {
		cancelCallLeg(dialogId, TerminationCause.TerminatedByServer);
	}

	public void cancelCallLeg(final String dialogId, final TerminationCause aDialogTerminationCause) {
		LOG.info(String.format("Cancelling dialog %s with termination cause %s", dialogId, aDialogTerminationCause));
		ConcurrentUpdateBlock concurrentUpdateBlock = new DialogConcurrentUpdateBlock(getDialogBeanHelper()) {
			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				DialogState dialogState = dialogInfo.getDialogState();
				dialogInfo.setTerminationCause(aDialogTerminationCause);

				if(DialogState.Confirmed.ordinal() <= dialogState.ordinal()) {
					throw new IllegalStateException(String.format("Attempted to cancel a dialog with state %s", dialogInfo.getDialogState()));
				}

				if (dialogInfo.setTerminationMethod(TerminationMethod.Cancel) == null) {
					LOG.info(String.format("Failed to cancel dialog %s - already cancelling or terminating", dialogInfo.getId()));
					return;
				}
				forceSequenceNumber(dialogInfo.getSipCallId(), dialogInfo.getSequenceNumber(), Request.CANCEL);
				getDialogCollection().replace(dialogInfo);
				if (DialogState.Early.equals(dialogState)) {
					Request cancelRequest = getDialogBeanHelper().createCancelRequest(dialogInfo);
					getDialogBeanHelper().sendRequest(cancelRequest);
				}
			}
			public String getResourceId() {
				return dialogId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}
	
	@Override
	protected void processReinviteErrorResponse(ResponseEvent responseEvent, final String dialogId) {
		LOG.debug(String.format("Processing REINVITE error for dialog %s", dialogId));
//		If a UAC receives a 491 response to a re-INVITE, it SHOULD start a
//		   timer with a value T chosen as follows:
//
//		      1. If the UAC is the owner of the Call-ID of the dialog ID
//		         (meaning it generated the value), T has a randomly chosen value
//		         between 2.1 and 4 seconds in units of 10 ms.
//
//		      2. If the UAC is not the owner of the Call-ID of the dialog ID, T
//		         has a randomly chosen value of between 0 and 2 seconds in units
//		         of 10 ms.
//
//		   When the timer fires, the UAC SHOULD attempt the re-INVITE once more,
//		   if it still desires for that session modification to take place.  For
//		   example, if the call was already hung up with a BYE, the re-INVITE
//		   would not take place.
		if (responseEvent.getResponse().getStatusCode() == Response.REQUEST_PENDING) {
			LOG.debug(String.format("processing %d", Response.REQUEST_PENDING));

			ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
				public void execute() {
					final DialogInfo dialogInfo = getDialogCollection().get(dialogId);
					if ( ! dialogInfo.isInbound()) { //TODO: could probably remove this if statement, as we wouldn't be here for an inbound dialog
						Random random = new Random(System.currentTimeMillis());
						int milliSecondsToRetry = 2100 + (random.nextInt(19) * 100);
						LOG.debug(String.format("re-sending invite in %d millis", milliSecondsToRetry));
						dialogInfo.setReinviteInProgess(ReinviteInProgress.None);
						Runnable runnable = new Runnable() {
							public void run() {
								reinviteDialog(dialogId, SessionDescriptionHelper.getActiveMediaDescription(dialogInfo.getSessionDescription()), dialogInfo.isAutoTerminate(), dialogInfo.getApplicationData());
							}
						};
						getDialogCollection().replace(dialogInfo);
						scheduledExecutorService.schedule(runnable, milliSecondsToRetry, TimeUnit.MILLISECONDS);
					}
				}
				public String getResourceId() {
					return dialogId;
				}
			};
			getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
		} else
			super.processReinviteErrorResponse(responseEvent, dialogId);
	}

	public void addOutboundCallLegListener(OutboundCallLegListener listener) {
		super.addOutboundDialogListener(new OutboundCallLegListenerAdapter(listener));
	}

	public void removeOutboundCallLegListener(OutboundCallLegListener listener) {
		super.removeOutboundDialogListener(new OutboundCallLegListenerAdapter(listener));
	}

	public void setOutboundCallLegListeners(List<OutboundCallLegListener> listeners) {
		List<DialogSipListener> dialogSipListeners = new ArrayList<DialogSipListener>();
		for (OutboundCallLegListener listener : listeners) {
			dialogSipListeners.add(new OutboundCallLegListenerAdapter(listener));
		}
		this.setDialogSipListeners(dialogSipListeners);
	}

	public void terminateCallLeg(String dialogId) {
		this.callLegHelper.terminateCallLeg(dialogId, TerminationCause.TerminatedByServer);
	}

	public void killHousekeeperCandidate(String infoId) {
		terminateCallLeg(infoId, TerminationCause.Housekept);
	}

	public void terminateCallLeg(String dialogId, TerminationCause aDialogTerminationCause) {
		this.callLegHelper.terminateCallLeg(dialogId, aDialogTerminationCause);
	}

	public CallLegInformation getCallLegInformation(String dialogId) {
		return this.callLegHelper.getCallLegInformation(dialogId);
	}

	public void acceptReceivedMediaOffer(String dialogId, MediaDescription mediaDescription, boolean offerInOkResponse, boolean initialInviteTransactionCompleted) {
		this.callLegHelper.acceptReceivedMediaOffer(dialogId, mediaDescription, offerInOkResponse, initialInviteTransactionCompleted);
	}

	public ScheduledExecutorService getScheduledExecutorService() {
		return scheduledExecutorService;
	}

	public void setUriParser(URIParser aURIParser) {
		this.uriParser = aURIParser;
	}

	public URIParser getURIParser() {
		return uriParser;
	}
}
