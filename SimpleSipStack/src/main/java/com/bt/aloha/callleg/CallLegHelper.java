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

import javax.sdp.MediaDescription;
import javax.sip.message.Request;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.dialog.DialogBeanHelper;
import com.bt.aloha.dialog.DialogConcurrentUpdateBlock;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.dialog.state.TerminationCause;
import com.bt.aloha.dialog.state.TerminationMethod;
import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateManager;

public abstract class CallLegHelper extends DialogBeanHelper {
    private static final Log LOG = LogFactory.getLog(CallLegHelper.class);

    public CallLegHelper() {
        super();
    }

       protected abstract DialogCollection getDialogCollection();
    protected abstract ConcurrentUpdateManager getConcurrentUpdateManager();
    protected abstract void endNonConfirmedDialog(ReadOnlyDialogInfo dialogInfo, TerminationMethod previousTerminationMethod);
    protected abstract void acceptReceivedMediaOffer(final String dialogId, final MediaDescription mediaDescription, boolean offerInOkResponse, final boolean initialInviteTransactionCompleted);

    protected DialogBeanHelper getDialogBeanHelper() {
        return this;
    }

    public void terminateCallLeg(final String dialogId, final TerminationCause aDialogTerminationCause) {
        LOG.info(String.format("Terminating dialog %s with termination cause %s", dialogId, aDialogTerminationCause));
        ConcurrentUpdateBlock concurrentUpdateBlock = new DialogConcurrentUpdateBlock(getDialogBeanHelper()) {
            public void execute() {
                DialogInfo dialogInfo = getDialogCollection().get(dialogId);
                if(dialogInfo==null){
                    LOG.warn(String.format("Attempt to terminate dialog '%s' failed, as object not in collection", dialogId));
                    return;
                }
                DialogState dialogState = dialogInfo.getDialogState();
                dialogInfo.setTerminationCause(aDialogTerminationCause);
                if (DialogState.Created.equals(dialogState)) {
                    dialogInfo.setDialogState(DialogState.Terminated);
                    getDialogCollection().replace(dialogInfo);
                    LOG.info(String.format("Dialog %s terminated from state Created, no messages sent for this dialog", dialogId));
                    return;
                }

                TerminationMethod previousTerminationMethod = dialogInfo.setTerminationMethod(TerminationMethod.Terminate);
                if (previousTerminationMethod == null) {
                    LOG.info(String.format("Failed to terminate dialog %s - already cancelling or terminating", dialogInfo.getId()));
                    return;
                }

                if (DialogState.Confirmed.equals(dialogState)) {
                    assignSequenceNumber(dialogInfo, Request.BYE);
                    Request byeRequest = getDialogBeanHelper().createByeRequest(dialogInfo);
                    getDialogCollection().replace(dialogInfo);
                    getDialogBeanHelper().sendRequest(byeRequest);
                } else if (DialogState.Confirmed.ordinal() > dialogState.ordinal()) {
                    getDialogCollection().replace(dialogInfo);
                    endNonConfirmedDialog(dialogInfo, previousTerminationMethod);
                }
            }

            public String getResourceId() {
                return dialogId;
            }
        };
        getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
    }

    public CallLegInformation getCallLegInformation(String callLegId) {
        if(callLegId == null)
            throw new IllegalArgumentException("Call leg identifier must be specified");

        ReadOnlyDialogInfo dialogInfo = getDialogCollection().get(callLegId);
        if(dialogInfo == null)
            throw new IllegalArgumentException(String.format("Unknown call leg identifier %s", callLegId));

        return new CallLegInformation(dialogInfo);
    }
}
