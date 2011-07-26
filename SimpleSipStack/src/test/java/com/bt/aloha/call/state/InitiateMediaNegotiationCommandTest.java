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
package com.bt.aloha.call.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.easymock.EasyMock;
import org.junit.Test;

import com.bt.aloha.call.state.CallInfo;
import com.bt.aloha.call.state.CallLegConnectionState;
import com.bt.aloha.call.state.InitiateMediaNegotiationCommand;
import com.bt.aloha.call.state.MediaNegotiationMethod;
import com.bt.aloha.call.state.MediaNegotiationState;
import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.callleg.OutboundCallLegBean;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.ImmutableDialogInfo;

public class InitiateMediaNegotiationCommandTest {
	/**
	 * Update call info where calllegconnectionstate is completed
	 */
	@Test
	public void updateCallInfoCompleted() {
		// setup
		DialogInfo info = new DialogInfo("dialogId1", "aBeanId", "111.111.11.11");
		InitiateMediaNegotiationCommand cmd = new InitiateMediaNegotiationCommand(info, AutoTerminateAction.False, "callId", CallLegConnectionState.Completed);
		CallInfo callInfo = new CallInfo("creatingBeanId", "callId", "dialogId1", "dialogId2", AutoTerminateAction.False, 100);
				
		// act
		cmd.updateCallInfo(callInfo);
				
		// assert
		assertEquals(MediaNegotiationState.Initiated, callInfo.getMediaNegotiationState());
		assertEquals(MediaNegotiationMethod.ReinviteRequest, callInfo.getMediaNegotiationMethod());
	}

	/**
	 * Update call info where calllegconnectionstate is pending and outbound
	 */
	@Test
	public void updateCallInfoPendingOutbound() {
		// setup
		DialogInfo info = new DialogInfo("dialogId1", "aBeanId", "111.111.11.11");
		InitiateMediaNegotiationCommand cmd = new InitiateMediaNegotiationCommand(info, AutoTerminateAction.False, "callId", CallLegConnectionState.Pending);
		CallInfo callInfo = new CallInfo("creatingBeanId", "callId", "dialogId1", "dialogId2", AutoTerminateAction.False, 100);
				
		// act
		cmd.updateCallInfo(callInfo);
				
		// assert
		assertEquals(MediaNegotiationState.Initiated, callInfo.getMediaNegotiationState());
		assertEquals(MediaNegotiationMethod.InitialInviteRequest, callInfo.getMediaNegotiationMethod());
	}

	/**
	 * Update call info where calllegconnectionstate is pending and inbound
	 */
	@Test
	public void updateCallInfoPendingInbound() {
		// setup
		ImmutableDialogInfo dialogInfo = new ImmutableDialogInfo() {

			public long getInitialInviteTransactionSequenceNumber() {
				return 0;
			}

			public boolean isInbound() {
				return true;
			}

			public boolean isSdpInInitialInvite() {
				return false;
			}

			public String getSimpleSipBeanId() {
				return "aBeanId";
			}

			public String getId() {
				return "dialogId";
			}

			public boolean isAutomaton() {
				return false;
			}
			
		};
		InitiateMediaNegotiationCommand cmd = new InitiateMediaNegotiationCommand(dialogInfo, AutoTerminateAction.False, "callId", CallLegConnectionState.Pending);
		CallInfo callInfo = new CallInfo("creatingBeanId", "callId", "dialogId1", "dialogId2", AutoTerminateAction.False, 100);
				
		// act
		cmd.updateCallInfo(callInfo);
				
		// assert
		assertEquals(MediaNegotiationState.Initiated, callInfo.getMediaNegotiationState());
		assertEquals(MediaNegotiationMethod.InitialOkResponse, callInfo.getMediaNegotiationMethod());
	}
	
