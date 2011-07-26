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

 	

 	
 	
 
package com.bt.aloha.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.bt.aloha.call.state.CallInfo;
import com.bt.aloha.call.state.CallLegCausingTermination;
import com.bt.aloha.call.state.CallState;
import com.bt.aloha.call.state.CallTerminationCause;
import com.bt.aloha.call.state.MediaNegotiationMethod;
import com.bt.aloha.call.state.ThirdPartyCallInfo;
import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.dao.CallInfoDaoImpl;
import com.bt.aloha.testing.DbTestCase;
import com.bt.aloha.util.ConcurrentUpdateException;

public class CallInfoDaoImplTest extends DbTestCase {

    private CallInfoDaoImpl callInfoDao;
    private List<String> callIds = new Vector<String>();

    class MyRowCallbackHandler implements RowCallbackHandler {

		public void processRow(ResultSet arg0) throws SQLException {
			callIds.add(arg0.getString("callid"));
		}
    }

    @Before
    public void before() throws Exception {
        super.before();

        super.createCallInfoTable();
        cleanCallInfoTable();

        this.callInfoDao = new CallInfoDaoImpl(ds);
        callIds = new Vector<String>();
    }

    @After
    public void cleanDb(){
        cleanCallInfoTable();
    }

    @Test
    public void testCreateSingle() throws Exception {
        // act
        CallInfo callInfo = new ThirdPartyCallInfo("unittest", "call1", "d1", "d2", AutoTerminateAction.False, 1);
        callInfoDao.create(callInfo);

        // assert
        assertEquals("1", query(String.format("select count(*) from CallInfo")));

        String select = query(String.format("select classname, simpleSipBeanId, callId, firstDialogId, secondDialogId, autoTerminate, maxDurationInMinutes from CallInfo"));
        assertEquals(ThirdPartyCallInfo.class.getName() + " unittest call1 d1 d2 False 1", select);
    }

    private void insertCallInfo(String id, String callState)  throws Exception {
    	insertCallInfo(id, callState, "11927218308830.273397", "1192721830883");
    }

    private void insertCallInfo(String id, String callState, String versionId, String lastUseTime) throws Exception {
    	insertCallInfo(id, callState, versionId, lastUseTime, "1111111111111", "d1", "d2");
    }

    private void insertCallInfo(String id, String callState, String versionId, String lastUseTime, String createTime, String d1, String d2) throws Exception {
    	insertCallInfo(id, callState, versionId, lastUseTime, "1111111111111", d1, d2, false, "1");
    }

    private void insertCallInfo(String id, String callState, String versionId, String lastUseTime, String createTime, String d1, String d2, boolean housekeepingForced) throws Exception {
    	insertCallInfo(id, callState, versionId, lastUseTime, "1111111111111", d1, d2, housekeepingForced, "1");
    }

   	private void insertCallInfo(String id, String callState, String versionId, String lastUseTime, String createTime, String d1, String d2, boolean housekeepingForced, String maxDurationMins) throws Exception {
        String sql = "INSERT INTO callinfo (callid, " +
        "classname, " +
        "simplesipbeanid, " +
        "createtime, " +
        "startTime, " +
        "endTime, " +
        "object_version, " +
        "last_use_time, " +
        "callstate, " +
        "firstdialogid, " +
        "seconddialogid, " +
        "firstcalllegconnectionstate, " +
        "secondcalllegconnectionstate, " +
        "medianegotiationstate, " +
        "medianegotiationmethod, " +
        "maxdurationinminutes, " +
        "autoterminate, " +
        "callterminationcause, " +
        "calllegcausingtermination, " +
        "pendingcallreinvite_dialogid, " +
        "pendingcallreinvite_remotecontact, " +
        "pendingcallreinvite_offerinokresponse, " +
        "pendingcallreinvite_autoterminate, " +
        "pendingcallreinvite_mediadescription, " +
        "pendingcallreinvite_applicationdata, " +
        "housekeepingForced) " +
        "VALUES ('" + id + "', " +
        "'" + ThirdPartyCallInfo.class.getName() + "', " +
        "'unittest', " +
        createTime + ", " +
        "2222222222222, " +
        "3333333333333, " +
        "'" + versionId + "', " +
        lastUseTime + ", " +
        "'" + callState + "', " +
        "'" + d1 + "', " +
        "'" + d2 + "', " +
        "'Pending', " +
        "'Pending', " +
        "'Pending', " +
        "'ReinviteRequest', " +
        maxDurationMins + ", " +
        "'False', " +
        "'Housekept', " +
        "'First', " +
        "'ddd111', " +
        "'remote contact', " +
        "false, " +
        "true, " +
        "null, " +
        "'SomeApplicationData', " +
        housekeepingForced + ");";
        update(sql);

    }

