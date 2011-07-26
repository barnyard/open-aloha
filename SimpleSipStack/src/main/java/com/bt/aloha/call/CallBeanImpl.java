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

 	

 	
 	
 
package com.bt.aloha.call;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.sdp.MediaDescription;
import javax.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.call.event.AbstractCallEndedEvent;
import com.bt.aloha.call.event.AbstractCallEvent;
import com.bt.aloha.call.event.CallConnectedEvent;
import com.bt.aloha.call.event.CallConnectionFailedEvent;
import com.bt.aloha.call.event.CallDisconnectedEvent;
import com.bt.aloha.call.event.CallTerminatedEvent;
import com.bt.aloha.call.event.CallTerminationFailedEvent;
import com.bt.aloha.call.state.AutomataInboundCallInfo;
import com.bt.aloha.call.state.AutomataThirdPartyCallInfo;
import com.bt.aloha.call.state.CallInfo;
import com.bt.aloha.call.state.CallLegCausingTermination;
import com.bt.aloha.call.state.CallLegConnectionState;
import com.bt.aloha.call.state.CallLegTerminationCauseToCallTerminationCauseMapper;
import com.bt.aloha.call.state.CallMessageFlow;
import com.bt.aloha.call.state.CallState;
import com.bt.aloha.call.state.CallTerminationCause;
import com.bt.aloha.call.state.ImmutableCallInfo;
import com.bt.aloha.call.state.InboundCallInfo;
import com.bt.aloha.call.state.MediaNegotiationCommand;
import com.bt.aloha.call.state.MediaNegotiationState;
import com.bt.aloha.call.state.PendingCallReinvite;
import com.bt.aloha.call.state.ReadOnlyCallInfo;
import com.bt.aloha.call.state.ThirdPartyCallInfo;
import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.callleg.InboundCallLegBean;
import com.bt.aloha.callleg.InboundCallLegListener;
import com.bt.aloha.callleg.OutboundCallLegBean;
import com.bt.aloha.callleg.OutboundCallLegListener;
import com.bt.aloha.callleg.event.CallLegAlertingEvent;
import com.bt.aloha.callleg.event.CallLegConnectedEvent;
import com.bt.aloha.callleg.event.CallLegConnectionFailedEvent;
import com.bt.aloha.callleg.event.CallLegDisconnectedEvent;
import com.bt.aloha.callleg.event.CallLegRefreshCompletedEvent;
import com.bt.aloha.callleg.event.CallLegTerminatedEvent;
import com.bt.aloha.callleg.event.CallLegTerminationFailedEvent;
import com.bt.aloha.callleg.event.IncomingCallLegEvent;
import com.bt.aloha.callleg.event.ReceivedCallLegRefreshEvent;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.state.DialogState;
import com.bt.aloha.dialog.state.ImmutableDialogInfo;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.dialog.state.TerminationCause;
import com.bt.aloha.dialog.state.TerminationMethod;
import com.bt.aloha.eventing.EventFilter;
import com.bt.aloha.stack.SessionDescriptionHelper;
import com.bt.aloha.stack.SimpleSipBeanBase;
import com.bt.aloha.stack.StackException;
import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateManager;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;
import com.bt.aloha.util.HousekeeperAware;
import com.bt.aloha.util.MessageDigestHelper;

public class CallBeanImpl extends SimpleSipBeanBase implements CallBean, OutboundCallLegListener, EventFilter, InboundCallLegListener, HousekeeperAware {
    private static final String STATE_NOT_FOUND_FOR_CALL_LEG_S_WHILE_TERMINATING_CALL_S_WILL_NOT_TRY_TO_TERMINATE_THIS_CALL_LEG = "State not found for call leg %s while terminating call %s - will not try to terminate this call leg";
	private static final String CANNOT_ADD_A_NULL_LISTENER = "Cannot add a null listener";
	private static final String CALL_ID_URI_SCHEME = "call";
	private DialogCollection dialogCollection;
	private CallCollection callCollection;
	private OutboundCallLegBean outboundCallLegBean;
	private InboundCallLegBean inboundCallLegBean;
	private MaxCallDurationScheduler maxCallDurationScheduler;
	private List<CallListener> callListeners = new ArrayList<CallListener>();
    private ConcurrentUpdateManager concurrentUpdateManager = new ConcurrentUpdateManagerImpl();
    private CallLegTerminationCauseToCallTerminationCauseMapper callLegTerminationCauseToCallTerminationCauseMapper = new CallLegTerminationCauseToCallTerminationCauseMapper();
    private Log log = LogFactory.getLog(this.getClass());

	public CallBeanImpl() {
		super();
	}

	public void setCallListeners(List<CallListener> callListenerList) {
		this.callListeners = callListenerList;
	}

	public void addCallListener(CallListener aCallListener){
		if (aCallListener == null)
			throw new IllegalArgumentException(CANNOT_ADD_A_NULL_LISTENER);
	    this.callListeners.add(aCallListener);
    }

	public void removeCallListener(CallListener listener) {
		if (listener == null)
			throw new IllegalArgumentException(CANNOT_ADD_A_NULL_LISTENER);
		this.callListeners.remove(listener);
	}

