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

package org.wso2.carbon.repository.api;

import org.wso2.carbon.repository.api.exceptions.RepositoryException;

import java.io.Reader;
import java.io.Writer;
import java.util.Date;
import java.util.Map;

/**
 * This is the "Full" Repository interface. It contains not only the get/put behavior
 * but also APIs which control rename, create link etc.
 */
public interface Repository extends TransactionManager {
	
    /**
     * Creates a new resource.
     *
     * @return the created resource.
     * @throws RepositoryException if the operation failed.
     */
    Resource newResource() throws RepositoryException;

    /**
     * Creates a new collection.
     *
     * @return the created collection.
     * @throws RepositoryException if the operation failed.
     */
    Collection newCollection() throws RepositoryException;

    /**
     * Returns the resource at the given path.
     *
     * @param path Path of the resource. e.g. /project1/server/deployment.xml
     *
     * @return Resource instance
     * @throws RepositoryException is thrown if the resource is not in the registry
     */
    Resource get(String path) throws RepositoryException;

    /**
     * Returns the Collection at the given path, with the specified number of content starting from
     * specified location
     *
     * @param path     	the path of the collection.  MUST point to a collection!
     * @param start    	the initial index of the child to return.  If there are fewer children than
     *                 	the specified value, a RepositoryException will be thrown.
     * @param num 		the maximum number of results to return
     *
     * @return 			a Collection containing the specified results in the content
     * @throws 			RepositoryException if the resource is not found, or if the path does not reference a
     *                           Collection, or if the start index is greater than the number of
     *                           children.
     */
    Collection get(String path, int start, int num) throws RepositoryException;

    /**
     * Check whether a resource exists at the given path
     *
     * @param 			path Path of the resource to be checked
     *
     * @return 			true if a resource exists at the given path, false otherwise.
     * @throws 			RepositoryException if an error occurs
     */
    boolean resourceExists(String path) throws RepositoryException;

    /**
     * Adds or updates resources in the repository. If there is no resource at the given path,
     * resource is added. If a resource already exist at the given path, it will be replaced with
     * the new resource.
     *
     * @param suggestedPath 	the path which we'd like to use for the new resource.
     * @param resource      	Resource instance for the new resource
     *
     * @return 					the actual path that the server chose to use for the Resource
     * @throws 					RepositoryException is thrown depending on the implementation.
     */
    String put(String suggestedPath, Resource resource) throws RepositoryException;

    /**
     * Deletes the resource at the given path. If the path refers to a directory (collection), all child
     * resources of the directory will also be deleted.
     *
     * @param path 				Path of the resource to be deleted.
     * @throws 					RepositoryException is thrown depending on the implementation.
     */
    void delete(String path) throws RepositoryException;

    /**
     * Returns the meta data of the resource at a given path.
     *
     * @param path 				Path of the resource. e.g. /project1/server/deployment.xml
     *
     * @return 					Resource instance
     * @throws 					RepositoryException is thrown if the resource is not in the registry
     */
    Resource getMetaData(String path) throws RepositoryException;

    /**
     * Creates a resource by fetching the resource content from the given URL.
     *
     * @param suggestedPath path where we'd like to add the new resource. Although this path is
     *                      specified by the caller of the method, resource may not be actually
     *                      added at this path.
     * @param sourceURL     source URL from which to fetch the resource content
     * @param resource      a template Resource
     *
     * @return 				actual path to the new resource
     * @throws 				RepositoryException if we couldn't get or store the new resource
     */
    String importResource(String suggestedPath, String sourceURL, Resource resource) throws RepositoryException;

    /**
     * Rename a resource in the repository. This is equivalent to 1) delete the resource, then 2) add
     * the resource by the new name. The operation is atomic, so if it fails the old resource will
     * still be there.
     *
     * @param currentPath 	current path of the resource
     * @param newName     	the name of the new resource
     *
     * @return 				the actual path for the new resource
     * @throws 				RepositoryException if something went wrong
     */
    String rename(String currentPath, String newName) throws RepositoryException;

    /**
     * Move a resource in the repository. This is equivalent to 1) delete the resource, then 2) add
     * the resource to the new location. The operation is atomic, so if it fails the old resource
     * will still be there.
     *
     * @param currentPath 	current path of the resource
     * @param newPath     	where we'd like to move the resource
     *
     * @return 				the actual path for the new resource
     * @throws 				RepositoryException if something went wrong
     */
    String move(String currentPath, String newPath) throws RepositoryException;

    /**
     * Copy a resource in the registry.  The operation is atomic, so if the resource was a
     * collection, all children and the collection would be copied in a single-go, otherwise
     * nothing will be copied
     *
     * @param sourcePath 	current path of the resource
     * @param targetPath 	where we'd like to copy the resource
     *
     * @return 				the actual path for the new resource
     * @throws 				RepositoryException if something went wrong
     */
    String copy(String sourcePath, String targetPath) throws RepositoryException;

    /**
     * Creates a new version of the resource.
     *
     * @param path 			the resource path.
     *
     * @throws 				RepositoryException if something went wrong.
     */
    void createVersion(String path) throws RepositoryException;

    /**
     * Get a list of all versions of the resource located at the given path. Version paths are
     * returned in the form /projects/resource?v=12
     *
     * @param path 			path of a current version of a resource
     *
     * @return 				a String array containing the individual paths of versions
     * @throws 				RepositoryException if there is an error
     */
    String[] getVersions(String path) throws RepositoryException;

