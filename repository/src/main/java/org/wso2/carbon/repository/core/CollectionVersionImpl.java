/*
 * Copyright (c) 2006, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.repository.core;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.core.dataobjects.ResourceDO;
import org.wso2.carbon.repository.core.exceptions.RepositoryConfigurationException;
import org.wso2.carbon.repository.core.utils.VersionRetriever;
import org.wso2.carbon.repository.spi.dao.ResourceVersionDAO;
import org.wso2.carbon.repository.spi.dataaccess.DataAccessManager;

/**
 * Implementation of CollectionImpl, Instance of this class is returned when requested for an early
 * version of some collection.
 */
public class CollectionVersionImpl extends CollectionImpl {

    private ResourceVersionDAO resourceVersionDAO;
    private VersionRetriever versionList;
    private int versionListIndex = 0;

    private static final Log log = LogFactory.getLog(CollectionVersionImpl.class);

    /**
     * Default Constructor. Creates an empty CollectionVersion instance.
     */
    public CollectionVersionImpl() {
        super();
        
        if (dataAccessManager != null) {
            this.resourceVersionDAO = dataAccessManager.getDAOManager().getResourceVersionDAO();
        }
    }

    /**
     * Constructor a CollectionVersionImpl for a provided path and a resourceDO.
     *
     * @param path       the path of the collection.
     * @param resourceDO the resourceDO instance.
     */
    public CollectionVersionImpl(String path, ResourceDO resourceDO) {
        super(path, resourceDO);
        
        if (dataAccessManager != null) {
            this.resourceVersionDAO = dataAccessManager.getDAOManager().getResourceVersionDAO();
        }
    }


    /**
     * A copy constructor used to create a shallow-copy of this collection.
     *
     * @param collection the collection of which the copy is created.
     */
    public CollectionVersionImpl(CollectionVersionImpl collection) {
        super(collection);
        
        this.versionList = collection.versionList;
        this.versionListIndex = collection.versionListIndex;
        this.resourceVersionDAO = collection.resourceVersionDAO;
    }

    /**
     * Setter method for DataAccessManager
     * 
     * @param  dataAccessManager DataAccessManager to be set
     **/
    public void setDataAccessManager(DataAccessManager dataAccessManager) {
        super.setDataAccessManager(dataAccessManager);
        
        if (dataAccessManager != null) {
            this.resourceVersionDAO = dataAccessManager.getDAOManager().getResourceVersionDAO();
        }
    }

    /**
     * Method to set the version retriever instance.
     *
     * @param versionList the version retriever.
     */
    public void setVersionList(VersionRetriever versionList) {
        this.versionList = versionList;
    }

    /**
     * Method to set the index of the current collection in the version list
     *
     * @param versionListIndex the index of the current collection in the list.
     */
    public void setVersionListIndex(int versionListIndex) {
        this.versionListIndex = versionListIndex;
    }

    /**
     * Method to return the absolute paths of the children of the collection. All the returning
     * paths will be in the same snapshot as the current collection.
     *
     * @return the array of absolute paths of the children
     * @throws RepositoryException if the operation fails.
     */
    public String[] getChildPaths() throws RepositoryException {
        if (getContent() instanceof String[]) {
            return fixPaths((String[])getContent());
        } else {
            return new String[0];
        }
    }

    /**
     * Method to return the paths of the selected range of children.
     *
     * @param start   the starting number of children.
     * @param pageLen the number of entries to retrieve.
     *
     * @return an array of paths of the selected range of children.
     * @throws RepositoryException if the operation fails.
     */
    public String[] getChildPaths(int start, int pageLen) throws RepositoryException {
        setContextInformation();
        
        try {
            if (content != null && content instanceof String[]) {
                String childNodes[] = (String[]) content;
                int limit = start + pageLen;
                
                if (start > childNodes.length) {
                    return new String[0];
                }
                
                if (limit > childNodes.length) {
                    limit = childNodes.length;
                }
                
                return fixPaths(Arrays.copyOfRange(childNodes, start, limit));
            }
            
            if (resourceVersionDAO == null) {
                String msg = "The data access object for versioned resources has not been created.";
                log.error(msg);
                throw new RepositoryConfigurationException(msg);
            }
            
            return fixPaths(resourceVersionDAO.getChildPaths(this.getResourceIDImpl(),
                        versionList, versionListIndex, start, pageLen, snapshotID, dataAccessManager));
        } finally {
            clearContextInformation();
        }
    }

    /**
     * Method to return the the number of children.
     *
     * @return the number of children.
     * @throws RepositoryException if the operation fails.
     */
    public int getChildCount() throws RepositoryException {
        if (content != null) {
            String[] childPaths = (String[]) content;
            return fixPaths(childPaths).length;
        }
        
        String[] childPaths = getChildPaths(0, -1);
        
        if (childPaths == null) {
            return 0;
        }
        
        return childPaths.length;
    }

    /**
     * Collection's content is a string array, which contains paths of its children. These paths are
     * loaded on demand to increase performance. It is recommended to use {@link #getChildren()}
     * method to get child paths of a collection, which provides pagination. Calling this method
     * will load all child paths.
     *
     * @return String array of child paths.
     * @throws RepositoryException On any error.
     */
    public Object getContent() throws RepositoryException {
        if (content == null) {
            String[] childPaths = getChildPaths(0, -1);
            setContentWithNoUpdate(childPaths);
        }
        
        return content;
    }
}
