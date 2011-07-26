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

 	

 	
 	
 
package com.bt.aloha.stack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import javax.sdp.Attribute;
import javax.sdp.Connection;
import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.Origin;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class SessionDescriptionHelper {
    private static final String ZERO_DOT_ZERO_DOT_ZERO_DOT_ZERO = "0.0.0.0";
    private static final String ZERO_SPACE_PCMU_8000 = "0 PCMU/8000";
    private static final String ZERO_STRING = "0";
    private static final String RTP_SLASH_AVP = "RTP/AVP";
    private static final String UNABLE_TO_CREATE_SESSION_DESCRIPTION = "Unable to create Session Description";
    private static final String VIDEO = "video";
    private static final String UNABLE_TO_EXTRACT_MEDIA_INFORMATION_FROM_SDP = "Unable to extract media information from SDP";
    private static final String SPACE = " ";
    private static final String AUDIO = "audio";
    private static final String DASH = "-";
    private static final String ERROR_ADDING_MEDIA_DESCRIPTION_S_TO_SDP = "Error adding media description to SDP";
    private static final String SKIPPING_MEDIA_DESCRIPTION_ATTRIBUTE_WITHOUT_NUMERIC_VALUE = "Skipping media description attribute without numeric value";
    private static final String RTPMAP = "rtpmap";
    private static final String FMTP = "fmtp";
    private static final String UNABLE_TO_EXTRACT_MEDIA_FORMATS_FROM_MEDIA_DESCRIPTION = "Unable to extract media formats from media description";
    private static final String IP4 = "IP4";
    private static final String IN = "IN";
    private static final int DEFAULT_PORT = 9876;
    private static final int DYNAMIC_MEDIA_PAYLOAD_TYPE_THRESHOLD = 96;

    private static Log log = LogFactory.getLog(SessionDescriptionHelper.class);

    private SessionDescriptionHelper() {}

    public static SessionDescription createSessionDescription(String sessionDescription) {
        try {
            return SdpFactory.getInstance().createSessionDescription(sessionDescription);
        } catch (SdpParseException e) {
            throw new StackException(UNABLE_TO_CREATE_SESSION_DESCRIPTION, e);
        }
    }

    public static SessionDescription createSessionDescription(String hostIpAddress, String sessionName) {
        if(hostIpAddress == null || sessionName == null)
            throw new IllegalArgumentException("Host IP address and session name must both be specified");

        try {
            SdpFactory sdpFactory = SdpFactory.getInstance();

            SessionDescription sd = sdpFactory.createSessionDescription();

            Origin o = sdpFactory.createOrigin(DASH, System.currentTimeMillis(), System.currentTimeMillis(), IN, IP4, hostIpAddress);
            sd.setOrigin(o);

            sd.setSessionName(sdpFactory.createSessionName(sessionName));

            return sd;
        } catch(SdpException e) {
            throw new StackException(UNABLE_TO_CREATE_SESSION_DESCRIPTION, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static boolean hasActiveVideoMediaDescription(SessionDescription sessionDescription) {
        try {
            List mediaDescriptionsVector = sessionDescription.getMediaDescriptions(false);
            if (null == mediaDescriptionsVector) return false;

            for (int i = 0; i < mediaDescriptionsVector.size(); i++) {
                MediaDescription mediaDescription = (MediaDescription) mediaDescriptionsVector.get(i);
                if (mediaDescription.getMedia().getMediaType().equals(AUDIO)) continue;
                if (mediaDescription.getMedia().getMediaType().equals(VIDEO)
                        && mediaDescription.getMedia().getMediaPort() > 0) return true;
            }
            return false;
        } catch (SdpException e) {
            throw new StackException(UNABLE_TO_EXTRACT_MEDIA_INFORMATION_FROM_SDP, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static MediaDescription getActiveMediaDescription(SessionDescription sessionDescription) {
        try {
            List mediaDescriptionsVector = sessionDescription.getMediaDescriptions(false);
            if (mediaDescriptionsVector == null) {
                log.debug(String.format("No media descriptions found in session descrption"));
                return null;
            }

            MediaDescription templateMediaDescription = getLastNonVideoMediaDescription(mediaDescriptionsVector);
            if (templateMediaDescription == null) {
                log.debug(String.format("No non-video media descriptions found in session descrption"));
                return null;
            }

            MediaDescription result = cloneMediaDescription(templateMediaDescription);
            if (templateMediaDescription.getConnection() == null
             &&	sessionDescription.getConnection() != null) {
                result.setConnection(sessionDescription.getConnection());
            }
            return result;
        } catch (SdpException e) {
            throw new StackException(UNABLE_TO_EXTRACT_MEDIA_INFORMATION_FROM_SDP, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static MediaDescription getLastNonVideoMediaDescription(List mediaDescriptionVector) throws SdpParseException {
        for (int i = mediaDescriptionVector.size() - 1; i > -1; i--) {
            MediaDescription mediaDescription = (MediaDescription)mediaDescriptionVector.get(i);
            if (mediaDescription.getMedia().getMediaType().equals(AUDIO))
                return mediaDescription;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static MediaDescription cloneMediaDescription(MediaDescription source) {
        if (source == null)
            return null;
        try {
            Media sourceMedia = source.getMedia();
            List mediaFormatsVector = sourceMedia.getMediaFormats(true);
            String[] mediaFormats = new String[mediaFormatsVector.size()];
            for (int i = 0; i < mediaFormatsVector.size(); i++) {
                mediaFormats[i] = mediaFormatsVector.get(i).toString();
            }
            MediaDescription target = SdpFactory.getInstance().createMediaDescription(sourceMedia.getMediaType(), sourceMedia.getMediaPort(), sourceMedia.getPortCount(), sourceMedia.getProtocol(), mediaFormats);
            if (source.getConnection() != null) {
                Connection connection = SdpFactory.getInstance().createConnection(source.getConnection().getAddress());
                target.setConnection(connection);
            }

            List<Attribute> targetAttributesVector = new Vector<Attribute>();
            List sourceAttributesVector = source.getAttributes(true);
            for (int i = 0; i < sourceAttributesVector.size(); i++) {
                Attribute sourceAttribute = (Attribute)sourceAttributesVector.get(i);
                Attribute targetAttribute = SdpFactory.getInstance().createAttribute(sourceAttribute.getName(), sourceAttribute.getValue());
                targetAttributesVector.add(targetAttribute);
            }
            target.setAttributes((Vector)targetAttributesVector);

            return target;
        } catch(SdpException e) {
            throw new StackException("Failed to clone media description", e);
        }
    }

    public static void setMediaDescription(SessionDescription sessionDescription, MediaDescription mediaDescriptions) {
        setMediaDescription(sessionDescription, mediaDescriptions, null);
    }

    public static void setMediaDescription(SessionDescription sessionDescription, MediaDescription mediaDescription, Map<String, String> dynamicPayloadMap) {
        List<MediaDescription> mediaDescriptions = new Vector<MediaDescription>();
        mediaDescriptions.add(mediaDescription);
        setMediaDescription(sessionDescription, mediaDescriptions, dynamicPayloadMap);
    }

    @SuppressWarnings("unchecked")
    public static void setMediaDescription(SessionDescription sessionDescription, List<MediaDescription> mediaDescriptions, Map<String, String> dynamicPayloadMap) {
        if (mediaDescriptions == null || mediaDescriptions.size() == 0)
            throw new IllegalArgumentException("Media description must not be null");

        if (sessionDescription == null)
            throw new IllegalArgumentException("SDP must not be null");

        if (sessionDescription.getConnection() == null) {
            for (MediaDescription mediaDescription : mediaDescriptions) {
                if (mediaDescription.getConnection() == null)
                    throw new IllegalArgumentException("SDP must have a Connection (c=) attribute either at session or at media level");
            }
        }

        log.debug(String.format("Setting media %s in session description", mediaDescriptions.toString().replace("\n", "").replace("\r", "")));


        try {
            Origin o = SdpFactory.getInstance().createOrigin(DASH, System.currentTimeMillis(), System.currentTimeMillis(), IN, IP4, sessionDescription.getOrigin().getAddress());
            sessionDescription.setOrigin(o);
        } catch(SdpException e) {
            throw new StackException(ERROR_ADDING_MEDIA_DESCRIPTION_S_TO_SDP, e);
        }

        List<MediaDescription> newMediaDescriptionVector = new Vector<MediaDescription>();
        for (MediaDescription mediaDescription : mediaDescriptions) {
            MediaDescription newMediaDescription = cloneMediaDescription(mediaDescription);
            //removeDynamicPayloadTypesFromMediaDescription(newMediaDescription);
            if (dynamicPayloadMap != null)
                mapDynamicPayloadTypes(newMediaDescription, dynamicPayloadMap);
            else
                log.debug("NOT mapping dynamic media payload types when adding media to SDP");
            newMediaDescriptionVector.add(newMediaDescription);
        }

        try {
            sessionDescription.setMediaDescriptions((Vector)newMediaDescriptionVector);

            if (newMediaDescriptionVector.get(0).getConnection() != null) {
                Connection sessionConnection = SdpFactory.getInstance().createConnection(newMediaDescriptionVector.get(0).getConnection().getAddress());
                sessionDescription.setConnection(sessionConnection);
            }
        } catch(SdpException e) {
            throw new StackException(ERROR_ADDING_MEDIA_DESCRIPTION_S_TO_SDP, e);
        }
    }

    @SuppressWarnings("unchecked")
    protected static void mapDynamicPayloadTypes(MediaDescription mediaDescription, Map<String, String> dynamicPayloadMap) {
        if (dynamicPayloadMap == null)
            return;

        Map<String, String> impliedDynamicPayloadTypeMap = extractDynamicMediaPayloadMappings(mediaDescription, false);

        List<String> mediaFormatsVector = new Vector<String>();
        try {
            Iterator iter = mediaDescription.getMedia().getMediaFormats(true).iterator();
            while (iter.hasNext()) {
                String currentFormatString = (String)iter.next();
                int currentFormat = Integer.parseInt(currentFormatString);
                if (currentFormat < DYNAMIC_MEDIA_PAYLOAD_TYPE_THRESHOLD)
                    mediaFormatsVector.add(currentFormatString);
                else {
                    String codec = impliedDynamicPayloadTypeMap.get(currentFormatString);
                    String mappedFormat = dynamicPayloadMap.get(codec);
                    if (mappedFormat != null)
                        mediaFormatsVector.add(mappedFormat);
                    else
                        log.debug(String.format("No dynamic mapping for codec %s", codec));
                }
            }
            mediaDescription.getMedia().setMediaFormats((Vector)mediaFormatsVector);

            List<Attribute> attributesVector = new Vector<Attribute>();
            Iterator iterAttr = mediaDescription.getAttributes(true).iterator();
            while (iterAttr.hasNext()) {
                Attribute currentAttribute = (Attribute)iterAttr.next();
                if (addCurrentAttribute(currentAttribute, impliedDynamicPayloadTypeMap, dynamicPayloadMap))
                    attributesVector.add(currentAttribute);

            }

            mediaDescription.setAttributes((Vector)attributesVector);
        } catch (SdpParseException e) {
            throw new StackException(UNABLE_TO_EXTRACT_MEDIA_FORMATS_FROM_MEDIA_DESCRIPTION, e);
        } catch (SdpException e) {
            throw new StackException(UNABLE_TO_EXTRACT_MEDIA_FORMATS_FROM_MEDIA_DESCRIPTION, e);
        }
    }

    private static boolean addCurrentAttribute(Attribute currentAttribute, Map<String,String> impliedDynamicPayloadTypeMap, Map<String, String> dynamicPayloadMap) throws SdpException{
    	if ( ! RTPMAP.equals(currentAttribute.getName()) && ! FMTP.equals(currentAttribute.getName())) return true;
        if ( ! currentAttribute.hasValue()) return true;
        String currentAttributeValue = currentAttribute.getValue();
        String[] split = currentAttributeValue.split(SPACE);
        if (split.length < 2) return true;
        String formatString = split[0].trim();
        String value = currentAttributeValue.substring(currentAttributeValue.indexOf(SPACE)).trim();

        int format = -1;
        try {
        	format = Integer.parseInt(formatString);
        } catch (NumberFormatException e) {
        	log.warn(String.format("invalid SDP attribute: %s:%s", currentAttribute.getName(), currentAttributeValue));
        	return true;
        }
        
        if (format < DYNAMIC_MEDIA_PAYLOAD_TYPE_THRESHOLD) return true;
        String codecForFormat = impliedDynamicPayloadTypeMap.get(formatString);
        if (codecForFormat == null) return true;
        if (dynamicPayloadMap.containsKey(codecForFormat)) {
            currentAttribute.setValue(String.format("%s %s", dynamicPayloadMap.get(codecForFormat), value));
            return true;
        }
        log.debug(String.format("LEAVING OUT media desc attribute %s:%s", currentAttribute.getName(), currentAttribute.getValue()));
        return false;
    }


    @SuppressWarnings("unchecked")
    private static Map<String,String> extractDynamicMediaPayloadMappings(MediaDescription mediaDescription, boolean useCodecAsKeyAndFormatAsValue) {
        Map<String, String> result = new HashMap<String, String>();
        List attributeList = mediaDescription.getAttributes(true);
        for (int i = 0; i < attributeList.size(); i++) {
            Attribute currentAttribute = (Attribute)attributeList.get(i);

            try {
                if ( ! RTPMAP.equals(currentAttribute.getName())) continue;
                if ( ! currentAttribute.hasValue()) continue;
                String currentAttributeValue = currentAttribute.getValue();
                int spaceIndex = currentAttributeValue.indexOf(SPACE);
                if (spaceIndex < 0 || spaceIndex > currentAttributeValue.length()-2)
                    continue;

                String formatString = currentAttributeValue.substring(0, spaceIndex);
                String codec = currentAttributeValue.substring(spaceIndex+1, currentAttributeValue.length());
                int format = Integer.parseInt(formatString);
                if (format < DYNAMIC_MEDIA_PAYLOAD_TYPE_THRESHOLD)
                    continue;

                if (useCodecAsKeyAndFormatAsValue)
                    result.put(codec, formatString);
                else
                    result.put(formatString, codec);
            } catch (NumberFormatException e) {
                log.debug(SKIPPING_MEDIA_DESCRIPTION_ATTRIBUTE_WITHOUT_NUMERIC_VALUE);
            } catch (SdpParseException e) {
                throw new StackException("Error updating dynamic media payload map", e);
            }
        }
        return result;
    }

    public static void updateDynamicMediaPayloadMappings(MediaDescription mediaDescription, Map<String, String> dynamicPayloadMap) {
        if (mediaDescription == null)
            return;

        Map<String,String> extractedDynamicPayloadsMap = extractDynamicMediaPayloadMappings(mediaDescription, true);

        Iterator<Entry<String, String>> extractedCodecsIterator = extractedDynamicPayloadsMap.entrySet().iterator();
        while (extractedCodecsIterator.hasNext()) {
            Entry<String, String> currentEntry = extractedCodecsIterator.next();

            //if ( !"telephone-event/8000".equals(currentCodec))
            //	continue;

            log.debug(String.format("Setting mapping for codec %s to %s in dynamic media payload map", currentEntry.getKey(), currentEntry.getValue()));
            dynamicPayloadMap.put(currentEntry.getKey(), currentEntry.getValue());
        }
    }

//  commented by Adrian as it's not currently used
//	@SuppressWarnings("unchecked")
//	protected static void removeDynamicPayloadTypesFromMediaDescription(MediaDescription mediaDescription) {
//		List<String> mediaFormatsVector = new Vector<String>();
//		Iterator<String> iter;
//		try {
//			iter = (Iterator<String>) mediaDescription.getMedia().getMediaFormats(true).iterator();
//			while (iter.hasNext()) {
//				String currentFormat = iter.next();
//				if (Integer.parseInt(currentFormat) < DYNAMIC_MEDIA_PAYLOAD_TYPE_THRESHOLD)
//					mediaFormatsVector.add(currentFormat);
//			}
//			mediaDescription.getMedia().setMediaFormats((Vector)mediaFormatsVector);
//
//			List<Attribute> attributesVector = new Vector<Attribute>();
//			Iterator<Attribute> iterAttr = (Iterator<Attribute>) mediaDescription.getAttributes(true).iterator();
//			while (iterAttr.hasNext()) {
//				Attribute currentAttribute = iterAttr.next();
//				if (currentAttribute.hasValue()) {
//					try {
//						int format = Integer.parseInt(currentAttribute.getValue().split(" ")[0]);
//						if (format >= DYNAMIC_MEDIA_PAYLOAD_TYPE_THRESHOLD)
//							continue;
//					} catch (NumberFormatException e) {
//						log.debug(SKIPPING_MEDIA_DESCRIPTION_ATTRIBUTE_WITHOUT_NUMERIC_VALUE);
//					}
//				}
//				attributesVector.add(currentAttribute);
//			}
//			mediaDescription.setAttributes((Vector)attributesVector);
//		} catch (SdpParseException e) {
//			throw new StackException(UNABLE_TO_EXTRACT_MEDIA_FORMATS_FROM_MEDIA_DESCRIPTION, e);
//		} catch (SdpException e) {
//			throw new StackException(UNABLE_TO_EXTRACT_MEDIA_FORMATS_FROM_MEDIA_DESCRIPTION, e);
//		}
//	}

    public static MediaDescription generateHoldMediaDescription() {
        return generateHoldMediaDescription(null);
    }

    @SuppressWarnings("unchecked")
    public static MediaDescription generateHoldMediaDescription(MediaDescription offerMediaDescription) {
        try {
            MediaDescription mediaDescription;
            List<Attribute> attributesVector = new Vector<Attribute>();
            if (offerMediaDescription == null) {
                mediaDescription = SdpFactory.getInstance().createMediaDescription(AUDIO, DEFAULT_PORT, 0, RTP_SLASH_AVP, new String[] {ZERO_STRING});
            } else {
                mediaDescription = cloneMediaDescription(offerMediaDescription);
                List offerAttributes = offerMediaDescription.getAttributes(true);
                for (int i = 0; i < offerAttributes.size(); i++) {
                    Attribute currentAttribute = (Attribute)offerAttributes.get(i);
                    if (!currentAttribute.getName().startsWith("send") && !currentAttribute.getName().startsWith("recv")) {
                        attributesVector.add(currentAttribute);
                    }
                }
            }

            if (attributesVector.size() < 1) {
                Attribute payloadTypeAttribute = SdpFactory.getInstance().createAttribute(RTPMAP, ZERO_SPACE_PCMU_8000);
                attributesVector.add(payloadTypeAttribute);
            }
            mediaDescription.setAttributes((Vector)attributesVector);
            mediaDescription.setAttribute("inactive", "");

            Connection connection = SdpFactory.getInstance().createConnection(ZERO_DOT_ZERO_DOT_ZERO_DOT_ZERO);
            mediaDescription.setConnection(connection);

            mediaDescription.getMedia().setMediaPort(DEFAULT_PORT);
            return mediaDescription;
        } catch(SdpException e) {
            throw new StackException(String.format("Error generating hold media description"), e);
        }
    }

    public static MediaDescription getVideoDescription() {
        try {
            final int arg1 = 5678;
            MediaDescription mediaDescription = SdpFactory.getInstance().createMediaDescription(VIDEO, arg1, 0, RTP_SLASH_AVP, new String[] {ZERO_STRING});
            mediaDescription.setAttribute(RTPMAP, ZERO_SPACE_PCMU_8000);
            Connection connection = SdpFactory.getInstance().createConnection(ZERO_DOT_ZERO_DOT_ZERO_DOT_ZERO);
            mediaDescription.setConnection(connection);
            return mediaDescription;
        } catch (SdpException e) {
            throw new StackException("Error during MediaDescription processing", e);
        }
    }
}
