/*
 *  Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.repository.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.repository.api.Activity;
import org.wso2.carbon.repository.api.Collection;
import org.wso2.carbon.repository.api.RepositoryConstants;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;

/**
 * Class providing the chroot related functionality. The instance of this is used by the
 * UserRegistry to wrap all the operation for inputs and outputs with chroot
 */
public class ChrootWrapper {

    private static final Log log = LogFactory.getLog(EmbeddedRepository.class);

    /**
     * The base prefix.
     */
    protected String basePrefix = null;

    /**
     * Construct a ChrootWrapper with a base prefix.
     *
     * @param basePrefix the base prefix.
     */
    public ChrootWrapper(String basePrefix) {
        if (basePrefix != null) {
            if (basePrefix.equals(RepositoryConstants.ROOT_PATH)) {
                basePrefix = null;
            }
            
            this.basePrefix = basePrefix;
        }
    }

    /**
     * Method to return the base prefix.
     *
     * @return the base prefix.
     */
    public String getBasePrefix() {
        return basePrefix;
    }

    /**
     * Get an absolute path for the given path argument, taking into account both initial
     * double-slashes (indicating an absolute path) and any basePrefix that has been established.
     * <p/>
     * This is the converse of getOutPath().
     *
     * @param path a relative path
     *
     * @return an absolute path into the "real" registry.
     */
    public String getInPath(String path) {
        // No worries if there's no base prefix
        if (basePrefix == null || basePrefix.length() == 0 || path == null || path.startsWith(basePrefix)) {
            return path;
        }
        
        if (log.isTraceEnabled()) {
            log.trace("Deriving the absolute path, chroot-base: " + basePrefix + ", path: " + path + ".");
        }

        if (path.startsWith("//")) {
            // This is an absolute path, so just strip the doubled slash
            return path.substring(1);
        }
        
        if (!path.startsWith(RepositoryConstants.ROOT_PATH)) {
            path = RepositoryConstants.ROOT_PATH + path;
        }
        
        if (path.equals(RepositoryConstants.ROOT_PATH)) {
            return basePrefix;
        }

        // Relative path, so prepend basePrefix appropriately
        return basePrefix + path;
    }

    /**
     * Take an absolute path in the "real" registry and convert it to a relative path suitable for
     * this particular RemoteRegistry (which may be rooted at a particular place).
     * <p/>
     * This is the converse of getInPath().
     *
     * @param absolutePath a full path from the root of the registry, starting with "/"
     *
     * @return a relative path which generates the correct absolute path
     */
    public String getOutPath(String absolutePath) {
        // No worries if there's no base prefix
        if (basePrefix == null || basePrefix.length() == 0 || absolutePath == null) {
            return absolutePath;
        }
        
        if (log.isTraceEnabled()) {
            log.trace("Deriving the relative path, chroot-base: " + basePrefix + ", path: " + absolutePath + ".");
        }

        if (absolutePath.startsWith(basePrefix + RepositoryConstants.PATH_SEPARATOR)) {
            return absolutePath.substring(basePrefix.length());
        } else if (absolutePath.equals(basePrefix)) {
            return RepositoryConstants.ROOT_PATH;
        } else if (absolutePath.startsWith(basePrefix + ";version")) {
            return "/" + absolutePath.substring(basePrefix.length());
        }

        // Somewhere else, so make sure there are dual slashes at the beginning
        return "/" + absolutePath;
    }

    /**
     * returns a set of relative path for the provided absolute paths.
     *
     * @param absolutePaths the array of absolute paths.
     *
     * @return the array of relative paths
     */
    public String[] getOutPaths(String[] absolutePaths) {
        if (basePrefix == null || basePrefix.length() == 0 || absolutePaths == null || absolutePaths.length == 0) {
            return absolutePaths;
        }
        
        for (int i = 0; i < absolutePaths.length; i++) {
            String absolutePath = absolutePaths[i];
            absolutePaths[i] = getOutPath(absolutePath);
        }
        
        return absolutePaths;
    }

    /**
     * The resource needed to be modified in case of out resource
     *
     * @param resource the resource that should prepared with chroot to return out.
     *
     * @return the resource after preparing with chroot processing
     * @throws RepositoryException throws if the operation failed.
     */
    public Resource getOutResource(Resource resource) throws RepositoryException {
        // No worries if there's no base prefix
        if (basePrefix == null || basePrefix.length() == 0) {
            return resource;
        }
        
        String absolutePath = resource.getPath();
        
        if (log.isTraceEnabled()) {
            log.trace("Deriving the relative resource, chroot-base: " + basePrefix + ", resource-absolute-path: " + absolutePath + ".");
        }
        
        if (resource instanceof CollectionImpl) {
            fixCollection((CollectionImpl) resource);
        }
        
        // fixing the path attribute of the resource
        if (absolutePath != null) {
            String relativePath = getOutPath(absolutePath);
            ((ResourceImpl) resource).setPath(relativePath);
        }
        
        String permanentPath = resource.getPermanentPath();
        
        if (permanentPath != null) {
            ((ResourceImpl) resource).setMatchingSnapshotID(((ResourceImpl) resource).getMatchingSnapshotID());
        }
        
        fixMountPoints(resource);
        
        return resource;
    }

