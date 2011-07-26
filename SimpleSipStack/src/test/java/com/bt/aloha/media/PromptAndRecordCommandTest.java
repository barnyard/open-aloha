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

 	

 	
 	
 
package com.bt.aloha.media;

import static org.junit.Assert.*;

import org.junit.Test;

import com.bt.aloha.media.PromptAndRecordCommand;

public class PromptAndRecordCommandTest {

    // public PromptAndRecordCommand(String aPromptFileUri,
    //                               boolean isAllowBarge,
    //                               String aDestinationFileUri,
    //                               boolean isAppend,
    //                               String aFormat,
    //                               int aMaxTimeSeconds,
    //                               int anInitialTimeoutSeconds,
    //                               int anExtraTimeoutSeconds,
    //                               Character aTerminationKey) {

    // test constructor
    @Test
    public void testConstructor() {
        PromptAndRecordCommand result = new PromptAndRecordCommand("file://fred.wav",
                                                true,
                                                "file://recording.wav",
                                                false,
                                                "audio/wav",
                                                10,
                                                1,
                                                2,
                                                '1');
        assertEquals("file://fred.wav", result.getPromptFileUri());
        assertTrue(result.isAllowBarge());
        assertEquals("file://recording.wav", result.getDestinationFileUri());
        assertFalse(result.isAppend());
        assertEquals("audio/wav", result.getFormat());
        assertEquals(10, result.getMaxTimeSeconds());
        assertEquals(1, result.getInitialTimeoutSeconds());
        assertEquals(2, result.getExtraTimeoutSeconds());
        assertEquals('1', result.getTerminationKey());
    }

    // test TerminationKey validation
    @Test
    public void testTerminationKeyValidation() {
        char[] termkeys = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '*', '#', 'A', 'B', 'C', 'D', 'a', 'b', 'c', 'd'};
        for (char termkey: termkeys) {
            assertNotNull(new PromptAndRecordCommand("file://fred.wav",
                                                true,
                                                "file://recording.wav",
                                                false,
                                                "audio/wav",
                                                10,
                                                1,
                                                2,
                                                termkey));
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidTerminationKey() {
        new PromptAndRecordCommand("file://fred.wav",
                                                true,
                                                "file://recording.wav",
                                                false,
                                                "audio/wav",
                                                10,
                                                1,
                                                2,
                                                'x');
    }

    // test audioFileUri validation
    @Test(expected=IllegalArgumentException.class)
    public void testPromptFileUriNull() {
        new PromptAndRecordCommand(null,
                true,
                "file://recording.wav",
                false,
                "audio/wav",
                10,
                1,
                2,
                null);
    }

    // test audioFileUri validation
    @Test(expected=IllegalArgumentException.class)
    public void testPromptFileUriNotProvisisonFileOrHttp() {
        new PromptAndRecordCommand("fred.wav",
                true,
                "file://recording.wav",
                false,
                "audio/wav",
                10,
                1,
                2,
                null);
    }

    // test destinationFileUri validation
    @Test(expected=IllegalArgumentException.class)
    public void testDestinationFileUriNull() {
        new PromptAndRecordCommand("file://fred.wav",
                true,
                null,
                false,
                "audio/wav",
                10,
                1,
                2,
                null);
    }

    // test destinationFileUri validation
    @Test(expected=IllegalArgumentException.class)
    public void testDestinationFileUriNotProvisisonFileOrHttp() {
        new PromptAndRecordCommand("file://fred.wav",
                true,
                "recording.wav",
                false,
                "audio/wav",
                10,
                1,
                2,
                null);
    }

    // test valid formats
    @Test
    public void testFormatValidation() {
        String[] allowableFormats = {"audio/wav", "audio/x-wav", "audio/vnd.wave;codec=1",
                "audio/vnd.wave;codec=6", "audio/vnd.wave;codec=7", "audio/vnd.wave;codec=83"};
        for (String format: allowableFormats) {
            assertNotNull(new PromptAndRecordCommand("file://fred.wav",
                true,
                "file://recording.wav",
                false,
                format,
                10,
                1,
                2,
                null));
        }
    }

    // test that null format is converted to "audio/wav"
    @Test
    public void testNullFormatConvertedToAudioWav() {
        PromptAndRecordCommand result = new PromptAndRecordCommand("file://fred.wav",
                true,
                "file://recording.wav",
                false,
                null,
                10,
                1,
                2,
                null);
        assertEquals("audio/wav", result.getFormat());
    }

    // test maxTimeout validation
    @Test(expected=IllegalArgumentException.class)
    public void testMaxTimeoutValidationLessThan1() {
        new PromptAndRecordCommand("file://fred.wav",
                true,
                "file://recording.wav",
                false,
                "audio/wav",
                0,
                1,
                2,
                null);
    }

    // test maxTimeout validation
    @Test(expected=IllegalArgumentException.class)
    public void testMaxTimeoutValidationGreaterThan108000() {
        new PromptAndRecordCommand("file://fred.wav",
                true,
                "file://recording.wav",
                false,
                "audio/wav",
                108001,
                1,
                2,
                null);
    }

    // test initialTimeout validation
    @Test(expected=IllegalArgumentException.class)
    public void testInitialTimeoutValidationLessThan0() {
        new PromptAndRecordCommand("file://fred.wav",
                true,
                "file://recording.wav",
                false,
                "audio/wav",
                10,
                -1,
                2,
                null);
    }

    // test initialTimeout validation
    @Test(expected=IllegalArgumentException.class)
    public void testInitialTimeoutValidationGreaterThan108000() {
        new PromptAndRecordCommand("file://fred.wav",
                true,
                "file://recording.wav",
                false,
                "audio/wav",
                10,
                108001,
                2,
                null);
    }

    // test extraTimeout validation
    @Test(expected=IllegalArgumentException.class)
    public void testExtraTimeoutValidationLessThan0() {
        new PromptAndRecordCommand("file://fred.wav",
                true,
                "file://recording.wav",
                false,
                "audio/wav",
                10,
                1,
                -1,
                null);
    }

    // test extraTimeout validation
    @Test(expected=IllegalArgumentException.class)
    public void testExtraTimeoutValidationGreaterThan108000() {
        new PromptAndRecordCommand("file://fred.wav",
                true,
                "file://recording.wav",
                false,
                "audio/wav",
                10,
                1,
                108001,
                null);
    }
}
