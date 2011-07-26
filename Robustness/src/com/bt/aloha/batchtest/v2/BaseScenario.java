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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

public abstract class BaseScenario implements Scenario{
	private static Log LOG = LogFactory.getLog(BaseScenario.class);
	private String name;
	private ApplicationContext applicationContext;
	private List<ScenarioRunResultListener> resultListeners;
	private String testEndpoint;
	private StackManagerSyncronizationSemaphore syncSemaphore;

	public BaseScenario() {
		resultListeners = new Vector<ScenarioRunResultListener>();
		this.name = getClass().getSimpleName();
	}
	
	public String getName(){
		return this.name;
	}

	public ApplicationContext getApplicationContext(){
		return this.applicationContext;
	}

	public void setApplicationContext(ApplicationContext c) {
		this.applicationContext = c;
	}

	public void setScenarioRunResultListener(ScenarioRunResultListener l) {
		this.resultListeners.add(l);
	}

	public void setScenarioRunResultListenerList(List<ScenarioRunResultListener> l) {
		this.resultListeners.addAll(l);
	}

	protected List<ScenarioRunResultListener> getScenarioRunResultListenerList(){
		return this.resultListeners;
	}

	protected void publishResultOnScenarioComplete(ScenarioRunResult result){
		LOG.debug("notifying run result listeners with " + result);
		if(getScenarioRunResultListenerList().size() > 0){
			for(ScenarioRunResultListener l : getScenarioRunResultListenerList()){
				l.notifyResultUpdateOnScenarioComplete(result);
			}
		}
		else
			LOG.warn("No result listener set for this scenario - fix it in the application context!");
	}

	public String getTestEndpoint() {
		return testEndpoint;
	}

	public void setTestEndpoint(String testEndpoint) {
		this.testEndpoint = testEndpoint;
	}

	public void setStackManagerSyncronizationSemaphore(StackManagerSyncronizationSemaphore syncSemaphore) {
		this.syncSemaphore = syncSemaphore;
	}

	protected void acquireSyncSemaphore() {
		if(syncSemaphore!=null && syncSemaphore.isInitialized())
			syncSemaphore.tryAcquire(5000);
	}
}
