/*
 * Copyright (c) 2007, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;

/**
 * The Collection Interface. Collection is specific type of {@link Resource} that can contain other
 * resources (including other collections). We call the resources contained in a collection as the
 * children of the collection and the collection is called the parent of its children.
 * <p/>
 * The path of the child = The path of the parent + RepositoryConstants.PATH_SEPARATOR + The resource
 * name of the child. The ROOT collection is a specific instance of the Collection interface which
 * doesn't have a parent.
 */
public interface Collection extends Resource {

    /**
     * Method to return the absolute paths of the children of the collection
     *
     * @return 		the array of absolute paths of the children
     * @throws 		RepositoryException if the operation fails.
     */
    String[] getChildPaths() throws RepositoryException;
  
    /**
     * Method to return the children of the collection as a Resource array
     *
     * @return 		the array of absolute paths of the children
     * @throws 		RepositoryException if the operation fails.
     */    
    Resource[] getChildren() throws RepositoryException;

    /**
     * Method to return the paths of the selected range of children.
     *
     * @param start the starting number of children.
     * @param num 	the number of entries to retrieve.
     *
     * @return		an array of paths of the selected range of children.
     * @throws  	RepositoryException if the operation fails.
     */
    String[] getChildPaths(int start, int num) throws RepositoryException;
    
    /**
     * Method to return the children of the collection within a range as a Resource array
     *
     * @param start the starting number of children.
     * @param num 	the number of entries to retrieve.
     *
     * @return 		the array of absolute paths of the children
     * @throws 		RepositoryException if the operation fails.
     */    
    Resource[] getChildren(int start, int num) throws RepositoryException;

    /**
     * Method to return the the number of children.
     *
     * @return 		the number of children.
     * @throws 		RepositoryException if the operation fails.
     */
    int getChildCount() throws RepositoryException;

//    /**
//     * Method to set the absolute paths of the children belonging to this collection. Absolute paths
//     * begin from the ROOT collection.
//     *
//     * @param paths the array of absolute paths of the children
//     * @throws 		RepositoryException if the operation fails.
//     */
//    void setChildren(String[] paths) throws RepositoryException;
}
