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

 	

 	
 	
 
package com.bt.aloha.phones;

import java.util.HashMap;

public class Controller {
    private static Controller theInstance = new Controller();
    private static HashMap<String, SipulatorPhone> phones = new HashMap<String, SipulatorPhone>();
    
    private Controller() {}
    
    public synchronized static Controller getInstance() {
        return theInstance;
    }

    public synchronized void createPhone(String name, String ipAddressPattern, String remoteSipAddress, String remoteSipProxy) throws Exception {
        SipulatorPhone phone = new SipulatorPhone(name, ipAddressPattern, remoteSipAddress, remoteSipProxy);
        phones.put(name, phone);
    }
    
    public synchronized boolean initiateCall(String name) throws Exception {
    	return initiateCall(name, null, null, null);
    }

    public boolean initiateCall(String name, String realm, String username, String password) {
        SipulatorPhone phone = phones.get(name);
       	phone.setCredentials(realm, username, password);
       	phone.start();
        return phone.initiateOutgoingCall();
	}
    
    public synchronized boolean sendInviteOkAck(String name) {
    	return phones.get(name).sendInviteOkAck();
	}
    
    public synchronized boolean respondToBye(String name) {
    	return phones.get(name).respondToBye();
	}
    
    public synchronized boolean waitResponse(String name, int responseCode, long waitTimeout) throws Exception {
        return phones.get(name).waitForResponse(responseCode, waitTimeout);
    }

	public boolean waitAnswer(String name, int responseCode, long waitTimeout) {
		return phones.get(name).waitAnswer(responseCode, waitTimeout);
	}

	public synchronized boolean waitForBye(String name, long waitTimeout) throws Exception {
        return phones.get(name).waitForBye(waitTimeout);
    }
    
    public synchronized boolean listenForIncomingCall(String name) throws Exception {
        return phones.get(name).listenForIncomingCall();
    }
    
    public synchronized boolean waitForInvite(String name, long waitTimeout) throws Exception {
        return phones.get(name).waitForInvite(waitTimeout);
    }
    
    public synchronized boolean waitForAck(String name, long waitTimeout) throws Exception {
        return phones.get(name).waitForAck(waitTimeout);
    }
    
    public synchronized boolean waitForReinviteAndRespond(String name, long waitTimeout, String sipAddress) throws Exception {
        return phones.get(name).waitForReinviteAndRespond(waitTimeout, sipAddress);
    }
    
    public synchronized boolean waitForCancel(String name, long waitTimeout) throws Exception {
        return phones.get(name).waitForCancel(waitTimeout);
    }    
    
    public synchronized boolean sendTrying(String name) throws Exception {
        return phones.get(name).sendTrying();
    }
    
    public synchronized boolean sendRinging(String name) throws Exception {
        return phones.get(name).sendRinging();
    }
    
    public synchronized boolean sendInviteOk(String name) throws Exception {
        return phones.get(name).sendInviteOk();
    }
    
    public synchronized boolean sendBusy(String name) throws Exception {
        return phones.get(name).sendBusy();
    }
    
    public synchronized void stopPhone(String name) throws Exception {
        if ( ! phones.containsKey(name)) {
            SipulatorPhone.log("Phone " + name + " NOT FOUND TO STOP!!");
            return;
        }
        SipulatorPhone phone = phones.get(name);
        phone.command("stop");
        phone.join(5000);
        phones.remove(name);
    }
}