    /**
     * Reverts a resource to a given version.
     *
     * @param versionPath 	path of the version to be reverted. It is not necessary to provide the
     *                    	path of the resource as it can be derived from the version path.
     *
     * @throws 				RepositoryException if there is an error
     */
    void restoreVersion(String versionPath) throws RepositoryException;

    /**
     * Executes a custom query which lives at the given path in the Registry.
     *
     * @param path       	Path of the query to execute.
     * @param parameters 	a Map of query parameters (name -> value)
     *
     * @return 				a Collection containing any resource paths which match the query
     * @throws 				RepositoryException depends on the implementation.
     */
    Collection executeQuery(String path, Map<?, ?> parameters) throws RepositoryException;

    /**
     * Returns the logs of the activities occurred in the registry.
     *
     * @param resourcePath If given, only the logs related to the resource path will be returned. If
     *                     null, logs for all resources will be returned.
     * @param action       Only the logs pertaining to this action will be returned.  For acceptable
     *                     values, see LogEntry.
     * @param userName     If given, only the logs for activities done by the given user will be
     *                     returned. If null, logs for all users will be returned.
     * @param from         If given, logs for activities occurred after the given date will be
     *                     returned. If null, there will not be a bound for the starting date.
     * @param to           If given, logs for activities occurred before the given date will be
     *                     returned. If null, there will not be a bound for the ending date.
     * @param recentFirst  If true, returned activities will be most-recent first. If false,
     *                     returned activities will be oldest first.
     *
     * @return Array of LogEntry objects representing the logs
     * @throws RepositoryException if there is a problem
     * @see Activity Accepted values for action parameter
     */
    Activity[] getLogs(String resourcePath, int action, String userName, Date from, Date to, boolean recentFirst) throws RepositoryException;
    
    /**
     * Search the content of resources
     *
     * @param keywords 	   	keywords to look for
     *
     * @return 			 	the result set as a collection
     * @throws 				RepositoryException throws if the operation fail
     */
    Collection searchContent(String keywords) throws RepositoryException;

    /**
     * Create a symbolic link or mount a repository
     *
     * @param path   the mount path
     * @param target the point to be mounted
     *
     * @throws RepositoryException throws if the operation fail
     */
    void createLink(String path, String target) throws RepositoryException;

    /**
     * Create a symbolic link or mount a repository
     *
     * @param path          the mount path
     * @param target        the point to be mounted
     * @param subTargetPath sub path in the remote instance to be mounted
     *
     * @throws RepositoryException throws if the operation fail
     */
    void createLink(String path, String target, String subTargetPath) throws RepositoryException;

    /**
     * Remove a symbolic link or mount point created
     *
     * @param path the mount path
     *
     * @throws RepositoryException throws if the operation fail
     */
    void removeLink(String path) throws RepositoryException;

    /**
     * Check in the input element into database.
     *
     * @param path   path to check in
     * @param reader reader containing resource
     *
     * @throws RepositoryException throws if the operation fail
     */
    void restore(String path, Reader reader) throws RepositoryException;

    /**
     * Check out the given path as an xml.
     *
     * @param path   path to check out
     * @param writer writer to write the response
     *
     * @throws RepositoryException throws if the operation fail
     */
    void dump(String path, Writer writer) throws RepositoryException;
    
    /**
     * Removes a given version history of a resource.
     *
     * @param path the path of the resource 
     * @param snapshotId the version number of the resource
     *
     * @return return true if the operation finished successful, false otherwise.
     * @throws RepositoryException throws if the operation fails.
     */
     boolean removeVersionHistory(String path, long snapshotId) throws RepositoryException; 
     
     /**
      * returns the RepositoryService instance of the invoking registry
      * 
      * @return RepositoryService instance of the registry
      */
     RepositoryService getRepositoryService();
     
     // Following methods are deprecated and eventually move out of the code ---------------------------------------------------------
     
     /**
      * Gets the URL of the WS-Eventing Service.
      *
      * @param path the path to which the WS-Eventing Service URL is required
      *
      * @return the URL of the WS-Eventing Service
      * @throws RepositoryException throws if the operation fail
      */
     String getEventingServiceURL(String path) throws RepositoryException;

     /**
      * Sets the URL of the WS-Eventing Service.
      *
      * @param path               the path to which the WS-Eventing Service URL is associated
      * @param eventingServiceURL the URL of the WS-Eventing Service
      *
      * @throws RepositoryException throws if the operation fail
      */
     void setEventingServiceURL(String path, String eventingServiceURL) throws RepositoryException;
     
     /**
      * Method to obtain resource media types.
      *
      * @return the resource media types.
      */
     public String getResourceMediaTypes() throws RepositoryException;

     /**
      * Method to set resource media types.
      *
      * @param resourceMediaTypes the resource media types.
      */
     public void setResourceMediaTypes(String resourceMediaTypes) throws RepositoryException;
     
     /**
      * Method to obtain resource media types.
      *
      * @return the resource media types.
      */
     public String getCollectionMediaTypes() throws RepositoryException;

     /**
      * Method to set resource media types.
      *
      * @param collectionMediaTypes the collection media types.
      */
     public void setCollectionMediaTypes(String collectionMediaTypes) throws RepositoryException;
     
     /**
      * Method to obtain resource media types.
      *
      * @return the resource media types.
      */
     public String getCustomUIMediaTypes() throws RepositoryException;

     /**
      * Method to set resource media types.
      *
      * @param customUIMediaTypes the custom UI media types.
      */
     public void setCustomUIMediaTypes(String customUIMediaTypes) throws RepositoryException;
}
