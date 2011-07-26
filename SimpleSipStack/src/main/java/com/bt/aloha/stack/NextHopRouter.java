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

 	

 	
 	
 
package com.bt.aloha.stack;

import gov.nist.core.net.AddressResolver;
import gov.nist.javax.sip.SIPConstants;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.Route;
import gov.nist.javax.sip.header.RouteList;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.stack.HopImpl;

import java.util.ListIterator;

import javax.sip.SipException;
import javax.sip.SipStack;
import javax.sip.address.Hop;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.message.Request;

import org.apache.log4j.Logger;

public class NextHopRouter implements javax.sip.address.Router {
    public static final String NEXT_HOP = "SimpleSipStackNextHop";
	private static final int PORT_5061 = 5061;
	private static final int PORT_5060 = 5060;
	private Logger log = Logger.getLogger(getClass());
	private String nextHopPropertyName;
	private SipStackImpl sipStack;

	// NIST stack currently can't instantiate a javax.sip.address.Router impl through a default constructor
	public NextHopRouter(SipStack aSipStack, String defaultRoute) {
		sipStack = (SipStackImpl)aSipStack;
		nextHopPropertyName = NEXT_HOP + "_" + sipStack.getStackName();
	}

	public Hop getNextHop(Request request) throws SipException {
		log.debug(String.format("Get next hop called for requestUri %s", request.getRequestURI().toString()));
		SipURI sipUri = null;
		if (request.getRequestURI().isSipURI())
			sipUri = (SipURI)request.getRequestURI();
		else
			throw new IllegalArgumentException("Don't know how to route non sip uri" + request.getRequestURI());
		String hostToMatch = sipUri.getHost();

		String nextHopMatcher = System.getProperty(nextHopPropertyName);
		if (nextHopMatcher != null) {
			String[] nextHops = nextHopMatcher.split(";");
			for (String nextHop : nextHops) {
				int indexOfEquals = nextHop.indexOf("=");
				String toReplace = nextHop.substring(0, indexOfEquals);
				String replaceWith = nextHop.substring(indexOfEquals+1);
				int indexOfColon = replaceWith.indexOf(":");
				String replaceWithHost;
				int replaceWithPort;
				if (indexOfColon < 0) {
					replaceWithHost = replaceWith;
					replaceWithPort = PORT_5060;
				} else {
					replaceWithHost = replaceWith.substring(0, indexOfColon);
					replaceWithPort = Integer.parseInt(replaceWith.substring(indexOfColon+1));
				}
				if (hostToMatch.matches(toReplace)) {
					log.debug(String.format("Returning next hop for %s with address %s and port %d", toReplace, replaceWithHost, replaceWithPort));
					return sipStack.getAddressResolver().resolveAddress(new HopImpl(replaceWithHost, replaceWithPort, SIPConstants.UDP));
				}
			}
		}

		SIPRequest sipRequest = (SIPRequest)request;
		RouteList routes = sipRequest.getRouteHeaders();
		if (routes != null) {
			// to send the request through a specified hop the application is supposed to prepend the appropriate Route header which.
			Route route = (Route) routes.getFirst();
			URI uri = route.getAddress().getURI();
			if (uri.isSipURI()) {
				sipUri = (SipURI) uri;
				if (!sipUri.hasLrParam()) {
					fixStrictRouting(sipRequest);
				}

				Hop hop = createHop(sipUri);
				return hop;
			} else {
				throw new RuntimeException("First Route not a SIP URI");
			}
		}
		Hop hop = createHop(sipUri);
		return hop;
	}

	@Deprecated
	public ListIterator<?> getNextHops(Request request) {
		throw new UnsupportedOperationException("Deprecated method getNextHops not implemented");
	}

	public Hop getOutboundProxy() {
		throw new UnsupportedOperationException("Method getOutboundProxy not implemented");
	}

	private Hop createHop(SipURI sipUri) {
		// always use TLS when secure
		String transport = sipUri.isSecure() ? SIPConstants.TLS : sipUri.getTransportParam();
		if (transport == null)
			transport = SIPConstants.UDP;

		int port;
		if (sipUri.getPort() != -1) {
			port = sipUri.getPort();
		} else {
			if (transport.equalsIgnoreCase(SIPConstants.TLS))
				port = PORT_5061;
			else
				port = PORT_5060;
		}
		String host = sipUri.getMAddrParam() != null ? sipUri.getMAddrParam() : sipUri.getHost();
		AddressResolver addressResolver = sipStack.getAddressResolver();
		return addressResolver.resolveAddress( new HopImpl(host, port, transport));

	}

	private void fixStrictRouting(SIPRequest req) {
		RouteList routes = req.getRouteHeaders();
		Route first = (Route) routes.getFirst();
		SipUri firstUri = (SipUri) first.getAddress().getURI();
		routes.removeFirst();

		// Add request-URI as last Route entry
		AddressImpl addr = new AddressImpl();
		addr.setAddess(req.getRequestURI());
		Route route = new Route(addr);

		routes.add(route);
		req.setRequestURI(firstUri);
	}
}
