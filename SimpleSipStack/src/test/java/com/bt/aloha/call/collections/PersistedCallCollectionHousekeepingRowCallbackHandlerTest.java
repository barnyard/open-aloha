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

 	

 	
 	
 
package com.bt.aloha.call.collections;

import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.call.collections.PersistedCallCollectionHousekeepingRowCallbackHandler;
import com.bt.aloha.call.state.CallInfo;
import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.util.ConcurrentUpdateManager;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;
import com.bt.aloha.util.HousekeeperAware;

public class PersistedCallCollectionHousekeepingRowCallbackHandlerTest {

	private PersistedCallCollectionHousekeepingRowCallbackHandler handler;
	private CallCollection mockCallCollection;
	private ConcurrentUpdateManager concurrentUpdateManager;
	private ApplicationContext mockApplicationContext;
	private HousekeeperAware mockHousekeeperAware;

	@Before
	public void before() {
		handler = new PersistedCallCollectionHousekeepingRowCallbackHandler();

		mockCallCollection = EasyMock.createMock(CallCollection.class);
		handler.setCallCollection(mockCallCollection);
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
		EasyMock.expect(mockResultSet.getString("callId")).andReturn("call1");
		EasyMock.expect(mockResultSet.getString("simpleSipBeanId")).andReturn("testBean");
		EasyMock.replay(mockResultSet);

		mockHousekeeperAware.killHousekeeperCandidate("call1");
		EasyMock.replay(mockHousekeeperAware);

		EasyMock.expect(mockApplicationContext.getBean("testBean")).andReturn(mockHousekeeperAware);
		EasyMock.replay(mockApplicationContext);

		CallInfo callInfo = new CallInfo("testBean", "call1", "d1", "d2", AutoTerminateAction.True, 1);
		EasyMock.expect(mockCallCollection.get("call1")).andReturn(callInfo);
		mockCallCollection.replace(callInfo);
		EasyMock.replay(mockCallCollection);

		// act
		handler.processRow(mockResultSet);

		// assert
		EasyMock.verify(mockResultSet);
		EasyMock.verify(mockHousekeeperAware);
		EasyMock.verify(mockApplicationContext);
		EasyMock.verify(mockCallCollection);
		assertTrue(callInfo.isHousekeepForced());
	}
}
