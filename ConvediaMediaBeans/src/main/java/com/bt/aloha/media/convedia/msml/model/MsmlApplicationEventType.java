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

public enum MsmlApplicationEventType {
	PLAY_COMMAND_COMPLETE ("app.playCommandComplete"),
	DTMF_PLAY_COMMAND_COMPLETE ("app.dtmfPlayCommandComplete"),
	DTMF_COLLECT_COMMAND_COMPLETE ("app.dtmfCollectCommandComplete"),
	DTMFGEN_COMMAND_COMPLETE ("app.dtmfgenDone"),
	MSML_DIALOG_EXIT ("msml.dialog.exit"),
	PPTREC_PLAY_COMMAND_COMPLETE ("app.recordPlayCommandComplete"),
	UNKNOWN ("unknown");

	private String value;
	private MsmlApplicationEventType(String aValue) {this.value = aValue;}
	public String value() { return value; }
    public static MsmlApplicationEventType fromValue(String value) {
    	for (MsmlApplicationEventType mae: MsmlApplicationEventType.values()) {
    		if (value.equals(mae.value)) {
    			return mae;
    		}
    	}
    	return MsmlApplicationEventType.UNKNOWN;
    }
}
