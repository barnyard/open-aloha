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
package com.bt.aloha.collections.memory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.collections.memory.InMemoryHousekeepingCollectionImpl;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.state.StateInfoBase;
import com.bt.aloha.util.Housekeeper;

public class InMemoryHousekeepingCollectionImplTest {

	private InMemoryHousekeepingCollectionImpl<DialogInfo> dc;
	private DialogInfo di1;
    private StateInfoBase<DialogInfo> di;

	@Before
	public void before() {
		dc = new InMemoryHousekeepingCollectionImpl<DialogInfo>();
		di1 = new DialogInfo("id1", "bean1", "1.2.3.4");
	}

	@Test
	public void testHouseKeeper() throws Exception {
		//setup
        di1.setDialogState(DialogState.Terminated);
		DialogInfo di2 = new DialogInfo("id2", "bean2", "1.2.3.4");
        di2.setDialogState(DialogState.Terminated);
        DialogInfo di3 = new DialogInfo("id3", "bean3", "1.2.3.4");
        di3.setDialogState(DialogState.Created);
		dc.add(di1);
		dc.add(di2);
        dc.add(di3);

		assertEquals(3, dc.size());

		dc.setMaxTimeToLive(1000);

		//act
		//wait for housekeeper to remove the old dialogInfos
		Thread.sleep(2000);

        ((Housekeeper)dc).housekeep();

		//assert
		assertEquals(1, dc.size());
	}

    // test that the housekeeping doesn't retain any locks on the collection objects
    @Test
    public void testHouseKeeperReleasesLockAfterProcessing() throws Exception {
        //setup
        dc.add(di1);
        this.di = null;

        assertEquals(1, dc.size());

        dc.setMaxTimeToLive(2000);

        assertEquals(1, this.dc.size());

        //act
        //wait for housekeeper to do it's stuff
        Thread.sleep(1000);

        ((Housekeeper)this.dc).housekeep();

        //assert
        assertEquals(1, this.dc.size());

        Runnable runnable = new Runnable() {
            public void run() {
                StateInfoBase<DialogInfo> dialogInfo = dc.get(di1.getSipCallId());
                setDialogInfo(dialogInfo);
            }
        };

        new Thread(runnable).start();

        Thread.sleep(500);

        assertNotNull(this.di);

        //post-test cleaning
        this.dc.destroy();
    }

    // test that locks get released even if checking a DialogInfo throws an exception
    @Test
    public void testHouseKeeperReleasesLockAfterException() throws Exception {
        //setup
        final DialogInfo dodgyDialogInfo = new DialogInfo("id", "dodgy", "1.2.3.4") {
            /**
			 *
			 */
			private static final long serialVersionUID = 3592406775445586709L;

			@Override
            public long getLastUsedTime() {
                throw new RuntimeException("shit happens");
            }
        };
        dc.add(dodgyDialogInfo);
        this.di = null;

        assertEquals(1, dc.size());

        dc.setMaxTimeToLive(2000);

        assertEquals(1, this.dc.size());

        //act
        //wait for housekeeper to do it's stuff
        Thread.sleep(1000);

        ((Housekeeper)this.dc).housekeep();

        //assert
        assertEquals(1, this.dc.size());

        Runnable runnable = new Runnable() {
            public void run() {
                StateInfoBase<DialogInfo> dialogInfo = dc.get(dodgyDialogInfo.getSipCallId());
                setDialogInfo(dialogInfo);
            }
        };

        new Thread(runnable).start();

        Thread.sleep(500);

        assertNotNull(this.di);

        //post-test cleaning
        this.dc.destroy();
    }

    private void setDialogInfo(StateInfoBase<DialogInfo> dialogInfo) {
        this.di = dialogInfo;
    }

	@Test
	public void testDoExtraUpdates() throws Exception {
		// setup
		DialogInfo di2 = di1.cloneObject();
		long lastUsedTime = di1.getLastUsedTime();
        Thread.sleep(2);

		// act
		dc.doExtraUpdates(di1, di2);

		// assert
		assertTrue("last used time was not updated", lastUsedTime <= di2.getLastUsedTime());
		assertEquals(di2.getLastUsedTime(), di1.getLastUsedTime());
	}
}
