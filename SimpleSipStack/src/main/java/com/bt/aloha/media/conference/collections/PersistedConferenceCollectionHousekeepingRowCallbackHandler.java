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

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.bt.aloha.media.conference.state.ConferenceInfo;
import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateManager;
import com.bt.aloha.util.HousekeeperAware;

// class to prepare conferences for house keeping

public class PersistedConferenceCollectionHousekeepingRowCallbackHandler implements RowCallbackHandler, ApplicationContextAware {
    private Log log = LogFactory.getLog(this.getClass());
    private ApplicationContext applicationContext;
    private ConferenceCollection conferenceCollection;
    private ConcurrentUpdateManager concurrentUpdateManager;

    public PersistedConferenceCollectionHousekeepingRowCallbackHandler() {}

	public void processRow(ResultSet resultSet) throws SQLException {
    	String conferenceId = null;
		try {
    		conferenceId = resultSet.getString("conferenceId");
        	String beanName = resultSet.getString("simpleSipBeanId");
        	log.info(String.format("Housekeeping: preparing call %s for being housekept by %s", conferenceId, beanName));
        	HousekeeperAware creatorBean = (HousekeeperAware) applicationContext.getBean(beanName);
    		creatorBean.killHousekeeperCandidate(conferenceId);
    	} catch (Throwable t) {
    		log.error(String.format("Unable to kill housekeeper candidate %s...will still remove from collection next housekeep", conferenceId), t);
    	} finally {
    		final String localConferenceId = conferenceId;
    		//force housekeeping next time for that object
    		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
				public void execute() {
					if (null == localConferenceId) return;
					log.info(String.format("Housekeeping: setting housekeepForced flag to true in info %s", localConferenceId));
					ConferenceInfo conferenceInfo = conferenceCollection.get(localConferenceId);
					conferenceInfo.setHousekeepForced(true);
					conferenceCollection.replace(conferenceInfo);
				}
				public String getResourceId() {
					return localConferenceId;
				}
			};
			concurrentUpdateManager.executeConcurrentUpdate(concurrentUpdateBlock);
    	}
	}

	public void setApplicationContext(ApplicationContext anApplicationContext) {
		this.applicationContext = anApplicationContext;
	}

	public void setConferenceCollection(ConferenceCollection aConferenceCollection) {
		this.conferenceCollection = aConferenceCollection;
	}

	public void setConcurrentUpdateManager(ConcurrentUpdateManager aConcurrentUpdateManager) {
		this.concurrentUpdateManager = aConcurrentUpdateManager;
	}
}
