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

import org.springframework.context.support.ClassPathXmlApplicationContext;

import fit.Fixture;

public class CallPersistencyFixtureBase extends Fixture {
	private String callId;
	private String ipAddressPattern;
	private int waitTimeoutSeconds;
	private int jvmDownTime = 0;
	protected ClassPathXmlApplicationContext applicationContext;

	public void setIpAddressPattern(String ipAddressPattern) {
		this.ipAddressPattern = ipAddressPattern;
	}

	public String getIpAddressPattern() {
		return this.ipAddressPattern;
	}

	public void setWaitTimeoutSeconds(int waitTimeoutSeconds) {
		this.waitTimeoutSeconds = waitTimeoutSeconds;
	}

	public void setJvmDownTime(int val) {
		this.jvmDownTime = val;
	}

	public String getCallId() {
		return callId;
	}

	protected void setCallId(String callId) {
		this.callId = callId;
	}

	public int getWaitTimeoutSeconds() {
		return waitTimeoutSeconds;
	}

	public int getJvmDownTime() {
		return jvmDownTime;
	}


}
