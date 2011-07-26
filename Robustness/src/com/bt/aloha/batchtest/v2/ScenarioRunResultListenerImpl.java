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

public class ScenarioRunResultListenerImpl implements ScenarioRunResultListener {

	private Map<String, ScenarioRunResult> results;
	private Log log = LogFactory.getLog(this.getClass());

	public ScenarioRunResultListenerImpl(){
	}

	public void notifyResultUpdateOnScenarioComplete(ScenarioRunResult result){
		if(results==null)
			results = new HashMap<String, ScenarioRunResult>();

		results.put(result.getScenarioId(), result);
		log.debug("Result updated and added to listener map (map size: " + results.size() + "). " + result);
	}

	public void setResults(Map<String, ScenarioRunResult> r){
		this.results = r;
	}

	public Map<String, ScenarioRunResult> getResults(){
		return results;
	}
}
