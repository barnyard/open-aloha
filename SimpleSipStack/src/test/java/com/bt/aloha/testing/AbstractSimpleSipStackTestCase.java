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

 	

 	
 	
 
package com.bt.aloha.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipRequest;
import org.cafesip.sipunit.SipResponse;
import org.cafesip.sipunit.SipStack;
import org.cafesip.sipunit.SipTransaction;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.aloha.call.event.AbstractCallEvent;
import com.bt.aloha.dialog.event.AbstractDialogEvent;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.stack.SimpleSipStack;
import com.bt.aloha.stack.StackException;
import com.bt.aloha.testing.SipUnitPhone;
import com.bt.aloha.util.MessageDigestHelper;

public abstract class AbstractSimpleSipStackTestCase {
	protected static final int SLEEP_INTERVAL_BEFORE_SENDING_MESSAGE = 30;
	protected static final String TEST_MDC_ID = "testId";
	protected static final String AT = "@";
	protected static final String COLON = ":";
	protected static final String SIP_PREFIX = "sip:";
	protected static final String TRUE = "true";
	protected static final int PORT_6061 = 6061;
	protected static final int PORT_6060 = 6060;
	protected static final int MEDIA_PORT = 60735;

	protected static ClassPathXmlApplicationContext applicationContext;
	protected static String host;
	protected static int port = PORT_6061;
	protected static String protocol = SipStack.PROTOCOL_UDP;
	protected static volatile int classTestNumber = 1;

	protected static final String DISPLAY_NAME = "display name";
	private static final String NO_REINVITE = "No reinvite";
	protected static final String SDP = "sdp";
	protected static final String APPLICATION = "application";
	protected static final String OK = "OK";
	protected static final String RINGING = "Ringing";
	protected static final String TRYING = "Trying";
	private static final String UNKNOWN_SIPUNIT_PHONE = "Unknown sipunit phone";
	private static final String DIDN_T_EVER_GET_OK = "Didn't ever get OK";
	protected static final String IN_IP4_0_0_0_0 = "IN IP4 0.0.0.0";
	private static final String SIMPLE_SIP_STACK_TEST_CASE = "<SimpleSipStack test case>";
    private static final String ONE_TWO_SEVEN_ZERO_ZERO_ONE = "127.0.0.1";
	private static final int ONE_THOUSAND = 1000;
	protected static final int FIVE_THOUSAND = 5000;
	private static final int OUTBOUND_PHONE_SDP_PORT = 10000;
	private static final int THIRD_INBOUND_PHONE_SDP_PORT = 10003;
	private static final int SECOND_INBOUND_PHONE_SDP_PORT = 10002;
	private static final int INBOUND_PHONE_SDP_PORT = 10001;

    private Log log = LogFactory.getLog(this.getClass());
    private SipStack sipStack;
    private SipPhone outboundPhone;
    private SipCall outboundCall;
	private SipPhone secondInboundPhone;
	private SipCall secondInboundCall;
	private SipPhone thirdInboundPhone;
	private SipCall thirdInboundCall;
    private SipPhone inboundPhone;
    private SipCall inboundCall;
    private String outboundPhoneName;
	private String secondInboundPhoneName;
	private String thirdInboundPhoneName;
    private String inboundPhoneName;
    private SessionDescription inboundPhoneSdp;
    private SessionDescription secondInboundPhoneSdp;
    private SessionDescription thirdInboundPhoneSdp;
    private SessionDescription outboundPhoneSdp;
    private MediaDescription inboundPhoneMediaDescription;
    private MediaDescription secondInboundPhoneMediaDescription;
    private MediaDescription thirdInboundPhoneMediaDescription;
    private MediaDescription outboundPhoneMediaDescription;
    private MediaDescription holdMediaDescription;
    private MediaDescription inactiveHoldMediaDescription;

    static {
    	try {
    		host = InetAddress.getLocalHost().getHostAddress();
    		applicationContext = null;
    	} catch(UnknownHostException e) {
    		throw new RuntimeException("Unknown host", e);
    	}
    }
    private String remoteUser = "remoteUser";
    private String remoteHost = ONE_TWO_SEVEN_ZERO_ZERO_ONE;
    private int remotePort = PORT_6060;

    private Properties properties = new Properties();

    // to keep findbugs happy
    protected AbstractSimpleSipStackTestCase() {
        sipStack = null;
        outboundPhone = null;
        outboundCall = null;
    	secondInboundPhone = null;
    	secondInboundCall = null;
    	thirdInboundPhone = null;
    	thirdInboundCall = null;
        inboundPhone = null;
        inboundCall = null;

        generateNewPhoneNames();

        inboundPhoneSdp = SessionDescriptionHelper.createSessionDescription(host, SIMPLE_SIP_STACK_TEST_CASE);
        inboundPhoneMediaDescription = createOfferMediaDescription(host, INBOUND_PHONE_SDP_PORT, new String[] {"1"});
        secondInboundPhoneSdp = SessionDescriptionHelper.createSessionDescription(host, SIMPLE_SIP_STACK_TEST_CASE);
        secondInboundPhoneMediaDescription = createOfferMediaDescription(host, SECOND_INBOUND_PHONE_SDP_PORT, new String[] {"2"});
        thirdInboundPhoneSdp = SessionDescriptionHelper.createSessionDescription(host, SIMPLE_SIP_STACK_TEST_CASE);
        thirdInboundPhoneMediaDescription = createOfferMediaDescription(host, THIRD_INBOUND_PHONE_SDP_PORT, new String[] {"3"});
        outboundPhoneSdp = SessionDescriptionHelper.createSessionDescription(host, SIMPLE_SIP_STACK_TEST_CASE);
        outboundPhoneMediaDescription = createOfferMediaDescription(host, OUTBOUND_PHONE_SDP_PORT, new String[] {"0"});
        holdMediaDescription = SessionDescriptionHelper.generateHoldMediaDescription();
        inactiveHoldMediaDescription = SessionDescriptionHelper.generateHoldMediaDescription();
        try {
			inactiveHoldMediaDescription.getMedia().setMediaPort(0);
		} catch (SdpException e) {
			throw new RuntimeException(e.getMessage(),e);
		}
    }

