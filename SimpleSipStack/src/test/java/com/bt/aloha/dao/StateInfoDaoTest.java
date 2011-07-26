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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentMap;

import javax.sdp.MediaDescription;
import javax.sdp.SessionDescription;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import com.bt.aloha.collections.database.DatabaseInfoCollectionHousekeepingRowCallBackHandler;
import com.bt.aloha.dao.StateInfoDao;
import com.bt.aloha.dao.StateInfoDaoImpl;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.state.StateInfoBase;
import com.bt.aloha.testing.DbTestCase;
import com.bt.aloha.testing.SimpleTestInfo;
import com.bt.aloha.util.ConcurrentUpdateException;
import com.bt.aloha.util.HousekeeperAware;


public class StateInfoDaoTest extends DbTestCase {
    private static Log log = LogFactory.getLog(StateInfoDaoTest.class);
	private StateInfoDao<DialogInfo> stateInfoDao;
	private DialogInfo dialogInfo;
	private DialogInfo dialogInfo2;
	private String collectionType;

	@Before
	public void setUp() {
		collectionType = "DialogInfo";
		createCollectionsTable();
		clearCollection();
		stateInfoDao = new StateInfoDaoImpl<DialogInfo>(ds);
        dialogInfo = new DialogInfo("dialogId", "beanId", "1.2.3.4");
        dialogInfo2 = new DialogInfo("dialogId2", "beanId", "1.2.3.4");
        dialogInfo.setLocalParty("sip:localParty1@abc.de");
        dialogInfo2.setLocalParty("sip:localParty2@abc.de");
	}
	
	// Check if an info object added to collection is stored correctly in database
	public void testAddInfo() throws Exception {
        // setup

		// act
        stateInfoDao.add(dialogInfo, collectionType);

        // assert
        assertEquals("1", query(String.format("select count(*) from StateInfo where object_type='DialogInfo'")));
        assertEquals(dialogInfo.getId(), query(String.format("select object_id from StateInfo where object_id='dialogId'")));
	}

	// Check if two info objects added to collection are stored correctly in database
    @Test
    public void testAddTwoInfos() throws Exception {
        // setup
        stateInfoDao.add(dialogInfo, collectionType);

        // act
        stateInfoDao.add(dialogInfo2, collectionType);

        // assert
        assertEquals("2", query(String.format("select count(*) from StateInfo where object_type='DialogInfo'")));
        assertEquals(dialogInfo.getId(), query(String.format("select object_id from StateInfo where object_id='dialogId'")));
        assertEquals(dialogInfo2.getId(),query(String.format("select object_id from StateInfo where object_id='dialogId2'")));
    }

    // Test that adding null info object results in an exception
    @Test(expected = IllegalArgumentException.class)
    public void testAddingNullInfo() throws Exception {
        // act
        stateInfoDao.add(null, collectionType);
    }

    // Test that adding info object without identifier results in meaningful exception
    @Test(expected = IllegalArgumentException.class)
    public void testAddingInfoWithNullId() throws Exception {
        // setup
    	dialogInfo = new DialogInfo(null, "beanId", "1.2.3.4");

        // act
        stateInfoDao.add(dialogInfo, collectionType);
    }
    
    // test that adding null collection type results in an exception
    @Test(expected = IllegalArgumentException.class)
    public void testAddingInfoWithNullCollectionType() throws Exception {
        // act
        stateInfoDao.add(dialogInfo, null);
    }

    // Test that adding the same info twice results in meaningful exception
    @Test(expected = IllegalArgumentException.class)
    public void testAddingInfoTwice() throws Exception {
        // setup
    	stateInfoDao.add(dialogInfo, collectionType);

    	// act
        stateInfoDao.add(dialogInfo, collectionType);
    }

    // Test retrieving previously serialized info object
    @Test
    public void testGetInfo() throws Exception {
        // setup
        stateInfoDao.add(dialogInfo, collectionType);
    
        // act
        StateInfoBase<DialogInfo> retrievedDialogInfo = stateInfoDao.get(dialogInfo.getId());
        
        // assert
        assertEquals(dialogInfo.getId(), retrievedDialogInfo.getId());
    }

    // Test retrieving previously serialized info object when there is more than one info in the collection
    @Test
    public void testGetWhenTwoInfosAreInCollection() throws Exception {
        // setup
        stateInfoDao.add(dialogInfo, collectionType);
        stateInfoDao.add(dialogInfo2, collectionType);

        // act
        StateInfoBase<DialogInfo> retrievedDialogInfo = stateInfoDao.get(dialogInfo.getId());
        
        // assert
        assertEquals(dialogInfo.getId(), retrievedDialogInfo.getId());
    }

