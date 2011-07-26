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
package com.bt.aloha.spring;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import com.bt.aloha.call.CallBean;
import com.bt.aloha.call.MaxCallDurationTermination;


public class MaxCallDurationTerminationPostProcessor extends BeanFactoryPostProcessorBase {

    public MaxCallDurationTerminationPostProcessor() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
	 */
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        String[] maxCallDurationCallTerminatorBeanFactoryPostProcessorNames = beanFactory.getBeanNamesForType(MaxCallDurationTermination.class);
        for (String maxCallDurationCallTerminatorBeanFactoryPostProcessor : maxCallDurationCallTerminatorBeanFactoryPostProcessorNames) {
        	super.injectBeanIfDefined(beanFactory, maxCallDurationCallTerminatorBeanFactoryPostProcessor, "callBean", CallBean.class);
        }
	}
}
