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
package com.bt.aloha.sipstone;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.BorderArrangement;
import org.jfree.chart.block.EmptyBlock;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.CompositeTitle;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

public class GenGraph {

	private Map<String, List<Double>> data;

	public GenGraph(String fileName) {
		this.data = readFile(fileName);
	}

	public Map<String, List<Double>> readFile(String fileName) {
		File f = new File(fileName);
		HashMap<String, List<Double>> ret = new HashMap<String, List<Double>>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			String line;
			List<String> headers = null;
			Vector<String> lines = new Vector<String>();
			while (null != (line = br.readLine())) {
				lines.add(line);
			}
			// last two lines are not needed
			lines.remove(1);
			lines.remove(1);
			lines.remove(lines.size()-1);
			for(int row = 0; row<lines.size(); row++) {
				line = lines.get(row);
				System.out.println(line);
				if (row == 0) {
					headers = populateMapWithHeaders(ret, line);
				} else {
					populateMapWithData(ret, line, headers);
				}
			}
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Unable to open " + fileName);
		} catch (IOException e) {
			throw new IllegalArgumentException("Unable to read " + fileName);
		}
		return ret;
	}

	private void populateMapWithData(HashMap<String, List<Double>> ret,
			String line, List<String> headers) {
		List<Double> data = splitData(line);
		for (int i = 0; i < headers.size(); i++) {
			String h = headers.get(i);
			List<Double> l = ret.get(h);
			l.add(data.get(i));
		}
	}

	private List<String> populateMapWithHeaders(
			HashMap<String, List<Double>> ret, String line) {
		List<String> headers;
		headers = splitHeaders(line);
		for (int i = 0; i < headers.size(); i++) {
			String h = headers.get(i);
			ret.put(h, new Vector<Double>());
		}
		return headers;
	}

	private List<String> splitHeaders(String line0) {
		String[] headers = line0.split(",");
		return Arrays.asList(headers);
	}

	private List<Double> splitData(String line) {
		String[] data = line.split(",");
		List<Double> dData = new Vector<Double>();
		for (String d : data) {
			if (d.contains(":"))
				dData.add(getTimeAsLong(d));
			else
				try {
					dData.add(Double.parseDouble(d));
				} catch (NumberFormatException nfe) {
					dData.add(Double.NaN);
				}
		}
		return dData;
	}

	private Double getTimeAsLong(String repr) {
		String[] comps = repr.split(":");
		long h = 3600 * 1000 * Long.parseLong(comps[0]);
		long m = 60 * 1000 * Long.parseLong(comps[1]);
		long s = 1000 * Long.parseLong(comps[2]);
		long ms = 0;
		if (comps.length == 4) {
			ms = Long.parseLong(comps[3]);
		}
		long t = h + m + s + ms;
		// System.err.println(repr + " [" + t + "]");
		return Double.parseDouble(Long.toString(t));
	}

	private XYDataset[] createDataset_TotalCallCreated_CallsPerSecond() {
		DefaultXYDataset dataSet = new DefaultXYDataset();
		DefaultXYDataset percDataSet = new DefaultXYDataset();
		final String CPS = "CPS";
		List<Double> calls = data.get("TotalCallCreated");
		List<Double> callRate = data.get("CallRate(P)");
		List<Double> succCall = data.get("SuccessfulCall(P)");
		List<Double> failedCall = data.get("FailedCall(P)");
		double[] callsArray = new double[calls.size()];
		double[] callRateArray = new double[calls.size()];
		double[] percSuccessfullArray = new double[calls.size()];
		for (int i = 0; i < calls.size(); i++) {
			callsArray[i] = calls.get(i).doubleValue();
			callRateArray[i] = callRate.get(i).doubleValue();
			percSuccessfullArray[i] = (100 * succCall.get(i))
					/ (succCall.get(i) + failedCall.get(i));
		}
		dataSet.addSeries(CPS, new double[][] { callsArray, callRateArray });
		percDataSet.addSeries("%", new double[][] { callsArray,
				percSuccessfullArray });
		return new XYDataset[] { dataSet, percDataSet };
	}

	private XYDataset[] createDataset_CallsPerSecond_AvgResponseTime() {
		DefaultXYDataset dataSet = new DefaultXYDataset();
		DefaultXYDataset percDataSet = new DefaultXYDataset();
		final String CPS = "CPS";
		List<Double> respTime = data.get("ResponseTime1(P)");
		List<Double> callRate = data.get("CallRate(P)");
		List<Double> succCall = data.get("SuccessfulCall(P)");
		List<Double> failedCall = data.get("FailedCall(P)");
		double[] respTimeArray = new double[respTime.size()];
		double[] callRateArray = new double[respTime.size()];
		double[] percSuccessfullArray = new double[respTime.size()];
		for (int i = 0; i < respTime.size(); i++) {
			respTimeArray[i] = respTime.get(i).doubleValue();
			callRateArray[i] = callRate.get(i).doubleValue();
			percSuccessfullArray[i] = (100 * succCall.get(i))
					/ (succCall.get(i) + failedCall.get(i));
		}
		dataSet.addSeries(CPS, new double[][] { callRateArray, respTimeArray });
		percDataSet.addSeries("%", new double[][] { callRateArray,
				percSuccessfullArray });

		double[] limitArray = new double[2];
		limitArray[0] = 200;
		limitArray[1] = 200;
		double[] callsLimitArray = new double[2];
		callsLimitArray[0] = callRateArray[0];
		callsLimitArray[1] = callRateArray[callRateArray.length - 1];

		return new XYDataset[] { dataSet, percDataSet };
	}

	private XYDataset createDataset_TotalCallCreated_AvgResponseTime() {
		DefaultXYDataset dataSet = new DefaultXYDataset();
		final String ART = "AvgRT";
		List<Double> calls = data.get("TotalCallCreated");
		List<Double> respTime = data.get("ResponseTime1(C)");
		double[] callsArray = new double[calls.size()];
		double[] respTimeArray = new double[calls.size()];
		for (int i = 0; i < calls.size(); i++) {
			callsArray[i] = calls.get(i).doubleValue();
			respTimeArray[i] = respTime.get(i).doubleValue();
		}
		dataSet.addSeries(ART, new double[][] { callsArray, respTimeArray });
		double[] limitArray = new double[2];
		limitArray[0] = 200;
		limitArray[1] = 200;
		double[] callsLimitArray = new double[2];
		callsLimitArray[0] = callsArray[0];
		callsLimitArray[1] = callsArray[callsArray.length - 1];
		dataSet.addSeries("ARTLimit", new double[][] { callsLimitArray,
				limitArray });
		return dataSet;
	}

	private JFreeChart createCombinedChart() {
		XYDataset xydatasetArray[] = createDataset_TotalCallCreated_CallsPerSecond();
		XYDataset xydataset = xydatasetArray[0];
		final XYDataset percXydataset = xydatasetArray[1];
		JFreeChart jfreechart = ChartFactory.createXYLineChart(
				"SIPStone graph", "Calls", "Call rate", xydataset,
				PlotOrientation.VERTICAL, false, true, false);
		XYPlot xyplot = (XYPlot) jfreechart.getPlot();
		NumberAxis numberaxis = new NumberAxis("Avg. Response Time");
		numberaxis.setAutoRangeIncludesZero(false);
		xyplot.setRangeAxis(1, numberaxis);
		xyplot.setDataset(1, createDataset_TotalCallCreated_AvgResponseTime());
		xyplot.mapDatasetToRangeAxis(1, 1);
		XYItemRenderer xyitemrenderer = xyplot.getRenderer();
		xyitemrenderer.setBaseToolTipGenerator(StandardXYToolTipGenerator
				.getTimeSeriesInstance());
		if (xyitemrenderer instanceof XYLineAndShapeRenderer) {
			XYLineAndShapeRenderer xylineandshaperenderer = (XYLineAndShapeRenderer) xyitemrenderer;
			xylineandshaperenderer.setBaseShapesVisible(true);
			xylineandshaperenderer.setShapesFilled(true);
		}
		XYLineAndShapeRenderer xylineandshaperenderer1 = new XYLineAndShapeRenderer();
		xylineandshaperenderer1.setSeriesPaint(0, Color.black);
		xylineandshaperenderer1.setBaseShapesVisible(true);
		xylineandshaperenderer1
				.setBaseToolTipGenerator(StandardXYToolTipGenerator
						.getTimeSeriesInstance());
		xyplot.setRenderer(1, xylineandshaperenderer1);
		NumberAxis timeaxis = (NumberAxis) xyplot.getDomainAxis();
		timeaxis.setAutoRange(true);
		timeaxis.setAxisLineVisible(true);
		LegendTitle legendtitle = new LegendTitle(xyitemrenderer);
		LegendTitle legendtitle1 = new LegendTitle(xylineandshaperenderer1);
		BlockContainer blockcontainer = new BlockContainer(
				new BorderArrangement());
		blockcontainer.add(legendtitle, RectangleEdge.LEFT);
		blockcontainer.add(legendtitle1, RectangleEdge.RIGHT);
		blockcontainer.add(new EmptyBlock(2000D, 0.0D));

		XYItemRenderer xyrenderer = (XYItemRenderer) xyplot.getRenderer();
		xyrenderer.setBaseItemLabelGenerator(new MyXYItemLabelGenerator(percXydataset));
		xyrenderer.setBaseItemLabelsVisible(true);

		CompositeTitle compositetitle = new CompositeTitle(blockcontainer);
		compositetitle.setPosition(RectangleEdge.BOTTOM);
		jfreechart.addSubtitle(compositetitle);
		return jfreechart;
	}

	private JFreeChart createSingleChart() {
		XYDataset xydatasetArray[] = createDataset_CallsPerSecond_AvgResponseTime();
		XYDataset xydataset = xydatasetArray[0];
		final XYDataset percXydataset = xydatasetArray[1];
		JFreeChart jfreechart = ChartFactory.createXYLineChart("SIPStone result", "Calls per second",
				"Avg response time", xydataset , PlotOrientation.VERTICAL, true, true, false);
		jfreechart.setBackgroundPaint(Color.white);
		XYPlot xyplot = (XYPlot) jfreechart.getPlot();
		xyplot.setBackgroundPaint(Color.lightGray);
		xyplot.setAxisOffset(new RectangleInsets(5D, 5D, 5D, 5D));
		xyplot.setDomainGridlinePaint(Color.white);
		xyplot.setRangeGridlinePaint(Color.white);
		XYItemRenderer xyrenderer = (XYItemRenderer) xyplot.getRenderer();
		xyrenderer.setBaseItemLabelGenerator(new MyXYItemLabelGenerator(percXydataset));
		xyrenderer.setBaseItemLabelsVisible(true);
		NumberAxis numberaxis = (NumberAxis) xyplot.getRangeAxis();
		numberaxis.setAutoRangeIncludesZero(false);
		numberaxis.setAutoRange(true);
		numberaxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		return jfreechart;
	}

	public void saveCharts(File file1, File file2) {
		try {
			ChartUtilities.saveChartAsJPEG(file1, .95f, createCombinedChart(),
					1024, 768);
		} catch (IOException e) {
			System.out.println("UNABLE to store chart: " + e.getMessage());
		}
		try {
			ChartUtilities.saveChartAsJPEG(file2, .95f, createSingleChart(),
					1024, 768);
		} catch (IOException e) {
			System.out.println("UNABLE to store chart: " + e.getMessage());
		}
	}

	public static void main(String[] args) {
		GenGraph g = new GenGraph("c:\\Temp\\sipp.csv");
		g.saveCharts(new File("c:\\Temp\\chartFromCvs.jpg"), new File("c:\\Temp\\chartFromCvs2.jpg"));
	}

	private static class MyXYItemLabelGenerator implements XYItemLabelGenerator {


		private XYDataset percXydataset;

		public MyXYItemLabelGenerator(XYDataset p){
			percXydataset = p;
		}

		public String generateLabel(XYDataset arg0, int arg1, int arg2) {
			if (arg0.getSeriesKey(arg1).equals("CPS")) {
				return Double.toString(trunc(percXydataset.getYValue(arg1,
						arg2), 2))
						+ "%";
			}
			return null;
		}

		private double trunc(double n, int dgt) {
			double tento = Math.pow(10, dgt);
			return Math.round(tento * n) / tento;
		}

	}

}
