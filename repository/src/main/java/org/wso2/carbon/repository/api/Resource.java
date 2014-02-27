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

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.wso2.carbon.repository.api.exceptions.RepositoryException;

/**
 * Interface to represent a resource that is stored in the repository. Each resource will have some
 * meta-data and content. Resources can also have properties, and versions.
 * <p/>
 */
public interface Resource {

    /**
     * Get the Resource ID, In the default implementation this returns the path.
     *
     * @return the resource id
     */
    String getId();

    /**
     * Get the user name of the resource author.
     *
     * @return the user name of the resource author.
     */
    String getAuthorUserName();

    /**
     * Method to get the created time.
     *
     * @return the created time
     */
    Date getCreatedTime();

    /**
     * Method to get the last modified date.
     *
     * @return the last modified date.
     */
    Date getLastModified();
    
    /**
     * Method to set the last modified date.
     *
     * @param lastModified the last modified date.
     */
    void setLastModified(Date lastModified) ;

    /**
     * Method to get the description.
     *
     * @return the description.
     */
    String getDescription();

    /**
     * Method to set the description.
     *
     * @param description the description.
     */
    void setDescription(String description);

    /**
     * Method to get the path. the unique identifier of the resources in the present state.
     *
     * @return the path.
     */
    String getPath();

    /**
     * If resource is version-ed, the associated version of the resource does not get modified by
     * any means. Therefore, the path of that version is the permanent path (permalink) of the
     * current state of the resource.
     *
     * @return Permanent path (permalink) of the resource.
     */
    String getPermanentPath();

    /**
     * Get media type of the resource.
     *
     * @return the media type.
     */
    String getMediaType();

    /**
     * Method to get the state.
     *
     * @return the state.
     */
    int getState();

    /**
     * Set media type.
     *
     * @param mediaType the media type.
     */
    void setMediaType(String mediaType);

    /**
     * Get the parent path.
     *
     * @return the parent path.
     */
    String getParentPath();

    /**
     * Get the property value for the given key, if there are multiple value for that key, it will
     * return the first value.
     *
     * @param key the property key.
     *
     * @return the property value.
     */
    String getProperty(String key);

    /**
     * Returns the list of values for the given property name. Note that these values are read-only.
     * Changes made to these values will not be persisted on putting the resource.
     *
     * @param key Key of the property.
     *
     * @return List of values of the given property key.
     */
    List<String> getPropertyValues(String key);

    /**
     * Returns all properties of the resource. Properties are stored as key (String) -> values
     * (List) pairs. It is not recommended to use this method to access properties. Instead, use
     * other property related Resource API methods provided.
     * <p/>
     * Note that these values are read-only. Changes made to these values will not be persisted on
     * putting the resource.
     *
     * @return All properties of the resource.
     */
    Properties getProperties();

    /**
     * Set a property with single value.
     *
     * @param key   the property key.
     * @param value the property value.
     */
    void setProperty(String key, String value);

    /**
     * Set a property with multiple value.
     *
     * @param key   the property key.
     * @param value the property values.
     */
    void setProperty(String key, List<String> value);

    /**
     * Add a property value for the provided key. If there are values associated with the key, this
     * will add append value. If not this will create a new property value for the key.
     *
     * @param key   the property key.
     * @param value the property value.
     */
    void addProperty(String key, String value);

    /**
     * Set properties.
     *
     * @param properties the properties.
     */
    void setProperties(Properties properties);

    /**
     * Edit property value.
     *
     * @param key      the key.
     * @param oldValue the old value.
     * @param newValue the new value.
     */
    void editPropertyValue(String key, String oldValue, String newValue);

    /**
     * Remove property.
     *
     * @param key the property key.
     */
    void removeProperty(String key);

    /**
     * Remove property value.
     *
     * @param key   the property key.
     * @param value the property value.
     */
    void removePropertyValue(String key, String value);

    /**
     * Method to get the content of the resource. If the resource is a collection this will return
     * an array of string that represent the paths of its children, otherwise it returns a byte
     * array or a string from the default resource implementation.
     *
     * @return the content.
     * @throws RepositoryException throws if the operation fail.
     */
    Object getContent() throws RepositoryException;

    /**
     * Set the content of the resource.
     *
     * @param content the resource.
     *
     * @throws RepositoryException throws if the operation fail.
     */
    void setContent(Object content) throws RepositoryException;

    /**
     * Method to get the last updated user name.
     *
     * @return the last updated user name.
     */
    String getLastUpdaterUserName();
    
    /**
     * Method to set the last updater user name.
     *
     * @param lastUpdaterUserName the last updater user name.
     */
    public void setLastUpdaterUserName(String lastUpdaterUserName) ;

    /**
     * Method to get the content stream.
     *
     * @return content as an input stream.
     * @throws RepositoryException throws if the operation fail.
     */
    InputStream getContentStream() throws RepositoryException;

    /**
     * Method to set the content stream.
     *
     * @param contentStream the content stream to set.
     *
     * @throws RepositoryException throws if the operation fail.
     */
    void setContentStream(InputStream contentStream) throws RepositoryException;

    /**
     * Method to discard the resource
     */
    void discard();

    /**
     * Check whether there are any changes that need to make a version
     *
     * @return true, if there are version-able changes, false otherwise.
     */
    boolean isVersionableChange();

    /**
     * Method to set whether there are any changes that need to make a version
     *
     * @param versionableChange whether version-able change is made or not.
     */
    void setVersionableChange(boolean versionableChange);

    /**
     * Method to set the UUID for a resource
     *
     * @param uuid the UUID to be set to the resource
     */
    void setUUID(String uuid);

    /**
     * Method to get the UUID of a resource
     *
     * @return UUID of the resource
     */
    String getUUID();
    
    /**
     * Method to get the path. the unique identifier of the resources in the present state.
     *
     * @param path the path.
     */
    void setPath(String path);
    
    /**
     * Method to get the version number.
     *
     * @return the version number.
     */
    long getVersionNumber();
    
    /**
     * Get the resource name.
     *
     * @return the resource name.
     */
    String getName() ;

    /**
     * Method to set the name.
     *
     * @param name the name.
     */
    void setName(String name) ;
    
    /**
     * Method to set the parent path.
     *
     * @param parentPath the parent path.
     */
    void setParentPath(String parentPath) ;
    
    /**
     * Method to set the author user name.
     *
     * @param authorUserName the author user name.
     */
    void setAuthorUserName(String authorUserName);
    
    /**
     * Method to set the created time.
     *
     * @param createdTime the created time.
     */
    void setCreatedTime(Date createdTime);

    /**
     * Method to get the matching snapshot id.
     *
     * @return the snapshot id.
     */
    long getMatchingSnapshotID() ;

    /**
     * Method to set the matching snapshot id.
     *
     * @param matchingSnapshotID the snapshot id.
     */
    void setMatchingSnapshotID(long matchingSnapshotID) ;
    
    /**
     * Method to set the resource id, you can set it to path
     *
     * @param id the path
     */
    void setId(String id) ;
}
