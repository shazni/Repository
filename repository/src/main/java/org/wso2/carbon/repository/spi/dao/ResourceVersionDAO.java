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

package org.wso2.carbon.repository.spi.dao;

import java.io.InputStream;

import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.core.CollectionImpl;
import org.wso2.carbon.repository.core.ResourceIDImpl;
import org.wso2.carbon.repository.core.ResourceImpl;
import org.wso2.carbon.repository.core.dataobjects.ResourceDO;
import org.wso2.carbon.repository.core.utils.VersionRetriever;
import org.wso2.carbon.repository.spi.dataaccess.DataAccessManager;

/**
 * Data Access Object for Resources when versioning for resources has been enabled.
 */
public interface ResourceVersionDAO {

    /**
     * Get the identifiers of the snapshots created for a given path.
     *
     * @param resourcePath the resource path
     *
     * @return a list of identifiers
     * @throws RepositoryException if an error occurs.
     */
    Long[] getSnapshotIDs(String resourcePath) throws RepositoryException;

    /**
     * Fill the archived content to the resource object.
     *
     * @param resourceImpl the resource object.
     *
     * @throws RepositoryException if an error occurs.
     */
    void fillResourceContentArchived(ResourceImpl resourceImpl) throws RepositoryException;

    /**
     * Returns the resource in the given path filled with meta-data and access to the content. If a
     * resource does not exist in the given resourceID, null is returned.
     *
     * @param resourceID the resource id
     * @param snapshotID the snapshot id
     *
     * @return resource object.
     * @throws RepositoryException throws if resource retrieval failed.
     */
    ResourceImpl get(ResourceIDImpl resourceID, long snapshotID) throws RepositoryException;

    /**
     * Method to check the resource existence for a given path.
     *
     * @param resourceID the resource id
     * @param snapshotID the snapshot id
     *
     * @return true, if the resource exists, false otherwise
     * @throws RepositoryException throws if checking existence failed.
     */
    boolean resourceExists(ResourceIDImpl resourceID, long snapshotID) throws RepositoryException;

    /**
     * Creates version retriever that can be used to get the list of versions of a given snapshot,
     * which includes the versions of the children if this was a collection.
     *
     * @param resourceID the resource id
     * @param snapshotID the snapshot id
     *
     * @return version retriever instance.
     * @throws RepositoryException if the operation failed.
     * @see VersionRetriever
     */
    VersionRetriever getVersionList(ResourceIDImpl resourceID, long snapshotID) throws RepositoryException;

    /**
     * Creates version retriever that can be used to get the list of versions of a given snapshot,
     * which includes the versions of the children.
     *
     * @param snapshotID the snapshot id
     *
     * @return version retriever instance.
     * @throws RepositoryException if the operation failed.
     * @see VersionRetriever
     */
    VersionRetriever getVersionList(long snapshotID) throws RepositoryException;

    /**
     * Return a collection with children only at the range of the intersect of the given range and
     * resource existence range provided the resource path. Use for resource browser pagination.
     *
     * @param resourceID resource id of the collection.
     * @param snapshotID snapshot id of the collection.
     * @param start      start value of the range of children.
     * @param pageLen    the length of the children to retrieve
     *
     * @return an instance of collection with child
     * @throws RepositoryException throws if resource retrieval failed.
     */
    CollectionImpl get(ResourceIDImpl resourceID, long snapshotID, int start, int pageLen) throws RepositoryException;

    /**
     * Fill the children for a resource that already filled with meta data. Children are filled only
     * at the at of the intersect of the given range and resource existence range.
     *
     * @param collectionImpl     collection to fill the children and properties.
     * @param versionRetriever   the version retriever used to get the versions.
     * @param parentVersionIndex the version index of the parent.
     * @param start              start value of the range of children.
     * @param pageLen            the length of the children to retrieve.
     * @param snapshotID         the snapshot id.
     *
     * @throws RepositoryException if the operation failed.
     */
    void fillChildren(CollectionImpl collectionImpl, VersionRetriever versionRetriever, int parentVersionIndex, int start, int pageLen, long snapshotID)
            throws RepositoryException;

    /**
     * Get the child paths of a resource, (should be a collection)
     *
     * @param resourceID         the resource id of the collection.
     * @param versionRetriever   the version retriever to be used.
     * @param snapshotID         the snapshot id.
     * @param start              start value of the range of children.
     * @param pageLen            the length of the children to retrieve.
     * @param parentVersionIndex the version index of the parent.
     * @param dataAccessManager  the data access manager to access the database.
     *
     * @return an array of child paths.
     * @throws RepositoryException throws if the operation failed.
     */
    String[] getChildPaths(ResourceIDImpl resourceID, VersionRetriever versionRetriever, int parentVersionIndex, int start, int pageLen,
                                  long snapshotID, DataAccessManager dataAccessManager) throws RepositoryException;

    /**
     * Creates a new snapshot of the resource.
     *
     * @param pathId         the path identifier
     * @param name           the name of the resource
     * @param versionsStream the input stream of versions
     *
     * @return the id of created snapshot.
     * @throws RepositoryException if the operation failed.
     */
    long createSnapshot(int pathId, String name, InputStream versionsStream) throws RepositoryException;

    /**
     * Check whether the resource is already in the history with the give version
     *
     * @param version the version
     *
     * @return whether the resource exists in the history.
     * @throws RepositoryException if the operation failed.
     */
    boolean isResourceHistoryExist(long version) throws RepositoryException;

    /**
     * Check whether the resource is already in the history with the give path
     *
     * @param path the resource path
     *
     * @return whether the resource exists in the history.
     * @throws RepositoryException if the operation failed.
     */
    boolean isResourceHistoryExist(String path) throws RepositoryException;

    /**
     * Check whether the resource is already in the history with the give resourceID
     *
     * @param resourceID the resource identifier.
     *
     * @return whether the resource exists in the history.
     * @throws RepositoryException if the operation failed.
     */
    boolean isResourceHistoryExist(ResourceIDImpl resourceID) throws RepositoryException;

    /**
     * Check whether the content is already in the history.
     *
     * @param contentId the content identifier.
     *
     * @return true if it exist in history, false otherwise.
     * @throws RepositoryException if the operation failed.
     */
    boolean isContentHistoryExist(int contentId) throws RepositoryException;

    /**
     * Restore the resource to the given version.
     *
     * @param version the version.
     * @param snapshotID the snapshot id
     *
     * @return the resource path.
     * @throws RepositoryException if the operation failed.
     */
    String restoreResources(long version, long snapshotID) throws RepositoryException;

    /**
     * Create a version of the given resource.
     *
     * @param resourceDO     the resource data object.
     * @param keepProperties whether to keep properties or not.
     *
     * @throws RepositoryException if the operation failed.
     * @see #putResourceToHistory
     */
    void versionResource(ResourceDO resourceDO, boolean keepProperties) throws RepositoryException;

    /**
     * Method to Archive Resource.
     *
     * @param resourceDO the resource data object.
     *
     * @throws RepositoryException if the operation failed.
     */
    void putResourceToHistory(ResourceDO resourceDO) throws RepositoryException;
    
    /**
     * Removes a snapshot of a given resource.
     *
     * @param snapshotId    id of the snapshot to be removed. 
     *
     * @throws RepositoryException if the operation failed.
     * 
     */
    void removeSnapshot(long snapshotId) throws RepositoryException;
    
    /**
     * Removes any properties associated with a given version of a resource.
     *
     * @param regVersionId   version id of the resource. 
     *
     * @throws RepositoryException if the operation failed.
     * 
     */
    void removePropertyValues(long regVersionId) throws RepositoryException;    
}
