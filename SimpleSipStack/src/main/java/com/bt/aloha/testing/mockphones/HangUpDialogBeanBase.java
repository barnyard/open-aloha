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

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.message.Request;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class HangUpDialogBeanBase extends MockphoneDialogBeanBase {
	private static Log log = LogFactory.getLog(HangUpDialogBeanBase.class);
	private Map<String, ScheduledFuture<?>> timers = new Hashtable<String, ScheduledFuture<?>>();;

	@Override
	protected void processBye(Request request, ServerTransaction serverTransaction, String dialogId) {
		super.processBye(request, serverTransaction, dialogId);
		log.debug(String.format("Descheduling hang up process waiting for dialogId %s", dialogId));
		ScheduledFuture<?> future = timers.remove(dialogId);
		if (null != future){
			log.debug(String.format("Descheduling for hangup the future [%s] for dialogId %s", future.toString(), dialogId));
			future.cancel(true);
		}
	}

	@Override
	protected void processByeResponse(ResponseEvent re, String dialogId) {
		super.processByeResponse(re, dialogId);
		timers.remove(dialogId);
	}

	protected Map<String, ScheduledFuture<?>> getTimers() {
		return timers;
	}
}
