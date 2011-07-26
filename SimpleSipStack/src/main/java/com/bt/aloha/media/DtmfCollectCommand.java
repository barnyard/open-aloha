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

 	

 	
 	
 
package com.bt.aloha.media;

/**
 * Details of a Prompt and collect command
 */
public class DtmfCollectCommand {
    public static final int NO_MIN_DIGITS = -1;
	public static final int NO_MAX_DIGITS = -1;
	public static final int TIMOUT_SECONDS_MAX = 300;
    public static final int DTMF_PATTERN_MAX_LEN = 50;
	public static final String ALLOWED_RETURN_KEY_CHARS = "0123456789#*";
    public static final String SEMI_COLON = ";";
    private static final String LENGTH_PREFIX = "length=";
    private static final String CANCEL_PREFIX = "cancel=";
    private static final String SECONDS = " seconds";
	private static final int PARAM_PREFIX_LENGTH = 4;

	private String promptFileUri;
	private boolean allowBarge;
	private boolean clearBuffer;
	private int firstDigitTimeoutSeconds;
	private int interDigitTimeoutSeconds;
	private int extraDigitTimeoutSeconds;
	private DtmfPattern dtmfPattern;

	/**
     * Constructor 1
     * @param aPromptFileUri URI of the announcement file
     * @param anAllowBarge whether to allow the announcement to be barged
     * @param aClearBuffer whether to clear the digit buffer
     * @param aFirstDigitTimeoutSeconds timeout in seconds when waiting for first digit
     * @param aInterDigitTimeoutSeconds timeout in seconds when waiting between digits
     * @param aExtraDigitTimeoutSeconds timeout in seconds when waiting for extra digits
     * @param length the number of digits to capture
	 */
    public DtmfCollectCommand(String aPromptFileUri, boolean anAllowBarge, boolean aClearBuffer, int aFirstDigitTimeoutSeconds, int aInterDigitTimeoutSeconds, int aExtraDigitTimeoutSeconds, int length) {
		init(aPromptFileUri, anAllowBarge, aClearBuffer, aFirstDigitTimeoutSeconds, aInterDigitTimeoutSeconds, aExtraDigitTimeoutSeconds, new DtmfLengthPattern(length));
	}

	/**
     * Constructor 2
     * @param aPromptFileUri URI of the announcement file
     * @param anAllowBarge whether to allow the announcement to be barged
     * @param aClearBuffer whether to clear the digit buffer
     * @param aFirstDigitTimeoutSeconds timeout in seconds when waiting for first digit
     * @param aInterDigitTimeoutSeconds timeout in seconds when waiting between digits
     * @param aExtraDigitTimeoutSeconds timeout in seconds when waiting for extra digits
     * @param length the number of digits to capture
     * @param aCancelKey the digit that cancels the collect i.e. '*'
	 */
    public DtmfCollectCommand(String aPromptFileUri, boolean anAllowBarge, boolean aClearBuffer, int aFirstDigitTimeoutSeconds, int aInterDigitTimeoutSeconds, int aExtraDigitTimeoutSeconds, int length, Character aCancelKey) {
		init(aPromptFileUri, anAllowBarge, aClearBuffer, aFirstDigitTimeoutSeconds, aInterDigitTimeoutSeconds, aExtraDigitTimeoutSeconds, new DtmfLengthPattern(length, aCancelKey));
	}

	/**
     * Constructor 3
     * @param aPromptFileUri URI of the announcement file
     * @param anAllowBarge whether to allow the announcement to be barged
     * @param isClearBuffer whether to clear the digit buffer
     * @param aFirstDigitTimeoutSeconds timeout in seconds when waiting for first digit
     * @param aInterDigitTimeoutSeconds timeout in seconds when waiting between digits
     * @param aExtraDigitTimeoutSeconds timeout in seconds when waiting for extra digits
     * @param aMinDigits the minimum number of digits to collect
     * @param aMaxDigits the maximum number of digits to collect
	 */
    public DtmfCollectCommand(String aPromptFileUri, boolean anAllowBarge, boolean isClearBuffer, int aFirstDigitTimeoutSeconds, int aInterDigitTimeoutSeconds, int aExtraDigitTimeoutSeconds, Integer aMinDigits, Integer aMaxDigits) {
    	if (aMinDigits == null)
			throw new DtmfCollectCommandException("Min digits cannot be null");
    	if (aMaxDigits == null)
			throw new DtmfCollectCommandException("Max digits cannot be null");
		init(aPromptFileUri, anAllowBarge, isClearBuffer, aFirstDigitTimeoutSeconds, aInterDigitTimeoutSeconds, aExtraDigitTimeoutSeconds, new DtmfMinMaxRetPattern(aMinDigits, aMaxDigits, null, null));
	}

