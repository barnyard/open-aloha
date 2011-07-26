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
import noNamespace.MsmlDocument.Msml.Dialogstart.Play;
import noNamespace.MsmlDocument.Msml.Dialogstart.Play.Audio;
import noNamespace.MsmlDocument.Msml.Dialogstart.Play.Playexit;
import noNamespace.MsmlDocument.Msml.Dialogstart.Play.Playexit.Send;

import com.bt.aloha.util.MessageDigestHelper;
import com.convedia.moml.ext.BooleanType;


public class MsmlAnnouncementRequest extends MsmlRequest {
	public static final String PREFIX = "ANNC";
	public static final int DEFAULT_ITERATIONS = 1;
	public static final int DEFAULT_INTERVAL = 0;
	private String audioFileUri;
	private boolean allowBarge;
	private boolean clearBuffer;
	private int iterations;
	private int interval;
	private MsmlApplicationEventType msmlApplicationEventType;

	public MsmlAnnouncementRequest(String aTargetAddress, String aAudioFileUri, boolean aAllowBarge, boolean aClearBuffer) {
		this(aTargetAddress, PREFIX + MessageDigestHelper.generateDigest(), aAudioFileUri, aAllowBarge, aClearBuffer, DEFAULT_ITERATIONS, DEFAULT_INTERVAL, MsmlApplicationEventType.PLAY_COMMAND_COMPLETE);
	}

	public MsmlAnnouncementRequest(String aTargetAddress, String aCommandId, String aAudioFileUri, boolean aAllowBarge, boolean aClearBuffer) {
		this(aTargetAddress, aCommandId, aAudioFileUri, aAllowBarge, aClearBuffer, DEFAULT_ITERATIONS, DEFAULT_INTERVAL, MsmlApplicationEventType.PLAY_COMMAND_COMPLETE);
	}

	public MsmlAnnouncementRequest(String aTargetAddress, String aAudioFileUri, boolean aAllowBarge, boolean aClearBuffer, int aIterations) {
		this(aTargetAddress, PREFIX + MessageDigestHelper.generateDigest(), aAudioFileUri, aAllowBarge, aClearBuffer, aIterations, DEFAULT_INTERVAL, MsmlApplicationEventType.PLAY_COMMAND_COMPLETE);
	}

	public MsmlAnnouncementRequest(String aTargetAddress, String aAudioFileUri, boolean aAllowBarge, boolean aClearBuffer, int aIterations, int aInterval) {
		this(aTargetAddress, PREFIX + MessageDigestHelper.generateDigest(), aAudioFileUri, aAllowBarge, aClearBuffer, aIterations, aInterval, MsmlApplicationEventType.PLAY_COMMAND_COMPLETE);
	}

	public MsmlAnnouncementRequest(String aTargetAddress, String aCommandId, String aAudioFileUri, boolean aAllowBarge, boolean aClearBuffer, int aIterations) {
		this(aTargetAddress, aCommandId, aAudioFileUri, aAllowBarge, aClearBuffer, aIterations, DEFAULT_INTERVAL, MsmlApplicationEventType.PLAY_COMMAND_COMPLETE);
	}

	public MsmlAnnouncementRequest(String aTargetAddress, String aCommandId, String aAudioFileUri, boolean aAllowBarge, boolean aClearBuffer, int aIterations, int aInterval) {
		this(aTargetAddress, aCommandId, aAudioFileUri, aAllowBarge, aClearBuffer, aIterations, aInterval, MsmlApplicationEventType.PLAY_COMMAND_COMPLETE);
	}

	public MsmlAnnouncementRequest(String aTargetAddress, String aCommandId, String aAudioFileUri, boolean aAllowBarge, boolean aClearBuffer, MsmlApplicationEventType aEventType) {
		this(aTargetAddress, aCommandId, aAudioFileUri, aAllowBarge, aClearBuffer, DEFAULT_ITERATIONS, DEFAULT_INTERVAL, aEventType);
	}

	protected MsmlAnnouncementRequest(String aTargetAddress, String aCommandId, String aAudioFileUri, boolean aAllowBarge, boolean aClearBuffer, int aIterations, int aInterval, MsmlApplicationEventType aEventType) {
		super(aCommandId, aTargetAddress);

		if(aTargetAddress == null)
			throw new IllegalArgumentException("Target address for msml command must be specified");
		if(aAudioFileUri == null)
			throw new IllegalArgumentException("Audio uri for msml command must be specified");
		if(aIterations < -1 || aIterations == 0)
			throw new IllegalArgumentException(String.format("Unsupported number of playback iterations %d", aIterations));
		if(aInterval < 0)
			throw new IllegalArgumentException(String.format("Unsupported interval %d", aInterval));

		this.audioFileUri = aAudioFileUri;
		this.allowBarge = aAllowBarge;
		this.clearBuffer = aClearBuffer;
		this.iterations = aIterations;
		this.interval = aInterval;
		this.msmlApplicationEventType = aEventType;
	}

	public boolean isAllowBarge() {
		return allowBarge;
	}

	public String getAudioFileUri() {
		return audioFileUri;
	}

	public boolean isClearBuffer() {
		return clearBuffer;
	}

	public int getIterations() {
		return iterations;
	}

	public int getInterval() {
		return interval;
	}

	@Override
	public String getXml() {
		MsmlDocument doc = MsmlDocument.Factory.newInstance();
		Dialogstart dialogStart = super.createDialogstart(doc, getTargetAddress());

		Play play = dialogStart.addNewPlay();
		play.setBarge(BooleanType.Enum.forString(Boolean.valueOf(allowBarge).toString()));
		play.setCleardb(BooleanType.Enum.forString(Boolean.valueOf(clearBuffer).toString()));
		if (iterations != DEFAULT_ITERATIONS)
			play.setIterations(Integer.toString(iterations));
		if (interval != DEFAULT_INTERVAL)
			play.setInterval(String.format("%dms", interval));

		Audio audio = play.addNewAudio();
		audio.setUri(audioFileUri);

		Playexit playexit = play.addNewPlayexit();
		Send send = playexit.addNewSend();
		send.setTarget(SOURCE);
		send.setEvent(msmlApplicationEventType.value());
		List<MomlNamelistDatatype.Item.Enum> l = new ArrayList<MomlNamelistDatatype.Item.Enum>();
		l.add(MomlNamelistDatatype.Item.PLAY_END);
		l.add(MomlNamelistDatatype.Item.PLAY_AMT);
		send.setNamelist(l);

		Map<String,String> map = new Hashtable<String,String>();
		map.put(CVD_NS, CVD_PREFIX);

		return XML_PREFIX + doc.xmlText(super.createXmlOptions(map));
	}
}
