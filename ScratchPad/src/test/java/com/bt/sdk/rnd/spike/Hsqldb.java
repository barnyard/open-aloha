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
package com.bt.sdk.rnd.spike;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class Hsqldb {

	private static String JAVA="java";
	private static String HSQLDB_CP="-cp lib/hsqldb/hsqldb.jar";
	private static String HSQLDB_ADDRESS="-address %2$s";
	private static String HSQLDB_PORT="-port %3$s";
	private static String HSQLDB_SERVER_CLASS="org.hsqldb.Server";
	private static String DATABASE="-database.%1$s mem:xdb%1$s";
	private static String DATABASE_NAME="-dbname.%1$s xdb%1$s";
	private static String HSQLDB_FLAGS="-silent false -trace true";
	private static String FORMAT = String.format("%s %s %s %s %s %s %s %s", JAVA, HSQLDB_CP, HSQLDB_SERVER_CLASS, HSQLDB_ADDRESS, HSQLDB_PORT, DATABASE, DATABASE_NAME, HSQLDB_FLAGS);

	private Process p;
	private String id;
	private String address;
	private String port;
	private Dao dao;

	public Hsqldb(Dao dao, String id, String ip, String port){
		this.id = id;
		this.address = ip;
		this.port = port;
		this.dao = dao;
	}

	public void start(){
		String command = String.format(FORMAT, id, address, port);
		String[] args = command.split(" ");
		List<String> argsList = Arrays.asList(args);
		ProcessBuilder b = new ProcessBuilder(argsList);
		b.redirectErrorStream();
		try{
			p = b.start();
			Runnable run = new Runnable(){
				public void run() {
					int count=0;
					while(count<20) {
						dumpInoutAndErrorStreams();
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
						}
						count ++;
					}
				}
			};
		} catch(IOException e){
			e.printStackTrace();
			throw new IllegalStateException("Unable to start hsqldb\n" + command);
		}
	}

	public void stop(){
		dao.shutdown();
	}

	public void dumpInoutAndErrorStreams(){
		if(p!=null){
			dump(p.getInputStream());
			dump(p.getErrorStream());
		}
	}

	private static void dump(InputStream is){
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		String line = "";
		int count = 0;
		while(null!=line){
			try{
				line = in.readLine();
				System.out.println(line);
			}
			catch(IOException e){
				System.err.println("Unable to read line " + count);
			}
			count++;
		}
	}
}
