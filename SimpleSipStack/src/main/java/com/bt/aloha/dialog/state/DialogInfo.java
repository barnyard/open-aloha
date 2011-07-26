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

 	

 	
 	
 
package com.bt.aloha.dialog.state;

import gov.nist.core.NameValueList;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.header.Route;
import gov.nist.javax.sip.header.RouteList;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sip.ClientTransaction;
import javax.sip.ServerTransaction;
import javax.sip.SipFactory;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.RecordRouteHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.stack.StackException;
import com.bt.aloha.state.StateInfoBase;
import com.bt.aloha.util.MediaDescriptionState;

public class DialogInfo extends StateInfoBase<DialogInfo> implements ReadOnlyDialogInfo {

	private static final String UNCHECKED = "unchecked";
	private static final String UNABLE_TO_GENERATE_ACK_REQUEST_FROM_STRING_S = "Unable to generate ACK request from string %s";
	private static final String INVITE_SERVER_TRANSACTION = "inviteServerTransaction";
    private static final String INVITE_CLIENT_TRANSACTION = "inviteClientTransaction";
    private static final String DEFAULT_SDP_SESSION_NAME = "-";
    private static final long serialVersionUID = -7959165135858061617L;
    private static final short INITIAL_OUTBOUND_INVITE_TRANSACTION_SEQUENCE_NUMBER = 1;
    private static final String ERROR_SETTING_SESSION_DESCRIPTION = "Error setting session description";

    private static Log log = LogFactory.getLog(DialogInfo.class);

    private boolean isInbound;
	private DialogState dialogState = DialogState.Created;
	private long sequenceNumber = INITIAL_OUTBOUND_INVITE_TRANSACTION_SEQUENCE_NUMBER;
	private long initialSequenceNumber;
	private String localParty;
	private String remoteParty;
	private String localTag;
	private String remoteTag;
	private String remoteContact;
	private String viaBranchId;
	private transient SessionDescription sessionDescription;
	private String sessionDescriptionString;
	private Vector<MediaDescriptionState> mediaDescriptions;
	private MediaDescriptionState remoteOfferMyMediaDescription;
	private RouteList routeList;
	private boolean sdpInInitialInvite;
	private long callAnswerTimeout;
	private boolean autoTerminate;
	private boolean autoPlaceOnHold;
	private transient ServerTransaction inviteServerTransaction;
	private transient ClientTransaction inviteClientTransaction;
	private Properties properties;
	private TerminationMethod terminationMethod;
	private String applicationData;
	private ReinviteInProgress reinviteInProgess = ReinviteInProgress.None;
	private TerminationCause terminationCause;
	private String lastAckString;
	private long lastReceivedOkSequenceNumber = -1;
	private Map<String, String> dynamicMediaPayloadTypeMap = new HashMap<String, String>();
	private boolean isAutomaton;
	private PendingReinvite pendingReinvite;
    private Integer lock = Integer.valueOf(1);
    private String username;
    private String password;

	public DialogInfo(String aDialogId, String aSimpleSipBeanId, String aHostIpAddress) {
		super(aSimpleSipBeanId);
		setId(aDialogId);
		this.isInbound = false;
		this.terminationMethod = TerminationMethod.None;
		this.autoTerminate = false;
		this.initialSequenceNumber = INITIAL_OUTBOUND_INVITE_TRANSACTION_SEQUENCE_NUMBER;
		this.sessionDescriptionString = SessionDescriptionHelper.createSessionDescription(aHostIpAddress, DEFAULT_SDP_SESSION_NAME).toString();
		this.sessionDescription = SessionDescriptionHelper.createSessionDescription(sessionDescriptionString);
		this.mediaDescriptions = new Vector<MediaDescriptionState>();
	}

	public DialogInfo(String aDialogId, String aSimpleSipBeanId, String aHostIpAddress, String fromSipUri, String toSipUri, String aLocalTag, long aCallAnswerTimeout, boolean shouldAutoTerminate, boolean shouldAutomaticallyPlaceOnHold) {
		this( aDialogId,  aSimpleSipBeanId,  aHostIpAddress,  fromSipUri,  toSipUri,  aLocalTag,  aCallAnswerTimeout,  shouldAutoTerminate,  shouldAutomaticallyPlaceOnHold, null,null);
	}
	
