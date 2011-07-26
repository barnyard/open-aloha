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

 	

 	
 	
 
package com.bt.aloha.testing;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.collections.memory.InMemoryHousekeepingCollectionImpl;
import com.bt.aloha.dialog.collections.DialogCollectionImpl;
import com.bt.aloha.dialog.state.DialogInfo;

public class AlternateConcurrentUpdateFailureDialogCollectionStub extends DialogCollectionImpl {
	private static final String ALTERNATE_FAILURE = "alternate.failure";
	private static final Log LOG = LogFactory.getLog(AlternateConcurrentUpdateFailureDialogCollectionStub.class);

    public AlternateConcurrentUpdateFailureDialogCollectionStub() {
        super(new InMemoryHousekeepingCollectionImpl<DialogInfo>());
    }

	@Override
	public void replace(DialogInfo dialogInfo) {
		if(dialogInfo.getStringProperty(ALTERNATE_FAILURE, null) == null) {
			DialogInfo dummyDialogInfo = get(dialogInfo.getId());
			Properties props = new Properties();
			props.put(ALTERNATE_FAILURE, "true");
			dummyDialogInfo.setProperties(props);
			LOG.debug(String.format("Replacing dialoginfo %s (version %s, hash %s) with dummy object (version %s, object %s)",
					dialogInfo.getId(), dialogInfo.getVersionId(), dialogInfo.hashCode(), dummyDialogInfo.getVersionId(), dummyDialogInfo.hashCode()));
			super.replace(dummyDialogInfo);
			LOG.debug(String.format("Replaced dialoginfo %s (version %s, hash %s) with dummy object (version %s, object %s)",
					dialogInfo.getId(), dialogInfo.getVersionId(), dialogInfo.hashCode(), dummyDialogInfo.getVersionId(), dummyDialogInfo.hashCode()));
		} else {
			dialogInfo.setProperties(null);
		}

		LOG.debug(String.format("Replacing dialoginfo %s (version %s, hash %s)", dialogInfo.getId(), dialogInfo.getVersionId(), dialogInfo.hashCode()));
		super.replace(dialogInfo);
	}
}
