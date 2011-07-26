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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationContext;

import com.bt.aloha.call.CallBean;
import com.bt.aloha.call.CallBeanImpl;
import com.bt.aloha.call.CallInformation;
import com.bt.aloha.call.CallListener;
import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.callleg.OutboundCallLegBean;
import com.bt.aloha.media.DtmfCollectCommand;
import com.bt.aloha.media.MediaCallBean;
import com.bt.aloha.media.MediaCallLegBean;
import com.bt.aloha.media.MediaCallListener;
import com.bt.aloha.media.PromptAndRecordCommand;
import com.bt.aloha.media.convedia.MediaCallBeanImpl;
import com.bt.aloha.media.convedia.MediaCallLegBeanImpl;
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

public class MediaCallFixture extends OutboundCallFixture implements MediaCallListener {
	private ApplicationContext mediaApplicationContext;
	private String audioFileUri;
	private String recordedFileUri;
	private MediaCallBean mediaCallBean;
	private String mediaCallId;
	private DtmfCollectCommand dtmfCollectCommand;
	private int maxDurationOfAudio;
	private PromptAndRecordCommand promptAndRecordCommand;
	private Semaphore announcementCompletedSemaphore = new Semaphore(0);
	private Semaphore announcementFailedSemaphore = new Semaphore(0);
	private Semaphore announcementTerminatedSemaphore = new Semaphore(0);
	private Semaphore digitsCollectedSemaphore = new Semaphore(0);
	private Semaphore digitsCollectionFailedSemaphore = new Semaphore(0);
	private Semaphore digitsCollectionTerminatedSemaphore = new Semaphore(0);
	private Semaphore dtmfGenerationCompletedSemaphore = new Semaphore(0);
	private Semaphore dtmfGenerationFailedSemaphore = new Semaphore(0);
	private Semaphore promptAndRecordCompletedSemaphore = new Semaphore(0);
	private Semaphore promptAndRecordFailedSemaphore = new Semaphore(0);
	private Semaphore promptAndRecordTerminatedSemaphore = new Semaphore(0);
	protected CallAnnouncementCompletedEvent announcementCompletedEvent = null;
	protected CallAnnouncementFailedEvent announcementFailedEvent = null;
	protected CallAnnouncementTerminatedEvent announcementTerminatedEvent = null;
	protected CallPromptAndCollectDigitsCompletedEvent digitsCollectedEvent = null;
	protected CallPromptAndCollectDigitsFailedEvent digitsCollectionFailedEvent = null;
	protected CallPromptAndCollectDigitsTerminatedEvent digitsCollectionTerminatedEvent = null;
	protected CallDtmfGenerationCompletedEvent dtmfGenerationCompletedEvent = null;
	protected CallDtmfGenerationFailedEvent dtmfGenerationFailedEvent = null;
	protected CallPromptAndRecordCompletedEvent promptAndRecordCompletedEvent = null;
	protected CallPromptAndRecordFailedEvent promptAndRecordFailedEvent = null;
	protected CallPromptAndRecordTerminatedEvent promptAndRecordTerminatedEvent = null;
	protected List<String> commandIds = new ArrayList<String>();
	protected CallCollection callCollection;
	private int iterations = 1;
	private int interval = 0;
	private boolean barge = false;

    public MediaCallFixture(ApplicationContext appCtx) {
        super();
        this.mediaApplicationContext = appCtx;
        commandIds = new ArrayList<String>();
        mediaCallBean = (MediaCallBean)mediaApplicationContext.getBean("mediaCallBean");
        callBean = (CallBean)mediaApplicationContext.getBean("callBean");

        cleanListenerList();

        outboundCallLegBean = (OutboundCallLegBean)mediaApplicationContext.getBean("outboundCallLegBean");
        ((CallBean)callBean).addCallListener(this);
        List<MediaCallListener> mediaCallListeners = new ArrayList<MediaCallListener>();
        mediaCallListeners.add(this);
        ((MediaCallBeanImpl)mediaCallBean).setMediaCallListeners(mediaCallListeners);
        callCollection = (CallCollection)mediaApplicationContext.getBean("callCollection");
    }

