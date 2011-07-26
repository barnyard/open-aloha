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
package com.bt.aloha.callleg;



import java.util.ArrayList;
import java.util.List;

import javax.sdp.MediaDescription;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.dialog.DialogBeanHelper;
import com.bt.aloha.dialog.DialogSipListener;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.inbound.InboundDialogSipBeanImpl;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.dialog.state.TerminationCause;
import com.bt.aloha.dialog.state.TerminationMethod;
import com.bt.aloha.util.ConcurrentUpdateManager;
import com.bt.aloha.util.HousekeeperAware;

public class InboundCallLegBeanImpl extends InboundDialogSipBeanImpl implements InboundCallLegBean, HousekeeperAware  {
	private static final Log LOG = LogFactory.getLog(InboundCallLegBeanImpl.class);
	private CallLegHelper callLegHelper = new CallLegHelper() {
		@Override
		protected void endNonConfirmedDialog(ReadOnlyDialogInfo dialogInfo, TerminationMethod previousTerminationMethod) {
			InboundCallLegBeanImpl.this.endNonConfirmedDialog(dialogInfo, previousTerminationMethod);
		}

		@Override
		protected ConcurrentUpdateManager getConcurrentUpdateManager() {
			return InboundCallLegBeanImpl.this.getConcurrentUpdateManager();
		}

		@Override
		protected DialogCollection getDialogCollection() {
			return InboundCallLegBeanImpl.this.getDialogCollection();
		}
		
		@Override
		protected void acceptReceivedMediaOffer(String dialogId, MediaDescription mediaDescription, boolean offerInOkResponse, boolean initialInviteTransactionCompleted) {
			LOG.debug(String.format("Accepting media offer for call leg %s, offer in ok response is %s, initial tx completed is %s", dialogId, offerInOkResponse, initialInviteTransactionCompleted));
			if (initialInviteTransactionCompleted) { 
				if (offerInOkResponse)
					InboundCallLegBeanImpl.this.sendReinviteAck(dialogId, mediaDescription);
				else
					InboundCallLegBeanImpl.this.sendReinviteOkResponse(dialogId, mediaDescription);
			} else {
				InboundCallLegBeanImpl.this.sendInitialOkResponse(dialogId, mediaDescription, null);
			}
		}
	};

    public InboundCallLegBeanImpl() {
        super();
    }

   	@Override
	public DialogBeanHelper getDialogBeanHelper() {
   		return callLegHelper;
	}

	public void addInboundCallLegListener(InboundCallLegListener listener) {
		super.addInboundDialogListener(new InboundCallLegListenerAdapter(listener));
	}

	public void removeInboundCallLegListener(InboundCallLegListener listener) {
		super.removeInboundDialogListener(new InboundCallLegListenerAdapter(listener));
	}

	public void setInboundCallLegListeners(List<InboundCallLegListener> listeners) {
		List<DialogSipListener> dialogSipListeners = new ArrayList<DialogSipListener>();
		for (InboundCallLegListener listener : listeners) {
			dialogSipListeners.add(new InboundCallLegListenerAdapter(listener));
		}
		this.setDialogSipListeners(dialogSipListeners);
	}

	public void reinviteCallLeg(String dialogId, MediaDescription offerMediaDescription, AutoTerminateAction autoTerminate, String applicationData) {
		super.reinviteDialog(dialogId, offerMediaDescription, autoTerminate.getBoolean(), applicationData);
	}

	public boolean acceptIncomingCallLeg(String dialogId, MediaDescription mediaDescription, String applicationData) {
		return sendInitialOkResponse(dialogId, mediaDescription, applicationData);
	}

	public boolean rejectIncomingCallLeg(String dialogId, int statusCode) {
		return sendInitialErrorResponse(dialogId, statusCode);
	}

	public boolean sendRingingResponse(final String dialogId) {
		return super.sendRingingResponse(dialogId);
	}

	public void terminateCallLeg(String dialogId) {
		this.callLegHelper.terminateCallLeg(dialogId, TerminationCause.TerminatedByServer);
	}

	public void terminateCallLeg(String dialogId, TerminationCause aDialogTerminationCause) {
		this.callLegHelper.terminateCallLeg(dialogId, aDialogTerminationCause);
	}

	public CallLegInformation getCallLegInformation(String dialogId) {
		return this.callLegHelper.getCallLegInformation(dialogId);
	}

	public void killHousekeeperCandidate(String infoId) {
		terminateCallLeg(infoId, TerminationCause.Housekept);
	}

	public void acceptReceivedMediaOffer(String dialogId, MediaDescription mediaDescription, boolean offferInOkResponse, boolean initialInviteTransactionCompleted) {
		this.callLegHelper.acceptReceivedMediaOffer(dialogId, mediaDescription, offferInOkResponse, initialInviteTransactionCompleted);
	}
}
