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

 	

 	
 	
 
package com.bt.aloha.media.convedia.msml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import noNamespace.MsmlDocument;
import noNamespace.MsmlEventNameValueDatatype;
import noNamespace.MsmlDocument.Msml.Event;
import noNamespace.MsmlDocument.Msml.Result;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.media.convedia.msml.model.MsmlAnnouncementRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlAnnouncementResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlApplicationEventType;
import com.bt.aloha.media.convedia.msml.model.MsmlDtmfGenerationRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlDtmfGenerationResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlEventResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndCollectDigitsAnnouncementResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndCollectDigitsCollectedResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndCollectDigitsRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndRecordAnnouncementResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndRecordRecordedResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndRecordRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlResponse;
import com.bt.aloha.media.convedia.msml.model.MsmlResultResponse;

public class MsmlResponseParser extends MsmlParserBase {
	private static Log log = LogFactory.getLog(MsmlResponseParser.class);
	private static final String EVENT_DIALOG_PREFIX = "dialog:";

	public MsmlResponseParser() {
	}

	public MsmlResponse parse(String xml) {
		MsmlDocument doc = super.preProcess(xml);
		Result result = doc.getMsml().getResult();

		if (null != result) {
			String commandId = extractMsmlDialogIdFromEventDialogId(result.getDialogidArray(0));
			String responseCode = result.getResponse();
			return new MsmlResultResponse(commandId, responseCode);
		}

		Event event = doc.getMsml().getEvent();
		if (null != event) {
			MsmlApplicationEventType eventName = MsmlApplicationEventType.fromValue(event.getName2());
			String commandId = extractMsmlDialogIdFromEventDialogId(event.getId());

			if (!eventName.equals(MsmlApplicationEventType.MSML_DIALOG_EXIT) &&	!eventName.equals(MsmlApplicationEventType.UNKNOWN)) {
				Map<MsmlEventNameValueDatatype.Enum, String> eventAttributes = new HashMap<MsmlEventNameValueDatatype.Enum, String>();
				List<MsmlEventNameValueDatatype.Enum> names = event.getNameList();
				List<String> values = event.getValueList();
				Iterator<MsmlEventNameValueDatatype.Enum> namesIterator = names.iterator();
				int i = 0;
				while (namesIterator.hasNext()) {
					eventAttributes.put(namesIterator.next(), values.get(i++));
				}
				if (commandId.startsWith(MsmlAnnouncementRequest.PREFIX))
					return processAnnouncementResponse(commandId, eventAttributes);
				else if (commandId.startsWith(MsmlPromptAndCollectDigitsRequest.PREFIX))
					return processPromptAndCollectDigitsResponse(commandId, eventAttributes);
                else if (commandId.startsWith(MsmlPromptAndRecordRequest.PREFIX))
                    return processPromptAndRecordResponse(commandId, eventAttributes);
				else if (commandId.startsWith(MsmlDtmfGenerationRequest.PREFIX))
					return processDtmfGenerationResponse(commandId, eventAttributes);
				else throw new MsmlParseException(String.format("Unknown command ID received: %s", commandId));
			} else {
				log.debug(String.format("Ignoring event %s for media command %s", eventName, commandId));
				return new UnknownMsmlEventResponse(commandId);
			}
		}
		throw new MsmlParseException(String.format("Unknown MSML response received:\n%s", xml));
	}

	protected static class UnknownMsmlEventResponse extends MsmlEventResponse {
		public UnknownMsmlEventResponse(String commandId) {
			super(commandId, MsmlApplicationEventType.UNKNOWN);
		}
		@Override protected void addEventNameValuePairs(Event event) {}
	}

	protected MsmlAnnouncementResponse processAnnouncementResponse(String commandId, Map<MsmlEventNameValueDatatype.Enum,String> eventAttributes) {
		return new MsmlAnnouncementResponse(commandId,
				eventAttributes.get(MsmlEventNameValueDatatype.PLAY_AMT),
				eventAttributes.get(MsmlEventNameValueDatatype.PLAY_END));
	}

	protected MsmlEventResponse processPromptAndCollectDigitsResponse(String commandId, Map<MsmlEventNameValueDatatype.Enum, String> eventAttributes) {
		if(eventAttributes.keySet().contains(MsmlEventNameValueDatatype.PLAY_AMT)) {
			return new MsmlPromptAndCollectDigitsAnnouncementResponse(commandId,
					eventAttributes.get(MsmlEventNameValueDatatype.PLAY_AMT),
					eventAttributes.get(MsmlEventNameValueDatatype.PLAY_END));
		} else {
			return new MsmlPromptAndCollectDigitsCollectedResponse(commandId,
					eventAttributes.get(MsmlEventNameValueDatatype.DTMF_DIGITS),
					eventAttributes.get(MsmlEventNameValueDatatype.DTMF_END));
		}
	}

    protected MsmlEventResponse processPromptAndRecordResponse(String commandId, Map<MsmlEventNameValueDatatype.Enum, String> eventAttributes) {
        if (eventAttributes.keySet().contains(MsmlEventNameValueDatatype.PLAY_AMT)) {
            return new MsmlPromptAndRecordAnnouncementResponse(commandId,
                    eventAttributes.get(MsmlEventNameValueDatatype.PLAY_AMT),
                    eventAttributes.get(MsmlEventNameValueDatatype.PLAY_END));
        } else {
            return new MsmlPromptAndRecordRecordedResponse(commandId,
                    eventAttributes.get(MsmlEventNameValueDatatype.RECORD_RECORDID),
                    eventAttributes.get(MsmlEventNameValueDatatype.RECORD_LEN),
                    eventAttributes.get(MsmlEventNameValueDatatype.RECORD_END));
        }
    }

	protected MsmlDtmfGenerationResponse processDtmfGenerationResponse(String commandId, Map<MsmlEventNameValueDatatype.Enum, String> eventAttributes) {
		return new MsmlDtmfGenerationResponse(commandId,
				eventAttributes.get(MsmlEventNameValueDatatype.DTMFGEN_END));
	}

	protected String extractMsmlDialogIdFromEventDialogId(String eventDialogId) {
		if(eventDialogId == null)
			throw new MsmlParseException(String.format("Empty event id"));
		int dialogStartIndex = eventDialogId.indexOf(EVENT_DIALOG_PREFIX);
		if(dialogStartIndex < 0 || dialogStartIndex == eventDialogId.length() - EVENT_DIALOG_PREFIX.length())
			throw new MsmlParseException(String.format("No dialog id in event id %s", eventDialogId));
		return eventDialogId.substring(dialogStartIndex+EVENT_DIALOG_PREFIX.length(), eventDialogId.length());
	}
}
