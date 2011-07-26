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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.media.conference.state.ConferenceInfo;
import com.bt.aloha.media.conference.state.ConferenceState;
import com.bt.aloha.util.HousekeeperOptimisticConcurrencyCollection;

public class ConferenceCollectionImpl implements ConferenceCollection {
	private static Log log = LogFactory.getLog(ConferenceCollectionImpl.class);
	private HousekeeperOptimisticConcurrencyCollection<ConferenceInfo> collection;

	public ConferenceCollectionImpl(HousekeeperOptimisticConcurrencyCollection<ConferenceInfo> aCollection) {
    	log.debug("Creating ConferenceCollection");
        this.collection = aCollection;
	}

    public ConferenceInfo getCurrentConferenceForCall(String callId) {
    	log.debug(String.format("Getting current conference from ConferenceCollection for call %s", callId));
        String mostRecentConferenceId = null;
        long mostRecentConferenceCreateTime = 0;

        ConcurrentMap<String, ConferenceInfo> allConferenceInfos = getAll();
        for (Map.Entry<String, ConferenceInfo> entry: allConferenceInfos.entrySet()) {
            ConferenceInfo conferenceInfo = entry.getValue();
            if (conferenceInfo.containsParticipant(callId)) {
				long createTime = conferenceInfo.getCreateTime();
                if (createTime > mostRecentConferenceCreateTime) {
                    mostRecentConferenceId = entry.getKey();
                    mostRecentConferenceCreateTime = createTime;
                }
            }
        }

        if (mostRecentConferenceId != null) {
            ConferenceInfo result = collection.get(mostRecentConferenceId);
            log.debug(String.format("Current conference for call %s is %s with sipUri %s",
                                    callId, mostRecentConferenceId, result.getConferenceSipUri()));
            return result;
        } else {
            log.debug("No conference for call " + callId);
            return null;
        }
    }

	public void add(ConferenceInfo conferenceInfo) {
    	log.debug(String.format("Adding conference %s to ConferenceCollection ", conferenceInfo.getId()));
		collection.add(conferenceInfo);
	}


	public ConferenceInfo get(String conferenceId) {
    	log.debug(String.format("Getting conference %s from ConferenceCollection", conferenceId));
		return collection.get(conferenceId);
	}

	public void remove(String conferenceId) {
		log.debug(String.format("Removing conference %s from ConferenceCollection", conferenceId));
		collection.remove(conferenceId);
	}

	public void replace(ConferenceInfo conferenceInfo) {
		String conferenceId = conferenceInfo != null ? conferenceInfo.getId(): "null";
		log.debug(String.format("Replacing conference %s in ConferenceCollection", conferenceId));
		collection.replace(conferenceInfo);
	}

	public void setMaxTimeToLive(long aConferenceMaxTimeToLive) {
		log.debug(String.format("Setting Max TTL in ConferenceCollection to %d", aConferenceMaxTimeToLive));
		collection.setMaxTimeToLive(aConferenceMaxTimeToLive);
	}

	public int size() {
		return collection.size();
	}

	public void destroy() {
		log.debug(String.format("Destroying ConferenceCollection"));
		collection.destroy();
	}

	public void init() {
		log.debug(String.format("Initialization of ConferenceCollection"));
		collection.init();
	}

	public void housekeep() {
		log.debug("HouseKeeping ConferenceCollection");
		collection.housekeep();
	}

	public ConcurrentMap<String, ConferenceInfo> getAll() {
		return collection.getAll();
	}

	public ConcurrentMap<String, ConferenceInfo> getAllActiveConferencesWithMaxDuration() {
        ConcurrentMap<String, ConferenceInfo> result = new ConcurrentHashMap<String, ConferenceInfo>();
	    for(Map.Entry<String, ConferenceInfo> entry: getAll().entrySet())
	        if (entry.getValue().getConferenceState().equals(ConferenceState.Active) && entry.getValue().getMaxDurationInMinutes() > 0)
	            result.put(entry.getKey(), entry.getValue());
	    return result;
	}
}
