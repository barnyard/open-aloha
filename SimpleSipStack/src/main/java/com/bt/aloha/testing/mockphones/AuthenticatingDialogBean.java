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

 	

 	
 	
 
package com.bt.aloha.testing.mockphones;

import java.util.concurrent.ConcurrentHashMap;

import javax.sip.ServerTransaction;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.ProxyAuthorizationHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.dialog.DigestClientAuthenticationMethod;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.util.ConcurrentUpdateBlock;

public class AuthenticatingDialogBean extends MockphoneDialogBeanBase {
	private static final String AUTHENTICATING401 = "authenticating401";
	private static final String REALM = "bt.com";
	private static final String USERNAME="Fred";
	private static final String PASSWORD="Wilma";
	private Log log = LogFactory.getLog(this.getClass());
	
	//TODO: Think about replacing this with field within the DialogInfo thing or house keep this hashmap
	private ConcurrentHashMap<String, String> authMap = new ConcurrentHashMap<String, String>();
	
	public AuthenticatingDialogBean() {
		super();
	}

	@Override
    public void processInitialInvite(final Request request, final ServerTransaction serverTransaction, final String dialogId) {
        log.debug(String.format("Replying to initial invite for mockphone dialog %s", dialogId));
    	ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				dialogInfo.setDialogState(DialogState.Initiated);
				ServerTransaction serverTransaction = dialogInfo.getInviteServerTransaction();
				String realm = REALM;
				String nonce = Long.toString(System.currentTimeMillis());
				getDialogCollection().replace(dialogInfo);
				authMap.put(dialogId, nonce);
				if(request.getRequestURI().toString().contains(AUTHENTICATING401))
					getDialogBeanHelper().sendWWWAuthenticationRequiredResponse(request, serverTransaction, dialogInfo.getSipUserName(), realm, nonce);
				else
					getDialogBeanHelper().sendProxyAuthenticationRequiredResponse(request, serverTransaction, dialogInfo.getSipUserName(), realm, nonce);
			}
			public String getResourceId() {
				return dialogId;
			}
    	};
    	getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
    }
	
	@Override
	protected void processReinvite(Request request,	ServerTransaction serverTransaction, String dialogId) {
        if (!authMap.containsKey(dialogId)){
        	super.processReinvite(request, serverTransaction, dialogId);
        	return;
        }
        if(checkAuth(authMap.get(dialogId), request)){
        	authMap.remove(dialogId);
        	sendInitialOKResponse(dialogId,request,serverTransaction);
        }
        else{
        	getDialogBeanHelper().sendResponse(request, serverTransaction, Response.FORBIDDEN);
        }
	}
	
	private boolean checkAuth(String nonce, Request request){
        String uri = request.getRequestURI().toString();
        AuthorizationHeader authHeader;
        if(request.getRequestURI().toString().contains(AUTHENTICATING401))
        	authHeader = (AuthorizationHeader)request.getHeader(AuthorizationHeader.NAME);
        else
        	authHeader = (AuthorizationHeader)request.getHeader(ProxyAuthorizationHeader.NAME);
        	
        if (authHeader==null){
        	log.debug(String.format("No authorisation header found"));
        	return false;
        }
        String authResponse = authHeader.getResponse();
		DigestClientAuthenticationMethod digestClientAuthenticationMethod = new DigestClientAuthenticationMethod();
		String expectedResponse = digestClientAuthenticationMethod.generateResponse(REALM, USERNAME, uri, nonce, PASSWORD, "INVITE", null, "MD5");
		
		if (authResponse.equals(expectedResponse))
			return true;
		else{
			log.debug(String.format("Expected and actual authentication tokens don't match expected = %s, actual = %s", expectedResponse, authResponse));
			return false;
		}
	}
}
