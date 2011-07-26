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

 	

 	
 	
 
/**
 * (c) British Telecommunications plc, 2008, All Rights Reserved
 */
package com.bt.aloha.spring;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ScheduledExecutorService;

import org.junit.Test;
import org.springframework.context.support.StaticApplicationContext;

import com.bt.aloha.callleg.OutboundCallLegBeanImpl;
import com.bt.aloha.callleg.URIParser;
import com.bt.aloha.spring.OutboundCallLegBeanPostProcessor;

public class OutboundCallLegBeanPostProcessorTest {
	
	// test that the Post processor injects the dependencies as required
	@Test
	public void testPostProcessBeanFactory() {
		// setup
		StaticApplicationContext appCtx = new StaticApplicationContext();
		appCtx.registerSingleton("outboundCallLegBean", OutboundCallLegBeanImpl.class);
		appCtx.registerSingleton("scheduledExecutorService", org.springframework.scheduling.concurrent.ScheduledExecutorFactoryBean.class);
		appCtx.registerSingleton("URIParser", URIParser.class);
		OutboundCallLegBeanPostProcessor outboundCallLegBeanPostProcessor = new OutboundCallLegBeanPostProcessor();
		
		// act
		outboundCallLegBeanPostProcessor.postProcessBeanFactory(appCtx.getBeanFactory());
		
		// assert
		OutboundCallLegBeanImpl outboundCallLegBeanImpl = (OutboundCallLegBeanImpl)appCtx.getBean("outboundCallLegBean");
		ScheduledExecutorService scheduledExecutorService = (ScheduledExecutorService)appCtx.getBean("scheduledExecutorService");
		URIParser uriParser = (URIParser)appCtx.getBean("URIParser");
		
		assertEquals(scheduledExecutorService, outboundCallLegBeanImpl.getScheduledExecutorService());
		assertEquals(uriParser, outboundCallLegBeanImpl.getURIParser());
	}
}
