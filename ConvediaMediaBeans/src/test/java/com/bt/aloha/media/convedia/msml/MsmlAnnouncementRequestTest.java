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

import com.bt.aloha.media.convedia.msml.model.MsmlAnnouncementRequest;


public class MsmlAnnouncementRequestTest {
	@Test
	public void testGeneratePlayCommand() throws Exception {
		// act
		String docString = new MsmlAnnouncementRequest("1.2.3.4:5678", "mydialog", "http://a.b.c",true,true).getXml();

		// assert
		assertTrue(docString.startsWith("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>"));
		assertTrue(docString.contains("<msml version=\"1.0\">"));
		assertTrue(docString.contains("<dialogstart target=\"1.2.3.4:5678\" type=\"application/moml+xml\" id=\"mydialog\">"));
		assertTrue(docString.contains("<play cvd:barge=\"true\" cvd:cleardb=\"true\" xmlns:cvd=\"http://convedia.com/moml/ext\">"));
		assertTrue(docString.contains("<audio uri=\"http://a.b.c\"/>"));
		assertTrue(docString.contains("<playexit>"));
		assertTrue(docString.contains("<send target=\"source\" event=\"app.playCommandComplete\" namelist=\"play.end play.amt\"/>"));
		assertTrue(docString.contains("</playexit>"));
		assertTrue(docString.contains("</play>"));
		assertTrue(docString.contains("</dialogstart>"));
		assertTrue(docString.endsWith("</msml>"));
	}

	@Test
	public void testGeneratePlayCommandWithIterations() throws Exception {
		// act
		String docString = new MsmlAnnouncementRequest("1.2.3.4:5678", "mydialog", "http://a.b.c",true,true, 5).getXml();

		// assert
		assertTrue(docString.startsWith("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>"));
		assertTrue(docString.contains("<msml version=\"1.0\">"));
		assertTrue(docString.contains("<dialogstart target=\"1.2.3.4:5678\" type=\"application/moml+xml\" id=\"mydialog\">"));
		assertTrue(docString.contains("<play cvd:barge=\"true\" cvd:cleardb=\"true\" iterations=\"5\" xmlns:cvd=\"http://convedia.com/moml/ext\">"));
		assertTrue(docString.contains("<audio uri=\"http://a.b.c\"/>"));
		assertTrue(docString.contains("<playexit>"));
		assertTrue(docString.contains("<send target=\"source\" event=\"app.playCommandComplete\" namelist=\"play.end play.amt\"/>"));
		assertTrue(docString.contains("</playexit>"));
		assertTrue(docString.contains("</play>"));
		assertTrue(docString.contains("</dialogstart>"));
		assertTrue(docString.endsWith("</msml>"));
	}

	@Test
	public void testGeneratePlayCommandWithIterationsInfinitive() throws Exception {
		// act
		String docString = new MsmlAnnouncementRequest("1.2.3.4:5678", "mydialog", "http://a.b.c",true,true, -1).getXml();

		// assert
		assertTrue(docString.startsWith("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>"));
		assertTrue(docString.contains("<msml version=\"1.0\">"));
		assertTrue(docString.contains("<dialogstart target=\"1.2.3.4:5678\" type=\"application/moml+xml\" id=\"mydialog\">"));
		assertTrue(docString.contains("<play cvd:barge=\"true\" cvd:cleardb=\"true\" iterations=\"-1\" xmlns:cvd=\"http://convedia.com/moml/ext\">"));
		assertTrue(docString.contains("<audio uri=\"http://a.b.c\"/>"));
		assertTrue(docString.contains("<playexit>"));
		assertTrue(docString.contains("<send target=\"source\" event=\"app.playCommandComplete\" namelist=\"play.end play.amt\"/>"));
		assertTrue(docString.contains("</playexit>"));
		assertTrue(docString.contains("</play>"));
		assertTrue(docString.contains("</dialogstart>"));
		assertTrue(docString.endsWith("</msml>"));
	}

