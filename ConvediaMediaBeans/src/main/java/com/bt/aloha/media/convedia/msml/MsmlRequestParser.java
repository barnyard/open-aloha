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

import noNamespace.MsmlDocument;
import noNamespace.MsmlDocument.Msml.Dialogend;
import noNamespace.MsmlDocument.Msml.Dialogstart;
import noNamespace.MsmlDocument.Msml.Dialogstart.Dtmf;
import noNamespace.MsmlDocument.Msml.Dialogstart.Dtmfgen;
import noNamespace.MsmlDocument.Msml.Dialogstart.Play;
import noNamespace.MsmlDocument.Msml.Dialogstart.Record;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.media.DtmfCollectCommand;
import com.bt.aloha.media.DtmfLengthPattern;
import com.bt.aloha.media.DtmfMinMaxRetPattern;
import com.bt.aloha.media.DtmfPattern;
import com.bt.aloha.media.PromptAndRecordCommand;
import com.bt.aloha.media.convedia.msml.model.MsmlAnnouncementRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlCancelMediaRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlDtmfGenerationRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndCollectDigitsRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlPromptAndRecordRequest;
import com.bt.aloha.media.convedia.msml.model.MsmlRequest;
import com.convedia.moml.ext.BooleanType;

public class MsmlRequestParser extends MsmlParserBase {
	private static final int SEVEN = 7;
	private static Log log = LogFactory.getLog(MsmlRequestParser.class);

	public MsmlRequestParser() {
	}

	public MsmlRequest parse(String xml) {
		MsmlDocument doc = super.preProcess(xml);
		try {
			Dialogend dialogend = doc.getMsml().getDialogendArray(0);
			if (dialogend != null) {
				String id = dialogend.getId();
				log.debug(String.format("dialog end id: %s", id));
				return processCancelMediaRequest(id.substring(id.indexOf("dialog:")+SEVEN));
			}
		} catch(IndexOutOfBoundsException e) {
			log.debug("No dialogend element in document, continuing with parsing");
		}

		Dialogstart dialogstart = doc.getMsml().getDialogstartArray(0);
		if (null == dialogstart)
			throw new MsmlParseException("dialogstart element not found in document");

		String commandId = dialogstart.getId();
		String target = dialogstart.getTarget();
		log.debug(String.format("Parsing msml request for command %s, target %s", commandId, target));

		if (commandId.startsWith(MsmlAnnouncementRequest.PREFIX))
			return processAnnouncementRequest(target, commandId, dialogstart);
		if (commandId.startsWith(MsmlPromptAndCollectDigitsRequest.PREFIX))
			return processPromptAndCollectDigitsRequest(target, commandId, dialogstart);
		if (commandId.startsWith(MsmlDtmfGenerationRequest.PREFIX))
			return processDtmfGenerationRequest(target, commandId, dialogstart.getDtmfgenArray(0));
        if (commandId.startsWith(MsmlPromptAndRecordRequest.PREFIX))
            return processPromptAndRecordRequest(target, commandId, dialogstart);

		throw new MsmlParseException("Unknown media command:\n" + doc);
	}

	protected MsmlRequest processCancelMediaRequest(String id) {
		return new MsmlCancelMediaRequest(id, "");
	}

	protected MsmlAnnouncementRequest processAnnouncementRequest(String target, String commandId, Dialogstart dialogstart) {
		Play play = dialogstart.getPlayArray(0);
		return new MsmlAnnouncementRequest(target, commandId, play.getAudioArray(0).getUri(), BooleanType.TRUE.equals(play.getBarge()), BooleanType.TRUE.equals(play.getCleardb()));
	}

	protected MsmlPromptAndCollectDigitsRequest processPromptAndCollectDigitsRequest(String target, String commandId, Dialogstart dialogstart) {
		Play play = dialogstart.getPlayArray(0);
		Dtmf dtmf = dialogstart.getDtmfArray(0);
		DtmfPattern pattern = DtmfCollectCommand.parseStringPattern(dtmf.getPattern().getDigits());

		DtmfCollectCommand dtmfCollectCommand;
		if(pattern instanceof DtmfLengthPattern)
			dtmfCollectCommand = new DtmfCollectCommand(play.getAudioArray(0).getUri(),
					BooleanType.TRUE.equals(play.getBarge()),
					BooleanType.TRUE.equals(play.getCleardb()),
					Integer.parseInt(dtmf.getFdt().substring(0,dtmf.getFdt().length() - 1)),
					Integer.parseInt(dtmf.getIdt().substring(0,dtmf.getIdt().length() - 1)),
					Integer.parseInt(dtmf.getEdt().substring(0,dtmf.getEdt().length() - 1)),
					((DtmfLengthPattern)pattern).getLength());
		else
			dtmfCollectCommand = new DtmfCollectCommand(play.getAudioArray(0).getUri(),
					BooleanType.TRUE.equals(play.getBarge()),
					BooleanType.TRUE.equals(play.getCleardb()),
					Integer.parseInt(dtmf.getFdt().substring(0,dtmf.getFdt().length() - 1)),
					Integer.parseInt(dtmf.getIdt().substring(0,dtmf.getIdt().length() - 1)),
					Integer.parseInt(dtmf.getEdt().substring(0,dtmf.getEdt().length() - 1)),
					((DtmfMinMaxRetPattern)pattern).getMinDigits(),
					((DtmfMinMaxRetPattern)pattern).getMaxDigits(),
					((DtmfMinMaxRetPattern)pattern).getReturnKey());

		return new MsmlPromptAndCollectDigitsRequest(target, commandId, dtmfCollectCommand);
	}

    private String removeSeconds(String in) {
        if (in.endsWith("s")) return in.substring(0,in.length()-1);
        return in;
    }


    protected MsmlPromptAndRecordRequest processPromptAndRecordRequest(String target, String commandId, Dialogstart dialogstart) {
        // assume only one Play and Record stanza
        Record record = dialogstart.getRecordArray(0);
        Play play = dialogstart.getPlayArray(0);
        return new MsmlPromptAndRecordRequest(target, dialogstart.getId(),
                new PromptAndRecordCommand(play.getAudioArray(0).getUri(),
                        Boolean.getBoolean(play.getBarge().toString()),
                        record.getDest(),
                        record.isSetAppend(),
                        record.getFormat().toString(),
                        Integer.parseInt(removeSeconds(record.getMaxtime())),
                        Integer.parseInt(removeSeconds(record.getPreSpeech())),
                        Integer.parseInt(removeSeconds(record.getPostSpeech())),
                        record.getTermkey() != null && record.getTermkey().length() > 0 ? record.getTermkey().charAt(0) : null)
        );
    }

    protected MsmlDtmfGenerationRequest processDtmfGenerationRequest(String target, String commandId, Dtmfgen dtmfgen) {
		return new MsmlDtmfGenerationRequest(target, commandId, dtmfgen.getDigits());
	}
}
