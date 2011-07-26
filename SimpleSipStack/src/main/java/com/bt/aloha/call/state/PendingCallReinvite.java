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

 	

 	
 	
 
package com.bt.aloha.call.state;

import javax.sdp.MediaDescription;

import com.bt.aloha.callleg.event.ReceivedCallLegRefreshEvent;
import com.bt.aloha.dialog.event.ReceivedDialogRefreshEvent;
import com.bt.aloha.dialog.state.PendingReinvite;
import com.bt.aloha.stack.SessionDescriptionHelper;

public class PendingCallReinvite extends PendingReinvite {
    private static final long serialVersionUID = 2898827869374077103L;
    private String dialogId;
    private String remoteContact;
    private Boolean offerInOkResponse;

    // TODO: check that having added this ctor to allow creation of a CallInfo loaded from db, is OK.
    // reason: setting autoTerminate in the PendingReinvite
    public PendingCallReinvite(String aDialogId, MediaDescription aMediaDescription, Boolean aAutoTerminate, String theRemoteContact, String aApplicationData, Boolean aOfferInOkResponse) {
        //TODO: why is aAutoTerminate ignored?
        this(aDialogId, aMediaDescription, theRemoteContact, aApplicationData, aOfferInOkResponse);
    }

    protected PendingCallReinvite(String aDialogId, MediaDescription aMediaDescription, String theRemoteContact, String aApplicationData, Boolean aOfferInOkResponse) {
//        super(aMediaDescription, null, aApplicationData); // TODO: why was this using null?
        super(aMediaDescription, false, aApplicationData);
        dialogId = aDialogId;
        remoteContact = theRemoteContact;
        offerInOkResponse = aOfferInOkResponse;
    }

    public PendingCallReinvite(ReceivedCallLegRefreshEvent receivedCallLegRefreshEvent) {
        this(receivedCallLegRefreshEvent.getId(), receivedCallLegRefreshEvent.getMediaDescription(), receivedCallLegRefreshEvent.getRemoteContact(), receivedCallLegRefreshEvent.getApplicationData(), receivedCallLegRefreshEvent.isOfferInOkResponse());
    }

    public ReceivedCallLegRefreshEvent getReceivedCallLegRefreshEvent() {

        MediaDescription md = getMediaDescription();
        String rc = getRemoteContact();
        String ad = getApplicationData();
        ReceivedDialogRefreshEvent receivedDialogRefreshEvent =
            new ReceivedDialogRefreshEvent(dialogId, md, rc, ad, offerInOkResponse);

        return new ReceivedCallLegRefreshEvent(receivedDialogRefreshEvent);
    }

    @Override
    protected PendingCallReinvite clone() {
        return new PendingCallReinvite(dialogId, SessionDescriptionHelper.cloneMediaDescription(getMediaDescription()), remoteContact, getApplicationData(), offerInOkResponse);
    }

    public String getDialogId() {
        return dialogId;
    }

    public Boolean getOfferInOkResponse() {
        return offerInOkResponse;
    }

    public String getRemoteContact() {
        return remoteContact;
    }
}
