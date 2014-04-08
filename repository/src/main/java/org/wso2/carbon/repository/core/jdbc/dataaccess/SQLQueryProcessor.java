/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.repository.core.jdbc.dataaccess;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.repository.api.Collection;
import org.wso2.carbon.repository.api.Repository;
import org.wso2.carbon.repository.api.RepositoryConstants;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.dataaccess.QueryProcessor;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.utils.RepositoryUtils;
import org.wso2.carbon.repository.core.CollectionImpl;
import org.wso2.carbon.repository.core.CurrentContext;
import org.wso2.carbon.repository.core.DatabaseConstants;
import org.wso2.carbon.repository.core.exceptions.RepositoryDBException;
import org.wso2.carbon.repository.core.utils.InternalConstants;
import org.wso2.carbon.repository.spi.dao.ResourceDAO;
import org.wso2.carbon.repository.spi.dataaccess.DataAccessManager;

/**
 * The query processor to execute sql queries.
 */
public class SQLQueryProcessor implements QueryProcessor {

    private static final Log log = LogFactory.getLog(SQLQueryProcessor.class);
    private ResourceDAO resourceDAO;

    /**
     * DataSource of the registry database. URL handlers can access this to construct resources by
     * combining various tables (e.g. comments).
     */
    protected DataSource dataSource;

    /**
     * Initialize the sql query processor
     *
     * @param dataAccessManager the data access manager to be set.
     */
    public SQLQueryProcessor(DataAccessManager dataAccessManager) {
        if (dataAccessManager instanceof JDBCDataAccessManager) {
            this.dataSource = ((JDBCDataAccessManager)dataAccessManager).getDataSource();
        } else {
            log.error("Invalid data access manager.");
        }
        
        this.resourceDAO = dataAccessManager.getDAOManager().getResourceDAO();
    }

    public Collection executeQuery(Repository registry, Resource query, Map<?, ?> parameters) throws RepositoryException {
        Collection resultCollection = null;

        Connection conn;
        String sqlString;
        ResultSet results = null;
        PreparedStatement s = null;

        try {

            Object obj = query.getContent();
            if (parameters != null) {
                Object querySQL = parameters.get("query");
                
                if (querySQL != null) {
                    obj = querySQL;
                }
                
                Object resultType = parameters.get(RepositoryConstants.RESULT_TYPE_PROPERTY_NAME);
                
                if (resultType != null) {
                    if (resultType instanceof String) {
                        query.setProperty(RepositoryConstants.RESULT_TYPE_PROPERTY_NAME,
                                (String) resultType);
                    }
                }
            }
            
            if (obj instanceof String) {
                sqlString = (String) obj;
            } else if (obj instanceof byte[]) {
                sqlString = RepositoryUtils.decodeBytes((byte[]) obj);
            } else {
                throw new RepositoryDBException("Unable to execute query at " + query.getPath()
                        + ".Found resource " + query + "'s content of type " +
                        (obj == null ? "null" : obj.getClass().getName())
                        + ".Expected java.lang.String or byte[]");
            }

            conn = JDBCDatabaseTransaction.getConnection();

            // adding the tenant ids for the query
            TenantAwareSQLTransformer transformer = new TenantAwareSQLTransformer(sqlString);
            String transformedQuery = transformer.getTransformedQuery();
            int transformedParameterCount = transformer.getAdditionalParameterCount();
//            int trailingParameterCount = transformer.getTrailingParameterCount();

            s = conn.prepareStatement(transformedQuery);
            /*s = conn.prepareStatement(transformedQuery, ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY);*/

            int nextParameterIndex = 0;

            // adding the additional parameters caused due to adding the tenant id,
            for (int i = 0; i < transformedParameterCount; i++) {
                nextParameterIndex++;
                s.setInt(nextParameterIndex, CurrentContext.getTenantId());
            }

            if (parameters != null) {
                List<String> exclusions = Arrays.asList("content", "query", "mediaType", RepositoryConstants.RESULT_TYPE_PROPERTY_NAME);
                
                for (Object parameterNumberObject : parameters.keySet()) {
                    String parameterNumber = (String) parameterNumberObject;
                    
                    if (exclusions.contains(parameterNumber)) {
                        continue;
                    }
                    
                    Object parameterValue = parameters.get(parameterNumber);
                    s.setObject(Integer.parseInt(parameterNumber) + nextParameterIndex, parameterValue);
                }
            }

            results = s.executeQuery();

            String resultType = query.getPropertyValue(RepositoryConstants.RESULT_TYPE_PROPERTY_NAME);

            if (resultType == null) {
                resultType = RepositoryConstants.RESOURCES_RESULT_TYPE;
                query.setProperty(RepositoryConstants.RESULT_TYPE_PROPERTY_NAME, resultType);
            }

            if (resultType.equals(RepositoryConstants.RESOURCES_RESULT_TYPE)) {
                // Result is a normal resource, which is stored in the Resources table or a
                // collection of normal resources.

                resultCollection = fillResourcesCollection(results);
            } else if (resultType.equals(InternalConstants.RESOURCE_UUID_RESULT_TYPE)) {
                resultCollection = fillResourceUUIDCollection(results);
            } else if (resultType.equals(InternalConstants.COMMENTS_RESULT_TYPE)) {
                resultCollection = fillCommentsCollection(results, conn);
            } else if (resultType.equals(RepositoryConstants.RATINGS_RESULT_TYPE)) {
                resultCollection = fillRatingsCollection(results);
            } else if (resultType.equals(RepositoryConstants.TAGS_RESULT_TYPE)) {
                resultCollection = fillTagsCollection(results);
            } else if (resultType.equals(InternalConstants.TAG_SUMMARY_RESULT_TYPE)){
                resultCollection = fillTagSummaryCollection(results);
            }

            if (resultCollection == null) {
                String msg = "Unknown result type: " + resultType + " defined for the query: " +
                        sqlString + ((query.getPath() != null) ? " located in path: " +
                        query.getPath() : "");
                log.error(msg);
                throw new RepositoryDBException(msg);
            }
        } catch (SQLException e) {
            throw new RepositoryDBException(e.getMessage());
        } finally {
            if (results != null) {
                try {
                    results.close();
                } catch (SQLException e) {
                    String msg = "Failed to close the result set. " + e.getMessage();
                    log.error(msg, e);
                }
            }

            if (s != null) {
                try {
                    s.close();
                } catch (SQLException e) {
                    log.error("Failed to close the statement. " + e.getMessage());
                }
            }
        }

        if (resultCollection != null && RepositoryConstants.RESOURCES_RESULT_TYPE.equals(
                query.getPropertyValue(RepositoryConstants.RESULT_TYPE_PROPERTY_NAME))) {

            List<String> filteredResults = new ArrayList<String>();
            String[] resultPaths = resultCollection.getChildPaths();
            for (String resultPath : resultPaths) {
                    filteredResults.add(resultPath);
            }

            String[] filteredContent = filteredResults.toArray(new String[filteredResults.size()]);
            resultCollection.setContent(filteredContent);
        }

        return resultCollection;
    }

