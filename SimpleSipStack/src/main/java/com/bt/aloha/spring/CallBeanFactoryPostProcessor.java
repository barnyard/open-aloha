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

 	

 	
 	
 
package com.bt.aloha.spring;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import com.bt.aloha.call.CallBean;
import com.bt.aloha.call.MaxCallDurationScheduler;
import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.dialog.collections.DialogCollection;

public class CallBeanFactoryPostProcessor extends BeanFactoryPostProcessorBase {

    public CallBeanFactoryPostProcessor() {
        super();
    }

	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		String[] callBeanNames = beanFactory.getBeanNamesForType(CallBean.class);
		if(callBeanNames != null) {
			for(String callBeanName : callBeanNames) {
				super.injectBeanIfDefined(beanFactory, callBeanName, "dialogCollection", DialogCollection.class);
				super.injectBeanIfDefined(beanFactory, callBeanName, "callCollection", CallCollection.class);
				super.injectBeanIfDefined(beanFactory, callBeanName, "maxCallDurationScheduler", MaxCallDurationScheduler.class);
			}
		}
	}
}
