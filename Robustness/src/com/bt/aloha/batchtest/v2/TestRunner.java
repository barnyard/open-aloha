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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public final class TestRunner implements ApplicationContextAware{
	private ThreadPoolTaskExecutor taskExecutor;
	private final Log log = LogFactory.getLog(this.getClass());
	private ResultLogger resultLogger;
	private TestRunnerConfig testRunnerConfig;
	private Map<String, Integer> scenarioBeanNameAndWeightMap = new HashMap<String, Integer>();
	private ApplicationContext applicationContext;

	public ResultLogger getResultLogger() {
		return resultLogger;
	}

	public void setResultLogger(ResultLogger resultLogger) {
		this.resultLogger = resultLogger;
	}

	@SuppressWarnings("unchecked")
	public ResultTotals run() {
		log.info("= Summary =============================================================");
		log.info("Number of scenario runs: " + testRunnerConfig.getNumberOfRuns());
		log.info("Number of scenario types: " + scenarioBeanNameAndWeightMap.size());
		log.info("Size of executor pool (core): " + taskExecutor.getCorePoolSize());
		log.info("Size of executor pool (max): " + taskExecutor.getMaxPoolSize());

		Vector v = (Vector)applicationContext.getBean("allScenarioLifecycleListeners");
		if(v!=null && v.get(0) != null)
			log.info("Using Stack Manager of type " + v.get(0).getClass());
		log.info("=======================================================================");

		normalizeWeightsToNumberOfConcurrentStarts();
		CountDownLatch latch = new CountDownLatch(testRunnerConfig.getNumberOfRuns());
		Vector<String> sNames = new Vector<String>();
		for (String s : scenarioBeanNameAndWeightMap.keySet()) {
			Integer weight = scenarioBeanNameAndWeightMap.get(s);
			for (int run = 0; run < weight; run++) {
				sNames.add(s);
			}
		}
		// shuffle names so that they can be executed randomly and not in a prefixed order
		Collections.shuffle(sNames,new Random(System.currentTimeMillis()));

		for(String s: sNames){
			Scenario scenario = (Scenario)applicationContext.getBean(s);
			// it's a prototype - so has to be extracted from the app ctx every time
			ScenarioRunner sRunner = (ScenarioRunner)applicationContext.getBean("scenarioRunner");
			sRunner.setCountdownLatch(latch);
			sRunner.setScenario(scenario);
			// note that scenario lifecycle listeners (needed for start/stop stack) are set in the app context
			taskExecutor.execute(sRunner);
		}

		taskExecutor.shutdown();
		try {
			// waits for all runners to finish
			latch.await();
		} catch (InterruptedException e) {
			log.warn("Unable to wait for latch to get to 0", e);
		}
		resultLogger.logResultEntries();
		return resultLogger.logResultsSummary();
	}

	/**
	 * reassigns weights to the actual relative of parallel runs proportional to
	 * the number of concurrent starts
	 */
	private void normalizeWeightsToNumberOfConcurrentStarts() {
		int total = 0;
		for (Integer w : scenarioBeanNameAndWeightMap.values()) {
			total += w;
		}
		int totalt = 0;
		for (String s : scenarioBeanNameAndWeightMap.keySet()) {
			int t = scenarioBeanNameAndWeightMap.get(s) * testRunnerConfig.getNumberOfRuns() / total;
			scenarioBeanNameAndWeightMap.put(s, t);
			totalt+=t;
			log.debug("[" + s + "] weight set to: " + t);
		}

		int adj = testRunnerConfig.getNumberOfRuns() - totalt;
		if(adj!=0){
			String aKey = scenarioBeanNameAndWeightMap.keySet().iterator().next();
			int old = scenarioBeanNameAndWeightMap.get(aKey);
			int diffCount = old + adj;
			log.debug(String.format("Adjusting weight for scenario '%s' from %s to %s", aKey, old, diffCount));
			scenarioBeanNameAndWeightMap.put(aKey, diffCount);
		}
	}

	public TestRunnerConfig getTestRunnerConfig() {
		return testRunnerConfig;
	}

	public void setTestRunnerConfig(TestRunnerConfig testRunnerConfig) {
		this.testRunnerConfig = testRunnerConfig;
	}

	public Map<String, Integer> getScenarioBeanNameAndWeightMap() {
		return scenarioBeanNameAndWeightMap;
	}

	public void setScenarioBeanNameAndWeightMap(Map<String, Integer> map) {
		this.scenarioBeanNameAndWeightMap = map;
	}

	public ThreadPoolTaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setApplicationContext(ApplicationContext a)
			throws BeansException {
		this.applicationContext = a;
	}


}
