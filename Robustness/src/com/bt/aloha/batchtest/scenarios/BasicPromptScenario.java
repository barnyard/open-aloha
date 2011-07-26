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

 	

 	
 	
 
package com.bt.aloha.batchtest.scenarios;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.batchtest.BatchTestScenarioBase;
import com.bt.aloha.batchtest.Resetable;
import com.bt.aloha.call.CallListener;
import com.bt.aloha.call.event.AbstractCallEvent;
import com.bt.aloha.call.event.CallConnectionFailedEvent;
import com.bt.aloha.call.event.CallDisconnectedEvent;
import com.bt.aloha.call.event.CallTerminatedEvent;
import com.bt.aloha.call.event.CallTerminationFailedEvent;
import com.bt.aloha.media.MediaCallListener;
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

public abstract class BasicPromptScenario extends BatchTestScenarioBase implements CallListener, MediaCallListener, Resetable {
    protected final Log log = LogFactory.getLog(this.getClass());
	private String promptEndpoint;
	
	protected abstract Object processEvent(AbstractCallEvent arg0);

	public void onCallConnectionFailed(CallConnectionFailedEvent arg0) {
		processEvent(arg0);
	}

	public void onCallDisconnected(CallDisconnectedEvent arg0) {
		processEvent(arg0);
	}

	public void onCallTerminated(CallTerminatedEvent arg0) {
		processEvent(arg0);
	}

	public void onCallTerminationFailed(CallTerminationFailedEvent arg0) {
		processEvent(arg0);
	}

	public void onCallAnnouncementCompleted(CallAnnouncementCompletedEvent arg0) {
		processEvent(arg0);
	}

	public void onCallAnnouncementFailed(CallAnnouncementFailedEvent arg0) {
		processEvent(arg0);
	}

    public void onCallPromptAndCollectDigitsCompleted(CallPromptAndCollectDigitsCompletedEvent arg0) {
		processEvent(arg0);
    }

    public void onCallPromptAndCollectDigitsFailed(CallPromptAndCollectDigitsFailedEvent arg0) {
		processEvent(arg0);
    }

	public void onCallDtmfGenerationCompleted(CallDtmfGenerationCompletedEvent arg0) {
		processEvent(arg0);
	}

	public void onCallDtmfGenerationFailed(CallDtmfGenerationFailedEvent arg0) {
		processEvent(arg0);
	}

	public void onCallAnnouncementTerminated(CallAnnouncementTerminatedEvent arg0) {
		processEvent(arg0);
	}

	public void onCallPromptAndCollectDigitsTerminated(CallPromptAndCollectDigitsTerminatedEvent arg0) {
		processEvent(arg0);
	}

    public void onCallPromptAndRecordCompleted(CallPromptAndRecordCompletedEvent arg0) {
		processEvent(arg0);
    }

    public void onCallPromptAndRecordFailed(CallPromptAndRecordFailedEvent arg0) {
		processEvent(arg0);
    }

    public void onCallPromptAndRecordTerminated(CallPromptAndRecordTerminatedEvent arg0) {
		processEvent(arg0);
    }
	
	protected URI getPromptEndpointUri() {
        return URI.create(this.promptEndpoint);
	}

	public String getPromptEndpoint() {
        return this.promptEndpoint;
	}

	public void setPromptEndpoint(String promptEndpoint) {
		this.promptEndpoint = promptEndpoint;
	}

    public abstract void reset();

    public void sleep(long ms){
    	try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			log.warn("Sleep interrupted!");
		}
    }
}
