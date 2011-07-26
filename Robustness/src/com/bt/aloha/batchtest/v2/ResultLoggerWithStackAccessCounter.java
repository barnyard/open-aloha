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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ResultLoggerWithStackAccessCounter extends ResultLogger{
	private static Log log = LogFactory.getLog(ResultLoggerWithStackAccessCounter.class);
	@Override
	public ResultTotals logResultsSummary() {
		ResultTotals res = super.logResultsSummary();
		Map<String, Map<String, Integer>> counters = new HashMap<String, Map<String, Integer>>();
		for(ScenarioRunResult r : super.getScenarioRunResults().values()){
			if(r.getScenarioData()!=null){
				try{
					processScenarioData(counters, r, "make");
					processScenarioData(counters, r, "terminate");
				} catch (Exception e){
					log.warn("Exception counting stack accesses", e);
				}
			}
		}
		log.info("-----------------");
		log.info("Split per stack and method - relative only to CreateCallTerminateCall scenario");
		log.info(counters);
		return res;
	}

	private void processScenarioData(Map<String, Map<String, Integer>> counters, ScenarioRunResult r, String call) {
		String host = (String)r.getScenarioData().get(call);
		Map<String, Integer> data = counters.get(call);
		if(data==null){
			data = new HashMap<String, Integer>();
			counters.put(call, data);
		}
		Integer c = data.get(host);
		if(c==null){
			c = Integer.valueOf(0);
		}
		c = c + 1;
//		log.info(String.format("Processed c:%s host:%s call:%s",c,host, call));
		data.put(host, c);
	}

}
