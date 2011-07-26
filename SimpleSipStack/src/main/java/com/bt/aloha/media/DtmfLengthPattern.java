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

 	

 	
 	
 
package com.bt.aloha.media;

public class DtmfLengthPattern extends DtmfPattern {
	private int length;
	public DtmfLengthPattern(int aLength){
		this(aLength, null);
	}

	public DtmfLengthPattern(int aLength, Character aCancelKey){
		super(aCancelKey);
		this.length = aLength;
	}

	public int getLength() {
		return this.length;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("length=" + getLength());
		if(getCancelKey() != null)
			sb.append((sb.length() > 0 ? SEMI_COLON : "") + "cancel=" + getCancelKey());
		return sb.toString();
	}
}