    @Test
    public void testRead() throws Exception {
        // setup
    	insertCallInfo("call2", "Connecting");

        // act
        CallInfo e = callInfoDao.read("call2");

        //assert
        assertTrue(e instanceof ThirdPartyCallInfo);
        assertEquals("call2", e.getId());
        assertEquals("unittest", e.getSimpleSipBeanId());
        assertEquals(1111111111111L, e.getCreateTime());
        assertEquals(2222222222222L, e.getStartTime());
        assertEquals(3333333333333L, e.getEndTime());
        assertEquals("11927218308830.273397", e.getVersionId());
        assertEquals(1192721830883L, e.getLastUsedTime());
        assertEquals(CallState.Connecting, e.getCallState());
        assertEquals("d1", e.getFirstDialogId());
        assertEquals("d2", e.getSecondDialogId());
        assertEquals("Pending", e.getFirstCallLegConnectionState().toString());
        assertEquals("Pending", e.getSecondCallLegConnectionState().toString());
        assertEquals("Pending", e.getMediaNegotiationState().toString());
        assertEquals(MediaNegotiationMethod.ReinviteRequest, e.getMediaNegotiationMethod());
        assertEquals(1, e.getMaxDurationInMinutes());
        assertEquals(AutoTerminateAction.False, e.getAutoTerminate());
        assertEquals(CallTerminationCause.Housekept, e.getCallTerminationCause());
        assertEquals(CallLegCausingTermination.First, e.getCallLegCausingTermination());
        assertEquals("ddd111", e.getPendingCallReinvite().getDialogId());
        assertEquals("remote contact", e.getPendingCallReinvite().getRemoteContact());
        assertFalse(e.getPendingCallReinvite().getReceivedCallLegRefreshEvent().isOfferInOkResponse());

        // TODO: this is always false - is it used?
        assertFalse(e.getPendingCallReinvite().getAutoTerminate());
        assertNull(e.getPendingCallReinvite().getMediaDescription());
        assertEquals("SomeApplicationData", e.getPendingCallReinvite().getApplicationData());
        assertEquals(1, e.getTransients().size());
        assertNull(e.getTransients().get("future"));
    }

    @Test
    public void testReadWhereStateGreaterThanConnecting() throws Exception {
        // setup
    	insertCallInfo("call2", "Terminated");

        // act
        CallInfo e = callInfoDao.read("call2");

        //assert
        assertTrue(e instanceof ThirdPartyCallInfo);
        assertEquals("call2", e.getId());
        assertEquals("unittest", e.getSimpleSipBeanId());
        assertEquals(1111111111111L, e.getCreateTime());
        assertEquals(2222222222222L, e.getStartTime());
        assertEquals(3333333333333L, e.getEndTime());
        assertEquals("11927218308830.273397", e.getVersionId());
        assertEquals(1192721830883L, e.getLastUsedTime());
        assertEquals(CallState.Terminated, e.getCallState());
        assertEquals("d1", e.getFirstDialogId());
        assertEquals("d2", e.getSecondDialogId());
        assertEquals("Pending", e.getFirstCallLegConnectionState().toString());
        assertEquals("Pending", e.getSecondCallLegConnectionState().toString());
        assertEquals("Pending", e.getMediaNegotiationState().toString());
        assertEquals(MediaNegotiationMethod.ReinviteRequest, e.getMediaNegotiationMethod());
        assertEquals(1, e.getMaxDurationInMinutes());
        assertEquals(AutoTerminateAction.False, e.getAutoTerminate());
        assertEquals(CallTerminationCause.Housekept, e.getCallTerminationCause());
        assertEquals(CallLegCausingTermination.First, e.getCallLegCausingTermination());
        assertEquals("ddd111", e.getPendingCallReinvite().getDialogId());
        assertEquals("remote contact", e.getPendingCallReinvite().getRemoteContact());
        assertFalse(e.getPendingCallReinvite().getReceivedCallLegRefreshEvent().isOfferInOkResponse());

        // TODO: this is always false - is it used?
        assertFalse(e.getPendingCallReinvite().getAutoTerminate());
        assertNull(e.getPendingCallReinvite().getMediaDescription());
        assertEquals("SomeApplicationData", e.getPendingCallReinvite().getApplicationData());
        assertEquals(1, e.getTransients().size());
        assertNull(e.getTransients().get("future"));
    }

