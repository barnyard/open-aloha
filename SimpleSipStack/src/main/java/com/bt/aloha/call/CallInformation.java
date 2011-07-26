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

 	

 	
 	
 
package com.bt.aloha.call;

import java.util.Calendar;

import com.bt.aloha.call.state.CallLegCausingTermination;
import com.bt.aloha.call.state.CallState;
import com.bt.aloha.call.state.CallTerminationCause;

/**
 * Information about a call.
 */
public class CallInformation {
	private CallState callState;
	private Calendar startTime;
	private long duration;
	private CallTerminationCause callTerminationCause;
	private CallLegCausingTermination callLegCausingTermination;
	private String firstCallLegId;
	private String secondCallLegId;

	/**
     * Constructor
     * @param aCallState call state
     * @param aStartTime the start time of the call
     * @param aDuration the duration of the call
     * @param aCallTerminationCause the termination cause of the call
     * @param aCallLegCausingTermination the call leg that caused call termination
     * @param aFirstCallLegId the call leg id of the first call leg
     * @param aSecondCallLegId the call leg id of the second call leg
     */
    public CallInformation(CallState aCallState, long aStartTime, long aDuration, CallTerminationCause aCallTerminationCause, CallLegCausingTermination aCallLegCausingTermination, String aFirstCallLegId, String aSecondCallLegId) {
		this.callState = aCallState;
		this.startTime = Calendar.getInstance();
		this.startTime.setTimeInMillis(aStartTime);
		this.duration = aDuration;
		this.callTerminationCause = aCallTerminationCause;
		this.callLegCausingTermination = aCallLegCausingTermination;
		this.firstCallLegId = aFirstCallLegId;
		this.secondCallLegId = aSecondCallLegId;
	}

	/**
     * get the call state
     * @return the call state
	 */
    public CallState getCallState() {
		return callState;
	}

    /**
     * get the call start time
     * @return the call start time
     */
	public Calendar getStartTime() {
		return startTime;
	}

    /**
     * get the call duration
     * @return the call duration in seconds
     */
	public long getDuration() {
		return duration;
	}

	/**
     * get the call termination cause
     * @return the call termination cause
	 */
    public CallTerminationCause getCallTerminationCause() {
		return callTerminationCause;
	}

	/**
     * get the call leg that caused the termination
     * @return the call leg that caused the termination
	 */
    public CallLegCausingTermination getCallLegCausingTermination() {
		return callLegCausingTermination;
	}

	/**
	 * get the Call Leg Id of the first call leg
	 * @return the id
	 */
    public String getFirstCallLegId() {
		return firstCallLegId;
	}

	/**
	 * get the Call Leg Id of the second call leg
	 * @return the id
	 */
	public String getSecondCallLegId() {
		return secondCallLegId;
	}
}
