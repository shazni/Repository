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

package org.wso2.carbon.registry.core.dao;

import java.io.InputStream;
import java.util.List;

import org.wso2.carbon.registry.core.CollectionImpl;
import org.wso2.carbon.registry.core.ResourceIDImpl;
import org.wso2.carbon.registry.core.ResourceImpl;
import org.wso2.carbon.registry.core.dataaccess.DataAccessManager;
import org.wso2.carbon.registry.core.jdbc.dataobjects.ResourceDO;
import org.wso2.carbon.repository.exceptions.RepositoryException;

/**
 * The data access object for resources.
 */
public interface ResourceDAO {

    /**
     * Returns the resource ID (RID) of the resource referred by the given path. NOTE: This doesn't
     * guarantee that the path exist in the resource. It only guarantee if it is a collection: the
     * path exist in the path table. if it is a resource: the parent path exist in the path table.
     * In order to make sure the existence of the resource please use the resourceExists function
     * Note that if the same path is used for a collection and resource, this returns the resourceID
     * of the collection.
     *
     * @param path Pure path of the resource
     *
     * @return Resource id of resource exists in path cache. null if the resource does not exists.
     * @throws RepositoryException throws the retrieval of resource id failed
     */
    ResourceIDImpl getResourceID(String path) throws RepositoryException;


    /**
     * Returns the resource ID (RID) of the resource referred by the given path. We use this
     * overloaded function when we know our resource is a collection or non-collection NOTE: This
     * doesn't guarantee that the path exist in the resource. It only guarantee if it is a
     * collection: the path exist in the path table. if it is a resource: the parent path exist in
     * the path table. In order to make sure the existence of the resource please use the
     * resourceExists function
     *
     * @param path         Pure path of the resource
     * @param isCollection true if it is a collection
     *
     * @return Resource id of resource exists. null if the resource does not exists.
     * @throws RepositoryException throws the retrieval of resource id failed
     */
    ResourceIDImpl getResourceID(String path, boolean isCollection)
            throws RepositoryException;

    /**
     * Method to check the whether a resource in the provided resource id exist or not.
     *
     * @param resourceID the resource id which is checked for existence of a resource.
     *
     * @return true if the resource exists, false otherwise.
     * @throws RepositoryException throws if checking existence failed.
     */
    boolean resourceExists(ResourceIDImpl resourceID) throws RepositoryException;

    /**
     * Method to check the resource existence for a given path.
     *
     * @param path the path to check the resource existence
     *
     * @return true, if the resource exists, false otherwise
     * @throws RepositoryException throws if checking existence failed.
     */
    boolean resourceExists(String path) throws RepositoryException;

    /**
     * Method to check the resource existence for a given path provided the resource is collection
     * or not.
     *
     * @param path         the path to check the resource existence.
     * @param isCollection provide whether the resource in the path is collection or not.
     *
     * @return true, if the resource exists, false otherwise.
     * @throws RepositoryException throws if checking existence failed.
     */
    boolean resourceExists(String path, boolean isCollection) throws RepositoryException;

    /**
     * Method to return the version of a resource from resourceID
     *
     * @param resourceID the id of the resource to get the version of.
     *
     * @return the version of the resource for the given resource id.
     * @throws RepositoryException throws if the version retrieval failed.
     */
    long getVersion(ResourceIDImpl resourceID) throws RepositoryException;

    /**
     * Returns the resource in the given path filled with meta-data and access to the content. If a
     * resource does not exist in the given path, null is returned.
     *
     * @param path Path of the resource.
     *
     * @return ResourceImpl filled with resource meta-data and access to the resource content.
     * @throws RepositoryException throws if the resource retrieval failed.
     */
    ResourceImpl get(String path) throws RepositoryException;

    /**
     * Returns the resource in the given path filled with meta-data and access to the content. If a
     * resource does not exist in the given resourceID, null is returned.
     *
     * @param resourceID the resource id
     *
     * @return resource object.
     * @throws RepositoryException throws if resource retrieval failed.
     */
    ResourceImpl get(ResourceIDImpl resourceID) throws RepositoryException;

    /**
     * Return a collection with children only at the range of the intersect of the given range and
     * resource existence range provided the resource path. Use for resource browser pagination.
     *
     * @param path    path of the collection
     * @param start   start value of the range of children.
     * @param pageLen the length of the children to retrieve
     *
     * @return an instance of collection with child
     * @throws RepositoryException throws if resource retrieval failed.
     */
    CollectionImpl get(String path, int start, int pageLen) throws RepositoryException;

