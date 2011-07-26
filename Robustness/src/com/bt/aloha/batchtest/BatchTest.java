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

import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.aloha.batchtest.utils.AnnouncementCallScenarioExtractor;
import com.bt.aloha.batchtest.utils.BasicCallAndMediaCallSharedDialogScenarioExtractor;
import com.bt.aloha.batchtest.utils.BasicConferenceScenarioMultiStackExtractor;
import com.bt.aloha.batchtest.utils.ByeScenarioExtractor;
import com.bt.aloha.batchtest.utils.CallAnswerTimeoutScenarioExtractor;
import com.bt.aloha.batchtest.utils.ConferenceScenarioTerminateParticipantsExtractor;
import com.bt.aloha.batchtest.utils.MaxCallDurationScenarioExtractor;
import com.bt.aloha.batchtest.utils.TwoCallsSharingCallLegScenarioExtractor;
import com.bt.aloha.call.CallBeanImpl;
import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.callleg.InboundCallLegBeanImpl;
import com.bt.aloha.callleg.OutboundCallLegBeanImpl;
import com.bt.aloha.dialog.DialogJainSipListener;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.media.conference.collections.ConferenceCollection;
import com.bt.aloha.media.convedia.MediaCallBeanImpl;
import com.bt.aloha.media.convedia.MediaCallLegBeanImpl;
import com.bt.aloha.media.convedia.conference.ConferenceBeanImpl;

public class BatchTest implements BatchTestScenarioResultListener {
	protected MultistackApplicationContextManager manager;
    protected ClassPathXmlApplicationContext applicationContext;
    private static final Log log = LogFactory.getLog(BatchTest.class);
    private Map<String, Double> scenarios = new Hashtable<String, Double>();
    private String audioFileUri;
    private int maximumScenarioCompletionWaitTimeSeconds;
    private int numberOfRuns;
    protected int numberOfConcurrentStarts;
    protected int numberCompleted = 0;
    protected int numberSucceeded = 0;
    private long totalTime = 0;
    private ConcurrentMap<String, Result> results;
    private Map<String, Analysis> analysis = new TreeMap<String, Analysis>();

    private DialogCollectionImplWithStats dialogCollection;
    private CallCollectionImplWithStats callCollection;
    private ConferenceCollectionImplWithStats conferenceCollection;
    protected ExecutorService executorService;
    private int executorServiceNewFixedThreadPool;

    private Vector<String> failedScenarios = new Vector<String>();
    private TwoCallsSharingCallLegScenarioExtractor twoCallsSharingCallLegScenarioExtractor = new TwoCallsSharingCallLegScenarioExtractor("app.log");
    private ConferenceScenarioTerminateParticipantsExtractor conferenceScenarioTerminateParticipantsLogExtractor = new ConferenceScenarioTerminateParticipantsExtractor("app.log");
    private CallAnswerTimeoutScenarioExtractor callAnswerTimeoutScenarioLogExtractor = new CallAnswerTimeoutScenarioExtractor("app.log");
    private BasicCallAndMediaCallSharedDialogScenarioExtractor basicCallAndMediaCallSharedDialogScenarioLogExtractor = new BasicCallAndMediaCallSharedDialogScenarioExtractor("app.log");
    private BasicCallAndMediaCallSharedDialogScenarioExtractor promptAndCollectScenarioLogExtractor = new BasicCallAndMediaCallSharedDialogScenarioExtractor("app.log");
    private BasicConferenceScenarioMultiStackExtractor basicConferenceScenarioMultiStackExtractor = new BasicConferenceScenarioMultiStackExtractor("app.log");
    private AnnouncementCallScenarioExtractor announcementCallScenarioLogExtractor = new AnnouncementCallScenarioExtractor("app.log");
    private MaxCallDurationScenarioExtractor maxCallDurationScenarioLogExtractor = new MaxCallDurationScenarioExtractor("app.log");
    private ByeScenarioExtractor loadBalancedByeScenarioMultistackExtractor = new ByeScenarioExtractor("app.log");

    public void setApplicationContext(ClassPathXmlApplicationContext theApplicationContext){
        this.applicationContext = theApplicationContext;
    }

    public ClassPathXmlApplicationContext getApplicationContext(){
        return this.applicationContext;
    }


    public void setApplicationContextManager(MultistackApplicationContextManager m){
		this.manager = m;
	}

