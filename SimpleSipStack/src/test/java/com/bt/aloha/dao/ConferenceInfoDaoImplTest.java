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
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.bt.aloha.dao.ConferenceInfoDaoImpl;
import com.bt.aloha.media.conference.state.ConferenceInfo;
import com.bt.aloha.media.conference.state.ConferenceState;
import com.bt.aloha.media.conference.state.ConferenceTerminationCause;
import com.bt.aloha.media.conference.state.ParticipantState;
import com.bt.aloha.testing.DbTestCase;
import com.bt.aloha.util.ConcurrentUpdateException;

public class ConferenceInfoDaoImplTest extends DbTestCase {

    private ConferenceInfoDaoImpl conferenceInfoDao;
    private List<String> conferenceIds = new Vector<String>();

    class MyRowCallbackHandler implements RowCallbackHandler {

		public void processRow(ResultSet arg0) throws SQLException {
			conferenceIds.add(arg0.getString("conferenceid"));
		}
    }

    @Before
    public void before() throws Exception {
        super.before();

        super.createConferenceInfoTable();
        cleanConferenceInfoTable();

        this.conferenceInfoDao = new ConferenceInfoDaoImpl(ds);
        conferenceIds = new Vector<String>();
    }

    @After
    public void cleanDb(){
    	cleanConferenceInfoTable();
    }

