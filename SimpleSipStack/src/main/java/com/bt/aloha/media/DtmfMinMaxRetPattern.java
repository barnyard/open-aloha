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

public class DtmfMinMaxRetPattern extends DtmfPattern {
	private final int minDigits;
	private final int maxDigits;
	private final Character returnKey;

	public DtmfMinMaxRetPattern(int aMinDigits, int aMaxDigits) {
		this(aMinDigits, aMaxDigits, null, null);
	}

	public DtmfMinMaxRetPattern(int aMinDigits, int aMaxDigits, Character aReturnKey, Character aCancelKey) {
		super(aCancelKey);
		this.minDigits = aMinDigits;
		this.maxDigits = aMaxDigits;
		this.returnKey = aReturnKey;
	}

	public int  getMinDigits() {
		return minDigits;
	}

	public int  getMaxDigits() {
		return maxDigits;
	}

	public Character getReturnKey() {
		return returnKey;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(minDigits > 0)
			sb.append("min=" + minDigits);
		if(maxDigits > 0 && maxDigits >= minDigits)
			sb.append((sb.length() > 0 ? SEMI_COLON : "") + "max=" + maxDigits);
		if(!(returnKey == null))
			sb.append((sb.length() > 0 ? SEMI_COLON : "") + "rtk=" + returnKey);
		if(getCancelKey() != null)
			sb.append((sb.length() > 0 ? SEMI_COLON : "") + "cancel=" + getCancelKey());
		return sb.toString();
	}
}
