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

 	

 	
 	
 
package com.bt.aloha.eventing;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;

public class EventDispatcherRunnable implements Runnable {
    private Log log = LogFactory.getLog(this.getClass());
    private Method method;
    private Object event;
    private Object listener;

    public EventDispatcherRunnable(final Method aMethod, final Object anEvent, final Object aListener, final String stackname) {
        this.method = aMethod;
        this.event = anEvent;
        this.listener = aListener;
        if (null != stackname)
            MDC.put("stackname", stackname);
    }

    public void run() {
        try {
            log.debug(String.format("Delivering asynchronous event %s to %s...", event.getClass().getSimpleName(), listener.getClass().getName()));
            method.invoke(listener, new Object[] { event });
            log.debug(String.format("...delivered asynchronous event %s to %s", event.getClass().getSimpleName(), listener.getClass().getName()));
        } catch (Throwable t) {
            log.error(String.format("Exception whilst processing event %s", event.getClass().getSimpleName()), t);
        }
    }
}
