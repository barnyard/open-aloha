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

import java.util.concurrent.CountDownLatch;

public class CallPersistencyFixture extends CallPersistencyFixtureBase {
	private String firstPhoneUri;
	private String secondPhoneUri;
	private OutboundCallFixture outboundCallFixture;

	static {
		// make sure to load this app ctx
		FixtureApplicationContexts.getInstance().startMockphonesApplicationContext();
	}

	public CallPersistencyFixture() {
	}

	public String connectCall() {
		setCallId(connectCall(getIpAddressPattern(), firstPhoneUri,
				secondPhoneUri));
		return getCallId();
	}

	public String connectCallWithOneMinuteDuration() {
		setCallId(connectCallWithOneMinuteDuration(getIpAddressPattern(),
				firstPhoneUri, secondPhoneUri));
		return getCallId();
	}

	public String terminateCall() {
		if (getCallId() == null)
			return "callId is null - maybe the call hasn't been initiated!";
		String ret = null;
		try {
			ret = terminateCall(getIpAddressPattern(), getCallId());
		} catch (Exception e) {
			e.printStackTrace();
			return "call not terminated [waitForTerminate:" + ret + "]: "
					+ e.getMessage();
		}
		if (ret.equals("OK"))
			return "OK";
		return "call alredy terminated or failed to receive terminate event";
	}

	private String terminateCall(String ipAddressPattern, String callId) {
		outboundCallFixture.terminateCall();
		return "OK";
	}

	public String waitForCallTerminatedEventWithTerminatedByApplication()
			throws Exception {
		return outboundCallFixture.waitForCallTerminatedEventWithTerminatedByApplication();
	}

	public String waitForCallTerminatedEventWithMaxCallDurationExceeded() throws Exception {
		return outboundCallFixture.waitForCallTerminatedEventWithMaxCallDurationExceeded();
	}

	public String waitForCallDisconnectedEventWithSecondRemotePartyHungUp() throws Exception {
		return outboundCallFixture
				.waitForCallDisconnectedEventWithSecondRemotePartyHungUp();
	}

	public void destroyPersistencyApplicationContext() {
		FixtureApplicationContexts.getInstance().destroyPersistencyApplicationContext();
	}

	public String startApplicationContext() {
		applicationContext = FixtureApplicationContexts.getInstance().startPersistencyApplicationContext();
		return "OK";
	}

	public String destroyAndStartApplicationContext() {
		applicationContext = FixtureApplicationContexts.getInstance().startPersistencyApplicationContext(true, getJvmDownTime());
		outboundCallFixture = new OutboundCallFixture(applicationContext);
		outboundCallFixture.ipAddressPattern(getIpAddressPattern());
		outboundCallFixture.waitTimeoutSeconds(getWaitTimeoutSeconds());
		if (getCallId() != null)
			outboundCallFixture.callIds.add(getCallId());
		outboundCallFixture.latch = new CountDownLatch(0);
        
        return "OK";
	}

	private String connectCall(String ipAddressPattern, String sip1, String sip2) {
		setupFixtureCall(ipAddressPattern, sip1, sip2);
		outboundCallFixture.joinDialogsOneAndTwo();
		return outboundCallFixture.getActiveCallId();
	}

	private String connectCallWithOneMinuteDuration(String ipAddressPattern,
			String sip1, String sip2) {
		setupFixtureCall(ipAddressPattern, sip1, sip2);
		outboundCallFixture.joinDialogsOneAndTwoWithOneMinuteDuration();
		return outboundCallFixture.getActiveCallId();
	}

	private void setupFixtureCall(String ipAddressPattern, String sip1,
			String sip2) {
		outboundCallFixture = new OutboundCallFixture(applicationContext);
		outboundCallFixture.ipAddressPattern(ipAddressPattern);
		outboundCallFixture.firstPhoneUri(sip1);
		outboundCallFixture.secondPhoneUri(sip2);
		outboundCallFixture.waitTimeoutSeconds(getWaitTimeoutSeconds());
		outboundCallFixture.createFirstDialog();
		outboundCallFixture.createSecondDialog();
	}

	public String waitForCallConnectedEvent() {
		try {
			return outboundCallFixture.waitForCallConnectedEvent();
		} catch (Exception e) {
			return "Exception: " + e.getMessage();
		}
	}

	public String callStatus() {
		return outboundCallFixture.callStatus();
	}

	public void setWaitTimeoutSeconds(int waitTimeoutSeconds) {
		super.setWaitTimeoutSeconds(waitTimeoutSeconds);
		if (outboundCallFixture != null)
			outboundCallFixture.waitTimeoutSeconds(waitTimeoutSeconds);
	}

	public void setFirstPhoneUri(String firstPhoneUri) {
		this.firstPhoneUri = firstPhoneUri;
	}

	public void setSecondPhoneUri(String secondPhoneUri) {
		this.secondPhoneUri = secondPhoneUri;
	}
}
