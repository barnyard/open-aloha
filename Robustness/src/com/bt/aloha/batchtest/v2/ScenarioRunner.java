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

import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.util.MessageDigestHelper;

public class ScenarioRunner implements Runnable {
	private static Log LOG = LogFactory.getLog(ScenarioRunner.class);
	private Scenario scenario;
	private CountDownLatch latch;
	private List<ScenarioLifecycleListener> lifecycleListener;
	private String scenarioId;

	public ScenarioRunner(){
		lifecycleListener = new Vector<ScenarioLifecycleListener>();
	}

	public List<ScenarioLifecycleListener> getLifecycleListeners() {
		return lifecycleListener;
	}

	public void setLifecycleListeners(List<ScenarioLifecycleListener> lifecycleListener) {
		this.lifecycleListener = lifecycleListener;
	}

	public void addScenarioLifecycleListener(ScenarioLifecycleListener l){
		lifecycleListener.add(l);
	}

	public void removeScenarioLifecycleListener(ScenarioLifecycleListener l){
		lifecycleListener.remove(l);
	}

	public void setScenario(Scenario aScenario){
		this.scenario = aScenario;
		this.scenarioId = generateId();
	}
	
	private String generateId() {
		final String randomString = System.currentTimeMillis() + "scenario" + Math.random();
		String name = this.scenario.getClass().getSimpleName();
		return String.format("%s:%s", name, MessageDigestHelper.generateDigest(randomString));
	}

	public void setCountdownLatch(CountDownLatch aLatch){
		this.latch = aLatch;
	}

	private void setup() {
		try {
			notifyStart();
			scenario.setup();
		} catch (RuntimeException t) {
			logException(scenario, t);
			throw t;
		}
	}

	private void teardown() {
		try {
			scenario.teardown();
		} catch (RuntimeException t) {
			logException(scenario, t);
			throw t;
		} finally{
			notifyCompletion();
		}
	}

	private void logException(Scenario s, Throwable t) {
		LOG.error(String.format("TEARDOWN for scenario %s (id:%s) threw an exception", scenario.getName(), scenarioId), t);
	}

	private void notifyStart(){
		for(ScenarioLifecycleListener l : lifecycleListener){
			l.scenarioStarted(scenarioId);
		}
	}

	private void notifyCompletion(){
		for(ScenarioLifecycleListener l : lifecycleListener){
			l.scenarioCompleted(scenarioId);
		}
	}

	public void run() {
		try {
			setup();
			try {
				LOG.debug("Starting scenario " + scenarioId + ", " + scenario.getClass().getName());
				scenario.run(scenarioId);
			} catch (RuntimeException t) {
				logException(scenario, t);
				throw t;
			}
		} finally {
			latch.countDown();
			//LOG.debug("Remaining count on latch: " + latch.getCount());
			teardown();
		}
	}
}
