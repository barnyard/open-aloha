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

package com.bt.aloha.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.bt.aloha.dialog.state.DialogInfo;

public class ObjectSerialiserTest {

    // Test serialization of the info object to byte array.
    @Test
    public void testConversionObjectAsByteArray() throws Exception {
        // setup
        DialogInfo info = new DialogInfo("1", "x", "y");
        ObjectSerialiser objectSerialiser = new ObjectSerialiser();
        // act
        byte[] asBytes = objectSerialiser.serialise(info);
        // assert
        assertTrue(asBytes.length > 0);
    }

    // Test deserialization of the info object from byte array.
    @Test
    public void testConversionByteArrayToObject() throws Exception {
        // setup
        DialogInfo info = new DialogInfo("1", "x", "y");
        ObjectSerialiser objectSerialiser = new ObjectSerialiser();
        byte[] asBytes = objectSerialiser.serialise(info);
        // act
        DialogInfo deserializedInfo = (DialogInfo) objectSerialiser.deserialise(asBytes);
        // assert
        assertEquals("1", deserializedInfo.getId());
    }
}
