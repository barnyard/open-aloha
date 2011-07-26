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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;

public abstract class BeanFactoryPostProcessorBase implements BeanFactoryPostProcessor {
	private Log log = LogFactory.getLog(this.getClass());
	
	protected void injectBeanIfDefined(ConfigurableListableBeanFactory beanFactory, String targetBeanName, String propertyName, Class<?> beanClassToInject) {
		String[] beanNamesForInjection = beanFactory.getBeanNamesForType(beanClassToInject);
		if (beanNamesForInjection != null && beanNamesForInjection.length > 0) {
			log.debug(String.format("Injecting bean %s into property %s of bean %s", beanClassToInject.getName(), propertyName, targetBeanName));
			if (beanNamesForInjection.length > 1)
				log.debug(String.format("Multiple beans of type %s found, injecting only the first one into %s", beanClassToInject.getName(), targetBeanName));
			PropertyValue propertyValue = new PropertyValue(propertyName, new RuntimeBeanReference(beanNamesForInjection[0]));
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(targetBeanName);
			beanDefinition.getPropertyValues().addPropertyValue(propertyValue);
		} else
			log.info(String.format("Property %s not set for bean %s", propertyName, targetBeanName));
	}
}
