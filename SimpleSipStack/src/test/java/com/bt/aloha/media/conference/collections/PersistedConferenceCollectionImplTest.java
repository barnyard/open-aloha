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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.bt.aloha.dao.ConferenceInfoDao;
import com.bt.aloha.media.conference.collections.ConferenceCollection;
import com.bt.aloha.media.conference.collections.PersistedConferenceCollectionHousekeepingRowCallbackHandler;
import com.bt.aloha.media.conference.collections.PersistedConferenceCollectionImpl;
import com.bt.aloha.media.conference.state.ConferenceInfo;

public class PersistedConferenceCollectionImplTest {

    private ConferenceCollection persistedConferenceCollectionImpl;
    private ConferenceInfoDao mockConferenceInfoDao;
    private ConferenceInfo conferenceInfo;

    static class ConferenceInfoDaoMock implements ConferenceInfoDao {

        private ConferenceInfo cache;
        private long deleteTime;
        private long updateTime;

    	public void create(ConferenceInfo ConferenceInfo) {
            if (null == ConferenceInfo)
                throw new IllegalArgumentException();
            cache = ConferenceInfo;
            cache.setFuture(null);
        }

        public ConferenceInfo read(String ConferenceId) {
            return cache;
        }

        public void delete(String ConferenceId) {
        }

        public void update(ConferenceInfo ConferenceInfo) {
        }

        public int size() {
            return 0;
        }

        public ConferenceInfo findConferenceForCallId(String callId) {
            return null;
        }

		public void updateByHousekeeperFlags(long timeThreshold, RowCallbackHandler handler) {
            this.updateTime = timeThreshold;
		}

		public void deleteByHousekeeperFlags(long timeThreshold) {
		    this.deleteTime = timeThreshold;
		}

        public long getDeleteTime() {
            return deleteTime;
        }

        public long getUpdateTime() {
            return updateTime;
        }

        public ConcurrentMap<String, ConferenceInfo> getAll() {
        	ConcurrentMap<String, ConferenceInfo> result = new ConcurrentHashMap<String, ConferenceInfo>();
        	result.put(cache.getId(), cache);
            return result;
        }

        public ConcurrentMap<String, ConferenceInfo> findConnectedMaxDurationConferences() {
            return null;
        }

		public List<String> findByHousekeeperFlags(long timeThreshold) {
			ArrayList<String> list = new ArrayList<String>();
			list.add("Conference1");
			list.add("Conference2");
			return list;
		}
    }

    @Before
    public void before() {
        conferenceInfo = new ConferenceInfo("unittest", "123.123.123.123:5060", 5, 5);
        conferenceInfo.setId("Conference1");
        mockConferenceInfoDao = EasyMock.createMock(ConferenceInfoDao.class);
        persistedConferenceCollectionImpl = new PersistedConferenceCollectionImpl(mockConferenceInfoDao);
    }

    // test a simple add
    @Test
    public void testAdd() {
        // setup
        mockConferenceInfoDao.create(conferenceInfo);
        EasyMock.replay(mockConferenceInfoDao);

        // act
        persistedConferenceCollectionImpl.add(conferenceInfo);

        // assert
        EasyMock.verify(mockConferenceInfoDao);
    }

    // test transients
    @Test
    public void testGetWithTransient() {
    	// setup
    	ScheduledFuture<?> future = EasyMock.createNiceMock(ScheduledFuture.class);
    	conferenceInfo.setFuture(future);
    	persistedConferenceCollectionImpl = new PersistedConferenceCollectionImpl(new ConferenceInfoDaoMock());

    	// act
    	persistedConferenceCollectionImpl.add(conferenceInfo);
    	ConferenceInfo ConferenceInfoCopy = persistedConferenceCollectionImpl.get("Conference1");

    	// assert
    	assertEquals(future, ConferenceInfoCopy.getFuture());
    }


