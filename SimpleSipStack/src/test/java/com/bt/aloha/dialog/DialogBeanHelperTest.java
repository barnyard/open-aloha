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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.sip.message.Request;

import org.junit.Test;

import com.bt.aloha.dialog.DialogBeanHelper;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.TerminationMethod;

public class DialogBeanHelperTest {
	/**
	 * Testing that we can not send INVITE requests when TerminateMethod is Terminate
	 */
	@Test
	public void ensureNoInviteWhenTerminateMethodIsTerminate() {
		// setup
		DialogBeanHelper helper = new DialogBeanHelper();
		
		// act & assert
		assertFalse(helper.canSendRequest(Request.INVITE, DialogState.Confirmed, TerminationMethod.Terminate));
	}

	/**
	 * Testing that we can only send BYE requests when TerminateMethod is Terminate
	 */
	@Test
	public void ensureOnlyByeWhenTerminateMethodIsTerminate() {
		// setup
		DialogBeanHelper helper = new DialogBeanHelper();
		
		// act & assert
		assertTrue(helper.canSendRequest(Request.BYE, DialogState.Confirmed, TerminationMethod.Terminate));
	}

	/**
	 * Testing that we can not send BYE requests when TerminateMethod is Cancel
	 */
	@Test
	public void ensureNoByeWhenTerminateMethodIsCancel() {
		// setup
		DialogBeanHelper helper = new DialogBeanHelper();
		
		// act & assert
		assertFalse(helper.canSendRequest(Request.BYE, DialogState.Confirmed, TerminationMethod.Cancel));
	}
	
	/**
	 * Testing that we can send CANCEL requests when TerminateMethod is Cancel
	 */
	@Test
	public void ensureCancelWhenTerminateMethodIsCancel() {
		// setup
		DialogBeanHelper helper = new DialogBeanHelper();
		
		// act & assert
		assertTrue(helper.canSendRequest(Request.CANCEL, DialogState.Early, TerminationMethod.Cancel));
	}
	
	/**
	 * Testing that we can send ACK requests when TerminateMethod is Cancel
	 */
	@Test
	public void ensureAckWhenTerminateMethodIsCancel() {
		// setup
		DialogBeanHelper helper = new DialogBeanHelper();
		
		// act & assert
		assertTrue(helper.canSendRequest(Request.ACK, DialogState.Early, TerminationMethod.Cancel));
	}

	/**
	 * Testing that we can send CANCEL requests when TerminateMethod is Terminate
	 */
	@Test
	public void ensureCancelWhenTerminateMethodIsTerminate() {
		// setup
		DialogBeanHelper helper = new DialogBeanHelper();
		
		// act & assert
		assertTrue(helper.canSendRequest(Request.CANCEL, DialogState.Early, TerminationMethod.Terminate));
	}
	
	/**
	 * Testing that we can send ACK requests when TerminateMethod is Terminate
	 */
	@Test
	public void ensureAckWhenTerminateMethodIsTerminate() {
		// setup
		DialogBeanHelper helper = new DialogBeanHelper();
		
		// act & assert
		assertTrue(helper.canSendRequest(Request.ACK, DialogState.Early, TerminationMethod.Terminate));
	}
	

}
