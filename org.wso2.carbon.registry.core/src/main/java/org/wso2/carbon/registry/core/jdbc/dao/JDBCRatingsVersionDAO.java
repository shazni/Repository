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

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.ResourceIDImpl;
import org.wso2.carbon.registry.core.ResourceImpl;
import org.wso2.carbon.registry.core.dao.RatingsDAO;
import org.wso2.carbon.registry.core.dao.ResourceDAO;
import org.wso2.carbon.registry.core.dataaccess.DAOManager;
import org.wso2.carbon.registry.core.exceptions.RepositoryDBException;
import org.wso2.carbon.registry.core.jdbc.DatabaseConstants;
import org.wso2.carbon.registry.core.jdbc.dataaccess.JDBCDatabaseTransaction;
import org.wso2.carbon.registry.core.jdbc.dataobjects.RatingDO;
import org.wso2.carbon.registry.core.session.CurrentSession;
import org.wso2.carbon.registry.core.utils.InternalUtils;
import org.wso2.carbon.repository.RepositoryConstants;;
import org.wso2.carbon.repository.exceptions.RepositoryException;
import org.wso2.carbon.utils.DBUtils;

/**
 * An extension of {@link JDBCRatingsDAO} implements {@link RatingsDAO} to store ratings on a
 * JDBC-based database, when versioning for ratings has been enabled.
 */
public class JDBCRatingsVersionDAO extends JDBCRatingsDAO implements RatingsDAO {

    private static final Log log = LogFactory.getLog(JDBCRatingsVersionDAO.class);
    private ResourceDAO resourceDAO;

    /**
     * Default constructor
     *
     * @param daoManager instance of the data access object manager.
     */
    public JDBCRatingsVersionDAO(DAOManager daoManager) {
        super(daoManager);
        this.resourceDAO = daoManager.getResourceDAO();
    }