    public int getExecutorServiceNewFixedThreadPool(){
        return executorServiceNewFixedThreadPool;
    }
    public void setExecutorServiceNewFixedThreadPool(int theExecutorServiceNewFixedThreadPool){
        this.executorServiceNewFixedThreadPool = theExecutorServiceNewFixedThreadPool;
    }

    public void init() {
        dialogCollection = new DialogCollectionImplWithStats((DialogCollection)applicationContext.getBean("dialogCollection"));
        callCollection = new CallCollectionImplWithStats((CallCollection)applicationContext.getBean("callCollection"));
        conferenceCollection = new ConferenceCollectionImplWithStats((ConferenceCollection)applicationContext.getBean("conferenceCollection"));
    }

    public void destroy() {
        applicationContext.destroy();
    }

    @SuppressWarnings("unchecked")
    public void reset() {
        this.scenarios = new Hashtable<String, Double>();
        getCallCollection().reset();
        getDialogCollection().reset();
        getConferenceCollection().reset();

        Map<String, Resetable> resetables = (Map<String, Resetable>) applicationContext.getBeansOfType(Resetable.class);
        for(Resetable resetable : resetables.values())
            resetable.reset();
    }

    class Result {
        private boolean succeeded;
        private String message;

        public Result(boolean b){
            this.succeeded = b;
        }

        public Result(String m) {
            this.message = m;
        }

        public String getMessage() {
            return message;
        }
        public void setMessage(String message) {
            this.message = message;
        }
        public boolean isSucceeded() {
            return succeeded;
        }
        public void setSucceeded(boolean succeeded) {
            this.succeeded = succeeded;
        }

        @Override
        public String toString() {
            if (isSucceeded()) return "succeeded";
            return "failed: " + getMessage();
        }
    }

    class Analysis {
        private int successCount;
        private int failureCount;
        public int getFailureCount() {
            return failureCount;
        }
        public void setFailureCount(int failureCount) {
            this.failureCount = failureCount;
        }
        public int getSuccessCount() {
            return successCount;
        }
        public void setSuccessCount(int successCount) {
            this.successCount = successCount;
        }
        @Override
        public String toString() {
            StringBuffer result = new StringBuffer();
            int total = getSuccessCount() + getFailureCount();
            result.append(getSuccessCount() + "/" + total);
            float percentage = 0;
            if (total > 0)
                percentage = 100.0f * getSuccessCount() / total;
            result.append(" (" + percentage + "%)");
            return result.toString();
        }
    }

    class RunScenario implements Runnable {
        BatchTestScenario batchTestScenario;
        String beanName;
        CyclicBarrier barrier;
        CountDownLatch latch;

        public RunScenario(BatchTestScenario aBatchTestScenario, String aBeanName, CyclicBarrier aBarrier, CountDownLatch aLatch) {
            batchTestScenario = aBatchTestScenario;
            beanName = aBeanName;
            barrier = aBarrier;
            latch = aLatch;
        }

        public void run() {
            try {
                barrier.await();
                String scenarioId = batchTestScenario.start(beanName);
                results.putIfAbsent(scenarioId, new Result(false));

                log.info("Started scenario " + scenarioId + ", " + beanName);
            } catch(Throwable t) {
                log.error(String.format("Test for scenario %s threw an exception", beanName), t);
            } finally {
                latch.countDown();
            }
        }
    }

    public void setAudioFileUri(String theAudioFileUri) {
        audioFileUri = theAudioFileUri;
    }

    public String getAudioFileUri() {
		return audioFileUri;
	}

	public void addBatchTestScenario(String batchTestScenarioNameAndWeight) {
        String[] split = batchTestScenarioNameAndWeight.split(",");
        scenarios.put(split[0], Double.parseDouble(split[1]));
        analysis.put(split[0], new Analysis());
    }

    protected void normalizeWeightings() {
        double cumulative = 0;
        for (String key: scenarios.keySet())
            cumulative += scenarios.get(key).doubleValue();

        for (String key: scenarios.keySet()) {
            double currentValue = scenarios.get(key);
            scenarios.put(key.toString(), currentValue / cumulative);
        }
    }

    public void setMaximumScenarioCompletionWaitTimeSeconds(int t) {
        maximumScenarioCompletionWaitTimeSeconds = t;
    }

