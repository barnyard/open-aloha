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

 	

 	
 	
 
package com.bt.aloha.testing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SimpleSipStackLogEnhancer {
	private static final Log LOG = LogFactory.getLog(SimpleSipStackLogEnhancer.class);
    private static final String BLACK = "ffffff";
    private static final String WHITE = "000000";
    private static final String CYAN = "#99FFFF";
    private static final String GREEN = "#99FF99";
    private static final String YELLOW = "#FFFF99";
    private static final String MAGENTA = "#FFBBFF";
    private static final String BLUE = "#BBBBFF";
    private static final String SPACE = " ";
	private static final String DOCUMENT_TEMPLATE_HEADER =
		"<html>\n" +
			"<head>\n" +
				"<title>Build file @FILENAME@</title>\n" +
			"</head>\n" +
		"<body>\n" +
			"<code>\n";

	private static final String DOCUMENT_TEMPLATE_FOOTER =
			"</code>\n" +
		"</body>\n" +
		"</html>";

	private Map<String,String> callIdColourMap = new Hashtable<String,String>();
	private List<String> colours = new Vector<String>();
	private Map<String,String> logLevelColourMap = new Hashtable<String, String>();
	private String sourceFilename;
	private String targetFilename;

	public SimpleSipStackLogEnhancer(String aSourceFilename, String aTargetFilename) {
		logLevelColourMap.put("DEBUG", "#EEFFFF");
		logLevelColourMap.put("INFO", "#E8E8FF");
		logLevelColourMap.put("WARN", "#FFCCAA");
		logLevelColourMap.put("ERROR", "#FFAAAA");
		logLevelColourMap.put("FATAL", "#FF2222");

		colours.add(CYAN);
		colours.add(GREEN);
		colours.add(YELLOW);
		colours.add(MAGENTA);
		colours.add(BLUE);


		this.sourceFilename = aSourceFilename;
		this.targetFilename = aTargetFilename;
	}

	public void allocateColoursToDialogs() throws IOException {
		BufferedReader in = null;
		try {
        	in = new BufferedReader(new FileReader(sourceFilename));
        	String line;
	        while ((line = in.readLine()) != null) {
	            allocateColourToCallId(line);
	        }
		} finally {
			if(in != null)
				in.close();
		}
	}

	private void allocateColourToCallId(String line) {
		Pattern pattern = Pattern.compile("^Call-ID:\\s(.+)$");
		Matcher matcher = pattern.matcher(line);
		if(matcher.find()) {
	    	String callId = matcher.group(1);
	    	if(callIdColourMap.get(callId) == null) {
		        String colour = colours.remove(0);
		        callIdColourMap.put(callId, colour);
		        colours.add(colour);
	    	}
	    }
	}

	public void buildReportHtml() throws IOException {
		BufferedWriter out = null;
		BufferedReader in = null;
		try {
			out = new BufferedWriter(new FileWriter(targetFilename, false));
			out.write(DOCUMENT_TEMPLATE_HEADER.replaceAll("@FILENAME@", sourceFilename));

			in = new BufferedReader(new FileReader(sourceFilename));
			String line;
			while ((line = in.readLine()) != null) {
				generateHtmlForLine(line, out);
			}
			out.write(DOCUMENT_TEMPLATE_FOOTER);
		} finally {
			if(in != null)
				in.close();
			if(out != null)
				out.close();
		}
	}

	private void generateHtmlForLine(String lineText, BufferedWriter out) throws IOException {
		String line = encodeHtmlEntities(lineText);

		// sip message highlighting

		if(
				// Request / response 1st line
				line.indexOf("SIP/2.0") > -1
				// Header
				|| line.matches("^[A-Za-z-]+:\\s.*?")
				// SDP
				|| line.matches("^[a-z]=.*?"))
			line = highlightText(line, WHITE, "ffffdd");

		// test case highlighting
		if(line.indexOf("Starting test") > -1)
			line = highlightText(line, BLACK, WHITE);
		if(line.indexOf("Ending test") > -1)
			line = highlightText(line, BLACK, "665555");

		// log level highlighting
        for (String level: logLevelColourMap.keySet()) {
            if(line.indexOf(SPACE + level + SPACE) > -1) {
                line = highlightText(line, WHITE, logLevelColourMap.get(level));
                break;
            }
        }

		// call id highlighting
        for (String callId: callIdColourMap.keySet()) {
			if(line.indexOf(callId) > -1) {
				line = line.replaceAll(callId, highlightText(callId, WHITE, callIdColourMap.get(callId)));
			}
		}

		out.write(line + "<br/>\n");
	}

	private String highlightText(String text, String forecolour, String backgroundColour) {
		return "<span style='background-color:" + backgroundColour + ";color:" + forecolour + ";'>" + text + "</span>";
	}

	private String encodeHtmlEntities(String text) {
		return text
			.replaceAll("&", "&amp;")
			.replaceAll("<", "&lt;")
			.replaceAll(">", "&gt;")
			.replaceAll("\"", "&quot;")
			.replaceAll("'", "&apos;");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length != 2) {
			System.err.println("Must specify exactly 2 args: source file and target file");
			System.exit(1);
		}

		SimpleSipStackLogEnhancer logEnhancer = new SimpleSipStackLogEnhancer(args[0], args[1]);
		try {
            System.err.println(new Date().toString() + " Log Enhancer starts....");
			System.out.println("Allocating colours to calls...");
			logEnhancer.allocateColoursToDialogs();

			System.out.println("Building HTML...");
			logEnhancer.buildReportHtml();

			System.out.println("Done.");
            System.err.println(new Date().toString() + " ...Log Enhancer ends");
		} catch(Exception ex) {
			LOG.error(ex.getMessage(), ex);
		}
	}
}
