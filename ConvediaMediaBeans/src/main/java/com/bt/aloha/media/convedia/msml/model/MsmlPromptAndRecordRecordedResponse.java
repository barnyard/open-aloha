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

 	

 	
 	
 
 package com.bt.aloha.media.convedia.msml.model;

import noNamespace.MsmlEventNameValueDatatype;
import noNamespace.MsmlDocument.Msml.Event;

public class MsmlPromptAndRecordRecordedResponse extends MsmlEventResponse {
	private String recordId;
    private String recordLen;
	private String recordEnd;

	public MsmlPromptAndRecordRecordedResponse(String aCommandId, String aRecordId, String aRecordLen, String aRecordEnd) {
		super(aCommandId, MsmlApplicationEventType.PPTREC_PLAY_COMMAND_COMPLETE);
		if (aRecordId == null)
			throw new IllegalArgumentException("Record ID must not be null");
		if (aRecordLen == null)
			throw new IllegalArgumentException("Record LEN state must not be null");
        if (aRecordEnd == null)
            throw new IllegalArgumentException("Record end state must not be null");

		this.recordId = aRecordId;
		this.recordLen = aRecordLen;
        this.recordEnd = aRecordEnd;
	}

	public String getRecordId() {
		return recordId;
	}

	public String getRecordLen() {
		return recordLen;
	}

    public String getRecordEnd() {
        return recordEnd;
    }

	@Override
	protected void addEventNameValuePairs(Event event) {
		event.addName(MsmlEventNameValueDatatype.RECORD_END);
		event.addValue(recordEnd);
		event.addName(MsmlEventNameValueDatatype.RECORD_LEN);
		event.addValue(recordLen);
        event.addName(MsmlEventNameValueDatatype.RECORD_RECORDID);
        event.addValue(recordId);
	}
}
