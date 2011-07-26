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

 	

 	
 	
 
package com.bt.aloha.dialog.event;



public class IncomingDialogEvent extends AbstractDialogEvent {
	private IncomingAction incomingDialogAction;
	private IncomingResponseCode responseCode;
	private String fromUri;
	private String toUri;
	private String requestUri;

	public IncomingDialogEvent(String dialogId) {
		super(dialogId);
		this.incomingDialogAction = IncomingAction.None;
	}

	public IncomingAction getIncomingAction() {
		return incomingDialogAction;
	}

	public void setIncomingAction(IncomingAction newIncomingDialogAction) {
		this.incomingDialogAction = newIncomingDialogAction;
	}

	public IncomingResponseCode getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(IncomingResponseCode newResponseCode) {
		this.responseCode = newResponseCode;
	}

	public String getRequestUri() {
		return requestUri;
	}

	public void setRequestUri(String newRequestUri) {
		this.requestUri = newRequestUri;
	}

	public String getFromUri() {
		return fromUri;
	}

	public void setFromUri(String newFromUri) {
		this.fromUri = newFromUri;
	}

	public String getToUri() {
		return toUri;
	}

	public void setToUri(String newToUri) {
		this.toUri = newToUri;
	}
}
