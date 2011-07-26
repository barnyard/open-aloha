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

 	

 	
 	
 
package com.bt.aloha.dialog.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.sdp.MediaDescription;

import org.junit.Test;

import com.bt.aloha.dialog.state.PendingReinvite;
import com.bt.aloha.stack.SessionDescriptionHelper;


public class PendingReinviteTest {
	@Test
	public void testCloneDoesDeepCopy() throws Exception {
		// setup
		MediaDescription mediaDescription = SessionDescriptionHelper.generateHoldMediaDescription();
		PendingReinvite p1 = new PendingReinvite(mediaDescription, true, "ho");

		// act
		PendingReinvite p2 = p1.clone();

		// assert
		assertEquals(p1.getMediaDescription().toString(), p2.getMediaDescription().toString());
		assertTrue(p1.getMediaDescription() != p2.getMediaDescription());
	}

    @Test
    public void testSerialisationAndDeserialisation() throws Exception {
        // setup
        MediaDescription mediaDescription = SessionDescriptionHelper.generateHoldMediaDescription();
        PendingReinvite p1 = new PendingReinvite(mediaDescription, true, "ho");

        // act
        byte[] byteArray = serialise(p1);
        PendingReinvite p2 = deserialise(byteArray);

        // assert
        assertNotSame(p1, p2);
        assertEquals(p1.getMediaDescription().toString(), p2.getMediaDescription().toString());
        assertEquals(p1.getApplicationData(), p2.getApplicationData());
        assertEquals(p1.getAutoTerminate(), p2.getAutoTerminate());

    }

    protected byte[] serialise(PendingReinvite info) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(info);
        oos.flush();
        return bos.toByteArray();
    }

    protected PendingReinvite deserialise(byte[] byteArray) throws Exception {
        ObjectInputStream oip = new ObjectInputStream(new ByteArrayInputStream(byteArray));
        return (PendingReinvite) oip.readObject();
    }

}
