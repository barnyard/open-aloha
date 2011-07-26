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

 	

 	
 	
 
package com.bt.aloha.sipp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.AfterClass;

import com.bt.aloha.testing.SimpleSipStackPerClassTestCase;

public class SippTestHelper extends SimpleSipStackPerClassTestCase {

    public SippTestHelper() {
    }

	@AfterClass
	public static void fixtureTeardown() {
		// After the test is finished, terminate all running instances of sipp, if any.
		SippEngineTestHelper.cleanSippInstances();
	}

	protected void checkSippStatus( SippEngineTestHelper sipp ) {
		try {
			int result = sipp.waitForSippResult();
			assertEquals(sipp.getErrors(), 0, result );
		}
		catch ( InterruptedException e ) {
			fail( "Sipp scenario timed out." );
		}
	}
}
