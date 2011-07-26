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

import java.util.Date;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.task.TaskExecutor;

import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.call.state.CallInfo;
import com.bt.aloha.call.state.CallTerminationCause;
import com.bt.aloha.call.state.ReadOnlyCallInfo;

public class MaxCallDurationTermination {

	public static final int ONE_MINUTE_IN_MILLIS = 60000;
	private CallCollection callCollection;
	private CallBean callBean;
	private Log log = LogFactory.getLog(this.getClass());
	private MaxCallDurationScheduler maxCallDurationScheduler;
	private boolean runOnStartup;
	private TaskExecutor taskExecutor;

	public MaxCallDurationTermination(){}

	public void runTask() {
		log.debug("runTask()");
		this.taskExecutor.execute(new Runnable() {
			public void run() {
				initialize();
			}
		});
	}
	
	protected void initialize() {
		log.debug("initialize() runOnStartup = " + runOnStartup);
		if (runOnStartup) {
			ConcurrentMap<String, CallInfo> calls = callCollection.getAllConnectedCallsWithMaxDuration();
			log.debug(String.format("Initialising MaxCallDurationTermination object with %s call(s)", calls.size()));
			for (ReadOnlyCallInfo callInfo : calls.values())
				setTerminationTime(callInfo);
		}
	}

	private void setTerminationTime(ReadOnlyCallInfo callInfo) {
		long timeToTerminate = callInfo.getStartTime() + callInfo.getMaxDurationInMinutes() * ONE_MINUTE_IN_MILLIS;
		log.debug(String.format("CallId: %s, time to terminate: %s, current time: %s", callInfo.getId(), new Date(timeToTerminate).toString(), new Date(System.currentTimeMillis()).toString()));
		if (timeToTerminate <= System.currentTimeMillis()){
			log.debug(String.format("Request termination on callId %s", callInfo.getId()));
            try {
                callBean.terminateCall(callInfo.getId(), CallTerminationCause.MaximumCallDurationExceeded);
            } catch (Throwable t) {
                log.warn("Error terminating call", t);
            }
		} else {
			log.debug(String.format("Termination time is %s on callId %s", new Date(timeToTerminate).toString(), callInfo.getId()));
            try {
                maxCallDurationScheduler.terminateCallAfterMaxDuration((CallInfo)callInfo, callBean);
            } catch (Throwable t) {
                log.warn("Error scheduling call termination", t);
            }
		}
	}

	public void setCallBean(CallBean aCallBean) {
		this.callBean = aCallBean;
	}

	public void setCallCollection(CallCollection aCallCollection) {
		this.callCollection = aCallCollection;
	}

	public void setMaxCallDurationScheduler(MaxCallDurationScheduler aMaxCallDurationScheduler){
		this.maxCallDurationScheduler = aMaxCallDurationScheduler;
	}

	public void setRunOnStartup(String isRunOnStartup) {
		this.runOnStartup = Boolean.parseBoolean(isRunOnStartup);
	}

	public void setTaskExecutor(TaskExecutor aTaskExecutor) {
		this.taskExecutor = aTaskExecutor;
	}
}
