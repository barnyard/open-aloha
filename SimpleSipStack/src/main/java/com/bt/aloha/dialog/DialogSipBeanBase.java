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

 	

 	
 	
 
/**
 * (c) British Telecommunications plc, 2007, All Rights Reserved
 */
package com.bt.aloha.dialog;

import gov.nist.javax.sip.header.ProxyAuthenticate;
import gov.nist.javax.sip.header.SIPHeaderNames;
import gov.nist.javax.sip.header.WWWAuthenticate;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.sdp.MediaDescription;
import javax.sip.ClientTransaction;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionUnavailableException;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.Header;
import javax.sip.header.ProxyAuthorizationHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.event.DialogConnectionFailedEvent;
import com.bt.aloha.dialog.event.DialogDisconnectedEvent;
import com.bt.aloha.dialog.event.DialogRefreshCompletedEvent;
import com.bt.aloha.dialog.event.DialogTerminatedEvent;
import com.bt.aloha.dialog.event.DialogTerminationFailedEvent;
import com.bt.aloha.dialog.event.ReceivedDialogRefreshEvent;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.ImmutableDialogInfo;
import com.bt.aloha.dialog.state.PendingReinvite;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.dialog.state.ReinviteInProgress;
import com.bt.aloha.dialog.state.TerminationCause;
import com.bt.aloha.dialog.state.TerminationMethod;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.stack.SimpleSipBeanBase;
import com.bt.aloha.stack.SimpleSipStack;
import com.bt.aloha.stack.StackException;
import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateManager;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;

public abstract class DialogSipBeanBase extends SimpleSipBeanBase implements DialogSipBean {
    protected static final String PROCESSING_OK_RESPONSE_FOR_OUTBOUND_DIALOG_S = "Processing OK response for outbound dialog %s";
    protected static final String FOUND_SDP_IN_ACK_FOR_DIALOG_S_S = "Found SDP in ACK for dialog %s:\n%s";
    protected static final String DIALOG_S_CONNECTED = "Dialog %s connected";
	private static final String MD5 = "MD5";
    private static final String SETTING_REMOTE_CONTACT_HEADER_FOR_DIALOG_S_TO_S = "Setting remote contact header for dialog %s to %s";
    private static final Log LOG = LogFactory.getLog(DialogSipBeanBase.class);
	private DialogCollection dialogCollection;
	private ConcurrentUpdateManager concurrentUpdateManager = new ConcurrentUpdateManagerImpl();
	private SimpleSipStack simpleSipStack;
	private DialogBeanHelper dialogBeanHelper = new DialogBeanHelper();
	private List<DialogSipListener> listeners = new ArrayList<DialogSipListener>();
	protected abstract void endNonConfirmedDialog(ReadOnlyDialogInfo readOnlyDialogInfo, TerminationMethod previousTerminationMethod) ;

	public void setDialogCollection(DialogCollection collection) {
		this.dialogCollection = collection;
	}

	protected DialogCollection getDialogCollection() {
		return this.dialogCollection;
	}

	protected ConcurrentUpdateManager getConcurrentUpdateManager() {
		return concurrentUpdateManager;
	}

	protected SimpleSipStack getSimpleSipStack() {
		return simpleSipStack;
	}

	public void setSimpleSipStack(SimpleSipStack aSimpleSipStack) {
		this.simpleSipStack = aSimpleSipStack;
		this.getDialogBeanHelper().setSimpleSipStack(aSimpleSipStack);
	}

	public DialogBeanHelper getDialogBeanHelper() {
		return dialogBeanHelper;
	}

	public void setDialogBeanHelper(DialogBeanHelper aDialogBeanHelper) {
		this.dialogBeanHelper = aDialogBeanHelper;
	}

	public List<DialogSipListener> getDialogListeners() {
		return this.listeners;
	}

	public void setDialogSipListeners(List<DialogSipListener> dialogListenerList) {
		this.listeners = dialogListenerList;
	}

	protected void addDialogSipListener(DialogSipListener dialogListener) {
		if (dialogListener == null)
			throw new IllegalArgumentException("Cannot add a null dialog sip listener");
		this.listeners.add(dialogListener);
	}

	protected void removeDialogSipListener(DialogSipListener dialogListener) {
		if (dialogListener == null)
			throw new IllegalArgumentException("Cannot remove a null dialog sip listener");
		this.listeners.remove(dialogListener);
	}

	public void processRequest(Request request, ServerTransaction serverTransaction, final ImmutableDialogInfo dialogInfo) {
		String dialogId = dialogInfo.getId();
		if (request.getMethod().equals(Request.INVITE)) {
			LOG.debug(String.format("Processing reINVITE request for %s", dialogId));
			processReinvite(request, serverTransaction, dialogId);
		} else if (request.getMethod().equals(Request.ACK)) {
			processReinviteAck(request, serverTransaction, dialogId);
		} else if (request.getMethod().equals(Request.BYE)) {
			processBye(request, serverTransaction, dialogId);
		} else if (request.getMethod().equals(Request.INFO)) {
			processInfo(request, serverTransaction, dialogId);
		} else {
			throw new UnsupportedRequestException("Unsupported request: " + request.getMethod());
		}
	}

