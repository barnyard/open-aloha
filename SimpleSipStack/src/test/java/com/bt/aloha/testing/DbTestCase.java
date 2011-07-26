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

 	

 	
 	
 
package com.bt.aloha.testing;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.springframework.jdbc.datasource.DataSourceUtils;


public abstract class DbTestCase {
    protected DataSource ds;
    protected Connection connection;
    private Log log = LogFactory.getLog(this.getClass());
    protected JdbcHelper jdbcHelper = new JdbcHelper();

    @Before
    public void before() throws Exception {
        log.debug("Before DbTestCase: initiating datasource and connection to database");
        if(ds==null){
            ds = jdbcHelper.getDefaultDataSource();
        }
        connection = DataSourceUtils.doGetConnection(ds);
    }


    protected void cleanCallInfoTable() {
        try {
            log.info("Deleted #" + jdbcHelper.executeUpdate(ds, "DELETE FROM CALLINFO"));
        } catch (SQLException e) {
            log.warn("error clearing CALLINFO table", e);
        }
    }

    protected void cleanConferenceInfoTable() {
        try {
            log.info("Deleted #" + jdbcHelper.executeUpdate(ds, "DELETE FROM CONFERENCEINFO"));
        } catch (SQLException e) {
            log.warn("error clearing CONFERENCEINFO table", e);
        }
    }


    @After
    public void after() throws Exception {
        log.debug("After DbTestCase");
        if(connection!=null)
            DataSourceUtils.releaseConnection(connection, ds);
    }

    public void clearCollection() {
        try {
            log.debug("Destroying data from StateInfo table");
            int count = jdbcHelper.clearTable(ds, "StateInfo");
            log.debug(String.format("deleted %d rows", count));
        } catch (SQLException e) {
        	log.warn(String.format("Error clearing database: %s", e.getMessage()));
        }
    }

    public void createCollectionsTable(){
        // Create schema if does not exist
        try {
            jdbcHelper.createCollectionsDefaultSchema(ds);
        } catch (SQLException e) {
            log.error(String.format("Error when creating database schema: %s", e.getMessage()));
        }
    }

    public void createCallInfoTable() {
        // Create schema if does not exist
        try {
            jdbcHelper.createCollectionsDefaultSchema(ds);
        } catch (SQLException e) {
            log.error(String.format("Error when creating database schema: %s", e.getMessage()));
        }
    }

    public void createConferenceInfoTable() {
        // Create schema if does not exist
        try {
            jdbcHelper.createCollectionsDefaultSchema(ds);
        } catch (SQLException e) {
            log.error(String.format("Error when creating database schema: %s", e.getMessage()));
        }
    }

    protected synchronized int update(String expression) throws SQLException {
        Statement st = connection.createStatement();
        try {
            return st.executeUpdate(expression);
        } finally {
            st.close();
        }
    }

    protected synchronized String query(String expression) throws SQLException {
        Statement st = connection.createStatement();
        try {
            ResultSet rs = st.executeQuery(expression);
            return dump(rs);
        } finally {
            st.close();
        }
    }

    private static String dump(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int colmax = meta.getColumnCount();
        Object o = null;

        StringBuffer sb = new StringBuffer();
        for (; rs.next(); ) {
            for (int i = 0; i < colmax; i++) {
                o = rs.getObject(i + 1);
                if (o != null)
                    sb.append(o.toString());
                else
                    sb.append("null");

                if(i < colmax-1)
                    sb.append(" ");
            }

            if(!rs.isLast())
                sb.append("\n");
        }
        return sb.toString();
    }

}
