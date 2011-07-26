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

 	

 	
 	
 
package com.bt.aloha.dialog;

import java.text.ParseException;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sip.ClientTransaction;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentLengthHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.header.ProxyAuthorizationHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.dialog.state.TerminationMethod;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.stack.SimpleSipStack;
import com.bt.aloha.stack.StackException;

public class DialogBeanHelper {
	private static final Log LOG = LogFactory.getLog(DialogBeanHelper.class);
	private SimpleSipStack simpleSipStack;

	protected DialogBeanHelper() {
		simpleSipStack = null;
	}

	public void setSimpleSipStack(SimpleSipStack aSimpleSipStack) {
		this.simpleSipStack = aSimpleSipStack;
	}

	protected SimpleSipStack getSimpleSipStack() {
		return this.simpleSipStack;
	}

	public String extractRemoteContactHeaderFromRequest(Request request) {
		ContactHeader contactHeader = (ContactHeader)request.getHeader(ContactHeader.NAME);
		return extractRemoteContactHeader(contactHeader);
	}

	public String extractRemoteContactHeaderFromResponse(Response response) {
		ContactHeader contactHeader = (ContactHeader)response.getHeader(ContactHeader.NAME);
		return extractRemoteContactHeader(contactHeader);
	}

	private String extractRemoteContactHeader(ContactHeader contactHeader) {
		if(contactHeader != null) {
			return contactHeader.getAddress().getURI().toString();
		}
		LOG.debug(String.format("No contact header in message"));
		return null;
	}

    public String getRemoteSdpFromRequest(Request request) {
        ContentTypeHeader contentTypeHeader = (ContentTypeHeader) request.getHeader(ContentTypeHeader.NAME);
        ContentLengthHeader contentLengthHeader = (ContentLengthHeader) request.getHeader(ContentLengthHeader.NAME);
        if (contentTypeHeader != null
                && SimpleSipStack.CONTENT_TYPE_APPLICATION.equals(contentTypeHeader.getContentType())
                && SimpleSipStack.CONTENT_SUBTYPE_SDP.equals(contentTypeHeader.getContentSubType())
                && contentLengthHeader != null
                && contentLengthHeader.getContentLength() > 0) {
            return new String(request.getRawContent());
        }

        LOG.debug("No SDP in request");
        return null;
    }
    
    public void sendProxyAuthenticationRequiredResponse(Request request, ServerTransaction serverTransaction, String sipUserName, String realm, String nonce) {
        try {
            Response response = simpleSipStack.getMessageFactory().createResponse(Response.PROXY_AUTHENTICATION_REQUIRED, request);
            simpleSipStack.addContactHeader(response, sipUserName);
    		ProxyAuthenticateHeader h = simpleSipStack.getHeaderFactory().createProxyAuthenticateHeader("Digest");
    		h.setNonce(nonce);
    		h.setRealm(realm);
    		response.addHeader(h);

            sendResponse(response, serverTransaction);
        } catch (ParseException e) {
            throw new StackException("Unable to send 407 response", e);
        }
    }

    public void sendInviteOkResponse(Request request, ServerTransaction serverTransaction, String localTag, String sipUserName, SessionDescription sessionDescription) {
        try {
            Response response = simpleSipStack.getMessageFactory().createResponse(Response.OK, request);
            simpleSipStack.addContactHeader(response, sipUserName);
            simpleSipStack.setContent(response, SimpleSipStack.CONTENT_TYPE_APPLICATION, SimpleSipStack.CONTENT_SUBTYPE_SDP, sessionDescription.toString());
            ((ToHeader) response.getHeader(ToHeader.NAME)).setTag(localTag);
            sendResponse(response, serverTransaction);
        } catch (ParseException e) {
            throw new StackException("Unable to send OK response to INVITE", e);
        }
    }

	public void sendResponse(Request request, ServerTransaction serverTransaction, int responseCode) {
		simpleSipStack.sendResponse(request, serverTransaction, responseCode);
	}

	public void sendResponse(Response response, ServerTransaction serverTransaction) {
		simpleSipStack.sendResponse(response, serverTransaction);
	}

	public ClientTransaction sendRequest(Request request) {
		return sendRequest(request, false);
	}

