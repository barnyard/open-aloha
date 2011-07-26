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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.sdp.MediaDescription;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.SqlLobValue;

import com.bt.aloha.call.state.CallInfo;
import com.bt.aloha.call.state.CallLegCausingTermination;
import com.bt.aloha.call.state.CallLegConnectionState;
import com.bt.aloha.call.state.CallState;
import com.bt.aloha.call.state.CallTerminationCause;
import com.bt.aloha.call.state.MediaNegotiationMethod;
import com.bt.aloha.call.state.MediaNegotiationState;
import com.bt.aloha.call.state.PendingCallReinvite;
import com.bt.aloha.callleg.AutoTerminateAction;
import com.bt.aloha.util.ConcurrentUpdateException;
import com.bt.aloha.util.ObjectSerialiser;

public class CallInfoDaoImpl extends DaoBase implements CallInfoDao {

    private static final String NO_CALL_FOUND_FOR_DIALOG_S_RETURNING_NULL = "no call found for dialog %s, returning null";

    private static final String INFO_S_ALREADY_EXISTS_IN_DATABASE_USE_REPLACE_DIALOG_INSTEAD = "Info %s already exists in database, use replaceDialog instead";

    private static final String COUNT_SQL = "select count(callId) from callInfo";

    private static final String COUNT_SQL_BY_STATE = COUNT_SQL + " where callState=?";

    private static final int V_ID_POS = 6;

    private static final String COLUMNS = "callId, "
            + "classname, "
            + "simpleSipBeanId, "
            + "createTime, "
            + "startTime, "
            + "endTime, "
            + "object_version, "
            + "last_use_time, "
            + "callState, "
            + "firstDialogId, "
            + "secondDialogId, "
            + "firstCallLegConnectionState, "
            + "secondCallLegConnectionState, "
            + "mediaNegotiationState, "
            + "mediaNegotiationMethod, "
            + "maxDurationInMinutes, "
            + "autoTerminate, "
            + "callTerminationCause, "
            + "callLegCausingTermination, "
            + "pendingCallReinvite_dialogId, "
            + "pendingCallReinvite_remoteContact, "
            + "pendingCallReinvite_offerInOkResponse, "
            + "pendingCallReinvite_autoTerminate, "
            + "pendingCallReinvite_mediaDescription, "
            + "pendingCallReinvite_applicationData, housekeepingForced";

    private static final String UPDATE_SQL = "update callinfo set classname=?,"
            + "simpleSipBeanId=?,"
            + "createTime=?,"
            + "startTime=?,"
            + "endTime=?,"
            + "object_version=?,"
            + "last_use_time=?,"
            + "callState=?,"
            + "firstDialogId=?,"
            + "secondDialogId=?,"
            + "firstCallLegConnectionState=?,"
            + "secondCallLegConnectionState=?,"
            + "mediaNegotiationState=?,"
            + "mediaNegotiationMethod=?,"
            + "maxDurationInMinutes=?,"
            + "autoTerminate=?,"
            + "callTerminationCause=?,"
            + "callLegCausingTermination=?,"
            + "pendingCallReinvite_dialogId=?,"
            + "pendingCallReinvite_remoteContact=?,"
            + "pendingCallReinvite_offerInOkResponse=?,"
            + "pendingCallReinvite_autoTerminate=?,"
            + "pendingCallReinvite_mediaDescription=?,"
            + "pendingCallReinvite_applicationData=?,"
            + "housekeepingForced=?"
            + "where callId=? and object_version=?";

    private static final String INSERT_SQL = "insert into callInfo (" + COLUMNS + ") "
            + "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_COLUMNS = "select " + COLUMNS + " from callInfo";

    private static final String SELECT_BY_ID_SQL = SELECT_COLUMNS + " where callId=?";

    private static final String SELECT_WHERE_DIALOG_ID_SQL = SELECT_COLUMNS + " where (firstDialogId=? or secondDialogId=?) and createTime = (select max(createTime) from callInfo where firstDialogId=? or secondDialogId=?)";
    private static final String SELECT_WHERE_DIALOG_ID_IGNORE_CALL_ID_SQL = SELECT_COLUMNS + " where callId != ? and (firstDialogId=? or secondDialogId=?) and createTime = (select max(createTime) from callInfo where callId != ? and (firstDialogId=? or secondDialogId=?))";

    private static final String DELETE_SQL = "delete from callinfo where callid = ?";

    private static final int[] INSERT_TYPES = new int[] {
            Types.VARCHAR, Types.VARCHAR,
            Types.VARCHAR, Types.BIGINT,  Types.BIGINT,  Types.BIGINT,
            Types.VARCHAR, Types.BIGINT,  Types.VARCHAR, Types.VARCHAR,
            Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
            Types.VARCHAR, Types.BIGINT,  Types.VARCHAR, Types.VARCHAR,
            Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.BOOLEAN,
            Types.BOOLEAN, Types.BLOB,    Types.VARCHAR, Types.BOOLEAN, };

