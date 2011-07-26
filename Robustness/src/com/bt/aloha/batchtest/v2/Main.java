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

 	

 	
 	
 
package com.bt.aloha.batchtest.v2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {
	private static Log LOG = LogFactory.getLog(Main.class);

	public static void main(String[] args) {
		int res = 256;
		try {
			String appCtx = "com/bt/aloha/batchtest/v2/RobustnessV2.xml";
			if(args.length != 0)
				appCtx = args[0];
			// load app context
			ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext(appCtx);
			// extracts TestRunner
			TestRunner testRunner = (TestRunner)appContext.getBean("testRunner");
			// runs Test Runner
			ResultTotals resTotal = testRunner.run();
			res = Long.valueOf(resTotal.getFailures()).intValue();
			LOG.info("Results: " + resTotal);
			appContext.destroy();
		} catch (Throwable t) {
			LOG.error("Exception cought in Main", t);
		}
		LOG.info("Returning code: " + res);
		if (res != 0){
			res = 1;
		}
		// return code is calc module 256
		System.exit(res);
	}
}
