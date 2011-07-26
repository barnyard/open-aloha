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

import javax.sip.message.Response;

import com.bt.aloha.phones.Controller;
import com.bt.aloha.phones.SipulatorPhone;

public class CallControlFixture extends SimpleSipStackBaseFixture {
	private String mySipAddress;
	private String remoteSipAddress;
	private String remoteSipProxy;
	private String myPhoneName;
	private String realm;
	private String username;
	private String password;
	protected long waitTimeout = 0;
	
	static{
		FixtureApplicationContexts.getInstance().startMockphonesApplicationContext();
	}
	
	public CallControlFixture(){
		super(FixtureApplicationContexts.getInstance().startApplicationContext());
	}
	
	public void mySipAddress(String value) {
		mySipAddress = value.replaceAll("localhost", SipulatorPhone.lookupIpAddress(null));
	}
	
	public void realm(String r) {
		this.realm = r;
	}

	public void username(String u) {
		this.username = u;
	}

	public void password(String p) {
		this.password = p;
	}
	
	public void remoteSipAddress(String value) {
		remoteSipAddress = getAddressAndPort(value);
	}
	
	public void remoteSipProxy(String value) {
		remoteSipProxy = getAddressAndPort(value);
	}
	
	public void waitTimeout(String value) throws Exception {
		waitTimeout = Long.parseLong(value);
	}
	
	public void createMyPhone() throws Exception {
		mySipAddress = checkAddress(mySipAddress);
	}
	
	public boolean sendInvite() throws Exception {
		return Controller.getInstance().initiateCall(myPhoneName, realm, username, password);
	}
	
	public boolean sendInviteOkAck() throws Exception {
		return Controller.getInstance().sendInviteOkAck(myPhoneName);
	}
	
	public boolean respondToBye() throws Exception {
		return Controller.getInstance().respondToBye(myPhoneName);
	}
	
	public boolean waitResponseServiceUnavailable() throws Exception {
		return Controller.getInstance().waitResponse(myPhoneName, Response.SERVICE_UNAVAILABLE, waitTimeout);
	}
	
	public boolean waitResponseTemporarilyUnavailable() throws Exception {
		return Controller.getInstance().waitResponse(myPhoneName, Response.TEMPORARILY_UNAVAILABLE, waitTimeout);
	}
	
	public boolean waitResponseBusy() throws Exception {
		return Controller.getInstance().waitResponse(myPhoneName, Response.BUSY_HERE, waitTimeout);
	}
	
	public boolean waitResponseError() throws Exception {
		return Controller.getInstance().waitResponse(myPhoneName, Response.SERVER_INTERNAL_ERROR, waitTimeout);
	}
	
	public boolean waitResponseOk() throws Exception {
		return Controller.getInstance().waitResponse(myPhoneName, Response.OK, waitTimeout);
	}
	
	public boolean waitAnswerOk() throws Exception {
		return Controller.getInstance().waitAnswer(myPhoneName, Response.OK, waitTimeout);
	}
	
	public boolean waitResponseRinging() throws Exception {
		return Controller.getInstance().waitResponse(myPhoneName, Response.RINGING, waitTimeout);
	}
	
	public boolean waitForBye() throws Exception {
		return Controller.getInstance().waitForBye(myPhoneName, waitTimeout);
	}
	
	public boolean waitForInvite() throws Exception {
		return Controller.getInstance().waitForInvite(myPhoneName, waitTimeout);
	}
	
    protected String checkAddress(String inputAddress) throws Exception {
        String address = inputAddress.trim();
        
        if (!address.toLowerCase().startsWith("mock:"))
            return address.trim().replaceAll("localhost", SipulatorPhone.lookupIpAddress(ipAddressPattern));

        myPhoneName = getMockPhoneName(address);

        Controller.getInstance().createPhone(myPhoneName, ipAddressPattern, remoteSipAddress, remoteSipProxy);
        return "sip:" + address.substring("mock:".length()).replaceAll("localhost", SipulatorPhone.lookupIpAddress(ipAddressPattern)) + (SipulatorPhone.port() == 5060 ? "" : (":" + SipulatorPhone.port()));
    }
    
    protected String getMockPhoneName(String address) {
        if (!address.toLowerCase().startsWith("mock:"))
        	return null;
        
        int at = address.indexOf("@");
        return address.substring("mock:".length(), at);
    }

    public String tearDown() throws Exception {
        try {
        	Controller.getInstance().stopPhone(myPhoneName);
        } catch (Exception e) {
            return e.getMessage();
        }
        Thread.sleep(1000);
        return "OK";
    }
}