	public MediaCallFixture() {
        this(FixtureApplicationContexts.getInstance().startMediaApplicationContext());
    }

    private void cleanListenerList() {
        List<CallListener> listenerList = ((CallBeanImpl)callBean).getCallListeners();
        List<CallListener> toBeRemoved = new ArrayList<CallListener>();
        for (Object l: listenerList) {
            if (l instanceof MediaCallFixture) {
                toBeRemoved.add((CallListener)l);
            }
        }
        for (CallListener l: toBeRemoved) {
            callBean.removeCallListener(l);
        }
    }

    public void audioFileUri(String aAudioFileUri) {
		this.audioFileUri = aAudioFileUri;
	}
    
    public void recordedFileUri(String aRecordedFileUri) {
		this.recordedFileUri = aRecordedFileUri;
	}

    @Override
	public void firstPhoneUri(String firstPhoneUri) {
		super.firstPhoneUri(firstPhoneUri);
		super.secondPhoneUri(firstPhoneUri);
	}

	@Override
	public String createFirstDialog() {
		firstDialogId = outboundCallLegBean.createCallLeg(URI.create(secondPhoneUri), URI.create(firstPhoneUri), firstDialogCallAnswerTimeout);
		return "OK";
	}

	public String createFirstDialogAsMediaDialog() {
		MediaCallLegBeanImpl mediaCallLegBean = (MediaCallLegBeanImpl)mediaApplicationContext.getBean("mediaCallLegBean");
		firstDialogId = mediaCallLegBean.createMediaDialogLeg(secondPhoneUri, firstDialogCallAnswerTimeout);
		return "OK";
	}
	
	public int countMediaCallLegs() {
		CallInformation callInformation = mediaCallBean.getCallInformation(mediaCallId);
		int result = 0;
		if (outboundCallLegBean.getCallLegInformation(callInformation.getFirstCallLegId()).isMediaCallLeg())
			result++;
		if (outboundCallLegBean.getCallLegInformation(callInformation.getSecondCallLegId()).isMediaCallLeg())
			result++;
		return result;
	}

	public String createMediaCall() {
		latch = new CountDownLatch(1);
		mediaCallId = mediaCallBean.createMediaCall(firstDialogId);
		callIds.add(mediaCallId);
		latch.countDown();
		return "OK";
	}

	public String playAnnouncement() {
		String commandId  = mediaCallBean.playAnnouncement(mediaCallId, audioFileUri);
		commandIds.add(commandId);
		return commandId;
	}

	public String playAnnouncementIterations() {
		try {
			String commandId  = mediaCallBean.playAnnouncement(mediaCallId, audioFileUri, iterations, interval);
			commandIds.add(commandId);
			return commandId;
		} catch (Exception e) {
			return e.getClass().getSimpleName();
		}
	}

	public String playAnnouncementBarge() {
		try {
			String commandId  = mediaCallBean.playAnnouncement(mediaCallId, audioFileUri, iterations, interval, barge);
			commandIds.add(commandId);
			return commandId;
		} catch (Exception e) {
			return e.getClass().getSimpleName();
		}
	}
	
	public boolean isAnnouncementBarged() {
		if (announcementCompletedEvent != null)
			return announcementCompletedEvent.getBarged();
		return false;
	}

	public void iterations(int val) {
		this.iterations = val;
	}

	public void interval(int val) {
		this.interval = val;
	}
	
	public void barge(String val) {
		this.barge = val.equalsIgnoreCase("true");
	}

	public String cancelLastMediaCommand() {
		mediaCallBean.cancelMediaCommand(mediaCallId, commandIds.get(commandIds.size()-1));
		return "OK";
	}

	public void generateDtmf(String digits) {
		String commandId = null;
		if (digits.contains(",")) {
			commandId = mediaCallBean.generateDtmfDigits(mediaCallId, digits.split(",")[0], Integer.parseInt(digits.split(",")[1]));
		} else {
			commandId = mediaCallBean.generateDtmfDigits(mediaCallId, digits);
		}
		commandIds.add(commandId);
	}

