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

 	

 	
 	
 
package com.bt.aloha.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.SqlLobValue;

import com.bt.aloha.media.conference.state.ConferenceInfo;
import com.bt.aloha.media.conference.state.ConferenceState;
import com.bt.aloha.media.conference.state.ConferenceTerminationCause;
import com.bt.aloha.media.conference.state.ParticipantState;
import com.bt.aloha.util.ConcurrentUpdateException;

public class ConferenceInfoDaoImpl extends DaoBase implements ConferenceInfoDao {

    private static final String NO_CONFERENCE_FOUND_FOR_CALL_S_RETURNING_NULL = "no conference found for call %s, returning null";

    private static final String PERCENT = "%";

    private static final String INFO_S_ALREADY_EXISTS_IN_DATABASE_USE_REPLACE_DIALOG_INSTEAD = "Info %s already exists in database, use replaceDialog instead";

    private static final String COUNT_SQL = "select count(conferenceId) from conferenceInfo";
    private static final int V_ID_POS = 3;

    private static final String COLUMNS = 
    	  "conferenceId, " + 
    	  "simpleSipBeanId ," + 
    	  "createTime ," + 
    	  "object_version ," + 
    	  "last_use_time, " + 
    	  "startTime, " + 
    	  "endTime, " + 
    	  "mediaServerAddress, " + 
    	  "conferenceState, " + 
    	  "conferenceTerminationCause, " + 
    	  "maxNumberOfParticipants, " + 
    	  "maxDurationInMinutes, " + 
    	  "housekeepingForced, " + 
    	  "participants";

    private static final String UPDATE_SQL = "update conferenceinfo set "
    	    + "simpleSipBeanId=?,"
            + "createTime=?,"
            + "object_version=?,"
            + "last_use_time=?,"
            + "startTime=?,"
            + "endTime=?,"
      	    + "mediaServerAddress=?,"  
    	    + "conferenceState=?," 
    	    + "conferenceTerminationCause=?," 
    	    + "maxNumberOfParticipants=?," 
    	    + "maxDurationInMinutes=?," 
    	    + "housekeepingForced=?,"
    	    + "participants=?"
            + " where conferenceId=? and object_version=?";

    private static final String INSERT_SQL = "insert into conferenceInfo (" + COLUMNS + ") "
            + "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_COLUMNS = "select " + COLUMNS + " from conferenceInfo";

    private static final String SELECT_BY_ID_SQL = SELECT_COLUMNS + " where conferenceId=?";

    private static final String SELECT_WHERE_CALL_ID_SQL = SELECT_COLUMNS + 
    " where (participants like ?) and createTime = (select max(createTime) from conferenceInfo where participants like ?)";

    private static final String DELETE_SQL = "delete from conferenceinfo where conferenceid = ?";

    private static final int[] INSERT_TYPES = new int[] {
            Types.VARCHAR, 
            Types.VARCHAR, 
            Types.BIGINT,  
            Types.VARCHAR, 
            Types.BIGINT,  
            Types.BIGINT,
            Types.BIGINT,
            Types.VARCHAR, 
            Types.VARCHAR, 
            Types.VARCHAR,
            Types.BIGINT,  
            Types.BIGINT,
            Types.BOOLEAN,
            Types.CLOB, 
            };

    private static final int[] UPDATE_TYPES = new int[] {
        Types.VARCHAR, 
        Types.BIGINT,  
        Types.VARCHAR, 
        Types.BIGINT,  
        Types.BIGINT,
        Types.BIGINT,
        Types.VARCHAR, 
        Types.VARCHAR, 
        Types.VARCHAR,
        Types.BIGINT,  
        Types.BIGINT,
        Types.BOOLEAN,
        Types.CLOB, 
        Types.VARCHAR, 
        Types.VARCHAR,
             };

