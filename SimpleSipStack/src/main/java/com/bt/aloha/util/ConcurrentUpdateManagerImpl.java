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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ConcurrentUpdateManagerImpl implements ConcurrentUpdateManager {
	private static final Log LOG = LogFactory.getLog(ConcurrentUpdateManager.class);
	private static final String CONCURRENT_MODIFICATION_MESSAGE = "Received ConcurrentUpdateException %d times for resource %s, giving up: %s";
    private static final int DEFAULT_CONCURRENT_MODIFICATION_RETRIES = 10;
	private int concurrentModificationRetries = DEFAULT_CONCURRENT_MODIFICATION_RETRIES;

	public ConcurrentUpdateManagerImpl() {
	}

	public ConcurrentUpdateManagerImpl(int aConcurrentModificationRetries) {
		this.concurrentModificationRetries = aConcurrentModificationRetries;
	}

	/* (non-Javadoc)
	 * @see com.bt.aloha.stack.concurrency.ConcurrentUpdateManager#executeConcurrentUpdate(com.bt.aloha.stack.concurrency.ConcurrentUpdateBlock)
	 */
	public void executeConcurrentUpdate(ConcurrentUpdateBlock concurrentUpdateBlock) {
		for (int numRetries=0; numRetries <= concurrentModificationRetries; numRetries++) {
			try {
				concurrentUpdateBlock.execute();
				break;
			} catch(ConcurrentUpdateException e) {
				notifyFailedUpdate(concurrentUpdateBlock);
				
				if (!e.getResourceId().equals(concurrentUpdateBlock.getResourceId())) {
					LOG.debug(String.format("Update manager for %s got ConcurrentUpdateException for %s, rethrowing...", concurrentUpdateBlock.getResourceId(), e.getResourceId()));
					throw e;
				}
				if (numRetries == concurrentModificationRetries) {
					LOG.error(String.format(CONCURRENT_MODIFICATION_MESSAGE, numRetries, concurrentUpdateBlock.getResourceId(), e.getMessage()));
					throw e;
				}
				else {
					LOG.info(String.format("----- Unable to update resource %s, got concurrent modification exception -----", concurrentUpdateBlock.getResourceId()));
				}
			}
		}
	}

	private void notifyFailedUpdate(ConcurrentUpdateBlock concurrentUpdateBlock) {
		if (concurrentUpdateBlock instanceof ConcurrentUpdateConflictAware) {
			try {
				((ConcurrentUpdateConflictAware)concurrentUpdateBlock).onConcurrentUpdateConflict();
			} catch (Throwable t) {
				LOG.error(String.format("Error whilst notifying block of concurrent update failure for %s", concurrentUpdateBlock.getResourceId()), t);
			}
		}
	}
}
