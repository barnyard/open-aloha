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

 	

 	
 	
 
/**
 * (c) British Telecommunications plc, 2007, All Rights Reserved
 */
package com.bt.aloha.stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Vector;

import javax.sdp.Attribute;
import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;

import org.junit.Test;

import com.bt.aloha.stack.SessionDescriptionHelper;

public class SessionDescriptionHelperTest {
	private static final String V_TEMPLATE = "v=0";

	@Test
	public void testCreateSessionDescription() {
		// act
		SessionDescription sd = SessionDescriptionHelper.createSessionDescription("0.0.0.0", "SimpleSipStack");

		// assert
		String[] sdpLines = sd.toString().split("\r\n");
		assertEquals(4, sdpLines.length);
		assertEquals(V_TEMPLATE, sdpLines[0]);
		assertTrue(sdpLines[1].matches("o=- \\d+ \\d+ IN IP4 \\d+\\.\\d+.\\d+.\\d+"));
		assertEquals("s=SimpleSipStack", sdpLines[2]);
		assertEquals("t=0 0", sdpLines[3]);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateSessionDescriptionNullAddress() throws Exception {
		SessionDescriptionHelper.createSessionDescription(null, "SimpleSipStack");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateSessionDescriptionNullSessionName() throws Exception {
		SessionDescriptionHelper.createSessionDescription("0.0.0.0", null);
	}

	@Test
	public void testAddMediaWithConnectionToSessionDescription() throws Exception {
		// setup
		SessionDescription sd = SessionDescriptionHelper.createSessionDescription("0.0.0.0", "SimpleSipStack");
		MediaDescription md = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"8"});
		Connection conn = SdpFactory.getInstance().createConnection("4.5.6.7");
		md.setConnection(conn);

		// act
		SessionDescriptionHelper.setMediaDescription(sd, md);

		// assert
		String[] sdpLines = sd.toString().split("\r\n");
		assertEquals(7, sdpLines.length);
		assertEquals("c=IN IP4 4.5.6.7", sdpLines[3]);
		assertEquals("m=audio 1234 RTP/AVP 8", sdpLines[5]);
		assertEquals("c=IN IP4 4.5.6.7", sdpLines[6]);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testAddMediaWithoutConnectionToSessionDescriptionWithoutSessionConnection() throws Exception {
		// setup
		SessionDescription sd = SessionDescriptionHelper.createSessionDescription("0.0.0.0", "SimpleSipStack");
		MediaDescription md = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"8"});

