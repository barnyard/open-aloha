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
package com.bt.aloha.dialog;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.stack.StackException;

/**
 * Calculate the Digest Hash for sending in proxy-authorisation header. Logic
 * largely copied from jain-sip example code
 */
public class DigestClientAuthenticationMethod {

	private static final String COLON = ":";
	private static final int HEX_FIFTEEN = 0x0F;
	private static final int FOUR = 4;
	private static final char[] TO_HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    private static final Log LOG = LogFactory.getLog(DigestClientAuthenticationMethod.class);
    
    public DigestClientAuthenticationMethod() {}

	private static String toHexString(byte[] b) {
		int pos = 0;
		char[] c = new char[b.length * 2];
		for (int i = 0; i < b.length; i++) {
			int arrayIndex1 = (b[i] >> FOUR) & HEX_FIFTEEN;
			c[pos] = TO_HEX[arrayIndex1];
			pos++;
			int arrayIndex2 = b[i] & HEX_FIFTEEN;
			c[pos] = TO_HEX[arrayIndex2];
			pos++;
		}
		return new String(c);
	}

	public String generateResponse(String realm, String userName, String uri, String nonce, String password, String method, String cnonce, String algorithm) {
		LOG.debug(String.format("generateResponse(%s, %s, %s, %s, %s, %s, %s, %s)", realm, userName, uri, nonce, password, method, cnonce, algorithm));
		
		if (algorithm == null)
			throw new StackException("The algorithm parameter is null");
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance(algorithm);
		} catch (NoSuchAlgorithmException ex) {
			throw new StackException(String.format("ERROR: %s digest algorithm does not exist.", algorithm));
		}

		StringBuffer buffer = new StringBuffer();

		// A1
		if (userName != null) 
			buffer.append(userName);
		buffer.append(COLON);
		if (realm != null) 
			buffer.append(realm);
		buffer.append(COLON);
		if (password != null) 
			buffer.append(password);
		
		String A1 = buffer.toString();
		byte[] mdbytes = messageDigest.digest(A1.getBytes());
		String HA1 = toHexString(mdbytes);
		LOG.debug(String.format("HA1: %s", HA1));
		
		// A2
		buffer = new StringBuffer();
		if (method != null)
			buffer.append(method.toUpperCase(Locale.UK));
		buffer.append(COLON);
		if (uri != null)
			buffer.append(uri);
		String A2 = buffer.toString();
		mdbytes = messageDigest.digest(A2.getBytes());
		String HA2 = toHexString(mdbytes);
		LOG.debug(String.format("HA2: %s", HA2));
		
		// KD
		buffer = new StringBuffer();
		buffer.append(HA1 + COLON);
		if (nonce != null)
			buffer.append(nonce);
		if (cnonce != null) {
			if (cnonce.length() > 0)
				buffer.append(COLON + cnonce);
		}
		buffer.append(COLON + HA2);
		String KD = buffer.toString();
		
		mdbytes = messageDigest.digest(KD.getBytes());
		String response = toHexString(mdbytes);

		LOG.debug(String.format("response generated: %s", response));

		return response;
	}
}