    /**
     * When returning collection (with pagination) it need to unset the collection content.
     *
     * @param collection the collection to be prepared with chroot to return out.
     *
     * @return the resource after preparing with chroot processing
     * @throws RepositoryException throws if the operation failed.
     */
    public Collection getOutCollection(Collection collection) throws RepositoryException {
        if (basePrefix == null || basePrefix.length() == 0) {
            return collection;
        }
        
        String absolutePath = collection.getPath();
        
        if (log.isTraceEnabled()) {
            log.trace("Deriving the relative resource, chroot-base: " + basePrefix + ", resource-absolute-path: " + absolutePath + ".");
        }
        
        fixCollection((CollectionImpl) collection);
        
        // fixing the path attribute of the resource
        if (absolutePath != null) {
            String relativePath = getOutPath(absolutePath);
            ((ResourceImpl) collection).setPath(relativePath);
        }
        
        fixMountPoints(collection);
        
        return collection;
    }

    /**
     * Filter search results, so the results outside the base prefix will be ignored and results
     * inside the base prefix will be converted to relative paths.
     *
     * @param collection unfiltered search results
     *
     * @return filtered search results
     * @throws RepositoryException throws if the operation failed.
     */
    public Collection filterSearchResult(Collection collection) throws RepositoryException {
        if (basePrefix == null || basePrefix.length() == 0) {
            return collection;
        }
        
        String[] results = collection.getChildPaths();
        
        if (results == null || results.length == 0) {
            return collection;
        }
        
        List<String> filteredResult = new ArrayList<String>();
        
        for (String result : results) {
            if (result.startsWith(basePrefix + RepositoryConstants.PATH_SEPARATOR)) {
                filteredResult.add(result);
            }
        }
        
        String[] filteredResultArr = filteredResult.toArray(new String[filteredResult.size()]);
        collection.setContent(filteredResultArr);
        
        return collection;
    }

    /**
     * The internal method to convert the collection to hold relative path values.
     *
     * @param collection the collection with absolute paths.
     *
     * @throws RepositoryException throws if the operation failed.
     */
    private void fixCollection(CollectionImpl collection) throws RepositoryException {
        if (basePrefix == null || basePrefix.length() == 0) {
            return;
        }
        
        Object content = collection.getContent();
        
        if (content instanceof String[]) {
            String[] paths = (String[]) content;
            
            for (int i = 0; i < paths.length; i++) {
                paths[i] = getOutPath(paths[i]);
            }
        } else if (content instanceof Resource[]) {
            Resource[] resources = (Resource[]) content;
            
            for (Resource resource : resources) {
                ((ResourceImpl) resource).setPath(getOutPath(resource.getPath()));
            }
        }
    }

    /**
     * Mount points of a give resource are converted to relative values.
     *
     * @param resource the resource which it is mount points are converting to relative values.
     */
    private void fixMountPoints(Resource resource) {
        if (basePrefix == null || basePrefix.length() == 0) {
            return;
        }
        
        String mountPoint = resource.getProperty(RepositoryConstants.REGISTRY_MOUNT_POINT);
      
        if (mountPoint != null) {
            resource.setProperty(RepositoryConstants.REGISTRY_MOUNT_POINT, getOutPath(mountPoint));
        }
        
        String targetPoint = resource.getProperty(RepositoryConstants.REGISTRY_TARGET_POINT);
        
        if (targetPoint != null) {
            resource.setProperty(RepositoryConstants.REGISTRY_TARGET_POINT, getOutPath(targetPoint));
        }
        
        String actualPath = resource.getProperty(RepositoryConstants.REGISTRY_ACTUAL_PATH);
        
        if (actualPath != null) {
            resource.setProperty(RepositoryConstants.REGISTRY_ACTUAL_PATH, getOutPath(actualPath));
        }
    }

    /**
     * Convert the paths of the log entries to relative values.
     *
     * @param logEntries the array of log entries to be converted to relative paths.
     *
     * @return the log entries after converting them relative values.
     */
    public Activity[] fixLogEntries(Activity[] logEntries) {
        if (basePrefix == null || basePrefix.length() == 0) {
            return logEntries;
        }
        
        List<Activity> fixedLogEntries = new ArrayList<Activity>();
        
        for (Activity logEntry : logEntries) {
            String logPath = logEntry.getResourcePath();
            
            if (logPath == null || (!logPath.startsWith(basePrefix + RepositoryConstants.PATH_SEPARATOR) && !logPath.equals(basePrefix))) {
                continue;
            }
            
            logEntry.setResourcePath(getOutPath(logPath));
            
            if (logEntry.getActionData() != null && logEntry.getActionData().startsWith(basePrefix)) {
                logEntry.setActionData(getOutPath(logEntry.getActionData()));
            } 

            fixedLogEntries.add(logEntry);
        }
        
        return fixedLogEntries.toArray(new Activity[fixedLogEntries.size()]);
    }
}
