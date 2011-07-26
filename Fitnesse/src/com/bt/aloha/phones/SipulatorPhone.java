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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.sip.RequestEvent;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.cafesip.sipunit.Credential;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipResponse;
import org.cafesip.sipunit.SipStack;
import org.cafesip.sipunit.SipTransaction;

public class SipulatorPhone extends Thread {
    private volatile boolean stop = false;

    protected static SipStack sipStack;
    protected SipCall call;
    private SipPhone sipPhone;
    
    private String ipAddressPattern;
    private String remoteSipAddress;
    private String remoteSipProxy;

    private static int phonesOnStack = 0;
    private static int portBase = 11980;
    private static final String sdpDataTemplate = 
        "v=0\r\n"
        + "o=- %timestamp% 2 IN IP4 %localIpAddress%\r\n"
        + "s=<BT SDK Mock Phone Session>\r\n"
        + "c=IN IP4 %localIpAddress%\r\n"
        + "t=0 0\r\n"
        + "m=audio %port% RTP/AVP 8\r\n"
        + "a=inactive\r\n"
        //+ "a=x-rtp-session-id:663BBA83C3294ABD8D343BFEFF2F64DA\r\n"
        ;
    
    private String userId = "sipulator";
    private static String ipAddress = "127.0.0.1";
    private static int port = 5161;
    private static final String transport = "udp";
    private static final String stackName = "SipulatorStack";

    public SipulatorPhone(String userId, String ipAddressPattern, String remoteSipAddress, String remoteSipProxy) throws Exception {
    	this.userId = userId;
    	this.ipAddressPattern = ipAddressPattern;
    	this.remoteSipAddress = remoteSipAddress;
    	this.remoteSipProxy = remoteSipProxy;
        ipAddress = lookupIpAddress(ipAddressPattern);
        init();
    }
    
    public void command(String command) {
        log("phone " + userId + " asked to Stop");
        if (command.equalsIgnoreCase("stop")) {
            this.stop = true;
            call.dispose();
            sipPhone.dispose();
        }
    }

    protected String getSdpData() {
    	int portOffset = (int)(Math.round(10.0 * Math.random()));
    	int port = portBase + portOffset;
    	
    	return sdpDataTemplate
    			.replace("%localIpAddress%", lookupIpAddress(ipAddressPattern))
    			.replace("%port%", Integer.toString(port))
    			.replace("%timestamp%", Long.toString(System.currentTimeMillis()))
    			;
    }
    
    private synchronized void init() throws Exception {
        if (null == sipStack) {
            Properties props = new Properties();
            props.setProperty("javax.sip.RETRANSMISSION_FILTER", "true");
            props.setProperty("javax.sip.STACK_NAME", stackName);
//            SipStack.setTraceEnabled(true);

//    	    props.setProperty("org.cafesip.sipunit.TRACE_LEVEL", "32");
//            props.setProperty("org.cafesip.sipunit.DEBUG_LOG", "testAgent_debug.txt");
//            props.setProperty("org.cafesip.sipunit.SERVER_LOG", "testAgent_log.txt");
//    	    
//    	    props.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");
//            props.setProperty("gov.nist.javax.sip.DEBUG_LOG", "testAgent_debug.txt");
//            props.setProperty("gov.nist.javax.sip.SERVER_LOG", "testAgent_log.txt");

            sipStack = new SipStack(transport, port(), props);
            log("SIP Stack started on localhost on port " + port());
        }
        this.sipPhone = sipStack.createSipPhone(ipAddress, transport, port(), getSipUri());
        phonesOnStack++;

        call = sipPhone.createSipCall();
    }

    public void run() {
        log(String.format("Starting %s as %s", userId, this.getClass().getName()));
        
        while (!stop) {}
        
        log("Phone " + userId + " stopped");
        cleanUp();
    }

    public boolean initiateOutgoingCall() {
        return call.initiateOutgoingCall(remoteSipAddress, remoteSipProxy);
    }

