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

package com.bt.aloha.stack;

import gov.nist.javax.sip.ListeningPointImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.CSeq;
import gov.nist.javax.sip.header.RouteList;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.message.SIPResponse;

import java.net.SocketException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import javax.sdp.SessionDescription;
import javax.sip.ClientTransaction;
import javax.sip.ListeningPoint;
import javax.sip.PeerUnavailableException;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TransactionAlreadyExistsException;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.URI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;

import com.bt.aloha.util.NetworkHelper;

public class SimpleSipStack {
    public static final String CONTENT_TYPE_APPLICATION = "application";

    public static final String CONTENT_SUBTYPE_SDP = "sdp";

    public static final String SIP_MESSAGES_LOGGER = "com.bt.aloha.messages";

    private static final String TRUE = "true";

    private static final String GOV_NIST_JAVAX_SIP_REENTRANT_LISTENER = "gov.nist.javax.sip.REENTRANT_LISTENER";

    private static final String GOV_NIST_JAVAX_SIP_LOG_MESSAGE_CONTENT = "gov.nist.javax.sip.LOG_MESSAGE_CONTENT";

    private static final String JAVAX_SIP_STACK_NAME = "javax.sip.STACK_NAME";

    private static final String JAVAX_SIP_AUTOMATIC_DIALOG_SUPPORT = "javax.sip.AUTOMATIC_DIALOG_SUPPORT";

    private static final String JAVAX_SIP_RETRANSMISSION_FILTER = "javax.sip.RETRANSMISSION_FILTER";

    private static final String JAVAX_SIP_ROUTER_PATH = "javax.sip.ROUTER_PATH";

    private static final String JAVAX_SIP_OUTBOUND_PROXY = "javax.sip.OUTBOUND_PROXY";

    private static final String GOV_NIST_JAVAX_SIP_TRACE_LEVEL = "gov.nist.javax.sip.TRACE_LEVEL";

    private static final String GOV_NIST_JAVAX_SIP_DEBUG_LOG = "gov.nist.javax.sip.DEBUG_LOG";

    private static final String GOV_NIST_JAVAX_SIP_SERVER_LOG = "gov.nist.javax.sip.SERVER_LOG";

    private static final String GOV_NIST_JAVAX_SIP_THREAD_POOL_SIZE = "gov.nist.javax.sip.THREAD_POOL_SIZE";

    private static final String GOV_NIST_JAVAX_SIP_CANCEL_CLIENT_TRANSACTION_CHECKED = "gov.nist.javax.sip.CANCEL_CLIENT_TRANSACTION_CHECKED";

    private static final String GOV_NIST_JAVAX_SIP_MAX_SERVER_TRANSACTIONS = "gov.nist.javax.sip.MAX_SERVER_TRANSACTIONS";

    private static final String HR = "--------------------------------------------------";

    private static final String ERROR_PROXYING_REQUEST_STRING = "Error proxying request";

    private static final int DEFAULT_MAX_FORWARDS_VALUE = 70;

    private static final String ERROR_CREATING_NEW_SERVER_TRANSACTION_STRING = "Error creating new server transaction for call id: %s";

    private static final int DEFAULT_SIP_PORT = 5060;

    private static final Log LOG = LogFactory.getLog(SimpleSipStack.class);

    private static final Log SIP_LOG = LogFactory.getLog(SIP_MESSAGES_LOGGER);

    private SipStack sipStack;

    private String ipAddress;

    private int port;

    private String transport;

    private SipProvider sipProvider;

    private SipListener sipListener;

    private MessageFactory messageFactory;

    private AddressFactory addressFactory;

    private HeaderFactory headerFactory;

    private String contactAddress;

    private Random random = new Random((new Date()).getTime());

    private String stackName;

    private String sipTraceLevel = "16";

    private String sipDebugLog = "debugLog.txt";

    private String sipServerLog = "serverLog.txt";

    private int sleepIntervalBeforeSending;

    private Properties jainStackProps = new Properties();

    private SipStackMessageQueueCollection sipStackMessageQueueCollection;

