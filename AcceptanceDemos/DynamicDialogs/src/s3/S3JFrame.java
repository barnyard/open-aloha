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
 * S3JFrame.java
 *
 * Created on 15 June 2007, 08:20
 */

package s3;

import java.net.URI;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.sdk.callcontrol.sip.call.CallBean;
import com.bt.sdk.callcontrol.sip.call.CallBeanImpl;
import com.bt.sdk.callcontrol.sip.call.CallListener;
import com.bt.sdk.callcontrol.sip.call.event.CallConnectedEvent;
import com.bt.sdk.callcontrol.sip.call.event.CallConnectionFailedEvent;
import com.bt.sdk.callcontrol.sip.call.event.CallDisconnectedEvent;
import com.bt.sdk.callcontrol.sip.call.event.CallTerminatedEvent;
import com.bt.sdk.callcontrol.sip.call.state.CallState;
import com.bt.sdk.callcontrol.sip.callleg.OutboundCallLegBean;
import com.bt.sdk.callcontrol.sip.dialog.state.DialogState;
import com.bt.sdk.callcontrol.sip.media.DtmfCollectCommand;
import com.bt.sdk.callcontrol.sip.media.MediaCallBean;
import com.bt.sdk.callcontrol.sip.media.MediaCallListener;
import com.bt.sdk.callcontrol.sip.media.convedia.MediaCallBeanImpl;
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
import com.bt.sdk.callcontrol.sip.stack.SimpleSipStack;

/**
 *
 * @author  802083751
 */
public class S3JFrame extends javax.swing.JFrame implements MediaCallListener, CallListener {
    private static final Log LOG = LogFactory.getLog(S3JFrame.class);
    private static final long serialVersionUID = -7130464613674839728L;
	private ApplicationContext applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");
    private MediaCallBean mediaCallBean = (MediaCallBean)applicationContext.getBean("mediaCallBean");
    private CallBean callBean = (CallBean)applicationContext.getBean("callBean");
    private OutboundCallLegBean outboundCallLegBean = (OutboundCallLegBean)applicationContext.getBean("outboundCallLegBean");
    private Semaphore callConnectedSemaphore = new Semaphore(0);
    private Semaphore announcementCompletedSemaphore = new Semaphore(0);
    private Hashtable<String, String> dialogs = new Hashtable<String, String>();
    private Hashtable<String, String> dtmfCollections = new Hashtable<String, String>();
    private Hashtable<String, String> mediaCallIds = new Hashtable<String, String>();


    /** Creates new form S3JFrame */
    public S3JFrame() {
        initComponents();
        
        SimpleSipStack stack = (SimpleSipStack)applicationContext.getBean("simpleSipStack");
        stack.setContactAddress("172.25.58.147:5060");

        ((MediaCallBeanImpl)mediaCallBean).setMediaCallListeners(Arrays.asList(new MediaCallListener[] {this}));
        ((CallBeanImpl)callBean).setCallListeners(Arrays.asList(new CallListener[] {this}));

        for (int i = 0; i < this.dialogList.getModel().getSize(); i++) {
            String sipAddress = (String)this.dialogList.getModel().getElementAt(i);
            String dialogId = outboundCallLegBean.createCallLeg(URI.create("sip:application@java"), URI.create(sipAddress));
            this.dialogs.put(sipAddress, dialogId);
        }
    }

