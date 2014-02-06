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

package org.wso2.carbon.repository;

import java.io.Reader;
import java.io.Writer;
import java.util.Date;
import java.util.Map;

import org.wso2.carbon.repository.exceptions.RepositoryException;

/**
 * This is the "Full" Registry interface.  It contains not only the get/put behavior from
 * {@link CoreRepository}, but also APIs which control tags, comments, ratings and versions.
 */
public interface Registry extends TransactionManager {
	
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
     * Returns the Collection at the given path, with the content paginated according to the
     * arguments.
     *
     * @param path     the path of the collection.  MUST point to a collection!
     * @param start    the initial index of the child to return.  If there are fewer children than
     *                 the specified value, a RegistryException will be thrown.
     * @param pageSize the maximum number of results to return
     *
     * @return a Collection containing the specified results in the content
     * @throws RepositoryException if the resource is not found, or if the path does not reference a
     *                           Collection, or if the start index is greater than the number of
     *                           children.
     */
    Collection get(String path, int start, int pageSize) throws RepositoryException;

    /**
     * Check whether a resource exists at the given path
     *
     * @param path Path of the resource to be checked
     *
     * @return true if a resource exists at the given path, false otherwise.
     * @throws RepositoryException if an error occurs
     */
    boolean resourceExists(String path) throws RepositoryException;

    /**
     * Adds or updates resources in the repository. If there is no resource at the given path,
     * resource is added. If a resource already exist at the given path, it will be replaced with
     * the new resource.
     *
     * @param suggestedPath the path which we'd like to use for the new resource.
     * @param resource      Resource instance for the new resource
     *
     * @return the actual path that the server chose to use for the Resource
     * @throws RepositoryException is thrown depending on the implementation.
     */
    String put(String suggestedPath, Resource resource) throws RepositoryException;

    /**
     * Deletes the resource at the given path. If the path refers to a directory, all child
     * resources of the directory will also be deleted.
     *
     * @param path Path of the resource to be deleted.
     *
     * @throws RepositoryException is thrown depending on the implementation.
     */
    void delete(String path) throws RepositoryException;

    /**
     * Returns the meta data of the resource at a given path.
     *
     * @param path Path of the resource. e.g. /project1/server/deployment.xml
     *
     * @return Resource instance
     * @throws RepositoryException
     *          is thrown if the resource is not in the registry
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
     * @return actual path to the new resource
     * @throws RepositoryException if we couldn't get or store the new resource
     */
    String importResource(String suggestedPath, String sourceURL, Resource resource) throws RepositoryException;

    /**
     * Rename a resource in the repository.  This is equivalent to 1) delete the resource, then 2) add
     * the resource by the new name.  The operation is atomic, so if it fails the old resource will
     * still be there.
     *
     * @param currentPath current path of the resource
     * @param newName     the name of the new resource
     *
     * @return the actual path for the new resource
     * @throws RepositoryException if something went wrong
     */
    String rename(String currentPath, String newName) throws RepositoryException;

    /**
     * Move a resource in the registry.  This is equivalent to 1) delete the resource, then 2) add
     * the resource to the new location.  The operation is atomic, so if it fails the old resource
     * will still be there.
     *
     * @param currentPath current path of the resource
     * @param newPath     where we'd like to move the resource
     *
     * @return the actual path for the new resource
     * @throws RepositoryException if something went wrong
     */
    String move(String currentPath, String newPath) throws RepositoryException;

    /**
     * Copy a resource in the registry.  The operation is atomic, so if the resource was a
     * collection, all children and the collection would be copied in a single-go, otherwise
     * nothing will be copied
     *
     * @param sourcePath current path of the resource
     * @param targetPath where we'd like to copy the resource
     *
     * @return the actual path for the new resource
     * @throws RepositoryException if something went wrong
     */
    String copy(String sourcePath, String targetPath) throws RepositoryException;

    /**
     * Creates a new version of the resource.
     *
     * @param path the resource path.
     *
     * @throws RepositoryException if something went wrong.
     */
    void createVersion(String path) throws RepositoryException;

    /**
     * Get a list of all versions of the resource located at the given path. Version paths are
     * returned in the form /projects/resource?v=12
     *
     * @param path path of a current version of a resource
     *
     * @return a String array containing the individual paths of versions
     * @throws RepositoryException if there is an error
     */
    String[] getVersions(String path) throws RepositoryException;

    /**
     * Reverts a resource to a given version.
     *
     * @param versionPath path of the version to be reverted. It is not necessary to provide the
     *                    path of the resource as it can be derived from the version path.
     *
     * @throws RepositoryException if there is an error
     */
    void restoreVersion(String versionPath) throws RepositoryException;

