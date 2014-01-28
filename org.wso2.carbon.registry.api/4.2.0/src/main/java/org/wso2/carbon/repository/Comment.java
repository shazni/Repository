/*                                                                             
 * Copyright 2004,2005 The Apache Software Foundation.                         
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

import java.util.Date;

/**
 * @deprecated
 * 
 * This will removed from carbon kernel as this is a social aspect and not part of a pure 
 * repository
 * 
 * This interface represents a comment that can be added to a resource. A comment is treated a
 * resource itself and it extends the {@link Resource} interface.
 * <p/>
 * This interface can be used to add, view or modify existing comments on the repository.
 */
@Deprecated
public interface Comment extends Resource {

    /**
     * Get the comment text.
     *
     * @return the text
     */
    String getText();

    /**
     * Set the comment text.
     *
     * @param text the text
     */
    void setText(String text);

    /**
     * Get the commenting user.
     *
     * @return the comment user
     */
    String getUser();

    /**
     * Set the commenting user.
     *
     * @param user the commenting user
     */
    void setUser(String user);

    /**
     * Method to get the comment path.
     *
     * @return the comment path
     */
    String getCommentPath();

    /**
     * Method to set the comment path.
     *
     * @param commentPath the comment path
     */
    void setCommentPath(String commentPath);


    /**
     * Method to get the comment id.
     *
     * @return the comment id.
     */
    long getCommentID();

    /**
     * Method to set the comment id.
     *
     * @param commentID the comment id.
     */
    void setCommentID(long commentID);
    
    /**
     * Get the path of the resource of the comment.
     *
     * @return the resource path.
     */
    public String getResourcePath() ;

    /**
     * Set the path of the resource of the comment.
     *
     * @param resourcePath the resource path.
     */
    public void setResourcePath(String resourcePath) ;
    
    /**
     * Returns the time of the comment
     * 
     * @return time of the Comment
     */
    public Date getTime() ;

    /**
     * Set the comment time
     *
     * @param time the time
     */
    public void setTime(Date time) ;
}