    @BeforeClass
    public static void beforeClass() {
        AbstractSimpleSipStackTestCase.classTestNumber = 1;
    }

    @Before
	public void setUp() throws Exception {
    	MDC.put(TEST_MDC_ID, AbstractSimpleSipStackTestCase.classTestNumber);
    	log.info(String.format("Starting test %s:%s", this.getClass().getSimpleName(), AbstractSimpleSipStackTestCase.classTestNumber));
    	AbstractSimpleSipStackTestCase.classTestNumber++;

	    properties.setProperty("javax.sip.STACK_NAME", "testAgent");
        properties.setProperty("javax.sip.RETRANSMISSION_FILTER", TRUE);
//        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32"); 
//        properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "testAgent_debug.txt");
//        properties.setProperty("gov.nist.javax.sip.SERVER_LOG", "testAgent_log.txt");

	    properties.setProperty("sipunit.trace", TRUE);

        try {
            sipStack = new SipStack(protocol, port, properties);
//            SipStack.setTraceEnabled(properties.getProperty("sipunit.trace").equalsIgnoreCase("true"));
            generateNewPhoneNames();
            outboundPhone = sipStack.createSipPhone(getOutboundPhoneSipAddress());
            outboundCall = outboundPhone.createSipCall();

            inboundPhone = sipStack.createSipPhone(getInboundPhoneSipAddress());
            inboundCall = inboundPhone.createSipCall();
            inboundCall.listenForIncomingCall();

            secondInboundPhone = this.getSipStack().createSipPhone(getSecondInboundPhoneSipAddress());
            secondInboundCall = secondInboundPhone.createSipCall();
            secondInboundCall.listenForIncomingCall();

            thirdInboundPhone = this.getSipStack().createSipPhone(getThirdInboundPhoneSipAddress());
            thirdInboundCall = thirdInboundPhone.createSipCall();
            thirdInboundCall.listenForIncomingCall();
        } catch (Exception e) {
        	throw new StackException("Unable to initialize SipUnit stack", e);
        }
	}

    @After
	public void tearDown() throws Exception {
		try {
			tearDownSipUnitPhones();
		} finally {
			log.info(String.format("Ending test %s:%s", this.getClass().getSimpleName(), MDC.get(TEST_MDC_ID)));
			MDC.remove(TEST_MDC_ID);
		}
	}

	protected void tearDownSipUnitPhones() {
		outboundCall.dispose();
		inboundCall.dispose();
		secondInboundCall.dispose();
		thirdInboundCall.dispose();
		outboundPhone.dispose();
		inboundPhone.dispose();
		secondInboundPhone.dispose();
		thirdInboundPhone.dispose();
		sipStack.dispose();
	}

	protected SipCall getSipUnitPhoneCall(SipUnitPhone sipUnitPhone) {
		SipCall result = null;
		if (sipUnitPhone == SipUnitPhone.Inbound)
			result = getInboundCall();
		else if (sipUnitPhone == SipUnitPhone.SecondInbound)
			result = getSecondInboundCall();
		else if (sipUnitPhone == SipUnitPhone.ThirdInbound)
			result = getThirdInboundCall();
		else if (sipUnitPhone == SipUnitPhone.Outbound)
			result = getOutboundCall();
		else
			throw new RuntimeException(UNKNOWN_SIPUNIT_PHONE);
		return result;
	}

	protected SessionDescription getSipUnitPhoneSdp(SipUnitPhone sipUnitPhone) {
		SessionDescription result = null;
		if (sipUnitPhone == SipUnitPhone.Inbound)
			result = getInboundPhoneSdp();
		else if (sipUnitPhone == SipUnitPhone.SecondInbound)
			result = getSecondInboundPhoneSdp();
		else if (sipUnitPhone == SipUnitPhone.ThirdInbound)
			result = getThirdInboundPhoneSdp();
		else if (sipUnitPhone == SipUnitPhone.Outbound)
			result = getOutboundPhoneSdp();
		else
			throw new RuntimeException(UNKNOWN_SIPUNIT_PHONE);
		return result;
	}

	protected MediaDescription getSipUnitPhoneMediaDescription(SipUnitPhone sipUnitPhone) {
		MediaDescription result = null;
		if (sipUnitPhone == SipUnitPhone.Inbound)
			result = getInboundPhoneMediaDescription();
		else if (sipUnitPhone == SipUnitPhone.SecondInbound)
			result = getSecondInboundPhoneMediaDescription();
		else if (sipUnitPhone == SipUnitPhone.ThirdInbound)
			result = getThirdInboundPhoneMediaDescription();
		else if (sipUnitPhone == SipUnitPhone.Outbound)
			result = getOutboundPhoneMediaDescription();
		else
			throw new RuntimeException(UNKNOWN_SIPUNIT_PHONE);
		return result;
	}

	protected String getSipUnitPhoneAddress(SipUnitPhone sipUnitPhone) {
		String result = null;
		if (sipUnitPhone == SipUnitPhone.Inbound)
			result = getInboundPhoneSipAddress();
		else if (sipUnitPhone == SipUnitPhone.SecondInbound)
			result = getSecondInboundPhoneSipAddress();
		else if (sipUnitPhone == SipUnitPhone.ThirdInbound)
			result = getThirdInboundPhoneSipAddress();
		else if (sipUnitPhone == SipUnitPhone.Outbound)
			result = getOutboundPhoneSipAddress();
		else
			throw new RuntimeException(UNKNOWN_SIPUNIT_PHONE);
		return result;
	}

	public String getOutboundPhoneSipAddress() {
		return SIP_PREFIX + outboundPhoneName + AT + host + COLON + port;
	}

	public String getInboundPhoneSipAddress() {
		return SIP_PREFIX + inboundPhoneName + AT + host + COLON + port;
	}

    public URI getInboundPhoneSipUri() {
        return URI.create(getInboundPhoneSipAddress());
    }

	public String getSecondInboundPhoneSipAddress() {
		return SIP_PREFIX + secondInboundPhoneName + AT + host + COLON + port;
	}

    public URI getSecondInboundPhoneSipUri() {
        return URI.create(getSecondInboundPhoneSipAddress());
    }