    /**
     * Fil the resource collection from a query result set with resources
     *
     * @param results The result set object.
     *
     * @return A collection containing results as children.
     * @throws SQLException      throws if the iterating results failed.
     * @throws RepositoryException throws if constructing child pas failed.
     */
    private Collection fillResourcesCollection(ResultSet results) throws SQLException, RepositoryException {
    // Result is a normal resource, which is stored in the Resources table or a collection
    // of normal resources.
	//We can't use a HashSet here, because it doesn't keep the order that may lead to failures of queries having ORDER BY
        Set<String>  pathSet = new LinkedHashSet<String>();        
        while (results.next()) {
            int pathId = results.getInt(DatabaseConstants.PATH_ID_FIELD);
            String resourceName = results.getString(DatabaseConstants.NAME_FIELD);
            String path = resourceDAO.getPath(pathId, resourceName, false);
            if(path!=null){
                pathSet.add(path);
            }
        }
        String[] paths = pathSet.toArray(new String[pathSet.size()]);
        return new CollectionImpl(paths);
    }

    /**
     * Fil the resource collection from a query result set with comments
     *
     * @param results The result set object.
     * @param conn    connection object.
     *
     * @return A collection containing results as children.
     * @throws SQLException      throws if the iterating results failed.
     * @throws RepositoryException throws if constructing child pas failed.
     */
    private Collection fillCommentsCollection(ResultSet results, Connection conn) throws SQLException, RepositoryException {
//        if (!(commentsDAO instanceof JDBCCommentsDAO)) {
//            String msg = "Failed to list of comments. Invalid comments data access object.";
//            log.error(msg);
//            throw new RepositoryDBException(msg);
//        }

        // SQL query should return a list of DatabaseConstants.COMMENT_ID_FIELD.
        // resultArtifact contains a String[] of URLs for comments.

        // URL for a comment <resource_path>?comment<comment_id>
        // e.g. /p1/r1?comment12

        List<Long> commentIDs = new ArrayList<Long>();

        while (results.next()) {
            long commentID = results.getLong(DatabaseConstants.COMMENT_ID_FIELD);
            commentIDs.add(commentID);
        }

        String[] commentPaths = null ;
//        commentPaths = ((JDBCCommentsDAO)commentsDAO).getResourcePathsOfComments(
//                commentIDs.toArray(new Long[commentIDs.size()]), conn);

        return new CollectionImpl(commentPaths);
    }

