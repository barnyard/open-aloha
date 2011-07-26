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

import java.text.ParseException;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.aloha.call.CallBean;
import com.bt.aloha.call.CallBeanImpl;
import com.bt.aloha.call.CallListener;
import com.bt.aloha.call.MaxCallDurationTermination;
import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.call.collections.PersistedCallCollectionImpl;
import com.bt.aloha.call.state.CallInfo;
import com.bt.aloha.fitnesse.siploadbalancer.SipLoadBalancer;
import com.bt.aloha.media.MediaCallBean;
import com.bt.aloha.media.MediaCallListener;
import com.bt.aloha.media.convedia.MediaCallBeanImpl;
import com.bt.aloha.media.convedia.conference.ConferenceBean;
import com.bt.aloha.media.convedia.conference.ConferenceBeanImpl;
import com.bt.aloha.media.convedia.conference.ConferenceListener;
import com.bt.aloha.stack.SimpleSipStack;

public class MultistackApplicationContextManager {

	private static Log log = LogFactory.getLog(MultistackApplicationContextManager.class);

	private int countReturnedAppCtx1 = 0;
	private int countReturnedAppCtx2 = 0;

	private static class ApplicationContextHolder{

		private static Log log = LogFactory.getLog(ApplicationContextHolder.class);
		private ClassPathXmlApplicationContext applicationContext;
		private String[] appCtxRes;
		private boolean running;
	
		public ApplicationContextHolder(String[] res){
			this.appCtxRes = res;
			if (appCtxRes == null)
				running = false;
		}
		
		private boolean init(){
			if (appCtxRes == null)
				return true;
			
			try{
				applicationContext = new ClassPathXmlApplicationContext(appCtxRes);
				running = true;
			} catch(RuntimeException e){
				StringBuffer b = new StringBuffer();
				for(String r : appCtxRes){
					b.append("\"").append(r).append("\", ");
				}
				log.error("Error initializing application context with " + b.toString());
				throw e;
			}
			int count = 3;
			while(!applicationContext.isRunning()){
				sleep();
				count--;
				if(count==0)
					break;
			}
			return true;
		}

		public synchronized boolean destroy() {
			running = false;
			applicationContext.close();
			int count = 3;
			while (applicationContext.isActive()) {
				count--;
				sleep();
				if(count==0)
					break;
			}
			return true;
		}

		private void sleep(){
			try {
				Thread.sleep(1 * 1000);
			} catch (Exception e) {

			}
		}

	}
	private int counter;

	private CallCollection callCollection1;
	private CallCollection callCollection2;

	private ApplicationContextHolder holder1;
	private ApplicationContextHolder holder2;
	private long numberOfRuns = -1;

	private long threshold2;

	private long threshold1;

	private long runCounter;

	private int contactPort = -1;

	private ApplicationContext loadBalancerApplicationContext;
	
	public MultistackApplicationContextManager(String[] appCtx1, String[] appCtx2){
		this.holder1 = new ApplicationContextHolder(appCtx1);
		this.holder2 = new ApplicationContextHolder(appCtx2);
		this.holder1.init();
		startApplicationContext2();
		callCollection1 = (CallCollection)this.holder1.applicationContext.getBean("callCollection");
		callCollection2 = holder2.applicationContext == null? null : (CallCollection)this.holder2.applicationContext.getBean("callCollection");
	}
	
	public void setNumberOfRuns(long nor){
		this.numberOfRuns = nor;
        threshold1 = numberOfRuns / 3;
        threshold2 = 2 * numberOfRuns / 3;
	}
	
	public void setContactAddress(ApplicationContext ac) {
		if (this.contactPort < 0)
			throw new RuntimeException("Unable to set ContactAddress, contact port not set");
		SimpleSipStack sss = (SimpleSipStack)ac.getBean("simpleSipStack");
		String ipAddress = sss.getIpAddress();
		sss.setContactAddress(ipAddress + ":" + this.contactPort);
	}

	public ClassPathXmlApplicationContext getApplicationContext1() {
		log.info("######## Returning APPCTX - 1");
		return holder1.applicationContext;
	}

	public ClassPathXmlApplicationContext getApplicationContext2() {
		log.info("######## Returning APPCTX - 2");
		return holder2.applicationContext;
	}

	private long getConnectingCalls(){
		long nocc = callCollection2.getNumberOfConnectingCalls();
		log.info("}------ Total num of calls in connecting state for the stack2: " + nocc );
		return nocc;
	}

	public void stopApplicationContext2(){
		long nocc = getConnectingCalls();
		int counter = 5;
		while(counter>0 && nocc>0){
			sleep(2000);
			nocc = getConnectingCalls();
			counter--;
		}
		
		log.debug("----------Printing out the call states of all calls in the collection----------");
		ConcurrentMap<String, CallInfo> allCalls = callCollection2.getAll();
		for (CallInfo callInfo : allCalls.values())
			log.debug(String.format("CallId %s: %s", callInfo.getId(), callInfo.getCallState().toString()));
		log.debug("--------------------------------------------------------------------------------");
		
		log.warn("Forcibly distroying app context 2");
		callCollection2 = null;
		removeAddressFromLoadBalancer(getApplicationContext2());
		holder2.destroy();
		
		((MaxCallDurationTermination)getApplicationContext1().getBean("maxCallDurationTermination")).runTask();
	}
	
	private void removeAddressFromLoadBalancer(ApplicationContext stackApplictionContext) {
		if (null == this.loadBalancerApplicationContext) return;
		SimpleSipStack simpleSipStack = (SimpleSipStack)stackApplictionContext.getBean("simpleSipStack");
		String address = "sip:" + simpleSipStack.getIpAddress() + ":" + simpleSipStack.getPort();
		SipLoadBalancer sipLoadBalancer = (SipLoadBalancer)this.loadBalancerApplicationContext.getBean("sipLoadBalancer");
		try {
			sipLoadBalancer.removeHost(address);
		} catch (ParseException e) {
			e.printStackTrace();
			throw new RuntimeException("error removing host from sip load balancer", e);
		}
		log.debug("removed " + address + " from SIP load balancer");
	}
	