	public void processResponse(ResponseEvent responseEvent, final ImmutableDialogInfo dialogInfo) {
		Response response = responseEvent.getResponse();
		String responseMethod = ((CSeqHeader) response.getHeader(CSeqHeader.NAME)).getMethod();
		if (responseMethod.equals(Request.INVITE)) {
			processReinviteResponse(responseEvent, dialogInfo.getId());
		} else if (responseMethod.equals(Request.BYE)) {
			processByeResponse(responseEvent, dialogInfo.getId());
		} else if (responseMethod.equals(Request.INFO)) {
			processInfoResponse(responseEvent, dialogInfo.getId());
		} else {
			LOG.warn(String.format("Dialog %s got nsupported response method %s, throwing response away", dialogInfo.getId(), responseMethod));
		}
	}

	protected void processReinvite(final Request request, final ServerTransaction serverTransaction, final String dialogId) {
		LOG.debug(String.format("Processing REINVITE request for dialog %s", dialogId));
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				DialogState dialogState = dialogInfo.getDialogState();
				// TODO: LOW until we see it - letting stuff through on EARLY - how do we make sure an initial ACK processed after a REINVITE
				// does not overwrite session info?
				// perhaps by storing the remote seq number?
				if (dialogState.ordinal() < DialogState.Early.ordinal()) {
					LOG.warn(String.format("Throwing away reinvite for dialog %s, state is %s", dialogId, dialogInfo.getDialogState()));
					return;
				}
				
				if (dialogInfo.getReinviteInProgess().ordinal() > ReinviteInProgress.None.ordinal()) {
					LOG.warn(String.format("Reinvite already in progress for dialog %s, responding with 491 Request Pending", dialogId));
					Response response;
					try {
						response = getSimpleSipStack().getMessageFactory().createResponse(Response.REQUEST_PENDING, request);
					} catch (ParseException e) {
						throw new StackException(e.getMessage(), e);
					}
					getDialogBeanHelper().sendResponse(response, serverTransaction);
					return;
				}

				if (request.getContentLength().getContentLength() == 0) {
					LOG.warn(String.format("Reinvite received for dialog %s with empty media, responding with 488 Not acceptable here", dialogId));
					Response response;
					try {
						response = getSimpleSipStack().getMessageFactory().createResponse(Response.NOT_ACCEPTABLE_HERE, request);
					} catch (ParseException e) {
						throw new StackException(e.getMessage(), e);
					}
					getDialogBeanHelper().sendResponse(response, serverTransaction);
					return;
				}

				dialogInfo.setReinviteInProgess(ReinviteInProgress.ReceivedReinvite);
				dialogInfo.setInviteServerTransaction(serverTransaction);
				updateDialogInfoFromInviteRequest(dialogInfo, request);

				getDialogCollection().replace(dialogInfo);

				getEventDispatcher().dispatchEvent(getDialogListeners(), new ReceivedDialogRefreshEvent(dialogId, dialogInfo.getRemoteOfferMediaDescription(), dialogInfo.getRemoteContact().getURI().toString(), null, false));
			}

