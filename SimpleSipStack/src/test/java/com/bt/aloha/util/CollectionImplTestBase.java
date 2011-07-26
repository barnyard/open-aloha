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
package com.bt.aloha.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.util.List;
import java.util.Vector;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.util.ConcurrentUpdateException;
import com.bt.aloha.util.OptimisticConcurrencyCollection;

@Ignore
public class CollectionImplTestBase {
	protected OptimisticConcurrencyCollection<DialogInfo> dc;
    protected DialogInfo di1;

    @After
    public void after() {
        cleanStore();
    }

	protected void cleanStore() {
		List<String> keysToRemove = new Vector<String>();
        keysToRemove.addAll(dc.getAll().keySet());
        for (String key: keysToRemove) {
            dc.remove(key);
        }
//        assertEquals(0, dc.size());
	}

    @Test
	public void testRemoveDialog() throws Exception {
		//setup
		String callId = di1.getId();

		dc.add(di1);
		assertEquals(1, dc.size());

		//act
		dc.remove(callId);

		//assert

		assertEquals(0, dc.size());
	}

	@Test
	public void testRemoveNotExistingDialog() throws Exception {
		//setup
		dc.add(di1);
		assertEquals(1, dc.size());

		//act
		dc.remove("iamanidiot");

		//assert
		assertEquals(1, dc.size());
	}

	@Test
	public void testGetDialogReturnsNewObjectButSameCallId() throws Exception {
		// setup
		dc.add(di1);

		String callId = di1.getId();

		// act
		DialogInfo di2 = dc.get(callId);

		// assert
		assertNotSame(di1, di2);
		assertEquals(callId, di2.getId());
	}

	@Test
	public void testAddDialogCreatesNewInstance() throws Exception {
		// setup
		di1.setDialogState(DialogState.Confirmed);
		dc.add(di1);

		// act
		di1.setDialogState(DialogState.Terminated);

		// assert
		assertEquals(DialogState.Confirmed, dc.get(di1.getId()).getDialogState());
	}

	@Test
	public void testReplaceDialog() throws Exception {
		// setup
		di1.setDialogState(DialogState.Confirmed);
		dc.add(di1);
		di1.setDialogState(DialogState.Terminated);

		// act
		dc.replace(di1);

		// assert
		assertEquals(DialogState.Terminated, dc.get(di1.getId()).getDialogState());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testReplaceDialogNullDialog() throws Exception {
		// act
		dc.replace(null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testReplaceDialogUnknownDialog() throws Exception {
		// act
		dc.replace(new DialogInfo("id", "bean1", "1.2.3.4"));
	}

	@Test(expected=ConcurrentUpdateException.class)
	public void testReplaceDialogAlreadyModifiedDialog() throws Exception {
		// setup
		DialogInfo di2 = di1.cloneObject();
		di1.setDialogState(DialogState.Confirmed);
		dc.add(di1);
		Thread.sleep(10);
		dc.replace(di1);
		Thread.sleep(10);

		// act
		dc.replace(di2);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetDialogNullArgument() throws Exception {
		// act
		dc.get(null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testAddDialogExistingDialog() throws Exception {
		// setup
		dc.add(di1);

		// act
		dc.add(di1);
	}
}
