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

import com.bt.aloha.media.DtmfLengthPattern;
import com.bt.aloha.media.DtmfMinMaxRetPattern;
import com.bt.aloha.media.convedia.msml.model.MsmlAnnouncementRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlDtmfGenerationRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndCollectDigitsRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndRecordRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlRequest;


public class MsmlRequestParserTest {
	private MsmlRequestParser parser = new MsmlRequestParser();

	@Test(expected=IllegalArgumentException.class)
	public void testParseNull() throws Exception {
		parser.parse(null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testParseEmpty() throws Exception {
		parser.parse("");
	}

	@Test
	public void testParseValidDialogstartRequest() throws Exception {
		// setup
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>");
		sb.append("<msml version=\"1.0\">");
		sb.append("<dialogstart target=\"1.2.3.4:5678\" type=\"application/moml+xml\" id=\"ANNCmydialog\">");
		sb.append("<play cvd:barge=\"true\" cvd:cleardb=\"true\" xmlns:cvd=\"http://convedia.com/moml/ext\">");
		sb.append("<audio uri=\"http://a.b.c\"/>");
		sb.append("</play>");
		sb.append("<exit namelist=\"play.end play.amt\"/>");
		sb.append("</dialogstart>");
		sb.append("</msml>");

		// act
		MsmlRequest announcement = parser.parse(sb.toString());

		// assert
		assertTrue(announcement instanceof MsmlAnnouncementRequest);
		assertEquals("ANNCmydialog", announcement.getCommandId());
	}

	@Test
	public void testPromptAndCollectRequestParserLengthPattern() throws Exception {
		// setup
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>");
		sb.append("<msml version=\"1.0\">");
		sb.append("<dialogstart target=\"1.2.3.4:5678\" type=\"application/moml+xml\" id=\"PPTCOLb932c7da65d77b225ccdef2af04c9870\">");
		sb.append("<play cvd:barge=\"true\" cvd:cleardb=\"true\" xmlns:cvd=\"http://convedia.com/moml/ext\">");
		sb.append("<audio uri=\"http://a.b.c\"/>");
		sb.append("<playexit>");
		sb.append("<send target=\"source\" event=\"app.dtmfPlayCommandComplete\" namelist=\"play.end play.amt\"/>");
		sb.append("</playexit>");
		sb.append("</play>");
		sb.append("<dtmf cleardb=\"true\" fdt=\"12s\" idt=\"34s\" edt=\"56s\">");
		sb.append("<pattern digits=\"length=44\" format=\"moml+digits\"/>");
	    sb.append("<noinput/>");
	    sb.append("<nomatch/>");
	    sb.append("<dtmfexit>");
	    sb.append("<send target=\"source\" event=\"app.dtmfCollectCommandComplete\" namelist=\"dtmf.digits dtmf.end\"/>");
	    sb.append("</dtmfexit>");
	    sb.append("</dtmf>");
		sb.append("</dialogstart>");
		sb.append("</msml>");

		// act
		MsmlRequest req = parser.parse(sb.toString());

		// assert
		assertTrue(req instanceof MsmlPromptAndCollectDigitsRequest);
		assertEquals("PPTCOLb932c7da65d77b225ccdef2af04c9870", req.getCommandId());
		assertEquals(12, ((MsmlPromptAndCollectDigitsRequest)req).getDtmfCollectCommand().getFirstDigitTimeoutSeconds());
		assertEquals(34, ((MsmlPromptAndCollectDigitsRequest)req).getDtmfCollectCommand().getInterDigitTimeoutSeconds());
		assertEquals(56, ((MsmlPromptAndCollectDigitsRequest)req).getDtmfCollectCommand().getExtraDigitTimeoutSeconds());
		assertTrue(((MsmlPromptAndCollectDigitsRequest)req).getDtmfCollectCommand().getPattern() instanceof DtmfLengthPattern);

		DtmfLengthPattern p = (DtmfLengthPattern) ((MsmlPromptAndCollectDigitsRequest)req).getDtmfCollectCommand().getPattern();
		assertEquals(44, p.getLength());
	}

	@Test
	public void testPromptAndCollectRequestParserMinMaxRetPattern() throws Exception {
		// setup
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>");
		sb.append("<msml version=\"1.0\">");
		sb.append("<dialogstart target=\"1.2.3.4:5678\" type=\"application/moml+xml\" id=\"PPTCOLb932c7da65d77b225ccdef2af04c9870\">");
		sb.append("<play cvd:barge=\"true\" cvd:cleardb=\"true\" xmlns:cvd=\"http://convedia.com/moml/ext\">");
		sb.append("<audio uri=\"http://a.b.c\"/>");
		sb.append("<playexit>");
		sb.append("<send target=\"source\" event=\"app.dtmfPlayCommandComplete\" namelist=\"play.end play.amt\"/>");
		sb.append("</playexit>");
		sb.append("</play>");
		sb.append("<dtmf cleardb=\"true\" fdt=\"12s\" idt=\"34s\" edt=\"56s\">");
		sb.append("<pattern digits=\"min=1;max=2;rtk=#\" format=\"moml+digits\"/>");
	    sb.append("<noinput/>");
	    sb.append("<nomatch/>");
	    sb.append("<dtmfexit>");
	    sb.append("<send target=\"source\" event=\"app.dtmfCollectCommandComplete\" namelist=\"dtmf.digits dtmf.end\"/>");
	    sb.append("</dtmfexit>");
	    sb.append("</dtmf>");
		sb.append("</dialogstart>");
		sb.append("</msml>");

		// act
		MsmlRequest req = parser.parse(sb.toString());

		// assert
		assertTrue(req instanceof MsmlPromptAndCollectDigitsRequest);
		assertEquals("PPTCOLb932c7da65d77b225ccdef2af04c9870", req.getCommandId());
		assertEquals(12, ((MsmlPromptAndCollectDigitsRequest)req).getDtmfCollectCommand().getFirstDigitTimeoutSeconds());
		assertEquals(34, ((MsmlPromptAndCollectDigitsRequest)req).getDtmfCollectCommand().getInterDigitTimeoutSeconds());
		assertEquals(56, ((MsmlPromptAndCollectDigitsRequest)req).getDtmfCollectCommand().getExtraDigitTimeoutSeconds());
		assertTrue(((MsmlPromptAndCollectDigitsRequest)req).getDtmfCollectCommand().getPattern() instanceof DtmfMinMaxRetPattern);

		DtmfMinMaxRetPattern p = (DtmfMinMaxRetPattern) ((MsmlPromptAndCollectDigitsRequest)req).getDtmfCollectCommand().getPattern();
		assertEquals(1, p.getMinDigits());
		assertEquals(2, p.getMaxDigits());
		assertEquals('#', p.getReturnKey());
	}

	@Test
	public void testDtmfGenerationRequestParser() throws Exception {
//		 setup
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>");
		sb.append("<msml version=\"1.0\">");
		sb.append("<dialogstart target=\"1.2.3.4:5678\" type=\"application/moml+xml\" id=\"DTMFGENmydialog\">");
		sb.append("<dtmfgen digits=\"123456\">");
		sb.append("<dtmfgenexit>");
		sb.append("<send target=\"source\" event=\"app.dtmfgenDone\" namelist=\"dtmfgen.end\"/>");
	    sb.append("</dtmfgenexit>");
	    sb.append("</dtmfgen>");
		sb.append("</dialogstart>");
		sb.append("</msml>");

		// act
		MsmlRequest dtmfGenReq = parser.parse(sb.toString());

		// assert
		assertTrue(dtmfGenReq instanceof MsmlDtmfGenerationRequest);
		assertEquals("DTMFGENmydialog", dtmfGenReq.getCommandId());
		assertEquals("123456", ((MsmlDtmfGenerationRequest)dtmfGenReq).getDigits());
	}

    @Test
    public void testPromptAndRecordRequestParser() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("<msml version=\"1.0\">");
        sb.append("<dialogstart target=\"127.0.0.1:10005\" type=\"application/moml+xml\" id=\"PPTREC23b571342fc4718e5ec0462c88352879\">");
        sb.append("<play cvd:barge=\"true\" cvd:cleardb=\"false\" xmlns:cvd=\"http://convedia.com/moml/ext\">");
        sb.append("<audio uri=\"file://PromptAndRecord_myFile.wav_1234\"/>");
        sb.append("<playexit>");
        sb.append("<send target=\"source\" event=\"app.recordPlayCommandComplete\" namelist=\"play.end play.amt\"/>");
        sb.append("</playexit>");
        sb.append("</play>");
        sb.append("<record dest=\"file://dest\" format=\"audio/wav\" append=\"false\" maxtime=\"10s\" cvd:pre-speech=\"1s\" cvd:post-speech=\"1s\" xmlns:cvd=\"http://convedia.com/moml/ext\">");
        sb.append("<recordexit>");
        sb.append("<send target=\"source\" event=\"app.recordPlayCommandComplete\" namelist=\"record.recordid record.len record.end\"/>");
        sb.append("</recordexit>");
        sb.append("</record>");
        sb.append("</dialogstart>");
        sb.append("</msml>");

        // act
        MsmlRequest promptAndRecordReq = parser.parse(sb.toString());

        // assert
        assertTrue(promptAndRecordReq instanceof MsmlPromptAndRecordRequest);
        assertEquals("PPTREC23b571342fc4718e5ec0462c88352879", promptAndRecordReq.getCommandId());

        assertEquals(1, ((MsmlPromptAndRecordRequest)promptAndRecordReq).getPromptAndRecordCommand().getInitialTimeoutSeconds());
    }
}
