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

 	

 	
 	
 
package com.bt.aloha.stack;

import static org.junit.Assert.assertEquals;

import javax.sip.message.Request;

import org.junit.Test;

import com.bt.aloha.stack.QueuedSipMessageKey;

public class QueuedSipMessageKeyTest {

	@Test
	public void testConstructorAndGetters() throws Exception {
		//setup
		QueuedSipMessageKey k1 = new QueuedSipMessageKey(4L, Request.ACK);

		//act/assert
		assertEquals(4L, k1.getSequenceNumber());
		assertEquals(Request.ACK, k1.getMethod());
	}

	// Compare keys with different seq num (smaller to bigger)
	@Test
	public void testCompareDifferentSeqNumSmallerToBigger() throws Exception {
		//setup
		QueuedSipMessageKey k1 = new QueuedSipMessageKey(4L, Request.ACK);
		QueuedSipMessageKey k2 = new QueuedSipMessageKey(5L, Request.ACK);

		//act/assert
		assertEquals(-1, k1.compareTo(k2));
	}

	// Compare keys with different seq num (bigger to smaller)
	@Test
	public void testCompareDifferentSeqNumBiggerToSmaller() throws Exception {
		//setup
		QueuedSipMessageKey k1 = new QueuedSipMessageKey(5L, Request.ACK);
		QueuedSipMessageKey k2 = new QueuedSipMessageKey(4L, Request.ACK);

		//act/assert
		assertEquals(1, k1.compareTo(k2));
	}

	// Compare keys with the same seq num and method
	@Test
	public void testCompareTheSame() throws Exception {
		//setup
		QueuedSipMessageKey k1 = new QueuedSipMessageKey(5L, "ACK");
		QueuedSipMessageKey k2 = new QueuedSipMessageKey(5L, "ACK");

		//act/assert
		assertEquals(0, k1.compareTo(k2));
	}

	// Compare keys with the same seq num and method of the same weight
	@Test
	public void testCompareMethodOfTheSameWeight() throws Exception {
		//setup
		QueuedSipMessageKey k1 = new QueuedSipMessageKey(5L, "INVITE");
		QueuedSipMessageKey k2 = new QueuedSipMessageKey(5L, "BYE");

		//act/assert
		assertEquals(0, k1.compareTo(k2));
	}

	// Compare INVITE and ACK with the same seq num
	@Test
	public void testCompareInviteAndAck() throws Exception {
		//setup
		QueuedSipMessageKey k1 = new QueuedSipMessageKey(5L, "INVITE");
		QueuedSipMessageKey k2 = new QueuedSipMessageKey(5L, "ACK");

		//act/assert
		assertEquals(-1, k1.compareTo(k2));
	}

	// Compare ACK and INVITE with the same seq num
	@Test
	public void testCompareAckAndInvite() throws Exception {
		//setup
		QueuedSipMessageKey k1 = new QueuedSipMessageKey(5L, "ACK");
		QueuedSipMessageKey k2 = new QueuedSipMessageKey(5L, "INVITE");

		//act/assert
		assertEquals(1, k1.compareTo(k2));
	}

	// Compare INVITE and ACK with the same seq num
	@Test
	public void testCompareInviteAndCancel() throws Exception {
		//setup
		QueuedSipMessageKey k1 = new QueuedSipMessageKey(5L, "INVITE");
		QueuedSipMessageKey k2 = new QueuedSipMessageKey(5L, "CANCEL");

		//act/assert
		assertEquals(-1, k1.compareTo(k2));
	}

	// Compare ACK and INVITE with the same seq num
	@Test
	public void testComparCancelAndInvite() throws Exception {
		//setup
		QueuedSipMessageKey k1 = new QueuedSipMessageKey(5L, "CANCEL");
		QueuedSipMessageKey k2 = new QueuedSipMessageKey(5L, "INVITE");

		//act/assert
		assertEquals(1, k1.compareTo(k2));
	}

	// Compare ACK and CANCEL with the same seq num
	@Test
	public void testCompareAckAndCancel() throws Exception {
		//setup
		QueuedSipMessageKey k1 = new QueuedSipMessageKey(5L, "ACK");
		QueuedSipMessageKey k2 = new QueuedSipMessageKey(5L, "CANCEL");

		//act/assert
		assertEquals(1, k1.compareTo(k2));
	}

	// Compare CANCEL and ACK with the same seq num
	@Test
	public void testCompareCancelAndAck() throws Exception {
		//setup
		QueuedSipMessageKey k1 = new QueuedSipMessageKey(5L, "CANCEL");
		QueuedSipMessageKey k2 = new QueuedSipMessageKey(5L, "ACK");

		//act/assert
		assertEquals(-1, k1.compareTo(k2));
	}

	//
	@Test
	public void testCompareWithNull() throws Exception {
		//setup
		QueuedSipMessageKey k1 = new QueuedSipMessageKey(5L, "CANCEL");

		//act/assert
		assertEquals(1, k1.compareTo(null));
	}

	// Test that we throw an exception when null past to constructor as seq num
	@Test(expected=IllegalArgumentException.class)
	public void testConstructorNullSeqNum() throws Exception {
		//setup
		new QueuedSipMessageKey(null, Request.ACK);
	}

	// Test that we throw an exception when null past to constructor as seq num
	@Test(expected=IllegalArgumentException.class)
	public void testConstructorNullMethod() throws Exception {
		//setup
		new QueuedSipMessageKey(5L, null);
	}
}