    // Test that retrieving an info object using null identifier results in an exception
    @Test(expected = IllegalArgumentException.class)
    public void testGetWithNullId() throws Exception {
    	// act
        stateInfoDao.get(null);
    }

    // Test that retrieving an info object which doesn't exist results in null being returned
    @Test
    public void testGetNonExisting() throws Exception {
        // act
        assertNull(stateInfoDao.get("non existing"));
    }

    // Test removing an info object from collection
    @Test
    public void testRemoveInfo() throws Exception {
        // setup
        stateInfoDao.add(dialogInfo, collectionType);
        
        // act
        stateInfoDao.remove(dialogInfo.getId());
        
        // assert
        assertEquals("0", query(String.format("select count(*) from StateInfo where object_type='DialogInfo'")));
    }

    // Test removing an info object from collection
    @Test
    public void testRemoveWhenTwoInfosInCollection() throws Exception {
        // setup
        stateInfoDao.add(dialogInfo, collectionType);
        stateInfoDao.add(dialogInfo2, collectionType);

        // act
        stateInfoDao.remove(dialogInfo.getId());

        // assert
        assertEquals("1", query(String.format("select count(*) from StateInfo where object_type='DialogInfo'")));
        assertEquals(dialogInfo2.getId(), query(String.format("select object_id from StateInfo where object_type='DialogInfo'")));
    }

    // Test that removing an info object using null identifier results in an exception
    @Test(expected = IllegalArgumentException.class)
    public void testRemoveWithNullId() throws Exception {
        // act
        stateInfoDao.remove(null);
    }

    // Test that removing an info object doesn't throw exception
    @Test
    public void testRemoveNonExisting() throws Exception {
        // act
    	try {
    		stateInfoDao.remove("non existing");
    	} catch(RuntimeException e) {
    		fail("Unexpected exception");
    	}
    }

    // Test replacing an info object in collection
    @Test
    public void testReplace() throws Exception {
        // setup
        stateInfoDao.add(dialogInfo, collectionType);
        dialogInfo.setLocalParty("sip:newLocalParty1@abc.de");
        
        // act
        stateInfoDao.replace(dialogInfo);
        
        // assert
        assertEquals("sip:newLocalParty1@abc.de", stateInfoDao.get(dialogInfo.getId()).getLocalParty().getURI().toString());
    }

    // Test replacing an info object when there are multiple info objects in collection
    @Test
    public void testReplaceWhenTwoInfosInCollection() throws Exception {
        // setup
        stateInfoDao.add(dialogInfo, collectionType);
        stateInfoDao.add(dialogInfo2, collectionType);
        dialogInfo.setLocalParty("sip:newLocalParty1@abc.de");
        
        // act
        stateInfoDao.replace(dialogInfo);

        // assert
        assertEquals("sip:newLocalParty1@abc.de", stateInfoDao.get(dialogInfo.getId()).getLocalParty().getURI().toString());
        assertEquals("sip:localParty2@abc.de", stateInfoDao.get(dialogInfo2.getId()).getLocalParty().getURI().toString());
    }

    // Test that replacing an info object using null identifier results in an exception
    @Test(expected = IllegalArgumentException.class)
    public void testReplaceWithNullId() throws Exception {
        // act
        stateInfoDao.replace(null);
    }

    // Test concurrent modification
    @Test(expected = ConcurrentUpdateException.class)
    public void testReplaceWithConcurrentModification() throws Exception {
        // setup
        stateInfoDao.add(dialogInfo, collectionType);
        DialogInfo ver1 = stateInfoDao.get(dialogInfo.getId());
        DialogInfo ver2 = stateInfoDao.get(dialogInfo.getId());
        stateInfoDao.replace(ver2);

        // act
        stateInfoDao.replace(ver1);
    }

    // Test size getter
    @Test
    public void testSize() throws Exception {
        // setup/act/assert
        assertEquals(0, stateInfoDao.size(collectionType));
        stateInfoDao.add(dialogInfo, collectionType);
        assertEquals(1, stateInfoDao.size(collectionType));
        stateInfoDao.add(dialogInfo2, collectionType);
        assertEquals(2, stateInfoDao.size(collectionType));
    }

