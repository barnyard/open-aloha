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

 	

 	
 	
 
package com.bt.aloha.fitnesse.beans;

import javax.sip.ServerTransaction;
import javax.sip.message.Request;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.callleg.OutboundCallLegBeanImpl;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.ReinviteInProgress;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.util.ConcurrentUpdateBlock;

public class EmptyReinviteHandlingOutboundCallLegBeanImpl extends OutboundCallLegBeanImpl {
	private static final Log LOG = LogFactory.getLog(EmptyReinviteHandlingOutboundCallLegBeanImpl.class);

	@Override
	protected void processReinvite(final Request request, final ServerTransaction serverTransaction, final String dialogId) {
		if (request.getRawContent() != null) {
			super.processReinvite(request, serverTransaction, dialogId);
			return;
		}

		LOG.debug(String.format("Processing REINVITE request for Fitnesse dialog %s (%s)", dialogId, this.getClass().getSimpleName()));
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				DialogState dialogState = dialogInfo.getDialogState();
				if (dialogState.ordinal() < DialogState.Early.ordinal()) {
					LOG.warn(String.format("Throwing away reinvite for dialog %s, state is %s", dialogId, dialogInfo.getDialogState()));
					return;
				}
				updateDialogInfoFromInviteRequest(dialogInfo, request);
				dialogInfo.setInviteServerTransaction(serverTransaction);
                dialogInfo.setReinviteInProgess(ReinviteInProgress.ReceivedReinvite);
				getDialogCollection().replace(dialogInfo);

				sendReinviteOkResponse(dialogId, SessionDescriptionHelper.generateHoldMediaDescription(dialogInfo.getRemoteOfferMediaDescription()));
			}

			public String getResourceId() {
				return dialogId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}
}
