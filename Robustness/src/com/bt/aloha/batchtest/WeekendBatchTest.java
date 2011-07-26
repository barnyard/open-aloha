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

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.aloha.util.OptimisticConcurrencyCollection;

public class WeekendBatchTest {

    private static Log log = LogFactory.getLog(WeekendBatchTest.class);
    protected static final String CONFIG_FILE = "etc/properties/WeekendBatchTest.properties";
    protected static final String SCENARIO_FILE = "etc/properties/WeekendBatchTest.scenarios.properties";
    protected static boolean stop = false;
    protected static final int defaultSleepTime = 1000 * 60 * 30;
    protected static int sleepTime = defaultSleepTime;
    private static ExecutorService executorService;

    protected static void configure(BatchTest batchTest) throws Exception {
        Properties properties = new Properties();
        InputStream is = new FileInputStream(CONFIG_FILE);
        properties.load(is);
        is.close();
        sleepTime = Integer.parseInt(properties.getProperty("sleepTime", Integer.toString(defaultSleepTime)));
        batchTest.setAudioFileUri(properties.getProperty("audioFileUri", "/provisioned/behave.wav"));
        stop = Boolean.parseBoolean(properties.getProperty("stop", "false"));
        batchTest.setNumberOfRuns(Integer.parseInt(properties.getProperty("numberOfRuns", "1000")));
        int concurrentStarts = Integer.parseInt(properties.getProperty("numberOfConcurrentStarts", "4"));
        batchTest.setNumberOfConcurrentStarts(concurrentStarts);
        if (executorService == null)
            executorService = Executors.newFixedThreadPool(concurrentStarts);
        batchTest.setExecutorService(executorService);
        batchTest.setMaximumScenarioCompletionWaitTimeSeconds(Integer.parseInt(properties.getProperty("maximumScenarioCompletionWaitTimeSeconds", "60")));
        addBatchScenarios(batchTest);
    }

    protected static void addBatchScenarios(BatchTest batchTest) throws Exception {
        Properties properties = new Properties();
        InputStream is = new FileInputStream(SCENARIO_FILE);
        properties.load(is);
        is.close();
        for (Object scenarioName : properties.keySet()) {
            String name = (String)scenarioName;
            String value = properties.getProperty(name);
            batchTest.addBatchTestScenario(name + "," + value);
        }
    }

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext ctx= new ClassPathXmlApplicationContext("batchTestApplicationContext.xml");
        BatchTest batchTest = (BatchTest)ctx.getBean("batchTestBean");
        batchTest.setApplicationContext(ctx);
        batchTest.init();
        batchTest.assignNewCollectionsToBeans();

        while (true) {
            configure(batchTest);
            if (stop)
                break;
            batchTest.run();
            logStatistics(log, batchTest);
            batchTest.reset();
            if (sleepTime > 0) {
                log.info(String.format("sleeping for %d minutes", sleepTime/60/1000));
                Thread.sleep(sleepTime);
            }
        }
        // wait until all things in collection should be ready for housekeeping
        Properties batchProps = new Properties();
        InputStream is = batchTest.getClass().getResourceAsStream("/batchrun.sip.properties");
        batchProps.load(is);
        is.close();
        Thread.sleep(Long.parseLong(batchProps.getProperty("dialog.max.time.to.live", "900000")) + Long.parseLong(batchProps.getProperty("housekeeping.interval", "300000")));
        // housekeeping should have happend
        // log out all things still left
        logCollections(batchTest);

        batchTest.destroy();
    }

    protected static void logStatistics(Log alog, BatchTest batchTest) {
        int dialogInvocationCount = batchTest.getDialogCollection().getInvocationCounterSize();
        int callInvocationCount = batchTest.getCallCollection().getInvocationCounterSize();
        int conferenceInvocationCount = batchTest.getConferenceCollection().getInvocationCounterSize();
        int dialogExceptionsCount = batchTest.getDialogCollection().getExceptionCounterSize();
        int callExceptionsCount = batchTest.getCallCollection().getExceptionCounterSize();
        int conferenceExceptionsCount = batchTest.getConferenceCollection().getExceptionCounterSize();
        alog.info(String.format("Success rate: %f", batchTest.successRate()));
        alog.info(String.format(
                "ReplaceInfo invocations at dialog level: %d, at call level: %d, at conference level: %d, combined: %d",
                dialogInvocationCount, callInvocationCount, conferenceInvocationCount, dialogInvocationCount + callInvocationCount + conferenceInvocationCount));
        alog.info(String.format(
                "Concurrent Exceptions at dialog level: %d, at call level: %d, at conference level: %d, combined: %d",
                dialogExceptionsCount, callExceptionsCount, conferenceExceptionsCount, dialogExceptionsCount + callExceptionsCount + conferenceExceptionsCount));
        alog.info(String.format("Total memory  : %d", Runtime.getRuntime().totalMemory()));
        alog.info(String.format("Maximum memory: %d", Runtime.getRuntime().maxMemory()));
        alog.info(String.format("Free memory   : %d", Runtime.getRuntime().freeMemory()));
    }

    protected static void logCollections(BatchTest batchTest) {
        OptimisticConcurrencyCollection<?> collection = batchTest.getDialogCollection();
        logCollectionItems(collection, "Dialog");
        collection = batchTest.getCallCollection();
        logCollectionItems(collection, "Call");
        collection = batchTest.getConferenceCollection();
        logCollectionItems(collection, "Conference");
    }

    protected static void logCollectionItems(OptimisticConcurrencyCollection<?> collection, String type) {
        log.info(String.format("Remaining count in %s Collection: %d", type, collection.size()));
        if (collection.size() > 0) {
            for(String dialogId : collection.getAll().keySet()) {
                log.warn(String.format("%s Remaining: %s", type, dialogId));
            }
        }
    }
}
