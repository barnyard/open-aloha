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

import java.util.Hashtable;
import java.util.Map;

import noNamespace.MsmlDocument;
import noNamespace.MsmlDocument.Msml;
import noNamespace.MsmlDocument.Msml.Event;

import org.apache.xmlbeans.XmlOptions;


public abstract class MsmlEventResponse extends MsmlResponse {
	private MsmlApplicationEventType msmlApplicationEventType;
	
	public MsmlEventResponse(String aCommandId, MsmlApplicationEventType aMsmlApplicationEventType) {
		super(aCommandId);
		this.msmlApplicationEventType = aMsmlApplicationEventType;
	}
	
	@Override
	public String getXml() {
		MsmlDocument doc = MsmlDocument.Factory.newInstance();
		Msml msml = doc.addNewMsml();
		msml.setVersion(MSML_VERSION);

		Event event = msml.addNewEvent();
		event.setName2(msmlApplicationEventType.value());
		event.setId("1.2.3.4:5;dialog:" + getCommandId());
		addEventNameValuePairs(event);
		
		Map<String, String> map = new Hashtable<String, String>();
		map.put(CVD_NS, CVD_PREFIX);

		XmlOptions opts = new XmlOptions();
		opts.setSavePrettyPrint();
		opts.setCharacterEncoding(ENCODING);
		opts.setSaveSuggestedPrefixes(map);
		return XML_PREFIX + doc.xmlText(opts);
	}
	
	protected abstract void addEventNameValuePairs(Event event);
}
