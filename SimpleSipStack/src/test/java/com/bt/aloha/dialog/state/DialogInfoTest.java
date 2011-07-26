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
package com.bt.aloha.dialog.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import gov.nist.javax.sip.address.AddressFactoryImpl;
import gov.nist.javax.sip.header.Route;
import gov.nist.javax.sip.header.RouteList;

import java.util.HashMap;
import java.util.Properties;

import javax.sdp.MediaDescription;
import javax.sip.ClientTransaction;
import javax.sip.ServerTransaction;
import javax.sip.SipFactory;
import javax.sip.address.Address;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.RecordRouteHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;

import org.easymock.classextension.EasyMock;
import org.junit.Test;

import com.bt.aloha.dialog.state.DialogInfo;
import com.bt.aloha.dialog.state.PendingReinvite;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.dialog.state.ReinviteInProgress;
import com.bt.aloha.dialog.state.TerminationCause;
import com.bt.aloha.dialog.state.TerminationMethod;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.stack.StackException;
import com.bt.aloha.state.StateInfoBase;


public class DialogInfoTest {
	private final String dummyAckString;
	private Properties props;
	private Request request;

	public DialogInfoTest() {
		StringBuilder sb = new StringBuilder(); 
		sb.append("ACK sip:inboundphone1186387187453@192.168.1.101:6061;lr;transport=udp SIP/2.0\r\n");
		sb.append("Via: SIP/2.0/UDP 127.0.0.1:6060;branch=z9hG4bK9d95b4a1313e097668e3e7ae6cdd4432\r\n");
		sb.append("CSeq: 1 ACK\r\n");
		sb.append("Call-ID: fe5ed1aeba71a33a28a03de164d73ad8@127.0.0.1\r\n");
		sb.append("From: <sip:secondinboundphone1186387187453@192.168.1.101:6061>;tag=1622351056\r\n");
		sb.append("To: <sip:inboundphone1186387187453@192.168.1.101:6061>;tag=332117887\r\n");
		sb.append("Expires: 0\r\n");
		sb.append("Max-Forwards: 70\r\n");
		sb.append("Content-Type: application/sdp\r\n");
		sb.append("Content-Length: 179\r\n");
		sb.append("\r\n");
		sb.append("v=0\r\n");
		sb.append("o=- 1186387187484 1186387187484 IN IP4 127.0.0.1\r\n");
		sb.append("s=PowerToTheTreehouse\r\n");
		sb.append("c=IN IP4 0.0.0.0\r\n");
		sb.append("t=0 0\r\n");
		sb.append("m=audio 9876 RTP/AVP 1\r\n");
		sb.append("c=IN IP4 0.0.0.0\r\n");
		sb.append("a=rtpmap:1 PCMU/8000\r\n");
		sb.append("a=inactive\r\n");
		dummyAckString = sb.toString();
	}
	
	@Test
	public void testStringToAddressBadAddress() throws Exception {
		DialogInfo d = new DialogInfo("id", "test", "1.2.3.4");
		d.setRemoteParty("iamanidiot");
		try {
			d.getRemoteParty();
			fail("Expected exception");
		} catch (IllegalArgumentException e) {
			assertEquals("Failed to convert string iamanidiot to an Address", e.getMessage());
		}
	}
	
	@Test
	public void testSetTerminationMethodNullCurrentMethod() throws Exception {
		// setup
		DialogInfo di = new DialogInfo("id", "bollocks", "1.2.3.4");
		
		// act
		TerminationMethod previous = di.setTerminationMethod(TerminationMethod.Cancel);
		
		// assert
		assertEquals(TerminationMethod.Cancel, di.getTerminationMethod());
		assertEquals(TerminationMethod.None, previous);
	}
	
	@Test
	public void testSetTerminationMethodNoneCurrentNoneNew() throws Exception {
		// setup
		DialogInfo di = new DialogInfo("id", "bollocks", "1.2.3.4");
		
		// act
		TerminationMethod previous = di.setTerminationMethod(TerminationMethod.None);
		
		// assert
		assertEquals(TerminationMethod.None, di.getTerminationMethod());
		assertEquals(TerminationMethod.None, previous);
	}
	
