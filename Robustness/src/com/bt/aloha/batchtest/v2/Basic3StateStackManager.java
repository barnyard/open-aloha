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
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implements logic to start/stop a stack in three states
 */
public class Basic3StateStackManager extends BasicStackManager {
	private static Log LOG = LogFactory.getLog(Basic3StateStackManager.class);
	private boolean chosenStackRunning = true;
	private int chosenStackId = 0;
	private Object lock = new Object();
	private StackManagerSyncronizationSemaphore syncSemaphore;

	public Basic3StateStackManager(){
	}

	public void setStackRunners(List<StackRunner> stackRunners) {
		super.setStackRunners(stackRunners);
		chosenStackId = new Random(System.currentTimeMillis()).nextInt(stackRunners.size());
	}

	public void setTestRunnerConfig(TestRunnerConfig config) {
		super.setTestRunnerConfig(config);
		initializeSemaphore();
	}

	public void setStackManagerSyncronizationSemaphore(StackManagerSyncronizationSemaphore syncSemaphore) {
		this.syncSemaphore = syncSemaphore;
		initializeSemaphore();
	}

	public StackManagerSyncronizationSemaphore getStackManagerSyncronizationSemaphore(){
		return syncSemaphore;
	}

	private void initializeSemaphore(){
		if(getTestRunnerConfig() != null && getStackManagerSyncronizationSemaphore()!=null){
			if(!getStackManagerSyncronizationSemaphore().isInitialized())
				getStackManagerSyncronizationSemaphore().initialize(getOneThirdOfTheTotal(),
						getTestRunnerConfig().getNumberOfRuns());
		}
	}

	public void scenarioStarted(String id) {

	}

	public void scenarioCompleted(String id) {
		super.scenarioCompleted(id);
		startStop();
	}

	protected void startStop(){
		final int oneThird = getOneThirdOfTheTotal();
		final int twoThirds = getTwoThirdsOfTheTotal();
		synchronized(lock){
			if (getRunningScenarioCounter() > oneThird && getRunningScenarioCounter() <= twoThirds && chosenStackRunning){
				if (getStackRunners() != null && getStackRunners().size() > 0){
					StackRunner stackRunner = getStackRunners().get(chosenStackId);
					LOG.info(String.format("TAKING STACK DOWN using stack runner %s", stackRunner));
					try{
						syncSemaphore.waitForAll();
						stackRunner.stopStack();
						sleep(10000);
						syncSemaphore.releaseAll();
						chosenStackRunning = false;
					} catch(RuntimeException e){
						LOG.warn("Exception tacking stack down", e);
					}
				}
			} else if (getRunningScenarioCounter() > twoThirds && !chosenStackRunning){
				if (getStackRunners() != null && getStackRunners().size() > 0){
					StackRunner stackRunner = getStackRunners().get(chosenStackId);
					LOG.info(String.format("TAKING STACK UP using stack runner %s", stackRunner));
					try{
						stackRunner.startStack();
						int count = 6;
						while(count>0){
							sleep(1000);
							if(stackRunner.isRunning())
								break;
							count--;
						}
						if(!stackRunner.isRunning()){
							LOG.warn("WARNING: stack could not be started after three attemps using stack runner " + stackRunner);
						}
						chosenStackRunning = true;
					} catch(RuntimeException e){
						LOG.warn("Exception bringing stack up", e);
					}
				}
			}
		}
	}

	private void sleep(long ms){
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			LOG.debug("Interrupted whilst sleeping");
		}
	}

	private int getOneThirdOfTheTotal() {
		return getTestRunnerConfig().getNumberOfRuns() / 3;
	}

	private int getTwoThirdsOfTheTotal() {
		return getTestRunnerConfig().getNumberOfRuns() - getOneThirdOfTheTotal();
	}

}
