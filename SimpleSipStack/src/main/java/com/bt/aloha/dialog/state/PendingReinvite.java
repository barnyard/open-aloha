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

 	

 	
 	
 
package com.bt.aloha.dialog.state;

import java.io.Serializable;

import javax.sdp.MediaDescription;

import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.util.MediaDescriptionState;

public class PendingReinvite implements Cloneable, Serializable {
	private static final long serialVersionUID = -4120661479083899681L;
	private Boolean autoTerminate;
	private MediaDescriptionState mediaDescription;
	private String applicationData;

	public PendingReinvite(MediaDescription aMediaDescription, Boolean aAutoTerminate, String aApplicationData) {
		this.autoTerminate = aAutoTerminate;
		this.mediaDescription = new MediaDescriptionState(aMediaDescription);
		this.applicationData = aApplicationData;
	}

	@Override
	protected PendingReinvite clone() {
		return new PendingReinvite(SessionDescriptionHelper.cloneMediaDescription(getMediaDescription()), autoTerminate, applicationData);
	}

	public String getApplicationData() {
		return applicationData;
	}

	public Boolean getAutoTerminate() {
		return autoTerminate;
	}

	public MediaDescription getMediaDescription() {
		return mediaDescription.getMediaDescription();
	}


}
