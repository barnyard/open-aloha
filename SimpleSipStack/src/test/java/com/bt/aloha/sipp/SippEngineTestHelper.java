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

package com.bt.aloha.sipp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.aloha.util.NetworkHelper;

public class SippEngineTestHelper {
    private Log log = LogFactory.getLog(this.getClass());
    private static final int TWO_THOUSAND = 2000;
    private static final int FIVE_THOUSAND = 5000;
    private static final String COLON = ":";
    private static List<SippEngineTestHelper> activeSippEngins = new LinkedList<SippEngineTestHelper>();
    private Process process;
    private Timer timer;
    private int port = -1;
    private String hostAddress;
    private StringBuffer errSB;

    public SippEngineTestHelper(String scenarioName, File directory, boolean respondToOriginatingAddress)
            throws IOException {
        init();
        setUpSippScenario(scenarioName, false, directory, respondToOriginatingAddress);
    }

    public SippEngineTestHelper(String scenarioName, boolean buildInScenario, File directory) throws IOException {
        init();
        setUpSippScenario(scenarioName, buildInScenario, directory, false);
    }

    private void init() throws UnknownHostException {
        hostAddress = InetAddress.getLocalHost().getHostAddress();
        activeSippEngins.add(this);
        errSB = new StringBuffer();
    }

    public String getSippAddress() {
        return "sip:" + hostAddress + COLON + Integer.toString(port);
    }

    public URI getSippUri() {
        return URI.create(getSippAddress());
    }

    public static void cleanSippInstances() {
        for (Object sipp : activeSippEngins) {
            ((SippEngineTestHelper) sipp).terminateSipp();
        }

        activeSippEngins.clear();
    }

    private void setUpSippScenario(String scenarioName, boolean buildInScenario, File directory,
            boolean respondToOriginatingAddress) throws IOException {
        if (buildInScenario) {
            setUpSipp("-sn " + scenarioName, directory, respondToOriginatingAddress);
        } else {
            setUpSipp("-sf " + scenarioName, directory, respondToOriginatingAddress);
        }
    }

    private String setIpAddress(final String ipPattern) {
        String hostAddress;
        try {
            hostAddress = NetworkHelper.lookupIpAddress(ipPattern);
        } catch (SocketException e) {
            throw new RuntimeException("Problem looking up network interfaces", e);
        }

        if (hostAddress == null) {
            throw new RuntimeException(String.format("No network adapter matches IP address pattern %s", ipPattern));
        }

        return hostAddress;
    }

    private void setUpSipp(String scenarioName, File directory, boolean respondToOriginatingAddress) throws IOException {

        // Give some time to settle between sipp calls.
        try {
            Thread.sleep(TWO_THOUSAND);
        } catch (InterruptedException e) {
        }

        Properties props = new Properties();
        props.load(getClass().getResourceAsStream("/sipp.properties"));

        final String sippPath = props.getProperty("sipp.home") + "/sipp";
        String localIpAddress = setIpAddress(props.getProperty("sip.stack.ip.address.pattern"));
        port = Integer.parseInt(props.getProperty("sipp.local.port"));
        String localPortOption = props.getProperty("sipp.local.port") == null ? "" : String.format("-p %s", port);
        String remoteAddressOption = respondToOriginatingAddress ? "" : String.format("-rsa %s:%s", localIpAddress,
                props.getProperty("sip.stack.port"));
        String runTimesOption = "-m 1";
        String remoteAddressPort = respondToOriginatingAddress ? localIpAddress : String.format("%s:%s",
                localIpAddress, props.getProperty("sip.stack.port"));
        String cmdLine = String.format("%s %s %s %s %s %s", sippPath, remoteAddressOption, runTimesOption,
                scenarioName, remoteAddressPort, localPortOption);
        log.debug(cmdLine);

        System.out.println("COMMAND LINE:");
        System.out.println("cd " + directory.getAbsolutePath());
        System.out.println(cmdLine);
        process = Runtime.getRuntime().exec(cmdLine, null, directory);

        final BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
        final OutputStream out = process.getOutputStream();
        final BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        timer = new Timer(false);
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                process.destroy();
            }

        }, 30000);

        final Object event = new Object();
        new Thread() {
            public void run() {
                try {
                    String line;
                    while ((line = err.readLine()) != null) {
                        // while (err.ready() && (line = err.readLine()) !=
                        // null) {
                        errSB.append(line);
                    }
                    err.close();
                } catch (IOException e) {
                    log.debug("Unable to read the error stream from sipp", e);
                }
            }
        }.start();

        new Thread() {
            public void run() {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        // while (in.ready() && (line = in.readLine()) != null)
                        // {
                        if (line.contains("Terminated")) {
                            break;
                        }

                        if (port == -1 && line.contains("Scenario Screen")) {
                            line = in.readLine();
                            String pattern;
                            int group;

                            if (line.contains("Transport")) {
                                pattern = "(\\d+)";
                                group = 1;
                            } else if (line.contains("Remote-host")) {
                                pattern = "(.*?\\ds.*?)(\\d+)";
                                group = 2;
                            } else
                                continue;

                            line = in.readLine();
                            final Pattern pat = Pattern.compile(pattern);
                            Matcher matcher = pat.matcher(line);
                            matcher.find();
                            port = Integer.parseInt(matcher.group(group));

                            synchronized (event) {
                                event.notify();
                            }
                        }
                    }
                    in.close();
                    out.close();
                } catch (IOException e) {
                    log.debug("Unable to read the input stream from sipp", e);
                }
            }
        }.start();

        synchronized (event) {
            try {
                event.wait(FIVE_THOUSAND);
            } catch (InterruptedException e) {
            }
        }

        if (port == -1)
            throw new IOException("Error reading sipp port");

        System.out.println("Running sipp at " + getSippAddress());
    }

    public int waitForSippResult() throws InterruptedException {
        try {
            return process.waitFor();
        } finally {
            timer.cancel();
        }
    }

    public void terminateSipp() {
        if (process != null)
            process.destroy();
    }

    public String getErrors() {
        return errSB.toString();
    }

    public int getPort() {
        return port;
    }

    public String getHostAddress() {
        return hostAddress;
    }
}
