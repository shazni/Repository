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
package org.wso2.carbon.registry.core;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.exceptions.RepositoryServerException;
import org.wso2.carbon.registry.core.jdbc.dataobjects.ResourceDO;
import org.wso2.carbon.repository.Collection;
import org.wso2.carbon.repository.Resource;
import org.wso2.carbon.repository.exceptions.RepositoryException;

/**
 * The default registry implementation of the Collection interface.
 */
public class CollectionImpl extends ResourceImpl implements Collection {

    private static final Log log = LogFactory.getLog(CollectionImpl.class);

    /**
     * The number of children in this collection.
     */
    protected int childCount = -1;

    /**
     * The default constructor of the CollectionImpl, Create an empty collection with no children.
     */
    public CollectionImpl() {
        childCount = -1;
    }

    /**
     * Construct a collection with the provided children paths.
     *
     * @param paths the children paths.
     */
    public CollectionImpl(String[] paths) {
        try {
        	if(paths != null) {
        		setChildren(paths);
        	}
        } catch (RepositoryException e) {
            log.warn("Unable to set child paths to this collection.", e);
        }
    }

    /**
     * Construct a collection with the provided path and the resource data object.
     *
     * @param path       the path of the collection.
     * @param resourceDO the resource data object.
     */
    public CollectionImpl(String path, ResourceDO resourceDO) {
        super(path, resourceDO);
        childCount = -1;
    }

    /**
     * A copy constructor used to create a shallow-copy of this collection.
     *
     * @param collection the collection of which the copy is created.
     */
    public CollectionImpl(CollectionImpl collection) {
        super(collection);
        try {
            pullContentFromOriginal();
        } catch (RepositoryException ignored) {
            // we are not interested in handling any failures here.
        }
        if (this.content != null) {
            if (this.content instanceof String[]) {
                String[] paths = (String[]) this.content;
                int length = paths.length;
                String[] output = new String[length];
                System.arraycopy(paths, 0, output, 0, length);
                this.content = output;
            }  else if (this.content instanceof CommentImpl[]) {
                CommentImpl[] paths = (CommentImpl[]) this.content;
                int length = paths.length;
                CommentImpl[] output = new CommentImpl[length];
                System.arraycopy(paths, 0, output, 0, length);
                for (int i = 0; i < length; i++) {
                    output[i] = new CommentImpl(output[i]);
                }
                this.content = output;
            } else if (this.content instanceof Resource[]) {
                Resource[] paths = (Resource[]) this.content;
                int length = paths.length;
                Resource[] output = new Resource[length];
                System.arraycopy(paths, 0, output, 0, length);
                for (int i = 0; i < length; i++) {
                    if (output[i] instanceof CollectionVersionImpl) {
                        output[i] = new CollectionVersionImpl((CollectionVersionImpl) output[i]);
                    } else if (output[i] instanceof CollectionImpl) {
                        output[i] = new CollectionImpl((CollectionImpl) output[i]);
                    } else if (output[i] instanceof CommentImpl) {
                        output[i] = new CommentImpl((CommentImpl) output[i]);
                    } else if (output[i] instanceof ResourceImpl) {
                        output[i] = new ResourceImpl((ResourceImpl) output[i]);
                    }
                }
                this.content = output;
            }
        }
        this.childCount = collection.childCount;
    }

    /**
     * Implementation for the setContent. Here the content should always be a array of strings which
     * corresponding to the children paths.
     *
     * @param content array of strings which corresponding to the children paths.
     *
     * @throws RepositoryException if the operation fails.
     */
    public void setContent(Object content) throws RepositoryException {
        if (content == null) {
            return;
        }
        if (content instanceof String[]) {
            super.setContentWithNoUpdate(content);
            childCount = ((String[])content).length;
            return;
        } else if (content instanceof Resource[]) {
            super.setContentWithNoUpdate(content);
            childCount = ((Resource[])content).length;
            return;
        } else if (content instanceof String) {
            super.setContentWithNoUpdate(content);

            return;
        }
        throw new IllegalArgumentException("Invalid content for collection. " +
                "Content of type " + content.getClass().toString() +
                " is not allowed for collections.");
    }

