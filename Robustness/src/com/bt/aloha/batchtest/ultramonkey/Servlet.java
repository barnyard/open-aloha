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

 	

 	
 	
 
package com.bt.aloha.batchtest.ultramonkey;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.bt.aloha.stack.SimpleSipStack;

/**
 * Spring loaded servlet to expose simple makeCall and terminateCall SpringRing methods
 */
public class Servlet extends HttpServlet {

    private static final Log LOG = LogFactory.getLog(Servlet.class);
	private static final long serialVersionUID = -2797343292851273212L;
	private Service service;
	private int makeCallCount = 0;
	private int terminateCallCount = 0;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
    	super.init(servletConfig);
    	
    	WebApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletConfig.getServletContext());
        service = (Service) applicationContext.getBean("serviceBean");
        // check for a system property to set the Stack contact address
        if (null != System.getProperty("sip.stack.contact.address", null)) {
        	SimpleSipStack simpleSipStack = (SimpleSipStack)applicationContext.getBean("simpleSipStack");
        	simpleSipStack.setContactAddress(System.getProperty("sip.stack.contact.address"));
        }
    }
    
    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)throws ServletException, IOException {
		LOG.debug(request.getPathInfo() + ":" + request.getQueryString());
		Properties resultProperties = new Properties();
		resultProperties.put("local.host.name", request.getLocalName());
		resultProperties.put("local.host.port", Integer.toString(request.getLocalPort()));
		resultProperties.put("remote.host.name", request.getRemoteHost());
		resultProperties.put("remote.host.port", Integer.toString(request.getRemotePort()));
		resultProperties.put("makeCall.count", Integer.toString(makeCallCount));
		resultProperties.put("terminateCall.count", Integer.toString(terminateCallCount));
		resultProperties.put("incomingCall.count", Integer.toString(((ServiceImpl)service).getIncomingCount()));
		String result = "something or rather";
		String resultKey = "callid";
		if (request.getPathInfo().equalsIgnoreCase("/makecall")) {
			makeCallCount++;
			String caller = request.getParameter("caller");
			String callee = request.getParameter("callee");
			result = this.service.makeCall(caller, callee);
		} else if (request.getPathInfo().equalsIgnoreCase("/terminatecall")) {
			terminateCallCount++;
			String callid = request.getParameter("callid");
			service.terminateCall(callid);
			result = callid;
		} else if (request.getPathInfo().equalsIgnoreCase("/reset")) {
			makeCallCount = 0;
			terminateCallCount = 0;
			((ServiceImpl)service).setIncomingCount(0);
		} else if (request.getPathInfo().equalsIgnoreCase("/status")) {
			result = "Sample SpringRing web application";
			resultKey = "status";
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		resultProperties.put(resultKey, result);
		resultProperties.store(response.getOutputStream(), "Response generated on:");
	}
}