	public String getThirdInboundPhoneSipAddress() {
		return SIP_PREFIX + thirdInboundPhoneName + AT + host + COLON + port;
	}

    public URI getThirdInboundPhoneSipUri() {
        return URI.create(getThirdInboundPhoneSipAddress());
    }

    protected String getRemoteSipAddress() {
		return SIP_PREFIX + remoteUser + System.currentTimeMillis() + AT + remoteHost + COLON + remotePort;
	}

	protected String getRemoteSipProxy() {
		return remoteHost + COLON + remotePort + "/" + protocol;
	}

	protected SipCall getOutboundCall() {
		return outboundCall;
	}

	protected static String getHost() {
		return host;
	}

	protected SipPhone getOutboundPhone() {
		return outboundPhone;
	}

	protected static int getPort() {
		return port;
	}

	protected String getRemoteHost() {
		return remoteHost;
	}

	protected int getRemotePort() {
		return remotePort;
	}

	protected String getRemoteUser() {
		return remoteUser;
	}

	protected void setOutboundCallTargetUsername(String aRemoteUser) {
		this.remoteUser = aRemoteUser;
	}

	protected SipStack getSipStack() {
		return sipStack;
	}

	public SipCall getInboundCall() {
		return inboundCall;
	}

	public SipPhone getInboundPhone() {
		return inboundPhone;
	}

	public SipCall getSecondInboundCall() {
		return secondInboundCall;
	}

	public SipCall getThirdInboundCall() {
		return thirdInboundCall;
	}

	public SessionDescription getInboundPhoneSdp() {
		return inboundPhoneSdp;
	}

	public SessionDescription getSecondInboundPhoneSdp() {
		return secondInboundPhoneSdp;
	}

	public SessionDescription getThirdInboundPhoneSdp() {
		return thirdInboundPhoneSdp;
	}

	public SessionDescription getOutboundPhoneSdp() {
		return outboundPhoneSdp;
	}

	public MediaDescription getHoldMediaDescription() {
		return holdMediaDescription;
	}

	public MediaDescription getInactiveHoldMediaDescription() {
		return inactiveHoldMediaDescription;
	}

    protected static void initializeApplicationContext(String applicationContextFileName) {
    	applicationContext = new ClassPathXmlApplicationContext(applicationContextFileName);
    	setSleepBeforeSendingMessages();
    }

    protected static void setSleepBeforeSendingMessages() {
    	SimpleSipStack simpleSipStack = (SimpleSipStack)applicationContext.getBean("simpleSipStack");
    	simpleSipStack.setSleepIntervalBeforeSending(SLEEP_INTERVAL_BEFORE_SENDING_MESSAGE);
    }

    protected static void destroyApplicationContext() {
    	if (applicationContext != null)
    		applicationContext.destroy();
    }

	protected static ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	protected static void setApplicationContext(ClassPathXmlApplicationContext ctx) {
		applicationContext = ctx;
	}

	private void generateNewPhoneNames() {
        outboundPhoneName = "outboundphone" + System.currentTimeMillis();
        inboundPhoneName = "inboundphone" + System.currentTimeMillis();
        secondInboundPhoneName = "secondinboundphone" + System.currentTimeMillis();
        thirdInboundPhoneName = "thirdinboundphone" + System.currentTimeMillis();
	}

