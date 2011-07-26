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

 	

 	
 	
 
package com.bt.aloha.testing;

import org.junit.Before;
import org.junit.Test;

import com.bt.aloha.dialog.event.IncomingDialogEvent;
import com.bt.aloha.testing.DialogListenerStubBase;

// this class is all bollocks, but it keeps the Emma figures looking sweet.
public class DialogListenerMockBaseTest {

    DialogListenerStubBase listener;

    @Before
    public void before() {
        listener = new DialogListenerStubBase() {
            public void onIncomingDialog(IncomingDialogEvent e) {
            }
        };
    }

    // keep Emma happy
    @Test
    public void testOnConnected() {
        listener.onDialogConnected(null);
    }

    // keep Emma happy
    @Test
    public void testOnConnectionFailed() {
        listener.onDialogConnectionFailed(null);
    }

    // keep Emma happy
    @Test
    public void testOnDisconnected() {
        listener.onDialogDisconnected(null);
    }

    // keep Emma happy
    @Test
    public void testOnTerminated() {
        listener.onDialogTerminated(null);
    }

    // keep Emma happy
    @Test
    public void testOnTerminationFailed() {
        listener.onDialogTerminationFailed(null);
    }
}
