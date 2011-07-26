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

 	

 	
 	
 
package com.bt.aloha.testing.mockphones;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;

import com.bt.aloha.dialog.IncomingDialogRouterRule;
import com.bt.aloha.testing.mockphones.MockphonesRouter;

public class MockphonesRouterTest {
	@Test
	public void testNoProperties() throws Exception {
		// setup
		IncomingDialogRouterRule routerRule = new IncomingDialogRouterRule(".*busy.*", null, null);
		
		IncomingDialogRouterRule[] routerRules = new IncomingDialogRouterRule[] {routerRule};
		MockphonesRouter router = new MockphonesRouter(routerRules);

		// act
		IncomingDialogRouterRule rule = router.findRule("busy.1234");
		
		// assert
		assertNotNull(rule);
		assertEquals(routerRule, rule);
	}

	@Test
	public void testNoPropertiesPhoneParams() throws Exception {
		// setup
		IncomingDialogRouterRule routerRule = new IncomingDialogRouterRule(".*busy.*", null, null);
		
		IncomingDialogRouterRule[] routerRules = new IncomingDialogRouterRule[] {routerRule};
		MockphonesRouter router = new MockphonesRouter(routerRules);

		// act
		IncomingDialogRouterRule rule = router.findRule("busy.1234.123");
		
		// assert
		assertNotNull(rule);
		assertEquals(routerRule, rule);
	}

	@Test
	public void testOnePropertyNoPropertyOrdering() throws Exception {
		// setup
		Properties props = new Properties();
		props.setProperty("max.call.duration", "60000");
		IncomingDialogRouterRule routerRule = new IncomingDialogRouterRule(".*noanswer.*", props, null);
		
		IncomingDialogRouterRule[] routerRules = new IncomingDialogRouterRule[] {routerRule};
		MockphonesRouter router = new MockphonesRouter(routerRules);

		// act
		IncomingDialogRouterRule rule = router.findRule("noanswer.1234.1000");
		
		// assert
		assertNotNull(rule);
		assertEquals(routerRule, rule);
	}

	@Test
	public void testOnePropertyPropertyOrdering() throws Exception {
		// setup
		Properties props = new Properties();
		props.setProperty("max.call.duration", "60000");
		props.setProperty("prop.ordering", "max.call.duration");
		IncomingDialogRouterRule routerRule = new IncomingDialogRouterRule(".*noanswer.*", props, null);
		
		IncomingDialogRouterRule[] routerRules = new IncomingDialogRouterRule[] {routerRule};
		MockphonesRouter router = new MockphonesRouter(routerRules);

		// act
		IncomingDialogRouterRule rule = router.findRule("noanswer.1234.1000");
		
		// assert
		assertNotNull(rule);
		assertNotSame(routerRule, rule);
		assertEquals(routerRule.getRulePattern(), rule.getRulePattern());
		assertEquals("1234", rule.getDialogProperties().getProperty("max.call.duration"));
	}

	@Test
	public void testMultiplePropertiesNoPropertyOrdering() throws Exception {
		// setup
		Properties props = new Properties();
		props.setProperty("initial.delay", "10000");
		props.setProperty("hang.up.period", "3000");
		IncomingDialogRouterRule routerRule = new IncomingDialogRouterRule(".*slowanswer.*", props, null);
		
		IncomingDialogRouterRule[] routerRules = new IncomingDialogRouterRule[] {routerRule};
		MockphonesRouter router = new MockphonesRouter(routerRules);

		// act
		IncomingDialogRouterRule rule = router.findRule("slowanswer.1234.1000");
		
		// assert
		assertNotNull(rule);
		assertEquals(routerRule, rule);
	}

	@Test
	public void testMultiplePropertiesPropertyOrdering() throws Exception {
		// setup
		Properties props = new Properties();
		props.setProperty("initial.delay", "10000");
		props.setProperty("hang.up.period", "3000");
		props.setProperty("prop.ordering", "initial.delay,hang.up.period");
		IncomingDialogRouterRule routerRule = new IncomingDialogRouterRule(".*slowanswer.*", props, null);
		
		IncomingDialogRouterRule[] routerRules = new IncomingDialogRouterRule[] {routerRule};
		MockphonesRouter router = new MockphonesRouter(routerRules);

		// act
		IncomingDialogRouterRule rule = router.findRule("slowanswer.1234.4321.1000");
		
		// assert
		assertNotNull(rule);
		assertNotSame(routerRule, rule);
		assertEquals(routerRule.getRulePattern(), rule.getRulePattern());
		assertEquals("1234", rule.getDialogProperties().getProperty("initial.delay"));
		assertEquals("4321", rule.getDialogProperties().getProperty("hang.up.period"));
	}

