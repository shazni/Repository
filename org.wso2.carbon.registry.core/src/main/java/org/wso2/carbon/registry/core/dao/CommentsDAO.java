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

import org.wso2.carbon.registry.core.CommentImpl;
import org.wso2.carbon.registry.core.ResourceIDImpl;
import org.wso2.carbon.registry.core.ResourceImpl;
import org.wso2.carbon.repository.exceptions.RepositoryException;
import org.wso2.carbon.registry.core.jdbc.dataobjects.CommentDO;

/**
 * Data Access Object for Comments
 */
public interface CommentsDAO {

    /**
     * Method to persist a comment.
     *
     * @param resource the resource
     * @param userID   the id of the user who added the comment.
     * @param comment  the comment to be persisted.
     *
     * @return the comment id of the newly added comment.
     * @throws RepositoryException if some error occurs while adding a comment
     */
    int addComment(ResourceImpl resource, String userID, CommentImpl comment)
            throws RepositoryException;

    /**
     * Method to persist comments.
     *
     * @param resource   the resource
     * @param commentDOs the comments to be persisted.
     *
     * @throws RepositoryException if some error occurs while adding comments
     */
    void addComments(ResourceImpl resource, CommentDO[] commentDOs)
            throws RepositoryException;

    /**
     * Method to copy comments.
     *
     * @param sourceResource the source resource.
     * @param targetResource the target resource.
     *
     * @throws RepositoryException if some error occurs while copying comments
     */
    void copyComments(ResourceImpl sourceResource, ResourceImpl targetResource)
            throws RepositoryException;

    /**
     * Method to update a comment.
     *
     * @param commentId the comment id.
     * @param text      the comment text.
     *
     * @throws RepositoryException if some error occurs while updating the comment.
     */
    void updateComment(long commentId, String text) throws RepositoryException;

    /**
     * Method to delete a comment.
     *
     * @param commentId the comment id.
     *
     * @throws RepositoryException if some error occurs while deleting the comment.
     */
    void deleteComment(long commentId) throws RepositoryException;

    /**
     * Method to remove all comments added to a resource.
     *
     * @param resource the resource.
     *
     * @throws RepositoryException if some error occurs while removing comments.
     */
    void removeComments(ResourceImpl resource) throws RepositoryException;

    /**
     * Method to get a comment added to a given resource.
     *
     * @param resourcePath the resource's path.
     * @param commentID    the identifier of the comment.
     *
     * @return the comment.
     * @throws RepositoryException if an error occurs while getting the comment.
     */
    CommentImpl getComment(long commentID, String resourcePath) throws RepositoryException;

    /**
     * Method to get comments added to a given resource.
     *
     * @param resource the resource.
     *
     * @return an array of comments.
     * @throws RepositoryException if an error occurs while getting comments.
     */
    CommentImpl[] getComments(ResourceImpl resource) throws RepositoryException;

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
    ResourceImpl getResourceWithMinimumData(String path) throws RepositoryException;

    /**
     * Method to move comments.
     *
     * @param source the source resource.
     * @param target the target resource.
     *
     * @throws RepositoryException if some error occurs while moving comments
     */
    void moveComments(ResourceIDImpl source, ResourceIDImpl target)
            throws RepositoryException;

    /**
     * Method to move comment paths. This function will move the paths from one path id to another
     * regardless of the resource name.
     *
     * @param source the source resource.
     * @param target the target resource.
     *
     * @throws RepositoryException if some error occurs while moving comment paths
     */
    void moveCommentPaths(ResourceIDImpl source, ResourceIDImpl target)
            throws RepositoryException;
    
    /**
     * Removes all comments added to the given resource for a given version. 
     * This applies only to versioned resources. 
     *
     * @param regVersion the version
     *
     * @throws RepositoryException if an error occurred while removing tags.
     */
    void removeVersionComments(long regVersion)
            throws RepositoryException;
}
