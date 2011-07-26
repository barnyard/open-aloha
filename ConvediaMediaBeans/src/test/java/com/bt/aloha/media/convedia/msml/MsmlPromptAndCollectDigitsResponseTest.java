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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndCollectDigitsCollectedResponse;

public class MsmlPromptAndCollectDigitsResponseTest {

	// Test generation of the Dtmf response
	@Test
	public void testDtmfCommandFinished() throws Exception {
		//setup
		//act
		String command = new MsmlPromptAndCollectDigitsCollectedResponse("123", "456", "dtmf.match").getXml();

		//assert
		assertTrue(command.indexOf("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>") > -1);
		assertTrue(command.indexOf("<msml version=\"1.0\">") > -1);
		assertTrue(command.indexOf("<event name=\"app.dtmfCollectCommandComplete\" id=\"1.2.3.4:5;dialog:123\">") > -1);
		assertTrue(command.indexOf("<name>dtmf.end</name>") > -1);
		assertTrue(command.indexOf("<value>dtmf.match</value>") > -1);
		assertTrue(command.indexOf("<name>dtmf.digits</name>") > -1);
		assertTrue(command.indexOf("<value>456</value>") > -1);
		assertTrue(command.indexOf("</event>") > -1);
		assertTrue(command.indexOf("</msml>") > -1);
	}

	// Test generation of the Dtmf response with null digits
	@Test(expected=IllegalArgumentException.class)
	public void testDtmfCommandFinishedNullDigits() throws Exception {
		//setup
		//act
		new MsmlPromptAndCollectDigitsCollectedResponse("123", null, "dtmf.match").getXml();
	}

	// Test generation of the Dtmf response with null status
	@Test(expected=IllegalArgumentException.class)
	public void testDtmfCommandFinishedNullStatus() throws Exception {
		//setup
		//act
		new MsmlPromptAndCollectDigitsCollectedResponse("123", "456", null).getXml();
	}
	
	// Test generation of the Dtmf response with null command id
	@Test(expected=IllegalArgumentException.class)
	public void testDtmfCommandFinishedNullCommandId() throws Exception {
		//setup
		//act
		new MsmlPromptAndCollectDigitsCollectedResponse(null, "345", "dtmf.match").getXml();
	}
}
