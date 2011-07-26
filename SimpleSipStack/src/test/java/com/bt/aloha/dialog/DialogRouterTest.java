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
 * (c) British Telecommunications plc, 2007, All Rights Reserved
 */
package com.bt.aloha.dialog;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.text.ParseException;

import gov.nist.javax.sip.address.AddressFactoryImpl;

import javax.sip.address.SipURI;

import org.junit.Test;

import com.bt.aloha.dialog.DialogRouter;
import com.bt.aloha.dialog.IncomingDialogRouterRule;

public class DialogRouterTest {

	@Test
	public void testConstructor() {
		// Setup
		IncomingDialogRouterRule[] routerRules = new IncomingDialogRouterRule[] {new IncomingDialogRouterRule()};
		DialogRouter router = new DialogRouter(routerRules);

		// Act
		IncomingDialogRouterRule[] returnedRules = router.getRules();

		// Assert
		assertArrayEquals(routerRules, returnedRules);
	}

	@Test
	public void testFindRuleConstructorNull() {
		// Setup

		// Act
		try {
			new DialogRouter(null);
			fail("AgumentException expected");
		} catch (IllegalArgumentException e) {
			assertEquals("Null RouterRule[] passed into constructor", e.getMessage());
		}
	}

	@Test
	public void testFindRuleWildCard() {
		// Setup
		final String destination = "1.2.3.4";
		final IncomingDialogRouterRule routerRule = new IncomingDialogRouterRule(".*", null, null);

		final IncomingDialogRouterRule[] routerRules = new IncomingDialogRouterRule[] {routerRule};
		final DialogRouter router = new DialogRouter(routerRules);

		// Act
		final IncomingDialogRouterRule returnedRule = router.findRule(destination);

		// Assert
		assertEquals(routerRule, returnedRule);
	}

	@Test
	public void testFindRuleNoMatch() {
		// Setup
		final String destination = "1.2.3.4";
		final IncomingDialogRouterRule routerRule = new IncomingDialogRouterRule("9*", null, null);

		final IncomingDialogRouterRule[] routerRules = new IncomingDialogRouterRule[] {routerRule};
		final DialogRouter router = new DialogRouter(routerRules);

		// Act
		assertNull(router.findRule(destination));
	}

	@Test
	public void testFindRuleMatch1() {
		// Setup
		final String destination = "bobby";
		final IncomingDialogRouterRule routerRule = new IncomingDialogRouterRule("bob.*", null, null);

		final IncomingDialogRouterRule[] routerRules = new IncomingDialogRouterRule[] {routerRule};
		final DialogRouter router = new DialogRouter(routerRules);

		// Act
		final IncomingDialogRouterRule returnedRule = router.findRule(destination);

		// Assert
		assertEquals(routerRule, returnedRule);
	}

	@Test
	public void testFindRuleMatch2() {
		// Setup
		final String destination = "bobby";
		final IncomingDialogRouterRule routerRuleA = new IncomingDialogRouterRule("bill.*", null, null);
		final IncomingDialogRouterRule routerRuleB = new IncomingDialogRouterRule("bob.*", null, null);

		final IncomingDialogRouterRule[] routerRules = new IncomingDialogRouterRule[] {routerRuleA, routerRuleB};
		final DialogRouter router = new DialogRouter(routerRules);

		// Act
		final IncomingDialogRouterRule returnedRule = router.findRule(destination);

		// Assert
		assertEquals(routerRuleB, returnedRule);
	}

    // test to see that a null username behaves like a ""
    @Test
    public void testRoutingWithNoUsernameAgainstDotStar() throws ParseException {
        // Setup
        final String destination = "sip:192.168.2.150";
        SipURI uri = new AddressFactoryImpl().createSipURI(destination);
        String username = uri.getUser();
        final IncomingDialogRouterRule routerRule = new IncomingDialogRouterRule(".*", null, null);
        final IncomingDialogRouterRule[] routerRules = new IncomingDialogRouterRule[] {routerRule};
        final DialogRouter router = new DialogRouter(routerRules);

        // Act
        final IncomingDialogRouterRule returnedRule = router.findRule(username);

        // Assert
        assertEquals(routerRule, returnedRule);
    }

    // test that a null username doesn't match against a specific pattern
    @Test
    public void testRoutingWithNoUsernameAgainstAPattern() throws ParseException {
        // Setup
        final String destination = "sip:192.168.2.150";
        SipURI uri = new AddressFactoryImpl().createSipURI(destination);
        String username = uri.getUser();
        final IncomingDialogRouterRule routerRule = new IncomingDialogRouterRule("fred", null, null);
        final IncomingDialogRouterRule[] routerRules = new IncomingDialogRouterRule[] {routerRule};
        final DialogRouter router = new DialogRouter(routerRules);

        // Act
        final IncomingDialogRouterRule returnedRule = router.findRule(username);

        // Assert
        assertNull(returnedRule);
    }
}