    // test the getAll method
    @Test
    public void testGetAll() throws Exception {
        // setup
    	stateInfoDao.add(dialogInfo, collectionType);
    	stateInfoDao.add(dialogInfo2, collectionType);

        // act
        ConcurrentMap<String, DialogInfo> result = stateInfoDao.getAll(collectionType);

        // assert
        assertEquals(2, result.size());
        assertNotNull(result.get(dialogInfo.getId()));
        assertNotNull(result.get(dialogInfo2.getId()));
    }

    /**
     * this test checks that adding an element to a collection correctly stores
     * the data in the DB. we check this by going directly to the db and
     * deserialize the data in the selected row.
     */
    @Test
    public void testPersistObjectInCollection() throws Exception {
        StateInfoDao<SimpleTestInfo> simpleStateInfoDao = new StateInfoDaoImpl<SimpleTestInfo>(ds);
        SimpleTestInfo myInfo1 = new SimpleTestInfo("id1", "one", "two");

        // act
        simpleStateInfoDao.add(myInfo1, "SimpleTestInfo");

        // assert
        List<SimpleTestInfo> l = retrieveSimpleTestInfo();
        assertEquals(1, l.size());
        SimpleTestInfo retrievedInfo = l.get(0);
        assertEquals("id1", retrievedInfo.getId());
        assertEquals("one", retrievedInfo.getF1());
    }

    /**
     * this test checks that getting an element from the db into a collection
     * correctly loads the db. we check this by setting some known data in the
     * db and the getting it out of the collection
     */
    @Test
    public void testRetrieveObjectFromDatabase() {
        // set
        SimpleTestInfo stored = insertSimpleTestInfo("idX", "f1X", "f2X");
        StateInfoDao<SimpleTestInfo> simpleStateInfoDao = new StateInfoDaoImpl<SimpleTestInfo>(ds);

        // act
        SimpleTestInfo sti = simpleStateInfoDao.get("idX");
        
        // assert
        assertEquals("idX", stored.getId());
        assertEquals("idX", sti.getId());
        assertEquals("f1X", sti.getF1());
    }

