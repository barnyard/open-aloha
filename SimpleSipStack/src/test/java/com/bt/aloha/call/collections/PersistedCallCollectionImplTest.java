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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.call.collections.PersistedCallCollectionHousekeepingRowCallbackHandler;
import com.bt.aloha.call.collections.PersistedCallCollectionImpl;
import com.bt.aloha.call.state.CallInfo;
import com.bt.aloha.call.state.CallState;
import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.dao.CallInfoDao;

public class PersistedCallCollectionImplTest {

    private CallCollection persistedCallCollectionImpl;
    private CallInfoDao mockCallInfoDao;
    private CallInfo callInfo1;
    private CallInfo callInfo2;
    private static Random random = new Random(System.currentTimeMillis());

    static class CallInfoDaoMock implements CallInfoDao {

        private CallInfo cache;
        private long deleteTime;
        private long updateTime;

    	public void create(CallInfo callInfo) {
            if (null == callInfo)
                throw new IllegalArgumentException();
            cache = callInfo;
            cache.setFuture(null);
        }

        public CallInfo read(String callId) {
            return cache;
        }

        public void delete(String callId) {
            // TODO Auto-generated method stub
        }

        public void update(CallInfo callInfo) {
            // TODO Auto-generated method stub
        }

        public int size() {
            // TODO Auto-generated method stub
            return 0;
        }

        public CallInfo findCallForDialogId(String dialogId) {
            // TODO Auto-generated method stub
            return null;
        }

        public CallInfo findCallForDialogId(String dialogId, String ignoreCallId) {
            // TODO Auto-generated method stub
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

        public ConcurrentMap<String, CallInfo> getAll() {
            return null;
        }

        public ConcurrentMap<String, CallInfo> findConnectedMaxDurationCalls() {
            // TODO Auto-generated method stub
            return null;
        }

		public List<String> findByHousekeeperFlags(long timeThreshold) {
			ArrayList<String> list = new ArrayList<String>();
			list.add("call1");
			list.add("call2");
			return list;
		}

		public long countByCallState(CallState state, Set<String> filter) {
			return 2;
		}
    }

    @Before
    public void before() {
        callInfo1 = new CallInfo("unittest", "call1", "d1", "d2", AutoTerminateAction.False, 1);
        callInfo2 = new CallInfo("unittest", "call2", "d3", "d4", AutoTerminateAction.False, 1);
        mockCallInfoDao = EasyMock.createMock(CallInfoDao.class);
        persistedCallCollectionImpl = new PersistedCallCollectionImpl(mockCallInfoDao);
    }

    // test a simple add
    @Test
    public void testAdd() {
        // setup
        mockCallInfoDao.create(callInfo1);
        EasyMock.replay(mockCallInfoDao);

        // act
        persistedCallCollectionImpl.add(callInfo1);

        // assert
        EasyMock.verify(mockCallInfoDao);
    }

    // test transients
    @Test
    public void testTransient() {
    	// setup
    	ScheduledFuture<?> future = EasyMock.createNiceMock(ScheduledFuture.class);
    	callInfo1.setFuture(future);
    	persistedCallCollectionImpl = new PersistedCallCollectionImpl(new CallInfoDaoMock());

    	// act
    	persistedCallCollectionImpl.add(callInfo1);
    	CallInfo callInfoCopy = persistedCallCollectionImpl.get("call1");

    	// assert
    	assertEquals(future, callInfoCopy.getFuture());
    }
    
    private void addMockCallInfoToCollection(ScheduledFuture<?> future) {
    	CallInfo info = new CallInfo("unittest", String.format("%d", random.nextInt()), "d1", "d2", AutoTerminateAction.False, 1);
    	info.setFuture(future);
    	persistedCallCollectionImpl = new PersistedCallCollectionImpl(new CallInfoDaoMock());

    	// act
    	persistedCallCollectionImpl.add(info);
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
		addMockCallInfoToCollection(future);
		
		// act
		persistedCallCollectionImpl.destroy();
		
		// assert
		EasyMock.verify(future);
	}
    
    // test destroy method with done future
    @Test
    public void testDestroyWithDoneFuture() throws Exception {
    	// setup
		ScheduledFuture<?> future = createMockFutureDone();
		addMockCallInfoToCollection(future);
		
		// act
		persistedCallCollectionImpl.destroy();
		
		// assert
		EasyMock.verify(future);
	}

    // test destroy method with future to be run after 100 milliseconds
    @Test
    public void testDestroyWithFutureToBeRunAfter100Milliseconds() throws Exception {
    	// setup
    	ScheduledFuture<?> future = createMockFutureToBeRunAfter100Milliseconds();
    	addMockCallInfoToCollection(future);
    	
    	// act
    	persistedCallCollectionImpl.destroy();
    	
    	// assert
    	EasyMock.verify(future);
    }

