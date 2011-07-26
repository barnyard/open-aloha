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
package com.bt.aloha.callleg;

import javax.sdp.MediaDescription;

import com.bt.aloha.dialog.state.TerminationCause;
import com.bt.aloha.stack.SimpleSipBean;

/**
 * Spring Bean to create and control call legs.
 */
public interface CallLegBean extends SimpleSipBean {
    /**
     * Terminate a call leg
     * @param callLegId the ID of the call leg
     */
	void terminateCallLeg(String callLegId);

	/**
     * Terminate a call leg providing a termination cause
     * @param callLegId
     * @param terminationCause
	 */
    void terminateCallLeg(String callLegId, TerminationCause terminationCause);

    /**
     * Re-invite a call leg specifying Media and application data - NOT FOR EXTERNAL USE
     * @param callLegId the ID of the call leg
     * @param offerMediaDescription the new Media description
     * @param autoTerminate whether to auto-terminate when the call leg is disconnected
     * @param applicationData user defined application specific data, echoed back in RefreshCompletedEvent
     */
	void reinviteCallLeg(String callLegId, MediaDescription offerMediaDescription, AutoTerminateAction autoTerminate, String applicationData);

    /**
     * Get some information about a Call Leg
     * @param callLegId the ID of the call leg
     * @return the call Leg imformation
     */
	CallLegInformation getCallLegInformation(String callLegId);

    /**
     * Accept a media offer - NOT FOR EXTERNAL USE
     * @param callLegId the ID of the call leg
     * @param mediaDescription the media description to accept
     * @param offerInOkResponse whether the offer was in the OK response
     * @param initialInviteTransactionCompleted
     */
	void acceptReceivedMediaOffer(final String callLegId, final MediaDescription mediaDescription, final boolean offerInOkResponse, final boolean initialInviteTransactionCompleted);
}