	@Test
	public void testSetTerminationMethodTerminateReplacesCancel() throws Exception {
		// setup
		DialogInfo di = new DialogInfo("id", "bollocks", "1.2.3.4");
		di.setTerminationMethod(TerminationMethod.Cancel);
		
		// act
		TerminationMethod previous = di.setTerminationMethod(TerminationMethod.Terminate);
		
		// assert
		assertEquals(TerminationMethod.Terminate, di.getTerminationMethod());
		assertEquals(TerminationMethod.Cancel, previous);
	}
	
	@Test
	public void testSetTerminationMethodCancelDoesNotReplaceTerminate() throws Exception {
		// setup
		DialogInfo di = new DialogInfo("id", "bollocks", "1.2.3.4");
		di.setTerminationMethod(TerminationMethod.Terminate);
		
		// act
		TerminationMethod previous = di.setTerminationMethod(TerminationMethod.Cancel);
		
		// assert
		assertEquals(TerminationMethod.Terminate, di.getTerminationMethod());
		assertNull(previous);
	}
	
	@Test
	public void testSetTerminationMethodNoneReplacesCancel() throws Exception {
		// setup
		DialogInfo di = new DialogInfo("id", "bollocks", "1.2.3.4");
		di.setTerminationMethod(TerminationMethod.Cancel);
		
		// act
		TerminationMethod previous = di.setTerminationMethod(TerminationMethod.None);
		
		// assert
		assertEquals(TerminationMethod.None, di.getTerminationMethod());
		assertEquals(TerminationMethod.Cancel, previous);
	}
	
	@Test
	public void testSetTerminationMethodNoneReplacesTerminate() throws Exception {
		// setup
		DialogInfo di = new DialogInfo("id", "bollocks", "1.2.3.4");
		di.setTerminationMethod(TerminationMethod.Terminate);
		
		// act
		TerminationMethod previous = di.setTerminationMethod(TerminationMethod.None);
		
		// assert
		assertEquals(TerminationMethod.None, di.getTerminationMethod());
		assertEquals(TerminationMethod.Terminate, previous);
	}
	
	@Test
	public void testSetTerminationMethodNullThrowsArgumentException() throws Exception {
		// setup
		DialogInfo di = new DialogInfo("id", "bollocks", "1.2.3.4");
		
		// act
		try { 
			di.setTerminationMethod(null);
			fail("No exception");
		} catch(IllegalArgumentException e) {
			// expectexd
		}
	}
	
	@Test
	public void testOutboundConstructor() throws Exception {
		// act
		DialogInfo di = new DialogInfo("id", "me", "1.2.3.4", "sip:me", "sip:you", "tag", 0, false, true);
		
		// asset
		assertEquals("me", di.getSimpleSipBeanId());
		assertEquals("sip:me", di.getLocalParty().getURI().toString());
		assertEquals("sip:you", di.getRemoteParty().getURI().toString());
		assertEquals("tag", di.getLocalTag());
		assertEquals("id", di.getId());
		assertEquals(0, di.getCallAnswerTimeout());
		assertTrue("Should not have automatically place on hold set", di.isAutomaticallyPlaceOnHold());
		assertNull(di.getUsername());
		assertNull(di.getPassword());
	}
	
	//Tests outbound constructor sets the DialogInfo username and password when they are passed in.
	@Test
	public void testOutboundConstructorWithUserNameAndPassword() throws Exception {
		// setup
		String username = "Fred";
		String password = "Wilma";
		// act
		DialogInfo di = new DialogInfo("id", "me", "1.2.3.4", "sip:me", "sip:you", "tag", 0, false, true, username, password);
		
		// asset
		assertEquals("me", di.getSimpleSipBeanId());
		assertEquals("sip:me", di.getLocalParty().getURI().toString());
		assertEquals("sip:you", di.getRemoteParty().getURI().toString());
		assertEquals("tag", di.getLocalTag());
		assertEquals("id", di.getId());
		assertEquals(0, di.getCallAnswerTimeout());
		assertTrue("Should not have automatically place on hold set", di.isAutomaticallyPlaceOnHold());
		assertEquals(username,di.getUsername());
		assertEquals(password,di.getPassword());
		
	}
	
