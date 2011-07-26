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

 	

 	
 	
 
package com.bt.aloha.util;

import java.util.Map;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Do housekeeping on any Spring bean that implements Housekeepable
 */
public class HousekeepingTimerTask extends TimerTask implements BeanFactoryPostProcessor {
    private Log log = LogFactory.getLog(this.getClass());

    private Map<String, Housekeeper> beansToBeHouseKept;

    public HousekeepingTimerTask() {
    }

    @Override
    public void run() {
        for (String name : beansToBeHouseKept.keySet()) {
            Housekeeper h = beansToBeHouseKept.get(name);
            log.info(String.format("invoking housekeeping on %s(%s)", name, h.getClass().getName()));
            h.housekeep();
        }
    }

    @SuppressWarnings("unchecked")
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        this.beansToBeHouseKept = (Map<String, Housekeeper>) beanFactory.getBeansOfType(Housekeeper.class);
    }
}
