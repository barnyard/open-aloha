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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MapHousekeeper implements Runnable {
	private Log log = LogFactory.getLog(this.getClass());
	private Map<String, Future<?>> conferenceFutures;// = new Hashtable<String, Future<?>>();
	private Map<String, String> conferences;// = new Hashtable<String, String>();
	private List<String> keysToRemove = new ArrayList<String>();
	
	public void run() {
		log.info("housekeeping Maps");
		this.keysToRemove = new ArrayList<String>();
		housekeepConferenceFutures();
		housekeepConferences();
	}

	private void housekeepConferenceFutures() {
		for (String key: conferenceFutures.keySet()) {
			// Test conference 3:1905:2008:01:07
			String[] parts = key.split(":");
			Calendar now = Calendar.getInstance();
			Calendar then = (Calendar) now.clone();
			then.set(Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
			//System.out.println(now);
			//System.out.println(then);
			if (then.before(now)) {
				log.info(String.format("removing old conferenceFuture: %s", key));
				conferenceFutures.remove(key);
				keysToRemove.add(key);
			}
		}
	}

	private void housekeepConferences() {
		for (String key: this.conferences.keySet()) {
			if (this.keysToRemove.contains(this.conferences.get(key))) {
				log.info(String.format("removing old conference: %s", key));
				this.conferences.remove(key);
			}
		}
	}
	
	public void setConferenceFutures(Map<String, Future<?>> conferenceFutures) {
		this.conferenceFutures = conferenceFutures;
	}
	
	public void setConferences(Map<String, String> conferences) {
		this.conferences = conferences;
	}
}
