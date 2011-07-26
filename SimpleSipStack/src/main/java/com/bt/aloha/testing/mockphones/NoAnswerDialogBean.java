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

import javax.sip.ServerTransaction;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.dialog.state.DialogInfo;


public class NoAnswerDialogBean extends MockphoneDialogBeanBase {
	public static final String MAX_CALL_DURATION_PROPERTY_KEY = "max.call.duration";
	public static final int MAX_CALL_DURATION = 60000;
	private static final long SLEEP_INTERVAL = 100;
	private Log log = LogFactory.getLog(getClass());

    public NoAnswerDialogBean() {
        super();
    }

    @Override
    public void processInitialInvite(final Request request, final ServerTransaction serverTransaction, final String dialogId) {
    	DialogInfo dialogInfo = getDialogCollection().get(dialogId);
    	try {
			Thread.sleep(SLEEP_INTERVAL);
		} catch (InterruptedException e) {
			log.info("Unable to sleep full interval", e);
		}

    	log.info("sending RINGING...");
    	getDialogBeanHelper().sendResponse(request, serverTransaction, Response.RINGING);

        int maxCallDuration = dialogInfo.getIntProperty(MAX_CALL_DURATION_PROPERTY_KEY, MAX_CALL_DURATION);

        log.debug(String.format("Sending Temporarily Unavailable in %s milliseconds", maxCallDuration));
        getScheduledExecutorService().schedule(new Runnable() {
            public void run() {
            	try {
            		log.info("sending TEMPORARILY_UNAVAILABLE...");
            		getDialogBeanHelper().sendResponse(request, serverTransaction, Response.TEMPORARILY_UNAVAILABLE);
				} catch (Exception e) {
					log.info("Exception occured in NoAnswerPhone " + e.getMessage());
				}
            }
        }, maxCallDuration, TimeUnit.MILLISECONDS);
    }
}
