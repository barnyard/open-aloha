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

import java.util.Locale;

import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipListener;
import javax.sip.Timeout;
import javax.sip.TimeoutEvent;
import javax.sip.Transaction;
import javax.sip.TransactionAlreadyExistsException;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ReasonHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.ImmutableDialogInfo;
import com.bt.aloha.stack.SimpleSipStack;
import com.bt.aloha.stack.StackException;

public class DialogJainSipListener implements SipListener, ApplicationContextAware {
	private static final Log LOG = LogFactory.getLog(DialogJainSipListener.class);
	private static final Log SIP_LOG = LogFactory.getLog(SimpleSipStack.SIP_MESSAGES_LOGGER);

	private SimpleSipStack simpleSipStack;
	private DialogCollection dialogCollection;
	private DialogRouter dialogRouter;

	private ApplicationContext applicationContext;

	public DialogJainSipListener(SimpleSipStack aSimpleSipStack, DialogCollection aDialogCollection) {
		this.simpleSipStack = aSimpleSipStack;
		this.dialogCollection = aDialogCollection;
		this.dialogRouter = new DialogRouter();
		this.applicationContext = null;
	}

	public void setDialogRouter(DialogRouter aRouter) {
		this.dialogRouter = aRouter;
	}

	public void processRequest(final RequestEvent requestEvent) {
		SIP_LOG.debug(String.format("Received request\n%s", requestEvent.getRequest()));
		final Request request = requestEvent.getRequest();
		final ServerTransaction serverTransaction;
		try {
			if(requestEvent.getServerTransaction() != null)
				serverTransaction = requestEvent.getServerTransaction();
			else
				try {
					serverTransaction = simpleSipStack.createNewServerTransaction(request);
				} catch(StackException e) {
					if(e.getCause() instanceof TransactionAlreadyExistsException) {
						LOG.warn("Transaction already exists for received INVITE request - throwing request away");
						return;
					} else {
						throw e;
					}
				}
		} catch (Exception e) {
			LOG.error(e.getMessage(),e);
			sendErrorResponse(request, null, Response.SERVER_INTERNAL_ERROR);
			return;
		}

		try {
			URI requestURI = request.getRequestURI();

			if(!(requestURI instanceof SipURI)) {
				sendErrorResponse(request, serverTransaction,  Response.UNSUPPORTED_URI_SCHEME, "NonSipRequestUri");
				return;
			}

			String username = ((SipURI)requestURI).getUser();
			final String callId  = ((CallIdHeader)request.getHeader(CallIdHeader.NAME)).getCallId();
			
			if (Request.OPTIONS.equals(request.getMethod())) {
				LOG.info(String.format("Sending OK in reponse to OPTIONS for <%s>", callId));
				Response response = simpleSipStack.getMessageFactory().createResponse(Response.OK, request);
				response.addHeader(simpleSipStack.getHeaderFactory().createAcceptHeader("application", "sdp"));
				response.addHeader(simpleSipStack.getHeaderFactory().createAcceptEncodingHeader("gzip"));
				response.addHeader(simpleSipStack.getHeaderFactory().createAcceptLanguageHeader(Locale.ENGLISH));
				response.addHeader(simpleSipStack.getHeaderFactory().createSupportedHeader(""));
				simpleSipStack.sendResponse(response, serverTransaction);
				return;
			}

			DialogInfo dialogInfo = dialogCollection.get(callId);
			if (dialogInfo == null) {
				if(!Request.INVITE.equals(request.getMethod())) {
					LOG.info(String.format("Sending Call or Transaction Does Not Exist response for <%s>", callId));
					sendErrorResponse(request, serverTransaction, Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
					return;
				}

				IncomingDialogRouterRule routerRule = dialogRouter.findRule(username);
				if (routerRule == null) {
					LOG.info(String.format("No inbound router rules found for call id <%s>", callId));
					sendErrorResponse(request, serverTransaction, Response.NOT_FOUND);
					return;
				}

				dialogInfo = createDialogInfo(request, serverTransaction, routerRule);
				dialogCollection.add(dialogInfo);
			}

			if(DialogState.Terminated.equals(dialogInfo.getDialogState())){
				LOG.info(String.format("Dialog %s is Terminated, sending error response", callId));
				sendErrorResponse(request, serverTransaction, Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST, "AlreadyTerminated");
				return;
			} else {
				LOG.debug("Got existing dialog " + callId);
			}

			final DialogSipBean dialogBean = (DialogSipBean)applicationContext.getBean(dialogInfo.getSimpleSipBeanId());
			LOG.debug("Fetching a dialog bean with beanId " + dialogInfo.getSimpleSipBeanId() + " to process the request ");
			dialogBean.processRequest(request, serverTransaction, dialogInfo);
		} catch (UnsupportedRequestException e) {
			LOG.info("Unsupported request: " + requestEvent.getRequest().getMethod());
			sendErrorResponse(request, serverTransaction, Response.BAD_REQUEST);
		} catch (Exception e) {
			LOG.error(e.getMessage(),e);
			sendErrorResponse(request, serverTransaction, Response.SERVER_INTERNAL_ERROR);
		}
	}

	private DialogInfo createDialogInfo(Request request, ServerTransaction serverTransaction, IncomingDialogRouterRule routerRule) {
		String localTag = simpleSipStack.generateNewTag();
		return new DialogInfo(routerRule.getDialogSipBean().getBeanName(), simpleSipStack.getIpAddress(), request, serverTransaction, localTag, routerRule.getDialogProperties());
	}

	protected void sendErrorResponse(Request request, ServerTransaction serverTransaction, int errorCode) {
		sendErrorResponse(request, serverTransaction, errorCode, null);
	}

	protected void sendErrorResponse(Request request, ServerTransaction serverTransaction, int errorCode, String reason) {
		try {
			LOG.debug(String.format("Sending error response for request %s. Error Code = %d, reason = %s", request.getMethod(), errorCode, reason));
			Response response = simpleSipStack.getMessageFactory().createResponse(errorCode, request);
			if(reason != null) {
				ReasonHeader reasonHeader = simpleSipStack.getHeaderFactory().createReasonHeader("Error", errorCode, reason);
				response.addHeader(reasonHeader);
			}

			LOG.info(String.format("Sending response\n%s", response.toString()));
			if(serverTransaction == null) {
				LOG.warn("Sending transaction-less response");
				simpleSipStack.getSipProvider().sendResponse(response);
			}
			else
				serverTransaction.sendResponse(response);
		} catch (Exception e) {
			LOG.error("Failed to send error response " + errorCode, e);
		}
	}

	public void processResponse(final ResponseEvent responseEvent) {
		try {
			Response response = responseEvent.getResponse();
			SIP_LOG.debug(String.format("Received response\n%s", response.toString()));
			LOG.info("Response " + response.getStatusCode() + " " + response.getReasonPhrase() + " received");

			final String callId  = ((CallIdHeader)response.getHeader(CallIdHeader.NAME)).getCallId();

			ImmutableDialogInfo dialogInfo = dialogCollection.get(callId);
			if(dialogInfo == null) {
				LOG.info(String.format("Dialog / transaction not found for response with call id <%s>", callId));
				return;
			}

			final DialogSipBean dialogBeanBase = (DialogSipBean) applicationContext.getBean(dialogInfo.getSimpleSipBeanId());
			dialogBeanBase.processResponse(responseEvent, dialogInfo);
		} catch (Exception e) {
			LOG.error(e.getMessage(),e);
		}
	}

	public void processTimeout(final TimeoutEvent timeOutEvent) {
		if(timeOutEvent.getTimeout().equals(Timeout.RETRANSMIT)) {
			LOG.debug("Retransmit Timeout Event received, ignoring");
			return;
		}

		LOG.warn("Transaction TimeoutEvent received: " + timeOutEvent.toString());
		Request request = null;
		if(timeOutEvent.isServerTransaction() && timeOutEvent.getServerTransaction() != null)
			request = timeOutEvent.getServerTransaction().getRequest();
		else if(!timeOutEvent.isServerTransaction() && timeOutEvent.getClientTransaction() != null)
			request = timeOutEvent.getClientTransaction().getRequest();

		if(request == null) {
			LOG.warn("Got timeout event, but no server or client transation");
			return;
		}

		final String callId = ((CallIdHeader)request.getHeader(CallIdHeader.NAME)).getCallId();

		ImmutableDialogInfo dialogInfo = dialogCollection.get(callId);
		if(dialogInfo != null) {
			DialogSipBean dialogBeanBase = (DialogSipBean) applicationContext.getBean(dialogInfo.getSimpleSipBeanId());
			dialogBeanBase.processTimeout(timeOutEvent, dialogInfo.getId());
		} else {
			LOG.info(String.format("Dialog for timeout event for call %s not found", callId));
		}
	}

	public void processIOException(IOExceptionEvent ioExceptionEvent) {
		LOG.warn("IOExceptionEvent received: " + ioExceptionEvent.toString());
	}

	public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
		// For the most part we don't need to do anything here - the exception is an INVITE
		// request where an OK is eventually sent, but no ACK ever arrives - currently it seems
		// that this is the only place we get 'told' about this by the stack, as a Tx Terminated
		// event is raised about 10s after the OK is sent. There is no timeout event. Need to
		// figure out if this is expected behaviour, and if so we need to identify any initial
		// invite transactions here and set the status of the relevant dialog to terminated
		// if that initial transaction fails.

		final Transaction transaction;
		if(transactionTerminatedEvent.isServerTransaction() && transactionTerminatedEvent.getServerTransaction() != null)
			transaction = transactionTerminatedEvent.getServerTransaction();
		else if(!transactionTerminatedEvent.isServerTransaction() && transactionTerminatedEvent.getClientTransaction() != null)
			transaction = transactionTerminatedEvent.getClientTransaction();
		else {
			LOG.warn("Got transaction terminated event, but no server or client transation");
			return;
		}
		Request request = transaction.getRequest();

		final String sipCallId = ((CallIdHeader)request.getHeader(CallIdHeader.NAME)).getCallId();
		LOG.debug(String.format("TransactionTerminatedEvent received for dialog %s(%s)", sipCallId, request.getMethod()));
	}

	public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
		LOG.warn("DalogTerminated event received unexpectedly, is dialog handling at stack level turned ON?");
	}

	public void setApplicationContext(ApplicationContext aApplicationContext) {
		this.applicationContext = aApplicationContext;
	}

	public void setDialogCollection(DialogCollection aDialogCollection) {
		this.dialogCollection = aDialogCollection;
	}
}
