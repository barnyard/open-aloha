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

import java.util.HashSet;
import java.util.Set;

public class BasicConferenceScenarioMultiStackExtractor extends
		ConferenceScenarioTerminateParticipantsExtractor {

	private Set<String> participantIds;

	public BasicConferenceScenarioMultiStackExtractor(String filename) {
		super(filename);
		participantIds = new HashSet<String>();
	}

	protected void processLine() throws Exception {
		porcessInitialStuff();
		processSipMessage();
		processLineWithParticipant();
	}

	protected void processSipMessage() throws Exception{
        if (line.contains("Sending request") || line.contains("Received response")){
            super.processSipMessage();
        }
	}

	private void porcessInitialStuff() {
		if (line.contains(scenarioId)) {
			log(line);
			if (confId == null) {
				String confIdString = "created for scenario ";
				if (line.contains(confIdString)) {
					int pos1 = line.indexOf("Conference ") + "Conference ".length();
					int pos2 = line.indexOf(" created", pos1);
					confId = line.substring(pos1, pos2);
					log(line);
				}
			}
		}
		if(confId!=null){
			if(line.contains(confId)){
				log(line);
			}
		}
	}

	protected void processLineWithParticipant() {
		String part = String.format("participant in conference %s is ", confId);
//		if (line.contains("First participant in conference"))
//			System.out.println(line);
		if (line.contains(part)) {
			System.out.println(line);
			int pos = line.indexOf(part) + part.length();
			int pos2 = line.indexOf("@", pos);
			String partId = line.substring(pos, pos2);
			participantIds.add(partId);
			System.out.println(partId);
		}

		for (String s : participantIds) {
			if (line.contains(s))
				log(line);
		}

	}

}
