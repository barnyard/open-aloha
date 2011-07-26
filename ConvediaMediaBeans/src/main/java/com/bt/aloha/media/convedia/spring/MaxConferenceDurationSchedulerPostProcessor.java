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

 	

 	
 	
 
package com.bt.aloha.media.convedia.spring;

import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import com.bt.aloha.media.convedia.conference.ScheduledExecutorServiceMaxConferenceDurationScheduler;
import com.bt.aloha.spring.BeanFactoryPostProcessorBase;

public class MaxConferenceDurationSchedulerPostProcessor extends BeanFactoryPostProcessorBase {
	public MaxConferenceDurationSchedulerPostProcessor() {
		super();
	}

	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		String[] scheduledMaxConferenceDurationBeans = beanFactory.getBeanNamesForType(ScheduledExecutorServiceMaxConferenceDurationScheduler.class);
		if(scheduledMaxConferenceDurationBeans != null) {
			for(String scheduledMaxConferenceDurationBean : scheduledMaxConferenceDurationBeans) {
				super.injectBeanIfDefined(beanFactory, scheduledMaxConferenceDurationBean, "scheduledExecutorService", ScheduledExecutorService.class);
			}
		}

	}

}
