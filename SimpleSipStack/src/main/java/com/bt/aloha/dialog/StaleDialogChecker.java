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

import java.util.concurrent.ConcurrentMap;

import javax.sip.message.Request;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.stack.SimpleSipStack;
import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateManager;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;

import org.springframework.core.task.TaskExecutor;

public class StaleDialogChecker {

	private DialogCollection dialogCollection;
	private DialogBeanHelper dialogBeanHelper = new DialogBeanHelper();
	private Log log = LogFactory.getLog(this.getClass());
    private ConcurrentUpdateManager concurrentUpdateManager = new ConcurrentUpdateManagerImpl();
	private TaskExecutor taskExecutor;

	public StaleDialogChecker(){}

	public void runTask() {
		log.debug("runTask()");
		this.taskExecutor.execute(new Runnable() {
			public void run() {
				initialize();
			}
		});
	}
	
	protected void initialize() {
		log.debug("initialize()");
		ConcurrentMap<String, DialogInfo> allDialogs = dialogCollection.getAll();
		log.debug(String.format("Initialising StaleDialogChecker with %s dialogs", allDialogs.size()));
		for (ReadOnlyDialogInfo dialogInfo : allDialogs.values())
			if (dialogInfo.getDialogState().equals(DialogState.Confirmed))
				pingCallLeg(dialogInfo);
	}

	private void pingCallLeg(final ReadOnlyDialogInfo readOnlyDialogInfo) {
        ConcurrentUpdateBlock concurrentUpdateBlock = new DialogConcurrentUpdateBlock(dialogBeanHelper) {

            public void execute() {
                DialogInfo dialogInfo = dialogCollection.get(readOnlyDialogInfo.getId());
                assignSequenceNumber(dialogInfo, Request.INFO);
                Request infoRequest = dialogBeanHelper.createInfoRequest(dialogInfo);
                dialogCollection.replace(dialogInfo);
                dialogBeanHelper.sendRequest(infoRequest);
            }

            public String getResourceId() {
                return readOnlyDialogInfo.getId();
            }
        };
        concurrentUpdateManager.executeConcurrentUpdate(concurrentUpdateBlock);
    }

	public void setDialogCollection(DialogCollection aDialogCollection) {
		this.dialogCollection = aDialogCollection;
	}

	public void setSimpleSipStack(SimpleSipStack aSimpleSipStack) {
		this.dialogBeanHelper.setSimpleSipStack(aSimpleSipStack);
	}

	protected void setDialogBeanHelper(DialogBeanHelper aDialogBeanHelper){
		this.dialogBeanHelper = aDialogBeanHelper;
	}

	public void setTaskExecutor(TaskExecutor aTaskExecutor) {
		this.taskExecutor = aTaskExecutor;
	}
}
