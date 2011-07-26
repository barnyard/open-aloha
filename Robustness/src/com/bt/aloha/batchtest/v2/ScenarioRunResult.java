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

import java.util.Map;

public class ScenarioRunResult {
	private String message;
	private boolean success;
	private String scenarioId;
	private String scenarioName;
	private Map<?, ?> scenarioData;

	public ScenarioRunResult(String scenarioId, String scenarioName) {
		this.success = false;
		this.message = "not run";
		this.scenarioId = scenarioId;
		this.scenarioName = scenarioName;
	}

	public String getScenarioId() {
		return scenarioId;
	}

	public String getScenarioName() {
		return scenarioName;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public Map<?, ?> getScenarioData() {
		return scenarioData;
	}

	public void setScenarioData(Map<?, ?> data){
		this.scenarioData = data;
	}

	public void setResult(boolean success, String message){
		setResult(success, message, null);
	}

	public void setResult(boolean success, String message, Map<?, ?> data){
		setSuccess(success);
		setMessage(message);
		setScenarioData(data);
	}

	@Override
	public String toString() {
		return String.format("Result[%s : %s : %s : %s]", getScenarioName(), isSuccess(), getMessage(), getScenarioId());
	}
}