    private List<SimpleTestInfo> retrieveSimpleTestInfo() {
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            List<SimpleTestInfo> l = new ArrayList<SimpleTestInfo>();
            statement = connection.prepareStatement("select * from StateInfo where object_type='SimpleTestInfo'");
            rs = statement.executeQuery();
            while (rs.next()) {
                byte[] bytes = rs.getBytes("object_value");
                ObjectInputStream oip = new ObjectInputStream(new ByteArrayInputStream(bytes));
                SimpleTestInfo info = (SimpleTestInfo) oip.readObject();
                l.add(info);
            }
            return l;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Unable to retrieve from SimpleTestInfo");
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to create ObjectInputStream");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to read object");
        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (SQLException e) {
                }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    private SimpleTestInfo insertSimpleTestInfo(String id, String f1,
            String f2) {
        SimpleTestInfo sti = new SimpleTestInfo(id, f1, f2);
        byte[] stiBytes = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(sti);
            oos.flush();
            oos.close();
            stiBytes = bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Unable to serialize SimpleTestInfo", e);
        }
        PreparedStatement s = null;
        try {
            s = connection.prepareStatement("insert into StateInfo"
                            + "(object_id, object_type, object_version, last_use_time, is_dead, force_housekeep, object_value) values(?, 'Collection', ?, ?, ?, 0, ?)");
            s.setString(1, id);
            s.setString(2, "1");
            s.setLong(3, new java.util.Date().getTime());
            s.setInt(4, 0);
            s.setBytes(5, stiBytes);
            log.debug("Inserted row in Collection "
                    + "for current SimpleTestInfo");
            s.execute();
            connection.commit();
        } catch (SQLException e) {
            try {
                if (connection != null)
                    connection.rollback();
            } catch (SQLException e1) {
                throw new RuntimeException(
                        "Unable to rollback operation on SimpleTestInfo", e);
            }
            throw new RuntimeException(
                    "Unable to execute db operation on SimpleTestInfo. op rolledback",
                    e);
        } finally {
            if (s != null)
                try {
                    s.close();
                } catch (SQLException e) {
                    log.warn("Unable to close prepared statement", e);
                }
        }
        return sti;
    }

    // test that session description gets serialized and deserialized and the media descriptions still exist.
    @Test
    public void testSessionDescriptionSerializeAndDeserialize() throws Exception {
        // setup
        DialogInfo di1 = new DialogInfo("id", "bean", "1.2.3.4.");
        SessionDescription sessionDescription = SessionDescriptionHelper.createSessionDescription("127.0.0.1", "test");
        Vector<MediaDescription> mediaDescriptions = new Vector<MediaDescription>();
        MediaDescription holdMediaDescription = SessionDescriptionHelper.generateHoldMediaDescription();
        mediaDescriptions.add(holdMediaDescription);
        sessionDescription.setMediaDescriptions(mediaDescriptions);
        di1.setSessionDescription(sessionDescription);
        di1.setRemoteOfferMediaDescription(holdMediaDescription);

        // act
        stateInfoDao.add(di1, collectionType);

        // assert
        DialogInfo di2 = stateInfoDao.get(di1.getId());
        assertNotNull(di2.getSessionDescription());
        assertNotNull(di2.getSessionDescription().getMediaDescriptions(true));
        assertTrue(di2.getSessionDescription().getMediaDescriptions(false).get(0).toString().contains("0.0.0.0"));
        assertNotNull(di2.getRemoteOfferMediaDescription());
        assertEquals(di1.getRemoteOfferMediaDescription().toString(), di2.getRemoteOfferMediaDescription().toString());
    }

    // test that session description gets serialized and deserialized and the media descriptions still exist on a replace in the collection.
    @Test
    public void testSessionDescriptionSerializeAndDeserializeOnReplace() throws Exception {
        // setup
        DialogInfo di1 = new DialogInfo("id", "bean", "1.2.3.4.");
        stateInfoDao.add(di1, collectionType);
        SessionDescription sessionDescription = SessionDescriptionHelper.createSessionDescription("127.0.0.1", "test");
        Vector<MediaDescription> mediaDescriptions = new Vector<MediaDescription>();
        MediaDescription holdMediaDescription = SessionDescriptionHelper.generateHoldMediaDescription();
        mediaDescriptions.add(holdMediaDescription);
        sessionDescription.setMediaDescriptions(mediaDescriptions);
        di1.setSessionDescription(sessionDescription);
        di1.setRemoteOfferMediaDescription(holdMediaDescription);

        // act
        stateInfoDao.replace(di1);

        // assert
        DialogInfo di2 = stateInfoDao.get(di1.getId());
        assertNotNull(di2.getSessionDescription());
        assertNotNull(di2.getSessionDescription().getMediaDescriptions(true));
        assertTrue(di2.getSessionDescription().getMediaDescriptions(false).get(0).toString().contains("0.0.0.0"));
        assertNotNull(di2.getRemoteOfferMediaDescription());
        assertEquals(di1.getRemoteOfferMediaDescription().toString(), di2.getRemoteOfferMediaDescription().toString());
    }

    // setup the table to have rows that have expired but are not marked for termination
    // force housekeeping on them and then assert they are removed on the next pass
    @Test
    public void testHouseKeeperForced() throws Exception {
        // setup - call dialogs
        dialogInfo.setDialogState(DialogState.Terminated);
        dialogInfo2.setDialogState(DialogState.Terminated);
        DialogInfo dialogInfo3 = new DialogInfo("id3", "bean3", "1.2.3.4");
        dialogInfo3.setDialogState(DialogState.Created);
        stateInfoDao.add(dialogInfo, collectionType);
        stateInfoDao.add(dialogInfo2, collectionType);
        stateInfoDao.add(dialogInfo3, collectionType);
        // setup - housekeeper bean
        HousekeeperAware outboundCallLegBean = EasyMock.createMock(HousekeeperAware.class);
        outboundCallLegBean.killHousekeeperCandidate(dialogInfo3.getId());
        EasyMock.replay(outboundCallLegBean);
        // set up - row call back handler
        DatabaseInfoCollectionHousekeepingRowCallBackHandler handler = new DatabaseInfoCollectionHousekeepingRowCallBackHandler();
        ApplicationContext applicationContext = EasyMock.createMock(ApplicationContext.class);
        EasyMock.expect(applicationContext.getBean("bean3")).andReturn(outboundCallLegBean);
        EasyMock.replay(applicationContext);
        handler.setApplicationContext(applicationContext);
        // setup - collection checks
        assertEquals(3, stateInfoDao.size(collectionType));
        stateInfoDao.housekeep(collectionType, 2000, handler);
        assertEquals(3, stateInfoDao.size(collectionType));
        // act
        // wait for housekeeper to remove the old dialogInfos
        Thread.sleep(3000);
        stateInfoDao.housekeep(collectionType, 2000, handler);
        assertEquals(1, stateInfoDao.size(collectionType));
        stateInfoDao.housekeep(collectionType, 2000, handler);
        // assert
        EasyMock.verify(applicationContext);
        EasyMock.verify(outboundCallLegBean);
        assertEquals(0, stateInfoDao.size(collectionType));
    }
}