    /**
     * Constructor 4
     * @param aPromptFileUri URI of the announcement file
     * @param anAllowBarge whether to allow the announcement to be barged
     * @param isClearBuffer whether to clear the digit buffer
     * @param aFirstDigitTimeoutSeconds timeout in seconds when waiting for first digit
     * @param aInterDigitTimeoutSeconds timeout in seconds when waiting between digits
     * @param aExtraDigitTimeoutSeconds timeout in seconds when waiting for extra digits
     * @param aMinDigits the minimum number of digits to collect
     * @param aMaxDigits the maximum number of digits to collect
     * @param aReturnKey the digit that finishes the collect i.e. '#'
     */
	public DtmfCollectCommand(String aPromptFileUri, boolean anAllowBarge, boolean isClearBuffer, int aFirstDigitTimeoutSeconds, int aInterDigitTimeoutSeconds, int aExtraDigitTimeoutSeconds, int aMinDigits, int aMaxDigits, Character aReturnKey) {
		init(aPromptFileUri, anAllowBarge, isClearBuffer, aFirstDigitTimeoutSeconds, aInterDigitTimeoutSeconds, aExtraDigitTimeoutSeconds, new DtmfMinMaxRetPattern(aMinDigits, aMaxDigits, aReturnKey, null));
	}

    /**
     * Constructor 5
     * @param aPromptFileUri URI of the announcement file
     * @param anAllowBarge whether to allow the announcement to be barged
     * @param isClearBuffer whether to clear the digit buffer
     * @param aFirstDigitTimeoutSeconds timeout in seconds when waiting for first digit
     * @param aInterDigitTimeoutSeconds timeout in seconds when waiting between digits
     * @param aExtraDigitTimeoutSeconds timeout in seconds when waiting for extra digits
     * @param aMinDigits the minimum number of digits to collect
     * @param aMaxDigits the maximum number of digits to collect
     * @param aReturnKey the digit that finishes the collect i.e. '#'
     * @param aCancelKey the digit that cancels the collect i.e. '*'
     */
	public DtmfCollectCommand(String aPromptFileUri, boolean anAllowBarge, boolean isClearBuffer, int aFirstDigitTimeoutSeconds, int aInterDigitTimeoutSeconds, int aExtraDigitTimeoutSeconds, int aMinDigits, int aMaxDigits, Character aReturnKey, Character aCancelKey) {
		init(aPromptFileUri, anAllowBarge, isClearBuffer, aFirstDigitTimeoutSeconds, aInterDigitTimeoutSeconds, aExtraDigitTimeoutSeconds, new DtmfMinMaxRetPattern(aMinDigits, aMaxDigits, aReturnKey, aCancelKey));
	}

