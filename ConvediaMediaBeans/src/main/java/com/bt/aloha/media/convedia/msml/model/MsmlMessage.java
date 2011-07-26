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

public abstract class MsmlMessage {
	protected static final String MSML_VERSION = "1.0";
    protected static final String CVD_NS = "http://convedia.com/moml/ext";
    protected static final String CVD_PREFIX = "cvd";
    protected static final String XML_PREFIX = "<?xml version=\"1.0\" encoding=\"US-ASCII\"?>\n";
    protected static final String ENCODING = "US-ASCII";
    protected static final String SOURCE = "source";
    protected static final String SECONDS = "s";
	private String commandId;
	
	public MsmlMessage(String aCommandId) {
		if(aCommandId == null)
			throw new IllegalArgumentException("Command id must be provided");
		this.commandId = aCommandId;
	}
	
	public String getCommandId() {
		return this.commandId;
	}
	
	public abstract String getXml();
}
