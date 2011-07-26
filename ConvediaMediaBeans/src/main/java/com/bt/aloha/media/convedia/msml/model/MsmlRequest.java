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

import java.util.Map;

import noNamespace.DialogLanguageDatatype;
import noNamespace.MsmlDocument;
import noNamespace.MsmlDocument.Msml;
import noNamespace.MsmlDocument.Msml.Dialogend;
import noNamespace.MsmlDocument.Msml.Dialogstart;

import org.apache.xmlbeans.XmlOptions;

public abstract class MsmlRequest extends MsmlMessage {
	private String targetAddress;

	public MsmlRequest(String aCommandId, String aTargetAddress) {
		super(aCommandId);
		this.targetAddress = aTargetAddress;
	}

	public String getTargetAddress() {
		return targetAddress;
	}

	private Msml addMsmlVersion(MsmlDocument doc) {
		Msml msml = doc.addNewMsml();
		msml.setVersion(MSML_VERSION);
		return msml;
	}

	protected Dialogstart createDialogstart(MsmlDocument doc, String aTargetAddress) {
		Msml msml = addMsmlVersion(doc);
		Dialogstart dialogStart = msml.addNewDialogstart();
		dialogStart.setTarget(aTargetAddress);
		dialogStart.setType(DialogLanguageDatatype.APPLICATION_MOML_XML);
		dialogStart.setId(getCommandId());

		return dialogStart;
	}
	
	protected void createDialogEnd(MsmlDocument doc) {
		Msml msml = addMsmlVersion(doc);
		Dialogend dialogEnd = msml.addNewDialogend();
		dialogEnd.setId(String.format("%s;dialog:%s", targetAddress, getCommandId()));
	}

	protected XmlOptions createXmlOptions(Map<String,String> map) {
		XmlOptions opts = new XmlOptions();
		opts.setSavePrettyPrint();
		opts.setCharacterEncoding(ENCODING);
		opts.setSaveSuggestedPrefixes(map);

		return opts;
	}
}
