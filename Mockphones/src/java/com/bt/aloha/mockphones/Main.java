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

 	

 	
 	
 
package com.bt.aloha.mockphones;

import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.stack.SimpleSipStack;

public class Main {
	private static Log log = LogFactory.getLog(Main.class);

	public static void main(String[] args) {
		start(args);
		try {
			while(true) {
				Thread.sleep(1000);
			}
		} catch (Exception e) {
			log.error("Exception in main: " + e.getMessage(), e);
		}
	}

	public static ClassPathXmlApplicationContext start(String[] args) {
		List<String> appCtx = new Vector<String>();
		appCtx.add("applicationContext.xml");
		if (args.length==1)
		{
			if("database".equals(args[0])){
				appCtx.add("database-collections-ctx.xml");
				appCtx.add("database-sipstone-datasource-ctx.xml");
			}
			else if("memory".equals(args[0])){
				appCtx.add("memory-collections-ctx.xml");
			}
			else if ("ha-database".equals(args[0])){
				appCtx.add("database-collections-ctx.xml");
				appCtx.add("database-ha-datasource-ctx.xml");
			}
			else{
				throw new IllegalArgumentException("args[0] not understood: need one of 'memory' or 'database']. Default is memory");
			}
		} else {
			appCtx.add("memory-collections-ctx.xml");
		}

		ClassPathXmlApplicationContext applicationContext =
			new ClassPathXmlApplicationContext(appCtx.toArray(new String[appCtx.size()]));
		applicationContext.registerShutdownHook();

		printStartupScreen(applicationContext);

		return applicationContext;
	}

	private static void printStartupScreen(AbstractApplicationContext applicationContext) {
		SimpleSipStack simpleSipStack = (SimpleSipStack)applicationContext.getBean("simpleSipStack");
		System.out.println("---------------------------------------------------------------------------");
		System.out.println("           Session Control Mock Phones                                     ");
		System.out.println("---------------------------------------------------------------------------");
		System.out.println("STARTED");
		System.out.println(" - IP Address:             " +  simpleSipStack.getIpAddress());
		System.out.println(" - Port:                   " +  simpleSipStack.getPort());
		System.out.println(" - Transport:              " +  simpleSipStack.getTransport());
		CallCollection callCollection = (CallCollection)applicationContext.getBean("callCollection");
		System.out.println(" - CallCollection:         " + callCollection.getClass().getName());
		DialogCollection dialogCollection = (DialogCollection)applicationContext.getBean("dialogCollection");
		System.out.println(" - dialogCollection:       " + dialogCollection.getClass().getName());
		Object dialogCollectionBacker = applicationContext.getBean("dialogCollectionBacker");
		System.out.println(" - dialogCollectionBacker: " + dialogCollectionBacker.getClass().getName());
		System.out.println("---------------------------------------------------------------------------");
	}
}