	public DialogInfo(String aDialogId, String aSimpleSipBeanId, String aHostIpAddress, String fromSipUri, String toSipUri, String aLocalTag, long aCallAnswerTimeout, boolean shouldAutoTerminate, boolean shouldAutomaticallyPlaceOnHold, String aUsername, String aPassword) {
		this(aDialogId, aSimpleSipBeanId, aHostIpAddress);
		setLocalParty(fromSipUri);
		setRemoteParty(toSipUri);
		this.localTag = aLocalTag;
		setCallAnswerTimeout(aCallAnswerTimeout);
		setAutoTerminate(shouldAutoTerminate);
		setAutomaticallyPlaceOnHold(shouldAutomaticallyPlaceOnHold);
		this.username = aUsername;
		this.password = aPassword;
	}

	public DialogInfo(String aSimpleSipBeanId, String aHostIpAddress, Request aRequest, ServerTransaction aInviteServerTransaction, String aLocalTag) {
		this(aSimpleSipBeanId, aHostIpAddress, aRequest, aInviteServerTransaction, aLocalTag, null);
	}

	public DialogInfo(String aDialogBeanId, String aHostIpAddress, Request request, ServerTransaction aInviteServerTransaction, String aLocalTag, Properties theProperties) {
		super(aDialogBeanId);
		log.debug(String.format("Constructor: %s, %s, %s, %s, %s, %s", aDialogBeanId, aHostIpAddress, request.getMethod(), aInviteServerTransaction, aLocalTag, theProperties));

		if(!Request.INVITE.equals(request.getMethod()))
			throw new IllegalArgumentException("A dialog can only be created from an INVITE request");

		this.dialogState = DialogState.Initiated;
		this.inviteServerTransaction = aInviteServerTransaction;
		this.localTag = aLocalTag;
		this.properties = theProperties;
		this.isInbound = true;
		this.sequenceNumber = 0;
		this.terminationMethod = TerminationMethod.None;
		this.autoTerminate = false;

		setId(((CallIdHeader)request.getHeader(CallIdHeader.NAME)).getCallId());
		localParty = ((ToHeader)request.getHeader(ToHeader.NAME)).getAddress().getURI().toString();
		remoteParty = ((FromHeader)request.getHeader(FromHeader.NAME)).getAddress().getURI().toString();
		remoteTag = ((FromHeader)request.getHeader(FromHeader.NAME)).getTag();
		remoteContact = ((ContactHeader)request.getHeader(ContactHeader.NAME)).getAddress().getURI().toString();

		this.sessionDescriptionString = SessionDescriptionHelper.createSessionDescription(aHostIpAddress, DEFAULT_SDP_SESSION_NAME).toString();
		this.mediaDescriptions = new Vector<MediaDescriptionState>();

		ContentTypeHeader contentTypeHeader = (ContentTypeHeader)request.getHeader(ContentTypeHeader.NAME);
		if(contentTypeHeader != null && "sdp".equals(contentTypeHeader.getContentSubType())) {
			 try {
				 SessionDescription sd = SdpFactory.getInstance().createSessionDescription(new String(request.getRawContent()));
				 MediaDescription offerMediaDescription = SessionDescriptionHelper.getActiveMediaDescription(sd);
				 if (offerMediaDescription != null)
					 SessionDescriptionHelper.updateDynamicMediaPayloadMappings(offerMediaDescription, dynamicMediaPayloadTypeMap);
				 else
					 log.warn(String.format("Dialog %s got a request with no media description", getId()));
				 setRemoteOfferMediaDescription(offerMediaDescription);
				 sdpInInitialInvite = true;
			 } catch(SdpException e) {
				 throw new StackException("Failed to create dialog: error parsing offered SDP", e);
			 }
		}

		CSeqHeader cseqHeader = (CSeqHeader)request.getHeader(CSeqHeader.NAME);
		this.initialSequenceNumber = cseqHeader.getSeqNumber();

		setRouteList(request.getHeaders(RecordRouteHeader.NAME), false);

		log.debug(String.format("End Constructor: %s, %s, %s", getSimpleSipBeanId(), request.getMethod(), this.properties));
	}

	protected void copyFieldsInto(DialogInfo newDialogInfo) {
		newDialogInfo.inviteServerTransaction = inviteServerTransaction;
		newDialogInfo.inviteClientTransaction = inviteClientTransaction;
	}

	public boolean isSdpInInitialInvite() {
		return sdpInInitialInvite;
	}

	public void setSdpInInitialInvite(boolean aSdpInInitialInvite) {
		this.sdpInInitialInvite = aSdpInInitialInvite;
	}

	public String getSipUserName() {
		return ((SipURI)getLocalParty().getURI()).getUser();
	}

	public long getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(long aSequenceNumber) {
		this.sequenceNumber = aSequenceNumber;
	}

	public long getInitialInviteTransactionSequenceNumber() {
		return initialSequenceNumber;
	}

