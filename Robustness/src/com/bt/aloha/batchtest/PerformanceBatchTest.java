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

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bt.aloha.batchtest.scenarios.CreateCallTerminateCallScenario;
import com.bt.aloha.batchtest.utils.PropertyLister;
import com.bt.aloha.call.collections.CallCollection;
import com.bt.aloha.call.state.ImmutableCallInfo;
import com.bt.aloha.dialog.collections.DialogCollection;
import com.bt.aloha.stack.SimpleSipStack;

public class PerformanceBatchTest extends BatchTest {
    private static Log log = LogFactory.getLog(PerformanceBatchTest.class);
    private static int INITIATED_SEMAPHORE_PERMIT_COUNT = 100;

    private CallCollection callCollection;
    private DialogCollection dialogCollection;
    private PropertyLister propertyLister;

    private Hashtable<String, Result> results = new Hashtable<String, Result>();
    private Hashtable<String, RunScenario> runScenarios = new Hashtable<String, RunScenario>();
    private Semaphore finishSemaphore = null;
    private long startTestTime;
    private long endTestTime;
    private boolean overallSuccess = true;
    private Metrics currentMetrics;

    private int numberOfInitialConcurrentStarts;
    private int numberOfMaxConcurrentStarts;
    private int numberOfConcurrentStartsIncrements;

    private double failIfSuccessPercentLessThan = 100;
    private double failIfRunsPerSecLessThan = 1;

    private int initialJainPoolSize;
    private int maxJainPoolSize;
    private int jainPoolSizeIncrement;

    private int initialSssPoolSize;
    private int maxSssPoolSize;
    private int sssPoolSizeIncrement;


    public int getInitialJainPoolSize() {
        return initialJainPoolSize;
    }

    public void setInitialJainPoolSize(int initialJainPoolSize) {
        this.initialJainPoolSize = initialJainPoolSize;
    }

    public int getMaxJainPoolSize() {
        return maxJainPoolSize;
    }

    public void setMaxJainPoolSize(int maxJainPoolSize) {
        this.maxJainPoolSize = maxJainPoolSize;
    }

    public int getJainPoolSizeIncrement() {
        return jainPoolSizeIncrement;
    }

    public void setJainPoolSizeIncrement(int jainPoolSizeIncrement) {
        this.jainPoolSizeIncrement = jainPoolSizeIncrement;
    }

    public int getInitialSssPoolSize() {
        return initialSssPoolSize;
    }

    public void setInitialSssPoolSize(int initialSssPoolSize) {
        this.initialSssPoolSize = initialSssPoolSize;
    }

    public int getMaxSssPoolSize() {
        return maxSssPoolSize;
    }

    public void setMaxSssPoolSize(int maxSssPoolSize) {
        this.maxSssPoolSize = maxSssPoolSize;
    }

    public int getSssPoolSizeIncrement() {
        return sssPoolSizeIncrement;
    }

    public void setSssPoolSizeIncrement(int sssPoolSizeIncrement) {
        this.sssPoolSizeIncrement = sssPoolSizeIncrement;
    }

    public int getNumberOfInitialConcurrentStarts() {
        return numberOfInitialConcurrentStarts;
    }

    public void setNumberOfInitialConcurrentStarts(
            int numberOfInitialConcurrentStarts) {
        this.numberOfInitialConcurrentStarts = numberOfInitialConcurrentStarts;
    }

    public int getNumberOfMaxConcurrentStarts() {
        return numberOfMaxConcurrentStarts;
    }

    public void setNumberOfMaxConcurrentStarts(int numberOfMaxConcurrentStarts) {
        this.numberOfMaxConcurrentStarts = numberOfMaxConcurrentStarts;
    }

    public int getNumberOfConcurrentStartsIncrements() {
        return numberOfConcurrentStartsIncrements;
    }

    public void setNumberOfConcurrentStartsIncrements(int numberOfConcurrentStartsIncrements) {
        this.numberOfConcurrentStartsIncrements = numberOfConcurrentStartsIncrements;
    }

