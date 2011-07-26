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

import java.util.concurrent.ConcurrentMap;

import com.bt.aloha.collections.memory.InMemoryCollectionImpl;
import com.bt.aloha.util.CollectionAccessInterruptedException;
import com.bt.aloha.util.ConcurrentUpdateException;
import com.bt.aloha.util.OptimisticConcurrencyCollection;

/**
 * Simple collection based on the OptimisticConcurrencyCollection
 */
public class TestCollectionImpl implements TestCollection {

    // could be Spring injected if required
    private OptimisticConcurrencyCollection<TestInfo> collection = new InMemoryCollectionImpl<TestInfo>();

    public void add(TestInfo arg0) throws IllegalArgumentException {
        collection.add(arg0);
    }

    public void destroy() {
        collection.destroy();
    }

    public TestInfo get(String arg0) throws CollectionAccessInterruptedException {
        return collection.get(arg0);
    }

    public ConcurrentMap<String, TestInfo> getAll() {
        return collection.getAll();
    }

    public void init() {
        collection.init();
    }

    public void remove(String arg0) throws CollectionAccessInterruptedException {
        collection.remove(arg0);
    }

    public void replace(TestInfo arg0) throws IllegalArgumentException, CollectionAccessInterruptedException, ConcurrentUpdateException {
        collection.replace(arg0);
    }

    public int size() {
        return collection.size();
    }
}
