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

 	

 	
 	
 
package com.bt.aloha.fitnesse;

import java.net.URI;

import org.springframework.context.ApplicationContext;

import com.bt.aloha.call.CallBean;
import com.bt.aloha.call.CallBeanImpl;
import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.callleg.OutboundCallLegBean;
import com.bt.aloha.callleg.OutboundCallLegBeanImpl;
import com.bt.aloha.dialog.DialogJainSipListener;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.media.DtmfCollectCommand;
import com.bt.aloha.phones.SipulatorPhone;

import fit.Fixture;

public abstract class SimpleSipStackBaseFixture extends Fixture {

	protected ApplicationContext applicationContext;
	protected String ipAddressPattern = "127.0.0.1";

    protected int firstDialogCallAnswerTimeout = 0;
	protected int secondDialogCallAnswerTimeout = 0;

    protected OutboundCallLegBean outboundCallLegBean;
	protected CallBean callBean;
	protected DialogJainSipListener dialogSipListener;
	private CallCollection callCollection;
	private DialogCollection dialogCollection;

	protected String firstPhoneUri;
	protected String secondPhoneUri;
	protected String thirdPhoneUri;

	protected String firstDialogId;
	protected String secondDialogId;
	protected String thirdDialogId;

    protected int waitTimeoutSeconds;

    static{
		FixtureApplicationContexts.getInstance().startMockphonesApplicationContext();
	}

    public SimpleSipStackBaseFixture(ApplicationContext applicationContext) {
    	super();
    	this.applicationContext = applicationContext;

        outboundCallLegBean = (OutboundCallLegBean)applicationContext.getBean("outboundCallLegBean");
    	callBean = (CallBean)applicationContext.getBean("callBean");
    	dialogSipListener = (DialogJainSipListener)applicationContext.getBean("dialogSipListener");
    	callCollection = (CallCollection)applicationContext.getBean("callCollection");
    	dialogCollection = (DialogCollection)applicationContext.getBean("dialogCollection");
    	
    	((CallBeanImpl)callBean).setCallCollection(getCallCollection());
		((CallBeanImpl)callBean).setDialogCollection(getDialogCollection());
		((OutboundCallLegBeanImpl)outboundCallLegBean).setDialogCollection(getDialogCollection());
		dialogSipListener.setDialogCollection(getDialogCollection());
    }

    protected CallCollection getCallCollection() {
		return callCollection;
	}

	protected DialogCollection getDialogCollection() {
		return dialogCollection;
	}

	public void ipAddressPattern(String ipAddressPattern) {
		this.ipAddressPattern = ipAddressPattern;
	}

    public void waitTimeoutSeconds(int seconds) {
    	this.waitTimeoutSeconds = seconds;
    }

    public void waitSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
        }
    }

    public void waitMilliSeconds(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    public void firstDialogCallAnswerTimeout(int timeout) {
		this.firstDialogCallAnswerTimeout = timeout;
	}

	public void secondDialogCallAnswerTimeout(int timeout) {
		this.secondDialogCallAnswerTimeout = timeout;
	}

    public void firstPhoneUri(String firstPhone) {
		this.firstPhoneUri = getAddressAndPort(firstPhone);
	}

	public void secondPhoneUri(String secondPhone) {
		this.secondPhoneUri = getAddressAndPort(secondPhone);
	}

	public void thirdPhoneUri(String thirdPhone) {
		this.thirdPhoneUri = getAddressAndPort(thirdPhone);
	}

	public String createFirstDialog() {
		firstDialogId = outboundCallLegBean.createCallLeg(URI.create(secondPhoneUri), URI.create(firstPhoneUri), firstDialogCallAnswerTimeout);
		return "OK";
	}

	public String createSecondDialog() {
		secondDialogId = outboundCallLegBean.createCallLeg(URI.create(firstPhoneUri), URI.create(secondPhoneUri), secondDialogCallAnswerTimeout);
		return "OK";
	}

	public String createThirdDialog() {
		thirdDialogId = outboundCallLegBean.createCallLeg(URI.create(firstPhoneUri), URI.create(thirdPhoneUri));
		return "OK";
	}

	protected String getAddressAndPort(String address) {
		if (address == null)
			return null;

		String name = "";
		if (address.startsWith("sip:"))
			name = address.substring(0, address.indexOf("@")+1);

		if(address.indexOf(":", 5) > -1) {
			String ipAddress = address.substring(name.length(), address.lastIndexOf(":"));
			String port = address.substring(address.lastIndexOf(":"));
			return name + SipulatorPhone.lookupIpAddress(ipAddress) + port;
		} else {
			String ipAddress = address.substring(name.length(), address.length());
            System.out.println("returning " + name + SipulatorPhone.lookupIpAddress(ipAddress));
			return name + SipulatorPhone.lookupIpAddress(ipAddress);
		}
	}

    protected DtmfCollectCommand getDtmfCollectCommandWithMinMaxNumberOfDigitsAndReturnKey(String dtmfCollectCommandString, String audioFileUri) {
        String[] commandElements = dtmfCollectCommandString.split(",");
        Character cancelKey = null;
        if (commandElements.length > 8)
            cancelKey = new Character(commandElements[8].charAt(0));
        return new DtmfCollectCommand(
                audioFileUri,
                commandElements[0].toLowerCase().equals("true"),    // barge
                commandElements[1].toLowerCase().equals("true"),    // clear buffer
                Integer.parseInt(commandElements[2]),               // first digit timeout
                Integer.parseInt(commandElements[3]),               // inter digit timeout
                Integer.parseInt(commandElements[4]),               // last digit timeout
                Integer.parseInt(commandElements[5]),               // min number of digits
                Integer.parseInt(commandElements[6]),               // max number of digits
                commandElements[7].charAt(0),                       // return key
                cancelKey);
    }
}