    private static final int[] UPDATE_TYPES = new int[] {
            Types.VARCHAR,
            Types.VARCHAR,
            Types.BIGINT,
            Types.BIGINT,
            Types.BIGINT,
            Types.VARCHAR,
            Types.BIGINT,
            Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
            Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.BIGINT,
            Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
            Types.VARCHAR, Types.BOOLEAN, Types.BOOLEAN, Types.BLOB,
            Types.VARCHAR, Types.BOOLEAN, Types.VARCHAR, Types.VARCHAR, };

    private static final String CALLINFO_DELETE_BY_HOUSEKEEPER_FLAGS_SQL = "delete from callinfo where (callstate = 'Terminated' or housekeepingForced = 'True') and last_use_time < ?";
    private static final String CALLINFO_FIND_BY_HOUSEKEEPER_FLAGS_SQL = "select callId from callinfo where (callstate = 'Terminated' or housekeepingForced = 'True') and last_use_time < ?";
    private static final String CALLINFO_UPDATE_BY_HOUSEKEEPER_FLAGS_SQL = SELECT_COLUMNS + " where callstate != 'Terminated' and last_use_time < ?";
    private static final String SELECT_CONNECTED_MAX_DURATION_CALLS_SQL = SELECT_COLUMNS + " where callstate = 'Connected' and maxDurationInMinutes > 0";

    private static final String SELECT_ALL_SQL = "select * from callinfo";

    private Log log = LogFactory.getLog(CallInfoDaoImpl.class);
    private ObjectSerialiser objectSerialiser;

    public CallInfoDaoImpl(DataSource aDataSource) {
    	super(aDataSource);
        this.objectSerialiser = new ObjectSerialiser();
    }

	public void create(CallInfo callInfo) {
        log.debug(String.format("create(%s)", callInfo.getId()));
        try {
            List<Object> paramList = createParams(callInfo);
            Object[] params = paramList.toArray();
            getJdbcTemplate().update(INSERT_SQL, params, INSERT_TYPES);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException(String.format(INFO_S_ALREADY_EXISTS_IN_DATABASE_USE_REPLACE_DIALOG_INSTEAD, callInfo.getId()), e);
        }
    }

    private List<Object> createParams(CallInfo callInfo){
        List<Object> result = new Vector<Object>();
        result.add(callInfo.getId());
        result.add(callInfo.getClass().getName());
        result.add(callInfo.getSimpleSipBeanId());
        result.add(callInfo.getCreateTime());
        result.add(callInfo.getStartTime());
        result.add(callInfo.getEndTime());
        result.add(callInfo.getVersionId());
        result.add(callInfo.getLastUsedTime());
        result.add(readEnum(callInfo.getCallState()));
        result.add(callInfo.getFirstDialogId());
        result.add(callInfo.getSecondDialogId());
        result.add(readEnum(callInfo.getFirstCallLegConnectionState()));
        result.add(readEnum(callInfo.getSecondCallLegConnectionState()));
        result.add(readEnum(callInfo.getMediaNegotiationState()));
        result.add(readEnum(callInfo.getMediaNegotiationMethod()));
        result.add(callInfo.getMaxDurationInMinutes());
        result.add(readEnum(callInfo.getAutoTerminate()));
        result.add(readEnum(callInfo.getCallTerminationCause()));
        result.add(readEnum(callInfo.getCallLegCausingTermination()));
        result.add(null == callInfo.getPendingCallReinvite() ? null : callInfo.getPendingCallReinvite().getDialogId());
        result.add(null == callInfo.getPendingCallReinvite() ? null : callInfo.getPendingCallReinvite().getRemoteContact());
        result.add(null == callInfo.getPendingCallReinvite() ? false : callInfo.getPendingCallReinvite().getOfferInOkResponse());
        result.add(null == callInfo.getPendingCallReinvite() ? false : callInfo.getPendingCallReinvite().getAutoTerminate());
        result.add(null == callInfo.getPendingCallReinvite() ? null : null == callInfo.getPendingCallReinvite().getMediaDescription() ? null : new SqlLobValue(objectSerialiser.serialise(callInfo.getPendingCallReinvite().getMediaDescription())));
        result.add(null == callInfo.getPendingCallReinvite() ? null : callInfo.getPendingCallReinvite().getApplicationData());
        result.add(callInfo.isHousekeepForced());
        return result;
    }

    private Object readEnum(Object e) {
        if (null == e)
            return null;
        return e.toString();
    }

    class CallInfoRowMapper implements RowMapper {

        private static final String ERROR_CREATING_CALL_INFO = "Error creating CallInfo";

        public CallInfoRowMapper () {}

