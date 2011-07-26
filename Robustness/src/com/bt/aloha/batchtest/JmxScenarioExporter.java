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
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class JmxScenarioExporter extends WeekendBatchTest implements ApplicationContextAware {

    private static Log log = LogFactory.getLog(JmxScenarioExporter.class);
    private BatchTest batchTest;
    
    public void setApplicationContext(ApplicationContext appCtx) throws BeansException {
        try {
            batchTest = new BatchTest();
            batchTest.setApplicationContext((ClassPathXmlApplicationContext)appCtx);
            batchTest.init();
            batchTest.assignNewCollectionsToBeans();
            configure(batchTest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runScenario(String scenarioName) throws Exception {
        try {
            batchTest.run(scenarioName);
            logStatistics(log, batchTest);
            batchTest.reset();
            logCollections(batchTest);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    protected static void configure(BatchTest batchTest) throws Exception {
        Properties properties = new Properties();
        InputStream is = new FileInputStream(CONFIG_FILE);
        properties.load(is);
        is.close();
        batchTest.setAudioFileUri(properties.getProperty("audioFileUri", "/provisioned/behave.wav"));
        batchTest.setNumberOfRuns(1);
        batchTest.setNumberOfConcurrentStarts(1);
        batchTest.setMaximumScenarioCompletionWaitTimeSeconds(Integer.parseInt(properties.getProperty("maximumScenarioCompletionWaitTimeSeconds", "60")));
        batchTest.setExecutorService(Executors.newFixedThreadPool(4));
        addBatchScenarios(batchTest);
    }

    public void createCallTerminateCallScenario() throws Exception {
        runScenario("createCallTerminateCallScenario");
    }
    public void twoCallsSharingCallLegScenario() throws Exception {
        runScenario("twoCallsSharingCallLegScenario");
    }
    public void announcementCallScenario() throws Exception {
        runScenario("announcementCallScenario");
    }
    public void basicCallAndMediaCallSharedDialogScenario() throws Exception {
        runScenario("basicCallAndMediaCallSharedDialogScenario");
    }
    public void firstCallLegFailureScenario() throws Exception {
        runScenario("firstCallLegFailureScenario");
    }
    public void secondCallLegFailureScenario() throws Exception {
        runScenario("secondCallLegFailureScenario");
    }
    public void basicConferenceScenario() throws Exception {
        runScenario("basicConferenceScenario");
    }
    public void conferenceScenarioTerminateParticipants() throws Exception {
        runScenario("conferenceScenarioTerminateParticipants");
    }
    public void maxCallDurationScenario() throws Exception {
        runScenario("maxCallDurationScenario");
    }
    public void badAddressScenario() throws Exception {
        runScenario("badAddressScenario");
    }
    public void callAnswerTimeoutScenario() throws Exception {
        runScenario("callAnswerTimeoutScenario");
    }
}
