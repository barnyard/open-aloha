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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ResultLogger {

	private Map<String, ScenarioRunResult> scenarioRunResults;
	private final Log log = LogFactory.getLog(this.getClass());

	public ResultLogger() {

	}

	public Map<String, ScenarioRunResult> getScenarioRunResults() {
		return scenarioRunResults;
	}

	public void setScenarioResults(Map<String, ScenarioRunResult> scenarioResults) {
		this.scenarioRunResults = scenarioResults;
	}

	public ResultTotals logResultsSummary() {
		log.info(String.format("Number of scenarios run: %s", getScenarioRunResults().size()));
		List<PerScenarioResult> psTotals = countPerScenarioTotals(scenarioRunResults.values());
		log.info("------------");
		for(PerScenarioResult perScenarioResult : psTotals){
			log.info(String.format("[%s] Successes: %s/%s (%s%%)", perScenarioResult.getName(), perScenarioResult.getResultTotals().getSuccesses(), perScenarioResult.getResultTotals().getTotal(), perScenarioResult.getResultTotals().percSuccess()));
			log.info("------------");
		}
		ResultTotals totals = countTotals(scenarioRunResults.values());
		log.info(String.format("Overall Successes: %s/%s (%s%%)", totals.getSuccesses(), totals.getTotal(), totals.percSuccess()));
		log.info(String.format("Overall Failures: %s/%s (%s%%)", totals.getFailures(), totals.getTotal(), totals.percFailures()));
		return totals;
	}

	public void logResultEntries(){
		log.info("------------");
		if(getScenarioRunResults().size()==0){
			log.warn("No results to log");
		} else {
			for(ScenarioRunResult r: getScenarioRunResults().values()){
				log.info(r.toString());
			}
		}
	}

	private List<PerScenarioResult> countPerScenarioTotals(Collection<ScenarioRunResult> scenarioRunResults) {
		Map<String, List<ScenarioRunResult>> map = new HashMap<String, List<ScenarioRunResult>>();
		for(ScenarioRunResult r : scenarioRunResults){
			List<ScenarioRunResult> l = map.get(r.getScenarioName());
			if(l==null){
				l = new Vector<ScenarioRunResult>();
				map.put(r.getScenarioName(), l);
			}
			l.add(r);
		}

		List<PerScenarioResult> psr = new Vector<PerScenarioResult>();
		for(String key : map.keySet()){
			List<ScenarioRunResult> l = map.get(key);
			ResultTotals r = countTotals(l);
			PerScenarioResult psResult = new PerScenarioResult(key);
			psResult.setResultTotals(r);
			psr.add(psResult);
		}

		return psr;
	}

	private ResultTotals countTotals(Collection<ScenarioRunResult> results) {
		ResultTotals totals = new ResultTotals();
		for (ScenarioRunResult r : results) {
			if(r.isSuccess())
				totals.incSuccesses();
			else
				totals.incFailures();
		}
		return totals;
	}

	private static class PerScenarioResult {
		private ResultTotals resultTotals;

		private String scenarioName;

		public PerScenarioResult(String name) {
			this.scenarioName = name;
		}

		public String getName() {
			return scenarioName;
		}

		public ResultTotals getResultTotals() {
			return resultTotals;
		}

		public void setResultTotals(ResultTotals resultTotals) {
			this.resultTotals = resultTotals;
		}
	}
}
