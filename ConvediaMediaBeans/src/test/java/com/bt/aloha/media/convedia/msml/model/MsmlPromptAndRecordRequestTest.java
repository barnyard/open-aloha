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

 	

 	
 	
 
package com.bt.aloha.media.convedia.msml.model;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.bt.aloha.media.PromptAndRecordCommand;

public class MsmlPromptAndRecordRequestTest {

    @Test(expected=IllegalArgumentException.class)
    public void testConstructorTargetAddressNull() {
        PromptAndRecordCommand promptAndRecordCommand = new PromptAndRecordCommand("uri", true, "dest", false, "format", 10, 1, 1, null);
        new MsmlPromptAndRecordRequest(null, promptAndRecordCommand);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testConstructorPromptAndRecordCommandNull() {
        new MsmlPromptAndRecordRequest("fred", null);
    }

    // test the construction of the xml with a null termination key
    @Test
    public void testGetXmlNullTerminationKey() {
        PromptAndRecordCommand promptAndRecordCommand = new PromptAndRecordCommand("file://uri", true, "file://dest", false, "audio/wav", 10, 1, 2, null);
        MsmlPromptAndRecordRequest request = new MsmlPromptAndRecordRequest("fred", "myCommandId", promptAndRecordCommand);
        String xml = request.getXml();
        assertXml(xml);
    }

    private void assertXml(String xml) {
        System.out.println(xml);
        assertTrue(xml.contains("<audio uri=\"file://uri\"/>"));
        assertTrue(xml.contains("<record dest=\"file://dest\""));
        assertTrue(xml.contains("format=\"audio/wav\""));
        assertTrue(xml.contains("append=\"false\""));
        assertTrue(xml.contains("maxtime=\"10s\""));
        assertTrue(xml.contains("cvd:pre-speech=\"1s\""));
        assertTrue(xml.contains("cvd:post-speech=\"2s\""));
        assertTrue(xml.contains("event=\"app.recordPlayCommandComplete\" namelist=\"record.recordid record.len record.end\""));
    }

    // test the construction of the xml with a termination key
    @Test
    public void testGetXmlWithTerminationKey() {
        PromptAndRecordCommand promptAndRecordCommand = new PromptAndRecordCommand("file://uri", true, "file://dest", false, "audio/wav", 10, 1, 2, '3');
        MsmlPromptAndRecordRequest request = new MsmlPromptAndRecordRequest("fred", "myCommandId", promptAndRecordCommand);
        String xml = request.getXml();
        assertXml(xml);
        assertTrue(xml.contains("cvd:termkey=\"3\""));
    }
}
