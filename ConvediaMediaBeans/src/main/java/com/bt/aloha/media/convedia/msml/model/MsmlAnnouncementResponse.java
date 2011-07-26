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

 	

 	
 	
 
package com.bt.aloha.media.convedia.msml.model;

import noNamespace.MsmlEventNameValueDatatype;
import noNamespace.MsmlDocument.Msml.Event;


public class MsmlAnnouncementResponse extends MsmlEventResponse {
	private String playAmount;
	private String playEnd;
	
	public MsmlAnnouncementResponse(String aCommandId, String aPlayAmount, String aPlayEnd) {
		this(aCommandId, aPlayAmount, aPlayEnd, MsmlApplicationEventType.PLAY_COMMAND_COMPLETE);
	}
	
	public MsmlAnnouncementResponse(String aCommandId, String aPlayAmount, String aPlayEnd, MsmlApplicationEventType aMsmlApplicationEventType) {
		super(aCommandId, aMsmlApplicationEventType);
		
		// NOTE that we allow null playend & playamt since we may receive responses taht do not contain these elements
		this.playAmount = aPlayAmount;
		this.playEnd = aPlayEnd;
	}

	public String getPlayAmount() {
		return playAmount;
	}

	public String getPlayEnd() {
		return playEnd;
	}

	@Override
	protected void addEventNameValuePairs(Event event) {
		event.addName(MsmlEventNameValueDatatype.PLAY_AMT);
		event.addValue(playAmount);
		event.addName(MsmlEventNameValueDatatype.PLAY_END);
		event.addValue(playEnd);
	}
}
