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

 	

 	
 	
 
package com.bt.aloha.callleg;

import java.util.Calendar;

import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.dialog.state.TerminationCause;

/**
 * Information about a Call Leg.
 */
public class CallLegInformation {
	private DialogState dialogState;
	private Calendar startTime;
	private int duration;
	private TerminationCause terminationCause;
	private boolean mediaCallLeg;

	/**
	 * Constructor
     * @param dialogInfo the DialogInfo related to the Call Leg
	 */
    public CallLegInformation(ReadOnlyDialogInfo dialogInfo) {
		this.dialogState = dialogInfo.getDialogState();
		this.startTime = Calendar.getInstance();
		this.startTime.setTimeInMillis(dialogInfo.getStartTime());
		this.duration = dialogInfo.getDuration();
		if(dialogInfo.getDialogState() != DialogState.Terminated)
			this.terminationCause = null;
		else
			this.terminationCause = dialogInfo.getTerminationCause();
		
		this.mediaCallLeg = dialogInfo.isMediaDialog();
	}

	/**
     * Get the DialogState
     * @return the DialogState
	 */
    public DialogState getState() {
		return dialogState;
	}

    /**
     * get the Call Leg duration in seconds
     * @return the duration in seconds
     */
	public int getDuration() {
		return duration;
	}

    /**
     * get the Call Leg termination cause
     * @return the Call Leg termination cause
     */
	public TerminationCause getTerminationCause() {
		return terminationCause;
	}

	/**
	 * indicate whether this call leg is a media call leg
	 * @return true or false
	 */
	public boolean isMediaCallLeg() {
		return mediaCallLeg;
	}
}
