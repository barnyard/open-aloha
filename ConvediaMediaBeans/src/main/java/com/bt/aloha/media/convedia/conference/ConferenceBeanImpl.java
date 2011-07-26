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

package com.bt.aloha.media.convedia.conference;

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.call.CallBean;
import com.bt.aloha.call.CallBeanImpl;
import com.bt.aloha.call.CallListener;
import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.call.event.AbstractCallEvent;
import com.bt.aloha.call.event.CallConnectedEvent;
import com.bt.aloha.call.event.CallConnectionFailedEvent;
import com.bt.aloha.call.event.CallDisconnectedEvent;
import com.bt.aloha.call.event.CallTerminatedEvent;
import com.bt.aloha.call.event.CallTerminationFailedEvent;
import com.bt.aloha.call.state.ImmutableCallInfo;
import com.bt.aloha.callleg.OutboundCallLegBean;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.dialog.state.ReadOnlyDialogInfo;
import com.bt.aloha.media.MediaDialogInfo;
import com.bt.aloha.media.conference.collections.ConferenceCollection;
import com.bt.aloha.media.conference.event.ConferenceActiveEvent;
import com.bt.aloha.media.conference.event.ConferenceEndedEvent;
import com.bt.aloha.media.conference.event.ParticipantConnectedEvent;
import com.bt.aloha.media.conference.event.ParticipantDisconnectedEvent;
import com.bt.aloha.media.conference.event.ParticipantFailedEvent;
import com.bt.aloha.media.conference.event.ParticipantTerminatedEvent;
import com.bt.aloha.media.conference.state.ConferenceInfo;
import com.bt.aloha.media.conference.state.ConferenceInformation;
import com.bt.aloha.media.conference.state.ConferenceState;
import com.bt.aloha.media.conference.state.ConferenceTerminationCause;
import com.bt.aloha.media.conference.state.ImmutableConferenceInfo;
import com.bt.aloha.media.conference.state.ParticipantState;
import com.bt.aloha.media.conference.state.ReadOnlyConferenceInfo;
import com.bt.aloha.media.convedia.MediaServerAddressFactory;
import com.bt.aloha.stack.SimpleSipBeanBase;
import com.bt.aloha.stack.SimpleSipStack;
import com.bt.aloha.util.ConcurrentUpdateBlock;
import com.bt.aloha.util.ConcurrentUpdateManager;
import com.bt.aloha.util.ConcurrentUpdateManagerImpl;
import com.bt.aloha.util.HousekeeperAware;

public class ConferenceBeanImpl extends SimpleSipBeanBase implements ConferenceBean, CallListener, HousekeeperAware {
    private static final String CANNOT_ADD_A_NULL_LISTENER = "Cannot add a null listener";
    private static final String NO_CONFERENCE_FOR_CALL = "No conference for call %s";
    private Log log = LogFactory.getLog(this.getClass());
    private OutboundCallLegBean outboundCallLegBean;
    private CallBean callBean;
    private ConferenceCollection conferenceCollection;
    private CallCollection callCollection;
    private DialogCollection dialogCollection;
    private List<ConferenceListener> conferenceListeners = new ArrayList<ConferenceListener>();
    private ConcurrentUpdateManager concurrentUpdateManager = new ConcurrentUpdateManagerImpl();
    private String conferenceServiceSipUri = "sip:conferencing@sdk.bt.com";
    private int defaultMaxNumberOfParticipants = ConferenceInfo.DEFAULT_MAX_NUMBER_OF_PARTICIPANTS;
    private long defaultMaxDurationInMinutes = ConferenceInfo.DEFAULT_MAX_DURATION_IN_MINUTES;
    private MaxConferenceDurationScheduler scheduler;
    private MediaServerAddressFactory mediaServerAddressFactory;
    private SimpleSipStack simpleSipStack;

    public ConferenceBeanImpl() {
    }

    public String createConference(int maxNumberOfParticipants, long maxDurationInMinutes) {
        log.debug(String.format("Creating conference with max %d participants", maxNumberOfParticipants));
        ConferenceInfo conferenceInfo = new ConferenceInfo(getBeanName(), mediaServerAddressFactory.getAddress(),
                maxNumberOfParticipants, maxDurationInMinutes);
        conferenceCollection.add(conferenceInfo);
        log.debug(String.format("Created conference %s", conferenceInfo.getConferenceSipUri()));
        return conferenceInfo.getId();
    }

