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

import com.bt.aloha.call.state.CallTerminationCause;
import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.callleg.OutboundCallLegListener;

/**
 * Spring Bean to join call legs into calls and teminate them.
 */
public interface CallBean extends OutboundCallLegListener {
    /**
     * Join two call legs into a call.
     * @param firstDialogId the Call Leg ID of the first call leg
     * @param secondDialogId the Call Leg ID of the second call leg
     * @return the call ID
     */
	String joinCallLegs(String firstDialogId, String secondDialogId);

    /**
     * Join two call legs into a call setting auto-terminate.
     * @param firstDialogId the Call Leg ID of the first call leg
     * @param secondDialogId the Call Leg ID of the second call leg
     * @param autoTerminateCallLegs whether the call leg is automatically terminated when disconnected from a call
     * @return the call ID
     */
	String joinCallLegs(String firstDialogId, String secondDialogId, AutoTerminateAction autoTerminateCallLegs);

    /**
     * Join two call legs into a call setting auto-terminate and the maximum duration.
     * @param firstDialogId the Call Leg ID of the first call leg
     * @param secondDialogId the Call Leg ID of the second call leg
     * @param autoTerminationCallLegs whether the call leg is automatically terminated when disconnected from a call
     * @param durationInMinutes the maximum duration of the call in minutes
     * @return the call ID
     */
	String joinCallLegs(String firstDialogId, String secondDialogId, AutoTerminateAction autoTerminationCallLegs, long durationInMinutes);

	/**
     * Terminate a call
     * @param callId the ID of the call to terminate
	 */
    void terminateCall(String callId);

    /**
     * Terminate a call specifying a cause
     * @param callId the ID of the call to terminate
     * @param callTerminationCause the reason the call is terminated
     */
    void terminateCall(String callId, CallTerminationCause callTerminationCause);

	/**
     * Get information about a call
     * @param callId the ID of the call
     * @return information about the call
	 */
    CallInformation getCallInformation(String callId);

	/**
     * Add a call listener to the call bean
     * @param callListener the CallListener to add
	 */
    void addCallListener(CallListener callListener);

    /**
     * remove a call listener from the call bean
     * @param callListener the listener to remove
     */
	void removeCallListener(CallListener callListener);
}
