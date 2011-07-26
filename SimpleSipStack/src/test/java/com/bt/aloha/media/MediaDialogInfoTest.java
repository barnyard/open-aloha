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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.bt.aloha.media.MediaDialogInfo;


public class MediaDialogInfoTest {

	// test to keep emma happy
	@Test
	public void testConstructorString() throws Exception {
		assertNotNull(new MediaDialogInfo("id", "p", "1.2.3.4"));
	}

	// test to make sure mediadialoginfo can never have autoterminate false and autohold true
    @Test
    public void testConstructorStringStringStringStringInt() throws Exception {
        //setup
        MediaDialogInfo mdi = new MediaDialogInfo("id", "p", "1.2.3.4", "q", "r", "s", 1);

        //act
        mdi.setAutoTerminate(false);
        mdi.setAutomaticallyPlaceOnHold(true);

        //assert
        assertTrue(mdi.isAutoTerminate());
        assertFalse(mdi.isAutomaticallyPlaceOnHold());
    }
    
	// test to make sure mediadialoginfo can never have autoterminate false and autohold true
	@Test
	public void testAutoTerminateNeverFalseAndAutoHoldIsNeverTrue() throws Exception {
		// act
		MediaDialogInfo mediaDialogInfo = new MediaDialogInfo("id", "p", "1.2.3.4");
		
		// assert
		assertTrue(mediaDialogInfo.isAutoTerminate());
		assertFalse(mediaDialogInfo.isAutomaticallyPlaceOnHold());
	}
}
