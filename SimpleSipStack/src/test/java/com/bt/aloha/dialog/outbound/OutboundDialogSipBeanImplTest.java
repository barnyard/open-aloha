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
package com.bt.aloha.dialog.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import javax.sip.ClientTransaction;
import javax.sip.ResponseEvent;
import javax.sip.message.Response;

import org.easymock.classextension.EasyMock;
import org.junit.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import com.bt.aloha.collections.memory.InMemoryHousekeepingCollectionImpl;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.collections.DialogCollectionImpl;
import com.bt.aloha.dialog.outbound.OutboundDialogSipBeanImpl;
import com.bt.aloha.dialog.outbound.OutboundDialogSipListener;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.eventing.EventDispatcher;


public class OutboundDialogSipBeanImplTest {
	/**
	 * Tests that we can add a listener to our list of listeners
	 */
	@Test
	public void addListener() {
		// setup
		OutboundDialogSipBeanImpl bean = new OutboundDialogSipBeanImpl();
		OutboundDialogSipListener listener = EasyMock.createMock(OutboundDialogSipListener.class);
		EasyMock.replay(listener);
		
		// act
		bean.addOutboundDialogListener(listener);
		
		// assert
		assertEquals(1, bean.getDialogListeners().size());
		assertSame(listener, bean.getDialogListeners().get(0));
		EasyMock.verify(listener);
	}
	
	/**
	 * Tests that we can remove ourselves from the list of listeners
	 */
	@Test
	public void removeListener() {
		// setup
		OutboundDialogSipBeanImpl bean = new OutboundDialogSipBeanImpl();
		OutboundDialogSipListener listener = EasyMock.createMock(OutboundDialogSipListener.class);
		EasyMock.replay(listener);
		bean.addOutboundDialogListener(listener);
		
		// act
		bean.removeOutboundDialogListener(listener);
		
		// assert
		assertEquals(0, bean.getDialogListeners().size());
		EasyMock.verify(listener);
		
	}
	
	// test that you cannot add a null listener
	@Test(expected=IllegalArgumentException.class)
	public void addNullListener() {
		// setup
		OutboundDialogSipBeanImpl bean = new OutboundDialogSipBeanImpl();
		
		// act
		bean.addOutboundDialogListener(null);
		
		// assert - exception
	}
	
	// test that you cannot remove a null listener
	@Test(expected=IllegalArgumentException.class)
	public void removeNullListener() {
		// setup
		OutboundDialogSipBeanImpl bean = new OutboundDialogSipBeanImpl();
		
		// act
		bean.removeOutboundDialogListener(null);
		
		// assert - exception
	}
	
	// test that a final error response to an invite clears the client transation
	@Test
	public void testInitialInviteErrorResponseClearsClientTransaction() throws Exception {
		// setup
		DialogInfo dialogInfo = new DialogInfo("a", "b", "c");
		dialogInfo.setInviteClientTransaction(EasyMock.createMock(ClientTransaction.class));

		DialogCollection dialogCollection = new DialogCollectionImpl(new InMemoryHousekeepingCollectionImpl<DialogInfo>());
		dialogCollection.add(dialogInfo);
		
		EventDispatcher eventDispatcher = new EventDispatcher();
		eventDispatcher.setTaskExecutor(new SimpleAsyncTaskExecutor());
		
		OutboundDialogSipBeanImpl bean = new OutboundDialogSipBeanImpl();
		bean.setDialogCollection(dialogCollection);
		bean.setEventDispatcher(eventDispatcher);
		
		Response response = EasyMock.createMock(Response.class);
		EasyMock.expect(response.getStatusCode()).andStubReturn(Response.SERVER_INTERNAL_ERROR);
		EasyMock.replay(response);
		
		ResponseEvent re = EasyMock.createMock(ResponseEvent.class);
		EasyMock.expect(re.getResponse()).andStubReturn(response);
		EasyMock.replay(re);
		
		// act
		bean.processInitialInviteResponse(re, dialogInfo.getId());
		
		// assert
		assertNull(dialogCollection.get(dialogInfo.getId()).getInviteClientTransaction());
	}
}
