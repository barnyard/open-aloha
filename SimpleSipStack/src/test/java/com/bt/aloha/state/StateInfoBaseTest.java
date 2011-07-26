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

 	

 	
 	
 
package com.bt.aloha.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Map;

import org.junit.Test;

import com.bt.aloha.call.state.CallInfo;
import com.bt.aloha.state.StateInfoBase;


public class StateInfoBaseTest {
	private StateInfoBase<Object> stateInfo = new StateInfoBase<Object>("beanName") {
		private static final long serialVersionUID = -8723065224834060764L;

		@Override
		public boolean isDead() {
			return false;
		}

        @Override
        public Map<String, Object> getTransients() {
            return null;
        }

        @Override
        public void setTransients(Map<String, Object> m) {
        }
	};

	// test returns 0 if call hasn't started yet
	@Test
    public void testGetDurationNullStartTime() {
        stateInfo.setStartTime(CallInfo.TIME_NOT_SET);
        assertEquals(0, stateInfo.getDuration());
    }

    // test for when call hasn't ended yet
	@Test
    public void testGetDurationNullEndTime() throws Exception{
		stateInfo.setStartTime(CallInfo.TIME_NOT_SET);
        long now = Calendar.getInstance().getTimeInMillis();
        stateInfo.setStartTime(now);
        stateInfo.setEndTime(CallInfo.TIME_NOT_SET);
        Thread.sleep(1100);
        int result = stateInfo.getDuration();
        assertTrue(result > 0);
    }

}
