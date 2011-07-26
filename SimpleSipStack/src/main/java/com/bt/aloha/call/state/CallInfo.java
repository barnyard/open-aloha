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

 	

 	
 	
 
package com.bt.aloha.call.state;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.state.StateInfoBase;

public class CallInfo extends StateInfoBase<CallInfo> implements ReadOnlyCallInfo {
    static final String CALL_LEG_S_NOT_IN_CALL_S = "Call leg %s not in call %s";
    private static final String FUTURE = "future";
    private static final long serialVersionUID = 1L;
    private static Log log = LogFactory.getLog(CallInfo.class);
    private CallState callState = CallState.Connecting;
    private String firstDialogId;
    private String secondDialogId;
    private CallLegConnectionState firstCallLegConnectionState = CallLegConnectionState.Pending;
    private CallLegConnectionState secondCallLegConnectionState = CallLegConnectionState.Pending;
    private MediaNegotiationState mediaNegotiationState = MediaNegotiationState.Pending;
    private MediaNegotiationMethod mediaNegotiationMethod;
    private long maxDurationInMinutes;
    private transient ScheduledFuture<?> future;
    private AutoTerminateAction autoTerminate;
    private CallTerminationCause callTerminationCause;
    private CallLegCausingTermination callLegCausingTermination;
    private PendingCallReinvite pendingCallReinvite;

    public CallInfo(String creatingBeanName, String aCallId, String theFirstDialogId, String theSecondDialogId, AutoTerminateAction aAutoTerminate, long theMaxDurationInMinutes) {
        super(creatingBeanName);
        this.firstDialogId = theFirstDialogId;
        this.secondDialogId = theSecondDialogId;
        setId(aCallId);
        this.maxDurationInMinutes = theMaxDurationInMinutes;
        this.callTerminationCause = null;
        this.callLegCausingTermination = null;
        this.mediaNegotiationMethod = null;
        this.autoTerminate = aAutoTerminate;
        this.pendingCallReinvite = null;
    }

    @Override
    public CallInfo cloneObject() {
        return (CallInfo)super.cloneObject();
    }

    public String getFirstDialogId() {
        return firstDialogId;
    }

    public String getSecondDialogId() {
        return secondDialogId;
    }

    public CallState getCallState() {
        return callState;
    }

    public long getMaxDurationInMinutes() {
        return maxDurationInMinutes;
    }

    public void setMaxDurationInMinutes(long aMaxDurationInMinutes) {
        maxDurationInMinutes = aMaxDurationInMinutes;
    }

    /**
     *
     * @param newCallState
     * @return Previous call state if the update resulted in the call state
     *         being advanced, null if it did not
     */
    public CallState setCallState(CallState newCallState) {
        CallState currentCallState = callState;
        if (newCallState.ordinal() > currentCallState.ordinal()) {
            this.callState = newCallState;
            updateLastUsedTime();

            if (newCallState.equals(CallState.Connected))
                setStartTime(Calendar.getInstance().getTimeInMillis());
            if (newCallState.equals(CallState.Terminated))
                setEndTime(Calendar.getInstance().getTimeInMillis());

            return currentCallState;
        }

        log.debug(String.format(
                "Attempt to set call state to same or previous or current (%s to %s) for call %s",
                currentCallState, newCallState, getId()));
        return null;
    }

    public ScheduledFuture<?> getFuture() {
        return future;
    }

    public void setFuture(ScheduledFuture<?> scheduledFuture) {
        this.future = scheduledFuture;
    }

    public CallTerminationCause getCallTerminationCause() {
        return callTerminationCause;
    }

    public CallLegCausingTermination getCallLegCausingTermination() {
        return callLegCausingTermination;
    }

    public boolean setCallTerminationCause(CallTerminationCause aCallTerminationCause, CallLegCausingTermination aCallLegCausingTermination) {
        if (this.callTerminationCause == null) {
            this.callTerminationCause = aCallTerminationCause;
            this.callLegCausingTermination = aCallLegCausingTermination;
            return true;
        }
        log.debug(String.format(
                "Attempt to set call termination cause twice (current cause:%s, new cause:%s, leg causing:%s) for call %s",
                this.callTerminationCause, aCallTerminationCause, aCallLegCausingTermination, getId()));

        return false;
    }

    @Override
    public boolean isDead() {
        return this.callState.equals(CallState.Terminated);
    }

    @Override
    public Map<String, Object> getTransients() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(FUTURE, this.getFuture());
        return result;
    }

    @Override
    public void setTransients(Map<String, Object> m) {
        if (m.containsKey(FUTURE)) this.setFuture((ScheduledFuture<?>)m.get(FUTURE));

    }

    public AutoTerminateAction getAutoTerminate() {
        return autoTerminate;
    }

    public Class<? extends CallMessageFlow> getCallMessageFlow() {
        return DefaultCallMessageFlowImpl.class;
    }

    public MediaNegotiationState getMediaNegotiationState() {
        return mediaNegotiationState;
    }

    public void setMediaNegotiationState(MediaNegotiationState aMediaNegotiationState) {
        this.mediaNegotiationState = aMediaNegotiationState;
    }

    public CallLegConnectionState getCallLegConnectionState(String callLegId) {
        if (firstDialogId.equals(callLegId))
            return firstCallLegConnectionState;
        else if (secondDialogId.equals(callLegId))
            return secondCallLegConnectionState;
        else
            throw new IllegalArgumentException(String.format(CALL_LEG_S_NOT_IN_CALL_S, callLegId, getId()));
    }

    public void setCallLegConnectionState(String callLegId, CallLegConnectionState aCallLegConnectionState) {
        if (aCallLegConnectionState == null)
            throw new IllegalArgumentException(String.format("Null call leg conn state for leg %s in call %s", callLegId, getId()));

        if (firstDialogId.equals(callLegId)) {
            log.debug(String.format("Set first leg (%s) connection state to %s for call %s", callLegId, aCallLegConnectionState, getId()));
            this.firstCallLegConnectionState = aCallLegConnectionState;
        } else if (secondDialogId.equals(callLegId)) {
            log.debug(String.format("Set second leg (%s) connection state to %s for call %s", callLegId, aCallLegConnectionState, getId()));
            this.secondCallLegConnectionState = aCallLegConnectionState;
        } else
            throw new IllegalArgumentException(String.format(CALL_LEG_S_NOT_IN_CALL_S, callLegId, getId()));
    }

    public boolean areBothCallLegsConnected() {
        return firstCallLegConnectionState.equals(CallLegConnectionState.Completed)
            && secondCallLegConnectionState.equals(CallLegConnectionState.Completed);
    }

    public MediaNegotiationMethod getMediaNegotiationMethod() {
        return mediaNegotiationMethod;
    }

    // TODO: check that having made it public - from protected - to allow creation of a CallInfo loaded from db, is OK
    public void setMediaNegotiationMethod(MediaNegotiationMethod aMediaNegotiationMethod) {
        this.mediaNegotiationMethod = aMediaNegotiationMethod;
    }

    public PendingCallReinvite getPendingCallReinvite() {
        return pendingCallReinvite;
    }

    public void setPendingCallReinvite(PendingCallReinvite aPendingCallReinvite) {
        this.pendingCallReinvite = aPendingCallReinvite;
    }

    public CallLegConnectionState getFirstCallLegConnectionState() {
        return firstCallLegConnectionState;
    }

    public CallLegConnectionState getSecondCallLegConnectionState() {
        return secondCallLegConnectionState;
    }
}
