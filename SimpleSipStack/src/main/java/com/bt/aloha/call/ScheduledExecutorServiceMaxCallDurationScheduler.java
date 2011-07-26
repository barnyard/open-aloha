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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.bt.aloha.call.state.CallInfo;


public class ScheduledExecutorServiceMaxCallDurationScheduler implements MaxCallDurationScheduler {
	private static final int NUMBER_OF_SECONDS_IN_ONE_MINUTE = 60;
    private static final int NUMBER_OF_MILLISECONDS_IN_ONE_MINUTE = NUMBER_OF_SECONDS_IN_ONE_MINUTE * 1000;
	private ScheduledExecutorService executorService;

	public ScheduledExecutorServiceMaxCallDurationScheduler() {
		executorService = null;
	}

	/* (non-Javadoc)
	 * @see com.bt.aloha.call.MaxCallDurationScheduler#terminateCallAfterMaxDuration(com.bt.aloha.call.CallInfo)
	 */
	public void terminateCallAfterMaxDuration(CallInfo callInfo, CallBean callBean) {
		cancelTerminateCall(callInfo);
	    long targetDelay = callInfo.getStartTime() + (callInfo.getMaxDurationInMinutes() * NUMBER_OF_MILLISECONDS_IN_ONE_MINUTE) - System.currentTimeMillis();
        ScheduledFuture<?> future = this.executorService.schedule(new TerminateCallTask(callInfo.getId(), callBean), targetDelay, TimeUnit.MILLISECONDS);
		callInfo.setFuture(future);
	}

	public void setScheduledExecutorService(ScheduledExecutorService theExecutorService) {
		this.executorService = theExecutorService;
	}

	public void cancelTerminateCall(CallInfo callInfo) {
		ScheduledFuture<?> future = callInfo.getFuture();
		if (callInfo.getFuture() != null)
			future.cancel(false);
	}
}
