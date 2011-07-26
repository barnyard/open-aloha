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
import java.util.ArrayList;
import java.util.List;

public abstract class ScenarioExtractor {
	protected String filename;
	protected Line line;
    protected BufferedReader br;
    protected boolean lineIslogged;
    protected List<List<Line>> sipMessages;

    protected ScenarioExtractor(String aFilename) {
        this.filename = aFilename;
        init();
    }

    protected void init() {
        this.sipMessages = new ArrayList<List<Line>>();
    }

    protected void log(List<Line> l) {
        log("--------------------------------------------------------------------------");
        for (Line s: l) {
        	log(s);
        }
        log("--------------------------------------------------------------------------");
    }

    protected void log(String s) {
        System.err.println(s);
    }

    protected void log(Line s) {
   		s.log();
    }

    protected void processSipMessage() throws Exception {
        String verb = "";
        if (line.contains("Sending "))
            verb = "Sending ";
        if (line.contains("Received "))
            verb = "Received ";

        String dateTime = line.split(" ")[0] + " " + line.split(" ")[1] + " " + line.split(" ")[2];

        line = new Line().setIfNotNull(br.readLine());
        boolean verbed = false;
        List<Line> sipMessage = new ArrayList<Line>();
        while (! line.startsWith("2007")) {
            if (line.trim().length() > 0) {
                if (verbed)
                    sipMessage.add(line);
                else {
                    sipMessage.add(new Line().setIfNotNull(dateTime + " " +  verb + "\n" + line));
                    verbed = true;
                }
            }
            line = new Line().setIfNotNull(br.readLine());
        }
        processLine();
        this.sipMessages.add(sipMessage);
    }

    public abstract void doit(String scenarioId) throws Exception;
    protected abstract void processLine() throws Exception;
}
