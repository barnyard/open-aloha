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

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

public class MapHousekeeperTest {
	
	private MapHousekeeper mapHousekeeper;
	private Map<String, Future<?>> conferenceFutures;
	private Map<String, String> conferences;
	Future<?> mockFuture;

	@Before
	public void setUp() {
		this.mapHousekeeper = new MapHousekeeper();
		this.conferenceFutures = new Hashtable<String, Future<?>>();
		mapHousekeeper.setConferenceFutures(this.conferenceFutures);
		this.conferences = new Hashtable<String, String>();
		mapHousekeeper.setConferences(this.conferences);
		this.mockFuture = EasyMock.createNiceMock(ScheduledFuture.class);
	}

	@Test
	public void testRunRemoveYesterday() {
		// setup
		Calendar now = Calendar.getInstance();
		String key = String.format("test:1805:%04d:%02d:%02d", now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH) - 1 );
		this.conferenceFutures.put(key, this.mockFuture);
		this.conferences.put("test", key);
		
		// act
		mapHousekeeper.run();
		
		// assert
		assertEquals(0, this.conferenceFutures.size());
		assertEquals(0, this.conferences.size());
	}

	@Test
	public void testRunDontRemoveToday() {
		// setup
		Calendar now = Calendar.getInstance();
		String key = String.format("test:1805:%04d:%02d:%02d", now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
		this.conferenceFutures.put(key, this.mockFuture);
		this.conferences.put("test", key);
		
		// act
		mapHousekeeper.run();
		
		// assert
		assertEquals(1, this.conferenceFutures.size());
		assertEquals(1, this.conferences.size());
	}
}
