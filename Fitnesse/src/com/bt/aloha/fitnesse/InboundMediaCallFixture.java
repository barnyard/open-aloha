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

 	

 	
 	
 
package com.bt.aloha.fitnesse;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.aloha.call.CallInformation;
import com.bt.aloha.call.event.AbstractCallEndedEvent;
import com.bt.aloha.call.state.CallLegCausingTermination;
import com.bt.aloha.call.state.CallTerminationCause;
import com.bt.aloha.callleg.InboundCallLegListener;
import com.bt.aloha.callleg.event.IncomingCallLegEvent;
import com.bt.aloha.dialog.event.IncomingAction;
import com.bt.aloha.eventing.EventFilter;
import com.bt.aloha.media.DtmfCollectCommand;
import com.bt.aloha.media.MediaCallBean;
import com.bt.aloha.media.MediaCallListener;
import com.bt.aloha.media.convedia.MediaCallBeanImpl;
import com.bt.aloha.media.event.call.CallAnnouncementCompletedEvent;
import com.bt.aloha.media.event.call.CallAnnouncementFailedEvent;
import com.bt.aloha.media.event.call.CallAnnouncementTerminatedEvent;
import com.bt.aloha.media.event.call.CallDtmfGenerationCompletedEvent;
import com.bt.aloha.media.event.call.CallDtmfGenerationFailedEvent;
import com.bt.aloha.media.event.call.CallPromptAndCollectDigitsCompletedEvent;
import com.bt.aloha.media.event.call.CallPromptAndCollectDigitsFailedEvent;
import com.bt.aloha.media.event.call.CallPromptAndCollectDigitsTerminatedEvent;
import com.bt.aloha.media.event.call.CallPromptAndRecordCompletedEvent;
import com.bt.aloha.media.event.call.CallPromptAndRecordFailedEvent;
import com.bt.aloha.media.event.call.CallPromptAndRecordTerminatedEvent;
import com.bt.aloha.sipp.SippEngineTestHelper;
import com.bt.aloha.testing.CallLegListenerStubBase;

public class InboundMediaCallFixture extends InboundDialogFixture implements MediaCallListener {

    private Log log = LogFactory.getLog(this.getClass());
    private MediaCallBean mediaCallBean;
    private String mediaCallId;
    private String audioFileUri;
    private String sippScenarioFile;
    private String commandId;
    private List<String> announcementCompletedEvents = new Vector<String>();
    private Semaphore announcementCompletedSemaphore = new Semaphore(0);
    SippEngineTestHelper sippEngineTestHelper;
    private DtmfCollectCommand dtmfCollectCommand;
    private CallPromptAndCollectDigitsCompletedEvent digitsCollectedEvent;
    private Semaphore digitsCollectedSemaphore = new Semaphore(0);
    private Semaphore digitsFailedSemaphore = new Semaphore(0);
    private CallPromptAndCollectDigitsFailedEvent digitsFailedEvent;

    public InboundMediaCallFixture() {
        super();
        ClassPathXmlApplicationContext inboundApplicationContext = FixtureApplicationContexts.getInstance().startInboundApplicationContext();
		mediaCallBean = (MediaCallBean)inboundApplicationContext.getBean("mediaCallBean");
        List<MediaCallListener> list = new ArrayList<MediaCallListener>();
        list.add(this);
        ((MediaCallBeanImpl)mediaCallBean).setMediaCallListeners(list);

        inboundApplicationContextBeans.getInboundCallLegBean().addInboundCallLegListener(new InboundAnnouncementCallLegListener());
        inboundApplicationContextBeans.getInboundCallLegBean().addInboundCallLegListener(new InboundPromptAndCollectCallLegListener());
    }

    public void audioFileUri(String audioFileUri) {
        this.audioFileUri = audioFileUri;
    }

    public void sippScenarioFile(String aFileName) {
        this.sippScenarioFile = aFileName;
    }

