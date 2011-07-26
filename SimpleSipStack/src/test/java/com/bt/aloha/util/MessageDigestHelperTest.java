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
 * (c) British Telecommunications plc, 2007, All Rights Reserved
 */
package com.bt.aloha.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.bt.aloha.util.MessageDigestHelper;

public class MessageDigestHelperTest {

	@Test
	public void testGenerateDigest() throws Exception {
		String hash1 = MessageDigestHelper.generateDigest();
		assertTrue(hash1.length() > 0);
        Thread.sleep(1);
        String hash2 = MessageDigestHelper.generateDigest();
        assertFalse(hash1.equals(hash2));
	}

    @Test
    public void testGenerateDigestWithKey() throws Exception {
        String hash = MessageDigestHelper.generateDigest("0");
        assertEquals("753b73031678229dea7b42f2899a4622", hash);
    }

    @Test(expected=IllegalArgumentException.class)
	public void testException() throws Exception {
		MessageDigestHelper.generateDigest("0", "XYZ");
	}
}
