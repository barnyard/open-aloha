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

import noNamespace.MomlNamelistDatatype;
import noNamespace.MsmlDocument;
import noNamespace.MsmlDocument.Msml.Dialogstart;
import noNamespace.MsmlDocument.Msml.Dialogstart.Dtmfgen;
import noNamespace.MsmlDocument.Msml.Dialogstart.Dtmfgen.Dtmfgenexit;

import com.bt.aloha.util.MessageDigestHelper;


public class MsmlDtmfGenerationRequest extends MsmlRequest {
	public static final String PREFIX = "DTMFGEN";
	public static final int DEFAULT_DIGIT_LENGTH_MILLIS = 100;
	public static final int MINIMUM_DIGIT_LENGTH = 50;
	public static final int MAXIMUM_DIGIT_LENGTH = 30000;
	private String digits;
	private int digitLengthMilliseconds;
	
	public MsmlDtmfGenerationRequest(String aTargetAddress, String aDigits, int digitLengthMilliseconds) {
		this(aTargetAddress, PREFIX + MessageDigestHelper.generateDigest(), aDigits, digitLengthMilliseconds);
	}

	public MsmlDtmfGenerationRequest(String aTargetAddress, String aDigits) {
		this(aTargetAddress, PREFIX + MessageDigestHelper.generateDigest(), aDigits);
	}
	
	public MsmlDtmfGenerationRequest(String aTargetAddress, String aCommandId, String aDigits) {
		this(aTargetAddress, aCommandId, aDigits, DEFAULT_DIGIT_LENGTH_MILLIS);
	}
	
	public MsmlDtmfGenerationRequest(String aTargetAddress, String aCommandId, String aDigits, int aDigitLengthMilliseconds) {
		super(aCommandId, aTargetAddress);
		
		if(aTargetAddress == null)
			throw new IllegalArgumentException("Target address for msml command must be specified");
		
		this.digits = aDigits;
		this.digitLengthMilliseconds = aDigitLengthMilliseconds;
	}
	
	public String getDigits() {
		return this.digits;
	}

	@Override
	public String getXml() {
		MsmlDocument doc = MsmlDocument.Factory.newInstance();
		Dialogstart dialogStart = super.createDialogstart(doc, getTargetAddress());
		
		Dtmfgen dtmfgen = dialogStart.addNewDtmfgen();
		dtmfgen.setDigits(digits);
		dtmfgen.setDur(String.format("%dms", this.digitLengthMilliseconds));

		Dtmfgenexit dtmfgenexit = dtmfgen.addNewDtmfgenexit();
		noNamespace.MsmlDocument.Msml.Dialogstart.Dtmfgen.Dtmfgenexit.Send send = dtmfgenexit.addNewSend();
		
		List<MomlNamelistDatatype.Item.Enum> l = new ArrayList<MomlNamelistDatatype.Item.Enum>();
		l.add(MomlNamelistDatatype.Item.DTMFGEN_END);

		send.setTarget(SOURCE);
		send.setEvent(MsmlApplicationEventType.DTMFGEN_COMMAND_COMPLETE.value());
		send.setNamelist(l);
		
		Map<String,String> map = new Hashtable<String,String>();
		map.put(CVD_NS, CVD_PREFIX);

		return XML_PREFIX + doc.xmlText(super.createXmlOptions(map));
	}	
}
