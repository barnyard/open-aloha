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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;

public class PromptAndRecordCommand implements Serializable {
    private static final long serialVersionUID = -4879879557687209281L;
    private static final int _108000 = 108000;
    private static final String URI_MUST_BE_EITHER_PROVISIONED_FILE_OR_HTTP = "%s must be either provisioned, file: or http://";
    private static final String DEFAULT_FORMAT = "audio/wav";
    private static final String[] ALLOWABLE_FORMATS = {DEFAULT_FORMAT, "audio/x-wav", "audio/vnd.wave;codec=1", "audio/vnd.wave;codec=6", "audio/vnd.wave;codec=7", "audio/vnd.wave;codec=83"};
    private static final String VALID_TERM_KEYS = "0123456789*#ABCDabcd";
    private String promptFileUri;
    private String destinationFileUri;
    private boolean append;
    private String format; // not sure whether to expose this or make it a default value?
    private int maxTimeSeconds;
    private int initialTimeoutSeconds;
    private int extraTimeoutSeconds;
    private Character terminationKey;
    private boolean allowBarge;

    /**
     * Constructor
     * @param aPromptFileUri
     * @param isAllowBarge
     * @param aDestinationFileUri
     * @param isAppend
     * @param aFormat
     * @param aMaxTimeSeconds
     * @param anInitialTimeoutSeconds
     * @param anExtraTimeoutSeconds
     * @param aTerminationKey
     */
    public PromptAndRecordCommand(String aPromptFileUri, boolean isAllowBarge, String aDestinationFileUri, boolean isAppend, String aFormat, int aMaxTimeSeconds, int anInitialTimeoutSeconds, int anExtraTimeoutSeconds, Character aTerminationKey) {
        this.promptFileUri = aPromptFileUri;
        validateFileUri(aPromptFileUri, "promptFileUri");
        this.allowBarge = isAllowBarge;
        this.destinationFileUri = aDestinationFileUri;
        validateFileUri(aDestinationFileUri, "destinationFileUri");
        this.append = isAppend;
        if (null == aFormat)
            this.format = DEFAULT_FORMAT;
        else {
            this.format = aFormat;
            validateFormat();
        }
        this.maxTimeSeconds = aMaxTimeSeconds;
        validateMaxTimeSeconds();
        this.initialTimeoutSeconds = anInitialTimeoutSeconds;
        validateInitialTimeout();
        this.extraTimeoutSeconds = anExtraTimeoutSeconds;
        validateExtraTimeout();
        this.terminationKey = aTerminationKey;
        validateTerminationKey();
    }

    private void validateTerminationKey() {
        if (null == this.terminationKey) return;
        if (VALID_TERM_KEYS.indexOf(this.terminationKey) > -1) return;
        throw new IllegalArgumentException(String.format("terminationKey must be between one of %s", VALID_TERM_KEYS ));
    }

    private void validateExtraTimeout() {
        if (this.extraTimeoutSeconds < 0 || this.extraTimeoutSeconds > _108000)
            throw new IllegalArgumentException(String.format("extraTimeoutSeconds must be between %d and %d", 0, _108000));
    }

    private void validateInitialTimeout() {
        if (this.initialTimeoutSeconds < 0 || this.initialTimeoutSeconds > _108000)
            throw new IllegalArgumentException(String.format("initialTimeoutSeconds must be between %d and %d", 0, _108000));
    }

    private void validateMaxTimeSeconds() {
        if (this.maxTimeSeconds < 1 || this.maxTimeSeconds > _108000)
            throw new IllegalArgumentException(String.format("maxTimeSeconds must be between %d and %d", 1, _108000));
    }

    private void validateFormat() {
        for (String f: ALLOWABLE_FORMATS) {
            if (f.equals(this.format)) return;
        }
        throw new IllegalArgumentException(String.format("format must be one of %s", Arrays.toString(ALLOWABLE_FORMATS) ));
    }

    private void validateFileUri(String uri, String name) {
        if (uri == null)
            throw new IllegalArgumentException(String.format(URI_MUST_BE_EITHER_PROVISIONED_FILE_OR_HTTP, name));
         if (! uri.toLowerCase(Locale.UK).startsWith("http://")
         && ! uri.toLowerCase(Locale.UK).startsWith("file:")
         && ! uri.toLowerCase(Locale.UK).startsWith("/provisioned/"))
            throw new IllegalArgumentException(String.format(URI_MUST_BE_EITHER_PROVISIONED_FILE_OR_HTTP, name));
    }


    public String getPromptFileUri() {
        return promptFileUri;
    }

    public boolean isAllowBarge() {
        return allowBarge;
    }

    public String getDestinationFileUri() {
        return destinationFileUri;
    }

    public boolean isAppend() {
        return append;
    }

    public int getExtraTimeoutSeconds() {
        return extraTimeoutSeconds;
    }

    public String getFormat() {
        return format;
    }

    public int getInitialTimeoutSeconds() {
        return initialTimeoutSeconds;
    }

    public int getMaxTimeSeconds() {
        return maxTimeSeconds;
    }

    public Character getTerminationKey() {
        return terminationKey;
    }
}
