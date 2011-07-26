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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implements logic to check if the stacks are up and running and eventually starts them
 */
public class BasicStackManager implements ScenarioLifecycleListener{
	private static Log LOG = LogFactory.getLog(BasicStackManager.class);
	private List<StackRunner> stackRunners;
	private int runningScenarioCounter;
	private TestRunnerConfig config;

	public BasicStackManager(){
		runningScenarioCounter = 0;
	}

	public int getRunningScenarioCounter() {
		return runningScenarioCounter;
	}

	public TestRunnerConfig getTestRunnerConfig() {
		return config;
	}

	public void setTestRunnerConfig(TestRunnerConfig config) {
		this.config = config;
	}

	public List<StackRunner> getStackRunners() {
		return stackRunners;
	}

	public void setStackRunners(List<StackRunner> stackRunners) {
		this.stackRunners = stackRunners;
		checkAllStacksAreUp();
	}

	public void scenarioStarted(String scenarioId) {
	}

	public void scenarioCompleted(String scenarioId) {
		runningScenarioCounter++;
		int totalNumberOfScenarios = config.getNumberOfRuns();
		double complete = (double)runningScenarioCounter/totalNumberOfScenarios * 100;
		LOG.debug(String.format("%% COMPLETE: %.2f", complete));
	}

	protected void checkAllStacksAreUp() {
		for(StackRunner r : getStackRunners()){
			if(!r.isRunning()){
				r.startStack();
			} else {
				LOG.info(String.format("Stack managed by runner %s is running", r));
			}
			if(!r.isRunning()){
				LOG.error(String.format("Unable to start stack using stack runner %s", r));
				throw new IllegalStateException("At least one stack could not be started - that's the one managed by " + r);
			}
		}
	}
}
