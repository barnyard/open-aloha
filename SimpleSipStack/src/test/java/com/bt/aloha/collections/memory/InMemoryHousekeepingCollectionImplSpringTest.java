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

 	

 	
 	
 
package com.bt.aloha.collections.memory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.collections.memory.InMemoryHousekeepingCollectionImpl;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.TerminationCause;
import com.bt.aloha.testing.SimpleSipStackPerClassTestCase;
import com.bt.aloha.util.Housekeeper;

public class InMemoryHousekeepingCollectionImplSpringTest extends SimpleSipStackPerClassTestCase {

	private InMemoryHousekeepingCollectionImpl<DialogInfo> dc;
	private DialogInfo di1;

	@SuppressWarnings("unchecked")
	@Before
	public void beforeMethod() {
		dc = (InMemoryHousekeepingCollectionImpl<DialogInfo>) getApplicationContext().getBean("dialogCollectionBacker");
		di1 = new DialogInfo("id1", "inboundCallLegBean", "1.2.3.4");
		for (String dialogId : dc.getAll().keySet())
			dc.remove(dialogId);
	}

	@Test
	public void housekeepThreeExpiredAndDeadObjects() throws Exception {
		//setup
        di1.setDialogState(DialogState.Terminated);
		DialogInfo di2 = new DialogInfo("id2", "inboundCallLegBean", "1.2.3.4");
        di2.setDialogState(DialogState.Terminated);
        DialogInfo di3 = new DialogInfo("id3", "inboundCallLegBean", "1.2.3.4");
        di3.setDialogState(DialogState.Terminated);
		dc.add(di1);
		dc.add(di2);
        dc.add(di3);

		assertEquals(3, dc.size());

		dc.setMaxTimeToLive(100);

		Thread.sleep(200);
		//act
		//wait for housekeeper to remove the old dialogInfos
        dc.housekeep();

		//assert
		assertEquals(0, dc.size());
	}

	@Test
	public void housekeepThreeExpiredObjectsOneShouldBePreparedForHousekeeping() throws Exception {
		//setup
		di1.setDialogState(DialogState.Terminated);
		DialogInfo di2 = new DialogInfo("id2", "inboundCallLegBean", "1.2.3.4");
		di2.setDialogState(DialogState.Terminated);
		DialogInfo di3 = new DialogInfo("id3", "inboundCallLegBean", "1.2.3.4");
		di3.setDialogState(DialogState.Created);
		dc.add(di1);
		dc.add(di2);
		dc.add(di3);

		assertEquals(3, dc.size());

		dc.setMaxTimeToLive(100);

		Thread.sleep(200);
		((Housekeeper)dc).housekeep();
		assertEquals(1, dc.size());
		assertTrue(dc.get("id3").isHousekeepForced());
		assertEquals(TerminationCause.Housekept, dc.get("id3").getTerminationCause());

		//act
		//wait for housekeeper to remove the old dialogInfos
		((Housekeeper)dc).housekeep();

		//assert
		assertEquals(0, dc.size());
	}

	@Test
	public void housekeepThreeExpiredButNotDeadObjects() throws Exception {
		//setup
		di1.setDialogState(DialogState.Created);
		DialogInfo di2 = new DialogInfo("id2", "inboundCallLegBean", "1.2.3.4");
		di2.setDialogState(DialogState.Created);
		DialogInfo di3 = new DialogInfo("id3", "inboundCallLegBean", "1.2.3.4");
		di3.setDialogState(DialogState.Created);
		dc.add(di1);
		dc.add(di2);
		dc.add(di3);

		assertEquals(3, dc.size());

		dc.setMaxTimeToLive(100);

		//wait for housekeeper to remove the old dialogInfos
		Thread.sleep(200);
		((Housekeeper)dc).housekeep();
		assertEquals(3, dc.size());
		assertTrue(dc.get("id1").isHousekeepForced());
		assertTrue(dc.get("id2").isHousekeepForced());
		assertTrue(dc.get("id3").isHousekeepForced());
		assertEquals(TerminationCause.Housekept, dc.get("id1").getTerminationCause());
		assertEquals(TerminationCause.Housekept, dc.get("id2").getTerminationCause());
		assertEquals(TerminationCause.Housekept, dc.get("id3").getTerminationCause());

		//act
		dc.housekeep();

		// assert
		assertEquals(0, dc.size());
	}

	// Make sure we don't stop housekeeping when expired not dead infos throw exceptions
	@Test
	public void expiredNotDeadThrowsExceptionButHousekeepingStillContinues() {
		// setup
		di1 = new DialogInfo("id1", "callBean", "1.2.3.4");
		di1.setDialogState(DialogState.Confirmed);
		di1.setLastUsedTime(System.currentTimeMillis()-1000000);
		DialogInfo di2 = new DialogInfo("id2", "callBean", "1.2.3.4");
        di2.setDialogState(DialogState.Confirmed);
		di2.setLastUsedTime(System.currentTimeMillis()-1000000);
		dc.add(di1);
		dc.add(di2);

		dc.housekeep();

		assertTrue(dc.get("id1").isHousekeepForced());
		assertTrue(dc.get("id2").isHousekeepForced());

		// act
		dc.housekeep();

		// assert
		assertEquals(0, dc.size());
	}

	@Test
	public void housekeepTwoExpiredButNotDeadObjectsWhenTerminatingThrowsException() throws Exception {
		//setup
		DialogInfo di1 = new DialogInfo("id1", "housekeeperAwareThrowingException", "1.1.1.1");
		di1.setDialogState(DialogState.Created);
		DialogInfo di2 = new DialogInfo("id2", "housekeeperAwareThrowingException", "1.2.3.4");
		di2.setDialogState(DialogState.Created);
		dc.add(di1);
		dc.add(di2);

		assertEquals(2, dc.size());

		dc.setMaxTimeToLive(100);

		//wait for housekeeper to remove the old dialogInfos
		Thread.sleep(200);
		((Housekeeper)dc).housekeep();
		assertEquals(2, dc.size());
		assertTrue(dc.get("id1").isHousekeepForced());
		assertTrue(dc.get("id2").isHousekeepForced());

		//act
		dc.housekeep();

		// assert
		assertEquals(0, dc.size());
	}
}
