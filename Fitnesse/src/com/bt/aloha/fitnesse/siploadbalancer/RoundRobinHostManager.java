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

 	

 	
 	
 
package com.bt.aloha.fitnesse.siploadbalancer;

import java.util.Vector;

import javax.sip.address.Address;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RoundRobinHostManager {
	private static class InstanceHolder {
		public static RoundRobinHostManager instance = new RoundRobinHostManager();
	}
	
	private Log log = LogFactory.getLog(getClass());
	private int currentHostIndex = 0;
	private Object lock = new Object[0];
	private Vector<Address> hosts = new Vector<Address>();
	
	public static RoundRobinHostManager getInstance() {
		return InstanceHolder.instance;
	}
	
	public void setHosts(Vector<Address> hosts) {
		synchronized(lock) {
			this.hosts = hosts;
		}
	}
	
	public void removeHost(Address address) {
		synchronized (lock) {
			hosts.remove(address);
		}
	}
	
	public void addHost(Address address) {
		synchronized (lock) {
			if (!hosts.contains(address))
				hosts.add(address);
		}
	}
	
	public void setCurrentHostIndex(int hostIndex) {
		synchronized(lock) {
			this.currentHostIndex = hostIndex;
		}
	}
	
	public Address lookupHost() {
		synchronized (lock) {
			if(currentHostIndex > hosts.size()-1)
				currentHostIndex = 0;
			Address next = hosts.get(currentHostIndex);
			log.info(this.getClass().getSimpleName() + ": returning host # " + currentHostIndex + ": " + next.toString());
			currentHostIndex += 1;
			return next;
		}
	}
}