    // test destroy method with future to be run after 100 milliseconds
    @Test
    public void testDestroyWithFutureRunningToFinishOnThirdQuery() throws Exception {
    	// setup
    	ScheduledFuture<?> future = createMockFutureRunningToFinishOnThirdQuery();
    	addMockCallInfoToCollection(future);
    	
    	// act
    	persistedCallCollectionImpl.destroy();
    	
    	// assert
    	EasyMock.verify(future);
    }
    
	// test adding a null callInfo
    @Test(expected=IllegalArgumentException.class)
    public void testAddNull() {
        // setup
        persistedCallCollectionImpl = new PersistedCallCollectionImpl(new CallInfoDaoMock());

        // act
        persistedCallCollectionImpl.add(null);
    }

    // test a simple get
    @Test
    public void testGet() {
        // setup
        EasyMock.expect(mockCallInfoDao.read("call1")).andReturn(callInfo1);
        EasyMock.replay(mockCallInfoDao);

        // act
        CallInfo result = persistedCallCollectionImpl.get("call1");

        // assert
        EasyMock.verify(mockCallInfoDao);
        assertEquals("d1", result.getFirstDialogId());
    }

    // test a simple remove
    @Test
    public void testRemove() {
        // setup
        mockCallInfoDao.delete("call1");
        EasyMock.replay(mockCallInfoDao);

        // act
        persistedCallCollectionImpl.remove("call1");

        // assert
        EasyMock.verify(mockCallInfoDao);
    }

    // test a simple replace
    @Test
    public void testReplace() {
        // setup
        mockCallInfoDao.update(callInfo1);
        EasyMock.replay(mockCallInfoDao);

        // act
        persistedCallCollectionImpl.replace(callInfo1);

        // assert
        EasyMock.verify(mockCallInfoDao);
    }

    // test size
    @Test
    public void testSize() {
        // setup
        EasyMock.expect(mockCallInfoDao.size()).andReturn(12);
        EasyMock.replay(mockCallInfoDao);

        // act
        int result = persistedCallCollectionImpl.size();

        // assert
        EasyMock.verify(mockCallInfoDao);
        assertEquals(12, result);
    }

    // test getCurrentCallForCallLeg single parameter method
    @Test
    public void testGetCurrentCallForCallLegSingleParam() {
        // setup
        EasyMock.expect(mockCallInfoDao.findCallForDialogId("d1")).andReturn(callInfo1);
        EasyMock.replay(mockCallInfoDao);

        // act
        CallInfo result = persistedCallCollectionImpl.getCurrentCallForCallLeg("d1");

        // assert
        EasyMock.verify(mockCallInfoDao);
        assertEquals("call1", result.getId());
    }

    // test getCurrentCallForCallLeg two parameter method
    @Test
    public void testGetCurrentCallForCallLegTwoParam() {
        // setup
        EasyMock.expect(mockCallInfoDao.findCallForDialogId("d1", "call1")).andReturn(callInfo1);
        EasyMock.replay(mockCallInfoDao);

        // act
        CallInfo result = persistedCallCollectionImpl.getCurrentCallForCallLeg("d1", "call1");

        // assert
        EasyMock.verify(mockCallInfoDao);
        assertEquals("call1", result.getId());
    }

    static class MyRowCallbackHandler implements RowCallbackHandler {
		public void processRow(ResultSet arg0) throws SQLException {
		}
    }

    // test getAll
    @Test
    public void testGetAll() {
        // setup
        ConcurrentMap<String, CallInfo> map = new ConcurrentHashMap<String, CallInfo>();
        map.put("call1", callInfo1);
        map.put("call2", callInfo2);

        ScheduledFuture<?> future1 = EasyMock.createNiceMock(ScheduledFuture.class);
        Map<String, Object> transients1 = new HashMap<String, Object>();
        transients1.put("future", future1);
        ((PersistedCallCollectionImpl)persistedCallCollectionImpl).getTransients().put("call1", transients1);

        ScheduledFuture<?> future2 = EasyMock.createNiceMock(ScheduledFuture.class);
        Map<String, Object> transients2 = new HashMap<String, Object>();
        transients2.put("future", future2);
        ((PersistedCallCollectionImpl)persistedCallCollectionImpl).getTransients().put("call2", transients2);

        EasyMock.expect(mockCallInfoDao.getAll()).andReturn(map);
        EasyMock.replay(mockCallInfoDao);

        // act
        ConcurrentMap<String, CallInfo> result = persistedCallCollectionImpl.getAll();

        // assert
        EasyMock.verify(mockCallInfoDao);
        assertEquals(2, result.size());
        assertEquals(future1, result.get("call1").getFuture());
        assertEquals(future2, result.get("call2").getFuture());
    }