    /**
     * Return a collection with children only at the range of the intersect of the given range and
     * resource existence range provided the resource id. Use for resource browser pagination.
     *
     * @param resourceID resourceID of the collection
     * @param start      start value of the range of children.
     * @param pageLen    the length of the children to retrieve
     *
     * @return an instance of collection with child
     * @throws RepositoryException throws if resource retrieval failed.
     */
    CollectionImpl get(ResourceIDImpl resourceID, int start, int pageLen)
            throws RepositoryException;

    /**
     * Fill the content (for non-collection) and the properties for a resource that already filled
     * meta data
     *
     * @param resourceImpl the resource instance to be filled with contents (if non-collection) and
     *                     fill properties.
     *
     * @throws RepositoryException throws if resource filling failed.
     */
    void fillResource(ResourceImpl resourceImpl) throws RepositoryException;

    /**
     * Fill the children and the properties for a resource that already filled with meta data.
     * Children are filled only at the at of the intersect of the given range and resource existence
     * range.
     *
     * @param collection collection to fill the children and properties.
     * @param start      start value of the range of children.
     * @param pageLen    the length of the children to retrieve
     *
     * @throws RepositoryException throws if resource filling failed.
     */
    void fillResource(CollectionImpl collection, int start, int pageLen)
            throws RepositoryException;

    /**
     * Fill the properties for a resource without making the properties modified flag.
     *
     * @param resourceImpl the resource object.
     *
     * @throws RepositoryException throws if the operation failed.
     */
    void fillResourcePropertiesWithNoUpdate(ResourceImpl resourceImpl)
            throws RepositoryException;


    /**
     * Fill the properties for a resource, this will change the properties modified flag.
     *
     * @param resourceImpl the resource object.
     *
     * @throws RepositoryException throws if the operation failed.
     */
    void fillResourceProperties(ResourceImpl resourceImpl) throws RepositoryException;

    /**
     * Add the root collection. Only called at the very first time resource loading process.
     *
     * @param resourceImpl the resource instance
     *
     * @throws RepositoryException throws if the operation failed
     */
    void addRoot(ResourceImpl resourceImpl)
            throws RepositoryException;

    /**
     * Add the resource to a pat when resource instance and the parent resource id is given.
     *
     * @param path         path of the resource
     * @param parentID     parent resourceID
     * @param resourceImpl the instance of the resource to be added.
     *
     * @throws RepositoryException throws if the operation failed
     */
    void add(String path, ResourceIDImpl parentID, ResourceImpl resourceImpl)
            throws RepositoryException;

    /**
     * The method to create a resource id and assign to resource instance
     *
     * @param path         path of the resource
     * @param parentID     parent path id
     * @param resourceImpl the resource instance to be assigned the resource id.
     *
     * @throws RepositoryException throws if operation failed.
     */
    void createAndApplyResourceID(String path, ResourceIDImpl parentID,
                                         ResourceImpl resourceImpl)
            throws RepositoryException;

    /**
     * Create a resource ID for a path given the parent resource id and whether it is a collection
     * or not.
     *
     * @param path         the path of the resource
     * @param parentID     parent resource id
     * @param isCollection whether the resource is a collection or not
     *
     * @return the newly created resource id.
     * @throws RepositoryException throws if operation failed.
     */
    ResourceIDImpl createResourceID(String path, ResourceIDImpl parentID,
                                           boolean isCollection)
            throws RepositoryException;

    /**
     * delete the content for a given content id.
     *
     * @param contentID content id.
     *
     * @throws RepositoryException throws if the operation failed.
     */
    void deleteContentStream(int contentID) throws RepositoryException;

    /**
     * Get the content input stream for a given content id.
     *
     * @param contentID the content id as an argument.
     *
     * @return the content input stream.
     * @throws RepositoryException throws if the operation failed.
     */
    InputStream getContentStream(int contentID) throws RepositoryException;

    /**
     * Save the updates of a given resource.
     *
     * @param resourceImpl the resource to be updated.
     *
     * @throws RepositoryException throws if the operation failed.
     */
    void update(ResourceImpl resourceImpl) throws RepositoryException;

    /**
     * Method to return a child count of a collection (database connection should also be provided)
     *
     * @param collection        the collection object which the children are calculated.
     * @param dataAccessManager the data access manager to access the database.
     *
     * @return the child count.
     * @throws RepositoryException throws if the operation failed.
     */
    int getChildCount(CollectionImpl collection, DataAccessManager dataAccessManager)
            throws RepositoryException;

