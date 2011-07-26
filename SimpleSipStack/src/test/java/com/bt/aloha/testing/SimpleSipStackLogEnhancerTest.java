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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.testing.SimpleSipStackLogEnhancer;


public class SimpleSipStackLogEnhancerTest {
	private static final Log log = LogFactory.getLog(SimpleSipStackLogEnhancerTest.class);
	private File tempSourceFile;
	private File tempTargetFile;
	
	@Before
	public void before() throws Exception {
		tempSourceFile = File.createTempFile("log-enhancer-source", ".txt");
		tempTargetFile = File.createTempFile("log-enhancer-target", ".html");
		
		tempSourceFile.deleteOnExit();
		tempTargetFile.deleteOnExit();
	}
	
	@After
	public void after() {

	}
	
	@Test
	public void testBasicHtmlConversion() throws Exception {
		// setup
		writeSourceFile("Hello world");
		
		// act
		SimpleSipStackLogEnhancer.main(new String[]{tempSourceFile.getAbsolutePath(), tempTargetFile.getAbsolutePath()});	
		
		// assert
		String expectedPattern = "<body>\n<code>\nHello world<br/>\n</code>\n</body>";
		String actualOutput = readTargetFile();
		assertTrue("Expected pattern not found: " + expectedPattern, actualOutput.indexOf(expectedPattern) > -1);
	}
	
	@Test
	public void testBasicHighlighting() throws Exception {
		// setup
		writeSourceFile("Call-ID: abc\nSomething else with abc in it");
		
		// act
		SimpleSipStackLogEnhancer.main(new String[]{tempSourceFile.getAbsolutePath(), tempTargetFile.getAbsolutePath()});	
		
		// assert
		String expectedPattern1 = "Call-ID: <span";		
		String actualOutput = readTargetFile();
		assertTrue("Expected pattern not found: " + expectedPattern1, actualOutput.indexOf(expectedPattern1) > -1);
		
		String expectedPattern2 = "abc</span>";		
		assertTrue("Expected pattern not found: " + expectedPattern2, actualOutput.indexOf(expectedPattern2) > -1);
	}

	private void writeSourceFile(String text) throws IOException {
		FileWriter fw = null;
		try {
			fw = new FileWriter(tempSourceFile);
			fw.write(text);
		} finally {
			if(fw != null) fw.close();
		}
	}
	
	private String readTargetFile() throws IOException {
		FileReader fr = null;
		try {
			char[] buff = new char[(int)tempTargetFile.length()];
			fr = new FileReader(tempTargetFile);
			fr.read(buff);
			String res = new String(buff);
			log.info("Read:\n" + res);
			return res;
		} finally {
			if(fr != null) fr.close();
		}
	}
}
