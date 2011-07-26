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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.bt.aloha.media.convedia.msml.model.MsmlAnnouncementResponse;

public class MsmlAnnouncementResponseTest {
	
	// Test generation of the announcement response
	@Test
	public void testPlayCommandFinished() throws Exception {
		//setup
		//act
		String command = new MsmlAnnouncementResponse("123", "100ms", "play.complete").getXml();

		//assert
		assertTrue(command.indexOf("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>") > -1);
		assertTrue(command.indexOf("<msml version=\"1.0\">") > -1);
		assertTrue(command.indexOf("<event name=\"app.playCommandComplete\" id=\"1.2.3.4:5;dialog:123\">") > -1);
		assertTrue(command.indexOf("<name>play.amt</name>") > -1);
		assertTrue(command.indexOf("<value>100ms</value>") > -1);
		assertTrue(command.indexOf("<name>play.end</name>") > -1);
		assertTrue(command.indexOf("<value>play.complete</value>") > -1);
		assertTrue(command.indexOf("</event>") > -1);
		assertTrue(command.indexOf("</msml>") > -1);
	}

	/**
	 * NOTE that we allow null playend & playamt since we may receive responses taht do not contain these elements
	 */
	
	// Test behaviour of null play amount
	@Test
	public void testNullPlayAmount() throws Exception {
		//act
		MsmlAnnouncementResponse resp = new MsmlAnnouncementResponse("12", null, "play.complete");
		
		// assert
		assertEquals(null, resp.getPlayAmount());
	}
	
	// Test behaviour of null play status
	@Test
	public void testNullPlayStatus() throws Exception {
		//act
		MsmlAnnouncementResponse resp = new MsmlAnnouncementResponse("12", "100ms", null);

		// assert
		assertEquals(null, resp.getPlayEnd());
	}
}