	public ClientTransaction sendRequest(Request request, boolean isResend) {
		if (isResend) {
			simpleSipStack.resendRequest(request);
			return null;
		} else {
			return simpleSipStack.sendRequest(request);
		}
	}

	public Request createInitialInviteRequest(String requestUri, final ReadOnlyDialogInfo dialogInfo) {
		SessionDescription sessionDescription = null;
		if(dialogInfo.isSdpInInitialInvite())
			sessionDescription = dialogInfo.getSessionDescription();
    	return simpleSipStack.createRequest(requestUri, Request.INVITE, dialogInfo.getSipCallId(), dialogInfo.getSequenceNumber(), dialogInfo.getLocalParty(), dialogInfo.getLocalTag(), dialogInfo.getRemoteParty(), dialogInfo.getRemoteTag(), null, dialogInfo.getRouteList(), sessionDescription);
    }

    public Request createReinviteRequest(String requestUri, final ReadOnlyDialogInfo dialogInfo, boolean sendSessionDescription) {
    	SessionDescription sessionDescription = sendSessionDescription ? dialogInfo.getSessionDescription() : null;
    	return simpleSipStack.createRequest(requestUri, Request.INVITE, dialogInfo.getSipCallId(),  dialogInfo.getSequenceNumber(), dialogInfo.getLocalParty(), dialogInfo.getLocalTag(), dialogInfo.getRemoteParty(), dialogInfo.getRemoteTag(), null, dialogInfo.getRouteList(), sessionDescription);
    }

    public Request createByeRequest(final ReadOnlyDialogInfo dialogInfo) {
    	if(dialogInfo.getRemoteContact() == null)
    		throw new IllegalStateException("Could not create BYE - no Contact header for dialog");
    	return simpleSipStack.createRequest(dialogInfo.getRemoteContact().getURI().toString(), Request.BYE, dialogInfo.getSipCallId(), dialogInfo.getSequenceNumber(), dialogInfo.getLocalParty(), dialogInfo.getLocalTag(), dialogInfo.getRemoteParty(), dialogInfo.getRemoteTag(), null, dialogInfo.getRouteList(), null);
    }

    public Request createCancelRequest(final ReadOnlyDialogInfo dialogInfo) {
    	if(dialogInfo.getRemoteParty() == null)
    		throw new IllegalStateException("Could not create CANCEL - no Contact header for dialog");
    	return simpleSipStack.createRequest(dialogInfo.getRemoteParty().getURI().toString(), Request.CANCEL, dialogInfo.getSipCallId(), dialogInfo.getSequenceNumber(), dialogInfo.getLocalParty(), dialogInfo.getLocalTag(), dialogInfo.getRemoteParty(), dialogInfo.getRemoteTag(), dialogInfo.getViaBranchId(), dialogInfo.getRouteList(), null);
    }

    public Request createAckRequest(final ReadOnlyDialogInfo dialogInfo) {
    	if(dialogInfo.getRemoteContact() == null)
    		throw new IllegalStateException("Could not create ACK - no Contact header for dialog");
    	return simpleSipStack.createRequest(dialogInfo.getRemoteContact().getURI().toString(), Request.ACK, dialogInfo.getSipCallId(), dialogInfo.getSequenceNumber(), dialogInfo.getLocalParty(), dialogInfo.getLocalTag(), dialogInfo.getRemoteParty(), dialogInfo.getRemoteTag(), null, dialogInfo.getRouteList(), null);
    }

    public Request createInviteOkAckRequest(final ReadOnlyDialogInfo dialogInfo, ResponseEvent okResponseEvent) {
    	return simpleSipStack.createAckRequest(okResponseEvent, dialogInfo.getRemoteContact().getURI().toString(), dialogInfo.getRouteList());
    }

    //TODO: LOW: Test for this inside SimpleSipStack
    public Request createInfoRequest(final ReadOnlyDialogInfo dialogInfo) {
    	if(dialogInfo.getRemoteContact() == null)
    		throw new IllegalStateException("Could not create INFO - no Contact header for dialog");
    	return simpleSipStack.createRequest(dialogInfo.getRemoteContact().getURI().toString(), Request.INFO, dialogInfo.getSipCallId(), dialogInfo.getSequenceNumber(), dialogInfo.getLocalParty(), dialogInfo.getLocalTag(), dialogInfo.getRemoteParty(), dialogInfo.getRemoteTag(), null, dialogInfo.getRouteList(), null);
    }

