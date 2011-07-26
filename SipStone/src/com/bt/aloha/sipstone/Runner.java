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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.ssh.SSHExec;
import org.apache.tools.ant.taskdefs.optional.ssh.Scp;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.sdk.common.NetworkHelper;


public class Runner {
	private static ClassPathXmlApplicationContext ctx;
	private static final String DEF_SIPP_CSV_FILE = "sipp.csv";
	private static final String DEF_SIPP_HOST = "132.146.185.195";
	private static final String DEF_SIPP_RESULT_FILE_DIR = "/tmp";
	private static final String DEF_SIPP_RESULT_FILE = "sipp.out";
	private static final String DEF_MOCKPHONE_HOST = "^1[0|7|3].*";
	private static final String DEF_SIPP_COMMAND = "/opt/sipp/sipp";
	private static final String DEF_USERID = "ccuser";
	private static final String DEF_PASSWORD = "qwer1234";
	private static final String DEF_TMP_FILE = "sipp.output.txt";
	private static final String DEF_GRAPH_FILE2 = "sipstone2.jpg";
	private static final String DEF_GRAPH_FILE1 = "sipstone1.jpg";

	private static final String INBOUND2_XML_FILE = "inbound2.xml";
//	private static final String CALLEE_CSV_FILE = "callees_allBusy.csv";
	private static final String CALLEE_CSV_FILE = "callees.csv";

	private String sippCsvFile;
	private String sippHost;
	private String sippResultFileDir;
	private String sippResultFile;
	private String mockphoneHost;
	private String sippCommand;
	private String userid;
	private String password;
	private String tmpFile;
	private String graphFile1;
	private String graphFile2;

	private static final String SIPP_COMMAND_STRING = "%s %s -bg -sf %s/" + INBOUND2_XML_FILE + " -inf %s/" + CALLEE_CSV_FILE + " -rsa %s:6060 -r 18 -rate_increase 18 -fd 90s -rate_max 180 -trace_stat -stf %s/%s";

	private Properties startupProperties;

	public Runner(){
		String userName = System.getProperty("user.name");
		String resName = userName + ".startup.properties";
		startupProperties = new Properties();
		if(!loadProperties(startupProperties, resName)){
			throw new IllegalStateException("properties file " + resName+ " not found in the classpath");
		}
		Properties sipProperties = new Properties();
		loadProperties(sipProperties, "sip.properties");
		sippCsvFile = startupProperties.getProperty("SIPP_CSV_FILE", DEF_SIPP_CSV_FILE);
		sippHost = startupProperties.getProperty("SIPP_HOST", DEF_SIPP_HOST);
		sippResultFileDir = startupProperties.getProperty("SIPP_RESULT_FILE_DIR", DEF_SIPP_RESULT_FILE_DIR);
		sippResultFile = startupProperties.getProperty("SIPP_RESULT_FILE", DEF_SIPP_RESULT_FILE);
		try{
			mockphoneHost = NetworkHelper.lookupIpAddress(sipProperties.getProperty("sip.stack.ip.address.pattern", DEF_MOCKPHONE_HOST));
		}
		catch(Exception e){
			throw new IllegalStateException("Unable to initialize mockphone host property");
		}
		sippCommand = startupProperties.getProperty("SIPP_COMMAND", DEF_SIPP_COMMAND);
		userid = startupProperties.getProperty("USERID", DEF_USERID);
		password = startupProperties.getProperty("PASSWORD", DEF_PASSWORD);
		tmpFile = startupProperties.getProperty("TMP_FILE", DEF_TMP_FILE);
		graphFile1 = startupProperties.getProperty("GRAPH_FILE1", DEF_GRAPH_FILE1);
		graphFile2 = startupProperties.getProperty("GRAPH_FILE2", DEF_GRAPH_FILE2);

	}