    public void setNumberOfRuns(int numberOfRuns) {
        this.numberOfRuns = numberOfRuns;
    }

    public void setNumberOfConcurrentStarts(int numberOfConcurrentStarts) {
        this.numberOfConcurrentStarts = numberOfConcurrentStarts;
    }

    /*
     * Run random scenario
     */
    public void run() {
        run(null);
    }

    /*
     * Run specific scenario
     */
    public void run(String scenarioName) {
        this.numberCompleted = 0;
        this.numberSucceeded = 0;
        this.results = new ConcurrentHashMap<String, Result>();
        normalizeWeightings();
        totalTime = 0;
        CyclicBarrier barrier = new CyclicBarrier(numberOfConcurrentStarts);
        for (int i = 0; i < numberOfRuns; i++) {
            try {
            	if(manager!=null)
            		manager.doApplicationContextStartStop();
                long startTime = System.currentTimeMillis();
                Thread.sleep(1000);
                String[] beans = new String[numberOfConcurrentStarts];
                BatchTestScenario[] concurrentScenarios = new BatchTestScenario[numberOfConcurrentStarts];
                for (int j=0; j<numberOfConcurrentStarts; j++) {
                    beans[j] = scenarioName == null ? pickScenarioName() : scenarioName;

                    BatchTestScenario s = (BatchTestScenario)applicationContext.getBean(beans[j]);
                    ((BatchTestScenarioBase)s).setCallCollection(callCollection);
                    ((BatchTestScenarioBase)s).setAudioFileUri(audioFileUri);

                    if (s.getBatchTestScenarioResultListener() == null)
                        s.setBatchTestScenarioResultListener(this);
                    concurrentScenarios[j] = s;
                }

                CountDownLatch latch = new CountDownLatch(numberOfConcurrentStarts);
                for (int j=0; j<numberOfConcurrentStarts; j++) {
                    if (concurrentScenarios[j] == null)
                        break;
                    RunScenario rs = new RunScenario(concurrentScenarios[j], beans[j], barrier, latch);
                    executorService.execute(rs);
                }
                latch.await();
                barrier.reset();
                totalTime += System.currentTimeMillis() - startTime;
            } catch(Throwable t) {
                log.error(String.format("Test run %d threw an exception", i), t);
            }
        }

        waitForAllToFinish(maximumScenarioCompletionWaitTimeSeconds);
        log.info("Finishing...");
        if (results.size() < 1) {
            log.info("NO scenarios run!");
            return;
        }
        numberSucceeded = 0;
        for (String o: results.keySet()) {
            String scenario = o.split(":")[0];
            Result res = results.get(o);
            Analysis result = analysis.get(scenario);
            if (result != null){
	            if (res.isSucceeded()) {
	                result.setSuccessCount(result.getSuccessCount() + 1);
	            } else {
	                result.setFailureCount(result.getFailureCount() + 1);
	            }
	            log.info(String.format("Scenario %s %s", o, res.toString()));
	            System.err.println(String.format("Scenario %s %s", o, res.toString()));
	            numberCompleted++;
	            if (res.isSucceeded())
	                numberSucceeded++;
	            else
	                failedScenarios.add(o);
            }
            else {
            	log.error("unable to find result for scenario " + scenario);
            }
        }
        log.info(numberSucceeded + " successful scenarios, " + successRate() + "% passed");
        log.info(numberCompleted());
        if(manager!=null){
	        log.info("Access to ApplicationContext1 #: " + manager.getCountReturnedAppCtx1());
	        log.info("Access to ApplicationContext2 #: " + manager.getCountReturnedAppCtx2());
        }
        resetDb();
    }

    protected void resetDb() {
    	MaintenanceDao dao = (MaintenanceDao)applicationContext.getBean("maintenanceDaoBean");
    	dao.truncateAllTables();
	}

	private void waitForAllToFinish(int totalTime) {
        log.info("waiting for scenarios to finish...");
        int loops = totalTime / 10;
        while (! allSucceeded() && loops > 0) {
            try {
                Thread.sleep(10 * 1000);
            } catch(InterruptedException e) {
                log.error(e.getMessage(), e);
            }
            loops--;
        }
        log.info("... finished waiting");
    }

    private boolean allSucceeded() {
        for (Result result: results.values()) {
            if ( ! result.isSucceeded()) {
                return false;
            }
        }
        return true;
    }

