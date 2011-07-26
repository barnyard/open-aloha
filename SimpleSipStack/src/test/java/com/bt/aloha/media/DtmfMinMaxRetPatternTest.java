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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.bt.aloha.media.DtmfMinMaxRetPattern;

public class DtmfMinMaxRetPatternTest {

    @Test
    public void testDtmfMinMaxRetPatternIntInt() {
        DtmfMinMaxRetPattern dtmfMinMaxRetPattern = new DtmfMinMaxRetPattern(12, 34);
        assertNotNull(dtmfMinMaxRetPattern);
        assertEquals("min=12;max=34", dtmfMinMaxRetPattern.toString());
    }

    @Test
    public void testDtmfMinMaxRetPatternIntIntCharacter() {
        DtmfMinMaxRetPattern dtmfMinMaxRetPattern = new DtmfMinMaxRetPattern(12, 34, 'r', 'c');
        assertNotNull(dtmfMinMaxRetPattern);
        assertEquals("min=12;max=34;rtk=r;cancel=c", dtmfMinMaxRetPattern.toString());
    }

    @Test
    public void testDtmfSameMinMaxRetPatternIntInt() {
        DtmfMinMaxRetPattern dtmfMinMaxRetPattern = new DtmfMinMaxRetPattern(1, 1);
        assertNotNull(dtmfMinMaxRetPattern);
        assertEquals("min=1;max=1", dtmfMinMaxRetPattern.toString());
    }

    @Test
    public void testDtmfSameMinMaxRetPatternIntIntCharacter() {
        DtmfMinMaxRetPattern dtmfMinMaxRetPattern = new DtmfMinMaxRetPattern(1, 1, 'r', 'c');
        assertNotNull(dtmfMinMaxRetPattern);
        assertEquals("min=1;max=1;rtk=r;cancel=c", dtmfMinMaxRetPattern.toString());
    }

    @Test
    public void testDtmfZeroMinMaxRetPatternIntInt() {
        DtmfMinMaxRetPattern dtmfMinMaxRetPattern = new DtmfMinMaxRetPattern(0, 0);
        assertNotNull(dtmfMinMaxRetPattern);
        assertEquals("", dtmfMinMaxRetPattern.toString());
    }

}