    /**
     * Method to persist a rating.
     *
     * @param resourceImpl the resource
     * @param userID       the id of the user who added the rating.
     * @param rating       the rating to be persisted.
     *
     * @throws RepositoryException if some error occurs while adding a rating
     */
    public void addRating(ResourceImpl resourceImpl, String userID, int rating)
            throws RepositoryException {

        JDBCDatabaseTransaction.ManagedRegistryConnection conn =
                JDBCDatabaseTransaction.getConnection();

        PreparedStatement ps1 = null, ps2 = null, ps3 = null;
        ResultSet result = null;
        try {

            String sql1 =
                    "INSERT INTO REG_RATING (REG_RATING, REG_USER_ID, REG_RATED_TIME, " +
                            "REG_TENANT_ID) VALUES (?,?,?,?)";
            String sql2 = "SELECT MAX(REG_ID) FROM REG_RATING";
            String dbProductName = conn.getMetaData().getDatabaseProductName();
            boolean returnsGeneratedKeys = DBUtils.canReturnGeneratedKeys(dbProductName);
            if (returnsGeneratedKeys) {
                ps1 = conn.prepareStatement(sql1, new String[]{DBUtils
                        .getConvertedAutoGeneratedColumnName(dbProductName,
                                DatabaseConstants.ID_FIELD)});
            } else {
                ps1 = conn.prepareStatement(sql1);
            }
            ps1.setInt(1, rating);
            ps1.setString(2, userID);
            long now = System.currentTimeMillis();
            ps1.setDate(3, new Date(now));
            ps1.setInt(4, CurrentSession.getTenantId());
            if (returnsGeneratedKeys) {
                ps1.executeUpdate();
                result = ps1.getGeneratedKeys();
            } else {
                synchronized (ADD_RATING_LOCK) {
                    ps1.executeUpdate();
                    ps2 = conn.prepareStatement(sql2);
                    result = ps2.executeQuery();
                }
            }
            if (result.next()) {
                int rateID = result.getInt(1);

                String sql3 =
                        "INSERT INTO REG_RESOURCE_RATING (REG_RATING_ID, REG_VERSION, " +
                                "REG_TENANT_ID) VALUES(?,?,?)";
                ps3 = conn.prepareStatement(sql3);

                ps3.setInt(1, rateID);
                ps3.setLong(2, resourceImpl.getVersionNumber());
                ps3.setInt(3, CurrentSession.getTenantId());

                ps3.executeUpdate();

            }

        } catch (SQLException e) {

            String msg = "Failed to rate resource " + resourceImpl.getVersionNumber() +
                    " with rating " + rating + ". " + e.getMessage();
            log.error(msg, e);
            throw new RepositoryDBException(msg, e);
        } finally {
            try {
                try {
                    if (result != null) {
                        result.close();
                    }
                } finally {
                    try {
                        if (ps1 != null) {
                            ps1.close();
                        }
                    } finally {
                        try {
                            if (ps2 != null) {
                                ps2.close();
                            }
                        } finally {
                            if (ps3 != null) {
                                ps3.close();
                            }
                        }
                    }
                }
            } catch (SQLException ex) {
                String msg = RepositoryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
    }

    /**
     * Method to copy ratings.
     *
     * @param fromResource the source resource.
     * @param toResource   the target resource.
     *
     * @throws RepositoryException if some error occurs while copying ratings
     */
    public void copyRatings(ResourceImpl fromResource, ResourceImpl toResource)
            throws RepositoryException {

        RatingDO[] ratingDOs = getResourceRatingDO(fromResource);
        addRatings(toResource, ratingDOs);
    }

    /**
     * Method to get a id of a rating added to a given resource.
     *
     * @param resourceImpl the resource.
     * @param userID       the id of the user who added the rating.
     *
     * @return the rate id.
     * @throws RepositoryException if an error occurs while getting the rate id.
     */
    public int getRateID(ResourceImpl resourceImpl, String userID) throws RepositoryException {

        RatingDO ratingDO = getRatingDO(resourceImpl, userID);
        return ratingDO.getRatingID();
    }

    /**
     * Method to get the average rating added to a given resource.
     *
     * @param resourceImpl the resource.
     *
     * @return the average rating.
     * @throws RepositoryException if an error occurs while getting the average rating.
     */
    public float getAverageRating(ResourceImpl resourceImpl) throws RepositoryException {

        JDBCDatabaseTransaction.ManagedRegistryConnection conn =
                JDBCDatabaseTransaction.getConnection();

        int sumRating;
        int countRating;

        PreparedStatement ps = null;
        ResultSet result1 = null;
        try {
            String sql = "SELECT SUM(R.REG_RATING) FROM REG_RATING R, REG_RESOURCE_RATING RR " +
                    "WHERE RR.REG_VERSION=? AND " +
                    "RR.REG_RATING_ID=R.REG_ID AND R.REG_TENANT_ID=? AND RR.REG_TENANT_ID=?";

            ps = conn.prepareStatement(sql);
            ps.setLong(1, resourceImpl.getVersionNumber());
            ps.setInt(2, CurrentSession.getTenantId());
            ps.setInt(3, CurrentSession.getTenantId());

            result1 = ps.executeQuery();

            sumRating = 0;
            if (result1.next()) {
                sumRating = result1.getInt(1);
            }

        } catch (SQLException e) {

            String msg = "Failed to get sum of all ratings on resource " +
                    resourceImpl.getPath() + ". " + e.getMessage();
            log.error(msg, e);
            throw new RepositoryDBException(msg, e);
        } finally {
            try {
                try {
                    if (result1 != null) {
                        result1.close();
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

        ps = null;
        ResultSet result2 = null;
        try {
            String sql = "SELECT COUNT(R.REG_RATING) FROM REG_RATING R, REG_RESOURCE_RATING RR " +
                    "WHERE RR.REG_VERSION=? AND " +
                    "RR.REG_RATING_ID=R.REG_ID AND R.REG_TENANT_ID=? AND RR.REG_TENANT_ID=?";

            ps = conn.prepareStatement(sql);
            ps.setLong(1, resourceImpl.getVersionNumber());
            ps.setInt(2, CurrentSession.getTenantId());
            ps.setInt(3, CurrentSession.getTenantId());

            result2 = ps.executeQuery();

            countRating = 0;
            if (result2.next()) {
                countRating = result2.getInt(1);
            }

        } catch (SQLException e) {

            String msg = "Failed to get ratings count on resource " +
                    resourceImpl.getPath() + ". " + e.getMessage();
            log.error(msg, e);
            throw new RepositoryDBException(msg, e);
        } finally {
            try {
                try {
                    if (result2 != null) {
                        result2.close();
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

        float averageRating = 0;
        if (countRating > 0) {
            averageRating = (float) sumRating / countRating;
        }

        return averageRating;
    }

    /**
     * Method to get a rating added by the given user to the given resource.
     *
     * @param resourceImpl the resource.
     * @param userID       the id of the user who added the rating.
     *
     * @return the rating data object.
     * @throws RepositoryException if an error occurs while getting the rating.
     */
    public RatingDO getRatingDO(ResourceImpl resourceImpl, String userID) throws RepositoryException {

        JDBCDatabaseTransaction.ManagedRegistryConnection conn =
                JDBCDatabaseTransaction.getConnection();

        PreparedStatement ps = null;
        ResultSet result = null;
        RatingDO ratingDO = new RatingDO();
        try {
            String sql = "SELECT R.REG_ID, R.REG_RATING, R.REG_RATED_TIME " +
                    "FROM REG_RATING R, REG_RESOURCE_RATING RR " +
                    "WHERE RR.REG_VERSION=? AND " +
                    "RR.REG_RATING_ID=R.REG_ID AND R.REG_USER_ID=? AND " +
                    "R.REG_TENANT_ID=? AND RR.REG_TENANT_ID=?";

            ps = conn.prepareStatement(sql);
            ps.setLong(1, resourceImpl.getVersionNumber());
            ps.setString(2, userID);
            ps.setInt(3, CurrentSession.getTenantId());
            ps.setInt(4, CurrentSession.getTenantId());

            result = ps.executeQuery();
            if (result.next()) {
                ratingDO.setRating(result.getInt(DatabaseConstants.RATING_FIELD));
                ratingDO.setRatedTime(new java.util.Date(
                        result.getTimestamp(DatabaseConstants.RATED_TIME_FIELD).getTime()));
                ratingDO.setRatedUserName(userID);
                ratingDO.setRatingID(result.getInt(DatabaseConstants.ID_FIELD));
            }
        } catch (SQLException e) {

            String msg = "Failed to get rating on resource " + resourceImpl.getPath() +
                    " done by user " + userID + ". " + e.getMessage();
            log.error(msg, e);
            throw new RepositoryDBException(msg, e);
        } finally {
            try {
                try {
                    if (result != null) {
                        result.close();
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
        return ratingDO;
    }

    /**
     * Method to get ratings added by all users to the given resource.
     *
     * @param resourceImpl the resource.
     *
     * @return array of rating data objects.
     * @throws RepositoryException if an error occurs while getting the rating.
     */
    public RatingDO[] getResourceRatingDO(ResourceImpl resourceImpl) throws RepositoryException {

        JDBCDatabaseTransaction.ManagedRegistryConnection conn =
                JDBCDatabaseTransaction.getConnection();

        PreparedStatement ps = null;
        ResultSet result = null;
        ArrayList<RatingDO> ratingDOs = new ArrayList<RatingDO>();
        try {
            String sql = "SELECT R.REG_ID, R.REG_RATING, R.REG_USER_ID, R.REG_RATED_TIME " +
                    "FROM REG_RATING R, REG_RESOURCE_RATING RR " +
                    "WHERE RR.REG_VERSION=? AND " +
                    "RR.REG_RATING_ID=R.REG_ID AND R.REG_TENANT_ID=? AND RR.REG_TENANT_ID=?";

            ps = conn.prepareStatement(sql);
            ps.setLong(1, resourceImpl.getVersionNumber());
            ps.setInt(2, CurrentSession.getTenantId());
            ps.setInt(3, CurrentSession.getTenantId());

            result = ps.executeQuery();
            while (result.next()) {
                RatingDO ratingDO = new RatingDO();
                ratingDO.setRating(result.getInt(DatabaseConstants.RATING_FIELD));
                ratingDO.setRatedTime(new java.util.Date(
                        result.getTimestamp(DatabaseConstants.RATED_TIME_FIELD).getTime()));
                ratingDO.setRatedUserName(result.getString(DatabaseConstants.USER_ID_FIELD));
                ratingDO.setRatingID(result.getInt(DatabaseConstants.ID_FIELD));

                ratingDOs.add(ratingDO);
            }
        } catch (SQLException e) {

            String msg = "Failed to get rating on resource " + resourceImpl.getPath() +
                    ". " + e.getMessage();
            log.error(msg, e);
            throw new RepositoryDBException(msg, e);
        } finally {
            try {
                try {
                    if (result != null) {
                        result.close();
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
        return ratingDOs.toArray(new RatingDO[ratingDOs.size()]);
    }

    /**
     * Method to get users who rated the given resource.
     *
     * @param resourceImpl the resource.
     *
     * @return array of user names.
     * @throws RepositoryException if an error occurs while getting the rating.
     */
    public String[] getRatedUserNames(ResourceImpl resourceImpl) throws RepositoryException {

        JDBCDatabaseTransaction.ManagedRegistryConnection conn =
                JDBCDatabaseTransaction.getConnection();

        List<String> userIDList = new ArrayList<String>();

        PreparedStatement ps = null;
        ResultSet results = null;
        try {
            String sql = "SELECT R.REG_USER_ID FROM REG_RATING R, REG_RESOURCE_RATING RR " +
                    "WHERE RR.REG_VERSION=? AND " +
                    "RR.REG_RATING_ID=R.REG_ID AND R.REG_TENANT_ID=? AND RR.REG_TENANT_ID=?";

            ps = conn.prepareStatement(sql);
            ps.setLong(1, resourceImpl.getVersionNumber());
            ps.setInt(2, CurrentSession.getTenantId());
            ps.setInt(3, CurrentSession.getTenantId());

            results = ps.executeQuery();
            while (results.next()) {
                String ratedUserID = results.getString(DatabaseConstants.USER_ID_FIELD);
                userIDList.add(ratedUserID);
            }

        } catch (SQLException e) {

            String msg = "Failed to users who have rated the resource " +
                    resourceImpl.getPath() + ". " + e.getMessage();
            log.error(msg, e);
            throw new RepositoryDBException(msg, e);
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

        String[] userIDs = new String[userIDList.size()];
        for (int i = 0; i < userIDs.length; i++) {
            userIDs[i] = userIDList.get(i);
        }

        return userIDs;
    }

    /**
     * Method to get a rating corresponding to the given id.
     *
     * @param ratingID the id of the rating.
     *
     * @return the rating data object.
     * @throws RepositoryException if an error occurs while getting the resource path.
     * @throws SQLException      if an error occurs while getting the rating.
     */
    public RatingDO getRating(long ratingID) throws SQLException, RepositoryException {

        JDBCDatabaseTransaction.ManagedRegistryConnection conn =
                JDBCDatabaseTransaction.getConnection();

        String sql = "SELECT RR.REG_VERSION, " +
                "R.REG_USER_ID, R.REG_RATING, R.REG_RATED_TIME " +
                "FROM REG_RATING R, REG_RESOURCE_RATING RR WHERE R.REG_ID =? AND " +
                "R.REG_ID = RR.REG_RATING_ID AND R.REG_TENANT_ID=? AND RR.REG_TENANT_ID=?";

        PreparedStatement s = conn.prepareStatement(sql);
        try {
            s.setLong(1, ratingID);
            s.setInt(2, CurrentSession.getTenantId());
            s.setInt(3, CurrentSession.getTenantId());

            ResultSet results = s.executeQuery();
            try {
                if (results.next()) {

                    java.util.Date ratedTime = new java.util.Date(
                            results.getTimestamp(DatabaseConstants.RATED_TIME_FIELD).getTime());

                    RatingDO ratingDAO = new RatingDO();
                    ratingDAO.setRatedUserName(results.getString(DatabaseConstants.USER_ID_FIELD));
                    ratingDAO.setRatedTime(ratedTime);
                    ratingDAO.setRating(results.getInt(DatabaseConstants.RATING_FIELD));
                    // TODO:
                    //ratingDAO.setResourceID(results.getString(DatabaseConstants.AID_FIELD));

                    String resourcePath = null;
                    long version = results.getLong(DatabaseConstants.VERSION_FIELD);
                    if (version > 0) {
                        resourcePath = resourceDAO.getPath(version);
                    }
                    if (resourcePath != null) {
                        ratingDAO.setResourcePath(resourcePath);
                    }
                    return ratingDAO;
                }
            } finally {
                if (results != null) {
                    results.close();
                }
            }
        } finally {
            if (s != null) {
                s.close();
            }
        }

        return null;
    }


    /**
     * Gets the resource with sufficient data to differentiate it from another resource. This would
     * populate a {@link ResourceImpl} with the <b>path</b>, <b>name</b> and <b>path identifier</b>
     * of a resource.
     *
     * @param path the path of the resource.
     *
     * @return the resource with minimum data.
     * @throws RepositoryException if an error occurs while retrieving resource data.
     */
    public ResourceImpl getResourceWithMinimumData(String path) throws RepositoryException {
        return InternalUtils.getResourceWithMinimumData(path, resourceDAO, true);
    }

    /**
     * Method to move ratings. This function is not applicable to versioned resources.
     *
     * @param source the source resource.
     * @param target the target resource.
     *
     * @throws RepositoryException if some error occurs while moving ratings
     */
    public void moveRatings(ResourceIDImpl source, ResourceIDImpl target) throws RepositoryException {
        // this is non-versioned specific function.
        // do nothing when the rating versioning on in configuration
    }

    /**
     * Method to move rating paths. This function is not applicable to versioned resources.
     *
     * @param source the source resource.
     * @param target the target resource.
     *
     * @throws RepositoryException if some error occurs while moving rating paths
     */
    public void moveRatingPaths(ResourceIDImpl source, ResourceIDImpl target)
            throws RepositoryException {
        // this is non-versioned specific function.
        // do nothing when the rating versioning on in configuration
    }
}