	public void setDialogCollection(DialogCollection aDialogCollection) {
		this.dialogCollection = aDialogCollection;
	}

	public DialogCollection getDialogCollection() {
		return this.dialogCollection;
	}

	public void setCallCollection(CallCollection aCallCollection) {
		this.callCollection = aCallCollection;
	}

	public void setOutboundCallLegBean(OutboundCallLegBean anOutboundDialogBean) {
		if (this.outboundCallLegBean != null)
			this.outboundCallLegBean.removeOutboundCallLegListener(this);
		this.outboundCallLegBean = anOutboundDialogBean;
		this.outboundCallLegBean.addOutboundCallLegListener(this);
	}

	public void setInboundCallLegBean(InboundCallLegBean anInboundDialogBean) {
		if (this.inboundCallLegBean != null)
			this.inboundCallLegBean.removeInboundCallLegListener(this);
		this.inboundCallLegBean = anInboundDialogBean;
		this.inboundCallLegBean.addInboundCallLegListener(this);
	}

	protected ConcurrentUpdateManager getConcurrentUpdateManager() {
		return this.concurrentUpdateManager;
	}

	public void setConcurrentUpdateManager(ConcurrentUpdateManager aConcurrentUpdateManager) {
		this.concurrentUpdateManager = aConcurrentUpdateManager;
	}

	public String joinCallLegs(String firstDialogId, String secondDialogId) {
		return joinCallLegs(firstDialogId, secondDialogId, AutoTerminateAction.Unchanged);
	}

	public String joinCallLegs(String firstDialogId, String secondDialogId, AutoTerminateAction autoTerminateDialogs) {
		return joinCallLegs(firstDialogId, secondDialogId, autoTerminateDialogs, -1);
	}

	public String generateCallId() {
		return String.format("%s:%s%s", CALL_ID_URI_SCHEME, MessageDigestHelper.generateDigest(), Double.toString(Math.random()));
	}

	public String joinCallLegs(String firstDialogId, String secondDialogId, final AutoTerminateAction autoTerminateDialogs, long durationInMinutes) {
		String callId = generateCallId();
		joinDialogs(callId, firstDialogId, secondDialogId, autoTerminateDialogs, durationInMinutes);
		return callId;
	}

	public void joinDialogs(String callId, String firstDialogId, String secondDialogId) {
		joinDialogs(callId, firstDialogId, secondDialogId, AutoTerminateAction.Unchanged, -1);
	}

	protected void joinDialogs(String callId, String firstDialogId, String secondDialogId, final AutoTerminateAction autoTerminate, long durationInMinutes) {
		log.debug(String.format("Joining dialogs %s and %s to create call %s", firstDialogId, secondDialogId, callId));
		final ReadOnlyDialogInfo firstDialogInfo = getDialogCollection().get(firstDialogId);
		final ReadOnlyDialogInfo secondDialogInfo = getDialogCollection().get(secondDialogId);

        if (null == firstDialogInfo || null == secondDialogInfo) {
            String message = String.format("Unknown dialog: %s", null == firstDialogInfo ? firstDialogId : secondDialogId);
            log.warn(message);
            throw new IllegalArgumentException(message);
        }

        if (firstDialogInfo.isInbound() && secondDialogInfo.isInbound())
        	throw new IllegalArgumentException(String.format("Dialogs %s and %s are both Inbound - cannot join two inbound dialogs in a call", firstDialogId, secondDialogId));

        if (firstDialogInfo.getDialogState().equals(DialogState.Terminated)
        		|| secondDialogInfo.getDialogState().equals(DialogState.Terminated)) {
            String message = String.format("Cannot connect a Terminated dialog in a call (%s, %s)", firstDialogId, secondDialogId);
            log.warn(message);
            throw new IllegalStateException(message);
        } else if (firstDialogInfo.getTerminationMethod().equals(TerminationMethod.Terminate)
        		|| secondDialogInfo.getTerminationMethod().equals(TerminationMethod.Terminate)) {
            String message = String.format("One or both dialogs being joined are being terminated (%s, %s)", firstDialogId, secondDialogId);
            log.warn(message);
            throw new IllegalStateException(message);
        }

		final CallInfo callInfo = createCallInfo(getBeanName(), callId, firstDialogInfo, secondDialogInfo, autoTerminate, durationInMinutes);
		callInfo.setCallLegConnectionState(firstDialogId, mapDialogInfoStateToCallConnectionState(firstDialogInfo));
		callInfo.setCallLegConnectionState(secondDialogId, mapDialogInfoStateToCallConnectionState(secondDialogInfo));

		MediaDescription offerMediaDescription = null;
		if (firstDialogInfo.isInbound())
			offerMediaDescription = getInitialOfferMediaDescription(firstDialogInfo);
		else if (secondDialogInfo.isInbound())
			offerMediaDescription = getInitialOfferMediaDescription(secondDialogInfo);

		CallMessageFlow callMessageFlow = getCallMessageFlow(callInfo);
		MediaNegotiationCommand command = callMessageFlow.initializeCall(firstDialogInfo, secondDialogInfo, callInfo, offerMediaDescription);
		if (command != null)
			command.updateCallInfo(callInfo);

        callCollection.add(callInfo);
        log.info(String.format("Created call %s (%s) between dialogs %s and %s", callId, callInfo.getClass().getSimpleName(), firstDialogId, secondDialogId));

        terminateExistingCallsForDialog(firstDialogId, callId);
        terminateExistingCallsForDialog(secondDialogId, callId);

        executeMediaNegotiationCommand(command);
	}

