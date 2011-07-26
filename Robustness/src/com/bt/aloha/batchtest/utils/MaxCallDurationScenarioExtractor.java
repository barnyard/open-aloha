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
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class MaxCallDurationScenarioExtractor extends ScenarioExtractor {
    private String scenarioId;
    private String callId;
    private SortedSet<String> linesToPrint;
    private int pass = 1;
    private String d1;
    private String d2;

    public MaxCallDurationScenarioExtractor(String filename) {
    	super(filename);
        init();
    }

    protected void init() {
        super.init();
        linesToPrint = new TreeSet<String>();
        callId = null;
        d1 = null;
        d2 = null;
        pass = 1;
    }

    public void doit(String scenarioId) throws Exception {
        init();
        this.scenarioId = scenarioId;
        br = new BufferedReader(new FileReader(this.filename));
        while (null != (line = new Line().setIfNotNull(br.readLine()))) {
            processLine();
        }
        br.close();

        pass = 2;
        br = new BufferedReader(new FileReader(this.filename));
        while (null != (line = new Line().setIfNotNull(br.readLine()))) {
            processLine();
        }
        br.close();

        for (String line: this.linesToPrint) {
            log(line);
        }

        System.err.println("--------------------------------------------------------------------------");
        if (null != this.callId) {
            for(List<Line> l: this.sipMessages) {
                inner:
                for ( Line line: l) {
                    if (null != this.d1 && line.contains(this.d1) || null != this.d2 && line.contains(this.d2)) {
                        log(l);
                        break inner;
                    }
                }
            }
        }
    }

    protected void processLine() throws Exception {
        if (line.trim().length() < 1) return;
        if (line.contains("conference")) return;
        if (line.contains("Retrieved info ")) return;
        boolean logged = false;

        if (pass == 1) {
            if (line.contains(this.scenarioId)) {
                log(line);
                logged = true;
                if (pass == 1) {
                    if (line.contains("started for scenario") && line.contains("call:")) {
                        callId = line.substring(line.indexOf("call:"));
                        callId = callId.split(" ")[0];
                    }
                }
            }
        }

        if (pass == 2) {
            if (this.callId != null && line.contains(this.callId) && !logged) {
                log(line);
                logged = true;
                if (line.contains("Joining dialogs ")) {
                    String dialogs = line.substring(line.indexOf("Joining dialogs ") + "Joining dialogs ".length());
                    d1 = dialogs.split(" ")[0];
                    d2 = dialogs.split(" ")[2];
                    log("d1: " + d1 + ", d2: " + d2);
                }
            }
            if (! logged && this.d1 != null && line.contains(this.d1) && ! line.contains("Call-ID")) {
                log(line);
                logged = true;
            }
            if (! logged && this.d2 != null && line.contains(this.d2) && ! line.contains("Call-ID")) {
                log(line);
                logged = true;
            }
        }

        if (pass == 1) {
            if (line.contains("Sending request") || line.contains("Received response")){
                processSipMessage();
            }
        }
    }

    public static void main(String[] args) throws Exception {
//      new ScenarioExtractor(args[0]).doit(args[1]);
          new MaxCallDurationScenarioExtractor("app.log").doit(
"maxCallDurationScenario:1385c647cf57df4842020040a8b0cbc7");
    }
}



