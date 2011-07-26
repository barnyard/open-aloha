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

import com.bt.aloha.media.convedia.msml.model.MsmlDtmfGenerationRequest;


public class MsmlDtmfGenerationRequestTest {
	@Test
	public void testGeneratePlayCommandWithPrefix() throws Exception {
		// act
		String docString = new MsmlDtmfGenerationRequest("1.2.3.4:5678", "mydialog", "234").getXml();

		// assert
		assertTrue(docString.startsWith("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>"));
		assertTrue(docString.contains("<msml version=\"1.0\">"));
		assertTrue(docString.contains("<dialogstart target=\"1.2.3.4:5678\" type=\"application/moml+xml\" id=\"mydialog\">"));
		assertTrue(docString.contains("<dtmfgen digits=\"234\" dur=\"100ms\">"));
	    assertTrue(docString.contains("<dtmfgenexit>"));
	    assertTrue(docString.contains("<send target=\"source\" event=\"app.dtmfgenDone\" namelist=\"dtmfgen.end\"/>"));
	    assertTrue(docString.contains("</dtmfgenexit>"));
	    assertTrue(docString.contains("</dtmfgen>"));
		assertTrue(docString.contains("</dialogstart>"));
		assertTrue(docString.endsWith("</msml>"));
	}
	
	@Test
	public void testGeneratePlayCommandWithPrefixAndDigitLength() throws Exception {
		// act
		String docString = new MsmlDtmfGenerationRequest("1.2.3.4:5678", "mydialog", "123", 400).getXml();
		
		// assert
		assertTrue(docString.contains("<dialogstart target=\"1.2.3.4:5678\" type=\"application/moml+xml\" id=\"mydialog\">"));
		assertTrue(docString.contains("<dtmfgen digits=\"123\" dur=\"400ms\">"));
	}
	
	@Test
	public void testGeneratePlayCommand() throws Exception {
		// act
		String docString = new MsmlDtmfGenerationRequest("1.2.3.4:5678", "123").getXml();

		// assert
		assertTrue(docString.contains("<dialogstart target=\"1.2.3.4:5678\" type=\"application/moml+xml\" id=\"DTMFGEN"));
	}
	
	@Test
	public void testGeneratePlayCommandWithLength() throws Exception {
		// act
		String docString = new MsmlDtmfGenerationRequest("1.2.3.4:5678", "123", 600).getXml();

		// assert
		assertTrue(docString.contains("<dialogstart target=\"1.2.3.4:5678\" type=\"application/moml+xml\" id=\"DTMFGEN"));
		assertTrue(docString.contains("<dtmfgen digits=\"123\" dur=\"600ms\">"));
	}
	
	@Test
	public void testConstructor() throws Exception {
		// act
		MsmlDtmfGenerationRequest req = new MsmlDtmfGenerationRequest("1.2.3.4:5678", "123");

		// assert
		assertEquals("1.2.3.4:5678", req.getTargetAddress());
		assertEquals("123", req.getDigits());
	}
}
