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
package com.bt.aloha.sipstone;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Collection;
import java.util.Vector;

public class SippCsvParser {
	public static Collection<Integer> initializeInterestedFieldNumbers() {
		Collection<Integer> fieldNumbers = new Vector<Integer>();
		for (int i=3; i<=6; i++)
			fieldNumbers.add(i);
		for (int i=9; i<=11; i++)
			fieldNumbers.add(i);
		for (int i=13; i<=16; i++)
			fieldNumbers.add(i);
		for (int i=41; i<=44; i++)
			fieldNumbers.add(i);
		return fieldNumbers;
	}
	
	public static void main(String args[]) throws Exception {
		Collection<Integer> interestedFields = initializeInterestedFieldNumbers();
		BufferedReader br = new BufferedReader(new FileReader(args[0]));
		BufferedWriter bw = new BufferedWriter(new FileWriter(args[1]));
		String line = null;
		while ((line = br.readLine()) != null) {
			System.out.println(line);
			String[] fields = line.split(";");
			StringBuffer outputLine = new StringBuffer();
			for (Integer i : interestedFields) {
				outputLine.append(String.format("%s,", fields[i]));
			}
			
			if (outputLine.length() > 0)
				outputLine.deleteCharAt(outputLine.length()-1);
			
			bw.write(outputLine.toString() + "\n");
		}
		br.close();
		bw.flush();
		bw.close();
	}
}
