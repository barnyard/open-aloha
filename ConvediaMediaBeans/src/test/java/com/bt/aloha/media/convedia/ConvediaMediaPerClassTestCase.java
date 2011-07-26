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

 	

 	
 	
 
package com.bt.aloha.media.convedia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.aloha.call.CallBean;
import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.callleg.OutboundCallLegBean;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.media.MediaCallBean;
import com.bt.aloha.media.MediaCallLegBean;
import com.bt.aloha.media.testing.ConvediaMediaBeansPerClassTestCase;

public abstract class ConvediaMediaPerClassTestCase extends ConvediaMediaBeansPerClassTestCase {	
	private static final int TEN = 10;
	private static ClassPathXmlApplicationContext mockphoneApplicationContext;
	protected CallBean callBean;
	protected MediaCallBean mediaCallBean;
	protected MediaCallLegBean mediaCallLegBean;
	protected OutboundCallLegBean outboundCallLegBean;
	protected CallCollection callCollection;
	protected DialogCollection dialogCollection;
	protected List<Object> eventVector;
	protected Semaphore semaphore;

	private static void initializeMockphoneApplicationContext() {
    	mockphoneApplicationContext = new ClassPathXmlApplicationContext("testMockphoneApplicationContext.xml");
    	setSleepBeforeSendingMessages();
	}
	
	@Before
	public void convediaTestCaseBefore() {
    	callBean = (CallBean)getApplicationContext().getBean("callBean");
		mediaCallBean = (MediaCallBean)getApplicationContext().getBean("mediaCallBean");
		mediaCallLegBean = (MediaCallLegBean)getApplicationContext().getBean("mediaCallLegBean");
		outboundCallLegBean = (OutboundCallLegBean)getApplicationContext().getBean("outboundCallLegBean");
		callCollection = (CallCollection)getApplicationContext().getBean("callCollection");
		dialogCollection = (DialogCollection)getApplicationContext().getBean("dialogCollection");

		eventVector = new Vector<Object>();
		semaphore = new Semaphore(0);
	}

	private static void destroyMockphoneApplicationContext() {
		if (mockphoneApplicationContext != null)
			mockphoneApplicationContext.destroy();
	}

	@BeforeClass
	public static void beforeMethod() {
		initializeMockphoneApplicationContext();
	}

	@AfterClass
	public static void afterMethod() throws Exception {
		destroyMockphoneApplicationContext();
	}
	
	protected void waitForCallEvent(Class<?> eventClass) throws InterruptedException {
		assertTrue(semaphore.tryAcquire(TEN, TimeUnit.SECONDS));
		assertEquals(eventClass, eventVector.get(eventVector.size()-1).getClass());
	}
}
