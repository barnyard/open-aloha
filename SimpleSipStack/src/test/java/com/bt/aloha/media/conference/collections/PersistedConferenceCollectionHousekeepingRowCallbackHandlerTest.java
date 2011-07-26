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

import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import com.bt.aloha.media.conference.collections.ConferenceCollection;
import com.bt.aloha.media.conference.collections.PersistedConferenceCollectionHousekeepingRowCallbackHandler;
import com.bt.aloha.media.conference.state.ConferenceInfo;
import com.bt.aloha.util.ConcurrentUpdateManager;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;
import com.bt.aloha.util.HousekeeperAware;

public class PersistedConferenceCollectionHousekeepingRowCallbackHandlerTest {

	private PersistedConferenceCollectionHousekeepingRowCallbackHandler handler;
	private ConferenceCollection mockConferenceCollection;
	private ConcurrentUpdateManager concurrentUpdateManager;
	private ApplicationContext mockApplicationContext;
	private HousekeeperAware mockHousekeeperAware;

	@Before
	public void before() {
		handler = new PersistedConferenceCollectionHousekeepingRowCallbackHandler();

		mockConferenceCollection = EasyMock.createMock(ConferenceCollection.class);
		handler.setConferenceCollection(mockConferenceCollection);
		concurrentUpdateManager = new ConcurrentUpdateManagerImpl();
		handler.setConcurrentUpdateManager(concurrentUpdateManager);
		mockApplicationContext = EasyMock.createMock(ApplicationContext.class);
		handler.setApplicationContext(mockApplicationContext);
		mockHousekeeperAware = EasyMock.createMock(HousekeeperAware.class);
	}

	@Test
	public void testProcessRow() throws Exception {
		// setup
		ResultSet mockResultSet = EasyMock.createMock(ResultSet.class);
		EasyMock.expect(mockResultSet.getString("conferenceId")).andReturn("conf1");
		EasyMock.expect(mockResultSet.getString("simpleSipBeanId")).andReturn("testBean");
		EasyMock.replay(mockResultSet);

		mockHousekeeperAware.killHousekeeperCandidate("conf1");
		EasyMock.replay(mockHousekeeperAware);

		EasyMock.expect(mockApplicationContext.getBean("testBean")).andReturn(mockHousekeeperAware);
		EasyMock.replay(mockApplicationContext);

		ConferenceInfo conferenceInfo = new ConferenceInfo("testBean", "123.123.123.123:5060", 5, 5);
		conferenceInfo.setId("conf1");
		EasyMock.expect(mockConferenceCollection.get("conf1")).andReturn(conferenceInfo);
		mockConferenceCollection.replace(conferenceInfo);
		EasyMock.replay(mockConferenceCollection);

		// act
		handler.processRow(mockResultSet);

		// assert
		EasyMock.verify(mockResultSet);
		EasyMock.verify(mockHousekeeperAware);
		EasyMock.verify(mockApplicationContext);
		EasyMock.verify(mockConferenceCollection);
		assertTrue(conferenceInfo.isHousekeepForced());
	}
}