    // test adding a null ConferenceInfo
    @Test(expected=IllegalArgumentException.class)
    public void testAddNull() {
        // setup
        persistedConferenceCollectionImpl = new PersistedConferenceCollectionImpl(new ConferenceInfoDaoMock());

        // act
        persistedConferenceCollectionImpl.add(null);
    }

    // test a simple get
    @Test
    public void testGet() {
        // setup
        EasyMock.expect(mockConferenceInfoDao.read("Conference1")).andReturn(conferenceInfo);
        EasyMock.replay(mockConferenceInfoDao);

        // act
        ConferenceInfo result = persistedConferenceCollectionImpl.get("Conference1");

        // assert
        EasyMock.verify(mockConferenceInfoDao);
        assertEquals("123.123.123.123:5060", result.getMediaServerAddress());
    }

    // test a simple remove
    @Test
    public void testRemove() {
        // setup
        mockConferenceInfoDao.delete("Conference1");
        EasyMock.replay(mockConferenceInfoDao);

        // act
        persistedConferenceCollectionImpl.remove("Conference1");

        // assert
        EasyMock.verify(mockConferenceInfoDao);
    }

    // test a simple relace
    @Test
    public void testReplace() {
        // setup
        mockConferenceInfoDao.update(conferenceInfo);
        EasyMock.replay(mockConferenceInfoDao);

        // act
        persistedConferenceCollectionImpl.replace(conferenceInfo);

        // assert
        EasyMock.verify(mockConferenceInfoDao);
    }

    // test size
    @Test
    public void testSize() {
        // setup
        EasyMock.expect(mockConferenceInfoDao.size()).andReturn(12);
        EasyMock.replay(mockConferenceInfoDao);

        // act
        int result = persistedConferenceCollectionImpl.size();

        // assert
        EasyMock.verify(mockConferenceInfoDao);
        assertEquals(12, result);
    }

    // test getCurrentConferenceForConferenceLeg single parameter method
    @Test
    public void testGetCurrentConferenceForConferenceLegSingleParam() {
        // setup
        EasyMock.expect(mockConferenceInfoDao.findConferenceForCallId("call1")).andReturn(conferenceInfo);
        EasyMock.replay(mockConferenceInfoDao);

        // act
        ConferenceInfo result = persistedConferenceCollectionImpl.getCurrentConferenceForCall("call1");

        // assert
        EasyMock.verify(mockConferenceInfoDao);
        assertEquals("Conference1", result.getId());
    }

    static class MyRowCallbackHandler implements RowCallbackHandler {
		public void processRow(ResultSet arg0) throws SQLException {
		}
    }

