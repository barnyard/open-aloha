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

 	

 	
 	
 
package com.bt.aloha.media.convedia.msml;

import noNamespace.MsmlDocument;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlbeans.XmlException;

public class MsmlParserBase {
	private static Log log = LogFactory.getLog(MsmlParserBase.class);
	
	protected MsmlParserBase() {}
	
	protected MsmlDocument preProcess(String xml) {
		if(null == xml || xml.length() < 1)
			throw new IllegalArgumentException("Unable to parse media command: missing input");

		MsmlDocument doc = null;
		try {
			doc = MsmlDocument.Factory.parse(xml);
		} catch (XmlException e) {
			String message = "Error parsing MSML response";
			log.error(String.format("%s: %s", message, e.getMessage()));
			throw new MsmlParseException(message, e);
		}

		if (!doc.validate()) {
			log.error("Couldn't validate xml response");
			throw new MsmlParseException("Invalid MSML document");
		}

		return doc;
	}
}