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

 	

 	
 	
 
package com.bt.aloha.media;

import com.bt.aloha.dialog.state.DialogInfo;

public class MediaDialogInfo extends DialogInfo {
    private static final long serialVersionUID = 1L;

    public MediaDialogInfo(String aDialogId, String aSimpleSipBeanId, String aHostIpAddress) {
		super(aDialogId, aSimpleSipBeanId, aHostIpAddress);
		this.setAutoTerminate(true);
		this.setAutomaticallyPlaceOnHold(false);
	}

	public MediaDialogInfo(String aDialogId, String aSimpleSipBeanId, String aHostIpAddress, String fromSipUri, String toSipUri, String aLocalTag, long aCallAnswerTimeout) {
		super(aDialogId, aSimpleSipBeanId, aHostIpAddress, fromSipUri, toSipUri, aLocalTag, aCallAnswerTimeout, true, false);
		this.setAutoTerminate(true);
		this.setAutomaticallyPlaceOnHold(false);
		this.setAutomaton(true);
	}

	@Override
	public void setAutoTerminate(boolean shouldAutoTerminate) {
		super.setAutoTerminate(true);
	}

	@Override
	public void setAutomaticallyPlaceOnHold(boolean shouldPlaceOnHold) {
		super.setAutomaticallyPlaceOnHold(false);
	}

	@Override
	public boolean isMediaDialog() {
		return true;
	}
}
