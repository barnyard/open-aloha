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

/**
 * Ad-hoc class to extract some details from app.log for the CallAnswerTimeoutScenario
 * @author 802083751
 */
public class BasicCallAndMediaCallSharedDialogScenarioExtractor extends ScenarioExtractor {
    private String scenarioId;
    private String c1;
    private String c2;
    private String c3;
    private SortedSet<String> linesToPrint;
    private int pass = 1;
    private String c1d1;
    private String c1d2;
    private String c2d1;
    private String c2d2;
    private String c3d1;
    private String c3d2;

    public BasicCallAndMediaCallSharedDialogScenarioExtractor(String filename) {
    	super(filename);
        init();
    }

    protected void init() {
        super.init();
        linesToPrint = new TreeSet<String>();
        c1 = null;
        c2 = null;
        c3 = null;
        c1d1 = null;
        c1d2 = null;
        c2d1 = null;
        c2d2 = null;
        c3d1 = null;
        c3d2 = null;
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

        log("--------------------------------------------------------------------------");
            for(List<Line> l: this.sipMessages) {
                inner:
                for(Line line: l) {
                    if ((null != this.c1d1 && line.contains(this.c1d1)) || (null != this.c1d2 && line.contains(this.c1d2))) {
                        log(l);
                        break inner;
                    }
                    if ((null != this.c2d1 && line.contains(this.c2d1)) || (null != this.c2d2 && line.contains(this.c2d2))) {
                        log(l);
                        break inner;
                    }
                    if ((null != this.c3d1 && line.contains(this.c3d1)) || (null != this.c3d2 && line.contains(this.c3d2))) {
                        log(l);
                        break inner;
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
                        if (null == c1) {
                            c1 = line.substring(line.indexOf("call:"));
                            c1 = c1.split(" ")[0];
                        } else if (null == c2) {
                            c2 = line.substring(line.indexOf("call:"));
                            c2 = c2.split(" ")[0];
                        }
                        if (line.contains("media call")) {
                            c3 = line.substring(line.indexOf("call:"));
                            c3 = c3.split(" ")[0];
                        }
                    }
                }
            }
        }

        if (pass == 2) {
            if (this.c1 != null && line.contains(this.c1) && !logged) {
                log(line);
                logged = true;
                if (line.contains("Joining dialogs ")) {
                    String dialogs = line.substring(line.indexOf("Joining dialogs ") + "Joining dialogs ".length());
                    c1d1 = dialogs.split(" ")[0];
                    c1d2 = dialogs.split(" ")[2];
                }
            }
            if (this.c2 != null && line.contains(this.c2) && !logged) {
                log(line);
                logged = true;
                if (line.contains("Joining dialogs ")) {
                    String dialogs = line.substring(line.indexOf("Joining dialogs ") + "Joining dialogs ".length());
                    c2d1 = dialogs.split(" ")[0];
                    c2d2 = dialogs.split(" ")[2];
                }
            }
            if (this.c3 != null && line.contains(this.c3) && !logged) {
                log(line);
                logged = true;
                if (line.contains("Joining dialogs ")) {
                    String dialogs = line.substring(line.indexOf("Joining dialogs ") + "Joining dialogs ".length());
                    c3d1 = dialogs.split(" ")[0];
                    c3d2 = dialogs.split(" ")[2];
                }
            }
            if (! logged && this.c1d1 != null && line.contains(this.c1d1) && ! line.contains("Call-ID")) {
                log(line);
                logged = true;
            }
            if (! logged && this.c1d2 != null && line.contains(this.c1d2) && ! line.contains("Call-ID")) {
                log(line);
                logged = true;
            }
            if (! logged && this.c2d1 != null && line.contains(this.c2d1) && ! line.contains("Call-ID")) {
                log(line);
                logged = true;
            }
            if (! logged && this.c2d2 != null && line.contains(this.c2d2) && ! line.contains("Call-ID")) {
                log(line);
                logged = true;
            }
            if (! logged && this.c3d1 != null && line.contains(this.c3d1) && ! line.contains("Call-ID")) {
                log(line);
                logged = true;
            }
            if (! logged && this.c3d2 != null && line.contains(this.c3d2) && ! line.contains("Call-ID")) {
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
//        new ScenarioExtractor(args[0]).doit(args[1]);
          new BasicCallAndMediaCallSharedDialogScenarioExtractor("app.log").doit(
"basicCallAndMediaCallSharedDialogScenario:fb941dac781c577a218672683494eb8d");
    }
}