	@Test
	public void testMultiplePropertiesPropertyOrderingNewSyntax() throws Exception {
		// setup
		Properties props = new Properties();
		props.setProperty("initial.delay", "10000");
		props.setProperty("hang.up.period", "3000");
		props.setProperty("prop.ordering", "initial.delay,hang.up.period");
		IncomingDialogRouterRule routerRule = new IncomingDialogRouterRule(".*slowanswer.*", props, null);
		
		IncomingDialogRouterRule[] routerRules = new IncomingDialogRouterRule[] {routerRule};
		MockphonesRouter router = new MockphonesRouter(routerRules);

		// act
		IncomingDialogRouterRule rule = router.findRule("slowanswer.1234.4321");
		
		// assert
		assertNotNull(rule);
		assertNotSame(routerRule, rule);
		assertEquals(routerRule.getRulePattern(), rule.getRulePattern());
		assertEquals("1234", rule.getDialogProperties().getProperty("initial.delay"));
		assertEquals("4321", rule.getDialogProperties().getProperty("hang.up.period"));
	}

	@Test
	public void testMultiplePropertiesPropertyOrderingWithSpaces() throws Exception {
		// setup
		Properties props = new Properties();
		props.setProperty("initial.delay", "10000");
		props.setProperty("hang.up.period", "3000");
		props.setProperty("prop.ordering", "initial.delay,  hang.up.period");
		IncomingDialogRouterRule routerRule = new IncomingDialogRouterRule(".*slowanswer.*", props, null);
		
		IncomingDialogRouterRule[] routerRules = new IncomingDialogRouterRule[] {routerRule};
		MockphonesRouter router = new MockphonesRouter(routerRules);

		// act
		IncomingDialogRouterRule rule = router.findRule("slowanswer.1234.4321.1000");
		
		// assert
		assertNotNull(rule);
		assertNotSame(routerRule, rule);
		assertEquals(routerRule.getRulePattern(), rule.getRulePattern());
		assertEquals("1234", rule.getDialogProperties().getProperty("initial.delay"));
		assertEquals("4321", rule.getDialogProperties().getProperty("hang.up.period"));
	}

	@Test
	public void testMultiplePropertiesPropertyOrderingOnePhoneParam() throws Exception {
		// setup
		Properties props = new Properties();
		props.setProperty("initial.delay", "10000");
		props.setProperty("hang.up.period", "3000");
		props.setProperty("prop.ordering", "initial.delay,hang.up.period");
		IncomingDialogRouterRule routerRule = new IncomingDialogRouterRule(".*slowanswer.*", props, null);
		
		IncomingDialogRouterRule[] routerRules = new IncomingDialogRouterRule[] {routerRule};
		MockphonesRouter router = new MockphonesRouter(routerRules);

		// act
		IncomingDialogRouterRule rule = router.findRule("slowanswer.1234");
		
		// assert
		assertNotNull(rule);
		assertNotSame(routerRule, rule);
		assertEquals(routerRule.getRulePattern(), rule.getRulePattern());
		assertEquals("1234", rule.getDialogProperties().getProperty("initial.delay"));
		assertEquals("3000", rule.getDialogProperties().getProperty("hang.up.period"));
	}

	@Test
	public void testNoPropertiesRegexCombination() throws Exception {
		// setup
		IncomingDialogRouterRule routerRule = new IncomingDialogRouterRule("^busy.*|^grumpy.*", null, null);
		
		IncomingDialogRouterRule[] routerRules = new IncomingDialogRouterRule[] {routerRule};
		MockphonesRouter router = new MockphonesRouter(routerRules);

		// act
		IncomingDialogRouterRule rule = router.findRule("bashful");
		IncomingDialogRouterRule rule2 = router.findRule("busy");
		IncomingDialogRouterRule rule3 = router.findRule("grumpy");
		IncomingDialogRouterRule rule4 = router.findRule("grump");
		IncomingDialogRouterRule rule5 = router.findRule("busg");
		IncomingDialogRouterRule rule6 = router.findRule("busy.1234");
		
		// assert
		assertNull(rule);
		assertNotNull(rule2);
		assertEquals(routerRule, rule2);
		assertNotNull(rule3);
		assertEquals(routerRule, rule3);
		assertNull(rule4);
		assertNull(rule5);
		assertNotNull(rule6);
	}
	
	@Test
	public void testRegex() throws Exception {
		assertTrue("busy.234".matches("^busy.*|^grumpy.*"));
	}		
}
