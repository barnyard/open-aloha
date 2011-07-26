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

package com.bt.aloha.state;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.util.CloneableObject;
import com.bt.aloha.util.Housekeepable;
import com.bt.aloha.util.Versionable;

public abstract class StateInfoBase<T> implements Housekeepable, Versionable, CloneableObject<T>, Serializable {
    private static final long serialVersionUID = -8906581644352688209L;

    public static final int TIME_NOT_SET = -1;
    protected static final int MILLIS_IN_SEC = 1000;
    private static Log log = LogFactory.getLog(StateInfoBase.class);
    private final String simpleSipBeanId;
    private String id;
    private long createTime;
    private long startTime = TIME_NOT_SET;
    private long endTime = TIME_NOT_SET;
    private long lastUseTime;
    private String versionId;
    private boolean forceHousekeep;

    protected StateInfoBase(String aSimpleSipBeanId) {
        this.simpleSipBeanId = aSimpleSipBeanId;
        this.createTime = Calendar.getInstance().getTimeInMillis();
        updateVersionId();
        updateLastUsedTime();
    }

    // TODO: MEDIUM seems wrong for these to be here rather than in an interface
    // somewhere?
    public abstract Map<String, Object> getTransients();

    public abstract void setTransients(Map<String, Object> m);

    @SuppressWarnings("unchecked")
    public T cloneObject() {
        try {
            return (T) super.clone();
        } catch (CloneNotSupportedException e) {
            // should never happen
            String message = "Unable to clone StateInfoBase";
            log.error(message);
            throw new RuntimeException(message, e);
        }
    }

    public long getLastUsedTime() {
        return lastUseTime;
    }

    public void updateLastUsedTime() {
        this.lastUseTime = System.currentTimeMillis();
    }

    public void setLastUsedTime(long aLastUseTime) {
        this.lastUseTime = aLastUseTime;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String aVersionId) {
        this.versionId = aVersionId;
    }

    public void updateVersionId() {
        this.versionId = generateNewVersionId();
    }

    public String generateNewVersionId() {
        return String.format("%d%f", System.currentTimeMillis(), Math.random());
    }

    public String getId() {
        return id;
    }

    public void setId(String anId) {
        this.id = anId;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long ct) {
        this.createTime = ct;
    }

    public boolean setStartTime(long aStartTime) {
        if (this.startTime == TIME_NOT_SET) {
            this.startTime = aStartTime;
            return true;
        }
        log.debug(String.format("Attempt to set start time again for state object %s (%s to %s)", getId(),
                this.startTime, aStartTime));
        return false;
    }

    public boolean setEndTime(long aEndTime) {
        if (this.endTime == TIME_NOT_SET) {
            this.endTime = aEndTime;
            return true;
        }
        log.debug(String
                .format("Attempt to set end time again for conf %s (%s to %s)", getId(), this.endTime, aEndTime));
        return false;
    }

    public int getDuration() {
        if (startTime == TIME_NOT_SET)
            return 0;
        if (endTime == TIME_NOT_SET)
            return (int) (Calendar.getInstance().getTimeInMillis() - startTime) / MILLIS_IN_SEC;
        return (int) (endTime - startTime) / MILLIS_IN_SEC;
    }

    public abstract boolean isDead();

    public String getSimpleSipBeanId() {
        return simpleSipBeanId;
    }

    public boolean isHousekeepForced() {
        return this.forceHousekeep;
    }

    public void setHousekeepForced(boolean aForceHousekeep) {
        this.forceHousekeep = aForceHousekeep;
    }
}
