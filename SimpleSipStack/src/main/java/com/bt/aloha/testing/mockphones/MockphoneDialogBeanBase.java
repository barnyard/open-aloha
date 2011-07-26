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

 	

 	
 	
 
package com.bt.aloha.testing.mockphones;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.sdp.MediaDescription;
import javax.sip.ServerTransaction;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.dialog.DialogConcurrentUpdateBlock;
import com.bt.aloha.dialog.inbound.InboundDialogSipBeanImpl;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.ImmutableDialogInfo;
import com.bt.aloha.dialog.state.ReinviteInProgress;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;
import com.bt.aloha.util.HousekeeperAware;

public abstract class MockphoneDialogBeanBase extends InboundDialogSipBeanImpl implements HousekeeperAware {
    private Log log = LogFactory.getLog(this.getClass());
    private ScheduledExecutorService scheduledExecutorService;

    protected void processInitialInviteWithDelayedResponse(final Request request, final ServerTransaction serverTransaction, final String dialogId, final int delayMilliseconds, final int responseCode) {
        getDialogBeanHelper().sendResponse(request, serverTransaction, Response.TRYING);
        try {
            Thread.sleep(delayMilliseconds);
        } catch (InterruptedException e) {
            log.info("Unable to sleep full interval", e);
        }

        log.info("sending " + responseCode + "...");
        getDialogBeanHelper().sendResponse(request, serverTransaction, responseCode);
    }
    
	protected void sendInitialOKResponse(final String dialogId, final Request request, final ServerTransaction serverTransaction){
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				MediaDescription offerMediaDescription = null;
		        MediaDescription holdMediaDescription = null;
				if (dialogInfo.isSdpInInitialInvite()) {
					String sdp = new String(request.getRawContent());
                    if (getDialogBeanHelper().hasActiveVideoMediaDescription(sdp)){
                        getDialogBeanHelper().sendResponse(request, serverTransaction, Response.NOT_ACCEPTABLE_HERE);
                        return;
                    }
					offerMediaDescription = getDialogBeanHelper().getActiveMediaDescriptionFromMessageBody(sdp);
					dialogInfo.setRemoteOfferMediaDescription(offerMediaDescription);
					holdMediaDescription = SessionDescriptionHelper.generateHoldMediaDescription(offerMediaDescription);
				} else {
					holdMediaDescription = SessionDescriptionHelper.generateHoldMediaDescription();
				}
				SessionDescriptionHelper.setMediaDescription(dialogInfo.getSessionDescription(), holdMediaDescription, dialogInfo.getDynamicMediaPayloadTypeMap());

                dialogInfo.setDialogState(DialogState.Early);
				getDialogCollection().replace(dialogInfo);

	            getDialogBeanHelper().sendResponse(request, serverTransaction, Response.RINGING);
	            getDialogBeanHelper().sendInviteOkResponse(request, serverTransaction, dialogInfo.getLocalTag(), dialogInfo.getSipUserName(), dialogInfo.getSessionDescription());
			}

			public String getResourceId() {
				return dialogId;
			}
    	};
    	getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

    protected ScheduledFuture<?> runHangupTimer(final ImmutableDialogInfo dialogInfo, int timeUntilHangup) {
        final String dialogId = dialogInfo.getId();
        log.info("call with hangup after " + timeUntilHangup + " milliseconds");
        return scheduledExecutorService.schedule(new Runnable() {
            public void run() {
                log.info("disconnecting call");
                try {
                    ConcurrentUpdateBlock concurrentUpdateBlock = new DialogConcurrentUpdateBlock(getDialogBeanHelper()) {
                        public void execute() {
                            DialogInfo dialogInfo = getDialogCollection().get(dialogId);
                            assignSequenceNumber(dialogInfo, Request.BYE);
                            Request byeRequest = getDialogBeanHelper().createByeRequest(dialogInfo);
                            replaceDialogIfCanSendRequest(byeRequest.getMethod(), dialogInfo.getDialogState(), dialogInfo.getTerminationMethod(), dialogInfo);
                            try {
                                getSimpleSipStack().sendRequest(byeRequest);
                            } catch (Exception e) {
                                throw new RuntimeException("Exception occured whilst sending BYE from HangUp Phone: " + e.getMessage(), e);
                            }
                        }

                        public String getResourceId() {
                            return dialogInfo.getId();
                        }
                    };

                    new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);
                } catch (Exception e) {
                    log.error("Exception occured while hanging up MOCKPhone: " + e.getMessage(), e);
                }
            }
        }, timeUntilHangup, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void processReinvite(final Request request, final ServerTransaction serverTransaction, final String dialogId) {
        log.debug(String.format("Processing REINVITE request for mockphone dialog %s (%s)", dialogId, this.getClass().getSimpleName()));
        ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
            public void execute() {
                processReinviteExecute(request, serverTransaction, dialogId);
            }

            public String getResourceId() {
                return dialogId;
            }
        };
        getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);

    }

    private void processReinviteExecute(final Request request, final ServerTransaction serverTransaction, final String dialogId){
        DialogInfo dialogInfo = getDialogCollection().get(dialogId);
        DialogState dialogState = dialogInfo.getDialogState();
        if (dialogState.ordinal() < DialogState.Early.ordinal()) {
            log.warn(String.format("Throwing away reinvite for dialog %s, state is %s", dialogId, dialogInfo.getDialogState()));
            return;
        }
        dialogInfo.setReinviteInProgess(ReinviteInProgress.ReceivedReinvite);
        dialogInfo.setInviteServerTransaction(serverTransaction);
        updateDialogInfoFromInviteRequest(dialogInfo, request);

        getDialogCollection().replace(dialogInfo);

        sendReinviteOkResponse(dialogId, getReinviteOkResponseMediaDescription(dialogInfo.getRemoteOfferMediaDescription()));
    }

    protected MediaDescription getReinviteOkResponseMediaDescription(MediaDescription offerMediaDescription) {
        return SessionDescriptionHelper.generateHoldMediaDescription(offerMediaDescription);
    }

	public void setScheduledExecutorService(ScheduledExecutorService aScheduledExecutorService) {
		this.scheduledExecutorService = aScheduledExecutorService;
	}

	public ScheduledExecutorService getScheduledExecutorService() {
		return scheduledExecutorService;
	}

	public void killHousekeeperCandidate(String infoId) {
		// do nothing
	}
}
