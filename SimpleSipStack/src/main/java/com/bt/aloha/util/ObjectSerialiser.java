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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class ObjectSerialiser {
    private static final String CANNOT_DESERIALIZE_INFO_OBJECT_S = "Cannot deserialize info object %s";

    public ObjectSerialiser() {

    }

    public byte[] serialise(Object info) {
        if (info == null)
            throw new IllegalArgumentException("Object to serialise is null");
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(info);
            oos.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Cannot serialize info object of type", info.getClass()),
                    e);
        }
    }

    public Object deserialise(byte[] byteArray) {
        if (byteArray == null)
            throw new IllegalArgumentException("Array to deserialise is null");
        try {
            ObjectInputStream oip = new ObjectInputStream(new ByteArrayInputStream(byteArray));
            return oip.readObject();
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format(CANNOT_DESERIALIZE_INFO_OBJECT_S, byteArray), e);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(String.format(CANNOT_DESERIALIZE_INFO_OBJECT_S, byteArray), e);
        }
    }
}