	private MediaDescription getInitialOfferMediaDescription(ReadOnlyDialogInfo dialogInfo) {
		if (dialogInfo.isInbound() && dialogInfo.getDialogState().equals(DialogState.Initiated) && dialogInfo.isSdpInInitialInvite())
			return dialogInfo.getRemoteOfferMediaDescription();
		else
			return null;
	}

	private CallInfo createCallInfo(String beanName, String callId, ReadOnlyDialogInfo firstDialogInfo, ReadOnlyDialogInfo secondDialogInfo, AutoTerminateAction autoTerminate, long durationInMinutes) {
		if (secondDialogInfo.isAutomaton())
			if (firstDialogInfo.isInbound() || secondDialogInfo.isInbound()) {
				return new AutomataInboundCallInfo(getBeanName(), callId, firstDialogInfo.getId(), secondDialogInfo.getId(), autoTerminate, durationInMinutes);
			} else {
				return new AutomataThirdPartyCallInfo(getBeanName(), callId, firstDialogInfo.getId(), secondDialogInfo.getId(), autoTerminate, durationInMinutes);
			}
		else if (firstDialogInfo.isInbound())
			return new InboundCallInfo(getBeanName(), callId, firstDialogInfo.getId(), secondDialogInfo.getId(), autoTerminate, durationInMinutes);
		else if (secondDialogInfo.isInbound())
			return new InboundCallInfo(getBeanName(), callId, secondDialogInfo.getId(), firstDialogInfo.getId(), autoTerminate, durationInMinutes);
		else if (DialogState.Created.equals(firstDialogInfo.getDialogState()) && DialogState.Created.equals(secondDialogInfo.getDialogState()))
			return new ThirdPartyCallInfo(getBeanName(), callId, firstDialogInfo.getId(), secondDialogInfo.getId(), autoTerminate, durationInMinutes);
		else
			return new CallInfo(getBeanName(), callId, firstDialogInfo.getId(), secondDialogInfo.getId(), autoTerminate, durationInMinutes);
	}

