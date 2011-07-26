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

 	

 	
 	
 
package com.bt.aloha.concurrentcollections.samples;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateManager;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;

/**
 * Unit tests to demonstrate Collection usage
 */
public class Main {

    private TestCollection testCollection;
    private ConcurrentUpdateManager conccurentUpdateManager = new ConcurrentUpdateManagerImpl();

    @Before
    public void setUp() {
        this.testCollection = new TestCollectionImpl();
    }

    @After
    public void tearDown() {
        this.testCollection.destroy();
        this.testCollection = null;
    }

    @Test
    public void testAdd() {
        // setup
        TestInfo t1 = new TestInfo("t1", "test1");

        // act
        this.testCollection.add(t1);

        // assert
        assertEquals(1, this.testCollection.size());
        assertEquals(t1.getData(), this.testCollection.get("t1").getData());
        assertEquals(t1.getId(), this.testCollection.get("t1").getId());
    }

    @Test
    public void testReplace() {
        // setup
        TestInfo t1 = new TestInfo("t1", "test1");
        this.testCollection.add(t1);

        // act
        ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
            public void execute() {
                TestInfo t2 = testCollection.get("t1");
                t2.setData("test2");
                testCollection.replace(t2);
            }

            public String getResourceId() {
                return null;
            }
        };
        this.conccurentUpdateManager.executeConcurrentUpdate(concurrentUpdateBlock);

        // assert
        assertEquals(1, this.testCollection.size());
        assertEquals("test2", this.testCollection.get("t1").getData());
        assertFalse(t1.getVersionId().equals(this.testCollection.get("t1").getVersionId()));
    }
}
