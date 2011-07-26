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

 	

 	
 	
 
package com.bt.aloha.dialog;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.util.ConflictAwareConcurrentUpdateBlock;

public abstract class DialogConcurrentUpdateBlock implements ConflictAwareConcurrentUpdateBlock {
	private static final Log LOG = LogFactory.getLog(DialogConcurrentUpdateBlock.class);
	protected Long lastAssignedSequenceNumber;
	protected String lastRequestMethod;
	private DialogBeanHelper dialogBeanHelper;
	
	public DialogConcurrentUpdateBlock(DialogBeanHelper aDialogBeanHelper) {
		this.dialogBeanHelper = aDialogBeanHelper;
	}
	
	public void onConcurrentUpdateConflict() {
		if (lastAssignedSequenceNumber != null && lastRequestMethod != null) {
			LOG.debug(String.format("Concurrent block releasing seq num %d for req %s for dialog %s", lastAssignedSequenceNumber, lastRequestMethod, getResourceId()));
			dialogBeanHelper.dequeueRequest(getResourceId(), lastAssignedSequenceNumber, lastRequestMethod);
		}
		lastAssignedSequenceNumber = null;
		lastRequestMethod = null;
	}
	
	protected void assignSequenceNumber(DialogInfo dialogInfo, String requestMethod) {
		lastRequestMethod = requestMethod;
		lastAssignedSequenceNumber = dialogBeanHelper.enqueueRequestGetSequenceNumber(dialogInfo.getSipCallId(), dialogInfo.getSequenceNumber() + 1, requestMethod);		
		LOG.debug(String.format("Concurrent block assigning seq num %d for req %s for dialog %s", lastAssignedSequenceNumber, requestMethod, dialogInfo.getSipCallId()));
		dialogInfo.setSequenceNumber(lastAssignedSequenceNumber);
	}
	
	protected void forceSequenceNumber(String sipCallId, long sequenceNumber, String requestMethod) {
		lastRequestMethod = requestMethod;
		lastAssignedSequenceNumber = sequenceNumber;
		LOG.debug(String.format("Concurrent block forcing seq num %d for req %s for dialog %s", lastAssignedSequenceNumber, requestMethod, sipCallId));
		dialogBeanHelper.enqueueRequestForceSequenceNumber(sipCallId, sequenceNumber, requestMethod);
	}
}
