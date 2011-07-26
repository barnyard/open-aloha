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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.collections.memory.InMemoryHousekeepingCollectionImpl;
import com.bt.aloha.dao.StateInfoDao;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.state.StateInfoBase;
import com.bt.aloha.state.TransientInfo;
import com.bt.aloha.testing.SimpleTestInfo;

public class DatabaseInfoCollectionImplTest {
    private DatabaseInfoCollectionImpl<DialogInfo> collection;
    private StateInfoDao<DialogInfo> collectionsDao;
    private String collectionTypeName;
    private DialogInfo info;
    private DatabaseInfoCollectionHousekeepingRowCallBackHandler rowCallBackHandler;

    @SuppressWarnings("unchecked")
    @Before
    public void before() throws Exception {
        collectionTypeName = "DialogInfo";
        collectionsDao = EasyMock.createMock(StateInfoDao.class);
        collection = new DatabaseInfoCollectionImpl<DialogInfo>(
                new InMemoryHousekeepingCollectionImpl<TransientInfo>(), "DialogInfo");
        collection.setCollectionsDao(collectionsDao);
        rowCallBackHandler = new DatabaseInfoCollectionHousekeepingRowCallBackHandler();
        collection.setRowCallBackHandler(rowCallBackHandler);
        collection.init();
        info = new DialogInfo("dialogId", "beanId", "1.2.3.4");
        info.setLocalParty("sip:localParty1@abc.de");
    }

