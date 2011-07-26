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

 	

 	
 	
 
package com.bt.aloha.testing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.call.collections.CallCollectionImpl;
import com.bt.aloha.call.state.CallInfo;
import com.bt.aloha.collections.memory.InMemoryHousekeepingCollectionImpl;

public class AlternateConcurrentUpdateFailureCallCollectionStub extends CallCollectionImpl {
	private static final int FIVE = 5;
	private static final int NEGATIVE_ONE_HUNDRED = -100;
	private static final Log LOG = LogFactory.getLog(AlternateConcurrentUpdateFailureCallCollectionStub.class);

    public AlternateConcurrentUpdateFailureCallCollectionStub() {
        super(new InMemoryHousekeepingCollectionImpl<CallInfo>());
    }

	@Override
	public void replace(CallInfo callInfo) {
		if(callInfo.getMaxDurationInMinutes() > NEGATIVE_ONE_HUNDRED) {
			CallInfo dummyCallInfo = get(callInfo.getId());
			dummyCallInfo.setMaxDurationInMinutes(NEGATIVE_ONE_HUNDRED);
			LOG.debug(String.format("Faking update via dummy call id for call %s", callInfo.getId()));
			super.replace(dummyCallInfo);
		} else {
			callInfo.setMaxDurationInMinutes(FIVE);
		}

		LOG.debug(String.format("Replacing callinfo %s (version %s, hash %s)", callInfo.getId(), callInfo.getVersionId(), callInfo.hashCode()));
		super.replace(callInfo);
	}
}
