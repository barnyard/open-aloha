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
/*
 * Main.java
 *
 * Created on 10 July 2007, 11:39
 */

package conferencing;

import java.net.URI;
import java.util.Arrays;
import java.util.Hashtable;

import javax.swing.DefaultListModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.sdk.callcontrol.sip.call.CallBean;
import com.bt.sdk.callcontrol.sip.call.CallListener;
import com.bt.sdk.callcontrol.sip.call.event.CallConnectedEvent;
import com.bt.sdk.callcontrol.sip.call.event.CallConnectionFailedEvent;
import com.bt.sdk.callcontrol.sip.call.event.CallDisconnectedEvent;
import com.bt.sdk.callcontrol.sip.call.event.CallTerminatedEvent;
import com.bt.sdk.callcontrol.sip.callleg.InboundCallLegBean;
import com.bt.sdk.callcontrol.sip.callleg.InboundCallLegListener;
import com.bt.sdk.callcontrol.sip.callleg.OutboundCallLegBean;
import com.bt.sdk.callcontrol.sip.callleg.OutboundCallLegListener;
import com.bt.sdk.callcontrol.sip.callleg.event.CallLegAlertingEvent;
import com.bt.sdk.callcontrol.sip.callleg.event.CallLegConnectedEvent;
import com.bt.sdk.callcontrol.sip.callleg.event.CallLegConnectionFailedEvent;
import com.bt.sdk.callcontrol.sip.callleg.event.CallLegDisconnectedEvent;
import com.bt.sdk.callcontrol.sip.callleg.event.CallLegRefreshCompletedEvent;
import com.bt.sdk.callcontrol.sip.callleg.event.CallLegTerminatedEvent;
import com.bt.sdk.callcontrol.sip.callleg.event.CallLegTerminationFailedEvent;
import com.bt.sdk.callcontrol.sip.callleg.event.IncomingCallLegEvent;
import com.bt.sdk.callcontrol.sip.callleg.event.ReceivedCallLegRefreshEvent;
import com.bt.sdk.callcontrol.sip.dialog.event.DialogAlertingEvent;
import com.bt.sdk.callcontrol.sip.dialog.event.DialogConnectedEvent;
import com.bt.sdk.callcontrol.sip.dialog.event.DialogConnectionFailedEvent;
import com.bt.sdk.callcontrol.sip.dialog.event.DialogDisconnectedEvent;
import com.bt.sdk.callcontrol.sip.dialog.event.DialogRefreshCompletedEvent;
import com.bt.sdk.callcontrol.sip.dialog.event.DialogTerminatedEvent;
import com.bt.sdk.callcontrol.sip.dialog.event.DialogTerminationFailedEvent;
import com.bt.sdk.callcontrol.sip.dialog.event.IncomingAction;
import com.bt.sdk.callcontrol.sip.media.DtmfCollectCommand;
import com.bt.sdk.callcontrol.sip.media.MediaCallBean;
import com.bt.sdk.callcontrol.sip.media.MediaCallListener;
import com.bt.sdk.callcontrol.sip.media.conference.event.ConferenceActiveEvent;
import com.bt.sdk.callcontrol.sip.media.conference.event.ConferenceEndedEvent;
import com.bt.sdk.callcontrol.sip.media.conference.event.ParticipantConnectedEvent;
import com.bt.sdk.callcontrol.sip.media.conference.event.ParticipantDisconnectedEvent;
import com.bt.sdk.callcontrol.sip.media.conference.event.ParticipantFailedEvent;
import com.bt.sdk.callcontrol.sip.media.conference.event.ParticipantTerminatedEvent;
import com.bt.sdk.callcontrol.sip.media.convedia.MediaCallBeanImpl;
import com.bt.sdk.callcontrol.sip.media.convedia.conference.ConferenceBean;
import com.bt.sdk.callcontrol.sip.media.convedia.conference.ConferenceBeanImpl;
import com.bt.sdk.callcontrol.sip.media.convedia.conference.ConferenceListener;
import com.bt.sdk.callcontrol.sip.media.event.call.CallAnnouncementCompletedEvent;
import com.bt.sdk.callcontrol.sip.media.event.call.CallAnnouncementFailedEvent;
import com.bt.sdk.callcontrol.sip.media.event.call.CallAnnouncementTerminatedEvent;
import com.bt.sdk.callcontrol.sip.media.event.call.CallDtmfGenerationCompletedEvent;
import com.bt.sdk.callcontrol.sip.media.event.call.CallDtmfGenerationFailedEvent;
import com.bt.sdk.callcontrol.sip.media.event.call.CallPromptAndCollectDigitsCompletedEvent;
import com.bt.sdk.callcontrol.sip.media.event.call.CallPromptAndCollectDigitsFailedEvent;
import com.bt.sdk.callcontrol.sip.media.event.call.CallPromptAndCollectDigitsTerminatedEvent;
import com.bt.sdk.callcontrol.sip.media.event.call.CallPromptAndRecordCompletedEvent;
import com.bt.sdk.callcontrol.sip.media.event.call.CallPromptAndRecordFailedEvent;
import com.bt.sdk.callcontrol.sip.media.event.call.CallPromptAndRecordTerminatedEvent;