        @SuppressWarnings("unchecked")
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            String classname = rs.getString("classname");
            String creatingBeanName = rs.getString("simpleSipBeanId");
            String aCallId = rs.getString("callId");
            String theFirstDialogId = rs.getString("firstDialogId");
            String theSecondDialogId = rs.getString("secondDialogId");
            String autoTerminateString = rs.getString("autoTerminate");
            AutoTerminateAction aAutoTerminateAction = autoTerminateString == null ? null : AutoTerminateAction.valueOf(autoTerminateString);
            long theMaxDurationInMinutes = rs.getLong("maxDurationInMinutes");

            CallInfo info = null;
            // TODO: test around the exception handling cases
            try {
                Class c = Class.forName(classname);
                Constructor constructor = c.getConstructor(new Class[] {String.class, String.class, String.class, String.class, AutoTerminateAction.class, long.class});
                info = (CallInfo)constructor.newInstance(creatingBeanName, aCallId, theFirstDialogId, theSecondDialogId, aAutoTerminateAction, theMaxDurationInMinutes);
                log.debug(String.format("created %s", info.getClass().getName()));
            } catch (ClassNotFoundException e) {
                log.error(ERROR_CREATING_CALL_INFO, e);
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                log.error(ERROR_CREATING_CALL_INFO, e);
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                log.error(ERROR_CREATING_CALL_INFO, e);
                throw new RuntimeException(e);
            } catch (SecurityException e) {
                log.error(ERROR_CREATING_CALL_INFO, e);
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                log.error(ERROR_CREATING_CALL_INFO, e);
                throw new RuntimeException(e);
            } catch (IllegalArgumentException e) {
                log.error(ERROR_CREATING_CALL_INFO, e);
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                log.error(ERROR_CREATING_CALL_INFO, e);
                throw new RuntimeException(e);
            }

            info.setVersionId(rs.getString("object_version"));
            long createTime = rs.getLong("createTime");
            info.setCreateTime(createTime);
            long startTime = rs.getLong("startTime");
            info.setStartTime(startTime);
            long endTime = rs.getLong("endTime");
            info.setEndTime(endTime);
            String callStateString = rs.getString("callState");
            info.setCallState(callStateString == null ? null : CallState.valueOf(callStateString));
            info.setLastUsedTime(rs.getLong("last_use_time"));
            String firstCallLegConnectionStateString = rs.getString("firstCallLegConnectionState");
            info.setCallLegConnectionState(theFirstDialogId, firstCallLegConnectionStateString == null ? null : CallLegConnectionState.valueOf(firstCallLegConnectionStateString));
            String secondCallLegConnectionState = rs.getString("secondCallLegConnectionState");
            info.setCallLegConnectionState(theSecondDialogId, secondCallLegConnectionState == null ? null : CallLegConnectionState.valueOf(secondCallLegConnectionState));
            String mediaNegStateString = rs.getString("mediaNegotiationState");
            info.setMediaNegotiationState(mediaNegStateString == null ? null : MediaNegotiationState.valueOf(mediaNegStateString));
            String mediaNegMtString = rs.getString("mediaNegotiationMethod");
            info.setMediaNegotiationMethod(mediaNegMtString == null ? null : MediaNegotiationMethod.valueOf(mediaNegMtString));
            String callTermCauseString = rs.getString("callTerminationCause");
            String callLegCausingTermString = rs.getString("callLegCausingTermination");
            info.setCallTerminationCause(callTermCauseString == null ? null : CallTerminationCause.valueOf(callTermCauseString), callLegCausingTermString == null ? null : CallLegCausingTermination.valueOf(callLegCausingTermString));
            String aDialogId = rs.getString("pendingCallReinvite_dialogId");
            MediaDescription aMediaDescription = null;
            byte[] mdba = rs.getBytes("pendingCallReinvite_mediaDescription");
            if (mdba != null) {
                aMediaDescription = (MediaDescription) objectSerialiser.deserialise(mdba);
            }
            String theRemoteContact = rs.getString("pendingCallReinvite_remoteContact");
            String aApplicationData = rs.getString("pendingCallReinvite_applicationData");

            Boolean aOfferInOkResponse = rs.getBoolean("pendingCallReinvite_offerInOkResponse");
            Boolean aPcrAutoTerminate = rs.getBoolean("pendingCallReinvite_autoTerminate");
            PendingCallReinvite pcr = new PendingCallReinvite(aDialogId, aMediaDescription, aPcrAutoTerminate, theRemoteContact, aApplicationData, aOfferInOkResponse);
            info.setPendingCallReinvite(pcr);
            info.setHousekeepForced(rs.getBoolean("housekeepingForced"));

