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

import com.bt.aloha.media.DtmfCollectCommand;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndCollectDigitsRequest;


public class MsmlPromptAndCollectDigitsRequestTest {
	String promptFileUri = "http://prompt.com";
	boolean allowBarge = true;
	boolean clearBuffer = true;
	int firstDigitTimeoutSeconds = 1;
	int interDigitTimeoutSeconds = 2;
	int extraDigitTimeoutSeconds = 3;
	int length = 1;
	int minDigits = 1;
	int maxDigits = 2;
	char returnKey = '#';
	
	@Test
	public void testGenerateDtmfCommandPattern() throws Exception {
		DtmfCollectCommand c = new DtmfCollectCommand(
				promptFileUri,
				allowBarge,
				clearBuffer,
				firstDigitTimeoutSeconds,
				interDigitTimeoutSeconds,
				extraDigitTimeoutSeconds,
				minDigits,
				maxDigits,
				returnKey);

		// asct
		String docString = new MsmlPromptAndCollectDigitsRequest("1.2.3.4:5678", "mydialog", c).getXml();

		// assert
		assertTrue(docString.startsWith("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>"));
		assertTrue(docString.contains("<msml version=\"1.0\">"));
		assertTrue(docString.contains("<dialogstart target=\"1.2.3.4:5678\" type=\"application/moml+xml\" id=\"mydialog\">"));
		assertTrue(docString.contains("<play cvd:barge=\"true\" cvd:cleardb=\"true\" xmlns:cvd=\"http://convedia.com/moml/ext\">"));
		assertTrue(docString.contains("<audio uri=\"http://prompt.com\"/>"));
		assertTrue(docString.contains("<playexit>"));
		assertTrue(docString.contains("<send target=\"source\" event=\"app.dtmfPlayCommandComplete\" namelist=\"play.end play.amt\"/>"));
		assertTrue(docString.contains("</playexit>"));
		assertTrue(docString.contains("</play>"));
		assertTrue(docString.contains("<dtmf cleardb=\"false\" fdt=\"1s\" idt=\"2s\" edt=\"3s\">"));
		assertTrue(docString.contains("<pattern digits=\"min=1;max=2;rtk=#\" format=\"moml+digits\"/>"));
		assertTrue(docString.contains("<noinput/>"));
		assertTrue(docString.contains("<nomatch/>"));
		assertTrue(docString.contains("<dtmfexit>"));
		assertTrue(docString.contains("<send target=\"source\" event=\"app.dtmfCollectCommandComplete\" namelist=\"dtmf.digits dtmf.end\"/>"));
		assertTrue(docString.contains("</dtmfexit>"));
		assertTrue(docString.contains("</dtmf>"));
//		assertTrue(docString.contains("<exit namelist=\"dtmf.digits dtmf.end\"/>"));
		assertTrue(docString.contains("</dialogstart>"));
		assertTrue(docString.endsWith("</msml>"));
	}

	@Test
	public void testGenerateDtmfCommandLength() throws Exception {
		DtmfCollectCommand c = new DtmfCollectCommand(
				promptFileUri,
				allowBarge,
				clearBuffer,
				firstDigitTimeoutSeconds,
				interDigitTimeoutSeconds,
				extraDigitTimeoutSeconds,
				length);

		// act
		String docString = new MsmlPromptAndCollectDigitsRequest("1.2.3.4:5678", "mydialog", c).getXml();
		
		// assert
		assertTrue(docString.startsWith("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>"));
		assertTrue(docString.contains("<msml version=\"1.0\">"));
		assertTrue(docString.contains("<dialogstart target=\"1.2.3.4:5678\" type=\"application/moml+xml\" id=\"mydialog\">"));
		assertTrue(docString.contains("<play cvd:barge=\"true\" cvd:cleardb=\"true\" xmlns:cvd=\"http://convedia.com/moml/ext\">"));
		assertTrue(docString.contains("<audio uri=\"http://prompt.com\"/>"));
		assertTrue(docString.contains("<playexit>"));
		assertTrue(docString.contains("<send target=\"source\" event=\"app.dtmfPlayCommandComplete\" namelist=\"play.end play.amt\"/>"));
		assertTrue(docString.contains("</playexit>"));
		assertTrue(docString.contains("</play>"));
		assertTrue(docString.contains("<dtmf cleardb=\"false\" fdt=\"1s\" idt=\"2s\" edt=\"3s\">"));
		assertTrue(docString.contains("<pattern digits=\"length=1\" format=\"moml+digits\"/>"));
		assertTrue(docString.contains("<noinput/>"));
		assertTrue(docString.contains("<nomatch/>"));
		assertTrue(docString.contains("<dtmfexit>"));
		assertTrue(docString.contains("<send target=\"source\" event=\"app.dtmfCollectCommandComplete\" namelist=\"dtmf.digits dtmf.end\"/>"));
		assertTrue(docString.contains("</dtmfexit>"));
		assertTrue(docString.contains("</dtmf>"));
//		assertTrue(docString.contains("<exit namelist=\"dtmf.digits dtmf.end\"/>"));
		assertTrue(docString.contains("</dialogstart>"));
		assertTrue(docString.endsWith("</msml>"));
	}
	
	@Test
	public void testGeneratePlayCommandWithPrefix() throws Exception {
		// act
		String docString = new MsmlPromptAndCollectDigitsRequest("1.2.3.4:5678", new DtmfCollectCommand("1", true, true, 1, 2, 3, 1)).getXml();

		// assert
		assertTrue(docString.contains("<dialogstart target=\"1.2.3.4:5678\" type=\"application/moml+xml\" id=\"PPTCOL"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNullTargetException() throws Exception {
		new MsmlPromptAndCollectDigitsRequest(null, new DtmfCollectCommand("1", true, true, 1, 2, 3, 1)).getXml();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNullDtmfParamsException() throws Exception {
		new MsmlPromptAndCollectDigitsRequest("1.2.3.4:5678", null).getXml();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNullCommandId() throws Exception {
		new MsmlPromptAndCollectDigitsRequest("1.2.3.4:5678", null, new DtmfCollectCommand("1", true, true, 1, 2, 3, 1)).getXml();
	}
}
