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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.SqlLobValue;

import com.bt.aloha.state.StateInfoBase;
import com.bt.aloha.util.ConcurrentUpdateException;
import com.bt.aloha.util.ObjectSerialiser;

public class StateInfoDaoImpl<T extends StateInfoBase<T>> extends DaoBase implements StateInfoDao<T> {
    private static Log log = LogFactory.getLog(StateInfoDaoImpl.class);
    private static final String UNCHECKED = "unchecked";
    private static final String NULL_INFO_ID = "Null info id";
    private static final String INSERT_SQL = "insert into StateInfo (object_id, object_type, object_version, last_use_time, is_dead, force_housekeep, object_value) values(?, ?, ?, ?, ?, 0, ?)";
    private static final String SELECT_SQL = "select object_id, object_value from StateInfo where object_id=?";
    private static final String SELECT_ALL_SQL = "select object_id, object_value from StateInfo where object_type=?";
    private static final String DELETE_SQL = "delete from StateInfo where object_id=?";
    private static final String UPDATE_SQL = "update StateInfo set object_version=?, last_use_time=?, is_dead=?, object_value=? where object_id=? and object_version=?";
    private static final String SIZE_SQL = "select count(*) from StateInfo where object_type=?";
    private static final String STATEINFO_DELETE_BY_HOUSEKEEPER_FLAGS_SQL = "delete from StateInfo where last_use_time < ? and (is_dead = 1 or force_housekeep = 1) and object_type=?";
    private static final String STATEINFO_SELECT_BY_HOUSEKEEPER_FLAGS_SQL = "select * from StateInfo where last_use_time < ? and is_dead = 0 and object_type=?";
    private static final String STATEINFO_UPDATE_BY_HOUSEKEEPER_FLAGS_SQL = "update StateInfo set force_housekeep = 1 where last_use_time < ? and is_dead = 0 and object_type=?";

    private static class TRowMapper<T> implements RowMapper {
        private ObjectSerialiser objectSerialiser;