	@Test
	public void testClone() throws Exception {
		// setup
		DialogInfo d1 = new DialogInfo("id", "me", "1.2.3.4", "sip:me", "sip:you", "tag", 0, false, true);
		d1.setInviteClientTransaction(EasyMock.createMock(ClientTransaction.class));
		d1.setTerminationCause(TerminationCause.RemotePartyHungUp);
		d1.setAutomaton(true);
		
		RouteList routeList = new RouteList();
		Route route = new Route();
		route.setAddress(SipFactory.getInstance().createAddressFactory().createAddress("sip:two@domain.com;name=dick"));
		route.setParameter("lr", null);
		routeList.add(route);
		d1.setRouteList(routeList);

		MediaDescription offerMediaDescription = SessionDescriptionHelper.generateHoldMediaDescription();
		d1.setRemoteOfferMediaDescription(offerMediaDescription);
		d1.setLastAckRequest(SipFactory.getInstance().createMessageFactory().createRequest(dummyAckString));

		MediaDescription pendingMediaDescription = SessionDescriptionHelper.generateHoldMediaDescription();		
		PendingReinvite pendingReinvite = new PendingReinvite(pendingMediaDescription, Boolean.TRUE, "moose");
		d1.setPendingReinvite(pendingReinvite);
		
		HashMap<String,String> dynamicPayloadMap = new HashMap<String, String>();
		dynamicPayloadMap.put("abc", "123");
		d1.setDynamicMediaPayloadTypeMap(dynamicPayloadMap);
		
		// act
		DialogInfo d2 = d1.cloneObject();

		// assert
		assertEquals(d1.getSimpleSipBeanId(), d2.getSimpleSipBeanId());
		assertEquals(d1.getSipCallId(), d2.getSipCallId());
		assertEquals(d1.isInbound(), d2.isInbound());
		assertEquals(d1.getDialogState(), d2.getDialogState());
		assertEquals(d1.getTerminationCause(), d2.getTerminationCause());
		assertEquals(d1.getSequenceNumber(), d2.getSequenceNumber());
		assertEquals(d1.getLastUsedTime(), d2.getLastUsedTime());
		assertEquals(d1.getLocalParty(), d2.getLocalParty());
		assertEquals(d1.getRemoteParty(), d2.getRemoteParty());
		assertEquals(d1.getLocalTag(), d2.getLocalTag());
		assertEquals(d1.getRemoteTag(), d2.getRemoteTag());
		assertNull(d1.getRemoteContact());
		assertEquals(d1.getRemoteOfferMediaDescription().toString(), d2.getRemoteOfferMediaDescription().toString());
		assertEquals(d1.getSessionDescription().toString(), d2.getSessionDescription().toString());
		assertEquals(d1.getPendingReinvite().getAutoTerminate(), d2.getPendingReinvite().getAutoTerminate());
		assertEquals(d1.getPendingReinvite().getMediaDescription().toString(), d2.getPendingReinvite().getMediaDescription().toString());
		assertEquals(d1.getPendingReinvite().getApplicationData().toString(), d2.getPendingReinvite().getApplicationData());
		assertEquals(d1.isSdpInInitialInvite(), d2.isSdpInInitialInvite());
		assertEquals(d1.getCallAnswerTimeout(), d2.getCallAnswerTimeout());
		assertEquals(d1.isAutoTerminate(), d2.isAutoTerminate());
		assertEquals(d1.getTerminationMethod(), d2.getTerminationMethod());
		assertEquals(d1.getVersionId(), d2.getVersionId());
		assertEquals(d1.getInitialInviteTransactionSequenceNumber(), d2.getInitialInviteTransactionSequenceNumber());
		assertEquals(d1.getReinviteInProgess(), d2.getReinviteInProgess());
		assertEquals(d1.getLastAckRequest().toString(), d2.getLastAckRequest().toString());
		assertEquals(d1.isAutomaton(), d2.isAutomaton());
		
		assertEquals(d1.getInviteClientTransaction(), d2.getInviteClientTransaction());
		assertEquals(d1.getInviteServerTransaction(), d2.getInviteServerTransaction());
		
		assertEquals(1, d2.getRouteList().size());
		assertEquals("sip:two@domain.com;name=dick", ((Route)d2.getRouteList().get(0)).getAddress().getURI().toString());
		assertEquals("lr", ((Route)d2.getRouteList().get(0)).getParameters().getNames().next());
		assertEquals(d1.getDynamicMediaPayloadTypeMap().get("abc"), d2.getDynamicMediaPayloadTypeMap().get("abc"));
		assertEquals(d1.isAutomaticallyPlaceOnHold(), d2.isAutomaticallyPlaceOnHold());
	}
	
