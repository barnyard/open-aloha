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

 	

 	
 	
 
package com.bt.aloha.media.convedia;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.bt.aloha.media.convedia.MediaServerAddressFactory;

public class MediaServerAddressFactoryTest {
    public static final String ADDRESS1 = "111.111.111.111:1111";
    public static final String ADDRESS2 = "222.222.222.222:2222";
    public static final String ADDRESS3 = "333.333.333.333:3333";

    // get address from single address
    @Test
    public void testGetMediaServerAddressFromSingleList() {
        // setup
        MediaServerAddressFactory mediaServerAddressFactory = new MediaServerAddressFactory();
        mediaServerAddressFactory.setMediaServerAddresses(ADDRESS1);
        
        // act/assert
        assertEquals(ADDRESS1, mediaServerAddressFactory.getAddress());
        assertEquals(ADDRESS1, mediaServerAddressFactory.getAddress());
    }

    // get address multiple times where multiple addresses have been configured
    @Test
    public void testGetMediaServerAddressMultipleTimesFromMultipleList() {
        // setup
        MediaServerAddressFactory mediaServerAddressFactory = new MediaServerAddressFactory();
        mediaServerAddressFactory.setMediaServerAddresses(ADDRESS1 + "," + ADDRESS2 + "," + ADDRESS3);
        
        // act/assert
        assertEquals(ADDRESS1, mediaServerAddressFactory.getAddress());
        assertEquals(ADDRESS2, mediaServerAddressFactory.getAddress());
        assertEquals(ADDRESS3, mediaServerAddressFactory.getAddress());
        assertEquals(ADDRESS1, mediaServerAddressFactory.getAddress());
        assertEquals(ADDRESS2, mediaServerAddressFactory.getAddress());
        assertEquals(ADDRESS3, mediaServerAddressFactory.getAddress());
    }

    //Test that we can set addresses dynamically
	@Test
	public void testMediaServerAddressesSetter() throws Exception {
		//setup
		MediaServerAddressFactory mediaServerAddressFactory = new MediaServerAddressFactory();
		mediaServerAddressFactory.setMediaServerAddresses(ADDRESS1 + "," + ADDRESS2 + "," + ADDRESS3);

		//act
		//assert
		assertEquals(ADDRESS1, mediaServerAddressFactory.getAddress());
		assertEquals(ADDRESS2, mediaServerAddressFactory.getAddress());
		assertEquals(ADDRESS3, mediaServerAddressFactory.getAddress());
		assertEquals(ADDRESS1, mediaServerAddressFactory.getAddress());
		assertEquals(ADDRESS2, mediaServerAddressFactory.getAddress());
		assertEquals(ADDRESS3, mediaServerAddressFactory.getAddress());
	}

	//Test that we can get media server addresses
	@Test
	public void testGetMediaServerAddresses() throws Exception {
		//setup
		MediaServerAddressFactory mediaServerAddressFactory = new MediaServerAddressFactory();
		mediaServerAddressFactory.setMediaServerAddresses(ADDRESS1 + "," + ADDRESS2 + "," + ADDRESS3);

		//act
		//assert
		assertEquals(ADDRESS1 + "," + ADDRESS2 + "," + ADDRESS3, mediaServerAddressFactory.getMediaServerAddresses());
	}
}
