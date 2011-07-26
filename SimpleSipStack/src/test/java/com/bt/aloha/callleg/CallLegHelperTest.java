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

 	

 	
 	
 
package com.bt.aloha.callleg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.sdp.MediaDescription;

import org.junit.Test;

import com.bt.aloha.callleg.CallLegHelper;
import com.bt.aloha.callleg.CallLegInformation;
import com.bt.aloha.collections.memory.InMemoryHousekeepingCollectionImpl;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.collections.DialogCollectionImpl;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.dialog.state.TerminationCause;
import com.bt.aloha.dialog.state.TerminationMethod;
import com.bt.aloha.media.MediaDialogInfo;
import com.bt.aloha.util.ConcurrentUpdateManager;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;

public class CallLegHelperTest {
	private DialogCollection dialogCollection = new DialogCollectionImpl(new InMemoryHousekeepingCollectionImpl<DialogInfo>());
	private CallLegHelper callLegHelper = new CallLegHelper() {
		@Override
		protected void endNonConfirmedDialog(ReadOnlyDialogInfo dialogInfo, TerminationMethod previousTerminationMethod) {
		}

		@Override
		protected ConcurrentUpdateManager getConcurrentUpdateManager() {
			return new ConcurrentUpdateManagerImpl();
		}

		@Override
		protected DialogCollection getDialogCollection() {
			return CallLegHelperTest.this.dialogCollection;
		}

		@Override
		protected void acceptReceivedMediaOffer(String dialogId, MediaDescription mediaDescription, boolean offerInOkResponse, boolean initialInviteTransactionCompleted) {
		}
	};

	@Test
	public void testGetCallInformationAllFieldsPresent() throws Exception {
		// setup
		DialogInfo dialogInfo = new DialogInfo("a", "b", "1.2.3.4");
		dialogInfo.setDialogState(DialogState.Terminated);
		dialogInfo.setTerminationCause(TerminationCause.AutoTerminated);
		dialogCollection.add(dialogInfo);

		// act
		CallLegInformation callLegInformation = callLegHelper.getCallLegInformation(dialogInfo.getId());

		// assert
		assertEquals(DialogState.Terminated, callLegInformation.getState());
		assertEquals(TerminationCause.AutoTerminated, callLegInformation.getTerminationCause());
		assertEquals(0, callLegInformation.getDuration());
		assertFalse(callLegInformation.isMediaCallLeg());
	}

	// test call leg info retrieval null dialog
	@Test(expected=IllegalArgumentException.class)
	public void testGetCallInfoNullDialogId() throws Exception {
		// act
		callLegHelper.getCallLegInformation(null);
	}

	// test info retrieval unknown dialog
	@Test(expected=IllegalArgumentException.class)
	public void testGetDialogStateUnknownDialogId() throws Exception {
		// act
		callLegHelper.getCallLegInformation("unknown");
	}

	// test that we expose teh termination cause ONLY after status set to terminated
	@Test
	public void testGetCallInformationTerminationCauseNullBeforeTerminated() throws Exception {
		// setup
		DialogInfo dialogInfo = new DialogInfo("a", "b", "1.2.3.4");
		dialogInfo.setDialogState(DialogState.Confirmed);
		dialogInfo.setTerminationCause(TerminationCause.CallAnswerTimeout);
		dialogCollection.add(dialogInfo);

		// act
		CallLegInformation callLegInformation = callLegHelper.getCallLegInformation(dialogInfo.getId());

		// assert
		assertEquals(DialogState.Confirmed, callLegInformation.getState());
		assertEquals(null, callLegInformation.getTerminationCause());
	}
	

	/**
	 * This tests that we don't throw an exception if the call leg is already terminated
	 */
	@Test
	public void terminateCallLegDoesNotThrowExceptionIfCallAlreadyTerminated() {
		// setup
		DialogInfo dialogInfo = new DialogInfo("a", "b", "1.2.3.4");
		dialogInfo.setDialogState(DialogState.Terminated);
		dialogInfo.setTerminationCause(TerminationCause.RemotePartyBusy);
		dialogCollection.add(dialogInfo);

		//act
		callLegHelper.terminateCallLeg(dialogInfo.getId(), TerminationCause.TerminatedByServer);
		
		// assert
		// no exception!
	}
	
	// test that the isMediaCallLeg flag works OK
	@Test
	public void testGetCallInformationForMediaCallLeg() {
		// setup
		DialogInfo dialogInfo = new MediaDialogInfo("a", "b", "1.2.3.4");
		dialogInfo.setDialogState(DialogState.Terminated);
		dialogInfo.setTerminationCause(TerminationCause.AutoTerminated);
		dialogCollection.add(dialogInfo);

		// act
		CallLegInformation callLegInformation = callLegHelper.getCallLegInformation(dialogInfo.getId());

		// assert
		assertTrue(callLegInformation.isMediaCallLeg());
	}
}
