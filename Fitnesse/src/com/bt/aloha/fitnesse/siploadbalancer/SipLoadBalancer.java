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

import gov.nist.javax.sip.header.Via;

import java.text.ParseException;
import java.util.Vector;

import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.SipListener;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.phones.SipulatorPhone;
import com.bt.aloha.stack.SimpleSipStack;
import com.bt.aloha.stack.StackException;

public class SipLoadBalancer implements SipListener {
	private Log log = LogFactory.getLog(getClass());
	private SimpleSipStack simpleSipStack;
	
	public SipLoadBalancer() {}
	
	public void setSimpleSipStack(SimpleSipStack simpleSipStack) {
		this.simpleSipStack = simpleSipStack;		
	}
	
	private String parseHostAddress(String address) {
		if (address == null)
			return null;
		
		String name = "";
		if (address.startsWith("sip:"))
			name = address.substring(0, address.indexOf(":")+1);
		
		String ipAddress = address.substring(name.length(), address.lastIndexOf(":"));
		String port = address.substring(address.lastIndexOf(":"));
		return name + SipulatorPhone.lookupIpAddress(ipAddress) + port;
	}
	
	public void setHosts(String hostList) throws ParseException {
		Vector<Address> hosts = new Vector<Address>();
		String[] list = hostList.split(",");
		for (String host : list) {
			Address addr = simpleSipStack.getAddressFactory().createAddress(parseHostAddress(host));
			log.info("Adding host " + addr.getURI() + " to hosts list for load balancer");
			hosts.add(addr);
		}
		RoundRobinHostManager.getInstance().setHosts(hosts);
	}

	public void removeHost(String address)  throws ParseException {
		Address addr = simpleSipStack.getAddressFactory().createAddress(parseHostAddress(address));
		RoundRobinHostManager.getInstance().removeHost(addr);
	}

	public void addHost(String address)  throws ParseException {
		Address addr = simpleSipStack.getAddressFactory().createAddress(parseHostAddress(address));
		RoundRobinHostManager.getInstance().addHost(addr);
	}

	public void processRequest(RequestEvent requestEvent) {		
		Request request = requestEvent.getRequest();
		log.info("SipLoadBalancer: request received");
		log.info(request.toString());
		proxyRequest(request);
	}
	
	private void proxyRequest(Request request) {
		try {
			simpleSipStack.getSipProvider().sendRequest(request);
		} catch (SipException e) {
			throw new StackException("Error proxying request",e);
		}
	}
	
	private void proxyResponse(Response response) {
		try {
			simpleSipStack.getSipProvider().sendResponse(response);
		} catch (SipException e) {
			throw new StackException("Error proxying request",e);
		}
	}
	
	public void processResponse(ResponseEvent responseEvent) {
		Response response = responseEvent.getResponse();		
		log.info("Response received");
		log.info(response.toString());
		ViaHeader viaHeader =(ViaHeader)response.getHeader(ViaHeader.NAME);
		// Strip off RPORT if destined for me
		if(viaHeader != null && Integer.toString(simpleSipStack.getPort()).equals(viaHeader.getParameter(Via.RPORT)))
			viaHeader.removeParameter(Via.RPORT);
		proxyResponse(response);
	}

	public void processTimeout(TimeoutEvent timeOutEvent) {
		log.warn("TimeoutEvent received: " + timeOutEvent.toString());
	}

	public void processIOException(IOExceptionEvent ioExceptionEvent) {
		log.warn("IOExceptionEvent received: " + ioExceptionEvent.toString());
	}

	public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
		log.debug("TransactionTerminatedEvent received: " + transactionTerminatedEvent);
	}

	public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
		String sipUri = dialogTerminatedEvent.getDialog().getLocalParty().getURI().toString();
		log.debug("DialogTerminatedEvent received for " + sipUri);		
	}
}