    @Test
    public void testDelete() throws Exception {
        // setup
    	insertCallInfo("call2", "Connecting");

    	// act
        callInfoDao.delete("call3");

        // assert
        String select = query(String.format("select * from CallInfo where callId = 'call3'"));
        assertTrue(select.length() < 1);
    }

    @Test
    public void testUpdate() throws Exception {
        // setup
    	String oldVersionId = "11927218308830.273397";
        long lastUseTime =    1111111111111L;
        insertCallInfo("call4", "Connecting", oldVersionId, Long.toString(lastUseTime));

        // act
        CallInfo callInfo = new CallInfo("unittest", "call4", "d1", "d3", AutoTerminateAction.False, 1);
        callInfo.setVersionId(oldVersionId);
        callInfoDao.update(callInfo);

        // assert
        String select = query(String.format("select secondDialogId, object_version, last_use_time from CallInfo where callId = '%s'", "call4"));
        assertEquals("d3", select.split(" ")[0]);
        assertFalse(oldVersionId.equals(select.split(" ")[1]));
        long newLastUseTime = Long.parseLong(select.split(" ")[2]);
        assertTrue(newLastUseTime > lastUseTime);
    }

    @Test(expected=ConcurrentUpdateException.class)
    public void testUpdateConcurrentUpdateException() throws Exception {
        // setup
    	insertCallInfo("call4", "Connecting");

        // act
        CallInfo callInfo = new CallInfo("unittest", "call4", "d1", "d3", AutoTerminateAction.False, 1);
        callInfo.setVersionId("11927218308830.999999");
        callInfoDao.update(callInfo);
    }

    @Test
    public void testSize() throws Exception {
        // setup
    	insertCallInfo("call7", "Connecting");
    	insertCallInfo("call8", "Connecting");

    	// act
        int result = callInfoDao.size();

        // assert
        assertEquals(2, result);
    }

    @Test
    public void testFindCallForDialogIdString() throws Exception {
        // setup
    	insertCallInfo("call9", "Connecting", "11927218308830.273397", "1192721830883", "1111111111", "d901", "d1001");
    	insertCallInfo("call10", "Connecting", "11927218308830.273397", "1192721830883", "2222222222", "d1001", "d1002");

        // act
        CallInfo result = callInfoDao.findCallForDialogId("d1001");

        // assert
        assertEquals("d1002", result.getSecondDialogId());
        assertEquals("call10", result.getId());
    }

    @Test
    public void testFindCallForDialogIdStringSameCreateTimes() throws Exception {
        // setup
    	insertCallInfo("call9", "Connecting", "11927218308830.273397", "1192721830883", "2222222222", "d901", "d902");
    	insertCallInfo("call10", "Connecting", "11927218308830.273397", "1192721830883", "2222222222", "d1001", "d1002");

        // act
        CallInfo result = callInfoDao.findCallForDialogId("d1001");

        // assert
        assertEquals("d1002", result.getSecondDialogId());
        assertEquals("call10", result.getId());
    }

    @Test
    public void testFindCallForDialogIdStringString() throws Exception {
        // setup
    	insertCallInfo("call9", "Connecting", "11927218308830.273397", "1192721830883", "3333333333333", "d901", "d1001");
    	insertCallInfo("call10", "Connecting", "11927218308830.273397", "1192721830883", "4444444444444", "d1001", "d1002");
    	insertCallInfo("call11", "Connecting", "11927218308830.273397", "1192721830883", "5555555555555", "d1001", "d1002");

        // act
        CallInfo result = callInfoDao.findCallForDialogId("d1001", "call11");

        // assert
        assertEquals("d1002", result.getSecondDialogId());
        assertEquals("call10", result.getId());
    }


