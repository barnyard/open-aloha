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

 	

 	
 	
 
package com.bt.aloha.media.conference.collections;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.dao.ConferenceInfoDao;
import com.bt.aloha.media.conference.state.ConferenceInfo;
import com.bt.aloha.util.CollectionHelper;

public class PersistedConferenceCollectionImpl implements ConferenceCollection {

    private ConferenceInfoDao conferenceInfoDao;
	private PersistedConferenceCollectionHousekeepingRowCallbackHandler rowCallbackHandler;
	private long maxTimeToLive;
	private ConcurrentMap<String, Map<String, Object>> transients = new ConcurrentHashMap<String, Map<String, Object>>();
	private Log log = LogFactory.getLog(this.getClass());

    public PersistedConferenceCollectionImpl(ConferenceInfoDao aConferenceInfoDao) {
        this.conferenceInfoDao = aConferenceInfoDao;
    }

    private ConferenceInfo readTransients(ConferenceInfo result) {
    	if (null == result) return null;
    	if (transients.containsKey(result.getId()))
    		result.setTransients(transients.get(result.getId()));
    	return result;
    }

    public ConferenceInfo getCurrentConferenceForCall(String callId) {
    	log.debug(String.format("getCurrentConferenceForCall %s", callId));
    	ConferenceInfo result = conferenceInfoDao.findConferenceForCallId(callId);
        return readTransients(result);
    }

    public void add(ConferenceInfo conferenceInfo) {
    	if (null == conferenceInfo)
    		throw new IllegalArgumentException("ConferenceInfo cannot be null");
    	transients.put(conferenceInfo.getId(), conferenceInfo.getTransients());
    	log.debug(String.format("Put %s into transient collection", conferenceInfo.getId()));
        conferenceInfoDao.create(conferenceInfo);
    }

    public void destroy() {
    	CollectionHelper.destroy(transients, this.getClass().getSimpleName());
    }

    public ConferenceInfo get(String id) {
    	log.debug(String.format("getting conference id %s ", id));
        ConferenceInfo result = conferenceInfoDao.read(id);
        return readTransients(result);
    }

    public ConcurrentMap<String, ConferenceInfo> getAll() {
    	ConcurrentMap<String, ConferenceInfo> results = conferenceInfoDao.getAll();
    	for (ConferenceInfo value: results.values()){
    		readTransients(value);
    	}
        return results;
    }

    public void init() {
    	throw new UnsupportedOperationException();
    }

    public void remove(String id) {
    	transients.remove(id);
        conferenceInfoDao.delete(id);
    }

    public void replace(ConferenceInfo conferenceInfo) {
    	transients.put(conferenceInfo.getId(), conferenceInfo.getTransients());
        conferenceInfoDao.update(conferenceInfo);
    }

    public int size() {
        return conferenceInfoDao.size();
    }

    public void housekeep() {
    	List<String> transientsToBeCleaned = conferenceInfoDao.findByHousekeeperFlags(Calendar.getInstance().getTimeInMillis() - maxTimeToLive);
    	conferenceInfoDao.deleteByHousekeeperFlags(Calendar.getInstance().getTimeInMillis() - maxTimeToLive);
    	for(String callId: transientsToBeCleaned){
    		transients.remove(callId);
    	}
    	conferenceInfoDao.updateByHousekeeperFlags(Calendar.getInstance().getTimeInMillis() - maxTimeToLive, this.rowCallbackHandler);
    }

    public void setMaxTimeToLive(long aMaxTimeToLive) {
        this.maxTimeToLive = aMaxTimeToLive;
    }

	public void setPersistedConferenceCollectionHousekeepingRowCallbackHandler(PersistedConferenceCollectionHousekeepingRowCallbackHandler aRowCallbackHandler) {
		this.rowCallbackHandler = aRowCallbackHandler;
		this.rowCallbackHandler.setConferenceCollection(this);
	}

    public ConcurrentMap<String, ConferenceInfo> getAllActiveConferencesWithMaxDuration() {
    	ConcurrentMap<String, ConferenceInfo> results = conferenceInfoDao.findConnectedMaxDurationConferences();
    	for (ConferenceInfo value: results.values()){
    		readTransients(value);
    	}
        return results;
    }

	protected ConcurrentMap<String, Map<String, Object>> getTransients() {
		return transients;
	}
}