	// test that when object is cloned, we deep copy all non-string, non-value members
	@Test
	public void testDeepCopyOnClone() throws Exception {
		// setup
		DialogInfo d1 = new DialogInfo("id", "me", "1.2.3.4");
		d1.setSessionDescription(SessionDescriptionHelper.createSessionDescription("whatever", "whatever"));
		d1.setRemoteOfferMediaDescription(SessionDescriptionHelper.generateHoldMediaDescription());

		RouteList routeList = new RouteList();
		Route route = new Route();
		route.setAddress(SipFactory.getInstance().createAddressFactory().createAddress("sip:two@domain.com;name=dick"));
		route.setParameter("lr", null);
		routeList.add(route);
		d1.setRouteList(routeList);
		
		HashMap<String, String> dynamicPayloadMap = new HashMap<String, String>();
		d1.setDynamicMediaPayloadTypeMap(dynamicPayloadMap);
		
		PendingReinvite pendingReinvite = new PendingReinvite(SessionDescriptionHelper.generateHoldMediaDescription(), Boolean.valueOf(true), "moose");
		d1.setPendingReinvite(pendingReinvite);
		
		// act
		DialogInfo d2 = d1.cloneObject();

		// assert
		assertTrue(d1 != d2);
		assertTrue(d1.getSessionDescription() != d2.getSessionDescription());
		assertTrue(d1.getRemoteOfferMediaDescription() != d2.getRemoteOfferMediaDescription());
		assertTrue(d1.getRouteList() != d2.getRouteList());
		assertTrue(d1.getRouteList().get(0) != d2.getRouteList().get(0));
		assertTrue(d1.getDynamicMediaPayloadTypeMap() != d2.getDynamicMediaPayloadTypeMap());
		assertTrue(d1.getPendingReinvite() != d2.getPendingReinvite());
	}
	
	@Test
	public void testCloneProperties() throws Exception {
		// setup
		setupForInboundConstructor();
		ServerTransaction serverTransaction = EasyMock.createMock(ServerTransaction.class);
		StateInfoBase<DialogInfo> d1 = new DialogInfo("a", "1.2.3.4", request, serverTransaction, "localTag", props);
		
		// act
		DialogInfo d2 = d1.cloneObject();
		
		// assert
		assertEquals(1, d2.getIntProperty("int", 0));
		assertEquals(1L, d2.getLongProperty("long", 0L));
		assertEquals("1", d2.getStringProperty("string", "0"));		
		assertEquals(serverTransaction, d2.getInviteServerTransaction());
	}
	
	// test that the constructor for inbound dialogs sets sequence number to 0
	@Test
	public void testInboundDialogInfoConstructorSetsSequenceNumberToZero() throws Exception {
		// setup
		setupForInboundConstructor();
		
		// act
		ReadOnlyDialogInfo d1 = new DialogInfo("a", "1.2.3.4", request, null, "localTag", props);
		
		// assert
		assertEquals(0, d1.getSequenceNumber());
	}
	
