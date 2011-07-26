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

import gov.nist.javax.sip.stack.HopImpl;

import java.util.ListIterator;

import javax.sip.SipException;
import javax.sip.SipStack;
import javax.sip.address.Address;
import javax.sip.address.Hop;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Request;

import org.apache.log4j.Logger;

public class Router implements javax.sip.address.Router {
	private Logger log = Logger.getLogger(getClass());

	// NIST stack currently can't instantiate a javax.sip.address.Router impl through a default constructor
	public Router(SipStack sipStack, String defaultRoute) {
	}
	
	public Hop getNextHop(Request request) throws SipException {
		log.debug(String.format("Get next hop called for sip call id: %s", ((CallIdHeader)request.getHeader(CallIdHeader.NAME)).getCallId()));
		Address hostAddress = RoundRobinHostManager.getInstance().lookupHost();
		URI hostURI = hostAddress.getURI();
		if(hostURI instanceof SipURI) {
			SipURI hostSipURI = (SipURI)hostURI;
			return new HopImpl(hostSipURI.getHost(), hostSipURI.getPort(), "udp");
		}
		throw new RuntimeException("Don't know how to route request " + request.getRequestURI());
	}

	public ListIterator<?> getNextHops(Request request) {
		throw new RuntimeException("Deprecated method getNextHops not implemented");
	}

	public Hop getOutboundProxy() {
		throw new RuntimeException("Method getOutboundProxy not implemented");
	}

}
