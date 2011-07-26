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

package com.bt.aloha.callleg.event;

import com.bt.aloha.dialog.event.DialogTerminationFailedEvent;
import com.bt.aloha.dialog.state.TerminationCause;

/**
 * Event that is fired when a call leg termination fails
 */
public class CallLegTerminationFailedEvent extends AbstractCallLegEvent {

    /**
     * Constructor
     * 
     * @param aCallLegId
     *            the call leg ID
     * @param aTerminationCause
     *            the termination cause
     */
    public CallLegTerminationFailedEvent(String aCallLegId, TerminationCause aTerminationCause) {
        super(new DialogTerminationFailedEvent(aCallLegId, aTerminationCause));
    }

    /**
     * Constructor
     * 
     * @param event
     *            the dialog event
     */
    public CallLegTerminationFailedEvent(DialogTerminationFailedEvent event) {
        super(event);
    }

    /**
     * get the termination cause
     * 
     * @return the termination cause
     */
    public TerminationCause getTerminationCause() {
        return ((DialogTerminationFailedEvent) this.event).getTerminationCause();
    }
}