	private void setupForInboundConstructor() throws Exception {
		props = new Properties();
		props.put("int", "1");
		props.put("long", "1");
		props.put("string", "1");
		
		CallIdHeader callIdHeader = EasyMock.createMock(CallIdHeader.class);
		EasyMock.expect(callIdHeader.getCallId()).andReturn("1");
		Address address = new AddressFactoryImpl().createAddress("sip:me");
		ToHeader toHeader = EasyMock.createMock(ToHeader.class);
		EasyMock.expect(toHeader.getAddress()).andReturn(address);
		FromHeader fromHeader = EasyMock.createMock(FromHeader.class);
		EasyMock.expect(fromHeader.getAddress()).andReturn(address);
		EasyMock.expect(fromHeader.getTag()).andReturn("1");
		ContactHeader contactHeader = EasyMock.createMock(ContactHeader.class);
		EasyMock.expect(contactHeader.getAddress()).andReturn(address);
		CSeqHeader cseqHeader = EasyMock.createMock(CSeqHeader.class);
		EasyMock.expect(cseqHeader.getSeqNumber()).andReturn((long)100);
		
		request = EasyMock.createMock(Request.class);
		EasyMock.expect(request.getMethod()).andStubReturn("INVITE");
		EasyMock.expect(request.getHeader(CallIdHeader.NAME)).andReturn(callIdHeader);
		EasyMock.expect(request.getHeader(ToHeader.NAME)).andReturn(toHeader);
		EasyMock.expect(request.getHeader(FromHeader.NAME)).andStubReturn(fromHeader);
		EasyMock.expect(request.getHeader(ContactHeader.NAME)).andReturn(contactHeader);
		EasyMock.expect(request.getHeader(ContentTypeHeader.NAME)).andReturn(null);
		EasyMock.expect(request.getHeader(CSeqHeader.NAME)).andReturn(cseqHeader);
		EasyMock.expect(request.getHeaders(RecordRouteHeader.NAME)).andReturn(null);
		
		EasyMock.replay(callIdHeader);
		EasyMock.replay(toHeader);
		EasyMock.replay(fromHeader);
		EasyMock.replay(contactHeader);
		EasyMock.replay(cseqHeader);
		EasyMock.replay(request);
	}
	
	// test that we don't allow a dialoginfo to be created from a non-invite request
	@Test(expected=IllegalArgumentException.class)
	public void testExceptionOnNonInviteRequest() throws Exception {
		// setup
		Request req = EasyMock.createMock(Request.class);
		EasyMock.expect(req.getMethod()).andReturn(Request.BYE).times(2);
		EasyMock.replay(req);
		
		// act
		new DialogInfo("whatever", "1.2.3.4", req, null, "123");
	}

	@Test
	public void testTerminationCauseSetGet() throws Exception {
		// setup
		DialogInfo di = new DialogInfo("a", "b", "1.2.3.4");
		
		// act
		boolean isSet = di.setTerminationCause(TerminationCause.RemotePartyHungUp);
		
		// assert
		assertEquals(TerminationCause.RemotePartyHungUp, di.getTerminationCause());
		assertTrue(isSet);
	}
	
	@Test
	public void testTerminationCauseSetFailsAsAlreadySet() throws Exception {
		// setup
		DialogInfo di = new DialogInfo("a", "b", "1.2.3.4");
		di.setTerminationCause(TerminationCause.CallAnswerTimeout);
		
		// act
		boolean isSet = di.setTerminationCause(TerminationCause.RemotePartyHungUp);
		
		// assert
		assertEquals(TerminationCause.CallAnswerTimeout, di.getTerminationCause());
		assertFalse(isSet);
	}
	
