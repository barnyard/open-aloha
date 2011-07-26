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

 	

 	
 	
 
package com.bt.aloha.media.testing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.collections.memory.InMemoryHousekeepingCollectionImpl;
import com.bt.aloha.media.conference.collections.ConferenceCollectionImpl;
import com.bt.aloha.media.conference.state.ConferenceInfo;

public class AlternateConcurrentUpdateFailureConferenceCollectionStub extends ConferenceCollectionImpl {
	private static final Log LOG = LogFactory.getLog(AlternateConcurrentUpdateFailureConferenceCollectionStub.class);

    public AlternateConcurrentUpdateFailureConferenceCollectionStub() {
        super(new InMemoryHousekeepingCollectionImpl<ConferenceInfo>());
    }

	@Override
	public void replace(ConferenceInfo conferenceInfo) {
		if(conferenceInfo.isFlipFlopForSimulatingConcurrencyFailures()) {
			ConferenceInfo dummyConferenceInfo = get(conferenceInfo.getId());
			dummyConferenceInfo.setFlipFlopForSimulatingConcurrencyFailures(false);
			LOG.debug(String.format("Faking update via dummy call id for call %s", conferenceInfo.getId()));
			super.replace(dummyConferenceInfo);
		} else {
			conferenceInfo.setFlipFlopForSimulatingConcurrencyFailures(true);
		}

		LOG.debug(String.format("Replacing callinfo %s (version %s, hash %s)", conferenceInfo.getId(), conferenceInfo.getVersionId(), conferenceInfo.hashCode()));
		super.replace(conferenceInfo);
	}
}
