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

/**
 * Ad-hoc class to extract some details from app.log for the ConferenceScenarioTerminateParticipants
 * @author 802083751
 *
 */
public class ConferenceScenarioTerminateParticipantsExtractor extends ScenarioExtractor {
	protected String scenarioId;
    protected String confId;

    public ConferenceScenarioTerminateParticipantsExtractor(String filename) {
    	super(filename);
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
        if (null != this.confId) {
            for(List<Line> l: this.sipMessages) {
                inner:
                for(Line line: l) {
                    if (line.contains(this.confId)) {
                        log(l);
                        break inner;
                    }
                }
            }
        }
    }

    protected void processLine() throws Exception {
        if (line.trim().length() < 1) return;
        if (line.contains("Retrieved info ")) return;

        if (line.contains(this.scenarioId)) {
            log(line);
            if (line.contains("created for scenario")) {
                confId = line.substring(line.indexOf("Conference") + "Conference".length() + 1);
                confId = confId.substring(0, confId.indexOf(" "));
                log("confId: " + confId);
            }
        }
        if (this.confId != null && line.contains(this.confId)) {
            log(line);
        }
        if (line.contains("Sending request") || line.contains("Received response")){
            processSipMessage();
        }
    }

    public static void main(String[] args) throws Exception {
          new ConferenceScenarioTerminateParticipantsExtractor("app.log")
          	.doit("basicConferenceScenarioMultiStack:6a6320b19fc548ce9f72442cb9ae1111");
    }
}



