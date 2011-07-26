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

 	

 	
 	
 
package com.bt.aloha.media.event.call;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.bt.aloha.media.event.call.CallAnnouncementCompletedEvent;


public class CallAnnouncementCompletedEventTest {

    // test constructor - not barged
    @Test
    public void testAnnouncementCompletedEventNotBarged() {
        CallAnnouncementCompletedEvent e = new CallAnnouncementCompletedEvent("test", "cmd", "1", false);

        assertEquals("test", e.getCallId());
        assertEquals("cmd", e.getMediaCommandId());
        assertEquals("1", e.getDuration());
        assertFalse(e.getBarged());
    }

    // test 4 argument constructor - barged
    @Test
    public void testAnnouncementCompletedEventBarged() {
        CallAnnouncementCompletedEvent e = new CallAnnouncementCompletedEvent("test", "cmd", "1", true);

        assertEquals("test", e.getCallId());
        assertEquals("cmd", e.getMediaCommandId());
        assertEquals("1", e.getDuration());
        assertTrue(e.getBarged());
    }
}
