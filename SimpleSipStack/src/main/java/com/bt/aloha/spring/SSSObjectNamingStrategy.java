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

 	

 	
 	
 
package com.bt.aloha.spring;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.jmx.export.naming.ObjectNamingStrategy;

import com.bt.aloha.stack.SimpleSipStack;

/**
 * this class replaces the default object naming strategy supplied by spring to allow collections belonging
 * to stacks running in parallel to be registered in the same mbean server.
 * The logic is to prepend the stack name to the bean key passed to the getObjectName and create an ObjectName that way.
 * Clearly this means that multiple stacks running against the same mbean srv must have different names.
 */
public class SSSObjectNamingStrategy implements ObjectNamingStrategy {

	private String stackName;
	private String ipAddress;
	private String port;

	/**
	 * empty ctor
	 */
	public SSSObjectNamingStrategy(SimpleSipStack simpleSipStack){
		this.stackName = simpleSipStack.getStackName();
		this.ipAddress = simpleSipStack.getIpAddress();
		this.port  = Integer.toString(simpleSipStack.getPort());
	}

	/**
	 * returns the object name created by prepending the stack name
	 */
	public ObjectName getObjectName(Object managedBean, String beanKey)
			throws MalformedObjectNameException {
		return new ObjectName(String.format("%s-%s-%s-%s", stackName, ipAddress, port, beanKey));
	}

}
