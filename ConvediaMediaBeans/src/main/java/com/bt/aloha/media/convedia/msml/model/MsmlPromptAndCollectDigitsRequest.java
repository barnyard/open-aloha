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
import noNamespace.MsmlDocument.Msml.Dialogstart.Dtmf;
import noNamespace.MsmlDocument.Msml.Dialogstart.Dtmf.Dtmfexit;
import noNamespace.MsmlDocument.Msml.Dialogstart.Dtmf.Pattern;

import org.apache.xmlbeans.XmlException;

import com.bt.aloha.media.DtmfCollectCommand;
import com.bt.aloha.util.MessageDigestHelper;


public class MsmlPromptAndCollectDigitsRequest extends MsmlRequest {
	public static final String PREFIX = "PPTCOL";
	private DtmfCollectCommand dtmfCollectCommand;

	public MsmlPromptAndCollectDigitsRequest(String aTargetAddress, DtmfCollectCommand aDtmfOptions) {
		this(aTargetAddress, PREFIX + MessageDigestHelper.generateDigest(), aDtmfOptions);
	}
	
	public MsmlPromptAndCollectDigitsRequest(String aTargetAddress, String aCommandId, DtmfCollectCommand aDtmfOptions) {
		super(aCommandId, aTargetAddress);
		
		if(aTargetAddress == null)
			throw new IllegalArgumentException("Target address for msml command must be specified");
		if(aDtmfOptions == null)
			throw new IllegalArgumentException("DTMF command options for msml command must be specified");
		
		this.dtmfCollectCommand = aDtmfOptions;
	}

	public DtmfCollectCommand getDtmfCollectCommand() {
		return dtmfCollectCommand;
	}

	@Override
	public String getXml() {
		MsmlDocument doc;
		try {
			doc = MsmlDocument.Factory.parse(new MsmlAnnouncementRequest(getTargetAddress(), getCommandId(), dtmfCollectCommand.getPromptFileUri(), dtmfCollectCommand.isAllowBarge(), dtmfCollectCommand.isClearBuffer(), MsmlApplicationEventType.DTMF_PLAY_COMMAND_COMPLETE).getXml());
		} catch (XmlException e) {
			throw new RuntimeException("Failed to generate prompt and collect command; parsing error for announcement command", e);
		}
		Msml msml = doc.getMsml();
		Dialogstart dialogStart = msml.getDialogstartArray(0);

		Dtmf dtmf = dialogStart.addNewDtmf();
		//dtmf.setCleardb(BooleanDatatype.Enum.forString(Boolean.valueOf(dtmfCollectCommand.isClearBuffer()).toString()));
		dtmf.setCleardb(BooleanDatatype.FALSE);
		dtmf.setFdt(dtmfCollectCommand.getFirstDigitTimeoutSeconds() + SECONDS);
		dtmf.setIdt(dtmfCollectCommand.getInterDigitTimeoutSeconds() + SECONDS);
		dtmf.setEdt(dtmfCollectCommand.getExtraDigitTimeoutSeconds() + SECONDS);

		Pattern pattern = dtmf.addNewPattern();
		pattern.setDigits(dtmfCollectCommand.getPattern().toString());
		pattern.setFormat("moml+digits");

		dtmf.addNewNoinput();
		dtmf.addNewNomatch();

		Dtmfexit dtmfexit = dtmf.addNewDtmfexit();
		noNamespace.MsmlDocument.Msml.Dialogstart.Dtmf.Dtmfexit.Send send = dtmfexit.addNewSend();

		List<MomlNamelistDatatype.Item.Enum> l = new ArrayList<MomlNamelistDatatype.Item.Enum>();
		l.add(MomlNamelistDatatype.Item.DTMF_DIGITS);
		l.add(MomlNamelistDatatype.Item.DTMF_END);

		send.setTarget(SOURCE);
		send.setEvent(MsmlApplicationEventType.DTMF_COLLECT_COMMAND_COMPLETE.value());
		send.setNamelist(l);

		Map<String,String> map = new Hashtable<String,String>();
        map.put(CVD_NS, CVD_PREFIX);

		return XML_PREFIX + doc.xmlText(super.createXmlOptions(map));
	}
}