    /**
     * Executes a custom query which lives at the given path in the Registry.
     *
     * @param path       Path of the query to execute.
     * @param parameters a Map of query parameters (name -> value)
     *
     * @return a Collection containing any resource paths which match the query
     * @throws RepositoryException depends on the implementation.
     */
    Collection executeQuery(String path, Map parameters) throws RepositoryException;

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
     * @param keywords keywords to look for
     *
     * @return the result set as a collection
     * @throws RepositoryException throws if the operation fail
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
     * Check in the input axiom element into database.
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
      * returns the RegistryService instance of the invoking registry
      * 
      * @return RegistryService instance of the registry
      */
     RegistryService getRegistryService();
     
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
      * @param resourceMediaTypes the resource media types.
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
      * @param resourceMediaTypes the resource media types.
      */
     public void setCustomUIMediaTypes(String customUIMediaTypes) throws RepositoryException;
     
     /**
      * Get a list of the available Aspects for this Registry
      *
      * @return a String array containing available Aspect names
      */
     String[] getAvailableAspects();

     /**
      * Associate an Aspect with a resource.
      *
      * @param resourcePath Path of the resource
      * @param aspect       Name of the aspect
      *
      * @throws RepositoryException If some thing went wrong while doing associating the phase
      */
     void associateAspect(String resourcePath, String aspect) throws RepositoryException;

     /**
      * This invokes an action on a specified Aspect, which must be associated with the Resource at
      * the given path.
      *
      * @param resourcePath Path of the resource
      * @param aspectName   Name of the aspect
      * @param action       Which action was selected - actions are aspect-specific
      *
      * @throws RepositoryException if the Aspect isn't associated with the Resource, or the action
      *                           isn't valid, or an Aspect-specific problem occurs.
      */
     void invokeAspect(String resourcePath, String aspectName, String action) throws RepositoryException;
     
     /**
      * This invokes an action on a specified Aspect, which must be associated with the Resource at
      * the given path.
      *
      * @param resourcePath Path of the resource
      * @param aspectName   Name of the aspect
      * @param action       Which action was selected - actions are aspect-specific
      * @param parameters   Parameters to be used for the operation
      *
      * @throws RepositoryException if the Aspect isn't associated with the Resource, or the action
      *                           isn't valid, or an Aspect-specific problem occurs.
      */
      void invokeAspect(String resourcePath, String aspectName, String action, Map<String, String> parameters) throws RepositoryException;

     /**
      * Obtain a list of the available actions on a given resource for a given Aspect.  The Aspect
      * must be associated with the Resource (@see associateAspect).  The actions are determined by
      * asking the Aspect itself, so they may change depending on the state of the Resource, the user
      * who's asking, etc)
      *
      * @param resourcePath path of the Resource
      * @param aspectName   name of the Aspect to query for available actions
      *
      * @return a String[] of action names
      * @throws RepositoryException if the Aspect isn't associated or an Aspect-specific problem
      *                           occurs
      */
     String[] getAspectActions(String resourcePath, String aspectName) throws RepositoryException;
     
     /**
      * @param name name of the aspect
      * 
      * @param aspect instance of the Aspect
      * 
      * @return true if the operation finished successful, false otherwise.
      * @throws RepositoryException throws if the operation fail
      */
     public boolean addAspect(String name, Aspect aspect) throws RepositoryException;
     
     /**
      * Remove the given aspect from registry context.
      *
      * @param aspect the name of the aspect to be removed
      *
      * @return return true if the operation finished successful, false otherwise.
      * @throws RegistryException throws if the operation fail
      */
     public boolean removeAspect(String name) throws RepositoryException;
     
     /**
      * Adds an association stating that the resource at "associationPath" associate on the resource
      * at "associationPath". Paths may be the resource paths of the current versions or paths of the
      * old versions. If a path refers to the current version, it should contain the path in the form
      * /c1/c2/r1. If it refers to an old version, it should be in the form /c1/c2/r1?v=2.
      *
      * @param sourcePath      Path of the source resource
      * @param targetPath      Path of the target resource
      * @param associationType Type of the association
      *
      * @throws RepositoryException Depends on the implementation
      */
     void addAssociation(String sourcePath, String targetPath, String associationType) throws RepositoryException;

     /**
      * To remove an association for a given resource
      *
      * @param sourcePath      Path of the source resource
      * @param targetPath      Path of the target resource
      * @param associationType Type of the association
      *
      * @throws RepositoryException Depends on the implementation
      */
     void removeAssociation(String sourcePath, String targetPath, String associationType) throws RepositoryException;