    private static final String CONFERENCEINFO_DELETE_BY_HOUSEKEEPER_FLAGS_SQL = "delete from conferenceinfo where (conferencestate = 'Ended' or housekeepingForced = 'True') and last_use_time < ?";
    private static final String CONFERENCEINFO_FIND_BY_HOUSEKEEPER_FLAGS_SQL = "select conferenceId from conferenceinfo where (conferencestate = 'Ended' or housekeepingForced = 'True') and last_use_time < ?";
    private static final String CONFERENCEINFO_UPDATE_BY_HOUSEKEEPER_FLAGS_SQL = SELECT_COLUMNS + " where conferencestate != 'Ended' and last_use_time < ?";
    private static final String SELECT_CONNECTED_MAX_DURATION_CONFERENCES_SQL = SELECT_COLUMNS + " where conferencestate = 'Active' and maxDurationInMinutes > 0";

    private static final String SELECT_ALL_SQL = "select * from conferenceinfo";

    private Log log = LogFactory.getLog(ConferenceInfoDaoImpl.class);

    public ConferenceInfoDaoImpl(DataSource aDataSource) {
    	super(aDataSource);
    }

	public void create(final ConferenceInfo conferenceInfo) {
        log.debug(String.format("create(%s)", conferenceInfo.getId()));
		try {
			List<Object> paramList1 = createParams(conferenceInfo);
			Object[] params1 = paramList1.toArray();
			getJdbcTemplate().update(INSERT_SQL, params1, INSERT_TYPES);
		} catch (DataIntegrityViolationException e) {
			throw new IllegalArgumentException(String.format(INFO_S_ALREADY_EXISTS_IN_DATABASE_USE_REPLACE_DIALOG_INSTEAD, conferenceInfo.getId()), e);
		} 
    }

    private List<Object> createParams(ConferenceInfo conferenceInfo){
        List<Object> result = new Vector<Object>();
        result.add(conferenceInfo.getId());
        result.add(conferenceInfo.getSimpleSipBeanId());
        result.add(conferenceInfo.getCreateTime());
        result.add(conferenceInfo.getVersionId());
        result.add(conferenceInfo.getLastUsedTime());
        result.add(conferenceInfo.getStartTime());
        result.add(conferenceInfo.getEndTime());
        result.add(conferenceInfo.getMediaServerAddress());
        result.add(readEnum(conferenceInfo.getConferenceState()));
        result.add(readEnum(conferenceInfo.getConferenceTerminationCause()));
        result.add(conferenceInfo.getMaxNumberOfParticipants());
        result.add(conferenceInfo.getMaxDurationInMinutes());
        result.add(conferenceInfo.isHousekeepForced());
        result.add(new SqlLobValue(conferenceInfo.getParticipants().toString()));
        return result;
    }

    private String readEnum(Object e) {
        if (null == e)
            return null;
        return e.toString();
    }

    class ConferenceInfoRowMapper implements RowMapper {

        public ConferenceInfoRowMapper () {}

        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            String creatingBeanName = rs.getString("simpleSipBeanId");
            String aMediaServerAddress = rs.getString("mediaServerAddress");
            int theMaxNumberOfParticipants = rs.getInt("maxNumberOfParticipants");
            long theMaxDurationInMinutes = rs.getLong("maxDurationInMinutes");

            ConferenceInfo info = new ConferenceInfo(creatingBeanName, aMediaServerAddress, theMaxNumberOfParticipants, theMaxDurationInMinutes);

            String aConferenceId = rs.getString("conferenceId");
            info.setId(aConferenceId);
            long startTime = rs.getLong("startTime");
            info.setStartTime(startTime);

            String conferenceStateString = rs.getString("conferenceState");
            info.updateConferenceState(conferenceStateString == null ? null : ConferenceState.valueOf(conferenceStateString));
            info.setVersionId(rs.getString("object_version"));
            info.setLastUsedTime(rs.getLong("last_use_time"));
            long createTime = rs.getLong("createTime");
            info.setCreateTime(createTime);
            long endTime = rs.getLong("endTime");
            info.setEndTime(endTime);
            String conferenceTerminationCause = rs.getString("conferenceTerminationCause");
            info.setConferenceTerminationCause(conferenceTerminationCause == null ? null : ConferenceTerminationCause.valueOf(conferenceTerminationCause));
            String participants = rs.getString("participants");
            loadParticipants(info, participants);
            info.setHousekeepForced(rs.getBoolean("housekeepingForced"));

            return info;
        }