	private void terminateExistingCallsForDialog(final String dialogId, final String callIdToIgnore) {
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			private String resourceId;

			public void execute() {
				CallInfo currentCallInfo = callCollection.getCurrentCallForCallLeg(dialogId, callIdToIgnore);
				if (currentCallInfo == null) {
					log.debug(String.format("Call leg termination for dialog %s, dialog not in a call", dialogId));
					return;
				}

				resourceId = currentCallInfo.getId();
				log.info(String.format("Dialog %s used to create a new call, but already in call %s - terminating that call", dialogId, currentCallInfo.getId()));
				if (currentCallInfo.setCallState(CallState.Terminated) != null) {
					currentCallInfo.setCallTerminationCause(CallTerminationCause.CallLegDetached,
							(currentCallInfo.getFirstDialogId().equals(dialogId)) ? CallLegCausingTermination.First : CallLegCausingTermination.Second);
					callCollection.replace(currentCallInfo);
			    	tearDownCallForDialog(dialogId, currentCallInfo, new CallTerminatedEvent(currentCallInfo.getId(), currentCallInfo.getCallTerminationCause(), currentCallInfo.getCallLegCausingTermination(), currentCallInfo.getDuration()));
				}
			}
			public String getResourceId() {
				return resourceId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	public void onCallLegAlerting(CallLegAlertingEvent alertingEvent) {
		String callLegId = alertingEvent.getId();
		log.info(String.format("Got alerting event for %s", callLegId));

        ImmutableCallInfo callInfo = callCollection.getCurrentCallForCallLeg(callLegId);
		if (callInfo == null) {
			log.debug(String.format("Doing nothing with alerting event for dialog %s  - dialog not in a call", callLegId));
			return;
		}

		final ImmutableDialogInfo firstDialogInfo = getDialogCollection().get(callInfo.getFirstDialogId());
		final ImmutableDialogInfo secondDialogInfo = getDialogCollection().get(callInfo.getSecondDialogId());

		if(callLegId.equals(firstDialogInfo.getId())
				&& secondDialogInfo.isInbound()) {
			inboundCallLegBean.sendRingingResponse(callInfo.getSecondDialogId());
		} else if(callLegId.equals(secondDialogInfo.getId())
				&& firstDialogInfo.isInbound()) {
			inboundCallLegBean.sendRingingResponse(callInfo.getFirstDialogId());
		} else {
			log.debug(String.format("Ignoring alerting event for %s", callLegId));
		}
	}

	public void onCallLegConnected(final CallLegConnectedEvent connectedEvent) {
		log.debug(String.format("Got dialog connected event for %s", connectedEvent.getId()));

		final String eventDialogId = connectedEvent.getId();
		final MediaDescription mediaDescription = connectedEvent.getMediaDescription();
		final CallBean callBean = this;

		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			private String callId;
			private ImmutableDialogInfo eventDialogInfo;
			private ImmutableDialogInfo otherDialogInfo;

			public void execute() {
				CallInfo currentCallInfo = callCollection.getCurrentCallForCallLeg(eventDialogId);
				if (currentCallInfo == null) {
					log.debug(String.format("Call leg connected for dialog %s, dialog not in a call", eventDialogId));
					return;
				}

				boolean refreshOriginatedByCurrentCall = connectedEvent.getApplicationData() != null && connectedEvent.getApplicationData().equals(currentCallInfo.getId());

				callId = currentCallInfo.getId();
				currentCallInfo.setCallLegConnectionState(eventDialogId, CallLegConnectionState.Completed);

				log.debug(String.format("Call leg connected event: got call %s for call leg %s, first leg conn state is %s, second is %s", callId, eventDialogId, currentCallInfo.getCallLegConnectionState(currentCallInfo.getFirstDialogId()), currentCallInfo.getCallLegConnectionState(currentCallInfo.getSecondDialogId())));
				log.debug(String.format("Call media negotiation state is %s, method is %s", currentCallInfo.getMediaNegotiationState(), currentCallInfo.getMediaNegotiationMethod()));

				if (connectedEvent.getApplicationData() != null && !currentCallInfo.getId().equals(connectedEvent.getApplicationData())) {
					log.info(String.format("Call leg %s connected - WAS in call %s, but now in call %s", eventDialogId, connectedEvent.getApplicationData(), currentCallInfo.getId()));
				}

				String otherDialogId = eventDialogId.equals(currentCallInfo.getFirstDialogId()) ? currentCallInfo.getSecondDialogId() : currentCallInfo.getFirstDialogId();

				if (eventDialogInfo == null)
					eventDialogInfo = getDialogCollection().get(eventDialogId);
				if (otherDialogInfo == null || !otherDialogInfo.getId().equals(otherDialogId))
					otherDialogInfo = getDialogCollection().get(otherDialogId);

				CallMessageFlow callMessageFlow = getCallMessageFlow(currentCallInfo);
				MediaNegotiationCommand command = callMessageFlow.processCallLegConnected(eventDialogInfo, otherDialogInfo, currentCallInfo, refreshOriginatedByCurrentCall, mediaDescription);
				if (command != null)
					command.updateCallInfo(currentCallInfo);

				boolean raiseCallConnectedEvent = false;
				if (currentCallInfo.getMediaNegotiationState().equals(MediaNegotiationState.Completed) && currentCallInfo.areBothCallLegsConnected()
						&& currentCallInfo.setCallState(CallState.Connected) != null) {
				    currentCallInfo.setStartTime(Calendar.getInstance().getTimeInMillis());
					if (currentCallInfo.getMaxDurationInMinutes() > 0)
						getMaxCallDurationScheduler().terminateCallAfterMaxDuration(currentCallInfo, callBean);
					raiseCallConnectedEvent = true;
				}

				PendingCallReinvite pendingCallReinvite = currentCallInfo.getPendingCallReinvite();
				if (pendingCallReinvite != null) {
					log.info(String.format("Clearing pending call reinvite for %s", callId));
					currentCallInfo.setPendingCallReinvite(null);
				}

				callCollection.replace(currentCallInfo);

				executeMediaNegotiationCommand(command);

				if (raiseCallConnectedEvent)
					getEventDispatcher().dispatchEvent(callListeners, new CallConnectedEvent(currentCallInfo.getId()));

				if (pendingCallReinvite != null)
					onReceivedCallLegRefresh(pendingCallReinvite.getReceivedCallLegRefreshEvent());
			}

			public String getResourceId() {
				return callId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	public void onCallLegConnectionFailed(final CallLegConnectionFailedEvent connectionFailedEvent) {
		final String eventDialogId = connectionFailedEvent.getId();
		log.debug(String.format("Got connection failed event for %s", eventDialogId));

		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			private String callId;

			public void execute() {
				CallInfo callInfo = callCollection.getCurrentCallForCallLeg(eventDialogId);
				if (callInfo == null) {
					log.debug(String.format("Call leg connection failed for dialog %s, dialog not in a call", eventDialogId));
				} else  {
					String otherDialogId = eventDialogId.equals(callInfo.getFirstDialogId()) ? callInfo.getSecondDialogId() : callInfo.getFirstDialogId();
					ImmutableDialogInfo eventDialogInfo = dialogCollection.get(eventDialogId);
					ImmutableDialogInfo otherDialogInfo = dialogCollection.get(otherDialogId);

					this.callId = callInfo.getId();
					if (callInfo.setCallState(CallState.Terminated) != null) {
						CallLegCausingTermination callLegCausingTermination = eventDialogId.equals(callInfo.getFirstDialogId())
								? CallLegCausingTermination.First : CallLegCausingTermination.Second;
						CallTerminationCause cause = callLegTerminationCauseToCallTerminationCauseMapper.map(connectionFailedEvent.getTerminationCause());
						callInfo.setCallTerminationCause(cause, callLegCausingTermination);
						callCollection.replace(callInfo);

						CallMessageFlow callMessageFlow = getCallMessageFlow(callInfo);
						MediaNegotiationCommand mediaNegotiationCommand = callMessageFlow.processCallLegConnectionFailed(eventDialogInfo, otherDialogInfo, callInfo);
						executeMediaNegotiationCommand(mediaNegotiationCommand);

			            tearDownCallForDialog(eventDialogId, callInfo, new CallConnectionFailedEvent(callInfo.getId(), callInfo.getCallTerminationCause(), callInfo.getCallLegCausingTermination(), callInfo.getDuration()));
					}
				}
			}

			public String getResourceId() {
				return callId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	public void onCallLegDisconnected(final CallLegDisconnectedEvent disconnectedEvent) {
		final String eventDialogId = disconnectedEvent.getId();
		log.debug(String.format("Got disconnected event for %s", eventDialogId));

		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			private String callId;
			public void execute() {
				CallInfo callInfo = callCollection.getCurrentCallForCallLeg(eventDialogId);
				if (callInfo == null) {
					log.debug(String.format("Call leg disconnected for dialog %s, dialog not in a call", eventDialogId));
				} else  {
					this.callId = callInfo.getId();
					if (callInfo.setCallState(CallState.Terminated) != null) {
						CallLegCausingTermination callLegCausingTermination = eventDialogId.equals(callInfo.getFirstDialogId())
								? CallLegCausingTermination.First : CallLegCausingTermination.Second;
						CallTerminationCause cause = callLegTerminationCauseToCallTerminationCauseMapper.map(disconnectedEvent.getTerminationCause());
						callInfo.setCallTerminationCause(cause, callLegCausingTermination);
						callCollection.replace(callInfo);
			            tearDownCallForDialog(eventDialogId, callInfo, new CallDisconnectedEvent(callInfo.getId(), callInfo.getCallTerminationCause(), callInfo.getCallLegCausingTermination(), callInfo.getDuration()));
					}
				}
			}
			public String getResourceId() {
				return callId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	private AbstractCallEndedEvent createCallTerminationEvent(Class<?> clazz, CallInfo callInfo) {
		if (CallLegTerminatedEvent.class.equals(clazz))
			return new CallTerminatedEvent(callInfo.getId(), callInfo.getCallTerminationCause(), callInfo.getCallLegCausingTermination(), callInfo.getDuration());
		else
			return new CallTerminationFailedEvent(callInfo.getId(), callInfo.getCallTerminationCause(), callInfo.getCallLegCausingTermination(), callInfo.getDuration());
	}

	private void processCallLegTermination(final Class<?> clazz, final TerminationCause terminationCause, final String eventDialogId) {
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			private String callId;
			public void execute() {
				CallInfo callInfo = callCollection.getCurrentCallForCallLeg(eventDialogId);
				if (callInfo == null) {
					log.debug(String.format("Call leg terminated for dialog %s, dialog not in a call", eventDialogId));
				} else  {
					this.callId = callInfo.getId();
					if (callInfo.setCallState(CallState.Terminated) != null) {
						CallLegCausingTermination callLegCausingTermination = CallLegCausingTermination.Neither;
						if ( ! TerminationCause.TerminatedByServer.equals(terminationCause)) {
							callLegCausingTermination = eventDialogId.equals(callInfo.getFirstDialogId()) ? CallLegCausingTermination.First : CallLegCausingTermination.Second;
						}
						CallTerminationCause cause = callLegTerminationCauseToCallTerminationCauseMapper.map(terminationCause);
						callInfo.setCallTerminationCause(cause, callLegCausingTermination);
						callCollection.replace(callInfo);
						tearDownCallForDialog(eventDialogId, callInfo, createCallTerminationEvent(clazz, callInfo));
					}
				}
			}
			
			public String getResourceId() {
				return callId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}
	
	public void onCallLegTerminated(final CallLegTerminatedEvent terminatedEvent) {
		final String eventDialogId = terminatedEvent.getId();
		log.debug(String.format("Got terminated event for %s", eventDialogId));

		processCallLegTermination(CallLegTerminatedEvent.class, terminatedEvent.getTerminationCause(), eventDialogId);
	}

	public void onCallLegTerminationFailed(CallLegTerminationFailedEvent terminationFailedEvent) {
		log.debug(String.format("Got termination failed event for %s", terminationFailedEvent.getId()));
		processCallLegTermination(CallLegTerminationFailedEvent.class, terminationFailedEvent.getTerminationCause(), terminationFailedEvent.getId());
	}
	
	public void onCallLegRefreshCompleted(final CallLegRefreshCompletedEvent callLegRefreshCompletedEvent) {
		final String eventDialogId = callLegRefreshCompletedEvent.getId();
		final String callIdOriginatingRefresh = callLegRefreshCompletedEvent.getApplicationData();
		log.debug(String.format("Got callLegRefreshCompletedEvent for dialog %s, call %s, with negotiated media %s", eventDialogId, callIdOriginatingRefresh, callLegRefreshCompletedEvent.getMediaDescription()));
		if (callIdOriginatingRefresh == null) {
			log.debug(String.format("Call leg refresh completed event for dialog %s was NOT originated by a call - IGNORING", eventDialogId));
			return;
		}

		final CallBean callBean = this;
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			private ImmutableDialogInfo firstDialogInfo;
			private ImmutableDialogInfo secondDialogInfo;

			public String getResourceId() {
				return callIdOriginatingRefresh;
			}

			public void execute() {
				CallInfo callInfo = callCollection.get(callIdOriginatingRefresh);
				if (callInfo == null) {
					log.debug(String.format("Call %s not found in collection, ignoring callLegRefreshCompletedEvent", callIdOriginatingRefresh));
					return;
				}

				// Note that we allow refresh completed for terminated calls, in order to avoid leaving
				// dangling call legs waiting for a response. Any hold reinvites should be queued and released
				// upon refresh completion

				if (firstDialogInfo == null || !firstDialogInfo.getId().equals(callInfo.getFirstDialogId()))
					firstDialogInfo = dialogCollection.get(callInfo.getFirstDialogId());
				if (secondDialogInfo == null || !secondDialogInfo.getId().equals(callInfo.getSecondDialogId()))
					secondDialogInfo = dialogCollection.get(callInfo.getSecondDialogId());

				boolean isEventForFirstDialogInfo = eventDialogId.equals(firstDialogInfo.getId());
				if (callInfo.getMediaNegotiationState().equals(MediaNegotiationState.Completed)
						&& callInfo.areBothCallLegsConnected()) {
					if (callInfo.setCallState(CallState.Connected) != null) {
						if (callInfo.getMaxDurationInMinutes() > 0)
							getMaxCallDurationScheduler().terminateCallAfterMaxDuration(callInfo, callBean);
						callInfo.setStartTime(Calendar.getInstance().getTimeInMillis());
		                callCollection.replace(callInfo);

		                getEventDispatcher().dispatchEvent(callListeners, new CallConnectedEvent(callInfo.getId()));
					}
				} else {
					CallMessageFlow callMessageFlow = getCallMessageFlow(callInfo);
					MediaNegotiationCommand command = callMessageFlow.processCallLegRefreshCompleted(firstDialogInfo, secondDialogInfo, callInfo, isEventForFirstDialogInfo, callLegRefreshCompletedEvent.getMediaDescription());
					if (command != null)
						command.updateCallInfo(callInfo);
					callCollection.replace(callInfo);

					executeMediaNegotiationCommand(command);
				}
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	public void onReceivedCallLegRefresh(final ReceivedCallLegRefreshEvent receivedCallLegRefreshEvent) {
		final String eventDialogId = receivedCallLegRefreshEvent.getId();
		final MediaDescription offerMediaDescription = receivedCallLegRefreshEvent.getMediaDescription();
		log.debug(String.format("Received refresh event for dialog %s, with app data %s", eventDialogId, receivedCallLegRefreshEvent.getApplicationData()));

		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			private String callId;
			private ImmutableDialogInfo eventDialogInfo;

			public void execute() {
				CallInfo currentCallInfo = callCollection.getCurrentCallForCallLeg(eventDialogId);
				if (currentCallInfo == null) {
					// TODO: Should respond to the reinvite request, rather than just throwing it away!
					log.warn(String.format("Ignoring received call refresh event for dialog %s, dialog not in a call", eventDialogId));
					return;
				}

				final CallInfo callInfo;
				if (receivedCallLegRefreshEvent.getApplicationData() != null && !currentCallInfo.getId().equals(receivedCallLegRefreshEvent.getApplicationData())) {
					log.warn(String.format("Received refresh event for PREVIOUS CALL for dialog %s - currently in call %s, was in call %s", eventDialogId, currentCallInfo.getId(), receivedCallLegRefreshEvent.getApplicationData()));
					callInfo = callCollection.get(receivedCallLegRefreshEvent.getApplicationData());
				} else {
					callInfo = currentCallInfo;
				}

				callId = callInfo.getId();
				// TODO MED: If a CallLegConnectedEvent never comes through, we need some sort of timer to reject the incoming invite. Currently,
				// just assume that the event will come through (which it always should)
				if (callInfo.getCallLegConnectionState(eventDialogId).ordinal() < CallLegConnectionState.Completed.ordinal() && receivedCallLegRefreshEvent.getApplicationData() == null) {
					log.info(String.format("Adding pending call reinvite for %s", callId));
					callInfo.setPendingCallReinvite(new PendingCallReinvite(receivedCallLegRefreshEvent));
					callCollection.replace(callInfo);
					return;
				}

				if (eventDialogInfo == null)
					eventDialogInfo = dialogCollection.get(eventDialogId);

				CallMessageFlow callMessageFlow = getCallMessageFlow(callInfo);
				if (CallState.Terminated.equals(callInfo.getCallState())) {
					log.debug(String.format("Received refresh event for dialog %s, dialog not in a call", eventDialogId));
					MediaNegotiationCommand command = callMessageFlow.processReceivedCallLegRefreshForTerminatedCall(eventDialogInfo, offerMediaDescription, callInfo);
					executeMediaNegotiationCommand(command);
				} else {
					log.debug(String.format("Received refresh event for dialog %s with conn state %s, in call %s, proxying reinvite", eventDialogId, callInfo.getCallLegConnectionState(eventDialogId), callId));
					String otherDialogId = eventDialogId.equals(callInfo.getFirstDialogId()) ? callInfo.getSecondDialogId() : callInfo.getFirstDialogId();

					ImmutableDialogInfo otherDialogInfo = dialogCollection.get(otherDialogId);

					MediaNegotiationCommand command = callMessageFlow.processReceivedCallLegRefresh(eventDialogInfo, otherDialogInfo, callInfo, receivedCallLegRefreshEvent.isOfferInOkResponse(), offerMediaDescription);
					if (command != null)
						command.updateCallInfo(callInfo);

					callCollection.replace(callInfo);

					executeMediaNegotiationCommand(command);
				}
			}

			public String getResourceId() {
				return callId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	private String getOtherDialogInCall(String eventDialogId, ImmutableCallInfo callInfo) {
		String dialogIdToTerminate;
		if(callInfo.getFirstDialogId().equals(eventDialogId))
			dialogIdToTerminate = callInfo.getSecondDialogId();
		else
			dialogIdToTerminate = callInfo.getFirstDialogId();
		return dialogIdToTerminate;
	}

	public void terminateCall(String callId) {
		terminateCall(callId, CallTerminationCause.TerminatedByApplication);
	}

	public void terminateCall(final String callId, final CallTerminationCause callTerminationCause) {
		log.info(String.format("Attempting to terminate call %s", callId));
		ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
			public void execute() {
				CallInfo callInfo = callCollection.get(callId);

				if (callInfo == null)
					throw new IllegalArgumentException("No calls for call id "+callId);

				if (callInfo.getCallState() != CallState.Terminated) {
					callInfo.setCallTerminationCause(callTerminationCause, CallLegCausingTermination.Neither);
                    callCollection.replace(callInfo);
					final ImmutableDialogInfo firstDialogInfo = getDialogCollection().get(callInfo.getFirstDialogId());
					final ImmutableDialogInfo secondDialogInfo = getDialogCollection().get(callInfo.getSecondDialogId());

					if (firstDialogInfo != null)
						terminateCallLeg(firstDialogInfo, TerminationCause.TerminatedByServer);
					else
						log.debug(String.format(STATE_NOT_FOUND_FOR_CALL_LEG_S_WHILE_TERMINATING_CALL_S_WILL_NOT_TRY_TO_TERMINATE_THIS_CALL_LEG, callInfo.getFirstDialogId(), callInfo.getId()));

					if (secondDialogInfo != null)
						terminateCallLeg(secondDialogInfo, TerminationCause.TerminatedByServer);
					else
						log.debug(String.format(STATE_NOT_FOUND_FOR_CALL_LEG_S_WHILE_TERMINATING_CALL_S_WILL_NOT_TRY_TO_TERMINATE_THIS_CALL_LEG, callInfo.getSecondDialogId(), callInfo.getId()));
				} else {
					log.debug(String.format("Call %s was already Terminated", callId));
				}
			}
			public String getResourceId() {
				return callId;
			}
		};
		getConcurrentUpdateManager().executeConcurrentUpdate(concurrentUpdateBlock);
	}

	private void terminateCallLeg(final ImmutableDialogInfo dialogInfo, final TerminationCause dialogTerminationCause) {
		if (dialogInfo.isInbound())
			inboundCallLegBean.terminateCallLeg(dialogInfo.getId(), dialogTerminationCause);
		else
			outboundCallLegBean.terminateCallLeg(dialogInfo.getId(), dialogTerminationCause);
	}

	private void reinviteCallLeg(final ImmutableDialogInfo dialogInfo, MediaDescription offerMediaDescription, AutoTerminateAction autoTerminateDialog, final String callId) {
		if (dialogInfo.isInbound())
			inboundCallLegBean.reinviteCallLeg(dialogInfo.getId(), offerMediaDescription, autoTerminateDialog, callId);
		else
			outboundCallLegBean.reinviteCallLeg(dialogInfo.getId(), offerMediaDescription, autoTerminateDialog, callId);
	}

	private void tearDownCallForDialog(String eventDialogId, CallInfo callInfo, AbstractCallEvent event) {
		getEventDispatcher().dispatchEvent(callListeners, event);
		releaseOtherDialogFromCall(callInfo, eventDialogId);
		if (callInfo.getMaxDurationInMinutes() > 0) {
			this.maxCallDurationScheduler.cancelTerminateCall(callInfo);
		}
	}

	private void releaseOtherDialogFromCall(ImmutableCallInfo callInfo, String dialogId) {
		String dialogIdToTerminate = getOtherDialogInCall(dialogId, callInfo);
		log.debug(String.format("Will try to release dialog %s from call %s", dialogIdToTerminate, callInfo.getId()));
		releaseCallLegFromCall(dialogIdToTerminate, callInfo.getId());
	}

	// TODO: replace readonly dialog infos with immutable
	// TODO: we currently reject incoming call if placed in new call - we should obey auto terminate flag
	private void releaseCallLegFromCall(String dialogId, String callId) {
		ReadOnlyCallInfo currentCallInfoForDialogToTerminate = callCollection.getCurrentCallForCallLeg(dialogId);
		if (currentCallInfoForDialogToTerminate == null) {
			log.warn(String.format("Failed to release call leg %s from call %s - unexpected failure to find current call for dialog", dialogId, callId));
			return;
		}

		if (!currentCallInfoForDialogToTerminate.getId().equals(callId)) {
			log.info(String.format("NOT releasing dialog %s from call %s, dialog in another call"));
			return;
		}

		log.debug(String.format("Releasing dialog %s in call %s with dialog %s, which was either terminated or moved into a new call", dialogId, currentCallInfoForDialogToTerminate.getId(), dialogId));
		final ReadOnlyDialogInfo dialogToTerminate = getDialogCollection().get(dialogId);
		if (dialogToTerminate.isInbound() && currentCallInfoForDialogToTerminate.getCallLegConnectionState(dialogId).equals(CallLegConnectionState.Pending)) {
			log.debug(String.format("Inbound dialog %s awaiting final response, rejecting", dialogId));
			inboundCallLegBean.rejectIncomingCallLeg(dialogId, Response.TEMPORARILY_UNAVAILABLE);
			return;
		}

		if (!dialogToTerminate.isAutoTerminate()) {
			log.debug(String.format("Dialog %s does not have auto-terminate flag set, placing it on hold", dialogId));

			MediaDescription currentMediaDescription = SessionDescriptionHelper.getActiveMediaDescription(dialogToTerminate.getSessionDescription());
			MediaDescription holdMediaDescription;
			if (currentMediaDescription != null)
				holdMediaDescription = SessionDescriptionHelper.generateHoldMediaDescription(currentMediaDescription);
			else
				holdMediaDescription = SessionDescriptionHelper.generateHoldMediaDescription();
			reinviteCallLeg(dialogToTerminate, holdMediaDescription, AutoTerminateAction.Unchanged, null);
		} else {
			log.debug(String.format("Auto-terminating call leg %s", dialogId));
			try {
				terminateCallLeg(dialogToTerminate, TerminationCause.AutoTerminated);
			} catch(IllegalStateException e) {
				log.info(String.format("Tried to auto-terminate already terminated dialog %s", dialogId));
			}
		}
	}

	public MaxCallDurationScheduler getMaxCallDurationScheduler() {
		return maxCallDurationScheduler;
	}

	public void setMaxCallDurationScheduler(MaxCallDurationScheduler theMaxCallDurationScheduler) {
		this.maxCallDurationScheduler = theMaxCallDurationScheduler;
	}

	/**
	 * @return the callListeners
	 */
	public List<CallListener> getCallListeners() {
		return callListeners;
	}

	public void onIncomingCallLeg(IncomingCallLegEvent e) {
		// we don't care...should be filtered out
	}

	public boolean shouldDeliverEvent(Object event) {
		return !(event instanceof IncomingCallLegEvent);
	}

	public CallInformation getCallInformation(String callId) {
		if(callId == null)
			throw new IllegalArgumentException("Call identifier must be specified");

		ReadOnlyCallInfo callInfo = callCollection.get(callId);
		if(callInfo == null)
			throw new IllegalArgumentException(String.format("Unknown call identifier %s", callId));
		return new CallInformation(callInfo.getCallState(), callInfo.getStartTime(), callInfo.getDuration(), callInfo.getCallTerminationCause(), callInfo.getCallLegCausingTermination(), callInfo.getFirstDialogId(), callInfo.getSecondDialogId());
	}

	public void killHousekeeperCandidate(String infoId) {
		terminateCall(infoId, CallTerminationCause.Housekept);
	}

	private CallMessageFlow getCallMessageFlow(ImmutableCallInfo callInfo) {
		CallMessageFlow messageFlow;
        try {
			messageFlow = callInfo.getCallMessageFlow().newInstance();
		} catch (InstantiationException e) {
			throw new StackException("Could not instantiate call message flow class", e);
		} catch (IllegalAccessException e) {
			throw new StackException("Could not create call message flow class", e);
		}

		return messageFlow;
	}

	private void executeMediaNegotiationCommand(MediaNegotiationCommand mediaNegotiationCommand) {
		if (mediaNegotiationCommand != null) {
			mediaNegotiationCommand.setInboundCallLegBean(inboundCallLegBean);
			mediaNegotiationCommand.setOutboundCallLegBean(outboundCallLegBean);
			mediaNegotiationCommand.execute();
		} else
			log.debug("Skipping NULL media negotiation command");
	}

	protected CallLegConnectionState mapDialogInfoStateToCallConnectionState(ReadOnlyDialogInfo dialogInfo) {
		DialogState dialogState = dialogInfo.getDialogState();
		if (dialogState.equals(DialogState.Created))
			return CallLegConnectionState.Pending;
		else if (dialogState.equals(DialogState.Initiated) && dialogInfo.isInbound())
			return CallLegConnectionState.Pending;
		else if (dialogState.ordinal() >= DialogState.Confirmed.ordinal())
			return CallLegConnectionState.Completed;
		else
			return CallLegConnectionState.InProgress;
	}
}