    /**
     * Set the resource content without marking the collection as updated.Here the content should
     * always be a array of strings which corresponding to the children paths.
     *
     * @param content array of strings which corresponding to the children paths.
     *
     * @throws RepositoryException if the operation fails.
     */
    public void setContentWithNoUpdate(Object content) throws RepositoryException {
        if (content == null) {
            return;
        }
        if (content instanceof String[] ||
                content instanceof Resource[] ||
                content instanceof String) {
            super.setContentWithNoUpdate(content);
            return;
        }
        throw new IllegalArgumentException("Invalid content for collection. " +
                "Content of type " + content.getClass().toString() +
                " is not allowed for collections.");
    }

    /**
     * Method to set the absolute paths of the children belonging to this collection. Absolute paths
     * begin from the ROOT collection.
     *
     * @param paths the array of absolute paths of the children
     *
     * @throws RepositoryException if the operation fails.
     */
    public void setChildren(String[] paths) throws RepositoryException {
        String[] temp = fixPaths(paths);
        content = temp;
        childCount = temp.length;
    }

    /**
     * Method to return the children.
     *
     * @return an array of children paths.
     * @throws RepositoryException if the operation fails.
     */
    public String[] getChildren() throws RepositoryException {
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
    public String[] getChildren(int start, int pageLen) throws RepositoryException {
        setSessionInformation();
        try {
            pullContentFromOriginal();
            if (content == null) {
                if (resourceDAO == null) {
                    String msg = "The data access object for resources has not been created.";
                    log.error(msg);
                    throw new RepositoryServerException(msg);
                }
                return fixPaths(resourceDAO.getChildren(this, start, pageLen, dataAccessManager));

            } else {

                if (content instanceof String[]) {

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
            }
            return new String[0];
        } finally {
            clearSessionInformation();
        }
    }

    /**
     * Method to return the the number of children.
     *
     * @return the number of children.
     * @throws RepositoryException if the operation fails.
     */
    public int getChildCount() throws RepositoryException {

        try {
            setSessionInformation();
            pullContentFromOriginal();
            if (childCount != -1) {
                return childCount;

            } else if (content != null && content instanceof String[]) {

                String[] childPaths = (String[]) content;
                return fixPaths(childPaths).length;

            }
            if (resourceDAO == null) {
                String msg = "The data access object for resources has not been created.";
                log.error(msg);
                throw new RepositoryServerException(msg);
            }
            return resourceDAO.getChildCount(this, dataAccessManager);
        } finally {
            clearSessionInformation();
        }
    }

    /**
     * Method to set the child count.
     *
     * @param count the child count.
     */
    public void setChildCount(int count) {
        childCount = count;
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
        setSessionInformation();
        try {
            pullContentFromOriginal();
            if (content == null) {
                if (resourceDAO == null) {
                    String msg = "The data access object for resources has not been created.";
                    log.error(msg);
                    throw new RepositoryServerException(msg);
                }
                resourceDAO.fillChildren(this, dataAccessManager);
            }
            return content;
        } finally {
            clearSessionInformation();
        }
    }

    /**
     * Method to return a shallow copy of a collection.
     *
     * @return the shallow copy of the collection.
     * @throws RepositoryException if the operation fails.
     */
    public ResourceImpl getShallowCopy() throws RepositoryException {
        CollectionImpl newCollection = new CollectionImpl();
        fillCollectionCopy(newCollection);
        return newCollection;
    }

    /**
     * Copy all the values of the current collection attribute to the passed collection.
     *
     * @param collection the collection to get all the current collection attribute copied.
     *
     * @throws RepositoryException if the operation fails.
     */
    public void fillCollectionCopy(CollectionImpl collection) throws RepositoryException {
        super.fillResourceCopy(collection);
        collection.setChildCount(this.childCount);
    }

    /**
     * Method to fix duplicated entries in a collection's child paths.
     * @param paths the collection's child paths.
     * @return the distinct set of children.
     */
    protected String[] fixPaths(String[] paths) {
        Set<String> temp = new LinkedHashSet<String>();
        for (String path : paths) {
            temp.add(path);
        }
        return temp.toArray(new String[temp.size()]);
    }
}
