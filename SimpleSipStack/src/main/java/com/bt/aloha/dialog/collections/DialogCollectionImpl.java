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

 	

 	
 	
 
package com.bt.aloha.dialog.collections;

import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.util.HousekeeperOptimisticConcurrencyCollection;

public class DialogCollectionImpl implements DialogCollection {
    private static Log log = LogFactory.getLog(DialogCollectionImpl.class);

    private HousekeeperOptimisticConcurrencyCollection<DialogInfo> collection;

    public DialogCollectionImpl(HousekeeperOptimisticConcurrencyCollection<DialogInfo> aCollection) {
    	log.debug(String.format("Creating DialogCollection with %s", aCollection.getClass().getSimpleName()));
    	this.collection = aCollection;
    	log.debug("collection size:" + this.collection.size());
    }

	public void add(DialogInfo dialogInfo) {
		log.debug(String.format("Adding dialog %s to DialogCollection ", dialogInfo.getId()));
		collection.add(dialogInfo);
	}

	public void destroy() {
		log.debug(String.format("Destroying DialogCollection"));
		collection.destroy();
	}

	public DialogInfo get(String dialogId) {
		log.debug(String.format("Getting dialog %s from DialogCollection", dialogId));
		return collection.get(dialogId);
	}

	public void init() {
		log.debug(String.format("Initialization of DialogCollection"));
		collection.init();
	}

	public void remove(String dialogId) {
		log.debug(String.format("Removing dialog %s from DialogCollection", dialogId));
		collection.remove(dialogId);
	}

	public void replace(DialogInfo dialogInfo) {
		String dialogId = dialogInfo != null ? dialogInfo.getId(): "null";
		log.debug(String.format("Replacing dialog %s in DialogCollection", dialogId));
		collection.replace(dialogInfo);
	}

	public void setMaxTimeToLive(long aDialogMaxTimeToLive) {
		log.debug(String.format("Setting Max TTL in DialogCollection to %d", aDialogMaxTimeToLive));
		collection.setMaxTimeToLive(aDialogMaxTimeToLive);
	}

	public int size() {
		return collection.size();
	}

	public void housekeep() {
		log.debug("HouseKeeping DialogCollection");
		collection.housekeep();
	}

	public ConcurrentMap<String, DialogInfo> getAll() {
		return collection.getAll();
	}
}