    /**
     * Fill the children for a resource that already filled with meta data. Children are filled only
     * at the at of the intersect of the given range and resource existence range.
     *
     * @param collection collection to fill the children and properties.
     * @param start      start value of the range of children.
     * @param pageLen    the length of the children to retrieve
     *
     * @throws RepositoryException if the operation failed.
     */
    void fillChildren(CollectionImpl collection, int start, int pageLen)
            throws RepositoryException;

    /**
     * Fill the children for a resource that already filled with meta data. Children are filled only
     * at the at of the intersect of the given range and resource existence range.
     *
     * @param collection        collection to fill the children and properties.
     * @param dataAccessManager the data access manager to access the database
     *
     * @throws RepositoryException if the operation failed.
     */
    void fillChildren(CollectionImpl collection, DataAccessManager dataAccessManager)
            throws RepositoryException;

    /**
     * Get the children of the collection. Children are filled only at the at of the intersect of
     * the given range and resource existence range.
     *
     * @param collection        collection to fill the children and properties.
     * @param start             start value of the range of children.
     * @param pageLen           the length of the children to retrieve
     * @param dataAccessManager the data access manager to access the database
     *
     * @return an array of children paths
     * @throws RepositoryException throws if the operation failed.
     */
    String[] getChildren(CollectionImpl collection, int start, int pageLen,
                                DataAccessManager dataAccessManager)
            throws RepositoryException;

    /**
     * Get the children of the collection. Children are filled only at the at of the intersect of
     * the given range and resource existence range.
     *
     * @param collection collection to fill the children and properties.
     * @param start      start value of the range of children.
     * @param pageLen    the length of the children to retrieve
     *
     * @return an array of children paths
     * @throws RepositoryException throws if the operation failed.
     */
    String[] getChildren(CollectionImpl collection, int start, int pageLen)
            throws RepositoryException;

    /**
     * Method to return the resource meta data (excluding properties, content and children)
     *
     * @param path the of the resource
     *
     * @return resource instance with the meta data filled.
     * @throws RepositoryException throws if the operation failed.
     */
    ResourceImpl getResourceMetaData(String path) throws RepositoryException;

    /**
     * Method to return the resource meta data (excluding properties, content and children)
     *
     * @param resourceID the resource id
     *
     * @return resource instance with the meta data filled.
     * @throws RepositoryException throws if the operation failed.
     */
    ResourceImpl getResourceMetaData(ResourceIDImpl resourceID) throws RepositoryException;

    /**
     * Method to get resource without setting the resource modified flags on.
     *
     * @param resourceID the resource id.
     *
     * @return the resource for the given id.
     * @throws RepositoryException throws if the operation failed.
     */
    ResourceImpl getResourceWithNoUpdate(ResourceIDImpl resourceID)
            throws RepositoryException;

    /**
     * Fill resource content for a given resource implementation.
     *
     * @param resourceImpl resource object.
     *
     * @throws RepositoryException throws if the operation failed.
     */
    void fillResourceContent(ResourceImpl resourceImpl) throws RepositoryException;

    /**
     * Fill resource content for a given resource implementation without setting the resource
     * modified flag on.
     *
     * @param resourceImpl resource object.
     *
     * @throws RepositoryException throws if the operation failed.
     */
    void fillResourceContentWithNoUpdate(ResourceImpl resourceImpl)
            throws RepositoryException;


    /**
     * Update the last updated time of a resource, This is called to update the parent's last
     * updated time when a child is created, deleted or moved out/in, copy in.
     *
     * @param resourceID the id of the resource to get the version of.
     *
     * @throws RepositoryException throws if the operation failed.
     */
    void updateCollectionLastUpdatedTime(ResourceIDImpl resourceID)
            throws RepositoryException;

    /**
     * Update the content id of a resource, Normally this should be called after calling
     * addResourceWithoutContentId is called.
     *
     * @param resourceImpl       the resource object.
     *
     * @throws RepositoryException throws if the operation failed.
     */
    void updateContentId(ResourceImpl resourceImpl)
            throws RepositoryException;

    /**
     * Add a resource without a content id, provided whether it is overwriting existing one or not.
     * If the resource is already existing the removal of the older resource will not be handled in
     * this function.
     *
     * @param resourceImpl       the resource object.
     * @param isUpdatingExisting whether the resource is updating or not.
     *
     * @throws RepositoryException throws if the operation failed.
     */
    void addResourceWithoutContentId(ResourceImpl resourceImpl, boolean isUpdatingExisting)
            throws RepositoryException;

    /**
     * Add a resource without setting the resource modified flag on.
     *
     * @param resourceImpl the resource to be added.
     *
     * @throws RepositoryException throws if the operation failed.
     */
    void addResourceWithNoUpdate(ResourceImpl resourceImpl)
            throws RepositoryException;