    protected boolean canSendRequest(String requestMethod, DialogState dialogState, TerminationMethod terminationMethod) {
    	boolean canSend = true;
    	if (dialogState.equals(DialogState.Terminated))
    		canSend = false;
    	else if (dialogState.equals(DialogState.Early) && !(requestMethod.equals(Request.ACK) || requestMethod.equals(Request.CANCEL)))
    		canSend = false;
    	else if (dialogState.equals(DialogState.Initiated))
    		canSend = false;
    	else if (dialogState.equals(DialogState.Created) && !requestMethod.equals(Request.INVITE))
    		canSend = false;
		else {
			if (TerminationMethod.Terminate.equals(terminationMethod) && !(requestMethod.equals(Request.BYE) || requestMethod.equals(Request.ACK) || requestMethod.equals(Request.CANCEL)))
				canSend = false;
			else if (TerminationMethod.Cancel.equals(terminationMethod) && !(requestMethod.equals(Request.ACK) || requestMethod.equals(Request.CANCEL)))
				canSend = false;
		}

    	if(!canSend)
    		LOG.info(String.format("Can't send %s request - status is %s, termination method is %s", requestMethod, dialogState, terminationMethod));
		return canSend;
    }

    private SessionDescription createSessionDescription(String sdp) {
        try {
            return SdpFactory.getInstance().createSessionDescription(sdp);
        } catch (SdpException e) {
            throw new StackException("Unable to parse SDP", e);
        }
    }

	public boolean hasActiveVideoMediaDescription(String sdp) {
		SessionDescription sessionDescription = createSessionDescription(sdp);
		return SessionDescriptionHelper.hasActiveVideoMediaDescription(sessionDescription);
	}

    public MediaDescription getActiveMediaDescriptionFromMessageBody(String sdp) {
        SessionDescription sessionDescription = createSessionDescription(sdp);
        return SessionDescriptionHelper.getActiveMediaDescription(sessionDescription);
    }

	protected long enqueueRequestGetSequenceNumber(String sipCallId, long sequenceNumber, String requestMethod) {
    	return simpleSipStack.enqueueRequestAssignSequenceNumber(sipCallId, sequenceNumber, requestMethod);
    }

    public void enqueueRequestForceSequenceNumber(String sipCallId, long sequenceNumber, String requestMethod) {
    	simpleSipStack.enqueueRequestForceSequenceNumber(sipCallId, sequenceNumber, requestMethod);
    }

    protected void dequeueRequest(String sipCallId, long sequenceNumber, String requestMethod) {
    	simpleSipStack.dequeueRequest(sipCallId, sequenceNumber, requestMethod);
    }
    
    public ProxyAuthorizationHeader createProxyAuthorizationHeader(String schema){
    	try {
			return simpleSipStack.getHeaderFactory().createProxyAuthorizationHeader(schema);
		} catch (ParseException e) {
			throw new StackException("Unable to create proxy authorisation header", e);
		}
    }

	public AuthorizationHeader createAuthorizationHeader(String schema) {
    	try {
			return simpleSipStack.getHeaderFactory().createAuthorizationHeader(schema);
		} catch (ParseException e) {
			throw new StackException("Unable to create authorisation header", e);
		}
	}

	public void sendWWWAuthenticationRequiredResponse(Request request,ServerTransaction serverTransaction, String sipUserName, String realm, String nonce) {
        try {
            Response response = simpleSipStack.getMessageFactory().createResponse(Response.UNAUTHORIZED, request);
            simpleSipStack.addContactHeader(response, sipUserName);
    		WWWAuthenticateHeader h = simpleSipStack.getHeaderFactory().createWWWAuthenticateHeader("Digest");
    		h.setNonce(nonce);
    		h.setRealm(realm);
    		response.addHeader(h);

            sendResponse(response, serverTransaction);
        } catch (ParseException e) {
            throw new StackException("Unable to send 401 response", e);
        }
		
	}
}