		// act
		SessionDescriptionHelper.setMediaDescription(sd, md);
	}
	
	// test that an invalid rtpmap entry doesn't throw a RuntimeException
	@Test
	public void testSetMediaDescriptionWithInvalidRtpMap() throws Exception {
		// setup
		SessionDescription sd = SessionDescriptionHelper.createSessionDescription("0.0.0.0", "SimpleSipStack");
		sd.setConnection(SdpFactory.getInstance().createConnection("1.2.3.4"));
		MediaDescription md = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"0"});
		Vector<Attribute> attributes = new Vector<Attribute>();
        attributes.add(SdpFactory.getInstance().createAttribute("rtpmap", "0 PCMU/8000"));
        attributes.add(SdpFactory.getInstance().createAttribute("rtpmap", "xxxxx PCMU/8000"));
        attributes.add(SdpFactory.getInstance().createAttribute("fmtp", "yyyy 192-194,200-202"));
        addSpecialAttributes(attributes);
        md.setAttributes(attributes);

		// act
		SessionDescriptionHelper.setMediaDescription(sd, md, new HashMap<String, String>());

		// assert
		String[] sdpLines = sd.toString().split("\r\n");
		assertEquals(16, sdpLines.length);
		assertEquals("m=audio 1234 RTP/AVP 0", sdpLines[5]);
		assertEquals("a=rtpmap:0 PCMU/8000", sdpLines[6]);
		
		assertEquals("a=rtpmap:xxxxx PCMU/8000", sdpLines[7]);
		assertEquals("a=fmtp:yyyy 192-194,200-202", sdpLines[8]);
		
		
		assertEquals("a=X-sqn:0", sdpLines[9]);
		assertEquals("a=X-cap:1 audio RTP/AVP 100", sdpLines[10]);
		assertEquals("a=X-cpar:a=rtpmap:100 X-NSE/8000", sdpLines[11]);
		assertEquals("a=X-cpar:a=fmtp:100 192-194,200-202", sdpLines[12]);
		assertEquals("a=X-cap:2 image udptl t38", sdpLines[13]);
		assertEquals("a=silenceSupp:off - - - -", sdpLines[14]);
		assertEquals("a=ptime:20", sdpLines[15]);
	}

	@Test
	public void testAddMediaWithAttributesToSessionDescription() throws Exception {
		// setup
		SessionDescription sd = SessionDescriptionHelper.createSessionDescription("0.0.0.0", "SimpleSipStack");
		sd.setConnection(SdpFactory.getInstance().createConnection("1.2.3.4"));
		MediaDescription md = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"0"});
		Vector<Attribute> attributes = new Vector<Attribute>();
        attributes.add(SdpFactory.getInstance().createAttribute("rtpmap", "0 PCMU/8000"));
        addSpecialAttributes(attributes);
        md.setAttributes(attributes);

		// act
		SessionDescriptionHelper.setMediaDescription(sd, md);

		// assert
		String[] sdpLines = sd.toString().split("\r\n");
		assertEquals(14, sdpLines.length);
		assertEquals("m=audio 1234 RTP/AVP 0", sdpLines[5]);
		assertEquals("a=rtpmap:0 PCMU/8000", sdpLines[6]);
		assertEquals("a=X-sqn:0", sdpLines[7]);
		assertEquals("a=X-cap:1 audio RTP/AVP 100", sdpLines[8]);
		assertEquals("a=X-cpar:a=rtpmap:100 X-NSE/8000", sdpLines[9]);
		assertEquals("a=X-cpar:a=fmtp:100 192-194,200-202", sdpLines[10]);
		assertEquals("a=X-cap:2 image udptl t38", sdpLines[11]);
		assertEquals("a=silenceSupp:off - - - -", sdpLines[12]);
		assertEquals("a=ptime:20", sdpLines[13]);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testAddMediaWithConnectionAndAttributesFromSessionDescription() throws Exception {
		// setup
		SessionDescription sd = SessionDescriptionHelper.createSessionDescription("0.0.0.0", "SimpleSipStack");

		SessionDescription templateSD = SessionDescriptionHelper.createSessionDescription("0.2.3.4", "whatever");
		MediaDescription md = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"0"});
		Vector<Attribute> attributes = new Vector<Attribute>();
        attributes.add(SdpFactory.getInstance().createAttribute("rtpmap", "0 PCMU/8000"));
        addSpecialAttributes(attributes);
        md.setAttributes(attributes);
		md.setConnection(SdpFactory.getInstance().createConnection("1.2.3.4"));
		templateSD.getMediaDescriptions(true).add(md);

		// act
		MediaDescription extractedMediaDescription = SessionDescriptionHelper.getActiveMediaDescription(templateSD);
		SessionDescriptionHelper.setMediaDescription(sd, extractedMediaDescription);

		// assert
		String[] sdpLines = sd.toString().split("\r\n");
		assertEquals(15, sdpLines.length);
		assertEquals("c=IN IP4 1.2.3.4", sdpLines[3]);
		assertEquals("m=audio 1234 RTP/AVP 0", sdpLines[5]);
		assertEquals("c=IN IP4 1.2.3.4", sdpLines[6]);
		assertEquals("a=rtpmap:0 PCMU/8000", sdpLines[7]);
		assertEquals("a=X-sqn:0", sdpLines[8]);
		assertEquals("a=X-cap:1 audio RTP/AVP 100", sdpLines[9]);
		assertEquals("a=X-cpar:a=rtpmap:100 X-NSE/8000", sdpLines[10]);
		assertEquals("a=X-cpar:a=fmtp:100 192-194,200-202", sdpLines[11]);
		assertEquals("a=X-cap:2 image udptl t38", sdpLines[12]);
		assertEquals("a=silenceSupp:off - - - -", sdpLines[13]);
		assertEquals("a=ptime:20", sdpLines[14]);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testAddMediaWithAttributesFromSessionDescriptionWithSessionLevelConnection() throws Exception {
		// setup
		SessionDescription sd = SessionDescriptionHelper.createSessionDescription("0.0.0.0", "SimpleSipStack");

		SessionDescription templateSD = SessionDescriptionHelper.createSessionDescription("0.2.3.4", "whatever");
		templateSD.setConnection(SdpFactory.getInstance().createConnection("1.2.3.4"));
		MediaDescription md = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"0"});
		Vector<Attribute> attributes = new Vector<Attribute>();
        attributes.add(SdpFactory.getInstance().createAttribute("rtpmap", "0 PCMU/8000"));
        addSpecialAttributes(attributes);
        md.setAttributes(attributes);
		templateSD.getMediaDescriptions(true).add(md);

		// act
		MediaDescription extractedMediaDescription = SessionDescriptionHelper.getActiveMediaDescription(templateSD);
		SessionDescriptionHelper.setMediaDescription(sd, extractedMediaDescription);

		// assert
		String[] sdpLines = sd.toString().split("\r\n");
		assertEquals(15, sdpLines.length);
		assertEquals("c=IN IP4 1.2.3.4", sdpLines[3]);
		assertEquals("m=audio 1234 RTP/AVP 0", sdpLines[5]);
		assertEquals("c=IN IP4 1.2.3.4", sdpLines[6]);
		assertEquals("a=rtpmap:0 PCMU/8000", sdpLines[7]);
		assertEquals("a=X-sqn:0", sdpLines[8]);
		assertEquals("a=X-cap:1 audio RTP/AVP 100", sdpLines[9]);
		assertEquals("a=X-cpar:a=rtpmap:100 X-NSE/8000", sdpLines[10]);
		assertEquals("a=X-cpar:a=fmtp:100 192-194,200-202", sdpLines[11]);
		assertEquals("a=X-cap:2 image udptl t38", sdpLines[12]);
		assertEquals("a=silenceSupp:off - - - -", sdpLines[13]);
		assertEquals("a=ptime:20", sdpLines[14]);
	}

	@SuppressWarnings("unchecked")
	@Test(expected=IllegalArgumentException.class)
	public void testAddMediaFromSessionDescriptionNoConnectionExceptionThrown() throws Exception {
		// setup
		SessionDescription sd = SessionDescriptionHelper.createSessionDescription("0.0.0.0", "SimpleSipStack");

		SessionDescription templateSD = SessionDescriptionHelper.createSessionDescription("0.2.3.4", "whatever");
		MediaDescription md = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"0"});
		md.setAttribute("rtpmap", "0 PCMU/8000");
		templateSD.getMediaDescriptions(true).add(md);

		// act
		MediaDescription extractedMediaDescription = SessionDescriptionHelper.getActiveMediaDescription(templateSD);
		SessionDescriptionHelper.setMediaDescription(sd, extractedMediaDescription);
	}

	@Test
	public void testAddMediaWithAttributesToSDPWithExistingMedia() throws Exception {
		// setup
		SessionDescription sd = SessionDescriptionHelper.createSessionDescription("0.0.0.0", "SimpleSipStack");
		MediaDescription md1 = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"0"});
		md1.setConnection(SdpFactory.getInstance().createConnection("4.5.6.7"));
		Vector<Attribute> attributes = new Vector<Attribute>();
        attributes.add(SdpFactory.getInstance().createAttribute("rtpmap", "0 PCMU/8000"));
        addSpecialAttributes(attributes);
        md1.setAttributes(attributes);
		SessionDescriptionHelper.setMediaDescription(sd, md1);

		MediaDescription md2 = SdpFactory.getInstance().createMediaDescription("audio", 5678, 0, "RTP/AVP", new String[] {"8"});
		md2.setConnection(SdpFactory.getInstance().createConnection("8.9.0.1"));
		md2.setAttribute("rtpmap", "8 PCMA/8000");

		// act
		SessionDescriptionHelper.setMediaDescription(sd, md2);
		
		// assert
		String[] sdpLines = sd.toString().split("\r\n");
		assertEquals(8, sdpLines.length);
		assertEquals("c=IN IP4 8.9.0.1", sdpLines[3]);
		assertEquals("m=audio 5678 RTP/AVP 8", sdpLines[5]);
		assertEquals("c=IN IP4 8.9.0.1", sdpLines[6]);
		assertEquals("a=rtpmap:8 PCMA/8000", sdpLines[7]);
	}

	@Test
	public void testGenerateHoldMedia() {
		// act
		MediaDescription holdMediaDescription = SessionDescriptionHelper.generateHoldMediaDescription();

		// assert
		String[] sdpLines = holdMediaDescription.toString().split("\r\n");
		assertEquals(4, sdpLines.length);
		assertEquals("m=audio 9876 RTP/AVP 0", sdpLines[0]);
		assertEquals("c=IN IP4 0.0.0.0", sdpLines[1]);
		assertEquals("a=rtpmap:0 PCMU/8000", sdpLines[2]);
		assertEquals("a=inactive", sdpLines[3]);
	}

	@Test
	public void testGenerateHoldMediaMultiplePayloadTypesSendRecvStrippedOff() throws Exception {
		// setup
		MediaDescription md1 = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"0", "8", "16"});
		md1.setConnection(SdpFactory.getInstance().createConnection("4.5.6.7"));
		Vector<Attribute> attribs = new Vector<Attribute>();
		attribs.add(SdpFactory.getInstance().createAttribute("rtpmap", "0 PCMU/8000"));
		attribs.add(SdpFactory.getInstance().createAttribute("rtpmap", "8 PCMA/8000"));
		attribs.add(SdpFactory.getInstance().createAttribute("sendrecv", null));
		attribs.add(SdpFactory.getInstance().createAttribute("sendrecv", null));
		attribs.add(SdpFactory.getInstance().createAttribute("sendonly", null));
		attribs.add(SdpFactory.getInstance().createAttribute("recvonly", null));
		addSpecialAttributes(attribs);
		md1.setAttributes(attribs);

		// act
		MediaDescription holdMediaDescription = SessionDescriptionHelper.generateHoldMediaDescription(md1);

		// assert
		String[] sdpLines = holdMediaDescription.toString().split("\r\n");
		assertEquals(12, sdpLines.length);
		assertEquals("m=audio 9876 RTP/AVP 0 8 16", sdpLines[0]);
		assertEquals("c=IN IP4 0.0.0.0", sdpLines[1]);
		assertEquals("a=rtpmap:0 PCMU/8000", sdpLines[2]);
		assertEquals("a=rtpmap:8 PCMA/8000", sdpLines[3]);
		assertEquals("a=X-sqn:0", sdpLines[4]);
		assertEquals("a=X-cap:1 audio RTP/AVP 100", sdpLines[5]);
		assertEquals("a=X-cpar:a=rtpmap:100 X-NSE/8000", sdpLines[6]);
		assertEquals("a=X-cpar:a=fmtp:100 192-194,200-202", sdpLines[7]);
		assertEquals("a=X-cap:2 image udptl t38", sdpLines[8]);
		assertEquals("a=silenceSupp:off - - - -", sdpLines[9]);
		assertEquals("a=ptime:20", sdpLines[10]);
		assertEquals("a=inactive", sdpLines[11]);
	}

	@Test
	public void testAddHoldMediaWithAttributesToSDPWithExistingMedia() throws Exception {
		// setup
		SessionDescription sd = SessionDescriptionHelper.createSessionDescription("0.0.0.0", "SimpleSipStack");
		MediaDescription md1 = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"0"});
		md1.setConnection(SdpFactory.getInstance().createConnection("4.5.6.7"));
		Vector<Attribute> attributes = new Vector<Attribute>();
        attributes.add(SdpFactory.getInstance().createAttribute("rtpmap", "0 PCMU/8000"));
        addSpecialAttributes(attributes);
        md1.setAttributes(attributes);

		SessionDescriptionHelper.setMediaDescription(sd, md1);

		MediaDescription md2 = SessionDescriptionHelper.generateHoldMediaDescription(md1);

		// act
		SessionDescriptionHelper.setMediaDescription(sd, md2);

		// assert
		String[] sdpLines = sd.toString().split("\r\n");
		assertEquals(16, sdpLines.length);
		assertEquals("c=IN IP4 0.0.0.0", sdpLines[3]);
		assertEquals("m=audio 9876 RTP/AVP 0", sdpLines[5]);
		assertEquals("c=IN IP4 0.0.0.0", sdpLines[6]);
		assertEquals("a=rtpmap:0 PCMU/8000", sdpLines[7]);
		assertEquals("a=X-sqn:0", sdpLines[8]);
		assertEquals("a=X-cap:1 audio RTP/AVP 100", sdpLines[9]);
		assertEquals("a=X-cpar:a=rtpmap:100 X-NSE/8000", sdpLines[10]);
		assertEquals("a=X-cpar:a=fmtp:100 192-194,200-202", sdpLines[11]);
		assertEquals("a=X-cap:2 image udptl t38", sdpLines[12]);
		assertEquals("a=silenceSupp:off - - - -", sdpLines[13]);
		assertEquals("a=ptime:20", sdpLines[14]);
		assertEquals("a=inactive", sdpLines[15]);
	}

	@Test
	public void testAddMediaWithAttributesToSDPWithHoldMedia() throws Exception {
		// setup
		SessionDescription sd = SessionDescriptionHelper.createSessionDescription("0.0.0.0", "SimpleSipStack");
		MediaDescription md1 = SessionDescriptionHelper.generateHoldMediaDescription();
		SessionDescriptionHelper.setMediaDescription(sd, md1);

		MediaDescription md2 = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"0"});
		md2.setConnection(SdpFactory.getInstance().createConnection("4.5.6.7"));

		Vector<Attribute> attributes = new Vector<Attribute>();
        attributes.add(SdpFactory.getInstance().createAttribute("rtpmap", "0 PCMU/8000"));
        addSpecialAttributes(attributes);
        md2.setAttributes(attributes);
		
		// act
		SessionDescriptionHelper.setMediaDescription(sd, md2);

		// assert
		String[] sdpLines = sd.toString().split("\r\n");
		assertEquals(15, sdpLines.length);
		assertEquals("c=IN IP4 4.5.6.7", sdpLines[3]);
		assertEquals("m=audio 1234 RTP/AVP 0", sdpLines[5]);
		assertEquals("c=IN IP4 4.5.6.7", sdpLines[6]);
		assertEquals("a=rtpmap:0 PCMU/8000", sdpLines[7]);
		assertEquals("a=X-sqn:0", sdpLines[8]);
		assertEquals("a=X-cap:1 audio RTP/AVP 100", sdpLines[9]);
		assertEquals("a=X-cpar:a=rtpmap:100 X-NSE/8000", sdpLines[10]);
		assertEquals("a=X-cpar:a=fmtp:100 192-194,200-202", sdpLines[11]);
		assertEquals("a=X-cap:2 image udptl t38", sdpLines[12]);
		assertEquals("a=silenceSupp:off - - - -", sdpLines[13]);
		assertEquals("a=ptime:20", sdpLines[14]);
	}

	@Test
	public void testCloneNullMediaDescription() {
		assertNull(SessionDescriptionHelper.cloneMediaDescription(null));
	}

