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

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Chart {
	private static Log log = LogFactory.getLog(Chart.class);

	private Map<Long, List<Metrics>> metrics;

	public Chart(Map<Long, List<Metrics>> m) {
		this.metrics = m;
	}

	private XYIntervalSeriesCollection createUnitPerSecondAndDeviationDataset() {
		XYIntervalSeriesCollection seriesCollection = new XYIntervalSeriesCollection();
		List<Long> orderedRunIds = sort(metrics.keySet());
		for (Long runId : orderedRunIds) {
			List<Metrics> metricsList = metrics.get(runId);
			String testType  = "";
			if(metricsList.size()!=0)
				testType = metricsList.get(0).getTestType();
			XYIntervalSeries series = new XYIntervalSeries("Run[" + testType + "]:" + runId);
			int size = metricsList.size();
			for (int i = 0; i < size; i++) {
				Metrics m = metricsList.get(i);
				int x = i;
				if (m.getThreadInfoObject() != null
						&& m.getThreadInfoObject().getCurrentThreads() != 0) {
					x = m.getThreadInfoObject().getCurrentThreads();
				}
				double ups = m.getUnitsPerSecond();
				double v = m.getStandardDeviation();
				series.add(x, x, x, ups, ups - v / 2, ups + v / 2);
			}
			seriesCollection.addSeries(series);
		}
		return seriesCollection;
	}

	private List<Long> sort(Set<Long> keySet){
		List<Long> orderedRunIds = new ArrayList<Long>();
		orderedRunIds.addAll(keySet);
		Collections.sort(orderedRunIds);
		return orderedRunIds;
	}

    private JFreeChart createCombinedChart(XYDataset xydataset1, String titleX1, String labelX1, String labelY1,
    		XYDataset xydataset2, String titleX2, String labelX2, String labelY2)
    {
        CombinedDomainXYPlot combineddomainxyplot = new CombinedDomainXYPlot(new NumberAxis("threads"));
        combineddomainxyplot.setGap(10D);
        combineddomainxyplot.add(createChart(xydataset1, titleX1, labelX1, labelY1).getXYPlot(), 1);
        combineddomainxyplot.add(createChart(xydataset2, titleX2, labelX2, labelY2).getXYPlot(), 1);
        combineddomainxyplot.setOrientation(PlotOrientation.VERTICAL);
        return new JFreeChart("Historical", JFreeChart.DEFAULT_TITLE_FONT, combineddomainxyplot, true);
    }

	private XYSeriesCollection createUnitPerSecondDataset() {
		XYSeriesCollection seriesCollection = new XYSeriesCollection();
		List<Long> orderedRunIds = sort(metrics.keySet());
		for (Long runId : orderedRunIds) {
			List<Metrics> metricsList = metrics.get(runId);
			XYSeries series = new XYSeries("Run:" + runId);
			int size = metricsList.size();
			for (int i = 0; i < size; i++) {
				Metrics m = metricsList.get(i);
				int x = i;
				if (m.getThreadInfoObject() != null
						&& m.getThreadInfoObject().getCurrentThreads() != 0) {
					x = m.getThreadInfoObject().getCurrentThreads();
				}
				double ups = m.getUnitsPerSecond();
				series.add(x, ups);
			}
			seriesCollection.addSeries(series);
		}
		return seriesCollection;
	}

    private XYSeriesCollection createDeviationDataset() {
		XYSeriesCollection seriesCollection = new XYSeriesCollection();
		List<Long> orderedRunIds = sort(metrics.keySet());
		for (Long runId : orderedRunIds) {
			List<Metrics> metricsList = metrics.get(runId);
			XYSeries series = new XYSeries("Run:" + runId);
			int size = metricsList.size();
			for (int i = 0; i < size; i++) {
				Metrics m = metricsList.get(i);
				int x = i;
				if (m.getThreadInfoObject() != null
						&& m.getThreadInfoObject().getCurrentThreads() != 0) {
					x = m.getThreadInfoObject().getCurrentThreads();
				}
				series.add(x, m.getStandardDeviation());
			}
			seriesCollection.addSeries(series);
		}
		return seriesCollection;
	}

	private JFreeChart createChart(XYDataset xydataset, String title, String xLabel, String yLabel) {
		JFreeChart jfreechart = ChartFactory.createXYLineChart(title, xLabel, yLabel, xydataset,PlotOrientation.VERTICAL, true, true, false);
		jfreechart.setBackgroundPaint(Color.white);
		XYPlot xyplot = (XYPlot) jfreechart.getPlot();
		xyplot.setBackgroundPaint(Color.lightGray);
		xyplot.setAxisOffset(new RectangleInsets(5D, 5D, 5D, 5D));
		xyplot.setDomainGridlinePaint(Color.white);
		xyplot.setRangeGridlinePaint(Color.white);
		if (xydataset instanceof IntervalXYDataset) {
			DeviationRenderer deviationrenderer = new DeviationRenderer(true, true);
			deviationrenderer.setSeriesStroke(0, new BasicStroke(3F, 1, 1));
			deviationrenderer.setSeriesFillPaint(0, new Color(255, 200, 200));
			xyplot.setRenderer(deviationrenderer);
		}
		NumberAxis numberaxis = (NumberAxis) xyplot.getRangeAxis();
		numberaxis.setAutoRangeIncludesZero(false);
		numberaxis.setAutoRange(true);
		numberaxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		return jfreechart;
	}

	public JFreeChart createChart(String title, String xLabel, String yLabel) {
		return createChart(createUnitPerSecondAndDeviationDataset(), title, xLabel, yLabel);
	}

	public JFreeChart createCombinedChart(String title1, String labelX1, String labelY1,String title2, String labelX2, String labelY2){
		return createCombinedChart(createUnitPerSecondDataset(), title1, labelX1, labelY1,
				createDeviationDataset(), title2, labelX2, labelY2);
	}

	public void saveChart(File file, String title, String xLabel, String yLabel) {
		try {
			ChartUtilities.saveChartAsJPEG(file, createChart(title, xLabel, yLabel), 1024, 768);
		} catch (IOException e) {
			log.warn("UNABLE to store chart");
		}
	}

	public void saveCombinedChart(File file, String title1, String labelX1, String labelY1,String title2, String labelX2, String labelY2) {
		try {
			ChartUtilities.saveChartAsJPEG(file, createCombinedChart(title1, labelX1, labelY1, title2, labelX2, labelY2), 1024, 768);
		} catch (IOException e) {
			log.warn("UNABLE to store chart");
		}
	}

	public static void main(String args[]) throws Exception {
		try {
			BasicConfigurator.configure();
			ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
					"batchTestApplicationContext.xml");
			PerformanceMeasurmentDao dao = (PerformanceMeasurmentDao) applicationContext
					.getBean("performanceMeasurementDaoBean");
			List<Metrics> list1 = dao.findMetricsByRunId(1);
			List<Metrics> list2 = dao.findMetricsByRunId(2);
			List<Metrics> list3 = dao.findMetricsByRunId(3);
			List<Metrics> list4 = dao.findMetricsByRunId(4);
			List<Metrics> list5 = dao.findMetricsByRunId(5);
			List<Metrics> list6 = dao.findMetricsByRunId(6);
			List<Metrics> list7 = dao.findMetricsByRunId(7);
			List<Metrics> list8 = dao.findMetricsByRunId(8);
			List<Metrics> list9 = dao.findMetricsByRunId(9);
			List<Metrics> list10 = dao.findMetricsByRunId(10);
			HashMap<Long, List<Metrics>> map = new HashMap<Long, List<Metrics>>();
			map.put(1L, list1);
			Chart c = new Chart(map);
			c.saveChart(new File("unitPerSecond.jpg"), "Run with standard deviation", "threads", "units per second");
			map.put(2L, list2);
			map.put(3L, list3);
			map.put(4L, list4);
			map.put(5L, list5);
			map.put(6L, list6);
			map.put(7L, list7);
			map.put(8L, list8);
			map.put(9L, list9);
			map.put(10L, list10);
			c = new Chart(map);
			c.saveCombinedChart(new File("unitPerSecond-historical.jpg"), "Runs Per Second", "threads", "runs per second",
					"Standard Deviation", "threads", "std. deviation");
		} catch (RuntimeException e){
			log.error(e);
		} finally {
			System.exit(0);
		}
	}
}
