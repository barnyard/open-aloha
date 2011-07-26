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

 	

 	
 	
 
package com.bt.aloha.stack;

import org.springframework.beans.factory.BeanNameAware;

import com.bt.aloha.eventing.EventDispatcher;

public class SimpleSipBeanBase implements SimpleSipBean, BeanNameAware {
	private String beanName;

    private EventDispatcher eventDispatcher;

	public SimpleSipBeanBase() {
	}

    public String getBeanName() {
        return this.beanName;
    }

    public void setBeanName(String aBeanName) {
        this.beanName = aBeanName;
    }

    public EventDispatcher getEventDispatcher() {
        return this.eventDispatcher;
    }

    public void setEventDispatcher(EventDispatcher aEventDispatcher) {
        this.eventDispatcher = aEventDispatcher;
    }

}
