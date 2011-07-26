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

 	

 	
 	
 
package com.bt.aloha.batchtest.v2.mock;

import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.batchtest.v2.BaseScenario;
import com.bt.aloha.batchtest.v2.ScenarioRunResult;

public class Scenario1 extends BaseScenario {
	protected Log log = LogFactory.getLog(this.getClass());

	public void run(String scenarioId) {
		acquireSyncSemaphore();
		ScenarioRunResult runResult = new ScenarioRunResult(scenarioId, this.getName());
		if (isSuccess())
			runResult.setResult(true, "OK");
		else
			runResult.setResult(false, "Unlucky!");
		publishResultOnScenarioComplete(runResult);
	}

	protected boolean isSuccess() {
		return new Random().nextInt(10) >= 3;
	}

	public void setup() {
	}

	public void teardown() {
	}
}
