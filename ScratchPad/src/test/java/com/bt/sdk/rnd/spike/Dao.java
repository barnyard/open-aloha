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
package com.bt.sdk.rnd.spike;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class Dao {
	private JdbcTemplate jdbcTemplate;

	public Dao(JdbcTemplate t) {
		this.jdbcTemplate = t;
	}

	public void update(int id, String newLabel) {
		jdbcTemplate.update("update testtable set label=? where id=?",
				new Object[] { newLabel, id });
	}

	public void truncateTable(){
		try {
			jdbcTemplate.update("delete from testtable;");
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
	}

	public void shutdown(){
		try {
			jdbcTemplate.update("shutdown;");
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
	}

	public boolean tableExists() {
		try{
			jdbcTemplate.query("select count(id) as c from testtable",
				new RowMapper() {
					public Object mapRow(ResultSet rs, int r)
							throws SQLException {
						return new Long(rs.getLong("c"));
					}
				});
		} catch (DataAccessException e){
			return false;
		}
		return true;
	}

	public boolean createTable() {
		try {
			jdbcTemplate.update("create table testtable(id INT not null primary key, label VARCHAR(100));");
			// support table for sequence
			jdbcTemplate.update("create table testtable_seq_support(id INT);");
			// must contain only one row for the 'select next value' to return only one value
			jdbcTemplate.update("insert into testtable_seq_support (id) values (0)");
			jdbcTemplate.execute("create sequence testtable_seq;");
			return true;
		} catch (DataAccessException e) {
			e.printStackTrace();
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public int nextId(){
		List<Integer> result = jdbcTemplate.query("select next value for testtable_seq from testtable_seq_support",
				new RowMapper() {
					public Object mapRow(ResultSet rs, int r)
							throws SQLException {
						return rs.getInt(1);
					}
				});
		// returns as many integers as there are in the support table
		return result.get(0);
	}


	@SuppressWarnings("unchecked")
	public String get(int id) {
		List<String> result = jdbcTemplate.query("select label from testtable where id=?", new Object[] { id },
			new RowMapper() {
				public Object mapRow(ResultSet rs, int r)
						throws SQLException {
					return rs.getString("label");
				}
			});
		if (result.size() == 0)
			throw new IllegalArgumentException("invalid id " + id + ". no record found");
		if (result.size() > 1)
			throw new IllegalArgumentException("invalid id " + id + ". too many records found");
		return result.get(0);
	}

	@SuppressWarnings("unchecked")
	public Map<Integer, String> getAll(){
		List<Object[]> result = jdbcTemplate.query("select id, label from testtable",
				new RowMapper() {
					public Object mapRow(ResultSet rs, int r)
							throws SQLException {
						Object[] data = new Object[2];
						data[0] = Integer.valueOf(rs.getInt("id"));
						data[1] = rs.getString("label");
						return data;
					}
				});
		Map<Integer, String> ret = new HashMap<Integer, String>();
		for(Object[] d : result){
			ret.put((Integer)d[0], (String)d[1]);
		}
		return ret;
	}

	public void insert(int id, String label) {
		jdbcTemplate.update("insert into testtable(id, label) values (?, ?)", new Object[] { id, label });
	}

	public void delete(int id) {
		jdbcTemplate.update("delete from testtable where id=?", new Object[] { id });
	}

}