    public SimpleSipStack() {
        this.contactAddress = null;
        this.sleepIntervalBeforeSending = 0;
        this.sipStackMessageQueueCollection = null;
    }

    public String getStackName() {
        return stackName;
    }

    public void setJainStackProperties(Properties someProps) {
        this.jainStackProps = someProps;
    }

    public Properties getJainStackProperties() {
        return this.jainStackProps;
    }

    public void setSipListener(final SipListener aSipListener) {
        this.sipListener = aSipListener;
    }

    public void setSipDebugLog(String aSipDebugLog) {
        this.sipDebugLog = aSipDebugLog;
    }

    public void setSipServerLog(String aSipServerLog) {
        this.sipServerLog = aSipServerLog;
    }

    public void setSipTraceLevel(String aSipTraceLevel) {
        this.sipTraceLevel = aSipTraceLevel;
    }

    public String getContactAddress() {
        String address = null;
        if (this.contactAddress != null) {
            address = this.contactAddress;
        } else {
            address = getIpAddress() + (getPort() == DEFAULT_SIP_PORT ? "" : ":" + getPort());
        }
        return address;
    }

    public void setContactAddress(final String aContactAddress) {
        if (null == aContactAddress || aContactAddress.length() < 1) {
            this.contactAddress = null;
            return;
        }
        LOG.info(String.format("Setting contact address for stack %s to %s", stackName, aContactAddress));
        this.contactAddress = aContactAddress;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public int getPort() {
        return this.port;
    }

    public String getTransport() {
        return this.transport;
    }

    public SipProvider getSipProvider() {
        return this.sipProvider;
    }

    public AddressFactory getAddressFactory() {
        return this.addressFactory;
    }

    public HeaderFactory getHeaderFactory() {
        return this.headerFactory;
    }

    public MessageFactory getMessageFactory() {
        return this.messageFactory;
    }

    public void setNextHopRoutes(String routes) {
        if (routes == null || routes.trim().length() == 0)
            return;
        String[] hops = routes.split(";");
        for (String hop : hops) {
            String[] values = hop.split("=");
            if (values.length != 2)
                throw new IllegalArgumentException(String.format("Invalid routes set: %s", routes));
        }
        System.setProperty(NextHopRouter.NEXT_HOP + "_" + stackName, routes);
    }

    public void setSipStackMessageQueueCollection(SipStackMessageQueueCollection aSipMessageQueueCollection) {
        this.sipStackMessageQueueCollection = aSipMessageQueueCollection;
    }

    protected SipStack createSipStack() throws PeerUnavailableException {
        MDC.put("stackname", this.stackName);
        SipFactory sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");

        this.headerFactory = sipFactory.createHeaderFactory();
        this.addressFactory = sipFactory.createAddressFactory();
        this.messageFactory = sipFactory.createMessageFactory();

        LOG.debug(String.format("Sip logging tracelevel: %s, debug log: %s, server log: %s", sipTraceLevel,
                sipDebugLog, sipServerLog));

        if (!jainStackProps.keySet().contains(GOV_NIST_JAVAX_SIP_LOG_MESSAGE_CONTENT))
            jainStackProps.setProperty(GOV_NIST_JAVAX_SIP_LOG_MESSAGE_CONTENT, TRUE);
        if (!jainStackProps.keySet().contains(JAVAX_SIP_STACK_NAME))
            jainStackProps.setProperty(JAVAX_SIP_STACK_NAME, this.stackName);
        if (!jainStackProps.keySet().contains(JAVAX_SIP_RETRANSMISSION_FILTER))
            jainStackProps.setProperty(JAVAX_SIP_RETRANSMISSION_FILTER, "on");
        if (!jainStackProps.keySet().contains(JAVAX_SIP_AUTOMATIC_DIALOG_SUPPORT))
            jainStackProps.setProperty(JAVAX_SIP_AUTOMATIC_DIALOG_SUPPORT, "off");
        if (!jainStackProps.keySet().contains(GOV_NIST_JAVAX_SIP_REENTRANT_LISTENER))
            jainStackProps.setProperty(GOV_NIST_JAVAX_SIP_REENTRANT_LISTENER, TRUE);

        return sipFactory.createSipStack(jainStackProps);
    }

    public void init() {
        LOG.info(String.format("Creating stack listening point - address: %s port: %d transport: %s", this.ipAddress,
                Integer.valueOf(this.port), this.transport));
        try {
            this.sipStack = this.createSipStack();
        } catch (PeerUnavailableException e) {
            throw new StackException("Problem creating Sip Stack using SipFactory.", e);
        }

        initStackListener(this.sipStack, this.ipAddress, this.port, this.transport);

        showStartupMessage();
    }

    private void showStartupMessage() {
        LOG.info(HR);
        LOG.info("               Started SimpleSipStack             ");
        LOG.info(HR);
        LOG.info(stackName);
        LOG.info(" - IP Address: " + getIpAddress());
        LOG.info(" - Port:       " + getPort());
        LOG.info(" - Transport:  " + getTransport());

        if (this.jainStackProps.keySet().contains(JAVAX_SIP_OUTBOUND_PROXY))
            LOG.info(" - Outbound Proxy:  " + this.jainStackProps.getProperty(JAVAX_SIP_OUTBOUND_PROXY));
        if (this.jainStackProps.keySet().contains(JAVAX_SIP_ROUTER_PATH))
            LOG.info(" - Sip Router class:  " + this.jainStackProps.getProperty(JAVAX_SIP_ROUTER_PATH));
        if (this.sipStackMessageQueueCollection != null)
            LOG.info(" - Sip message queue retry interval:  "
                    + this.sipStackMessageQueueCollection.getQueuedSipMessageBlockingInterval());
        LOG.info(HR);
    }

    protected void initStackListener(SipStack stack, String ip, int aPort, String aTransport) {
        try {
            ListeningPoint lp = stack.createListeningPoint(ip, aPort, aTransport);
            this.sipProvider = stack.createSipProvider(lp);
            this.sipProvider.addSipListener(this.sipListener);
            stack.start();
        } catch (Throwable t) {
            throw new StackException("Failed to add listening point", t);
        }
    }

    public void destroy() {
        if (this.sipStack != null) {
            this.sipStack.stop();
            this.sipStack = null;
        }

        LOG.info(String.format("One listening point (port %d, transport %s) removed for stack on %s", Integer
                .valueOf(this.port), this.transport, this.ipAddress));
    }

    /**
     * @param ipPattern
     * @return The host address matching the wildcard pattern passed in. If no
     *         network address matches the pattern then a StackException is
     *         thrown.
     * @throws StackException
     */
    public void setIpAddress(final String ipPattern) {
        String hostAddress;
        try {
            hostAddress = NetworkHelper.lookupIpAddress(ipPattern);
        } catch (SocketException e) {
            throw new StackException("Problem looking up network interfaces", e);
        }

        if (hostAddress == null) {
            throw new StackException(String.format("No network adapter matches IP address pattern %s", ipPattern));
        }

        ipAddress = hostAddress;
    }

    public String generateNewTag() {
        int r = this.random.nextInt(Integer.MAX_VALUE);
        return Integer.toString(r);
    }

    protected SipStack getSipStack() {
        return sipStack;
    }

    protected void setSipStack(SipStack aSipStack) {
        this.sipStack = aSipStack;
    }

    public int getSleepIntervalBeforeSending() {
        return sleepIntervalBeforeSending;
    }

    public void setSleepIntervalBeforeSending(int aSleepIntervalBeforeSending) {
        if (aSleepIntervalBeforeSending > 0) {
            LOG.warn(HR);
            LOG.warn(String.format("Setting sleep interval before sends to %d", aSleepIntervalBeforeSending));
            LOG.warn(HR);
        }
        this.sleepIntervalBeforeSending = aSleepIntervalBeforeSending;
    }

    public ServerTransaction createNewServerTransaction(Request request) {
        try {
            return getSipProvider().getNewServerTransaction(request);
        } catch (TransactionAlreadyExistsException e) {
            throw new StackException(String.format(ERROR_CREATING_NEW_SERVER_TRANSACTION_STRING,
                    ((CallIdHeader) request.getHeader(CallIdHeader.NAME)).getCallId()), e);
        } catch (TransactionUnavailableException e) {
            throw new StackException(String.format(ERROR_CREATING_NEW_SERVER_TRANSACTION_STRING,
                    ((CallIdHeader) request.getHeader(CallIdHeader.NAME)).getCallId()), e);
        }
    }

    public void sendResponse(Request request, ServerTransaction serverTransaction, int responseCode) {
        LOG.info("Sending response " + responseCode + " with server transaction");
        ServerTransaction newServerTransaction = serverTransaction;
        if (newServerTransaction == null) {
            LOG.warn("No server transaction, creating one");
            newServerTransaction = createNewServerTransaction(request);
        }
        Response response;
        try {
            response = getMessageFactory().createResponse(responseCode, request);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Error creating response", e);
        }
        sendResponse(response, newServerTransaction);
    }

    public void sendResponse(Response response, ServerTransaction serverTransaction) {
        sleepBeforeSending();
        SIP_LOG.debug(String.format("Sending response:\n%s", response.toString()));
        try {
            serverTransaction.sendResponse(response);
        } catch (Throwable t) {
            throw new StackException("Unable to send response", t);
        }
    }

    public Request createAckRequest(ResponseEvent responseEvent, String requestUri, RouteList routeList) {
        try {
            SipUri requestURI = (SipUri) getAddressFactory().createAddress(requestUri).getURI();
            CSeq cseq = new CSeq();
            cseq.setMethod(Request.ACK);
            cseq.setSeqNumber(((CSeqHeader) responseEvent.getResponse().getHeader(CSeqHeader.NAME)).getSeqNumber());

            // Via header
            ListeningPointImpl lp = (ListeningPointImpl) getSipProvider().getListeningPoint(getTransport());
            Via via = lp.getViaHeader();

            Request ackRequest = ((SIPResponse) responseEvent.getResponse()).createRequest(requestURI, via, cseq);
            if (routeList != null && routeList.size() > 0) {
                LOG.debug(String.format("Setting route list for ACK request to %s", routeList.toString()));
                ackRequest.addHeader(routeList);
            }
            return ackRequest;
        } catch (Throwable t) {
            throw new StackException("Failed to create ACK request", t);
        }
    }

    public Request createRequest(String requestUri, String method, String callId, long sequenceNumber,
            Address localParty, String localTag, Address remoteParty, String remoteTag, String viaBranchId,
            RouteList routeList, SessionDescription sessionDescription) {
        LOG
                .debug(String
                        .format(
                                "Creating request with requestUri %s, method %s, callId %s, seqnum %d, localParty %s, localTag %s, remoteParty %s, remoteTag %s, sdp\n%s\n",
                                requestUri, method, callId, sequenceNumber, localParty.getURI().toString(), localTag,
                                remoteParty.getURI().toString(), remoteTag, sessionDescription));

        try {
            SipUri requestURI = (SipUri) getAddressFactory().createAddress(requestUri).getURI();

            // Call-ID:
            CallIdHeader callIdHeader;
            callIdHeader = getHeaderFactory().createCallIdHeader(callId);

            CSeqHeader cseqHeader = getHeaderFactory().createCSeqHeader(sequenceNumber, method);

            // From Header:
            FromHeader fromHeader = getHeaderFactory().createFromHeader(localParty, localTag);

            // To header:
            ToHeader toHeader = getHeaderFactory().createToHeader(remoteParty, remoteTag);

            // Via header
            ListeningPointImpl lp = (ListeningPointImpl) getSipProvider().getListeningPoint(getTransport());
            ViaHeader viaHeader = lp.getViaHeader();
            if (viaBranchId != null)
                viaHeader.setBranch(viaBranchId);
            List<ViaHeader> viaList = new Vector<ViaHeader>();
            viaList.add(viaHeader);

            // MaxForwards header:
            MaxForwardsHeader maxForwardsHeader = getHeaderFactory()
                    .createMaxForwardsHeader(DEFAULT_MAX_FORWARDS_VALUE);

            Request request = null;
            if (sessionDescription != null) {
                // Content-Type:
                ContentTypeHeader contentTypeHeader = getHeaderFactory().createContentTypeHeader(
                        CONTENT_TYPE_APPLICATION, CONTENT_SUBTYPE_SDP);

                request = getMessageFactory().createRequest(requestURI, method, callIdHeader, cseqHeader, fromHeader,
                        toHeader, viaList, maxForwardsHeader, contentTypeHeader, sessionDescription.toString());
            } else {
                request = getMessageFactory().createRequest(requestURI, method, callIdHeader, cseqHeader, fromHeader,
                        toHeader, viaList, maxForwardsHeader);
            }

            // Contact header
            if (Request.INVITE.equals(request.getMethod()))
                addContactHeader(request);

            // Route set
            if (routeList != null && routeList.size() > 0) {
                LOG.debug(String.format("Setting route list for %s request to %s", method, routeList.toString()));
                request.addHeader(routeList);
            }

            return request;
        } catch (Throwable t) {
            throw new StackException("Failed to create request", t);
        }
    }

    protected void addContactHeader(Request request) {
        addContactHeader(request, null);
    }

    protected void addContactHeader(Request request, String userId) {
        Address contact = getContactAddress(userId);
        request.addHeader(getHeaderFactory().createContactHeader(contact));
    }

    public void addContactHeader(Response response) {
        addContactHeader(response, null);
    }

    public void addContactHeader(Response response, String userId) {
        Address contact = getContactAddress(userId);
        response.addHeader(getHeaderFactory().createContactHeader(contact));
    }

    private Address getContactAddress(String userId) {
        URI calleeContact;
        try {
            calleeContact = getAddressFactory().createURI(getContactSipUri(userId));
        } catch (ParseException e) {
            throw new IllegalArgumentException("Error adding contact header", e);
        }
        return getAddressFactory().createAddress(calleeContact);
    }

    public String getContactSipUri(String userName) {
        return "sip:" + ((userName == null || userName.length() == 0) ? "" : userName + "@") + getContactAddress();
    }

    public void setContent(Response response, String type, String subType, String content) {
        try {
            ContentTypeHeader ct = getHeaderFactory().createContentTypeHeader(type, subType);
            response.setContent(content, ct);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Error adding content", e);
        }
    }

    public ClientTransaction sendRequest(Request request) {
        ClientTransaction clientTransaction;
        try {
            clientTransaction = getSipProvider().getNewClientTransaction(request);
        } catch (TransactionUnavailableException e) {
            throw new StackException("Unable to create a new client transaction", e);
        }
        return sendRequest(clientTransaction);
    }

    public ClientTransaction sendRequest(ClientTransaction clientTransaction) {
        return sendRequest(clientTransaction.getRequest(), clientTransaction);
    }

    public void resendRequest(Request request) {
        sendRequest(request, null);
    }

    private ClientTransaction sendRequest(Request request, ClientTransaction clientTransaction) {
        String sipCallId = ((CallIdHeader) request.getHeader(CallIdHeader.NAME)).getCallId();
        long sequenceNumber = ((CSeqHeader) request.getHeader(CSeqHeader.NAME)).getSeqNumber();
        String requestMethod = request.getMethod();
        try {
            sipStackMessageQueueCollection.blockUntilCanSendRequest(sipCallId, sequenceNumber, requestMethod);
            sleepBeforeSending();
            if (clientTransaction != null) {
                SIP_LOG.debug(String.format("Sending request\n%s", request.toString()));
                clientTransaction.sendRequest();
            } else {
                SIP_LOG.debug(String.format("Sending transactionless request\n%s", request.toString()));
                getSipProvider().sendRequest(request);
            }
            dequeueRequest(sipCallId, sequenceNumber, requestMethod);
            return clientTransaction;
        } catch (Exception t) {
            throw new StackException(String.format("Unable to send request (%s:%s:%d)", sipCallId, requestMethod,
                    sequenceNumber), t);
        }
    }

    private void sleepBeforeSending() {
        if (sleepIntervalBeforeSending > 0) {
            LOG.debug(String.format("sleeping for %d ms", sleepIntervalBeforeSending));
            try {
                Thread.sleep(sleepIntervalBeforeSending);
            } catch (InterruptedException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    protected void proxyRequest(Request request) {
        proxyRequest(request, null);
    }

    protected void proxyRequest(Request request, String destinationUri) {
        try {
            if (destinationUri != null)
                request.setRequestURI(getAddressFactory().createURI(destinationUri));
            LOG.info(String.format("Proxying request\n%s", request.toString()));
            getSipProvider().sendRequest(request);
        } catch (SipException e) {
            throw new StackException(ERROR_PROXYING_REQUEST_STRING, e);
        } catch (ParseException e) {
            throw new StackException(ERROR_PROXYING_REQUEST_STRING, e);
        }
    }

    protected void proxyResponse(Response response) {
        try {
            ViaHeader viaHeader = (ViaHeader) response.getHeader(ViaHeader.NAME);
            if (viaHeader != null)
                viaHeader.removeParameter(Via.RPORT);
            LOG.info(String.format("Proxying response\n%s", response.toString()));
            getSipProvider().sendResponse(response);
        } catch (SipException e) {
            throw new StackException(ERROR_PROXYING_REQUEST_STRING, e);
        }
    }

    public long enqueueRequestAssignSequenceNumber(String sipCallId, long sequenceNumber, String method) {
        return sipStackMessageQueueCollection.enqueueRequest(sipCallId, sequenceNumber, method);
    }

    public void enqueueRequestForceSequenceNumber(String sipCallId, long sequenceNumber, String method) {
        sipStackMessageQueueCollection.enqueueRequestForceSequenceNumber(sipCallId, sequenceNumber, method);
    }

    public void dequeueRequest(String sipCallId, long sequenceNumber, String requestMethod) {
        sipStackMessageQueueCollection.dequeueRequest(sipCallId, sequenceNumber, requestMethod);
    }

    public void setPort(int aPort) {
        this.port = aPort;
    }

    public void setTransport(String aTransport) {
        this.transport = aTransport;
    }

    public void setStackName(String aStackName) {
        this.stackName = aStackName;
    }

    public void setJainStackPropertiesTraceLevel(String traceLevel) {
        this.jainStackProps.put(GOV_NIST_JAVAX_SIP_TRACE_LEVEL, traceLevel);
    }

    public void setJainStackPropertiesDebugLog(String debugLog) {
        this.jainStackProps.put(GOV_NIST_JAVAX_SIP_DEBUG_LOG, debugLog);
    }

    public void setJainStackPropertiesServerLog(String serverLog) {
        this.jainStackProps.put(GOV_NIST_JAVAX_SIP_SERVER_LOG, serverLog);
    }

    public void setJainStackPropertiesThreadPoolSize(String threadPoolSize) {
        this.jainStackProps.put(GOV_NIST_JAVAX_SIP_THREAD_POOL_SIZE, threadPoolSize);
    }

    public void setJainStackPropertiesCancelClientTransactionChecked(String cancelClientTransactionChecked) {
        this.jainStackProps.put(GOV_NIST_JAVAX_SIP_CANCEL_CLIENT_TRANSACTION_CHECKED, cancelClientTransactionChecked);
    }

    public void setJainStackPropertiesMaxServerTransactions(int maxServerTransactions) {
        this.jainStackProps.put(GOV_NIST_JAVAX_SIP_MAX_SERVER_TRANSACTIONS, maxServerTransactions);
    }

    public void setJainStackPropertiesRouterPath(String routerPath) {
        this.jainStackProps.put(JAVAX_SIP_ROUTER_PATH, routerPath);
    }
}