    protected String pickScenarioName() {
        double random = Math.random();
        double cumulative = 0;
        String beanName = null;
        for (String key : scenarios.keySet()) {
            cumulative += scenarios.get(key);
            if (cumulative > random) {
                beanName = key;
                break;
            }
        }
        return beanName;
    }

    public String numberCompleted() {
        StringBuffer result = new StringBuffer(this.numberCompleted + " tests run\n");
        result.append(String.format("Average delay between scenarios: %fms\n", this.numberOfRuns==0 ? 0 : this.totalTime/(double)this.numberOfRuns));
        for (String scenario: analysis.keySet()) {
            result.append(scenario + ": " + analysis.get(scenario).toString());
            result.append("\n");
        }
        result.append("Overall success rate: " + successRate() + "%");
        return result.toString();
    }

    public float successRate() {
        return 100 * (1.0f * numberSucceeded / numberCompleted);
    }

    public void runCompleted(String id, boolean successful) {
        if (results.containsKey(id)) {
            results.get(id).setSucceeded(successful);
        } else {
            results.put(id, new Result(successful));
        }
    }

    public void updateRunStatus(String id, String message) {
        if (results.containsKey(id))
            results.get(id).setMessage(message);
        else
            results.putIfAbsent(id, new Result(message));
    }

    public int numberFailed() {
        return this.numberCompleted - this.numberSucceeded;
    }

    // all hard-coded for now, will eventually read from props
    public static void main(String[] args)  throws Exception{
		MultistackApplicationContextManager manager = null;
        try {
    		manager = new MultistackApplicationContextManager(
    				new String[]{"batchTestApplicationContext.xml", "propertyListenerApplicationContext.1.xml"},
    				null);
        }
        catch(Exception e){
        	log.error(e);
        	e.printStackTrace();
        	System.exit(1);
        }

        BatchTest batchTest = (BatchTest)manager.getApplicationContext1().getBean("batchTestBean");
        batchTest.setApplicationContext(manager.getApplicationContext1());
        batchTest.setApplicationContextManager(manager);
        batchTest.init();
        batchTest.assignNewCollectionsToBeans();
        batchTest.setExecutorService(Executors.newFixedThreadPool(batchTest.getExecutorServiceNewFixedThreadPool()));
        batchTest.addBatchScenarios();
        batchTest.run();
        log.info(String.format("Number completed: %s", batchTest.numberCompleted()));
        batchTest.destroy();

        int dialogInvocationCount = batchTest.getDialogCollection().getInvocationCounterSize();
        int callInvocationCount = batchTest.getCallCollection().getInvocationCounterSize();
        int conferenceInvocationCount = batchTest.getConferenceCollection().getInvocationCounterSize();
        int dialogExceptionsCount = batchTest.getDialogCollection().getExceptionCounterSize();
        int callExceptionsCount = batchTest.getCallCollection().getExceptionCounterSize();
        int conferenceExceptionsCount = batchTest.getConferenceCollection().getExceptionCounterSize();

        log.debug(String.format("ReplaceInfo invocations at dialog level: %d, at call level: %d, at conference level: %d, combined: %d", dialogInvocationCount, callInvocationCount, conferenceInvocationCount, dialogInvocationCount + callInvocationCount + conferenceInvocationCount));
        log.debug(String.format("Concurrent Exceptions at dialog level: %d, at call level: %d, at conference level: %d, combined: %d", dialogExceptionsCount, callExceptionsCount, conferenceExceptionsCount, dialogExceptionsCount + callExceptionsCount + conferenceExceptionsCount));

//        batchTest.logFailedScenarios();

        System.exit(batchTest.numberFailed());
    }

    public void addBatchScenarios() {
        addBatchTestScenario("createCallTerminateCallScenario,10");
        addBatchTestScenario("twoCallsSharingCallLegScenario,10");
        addBatchTestScenario("announcementCallScenario,2");
//        addBatchTestScenario("basicCallAndMediaCallSharedDialogScenario,2");
        addBatchTestScenario("firstCallLegFailureScenario,0");
        addBatchTestScenario("secondCallLegFailureScenario,0");
//        addBatchTestScenario("basicConferenceScenario,2"); // 3
//        addBatchTestScenario("conferenceScenarioTerminateParticipants,2"); // 3
        addBatchTestScenario("maxCallDurationScenario,10");
        addBatchTestScenario("badAddressScenario,10");
        addBatchTestScenario("callAnswerTimeoutScenario,10");
//        addBatchTestScenario("promptAndCollectScenario,1");
//        addBatchTestScenario("promptAndRecordScenario,1");
    }