    @Test
    public void testDeleteByHousekeeperFlags() throws Exception {
        // setup
    	setupHousekeepingData();

    	// act
    	callInfoDao.deleteByHousekeeperFlags(8888888888889L);

    	// assert
        String select = query("select * from CallInfo;");
        assertTrue(select.indexOf("call10") < 0);
        assertTrue(select.indexOf("call11") < 0);
        assertTrue(select.contains("call9"));
        assertEquals(1, callInfoDao.size());
    }

    private void setupHousekeepingData() throws Exception {
    	insertCallInfo("call9", "Connecting", "11927218308830.273397", "1192721830883", "3333333333333", "d901", "d1001", false);
    	insertCallInfo("call10", "Terminated", "11927218308830.273397", "8888888888888", "4444444444444", "d1001", "d1002", false);
        insertCallInfo("call11", "Connected", "11927218308830.273397", "8888888888888", "4444444444444", "d1001", "d1002", true);
    }

    @Test
    public void testUpdateByHousekeeperFlags() throws Exception {
        // setup
    	insertCallInfo("call9", "Connecting", "11927218308830.273397", "9999999999999", "9999999999999", "d901", "d1001", false);
    	insertCallInfo("call10", "Connected", "11927218308830.273397", "8888888888888", "4444444444444", "d1001", "d1002", false);

        // act
    	callInfoDao.updateByHousekeeperFlags(8888888888889L, new MyRowCallbackHandler());

    	// assert
    	assertEquals(1, callIds.size());
    	assertEquals("call10", callIds.get(0));
    }

    @Test
    public void testFindByHouseKeeperFlags() throws Exception {
    	// setup
    	setupHousekeepingData();

    	// act
    	List<String> callsToBeDeletedLocally = callInfoDao.findByHousekeeperFlags(8888888888889L);

    	// assert
    	assertEquals(2, callsToBeDeletedLocally.size());
        assertTrue(callsToBeDeletedLocally.contains("call10"));
        assertTrue(callsToBeDeletedLocally.contains("call11"));
    }

    @Test
    public void testGetAll() throws Exception {
        // setup
    	setupHousekeepingData();

        // act
        ConcurrentMap<String, CallInfo> result = callInfoDao.getAll();

        // assert
        assertEquals(3, result.size());
    }

    // try finding a call for a dialog id that doesn't exist
    @Test
    public void testFindCallForDialogIdStringStringNotFound() throws Exception {
    	// act
    	CallInfo result = callInfoDao.findCallForDialogId("aaaaa", "bbbbb");

    	// assert
    	assertNull(result);
    }

    // ensure when null is pass to find a call, that null is returned
    @Test
    public void testFindCallForDialogIdStringNull(){
    	assertNull(callInfoDao.findCallForDialogId(null));
    }

    @Test
    public void testFindCallForDialogIdStringNotFoundReturnNull(){
    	assertNull(callInfoDao.findCallForDialogId("notfound"));
    }

