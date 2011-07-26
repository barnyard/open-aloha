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

import java.util.concurrent.TimeUnit;

import javax.sdp.MediaDescription;
import javax.sip.ServerTransaction;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.stack.SessionDescriptionHelper;

public class SlowAnswerDialogBean extends HangUpDialogBeanBase {
	public static final String INITIAL_DELAY_PROPERTY_KEY = "initial.delay";
	public static final long DEFAULT_INITIAL_DELAY = 10000;
	private Log log = LogFactory.getLog(getClass());

	public SlowAnswerDialogBean() {
        super();
    }

    @Override
    public void processInitialInvite(final Request request, final ServerTransaction serverTransaction, final String dialogId) {
    	getDialogBeanHelper().sendResponse(request, serverTransaction, Response.RINGING);
    	log.info("Ringing response sent");

    	final DialogInfo dialogInfo = getDialogCollection().get(dialogId);

        long initialDelay = dialogInfo.getLongProperty(INITIAL_DELAY_PROPERTY_KEY, DEFAULT_INITIAL_DELAY);

        MediaDescription holdMediaDescription;
		if(dialogInfo.isSdpInInitialInvite()) {
			String sdp = new String(request.getRawContent());
			MediaDescription offerMediaDescription = getDialogBeanHelper().getActiveMediaDescriptionFromMessageBody(sdp);
			dialogInfo.setRemoteOfferMediaDescription(offerMediaDescription);
			holdMediaDescription = SessionDescriptionHelper.generateHoldMediaDescription(offerMediaDescription);
		} else {
			holdMediaDescription = SessionDescriptionHelper.generateHoldMediaDescription();
		}
		SessionDescriptionHelper.setMediaDescription(dialogInfo.getSessionDescription(), holdMediaDescription, dialogInfo.getDynamicMediaPayloadTypeMap());

        getDialogCollection().replace(dialogInfo);

        log.info("call with ok response after " + initialDelay + " milliseconds");
        getScheduledExecutorService().schedule(new Runnable() {
            public void run() {
            	try {
            		log.info("sending OK with sdp " + dialogInfo.getSessionDescription());
            		getDialogBeanHelper().sendInviteOkResponse(request, serverTransaction, dialogInfo.getLocalTag(), dialogInfo.getSipUserName(), dialogInfo.getSessionDescription());

                	int timeUntilHangup = dialogInfo.getIntProperty(HangUpDialogBean.HANG_UP_PERIOD_PROPERTY_KEY, HangUpDialogBean.DEFAULT_SHORT_HANG_UP_PERIOD);
    		    	getTimers().put(dialogId, runHangupTimer(dialogInfo, timeUntilHangup));
				} catch (Exception e) {
					log.info("Exception occured in SlowAnswerPhone " + e.getMessage());
				}
            }
        }, initialDelay, TimeUnit.MILLISECONDS);
    }
}