	private static boolean loadProperties(Properties p, String resName){
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resName);
		try{
			System.err.println("Loading properites from  " + resName);
			p.load(is);
			return true;
		}
		catch(IOException e){
			System.err.println("Unable to load properties from startup.properties");
			e.printStackTrace();
		}
		return false;
	}

	public void startMockphones(final String[] args){
		Runnable r = new Runnable() {
			public void run() {
				ctx = com.bt.aloha.mockphones.Main.start(args);
			}
		};
		new Thread(r).start();
		System.err.println("Mockphones started");
	}

	private void stopMockphones() {
		if (null != ctx) {
			ctx.destroy();
			System.err.println("Mockphones stopped");
		}
	}

	public void copyResourcesToSippBox(){
		putLocalFile(INBOUND2_XML_FILE);
		putLocalFile(CALLEE_CSV_FILE);
	}

	public void runSipp() {
		SSHExec t = new SSHExec();
		t.setHost(startupProperties.getProperty("SIPP_HOST", DEF_SIPP_HOST));
		t.setUsername(startupProperties.getProperty("USERID", DEF_USERID));
		t.setPassword(startupProperties.getProperty("PASSWORD", DEF_PASSWORD));

		String command = String.format(SIPP_COMMAND_STRING,
				sippCommand, sippHost, sippResultFileDir, sippResultFileDir,
				mockphoneHost, sippResultFileDir, sippResultFile);
		System.err.println(command);
		t.setCommand(command);
		t.setTrust(true);
		try {
			t.execute();
		} catch (RuntimeException e) {
			System.err.println(e.getMessage());
			if (! e.getMessage().contains("status 99"))
				throw e;
		}
		System.err.println("SIPp started on " + sippHost);
	}

	private boolean sippFinished() throws Exception {
		System.err.println("------------------------");
		new File(tmpFile).delete();
		SSHExec t = new SSHExec();
		t.setHost(sippHost);
		t.setUsername(userid);
		t.setPassword(password);
		t.setOutput(new File(tmpFile));
		t.setCommand("ps -ef|grep \"" + sippCommand + "\"|grep " + INBOUND2_XML_FILE);
		t.setTrust(true);
		t.execute();
		BufferedReader br = new BufferedReader(new FileReader(tmpFile));
		String line;

		while (null != (line = br.readLine())) {
			System.err.println(line);
			if (line.contains("bash")) continue;
			return false;
		}
		br.close();

		return true;
	}

	private void waitForSippToFinish() throws Exception {
		while(! sippFinished()) {
			Thread.sleep(60000);
		}
	}

	private void putLocalFile(String fileName) {
		Scp scp = new Scp();
		scp.setTrust(true);
		scp.setLocalFile(fileName);
		scp.setRemoteTofile(userid + ":" + password + "@" + sippHost + ":" + sippResultFileDir + "/" + fileName);
		scp.setVerbose(true);
		Project p = new Project();
		scp.setProject(p);
		p.setBaseDir(new File("."));
		scp.execute();
		System.err.println("Copied " + fileName + " to " + sippHost);
	}


	private void getRemoteFile() {
		new File(sippResultFile).delete();
		Scp scp = new Scp();
		scp.setTrust(true);
		scp.setRemoteFile(userid + ":" + password + "@" + sippHost + ":" + sippResultFileDir + "/" + sippResultFile);
		scp.setTodir(".");
		scp.setVerbose(true);
		Project p = new Project();
		scp.setProject(p);
		p.setBaseDir(new File("."));
		scp.execute();
		System.err.println("Copied SIPp results back from " + sippHost);
	}

	private void formatFile() throws Exception {
		new File(sippCsvFile).delete();
		SippCsvParser.main(new String[]{ sippResultFile, sippCsvFile});
		System.err.println(String.format("%s converted to %s", sippResultFile, sippCsvFile));
	}

	public void createGraph(){
		new File(graphFile1).delete();
		new File(graphFile2).delete();
		GenGraph g = new GenGraph(sippCsvFile);
		g.saveCharts(new File(graphFile1), new File(graphFile2));
		System.err.println(graphFile1 + " and " + graphFile2 + " created");
	}
	public void clear(){
		new File(tmpFile).delete();
		new File(sippResultFile).delete();
	}

	private void clearDB() throws Exception {
		System.err.println("clearing DB tables");
		// make sure that the params in the
		// mantainance dao match the ones in the
		// datasource app ctx and ha jdbc config
		MaintenanceDao dao = new MaintenanceDao("org.postgresql.Driver", "jdbc:postgresql://radon190.nat.bt.com:5432/springringha1", "springringuser", "springringuser");
		dao.truncateAllTables();
		dao = new MaintenanceDao("org.postgresql.Driver", "jdbc:postgresql://radon195.nat.bt.com:5432/springringha2", "springringuser", "springringuser");
		dao.truncateAllTables();
	}

	public static void main(String[] args) throws Exception {
		Runner r = new Runner();
		try {
			if (args.length == 1 && args[0].contains("database")) {
				r.clearDB();
			}
			System.err.println("Starting Mockphones stack with args: " + Arrays.toString(args));
			r.startMockphones(args);

			System.err.println("Copying resource files to remote sipp box");
			r.copyResourcesToSippBox();

			System.err.println("Waiting 5 seconds for the copy to happen");
			Thread.sleep(5000);
			System.gc();
			System.err.println("Running Sipp");
			r.runSipp();

			System.err.println("Waiting for sipp to finish");
			r.waitForSippToFinish();

			System.err.println("Stopping Mockphones stack");
			r.stopMockphones();

			System.err.println("Getting remote Sipp result file");
			r.getRemoteFile();

			System.err.println("Parsing the file and generating the graph");
			r.formatFile();
			r.createGraph();

			System.exit(0);
		} catch (Exception e){
			System.err.println("Exception caught ");
			e.printStackTrace();
		} finally {
			r.clear();
			System.exit(1);
		}
	}
}