	public void sendDtmfToMediaDialog(String digits) {
		MediaCallLegBean mediaCallLegBean = (MediaCallLegBean)mediaApplicationContext.getBean("mediaCallLegBean");
		String managedMediaDialogId = callCollection.get(mediaCallId).getSecondDialogId();
		mediaCallLegBean.generateDtmfDigits(firstDialogId, managedMediaDialogId, digits);
	}

	public void sendAnnouncementToMediaDialog(String announcementFileUri) {
		MediaCallLegBean mediaCallLegBean = (MediaCallLegBean)mediaApplicationContext.getBean("mediaCallLegBean");
		String managedMediaDialogId = callCollection.get(mediaCallId).getSecondDialogId();
		mediaCallLegBean.playAnnouncement(firstDialogId, managedMediaDialogId, announcementFileUri, false, true);
	}

	public void dtmfCollectCommand(String dtmfCollectCommandString) {
		String[] commandElements = dtmfCollectCommandString.split(",");
		Character cancelKey = null;
		if (commandElements.length > 6)
			cancelKey = new Character(commandElements[6].charAt(0));
		dtmfCollectCommand = new DtmfCollectCommand(
				audioFileUri,
				commandElements[0].toLowerCase().equals("true"),	// barge
				commandElements[1].toLowerCase().equals("true"),	// clear buffer
				Integer.parseInt(commandElements[2]),				// first digit timeout
				Integer.parseInt(commandElements[3]),				// inter digit timeout
				Integer.parseInt(commandElements[4]),				// last digit timeout
				Integer.parseInt(commandElements[5]),				// num digits
				cancelKey);
	}

	public void dtmfCollectCommandWithMinMaxNumberOfDigits(String dtmfCollectCommandString) {
		String[] commandElements = dtmfCollectCommandString.split(",");
		dtmfCollectCommand = new DtmfCollectCommand(
				audioFileUri,
				commandElements[0].toLowerCase().equals("true"),	// barge
				commandElements[1].toLowerCase().equals("true"),	// clear buffer
				Integer.parseInt(commandElements[2]),				// first digit timeout
				Integer.parseInt(commandElements[3]),				// inter digit timeout
				Integer.parseInt(commandElements[4]),				// last digit timeout
				Integer.parseInt(commandElements[5]),				// min number of digits
				Integer.parseInt(commandElements[6]));				// max number of digits
	}

	public void dtmfCollectCommandWithMinMaxNumberOfDigitsAndReturnKey(String dtmfCollectCommandString) {
		dtmfCollectCommand = getDtmfCollectCommandWithMinMaxNumberOfDigitsAndReturnKey(dtmfCollectCommandString, audioFileUri);
	}

	public void promptAndRecordCommand(String promptRecordCommandString) {
		String[] commandElements = promptRecordCommandString.split(",");
		promptAndRecordCommand = new PromptAndRecordCommand(
				audioFileUri,
				commandElements[0].toLowerCase().equals("true"),
				recordedFileUri,
				commandElements[1].toLowerCase().equals("true"),
				commandElements[2],
				Integer.parseInt(commandElements[3]),
				Integer.parseInt(commandElements[4]),
				Integer.parseInt(commandElements[5]),
				commandElements[6].charAt(0));
	}

	public String promptAndCollectDigits() {
        try {
    		String commandId  = mediaCallBean.promptAndCollectDigits(mediaCallId, dtmfCollectCommand);
    		commandIds.add(commandId);
    		return commandId;
        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            return sw.toString();
        }
	}

	public String promptAndRecord() {
        try {
    		String commandId  = mediaCallBean.promptAndRecord(mediaCallId, promptAndRecordCommand);
    		commandIds.add(commandId);
    		return commandId;
        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            return sw.toString();
        }
	}

	@Override
	public void terminateCall() {
		mediaCallBean.terminateMediaCall(callIds.get(activeCall-1));
	}

