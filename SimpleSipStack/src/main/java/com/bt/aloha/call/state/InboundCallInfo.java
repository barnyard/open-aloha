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

import com.bt.aloha.callleg.AutoTerminateAction;

public class InboundCallInfo extends CallInfo {
	private static final long serialVersionUID = -4606818640369969857L;

	public InboundCallInfo(String creatingBeanName, String aCallId, String theInboundDialogId, String theOutboundDialogId, AutoTerminateAction autoTerminate, long theMaxDurationInMinutes) {
		super(creatingBeanName, aCallId, theInboundDialogId, theOutboundDialogId, autoTerminate, theMaxDurationInMinutes);
	}
	
	public String getInboundCallLegId() {
		return getFirstDialogId();
	}
	
	public String getOutboundCallLegId() {
		return getSecondDialogId();
	}
	
	@Override
	public Class<? extends CallMessageFlow> getCallMessageFlow() {
		return InboundCallMessageFlowImpl.class;
	}
}
