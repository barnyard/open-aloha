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

 	

 	
 	
 
package com.bt.aloha.collections;

import java.util.Hashtable;
import java.util.concurrent.ConcurrentMap;

import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.state.DialogInfo;

public class DialogCollectionHashtableImpl implements DialogCollection {
    private static Hashtable<String, DialogInfo> dialogInfos = new Hashtable<String, DialogInfo>();

	public void add(DialogInfo dialogInfo) {
        dialogInfos.put(dialogInfo.getId(), dialogInfo);
	}

    public void replace(DialogInfo dialogInfo) {
        add(dialogInfo);
    }

	public DialogInfo get(String dialogId) {
        if (dialogInfos.containsKey(dialogId))
            return dialogInfos.get(dialogId);
        else
            return null;
	}

	public void remove(String dialogId) {
		dialogInfos.remove(dialogId);
	}

	public void setHousekeepingInterval(long arg0) {
	}

	public int size() {
        return dialogInfos.size();
	}

	public void init() {
	}

	public void destroy() {
	}

	public void housekeep() {
	}

	public ConcurrentMap<String, DialogInfo> getAll() {
		return null;
	}

	public void setMaxTimeToLive(long aMaxTimeToLive) {
	}
}
