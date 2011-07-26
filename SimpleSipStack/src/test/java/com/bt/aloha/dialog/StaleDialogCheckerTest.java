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

 	

 	
 	
 
package com.bt.aloha.dialog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Vector;

import javax.sip.ClientTransaction;
import javax.sip.message.Request;

import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bt.aloha.collections.memory.InMemoryHousekeepingCollectionImpl;
import com.bt.aloha.dialog.DialogBeanHelper;
import com.bt.aloha.dialog.StaleDialogChecker;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.collections.DialogCollectionImpl;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.stack.SimpleSipStack;

public class StaleDialogCheckerTest {

	private StaleDialogChecker staleDialogChecker;
	private DialogCollection dialogCollection;
	private Request request;
	private int createInfoRequestCalled = 0;
	private int sendRequestCount = 0;
	private List<String> dialogInfoId;
    private SimpleSipStack simpleSipStack;

	class MyDialogBeanHelper extends DialogBeanHelper{

		@Override
		public Request createInfoRequest(ReadOnlyDialogInfo dialogInfo) {
			createInfoRequestCalled++;
			dialogInfoId.add(dialogInfo.getId());
			return request;
		}

		@Override
		public ClientTransaction sendRequest(Request request) {
			sendRequestCount++;
			return null;
		}

	}

	@Before
	public void setup(){
		dialogInfoId = new Vector<String>();
		sendRequestCount = 0;
		createInfoRequestCalled = 0;
		staleDialogChecker = new StaleDialogChecker();

		dialogCollection = new DialogCollectionImpl(new InMemoryHousekeepingCollectionImpl<DialogInfo>());
		staleDialogChecker.setDialogCollection(dialogCollection);
        DialogBeanHelper dialogBeanHelper = new MyDialogBeanHelper();
        simpleSipStack = EasyMock.createNiceMock(SimpleSipStack.class);

        dialogBeanHelper.setSimpleSipStack(simpleSipStack);
		staleDialogChecker.setDialogBeanHelper(dialogBeanHelper);
		request = EasyMock.createNiceMock(Request.class);
	}

	// add one dialog info to the collection, on init check that the state is confirmed
	// and assert that the appropriate methods are called on DialogBeanHelper
	@Test
	public void testInitializeSingleConfirmed() throws Exception {
		// setup
		ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
		te.initialize();
		staleDialogChecker.setTaskExecutor(te);
		
		DialogInfo dialogInfo = new DialogInfo("1", "sipBeanId", "localhost");
		dialogInfo.setDialogState(DialogState.Confirmed);
		dialogCollection.add(dialogInfo);
        EasyMock.expect(simpleSipStack.enqueueRequestAssignSequenceNumber(dialogInfo.getSipCallId(), dialogInfo.getSequenceNumber() + 1, Request.INFO)).andReturn(dialogInfo.getSequenceNumber()+1);
        EasyMock.replay(simpleSipStack);

        // act
		staleDialogChecker.runTask();
		Thread.sleep(1000); // give thread time to complete

        // assert
		assertEquals(1, createInfoRequestCalled);
		assertEquals(1, sendRequestCount);
		assertTrue(dialogInfoId.contains("1"));
		assertEquals(1, dialogInfoId.size());
        assertEquals(2, dialogCollection.get("1").getSequenceNumber());
        EasyMock.verify(simpleSipStack);
	}

	// add one dialog info to the collection, on init check that the state is not confirmed
	// and assert that the appropriate methods are not called on DialogBeanHelper
	@Test
	public void testInitializeSingleNotConfirmed() {
		// setup
		DialogInfo dialogInfo = new DialogInfo("2", "sipBeanId", "localhost");
		dialogInfo.setDialogState(DialogState.Early);
		dialogCollection.add(dialogInfo);
        EasyMock.replay(simpleSipStack);
		// act
		staleDialogChecker.initialize();
		// assert
		assertEquals(0, createInfoRequestCalled);
		assertEquals(0, sendRequestCount);
		assertEquals(0, dialogInfoId.size());
        EasyMock.verify(simpleSipStack);
	}

	// add two dialog infos to the collection,
	// assert method calls for info1, but not info2 through use of a counter
	@Test
	public void testInitializeMultipleOneConfirmed() {
		// setup
		DialogInfo dialogInfo1 = new DialogInfo("1", "sipBeanId", "localhost");
		DialogInfo dialogInfo2 = new DialogInfo("2", "sipBeanId", "localhost");
		dialogInfo1.setDialogState(DialogState.Confirmed);
		dialogInfo2.setDialogState(DialogState.Terminated);
		dialogCollection.add(dialogInfo1);
		dialogCollection.add(dialogInfo2);
        EasyMock.expect(simpleSipStack.enqueueRequestAssignSequenceNumber(dialogInfo1.getSipCallId(), dialogInfo1.getSequenceNumber() + 1, Request.INFO)).andReturn(dialogInfo1.getSequenceNumber()+1);
        EasyMock.replay(simpleSipStack);
		// act
		staleDialogChecker.initialize();
		// assert
		assertEquals(1, createInfoRequestCalled);
		assertEquals(1, sendRequestCount);
		assertTrue(dialogInfoId.contains("1"));
		assertEquals(1, dialogInfoId.size());
        assertEquals(2, dialogCollection.get("1").getSequenceNumber());
        EasyMock.verify(simpleSipStack);
	}

	// add three dialog infos to the collection,
	// assert method calls for info1 & into3, but not info2 through use of a counter
	@Test
	public void testInitializeMultipleTwoConfirmed() {
		// setup
		DialogInfo dialogInfo1 = new DialogInfo("1", "sipBeanId", "localhost");
		DialogInfo dialogInfo2 = new DialogInfo("2", "sipBeanId", "localhost");
		DialogInfo dialogInfo3 = new DialogInfo("3", "sipBeanId", "localhost");
		dialogInfo1.setDialogState(DialogState.Confirmed);
		dialogInfo2.setDialogState(DialogState.Terminated);
		dialogInfo3.setDialogState(DialogState.Confirmed);
		dialogCollection.add(dialogInfo1);
		dialogCollection.add(dialogInfo2);
		dialogCollection.add(dialogInfo3);
        EasyMock.expect(simpleSipStack.enqueueRequestAssignSequenceNumber(dialogInfo1.getSipCallId(), dialogInfo1.getSequenceNumber() + 1, Request.INFO)).andReturn(dialogInfo1.getSequenceNumber()+1);
        EasyMock.expect(simpleSipStack.enqueueRequestAssignSequenceNumber(dialogInfo3.getSipCallId(), dialogInfo3.getSequenceNumber() + 1, Request.INFO)).andReturn(dialogInfo3.getSequenceNumber()+2);
        EasyMock.replay(simpleSipStack);
		// act
		staleDialogChecker.initialize();
		// assert
		assertEquals(2, createInfoRequestCalled);
		assertEquals(2, sendRequestCount);
		assertTrue(dialogInfoId.contains("1"));
		assertTrue(dialogInfoId.contains("3"));
		assertEquals(2, dialogInfoId.size());
        assertEquals(2, dialogCollection.get("1").getSequenceNumber());
        assertEquals(3, dialogCollection.get("3").getSequenceNumber());
        EasyMock.verify(simpleSipStack);
	}

}
