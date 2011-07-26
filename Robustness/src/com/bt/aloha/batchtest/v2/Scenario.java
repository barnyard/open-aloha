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

import org.springframework.context.ApplicationContext;

public interface Scenario {

	/*
	 * identity
	 */
//	String getId();
	String getName();

	/*
	 * lifecycle methods
	 */
	void setup();
	void run(String scenarioId);
	void teardown();

	/*
	 * listening directly to a stack
	 */
	void setApplicationContext(ApplicationContext c);

	/*
	 * publishing results
	 */
	void setScenarioRunResultListener(ScenarioRunResultListener l);
	void setScenarioRunResultListenerList(List<ScenarioRunResultListener> l);

	/*
	 * set this to an implementation that allows scenarios and stack
	 * managers to sync each other when the stack needs to go up or down
	 */
	void setStackManagerSyncronizationSemaphore(StackManagerSyncronizationSemaphore syncSemaphore);
}
