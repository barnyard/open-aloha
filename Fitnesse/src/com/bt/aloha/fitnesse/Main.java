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

public class Main {

    public static void announcementAfterRestart() throws Exception{
        MediaCallPersistencyFixture fixture = new MediaCallPersistencyFixture();
        fixture.setIpAddressPattern("^10.*");
        fixture.setWaitTimeoutSeconds(10);
        fixture.setFirstPhoneUri("sip:hangup.1200000@^10.*:5660");
        fixture.audioFileUri("/provisioned/behave.wav");
        StringBuffer log = new StringBuffer();

        addToLog(log,fixture.startApplicationContext());
        addToLog(log,fixture.createFirstDialog());
        addToLog(log,fixture.createMediaCall());
        addToLog(log,fixture.waitForMediaCallConnectedEvent());
        addToLog(log,fixture.playAnnouncement());
        addToLog(log,fixture.waitForAnnouncementCompletedEvent());
        addToLog(log,fixture.destroyAndStartApplicationContext());
        addToLog(log,fixture.playAnnouncement());
        addToLog(log,fixture.waitForAnnouncementCompletedEvent());
        addToLog(log,fixture.terminateMediaCall());
        addToLog(log,fixture.waitForMediaCallTerminatedEventWithTerminatedByApplication());
        fixture.destroyPersistencyApplicationContext();

        System.err.println(log.toString());
    }

    private static void addToLog(StringBuffer sb, String s) {
        System.err.println(s);
        sb.append(s);
        sb.append("\n");
    }

    public static void main(String[] args) throws Exception {
        announcementAfterRestart();
        System.exit(0);
    }
}
