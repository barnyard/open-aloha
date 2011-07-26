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

import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.media.convedia.msml.model.MsmlCancelMediaRequest;


public class MsmlCancelMediaRequestTest {
	String commandId;
	String targetAddress;
	
	@Before
	public void before() {
		commandId = "testCommand";
		targetAddress = "targetAddress";
	}
	
	@Test
	public void testGenerateCancelMediaCommand() throws Exception {
		// act
		String docString = new MsmlCancelMediaRequest(commandId, targetAddress).getXml();

		// assert
		assertTrue(docString.startsWith("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>"));
		assertTrue(docString.contains("<msml version=\"1.0\">"));
		assertTrue(docString.contains("<dialogend id=\"" + targetAddress + ";dialog:" + commandId + "\"/>"));
		assertTrue(docString.endsWith("</msml>"));
	}
	
	@Test
	public void testConstructor() throws Exception {
		// act
		MsmlCancelMediaRequest req = new MsmlCancelMediaRequest(commandId, targetAddress);

		// assert
		assertEquals(commandId, req.getCommandId());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNullCommandId() throws Exception {
		new MsmlCancelMediaRequest(null, targetAddress).getXml();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNullTargetAddress() throws Exception {
		new MsmlCancelMediaRequest(commandId, null).getXml();
	}
}