	public String getLocalTag() {
		return localTag;
	}

	public void setProperties(Properties props) {
		this.properties = props;
	}

	public String getSipCallId() {
		return getId();
	}

	public Address getLocalParty() {
		return stringToAddress(localParty);
	}

	public void setLocalParty(String aLocalParty) {
		this.localParty = aLocalParty;
	}

	public Address getRemoteContact() {
		return stringToAddress(remoteContact);
	}

	public void setRemoteContact(String aRemoteContact) {
		this.remoteContact = aRemoteContact;
	}

	public Address getRemoteParty() {
		return stringToAddress(remoteParty);
	}

	public void setRemoteParty(String aRemoteParty) {
		this.remoteParty = aRemoteParty;
	}

	public String getRemoteTag() {
		return remoteTag;
	}

	public void setRemoteTag(String aRemoteTag) {
		this.remoteTag = aRemoteTag;
	}

	public RouteList getRouteList() {
		return routeList;
	}

	public void setRouteList(RouteList aRouteList) {
		this.routeList = aRouteList;
	}

	public void setRouteList(ListIterator<?> recordRouteHeaderIterator, boolean backwards) {
		routeList = new RouteList();
		if(recordRouteHeaderIterator == null)
			return;

		if(backwards)
			while(recordRouteHeaderIterator.hasNext())
				recordRouteHeaderIterator.next();

		while (backwards ? recordRouteHeaderIterator.hasPrevious() : recordRouteHeaderIterator.hasNext()) {
			RecordRouteHeader recordRouteHeader = backwards
				? (RecordRouteHeader)recordRouteHeaderIterator.previous()
				: (RecordRouteHeader)recordRouteHeaderIterator.next();
			Route route = new Route();
			AddressImpl address = (AddressImpl)((AddressImpl)recordRouteHeader.getAddress()).clone();
			route.setAddress(address);
			NameValueList nameValueList = new NameValueList();
			Iterator<?> paramIterator = recordRouteHeader.getParameterNames();
			while(paramIterator.hasNext()) {
				String paramName = (String)paramIterator.next();
				nameValueList.set(paramName, recordRouteHeader.getParameter(paramName));
			}
			route.setParameters(nameValueList);
			log.debug(String.format("Added route address %s, params %s to route set for dialog %s", address, nameValueList.toString(), getId()));
			routeList.add(route);
		}
	}

	public SessionDescription getSessionDescription() {
		try {
			if (sessionDescription == null) {
				sessionDescription = SdpFactory.getInstance().createSessionDescription(sessionDescriptionString);
				log.debug(String.format("Getting session description: %s", sessionDescriptionString));
				Vector<MediaDescription> media = new Vector<MediaDescription>();
				if (mediaDescriptions != null)
					for (MediaDescriptionState m : mediaDescriptions) {
						media.add(m.getMediaDescription());
					}
				sessionDescription.setMediaDescriptions(media);
			}
			return sessionDescription;
		} catch (SdpParseException e) {
			throw new StackException(ERROR_SETTING_SESSION_DESCRIPTION, e);
		} catch (SdpException e) {
			throw new StackException(ERROR_SETTING_SESSION_DESCRIPTION, e);
		}
	}

	public void setSessionDescription(SessionDescription aSessionDescription) {
		sessionDescriptionString = aSessionDescription.toString();
		this.sessionDescription = SessionDescriptionHelper.createSessionDescription(sessionDescriptionString);
		try {
			mediaDescriptions.clear();
			for (Object m : aSessionDescription.getMediaDescriptions(true))
				if (m instanceof MediaDescription) {
					log.debug(String.format("Setting media description: %s", m.toString()));
					MediaDescriptionState mediaDescriptionState = new MediaDescriptionState((MediaDescription)m);
					if (mediaDescriptionState != null)
						mediaDescriptions.add(mediaDescriptionState);
				}
		} catch (SdpException e) {
			throw new StackException(ERROR_SETTING_SESSION_DESCRIPTION, e);
		}
	}

	public ServerTransaction getInviteServerTransaction() {
		return inviteServerTransaction;
	}

	public void setInviteServerTransaction(ServerTransaction anInviteServerTransaction) {
		this.inviteServerTransaction = anInviteServerTransaction;
	}

	// TODO: MEDIUM Refactor this class to separate inbound and outbound stuff
	public ClientTransaction getInviteClientTransaction() {
		return inviteClientTransaction;
	}

	public void setInviteClientTransaction(ClientTransaction newInviteClientTransaction) {
		this.inviteClientTransaction = newInviteClientTransaction;
		if (newInviteClientTransaction != null)
			viaBranchId = newInviteClientTransaction.getBranchId();
	}

