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

 	

 	
 	
 
package com.bt.aloha.media.event.call;

/**
 * Event fired when an announcement has completed
 */
public class CallAnnouncementCompletedEvent extends AbstractMediaCallCommandEvent {
	private String duration;
    private boolean barged;

	/**
     * Constructor
     * @param aCallId the call Id
     * @param aMediaCommandId the media command Id
     * @param aDuration the duration in seconds
     * @param isBarged whether the announcement was barged
	 */
    public CallAnnouncementCompletedEvent(String aCallId, String aMediaCommandId, String aDuration, boolean isBarged) {
		super(aCallId, aMediaCommandId);
		this.duration = aDuration;
        this.barged = isBarged;
	}

    /**
     * return the duration in seconds
     * @return the duration in seconds
     */
	public String getDuration() {
		return duration;
	}

    /**
     * return whether the announcement was barged
     * @return whether the announcement was barged
     */
    public boolean getBarged() {
        return this.barged;
    }
}
