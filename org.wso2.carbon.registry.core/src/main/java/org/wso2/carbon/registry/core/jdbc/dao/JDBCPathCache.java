/*
 *  Copyright (c) 2005-2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.registry.core.jdbc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.cache.Cache;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.caching.PathCache;
import org.wso2.carbon.registry.core.caching.RegistryCacheEntry;
import org.wso2.carbon.registry.core.caching.RegistryCacheKey;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.dataaccess.DataAccessManager;
import org.wso2.carbon.registry.core.exceptions.RepositoryServerException;
import org.wso2.carbon.registry.core.jdbc.DatabaseConstants;
import org.wso2.carbon.registry.core.jdbc.dataaccess.JDBCDataAccessManager;
import org.wso2.carbon.registry.core.session.CurrentSession;
import org.wso2.carbon.registry.core.utils.InternalUtils;
import org.wso2.carbon.repository.RepositoryConstants;;
import org.wso2.carbon.repository.exceptions.RepositoryException;
import org.wso2.carbon.utils.DBUtils;

/**
 * An extension of the {@link PathCache} to store paths of registry resources on a JDBC-based
 * database.
 */
public class JDBCPathCache extends PathCache {

    private static final Log log = LogFactory.getLog(JDBCPathCache.class);
    private static final Object ADD_ENTRY_LOCK = new Object();

    public static JDBCPathCache getPathCache() {
        return new JDBCPathCache();
    }

