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

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/*
 * start/stop an existing WebApp in a tomcat container.
 * Assumes that the webapp is already deployed
 */
public class RemoteTomcatStackRunner implements StackRunner{

    private static final Log LOG = LogFactory.getLog(RemoteTomcatStackRunner.class);

    private String stackAddress;

	private String username;

	private String password;

	public String getStackAddress() {
		return stackAddress;
	}

	public void setStackAddress(String setStackAddress) {
		this.stackAddress = setStackAddress;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	private String getResponse(String uri) {
		HttpClient httpClient = new HttpClient();
		httpClient.getState().setCredentials(AuthScope.ANY,	new UsernamePasswordCredentials(getUsername(), getPassword()));
		HttpMethod getMethod = new GetMethod(uri);
		getMethod.setDoAuthentication(true);

		try {
			int rc = httpClient.executeMethod(getMethod);
			LOG.debug(rc);
			if (rc != 200)
				throw new RuntimeException(String.format("bad http response from Tomcat: %d", rc));
			return getMethod.getResponseBodyAsString();
		} catch (HttpException e) {
			LOG.warn(e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			LOG.warn(e);
			throw new RuntimeException(e);
		}
	}

	private void processResponse(String response, String expectedStringContainedInResponse){
		if (! response.contains(expectedStringContainedInResponse))
			throw new RuntimeException(String.format("bad response from Tomcat: %s", response));
		LOG.debug(response);
	}

	public boolean isRunning(){
		try{
			String uri = String.format("http://%s/SpringRing/status", getStackAddress());
			String response = getResponse(uri);
			processResponse(response, "Sample SpringRing web application");
		} catch(RuntimeException e) {
			return false;
		}
		return true;
	}

	public void startStack() {
		String uri = String.format("http://%s/manager/start?path=/SpringRing", getStackAddress());
		LOG.info("Starting stack using: " + uri);
		String response = getResponse(uri);
		processResponse(response, "OK - Started");
	}

	public void stopStack() {
		String uri = String.format("http://%s/manager/stop?path=/SpringRing", getStackAddress());
		LOG.info("Stopping stack using: " + uri);
		String response = getResponse(uri);
		processResponse(response, "OK - Stopped");
	}

	public String toString(){
		return String.format("%s[%s@%s]", getClass().getSimpleName(), getUsername(), getStackAddress());
	}

	public static void main(String[] args) {
		RemoteTomcatStackRunner remoteTomcatStackRunner = new RemoteTomcatStackRunner();
		remoteTomcatStackRunner.setUsername("admin");
		remoteTomcatStackRunner.setPassword("admin");
		remoteTomcatStackRunner.setStackAddress("radon193.nat.bt.com:9072");

		remoteTomcatStackRunner.startStack();
	}
}
