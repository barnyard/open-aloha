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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NetworkHelperTest {
    // test that given an exact match, it returns the right ip address
    @Test
    public void testExactIpAddress() throws Exception {
        // setup
        String ip = "127.0.0.1";

        // act
        String ipAddress = NetworkHelper.lookupIpAddress(ip);

        // assert
        assertEquals(ip, ipAddress);
    }

    // test that given a regex, it returns the right ip address
    @Test
    public void testRegexIp() throws Exception {
        // setup
        String ip = "127.0.0.1";
        String regex = "^127.*";

        // act
        String ipAddress = NetworkHelper.lookupIpAddress(regex);

        // assert
        assertEquals(ip, ipAddress);
    }
}