    public int countMediaCallLegs() {
		CallInformation callInformation = mediaCallBean.getCallInformation(mediaCallId);
		int result = 0;
		if (outboundCallLegBean.getCallLegInformation(callInformation.getFirstCallLegId()).isMediaCallLeg())
			result++;
		if (inboundApplicationContextBeans.getInboundCallLegBean().getCallLegInformation(callInformation.getSecondCallLegId()).isMediaCallLeg())
			result++;
		return result;
	}

    public String startSippInboundCall() throws Exception {
        int lastSlash = sippScenarioFile.lastIndexOf("/");
        String directory = sippScenarioFile.substring(0, lastSlash);
        String filename = sippScenarioFile.substring(lastSlash+1);
        sippEngineTestHelper = new SippEngineTestHelper(filename, new File(directory), false);
        System.out.println(sippEngineTestHelper.getErrors());
        log.debug(sippEngineTestHelper.getErrors());
        return "OK";
    }

    public void stopSipp() {
        if (null == sippEngineTestHelper) return;
        sippEngineTestHelper.terminateSipp();
    }

    public void dtmfCollectCommandWithMinMaxNumberOfDigitsAndReturnKey(String dtmfCollectCommandString) {
        dtmfCollectCommand = getDtmfCollectCommandWithMinMaxNumberOfDigitsAndReturnKey(dtmfCollectCommandString, audioFileUri);
    }

    public class InboundAnnouncementCallLegListener extends CallLegListenerStubBase implements InboundCallLegListener, EventFilter {
        private Log log = LogFactory.getLog(this.getClass());

        public void onIncomingCallLeg(IncomingCallLegEvent e) {
            e.setIncomingCallAction(IncomingAction.None);
            String incomingDialogId = e.getId();
            log.debug("Got onIncomingDialog in the InboundAnnouncementCallLegListener, initiating media call");

            mediaCallId = mediaCallBean.createMediaCall(incomingDialogId);
            callIds.add(mediaCallId);
        }

        public boolean shouldDeliverEvent(Object event) {
            return event instanceof IncomingCallLegEvent
               && ((IncomingCallLegEvent) event).getToUri().contains("announcement");
        }
    }

    public class InboundPromptAndCollectCallLegListener extends CallLegListenerStubBase implements InboundCallLegListener, EventFilter {
        private Log log = LogFactory.getLog(this.getClass());

        public void onIncomingCallLeg(IncomingCallLegEvent e) {
            e.setIncomingCallAction(IncomingAction.None);
            String incomingDialogId = e.getId();
            log.debug("Got onIncomingDialog in the InboundPromptAndCollectCallLegListener, initiating media call");

            mediaCallId = mediaCallBean.createMediaCall(incomingDialogId);
            callIds.add(mediaCallId);
        }

        public boolean shouldDeliverEvent(Object event) {
            return event instanceof IncomingCallLegEvent
               && ((IncomingCallLegEvent) event).getToUri().contains("promptandcollect");
        }
    }

    public String playAnnouncement() {
        commandId = mediaCallBean.playAnnouncement(mediaCallId, audioFileUri);
        return commandId;
    }

    public String promptAndCollectDigits() {
        commandId = mediaCallBean.promptAndCollectDigits(mediaCallId, dtmfCollectCommand);
        return commandId;
    }