	/**
	 * Update call info where calllegconnectionstate is InProgres
	 */
	@Test
	public void updateCallInfoInProgress() {
		// setup
		DialogInfo info = new DialogInfo("dialogId1", "aBeanId", "111.111.11.11");
		InitiateMediaNegotiationCommand cmd = new InitiateMediaNegotiationCommand(info, AutoTerminateAction.False, "callId", CallLegConnectionState.InProgress);
		CallInfo callInfo = new CallInfo("creatingBeanId", "callId", "dialogId1", "dialogId2", AutoTerminateAction.False, 100);
				
		// act
		cmd.updateCallInfo(callInfo);
				
		// assert
		assertEquals(MediaNegotiationState.Initiated, callInfo.getMediaNegotiationState());
		assertNull(callInfo.getMediaNegotiationMethod());
	}
	
	/**
	 * execute completed call leg connection state shoudl reinvite
	 */
	@Test
	public void executeCompleted() {
		// setup
		OutboundCallLegBean outboundBean = EasyMock.createMock(OutboundCallLegBean.class);
		outboundBean.reinviteCallLeg("dialogId1", null, AutoTerminateAction.False, "callId");
		EasyMock.replay(outboundBean);
		DialogInfo info = new DialogInfo("dialogId1", "aBeanId", "111.111.11.11");
		InitiateMediaNegotiationCommand cmd = new InitiateMediaNegotiationCommand(info, AutoTerminateAction.False, "callId", CallLegConnectionState.Completed);
		cmd.setOutboundCallLegBean(outboundBean);
				
		// act
		cmd.execute();
				
		// assert
		EasyMock.verify(outboundBean);
	}

	/**
	 * execute pending call leg connection state shoudl connect
	 */
	@Test
	public void executePending() {
		// setup
		OutboundCallLegBean outboundBean = EasyMock.createMock(OutboundCallLegBean.class);
		outboundBean.connectCallLeg("dialogId1", AutoTerminateAction.False, "callId", null, false);
		EasyMock.replay(outboundBean);
		DialogInfo info = new DialogInfo("dialogId1", "aBeanId", "111.111.11.11");
		InitiateMediaNegotiationCommand cmd = new InitiateMediaNegotiationCommand(info, AutoTerminateAction.False, "callId", CallLegConnectionState.Pending);
		cmd.setOutboundCallLegBean(outboundBean);
				
		// act
		cmd.execute();
				
		// assert
		EasyMock.verify(outboundBean);
	}

	/**
	 * execute inprogress call leg connection state shoudl noop
	 */
	@Test
	public void executeInProgress() {
		// setup
		OutboundCallLegBean outboundBean = EasyMock.createMock(OutboundCallLegBean.class);
		EasyMock.replay(outboundBean);
		DialogInfo info = new DialogInfo("dialogId1", "aBeanId", "111.111.11.11");
		InitiateMediaNegotiationCommand cmd = new InitiateMediaNegotiationCommand(info, AutoTerminateAction.False, "callId", CallLegConnectionState.InProgress);
		cmd.setOutboundCallLegBean(outboundBean);
				
		// act
		cmd.execute();
				
		// assert
		EasyMock.verify(outboundBean);
	}
	
	/**
	 * execute inbound and not completed call leg connection state shoudl throw exception
	 */
	@Test(expected=IllegalStateException.class)
	public void executeInboundNotCompleted() {
		// setup
		ImmutableDialogInfo info = new ImmutableDialogInfo() {

			public long getInitialInviteTransactionSequenceNumber() {
				return 0;
			}

			public boolean isInbound() {
				return true;
			}

			public boolean isSdpInInitialInvite() {
				return false;
			}

			public String getSimpleSipBeanId() {
				return "aBeanId";
			}

			public String getId() {
				return "dialogId";
			}

			public boolean isAutomaton() {
				return false;
			}
			
		};
		InitiateMediaNegotiationCommand cmd = new InitiateMediaNegotiationCommand(info, AutoTerminateAction.False, "callId", CallLegConnectionState.InProgress);
				
		// act
		cmd.execute();
	}
}
