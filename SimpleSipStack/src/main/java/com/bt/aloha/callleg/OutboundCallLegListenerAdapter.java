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
package com.bt.aloha.callleg;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.callleg.event.AbstractCallLegEvent;
import com.bt.aloha.callleg.event.CallLegAlertingEvent;
import com.bt.aloha.dialog.event.AbstractDialogEvent;
import com.bt.aloha.dialog.event.DialogAlertingEvent;
import com.bt.aloha.dialog.outbound.OutboundDialogSipListener;

public class OutboundCallLegListenerAdapter extends CallLegListenerAdapter implements OutboundDialogSipListener {
	private static final Log LOG = LogFactory.getLog(OutboundCallLegListenerAdapter.class);
	public OutboundCallLegListenerAdapter(OutboundCallLegListener listener) {
		super(listener);
	}

	public void onDialogAlerting(DialogAlertingEvent alertingEvent) {
		LOG.debug(String.format("Delivering %s event to %s", alertingEvent.getClass().toString(), this.listener.getClass().toString()));
		((OutboundCallLegListener)this.listener).onCallLegAlerting(new CallLegAlertingEvent(alertingEvent));
	}

	@Override
	protected AbstractCallLegEvent mapDialogEventToCallLegEvent(AbstractDialogEvent event) {
		if (event instanceof DialogAlertingEvent)
			return new CallLegAlertingEvent((DialogAlertingEvent)event);
		return super.mapDialogEventToCallLegEvent(event);
	}

}
