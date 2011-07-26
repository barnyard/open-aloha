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

 	

 	
 	
 
package com.bt.aloha.batchtest.v2;

public class ResultTotals {
	private long successes = 0;
	private long failures = 0;

	public long getSuccesses() {
		return successes;
	}

	public long getFailures() {
		return failures;
	}

	public double percSuccess() {
		return perc(successes);
	}

	public double percFailures() {
		return perc(failures);
	}

	public long getTotal() {
		return successes + failures;
	}

	protected double perc(long what) {
		if (getTotal() != 0)
			return (double) what * 100.0 / getTotal();
		return 0.0;
	}

	public void incSuccesses() {
		successes++;
	}

	public void incFailures() {
		failures++;
	}

	public String toString(){
		return String.format("T:%s,S:%s,F:%s,S%%:%s,F%%:%s", getTotal(), getSuccesses(), getFailures(), percSuccess(), percFailures());
	}

}
