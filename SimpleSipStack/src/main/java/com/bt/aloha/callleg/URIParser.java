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

 	

 	
 	
 
/**
 * (c) British Telecommunications plc, 2008, All Rights Reserved
 */
package com.bt.aloha.callleg;

import java.net.URI;

public class URIParser {
	
	private static final String EQUALS = "=";
	private static final String SEMICOLON = ";";
	private String usernameParameterName = "username";
	private String passwordParameterName = "password";
	private Boolean removeUserAndPasswordParameters = true;
	
	public URIParser(){
		
	}
	
	public URIParameters parseURI(URI uri){
		String username = extractParameter(uri, usernameParameterName);
		String password = extractParameter(uri, passwordParameterName);
		URI strippedURI = uri;
		if (removeUserAndPasswordParameters)
			strippedURI = this.removeUserAndPasswordParams(uri);
		return new URIParameters(username, password, strippedURI);
	}
	
	private String extractParameter(URI uri, String parameterName){
		String uriString = uri.toString();
		String result = null;
		String parameterToken = SEMICOLON + parameterName + EQUALS;
		int parameterStart = uriString.indexOf(parameterToken);
		if (parameterStart > -1){
			int parameterEnd = uriString.indexOf(SEMICOLON, parameterStart+parameterToken.length());
			if (parameterEnd < 0)
				parameterEnd=uriString.length();
			result = uriString.substring(parameterStart + parameterToken.length(), parameterEnd);
			if (result.length() < 1)
				result=null;
		}
		return result;
	}
	


	public void setUsernameParameterName(String aUsernameParameterName) {
		this.usernameParameterName = aUsernameParameterName;
	}

	public void setPasswordParameterName(String aPasswordParameterName) {
		this.passwordParameterName = aPasswordParameterName;
	}

	public void setRemoveUserAndPasswordParameters(
			Boolean aRemoveUserAndPasswordParameters) {
		removeUserAndPasswordParameters = aRemoveUserAndPasswordParameters;
	}
	
	private URI removeUserAndPasswordParams(URI uri){
		String uriString = uri.toString();
		String[] parts = uriString.split(SEMICOLON);
		StringBuffer buf = new StringBuffer(parts[0]);
		for(int i=1;i<parts.length;i++)
		{
			if (!(parts[i].contains(usernameParameterName +EQUALS) || parts[i].contains(passwordParameterName+ EQUALS)))
				buf.append(SEMICOLON + parts[i]);
		}
				 
		return URI.create(buf.toString());
	}

}