        public TRowMapper() {
            objectSerialiser = new ObjectSerialiser();
        }

        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            byte[] bytes = rs.getBytes("object_value");
            return objectSerialiser.deserialise(bytes);
        }
    }

    public StateInfoDaoImpl(DataSource dataSource) {
        super(dataSource);
        checkTableExistence();
    }

    private void checkTableExistence() {
        try {
            getJdbcTemplate().execute("select 1 from StateInfo");
        } catch (DataAccessException e) {
            throw new IllegalStateException("Error reading StateInfo table", e);
        }
    }

    public void add(StateInfoBase<T> info, String collectionTypeName) {
        if (collectionTypeName == null)
            throw new IllegalArgumentException("Cannot add null collection type to collection.");

        if (info == null)
            throw new IllegalArgumentException("Cannot add null info object to collection.");

        if (info.getId() == null)
            throw new IllegalArgumentException("Cannot add info object with null id to collection.");

        try {
            Object[] params = new Object[] { info.getId(), collectionTypeName, info.getVersionId(),
                    info.getLastUsedTime(), info.isDead() ? 1 : 0,
                    new SqlLobValue(new ObjectSerialiser().serialise(info)), };
            int[] types = new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.BIGINT, Types.INTEGER,
                    Types.BLOB };
            getJdbcTemplate().update(INSERT_SQL, params, types);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException(String.format(
                    "Info %s already exists in database, use replaceDialog instead", info.getId()), e);
        } catch (DataAccessException e) {
            throw new IllegalArgumentException(String.format("Cannot add info %s to database", info.getId()), e);
        }
    }

    @SuppressWarnings(UNCHECKED)
    public T get(String infoId) {
        if (infoId == null)
            throw new IllegalArgumentException(NULL_INFO_ID);

        try {
            Object[] params = new Object[] { infoId };
            int[] types = new int[] { Types.VARCHAR };

            List results = getJdbcTemplate().query(SELECT_SQL, params, types, new TRowMapper());

            if (results.size() > 1)
                throw new IllegalStateException(String.format("More than one info object with id %s", infoId));
            if (results.size() == 0) {
                log.debug(String.format("No info object for id %s , returning null ", infoId));
                return null;
            }
            return (T) results.get(0);

        } catch (DataAccessException e) {
            throw new IllegalArgumentException(String.format("Cannot retrieve info %s from database", infoId), e);
        }
    }

    public void remove(String infoId) {
        if (infoId == null)
            throw new IllegalArgumentException(NULL_INFO_ID);

        try {
            Object[] params = new Object[] { infoId };
            int[] types = new int[] { Types.VARCHAR };
            getJdbcTemplate().update(DELETE_SQL, params, types);
        } catch (DataAccessException e) {
            throw new IllegalArgumentException(String.format("Cannot delete info %s from database", infoId), e);
        }
    }

    public void replace(final T info) {
        if (info == null)
            throw new IllegalArgumentException("Cannot replace null info object in collection.");

        T newInfo = info.cloneObject();
        newInfo.updateVersionId();
        newInfo.updateLastUsedTime();

        int updated = 0;
        try {
            Object[] params = new Object[] { newInfo.getVersionId(), newInfo.getLastUsedTime(),
                    newInfo.isDead() ? 1 : 0, new SqlLobValue(new ObjectSerialiser().serialise(newInfo)), info.getId(),
                    info.getVersionId(), };
            int[] types = new int[] { Types.VARCHAR, Types.BIGINT, Types.INTEGER, Types.BLOB, Types.VARCHAR,
                    Types.VARCHAR };
            updated = getJdbcTemplate().update(UPDATE_SQL, params, types);
        } catch (DataAccessException e) {
            throw new IllegalArgumentException(String.format("Cannot update info %s in database", info.getId()), e);
        }
        if (updated == 0)
            throw new ConcurrentUpdateException(info.getId(), String.format("Info %s modified in database, try again",
                    info.getId()));

        info.setVersionId(newInfo.getVersionId());
        info.setLastUsedTime(newInfo.getLastUsedTime());
    }

    public int size(String collectionTypeName) {
        return getJdbcTemplate().queryForInt(SIZE_SQL, new Object[] { collectionTypeName });
    }

    @SuppressWarnings(UNCHECKED)
    public ConcurrentMap<String, T> getAll(String collectionTypeName) {
        List<T> all;
        Object[] params = new Object[] { collectionTypeName };
        try {
            all = getJdbcTemplate().query(SELECT_ALL_SQL, params, new TRowMapper());
        } catch (DataAccessException e) {
            throw new IllegalArgumentException("Cannot retrieve all info objects from database", e);
        }
        ConcurrentMap<String, T> map = new ConcurrentHashMap<String, T>();
        for (T item : all) {
            map.put(item.getId(), item);
        }
        return map;
    }

    public void housekeep(String collectionTypeName, long maxTimeToLive, RowCallbackHandler rowCallBackHandler) {
        try {
            log.debug(String.format("Housekeeping: current number of objects in database is %d",
                    size(collectionTypeName)));
            long houseKeepBefore = System.currentTimeMillis() - maxTimeToLive;
            Object[] params = new Object[] { houseKeepBefore, collectionTypeName };
            int[] types = new int[] { Types.BIGINT, Types.VARCHAR };
            int deleted = getJdbcTemplate().update(STATEINFO_DELETE_BY_HOUSEKEEPER_FLAGS_SQL, params, types);
            log.info(String.format("Removed %d objects from database", deleted));
            getJdbcTemplate().query(STATEINFO_SELECT_BY_HOUSEKEEPER_FLAGS_SQL, params, rowCallBackHandler);
            int updated = getJdbcTemplate().update(STATEINFO_UPDATE_BY_HOUSEKEEPER_FLAGS_SQL, params, types);
            log.info(String.format("Forced housekeeping on %d objects from database", updated));
        } catch (DataAccessException e) {
            log.error(String.format("Error occurred during database housekeeping: %s", e.getMessage()));
        }
    }

    @Override
    protected String getDeleteByHouseKeeperFlagsSql() {
        return STATEINFO_DELETE_BY_HOUSEKEEPER_FLAGS_SQL;
    }

    @Override
    protected String getFindByHouseKeeperFlagsSql() {
        return STATEINFO_SELECT_BY_HOUSEKEEPER_FLAGS_SQL;
    }

    @Override
    protected String getUpdateByHouseKeeperFlagsSql() {
        return STATEINFO_UPDATE_BY_HOUSEKEEPER_FLAGS_SQL;
    }
}