    private static int[] getInitParams(){
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
        	"batchTestApplicationContext.xml");
        PerformanceBatchTest t = (PerformanceBatchTest)applicationContext.getBean("performanceBatchTestBean");
        PerformanceMeasurmentDao dao = (PerformanceMeasurmentDao)applicationContext.getBean("performanceMeasurementDaoBean");
        long runId = dao.generateId();
        int jainPoolStart = t.getInitialJainPoolSize();
        int jainPoolMax = t.getMaxJainPoolSize();
        int jainPoolInc = t.getJainPoolSizeIncrement();
        int sssPoolStart = t.getInitialSssPoolSize();
        int sssPoolMax = t.getMaxSssPoolSize();
        int sssPoolInc = t.getSssPoolSizeIncrement();
        int start = t.getNumberOfInitialConcurrentStarts();
        int max = t.getNumberOfMaxConcurrentStarts();
        int inc = t.getNumberOfConcurrentStartsIncrements();
        applicationContext.destroy();

        return new int[]{(int)runId, jainPoolStart, jainPoolMax, jainPoolInc, sssPoolStart, sssPoolMax, sssPoolInc, start, max, inc};
    }

    public static void main(String[] args) throws Exception{
	MultistackApplicationContextManager manager = null;

        try {
    		manager = new MultistackApplicationContextManager(
    				new String[]{"batchTestApplicationContext.xml", "propertyListenerApplicationContext.performance.xml"},
    				null);
		manager.injectManagerIntoApplicatonContext1Beans();
        }
        catch(Exception e){
        	log.error(e);
        	e.printStackTrace();
        	System.exit(1);
        }
        
        log.info("Loading application context");

        boolean success = true;
        PerformanceMeasurmentDao dao = null;
        //ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("batchTestApplicationContext.xml");
        ClassPathXmlApplicationContext applicationContext = manager.getApplicationContext1();
        dao = (PerformanceMeasurmentDao)applicationContext.getBean("performanceMeasurementDaoBean");
        PerformanceBatchTest performanceBatchTest = (PerformanceBatchTest)applicationContext.getBean("performanceBatchTestBean");
        int start = performanceBatchTest.getNumberOfInitialConcurrentStarts();
        int max = performanceBatchTest.getNumberOfMaxConcurrentStarts();
        int inc = performanceBatchTest.getNumberOfConcurrentStartsIncrements();
        long runId = dao.generateId();
        performanceBatchTest.setApplicationContext(applicationContext);
        performanceBatchTest.resetDb();
        for (int currNumberOfAppThreads = start; currNumberOfAppThreads <= max; currNumberOfAppThreads += inc) {

            performanceBatchTest.init();
            performanceBatchTest.addBatchScenarios();
            performanceBatchTest.setNumberOfConcurrentStarts(currNumberOfAppThreads);
            logSystemInformation(performanceBatchTest);
            performanceBatchTest.setExecutorService(Executors.newFixedThreadPool(currNumberOfAppThreads));
            log.info("Running tests with " + currNumberOfAppThreads + " concurrent threads");
            performanceBatchTest.run();
            performanceBatchTest.currentMetrics.setThreadInfo(String.format(Metrics.TI_STRING, currNumberOfAppThreads, start, max, inc));
            performanceBatchTest.currentMetrics.setTestType(performanceBatchTest.getTestType());
            dao.record("Call", runId, performanceBatchTest.currentMetrics);
            performanceBatchTest.executorService.shutdownNow();
            performanceBatchTest.results.clear();
            success &= performanceBatchTest.overallSuccess;
        }
        applicationContext.destroy();

        if (dao != null) {
			List<Metrics> metrics = dao.findMetricsByRunId(runId);
			Map<Long, List<Metrics>> m = new HashMap<Long, List<Metrics>>();
			m.put(runId, metrics);
			Chart c = new Chart(m);
			//String xLabel = String.format("Runs - %s calls per thread, %s min threads, %s max theads, %s increment", cpt, start, max, inc);
			c.saveChart(new File("unitPerSecond.jpg"), "UPS with Std deviation", "threads", "units per second");
			m = dao.findLastXMetricsForTestType(5, performanceBatchTest.getTestType());
			c = new Chart(m);
			c.saveCombinedChart(new File("unitPerSecond-historical.jpg"), "Runs Per Second", "threads", "runs per second",
					"Standard Deviation", "threads", "std. deviation");
		}
        try {
            // allow sipp to settle down (in terms of sending its responses and us being up to receive them)
            Thread.sleep(30000);
        } catch (Throwable tex) {
        }
        System.exit(success? 0 : 1);
    }

    @Override
    public void init() {
        callCollection = (CallCollection)applicationContext.getBean("callCollection");
        dialogCollection = (DialogCollection)applicationContext.getBean("dialogCollection");
        propertyLister = (PropertyLister)applicationContext.getBean("propertyLister");
    }

    private void restartJainSipStack(int numberJainSipThreads) {
        SimpleSipStack simpleSipStack = (SimpleSipStack)applicationContext.getBean("simpleSipStack");simpleSipStack.destroy();
        simpleSipStack.getJainStackProperties().put("gov.nist.javax.sip.THREAD_POOL_SIZE", numberJainSipThreads);
        simpleSipStack.init();
    }

    private void restartSimpleSipStack(int numberSimpleSipThreads) {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = (ThreadPoolTaskExecutor)applicationContext.getBean("taskExecutor");
        threadPoolTaskExecutor.destroy();
        threadPoolTaskExecutor.setCorePoolSize(numberSimpleSipThreads);
        threadPoolTaskExecutor.initialize();
    }

    private static String getJainSipVersion() {
        String classpath = System.getProperty("java.class.path");
        String jainSipRiVersion = "could not get it from the classpath";
    	String jainSipApiVersion = jainSipRiVersion;
    	int startIndex = -1;
    	int endIndex = -1;
        if(classpath!=null){
	        startIndex = classpath.indexOf("JainSipApi");
	        if(startIndex==-1)
	        	startIndex = classpath.indexOf("jain-sip-api");
	        endIndex = classpath.indexOf(".jar", startIndex);
	        jainSipApiVersion = classpath.substring(startIndex, endIndex+4);

	        startIndex = classpath.indexOf("JainSipRi");
	        if(startIndex==-1)
	        	startIndex = classpath.indexOf("jain-sip-ri");
	        endIndex = classpath.indexOf(".jar", startIndex);
	        jainSipRiVersion = classpath.substring(startIndex, endIndex+4);
        }
        return String.format("Jain Sip Jars: %s, %s", jainSipApiVersion, jainSipRiVersion);
    }

    private static String getSystemInformation(PerformanceBatchTest performanceBatchTest) {
        StringBuffer b = new StringBuffer();
        String cr = System.getProperty("line.separator");
        b.append(String.format("OS Name: %s, version: %s, architecture: %s", System.getProperty("os.name"), System.getProperty("os.arch"), System.getProperty("os.version"))).append(cr);
        b.append(String.format("Java version: %s, vendor: %s", System.getProperty("java.version"), System.getProperty("java.vendor"))).append(cr);
        b.append(String.format("Database driver: %s, url: %s", performanceBatchTest.propertyLister.getDataSourceDriver(), performanceBatchTest.propertyLister.getDataSourceUrl())).append(cr);
        b.append(String.format("Collections Information - Dialogs: %s, Calls: %s", performanceBatchTest.dialogCollection.getClass().getName(), performanceBatchTest.callCollection.getClass().getName())).append(cr);
        b.append(getJainSipVersion()).append(cr);
        b.append(String.format("Jain Sip Thread Count: %d, Simple Sip Stack Thread Count: %d, Database Pool Thread Count: %d", performanceBatchTest.propertyLister.getJainSipStackThreadCount(), performanceBatchTest.propertyLister.getSimpleSipStackThreadCount(), performanceBatchTest.propertyLister.getDataSourcePoolCount())).append(cr);
        b.append(String.format("Number of scenarios to be run: %d, number of application threads: %d", performanceBatchTest.getNumberOfRuns(), performanceBatchTest.getNumberOfConcurrentStarts())).append(cr);
        return b.toString();
    }

    private static void logSystemInformation(PerformanceBatchTest performanceBatchTest) {
        log.info("--------------------------------------------------------------------------------");
        log.info(getSystemInformation(performanceBatchTest));
        log.info("--------------------------------------------------------------------------------");
    }

    private void analyze() {
        long numberSuccessful = 0;
        double averageDuration = 0;
        double cleanupTime = 0;
        Collection<Result> resultsCollection = results.values();
        int numberOfResults = resultsCollection.size();
        for (Result result : resultsCollection) {
            if (result.isSucceeded()) {
                numberSuccessful++;
                averageDuration += result.getDuration();
                cleanupTime += result.getCleanupDuration();
            }
        }
        if(numberSuccessful!=0)
        	averageDuration /= numberSuccessful;

        double variance = 0;
        if (numberSuccessful != 0) {
			for (Result result : resultsCollection) {
				if (result.isSucceeded()) {
					double diff = result.getDuration() - averageDuration;
					variance += (diff * diff);
				}
			}
			variance /= numberSuccessful;
		}
        double standardDeviation = Math.sqrt(variance);

        double totalTimeForTests = (endTestTime - startTestTime) / 1000;
        double totalTimeForTestsExcludingCleanupTime = Math.abs(totalTimeForTests - cleanupTime);
        long numberOfRuns = numberOfResults;//getNumberOfRuns();
        double successPercent = (100.0*numberSuccessful/numberOfRuns);
        double callsPerSecond = (1.0*numberOfRuns/totalTimeForTestsExcludingCleanupTime);

        log.info(String.format("Total time taken to run test: %f seconds, calls per second: %f", totalTimeForTests, (1.0*numberOfRuns/totalTimeForTests)));
        log.info(String.format("Excluding cleanup time, total time taken to run test: %f seconds, calls per second: %f", totalTimeForTestsExcludingCleanupTime, callsPerSecond));
        log.info(String.format("Number successful: %d, is equivalent to %f percent", numberSuccessful, successPercent));
        log.info(String.format("Average duration of successful scenario: %f seconds", averageDuration));
        log.info(String.format("Variance from mean: %f, standard deviation: %f", variance, standardDeviation));

        boolean success = true;
        if (successPercent < getFailIfSuccessPercentLessThan() || callsPerSecond < getFailIfRunsPerSecLessThan())
        {
            success=false;
            overallSuccess &= false;
        }
        String description = getSystemInformation(this);
        String testType = getTestType();
        currentMetrics = new Metrics(testType, callsPerSecond, averageDuration, numberOfRuns, numberSuccessful, variance, standardDeviation, success, description);
    }


    private String getTestType() {
        String testType = System.getProperty("test.type");
        if(testType==null){
        	testType = "performance.database";
        	log.warn("\n\n>>>> Please set the system property test.type - using default of 'performance.database' <<<< \n\n");
        }
        return testType;
	}

	@Override
    public void addBatchScenarios() {
        addBatchTestScenario("createCallTerminateCallScenario,10");
    }

    @Override
    public void run() {
        normalizeWeightings();
        try {
            finishSemaphore = new Semaphore(1 - getNumberOfConcurrentStarts());
            String[] beans = new String[getNumberOfConcurrentStarts()];
            BatchTestScenario[] concurrentScenarios = new BatchTestScenario[getNumberOfConcurrentStarts()];
            for (int j=0; j<getNumberOfConcurrentStarts(); j++) {
                beans[j] = pickScenarioName();

                BatchTestScenario s = (BatchTestScenario)applicationContext.getBean(beans[j]);

                if (s.getBatchTestScenarioResultListener() == null)
                    s.setBatchTestScenarioResultListener(this);
                concurrentScenarios[j] = s;
            }

            startTestTime = System.currentTimeMillis();
            for (int j=0; j<getNumberOfConcurrentStarts(); j++) {
                if (concurrentScenarios[j] == null)
                    break;
                RunScenario rs = new RunScenario(concurrentScenarios[j], beans[j], getNumberOfRuns());
                executorService.execute(rs);
            }

            finishSemaphore.acquire();
            endTestTime = System.currentTimeMillis();
            analyze();
        } catch(Throwable t) {
            log.warn("The main thread was interrupted for some reason!");
            log.error(t.getMessage());
        }
    }

    @Override
    public void updateRunStatus(String id, String message) {
        if (message.equals("Start of scenario")) return;

        try {
            if (runScenarios.containsKey(id))
                runScenarios.get(id).getInitiatedSemaphore().tryAcquire(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn(String.format("Unable to acquire semaphore to update run status for scenario %s with message %s", id, message));
        }
        if (results.containsKey(id))
            results.get(id).setMessage(message);

        runScenarios.get(id).getInitiatedSemaphore().release();
    }

    @Override
    public void runCompleted(String id, boolean successful) {
        RunScenario runScenario = runScenarios.get(id);
        try {
            runScenario.getInitiatedSemaphore().tryAcquire(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn(String.format("Unable to acquire semaphore to mark run for scenario %s as completed", id));
        }
        Result result = results.get(id);
        if (result != null)
            result.setSucceeded(successful);

        if (runScenario.batchTestScenario instanceof CreateCallTerminateCallScenario) {
            long start = System.currentTimeMillis();
            String callId = ((CreateCallTerminateCallScenario)runScenario.batchTestScenario).getCallId(id);
            ImmutableCallInfo callInfo = callCollection.get(callId);
            String firstDialogId = callInfo.getFirstDialogId();
            String secondDialogId = callInfo.getSecondDialogId();
            callCollection.remove(callId);
            dialogCollection.remove(firstDialogId);
            dialogCollection.remove(secondDialogId);
            long end = System.currentTimeMillis();
            if (result != null)
                result.setCleanupDuration(1.0*(end - start)/1000);
        }

        runScenario.getInitiatedSemaphore().release();
        runScenario.getPreviousCompletedSemaphore().release();
    }

    class RunScenario implements Runnable {
        private BatchTestScenario batchTestScenario;
        private String beanName;
        private int numberRuns;
        private Semaphore previousCompletedSemaphore = new Semaphore(1);
        private Semaphore initiatedSemaphore = new Semaphore(INITIATED_SEMAPHORE_PERMIT_COUNT);

        public RunScenario(BatchTestScenario aBatchTestScenario, String aBeanName, int aNumberRuns) {
            batchTestScenario = aBatchTestScenario;
            beanName = aBeanName;
            numberRuns = aNumberRuns;
        }

        public void run() {
            try {
                for (int i=0; previousCompletedSemaphore.tryAcquire(60, TimeUnit.SECONDS) && i<numberRuns ; i++) {
                    if (!initiatedSemaphore.tryAcquire(INITIATED_SEMAPHORE_PERMIT_COUNT, 60, TimeUnit.SECONDS))
                        log.warn("Unable to acquire lock to start scenario!!");
                    String scenarioId = batchTestScenario.start(beanName);
                    results.put(scenarioId, new Result(scenarioId));
                    runScenarios.put(scenarioId, this);
                    initiatedSemaphore.release(INITIATED_SEMAPHORE_PERMIT_COUNT);
                }
            } catch(Throwable t) {
                log.error(String.format("Test for scenario %s threw an exception", beanName), t);

                t.printStackTrace(System.err);
            }
            finishSemaphore.release();
        }

        public Semaphore getPreviousCompletedSemaphore() {
            return previousCompletedSemaphore;
        }

        public Semaphore getInitiatedSemaphore() {
            return initiatedSemaphore;
        }
    }

    private static class Result {
        private boolean succeeded;
        private String message;
        private long startTime;
        private long endTime;
        private double cleanupDuration;

        public Result(String aScenarioId) {
            succeeded = false;
            startTime = System.currentTimeMillis();
            endTime = 0;
            cleanupDuration = 0;
        }

        public void setSucceeded(boolean isSucceeded) {
            succeeded = isSucceeded;
            endTime = System.currentTimeMillis();
        }

        public void setMessage(String aMessage) {
            message = aMessage;
        }

        public String getMessage() {
            return message;
        }

        public double getDuration() {
            return 1.0*(endTime - startTime)/1000;
        }

        public boolean isSucceeded() {
            return succeeded;
        }

        public double getCleanupDuration() {
            return cleanupDuration;
        }

        public void setCleanupDuration(double aCleanupDuration) {
            this.cleanupDuration = aCleanupDuration;
        }
    }

    public double getFailIfSuccessPercentLessThan() {
        return failIfSuccessPercentLessThan;
    }

    public void setFailIfSuccessPercentLessThan(double failIfSuccessPercentLessThan) {
        this.failIfSuccessPercentLessThan = failIfSuccessPercentLessThan;
    }

    public double getFailIfRunsPerSecLessThan() {
        return failIfRunsPerSecLessThan;
    }

    public void setFailIfRunsPerSecLessThan(double failIfRunsPerSecLessThan) {
        this.failIfRunsPerSecLessThan = failIfRunsPerSecLessThan;
    }
}
