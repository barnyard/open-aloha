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
package com.bt.aloha.sipstone;

import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class MaintenanceDao {
    private JdbcTemplate jdbcTemplate;

    public MaintenanceDao(String driver, String url, String uname, String pwd){
    	BasicDataSource ds = new BasicDataSource();
    	ds.setDriverClassName(driver);
    	ds.setUrl(url);
    	ds.setUsername(uname);
    	ds.setPassword(pwd);
    	this.jdbcTemplate = new JdbcTemplate(ds);
    	testConnection();
    }

    public MaintenanceDao() throws Exception {
    	InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("sip.properties");
    	Properties dbProps = new Properties();
    	dbProps.load(is);
    	BasicDataSource ds = new BasicDataSource();
    	ds.setDriverClassName(dbProps.getProperty("database.driverClassName", "org.postgresql.Driver"));
    	ds.setUrl(dbProps.getProperty("database.url", "jdbc:postgresql://localhost:5432/springring"));
    	ds.setUsername(dbProps.getProperty("database.username", "springringuser"));
    	ds.setPassword(dbProps.getProperty("database.password", "springringuser"));
    	this.jdbcTemplate = new JdbcTemplate(ds);
    }

	public void truncateAllTables() {
		truncateTable("callinfo");
		truncateTable("conferenceinfo");
		truncateTable("stateinfo");
	}

	private void testConnection(){
		this.jdbcTemplate.execute("select count(1) from callinfo");
	}

	private void truncateTable(String tablename){
		try {
			this.jdbcTemplate.execute("truncate " + tablename + ";");
		} catch(DataAccessException e){
			e.printStackTrace(System.err);
		}
	}
}