    public String waitForAnnouncementCompletedEvent() throws Exception {
        if (this.announcementCompletedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS)) {
            if (announcementCompletedEvents.contains(commandId))
                return "OK";
            return announcementCompletedEvents.toString();
        }
        return "No event";
    }

    public String waitForDigitsCollectedEvent() throws Exception {
        if (digitsCollectedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS)) {
            if (digitsCollectedEvent.getMediaCommandId().equals(commandId))
                return digitsCollectedEvent.getDigits();
            return digitsCollectedEvent.getMediaCommandId();
        }
        return "No event";
    }

    public String waitForDigitsFailedEvent() throws Exception {
        if (digitsFailedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS)) {
            if (digitsFailedEvent.getMediaCommandId().equals(commandId))
                return digitsFailedEvent.getDigits();
            return digitsFailedEvent.getMediaCommandId();
        }
        return "No event";
    }

    public void terminateCall() {
        mediaCallBean.terminateMediaCall(mediaCallId);
    }

    private String waitForCallTerminatedEvent(CallTerminationCause callTerminationCause, CallLegCausingTermination callLegCausingTermination) throws Exception {
        String targetId = mediaCallId;
        if (callTerminatedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS)) {
            return waitForCallEndedEvent(callTerminatedEvents, callTerminationCause, callLegCausingTermination, false, targetId);
        }
        return "No event";
    }

    private String waitForCallEndedEvent(Hashtable<String, AbstractCallEndedEvent> callEndedEvents, CallTerminationCause callTerminationCause, CallLegCausingTermination callLegCausingTermination, boolean zeroDuration, String targetId) {
        AbstractCallEndedEvent callEndedEvent = callEndedEvents.get(targetId);
        if (callEndedEvent != null && !callEndedEvent.getCallTerminationCause().equals(callTerminationCause)) {
            if ((zeroDuration && callEndedEvent.getDuration() == 0) || (!zeroDuration && callEndedEvent.getDuration() > 0))
                return callEndedEvent.getCallTerminationCause().toString();
            return String.format("Call duration: %d seconds", callEndedEvent.getDuration());
        } else if (callEndedEvent != null && !callEndedEvent.getCallLegCausingTermination().equals(callLegCausingTermination))
            return callEndedEvent.getCallLegCausingTermination().toString();
        else if (callEndedEvent != null)
            return "OK";
        else return callEndedEvents.keySet().toString();
    }

    public String waitForCallTerminatedEventWithTerminatedByApplication() throws Exception {
        return waitForCallTerminatedEvent(CallTerminationCause.TerminatedByApplication, CallLegCausingTermination.Neither);
    }

    public void onCallAnnouncementCompleted(CallAnnouncementCompletedEvent announcementCompletedEvent) {
        this.announcementCompletedEvents.add(announcementCompletedEvent.getMediaCommandId());
        announcementCompletedSemaphore.release();
    }

    public void onCallAnnouncementFailed(CallAnnouncementFailedEvent announcementFailedEvent) {
    }

    public void onCallAnnouncementTerminated(CallAnnouncementTerminatedEvent announcementTerminatedEvent) {
    }

    public void onCallDtmfGenerationCompleted(CallDtmfGenerationCompletedEvent dtmfGenerationCompletedCompletedEvent) {
    }

    public void onCallDtmfGenerationFailed(CallDtmfGenerationFailedEvent dtmfGenerationFailedEvent) {
    }

    public void onCallPromptAndCollectDigitsCompleted(CallPromptAndCollectDigitsCompletedEvent dtmfCollectDigitsCompletedEvent) {
        this.digitsCollectedEvent = dtmfCollectDigitsCompletedEvent;
        digitsCollectedSemaphore.release();
    }

    public void onCallPromptAndCollectDigitsFailed(CallPromptAndCollectDigitsFailedEvent dtmfCollectDigitsFailedEvent) {
        this.digitsFailedEvent = dtmfCollectDigitsFailedEvent;
        digitsFailedSemaphore.release();
    }

    public void onCallPromptAndCollectDigitsTerminated(CallPromptAndCollectDigitsTerminatedEvent dtmfCollectDigitsTerminatedEvent) {
    }

    public void onCallPromptAndRecordCompleted(CallPromptAndRecordCompletedEvent callPromptAndRecordCompletedEvent) {
    }

    public void onCallPromptAndRecordFailed(CallPromptAndRecordFailedEvent callPromptAndRecordFailedEvent) {
    }

    public void onCallPromptAndRecordTerminated(CallPromptAndRecordTerminatedEvent callPromptAndRecordTerminatedEvent) {
    }
}