    // test the housekeep method
    @Test
    public void testHousekeep() {
        // setup
        CallInfoDaoMock callInfoDaoMock = new CallInfoDaoMock();
        persistedCallCollectionImpl = new PersistedCallCollectionImpl(callInfoDaoMock);
        PersistedCallCollectionHousekeepingRowCallbackHandler mockHandler = new PersistedCallCollectionHousekeepingRowCallbackHandler() {

			@Override
			public void processRow(ResultSet resultSet) throws SQLException {
			}
    	};
    	((PersistedCallCollectionImpl)persistedCallCollectionImpl).setPersistedCallCollectionHousekeepingRowCallbackHandler(mockHandler);
    	persistedCallCollectionImpl.setMaxTimeToLive(10000);

        // act
        persistedCallCollectionImpl.housekeep();

        // assert
        assertThreshold(System.currentTimeMillis() - 10000, callInfoDaoMock.getDeleteTime(), 100);
        assertThreshold(System.currentTimeMillis() - 10000, callInfoDaoMock.getUpdateTime(), 100);
    }

    // test that housekeep cleans the transient collections
    @Test
    public void testHouseKeepCleanTransient(){
    	// setup
    	CallInfoDaoMock callInfoDaoMock = new CallInfoDaoMock();
        persistedCallCollectionImpl = new PersistedCallCollectionImpl(callInfoDaoMock);
    	((PersistedCallCollectionImpl)persistedCallCollectionImpl).getTransients().put("call1", new HashMap<String, Object>());
    	((PersistedCallCollectionImpl)persistedCallCollectionImpl).getTransients().put("call2", new HashMap<String, Object>());
    	((PersistedCallCollectionImpl)persistedCallCollectionImpl).getTransients().put("call3", new HashMap<String, Object>());
    	
    	// act
    	persistedCallCollectionImpl.housekeep();
    	
    	// assert
    	assertEquals(1, ((PersistedCallCollectionImpl)persistedCallCollectionImpl).getTransients().size());
    }

    // get all connected calls with maximum duration set
    @Test
    public void testGetAllConnectedCallsWithMaxDuration() {
        // setup
        ConcurrentMap<String, CallInfo> map = new ConcurrentHashMap<String, CallInfo>();
        ScheduledFuture<?> future = EasyMock.createNiceMock(ScheduledFuture.class);
        
        Map<String, Object> transients = new HashMap<String, Object>();
        transients.put("future", future);
        ((PersistedCallCollectionImpl)persistedCallCollectionImpl).getTransients().put("call1", transients);
        
        map.put("call1", callInfo1);
        EasyMock.expect(mockCallInfoDao.findConnectedMaxDurationCalls()).andReturn(map);
        EasyMock.replay(mockCallInfoDao);

        // act
        ConcurrentMap<String, CallInfo> result = persistedCallCollectionImpl.getAllConnectedCallsWithMaxDuration();

        // assert
        EasyMock.verify(mockCallInfoDao);
        assertEquals(1, result.size());
        assertEquals(future, result.get("call1").getFuture());
    }

    // this method is not implemented, ensure that the exception is thrown
    @Test (expected = UnsupportedOperationException.class)
    public void testInitNotImplemented(){
    	persistedCallCollectionImpl.init();
    }

    // on the readTransients method, cover the condition where the object passed is null and expect that
    // the returned object is also null.  Do this, but passing a rubbish call id that will return null from the table
    @Test
    public void testReadTransientsWithNullExpectNull(){
    	assertNull(persistedCallCollectionImpl.get("rubbish"));
    }

    private void assertThreshold(long expected, long actual, int wobble) {
        assertTrue(actual > (expected - wobble));
        assertTrue(actual < (expected + wobble));
    }

    // pass in an empty collection and ensure we get back no connecting calls in this stack due to no transients
    @Test
    public void testGetNumberOfConnectingCallsEmptyIdFilterSet(){
        EasyMock.expect(mockCallInfoDao.countByCallState(CallState.Connecting, new HashSet<String>())).andReturn(0L);
        EasyMock.replay(mockCallInfoDao);
        long number = persistedCallCollectionImpl.getNumberOfConnectingCalls();
        EasyMock.verify(mockCallInfoDao);
        assertEquals(0, number);
    }
}