	private void init(String aPromptFileUri, boolean isAllowBarge, boolean isClearBuffer, int aFirstDigitTimeoutSeconds, int aInterDigitTimeoutSeconds, int aExtraDigitTimeoutSeconds, DtmfPattern aDtmfPattern ) {
		if (aFirstDigitTimeoutSeconds < 0 || aFirstDigitTimeoutSeconds > TIMOUT_SECONDS_MAX)
			throw new DtmfCollectCommandException("First digit timeout must be between 0 (infinity) and " + TIMOUT_SECONDS_MAX);
		if (aInterDigitTimeoutSeconds < 1 || aInterDigitTimeoutSeconds > TIMOUT_SECONDS_MAX)
			throw new DtmfCollectCommandException("Inter digit timeout must be between 1 and " + TIMOUT_SECONDS_MAX + SECONDS);
		if (aExtraDigitTimeoutSeconds < 1 || aExtraDigitTimeoutSeconds > TIMOUT_SECONDS_MAX)
			throw new DtmfCollectCommandException("Extra digit timeout must be between 1 and " + TIMOUT_SECONDS_MAX + SECONDS);

		if (aDtmfPattern instanceof DtmfLengthPattern)
			if (((DtmfLengthPattern)aDtmfPattern).getLength() < 1 || ((DtmfLengthPattern)aDtmfPattern).getLength() > DTMF_PATTERN_MAX_LEN)
				throw new DtmfCollectCommandException("Length must be between 1 and " + DTMF_PATTERN_MAX_LEN);

		if (aDtmfPattern instanceof DtmfMinMaxRetPattern) {
			DtmfMinMaxRetPattern p = (DtmfMinMaxRetPattern)aDtmfPattern;
			if (p.getMinDigits() != NO_MIN_DIGITS && (p.getMinDigits() < 1 || p.getMinDigits() > DTMF_PATTERN_MAX_LEN))
				throw new DtmfCollectCommandException("Min digits must be between 1 and " + DTMF_PATTERN_MAX_LEN);
			if (p.getMaxDigits() != NO_MAX_DIGITS && (p.getMaxDigits() < 1 || p.getMaxDigits() > DTMF_PATTERN_MAX_LEN))
				throw new DtmfCollectCommandException("Max digits must be between 1 and " + DTMF_PATTERN_MAX_LEN);
			if (p.getMinDigits() != NO_MIN_DIGITS && p.getMaxDigits() != NO_MAX_DIGITS && p.getMaxDigits() < p.getMinDigits())
				throw new DtmfCollectCommandException("Max digits must not be less than min digits");
			if (p.getReturnKey() != null && (!ALLOWED_RETURN_KEY_CHARS.contains(Character.toString(p.getReturnKey()))))
				throw new DtmfCollectCommandException("Return key must be a digit, * or #");
			if (p.getCancelKey() != null && (!ALLOWED_RETURN_KEY_CHARS.contains(Character.toString(p.getCancelKey()))))
				throw new DtmfCollectCommandException("Cancel key must be a digit, * or #");
			if (p.getMinDigits() == NO_MIN_DIGITS && p.getMaxDigits() == NO_MAX_DIGITS && p.getReturnKey() == null)
				throw new DtmfCollectCommandException("Must specify at least one of the following: min digits, max digits, or return key");
		}

		this.promptFileUri = aPromptFileUri;
		this.allowBarge = isAllowBarge;
		this.clearBuffer = isClearBuffer;
		this.firstDigitTimeoutSeconds = aFirstDigitTimeoutSeconds;
		this.interDigitTimeoutSeconds = aInterDigitTimeoutSeconds;
		this.extraDigitTimeoutSeconds = aExtraDigitTimeoutSeconds;
		this.dtmfPattern = aDtmfPattern;
	}

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append(getPromptFileUri() + SEMI_COLON);
        result.append(isAllowBarge() + SEMI_COLON);
        result.append(isClearBuffer() + SEMI_COLON);
        result.append(getFirstDigitTimeoutSeconds() + SEMI_COLON);
        result.append(getInterDigitTimeoutSeconds() + SEMI_COLON);
        result.append(getExtraDigitTimeoutSeconds() + SEMI_COLON);
        result.append(dtmfPattern.toString());
        return result.toString();
    }

	/**
     * get whether barging is allowed
     * @return whether barging is allowed
	 */
    public boolean isAllowBarge() {
		return allowBarge;
	}

	/**
     * get whether clear buffer is set
     * @return whether clear buffer is set
	 */
    public boolean isClearBuffer() {
		return clearBuffer;
	}

	/**
     * get the extra digit timeout in seconds
     * @return the extra digit timeout in seconds
	 */
    public int getExtraDigitTimeoutSeconds() {
		return extraDigitTimeoutSeconds;
	}

    /**
     * get the first digit timeout in seconds
     * @return the first digit timeout in seconds
     */
	public int getFirstDigitTimeoutSeconds() {
		return firstDigitTimeoutSeconds;
	}

    /**
     * get the intra adigit timeout in seconds
     * @return the intra digit timeout in seconds
     */
	public int getInterDigitTimeoutSeconds() {
		return interDigitTimeoutSeconds;
	}

	/**
     * set the annoucement uri
     * @param aPromptFileUri the announcement media file uri
	 */
    public void setPromptFileUri(String aPromptFileUri) {
		this.promptFileUri = aPromptFileUri;
	}

	/**
     * get the announcement media file uri
     * @return the announcement media file uri
	 */
    public String getPromptFileUri() {
		return promptFileUri;
	}

	/**
     * get the DtmfPattern
     * @return the DtmfPattern
	 */
    public DtmfPattern getPattern() {
		return dtmfPattern;
	}

	/**
     * Helper method used when parsing msml in the ConvediaMockphoneBean - NOT FOR EXTERNAL USE
     * @param pattern the string pattern
     * @return a DtmfPattern object
	 */
    //TODO: move this into ConvediaMockphoneBean
    public static DtmfPattern parseStringPattern(String pattern) {
    	int length = 1;
    	Character cancelKey = null;
		if(pattern.contains(LENGTH_PREFIX)) {
			for(String param : pattern.split(SEMI_COLON)) {
				if(param.startsWith(LENGTH_PREFIX))
					length = Integer.parseInt(param.substring(LENGTH_PREFIX.length()));
				if(param.startsWith(CANCEL_PREFIX))
					cancelKey = param.charAt(CANCEL_PREFIX.length());
			}
			return new DtmfLengthPattern(length, cancelKey);
		} else {
			int minDigits = 1;
			int maxDigits = 1;
			Character returnKey = null;
			for(String param : pattern.split(SEMI_COLON)) {
				if(param.startsWith("min="))
					minDigits = Integer.parseInt(param.substring(PARAM_PREFIX_LENGTH));
				if(param.startsWith("max="))
					maxDigits = Integer.parseInt(param.substring(PARAM_PREFIX_LENGTH));
				if(param.startsWith("rtk="))
					returnKey = param.charAt(PARAM_PREFIX_LENGTH);
				if(param.startsWith(CANCEL_PREFIX))
					cancelKey = param.charAt(CANCEL_PREFIX.length());
			}
			return new DtmfMinMaxRetPattern(minDigits, maxDigits, returnKey, cancelKey);
		}
	}
}
