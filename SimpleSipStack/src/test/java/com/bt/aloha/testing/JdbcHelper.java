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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcHelper {
    private static final String COLLECTIONS_CREATE_SCRIPT_HSQLDB = "collections.hsqldb.sql";

    private static final String COLLECTIONS_CREATE_SCRIPT_POSTGRES = "collections.postgres.sql";

    private static final String ERROR_CLOSING_S = "Error closing %s";

    private static final String INT_IDENTITY = "int identity";

    private static final String SERIAL = "serial";

    private static Log log = LogFactory.getLog(JdbcHelper.class);

    public JdbcHelper() {
        super();
    }

    private DataSource buildDataSource(String driver, String url, String username, String password) throws Exception {
        log.debug(String.format("Building datasource %s, %s, %s, %s", driver, url, username, password));
        BasicDataSource ds = new BasicDataSource();

        ds.setDriverClassName(driver);
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);

        return ds;
    }

    private String readContent(String filename) {
        Reader r = null;
        BufferedReader br = null;
        InputStream is = null;
        try {
            is = JdbcHelper.class.getClassLoader().getResourceAsStream(filename);
            r = new InputStreamReader(is);
            br = new BufferedReader(r);
            StringBuffer sb = new StringBuffer();
            String line = "";
            while ((line = br.readLine()) != null) {
                if (line.indexOf(SERIAL) != -1)
                    sb.append(line.replaceAll(SERIAL, INT_IDENTITY));
                else
                    sb.append(line);
            }
            String content = sb.toString();
            return content;
        } catch (Throwable e) {
            throw new IllegalArgumentException(String.format("Couldn't read content of create script %s", filename), e);
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (IOException e) {
                log.error(String.format(ERROR_CLOSING_S, br.getClass().getSimpleName()), e);
            }
            try {
                if (r != null)
                    r.close();
            } catch (IOException e) {
                log.error(String.format(ERROR_CLOSING_S, r.getClass().getSimpleName()), e);
            }
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
                log.error(String.format(ERROR_CLOSING_S, is.getClass().getSimpleName()), e);
            }
        }
    }

    protected int executeUpdate(DataSource ds, String createScript) throws SQLException {
        Connection conn = null;
        Statement st = null;
        int rowCount = -1;
        try {
            conn = DataSourceUtils.getConnection(ds);
            st = conn.createStatement();
            rowCount = st.executeUpdate(createScript);
            assert rowCount > 0;
        } catch(SQLException e){
            log.warn("Unable to execute create sctipt: " + e.getMessage());
        } finally {
            try {
                if (st != null)
                    st.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                if (conn != null)
                    DataSourceUtils.releaseConnection(conn, ds);
            }
        }
        return rowCount;
    }
    
    public int clearTable(DataSource ds, String tableName) throws SQLException {
    	return executeUpdate(ds, "delete from " + tableName);
    }

    public DataSource getDefaultDataSource() throws Exception {
        Properties p = new Properties();
        p.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("database.properties"));
        log.debug("Creating default datasource - reading properties from database.properties");
        String driver = p.getProperty("database.driverClassName","org.hsqldb.jdbcDriver");
        String url = p.getProperty("database.url", "jdbc:hsqldb:file:/tmp/ssstestdb2");
        String username = p.getProperty("database.username", "sa");
        String password = p.getProperty("database.password","");
        return buildDataSource(driver, url, username, password);
    }

    public void createCollectionsDefaultSchema(DataSource ds) throws SQLException {
        String createScriptFileName = COLLECTIONS_CREATE_SCRIPT_HSQLDB;
        if(((BasicDataSource)ds).getDriverClassName().contains("postgresql")){
            createScriptFileName = COLLECTIONS_CREATE_SCRIPT_POSTGRES;
        }
        log.debug(String.format("Creating schema using file %s", createScriptFileName));
        String createScript = readContent(createScriptFileName);
        log.debug(createScript);
        executeUpdate(ds, createScript);
    }
}