//	@Test
//	public void testRemoveDynamicPayloadTypesFromMediaDescription() throws Exception {
//		// setup
//		MediaDescription md = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"8", "95", "96", "120"});
//		Vector<Attribute> attribs = new Vector<Attribute>();
//		attribs.add(SdpFactory.getInstance().createAttribute("sendrecv", null));
//		attribs.add(SdpFactory.getInstance().createAttribute("rtpmap", "abc"));
//		attribs.add(SdpFactory.getInstance().createAttribute("rtpmap", "8 something"));
//		attribs.add(SdpFactory.getInstance().createAttribute("rtpmap", "95 something"));
//		attribs.add(SdpFactory.getInstance().createAttribute("rtpmap", "96 something"));
//		attribs.add(SdpFactory.getInstance().createAttribute("rtpmap", "120 something"));
//		md.setAttributes(attribs);
//
//		// act
//		SessionDescriptionHelper.removeDynamicPayloadTypesFromMediaDescription(md);
//
//		// assert
//		String[] mediaDescriptionLines = md.toString().split("\r\n");
//		assertEquals(5, mediaDescriptionLines.length);
//		assertEquals("m=audio 1234 RTP/AVP 8 95", mediaDescriptionLines[0]);
//		assertEquals("a=sendrecv", mediaDescriptionLines[1]);
//		assertEquals("a=rtpmap:abc", mediaDescriptionLines[2]);
//		assertEquals("a=rtpmap:8 something", mediaDescriptionLines[3]);
//		assertEquals("a=rtpmap:95 something", mediaDescriptionLines[4]);
//	}
	
	private void addSpecialAttributes(Vector<Attribute> attribs) {
		attribs.add(SdpFactory.getInstance().createAttribute("X-sqn", "0"));
		attribs.add(SdpFactory.getInstance().createAttribute("X-cap", "1 audio RTP/AVP 100"));
		attribs.add(SdpFactory.getInstance().createAttribute("X-cpar", "a=rtpmap:100 X-NSE/8000"));
		attribs.add(SdpFactory.getInstance().createAttribute("X-cpar", "a=fmtp:100 192-194,200-202"));
		attribs.add(SdpFactory.getInstance().createAttribute("X-cap", "2 image udptl t38"));
		attribs.add(SdpFactory.getInstance().createAttribute("silenceSupp", "off - - - -"));
		attribs.add(SdpFactory.getInstance().createAttribute("ptime", "20"));
	}
	
	@Test
	public void testUpdateDynamicMediaPayloadMappingsSimpleAdd() throws Exception {
		// setup
		MediaDescription md = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"8", "95", "96", "120"});
		Vector<Attribute> attribs = new Vector<Attribute>();
		attribs.add(SdpFactory.getInstance().createAttribute("rtpmap", "100 something"));
		addSpecialAttributes(attribs);
		md.setAttributes(attribs);

		HashMap<String, String> dynamicPayloadMap = new HashMap<String, String>();

		// act
		SessionDescriptionHelper.updateDynamicMediaPayloadMappings(md, dynamicPayloadMap);

		// assert
		assertEquals(1, dynamicPayloadMap.size());
		assertEquals("100", dynamicPayloadMap.get("something"));
	}

	@Test
	public void testUpdateDynamicMediaPayloadFiltersNonDynamicTypes() throws Exception {
		// setup
		MediaDescription md = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"8", "95", "96", "120"});
		Vector<Attribute> attribs = new Vector<Attribute>();
		attribs.add(SdpFactory.getInstance().createAttribute("sendrecv", null));
		attribs.add(SdpFactory.getInstance().createAttribute("rtpmap", "abc"));
		attribs.add(SdpFactory.getInstance().createAttribute("rtpmap", "8 something"));
		addSpecialAttributes(attribs);
		md.setAttributes(attribs);

		HashMap<String, String> dynamicPayloadMap = new HashMap<String, String>();

		// act
		SessionDescriptionHelper.updateDynamicMediaPayloadMappings(md, dynamicPayloadMap);

		// assert
		assertEquals(0, dynamicPayloadMap.size());
	}

	@Test
	public void testUpdateDynamicMediaPayloadUpdatesExistingValue() throws Exception {
		// setup
		MediaDescription md = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"8", "95", "96", "120"});
		Vector<Attribute> attribs = new Vector<Attribute>();
		attribs.add(SdpFactory.getInstance().createAttribute("rtpmap", "98 x"));
		addSpecialAttributes(attribs);
		md.setAttributes(attribs);

		HashMap<String, String> dynamicPayloadMap = new HashMap<String, String>();
		dynamicPayloadMap.put("x", "100");

		// act
		SessionDescriptionHelper.updateDynamicMediaPayloadMappings(md, dynamicPayloadMap);

		// assert
		assertEquals(1, dynamicPayloadMap.size());
		assertEquals("98", dynamicPayloadMap.get("x"));
	}

	@Test
	public void testUpdateDynamicMediaPayloadSkipsValueWithExtraSpace() throws Exception {
		// setup
		MediaDescription md = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"8", "98"});
		Vector<Attribute> attribs = new Vector<Attribute>();
		attribs.add(SdpFactory.getInstance().createAttribute("rtpmap", "98 "));
		addSpecialAttributes(attribs);
		md.setAttributes(attribs);

		HashMap<String, String> dynamicPayloadMap = new HashMap<String, String>();

		// act
		SessionDescriptionHelper.updateDynamicMediaPayloadMappings(md, dynamicPayloadMap);

		// assert
		assertEquals(0, dynamicPayloadMap.size());
	}

	@Test
	public void testMapDynamicPayloadTypesPreservesNonDynamicValuesForNullMap() throws Exception {
		// setup
		MediaDescription md = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"8", "95", "96", "120"});
		Vector<Attribute> attribs = new Vector<Attribute>();
		attribs.add(SdpFactory.getInstance().createAttribute("sendrecv", null));
		attribs.add(SdpFactory.getInstance().createAttribute("rtpmap", "abc"));
		attribs.add(SdpFactory.getInstance().createAttribute("rtpmap", "8 something"));
		addSpecialAttributes(attribs);
		md.setAttributes(attribs);

		// act
		SessionDescriptionHelper.mapDynamicPayloadTypes(md, null);

		// assert
		String[] mediaDescriptionLines = md.toString().split("\r\n");
		assertEquals(11, mediaDescriptionLines.length);
		assertEquals("m=audio 1234 RTP/AVP 8 95 96 120", mediaDescriptionLines[0]);
		assertEquals("a=sendrecv", mediaDescriptionLines[1]);
		assertEquals("a=rtpmap:abc", mediaDescriptionLines[2]);
		assertEquals("a=rtpmap:8 something", mediaDescriptionLines[3]);
		assertEquals("a=X-sqn:0", mediaDescriptionLines[4]);
		assertEquals("a=X-cap:1 audio RTP/AVP 100", mediaDescriptionLines[5]);
		assertEquals("a=X-cpar:a=rtpmap:100 X-NSE/8000", mediaDescriptionLines[6]);
		assertEquals("a=X-cpar:a=fmtp:100 192-194,200-202", mediaDescriptionLines[7]);
		assertEquals("a=X-cap:2 image udptl t38", mediaDescriptionLines[8]);
		assertEquals("a=silenceSupp:off - - - -", mediaDescriptionLines[9]);
		assertEquals("a=ptime:20", mediaDescriptionLines[10]);
	}

	@Test
	public void testMapDynamicPayloadTypesPreservesNonDynamicValuesForEmptyMap() throws Exception {
		// setup
		MediaDescription md = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"8"});
		Vector<Attribute> attribs = new Vector<Attribute>();
		attribs.add(SdpFactory.getInstance().createAttribute("sendrecv", null));
		attribs.add(SdpFactory.getInstance().createAttribute("rtpmap", "abc"));
		attribs.add(SdpFactory.getInstance().createAttribute("rtpmap", "8 something"));
		addSpecialAttributes(attribs);
		md.setAttributes(attribs);

		// act
		SessionDescriptionHelper.mapDynamicPayloadTypes(md, new HashMap<String, String>());

        // assert
		String[] mediaDescriptionLines = md.toString().split("\r\n");
		assertEquals(11, mediaDescriptionLines.length);
		assertEquals("m=audio 1234 RTP/AVP 8", mediaDescriptionLines[0]);
		assertEquals("a=sendrecv", mediaDescriptionLines[1]);
		assertEquals("a=rtpmap:abc", mediaDescriptionLines[2]);
		assertEquals("a=rtpmap:8 something", mediaDescriptionLines[3]);
		assertEquals("a=X-sqn:0", mediaDescriptionLines[4]);
		assertEquals("a=X-cap:1 audio RTP/AVP 100", mediaDescriptionLines[5]);
		assertEquals("a=X-cpar:a=rtpmap:100 X-NSE/8000", mediaDescriptionLines[6]);
		assertEquals("a=X-cpar:a=fmtp:100 192-194,200-202", mediaDescriptionLines[7]);
		assertEquals("a=X-cap:2 image udptl t38", mediaDescriptionLines[8]);
		assertEquals("a=silenceSupp:off - - - -", mediaDescriptionLines[9]);
		assertEquals("a=ptime:20", mediaDescriptionLines[10]);
	}

	@Test
	public void testMapDynamicPayloadTypesMapsDynamicRtpmapType() throws Exception {
		// setup
		MediaDescription md = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"8", "100"});
		Vector<Attribute> attribs = new Vector<Attribute>();
		attribs.add(SdpFactory.getInstance().createAttribute("rtpmap", "8 something"));
		attribs.add(SdpFactory.getInstance().createAttribute("rtpmap", "100 mapped"));
		addSpecialAttributes(attribs);
		md.setAttributes(attribs);

		HashMap<String, String> dynamicPayloadMap = new HashMap<String, String>();
		dynamicPayloadMap.put("mapped", "120");

		// act
		SessionDescriptionHelper.mapDynamicPayloadTypes(md, dynamicPayloadMap);

		// assert
		String[] mediaDescriptionLines = md.toString().split("\r\n");
		assertEquals(10, mediaDescriptionLines.length);
		assertEquals("m=audio 1234 RTP/AVP 8 120", mediaDescriptionLines[0]);
		assertEquals("a=rtpmap:8 something", mediaDescriptionLines[1]);
		assertEquals("a=rtpmap:120 mapped", mediaDescriptionLines[2]);
		assertEquals("a=X-sqn:0", mediaDescriptionLines[3]);
		assertEquals("a=X-cap:1 audio RTP/AVP 100", mediaDescriptionLines[4]);
		assertEquals("a=X-cpar:a=rtpmap:100 X-NSE/8000", mediaDescriptionLines[5]);
		assertEquals("a=X-cpar:a=fmtp:100 192-194,200-202", mediaDescriptionLines[6]);
		assertEquals("a=X-cap:2 image udptl t38", mediaDescriptionLines[7]);
		assertEquals("a=silenceSupp:off - - - -", mediaDescriptionLines[8]);
		assertEquals("a=ptime:20", mediaDescriptionLines[9]);
	}

	@Test
	public void testMapDynamicPayloadTypesMapsDynamicRtpmapAndFmtpType() throws Exception {
		// setup
		MediaDescription md = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"8", "100"});
		Vector<Attribute> attribs = new Vector<Attribute>();
		attribs.add(SdpFactory.getInstance().createAttribute("rtpmap", "8 something"));
		attribs.add(SdpFactory.getInstance().createAttribute("rtpmap", "100 mapped"));
		attribs.add(SdpFactory.getInstance().createAttribute("fmtp", "100 some param"));
		addSpecialAttributes(attribs);
		md.setAttributes(attribs);

		HashMap<String, String> dynamicPayloadMap = new HashMap<String, String>();
		dynamicPayloadMap.put("mapped", "120");

		// act
		SessionDescriptionHelper.mapDynamicPayloadTypes(md, dynamicPayloadMap);

		// assert
		String[] mediaDescriptionLines = md.toString().split("\r\n");
		assertEquals(11, mediaDescriptionLines.length);
		assertEquals("m=audio 1234 RTP/AVP 8 120", mediaDescriptionLines[0]);
		assertEquals("a=rtpmap:8 something", mediaDescriptionLines[1]);
		assertEquals("a=rtpmap:120 mapped", mediaDescriptionLines[2]);
		assertEquals("a=fmtp:120 some param", mediaDescriptionLines[3]);
		assertEquals("a=X-sqn:0", mediaDescriptionLines[4]);
		assertEquals("a=X-cap:1 audio RTP/AVP 100", mediaDescriptionLines[5]);
		assertEquals("a=X-cpar:a=rtpmap:100 X-NSE/8000", mediaDescriptionLines[6]);
		assertEquals("a=X-cpar:a=fmtp:100 192-194,200-202", mediaDescriptionLines[7]);
		assertEquals("a=X-cap:2 image udptl t38", mediaDescriptionLines[8]);
		assertEquals("a=silenceSupp:off - - - -", mediaDescriptionLines[9]);
		assertEquals("a=ptime:20", mediaDescriptionLines[10]);
	}

	@Test
	public void testSetSessionDescriptionWithMap() throws Exception {
		// setup
		StringBuilder sb = new StringBuilder();
		sb.append("v=0\r\n");
		sb.append("o=- 38218 38218 IN IP4 cmsmpccontrol\r\n");
		sb.append("s=media server session\r\n");
		sb.append("t=0 0\r\n");
		sb.append("m=audio 34848 RTP/AVP 0 8 97 98 99 100 18 4 96\r\n");
		sb.append("c=IN IP4 172.25.19.70\r\n");
		sb.append("a=rtpmap:0 PCMU/8000\r\n");
		sb.append("a=rtpmap:8 PCMA/8000\r\n");
		sb.append("a=rtpmap:97 G726-40/8000\r\n");
		sb.append("a=rtpmap:98 G726-32/8000\r\n");
		sb.append("a=rtpmap:99 G726-24/8000\r\n");
		sb.append("a=rtpmap:100 G726-16/8000\r\n");
		sb.append("a=rtpmap:18 G729/8000\r\n");
		sb.append("a=fmtp:18 annexb=no\r\n");
		sb.append("a=rtpmap:4 G723/8000\r\n");
		sb.append("a=fmtp:4 annexa=no\r\n");
		sb.append("a=rtpmap:96 telephone-event/8000\r\n");
		sb.append("a=fmtp:96 0-15,36\r\n");
		sb.append("a=X-sqn:0\r\n");
		sb.append("a=X-cap:1 audio RTP/AVP 100\r\n");
		sb.append("a=X-cpar:a=rtpmap:100 X-NSE/8000\r\n");
		sb.append("a=X-cpar:a=fmtp:100 192-194,200-202\r\n");
		sb.append("a=X-cap:2 image udptl t38\r\n");
		sb.append("a=silenceSupp:off - - - -\r\n");
		sb.append("a=ptime:20");

		SessionDescription sd = SdpFactory.getInstance().createSessionDescription(sb.toString());

		HashMap<String, String> dynamicPayloadMap = new HashMap<String, String>();
		dynamicPayloadMap.put("telephone-event/8000", "101");

		// act
		SessionDescriptionHelper.setMediaDescription(sd, (MediaDescription)sd.getMediaDescriptions(false).get(0), dynamicPayloadMap);

		// assert
		String[] sdpLines = sd.toString().split("\r\n");
		assertEquals(22, sdpLines.length);
		assertEquals("m=audio 34848 RTP/AVP 0 8 18 4 101", sdpLines[5]);
		assertEquals("a=rtpmap:0 PCMU/8000", sdpLines[7]);
		assertEquals("a=rtpmap:8 PCMA/8000", sdpLines[8]);
		assertEquals("a=rtpmap:101 telephone-event/8000", sdpLines[13]);
		assertEquals("a=fmtp:101 0-15,36", sdpLines[14]);
		assertEquals("a=X-sqn:0", sdpLines[15]);
		assertEquals("a=X-cap:1 audio RTP/AVP 100", sdpLines[16]);
		assertEquals("a=X-cpar:a=rtpmap:100 X-NSE/8000", sdpLines[17]);
		assertEquals("a=X-cpar:a=fmtp:100 192-194,200-202", sdpLines[18]);
		assertEquals("a=X-cap:2 image udptl t38", sdpLines[19]);
		assertEquals("a=silenceSupp:off - - - -", sdpLines[20]);
		assertEquals("a=ptime:20", sdpLines[21]);
	}

    // test that the active media description returned is not a video one
    @Test
    public void testGetActiveMediaDescription() throws Exception {
        // setup
        SessionDescription sd = SessionDescriptionHelper.createSessionDescription("0.0.0.0", "SimpleSipStack");

        MediaDescription md1 = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"0"});
        Vector<Attribute> attributes = new Vector<Attribute>();
        attributes.add(SdpFactory.getInstance().createAttribute("rtpmap", "0 PCMU/8000"));
        addSpecialAttributes(attributes);
        md1.setAttributes(attributes);
        
        MediaDescription md2 = SdpFactory.getInstance().createMediaDescription("video", 5678, 0, "RTP/AVP", new String[] {"0"});
        md2.setAttributes(attributes);
        
        Vector<MediaDescription> mediaDescriptions = new Vector<MediaDescription>();
        mediaDescriptions.add(md1);
        mediaDescriptions.add(md2);
        sd.setMediaDescriptions(mediaDescriptions);

        // act
        MediaDescription extractedMediaDescription = SessionDescriptionHelper.getActiveMediaDescription(sd);

        // assert
        assertFalse(extractedMediaDescription.toString().contains("video"));
    }

    // test that the active media description returns null when there are no Audio Media Descriptions in the SDP
    @Test
    public void testGetActiveMediaDescriptionReturnsNullWhenNoAudioMediaDescriptions() throws Exception {
        // setup
        SessionDescription sd = SessionDescriptionHelper.createSessionDescription("0.0.0.0", "SimpleSipStack");

        MediaDescription md1 = SdpFactory.getInstance().createMediaDescription("video", 1234, 0, "RTP/AVP", new String[] {"0"});
        Vector<Attribute> attributes = new Vector<Attribute>();
        attributes.add(SdpFactory.getInstance().createAttribute("rtpmap", "0 PCMU/8000"));
        addSpecialAttributes(attributes);
        md1.setAttributes(attributes);

        MediaDescription md2 = SdpFactory.getInstance().createMediaDescription("video", 5678, 0, "RTP/AVP", new String[] {"0"});
        md2.setAttributes(attributes);

        Vector<MediaDescription> mediaDescriptions = new Vector<MediaDescription>();
        mediaDescriptions.add(md1);
        mediaDescriptions.add(md2);
        sd.setMediaDescriptions(mediaDescriptions);

        // act
        MediaDescription extractedMediaDescription = SessionDescriptionHelper.getActiveMediaDescription(sd);

        // assert
        assertNull(extractedMediaDescription);
    }

    // test that we can detect whether there is an active (port not zero) Video MediaDescription - True
    @Test
    public void testHasActiveVideoMediaDescriptionTrue() throws Exception {
        // setup
        SessionDescription sd = SessionDescriptionHelper.createSessionDescription("0.0.0.0", "SimpleSipStack");

        MediaDescription md1 = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"0"});
        Vector<Attribute> attributes = new Vector<Attribute>();
        attributes.add(SdpFactory.getInstance().createAttribute("rtpmap", "0 PCMU/8000"));
        addSpecialAttributes(attributes);
        md1.setAttributes(attributes);

        MediaDescription md2 = SdpFactory.getInstance().createMediaDescription("video", 5678, 0, "RTP/AVP", new String[] {"0"});
        md2.setAttributes(attributes);

        Vector<MediaDescription> mediaDescriptions = new Vector<MediaDescription>();
        mediaDescriptions.add(md1);
        mediaDescriptions.add(md2);
        sd.setMediaDescriptions(mediaDescriptions);

        // act/assert
        assertTrue(SessionDescriptionHelper.hasActiveVideoMediaDescription(sd));
    }

    // test that we can detect whether there is an active (port not zero) Video MediaDescription - False - no Video
    @Test
    public void testHasActiveVideoMediaDescriptionFalseNoVideo() throws Exception {
        // setup
        SessionDescription sd = SessionDescriptionHelper.createSessionDescription("0.0.0.0", "SimpleSipStack");

        MediaDescription md1 = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"0"});
        Vector<Attribute> attributes = new Vector<Attribute>();
        attributes.add(SdpFactory.getInstance().createAttribute("rtpmap", "0 PCMU/8000"));
        addSpecialAttributes(attributes);
        md1.setAttributes(attributes);

        Vector<MediaDescription> mediaDescriptions = new Vector<MediaDescription>();
        mediaDescriptions.add(md1);
        sd.setMediaDescriptions(mediaDescriptions);

        // act/assert
        assertFalse(SessionDescriptionHelper.hasActiveVideoMediaDescription(sd));
    }

    // test that we can detect whether there is an active (port not zero) Video MediaDescription - False - Video with zero port
    @Test
    public void testHasActiveVideoMediaDescriptionFalseVideoPortZero() throws Exception {
        // setup
        SessionDescription sd = SessionDescriptionHelper.createSessionDescription("0.0.0.0", "SimpleSipStack");

        MediaDescription md1 = SdpFactory.getInstance().createMediaDescription("audio", 1234, 0, "RTP/AVP", new String[] {"0"});
        Vector<Attribute> attributes = new Vector<Attribute>();
        attributes.add(SdpFactory.getInstance().createAttribute("rtpmap", "0 PCMU/8000"));
        addSpecialAttributes(attributes);
        md1.setAttributes(attributes);

        MediaDescription md2 = SdpFactory.getInstance().createMediaDescription("video", 0, 0, "RTP/AVP", new String[] {"0"});
        md2.setAttributes(attributes);

        Vector<MediaDescription> mediaDescriptions = new Vector<MediaDescription>();
        mediaDescriptions.add(md1);
        mediaDescriptions.add(md2);
        sd.setMediaDescriptions(mediaDescriptions);

        // act/assert
        assertFalse(SessionDescriptionHelper.hasActiveVideoMediaDescription(sd));
    }
}