	@Test
	public void testGeneratePlayCommandWithIterationsAndInterval() throws Exception {
		// act
		String docString = new MsmlAnnouncementRequest("1.2.3.4:5678", "mydialog", "http://a.b.c",true,true, 5, 100).getXml();

		// assert
		assertTrue(docString.startsWith("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>"));
		assertTrue(docString.contains("<msml version=\"1.0\">"));
		assertTrue(docString.contains("<dialogstart target=\"1.2.3.4:5678\" type=\"application/moml+xml\" id=\"mydialog\">"));
		assertTrue(docString.contains("<play cvd:barge=\"true\" cvd:cleardb=\"true\" iterations=\"5\" interval=\"100ms\" xmlns:cvd=\"http://convedia.com/moml/ext\">"));
		assertTrue(docString.contains("<audio uri=\"http://a.b.c\"/>"));
		assertTrue(docString.contains("<playexit>"));
		assertTrue(docString.contains("<send target=\"source\" event=\"app.playCommandComplete\" namelist=\"play.end play.amt\"/>"));
		assertTrue(docString.contains("</playexit>"));
		assertTrue(docString.contains("</play>"));
		assertTrue(docString.contains("</dialogstart>"));
		assertTrue(docString.endsWith("</msml>"));
	}

	@Test
	public void testGeneratePlayCommandWithPrefix() throws Exception {
		// act
		String docString = new MsmlAnnouncementRequest("1.2.3.4:5678", "http://a.b.c",true,true).getXml();

		// assert
		assertTrue(docString.contains("<dialogstart target=\"1.2.3.4:5678\" type=\"application/moml+xml\" id=\"ANNC"));
	}

	@Test
	public void testConstructor() throws Exception {
		// act
		MsmlAnnouncementRequest req = new MsmlAnnouncementRequest("1.2.3.4:5678", "http://a.b.c",true,true);

		// assert
		assertEquals("1.2.3.4:5678", req.getTargetAddress());
		assertEquals("http://a.b.c", req.getAudioFileUri());
		assertEquals(true, req.isAllowBarge());
		assertEquals(true, req.isClearBuffer());
		assertEquals(1, req.getIterations());
		assertEquals(0, req.getInterval());
	}

	@Test
	public void testConstructorWithIterations() throws Exception {
		// act
		MsmlAnnouncementRequest req = new MsmlAnnouncementRequest("1.2.3.4:5678", "http://a.b.c",true,true, 6);

		// assert
		assertEquals("1.2.3.4:5678", req.getTargetAddress());
		assertEquals("http://a.b.c", req.getAudioFileUri());
		assertEquals(true, req.isAllowBarge());
		assertEquals(true, req.isClearBuffer());
		assertEquals(6, req.getIterations());
		assertEquals(0, req.getInterval());
	}

	@Test
	public void testConstructorWithIterationsAndInterval() throws Exception {
		// act
		MsmlAnnouncementRequest req = new MsmlAnnouncementRequest("1.2.3.4:5678", "http://a.b.c",true,true, 6, 150);

		// assert
		assertEquals("1.2.3.4:5678", req.getTargetAddress());
		assertEquals("http://a.b.c", req.getAudioFileUri());
		assertEquals(true, req.isAllowBarge());
		assertEquals(true, req.isClearBuffer());
		assertEquals(6, req.getIterations());
		assertEquals(150, req.getInterval());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNullTargetException() throws Exception {
		new MsmlAnnouncementRequest(null, "http://a.b.c",true,true).getXml();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNullUriException() throws Exception {
		new MsmlAnnouncementRequest("1.2.3.4:5678", null, true, true).getXml();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNullCommandId() throws Exception {
		new MsmlAnnouncementRequest("1.2.3.4:5678", null, "http://a.b.c", true, true).getXml();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNotValidIterationsNegative() throws Exception {
		new MsmlAnnouncementRequest("1.2.3.4:5678", null, "http://a.b.c", true, true, -2).getXml();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNotValidIterationsZero() throws Exception {
		new MsmlAnnouncementRequest("1.2.3.4:5678", null, "http://a.b.c", true, true, 0).getXml();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNotValidIntervalNegative() throws Exception {
		new MsmlAnnouncementRequest("1.2.3.4:5678", null, "http://a.b.c", true, true, 5, -1).getXml();
	}
}