    @Test
    public void testCreateSingle() throws Exception {
        // setup
        ConferenceInfo conferenceInfo = new ConferenceInfo("unittest", "123.123.123.123:5060", 5, 5);
        conferenceInfo.getParticipants().put("p1", ParticipantState.Connected);
        conferenceInfo.getParticipants().put("p2", ParticipantState.Disconnected);
        
        // act
        conferenceInfoDao.create(conferenceInfo);

        // assert
        assertEquals("1", query(String.format("select count(*) from ConferenceInfo")));

        String select = query(String.format("select simpleSipBeanId, mediaServerAddress, maxNumberOfParticipants, maxDurationInMinutes, conferenceState, participants from ConferenceInfo"));
        assertTrue(select.contains("unittest 123.123.123.123:5060 5 5 Initial"));
        assertTrue(select.contains("p1=Connected"));
        assertTrue(select.contains("p2=Disconnected"));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateAlreadyExists(){
        // setup
        ConferenceInfo conferenceInfo = new ConferenceInfo("unittest", "123.123.123.123:5060", 5, 5);
        conferenceInfo.getParticipants().put("p1", ParticipantState.Connected);
        conferenceInfo.getParticipants().put("p2", ParticipantState.Disconnected);
        conferenceInfo.setId("conf2");
        conferenceInfoDao.create(conferenceInfo);

        //act
        conferenceInfoDao.create(conferenceInfo);
    }
    
    @Test
    public void testRead() throws Exception {
        // setup
        insertSimpleConference("conf2", "{p1=Connected, p2=Disconnected}", 1111111111111L, "Active", false, 1192721830883L, 5);
        
        // act
        ConferenceInfo e = conferenceInfoDao.read("conf2");

        //assert
        assertEquals("conf2", e.getId());
        assertEquals("unittest", e.getSimpleSipBeanId());
        assertEquals(1111111111111L, e.getCreateTime());
        assertEquals("11927218308830.273397", e.getVersionId());
        assertEquals(1192721830883L, e.getLastUsedTime());
        assertEquals(2222222222222L, e.getStartTime());
        assertEquals(3333333333333L, e.getEndTime());
        assertEquals("123.123.123.133:5060", e.getMediaServerAddress());
        assertEquals(ConferenceState.Active, e.getConferenceState());
        assertEquals(ConferenceTerminationCause.EndedByApplication, e.getConferenceTerminationCause());
        assertEquals(5, e.getMaxNumberOfParticipants());
        assertEquals(5, e.getMaxDurationInMinutes());
        assertFalse(e.isHousekeepForced());
        assertEquals(2, e.getParticipants().size());
        assertEquals(ParticipantState.Connected, e.getParticipantState("p1"));
        assertEquals(ParticipantState.Disconnected, e.getParticipantState("p2"));
    }
    
    private void insertSimpleConference() throws Exception {
    	insertSimpleConference("conf2");
    }

    private void insertSimpleConference(String confId) throws Exception {
    	insertSimpleConference(confId, "{p1=Connected, p2=Disconnected}", 1111111111111L);
    }
    
    private void insertSimpleConference(String confId, String participants, long createTime ) throws Exception {
    	insertSimpleConference(confId, participants, createTime, "Initial", false, 1192721830883L, 5);
    }

    private void insertSimpleConference(String confId, String participants, long createTime, String conferenceState, boolean houseKeepForced, long lastUseTime) throws Exception {
    	insertSimpleConference(confId, participants, createTime, conferenceState, houseKeepForced, lastUseTime, 5);
    }
    
    private void insertSimpleConference(String confId, String participants, long createTime, String conferenceState, boolean houseKeepForced, long lastUseTime, int maxDuration) throws Exception {
        String sql = "INSERT INTO conferenceinfo (" +
		  "conferenceId, " + 
		  "simpleSipBeanId ," + 
		  "createTime ," + 
		  "object_version ," + 
		  "last_use_time, " + 
		  "startTime, " + 
		  "endTime, " + 
		  "mediaServerAddress, " + 
		  "conferenceState, " + 
		  "conferenceTerminationCause, " + 
		  "maxNumberOfParticipants, " + 
		  "maxDurationInMinutes, " + 
		  "housekeepingForced, " + 
		  "participants) " +
			"VALUES (" + 
			"'" + confId + "', " +
			"'unittest', " +
			createTime + ", " +
			"'11927218308830.273397', " +
			lastUseTime + ", " +
			"2222222222222, " +
			"3333333333333, " +
			"'123.123.123.133:5060', " +
			"'" + conferenceState + "', " +
			"'EndedByApplication', " +
			"5, " +
			maxDuration + ", " +
			Boolean.toString(houseKeepForced) + ", " +
			"'" + participants + "');";
        update(sql);
    }
    

    @Test
    public void testDelete() throws Exception {
        // setup
        insertSimpleConference();

        // act
        conferenceInfoDao.delete("conf2");

        // assert
        String select = query(String.format("select * from ConferenceInfo where conferenceId = 'conf2'"));
        assertTrue(select.length() < 1);
    }

    // ensure we don't get an exception when we remove
    // a non existent record
    @Test 
    public void testDeleteNonExistentCall(){
    	conferenceInfoDao.delete("callnotexist");
    }

    @Test
    public void testUpdate() throws Exception {
        // setup
    	String oldVersionId = "11927218308830.273397";
        long lastUseTime =    1111111111111L;

        String sql = "INSERT INTO conferenceinfo (" +
		  "conferenceId, " + 
		  "simpleSipBeanId ," + 
		  "createTime ," + 
		  "object_version ," + 
		  "last_use_time, " + 
		  "startTime, " + 
		  "endTime, " + 
		  "mediaServerAddress, " + 
		  "conferenceState, " + 
		  "conferenceTerminationCause, " + 
		  "maxNumberOfParticipants, " + 
		  "maxDurationInMinutes, " + 
		  "housekeepingForced, " + 
		  "participants) " +
			"VALUES (" + 
			"'conf2', " +
			"'unittest', " +
			"1111111111111, " +
			"'" + oldVersionId + "', " +
			lastUseTime + ", " +
			"2222222222222, " +
			"3333333333333, " +
			"'123.123.123.133:5060', " +
			"'Initial', " +
			"'EndedByApplication', " +
			"5, " +
			"5, " +
			"false, " +
			"'{p1=Connected, p2=Disconnected}');";
        update(sql);

        // act
        ConferenceInfo conferenceInfo = new ConferenceInfo("unittest1", "124.124.124.124:5060", 6, 7);
        conferenceInfo.setStartTime(555555555L);
        conferenceInfo.setId("conf2");
        conferenceInfo.updateConferenceState(ConferenceState.Active);
        conferenceInfo.setEndTime(4444444444L);
        conferenceInfo.setConferenceTerminationCause(ConferenceTerminationCause.Housekept);
        conferenceInfo.setHousekeepForced(true);
        conferenceInfo.getParticipants().put("p3", ParticipantState.Connected);
        conferenceInfo.getParticipants().put("p4", ParticipantState.Disconnected);
        conferenceInfo.setVersionId(oldVersionId);
        
      	conferenceInfoDao.update(conferenceInfo);

        // assert
        String select = query(String.format(
      		  "select conferenceId, " + 
    		  "simpleSipBeanId ," + 
    		  "createTime ," + 
    		  "object_version ," + 
    		  "last_use_time, " + 
    		  "startTime, " + 
    		  "endTime, " + 
    		  "mediaServerAddress, " + 
    		  "conferenceState, " + 
    		  "conferenceTerminationCause, " + 
    		  "maxNumberOfParticipants, " + 
    		  "maxDurationInMinutes, " + 
    		  "housekeepingForced, " + 
    		  "participants " +
        		" from ConferenceInfo where conferenceId = '%s'", "conf2"));

        assertEquals("conf2", select.split(" ")[0]);
        assertEquals("unittest1", select.split(" ")[1]);
        assertEquals(Long.toString(conferenceInfo.getCreateTime()), select.split(" ")[2]);
        assertFalse(oldVersionId.equals(select.split(" ")[3]));
        long newLastUseTime = Long.parseLong(select.split(" ")[4]);
        assertTrue(newLastUseTime > lastUseTime);
        assertEquals("555555555", select.split(" ")[5]);
        assertEquals("4444444444", select.split(" ")[6]);
        assertEquals("124.124.124.124:5060", select.split(" ")[7]);
        assertEquals("Active", select.split(" ")[8]);
        assertEquals("Housekept", select.split(" ")[9]);
        assertEquals("6", select.split(" ")[10]);
        assertEquals("7", select.split(" ")[11]);
        assertEquals("true", select.split(" ")[12]);
        assertTrue(select.contains("p3=Connected"));
        assertTrue(select.contains("p4=Disconnected"));
    }

    @Test(expected=ConcurrentUpdateException.class)
    public void testUpdateConcurrentUpdateException() throws Exception {
        // setup
    	insertSimpleConference();

        // act
        ConferenceInfo conferenceInfo = new ConferenceInfo("unittest", "124.124.124.124:5060", 6, 7);
        conferenceInfo.setId("conf2");
        conferenceInfo.setVersionId("11927218308830.999999");
        conferenceInfoDao.update(conferenceInfo);
    }

    @Test
    public void testSize() throws Exception {
        // setup
    	insertSimpleConference();
    	insertSimpleConference("conf3");

        // act
        int result = conferenceInfoDao.size();

        // assert
        assertEquals(2, result);
    }

    @Test
    public void testFindConferenceForCallId() throws Exception {
        // setup
    	insertSimpleConference("conf2", "{p1=Connected, p2=Disconnected}", 123456L);
    	insertSimpleConference("conf3", "{p3=Connected, p4=Disconnected}", 555555L);
    	insertSimpleConference("conf4", "{p3=Connected, p5=Disconnected}", 444444L);

        // act
        ConferenceInfo result = conferenceInfoDao.findConferenceForCallId("p3");

        // assert
        assertEquals("conf3", result.getId());
    }

    // try finding a call for a dialog id that doesn't exist
    @Test
    public void testFindConferenceForCallIdNotFound() throws Exception {
    	// act
        ConferenceInfo result = conferenceInfoDao.findConferenceForCallId("xxxxxx");
    	
    	// assert
    	assertNull(result);
    }

    // ensure when null is pass to find a call, that null is returned
    @Test 
    public void testFindCallForDialogIdStringNull(){
    	assertNull(conferenceInfoDao.findConferenceForCallId(null));
    }


    //TODO: which record should we really expect to be returned here?
    @Test
    public void testFindConferenceForCallIdSameCreateTimes() throws Exception {
        // setup
    	insertSimpleConference("conf2", "{p1=Connected, p2=Disconnected}", 123456L);
    	insertSimpleConference("conf3", "{p3=Connected, p4=Disconnected}", 555555L);
    	insertSimpleConference("conf4", "{p3=Connected, p5=Disconnected}", 555555L);

        // act
        ConferenceInfo result = conferenceInfoDao.findConferenceForCallId("p3");

        // assert
        assertTrue(result.getId().equals("conf3") || result.getId().equals("conf4"));
    }

    @Test
    public void testDeleteByHousekeeperFlags() throws Exception {
        // setup
    	setupHousekeepingData();

    	// act
    	conferenceInfoDao.deleteByHousekeeperFlags(3500000L);
    	
    	// assert
        String select = query("select * from ConferenceInfo;");
        System.out.println(select);
        assertTrue(select.contains("conf4"));
        assertTrue(select.contains("conf5"));
        assertTrue(select.contains("conf6"));
        assertEquals(3, conferenceInfoDao.size());
    }

    private void setupHousekeepingData() throws Exception {
    	insertSimpleConference("conf1", "", 11111L, "Active", true, 1000000L);
    	insertSimpleConference("conf2", "", 22222L, "Ended", false, 2000000L);
    	insertSimpleConference("conf3", "", 33333L, "Ended", true, 3000000L);
    	insertSimpleConference("conf4", "", 33333L, "Ended", true, 4000000L);
    	insertSimpleConference("conf5", "", 33333L, "Ended", true, 5000000L);
    	insertSimpleConference("conf6", "", 33333L, "Active", false, 1000000L);
    }
    
    @Test
    public void testUpdateByHousekeeperFlags() throws Exception {
        // setup
    	setupHousekeepingData();

        // act
    	conferenceInfoDao.updateByHousekeeperFlags(3500000L, new MyRowCallbackHandler());

    	// assert
    	assertEquals(2, conferenceIds.size());
    	System.out.println(conferenceIds);
    	assertTrue(conferenceIds.contains("conf1"));
    	assertTrue(conferenceIds.contains("conf6"));
    }

    @Test
    public void testFindByHouseKeeperFlags() throws Exception {
    	// setup
    	setupHousekeepingData();
    	
    	// act
    	List<String> confsToBeDeletedLocally = conferenceInfoDao.findByHousekeeperFlags(3500000L);
    	
    	// assert
    	assertEquals(3, confsToBeDeletedLocally.size());
        assertTrue(confsToBeDeletedLocally.contains("conf1"));
        assertTrue(confsToBeDeletedLocally.contains("conf2"));
        assertTrue(confsToBeDeletedLocally.contains("conf3"));
    }
    
    @Test
    public void testGetAll() throws Exception {
        // setup
    	setupHousekeepingData();

        // act
        ConcurrentMap<String, ConferenceInfo> result = conferenceInfoDao.getAll();

        // assert
        assertEquals(6, result.size());
    }
    
    @Test
    public void testFindConnectedMaxDurationCalls() throws Exception {
        // setup
    	insertSimpleConference("conf1", "", 11111L, "Active", true, 1000000L, 5);
    	insertSimpleConference("conf2", "", 22222L, "Active", false, 2000000L, 0);
    	insertSimpleConference("conf3", "", 33333L, "Ended", true, 3000000L, 5);
    	insertSimpleConference("conf4", "", 33333L, "Ended", true, 4000000L, 0);
    	
        // act
    	ConcurrentMap<String, ConferenceInfo> result = conferenceInfoDao.findConnectedMaxDurationConferences();

        // assert
    	assertEquals(1, result.size());
    	assertTrue(result.containsKey("conf1"));
    }
}
