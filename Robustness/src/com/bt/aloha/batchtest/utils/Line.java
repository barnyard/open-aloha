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

 	

 	
 	
 
package com.bt.aloha.batchtest.utils;

public class Line {
	private String line;
	private boolean logged;

	public Line(){
	}

	public String getLine() {
		return line;
	}

	public boolean isLogged() {
		return logged;
	}

	private void setLogged(boolean logged) {
		this.logged = logged;
	}

	public String trim(){
		return line.trim();
	}

	public boolean contains(CharSequence containee){
		if(line==null || containee == null){
			return false;
		}
		return line.contains(containee);
	}

	public void log(){
		if(!isLogged()){
			setLogged(true);
			System.err.println(line);
		}
	}

	public String[] split(String regex){
		return line.split(regex);
	}

	public boolean startsWith(String string){
		return line.startsWith(string);
	}

	@Override
	public String toString() {
		return line.toString();
	}

	public int indexOf(String ch) {
		return line.indexOf(ch);
	}

	public String substring(int i) {
		return line.substring(i);
	}

	public String substring(int i, int j) {
		return line.substring(i, j);
	}

	public Line setIfNotNull(String readLine) {
		if(readLine==null)
			return null;
		line = readLine;
		return this;
	}

	public int indexOf(String string, int fromIndex) {
		return line.indexOf(string, fromIndex);
	}

}