    public String createConference() {
        return createConference(defaultMaxNumberOfParticipants, defaultMaxDurationInMinutes);
    }

    public String createParticipantCallLeg(String conferenceId, URI sipUri) {
        if (sipUri == null)
            throw new IllegalArgumentException(String.format(
                    "Cannot create conference dialog for conference %s with null sip uri", conferenceId));
        log
                .debug(String.format("Creating conference dialog for %s in conference %s ", sipUri.toString(),
                        conferenceId));
        ImmutableConferenceInfo confInfo = conferenceCollection.get(conferenceId);
        if (confInfo == null)
            throw new IllegalArgumentException(String.format(
                    "Cannot create conference dialog %s for non-existing conference %s", sipUri.toString(),
                    conferenceId));
        return outboundCallLegBean.createCallLeg(getConferenceServiceSipUri(), sipUri);
    }

    public void inviteParticipant(final String conferenceId, final String callLegId) {
        log.debug(String.format("Inviting participant dialog %s to conference %s", callLegId, conferenceId));

        ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
            public void execute() {
                ConferenceInfo confInfo = conferenceCollection.get(conferenceId);
                if (confInfo == null)
                    throw new IllegalArgumentException(String.format(
                            "Cannot invite participant %s to non-existing conference %s", callLegId, conferenceId));
                if (confInfo.getConferenceState().ordinal() > ConferenceState.Active.ordinal())
                    throw new IllegalStateException(String.format("Cannot invite participant to Ended conference (%s)",
                            conferenceId));
                if (confInfo.isMaxNumberOfParticipants())
                    throw new IllegalStateException(
                            String
                                    .format(
                                            "Max number of participants (%d) in conference %s. Cannot add a new participant %s to that conference.",
                                            confInfo.getMaxNumberOfParticipants(), conferenceId, callLegId));
                ReadOnlyDialogInfo dialogInfo = dialogCollection.get(callLegId);
                if (dialogInfo == null)
                    throw new IllegalArgumentException(String.format(
                            "Cannot invite participant to conference %s, dialog %s does not exist", conferenceId,
                            callLegId));
                URI participantUri = URI.create(dialogInfo.getRemoteParty().getURI().toString());
                URI conferenceUri = URI.create(confInfo.getConferenceSipUri());
                String conferenceDialogId = createConferenceCallLeg(participantUri, conferenceUri);
                String callId = ((CallBeanImpl) callBean).generateCallId();
                confInfo.addParticipant(callId);
                conferenceCollection.replace(confInfo);
                ((CallBeanImpl) callBean).joinDialogs(callId, callLegId, conferenceDialogId);
            }

            public String getResourceId() {
                return conferenceId;
            }
        };
        concurrentUpdateManager.executeConcurrentUpdate(concurrentUpdateBlock);

    }

    // TODO should this be in a ConferenceCallLegBean class instead? Kinda like
    // all the others?
    protected String createConferenceCallLeg(URI participantUri, URI conferenceUri) {
        String dialogId = this.simpleSipStack.getSipProvider().getNewCallId().getCallId();
        MediaDialogInfo conferenceDialogInfo = new MediaDialogInfo(dialogId, this.outboundCallLegBean.getBeanName(),
                this.simpleSipStack.getIpAddress(), participantUri.toString(), conferenceUri.toString(),
                this.simpleSipStack.generateNewTag(), -1);
        this.dialogCollection.add(conferenceDialogInfo);
        return conferenceDialogInfo.getId();
    }

    public void terminateParticipant(String conferenceId, String callLegId) {
        log.debug(String.format("Terminating participant dialog %s from conference %s", callLegId, conferenceId));

        ImmutableCallInfo callInfo = callCollection.getCurrentCallForCallLeg(callLegId);
        if (null == callInfo) {
            log
                    .debug(String.format("Participant %s not currently in a call in conference %s", callLegId,
                            conferenceId));
            return;
        }
        ReadOnlyConferenceInfo confInfo = conferenceCollection.get(conferenceId);
        if (confInfo == null)
            throw new IllegalArgumentException(String.format(
                    "Cannot terminate participant %s in non-existing conference %s", callLegId, conferenceId));
        if (!confInfo.containsParticipant(callInfo.getId())) {
            log.debug(String.format("Participant %s not currently in conference %s", callLegId, conferenceId));
            return;
        }
        if (confInfo.getParticipantState(callInfo.getId()).ordinal() > ParticipantState.Connected.ordinal()) {
            log.debug(String.format(
                    "Participant %s not already terminating, terminated, disconnected, or failed in conference %s",
                    callLegId, conferenceId));
            return;

        }
        callBean.terminateCall(callInfo.getId());
    }

    public void endConference(final String conferenceId) {
        endConference(conferenceId, ConferenceTerminationCause.EndedByApplication);
    }

    public void endConference(final String conferenceId, final ConferenceTerminationCause terminationCause) {
        log.debug(String.format("Ending conference %s", conferenceId));
        ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
            public void execute() {
                List<String> callsToTerminate = new ArrayList<String>();
                ConferenceInfo conferenceInfo = conferenceCollection.get(conferenceId);
                if (conferenceInfo == null)
                    throw new IllegalArgumentException(String.format("Cannot end non-existing conference %s",
                            conferenceId));
                if (conferenceInfo.getConferenceState().ordinal() > ConferenceState.Active.ordinal()) {
                    log.debug(String.format("Conference %s is Ending or already Ended", conferenceId));
                    return;
                }
                if (conferenceInfo.getNumberOfActiveParticipants() > 0) {
                    for (String callId : conferenceInfo.getParticipants().keySet()) {
                        if (conferenceInfo.updateParticipantState(callId, ParticipantState.Terminating)) {
                            callsToTerminate.add(callId);
                        } else {
                            log.debug(String.format("call %s is already terminating or has terminated", callId));
                        }
                    }
                    if (conferenceInfo.updateConferenceState(ConferenceState.Ending)) {
                        conferenceInfo.setConferenceTerminationCause(terminationCause);
                        conferenceCollection.replace(conferenceInfo);
                        for (String callId : callsToTerminate) {
                            callBean.terminateCall(callId);
                        }
                    } else {
                        log.debug(String.format("Conference %s is already ending or ended", conferenceId));
                    }
                } else {
                    log.debug(String.format("Ending conference %s without any participant", conferenceId));
                    if (conferenceInfo.setConferenceTerminationCause(terminationCause)
                            && conferenceInfo.updateConferenceState(ConferenceState.Ended)) {
                        conferenceCollection.replace(conferenceInfo);
                        getEventDispatcher().dispatchEvent(
                                getConferenceListeners(),
                                new ConferenceEndedEvent(conferenceInfo.getId(), conferenceInfo
                                        .getConferenceTerminationCause(), conferenceInfo.getDuration()));
                    }
                }
            }

            public String getResourceId() {
                return conferenceId;
            }
        };
        concurrentUpdateManager.executeConcurrentUpdate(concurrentUpdateBlock);
    }

    public void addConferenceListener(ConferenceListener listener) {
        if (listener == null)
            throw new IllegalArgumentException(CANNOT_ADD_A_NULL_LISTENER);
        this.getConferenceListeners().add(listener);
    }

    public void removeConferenceListener(ConferenceListener listener) {
        if (listener == null)
            throw new IllegalArgumentException(CANNOT_ADD_A_NULL_LISTENER);
        this.getConferenceListeners().remove(listener);
    }

    public ConferenceInformation getConferenceInformation(String conferenceId) {
        return conferenceCollection.get(conferenceId).getConferenceInformation();
    }

    public List<ConferenceListener> getConferenceListeners() {
        return conferenceListeners;
    }

    // Setters
    public void setConferenceListeners(List<ConferenceListener> conferenceListenerList) {
        this.conferenceListeners = conferenceListenerList;
    }

    public void setCallBean(CallBean aCallBean) {
        if (this.callBean != null)
            this.callBean.removeCallListener(this);
        this.callBean = aCallBean;
        this.callBean.addCallListener(this);
    }

    public void setConferenceCollection(ConferenceCollection aConferenceCollection) {
        this.conferenceCollection = aConferenceCollection;
    }

    public void setOutboundCallLegBean(OutboundCallLegBean aOutboundCallLegBean) {
        this.outboundCallLegBean = aOutboundCallLegBean;
    }

    public void setMediaServerAddressFactory(MediaServerAddressFactory aMediaServerAddressFactory) {
        this.mediaServerAddressFactory = aMediaServerAddressFactory;
    }

    public void setDialogCollection(DialogCollection aDialogCollection) {
        this.dialogCollection = aDialogCollection;
    }

    public void setCallCollection(CallCollection aCallCollection) {
        this.callCollection = aCallCollection;
    }

    // Processing Call Events

    public void onCallConnected(CallConnectedEvent callConnectedEvent) {
        final String callId = callConnectedEvent.getCallId();
        final ConferenceBean conferenceBean = this;
        ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
            private String conferenceId;

            public void execute() {
                ConferenceInfo conferenceInfo = conferenceCollection.getCurrentConferenceForCall(callId);
                if (null == conferenceInfo) {
                    log.info(String.format(NO_CONFERENCE_FOR_CALL, callId));
                    return;
                }
                if (conferenceInfo.updateParticipantState(callId, ParticipantState.Connected)) {
                    this.conferenceId = conferenceInfo.getId();
                    ImmutableCallInfo callInfo = callCollection.get(callId);
                    boolean goingActive = false;
                    if (ConferenceState.Initial.equals(conferenceInfo.getConferenceState())
                            && conferenceInfo.updateConferenceState(ConferenceState.Active)) {
                        goingActive = true;
                    }
                    if (goingActive) {
                        if (conferenceInfo.getMaxDurationInMinutes() > 0)
                            scheduler.terminateConferenceAfterMaxDuration(conferenceInfo, conferenceBean);
                        else
                            log.debug(String.format("Conference %s is not scheduled for termination.", conferenceInfo
                                    .getId()));
                    }
                    conferenceCollection.replace(conferenceInfo);
                    if (goingActive) {
                        getEventDispatcher().dispatchEvent(getConferenceListeners(),
                                new ConferenceActiveEvent(conferenceInfo.getId()));
                    }
                    getEventDispatcher().dispatchEvent(getConferenceListeners(),
                            new ParticipantConnectedEvent(conferenceInfo.getId(), callInfo.getFirstDialogId()));
                } else {
                    log.debug(String.format("Participant %s already at least Connected", callId));
                }
            }

            public String getResourceId() {
                return conferenceId;
            }
        };
        concurrentUpdateManager.executeConcurrentUpdate(concurrentUpdateBlock);
    }

    public void onCallConnectionFailed(CallConnectionFailedEvent callConnectionFailedEvent) {
        final String callId = callConnectionFailedEvent.getCallId();
        ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
            private String conferenceId;

            public void execute() {
                ImmutableCallInfo callInfo = callCollection.get(callId);
                ConferenceInfo conferenceInfo = conferenceCollection.getCurrentConferenceForCall(callId);
                if (null == conferenceInfo) {
                    log.info(String.format(NO_CONFERENCE_FOR_CALL, callId));
                    return;
                }
                conferenceId = conferenceInfo.getId();
                if (conferenceInfo.updateParticipantState(callId, ParticipantState.Failed)) {
                    conferenceCollection.replace(conferenceInfo);
                    getEventDispatcher().dispatchEvent(getConferenceListeners(),
                            new ParticipantFailedEvent(conferenceInfo.getId(), callInfo.getFirstDialogId()));
                } else {
                    log.debug(String.format("Participant %s already Failed", callId));
                }
            }

            public String getResourceId() {
                return conferenceId;
            }
        };
        concurrentUpdateManager.executeConcurrentUpdate(concurrentUpdateBlock);
    }

    private void endCall(final AbstractCallEvent event) {
        final String callId = event.getCallId();
        ConcurrentUpdateBlock concurrentUpdateBlock = new ConcurrentUpdateBlock() {
            private String conferenceId;

            public void execute() {
                boolean endConference = false;
                ConferenceInfo conferenceInfo = conferenceCollection.getCurrentConferenceForCall(callId);
                if (null == conferenceInfo) {
                    log.info(String.format(NO_CONFERENCE_FOR_CALL, callId));
                    return;
                }
                this.conferenceId = conferenceInfo.getId();
                ImmutableCallInfo callInfo = callCollection.get(callId);
                boolean participantUpdated = false;
                ConferenceTerminationCause terminationCause;
                if (event instanceof CallDisconnectedEvent) {
                    participantUpdated = conferenceInfo.updateParticipantState(callId, ParticipantState.Disconnected);
                    terminationCause = ConferenceTerminationCause.LastParticipantDisconnected;
                } else {
                    participantUpdated = conferenceInfo.updateParticipantState(callId, ParticipantState.Terminated);
                    terminationCause = ConferenceTerminationCause.LastParticipantTerminated;
                }
                if (conferenceInfo.getNumberOfActiveParticipants() < 1) {
                    if (conferenceInfo.updateConferenceState(ConferenceState.Ended)) {
                        conferenceInfo.setEndTime(Calendar.getInstance().getTimeInMillis());
                        conferenceInfo.setConferenceTerminationCause(terminationCause);
                        endConference = true;
                    }
                }
                conferenceCollection.replace(conferenceInfo);
                if (participantUpdated) {
                    if (event instanceof CallDisconnectedEvent) {
                        getEventDispatcher().dispatchEvent(getConferenceListeners(),
                                new ParticipantDisconnectedEvent(conferenceInfo.getId(), callInfo.getFirstDialogId()));
                    } else {
                        getEventDispatcher().dispatchEvent(getConferenceListeners(),
                                new ParticipantTerminatedEvent(conferenceInfo.getId(), callInfo.getFirstDialogId()));
                    }
                }
                if (endConference) {
                    scheduler.cancelTerminateConference(conferenceInfo);
                    getEventDispatcher().dispatchEvent(
                            getConferenceListeners(),
                            new ConferenceEndedEvent(conferenceInfo.getId(), conferenceInfo
                                    .getConferenceTerminationCause(), conferenceInfo.getDuration()));
                }
            }

            public String getResourceId() {
                return conferenceId;
            }
        };
        concurrentUpdateManager.executeConcurrentUpdate(concurrentUpdateBlock);
    }

    public void onCallDisconnected(CallDisconnectedEvent callDisconnectedEvent) {
        endCall(callDisconnectedEvent);
    }

    public void onCallTerminated(CallTerminatedEvent callTerminatedEvent) {
        endCall(callTerminatedEvent);
    }

    public void onCallTerminationFailed(CallTerminationFailedEvent callTerminationFailedEvent) {
        endCall(callTerminationFailedEvent);
    }

    public void setConferenceServiceSipUri(String aConferenceServiceSipUri) {
        this.conferenceServiceSipUri = aConferenceServiceSipUri;
    }

    private URI getConferenceServiceSipUri() {
        return URI.create(this.conferenceServiceSipUri);
    }

    public void setDefaultMaxNumberOfParticipants(int aDefaultMaxNumberOfParticipants) {
        if (aDefaultMaxNumberOfParticipants < 0 || aDefaultMaxNumberOfParticipants == 1)
            throw new IllegalArgumentException(String.format("Illegal default maximum number of participants: %d",
                    aDefaultMaxNumberOfParticipants));
        this.defaultMaxNumberOfParticipants = aDefaultMaxNumberOfParticipants;
    }

    public void setDefaultMaxDurationInMinutes(long aDefaultMaxDurationInMinutes) {
        if (aDefaultMaxDurationInMinutes < 0)
            throw new IllegalArgumentException(String.format("Illegal default maximum duration in minutes: %d",
                    aDefaultMaxDurationInMinutes));
        this.defaultMaxDurationInMinutes = aDefaultMaxDurationInMinutes;
    }

    public void setMaxConferenceDurationScheduler(MaxConferenceDurationScheduler aScheduler) {
        this.scheduler = aScheduler;
    }

    public void killHousekeeperCandidate(String infoId) {
        endConference(infoId, ConferenceTerminationCause.Housekept);
    }

    public void setSimpleSipStack(SimpleSipStack aSimpleSipStack) {
        this.simpleSipStack = aSimpleSipStack;
    }
}
