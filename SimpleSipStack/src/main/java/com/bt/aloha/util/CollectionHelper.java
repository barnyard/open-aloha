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
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class CollectionHelper {

	private static Log log = LogFactory.getLog(CollectionHelper.class);
    private static final int ONE_THOUSAND = 1000;
	private static final int THREE = 3;
	private static final int ONE_HUNDRED = 100;

	private CollectionHelper(){}
	
	public static void destroy(ConcurrentMap<String, Map<String, Object>> transients, String classname){
		log.debug(String.format("Destroy method called on collection: %s", classname));
		for (Map<String, Object> element : transients.values()) {
			ScheduledFuture<?> future = (ScheduledFuture<?>)element.get("future");
			if (future == null || future.isCancelled() || future.isDone())
				continue;
			if (future.getDelay(TimeUnit.MILLISECONDS) > ONE_HUNDRED) {
				future.cancel(true);
				continue;
			}
			int counter = 0;
			while (!future.isDone() && counter++ < THREE) {
				try {
					log.debug("Waiting for future to get done for some call...");
					Thread.sleep(ONE_THOUSAND);
				} catch (InterruptedException e) {
					log.warn(e.getMessage());
					continue;
				}
			}
		}
	}
}