    @After
    public void after() throws Exception {
        collection.destroy();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTransientField() throws Exception {
        // setup
        StateInfoDao<SimpleTestInfo> simpleCollectionsDao = EasyMock.createNiceMock(StateInfoDao.class);
        DatabaseInfoCollectionImpl<SimpleTestInfo> collection1 = new DatabaseInfoCollectionImpl<SimpleTestInfo>(
                new InMemoryHousekeepingCollectionImpl<TransientInfo>(), "MyTestInfo");
        collection1.setCollectionsDao(simpleCollectionsDao);
        collection1.init();
        collection1.destroy();

        SimpleTestInfo myInfo1 = new SimpleTestInfo("id1", "one", "two");
        SimpleTestInfo myInfo2 = new SimpleTestInfo("id2", "three", "four");

        EasyMock.expect(simpleCollectionsDao.get("id1")).andStubReturn(myInfo1.cloneObject());
        EasyMock.expect(simpleCollectionsDao.get("id2")).andReturn(myInfo2.cloneObject());
        EasyMock.replay(simpleCollectionsDao);

        collection1.add(myInfo1);
        collection1.add(myInfo2);

        // act
        SimpleTestInfo myInfo3 = collection1.get("id1");
        SimpleTestInfo myInfo4 = collection1.get("id2");

        // assert
        assertEquals(myInfo1.getF2(), myInfo3.getF2());
        assertEquals(myInfo2.getF2(), myInfo4.getF2());

        myInfo1.setF2("ttt");
        collection1.replace(myInfo1);
        SimpleTestInfo myInfo5 = collection1.get("id1");

        assertEquals("ttt", myInfo5.getF2());
        EasyMock.verify(simpleCollectionsDao);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetAllWithTransientField() throws Exception {
        // setup
        StateInfoDao<SimpleTestInfo> simpleCollectionsDao = EasyMock.createNiceMock(StateInfoDao.class);
        DatabaseInfoCollectionImpl<SimpleTestInfo> collection1 = new DatabaseInfoCollectionImpl<SimpleTestInfo>(
                new InMemoryHousekeepingCollectionImpl<TransientInfo>(), "MyTestInfo");
        collection1.setCollectionsDao(simpleCollectionsDao);
        collection1.init();
        collection1.destroy();

        SimpleTestInfo myInfo1 = new SimpleTestInfo("id1", "one", "two");
        SimpleTestInfo myInfo2 = new SimpleTestInfo("id2", "three", "four");

        Collection<SimpleTestInfo> values = new Vector<SimpleTestInfo>();
        values.add(myInfo1.cloneObject());
        values.add(myInfo2.cloneObject());
        ConcurrentMap<String, SimpleTestInfo> concurrentMap = EasyMock.createMock(ConcurrentMap.class);
        EasyMock.expect(concurrentMap.values()).andReturn(values);
        EasyMock.expect(concurrentMap.get("id1")).andReturn(myInfo1.cloneObject());
        EasyMock.expect(concurrentMap.get("id2")).andReturn(myInfo2.cloneObject());
        EasyMock.replay(concurrentMap);
        EasyMock.expect(simpleCollectionsDao.getAll("MyTestInfo")).andReturn(concurrentMap);
        EasyMock.replay(simpleCollectionsDao);

        collection1.add(myInfo1);
        collection1.add(myInfo2);

        // act
        Map<String, SimpleTestInfo> results = collection1.getAll();

        // assert
        assertEquals(myInfo1.getF2(), results.get("id1").getF2());
        assertEquals(myInfo2.getF2(), results.get("id2").getF2());

        EasyMock.verify(concurrentMap);
        EasyMock.verify(simpleCollectionsDao);
    }

    // Check if info object added to collection is stored correctly
    @Test
    public void testAddingInfoToCollection() throws Exception {
        // setup
        collectionsDao.add(info, collectionTypeName);
        EasyMock.replay(collectionsDao);

        // act
        collection.add(info);

        // assert
        EasyMock.verify(collectionsDao);
    }

    // Test retrieving previously serialized info object
    @Test
    public void testGetInfo() throws Exception {
        // setup
        EasyMock.expect(collectionsDao.get(info.getId())).andReturn(info);
        EasyMock.replay(collectionsDao);

        // act
        StateInfoBase<DialogInfo> retrievedDialogInfo = collection.get(info.getId());

        // assert
        EasyMock.verify(collectionsDao);
        assertEquals(info, retrievedDialogInfo);
    }

    // test doing a get when there is no record returned from the DAO but there
    // is some transient data
    @Test
    public void testGetInfoNotFoundWithTransient() throws Exception {
        // setup
        collection.add(info);
        EasyMock.reset(collectionsDao);
        EasyMock.expect(collectionsDao.get(info.getId())).andReturn(null);
        EasyMock.replay(collectionsDao);

        // act
        StateInfoBase<DialogInfo> retrievedDialogInfo = collection.get(info.getId());

        // assert
        EasyMock.verify(collectionsDao);
        assertNull(retrievedDialogInfo);
    }

    // Test removing an info object from collection
    @Test
    public void testRemoveInfo() throws Exception {
        // setup
        collectionsDao.remove(info.getId());
        EasyMock.replay(collectionsDao);

        // act
        collection.remove(info.getId());

        // assert
        EasyMock.verify(collectionsDao);
    }

    // Test replacing an info object from collection
    @Test
    public void testReplace() throws Exception {
        // setup
        collectionsDao.replace(info);
        EasyMock.replay(collectionsDao);

        // act
        collection.replace(info);

        // assert
        EasyMock.verify(collectionsDao);
    }

    // Test size getter
    @Test
    public void testSize() throws Exception {
        // setup
        EasyMock.expect(collectionsDao.size("DialogInfo")).andReturn(3);
        EasyMock.replay(collectionsDao);

        // act
        int size = collection.size();

        // assert
        EasyMock.verify(collectionsDao);
        assertEquals(3, size);
    }

    // setup the table to have rows that have expired but are not marked for
    // termination
    // force housekeeping on them and then assert they are removed on the next
    // pass
    @Test
    public void testHouseKeeperForced() throws Exception {
        // setup
        collectionsDao.housekeep("DialogInfo", DatabaseInfoCollectionImpl.DEFAULT_TIMEOUT, rowCallBackHandler);
        EasyMock.replay(collectionsDao);

        // act
        collection.housekeep();

        // assert
        EasyMock.verify(collectionsDao);
    }

    // Test that database initialisation doesn't throw exception when schema
    // created twice
    @Test
    public void testInit() throws Exception {
        // setup
        collection.init();

        // act
        try {
            collection.init();
        } catch (RuntimeException e) {
            fail("Unexpected exception");
        }
    }

    // test the getAll method
    @Test
    public void testGetAll() throws Exception {
        // setup
        ConcurrentMap<String, DialogInfo> concurrentMap = new ConcurrentHashMap<String, DialogInfo>();
        EasyMock.expect(collectionsDao.getAll("DialogInfo")).andReturn(concurrentMap);
        EasyMock.replay(collectionsDao);

        // act
        ConcurrentMap<String, DialogInfo> retrievedConcurrentMap = collection.getAll();

        // assert
        EasyMock.verify(collectionsDao);
        assertEquals(concurrentMap, retrievedConcurrentMap);
    }
}
