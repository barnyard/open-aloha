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

 	

 	
 	
 
package com.bt.aloha.util;

import java.io.Serializable;
import java.util.Vector;

import javax.sdp.Attribute;
import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class MediaDescriptionState implements Serializable, Cloneable {
    private static final long serialVersionUID = -7274543387142291012L;
    private static final String UNCHECKED = "unchecked";

    private String media;
    private int port;
    private int numPorts;
    private String transport;
    private String[] formats;
    private Vector<Attribute> attributes;
    private Connection connection;
    private transient Log log = LogFactory.getLog(MediaDescriptionState.class);

    public MediaDescriptionState(MediaDescription mediaDescription) {
        if (mediaDescription != null) {
            parseMediaDescription(mediaDescription);
        }
    }

    public MediaDescriptionState clone() {
        try {
            return (MediaDescriptionState)super.clone();
        } catch(CloneNotSupportedException e) {
            // should never happen
            String message = "Unable to clone MediaDescriptionState";
            log.error(message);
            throw new RuntimeException(message, e);
        }
    }

    public MediaDescription getMediaDescription() {
        if (this.media == null)
            return null;

        MediaDescription mediaDescription = SdpFactory.getInstance().createMediaDescription(this.media, this.port, this.numPorts, this.transport, this.formats);
        try {
            if (this.attributes != null)
                mediaDescription.setAttributes(this.attributes);
            if (this.connection != null)
                mediaDescription.setConnection(this.connection);
        } catch(SdpException e) {
            throw new MediaDescriptionException("Error creating MediaDescription object", e);
        }
        return mediaDescription;
    }

    @SuppressWarnings(UNCHECKED)
    private void parseMediaDescription(MediaDescription mediaDescription) {
        try {
            this.media = mediaDescription.getMedia().getMediaType();
            this.port = mediaDescription.getMedia().getMediaPort();
            this.numPorts = mediaDescription.getMedia().getPortCount();
            this.transport = mediaDescription.getMedia().getProtocol();
            this.formats = new String[mediaDescription.getMedia().getMediaFormats(true).size()];
            this.attributes = (Vector<Attribute>)mediaDescription.getAttributes(true);
            this.connection = mediaDescription.getConnection();
            int i = 0;
            for (Object o : mediaDescription.getMedia().getMediaFormats(false)) {
                this.formats[i] = o.toString();
                i++;
            }
        } catch(SdpParseException e) {
            throw new MediaDescriptionException("Error parsing MediaDescription object", e);
        }
    }
}
