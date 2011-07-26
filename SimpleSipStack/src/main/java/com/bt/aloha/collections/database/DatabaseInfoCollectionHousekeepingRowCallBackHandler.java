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

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.bt.aloha.state.StateInfoBase;
import com.bt.aloha.util.HousekeeperAware;
import com.bt.aloha.util.ObjectSerialiser;

public class DatabaseInfoCollectionHousekeepingRowCallBackHandler implements RowCallbackHandler, ApplicationContextAware {

    private ApplicationContext applicationContext;
    private Log log = LogFactory.getLog(this.getClass());

    public DatabaseInfoCollectionHousekeepingRowCallBackHandler() {}

    @SuppressWarnings("unchecked")
    public void processRow(ResultSet resultSet) throws SQLException {

        String objectId = null;
        try {
            objectId = resultSet.getString("object_id");
            byte[] bytes = resultSet.getBytes("object_value");
            Object deserialised = new ObjectSerialiser().deserialise(bytes);
            String beanName = ((StateInfoBase)deserialised).getSimpleSipBeanId();
            log.info(String.format("Housekeeping: preparing an dialogInfo %s for being housekept by %s", objectId, beanName));
            HousekeeperAware creatorBean = (HousekeeperAware) applicationContext.getBean(beanName);
            creatorBean.killHousekeeperCandidate(objectId);
        } catch (Throwable t) {
            log.error(String.format("Unable to kill housekeeper candidate %s...will still remove from collection next housekeep", objectId), t);
        }
    }

    public void setApplicationContext(ApplicationContext anApplicationContext) {
        this.applicationContext = anApplicationContext;
    }
}