	public boolean waitAnswer(int responseCode, long waitTimeout) {
		call.waitForAnswer(waitTimeout);
		List<?> responses = call.getAllReceivedResponses();
		for(Object o : responses) {
			SipResponse currentResponse = (SipResponse)o;
			if(currentResponse.getStatusCode() == responseCode)
				return true;
		}
		return false;
	}
    
    public boolean waitForResponse(int responseCode, long millis) {
        while (call.waitOutgoingCallResponse(millis)) {
        	if (call.getReturnCode() == responseCode) {
        		return true;
        	}
        }
        return false;
    }
    
    public boolean waitForBye(long millis) {
    	call.listenForDisconnect();
    	return call.waitForDisconnect(millis);
    }
    
    public boolean listenForIncomingCall() {
    	return call.listenForIncomingCall();
    }
    
    public boolean waitForInvite(long millis) {
    	return call.waitForIncomingCall(millis);
    }

    public boolean sendInviteOkAck() {
    	return call.sendInviteOkAck();
    }
    
    public boolean respondToBye() {
    	return call.respondToDisconnect();
    }
    
    public boolean sendTrying() {
    	return call.sendIncomingCallResponse(Response.TRYING, "Trying", 0);
    }
    
    public boolean sendRinging() {
    	return call.sendIncomingCallResponse(Response.RINGING, "Ringing", 0);
    }
    
    public boolean sendInviteOk() {
    	return call.sendIncomingCallResponse(Response.OK, "OK", 0, getSdpData(), "application", "sdp", null, null);
    }
    
    public boolean sendBusy() {
    	return call.sendIncomingCallResponse(Response.BUSY_HERE, "Busy", 0);
    }
    
    public boolean waitForAck(long millis) {
    	return call.waitForAck(millis);
    }
    
    public boolean waitForReinviteAndRespond(long millis, String sipAddress) {
		SipTransaction reinviteTransaction = call.waitForReinvite(5000);
		if (reinviteTransaction != null)
			return call.respondToReinvite(reinviteTransaction, Response.OK, "OK", 0, sipAddress, "display name", getSdpData(), "application", "sdp");

		return false;
    }
    
	public boolean waitForCancel(long waitTimeout) {
		RequestEvent re = sipPhone.waitRequest(waitTimeout);
        if (re.getRequest().getMethod().equals(Request.CANCEL))
        	return true;
        else
        	return false;
	}
    
    
    private void cleanUp() {
        if (phonesOnStack < 1 && null != sipStack) {
            synchronized (sipStack) {
                sipStack.dispose();
                sipStack = null;
                log("SIP stack stopped");
            }
        }
    }
    
    protected String getSipUri() {
        return "sip:" + userId + "@" + ipAddress + (port == 5060 ? "" : ":" + port);
    }

    protected SipCall getCall() {
        return this.call;
    }

    protected SipStack getSipStack() {
        return sipStack;
    }

    protected SipPhone getSipPhone() {
        return sipPhone;
    }

    public static String lookupIpAddress(String ipAddressPattern) {
        try {
        	if (ipAddressPattern != null) {
		    	Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
		        while (nis.hasMoreElements()) {
		            NetworkInterface ni = nis.nextElement();
		            Enumeration<InetAddress> addresses = ni.getInetAddresses();
		            while (addresses.hasMoreElements()) {
		                InetAddress address = addresses.nextElement();
		                if (address.getHostAddress().matches(ipAddressPattern))
		                    return address.getHostAddress();
		            }
		        }
		        return ipAddressPattern;
        	}
       		return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
            return "127.0.0.1";
        }
    }

    static void log(String line) {
        System.out.println(line);
    }

	public static int port() {
		return port;
	}

	public void setCredentials(String realm, String username, String password) {
		if (realm == null || username == null || password == null) return;
		log("adding " + realm + "/" + username + "/" + password + " to credentials");
		call.getParent().addUpdateCredential(new Credential(realm, username, password));
	}
}
