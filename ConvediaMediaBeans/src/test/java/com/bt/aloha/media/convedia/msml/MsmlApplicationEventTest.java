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

 	

 	
 	
 
package com.bt.aloha.media.convedia.msml;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.bt.aloha.media.convedia.msml.model.MsmlApplicationEventType;

public class MsmlApplicationEventTest {

	// for Emma
	@Test
	public void testValueOf() {
		//assert
		assertEquals(MsmlApplicationEventType.MSML_DIALOG_EXIT, MsmlApplicationEventType.valueOf("MSML_DIALOG_EXIT"));

	}

	// for Emma
	@Test
	public void testValues() {
		//assert
		assertEquals(7, MsmlApplicationEventType.values().length);
	}

	// test that fromValue returns correctly for good value
	@Test
	public void testFromValueGood() {
		assertEquals(MsmlApplicationEventType.MSML_DIALOG_EXIT, MsmlApplicationEventType.fromValue("msml.dialog.exit"));
	}

	// test that fromValue returns UNKNOWN for bad value
	@Test
	public void testFromValueBad() throws Exception {
		assertEquals(MsmlApplicationEventType.UNKNOWN, MsmlApplicationEventType.fromValue("blahlah"));
	}
}