    /**
     * Method to add resource path entry to the database.
     *
     * @param path         the path to add.
     * @param parentPathId the parent path's id.
     *
     * @return the path's id.
     * @throws RepositoryException if the data access manager was invalid.
     * @throws SQLException      if an error occurs while adding the entry.
     */
    public int addEntry(String path, int parentPathId) throws SQLException, RepositoryException {
        ResultSet results = null;
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        DataAccessManager dataAccessManager;
        if (CurrentSession.getUserRegistry() != null
                && CurrentSession.getUserRegistry().getRegistryContext() != null) {
            dataAccessManager = CurrentSession.getUserRegistry().getRegistryContext()
                    .getDataAccessManager();
        } else {
            // TODO: This code block doesn't seem to get hit. Remove if unused.
            dataAccessManager = RegistryContext.getBaseInstance().getDataAccessManager();
        }
        if (!(dataAccessManager instanceof JDBCDataAccessManager)) {
            String msg = "Failed to add path entry. Invalid data access manager.";
            log.error(msg);
            throw new RepositoryServerException(msg);
        }
        DataSource dataSource = ((JDBCDataAccessManager)dataAccessManager).getDataSource();
        Connection conn = dataSource.getConnection();
        if (conn != null) {
            if (conn.getTransactionIsolation() != Connection.TRANSACTION_READ_COMMITTED) {
                conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            }
            conn.setAutoCommit(false);
        } else {
            log.error("Unable to acquire connection to database.");
            return -1;
        }
        boolean success = false;
        int pathId = 0;

        try {
            String sql =
                    "INSERT INTO REG_PATH(REG_PATH_VALUE, REG_PATH_PARENT_ID, REG_TENANT_ID) " +
                            "VALUES (?, ?, ?)";
            String sql1 = "SELECT MAX(REG_PATH_ID) FROM REG_PATH";
            String dbProductName = conn.getMetaData().getDatabaseProductName();
            boolean returnsGeneratedKeys = DBUtils.canReturnGeneratedKeys(dbProductName);
            if (returnsGeneratedKeys) {
                ps = conn.prepareStatement(sql, new String[]{
                        DBUtils.getConvertedAutoGeneratedColumnName(dbProductName, "REG_PATH_ID")});
            } else {
                ps = conn.prepareStatement(sql);
            }
            ps.setString(1, path);
            ps.setInt(2, parentPathId);
            ps.setInt(3, CurrentSession.getTenantId());
            if (returnsGeneratedKeys) {
                ps.executeUpdate();
                results = ps.getGeneratedKeys();
            } else {
                synchronized (ADD_ENTRY_LOCK) {
                    ps.executeUpdate();
                    ps1 = conn.prepareStatement(sql1);
                    results = ps1.executeQuery();
                }
            }
            if (results.next()) {
                pathId = results.getInt(1);
                if (pathId > 0) {
                    success = true;
                    return pathId;
                }
            }
        } catch (SQLException e) {
            // we have to be expecting an exception with the duplicate value for the path value
            // which can be further checked from here..
            String msg = "Failed to insert resource to " + path + ". " + e.getMessage();
            log.error(msg, e);
            throw e;
        } finally {
            if (success) {
                try {
                    conn.commit();
                    RegistryCacheEntry e = new RegistryCacheEntry(pathId);
                    String connectionId = null;
                    if (conn.getMetaData() != null) {
                        connectionId = InternalUtils.getConnectionId(conn);
                    }
                    RegistryCacheKey key =
                    		InternalUtils.buildRegistryCacheKey(connectionId,
                                    CurrentSession.getTenantId(), path);
                    getCache().put(key, e);

                } catch (SQLException e) {
                    String msg = "Failed to commit transaction. Inserting " + path + ". " +
                            e.getMessage();
                    log.error(msg, e);
                } finally {
                    try {
                        try {
                            if (results != null) {
                                results.close();
                            }
                        } finally {
                            try {
                                if (ps1 != null) {
                                    ps1.close();
                                }
                            } finally {
                                try {
                                    if (ps != null) {
                                        ps.close();
                                    }
                                } finally {
                                    conn.close();
                                }
                            }
                        }
                    } catch (SQLException e) {
                        String msg = RepositoryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR +
                                e.getMessage();
                        log.error(msg, e);
                    }
                }
            } else {
                try {
                    conn.rollback();

                } catch (SQLException e) {
                    String msg = "Failed to rollback transaction. Inserting " + path + ". " +
                            e.getMessage();
                    log.error(msg, e);
                } finally {
                    try {
                        try {
                            if (results != null) {
                                results.close();
                            }
                        } finally {
                            try {
                                if (ps != null) {
                                    ps.close();
                                }
                            } finally {
                                if (conn != null) {
                                    conn.close();
                                }
                            }
                        }
                    } catch (SQLException e) {
                        String msg = RepositoryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR +
                                e.getMessage();
                        log.error(msg, e);
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Method to get the path of a given path id.
     *
     * @param conn the database connection to use.
     * @param id the path.
     *
     * @return the path corresponding to the given path id.
     * @throws SQLException if an error occurs while obtaining the path id.
     */
    public String getPath(Connection conn, int id) throws SQLException {
        String connectionId;
        if (conn != null && conn.getMetaData() != null) {
            connectionId = InternalUtils.getConnectionId(conn);
        } else {
            throw new SQLException("Connection is null");
        }
        RegistryCacheKey key =
        		InternalUtils.buildRegistryCacheKey(connectionId,
                        CurrentSession.getTenantId(), Integer.toString(id));
        Cache<RegistryCacheKey, RegistryCacheEntry> cache = getCache();
        RegistryCacheEntry result = cache.get(key);
        if (result != null) {
            return result.getPath();
        } else {
            PreparedStatement ps = null;
            ResultSet results = null;
            try {
                String sql =
                        "SELECT REG_PATH_VALUE FROM REG_PATH WHERE REG_PATH_ID=? AND REG_TENANT_ID=?";

                ps = conn.prepareStatement(sql);
                ps.setInt(1, id);
                ps.setInt(2, CurrentSession.getTenantId());

                results = ps.executeQuery();
                if (results.next()) {
                    result = new RegistryCacheEntry(results.getString(DatabaseConstants.PATH_VALUE_FIELD));
                    cache.put(key, result);
                    return result.getPath();
                }

            } finally {
                try {
                    try {
                        if (results != null) {
                            results.close();
                        }
                    } finally {
                        if (ps != null) {
                            ps.close();
                        }
                    }
                } catch (SQLException ex) {
                    String msg = RepositoryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                    log.error(msg, ex);
                }
            }
            return null;
        }
    }

    /**
     * Method to get the path id of a given path.
     *
     * @param conn the database connection to use.
     * @param path the path.
     *
     * @return the path id corresponding to the given path.
     * @throws SQLException if an error occurs while obtaining the path id.
     */
    public int getPathID(Connection conn, String path) throws SQLException {
        String connectionId = null;
        if (conn != null && conn.getMetaData() != null) {
            connectionId = InternalUtils.getConnectionId(conn);
        } else {
            throw new SQLException("Connection is null");
        }
        RegistryCacheKey key =
        		InternalUtils.buildRegistryCacheKey(connectionId,
                        CurrentSession.getTenantId(), path);
        Cache<RegistryCacheKey,RegistryCacheEntry> cache = getCache();
        RegistryCacheEntry result = (RegistryCacheEntry) cache.get(key);

        // TODO: FIX: Path Cache should only be updated if the key yields a valid registry path.
        // Recently, this has lead to:
        // org.wso2.carbon.registry.core.exceptions.RepositoryException: Failed to add resource to
        // path /_system. Cannot add or update a child row: a foreign key constraint fails
        // (`stratos_db`.`REG_RESOURCE`, CONSTRAINT `REG_RESOURCE_FK_BY_PATH_ID` FOREIGN KEY
        // (`REG_PATH_ID`, `REG_TENANT_ID`) REFERENCES `REG_PATH` (`REG_PATH_ID`, `REG_TENANT_ID`))
        //
        // when registry separation is enabled. Thus, we need a better solution to address this, and
        // a better key which also contains the name of the DB in use. Un-comment the below once
        // this has been done.
        //
        // IMPORTANT: Never remove this comment until we are certain that the current fix is
        // actually working - Senaka.

        if (result != null) {
            return result.getPathId();
        } else {
            ResultSet results = null;
            PreparedStatement ps = null;
            try {
                String sql =
                        "SELECT REG_PATH_ID FROM REG_PATH WHERE REG_PATH_VALUE=? " +
                                "AND REG_TENANT_ID=?";
                ps = conn.prepareStatement(sql);
                ps.setString(1, path);
                ps.setInt(2, CurrentSession.getTenantId());
                results = ps.executeQuery();

                int pathId;
                if (results.next()) {
                    pathId = results.getInt(DatabaseConstants.PATH_ID_FIELD);

                    if (pathId > 0) {
                        RegistryCacheEntry e = new RegistryCacheEntry(pathId);
                        cache.put(key, e);
                        return pathId;
                    }
                }

            } catch (SQLException e) {
                String msg = "Failed to retrieving resource from " + path + ". " + e.getMessage();
                log.error(msg, e);
                throw e;
            } finally {
                try {
                    try {
                        if (results != null) {
                            results.close();
                        }
                    } finally {
                        if (ps != null) {
                            ps.close();
                        }
                    }
                } catch (SQLException e) {
                    String msg = RepositoryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR +
                            e.getMessage();
                    log.error(msg, e);
                }
            }
        }
        return -1;
    }
}