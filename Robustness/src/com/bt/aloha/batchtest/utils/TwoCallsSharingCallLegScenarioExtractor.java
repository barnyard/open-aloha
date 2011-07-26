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

 	

 	
 	
 
package com.bt.aloha.batchtest.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ad-hoc class to extract some details from app.log for the TwoCallsSharingCallLegScenario
 * @author 802083751
 *
 */
public class TwoCallsSharingCallLegScenarioExtractor extends ScenarioExtractor {
    private String scenarioId;
    private static String FIRST_CALLID_TARGET = "1st call initiated: ";
    private static String SECOND_CALLID_TARGET = "2nd call initiated: ";
    private String firstCallId;
    private String secondCallId;
    private Map<String, String[]> callToDialogs;

    public TwoCallsSharingCallLegScenarioExtractor(String filename) {
    	super(filename);
        init();
    }

    protected void init() {
        super.init();
        callToDialogs = new HashMap<String, String[]>();
        firstCallId = null;
        secondCallId = null;
    }

    public void doit(String scenarioId) throws Exception {
        init();
        this.scenarioId = scenarioId;
        br = new BufferedReader(new FileReader(this.filename));

        while (null != (line = new Line().setIfNotNull(br.readLine()))) {
            processLine();
        }

        br.close();

        log("--------------------------------------------------------------------------");
        if (null != this.firstCallId) {
            String c1d1 = this.callToDialogs.get(this.firstCallId)[0];
            String c1d2 = this.callToDialogs.get(this.firstCallId)[1];
            for (List<Line> l: this.sipMessages) {
                if (l.contains("Call-ID: " + c1d1)) {
                    log(l);
                }
                if (l.contains("Call-ID: " + c1d2)) {
                    log(l);
                }
            }
        }
        if (null != this.secondCallId) {
            String c2d1 = this.callToDialogs.get(this.secondCallId)[0];
            String c2d2 = this.callToDialogs.get(this.secondCallId)[1];
            for(List<Line> l: this.sipMessages) {
                if (l.contains("Call-ID: " + c2d1)) {
                    log(l);
                }
                if (l.contains("Call-ID: " + c2d2)) {
                    log(l);
                }
            }
        }
    }

    protected void processLine() throws Exception {
        if (line.trim().length() < 1) return;
        if (line.contains("conference")) return;
        if (line.contains("Retrieved info ")) return;
        boolean logged = false;

        if (line.contains(this.scenarioId)) {
            log(line);
            logged = true;
            if (line.contains(FIRST_CALLID_TARGET)) {
                firstCallId = line.substring(line.indexOf(FIRST_CALLID_TARGET) + FIRST_CALLID_TARGET.length());
                log("c1: " + firstCallId);
                if (this.callToDialogs.containsKey(firstCallId)) {
                    log("c1d1: " + this.callToDialogs.get(firstCallId)[0]);
                    log("c1d2: " + this.callToDialogs.get(firstCallId)[1]);
                }
            }
            if (line.contains(SECOND_CALLID_TARGET)) {
                secondCallId = line.substring(line.indexOf(SECOND_CALLID_TARGET) + SECOND_CALLID_TARGET.length());
                log("c2: " + secondCallId);
                if (this.callToDialogs.containsKey(secondCallId)) {
                    log("c2d1: " + this.callToDialogs.get(secondCallId)[0]);
                    log("c2d2: " + this.callToDialogs.get(secondCallId)[1]);
                }
            }
        }

        if (line.contains("Created call ") && line.contains("(ThirdPartyCallInfo")) {
            String callId = line.substring(line.indexOf("Created call ") + "Created call ".length(), line.indexOf("(ThirdPartyCallInfo") -1 );
            String dialogId1 = line.substring(line.indexOf("dialogs ") + "dialogs ".length(), line.indexOf(" and "));
            String dialogId2 = line.substring(line.indexOf(" and ") + " and ".length());
            this.callToDialogs.put(callId.trim(), new String[] { dialogId1.trim(), dialogId2.trim() });
        }

        if (line.contains("Created call ") && line.contains("(CallInfo")) {
            String callId = line.substring(line.indexOf("Created call ") + "Created call ".length(), line.indexOf("(CallInfo") -1 );
            String dialogId1 = line.substring(line.indexOf("dialogs ") + "dialogs ".length(), line.indexOf(" and "));
            String dialogId2 = line.substring(line.indexOf(" and ") + " and ".length());
            this.callToDialogs.put(callId.trim(), new String[] { dialogId1.trim(), dialogId2.trim() });
        }

        if (this.firstCallId != null && line.contains(this.firstCallId) && ! logged) {
            log(line);
            logged = true;
        }
        if (this.secondCallId != null && line.contains(this.secondCallId) && ! logged) {
            log(line);
            logged = true;
        }

        if (line.contains("Sending request") || line.contains("Received response")){
            processSipMessage();
        }
    }

    public static void main(String[] args) throws Exception {
//        new ScenarioExtractor(args[0]).doit(args[1]);
          new TwoCallsSharingCallLegScenarioExtractor("D:/Documents and Settings/802083751/Desktop/app.log").doit(
"twoCallsSharingCallLegScenario:cbd3f4f3ebfbe466ea046fdfb0bbbdfe");
    }
}



