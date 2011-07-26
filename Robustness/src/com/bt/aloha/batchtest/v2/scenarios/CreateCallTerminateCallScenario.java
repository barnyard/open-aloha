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

 	

 	
 	
 
package com.bt.aloha.batchtest.v2.scenarios;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.batchtest.v2.BaseScenario;
import com.bt.aloha.batchtest.v2.ScenarioRunResult;

public class CreateCallTerminateCallScenario extends BaseScenario {
	private Log log = LogFactory.getLog(CreateCallTerminateCallScenario.class);

	private String httpEndpoint;

	public void run(String scenarioId) {
		HttpClient httpClient = new HttpClient();

		String makeCallAddress = String.format("http://%s/SpringRing/makeCall?caller=%s&callee=%s", getHttpEndpoint(), getTestEndpoint(), getTestEndpoint() );

		HttpMethod makeCallMethod = new GetMethod(makeCallAddress);
		ScenarioRunResult runResult = new ScenarioRunResult(scenarioId, this.getName());
		try {
			log.debug("Executing GET on " + makeCallAddress);
			acquireSyncSemaphore();
			int result = httpClient.executeMethod(makeCallMethod);
			log.debug("GET returned " + result);
			if (result != 200) {
				runResult.setResult(false, String.format("HTTP %d received from host when calling makeCall", result));
				log.error(makeCallMethod.getResponseBodyAsString());
			} else {
				String resultString = makeCallMethod.getResponseBodyAsString();
				Properties makeCallResult = new Properties();
				makeCallResult.load(new ByteArrayInputStream(resultString.getBytes()));
				String callId = makeCallResult.getProperty("callid", null);
				log.debug("result from makeCall " + callId);

				String terminateCallAddress = "http://%s/SpringRing/terminateCall?callid=%s";
				HttpMethod terminateCallMethod = new GetMethod(String.format(terminateCallAddress, getHttpEndpoint(), callId));
				result = httpClient.executeMethod(terminateCallMethod);
				resultString = terminateCallMethod.getResponseBodyAsString();
				Properties terminateCallResult = new Properties();
				terminateCallResult.load(new ByteArrayInputStream(resultString.getBytes()));
				log.debug("result from terminateCall " + result);
				if (result != 200) {
					runResult.setResult(false, String.format("HTTP %d received from host when calling terminateCall", result));
					log.error(terminateCallMethod.getResponseBodyAsString());
				} else {
					String makeLocalHostName = makeCallResult.getProperty("local.host.name");
					String termLocalHostName = terminateCallResult.getProperty("local.host.name");
					Map<String, String> data = new HashMap<String, String>();
					data.put("make", makeLocalHostName);
					data.put("terminate", termLocalHostName);
					runResult.setResult(true, "OK", data);
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
			runResult.setResult(false, String.format("Exception %s", e.getMessage()));
		} finally {
			publishResultOnScenarioComplete(runResult);
		}
	}

	public void setup() {
	}

	public void teardown() {
	}

	public static void main(String[] args) {
		CreateCallTerminateCallScenario scenario = new CreateCallTerminateCallScenario();
		scenario.setTestEndpoint("sip:132.146.253.12:5060");
		scenario.setHttpEndpoint("132.146.185.190:9072");
		scenario.run("fred");
	}

	public String getHttpEndpoint() {
		return httpEndpoint;
	}

	public void setHttpEndpoint(String httpEndpoint) {
		this.httpEndpoint = httpEndpoint;
	}
}
