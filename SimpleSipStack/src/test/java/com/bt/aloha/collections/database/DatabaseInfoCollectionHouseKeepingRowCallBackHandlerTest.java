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

 	

 	
 	
 
package com.bt.aloha.collections.database;

import java.sql.ResultSet;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import com.bt.aloha.collections.database.DatabaseInfoCollectionHousekeepingRowCallBackHandler;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.util.HousekeeperAware;
import com.bt.aloha.util.ObjectSerialiser;

public class DatabaseInfoCollectionHouseKeepingRowCallBackHandlerTest {

    private DatabaseInfoCollectionHousekeepingRowCallBackHandler handler;
    private ApplicationContext mockApplicationContext;
    private HousekeeperAware mockHousekeeperAware;

    @Before
    public void setUp() throws Exception {
        handler = new DatabaseInfoCollectionHousekeepingRowCallBackHandler();

        mockHousekeeperAware = EasyMock.createMock(HousekeeperAware.class);
        mockApplicationContext = EasyMock.createMock(ApplicationContext.class);
        handler.setApplicationContext(mockApplicationContext);
    }

    @Test
    public void testProcessRow() throws Exception {
        // setup - stub
        // setup - expectations
        ResultSet mockResultSet = EasyMock.createMock(ResultSet.class);
        byte[] byteArr = new ObjectSerialiser().serialise(new DialogInfo("beanId","beanId", "1.2.3.4"));
        EasyMock.expect(mockResultSet.getString("object_id")).andReturn("id1");
        EasyMock.expect(mockResultSet.getBytes("object_value")).andReturn(byteArr);
        EasyMock.expect(mockApplicationContext.getBean("beanId")).andReturn(mockHousekeeperAware);
        mockHousekeeperAware.killHousekeeperCandidate("id1");
        // setup - replays
        EasyMock.replay(mockResultSet);
        EasyMock.replay(mockApplicationContext);
        EasyMock.replay(mockHousekeeperAware);
        // act
        handler.processRow(mockResultSet);

        // assert
        EasyMock.verify(mockResultSet);
        EasyMock.verify(mockApplicationContext);
        EasyMock.verify(mockHousekeeperAware);
    }

}
