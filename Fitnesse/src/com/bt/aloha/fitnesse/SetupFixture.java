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

 	

 	
 	
 
package com.bt.aloha.fitnesse;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import fit.Fixture;

public class SetupFixture extends Fixture {

    private static Log log = LogFactory.getLog(SetupFixture.class);

    private Properties loadDatabaseProperties() {
        try {
            InputStream is = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("database.properties");
            Properties p = new Properties();
            p.load(is);
            return p;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to load properties from 'database.properties'. Maybe it's not in the classpath??");
        }
    }

    private Connection getConnection(Properties p) {
        Connection connection = null;
        String driverName = null;
        try {
            driverName = p.getProperty("sssDatasource.driverClassName");
            String url = p.getProperty("sssDatasource.url");
            String username = p.getProperty("sssDatasource.username");
            String password = p.getProperty("sssDatasource.password");
            // Load the JDBC driver
            Class.forName(driverName);
            // Create a connection to the database
            connection = DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load driver class "
                    + driverName + ". Check the classpath");
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to connect to database", e);
        }
        return connection;
    }

    public int clearDatabaseData(){
        Connection c = getConnection(loadDatabaseProperties());
        Statement statement = null;
        int deletedRows = 0;
        try{
            statement = c.createStatement();
            deletedRows = statement.executeUpdate("delete from StateInfo");
            deletedRows += statement.executeUpdate("delete from callinfo");
            deletedRows += statement.executeUpdate("delete from conferenceinfo");
            return deletedRows;
        }
        catch(SQLException e){
            throw new IllegalStateException("Unable to delete data", e);
        }
        finally{
            if (statement != null){
                try {
                    statement.close();
                } catch (SQLException e) {
                    log.warn(e);
                }
            }
            if (c != null){
                try {
                    c.close();
                } catch (SQLException e) {
                    log.warn(e);
                }
            }
        }
    }

    public static void main(String[] args) {
        new SetupFixture().clearDatabaseData();
    }
}
