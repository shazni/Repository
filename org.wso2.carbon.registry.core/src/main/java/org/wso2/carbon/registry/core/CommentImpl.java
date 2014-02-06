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

package org.wso2.carbon.registry.core;

import java.util.Date;

import org.wso2.carbon.repository.exceptions.RepositoryException;

/**
 * Represents comments and its meta data. Note that only the Comment.text field needs to be filled
 * when adding new comments. All other attributes are ignored and they are filled with appropriate
 * values for the current context. Therefore, when constructing an instance of this class outside
 * the Registry impl, it is recommended to use new Comment("my comment text") constructor.
 */
@Deprecated
public class CommentImpl extends ResourceImpl implements org.wso2.carbon.repository.Comment {

    /**
     * Path of the comment. Each comment has a path in the form /projects/esb/config.xml;comments:12
     */
    private transient String commentPath;

    /**
     * Comment text. This may contain any string including HTML segments.
     */
    private transient String text;

    /**
     * Username of the user who added this comment.
     */
    private transient String user;

    /**
     * Path of the resource on which this comment is made.
     */
    private transient String resourcePath;

    /**
     * the id unique to the comment.
     */
    private transient long commentID;

    /**
     * Default constructor to create an empty comment.
     */
    public CommentImpl() {
    }

    /**
     * Construct a comment with a text.
     *
     * @param commentText the comment text
     */
    public CommentImpl(String commentText) {
        this.text = commentText;
        setCreatedTime(new Date());
    }

    /**
     * A copy constructor used to create a shallow-copy of this comment.
     *
     * @param comment the comment of which the copy is created.
     */
    public CommentImpl(CommentImpl comment) {
        super(comment);
        this.commentPath = comment.commentPath;
        this.text = comment.text;
        this.user = comment.user;
        this.resourcePath = comment.resourcePath;
        this.commentID = comment.commentID;
    }

    /**
     * Get the comment text.
     *
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * Set the comment text.
     *
     * @param text the text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Get the commenting user.
     *
     * @return the comment user
     */
    public String getUser() {
        return user;
    }

    /**
     * Set the commenting user.
     *
     * @param user the commenting user
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Get the comment time.
     *
     * @return the time the comment was made.
     * @deprecated please use {@link #getCreatedTime} instead
     */
    @Deprecated
    public Date getTime() {
        return getCreatedTime();
    }

    /**
     * Set the comment time
     *
     * @param time the time
     *
     * @deprecated please use {@link #setCreatedTime(java.util.Date)} instead
     */
    @Deprecated
    public void setTime(Date time) {
        setCreatedTime(time);
    }


    /**
     * Method to set the created time.
     * @param createdTime the created time.
     */
    public void setCreatedTime(Date createdTime) {
        super.setCreatedTime(createdTime);
        setLastModified(createdTime);
    }

    /**
     * Get the path of the resource of the comment.
     *
     * @return the resource path.
     */
    public String getResourcePath() {
        return resourcePath;
    }

    /**
     * Set the path of the resource of the comment.
     *
     * @param resourcePath the resource path.
     */
    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    /**
     * Get the comment text.
     *
     * @return the comment text
     * @throws RepositoryException throws if the operation fail.
     */
    public Object getContent() throws RepositoryException {
        return getText();
    }

    /**
     * Get the description.
     *
     * @return the description.
     */
    public String getDescription() {
        return getText();
    }

    /**
     * Method to get the comment path.
     *
     * @return the comment path
     */
    public String getCommentPath() {
        return commentPath;
    }

    /**
     * Method to set the comment path.
     *
     * @param commentPath the comment path
     */
    public void setCommentPath(String commentPath) {
        this.commentPath = commentPath;
    }

    /**
     * Method to get the author user name.
     *
     * @return the author user name
     */
    public String getAuthorUserName() {
        return user;
    }

    /**
     * Method to get the comment id.
     *
     * @return the comment id.
     */
    public long getCommentID() {
        return commentID;
    }

    /**
     * Method to set the comment id.
     *
     * @param commentID the comment id.
     */
    public void setCommentID(long commentID) {
        this.commentID = commentID;
    }
}