	@Test
	public void testTerminationCauseNullOverwritesPreviousValue() throws Exception {
		// setup
		DialogInfo di = new DialogInfo("a", "b", "1.2.3.4");
		di.setTerminationCause(TerminationCause.CallAnswerTimeout);
		
		// act
		boolean isSet = di.setTerminationCause(null);
		
		// assert
		assertEquals(null, di.getTerminationCause());
		assertTrue(isSet);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testSettingReinviteInProgressFailsWhenReinviteAlreadyInProgress() throws Exception {
		// setup
		DialogInfo di = new DialogInfo("a", "b", "1.2.3.4");
		di.setReinviteInProgess(ReinviteInProgress.ReceivedReinvite);
		
		// act
		di.setReinviteInProgess(ReinviteInProgress.SendingReinviteWithoutSessionDescription);
	}
	
	@Test
	public void testSettingReinviteInProgressToNoneSucceedsWhenReinviteAlreadyInProgress() throws Exception {
		// setup
		DialogInfo di = new DialogInfo("a", "b", "1.2.3.4");
		di.setReinviteInProgess(ReinviteInProgress.ReceivedReinvite);
		
		// act
		di.setReinviteInProgess(ReinviteInProgress.None);
		
		// assert
		assertEquals(ReinviteInProgress.None, di.getReinviteInProgess());
	}
	
	@Test
	public void testLastAckGetterAndSetter() throws Exception {
		// setup
		Request ackRequest = SipFactory.getInstance().createMessageFactory().createRequest(dummyAckString);
		
		// act
		DialogInfo di = new DialogInfo("a", "b", "1.2.3.4");
		di.setLastAckRequest(ackRequest);
		
		// assert
		assertEquals(dummyAckString, di.getLastAckRequest().toString());
		assertNotNull(di.getLastAckRequest().getRawContent());
	}
	
	@Test(expected=StackException.class)
	public void testLastAckGetterBadRequestString() throws Exception {
		// setup
		Request request = EasyMock.createNiceMock(Request.class);		
		DialogInfo di = new DialogInfo("a", "b", "1.2.3.4");
		di.setLastAckRequest(request);
		
		// act
		di.getLastAckRequest();
	}
	
	@Test
	public void testLastAckGetterNullLastAck() throws Exception {
		// setup
		DialogInfo di = new DialogInfo("a", "b", "1.2.3.4");
		
		// assert
		assertNull(di.getLastAckRequest());		
	}
	
	@Test
	public void testLastAckSetterNullLastAck() throws Exception {
		// setup
		DialogInfo di = new DialogInfo("a", "b", "1.2.3.4");
		di.setLastAckRequest(null);
		
		// assert
		assertNull(di.getLastAckRequest());		
	}
	
	@Test
	public void testLastOkSequenceNumberGetterSetter() throws Exception {
		// setup
		DialogInfo di = new DialogInfo("a", "b", "1.2.3.4");
		
		// act
		di.setLastReceivedOkSequenceNumber(5);
		
		// assert
		assertEquals(5, di.getLastReceivedOkSequenceNumber());
	}
	
	@Test
	public void testLastOkSequenceNumberNewValueDoesNotOverwriteHigherValue() throws Exception {
		// setup
		DialogInfo di = new DialogInfo("a", "b", "1.2.3.4");
		di.setLastReceivedOkSequenceNumber(6);
		
		// act
		di.setLastReceivedOkSequenceNumber(5);
		
		// assert
		assertEquals(6, di.getLastReceivedOkSequenceNumber());
	}
	
	// make sure the default constructors set automatically place on hold to false
	@Test
	public void defaultConstructor1SetsAutomaticPlaceOnHoldToFalse() {
		// act
		DialogInfo di = new DialogInfo("id", "bean", "1.2.3.4.");
		
		// assert
		assertFalse("Should not have automatically place on hold set", di.isAutomaticallyPlaceOnHold());
	}

	// make sure the default constructors set automatically place on hold to false
	@Test
	public void defaultConstructor2SetsAutomaticPlaceOnHoldToFalse() throws Exception {
		// setup
		setupForInboundConstructor();

		// act
		DialogInfo di = new DialogInfo("bean", "1.2.3.4", request, EasyMock.createMock(ServerTransaction.class), "tag");
		
		// assert
		assertFalse("Should not have automatically place on hold set", di.isAutomaticallyPlaceOnHold());
	}
	

	// make sure the default constructors set automatically place on hold to false
	@Test
	public void defaultConstructor3SetsAutomaticPlaceOnHoldToFalse() throws Exception {
		// setup
		setupForInboundConstructor();

		// act
		DialogInfo di = new DialogInfo("bean", "1.2.3.4", request, EasyMock.createMock(ServerTransaction.class), "tag", props);
		
		// assert
		assertFalse("Should not have automatically place on hold set", di.isAutomaticallyPlaceOnHold());
	}
}
