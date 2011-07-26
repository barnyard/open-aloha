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

public class Metrics {
    public static final String TI_STRING = "cur=%s,min=%s,max=%s,inc=%s";
	private double averageDuration;
    private long numberOfRuns;
    private double unitsPerSecond;
    private long numberOfSuccessfulRuns;
    private double standardDeviation;
    private double variance;
    private boolean success;
    private String description;
    private String threadInfo="";
    private String testType;
    private ThreadInfo threadInfoObject = null;

    static class ThreadInfo{
    	private int currentThreads = 0;
    	private int minThreads = 0;
    	private int maxThreads = 0;
    	private int increment = 0;
    	public ThreadInfo(String data){
    		if(data==null){
    			return;
    		}
    		String[] nums = data.split("[,=]");
    		currentThreads =  parseInt(nums[1], 0);
    		minThreads =  parseInt(nums[3], 0);
    		maxThreads =  parseInt(nums[5], 0);
    		increment =  parseInt(nums[7], 0);
    	}

    	private int parseInt(String s, int def){
    		try{
    			return Integer.parseInt(s);
    		}
    		catch(RuntimeException e){
    			// ignore
    		}
    		return def;
    	}

		public int getCurrentThreads() {
			return currentThreads;
		}
		public int getMinThreads() {
			return minThreads;
		}
		public int getMaxThreads() {
			return maxThreads;
		}
		public int getIncrement() {
			return increment;
		}
    }

	public Metrics(String testType, double unitsPerSecond, double averageDuration,
            long numberOfRuns, long numberOfSuccessfulRuns, double variance,
            double standardDeviation, boolean success, String description) {
        this.testType = testType;
		this.unitsPerSecond = unitsPerSecond;
        this.averageDuration = averageDuration;
        this.numberOfRuns = numberOfRuns;
        this.numberOfSuccessfulRuns = numberOfSuccessfulRuns;
        this.variance = variance;
        this.standardDeviation = standardDeviation;
        this.success = success;
        this.description = description;
    }

    public String getTestType() {
		return testType;
	}

	public void setTestType(String testType) {
		this.testType = testType;
	}

	public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public double getAverageDuration() {
        return averageDuration;
    }

    public void setAverageDuration(double averageDuration) {
        this.averageDuration = averageDuration;
    }

    public long getNumberOfRuns() {
        return numberOfRuns;
    }

    public void setNumberOfRuns(long numberOfRuns) {
        this.numberOfRuns = numberOfRuns;
    }

    public double getUnitsPerSecond() {
        return unitsPerSecond;
    }

    public void setUnitsPerSecond(double callsPerSecond) {
        this.unitsPerSecond = callsPerSecond;
    }

    public long getNumberOfSuccessfulRuns() {
        return numberOfSuccessfulRuns;
    }

    public void setNumberOfSuccessfulRuns(long numberSuccessful) {
        this.numberOfSuccessfulRuns = numberSuccessful;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }

    public void setStandardDeviation(double standardDeviation) {
        this.standardDeviation = standardDeviation;
    }

    public double getVariance() {
        return variance;
    }

    public void setVariance(double variance) {
        this.variance = variance;
    }

    public String getDescription() {
        return description;
    }

    public String getThreadInfo() {
		return threadInfo;
	}

	public void setThreadInfo(String threadsInfo) {
		this.threadInfo = threadsInfo;
		this.threadInfoObject = new ThreadInfo(this.threadInfo);
	}

    public void setDescription(String description) {
        this.description = description;
    }

	public ThreadInfo getThreadInfoObject() {
		return threadInfoObject;
	}

	public void setThreadInfoObject(ThreadInfo threadInfoObject) {
		this.threadInfoObject = threadInfoObject;
	}
}
