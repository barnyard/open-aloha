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
package com.bt.sdk.callcontrol.demo.standup;

import java.util.Calendar;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

public class MainBean implements Runnable {
	private final static String SELECT = "select name, time from conferences where time > ? and time < ?";
	private JdbcTemplate jdbcTemplate;
	private RowCallbackHandler rowCallbackHandler;
	
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}
	
	public void run() {
		Calendar now = Calendar.getInstance();
		String timeNow = String.format("%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
		Calendar fiveMinutesAhead = Calendar.getInstance(); 
		fiveMinutesAhead.add(Calendar.MINUTE, 10);
		String timeFiveMinutesAhead = String.format("%02d:%02d", fiveMinutesAhead.get(Calendar.HOUR_OF_DAY), fiveMinutesAhead.get(Calendar.MINUTE));
		Object[] args = new Object[] {timeNow ,timeFiveMinutesAhead};
		this.jdbcTemplate.query(SELECT, args, rowCallbackHandler);
	}

	public void setRowCallbackHandler(RowCallbackHandler rowCallbackHandler) {
		this.rowCallbackHandler = rowCallbackHandler;
	}
}