	private void addAddressToLoadBalancer(ApplicationContext stackApplictionContext) {
		if (null == this.loadBalancerApplicationContext) return;
		SimpleSipStack simpleSipStack = (SimpleSipStack)stackApplictionContext.getBean("simpleSipStack");
		String address = "sip:" + simpleSipStack.getIpAddress() + ":" + simpleSipStack.getPort();
		SipLoadBalancer sipLoadBalancer = (SipLoadBalancer)this.loadBalancerApplicationContext.getBean("sipLoadBalancer");
		try {
			sipLoadBalancer.addHost(address);
		} catch (ParseException e) {
			e.printStackTrace();
			throw new RuntimeException("error adding host to sip load balancer", e);
		}
		log.debug("added " + address + " to SIP load balancer");
	}

	public void startApplicationContext2(){
		this.holder2.init();
		if (this.contactPort > -1)
			setContactAddress(getApplicationContext2());
		injectManagerIntoApplicatonContext1Beans();
		addAddressToLoadBalancer(getApplicationContext2());
	}

	public void injectManagerIntoApplicatonContext1Beans() {
		String[] multiStackBeanNames = getApplicationContext1().getBeanNamesForType(BatchTestScenarioBase.class);
		for (String beanName : multiStackBeanNames) {
			BatchTestScenarioBase bean = (BatchTestScenarioBase)getApplicationContext1().getBean(beanName);
			log.debug(String.format("injecting applicationContextManger into %s from ApplicationContext1", beanName));
			bean.setApplicationContextManager(this);
		}
	}
	
	public CallBean selectNextCallBean(CallListener callListener) {
		CallBean callBean = (CallBean)selectNextApplicationContext().getBean("callBean");
		if (!((CallBeanImpl)callBean).getCallListeners().contains(callListener))
			callBean.addCallListener(callListener);
		return callBean;
	}

	public MediaCallBean selectNextMediaCallBean(MediaCallListener mediaCallListener) {
		MediaCallBean mediaCallBean = (MediaCallBean)selectNextApplicationContext().getBean("mediaCallBean");
		if (!((MediaCallBeanImpl)mediaCallBean).getMediaCallListeners().contains(mediaCallListener))
			mediaCallBean.addMediaCallListener(mediaCallListener);
		return mediaCallBean;
	}

	public ConferenceBean selectNextConferenceBean(ConferenceListener conferenceListener) {
		ConferenceBean conferenceBean = (ConferenceBean)selectNextApplicationContext().getBean("conferenceBean");
		if (!((ConferenceBeanImpl)conferenceBean).getConferenceListeners().contains(conferenceListener))
			conferenceBean.addConferenceListener(conferenceListener);
		return conferenceBean;
	}

	private synchronized ClassPathXmlApplicationContext selectNextApplicationContext(){
		counter++;
		if(!holder2.running)
		{
			countReturnedAppCtx1++;
			return getApplicationContext1();
		}
		else
		{
			return randomRoundRobin();
		}
	}

	private ClassPathXmlApplicationContext standardRoundRobin() {
		if(counter % 2 == 0)
		{
			countReturnedAppCtx1++;
			return getApplicationContext1();
		}
		countReturnedAppCtx2++;
		return getApplicationContext2();
	}

	private ClassPathXmlApplicationContext randomRoundRobin() {
		Random r = new Random();
		int n = r.nextInt(2);
		if(n == 0)
		{
			countReturnedAppCtx1++;
			return getApplicationContext1();
		}
		countReturnedAppCtx2++;
		return getApplicationContext2();
	}

	public synchronized void doApplicationContextStartStop() {
		String counters = String.format("Counter %s, threshold1=%s, threshold2=%s", runCounter, threshold1, threshold2);
		log.info(">>>>>>>>>>> ------------------ <<<<<<<<<<<<");
		log.info(">>>>>>>>>>>" + counters);
		log.info(">>>>>>>>>>> Collection transients stack1: " + getTransientsSize(callCollection1));
		log.info(">>>>>>>>>>> Collection transients stack2: " + getTransientsSize(callCollection2));
		if(holder2.running && runCounter > threshold1 && runCounter < threshold2)
		{
			log.info(">>>>>>>>>>> Destroying stack 2 <<<<<<<<<<<<");
			stopApplicationContext2();
		}
		if(!holder2.running && runCounter > threshold2) {
			log.info(">>>>>>>>>>> Booting up stack 2 <<<<<<<<<<<<");
			startApplicationContext2();
		}
		log.info(">>>>>>>>>>> ------------------ <<<<<<<<<<<<");
		runCounter ++;
	}

	private void sleep(int ms){
		try {
			Thread.sleep(ms);
		} catch (Exception e) {

		}
	}

	private int getTransientsSize(CallCollection c){
		if (c == null || !(c instanceof PersistedCallCollectionImpl))
			return -1;
		return ((PersistedCallCollectionImpl)c).sizeTransients();
	}

	public int getCountReturnedAppCtx1() {
		return countReturnedAppCtx1;
	}

	public int getCountReturnedAppCtx2() {
		return countReturnedAppCtx2;
	}

	public void setContactPort(int _contactPort) {
		this.contactPort = _contactPort;
	}

	public void setLoadBalancerApplicationContext(ApplicationContext _loadBalancerApplicationContext) {
		this.loadBalancerApplicationContext = _loadBalancerApplicationContext;
	}
}
