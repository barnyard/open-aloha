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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class PerformanceMeasurmentDao {

    private JdbcTemplate jdbcTemplate;
    private boolean exists = true;

    private static final Log log = LogFactory
            .getLog(PerformanceMeasurmentDao.class);

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PerformanceMeasurmentDao() {
    }

    public void init(){
        try{
            createSchemaIfNecessary();
            exists = true;
        } catch(Exception e){
            exists = false;
        }
    }

    public void updateThreadInfo(long id, String content){
    	jdbcTemplate.update("update performance set threadInfo=? where id=?", new Object[]{content, id});
    }

    private boolean createSchemaIfNecessary() throws Exception {
        boolean create = true;
        try {
            log.debug("checking schema for performance support...");
            jdbcTemplate.queryForLong("select count(id) from performance");
            create = false;
            log.debug("table performance found ...");
        } catch (DataAccessException e) {
            log.warn("performance table does not seem to exist...");
        }
        if (create) {
            InputStream is = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("performance.sql");
            if (is == null) {
                log.warn("script not found...");
                create = false;
            } else {
                log.debug("loaded creation table from performance.sql ...");
                String sql = inputStreamAsString(is);
                sql = sql.replaceAll("%%USERNAME%%", ((BasicDataSource)jdbcTemplate.getDataSource()).getUsername());
                jdbcTemplate.update(sql);
                log.debug("executed script...");
                create = true;
            }
        }
        return create;
    }

    public static String inputStreamAsString(InputStream stream)
            throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        StringBuilder sb = new StringBuilder();
        String line = null;

        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }

        br.close();
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
	public List<Metrics> findMetricsByRunId(long runId){

        if (!exists) {
            log.warn("record skipped as schema does not exists");
            return null;
        }
        List<Metrics> metrics = null;
        try{
	        metrics = jdbcTemplate.query("select unitPerSecond, averageDuration," +
	        		"numberOfRuns, numberOfSuccessfulRuns, variance, standardDeviation, success, description, threadInfo, testType " +
	        		"from Performance where runId=?", new Object[]{runId}, new RowMapper(){
				public Object mapRow(ResultSet rs, int row) throws SQLException {
					double ups = rs.getDouble("unitPerSecond");
					double ad = rs.getDouble("averageDuration");
					long nor = rs.getLong("numberOfRuns");
					long nosr = rs.getLong("numberOfSuccessfulRuns");
					double v = rs.getDouble("variance");
					double sd = rs.getDouble("standardDeviation");
					boolean s = rs.getBoolean("success");
					String d = rs.getString("description");
					String ti = rs.getString("threadInfo");
					String tt = rs.getString("testType");
					Metrics m = new Metrics(tt, ups, ad, nor, nosr, v, sd, s, d);
					m.setThreadInfo(ti);
					return m;
				}
	        });
        }
        catch(DataAccessException e){
        	log.error("Unable to access data for runId: " + runId, e);
        }
        return metrics;
    }

    public Map<Long, List<Metrics>> findLastXMetricsForTestType(int x, String testType){
    	List<Long> runIds = findLastXRunId(x, testType);
    	return findMetricsByRunId(runIds);
    }

    @SuppressWarnings("unchecked")
    public List<Long[]> getNumberOfApplicationThreadsFromDescription(long runId){
        List<Long[]> data = jdbcTemplate.query("select id, description " +
        		"from Performance where runId=? order by id", new Object[]{runId}, new RowMapper(){
			public Object mapRow(ResultSet rs, int row) throws SQLException {
				long id = rs.getInt("id");
				String tt = rs.getString("description");
				final String what = "number of application threads: ";
				int pos = tt.indexOf(what) + what.length();
				String num = tt.substring(pos).trim();
				Long ret = Long.parseLong(num);
				return new Long[]{id, ret};
			}
        });
        return data;
    }

    public Map<Long, List<Metrics>> findMetricsByRunId(List<Long> runIds){
    	Map<Long, List<Metrics>> map = new HashMap<Long, List<Metrics>> ();
    	for(Long i : runIds){
    		map.put(i, findMetricsByRunId(i));
    	}
    	return map;
    }

    @SuppressWarnings("unchecked")
    public List<Long> findLastXRunId(int x, String testType){
    	List<Long> runIds = jdbcTemplate.query("select distinct(runId) as r from performance where testType='" + testType + "'",
    			new RowMapper(){
			public Object mapRow(ResultSet rs, int row) throws SQLException {
				long runId = rs.getLong("r");
				return runId;
			}
    	});
    	int s = runIds.size();
		if (s > x) {
			int remNr = s - x;
			for (int j = 0; j < remNr; j++)
				runIds.remove(0);
		}
		return runIds;
    }

    public boolean record(String name, long runId, Metrics m) throws IllegalStateException {

        if (!exists) {
            log.warn("record skipped as schema does not exists");
            return false;
        }

        long id = generateId();

        String desc = m.getDescription() == null? "" : m.getDescription().substring(0, Math.min(4096, m.getDescription().length()));
        Object[] args = new Object[] { id, runId, name, m.getUnitsPerSecond(),
                m.getAverageDuration(), m.getNumberOfRuns(), m.getNumberOfSuccessfulRuns(),
                m.getVariance(), m.getStandardDeviation(),
                new Timestamp(System.currentTimeMillis()), m.isSuccess(), desc, m.getThreadInfo(), m.getTestType()};

        int[] types = new int[] { Types.BIGINT, Types.BIGINT, Types.VARCHAR,
                Types.DOUBLE, Types.DOUBLE, Types.BIGINT, Types.BIGINT,
                Types.DOUBLE, Types.DOUBLE, Types.TIMESTAMP, Types.BOOLEAN, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR };

        try {
            int updated = jdbcTemplate
                    .update(
                            "insert into Performance (id, runid, name, unitPerSecond, averageDuration, numberOfRuns, "
                                    + "numberOfSuccessfulRuns, variance, standardDeviation, whenCreated, success, description, threadInfo, testType) "
                                    + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            args, types);
            if (updated < 1)
            	log.warn("no rows inserted into performance table");
            return updated == 1;
        } catch (RuntimeException e) {
            throw new IllegalStateException("Unable to record data", e);
        }

    }

    public long generateId() {
        return jdbcTemplate
                .queryForLong("select nextval('performance_sequence')");
    }

    public void vacuumDb(){
    	try{
	    	jdbcTemplate.execute("vacuum full analyze performance; " +
	    			"vacuum full analyze callinfo; " +
	    			"vacuum full analyze performancebatchrunsipstack_callinfo; " +
	    			"vacuum full analyze performancebatchrunsipstack_conferenceinfo; " +
	    			"vacuum full analyze performancebatchrunsipstack_dialoginfo;");
	    	Thread.sleep(10000);
    	}
    	catch(Exception e){
    		log.warn("Exception vacuuming the database!!!", e);
    	}
    }

    public static void main(String[] args) throws Exception{
        BasicConfigurator.configure();
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
                "batchTestApplicationContext.xml");
        PerformanceMeasurmentDao dao = (PerformanceMeasurmentDao)applicationContext.getBean("performanceMeasurementDaoBean");
        if (dao != null) {
			List<Metrics> metrics = dao.findMetricsByRunId(2192);
			Map<Long, List<Metrics>> m = new HashMap<Long, List<Metrics>>();
			m.put(2192L, metrics);
			Chart c = new Chart(m);
			m = dao.findLastXMetricsForTestType(10, "performance.database");
			c = new Chart(m);
			c.saveCombinedChart(new File("unitPerSecond-historical.jpg"), "Runs Per Second", "threads", "runs per second",
					"Standard Deviation", "threads", "std. deviation");
		}


//        List<Long> runids = dao.findLastXRunId(1000, "unknown");
//        for(long runid : runids)
//        {
//        	List<Long[]> vals = dao.getNumberOfApplicationThreadsFromDescription(runid);
//	        int sz = vals.size();
//	        Long[] v = new Long[sz];
//	        int p = 0;
//	        for(Long[] i : vals)
//	        	v[p++] = i[1];
//	        long min = v[0];
//	        long max = v[sz-1];
//	        long inc = 0;
//	        if(sz>1)
//	        	inc = (max - min) / (sz - 1);
//	        for(Long[] i : vals)
//	        {
//	        	String format = String.format(Metrics.TI_STRING, i[1], min, max, inc);
//				dao.updateThreadInfo(i[0], format);
//	        }
//
//        }
        System.exit(0);
    }
}