        private void loadParticipants(ConferenceInfo info, final String participants) {
        	log.debug(String.format("loadParticipants(%s, %s)", info, participants));
        	String p1 = participants.replaceAll(" ", "");
        	String p2 = p1.startsWith("{") ? p1.substring(1) : p1 ;
        	String p3 = p2.endsWith("}") ? p2.substring(0, p2.length() - 1) : p2;
        	if (p3.length() < 1) return;
        	String[] participantsArray = p3.split(",");
        	for (String participant: participantsArray) {
        		String[] parts = participant.split("=");
        		info.getParticipants().put(parts[0], ParticipantState.valueOf(parts[1]));
        	}
        }
    }
    

    public ConferenceInfo read(String callId) {
    	ConferenceInfo conferenceInfo = (ConferenceInfo) getJdbcTemplate().queryForObject(SELECT_BY_ID_SQL, new Object[] { callId }, new ConferenceInfoRowMapper());
        return conferenceInfo;
    }

    public void delete(String conferenceId) {
        Object[] params = new Object[] { conferenceId };
        int[] types = new int[] { Types.VARCHAR };
        getJdbcTemplate().update(DELETE_SQL, params, types);
    }

    public void update(final ConferenceInfo conferenceInfo) {
        List<Object> paramList = createParams(conferenceInfo);
        // replace version id with new one
        paramList.set(V_ID_POS, conferenceInfo.generateNewVersionId());
        // remove ID
        paramList.remove(0);
        // add the ID at the end of the list
        paramList.add(conferenceInfo.getId());
        // add old version ID
        String oldVersionId = conferenceInfo.getVersionId();
        paramList.add(oldVersionId);

        Object[] params = paramList.toArray();

        int rowsUpdated = getJdbcTemplate().update(UPDATE_SQL, params, UPDATE_TYPES);
        if (1 != rowsUpdated)
            throw new ConcurrentUpdateException(conferenceInfo.getId(), String.format("Info %s modified in database, try again", conferenceInfo.getId()));
    }

    public int size(){
        return getJdbcTemplate().queryForInt(COUNT_SQL);
    }

    @SuppressWarnings("unchecked")
	public ConferenceInfo findConferenceForCallId(String callId) {
        log.debug(String.format("findConferenceForCallId(%s)", callId));    
    	if (null == callId) return null;
    	String like = PERCENT + callId + PERCENT;
    	List result = getJdbcTemplate().query(SELECT_WHERE_CALL_ID_SQL, new Object[] { like, like }, new ConferenceInfoRowMapper());
        if (result.size() < 1) {
            log.debug(String.format(NO_CONFERENCE_FOUND_FOR_CALL_S_RETURNING_NULL, callId));
            return null;
        }
        return (ConferenceInfo)result.get(0);
    }
    
    public ConcurrentMap<String, ConferenceInfo> getAll() {
    	return getSelectedConferences(SELECT_ALL_SQL);
    }

	public ConcurrentMap<String, ConferenceInfo> findConnectedMaxDurationConferences() {
        return getSelectedConferences(SELECT_CONNECTED_MAX_DURATION_CONFERENCES_SQL);
    }
    
    @SuppressWarnings("unchecked")
	private ConcurrentMap<String, ConferenceInfo> getSelectedConferences(String sql){
        ConcurrentMap<String, ConferenceInfo> result = new ConcurrentHashMap<String, ConferenceInfo>();
        List records = getJdbcTemplate().query(sql, new ConferenceInfoRowMapper());
        for (int i = 0; i < records.size(); i++) {
        	ConferenceInfo conferenceInfo = (ConferenceInfo)records.get(i);
            result.put(conferenceInfo.getId(), conferenceInfo);
        }
        return result;
    }

    @Override
	public String getDeleteByHouseKeeperFlagsSql() {
		return CONFERENCEINFO_DELETE_BY_HOUSEKEEPER_FLAGS_SQL;
	}

    @Override
	public String getUpdateByHouseKeeperFlagsSql() {
		return CONFERENCEINFO_UPDATE_BY_HOUSEKEEPER_FLAGS_SQL;
	}

    @Override
	public String getFindByHouseKeeperFlagsSql() {
		return CONFERENCEINFO_FIND_BY_HOUSEKEEPER_FLAGS_SQL;
	}
}