    /**
     * Fil the resource collection from a query result set with ratings
     *
     * @param results The result set object.
     *
     * @return A collection containing results as children.
     * @throws SQLException      throws if the iterating results failed.
     * @throws RepositoryException throws if constructing child pas failed.
     */
    private Collection fillRatingsCollection(ResultSet results) throws SQLException, RepositoryException {
//        if (!(ratingsDAO instanceof JDBCRatingsDAO)) {
//            String msg = "Failed to list of ratings. Invalid ratings data access object.";
//            log.error(msg);
//            throw new RepositoryDBException(msg);
//        }

        // SQL query should return a list of DatabaseConstants.RATING_ID_FIELD
        // resultArtifact contains a String[] of URLs for ratings.

        // URL for a comment <resource_path>;ratings:<userName>
        // e.g. /p1/r1;ratings:foo

        List<String> ratingPathList = new ArrayList<String>();

//        while (results.next()) {
//            long ratingID = results.getLong(DatabaseConstants.RATING_ID_FIELD);
//            RatingDO ratingDAO = ((JDBCRatingsDAO)ratingsDAO).getRating(ratingID);
//            String ratingPath =
//                    ratingDAO.getPath() + RepositoryConstants.URL_SEPARATOR + "ratings:" +
//                            ratingDAO.getRatedUserName();
//            ratingPathList.add(ratingPath);
//        }

        String[] ratingPaths = ratingPathList.toArray(new String[ratingPathList.size()]);

        return new CollectionImpl(ratingPaths);
    }

    /**
     * Fil the resource collection from a query result set with tags
     *
     * @param results The result set object.
     *
     * @return A collection containing results as children.
     * @throws SQLException      throws if the iterating results failed.
     * @throws RepositoryException throws if constructing child pas failed.
     */
    private Collection fillTagsCollection(ResultSet results) throws SQLException, RepositoryException {

        // URL for a tag /p1/r1;tags:tagName:userName

        List<String> tagPathList = new ArrayList<String>();
        while (results.next()) {
            long taggingID = results.getLong(DatabaseConstants.TAGGING_ID_FIELD);

//            TaggingDO taggingDO = tagsDAO.getTagging(taggingID);

//            String tagPath = taggingDO.getPath() + RepositoryConstants.URL_SEPARATOR +
//                    "tags:" + taggingDO.getTagName() + ":" + taggingDO.getTaggedUserName();
//            tagPathList.add(tagPath);
        }

        String[] tagPaths = tagPathList.toArray(new String[tagPathList.size()]);

        return new CollectionImpl(tagPaths);
    }

    /**
     * A summary count of all tags. Format is "tagname # totalcount"
     * @param results
     * @return tag summery collection
     * @throws SQLException
     * @throws RepositoryException
     */
    private Collection fillTagSummaryCollection(ResultSet results)
        throws SQLException, RepositoryException{
        List<String> tagPathList = new ArrayList<String>();
        String mockPath;
        String tagName;
        int tagOccurrence;

        while(results.next()){
            mockPath = results.getString(DatabaseConstants.MOCK_PATH);
            tagName = results.getString(DatabaseConstants.TAG_NAME);
            tagOccurrence = results.getInt(DatabaseConstants.USED_COUNT);

            String tagPath = mockPath+";"+tagName +":"+String.valueOf(tagOccurrence);
            tagPathList.add(tagPath);
        }
        
        String[] tagPaths = tagPathList.toArray(new String[tagPathList.size()]);
        
        return new CollectionImpl(tagPaths);
    }

    /**
     * Fill resource path collection from a query result set
     *
     * @param results The result set object.
     *
     * @return A collection containing results as children.
     * @throws SQLException      throws if the iterating results failed.
     * @throws RepositoryException throws if constructing child pas failed.
     */
    private Collection fillResourceUUIDCollection(ResultSet results) throws SQLException, RepositoryException {
        List<String> uuidList = new ArrayList<String>();
        String mockPath;
        String resourceUUID;
        String path;
        
        while (results.next()) {
            mockPath = results.getString(DatabaseConstants.MOCK_PATH);
            resourceUUID = results.getString(DatabaseConstants.UUID_FIELD);
            path = mockPath+";"+resourceUUID;

            if (path != null && (!uuidList.contains(path))) {
                uuidList.add(path);
            }
        }

        String[] paths = uuidList.toArray(new String[uuidList.size()]);
        
        return new CollectionImpl(paths);
    }

}