	protected void assertWeGetOK(MediaDescription mediaDescription) {
		boolean receivedResponse = getOutboundCall().waitOutgoingCallResponse(FIVE_THOUSAND);
		assertTrue(DIDN_T_EVER_GET_OK, receivedResponse);
		while(receivedResponse) {
			if (getOutboundCall().getReturnCode() == Response.OK) {
				SipResponse response = getOutboundCall().getLastReceivedResponse();
				assertNotNull(response.getRawContent());
				try {
					SessionDescription sd = SdpFactory.getInstance().createSessionDescription(new String(response.getRawContent()));
					assertEquals("Didn't receive expected media description", mediaDescription.toString(), SessionDescriptionHelper.getActiveMediaDescription(sd).toString());
				} catch (SdpParseException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
				break;
			}
			receivedResponse = getOutboundCall().waitOutgoingCallResponse(FIVE_THOUSAND);
			assertTrue(DIDN_T_EVER_GET_OK, receivedResponse);
		}
	}

	protected void assertWeGetOKWithHoldSdp() {
		boolean receivedResponse = getOutboundCall().waitOutgoingCallResponse(FIVE_THOUSAND);
		assertTrue(DIDN_T_EVER_GET_OK, receivedResponse);
		while(receivedResponse) {
			if (getOutboundCall().getReturnCode() == Response.OK) {
				SipResponse response = getOutboundCall().getLastReceivedResponse();
				assertNotNull(response.getRawContent());
				assertTrue("Didn't receive expected sdp", new String(response.getRawContent()).indexOf(IN_IP4_0_0_0_0) > -1);
				break;
			}
			receivedResponse = getOutboundCall().waitOutgoingCallResponse(FIVE_THOUSAND);
			assertTrue(DIDN_T_EVER_GET_OK, receivedResponse);
		}
	}

	protected void waitForCallSendTryingBusyHere(SipCall call) throws Exception {
		assertTrue(call.waitForIncomingCall(FIVE_THOUSAND));
		assertTrue(call.sendIncomingCallResponse(Response.TRYING, TRYING, 0));
		Thread.sleep(ONE_THOUSAND);
		assertTrue(call.sendIncomingCallResponse(Response.BUSY_HERE, "Busy", 0));
	}

	protected void waitForCallSendTryingRingingOkWaitForAck(SipUnitPhone sipUnitPhone) {
		SipCall sipCall = getSipUnitPhoneCall(sipUnitPhone);
		SessionDescription sdp = getSipUnitPhoneSdp(sipUnitPhone);
		MediaDescription mediaDescription = getSipUnitPhoneMediaDescription(sipUnitPhone);
		waitForCallSendTryingRingingOkWaitForAck(sipCall, sdp, mediaDescription);
	}

	private void waitForCallSendTryingRingingOkWaitForAck(SipCall call, SessionDescription sessionDescription, MediaDescription mediaDescription) {
		waitForCallAssertMediaDescriptionSendTryingRingingOk(call, null, sessionDescription, mediaDescription);
		assertTrue("No ACK", call.waitForAck(FIVE_THOUSAND));
		assertTrue(new String(call.getLastReceivedRequest().getRawContent()).contains(IN_IP4_0_0_0_0));
	}

	protected void waitForCallSendTryingRingingOk(SipUnitPhone sipUnitPhone) {
		waitForCallAssertMediaDescriptionSendTryingRingingOk(sipUnitPhone, null);
	}

	protected void waitForCallAssertMediaDescriptionSendTryingRingingOk(SipUnitPhone sipUnitPhone, MediaDescription mediaDescriptionToAssert) {
		SipCall sipCall = getSipUnitPhoneCall(sipUnitPhone);
		SessionDescription sdp = getSipUnitPhoneSdp(sipUnitPhone);
		MediaDescription mediaDescription = getSipUnitPhoneMediaDescription(sipUnitPhone);
		waitForCallAssertMediaDescriptionSendTryingRingingOk(sipCall, mediaDescriptionToAssert, sdp, mediaDescription);
	}

	private void waitForCallAssertMediaDescriptionSendTryingRingingOk(SipCall call, MediaDescription mediaDescriptionToAssert, SessionDescription sessionDescription,  MediaDescription responseMediaDescription) {
		assertTrue(call.waitForIncomingCall(FIVE_THOUSAND));
		SipRequest inviteRequest = call.getLastReceivedRequest();
		assertEquals(Request.INVITE, ((Request)inviteRequest.getMessage()).getMethod());
		if (mediaDescriptionToAssert != null) {
			if (inviteRequest.getRawContent() == null)
				fail("Expected SDP in initial invite");
			String body = new String(call.getLastReceivedRequest().getRawContent());
			try {
				assertMediaDescriptionInSessionDescription(mediaDescriptionToAssert, body);
			} catch (SdpException e) {
				throw new RuntimeException(e.getMessage(),e);
			}
		}

		assertTrue(call.sendIncomingCallResponse(Response.TRYING, TRYING, 0));
		assertTrue(call.sendIncomingCallResponse(Response.RINGING, RINGING, 0));

		SessionDescriptionHelper.setMediaDescription(sessionDescription, responseMediaDescription);
		assertTrue(call.sendIncomingCallResponse(Response.OK, OK, 0, sessionDescription.toString(), APPLICATION, SDP, null, null));
	}

	protected void waitForCallSendOk(SipCall call, SessionDescription sdp) {
		assertTrue(call.waitForIncomingCall(FIVE_THOUSAND));
		assertTrue(call.sendIncomingCallResponse(Response.OK, OK, 0, sdp.toString(), APPLICATION, SDP, null, null));
	}

	protected void waitForCallSendTryingRinging(SipCall call) {
		assertTrue(call.waitForIncomingCall(FIVE_THOUSAND));
		assertTrue(call.sendIncomingCallResponse(Response.TRYING, TRYING, 0));
		assertTrue(call.sendIncomingCallResponse(Response.RINGING, RINGING, 0));
	}

	protected void waitForAckAssertMediaDescription(SipUnitPhone sipUnitPhone, MediaDescription mediaDescriptionToAssert) throws Exception {
		SipCall sipCall = getSipUnitPhoneCall(sipUnitPhone);
		waitForAckAssertMediaDescription(sipCall, mediaDescriptionToAssert);
	}

	protected void waitForEmptyAck(SipUnitPhone sipUnitPhone) {
		SipCall sipCall = getSipUnitPhoneCall(sipUnitPhone);
		assertTrue("No ACK", sipCall.waitForAck(FIVE_THOUSAND));
		assertNull("Expected empty ACK", sipCall.getLastReceivedRequest().getRawContent());
	}

	private void waitForAckAssertMediaDescription(SipCall call, MediaDescription mediaDescriptionToAssert) throws Exception {
		assertTrue("No ACK", call.waitForAck(FIVE_THOUSAND));
		String sdpString = new String(call.getLastReceivedRequest().getRawContent());
		assertMediaDescriptionInSessionDescription(mediaDescriptionToAssert, SdpFactory.getInstance().createSessionDescription(sdpString));
	}

	protected void waitForEmptyReinviteRespondOkAssertAckMediaDescription(SipUnitPhone sipUnitPhone, MediaDescription mediaDescriptionToAssert) throws Exception {
		waitForEmptyReinviteRespondOk(sipUnitPhone);
		waitForReinviteAckAssertMediaDescription(sipUnitPhone, mediaDescriptionToAssert);
	}

	protected void waitForEmptyReinviteRespondOk(SipUnitPhone sipUnitPhone) {
		SipCall sipCall = getSipUnitPhoneCall(sipUnitPhone);
		String sipAddress = getSipUnitPhoneAddress(sipUnitPhone);
		SessionDescription sdp = getSipUnitPhoneSdp(sipUnitPhone);
		MediaDescription mediaDescription = getSipUnitPhoneMediaDescription(sipUnitPhone);

		SipTransaction firstReinviteTransaction = sipCall.waitForReinvite(FIVE_THOUSAND);
		assertNotNull(NO_REINVITE, firstReinviteTransaction);
		assertNull("Did not expect SDP in reinvite", firstReinviteTransaction.getRequest().getRawContent());

		SessionDescriptionHelper.setMediaDescription(sdp, mediaDescription);
		sipCall.respondToReinvite(firstReinviteTransaction, Response.OK, OK, 0, sipAddress, DISPLAY_NAME, sdp.toString(), APPLICATION, SDP);
	}

	protected void respondWithInitialOk(SipUnitPhone sipUnitPhone) {
		SipCall sipCall = getSipUnitPhoneCall(sipUnitPhone);
		SessionDescription sdp = getSipUnitPhoneSdp(sipUnitPhone);
		MediaDescription mediaDescription = getSipUnitPhoneMediaDescription(sipUnitPhone);

		SessionDescriptionHelper.setMediaDescription(sdp, mediaDescription);
		assertTrue(sipCall.sendIncomingCallResponse(Response.OK, OK, 0, sdp.toString(), APPLICATION, SDP, null, null));
	}

	protected void waitForReinviteAckAssertMediaDescription(SipUnitPhone sipUnitPhone, MediaDescription mediaDescriptionToAssert) {
		SipCall sipCall = getSipUnitPhoneCall(sipUnitPhone);
		assertTrue("No ACK for reinvite", sipCall.waitForAck(FIVE_THOUSAND));
		SipRequest ackRequest = sipCall.getLastReceivedRequest();

		if (mediaDescriptionToAssert == null && ackRequest.getRawContent() == null)
			return;
		if (ackRequest.getRawContent() == null)
			fail("Expected SDP in ACK");

		String ackSdp = new String(ackRequest.getRawContent());
		try {
			SessionDescription sessionDescription = SdpFactory.getInstance().createSessionDescription(ackSdp);
			assertMediaDescriptionInSessionDescription(mediaDescriptionToAssert, sessionDescription);
		} catch (SdpParseException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (SdpException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	protected SipTransaction waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone sipUnitPhone, MediaDescription mediaDescriptionToAssert) {
		return waitForReinviteAssertMediaDescriptionRespondOk(sipUnitPhone, mediaDescriptionToAssert, true);
	}

	protected SipTransaction waitForReinviteAssertMediaDescription(SipUnitPhone sipUnitPhone, MediaDescription mediaDescriptionToAssert) {
		return waitForReinviteAssertMediaDescriptionRespondOk(sipUnitPhone, mediaDescriptionToAssert, false);
	}

	private SipTransaction waitForReinviteAssertMediaDescriptionRespondOk(SipUnitPhone sipUnitPhone, MediaDescription mediaDescriptionToAssert, boolean respondOk) {
		SipCall sipCall = getSipUnitPhoneCall(sipUnitPhone);
		String sipAddress = getSipUnitPhoneAddress(sipUnitPhone);
		SessionDescription sdp = getSipUnitPhoneSdp(sipUnitPhone);
		MediaDescription mediaDescription = getSipUnitPhoneMediaDescription(sipUnitPhone);

		return waitForReinviteAssertMediaDescriptionRespondOk(sipCall, mediaDescriptionToAssert, sipAddress, sdp, mediaDescription, respondOk);
	}

	private SipTransaction waitForReinviteAssertMediaDescriptionRespondOk(SipCall call, MediaDescription mediaDescriptionToAssert, String address, SessionDescription responseSessionDescription, MediaDescription responseMediaDescription, boolean respondOk) {
		SipTransaction reinviteTransaction = call.waitForReinvite(FIVE_THOUSAND);
		assertNotNull(NO_REINVITE, reinviteTransaction);
		SessionDescription sessionDescription = null;
		try {
			if (reinviteTransaction.getRequest().getRawContent() == null)
				fail("Expected SDP in reinvite");
			String receivedSdp = new String(reinviteTransaction.getRequest().getRawContent());
			if (receivedSdp != null)
				sessionDescription = SdpFactory.getInstance().createSessionDescription(receivedSdp);

			assertMediaDescriptionInSessionDescription(mediaDescriptionToAssert, sessionDescription);
		} catch (SdpParseException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (SdpException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		if (respondOk) {
			SessionDescriptionHelper.setMediaDescription(responseSessionDescription, responseMediaDescription);
			call.respondToReinvite(reinviteTransaction, Response.OK, OK, 0, address, DISPLAY_NAME, responseSessionDescription.toString(), APPLICATION, SDP);
			call.listenForAck();
		}

		return reinviteTransaction;
	}

	protected void assertMediaDescriptionInSessionDescription(MediaDescription mediaDescription, String sdpBody) throws SdpException {
		assertMediaDescriptionInSessionDescription(mediaDescription, SdpFactory.getInstance().createSessionDescription(sdpBody));
	}

	protected void assertMediaDescriptionInSessionDescription(MediaDescription mediaDescription, SessionDescription sessionDescription) throws SdpException {
		if (mediaDescription == null && sessionDescription != null)
			fail("Unexpected SDP");
		else if (mediaDescription != null && sessionDescription == null)
			fail("Did not expect empty SDP");

		assertEquals("More than one media description found", 1, sessionDescription.getMediaDescriptions(false).size());
		String mediaDescriptionString  = mediaDescription.toString();
		assertEquals(mediaDescriptionString, ((MediaDescription)sessionDescription.getMediaDescriptions(false).get(0)).toString());
	}

	protected void waitForReinviteRespondError(SipCall call, String address) {
		waitForReinviteRespondError(call, address, Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST, "Computer says no");
	}
	
	protected void waitForReinviteRespondError(SipCall call, String address, int statusCode, String message) {
		SipTransaction firstReinviteTransaction = call.waitForReinvite(FIVE_THOUSAND);
		assertNotNull(firstReinviteTransaction);
		call.respondToReinvite(firstReinviteTransaction, statusCode, message, 0, address, DISPLAY_NAME, null, (String)null, null);
	}

	protected void waitForByeAndRespond(SipCall call) {
		assertTrue(call.waitForDisconnect(FIVE_THOUSAND));
		assertTrue(call.respondToDisconnect());
	}

	protected void waitForCancelRespondOk(SipPhone phone) throws ParseException {
		RequestEvent re = waitForCancel(phone);
		respondToCancelWithOk(phone, re);
	}

	protected RequestEvent waitForCancel(SipPhone phone) {
		RequestEvent re = phone.waitRequest(FIVE_THOUSAND);
		assertTrue(re != null);
        while (re.getRequest().getMethod().equals(Request.CANCEL) == false) {
    		re = phone.waitRequest(FIVE_THOUSAND);
    		assertTrue(re != null);
        }
		return re;
	}

	protected void respondToCancelWithOk(SipPhone phone, RequestEvent re) throws ParseException {
		Response response = getSipStack().getMessageFactory().createResponse(Response.OK, re.getRequest());
		phone.sendReply(re, response);
	}

	private List<SipResponse> removeDuplicates(List<SipResponse> list) {
		List<SipResponse> res = new ArrayList<SipResponse>();
		HashMap<String, SipResponse> messages = new HashMap<String, SipResponse>();
		for (SipResponse sipResponse : list) {
			String messageHash = MessageDigestHelper.generateDigest(sipResponse.getMessage().toString());
			if (messages.containsKey(messageHash)) {
				continue;
			} else {
				res.add(sipResponse);
				messages.put(messageHash, sipResponse);
			}
		}
		return res;
	}

	protected void assertOutboundCallResponses(int[] expectedResponses) {
		getOutboundCall().waitForAnswer(FIVE_THOUSAND);

		List<?> originalResponses = getOutboundCall().getAllReceivedResponses();
		List<SipResponse> responsesWithoutTrying = new ArrayList<SipResponse>();
		for(Object o : originalResponses) {
			SipResponse currentResponse = (SipResponse)o;
			if(currentResponse.getStatusCode() == Response.TRYING)
				continue;
			responsesWithoutTrying.add(currentResponse);
		}
		List<SipResponse>responses = removeDuplicates(responsesWithoutTrying);

		if(expectedResponses.length != responses.size()) {
			StringBuilder message = new StringBuilder();
			message.append(String.format("Expected %d but got %d responses: ", expectedResponses.length, responses.size()));
			for(Object response : responses)
				message.append(response + " ");
			fail(message.toString());
		}

		for(int i = 0; i < expectedResponses.length; i++)
			assertEquals(expectedResponses[i], ((SipResponse)responses.get(i)).getStatusCode());
	}

	protected void waitForReinviteErrorResponse(SipPhone phone, SipTransaction reinviteTransaction, int responseCode) {
		Response response = waitForNonProvisionalResponse(phone, reinviteTransaction);
		assertEquals(responseCode, response.getStatusCode());
	}

	protected void waitForReinviteOKResponseAndAssertHoldSdp(SipPhone phone, SipTransaction reinviteTransaction) {
		Response response = waitForNonProvisionalResponse(phone, reinviteTransaction);
		assertEquals(Response.OK, response.getStatusCode());
		assertTrue(new String(response.getRawContent()).indexOf(IN_IP4_0_0_0_0) > -1);
	}

	protected void waitForReinviteOKResponseAndAssertMediaDescription(SipPhone phone, SipTransaction reinviteTransaction, MediaDescription mediaDescription) {
		Response response = waitForNonProvisionalResponse(phone, reinviteTransaction);
		assertEquals(Response.OK, response.getStatusCode());
		try {
			assertMediaDescriptionInSessionDescription(mediaDescription, new String(response.getRawContent()));
		} catch (SdpException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	protected Response waitForNonProvisionalResponse(SipPhone phone, SipTransaction reinviteTransaction) {
		while (true) {
			EventObject e = phone.waitResponse(reinviteTransaction, FIVE_THOUSAND);
			assertNotNull(e);
			assertTrue(e instanceof ResponseEvent);
			Response response = ((ResponseEvent)e).getResponse();
			if (response.getStatusCode() < Response.OK)
				continue;

			return response;
		}
	}

	protected void assertNoFurtherMessages(SipUnitPhone first, SipUnitPhone second) {
		SipCall firstCall = getSipUnitPhoneCall(first);
		SipCall secondCall = getSipUnitPhoneCall(second);

		int firstMessagesBefore = firstCall.getAllReceivedRequests().size() + firstCall.getAllReceivedResponses().size();
		int secondMessagesBefore = secondCall.getAllReceivedRequests().size() + secondCall.getAllReceivedResponses().size();

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		int firstMessagesAfter = firstCall.getAllReceivedRequests().size() + firstCall.getAllReceivedResponses().size();
		int secondMessagesAfter = secondCall.getAllReceivedRequests().size() + secondCall.getAllReceivedResponses().size();

		assertEquals("Got extra " + (firstMessagesAfter - firstMessagesBefore) + " messages for " + first, firstMessagesBefore, firstMessagesAfter);
		assertEquals("Got extra " + (secondMessagesAfter - secondMessagesBefore) + " messages for " + second, secondMessagesBefore, secondMessagesAfter);
	}

	public MediaDescription getInboundPhoneMediaDescription() {
		return inboundPhoneMediaDescription;
	}

	public MediaDescription getOutboundPhoneMediaDescription() {
		return outboundPhoneMediaDescription;
	}

	public MediaDescription getSecondInboundPhoneMediaDescription() {
		return secondInboundPhoneMediaDescription;
	}

	public MediaDescription getThirdInboundPhoneMediaDescription() {
		return thirdInboundPhoneMediaDescription;
	}

	public MediaDescription getInboundPhoneHoldMediaDescription() {
		return SessionDescriptionHelper.generateHoldMediaDescription(getInboundPhoneMediaDescription());
	}

	public MediaDescription getOutboundPhoneHoldMediaDescription() {
		return SessionDescriptionHelper.generateHoldMediaDescription(getOutboundPhoneMediaDescription());
	}

	public MediaDescription getSecondInboundPhoneHoldMediaDescription() {
		return SessionDescriptionHelper.generateHoldMediaDescription(getSecondInboundPhoneMediaDescription());
	}

	public MediaDescription getThirdInboundPhoneHoldMediaDescription() {
		return SessionDescriptionHelper.generateHoldMediaDescription(getThirdInboundPhoneMediaDescription());
	}

	public SessionDescription createHoldSessionDescription() {
		SessionDescription sd = SessionDescriptionHelper.createSessionDescription(ONE_TWO_SEVEN_ZERO_ZERO_ONE, SIMPLE_SIP_STACK_TEST_CASE);
		MediaDescription md = SessionDescriptionHelper.generateHoldMediaDescription();
		SessionDescriptionHelper.setMediaDescription(sd, md);
		return sd;
	}

	protected MediaDescription createOfferMediaDescription(String host, int port, String[] mediaTypes) {
		MediaDescription md = SdpFactory.getInstance().createMediaDescription("audio", port, 0, "RTP/AVP", mediaTypes);
		try {
			Connection conn = SdpFactory.getInstance().createConnection(host);
			md.setConnection(conn);
			for(String mediaType : mediaTypes)
				md.setAttribute("rtpmap", mediaType + " PCMU/8000");
		} catch(SdpException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return md;
	}

	protected String createInitialOkResponseWithOfferString(String dialogId) {
		StringBuilder sdp = new StringBuilder();
		sdp.append("v=0\r\n");
		sdp.append("o=- 1186677580516 1186677580516 IN IP4 127.0.0.1\r\n");
		sdp.append("s=-\r\n");
		sdp.append("c=IN IP4 127.0.0.1\r\n");
		sdp.append("t=0 0\r\n");
		sdp.append("m=audio 9876 RTP/AVP 1\r\n");
		sdp.append("c=IN IP4 127.0.0.1\r\n");
		sdp.append("a=rtpmap:1 PCMU/8000\r\n");
		sdp.append("a=sendrecv\r\n");
		StringBuilder sb = new StringBuilder();
        sb.append("SIP/2.0 200 Ok\r\n");
        sb.append(String.format("Call-ID: %s\r\n", dialogId));
        sb.append("CSeq: 1 INVITE\r\n");
        sb.append("From: <sip:secondinboundphone@10.237.33.51:6061>;tag=1220844105\r\n");
        sb.append("To: <sip:inboundphone1186408538178@10.237.33.51:6061>;tag=1531384406\r\n");
        sb.append("Via: SIP/2.0/UDP 127.0.0.1:6060;branch=z9hG4bKbded7565b0ea30f2c104b1cf38cdb9f0\r\n");
        sb.append("Contact: <sip:inboundphone1186408538178@127.0.0.1:6061;lr;transport=udp>\r\n");
        sb.append("Expires: 0\r\n");
        sb.append("Content-Type: application/sdp\r\n");
        sb.append(String.format("Content-Length: %d\r\n\r\n", sdp.length()));
        sb.append(sdp);
		return sb.toString();
	}

	protected String createReinviteOkResponseWithOfferString(String dialogId) {
		StringBuilder sdp = new StringBuilder();
		sdp.append("v=0\r\n");
		sdp.append("o=- 1186677580516 1186677580516 IN IP4 127.0.0.1\r\n");
		sdp.append("s=-\r\n");
		sdp.append("c=IN IP4 127.0.0.1\r\n");
		sdp.append("t=0 0\r\n");
		sdp.append("m=audio 9876 RTP/AVP 1\r\n");
		sdp.append("c=IN IP4 127.0.0.1\r\n");
		sdp.append("a=rtpmap:1 PCMU/8000\r\n");
		sdp.append("a=sendrecv\r\n");
		StringBuilder sb = new StringBuilder();
        sb.append("SIP/2.0 200 Ok\r\n");
        sb.append(String.format("Call-ID: %s\r\n", dialogId));
        sb.append("CSeq: 2 INVITE\r\n");
        sb.append("From: <sip:secondinboundphone@10.237.33.51:6061>;tag=1220844105\r\n");
        sb.append("To: <sip:inboundphone1186408538178@10.237.33.51:6061>;tag=1531384406\r\n");
        sb.append("Via: SIP/2.0/UDP 127.0.0.1:6060;branch=z9hG4bKbded7565b0ea30f2c104b1cf38cdb9f0\r\n");
        sb.append("Contact: <sip:inboundphone1186408538178@127.0.0.1:6061;lr;transport=udp>\r\n");
        sb.append("Expires: 0\r\n");
        sb.append("Content-Type: application/sdp\r\n");
        sb.append(String.format("Content-Length: %d\r\n\r\n", sdp.length()));
        sb.append(sdp);
		return sb.toString();
	}

	protected String createInitialOkResponseString(String dialogId) {
		return createResponseString(dialogId, "INVITE", "200 Ok");
	}

	private String createResponseString(String dialogId, String requestType, String responseNumberAndText) {
		StringBuilder sb = new StringBuilder();
        sb.append("SIP/2.0 " + responseNumberAndText + "\r\n");
        sb.append(String.format("Call-ID: %s\r\n", dialogId));
        sb.append("CSeq: 1 " + requestType + "\r\n");
        sb.append("From: <sip:secondinboundphone@10.237.33.51:6061>;tag=1220844105\r\n");
        sb.append("To: <sip:inboundphone1186408538178@10.237.33.51:6061>;tag=1531384406\r\n");
        sb.append("Via: SIP/2.0/UDP 127.0.0.1:6060;branch=z9hG4bKbded7565b0ea30f2c104b1cf38cdb9f0\r\n");
        sb.append("Contact: <sip:inboundphone1186408538178@127.0.0.1:6061;lr;transport=udp>\r\n");
        sb.append("Expires: 0\r\n");
        sb.append("Content-Length: 0");
        return sb.toString();
	}

	protected String createInfo481ResponseString(String dialogId){
		return createResponseString(dialogId, "INFO", "481 Call Leg does not exist");
	}

	protected String createInfoOkResponseString(String dialogId){
		return createResponseString(dialogId, "INFO", "200 Ok");
	}

	protected String createInitialErrorResponseString(String dialogId, int responseCode, String responseString) {
		StringBuilder sb = new StringBuilder();
        sb.append(String.format("SIP/2.0 %d %s\r\n", responseCode, responseString));
        sb.append(String.format("Call-ID: %s\r\n", dialogId));
        sb.append("CSeq: 1 INVITE\r\n");
        sb.append("From: <sip:secondinboundphone@10.237.33.51:6061>;tag=1220844105\r\n");
        sb.append("To: <sip:inboundphone1186408538178@10.237.33.51:6061>;tag=1531384406\r\n");
        sb.append("Via: SIP/2.0/UDP 127.0.0.1:6060;branch=z9hG4bKbded7565b0ea30f2c104b1cf38cdb9f0\r\n");
        sb.append("Contact: <sip:inboundphone1186408538178@127.0.0.1:6061;lr;transport=udp>\r\n");
        sb.append("Expires: 0\r\n");
        sb.append("Content-Length: 0");
        return sb.toString();
	}

	protected String createReinviteErrorResponseString(String dialogId, int responseCode, String responseString) {
		StringBuilder sb = new StringBuilder();
        sb.append(String.format("SIP/2.0 %d %s\r\n", responseCode, responseString));
        sb.append(String.format("Call-ID: %s\r\n", dialogId));
        sb.append("CSeq: 1 INVITE\r\n");
        sb.append("From: <sip:secondinboundphone@10.237.33.51:6061>;tag=1220844105\r\n");
        sb.append("To: <sip:inboundphone1186408538178@10.237.33.51:6061>;tag=1531384406\r\n");
        sb.append("Via: SIP/2.0/UDP 127.0.0.1:6060;branch=z9hG4bKbded7565b0ea30f2c104b1cf38cdb9f0\r\n");
        sb.append("Contact: <sip:inboundphone1186408538178@127.0.0.1:6061;lr;transport=udp>\r\n");
        sb.append("Expires: 0\r\n");
        sb.append("Content-Length: 0");
        return sb.toString();
	}

	protected String createByeOkResponseString(String dialogId) {
		StringBuilder sb = new StringBuilder();
        sb.append("SIP/2.0 200 Ok\r\n");
        sb.append(String.format("Call-ID: %s\r\n", dialogId));
        sb.append("CSeq: 2 BYE\r\n");
        sb.append("From: <sip:secondinboundphone@10.237.33.51:6061>;tag=1220844105\r\n");
        sb.append("To: <sip:inboundphone1186408538178@10.237.33.51:6061>;tag=1531384406\r\n");
        sb.append("Via: SIP/2.0/UDP 127.0.0.1:6060;branch=z9hG4bKbded7565b0ea30f2c104b1cf38cdb9f0\r\n");
        sb.append("Contact: <sip:inboundphone1186408538178@127.0.0.1:6061;lr;transport=udp>\r\n");
        sb.append("Expires: 0\r\n");
        sb.append("Content-Length: 0");
        return sb.toString();
	}

	protected String createByeErrorResponseString(String dialogId) {
		StringBuilder sb = new StringBuilder();
        sb.append("SIP/2.0 500 Internal Server Error\r\n");
        sb.append(String.format("Call-ID: %s\r\n", dialogId));
        sb.append("CSeq: 2 BYE\r\n");
        sb.append("From: <sip:secondinboundphone@10.237.33.51:6061>;tag=1220844105\r\n");
        sb.append("To: <sip:inboundphone1186408538178@10.237.33.51:6061>;tag=1531384406\r\n");
        sb.append("Via: SIP/2.0/UDP 127.0.0.1:6060;branch=z9hG4bKbded7565b0ea30f2c104b1cf38cdb9f0\r\n");
        sb.append("Contact: <sip:inboundphone1186408538178@127.0.0.1:6061;lr;transport=udp>\r\n");
        sb.append("Expires: 0\r\n");
        sb.append("Content-Length: 0");
        return sb.toString();
	}

	protected String createByeRequestString() {
		StringBuilder sb = new StringBuilder();
        sb.append("BYE sip:127.0.0.1:6060 SIP/2.0\r\n");
        sb.append("Call-ID: c8da1e127ba14de5fca2c4ea8b6888e9@127.0.0.1\r\n");
        sb.append("CSeq: 1 BYE\r\n");
        sb.append("From: <sip:secondinboundphone@10.237.33.51:6061>;tag=1220844105\r\n");
        sb.append("To: <sip:inboundphone1186408538178@10.237.33.51:6061>;tag=1531384406\r\n");
        sb.append("Via: SIP/2.0/UDP 127.0.0.1:6060;branch=z9hG4bKbded7565b0ea30f2c104b1cf38cdb9f0\r\n");
        sb.append("Contact: <sip:inboundphone1186408538178@127.0.0.1:6061;lr;transport=udp>\r\n");
        sb.append("Expires: 0\r\n");
        sb.append("Content-Length: 0");
        return sb.toString();
	}

	protected String createInviteRequestString() {
		StringBuilder sdp = new StringBuilder();
		sdp.append("v=0\r\n");
		sdp.append("o=- 1186677580516 1186677580516 IN IP4 127.0.0.1\r\n");
		sdp.append("s=-\r\n");
		sdp.append("c=IN IP4 127.0.0.1\r\n");
		sdp.append("t=0 0\r\n");
		sdp.append("m=audio 9876 RTP/AVP 1\r\n");
		sdp.append("c=IN IP4 127.0.0.1\r\n");
		sdp.append("a=rtpmap:1 PCMU/8000\r\n");
		sdp.append("a=sendrecv\r\n");
		StringBuilder sb = new StringBuilder();
        sb.append("INVITE sip:127.0.0.1:6060 SIP/2.0\r\n");
        sb.append("Call-ID: c8da1e127ba14de5fca2c4ea8b6888e9@127.0.0.1\r\n");
        sb.append("CSeq: 1 INVITE\r\n");
        sb.append("From: <sip:secondinboundphone@10.237.33.51:6061>;tag=1220844105\r\n");
        sb.append("To: <sip:inboundphone1186408538178@10.237.33.51:6061>;tag=1531384406\r\n");
        sb.append("Via: SIP/2.0/UDP 127.0.0.1:6060;branch=z9hG4bKbded7565b0ea30f2c104b1cf38cdb9f0\r\n");
        sb.append("Contact: <sip:inboundphone1186408538178@127.0.0.1:6061;lr;transport=udp>\r\n");
        sb.append("Expires: 0\r\n");
        sb.append("Content-Type: application/sdp\r\n");
        sb.append(String.format("Content-Length: %d\r\n\r\n", sdp.length()));
        sb.append(sdp);
        return sb.toString();
	}

	protected List<AbstractCallEvent> filterCallEventsForCallId(List<AbstractCallEvent> callEvents, String callId) {
		List<AbstractCallEvent> res = new ArrayList<AbstractCallEvent>();
		AbstractCallEvent[] events = callEvents.toArray(new AbstractCallEvent[callEvents.size()]);
		for (AbstractCallEvent current : events) {
			if (current.getCallId().equals(callId))
				res.add(current);
		}
		return res;
	}

	protected List<AbstractDialogEvent> filterDialogEventsForDialogId(List<AbstractDialogEvent> dialogEvents, String id) {
		List<AbstractDialogEvent> res = new ArrayList<AbstractDialogEvent>();
		AbstractDialogEvent[] events = dialogEvents.toArray(new AbstractDialogEvent[dialogEvents.size()]);
		for (AbstractDialogEvent current : events) {
			if (current.getId().equals(id))
				res.add(current);
		}
		return res;
	}
}