    protected DialogCollectionImplWithStats getDialogCollection() {
        return dialogCollection;
    }

    protected CallCollectionImplWithStats getCallCollection() {
        return callCollection;
    }

    public ConferenceCollectionImplWithStats getConferenceCollection() {
        return conferenceCollection;
    }

    protected void assignNewCollectionsToBeans() {
    	assignNewCollectionsToBeans(applicationContext, dialogCollection, callCollection, conferenceCollection);
    }

    protected void assignNewCollectionsToBeans(ClassPathXmlApplicationContext ac, DialogCollectionImplWithStats _dialogCollection, CallCollectionImplWithStats _callCollection, ConferenceCollectionImplWithStats _conferenceCollection	) {
        ((DialogJainSipListener)ac.getBean("dialogSipListener")).setDialogCollection(_dialogCollection);
        ((OutboundCallLegBeanImpl)ac.getBean("outboundCallLegBean")).setDialogCollection(_dialogCollection);
        ((InboundCallLegBeanImpl)ac.getBean("inboundCallLegBean")).setDialogCollection(_dialogCollection);
        ((CallBeanImpl)ac.getBean("callBean")).setDialogCollection(_dialogCollection);
        ((MediaCallLegBeanImpl)ac.getBean("mediaCallLegBean")).setDialogCollection(_dialogCollection);

        ((CallBeanImpl)ac.getBean("callBean")).setCallCollection(_callCollection);
        ((MediaCallBeanImpl)ac.getBean("mediaCallBean")).setCallCollection(_callCollection);
        ((ConferenceBeanImpl)ac.getBean("conferenceBean")).setConferenceCollection(_conferenceCollection);
    }

    public void logFailedScenarios() {
        for (String scenario : failedScenarios)
            logFailedScenario(scenario);
    }

    public void logFailedScenario(String scenarioId) {
        try {
            if (scenarioId.startsWith("twoCallsSharingCallLegScenario:"))
                twoCallsSharingCallLegScenarioExtractor.doit(scenarioId);
            else if (scenarioId.startsWith("conferenceScenarioTerminateParticipants:"))
                conferenceScenarioTerminateParticipantsLogExtractor.doit(scenarioId);
            else if (scenarioId.startsWith("callAnswerTimeoutScenario:"))
                callAnswerTimeoutScenarioLogExtractor.doit(scenarioId);
            else if (scenarioId.startsWith("basicCallAndMediaCallSharedDialogScenario:"))
                basicCallAndMediaCallSharedDialogScenarioLogExtractor.doit(scenarioId);
            else if (scenarioId.startsWith("promptAndCollectScenario:"))
                promptAndCollectScenarioLogExtractor.doit(scenarioId);
            else if (scenarioId.startsWith("promptAndRecordScenario:"))
                promptAndCollectScenarioLogExtractor.doit(scenarioId);
            else if (scenarioId.startsWith("announcementCallScenario:"))
                announcementCallScenarioLogExtractor.doit(scenarioId);
            else if (scenarioId.startsWith("maxCallDurationScenario:"))
                maxCallDurationScenarioLogExtractor.doit(scenarioId);
            else if (scenarioId.startsWith("basicConferenceScenarioMultiStack:"))
            	basicConferenceScenarioMultiStackExtractor.doit(scenarioId);
            else if (scenarioId.startsWith("byeScenario:"))
            	loadBalancedByeScenarioMultistackExtractor.doit(scenarioId);
            else
                System.err.println(String.format("Unknown log extractor for scenario %s, please write one", scenarioId));
        } catch(Exception e) {
            log.warn(String.format("Error extracting logs from logfile for scenario %s", scenarioId), e);
        }
    }

    /**
     * @param executorService the executorService to set
     */
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public int getNumberOfRuns() {
        return numberOfRuns;
    }

    public int getNumberOfConcurrentStarts() {
        return numberOfConcurrentStarts;
    }

	public int getMaximumScenarioCompletionWaitTimeSeconds() {
		return maximumScenarioCompletionWaitTimeSeconds;
	}
}