	public String getStringProperty(String key, String defaultValue) {
		return (properties == null || properties.getProperty(key) == null)
			? defaultValue : properties.getProperty(key);
	}

	public int getIntProperty(String key, int defaultValue) {
		return (properties == null || properties.getProperty(key) == null)
			? defaultValue : Integer.parseInt(properties.getProperty(key));
	}

	public long getLongProperty(String key, long defaultValue) {
		return (properties == null || properties.getProperty(key) == null)
		? defaultValue : Long.parseLong(properties.getProperty(key));
	}

	private Address stringToAddress(String s) {
		if (s == null)
			return null;
		try {
			AddressFactory addressFactory = SipFactory.getInstance().createAddressFactory();
			return addressFactory.createAddress(s);
		} catch(Throwable t) {
			throw new IllegalArgumentException("Failed to convert string " + s + " to an Address",t);
		}
	}

	public DialogState getDialogState() {
		return dialogState;
	}

	/**
	 *
	 * @param newDialogState
	 * @return Previous call state if the update resulted in the dialog state being advanced,
	 *  null if it did not
	 */
	public DialogState setDialogState(DialogState newDialogState) {
		DialogState currentDialogState = dialogState;
		if(newDialogState.ordinal() > currentDialogState.ordinal()) {
			this.dialogState = newDialogState;
			if(newDialogState == DialogState.Terminated)
				setEndTime(Calendar.getInstance().getTimeInMillis());
			return currentDialogState;
		}

		log.debug(String.format("Attempt to set dialog state to same or previous or current (%s to %s) for dialog %s", currentDialogState, newDialogState, getId()));
		return null;
	}

	public TerminationCause getTerminationCause() {
		return terminationCause;
	}

	/**
	 *
	 * @param newTerminationCause
	 * @return True if the update resulted in the terminatino cause being set,
	 *  false if it was already set & hence failed
	 */
	public boolean setTerminationCause(TerminationCause newTerminationCause) {
		TerminationCause currentTerminationCause = terminationCause;
		if(newTerminationCause == null || currentTerminationCause == null) {
			this.terminationCause = newTerminationCause;
			return true;
		}
		log.debug(String.format("Attempt to set termination cause to %s for dialog %s failed as it was already set to %s", newTerminationCause, getId(), currentTerminationCause));
		return false;
	}

	/* (non-Javadoc)
	 * @see com.bt.aloha.dialog.ImmutableDialogInfo#isInbound()
	 */
	public boolean isInbound() {
		return this.isInbound;
	}

	public void setCallAnswerTimeout(long anAnswerTimeout) {
		this.callAnswerTimeout = anAnswerTimeout;
	}

	public long getCallAnswerTimeout() {
		return this.callAnswerTimeout;
	}

	public TerminationMethod getTerminationMethod() {
		return terminationMethod;
	}

	public boolean isAutoTerminate() {
		return autoTerminate;
	}

	public void setAutoTerminate(boolean shouldAutoTerminate) {
		this.autoTerminate = shouldAutoTerminate;
	}

	public TerminationMethod setTerminationMethod(TerminationMethod aTerminationMethod) {
		if(aTerminationMethod == null)
			throw new IllegalArgumentException("Termination method must not be null");
		TerminationMethod currentTerminationMethod = this.terminationMethod;
		if(TerminationMethod.None.equals(aTerminationMethod) ||
				aTerminationMethod.ordinal() > currentTerminationMethod.ordinal()) {
			this.terminationMethod = aTerminationMethod;
			return currentTerminationMethod;
		}
		log.debug(String.format("Attempt to set termination method to same or previous or current (%s to %s) for dialog %s", currentTerminationMethod, aTerminationMethod, getId()));
		return null;
	}

	public ReinviteInProgress getReinviteInProgess() {
		return reinviteInProgess;
	}

	public void setReinviteInProgess(ReinviteInProgress aReinviteInProgess) {
		if (!ReinviteInProgress.None.equals(aReinviteInProgess) && this.reinviteInProgess.ordinal() > ReinviteInProgress.None.ordinal())
			throw new IllegalStateException("Cannot initialise a reinvite in progress as one is already ongoing");
		this.reinviteInProgess = aReinviteInProgess;
	}

	public String getApplicationData() {
		return applicationData;
	}

	public void setApplicationData(String theApplicationData) {
		this.applicationData = theApplicationData;
	}
	
