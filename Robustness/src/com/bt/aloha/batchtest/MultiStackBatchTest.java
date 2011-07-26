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

 	

 	
 	
 
package com.bt.aloha.batchtest;

import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.aloha.batchtest.scenarios.InboundCallScenario;
import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.callleg.InboundCallLegBean;
import com.bt.aloha.callleg.OutboundCallLegBean;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.media.conference.collections.ConferenceCollection;
import com.bt.aloha.stack.SimpleSipStack;

public class MultiStackBatchTest extends BatchTest{
	private static Log log = LogFactory.getLog(MultiStackBatchTest.class);
	private DialogCollectionImplWithStats dialogCollection2;
	private CallCollectionImplWithStats callCollection2;
	private ConferenceCollectionImplWithStats conferenceCollection2;
	private ClassPathXmlApplicationContext mockPhoneStack;
	private ClassPathXmlApplicationContext loadBalancerStack;

	@Override
	public void init(){
		super.init();
		super.setApplicationContext(manager.getApplicationContext1());
        dialogCollection2 = new DialogCollectionImplWithStats((DialogCollection) manager.getApplicationContext2().getBean("dialogCollection"));
        callCollection2 = new CallCollectionImplWithStats((CallCollection) manager.getApplicationContext2().getBean("callCollection"));
        conferenceCollection2 = new ConferenceCollectionImplWithStats((ConferenceCollection) manager.getApplicationContext2().getBean("conferenceCollection"));
	}

	@Override
	public void destroy(){
		super.destroy();
		this.destroy2();
	}

	public void destroy2(){
		manager.getApplicationContext2().destroy();
	}

    @Override
	protected void assignNewCollectionsToBeans() {
    	super.assignNewCollectionsToBeans();
    	super.assignNewCollectionsToBeans(manager.getApplicationContext2(), dialogCollection2, callCollection2, conferenceCollection2);
    }

    @Override
    public void addBatchScenarios() {
        addBatchTestScenario("createCallTerminateCallScenario,10");
        addBatchTestScenario("basicConferenceScenario,1");
        addBatchTestScenario("byeScenario,10");
        addBatchTestScenario("inboundCallScenario,5");
        addBatchTestScenario("infoScenario,1");
        addBatchTestScenario("maxCallDurationScenario,5");
    }

    public void wireInboundScenarios(){
    	OutboundCallLegBean outboundCallLegBeanMockPhoneStack = (OutboundCallLegBean)mockPhoneStack.getBean("outboundCallLegBean");
    	InboundCallScenario inboundCallScenario = (InboundCallScenario)manager.getApplicationContext1().getBean("inboundCallScenario");
    	inboundCallScenario.setOutboundCallLegBeanFromMockphoneContext(outboundCallLegBeanMockPhoneStack);
    	InboundCallLegBean inboundCallLegBean1 = (InboundCallLegBean )manager.getApplicationContext1().getBean("inboundCallLegBean");
    	InboundCallLegBean inboundCallLegBean2 = (InboundCallLegBean )manager.getApplicationContext2().getBean("inboundCallLegBean");
    	inboundCallLegBean1.addInboundCallLegListener(inboundCallScenario);
    	inboundCallLegBean2.addInboundCallLegListener(inboundCallScenario);
        int loadBalancerPort = ((SimpleSipStack)loadBalancerStack.getBean("simpleSipStack")).getPort();
        String loadBalancerIp = ((SimpleSipStack)loadBalancerStack.getBean("simpleSipStack")).getIpAddress();
        inboundCallScenario.setIncomingUri("sip:fred@" + loadBalancerIp + ":" + loadBalancerPort);
    }
    
	public void go(){
		init();
        assignNewCollectionsToBeans();
        setExecutorService(Executors.newFixedThreadPool(getExecutorServiceNewFixedThreadPool()));
        addBatchScenarios();
        wireInboundScenarios();
        run();
	}

	public static void main(String[] args) {
		//BasicConfigurator.configure();
		MultistackApplicationContextManager manager = null;

        try {
    		manager = new MultistackApplicationContextManager(
    				new String[]{"batchTestApplicationContext.xml", "propertyListenerApplicationContext.1.xml"},
    				new String[]{"DatabaseBatchTestApplicationContext2.xml", "propertyListenerApplicationContext.2.xml"});
        }
        catch(Exception e){
        	log.error(e);
        	e.printStackTrace();
        	System.exit(1);
        }
        
        MultiStackBatchTest multiStackBatchTest = new MultiStackBatchTest();
        multiStackBatchTest.loadBalancerStack = new ClassPathXmlApplicationContext("sipLoadBalancerApplicationContext.xml");
        int loadBalancerPort = ((SimpleSipStack)multiStackBatchTest.loadBalancerStack.getBean("simpleSipStack")).getPort();
        manager.setContactPort(loadBalancerPort);
        
        manager.setContactAddress(manager.getApplicationContext1());
        manager.setContactAddress(manager.getApplicationContext2());
        
        manager.setLoadBalancerApplicationContext(multiStackBatchTest.loadBalancerStack);

        multiStackBatchTest.setApplicationContextManager(manager);
        multiStackBatchTest.setApplicationContext(manager.getApplicationContext1());
        BatchTest batchTest = (BatchTest)manager.getApplicationContext1().getBean("batchTestBean");

        multiStackBatchTest.mockPhoneStack = new ClassPathXmlApplicationContext("mockphonesApplicationContext.xml");
        
        multiStackBatchTest.setExecutorServiceNewFixedThreadPool(batchTest.getExecutorServiceNewFixedThreadPool());
        multiStackBatchTest.setNumberOfConcurrentStarts(batchTest.getNumberOfConcurrentStarts());
        multiStackBatchTest.setNumberOfRuns(batchTest.getNumberOfRuns());
        multiStackBatchTest.setMaximumScenarioCompletionWaitTimeSeconds(batchTest.getMaximumScenarioCompletionWaitTimeSeconds());
        multiStackBatchTest.setAudioFileUri(batchTest.getAudioFileUri());
        manager.setNumberOfRuns(batchTest.getNumberOfRuns());

        multiStackBatchTest.go();
        System.err.println("Number completed: " + multiStackBatchTest.numberCompleted());
        System.err.println("Access to ApplicationContext1 #: " + manager.getCountReturnedAppCtx1());
        System.err.println("Access to ApplicationContext2 #: " + manager.getCountReturnedAppCtx2());
        
        multiStackBatchTest.logFailedScenarios();

        System.exit(multiStackBatchTest.numberFailed());
	}
}
