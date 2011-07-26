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

 	

 	
 	
 
package com.bt.aloha.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class MessageDigestHelper {
	/**
	 * 
	 */
	private static final int HEX_0XFF = 0xff;
	private static final byte[] HASH_WORD = new byte[] {'g', 3, 3, 'k'};
    
    private MessageDigestHelper() {}
	
	public static String generateDigest(String key) {
		return generateDigest(key, "MD5");
	}
	
	public static String generateDigest() {
		return generateDigest(Long.toString(System.currentTimeMillis())
                + Double.toString(Math.random()));
	}
	
	public static String generateDigest(String key, String algorithm) {
		try {
			byte[] defaultBytes = (new String(HASH_WORD) + key).getBytes();
			MessageDigest digest = MessageDigest.getInstance(algorithm);
			digest.reset();
			digest.update(defaultBytes);
			byte[] messageDigest = digest.digest();

			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++) {
				String hex = Integer.toHexString(HEX_0XFF & messageDigest[i]);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}
			return hexString + "";
		} catch (NoSuchAlgorithmException nsae) {
			throw new IllegalArgumentException(
					"Problem occured during hash code generation, "
							+ nsae.getMessage());
		}
	}
}