     /**
      * Get all associations of the given resource. This is a chain of association starting from the
      * given resource both upwards (source to destination) and downwards (destination to source). T
      * his is useful to analyze how changes to other resources would affect the given resource.
      *
      * @param resourcePath Path of the resource to analyze associations.
      *
      * @return List of Association
      * @throws RepositoryException If something went wrong
      */
     Association[] getAllAssociations(String resourcePath) throws RepositoryException;

     /**
      * Get all associations of the given resource for a give association type. This is a chain of
      * association starting from the given resource both upwards (source to destination) and
      * downwards (destination to source). T his is useful to analyze how changes to other resources
      * would affect the given resource.
      *
      * @param resourcePath    Path of the resource to analyze associations.
      * @param associationType Type of the association , that could be dependency, or some other
      *                        type.
      *
      * @return List of Association
      * @throws RepositoryException If something went wrong
      */
     Association[] getAssociations(String resourcePath, String associationType) throws RepositoryException;

     /**
      * Applies the given tag to the resource in the given path. If the given tag is not defined in
      * the repository, it will be defined.
      *
      * @param resourcePath Path of the resource to be tagged.
      * @param tag          Tag. Any string can be used for the tag.
      *
      * @throws RepositoryException is thrown if a resource does not exist in the given path.
      */
     void applyTag(String resourcePath, String tag) throws RepositoryException;

     /**
      * Returns the paths of all Resources that are tagged with the given tag.
      *
      * @param tag the tag to search for
      *
      * @return an array of TaggedResourcePaths
      * @throws RepositoryException if an error occurs
      */
     TaggedResourcePath[] getResourcePathsWithTag(String tag) throws RepositoryException;

     /**
      * Returns all tags used for tagging the given resource.
      *
      * @param resourcePath Path of the resource
      *
      * @return Tags tag names
      * @throws RepositoryException is thrown if a resource does not exist in the given path.
      */
     Tag[] getTags(String resourcePath) throws RepositoryException;

     /**
      * Removes a tag on a resource. If the resource at the path is owned by the current user, all
      * tagging done using the given tag will be removed. If the resource is not owned by the
      * current user, only the tagging done by the current user will be removed.
      *
      * @param path Resource path tagged with the given tag.
      * @param tag  Name of the tag to be removed.
      *
      * @throws RepositoryException if there's a problem
      */
     void removeTag(String path, String tag) throws RepositoryException;

     /**
      * Adds a comment to a resource.
      *
      * @param resourcePath Path of the resource to add the comment.
      * @param comment      Comment instance for the new comment.
      *
      * @return the path of the new comment.
      * @throws RepositoryException is thrown if a resource does not exist in the given path.
      */
     String addComment(String resourcePath, Comment comment) throws RepositoryException;

     /**
      * Change the text of an existing comment.
      *
      * @param commentPath path to comment resource ("..foo/r1;comment:1")
      * @param text        new text for the comment.
      *
      * @throws RepositoryException Registry implementations may handle exceptions and throw
      *                           RegistryException if the exception has to be propagated to the
      *                           client.
      */
     void editComment(String commentPath, String text) throws RepositoryException;

     /**
      * Delete an existing comment.
      *
      * @param commentPath path to comment resource ("..foo/r1;comment:1")
      *
      * @throws RepositoryException Registry implementations may handle exceptions and throw
      *                           RegistryException if the exception has to be propagated to the
      *                           client.
      */
     void removeComment(String commentPath) throws RepositoryException;

     /**
      * Get all comments for the given resource.
      *
      * @param resourcePath path of the resource.
      *
      * @return an array of Comment objects.
      * @throws RepositoryException Registry implementations may handle exceptions and throw
      *                           RegistryException if the exception has to be propagated to the
      *                           client.
      */
     Comment[] getComments(String resourcePath) throws RepositoryException;

     /**
      * Rate the given resource.
      *
      * @param resourcePath Path of the resource.
      * @param rating       Rating value between 1 and 5.
      *
      * @throws RepositoryException Registry implementations may handle exceptions and throw
      *                           RegistryException if the exception has to be propagated to the
      *                           client.
      */
     void rateResource(String resourcePath, int rating) throws RepositoryException;

     /**
      * Returns the average rating for the given resource. This is the average of all ratings done by
      * all users for the given resource.
      *
      * @param resourcePath Path of the resource.
      *
      * @return Average rating between 1 and 5.
      * @throws RepositoryException if an error occurs
      */
     float getAverageRating(String resourcePath) throws RepositoryException;

     /**
      * Returns the rating given to the specified resource by the given user
      *
      * @param path     Path of the resource
      * @param userName username of the user
      *
      * @return rating given by the given user
      * @throws RepositoryException if there is a problem
      */
     int getRating(String path, String userName) throws RepositoryException;
}