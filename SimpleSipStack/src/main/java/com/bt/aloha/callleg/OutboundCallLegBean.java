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

import java.net.URI;

import javax.sdp.MediaDescription;

import com.bt.aloha.dialog.state.TerminationCause;

/**
 * Spring Bean to create and manage call legs.
 */
public interface OutboundCallLegBean extends CallLegBean {
    /**
     * Create a call leg with an answer timeout
     * @param from the originating sip URI
     * @param to the destination sip URI
     * @param callAnswerTimeout the number of seconds to wait for the destination to answer
     * @return the call leg ID
     */
	String createCallLeg(URI from, URI to, int callAnswerTimeout);

    /**
     * Create a call leg
     * @param from the originating sip URI
     * @param to the destination sip URI
     * @return the call leg ID
     */
	String createCallLeg(URI from, URI to);

    /**
     * Connect a call leg
     * @param callLegId the call leg ID
     */
    void connectCallLeg(String callLegId);

    /**
     * Connect a call leg specifying whether to auto-terminate the call leg
     * @param callLegId the call leg ID
     * @param autoTerminate whether to auto-terminate
     */
	void connectCallLeg(final String callLegId, final AutoTerminateAction autoTerminate);

    /**
     * Connect a call leg specifying whether to auto-terminate the call leg, the application data to store, and the media description to include
     * @param callLegId the call leg ID
     * @param autoTerminate whether to auto-terminate
     * @param mediaDescription the media description to add to the invite
     * @param applicationData the application data to store with this dialog info
     * @param shouldPlaceOnHold determines whether the connecting leg is automatically placed on hold upon connection by sending an ACK, or whether the responsibility for sending that ACK is delegated to the application
     */
	void connectCallLeg(final String callLegId, final AutoTerminateAction autoTerminate, final String applicationData, final MediaDescription mediaDescription, final boolean shouldPlaceOnHold);

    /**
     * Cancel a call leg before it is connected
     * @param callLegId the call leg ID
     */
	void cancelCallLeg(String callLegId);

    /**
     * Cancel a call leg before it is connected specifying a Termination Cause
     * @param callLegId the call leg ID
     * @param terminationCause the termination cause
     */
	void cancelCallLeg(String callLegId, TerminationCause terminationCause);

    /**
     * Add a listener to this OutboundCallLegBean
     * @param listener the OutboundCallLegListener to add
     */
	void addOutboundCallLegListener(OutboundCallLegListener listener);

    /**
     * Remove a listener from this OutboundCallLegBean
     * @param listener the OutboundCallLegListener to remove
     */
	void removeOutboundCallLegListener(OutboundCallLegListener listener);
}