    private String getDialogId(String sipAddress) {
        String id = this.dialogs.get(sipAddress);
        if (null == id ||
           DialogState.Terminated.equals(outboundCallLegBean.getCallLegInformation(id).getState())) {
            String newId = outboundCallLegBean.createCallLeg(URI.create("sip:application@java"), URI.create(sipAddress));
            this.dialogs.put(sipAddress, newId);
            return newId;
        }
        return id;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        jScrollPane1 = new javax.swing.JScrollPane();
        dialogList = new javax.swing.JList();
        joinDialogsButton = new javax.swing.JButton();
        playAnnouncementButton = new javax.swing.JButton();
        fileList = new javax.swing.JComboBox();
        dtmfDigits = new javax.swing.JTextField();
        sendDtmfButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        collectButton = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        digitsCollectedLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        dialogList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = {  
            		"sip:uros@10.237.33.51:10000", 
            		"sip:piotr@10.237.33.43:10000", 
            		"sip:raghav@132.146.185.199", 
            		"sip:fabc@132.146.185.199",
            		"sip:07918039798@10.238.67.22", 
            		"sip:adrian@132.146.185.199", 
            		"sip:07918029610@10.238.67.22", 
            		"sip:07918039480@10.238.67.22", 
            		"sip:07795986629@10.238.67.22", 
            		"sip:07918604418@10.238.67.22", 
            		"sip:08000121176@10.238.67.22" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jScrollPane1.setViewportView(dialogList);

        joinDialogsButton.setText("Join");
        joinDialogsButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                joinDialogsButtonMouseClicked(evt);
            }
        });

        playAnnouncementButton.setText("Play");
        playAnnouncementButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                playAnnouncementButtonMouseClicked(evt);
            }
        });

        fileList.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "/provisioned/select123hashstar.wav", "/provisioned/behave.wav", "file://mnt/172.25.19.54/uros/clips/behave.wav", "file://mnt/172.25.19.54/uros/clips/1s.wav", "file://mnt/172.25.19.54/uros/bubba_lite.wav", "file://mnt/172.25.19.54/uros/clips/prompt123.wav", "file://mnt/172.25.19.54/uros/karsten_loc.wav", "file://mnt/172.25.19.54/uros/karsten_dirk.wav" }));

        dtmfDigits.setText("12689620#");

        sendDtmfButton.setText("send");
        sendDtmfButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                sendDtmfButtonMouseClicked(evt);
            }
        });

        jLabel1.setText("DTMF Digits");

        jLabel2.setText("Media files:");

        collectButton.setText("collect");
        collectButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                collectButtonMouseClicked(evt);
            }
        });

        jLabel3.setText("Digits collected:");

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel2)
                .addContainerGap(337, Short.MAX_VALUE))
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(layout.createSequentialGroup()
                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 284, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 17, Short.MAX_VALUE)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                            .add(playAnnouncementButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(joinDialogsButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 64, Short.MAX_VALUE)))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, fileList, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 364, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(26, Short.MAX_VALUE))
            .add(layout.createSequentialGroup()
                .add(35, 35, 35)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(jLabel3)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(digitsCollectedLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 71, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(layout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(dtmfDigits, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 73, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(14, 14, 14)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(collectButton)
                            .add(sendDtmfButton))))
                .addContainerGap(156, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(43, 43, 43)
                .add(jLabel2)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(fileList, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(21, 21, 21)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 162, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(30, 30, 30)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel1)
                            .add(sendDtmfButton)
                            .add(dtmfDigits, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(layout.createSequentialGroup()
                        .add(joinDialogsButton)
                        .add(34, 34, 34)
                        .add(playAnnouncementButton)))
                .add(18, 18, 18)
                .add(collectButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 33, Short.MAX_VALUE)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(digitsCollectedLabel))
                .add(25, 25, 25))
        );
        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void collectButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_collectButtonMouseClicked
        promptAndCollect();
    }//GEN-LAST:event_collectButtonMouseClicked

    private void promptAndCollect() {
        Object[] sipAddresses = this.dialogList.getSelectedValues();
        if (sipAddresses == null || sipAddresses.length < 1) return;
        String mediaCallId = getMediaCallId((String)sipAddresses[0]);
        DtmfCollectCommand dtmfCollectCommand = new DtmfCollectCommand(this.fileList.getModel().getSelectedItem().toString(),
                true, true, 20, 5, 5, 1, 2, '#', '*');
        String commandId = mediaCallBean.promptAndCollectDigits(mediaCallId, dtmfCollectCommand);
        this.dtmfCollections.put(commandId, (String)sipAddresses[0]);
    }

    private void sendDtmfButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sendDtmfButtonMouseClicked
        Object[] sipAddresses = this.dialogList.getSelectedValues();
        if (sipAddresses == null || sipAddresses.length < 1) return;
        if (this.dtmfDigits.getText().length() < 1) return;

        String mediaCallId = getMediaCallId((String)sipAddresses[0]);
        mediaCallBean.generateDtmfDigits(mediaCallId, this.dtmfDigits.getText());
    }//GEN-LAST:event_sendDtmfButtonMouseClicked

    private String getMediaCallId(String sipAddress){
        String mediaCallId = this.mediaCallIds.get(sipAddress);
        CallState callState = null;
        if (mediaCallId != null)
        	callState = callBean.getCallInformation(mediaCallId).getCallState();

        if (null == mediaCallId || CallState.Terminated.equals(callState)) {
            this.callConnectedSemaphore = new Semaphore(0);
            mediaCallId = mediaCallBean.createMediaCall(getDialogId(sipAddress));
            this.waitForCallConnectedEvent();
            this.mediaCallIds.put(sipAddress, mediaCallId);
        }
        return mediaCallId;
    }

    private void playAnnouncementButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_playAnnouncementButtonMouseClicked
        Object[] sipAddresses = this.dialogList.getSelectedValues();
        if (sipAddresses == null || sipAddresses.length < 1) return;

        String mediaCallId = getMediaCallId((String)sipAddresses[0]);
        mediaCallBean.playAnnouncement(mediaCallId, this.fileList.getModel().getSelectedItem().toString());
    }//GEN-LAST:event_playAnnouncementButtonMouseClicked

    private void joinDialogsButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_joinDialogsButtonMouseClicked
        Object[] sipAddresses = this.dialogList.getSelectedValues();
        if (sipAddresses == null || sipAddresses.length < 1) return;

        if (sipAddresses.length < 2) {
            outboundCallLegBean.connectCallLeg(getDialogId((String)sipAddresses[0]));
            return;
        }

        callBean.joinCallLegs(getDialogId((String)sipAddresses[0]), getDialogId((String)sipAddresses[1]));
    }//GEN-LAST:event_joinDialogsButtonMouseClicked

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new S3JFrame().setVisible(true);
            }
        });
    }

    private void waitForCallConnectedEvent() {
        try {
            this.callConnectedSemaphore.tryAcquire(15, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public void onCallConnected(CallConnectedEvent arg) {
        LOG.info(new java.util.Date().toString() + " ############# " + arg.getClass().getSimpleName() + ": " + arg.getCallId());
        this.callConnectedSemaphore.release();
    }

    public void onCallConnectionFailed(CallConnectionFailedEvent arg) {
    	LOG.info(new java.util.Date().toString() + " ############# " + arg.getClass().getSimpleName() + ": " + arg.getCallId());
    }

    public void onCallDisconnected(CallDisconnectedEvent arg) {
    	LOG.info(new java.util.Date().toString() + " ############# " + arg.getClass().getSimpleName() + ": " + arg.getCallId());

        String callId = arg.getCallId();

        if (this.mediaCallIds.containsValue(callId)) {
            for (String uri: this.mediaCallIds.keySet()) {
                if (this.mediaCallIds.get(uri).equals(callId)) {
                    this.mediaCallIds.remove(uri);
                }
            }

        }
    }

    public void onCallTerminated(CallTerminatedEvent arg) {
    	LOG.info(new java.util.Date().toString() + " ############# " + arg.getClass().getSimpleName() + ": " + arg.getCallId());
        String callId = arg.getCallId();
        if (this.mediaCallIds.containsKey(callId)) {
            this.mediaCallIds.remove(callId);
        }
    }

    public void onCallAnnouncementCompleted(CallAnnouncementCompletedEvent arg) {
    	LOG.info(new java.util.Date().toString() + " ############# " + arg.getClass().getSimpleName() + ": " + arg.getCallId());
        this.announcementCompletedSemaphore.release();
    }

    public void onCallAnnouncementFailed(CallAnnouncementFailedEvent arg) {
    	LOG.info(new java.util.Date().toString() + " ############# " + arg.getClass().getSimpleName() + ": " + arg.getCallId());
    }

    public void onCallPromptAndCollectDigitsCompleted(CallPromptAndCollectDigitsCompletedEvent arg) {
    	LOG.info(new java.util.Date().toString() + " ############# " + arg.getClass().getSimpleName() + ": " + arg.getCallId());
        String digits = arg.getDigits();
        this.digitsCollectedLabel.setText(digits);
        System.out.println("==================== digits: " + digits);
        String commandId = arg.getMediaCommandId();
        System.out.println("==================== commandId: " + commandId);
        if (digits.length() < 1) return;
        char digit = digits.charAt(0);
        System.out.println("==================== digit: " + digit);
        switch (digit) {
            case '1':
                {
                    String sipAddress = this.dtmfCollections.get(commandId);
                    String mediaCallId = getMediaCallId(sipAddress);
                    mediaCallBean.playAnnouncement(mediaCallId, "file://mnt/172.25.19.54/uros/clips/cat.wav");
                }
                break;
            case '2':
                {
                    String sipAddress = this.dtmfCollections.get(commandId);
                    String mediaCallId = getMediaCallId(sipAddress);
                    mediaCallBean.playAnnouncement(mediaCallId, "file://mnt/172.25.19.54/uros/bubba_lite.wav");
                }
                break;
            case '3':
                {
                    String sipAddress = this.dtmfCollections.get(commandId);
                    String mediaCallId = getMediaCallId(sipAddress);
                    mediaCallBean.playAnnouncement(mediaCallId, "file://mnt/172.25.19.54/uros/karsten_loc.wav");
                }
                break;
            default:
                break;
        }
    }

    public void onCallPromptAndCollectDigitsFailed(CallPromptAndCollectDigitsFailedEvent arg) {
    	LOG.info(new java.util.Date().toString() + " ############# " + arg.getClass().getSimpleName() + ": " + arg.getCallId());
        String digits = arg.getDigits();
        this.digitsCollectedLabel.setText(digits);
        promptAndCollect();
    }

    public void onCallDtmfGenerationCompleted(CallDtmfGenerationCompletedEvent arg) {
    	LOG.info(new java.util.Date().toString() + " ############# " + arg.getClass().getSimpleName() + ": " + arg.getCallId());
    }

    public void onCallDtmfGenerationFailed(CallDtmfGenerationFailedEvent arg) {
    	LOG.info(new java.util.Date().toString() + " ############# " + arg.getClass().getSimpleName() + ": " + arg.getCallId());
    }

    public void onCallAnnouncementTerminated(CallAnnouncementTerminatedEvent arg) {
    	LOG.info(new java.util.Date().toString() + " ############# " + arg.getClass().getSimpleName() + ": " + arg.getCallId());
    }

    public void onCallPromptAndCollectDigitsTerminated(CallPromptAndCollectDigitsTerminatedEvent arg) {
    	LOG.info(new java.util.Date().toString() + " ############# " + arg.getClass().getSimpleName() + ": " + arg.getCallId());
    }

    public void onCallPromptAndRecordCompleted(CallPromptAndRecordCompletedEvent arg) {
    	LOG.info(new java.util.Date().toString() + " ############# " + arg.getClass().getSimpleName() + ": " + arg.getCallId());
    }

    public void onCallPromptAndRecordFailed(CallPromptAndRecordFailedEvent arg) {
    	LOG.info(new java.util.Date().toString() + " ############# " + arg.getClass().getSimpleName() + ": " + arg.getCallId());
    }

    public void onCallPromptAndRecordTerminated(CallPromptAndRecordTerminatedEvent arg) {
    	LOG.info(new java.util.Date().toString() + " ############# " + arg.getClass().getSimpleName() + ": " + arg.getCallId());
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton collectButton;
    private javax.swing.JList dialogList;
    private javax.swing.JLabel digitsCollectedLabel;
    private javax.swing.JTextField dtmfDigits;
    private javax.swing.JComboBox fileList;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton joinDialogsButton;
    private javax.swing.JButton playAnnouncementButton;
    private javax.swing.JButton sendDtmfButton;
    // End of variables declaration//GEN-END:variables
}
