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

import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

public abstract class DaoBase {
    private DataSource dataSource;
    private JdbcTemplate jdbcTemplate;

    public DaoBase(DataSource aDataSource) {
        this.dataSource = aDataSource;
        this.jdbcTemplate = new JdbcTemplate(this.dataSource);
	}
    public void deleteByHousekeeperFlags(long timeThreshold) {
        jdbcTemplate.update(getDeleteByHouseKeeperFlagsSql(), new Object[] { timeThreshold} );
    }

    public void updateByHousekeeperFlags(long timeThreshold, RowCallbackHandler handler) {
        jdbcTemplate.query(getUpdateByHouseKeeperFlagsSql(), new Object[] {timeThreshold}, handler);
    }

    @SuppressWarnings("unchecked")
	public List<String> findByHousekeeperFlags(long timeThreshold) {
    	return jdbcTemplate.queryForList(getFindByHouseKeeperFlagsSql(), new Object[] {timeThreshold}, String.class);
    }

    protected abstract String getDeleteByHouseKeeperFlagsSql();
    protected abstract String getUpdateByHouseKeeperFlagsSql();
    protected abstract String getFindByHouseKeeperFlagsSql();

	protected JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}
}
