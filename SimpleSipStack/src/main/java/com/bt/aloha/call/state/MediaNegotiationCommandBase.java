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

 	

 	
 	
 
package com.bt.aloha.call.state;

import javax.sdp.MediaDescription;

import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.callleg.InboundCallLegBean;
import com.bt.aloha.callleg.OutboundCallLegBean;
import com.bt.aloha.dialog.state.ImmutableDialogInfo;
import com.bt.aloha.dialog.state.TerminationCause;

public abstract class MediaNegotiationCommandBase implements MediaNegotiationCommand {
	private ImmutableDialogInfo dialogInfo;
	private CallLegConnectionState callLegConnectionState;
	private OutboundCallLegBean outboundCallLegBean;
	private InboundCallLegBean inboundCallLegBean;
	
	public MediaNegotiationCommandBase(ImmutableDialogInfo aDialogInfo, CallLegConnectionState aCallLegConnectionState) {
		this.dialogInfo = aDialogInfo;
		this.callLegConnectionState = aCallLegConnectionState;
		this.outboundCallLegBean = null;
		this.inboundCallLegBean = null;
	}
	
	protected CallLegConnectionState getCallLegConnectionState() {
		return callLegConnectionState;
	}
	
	protected ImmutableDialogInfo getDialogInfo() {
		return dialogInfo;
	}

	protected InboundCallLegBean getInboundCallLegBean() {
		return inboundCallLegBean;
	}

	protected OutboundCallLegBean getOutboundCallLegBean() {
		return outboundCallLegBean;
	}
	
	public void setInboundCallLegBean(InboundCallLegBean aInboundCallLegBean) {
		this.inboundCallLegBean = aInboundCallLegBean;
	}
	
	public void setOutboundCallLegBean(OutboundCallLegBean aOutboundCallLegBean) {
		this.outboundCallLegBean = aOutboundCallLegBean;
	}
	
	protected void reinviteCallLeg(final ImmutableDialogInfo aDialogInfo, MediaDescription offerMediaDescription, AutoTerminateAction autoTerminateDialog, final String callId) {
		if (dialogInfo.isInbound())
			inboundCallLegBean.reinviteCallLeg(aDialogInfo.getId(), offerMediaDescription, autoTerminateDialog, callId);
		else
			outboundCallLegBean.reinviteCallLeg(aDialogInfo.getId(), offerMediaDescription, autoTerminateDialog, callId);
	}

	protected void terminateCallLeg(final ImmutableDialogInfo aDialogInfo, final TerminationCause dialogTerminationCause) {
		if (dialogInfo.isInbound())
			inboundCallLegBean.terminateCallLeg(aDialogInfo.getId(), dialogTerminationCause);
		else
			outboundCallLegBean.terminateCallLeg(aDialogInfo.getId(), dialogTerminationCause);
	}
}