            return info;
        }
    }

    public CallInfo read(String callId) {
        CallInfo callInfo = (CallInfo) getJdbcTemplate().queryForObject(SELECT_BY_ID_SQL, new Object[] { callId }, new CallInfoRowMapper());
        return callInfo;
    }

    public void delete(String callId) {
        Object[] params = new Object[] { callId };
        int[] types = new int[] { Types.VARCHAR };
        getJdbcTemplate().update(DELETE_SQL, params, types);
    }

    public void update(CallInfo callInfo) {
        List<Object> paramList = createParams(callInfo);
        // replace version id with new one
        paramList.set(V_ID_POS, callInfo.generateNewVersionId());
        // remove ID
        paramList.remove(0);
        // add the ID at the end of the list
        paramList.add(callInfo.getId());
        // add old version ID
        String oldVersionId = callInfo.getVersionId();
        paramList.add(oldVersionId);

        Object[] params = paramList.toArray();

        int rowsUpdated = getJdbcTemplate().update(UPDATE_SQL, params, UPDATE_TYPES);
        if (1 != rowsUpdated)
            throw new ConcurrentUpdateException(callInfo.getId(), String.format("Info %s modified in database, try again", callInfo.getId()));
    }

    public int size(){
        return getJdbcTemplate().queryForInt(COUNT_SQL);
    }

    public CallInfo findCallForDialogId(String dialogId) {
        log.debug(String.format("findCallForDialogId(%s)", dialogId));
        return findCallForDialogId(SELECT_WHERE_DIALOG_ID_SQL, new Object[] { dialogId, dialogId, dialogId, dialogId }, dialogId);
    }

    public CallInfo findCallForDialogId(String dialogId, String ignoreCallId) {
        log.debug(String.format("findCallForDialogId(%s, %s)", dialogId, ignoreCallId));
        return findCallForDialogId(SELECT_WHERE_DIALOG_ID_IGNORE_CALL_ID_SQL, new Object[] { ignoreCallId, dialogId, dialogId, ignoreCallId, dialogId, dialogId }, dialogId);
    }

    @SuppressWarnings("unchecked")
	private CallInfo findCallForDialogId(String sql, Object[] params, String dialogId){
    	if (null == dialogId) return null;
    	List result = getJdbcTemplate().query(sql, params, new CallInfoRowMapper());
        if (result.size() < 1) {
            log.debug(String.format(NO_CALL_FOUND_FOR_DIALOG_S_RETURNING_NULL, dialogId));
            return null;
        }
        return (CallInfo)result.get(0);
    }

    public long countByCallState(CallState state, Set<String> callIds){
    	if(state == null)
    		return 0;
    	if(callIds==null){
        	long count = getJdbcTemplate().queryForLong(COUNT_SQL_BY_STATE,new Object[]{state.toString()});
        	return count;
    	}
    	if(callIds.size()==0)
    		return 0;
    	StringBuffer buffer = new StringBuffer();
    	String[] params = new String[callIds.size() + 1];
    	params[0] = state.toString();
    	buffer.append(" and callId in (");
    	String[] callIdsArray = callIds.toArray(new String[callIds.size()]);
    	for(int i = 1; i< callIds.size(); i++){
    		buffer.append("?,");
    		params[i] = callIdsArray[i-1];
    	}
		params[params.length - 1] = callIdsArray[callIdsArray.length - 1];
    	buffer.append("?)");
    	long count = getJdbcTemplate().queryForLong(COUNT_SQL_BY_STATE + buffer.toString(), params);
    	return count;
    }


    public ConcurrentMap<String, CallInfo> getAll() {
    	return getSelectedCalls(SELECT_ALL_SQL);
    }

	public ConcurrentMap<String, CallInfo> findConnectedMaxDurationCalls() {
        return getSelectedCalls(SELECT_CONNECTED_MAX_DURATION_CALLS_SQL);
    }

    @SuppressWarnings("unchecked")
	private ConcurrentMap<String, CallInfo> getSelectedCalls(String sql){
        ConcurrentMap<String, CallInfo> result = new ConcurrentHashMap<String, CallInfo>();
        List records = getJdbcTemplate().query(sql, new CallInfoRowMapper());
        for (int i = 0; i < records.size(); i++) {
            CallInfo callInfo = (CallInfo)records.get(i);
            result.put(callInfo.getId(), callInfo);
        }
        return result;
    }

    @Override
	public String getDeleteByHouseKeeperFlagsSql() {
		return CALLINFO_DELETE_BY_HOUSEKEEPER_FLAGS_SQL;
	}

    @Override
	public String getUpdateByHouseKeeperFlagsSql() {
		return CALLINFO_UPDATE_BY_HOUSEKEEPER_FLAGS_SQL;
	}

    @Override
	public String getFindByHouseKeeperFlagsSql() {
		return CALLINFO_FIND_BY_HOUSEKEEPER_FLAGS_SQL;
	}
}
