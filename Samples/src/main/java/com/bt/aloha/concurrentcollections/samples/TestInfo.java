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

import com.bt.aloha.util.CloneableObject;
import com.bt.aloha.util.Versionable;

/**
 * Simple databag to be stored in the TestCollection
 */
public class TestInfo implements Versionable, CloneableObject<TestInfo> {

    private String id;
    private String versionId;
    private String data;

    public TestInfo(String id, String data) {
        updateVersionId();
        this.data = data;
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getVersionId() {
        return this.versionId;
    }

    public void setVersionId(String arg0) {
        this.versionId = arg0;
    }

    public void updateVersionId() {
        this.versionId = generateNewVersionId();
    }

    private String generateNewVersionId() {
        return String.format("%d%f", System.currentTimeMillis(), Math.random());
    }

    public String getId() {
        return this.id;
    }

    public TestInfo cloneObject() {
        try {
            return (TestInfo)super.clone();
        } catch (CloneNotSupportedException e) {
            // should never happen
            String message = "Unable to clone StateInfoBase";
            throw new RuntimeException(message, e);
        }
    }
}