			public String getResourceId() {
				return dialogId;
			}
		};

		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	protected void processBye(final Request request, final ServerTransaction serverTransaction, final String dialogId) {
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				LOG.debug(String.format("Processing BYE request for %s", dialogId));
				if (dialogInfo.setDialogState(DialogState.Terminated) != null) {
					dialogInfo.setTerminationCause(TerminationCause.RemotePartyHungUp);
					getDialogCollection().replace(dialogInfo);
					getDialogBeanHelper().sendResponse(request, serverTransaction, Response.OK);
					final DialogDisconnectedEvent disconnectedEvent = new DialogDisconnectedEvent(dialogInfo.getId(), dialogInfo.getTerminationCause());
					getEventDispatcher().dispatchEvent(getDialogListeners(), disconnectedEvent);
				} else {
					LOG.info(String.format("BYE request received for terminated dialog %s, responding with OK", dialogId));
					getDialogBeanHelper().sendResponse(request, serverTransaction, Response.OK);
				}
			}

			public String getResourceId() {
				return dialogId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	public void processReinviteAck(final Request request, final ServerTransaction serverTransaction, final String dialogId) {
        LOG.debug(String.format("Processing REINVITE-ACK request for dialog %s", dialogId));
		final String remoteSdp = getDialogBeanHelper().getRemoteSdpFromRequest(request);
		if (remoteSdp == null) {
			LOG.debug(String.format("No SDP found in ACK for dialog %s", dialogId));
		} else {
			LOG.info(String.format(FOUND_SDP_IN_ACK_FOR_DIALOG_S_S, dialogId, remoteSdp));
			ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
				public void execute() {
					DialogInfo dialogInfo = getDialogCollection().get(dialogId);
					MediaDescription answerMediaDescription = getDialogBeanHelper().getActiveMediaDescriptionFromMessageBody(remoteSdp);
					SessionDescriptionHelper.updateDynamicMediaPayloadMappings(answerMediaDescription, dialogInfo.getDynamicMediaPayloadTypeMap());

					getDialogCollection().replace(dialogInfo);
				}
				public String getResourceId() {
					return dialogId;
				}
			};
			getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
		}
	}

	protected void processInfo(final Request request, final ServerTransaction serverTransaction, final String dialogId) {
		LOG.debug(String.format("Responding with OK to INFO request for %s", dialogId));
		getDialogBeanHelper().sendResponse(request, serverTransaction, Response.OK);
	}

	protected void processByeResponse(final ResponseEvent re, final String dialogId) {
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				LOG.debug(String.format("Processing BYE response for %s", dialogId));
				DialogState previousState = dialogInfo.setDialogState(DialogState.Terminated);
				dialogInfo.setTerminationMethod(TerminationMethod.None);
				getDialogCollection().replace(dialogInfo);

				if (re.getResponse().getStatusCode() >= Response.OK && re.getResponse().getStatusCode() < Response.MULTIPLE_CHOICES) {
					if (previousState != null) {
						final DialogTerminatedEvent terminatedEvent = new DialogTerminatedEvent(dialogInfo.getId(), dialogInfo.getTerminationCause());
						getEventDispatcher().dispatchEvent(getDialogListeners(), terminatedEvent);
					}
				} else {
					LOG.warn(String.format("Dialog %s responded to BYE with status code %d", dialogInfo.getId(), re.getResponse().getStatusCode()));
					if (previousState != null) {
						final DialogTerminationFailedEvent terminationFailedEvent = new DialogTerminationFailedEvent(dialogInfo.getId(), dialogInfo.getTerminationCause());
						getEventDispatcher().dispatchEvent(getDialogListeners(), terminationFailedEvent);
					}
				}
			}

			public String getResourceId() {
				return dialogId;
			}
		};

		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	protected void processInfoResponse(final ResponseEvent re, final String dialogId) {
		LOG.debug(String.format("Default processing for INFO response for %s", dialogId));
		if (re.getResponse().getStatusCode() != Response.OK){
			LOG.warn(String.format("Dialog %s responded to INFO with status code %d", dialogId, re.getResponse().getStatusCode()));
			if (re.getResponse().getStatusCode() == Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST){

				ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
					public void execute() {
						DialogInfo dialogInfo = getDialogCollection().get(dialogId);
						LOG.debug(String.format("Processing CALL_OR_TRANSACTION_DOES_NOT_EXIST (481) response for %s", dialogId));
						if (dialogInfo.setDialogState(DialogState.Terminated) != null) {
							//TODO should this be a different cause as the call may have ended some time earlier?
							dialogInfo.setTerminationCause(TerminationCause.RemotePartyHungUp);
							getDialogCollection().replace(dialogInfo);
							final DialogDisconnectedEvent disconnectedEvent = new DialogDisconnectedEvent(dialogInfo.getId(), dialogInfo.getTerminationCause());
							getEventDispatcher().dispatchEvent(getDialogListeners(), disconnectedEvent);
						} else {
							LOG.info(String.format("INFO response received for terminated dialog %s", dialogId));
						}
					}

					public String getResourceId() {
						return dialogId;
					}
				};
				getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
			}
		}
	}

	protected void processReinviteResponse(final ResponseEvent re, final String dialogId) {
		LOG.debug(String.format("Processing INVITE response for %s", dialogId));
		if (re.getResponse().getStatusCode() >= Response.BAD_REQUEST) {
			if ( (re.getResponse().getStatusCode() == Response.PROXY_AUTHENTICATION_REQUIRED || re.getResponse().getStatusCode() == Response.UNAUTHORIZED) )
				processReInviteUnauthorised(re, dialogId);
			else
				processReinviteErrorResponse(re, dialogId);
		} else if (re.getResponse().getStatusCode() < Response.OK) {
			LOG.info(String.format("Unexpected provisional response received for REINVITE for dialog %s, ignoring", dialogId));
		} else if (re.getResponse().getStatusCode() == Response.OK) {
			processReinviteOkResponse(re, dialogId);
		}
	}
	
	private void processReInviteUnauthorised(ResponseEvent re,	String dialogId) {
		// Send Invite with digest response.
		sendAuthorisationReInvite(dialogId, re.getResponse(), false);
	}
	
	protected void sendAuthorisationReInvite(final String dialogId, final Response response) {
		sendAuthorisationReInvite(dialogId, response, true);
	}
	
	protected void sendAuthorisationReInvite(final String dialogId, final Response response, final boolean initialInvite) {
		LOG.debug(String.format("Sending Authorisation Re Invite : %s ", dialogId));
		ConcurrentUpdateBlock concurrentUpdateBlock = new DialogConcurrentUpdateBlock(getDialogBeanHelper()) {
			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				if (dialogInfo == null) {
					LOG.warn(String.format("DialogInfo with id %s not found in collection", dialogId));
					return;
				}
				
				assignSequenceNumber(dialogInfo, Request.INVITE);
				// The next lines update the Initial Sequence number in the dialogInfo 
				// This is done because the second invite (in response to the 40[1|7] ) should not be treated as a reinvite but handled in the same way as an initial invite.
				if (initialInvite)
					dialogInfo.setInitialInviteTransactionSequenceNumber(dialogInfo.getSequenceNumber());
				getDialogCollection().replace(dialogInfo);

				String requestURI = dialogInfo.getRemoteContact() == null ? dialogInfo.getRemoteParty().getURI().toString() : dialogInfo.getRemoteContact().getURI().toString();
				Request dialogAuthenticationReinviteRequest = getDialogBeanHelper().createReinviteRequest(requestURI, dialogInfo, !initialInvite);
				Header authHeader;
				if (response.getStatusCode()== Response.PROXY_AUTHENTICATION_REQUIRED)
					authHeader = createProxyAuthenticationHeader(response, dialogInfo.getUsername(), dialogInfo.getPassword(), dialogAuthenticationReinviteRequest.getRequestURI() );
				else 
					authHeader = createAuthenticationHeader(response, dialogInfo.getUsername(), dialogInfo.getPassword(), dialogAuthenticationReinviteRequest.getRequestURI() );
					
				dialogAuthenticationReinviteRequest.addHeader(authHeader);
				
				ClientTransaction clientTransaction = null;
				try {
					clientTransaction = getSimpleSipStack().getSipProvider().getNewClientTransaction(dialogAuthenticationReinviteRequest);
				} catch (TransactionUnavailableException e) {
					throw new StackException(e.getMessage(), e);
				}
				
				getSimpleSipStack().sendRequest(clientTransaction);
			}

			public String getResourceId() {
				return dialogId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	protected Header createProxyAuthenticationHeader(Response response, String username, String password, javax.sip.address.URI uri){
        try {
            String schema = ((ProxyAuthenticate)(response.getHeader(SIPHeaderNames.PROXY_AUTHENTICATE))).getScheme();
            String nonce = ((ProxyAuthenticate)(response.getHeader(SIPHeaderNames.PROXY_AUTHENTICATE))).getNonce();
            String realm = ((ProxyAuthenticate)(response.getHeader(SIPHeaderNames.PROXY_AUTHENTICATE))).getRealm();
            
            ProxyAuthorizationHeader proxyAuthheader = getDialogBeanHelper().createProxyAuthorizationHeader(schema);
            proxyAuthheader.setRealm(realm);
            proxyAuthheader.setNonce(nonce);
            proxyAuthheader.setAlgorithm(MD5);
            proxyAuthheader.setUsername(username == null ? "" : username);
            proxyAuthheader.setURI(uri);
            DigestClientAuthenticationMethod digest=new DigestClientAuthenticationMethod();
        
            String digestHeader = digest.generateResponse(realm, username, uri.toString(), nonce, password, ((CSeqHeader) response.getHeader(CSeqHeader.NAME)).getMethod(), null, MD5);
            proxyAuthheader.setResponse(digestHeader);

            return proxyAuthheader;
        }catch (ParseException pa){
            throw new StackException("Error creating proxy authentication header ",pa);
        }
	}

	protected Header createAuthenticationHeader(Response response, String username, String password, javax.sip.address.URI uri){
        try {
            String schema = ((WWWAuthenticate)(response.getHeader(SIPHeaderNames.WWW_AUTHENTICATE))).getScheme();
            String nonce = ((WWWAuthenticate)(response.getHeader(SIPHeaderNames.WWW_AUTHENTICATE))).getNonce();
            String realm = ((WWWAuthenticate)(response.getHeader(SIPHeaderNames.WWW_AUTHENTICATE))).getRealm();
            
            AuthorizationHeader authheader = getDialogBeanHelper().createAuthorizationHeader(schema);
            authheader.setRealm(realm);
            authheader.setNonce(nonce);
            authheader.setAlgorithm(MD5);
            authheader.setUsername(username == null ? "" : username);
            authheader.setURI(uri);
            DigestClientAuthenticationMethod digest=new DigestClientAuthenticationMethod();
        
            String digestHeader = digest.generateResponse(realm, username, uri.toString(), nonce, password, ((CSeqHeader) response.getHeader(CSeqHeader.NAME)).getMethod(), null, MD5);
            authheader.setResponse(digestHeader);

            return authheader;
        }catch (ParseException pa){
            throw new StackException("Error creating authentication header ",pa);
        }
	}
	
	public void processTimeout(final TimeoutEvent timeoutEvent, final String dialogId) {
		LOG.debug(String.format("Processing timeout for dialog %s", dialogId));

		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				dialogInfo.setTerminationCause(TerminationCause.SipSessionError);
				dialogInfo.setInviteClientTransaction(null);
				DialogState previousDialogState = dialogInfo.setDialogState(DialogState.Terminated);
				if (previousDialogState != null) {
					getDialogCollection().replace(dialogInfo);

					if (timeoutEvent.isServerTransaction() || timeoutEvent.getClientTransaction() == null) {
						LOG.info(String.format("Got TIMEOUT event for server transaction for dialog %s", dialogId));
						return;
					}

					if (previousDialogState.ordinal() < DialogState.Confirmed.ordinal()) {
						DialogConnectionFailedEvent connectionFailedEvent = new DialogConnectionFailedEvent(dialogId, dialogInfo.getTerminationCause());
						getEventDispatcher().dispatchEvent(getDialogListeners(), connectionFailedEvent);
					} else {
						DialogTerminatedEvent terminatedEvent = new DialogTerminatedEvent(dialogId, dialogInfo.getTerminationCause());
						getEventDispatcher().dispatchEvent(getDialogListeners(), terminatedEvent);

						if (!Request.BYE.equals(timeoutEvent.getClientTransaction().getRequest().getMethod())) {
							if (TerminationMethod.Terminate != dialogInfo.getTerminationMethod()) {
								LOG.debug(String.format("Sending BYE to terminating dialog %s after non-BYE request timed out", dialogId));
								getEventDispatcher().getTaskExecutor().execute(new DialogTerminationTask(dialogId));
							} else {
								LOG.debug(String.format("Doing NOTHING after setting dialog %s to TERMINATED in response to timeout event, this dialog is already being terminated", dialogId));
							}
						} else {
							LOG.debug(String.format("Doing NOTHING after setting dialog %s to TERMINATED in response to BYE request timeout", dialogId));
						}
					}
				}
			}

			public String getResourceId() {
				return dialogId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	protected void processReinviteOkResponse(final ResponseEvent responseEvent, final String dialogId) {
		ConcurrentUpdateBlock concurrentUpdateBlock = new DialogConcurrentUpdateBlock(getDialogBeanHelper()) {
			public void execute() {
				LOG.debug(String.format(PROCESSING_OK_RESPONSE_FOR_OUTBOUND_DIALOG_S, dialogId));
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				ReinviteInProgress reinviteInProgess = dialogInfo.getReinviteInProgess();

				if (handleInviteOkResponseIfResent(responseEvent, dialogInfo)) {
					LOG.debug(String.format("Handled resent reinvite OK response for dialog %s", dialogId));
					return;
				} else if (DialogState.Confirmed.equals(dialogInfo.getDialogState()) && TerminationMethod.None.equals(dialogInfo.getTerminationMethod())) {
					updateDialogInfoFromInviteOkResponse(dialogInfo, responseEvent.getResponse());
					Request ackRequest = null;
					if (reinviteInProgess.equals(ReinviteInProgress.SendingReinviteWithSessionDescription)) {
						dialogInfo.setReinviteInProgess(ReinviteInProgress.None);
						forceSequenceNumber(dialogInfo.getSipCallId(), dialogInfo.getLastReceivedOkSequenceNumber(), Request.ACK);
						ackRequest = getDialogBeanHelper().createInviteOkAckRequest(dialogInfo, responseEvent);
						dialogInfo.setLastAckRequest(ackRequest);
					}
					getDialogCollection().replace(dialogInfo);

					if (reinviteInProgess.equals(ReinviteInProgress.SendingReinviteWithSessionDescription)) {
						LOG.debug(String.format("Got OK for reinvite WITH SDP for dialog %s, sending empty ACK & raising refresh completed event", dialogId));
						if (responseEvent.getResponse().getRawContent() != null) {
							sendReinviteAck(ackRequest, dialogInfo);

							MediaDescription negotiatedMediaDescription = getDialogBeanHelper().getActiveMediaDescriptionFromMessageBody(new String(responseEvent.getResponse().getRawContent()));
							final DialogRefreshCompletedEvent dialogRefreshCompletedEvent = new DialogRefreshCompletedEvent(dialogId, dialogInfo.getApplicationData(), negotiatedMediaDescription);
							getEventDispatcher().dispatchEvent(getDialogListeners(), dialogRefreshCompletedEvent);
						} // else {
							// TODO: LOW - need to properly handle case when no SDP in OK resposne
						//}
					} else {
						LOG.debug(String.format("Got OK for reinvite (%s) for dialog %s, raising refresh completed event & sending empty ACK", reinviteInProgess, dialogId));
						String remoteContact = dialogInfo.getRemoteContact() != null ? dialogInfo.getRemoteContact().getURI().toString() : null;
						final ReceivedDialogRefreshEvent receivedDialogRefreshEvent = new ReceivedDialogRefreshEvent(dialogId, dialogInfo.getRemoteOfferMediaDescription(), remoteContact, dialogInfo.getApplicationData(), true);
						getEventDispatcher().dispatchEvent(getDialogListeners(), receivedDialogRefreshEvent);
					}
				} else {
					LOG.debug(String.format("Received OK response to a reinvite for Dialog %s with State %s and TerminationMethod %s, ACKing, but ignoring otherwise", dialogId, dialogInfo.getDialogState(), dialogInfo.getTerminationMethod()));
					getDialogBeanHelper().enqueueRequestForceSequenceNumber(dialogId, ((CSeqHeader)responseEvent.getResponse().getHeader(CSeqHeader.NAME)).getSeqNumber(), Request.ACK);
					Request ackRequest = getDialogBeanHelper().createInviteOkAckRequest(dialogInfo, responseEvent);
					sendReinviteAck(ackRequest, dialogInfo);
				}
			}
			
			public String getResourceId() {
				return dialogId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	protected boolean handleInviteOkResponseIfResent(ResponseEvent responseEvent, ReadOnlyDialogInfo dialogInfo) {
		long okResponseSequenceNumber = ((CSeqHeader)responseEvent.getResponse().getHeader(CSeqHeader.NAME)).getSeqNumber();
		if (dialogInfo.getLastReceivedOkSequenceNumber() < okResponseSequenceNumber)
			return false;

		Request lastAckRequest = dialogInfo.getLastAckRequest();
		if (lastAckRequest != null && ((CSeqHeader)lastAckRequest.getHeader(CSeqHeader.NAME)).getSeqNumber() == okResponseSequenceNumber) {
			LOG.debug(String.format("Received resent OK response for dialog %s - resending ACK", dialogInfo.getId()));
			getDialogBeanHelper().enqueueRequestForceSequenceNumber(dialogInfo.getId(), ((CSeqHeader)lastAckRequest.getHeader(CSeqHeader.NAME)).getSeqNumber(), Request.ACK);
			getDialogBeanHelper().sendRequest(lastAckRequest, true);
		} else {
			LOG.info(String.format("Received resent OK response for dialog %s - ignoring", dialogInfo.getId()));
		}
		return true;
	}

	private void sendReinviteAck(final Request ackRequest, final ReadOnlyDialogInfo dialogInfo) {
		getDialogBeanHelper().sendRequest(ackRequest);

		if (dialogInfo.getPendingReinvite() != null)
			reinviteDialog(dialogInfo.getId(), dialogInfo.getPendingReinvite().getMediaDescription(), dialogInfo.getPendingReinvite().getAutoTerminate(), dialogInfo.getPendingReinvite().getApplicationData());
	}

	protected void processReinviteErrorResponse(final ResponseEvent responseEvent, final String dialogId) {
		LOG.debug(String.format("Processing REINVITE error for dialog %s", dialogId));
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				LOG.info(String.format("Got reinvite error response %d for dialog %s, need to terminate", responseEvent.getResponse().getStatusCode(), dialogInfo.getId()));
				dialogInfo.setReinviteInProgess(ReinviteInProgress.None);
				dialogInfo.setPendingReinvite(null);
				dialogInfo.setTerminationCause(TerminationCause.SipSessionError);
				if (dialogInfo.setTerminationMethod(TerminationMethod.Terminate) != null) {
					getDialogCollection().replace(dialogInfo);
					getEventDispatcher().getTaskExecutor().execute(new DialogTerminationTask(dialogId));
				}
			}

			public String getResourceId() {
				return dialogId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	protected class DialogTerminationTask implements Runnable {
		private final String dialogId;

		public DialogTerminationTask(String aDialogId) {
			this.dialogId = aDialogId;
		}

		public void run() {
			ConcurrentUpdateBlock concurrentUpdateBlock = new DialogConcurrentUpdateBlock(getDialogBeanHelper()) {
				public void execute() {
					DialogInfo dialogInfo = getDialogCollection().get(dialogId);
					assignSequenceNumber(dialogInfo, Request.BYE);
					getDialogCollection().replace(dialogInfo);
					Request byeRequest = getDialogBeanHelper().createByeRequest(dialogInfo);
					getDialogBeanHelper().sendRequest(byeRequest);
				}

				public String getResourceId() {
					return dialogId;
				}
			};
			try {
				getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
			} catch (Throwable t) {
				LOG.error(String.format("Failed to terminate dialog %s scheduled for termination", dialogId), t);
			}
		}
	}

	protected void updateDialogInfoFromInviteRequest(DialogInfo dialogInfo, Request request) {
		String sdpString = getDialogBeanHelper().getRemoteSdpFromRequest(request);
		if (sdpString != null) {
			MediaDescription offerMediaDescription = getDialogBeanHelper().getActiveMediaDescriptionFromMessageBody(sdpString);
			SessionDescriptionHelper.updateDynamicMediaPayloadMappings(offerMediaDescription, dialogInfo.getDynamicMediaPayloadTypeMap());
			dialogInfo.setRemoteOfferMediaDescription(offerMediaDescription);
		}

		String contactHeader = getDialogBeanHelper().extractRemoteContactHeaderFromRequest(request);
		if (contactHeader != null) {
			LOG.debug(String.format(SETTING_REMOTE_CONTACT_HEADER_FOR_DIALOG_S_TO_S, dialogInfo.getId(), contactHeader));
			dialogInfo.setRemoteContact(contactHeader);
		} else {
			LOG.warn(String.format("Invite or reinvite request did not have a Contact header"));
		}
	}

	protected void updateDialogInfoFromInviteOkResponse(DialogInfo dialogInfo, Response response) {
		setRemoteContactHeader(response, dialogInfo);
		dialogInfo.setLastReceivedOkSequenceNumber(((CSeqHeader)response.getHeader(CSeqHeader.NAME)).getSeqNumber());

		if(response.getRawContent() != null) {
			String body = new String(response.getRawContent());
			MediaDescription mediaDescription = getDialogBeanHelper().getActiveMediaDescriptionFromMessageBody(body);
			SessionDescriptionHelper.updateDynamicMediaPayloadMappings(mediaDescription, dialogInfo.getDynamicMediaPayloadTypeMap());
			if (!dialogInfo.isSdpInInitialInvite()
					|| dialogInfo.getReinviteInProgess().equals(ReinviteInProgress.SendingReinviteWithoutSessionDescription)){
				LOG.debug(String.format("Setting offered media description for %s to %s", dialogInfo.getId(), body));
				dialogInfo.setRemoteOfferMediaDescription(mediaDescription);
			} else {
				LOG.debug(String.format("Did not update offer media from invite OK response for dialog %s - OK response contained SDP answer", dialogInfo.getId()));
			}
		} else {
			LOG.warn(String.format("No SDP in OK response for %s", dialogInfo.getId()));
		}
	}

	protected void setRemoteContactHeader(Response response, DialogInfo dialogInfo) {
		String remoteContactHeader = getDialogBeanHelper().extractRemoteContactHeaderFromResponse(response);
		if (remoteContactHeader != null) {
			LOG.debug(String.format(SETTING_REMOTE_CONTACT_HEADER_FOR_DIALOG_S_TO_S, dialogInfo.getId(), remoteContactHeader));
			dialogInfo.setRemoteContact(remoteContactHeader);
		} else {
			LOG.warn(String.format("Response for dialog %s did not have a Contact header", dialogInfo.getId()));
		}
	}

	protected void replaceDialogIfCanSendRequest(String requestMethod, DialogState dialogState, TerminationMethod terminationMethod, final DialogInfo dialogInfo) {
		if (getDialogBeanHelper().canSendRequest(requestMethod, dialogState, terminationMethod)) {
			getDialogCollection().replace(dialogInfo);
			return;
		}
		throw new IllegalStateException(String.format("NOT sending %s request, invalid dialog state %s for dialog %s", requestMethod, dialogInfo
				.getDialogState(), dialogInfo.getId()));
	}

	protected void reinviteDialog(final String dialogId, final MediaDescription offerMediaDescription, final Boolean autoTerminate, final String applicationData) {
		LOG.debug(String.format("Reinviting dialog: %s with autoTerminate set to %s and app data %s", dialogId, autoTerminate, applicationData));
		ConcurrentUpdateBlock concurrentUpdateBlock = new DialogConcurrentUpdateBlock(getDialogBeanHelper()) {
			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				if (dialogInfo == null) {
					LOG.warn(String.format("DialogInfo with id %s not found in collection", dialogId));
					return;
				}
				if ((DialogState.Early.equals(dialogInfo.getDialogState()) || DialogState.Confirmed.equals(dialogInfo.getDialogState())) && TerminationMethod.None.equals(dialogInfo.getTerminationMethod())) {

					if (DialogState.Early.equals(dialogInfo.getDialogState()) || dialogInfo.getReinviteInProgess().ordinal() > ReinviteInProgress.None.ordinal()) {
						LOG.info(String.format("QUEUEING UP reinvite as Invite already in progress for %s", dialogId));
						PendingReinvite pendingReinvite = new PendingReinvite(offerMediaDescription, autoTerminate, applicationData);
						dialogInfo.setPendingReinvite(pendingReinvite);
						getDialogCollection().replace(dialogInfo);
						return;
					}

					if (autoTerminate != null)
						dialogInfo.setAutoTerminate(autoTerminate);

					dialogInfo.setApplicationData(applicationData);
					assignSequenceNumber(dialogInfo, Request.INVITE);
					dialogInfo.setPendingReinvite(null);
					if (offerMediaDescription == null) {
						dialogInfo.setReinviteInProgess(ReinviteInProgress.SendingReinviteWithoutSessionDescription);
					} else {
						dialogInfo.setReinviteInProgess(ReinviteInProgress.SendingReinviteWithSessionDescription);
						SessionDescriptionHelper.setMediaDescription(dialogInfo.getSessionDescription(), offerMediaDescription, dialogInfo.getDynamicMediaPayloadTypeMap());
					}
					getDialogCollection().replace(dialogInfo);

					Request dialogReinviteRequest = getDialogBeanHelper().createReinviteRequest(dialogInfo.getRemoteContact().getURI().toString(), dialogInfo, offerMediaDescription != null);
					ClientTransaction clientTransaction = null;
					try {
						clientTransaction = getSimpleSipStack().getSipProvider().getNewClientTransaction(dialogReinviteRequest);
					} catch (TransactionUnavailableException e) {
						throw new StackException(e.getMessage(), e);
					}
					getSimpleSipStack().sendRequest(clientTransaction);
				} else {
					LOG.info(String.format("Can't send reinvite for dialog %s, status %s, termination method %s", dialogId, dialogInfo.getDialogState(),
							dialogInfo.getTerminationMethod()));
				}
			}

			public String getResourceId() {
				return dialogId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	public void sendReinviteOkResponse(final String dialogId, final MediaDescription mediaDescription) {
		LOG.debug(String.format("Sending reinvite response to dialog: %s", dialogId));
		if(mediaDescription == null)
			throw new IllegalArgumentException(String.format("Could not send reinvite response for dialog %s: no SDP", dialogId));

		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);

				// TODO: a test for this
				if (dialogInfo.getReinviteInProgess().equals(ReinviteInProgress.None)) {
					LOG.warn(String.format("NOT sending reinivite OK response for dialog %s - no reinvite in progress", dialogId));
					return;
				}

				dialogInfo.setReinviteInProgess(ReinviteInProgress.None);
				ServerTransaction serverTransaction = dialogInfo.getInviteServerTransaction();
				dialogInfo.setInviteServerTransaction(null);
				SessionDescriptionHelper.setMediaDescription(dialogInfo.getSessionDescription(), mediaDescription, dialogInfo.getDynamicMediaPayloadTypeMap());
				dialogCollection.replace(dialogInfo);

				Response response;
				try {
					response = getSimpleSipStack().getMessageFactory().createResponse(Response.OK, serverTransaction.getRequest());
				} catch (ParseException e) {
					throw new StackException(e.getMessage(), e);
				}
				getSimpleSipStack().addContactHeader(response, dialogInfo.getSipUserName());
				getSimpleSipStack().setContent(response, SimpleSipStack.CONTENT_TYPE_APPLICATION, SimpleSipStack.CONTENT_SUBTYPE_SDP, dialogInfo.getSessionDescription().toString());
				getDialogBeanHelper().sendResponse(response, serverTransaction);
			}

			public String getResourceId() {
				return dialogId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	public void sendReinviteAck(final String dialogId, final MediaDescription mediaDescription) {
		ConcurrentUpdateBlock concurrentUpdateBlock = new DialogConcurrentUpdateBlock(getDialogBeanHelper()) {
			public void execute() {
				DialogInfo dialogInfo = getDialogCollection().get(dialogId);
				LOG.debug(String.format("Sending Reinvite ACK for dialog %s", dialogId));
				ReinviteInProgress reinviteInProgress = dialogInfo.getReinviteInProgess();
				if (reinviteInProgress.equals(ReinviteInProgress.SendingReinviteWithoutSessionDescription)) {
					dialogInfo.setReinviteInProgess(ReinviteInProgress.None);

					Request ackRequest = getDialogBeanHelper().createAckRequest(dialogInfo);
					addContentToAckRequest(ackRequest, dialogInfo, mediaDescription);
					forceSequenceNumber(dialogInfo.getSipCallId(), dialogInfo.getLastReceivedOkSequenceNumber(), Request.ACK);
					getDialogCollection().replace(dialogInfo);

					sendReinviteAck(ackRequest, dialogInfo);

					final DialogRefreshCompletedEvent dialogRefreshCompletedEvent = new DialogRefreshCompletedEvent(dialogId, dialogInfo.getApplicationData(), mediaDescription);
					getEventDispatcher().dispatchEvent(getDialogListeners(), dialogRefreshCompletedEvent);
				} else {
					LOG.warn(String.format("Asked to send ACK for media offer for dialog %s with reinvite state %s", dialogId, reinviteInProgress));
				}
			}

			public String getResourceId() {
				return dialogId;
			}
		};
		new ConcurrentUpdateManagerImpl().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	protected void addContentToAckRequest(Request ackRequest, DialogInfo dialogInfo, MediaDescription mediaDescription ) {
		try {
			SessionDescriptionHelper.setMediaDescription(dialogInfo.getSessionDescription(), mediaDescription, dialogInfo.getDynamicMediaPayloadTypeMap());

			ContentTypeHeader contentTypeHeader = getSimpleSipStack().getHeaderFactory().createContentTypeHeader("application", "sdp");
			ackRequest.setHeader(contentTypeHeader);
			ackRequest.setContent(dialogInfo.getSessionDescription(), contentTypeHeader);
		} catch (ParseException e) {
				throw new StackException("Error adding Content Type to ACK");
		}
		dialogInfo.setLastAckRequest(ackRequest);
	}
}
