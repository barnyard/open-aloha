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

import java.util.List;
import java.util.Vector;
import java.util.concurrent.ScheduledFuture;

import javax.sdp.MediaDescription;
import javax.sip.ServerTransaction;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.TerminationMethod;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.util.ConcurrentUpdateBlock;

public class HangUpDialogBean extends HangUpDialogBeanBase {
    public static final int DEFAULT_SHORT_HANG_UP_PERIOD = 3000;
    public static final String HANG_UP_PERIOD_PROPERTY_KEY = "hang.up.period";
	private static final int INTERVAL_BETWEEN_INVITE_RESPONSES = 100;
	private Log log = LogFactory.getLog(this.getClass());

	public HangUpDialogBean() {
		super();
	}

    @Override
    public void processInitialInvite(final Request request, final ServerTransaction serverTransaction, final String dialogId) {
        log.debug(String.format("Replying to initial invite for mockphone dialog %s", dialogId));

    	ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				MediaDescription offerMediaDescription = null;
				if (dialogInfo.isSdpInInitialInvite()) {
					String sdp = new String(request.getRawContent());
                    if (getDialogBeanHelper().hasActiveVideoMediaDescription(sdp)){
                        getDialogBeanHelper().sendResponse(request, serverTransaction, Response.NOT_ACCEPTABLE_HERE);
                        return;
                    }
					offerMediaDescription = getDialogBeanHelper().getActiveMediaDescriptionFromMessageBody(sdp);
					dialogInfo.setRemoteOfferMediaDescription(offerMediaDescription);
				}
				List<MediaDescription> responseMediaDescriptions = new Vector<MediaDescription>();
				responseMediaDescriptions.add(getInitialInviteOkResponseMediaDescription(offerMediaDescription));
				if (request.getRequestURI().toString().contains("video"))
					responseMediaDescriptions.add(SessionDescriptionHelper.getVideoDescription());

				SessionDescriptionHelper.setMediaDescription(dialogInfo.getSessionDescription(), responseMediaDescriptions, dialogInfo.getDynamicMediaPayloadTypeMap());

                dialogInfo.setDialogState(DialogState.Early);
				getDialogCollection().replace(dialogInfo);

	            try {
	            	Thread.sleep(INTERVAL_BETWEEN_INVITE_RESPONSES);
	            } catch(InterruptedException e) {
	            	log.error(e.getMessage(), e);
	            }
	            getDialogBeanHelper().sendResponse(request, serverTransaction, Response.RINGING);
	            try {
	            	Thread.sleep(INTERVAL_BETWEEN_INVITE_RESPONSES);
	            } catch(InterruptedException e) {
	            	log.error(e.getMessage(), e);
	            }

	            getDialogBeanHelper().sendInviteOkResponse(request, serverTransaction, dialogInfo.getLocalTag(), dialogInfo.getSipUserName(), dialogInfo.getSessionDescription());

		    	int timeUntilHangup = dialogInfo.getIntProperty(HANG_UP_PERIOD_PROPERTY_KEY, DEFAULT_SHORT_HANG_UP_PERIOD);
		    	log.info(String.format("Scheduling hangup for Hangup mockphone %s in %d secs", dialogInfo.getId(), timeUntilHangup));
		    	ScheduledFuture<?> runHangupTimer = runHangupTimer(dialogInfo, timeUntilHangup);
		    	log.debug(String.format("Scheduling for hangup using future [%s] for dialogId %s", runHangupTimer, dialogId));
				getTimers().put(dialogId, runHangupTimer);
			}

			public String getResourceId() {
				return dialogId;
			}
    	};
    	getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
    }

    @Override
    protected void processInfo(Request request, ServerTransaction serverTransaction, String dialogId) {
    	DialogInfo dialogInfo = getDialogCollection().get(dialogId);
    	if (dialogInfo.getDialogState().equals(DialogState.Terminated) || dialogInfo.getTerminationMethod().equals(TerminationMethod.Terminate))
    		getDialogBeanHelper().sendResponse(request, serverTransaction, Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
    	else
    		super.processInfo(request, serverTransaction, dialogId);
    }

	protected MediaDescription getInitialInviteOkResponseMediaDescription(MediaDescription offerMediaDescription) {
    	if (offerMediaDescription != null)
    		return SessionDescriptionHelper.generateHoldMediaDescription(offerMediaDescription);
    	else
    		return SessionDescriptionHelper.generateHoldMediaDescription();
    }
}
