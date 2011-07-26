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

 	

 	
 	
 
package com.bt.aloha.media.testing.mockphones;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.bt.aloha.media.testing.mockphones.ConvediaMockphoneBean;

public class ConvediaMockphoneBeanTest {

	// Test retrieving of the DTMF value from audio file uri
	@Test
	public void testRetrievingDtmfValue() throws Exception {
		//setup
		String audioFileUri = "AbcDtmfValue_1234_Barged";
		
		//act
		String value = ConvediaMockphoneBean.retrieveExpectedDtmfValue(audioFileUri);
		
		//assert
		assertEquals("1234", value);
	}
	
	// Test retrieving of the DTMF value from audio file uri without Barged
	@Test
	public void testRetrievingDtmfValueNoBarged() throws Exception {
		//setup
		String audioFileUri = "AbcDtmfValue_1234";
		
		//act
		String value = ConvediaMockphoneBean.retrieveExpectedDtmfValue(audioFileUri);
		
		//assert
		assertEquals("1234", value);
	}
	
	// Test retrieving of the DTMF value from audio file uri if there is no separator
	@Test
	public void testRetrievingDtmfValueNoSeparator() throws Exception {
		//setup
		String audioFileUri = "AbcDtmfValue1234";
		
		//act
		String value = ConvediaMockphoneBean.retrieveExpectedDtmfValue(audioFileUri);
		
		//assert
		assertNull(value);
	}
}
