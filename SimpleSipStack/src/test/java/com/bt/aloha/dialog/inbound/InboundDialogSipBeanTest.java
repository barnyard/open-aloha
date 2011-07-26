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
package com.bt.aloha.dialog.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import javax.sdp.SessionDescription;
import javax.sip.ServerTransaction;
import javax.sip.TransactionState;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.easymock.classextension.EasyMock;
import org.junit.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import com.bt.aloha.collections.memory.InMemoryHousekeepingCollectionImpl;
import com.bt.aloha.dialog.DialogBeanHelper;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.collections.DialogCollectionImpl;
import com.bt.aloha.dialog.inbound.InboundDialogSipBean;
import com.bt.aloha.dialog.inbound.InboundDialogSipBeanImpl;
import com.bt.aloha.dialog.inbound.InboundDialogSipListener;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.eventing.EventDispatcher;


public class InboundDialogSipBeanTest {
	/**
	 * Test that we can add the listeners
	 */
	@Test
	public void addListener() {
		// setup
		InboundDialogSipListener listener = EasyMock.createMock(InboundDialogSipListener.class);
		InboundDialogSipBean bean = new InboundDialogSipBeanImpl();
		
		// act
		bean.addInboundDialogListener(listener);
		
		// assert
		assertEquals(1, ((InboundDialogSipBeanImpl)bean).getDialogListeners().size());
		assertSame(listener, ((InboundDialogSipBeanImpl)bean).getDialogListeners().get(0));
	}
	
	/**
	 * Test that we can remove listeners
	 */
	@Test
	public void removeListener() {
		// setup
		InboundDialogSipListener listener = EasyMock.createMock(InboundDialogSipListener.class);
		InboundDialogSipBean bean = new InboundDialogSipBeanImpl();
		bean.addInboundDialogListener(listener);
		
		// act
		bean.removeInboundDialogListener(listener);
		
		// assert
		assertEquals(0, ((InboundDialogSipBeanImpl)bean).getDialogListeners().size());
	}
	
	// test that you cannot add a null listener
	@Test(expected=IllegalArgumentException.class)
	public void addNullListener() {
		// setup
		InboundDialogSipBean bean = new InboundDialogSipBeanImpl();
		
		// act
		bean.addInboundDialogListener(null);
		
		// assert - exception
	}
	
	// test that you cannot remove a null listener
	@Test(expected=IllegalArgumentException.class)
	public void removeNullListener() {
		// setup
		InboundDialogSipBean bean = new InboundDialogSipBeanImpl();
		
		// act
		bean.removeInboundDialogListener(null);
		
		// assert - exception
	}
	
	// test that we clear the initial invite server transaction when sending ok response
	@Test
	public void initialInviteOkResponseClearsInviteServerTransaction() {
		// setup
		DialogBeanHelper dialogBeanHelper = new DialogBeanHelper() {
			@Override
			public void sendInviteOkResponse(Request request, ServerTransaction serverTransaction, String localTag, String sipUserName, SessionDescription sessionDescription) {
				// do nothing
			}
		};
		
		DialogInfo dialogInfo = new DialogInfo("a", "b", "c");
		
		DialogCollection dialogCollection = new DialogCollectionImpl(new InMemoryHousekeepingCollectionImpl<DialogInfo>());
		dialogInfo.setInviteServerTransaction(EasyMock.createMock(ServerTransaction.class));
		dialogInfo.setLocalParty("sip:here");
		dialogInfo.setRemoteParty("sip:there");
		dialogCollection.add(dialogInfo);
		
		InboundDialogSipBeanImpl bean = new InboundDialogSipBeanImpl();
		bean.setDialogBeanHelper(dialogBeanHelper);	
		bean.setDialogCollection(dialogCollection);
		
		// act
		bean.sendInitialOkResponse(dialogInfo.getId());
		
		// assert
		assertNull(dialogCollection.get(dialogInfo.getId()).getInviteServerTransaction());
	}
	
	// test that we clear the initial invite server transaction when sending error response
	@Test
	public void initialInviteErrorResponseClearsInviteServerTransaction() {
		// setup
		DialogBeanHelper dialogBeanHelper = new DialogBeanHelper() {
			@Override
			public void sendResponse(Request request, ServerTransaction serverTransaction, int responseCode) {
				// do nothing
			}
		};
		
		DialogInfo dialogInfo = new DialogInfo("a", "b", "c");
		
		DialogCollection dialogCollection = new DialogCollectionImpl(new InMemoryHousekeepingCollectionImpl<DialogInfo>());
		dialogInfo.setInviteServerTransaction(EasyMock.createMock(ServerTransaction.class));
		dialogCollection.add(dialogInfo);
		
		InboundDialogSipBeanImpl bean = new InboundDialogSipBeanImpl();
		bean.setDialogBeanHelper(dialogBeanHelper);	
		bean.setDialogCollection(dialogCollection);
		
		// act
		bean.sendInitialErrorResponse(dialogInfo.getId(), Response.SERVER_INTERNAL_ERROR);
		
		// assert
		assertNull(dialogCollection.get(dialogInfo.getId()).getInviteServerTransaction());
	}
	
	// test that we clear the initial invite server transaction when cancelling the transaction
	// in response to a received CANCEL
	@Test
	public void initialInviteCancelResponseClearsInviteServerTransaction() {
		// setup
		DialogBeanHelper dialogBeanHelper = new DialogBeanHelper() {
			@Override
			public void sendResponse(Request request, ServerTransaction serverTransaction, int responseCode) {
				// do nothing
			}
		};
		
		DialogInfo dialogInfo = new DialogInfo("a", "b", "c");
		
		ServerTransaction serverTransaction = EasyMock.createNiceMock(ServerTransaction.class);
		EasyMock.expect(serverTransaction.getState()).andReturn(TransactionState.PROCEEDING);
		EasyMock.replay(serverTransaction);
		
		DialogCollection dialogCollection = new DialogCollectionImpl(new InMemoryHousekeepingCollectionImpl<DialogInfo>());
		dialogInfo.setInviteServerTransaction(serverTransaction);
		dialogCollection.add(dialogInfo);
		
		EventDispatcher eventDispatcher = new EventDispatcher();
		eventDispatcher.setTaskExecutor(new SimpleAsyncTaskExecutor());
		
		InboundDialogSipBeanImpl bean = new InboundDialogSipBeanImpl();
		bean.setDialogBeanHelper(dialogBeanHelper);	
		bean.setDialogCollection(dialogCollection);
		bean.setEventDispatcher(eventDispatcher);
		
		Request request = EasyMock.createMock(Request.class);
		
		// act
		bean.processCancel(request, serverTransaction, dialogInfo.getId());
		
		// assert
		assertNull(dialogCollection.get(dialogInfo.getId()).getInviteServerTransaction());
	}
}
