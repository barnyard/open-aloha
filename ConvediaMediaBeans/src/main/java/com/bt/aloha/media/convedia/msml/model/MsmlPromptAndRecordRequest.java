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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import noNamespace.BooleanDatatype;
import noNamespace.MomlNamelistDatatype;
import noNamespace.MsmlDocument;
import noNamespace.MsmlDocument.Msml;
import noNamespace.MsmlDocument.Msml.Dialogstart;
import noNamespace.MsmlDocument.Msml.Dialogstart.Record;
import noNamespace.MsmlDocument.Msml.Dialogstart.Record.Format;
import noNamespace.MsmlDocument.Msml.Dialogstart.Record.Recordexit;
import noNamespace.MsmlDocument.Msml.Dialogstart.Record.Recordexit.Send;

import org.apache.xmlbeans.XmlException;

import com.bt.aloha.media.PromptAndRecordCommand;
import com.bt.aloha.util.MessageDigestHelper;

public class MsmlPromptAndRecordRequest extends MsmlRequest {
	public static final String PREFIX = "PPTREC";
	private PromptAndRecordCommand promptAndRecordCommand;

	public MsmlPromptAndRecordRequest(String aTargetAddress, PromptAndRecordCommand aPromptAndRecordCommand) {
		this(aTargetAddress, PREFIX + MessageDigestHelper.generateDigest(), aPromptAndRecordCommand);
	}

	public MsmlPromptAndRecordRequest(String aTargetAddress, String aCommandId, PromptAndRecordCommand aPromptAndRecordCommand) {
		super(aCommandId, aTargetAddress);

		if (aTargetAddress == null)
			throw new IllegalArgumentException("Target address for msml command must be specified");
		if (aPromptAndRecordCommand == null)
			throw new IllegalArgumentException("Prompt and Record command options for msml command must be specified");

		this.promptAndRecordCommand = aPromptAndRecordCommand;
	}

	public PromptAndRecordCommand getPromptAndRecordCommand() {
		return promptAndRecordCommand;
	}

	@Override
	public String getXml() {
		MsmlDocument doc;
		try {
			doc = MsmlDocument.Factory.parse(new MsmlAnnouncementRequest(getTargetAddress(), getCommandId(), promptAndRecordCommand.getPromptFileUri(), promptAndRecordCommand.isAllowBarge(), false, MsmlApplicationEventType.PPTREC_PLAY_COMMAND_COMPLETE).getXml());
		} catch (XmlException e) {
			throw new RuntimeException("Failed to generate prompt and record command; parsing error for announcement command", e);
		}
		Msml msml = doc.getMsml();
		Dialogstart dialogStart = msml.getDialogstartArray(0);

		Record record = dialogStart.addNewRecord();
        record.setDest(promptAndRecordCommand.getDestinationFileUri());
        record.setFormat(Format.Enum.forString(promptAndRecordCommand.getFormat()));
        record.setAppend(BooleanDatatype.Enum.forString(Boolean.toString(promptAndRecordCommand.isAppend())));
        record.setMaxtime(promptAndRecordCommand.getMaxTimeSeconds() + SECONDS);
        record.setPreSpeech(promptAndRecordCommand.getInitialTimeoutSeconds() + SECONDS);
        record.setPostSpeech(promptAndRecordCommand.getExtraTimeoutSeconds() + SECONDS);
        if (null != promptAndRecordCommand.getTerminationKey())
            record.setTermkey(promptAndRecordCommand.getTerminationKey().toString());

		Recordexit recordexit = record.addNewRecordexit();
		Send send = recordexit.addNewSend();

		List<MomlNamelistDatatype.Item.Enum> l = new ArrayList<MomlNamelistDatatype.Item.Enum>();
		l.add(MomlNamelistDatatype.Item.RECORD_RECORDID);
		l.add(MomlNamelistDatatype.Item.RECORD_LEN);
        l.add(MomlNamelistDatatype.Item.RECORD_END);

		send.setTarget(SOURCE);
		send.setEvent(MsmlApplicationEventType.PPTREC_PLAY_COMMAND_COMPLETE.value());
		send.setNamelist(l);

		Map<String,String> map = new Hashtable<String,String>();
        map.put(CVD_NS, CVD_PREFIX);

		return XML_PREFIX + doc.xmlText(super.createXmlOptions(map));
	}
}
