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
package com.bt.aloha.dialog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.header.CSeqHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.collections.memory.InMemoryHousekeepingCollectionImpl;
import com.bt.aloha.dialog.DialogSipBeanBase;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.collections.DialogCollectionImpl;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.dialog.state.ReinviteInProgress;
import com.bt.aloha.dialog.state.TerminationMethod;


public class DialogSipBeanTest {
	private DialogCollection dialogCollection;
	
	@Before
	public void before() {
		dialogCollection = new DialogCollectionImpl(new InMemoryHousekeepingCollectionImpl<DialogInfo>());
	}

	// test that executing reinviteDialog for a dialogId not in the collection is handled gracefully and no NPE is thrown
	@Test
	public void testReinviteDialogNonExistentDialogId() throws Exception {
		DialogSipBeanBase dialogSipBeanBase = new DialogSipBeanBase() {
			@Override
			protected void endNonConfirmedDialog(ReadOnlyDialogInfo readOnlyDialogInfo, TerminationMethod previousTerminationMethod) {
			}
		};
		dialogSipBeanBase.setDialogCollection(dialogCollection);
		try {
			dialogSipBeanBase.reinviteDialog("some id", null, null, null);
		} catch(NullPointerException e) {
			fail("Unexpected exception");
		}
	}
	
	/**
	 * Testing that reinvites received before state is early are discarded
	 */
	@Test
	public void ensureReinvitesReceivedTooEarlyAreDiscarded() {
		// setup
		DialogInfo dialog = new DialogInfo("id", "beanid", "1.2.3.4");
		dialog.setDialogState(DialogState.Initiated);
		DialogCollection dialogCollection = new DialogCollectionImpl(new InMemoryHousekeepingCollectionImpl<DialogInfo>());
		dialogCollection.add(dialog);
		DialogSipBeanBase dialogBean = new DialogSipBeanBase() {

			@Override
			protected void endNonConfirmedDialog(ReadOnlyDialogInfo readOnlyDialogInfo, TerminationMethod previousTerminationMethod) {
			}};
		Request req = EasyMock.createNiceMock(Request.class);
		ServerTransaction trans = EasyMock.createNiceMock(ServerTransaction.class);
		EasyMock.replay(trans);
		EasyMock.replay(req);
		dialogBean.setDialogCollection(dialogCollection);

		// act
		dialogBean.processReinvite(req, trans, dialog.getId());

		// assert
		EasyMock.verify(req);
	}

	/**
	 * Tests that when the dialog helper returns false we throw illegal state exception
	 */
	@Test(expected=IllegalStateException.class)
	public void throwIllegalStateExceptionWhenCannotSendRequest() {
		// setup
		DialogSipBeanBase dialogBean = new DialogSipBeanBase() {
			@Override
			protected void endNonConfirmedDialog(ReadOnlyDialogInfo readOnlyDialogInfo, TerminationMethod previousTerminationMethod) {
			}
		};
		
		// act
		dialogBean.replaceDialogIfCanSendRequest("INVITE", DialogState.Terminated, TerminationMethod.Terminate, new DialogInfo("ad", "bean", "1.2.3.4"));
	}

	/**
	 * Tests that the dialog listener collection isn't null
	 */
	@Test
	public void dialogListenerCollectionIsNotNull() {
		// act
		DialogSipBeanBase bean = new DialogSipBeanBase() {
			@Override
			protected void endNonConfirmedDialog(ReadOnlyDialogInfo readOnlyDialogInfo, TerminationMethod previousTerminationMethod) {
			}
		};

		// assert
		assertNotNull(bean.getDialogListeners());
	}

	// test that a duplicate ok response to a reinvite returns making no changes to dialogInfo
	@Test
	public void duplicateOkResponseToReinvite() throws Exception {
		// setup
		DialogInfo dialogInfo = new DialogInfo("id", "beanid", "1.2.3.4");
		String dialogId = dialogInfo.getId();
		dialogInfo.setDialogState(DialogState.Confirmed);
		dialogInfo.setTerminationMethod(TerminationMethod.None);
		dialogInfo.setReinviteInProgess(ReinviteInProgress.None);
		dialogInfo.setLastReceivedOkSequenceNumber(1);
		
		dialogCollection.add(dialogInfo);
		DialogSipBeanBase bean = new DialogSipBeanBase() {
			@Override
			protected void endNonConfirmedDialog(ReadOnlyDialogInfo readOnlyDialogInfo, TerminationMethod previousTerminationMethod) {
			}
		};
		bean.setDialogCollection(dialogCollection);
		
		CSeqHeader cseqHeader = EasyMock.createMock(CSeqHeader.class);
		EasyMock.expect(cseqHeader.getSeqNumber()).andStubReturn(1L);
		EasyMock.replay(cseqHeader);
		
		Response response = EasyMock.createMock(Response.class);
		EasyMock.expect(response.getHeader(CSeqHeader.NAME)).andStubReturn(cseqHeader);
		EasyMock.replay(response);
		
		ResponseEvent responseEvent = EasyMock.createMock(ResponseEvent.class);
		EasyMock.expect(responseEvent.getResponse()).andStubReturn(response);
		EasyMock.replay(responseEvent);

		// act
		bean.processReinviteOkResponse(responseEvent, dialogId);

		// assert
		assertEquals(dialogInfo.getVersionId(), dialogCollection.get(dialogId).getVersionId());
	}
	
	// test that a request to send a reinvite response fails if null is passed for sdp
	@Test(expected=IllegalArgumentException.class)
	public void testReinviteOkResponseWithNullSdpException() throws Exception {
		// setup
		DialogSipBeanBase bean = new DialogSipBeanBase() {
			@Override
			protected void endNonConfirmedDialog(ReadOnlyDialogInfo readOnlyDialogInfo, TerminationMethod previousTerminationMethod) {
			}
		};
		
		// act
		bean.sendReinviteOkResponse("whatever", null);
	}
	
	// test that you cannot add a null listener
	@Test(expected=IllegalArgumentException.class)
	public void addNullListener() {
		// setup
		DialogSipBeanBase bean = new DialogSipBeanBase() {
			@Override
			protected void endNonConfirmedDialog(ReadOnlyDialogInfo readOnlyDialogInfo, TerminationMethod previousTerminationMethod) {
			}
		};
		
		// act
		bean.addDialogSipListener(null);
		
		// assert - exception
	}
	
	// test that you cannot remove a null listener
	@Test(expected=IllegalArgumentException.class)
	public void removeNullListener() {
		// setup
		DialogSipBeanBase bean = new DialogSipBeanBase() {
			@Override
			protected void endNonConfirmedDialog(ReadOnlyDialogInfo readOnlyDialogInfo, TerminationMethod previousTerminationMethod) {
			}
		};
		
		// act
		bean.removeDialogSipListener(null);
		
		// assert - exception
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void cannotSendReinviteOkResponseWithoutMediaDescription() { 
		// setup
		DialogSipBeanBase bean = new DialogSipBeanBase() {
			@Override
			protected void endNonConfirmedDialog(ReadOnlyDialogInfo readOnlyDialogInfo, TerminationMethod previousTerminationMethod) {
			}
		};
		
		// act
		bean.sendReinviteOkResponse("aaa", null);
	}
}
