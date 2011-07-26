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

import com.bt.aloha.dialog.event.IncomingAction;
import com.bt.aloha.dialog.event.IncomingDialogEvent;
import com.bt.aloha.dialog.event.IncomingResponseCode;

/**
 * Event that is fired when an incoming call leg 'arrives'
 */
public class IncomingCallLegEvent extends AbstractCallLegEvent {
	/**
     * Constructor
     * @param event the dialog event
	 */
    public IncomingCallLegEvent(IncomingDialogEvent event) {
		super(event);
	}

	/**
     * Set the action to be taken with this incoming call leg
     * @param newIncomingCallLegAction the action
	 */
    public void setIncomingCallAction(IncomingAction newIncomingCallLegAction) {
		((IncomingDialogEvent)this.event).setIncomingAction(newIncomingCallLegAction);	}

	/**
     * get the originating URI
     * @return the originating URI
	 */
    //TODO: make this return a URI?
    public String getFromUri() {
		return ((IncomingDialogEvent)this.event).getFromUri();
	}

    /**
     * get the destination URI
     * @return the destination URI
     */
    //TODO: make this return a URI?
	public String getToUri() {
		return ((IncomingDialogEvent)this.event).getToUri();
	}

	/**
     * set the response code
     * @param responseCode the response code
	 */
    public void setResponseCode(IncomingResponseCode responseCode) {
		((IncomingDialogEvent)this.event).setResponseCode(responseCode);
	}
}
