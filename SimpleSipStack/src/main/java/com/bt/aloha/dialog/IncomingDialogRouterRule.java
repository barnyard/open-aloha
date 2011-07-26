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

 	

 	
 	
 
package com.bt.aloha.dialog;

import java.util.Properties;



public class IncomingDialogRouterRule {

	private String rulePattern;
	private Properties dialogProperties; 
	private DialogSipBean dialogSipBean;

	public IncomingDialogRouterRule() {
		//
	}

	public IncomingDialogRouterRule(String newRulePattern, Properties newDialogProperties, DialogSipBean newDialogBean) {
		this.setRulePattern(newRulePattern);
		this.setDialogProperties(newDialogProperties);
		this.setDialogSipBean(newDialogBean);
	}

	public Properties getDialogProperties() {
		return dialogProperties;
	}
	public void setDialogProperties(Properties newDialogProperties) {
		this.dialogProperties = newDialogProperties;
	}
	public String getRulePattern() {
		return rulePattern;
	}
	public void setRulePattern(String newRulePattern) {
		this.rulePattern = newRulePattern;
	}
	public DialogSipBean getDialogSipBean() {
		return dialogSipBean;
	}
	public void setDialogSipBean(DialogSipBean newSipBean) {
		this.dialogSipBean = newSipBean;
	}	
}