    @Test
    public void testFindConnectedMaxDurationCalls() throws Exception {
        // setup
    	insertCallInfo("call9", "Connecting", "11927218308830.273397", "1192721830883", "3333333333333", "d901", "d1001", false, "123");
    	insertCallInfo("call10", "Connected", "11927218308830.273397", "8888888888888", "4444444444444", "d1001", "d1002", false, "112");
        insertCallInfo("call11", "Terminated", "11927218308830.273397", "8888888888888", "4444444444444", "d1001", "d1002", true, "1");

        // act
        ConcurrentMap<String, CallInfo> result = callInfoDao.findConnectedMaxDurationCalls();

        // assert
        assertEquals(1, result.size());
        assertTrue(result.containsKey("call10"));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateAlreadyExists(){
        // act
        CallInfo callInfo = new ThirdPartyCallInfo("unittest", "call1", "d1", "d2", AutoTerminateAction.False, 1);
        callInfoDao.create(callInfo);
        callInfoDao.create(callInfo);
    }

    // ensure we don't get an exception when we remove
    // a non existent record
    @Test
    public void testDeleteNonExistentCall(){
    	callInfoDao.delete("callnotexist");
    }

    // tests that we get the correct number of calls by selecting by state whit a null filtering set
    @Test
    public void testCountByStateNullSet(){
    	// set
    	populateCallInfoCollectionForCountByState();
        // act
        long conn = callInfoDao.countByCallState(CallState.Connected, null);
        long term = callInfoDao.countByCallState(CallState.Terminated, null);

        // assert
        assertEquals(2, conn);
        assertEquals(2, term);
    }

    // tests that we get the correct number of calls by selecting by state with an empty set.
    // in this case it returns always 0;
    @Test
    public void testCountByStateEmptySet(){
    	// set
    	populateCallInfoCollectionForCountByState();
        // act
        long term = callInfoDao.countByCallState(CallState.Terminated, new HashSet<String>());
        long conn = callInfoDao.countByCallState(CallState.Connected, new HashSet<String>());

        // assert
        assertEquals(0, conn);
        assertEquals(0, term);
    }

    // tests that we get the correct number of calls by selecting by state passing also valid data in set
    // this joins the results by state and by id
    @Test
    public void testCountByStateWhenStateIsNotAvailable(){
    	// set
    	populateCallInfoCollectionForCountByState();
    	HashSet<String> set = new HashSet<String>();
    	set.add("call1");
    	set.add("call4");
    	// act
        long c = callInfoDao.countByCallState(CallState.Connecting, set);

        // assert
        assertEquals(0, c);
    }

    // tests that we get the correct number of calls by selecting by state passing also valid data in set
    // this joins the results by state and by id
    @Test
    public void testCountByStateWithDataInSet(){
    	// set
    	populateCallInfoCollectionForCountByState();
    	HashSet<String> set = new HashSet<String>();
    	set.add("call1");
    	set.add("call4");
    	// act
        long term = callInfoDao.countByCallState(CallState.Terminated, set);
        long conn = callInfoDao.countByCallState(CallState.Connected, set);

        // assert
        assertEquals(1, conn);
        assertEquals(1, term);
    }

    // tests that we get the correct number of calls by selecting by state - no filtering ids are present
    @Test
    public void testCountByStateWithNotMatchingDataInSet(){
    	// set
    	populateCallInfoCollectionForCountByState();
    	HashSet<String> set = new HashSet<String>();
    	set.add("call1x");
    	set.add("call4x");
    	// act
        long term = callInfoDao.countByCallState(CallState.Terminated, set);
        long conn = callInfoDao.countByCallState(CallState.Connected, set);

        // assert
        assertEquals(0, conn);
        assertEquals(0, term);
    }

    // tests that we get 0 calls by selecting by state - when state is null
    @Test
    public void testCountByStateWithNullState(){
    	assertEquals(0, callInfoDao.countByCallState(null, null));
    	HashSet<String> set = new HashSet<String>();
    	assertEquals(0, callInfoDao.countByCallState(null, set));
    	set.add("call1x");
    	set.add("call4x");
    	assertEquals(0, callInfoDao.countByCallState(null, set));
    }

	private void populateCallInfoCollectionForCountByState() {
        CallInfo callInfo1 = new ThirdPartyCallInfo("unittest", "call1", "d1", "d2", AutoTerminateAction.False, 1);
        callInfo1.setCallState(CallState.Connected);
        callInfoDao.create(callInfo1);
        CallInfo callInfo2 = new ThirdPartyCallInfo("unittest", "call2", "d1", "d2", AutoTerminateAction.False, 1);
        callInfo2.setCallState(CallState.Connected);
        callInfoDao.create(callInfo2);
        CallInfo callInfo3 = new ThirdPartyCallInfo("unittest", "call3", "d1", "d2", AutoTerminateAction.False, 1);
        callInfo3.setCallState(CallState.Terminated);
        callInfoDao.create(callInfo3);
        CallInfo callInfo4 = new ThirdPartyCallInfo("unittest", "call4", "d1", "d2", AutoTerminateAction.False, 1);
        callInfo4.setCallState(CallState.Terminated);
        callInfoDao.create(callInfo4);
	}
}
