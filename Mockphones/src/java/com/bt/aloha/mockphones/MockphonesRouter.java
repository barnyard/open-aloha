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

 	

 	
 	
 
package com.bt.aloha.mockphones;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.dialog.DialogRouter;
import com.bt.aloha.dialog.IncomingDialogRouterRule;


public class MockphonesRouter extends DialogRouter {
	private Log log = LogFactory.getLog(this.getClass());

	public MockphonesRouter(IncomingDialogRouterRule[] rules) {
		super(rules);
	}
	
	@Override
	public IncomingDialogRouterRule findRule(String destination) {
		IncomingDialogRouterRule rule = super.findRule(destination);
		
		if (rule == null) {
			return rule;
		}
		
		log.debug("Checking for parameters specified in sip uri");
		String[] params = MockphonesHelper.getParams(destination);
		if (params == null || params.length == 0)
			return rule;

		log.debug(String.format("Creating new router rule for %d arguments specified in sip uri", params.length));
		Properties props = rule.getDialogProperties();
		if (props == null)
			return rule;
		props = (Properties)props.clone();
		String propertyOrdering = props.getProperty("prop.ordering");
		if (propertyOrdering == null || propertyOrdering.length() == 0)
			return rule;
		
		log.debug("Parsing arguments to create new router rule");
		String[] propertyOrder = propertyOrdering.split(",");
		for (int i=0; i<params.length && i<propertyOrder.length; i++) {
			props.setProperty(propertyOrder[i].trim(), params[i]);
		}
		IncomingDialogRouterRule newRule = new IncomingDialogRouterRule(rule.getRulePattern(), props, rule.getDialogSipBean());
		log.debug("New rule created");
		
		return newRule;
	}
}
