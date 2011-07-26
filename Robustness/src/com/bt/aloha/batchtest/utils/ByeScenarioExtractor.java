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

public class ByeScenarioExtractor extends	ScenarioExtractor {

	private String scenarioId;
	private String callId;
	private String d1;
	private String d2;
	private int pass = 1;

	public ByeScenarioExtractor(String filename) {
		super(filename);
	}

	@Override
	public void doit(String scenarioId) throws Exception {
		init();
		pass = 1;
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

	@Override
	protected void processLine() throws Exception {
		if (pass == 1) {
			if (line.contains(this.scenarioId)) {
				//line.log();
				if (this.callId == null) {
					String target = "call call:";
					if (line.contains(target)) {
						this.callId = line.substring(line.indexOf(target) + target.length() - 5);
						this.callId = this.callId.substring(0, this.callId.indexOf(" "));
						log("callId: " + this.callId);
					}
				}
			}
			
			if (null != this.callId && line.contains(this.callId))
			{
				//line.log();
				if (null == d1) {
					if (line.contains("Set first leg ")) {
						this.d1 = line.substring(line.indexOf("leg (") + 5);
						this.d1 = this.d1.substring(0, this.d1.indexOf(")"));
						log("d1: " + this.d1);
					}
				}
				if (null == d2) {
					if (line.contains("Set second leg ")) {
						this.d2 = line.substring(line.indexOf("leg (") + 5);
						this.d2 = this.d2.substring(0, this.d2.indexOf(")"));
						log("d2: " + this.d2);
					}
				}
			}
		}
		
		if (pass == 2) {
			if (line.contains(this.scenarioId))
				line.log();
			if (null != this.callId && line.contains(this.callId)) 
				line.log();
			if (null != this.d1 && line.contains(this.d1) && ! line.startsWith("Call-ID")) 
				line.log();
			if (null != this.d2 && line.contains(this.d2) && ! line.startsWith("Call-ID")) 
				line.log();
		
			if (line.contains("Sending request") || line.contains("Received response") || line.contains("Sending response") || line.contains("Received request")){
				processSipMessage();
			}
        }
	}
}
