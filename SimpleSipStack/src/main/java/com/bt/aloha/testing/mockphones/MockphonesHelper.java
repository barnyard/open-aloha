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

 	

 	
 	
 
package com.bt.aloha.testing.mockphones;

import javax.sip.address.URI;
import javax.sip.message.Request;

public class MockphonesHelper {
	public static final String SEPARATOR = ".";
	
	protected MockphonesHelper() {
		super();
	}
	public static String getRecipientFromRequest(Request request) {
		URI requestURI = request.getRequestURI();
		String uri = requestURI.toString();
		return getRecipient(uri);
	}

	public static String getRecipient(String uri) {
		String recipient = "";
		int start = uri.indexOf(":");
		int end = uri.indexOf("@");
		if (start != -1 && end != -1) {
			recipient = uri.substring(start + 1, end);
		}
		return recipient;
	}

	public static String getDwarfName(String recipient) {
			int idx = recipient.indexOf(SEPARATOR);
			if (idx == -1) return recipient;
			return recipient.substring(0, idx);
	}
	
	public static String[] getParams(String recipient) {
		int first = recipient.indexOf(SEPARATOR);
		if (first == -1) return new String[0];
		return parseParams(recipient.substring(first + 1, recipient.length()), SEPARATOR);
	}

	private static String[] parseParams(String paramsString, String separator) {
		if (paramsString == null || paramsString.equals(""))
			return new String [0];
		return paramsString.split("\\" + separator);
	}
}