	@SuppressWarnings(UNCHECKED)
	@Override
	public DialogInfo cloneObject() {
		synchronized (lock) {
    		DialogInfo newDialogInfo = (DialogInfo)super.cloneObject();
    		try {
    			if (sessionDescription != null)
    				newDialogInfo.setSessionDescription(SdpFactory.getInstance().createSessionDescription(this.sessionDescription));
    			else
    				newDialogInfo.setSessionDescription(getSessionDescription());
    		} catch (SdpException e) {
    			throw new StackException("Failed to clone session description for dialog", e);
    		}
    		newDialogInfo.setRemoteOfferMediaDescription(SessionDescriptionHelper.cloneMediaDescription(this.getRemoteOfferMediaDescription()));
    		newDialogInfo.setDynamicMediaPayloadTypeMap((HashMap<String, String>)((HashMap<String, String>)dynamicMediaPayloadTypeMap).clone());
    		if (routeList != null)
    			newDialogInfo.setRouteList((RouteList)routeList.clone());
    		if (pendingReinvite != null)
    			newDialogInfo.setPendingReinvite(pendingReinvite.clone());
    		copyFieldsInto(newDialogInfo);
    		return newDialogInfo;
		}
	}

    @Override
    public boolean isDead() {
        return this.dialogState.equals(DialogState.Terminated);
    }

	public MediaDescription getRemoteOfferMediaDescription() {
        return remoteOfferMyMediaDescription == null? null : remoteOfferMyMediaDescription.getMediaDescription();
	}

	public void setRemoteOfferMediaDescription(MediaDescription aOfferedMediaDescription) {
		remoteOfferMyMediaDescription = new MediaDescriptionState(aOfferedMediaDescription);
	}

	public PendingReinvite getPendingReinvite() {
		return this.pendingReinvite;
	}

	public void setPendingReinvite(PendingReinvite aPendingReinvite) {
		this.pendingReinvite = aPendingReinvite;
	}

    @Override
    public Map<String, Object> getTransients() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(INVITE_CLIENT_TRANSACTION, this.getInviteClientTransaction());
        result.put(INVITE_SERVER_TRANSACTION, this.getInviteServerTransaction());
        return result;
    }

    @Override
    public void setTransients(Map<String, Object> m) {
        if (m.containsKey(INVITE_CLIENT_TRANSACTION)) {
            this.setInviteClientTransaction((ClientTransaction)m.get(INVITE_CLIENT_TRANSACTION));
        }
        if (m.containsKey(INVITE_SERVER_TRANSACTION)) {
            this.inviteServerTransaction = (ServerTransaction)m.get(INVITE_SERVER_TRANSACTION);
        }
    }

    public Request getLastAckRequest() {
    	if (lastAckString == null)
    		return null;
    	try {
			return SipFactory.getInstance().createMessageFactory().createRequest(lastAckString);
		} catch (Throwable t) {
			throw new StackException(String.format(UNABLE_TO_GENERATE_ACK_REQUEST_FROM_STRING_S, lastAckString), t);
		}
    }

	public void setLastAckRequest(Request ackRequest) {
		if (ackRequest == null)
			this.lastAckString = null;
		else
			this.lastAckString = ackRequest.toString();
	}

	public void setLastReceivedOkSequenceNumber(long aLastReceivedOkSequenceNumber) {
		if (this.lastReceivedOkSequenceNumber < aLastReceivedOkSequenceNumber)
			this.lastReceivedOkSequenceNumber = aLastReceivedOkSequenceNumber;
	}

	public long getLastReceivedOkSequenceNumber() {
		return lastReceivedOkSequenceNumber;
	}

	public Map<String, String> getDynamicMediaPayloadTypeMap() {
		return dynamicMediaPayloadTypeMap;
	}

	public void setDynamicMediaPayloadTypeMap(Map<String, String> aDynamicMediaPayloadTypeMap) {
		this.dynamicMediaPayloadTypeMap = aDynamicMediaPayloadTypeMap;
	}

	public boolean isAutomaticallyPlaceOnHold() {
		return this.autoPlaceOnHold;
	}

	public void setAutomaticallyPlaceOnHold(boolean shouldPlaceOnHold) {
		this.autoPlaceOnHold = shouldPlaceOnHold;
	}

	public boolean isAutomaton() {
		return this.isAutomaton;
	}

	public void setAutomaton(boolean aIsAutomaton) {
		this.isAutomaton = aIsAutomaton;
	}

	public String getViaBranchId() {
		return viaBranchId;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public boolean isMediaDialog() {
		return false;
	}

	public void setInitialInviteTransactionSequenceNumber(long sequenceNumber2) {
		this.initialSequenceNumber=sequenceNumber2;
	}
}