/**
 *
 * @author  802083751
 */
public class Main extends javax.swing.JFrame implements ConferenceListener, InboundCallLegListener, MediaCallListener, OutboundCallLegListener, CallListener {
    private Log log = LogFactory.getLog(this.getClass());
    private DefaultListModel participants = new DefaultListModel();
    private ClassPathXmlApplicationContext applicationContext;
    private ConferenceBean conferenceBean;
    private CallBean callBean;
    private Hashtable<String, String> dialogs = new Hashtable<String, String>(); // dialogId - > sipAddress
    private Hashtable<String, String> sipAddresses = new Hashtable<String, String>(); //  sipAddress -> dialogId
    private String confId;
    private String mediaCallId;
    private String incomingDialogId;
    private String incomingUri;
    private String commandId;
    private String raghavId;

    // Inbound call routing
    private InboundCallLegBean inboundCallLegBean;
    private OutboundCallLegBean outboundCallLegBean;

    // media
    private MediaCallBean mediaCallBean;


    /** Creates new form Main */
    public Main() {
        initComponents();
        this.participantList.setModel(this.participants);
        this.conferenceIdLabel.setText("No conference started");
        applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");

        conferenceBean = (ConferenceBean)applicationContext.getBean("conferenceBean");
        ((ConferenceBeanImpl)conferenceBean).setConferenceListeners(Arrays.asList(new ConferenceListener[] {this}));

        mediaCallBean = (MediaCallBean)applicationContext.getBean("mediaCallBean");
        ((MediaCallBeanImpl)mediaCallBean).setMediaCallListeners(Arrays.asList(new MediaCallListener[] {this}));

        outboundCallLegBean = (OutboundCallLegBean)applicationContext.getBean("outboundCallLegBean");
        outboundCallLegBean.addOutboundCallLegListener(this);

        inboundCallLegBean = (InboundCallLegBean)applicationContext.getBean("inboundCallLegBean");
        inboundCallLegBean.addInboundCallLegListener(this);

        callBean = (CallBean)applicationContext.getBean("callBean");
        callBean.addCallListener(this);

        clearError();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        jScrollPane1 = new javax.swing.JScrollPane();
        participantList = new javax.swing.JList();
        jLabel1 = new javax.swing.JLabel();
        addParticipantTextField = new javax.swing.JTextField();
        addParticipantButton = new javax.swing.JButton();
        removeParticipantButton = new javax.swing.JButton();
        createConferenceButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        conferenceIdLabel = new javax.swing.JLabel();
        endConferenceButton = new javax.swing.JButton();
        errorLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        jScrollPane1.setViewportView(participantList);

        jLabel1.setText("Conference participants");

        addParticipantTextField.setText("sip:07918029610@10.238.67.22");

        addParticipantButton.setText("Add");
        addParticipantButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                addParticipantButtonMouseClicked(evt);
            }
        });

        removeParticipantButton.setText("Remove");
        removeParticipantButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                removeParticipantButtonMouseClicked(evt);
            }
        });

        createConferenceButton.setText("Create Conference");
        createConferenceButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                createConferenceButtonMouseClicked(evt);
            }
        });

        jLabel2.setText("Conference Id:");

        endConferenceButton.setText("End Conference");
        endConferenceButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                endConferenceButtonMouseClicked(evt);
            }
        });

        errorLabel.setFont(new java.awt.Font("Tahoma", 1, 11));
        errorLabel.setForeground(new java.awt.Color(255, 51, 51));

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(layout.createSequentialGroup()
                                .add(jLabel2)
                                .add(18, 18, 18)
                                .add(conferenceIdLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 261, Short.MAX_VALUE))
                            .add(layout.createSequentialGroup()
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(layout.createSequentialGroup()
                                        .add(jLabel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 168, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(59, 59, 59))
                                    .add(layout.createSequentialGroup()
                                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 221, Short.MAX_VALUE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)))
                                .add(removeParticipantButton)
                                .add(54, 54, 54)))
                        .add(28, 28, 28)
                        .add(createConferenceButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(endConferenceButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 140, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(24, 24, 24))
                    .add(layout.createSequentialGroup()
                        .add(addParticipantTextField)
                        .add(21, 21, 21)
                        .add(addParticipantButton)
                        .add(433, 433, 433))
                    .add(layout.createSequentialGroup()
                        .add(errorLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 665, Short.MAX_VALUE)
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(conferenceIdLabel)
                    .add(createConferenceButton)
                    .add(endConferenceButton))
                .add(32, 32, 32)
                .add(jLabel1)
                .add(14, 14, 14)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(removeParticipantButton))
                .add(15, 15, 15)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(addParticipantTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(addParticipantButton))
                .add(19, 19, 19)
                .add(errorLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 32, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(19, Short.MAX_VALUE))
        );
        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        this.applicationContext.destroy();
    }//GEN-LAST:event_formWindowClosed

    private void endConferenceButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_endConferenceButtonMouseClicked
        clearError();
        if (null == this.confId) return;
        this.conferenceBean.endConference(this.confId);
        this.conferenceIdLabel.setText("No conference started");
    }//GEN-LAST:event_endConferenceButtonMouseClicked

    private void createConferenceButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_createConferenceButtonMouseClicked
        clearError();
        if (null != this.confId) {
            this.errorLabel.setText("please end the previous conference first");
            return;
        }
        this.participants.clear();
        this.dialogs.clear();
        this.sipAddresses.clear();
        this.confId = conferenceBean.createConference();
        this.conferenceIdLabel.setText(this.confId);
    }//GEN-LAST:event_createConferenceButtonMouseClicked

    private void removeParticipantButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_removeParticipantButtonMouseClicked
        clearError();
        Object[] addresses = this.participantList.getSelectedValues();
        if (null == addresses || addresses.length < 1) return;
        for (int i = 0; i < addresses.length; i++) {
            String dialogId = this.sipAddresses.get(addresses[i]);
            this.conferenceBean.terminateParticipant(this.confId, dialogId);
        }
    }//GEN-LAST:event_removeParticipantButtonMouseClicked

    private void addParticipantButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_addParticipantButtonMouseClicked
        clearError();
        String sipAddress = this.addParticipantTextField.getText();
        if (sipAddress.length() < 1) return;
        if (null == this.confId) {
            this.errorLabel.setText("No active conference!");
            return;
        }
        String dialogId;
        dialogId = conferenceBean.createParticipantCallLeg(this.confId, URI.create(sipAddress));
        this.dialogs.put(dialogId, sipAddress);
        this.conferenceBean.inviteParticipant(this.confId, dialogId);
    }//GEN-LAST:event_addParticipantButtonMouseClicked

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Main().setVisible(true);
            }
        });
    }

    public void onConferenceActive(ConferenceActiveEvent conferenceActiveEvent) {
    }

    public void onConferenceEnded(ConferenceEndedEvent conferenceEndedEvent) {
        this.confId = null;
        this.conferenceIdLabel.setText("No conference started");
        this.errorLabel.setText("ConferenceEnded");
    }

    public void onParticipantConnected(ParticipantConnectedEvent participantConnectedEvent) {
        log.info("################################################# " + participantConnectedEvent.getConferenceId() + " : " + participantConnectedEvent.getDialogId());
        String dialogId = participantConnectedEvent.getDialogId();
        String sipAddress = this.dialogs.get(dialogId);
        this.sipAddresses.put(sipAddress, dialogId);
        this.participants.addElement(sipAddress);
    }

    public void onParticipantTerminated(ParticipantTerminatedEvent participantTerminatedEvent) {
        String dialogId = participantTerminatedEvent.getDialogId();
        String sipAddress = this.dialogs.remove(dialogId);
        this.sipAddresses.remove(sipAddress);
        this.participants.removeElement(sipAddress);
    }

    public void onParticipantDisconnected(ParticipantDisconnectedEvent participantDisconnectedEvent) {
        String dialogId = participantDisconnectedEvent.getDialogId();
        String sipAddress = this.dialogs.remove(dialogId);
        this.sipAddresses.remove(sipAddress);
        this.participants.removeElement(sipAddress);
        this.errorLabel.setText(sipAddress + " has hung up");
    }

    public void onParticipantFailed(ParticipantFailedEvent participantFailedEvent) {
        String dialogId = participantFailedEvent.getDialogId();
        String sipAddress = this.dialogs.get(dialogId);
        this.errorLabel.setText(sipAddress + " failed to connect");
    }

    private void clearError() {
        this.errorLabel.setText("");
    }

    public void onIncomingCallLeg(IncomingCallLegEvent incomingCallLegEvent) {
        this.incomingDialogId = incomingCallLegEvent.getId();
        this.incomingUri = incomingCallLegEvent.getFromUri();
        incomingCallLegEvent.setIncomingCallAction(IncomingAction.None);

        // option 1 - connect to another phone - this works OK
        //raghavId = outboundCallLegBean.createDialog(URI.create(this.incomingUri), URI.create("sip:07918029610@10.238.67.22"));
        //String callId = this.callBean.joinDialogs(this.incomingDialogId, this.raghavId );

        // option 2 - connect to conference - this works OK
        //this.dialogs.put(this.incomingDialogId, this.incomingUri);
        //this.conferenceBean.inviteParticipant(this.confId, this.incomingDialogId);

        // option 3 - play dtmf
        this.mediaCallId = mediaCallBean.createMediaCall(this.incomingDialogId);
    }

    public void onDialogConnected(DialogConnectedEvent connectedEvent) {
    }

    public void onDialogConnectionFailed(DialogConnectionFailedEvent connectionFailedEvent) {
    }

    public void onDialogDisconnected(DialogDisconnectedEvent disconnectedEvent) {
    }

    public void onDialogTerminated(DialogTerminatedEvent terminatedEvent) {
    }

    public void onDialogTerminationFailed(DialogTerminationFailedEvent terminationFailedEvent) {
    }

    public void onDialogRefreshCompleted(DialogRefreshCompletedEvent dialogRefreshCompletedEvent) {
    }

    public void onCallAnnouncementCompleted(CallAnnouncementCompletedEvent callAnnouncementCompletedEvent) {
    }

    public void onCallAnnouncementFailed(CallAnnouncementFailedEvent callAnnouncementFailedEvent) {
    }

    public void onCallAnnouncementTerminated(CallAnnouncementTerminatedEvent callAnnouncementTerminatedEvent) {
    }

    public void onCallPromptAndCollectDigitsCompleted(CallPromptAndCollectDigitsCompletedEvent callPromptAndCollectDigitsCompletedEvent) {
        log.info("################################## " + callPromptAndCollectDigitsCompletedEvent.getDigits());
        if (callPromptAndCollectDigitsCompletedEvent.getMediaCommandId().equals(this.commandId)) {
            if (callPromptAndCollectDigitsCompletedEvent.getDigits().equals("1234")) {
                this.conferenceBean.inviteParticipant(this.confId, this.incomingDialogId);
                this.dialogs.put(this.incomingDialogId, this.incomingUri);
            } else {
                promptAndCollect();
            }
        }
    }

    public void onCallPromptAndCollectDigitsFailed(CallPromptAndCollectDigitsFailedEvent callPromptAndCollectDigitsFailedEvent) {
        log.info("################################## CallPromptAndCollectDigitsFailedEvent");
    }

    public void onCallPromptAndCollectDigitsTerminated(CallPromptAndCollectDigitsTerminatedEvent callPromptAndCollectDigitsTerminatedEvent) {
    }

    public void onCallDtmfGenerationCompleted(CallDtmfGenerationCompletedEvent callDtmfGenerationCompletedEvent) {
    }

    public void onCallDtmfGenerationFailed(CallDtmfGenerationFailedEvent callDtmfGenerationFailedEvent) {
    }

    public void onDialogAlerting(DialogAlertingEvent alertingEvent) {
    }

    private void promptAndCollect() {
//        DtmfCollectCommand command = new DtmfCollectCommand("file://mnt/172.25.19.54/uros/clips/enterpin.wav", true, true, 15, 15, 15, 4);
        DtmfCollectCommand command = new DtmfCollectCommand("file://mnt/172.25.58.146/audio/standupapp/welcome.wav", true, true, 15, 15, 15, 4);
        this.commandId = mediaCallBean.promptAndCollectDigits(this.mediaCallId, command);
    }

    public void onCallConnected(CallConnectedEvent callConnectedEvent) {
        log.info(new java.util.Date().toString() + " ############# " + callConnectedEvent.getClass().getSimpleName() + ": " + callConnectedEvent.getCallId());
        if (callConnectedEvent.getCallId().equals(this.mediaCallId)) {
            promptAndCollect();
        }
    }

    public void onCallConnectionFailed(CallConnectionFailedEvent callConnectionFailedEvent) {
    }

    public void onCallDisconnected(CallDisconnectedEvent callDisconnectedEvent) {
    }

    public void onCallTerminated(CallTerminatedEvent callTerminatedEvent) {
    }

    public void onCallLegConnected(CallLegConnectedEvent callLegConnectedEvent) {
    }

    public void onCallLegConnectionFailed(CallLegConnectionFailedEvent callLegConnectionFailedEvent) {
    }

    public void onCallLegDisconnected(CallLegDisconnectedEvent callLegDisconnectedEvent) {
    }

    public void onCallLegTerminated(CallLegTerminatedEvent callLegTerminatedEvent) {
    }

    public void onCallLegTerminationFailed(CallLegTerminationFailedEvent callLegTerminationFailedEvent) {
    }

    public void onCallLegRefreshCompleted(CallLegRefreshCompletedEvent callLegRefreshCompletedEvent) {
    }

    public void onCallLegAlerting(CallLegAlertingEvent callLegAlertingEvent) {
    }

    public void onReceivedCallLegRefresh(ReceivedCallLegRefreshEvent receivedCallLegRefreshEvent) {
    }

    public void onCallPromptAndRecordCompleted(CallPromptAndRecordCompletedEvent callPromptAndRecordCompletedEvent) {
    }

    public void onCallPromptAndRecordFailed(CallPromptAndRecordFailedEvent callPromptAndRecordFailedEvent) {
    }

    public void onCallPromptAndRecordTerminated(CallPromptAndRecordTerminatedEvent callPromptAndRecordTerminatedEvent) {
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addParticipantButton;
    private javax.swing.JTextField addParticipantTextField;
    private javax.swing.JLabel conferenceIdLabel;
    private javax.swing.JButton createConferenceButton;
    private javax.swing.JButton endConferenceButton;
    private javax.swing.JLabel errorLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JList participantList;
    private javax.swing.JButton removeParticipantButton;
    // End of variables declaration//GEN-END:variables
}