	public String waitForAnnouncementCompletedEvent() throws Exception {
		if(announcementCompletedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS))
			return announcementCompletedEvent.getCallId().equals(callIds.get(activeCall-1)) &&
				   commandIds.contains(announcementCompletedEvent.getMediaCommandId())
					? "OK" : announcementCompletedEvent.getCallId();
		return "No event";

	}

	public String waitForAnnouncementFailedEvent() throws Exception {
		if(announcementFailedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS))
			return announcementFailedEvent.getCallId().equals(callIds.get(activeCall-1)) &&
			       commandIds.contains(announcementFailedEvent.getMediaCommandId())
			       ? "OK" : announcementFailedEvent.getCallId();
		return "No event";

	}

	public String waitForAnnouncementTerminatedEvent() throws Exception {
		if(announcementTerminatedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS))
			return announcementTerminatedEvent.getCallId().equals(callIds.get(activeCall-1)) &&
			       commandIds.contains(announcementTerminatedEvent.getMediaCommandId())
			       ? "OK" : announcementTerminatedEvent.getCallId();
		return "No event";

	}

	public String waitForDigitsCollectedEvent() throws Exception {
		if(digitsCollectedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS))
			return digitsCollectedEvent.getCallId().equals(callIds.get(activeCall-1)) &&
				   commandIds.contains(digitsCollectedEvent.getMediaCommandId())
					? digitsCollectedEvent.getDigits() : digitsCollectedEvent.getCallId();
		return "No event";
	}

	public String waitForDigitsFailedEvent() throws Exception {
		if(digitsCollectionFailedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS))
			return digitsCollectionFailedEvent.getCallId().equals(callIds.get(activeCall-1)) &&
			commandIds.contains(digitsCollectionFailedEvent.getMediaCommandId())
			? digitsCollectionFailedEvent.getDigits() : digitsCollectionFailedEvent.getCallId();
			return "No event";
	}

	public String waitForDigitsTerminatedEvent() throws Exception {
		if(digitsCollectionTerminatedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS))
			return digitsCollectionTerminatedEvent.getCallId().equals(callIds.get(activeCall-1)) &&
			commandIds.contains(digitsCollectionTerminatedEvent.getMediaCommandId())
			? "OK" : digitsCollectionTerminatedEvent.getCallId();
			return "No event";
	}

	public String waitDtmfGenerationCompletedEvent() throws Exception {
		if(dtmfGenerationCompletedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS))
			return dtmfGenerationCompletedEvent.getCallId().equals(callIds.get(activeCall-1)) &&
				   commandIds.contains(dtmfGenerationCompletedEvent.getMediaCommandId())
					? "OK" : dtmfGenerationCompletedEvent.getCallId();
		return "No event";
	}

	public String waitDtmfGenerationFailedEvent() throws Exception {
		if(dtmfGenerationFailedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS))
			return dtmfGenerationFailedEvent.getCallId().equals(callIds.get(activeCall-1)) &&
				   commandIds.contains(dtmfGenerationFailedEvent.getMediaCommandId())
					? "OK" : dtmfGenerationFailedEvent.getCallId();
		return "No event";
	}

	public String waitPromptAndRecordCompletedEvent() throws Exception {
		if(promptAndRecordCompletedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS))
			return promptAndRecordCompletedEvent.getCallId().equals(callIds.get(activeCall-1)) &&
				   commandIds.contains(promptAndRecordCompletedEvent.getMediaCommandId())
					? "OK" : promptAndRecordCompletedEvent.getCallId();
		return "No event";
	}

	public String waitPromptAndRecordFailedEvent() throws Exception {
		if(promptAndRecordFailedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS))
			return promptAndRecordFailedEvent.getCallId().equals(callIds.get(activeCall-1)) &&
				   commandIds.contains(promptAndRecordFailedEvent.getMediaCommandId())
					? "OK" : promptAndRecordFailedEvent.getCallId();
		return "No event";
	}
	
	public String getPromptAndRecordFailedEvent() throws Exception {
		if (promptAndRecordFailedEvent.getCallId().equals(callIds.get(activeCall-1))) {
			return promptAndRecordFailedEvent.getRecordResult();
		}
		return "NOTFOUND";
	}

	public String waitPromptAndRecordTerminated() throws Exception {
		if(promptAndRecordTerminatedSemaphore.tryAcquire(waitTimeoutSeconds, TimeUnit.SECONDS))
			return promptAndRecordTerminatedEvent.getCallId().equals(callIds.get(activeCall-1)) &&
			commandIds.contains(promptAndRecordTerminatedEvent.getMediaCommandId())
			? "OK" : promptAndRecordTerminatedEvent.getCallId();
			return "No event";
	}

	public void onCallAnnouncementCompleted(CallAnnouncementCompletedEvent event) {
		this.announcementCompletedEvent = event;
		announcementCompletedSemaphore.release();
	}

	public void onCallAnnouncementFailed(CallAnnouncementFailedEvent event) {
		this.announcementFailedEvent = event;
		announcementFailedSemaphore.release();
	}

	public void onCallPromptAndCollectDigitsCompleted(CallPromptAndCollectDigitsCompletedEvent dtmfCollectDigitsCompletedEvent) {
    	this.digitsCollectedEvent = dtmfCollectDigitsCompletedEvent;
		digitsCollectedSemaphore.release();
    }

	public void onCallPromptAndCollectDigitsFailed(CallPromptAndCollectDigitsFailedEvent dtmfCollectDigitsFailedEvent) {
    	this.digitsCollectionFailedEvent = dtmfCollectDigitsFailedEvent;
		digitsCollectionFailedSemaphore.release();
    }

	public void onCallDtmfGenerationCompleted(CallDtmfGenerationCompletedEvent dtmfGenerationCompletedEvent) {
		this.dtmfGenerationCompletedEvent = dtmfGenerationCompletedEvent;
		dtmfGenerationCompletedSemaphore.release();
	}

	public void onCallDtmfGenerationFailed(CallDtmfGenerationFailedEvent dtmfGenerationFailedEvent) {
		this.dtmfGenerationFailedEvent = dtmfGenerationFailedEvent;
		dtmfGenerationFailedSemaphore.release();
	}

	public void onCallAnnouncementTerminated(CallAnnouncementTerminatedEvent announcementTerminatedEvent) {
		this.announcementTerminatedEvent = announcementTerminatedEvent;
		announcementTerminatedSemaphore.release();
	}

	public void onCallPromptAndCollectDigitsTerminated(CallPromptAndCollectDigitsTerminatedEvent dtmfCollectDigitsTerminatedEvent) {
		this.digitsCollectionTerminatedEvent = dtmfCollectDigitsTerminatedEvent;
		digitsCollectionTerminatedSemaphore.release();
	}

    public void onCallPromptAndRecordCompleted(CallPromptAndRecordCompletedEvent callPromptAndRecordCompletedEvent) {
		this.promptAndRecordCompletedEvent = callPromptAndRecordCompletedEvent;
		promptAndRecordCompletedSemaphore.release();
    }

    public void onCallPromptAndRecordFailed(CallPromptAndRecordFailedEvent callPromptAndRecordFailedEvent) {
		this.promptAndRecordFailedEvent = callPromptAndRecordFailedEvent;
		promptAndRecordFailedSemaphore.release();
    }

    public void onCallPromptAndRecordTerminated(CallPromptAndRecordTerminatedEvent callPromptAndRecordTerminatedEvent) {
        this.promptAndRecordTerminatedEvent = callPromptAndRecordTerminatedEvent;
        promptAndRecordTerminatedSemaphore.release();
    }

	public void maxDurationOfAudio(int maxDurationOfAudio) {
		this.maxDurationOfAudio = maxDurationOfAudio;
	}

	public boolean recordedDurationLessThanMaxDurationOfAudio(){
		return promptAndRecordCompletedEvent.getRecordingLengthMillis() <= (maxDurationOfAudio * 1000);
	}

	public boolean recordedDurationEqualMaxDurationOfAudio(){
		return promptAndRecordCompletedEvent.getRecordingLengthMillis() == (maxDurationOfAudio * 1000);
	}

    public String getMediaCallId() {
        return mediaCallId;
    }

    public void setMediaCallId(String mediaCallId) {
        this.mediaCallId = mediaCallId;
    }
}
