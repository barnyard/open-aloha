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

import javax.sip.message.Request;

public class QueuedSipMessageKey implements Comparable<QueuedSipMessageKey> {
    private static final int PRIME = 31;
	private final Long sequenceNumber;
	private final String method;

	public QueuedSipMessageKey(Long aSequenceNumber, String aMethod) {
		if (aSequenceNumber == null) throw new IllegalArgumentException("Sequence number cannot be null");
		if (aMethod == null) throw new IllegalArgumentException("Method cannot be null");
		this.sequenceNumber = aSequenceNumber;
		this.method = aMethod;
	}

	public int compareTo(QueuedSipMessageKey other) {
		if (other == null) return 1;
		int result = this.sequenceNumber.compareTo(other.sequenceNumber);
		if (result != 0)
			return result;
		return weightRequest(this.method).compareTo(weightRequest(other.method));
	}

	private Integer weightRequest(String aMethod) {
		if (aMethod.equals(Request.CANCEL))
			return 1;
		if (aMethod.equals(Request.ACK))
			return 2;
		return 0;
	}

	public Long getSequenceNumber() {
		return sequenceNumber;
	}

	public String getMethod() {
		return method;
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = PRIME * result + ((method == null) ? 0 : method.hashCode());
		result = PRIME * result + ((sequenceNumber == null) ? 0 : sequenceNumber.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final QueuedSipMessageKey other = (QueuedSipMessageKey) obj;
		if (method == null) {
			if (other.method != null)
				return false;
		} else if (!method.equals(other.method))
			return false;
		if (sequenceNumber == null) {
			if (other.sequenceNumber != null)
				return false;
		} else if (!sequenceNumber.equals(other.sequenceNumber))
			return false;
		return true;
	}
}
