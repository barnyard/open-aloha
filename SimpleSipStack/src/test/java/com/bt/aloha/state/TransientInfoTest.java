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

 	

 	
 	
 
package com.bt.aloha.state;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.state.TransientInfo;

// all this stuff is just for Emma to increase coverage
public class TransientInfoTest {

    TransientInfo transientInfo;

    @Before
    public void setUp() {
        this.transientInfo = new TransientInfo("t", new HashMap<String, Object>());
    }

    @Test
    public void testGetTransients() {
        assertNull(transientInfo.getTransients());
    }

    @Test
    public void testSetTransients() {
        transientInfo.setTransients(null);
    }

    @Test
    public void testIsDead() {
        assertFalse(transientInfo.isDead());
    }
}