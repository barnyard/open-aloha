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

import com.bt.aloha.media.convedia.msml.MsmlParseException;
import com.bt.aloha.media.convedia.msml.MsmlResponseParser;
import com.bt.aloha.media.convedia.msml.model.MsmlAnnouncementResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndCollectDigitsAnnouncementResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndCollectDigitsCollectedResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlResultResponse;

public class MsmlResponseParserTest  {
	private MsmlResponseParser parser = new MsmlResponseParser();
	
	@Test(expected=IllegalArgumentException.class)
	public void testParseNull() throws Exception {
		// act
		parser.parse(null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testParseEmpty() throws Exception {
		// act
		parser.parse("");
	}

	@Test(expected=MsmlParseException.class)
	public void testParseInvalidXml() throws Exception {
		// setup
		String xml = "<msmlXXXXXXXXX version=\"1.0\">"
				+ "<result response=\"200\">"
				+ "MSML document execution completes"
				+ "<dialogid>172.25.19.70:39010;dialog:1</dialogid>"
				+ "</result>" + "</msml>";

		// act
		parser.parse(xml);
	}

	@Test
	public void testExtractMsmlDialogIdFromEventDialogId() {
		// act
		String msmlDialogId = parser.extractMsmlDialogIdFromEventDialogId("1.2.3.4:5;dialog:abc");
		
		// assert
		assertEquals("abc", msmlDialogId);
	}
	
	@Test(expected=MsmlParseException.class)
	public void testExtractMsmlDialogIdFromEmptyEventDialogId() {
		// act
		parser.extractMsmlDialogIdFromEventDialogId("1.2.3.4:5;dialog:");
	}
	
	@Test(expected=MsmlParseException.class)
	public void testExtractMsmlDialogIdFromNoEventDialogId() {
		// act
		parser.extractMsmlDialogIdFromEventDialogId("smelly");
	}
	
	@Test(expected=MsmlParseException.class)
	public void testExtractMsmlDialogIdFromNullEventDialogId() {
		// act
		parser.extractMsmlDialogIdFromEventDialogId(null);
	}
	
	@Test
	public void testParseOKResult() throws Exception {
		// setup
		String xml = "<msml version=\"1.0\">" + "<result response=\"200\">"
				+ "MSML document execution completes"
				+ "<dialogid>172.25.19.70:39010;dialog:1</dialogid>"
				+ "</result>" + "</msml>";

		// act
		MsmlResponse resp = parser.parse(xml);
		
		// assert
		assertTrue(resp instanceof MsmlResultResponse);
		assertEquals("200", ((MsmlResultResponse)resp).getResponseCode());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testParseNoResponseAttribute() throws Exception {
		// setup
		String xml = "<msml version=\"1.0\">" + "<result>"
				+ "MSML document execution completes"
				+ "<dialogid>172.25.19.70:39010;dialog:1</dialogid>"
				+ "</result>" + "</msml>";
		
		// act
		parser.parse(xml);
	}

	@Test(expected=MsmlParseException.class)
	public void testParseOKButInvalidAgainstSchema() throws Exception {
		// setup
		String xml = "<?xml version=\"1.0\" encoding=\"US-ASCII\"?>"
				+ "<msml version=\"1.0\">"
				+ "<event name=\"app.playdone\" id=\"10.3.13.105:33794;dialog:mydialogname\">"
				+ "<name>play.amtzzzzzzzzzzz</name>" + "<value>4080ms</value>"
				+ "</event>" + "</msml>";

		// act
		parser.parse(xml);
	}

	@Test
	public void testParseOKPlayCommandCompleteEventNoStatus() throws Exception {
		// setup
		String xml = "<?xml version=\"1.0\" encoding=\"US-ASCII\"?>"
				+ "<msml version=\"1.0\">"
				+ "<event name=\"app.playCommandComplete\" id=\"10.3.13.105:33794;dialog:ANNCmydialogname\">"
				+ "<name>play.amt</name>" + "<value>4080ms</value>"
				+ "</event>" + "</msml>";

		// act
		MsmlResponse resp = parser.parse(xml);
		
		// assert
		assertTrue(resp instanceof MsmlAnnouncementResponse);
		assertEquals("4080ms", ((MsmlAnnouncementResponse)resp).getPlayAmount());
		assertEquals(null, ((MsmlAnnouncementResponse)resp).getPlayEnd());
	}
	
	@Test
	public void testParseOKDtmfPlayCommandCompleteEventNoStatus() throws Exception {
		// setup
		String xml = "<?xml version=\"1.0\" encoding=\"US-ASCII\"?>"
			+ "<msml version=\"1.0\">"
			+ "<event name=\"app.dtmfPlayCommandComplete\" id=\"10.3.13.105:33794;dialog:PPTCOLmydialogname\">"
			+ "<name>play.amt</name>" + "<value>4080ms</value>"
			+ "</event>" + "</msml>";
		
		// act
		MsmlResponse resp = parser.parse(xml);

		// assert
		assertTrue(resp instanceof MsmlPromptAndCollectDigitsAnnouncementResponse);
		assertEquals("4080ms", ((MsmlPromptAndCollectDigitsAnnouncementResponse)resp).getPlayAmount());
		assertEquals(null, ((MsmlPromptAndCollectDigitsAnnouncementResponse)resp).getPlayEnd());		
	}
	
	@Test
	public void testParseOKDtmfCollectCommandCompleteEvent() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"US-ASCII\"?>"
			+ "<msml version=\"1.0\">"
			+ "<event name=\"app.dtmfCollectCommandComplete\" id=\"10.3.13.105:33794;dialog:PPTCOLmydialogname\">"
			+ "<name>dtmf.end</name>" + "<value>dtmf.match</value>"
			+ "<name>dtmf.digits</name>" + "<value>1234</value>"
			+ "</event>" + "</msml>";
		
		// act
		MsmlResponse resp = parser.parse(xml);

		// assert
		assertTrue(resp instanceof MsmlPromptAndCollectDigitsCollectedResponse);
		assertEquals("1234", ((MsmlPromptAndCollectDigitsCollectedResponse)resp).getDtmfDigits());
		assertEquals("dtmf.match", ((MsmlPromptAndCollectDigitsCollectedResponse)resp).getDtmfEnd());
	}

	@Test
	public void testParseMsmlDialogExitEventPlayAmountPlayEnd() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"US-ASCII\"?>" +
			"<msml version=\"1.0\">" +
			"<event name=\"app.playCommandComplete\" id=\"172.25.19.70:44186;dialog:ANNCdialog12e3883e810d3254ccbb7fdd69e5bb4f\">" +
			"<name>play.amt</name>" +
			"<value>7370ms</value>" +
			"<name>play.end</name>" +
			"<value>play.complete</value>" +
			"</event>" +
			"</msml>";

		// act
		MsmlResponse resp = parser.parse(xml);
		
		// assert
		assertTrue(resp instanceof MsmlAnnouncementResponse);
		assertEquals("7370ms", ((MsmlAnnouncementResponse)resp).getPlayAmount());
		assertEquals("play.complete", ((MsmlAnnouncementResponse)resp).getPlayEnd());
	}
	
	@Test
	public void testParseUnknownEvent() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"US-ASCII\"?>" +
			"<msml version=\"1.0\">" +
			"<event name=\"msml.dialog.exit\" id=\"172.25.19.70:44186;dialog:ANNCdialog12e3883e810d3254ccbb7fdd69e5bb4f\">" +
			"</event>" +
			"</msml>";

		// act
		MsmlResponse resp = parser.parse(xml);
		
		// assert
		assertTrue(resp instanceof MsmlResponseParser.UnknownMsmlEventResponse);
	}
	
	@Test(expected=MsmlParseException.class)
	public void testParseUnknownDialogId() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"US-ASCII\"?>" +
			"<msml version=\"1.0\">" +
			"<event name=\"unknown\" id=\"172.25.19.70:44186;dialog:WHATdialog12e3883e810d3254ccbb7fdd69e5bb4f\">" +
			"<name>myname</name>" +
			"<value>myvalue</value>" +
			"</event>" +
			"</msml>";

		// act
		parser.parse(xml);
	}
}
