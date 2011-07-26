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
package com.bt.aloha.call;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.call.state.CallTerminationCause;

public class TerminateCallTask implements Runnable {
	private static final Log LOG = LogFactory.getLog(TerminateCallTask.class);
	private String callId;
	private CallBean callBean;

	/**
	 * @param theCallId
	 * @param theCallBean
	 */
	public TerminateCallTask(String theCallId, CallBean theCallBean) {
		this.callId = theCallId;
		this.callBean  = theCallBean;
	}

	public void run() {
		LOG.debug("terminating call: "+this.callId);
		try {
			this.callBean.terminateCall(this.callId, CallTerminationCause.MaximumCallDurationExceeded);
		} catch (Throwable t) {
			LOG.warn("Unable to terminate call via MaxCallDurationScheduler", t);
		}
	}

}