    /**
     * Add the resource data object.
     *
     * @param resourceDO the resource data object.
     *
     * @throws RepositoryException throws if the operation failed.
     */
    void addResourceDO(ResourceDO resourceDO)
            throws RepositoryException;

    /**
     * Delete the resource provided as a resource DO
     *
     * @param resourceDO the resource to be deleted.
     *
     * @throws RepositoryException throws if the operation failed.
     */
    void deleteResource(ResourceDO resourceDO) throws RepositoryException;

    /**
     * Add the properties to the database from  given resource
     *
     * @param resource to add properties for
     *
     * @throws RepositoryException throws if the operation failed.
     */
    void addProperties(ResourceImpl resource) throws RepositoryException;

    /**
     * Remove properties of a resource.
     *
     * @param resourceDO the resource DO which the properties have to be deleted.
     *
     * @throws RepositoryException throws if the operation failed.
     */
    void removeProperties(ResourceDO resourceDO) throws RepositoryException;

    /**
     * Add the content for a resource.
     *
     * @param resourceImpl the resource to add content.
     *
     * @throws RepositoryException throws if the operation failed.
     */
    void addContent(ResourceImpl resourceImpl) throws RepositoryException;

    /**
     * Add the content to the content table and return the auto generated id of content table.
     *
     * @param contentStream the input stream.
     *
     * @return the auto generated id of content table.
     * @throws RepositoryException throws if the operation failed.
     */
    int addContentBytes(InputStream contentStream) throws RepositoryException;

    /**
     * Method to return resourceDO from a version number.
     *
     * @param version the version of the resource.
     *
     * @return the resourceDO for the given version.
     * @throws RepositoryException throws if the operation failed.
     */
    ResourceDO getResourceDO(long version) throws RepositoryException;

    /**
     * Method to get resource from resource id.
     *
     * @param resourceID the resource id.
     *
     * @return the resource DO for the resource id.
     * @throws RepositoryException throws if the operation failed.
     */
    ResourceDO getResourceDO(ResourceIDImpl resourceID) throws RepositoryException;

    /**
     * Get the child path ids of a resource, (should be a collection)
     *
     * @param resourceID the resource id of the collection.
     *
     * @return an array of child path ids.
     * @throws RepositoryException throws if the operation failed.
     */
    List<ResourceIDImpl> getChildPathIds(ResourceIDImpl resourceID)
            throws RepositoryException;

    /**
     * Get the path from the path id.
     *
     * @param pathId the path id.
     *
     * @return the path corresponding to the path id.
     * @throws RepositoryException throws if operation failed.
     */
    String getPathFromId(int pathId) throws RepositoryException;

    /**
     * Get the path provided the resource version.
     *
     * @param version the version of the resource.
     *
     * @return the path of the resource.
     * @throws RepositoryException throws if the operation failed.
     */
    String getPath(long version) throws RepositoryException;

    /**
     * Get the path given the path id, resource name and provided whether the resourceExistence
     * should be checked.
     *
     * @param pathId         the path id
     * @param resourceName   the resource name
     * @param checkExistence boolean to indicate whether the resource existence should be checked.
     *
     * @return the path
     * @throws RepositoryException if the operation failed.
     */
    String getPath(int pathId, String resourceName, boolean checkExistence)
            throws RepositoryException;

    /**
     * Move resource provided the source and target resource ids.
     *
     * @param source the resource Id of the source.
     * @param target the resource id of the target.
     *
     * @throws RepositoryException throws if the operation failed.
     */
    void moveResources(ResourceIDImpl source, ResourceIDImpl target)
            throws RepositoryException;


    /**
     * This method will move the paths from one path id to another regardless of the resource name
     *
     * @param source the path id of the source resource.
     * @param target the path id of the target resource.
     *
     * @throws RepositoryException throws if the operation failed.
     */
    void moveResourcePaths(ResourceIDImpl source, ResourceIDImpl target)
            throws RepositoryException;

    /**
     * Move the properties.
     *
     * @param source the resource id of the source resource.
     * @param target the resource id of the target resource.
     *
     * @throws RepositoryException throws if the operation failed.
     */
    void moveProperties(ResourceIDImpl source, ResourceIDImpl target)
            throws RepositoryException;

    /**
     * this function will move the paths from one path id to another regardless of the resource
     * name
     *
     * @param source the resource id of the source resource.
     * @param target the resource id of the target resource.
     *
     * @throws RepositoryException throws if the operation failed.
     */
    void movePropertyPaths(ResourceIDImpl source, ResourceIDImpl target)
            throws RepositoryException;
}