    // test getAll
    @Test
    public void testGetAll() {
        // setup
        ConcurrentMap<String, ConferenceInfo> map = new ConcurrentHashMap<String, ConferenceInfo>();
        map.put("Conference1", conferenceInfo);
        EasyMock.expect(mockConferenceInfoDao.getAll()).andReturn(map);
        EasyMock.replay(mockConferenceInfoDao);

        // act
        ConcurrentMap<String, ConferenceInfo> result = persistedConferenceCollectionImpl.getAll();

        // assert
        EasyMock.verify(mockConferenceInfoDao);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("Conference1"));
    }
    
    // test that getAll also returns transients
    @Test
    public void testGetAllWithTransient() {
    	// setup
    	ScheduledFuture<?> future = EasyMock.createNiceMock(ScheduledFuture.class);
    	conferenceInfo.setFuture(future);
    	persistedConferenceCollectionImpl = new PersistedConferenceCollectionImpl(new ConferenceInfoDaoMock());

    	// act
    	persistedConferenceCollectionImpl.add(conferenceInfo);
        ConcurrentMap<String, ConferenceInfo> result = persistedConferenceCollectionImpl.getAll();

    	// assert
        assertEquals(1, result.size());
        assertTrue(result.containsKey("Conference1"));
        assertEquals(future, result.get("Conference1").getFuture());
    }


    // test the housekeep method
    @Test
    public void testHousekeep() {
        // setup
        ConferenceInfoDaoMock ConferenceInfoDaoMock = new ConferenceInfoDaoMock();
        persistedConferenceCollectionImpl = new PersistedConferenceCollectionImpl(ConferenceInfoDaoMock);
        PersistedConferenceCollectionHousekeepingRowCallbackHandler mockHandler = new PersistedConferenceCollectionHousekeepingRowCallbackHandler() {

			@Override
			public void processRow(ResultSet resultSet) throws SQLException {
			}
    	};
    	((PersistedConferenceCollectionImpl)persistedConferenceCollectionImpl).setPersistedConferenceCollectionHousekeepingRowCallbackHandler(mockHandler);
    	persistedConferenceCollectionImpl.setMaxTimeToLive(10000);

        // act
        persistedConferenceCollectionImpl.housekeep();

        // assert
        assertThreshold(System.currentTimeMillis() - 10000, ConferenceInfoDaoMock.getDeleteTime(), 100);
        assertThreshold(System.currentTimeMillis() - 10000, ConferenceInfoDaoMock.getUpdateTime(), 100);
    }
    
    // test the housekeep cleans the tranient collections
    @Test
    public void testHouseKeepCleanTransient(){
    	// setup
    	ConferenceInfoDaoMock ConferenceInfoDaoMock = new ConferenceInfoDaoMock();
        persistedConferenceCollectionImpl = new PersistedConferenceCollectionImpl(ConferenceInfoDaoMock);
    	((PersistedConferenceCollectionImpl)persistedConferenceCollectionImpl).getTransients().put("Conference1", new HashMap<String, Object>());
    	((PersistedConferenceCollectionImpl)persistedConferenceCollectionImpl).getTransients().put("Conference2", new HashMap<String, Object>());
    	((PersistedConferenceCollectionImpl)persistedConferenceCollectionImpl).getTransients().put("Conference3", new HashMap<String, Object>());
    	// act
    	persistedConferenceCollectionImpl.housekeep();
    	// assert
    	assertEquals(1, ((PersistedConferenceCollectionImpl)persistedConferenceCollectionImpl).getTransients().size());
    }
    
    // get all active Conferences with maximum duration set
    @Test
    public void testGetAllActiveConferencesWithMaxDuration() {
        // setup
        ConcurrentMap<String, ConferenceInfo> map = new ConcurrentHashMap<String, ConferenceInfo>();
    	ScheduledFuture<?> future = EasyMock.createNiceMock(ScheduledFuture.class);
    	conferenceInfo.setFuture(future);
        map.put("Conference1", conferenceInfo);
        EasyMock.expect(mockConferenceInfoDao.findConnectedMaxDurationConferences()).andReturn(map);
        EasyMock.replay(mockConferenceInfoDao);

        // act
        ConcurrentMap<String, ConferenceInfo> result = persistedConferenceCollectionImpl.getAllActiveConferencesWithMaxDuration();

        // assert
        EasyMock.verify(mockConferenceInfoDao);
        assertEquals(1, result.size());
        assertEquals(future, result.get("Conference1").getFuture());
    }
    
    private ScheduledFuture<?> createMockFutureCancelled() {
    	ScheduledFuture<?> future = EasyMock.createMock(ScheduledFuture.class);
    	EasyMock.expect(future.isCancelled()).andReturn(true);
    	EasyMock.replay(future);
    	return future;
    }
    
    private ScheduledFuture<?> createMockFutureDone() {
    	ScheduledFuture<?> future = EasyMock.createMock(ScheduledFuture.class);
    	EasyMock.expect(future.isCancelled()).andStubReturn(false);
    	EasyMock.expect(future.isDone()).andReturn(true);
    	EasyMock.replay(future);
    	return future;
    }
    
    private ScheduledFuture<?> createMockFutureToBeRunAfter100Milliseconds() {
    	ScheduledFuture<?> future = EasyMock.createMock(ScheduledFuture.class);
    	EasyMock.expect(future.isCancelled()).andStubReturn(false);
    	EasyMock.expect(future.isDone()).andStubReturn(false);
    	EasyMock.expect(future.getDelay(TimeUnit.MILLISECONDS)).andReturn(101L);
    	EasyMock.expect(future.cancel(true)).andReturn(true);
    	EasyMock.replay(future);
    	return future;
    }
    
    private ScheduledFuture<?> createMockFutureRunningToFinishOnThirdQuery() {
    	ScheduledFuture<?> future = EasyMock.createMock(ScheduledFuture.class);
    	EasyMock.expect(future.isCancelled()).andStubReturn(false);
    	EasyMock.expect(future.isDone()).andStubReturn(false);
    	EasyMock.expect(future.getDelay(TimeUnit.MILLISECONDS)).andReturn(50L);
    	EasyMock.expect(future.isDone()).andReturn(false);
    	EasyMock.expect(future.isDone()).andReturn(false);
    	EasyMock.expect(future.isDone()).andReturn(true);
    	EasyMock.replay(future);
    	return future;
    }
    
    // test destroy method with cancelled future
    @Test
    public void testDestroyWithCancelledFuture() throws Exception {
    	// setup
		ScheduledFuture<?> future = createMockFutureCancelled();
		conferenceInfo.setFuture(future);
		persistedConferenceCollectionImpl.replace(conferenceInfo);
		
		// act
		persistedConferenceCollectionImpl.destroy();
		
		// assert
		EasyMock.verify(future);
	}
    
    // test destroy method with done future
    @Test
    public void testDestroyWithDoneFuture() throws Exception {
    	// setup
		ScheduledFuture<?> future = createMockFutureDone();
		conferenceInfo.setFuture(future);
		persistedConferenceCollectionImpl.replace(conferenceInfo);
		
		// act
		persistedConferenceCollectionImpl.destroy();
		
		// assert
		EasyMock.verify(future);
	}
    
    // test destroy method with future to be run after 100 milliseconds
    @Test
    public void testDestroyWithFutureToBeRunAfter100Milliseconds() throws Exception {
    	// setup
    	ScheduledFuture<?> future = createMockFutureToBeRunAfter100Milliseconds();
		conferenceInfo.setFuture(future);
		persistedConferenceCollectionImpl.replace(conferenceInfo);
    	
    	// act
    	persistedConferenceCollectionImpl.destroy();
    	
    	// assert
    	EasyMock.verify(future);
    }
    
    // test destroy method with future to be run after 100 milliseconds
    @Test
    public void testDestroyWithFutureRunningToFinishOnThirdQuery() throws Exception {
    	// setup
    	ScheduledFuture<?> future = createMockFutureRunningToFinishOnThirdQuery();
		conferenceInfo.setFuture(future);
		persistedConferenceCollectionImpl.replace(conferenceInfo);
    	
    	// act
		persistedConferenceCollectionImpl.destroy();
    	
    	// assert
    	EasyMock.verify(future);
    }
    
    // this method is not implmented, ensure that the exception is thrown
    @Test (expected = UnsupportedOperationException.class)
    public void testInitNotImplemented(){
    	persistedConferenceCollectionImpl.init();
    }
    
    // on the readTransients method, cover the condition where the object passed is null and expect that
    // the returned object is also null.  Do this, but passing a rubbish Conference id that will return null from the table
    @Test
    public void testReadTransientsWithNullExpectNull(){
    	assertNull(persistedConferenceCollectionImpl.get("rubbish"));
    }
    
    private void assertThreshold(long expected, long actual, int wobble) {
        assertTrue(actual > (expected - wobble));
        assertTrue(actual < (expected + wobble));
    }
}
