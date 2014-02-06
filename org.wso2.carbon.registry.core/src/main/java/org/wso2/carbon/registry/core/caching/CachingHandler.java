/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.registry.core.caching;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import javax.cache.Cache;

import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.registry.core.config.DataBaseConfiguration;
import org.wso2.carbon.registry.core.config.Mount;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.config.RemoteConfiguration;
import org.wso2.carbon.registry.core.session.CurrentSession;
import org.wso2.carbon.registry.core.utils.InternalConstants;
import org.wso2.carbon.registry.core.utils.InternalUtils;
import org.wso2.carbon.repository.Collection;
import org.wso2.carbon.repository.GhostResource;
import org.wso2.carbon.repository.Registry;
import org.wso2.carbon.repository.RepositoryConstants;
import org.wso2.carbon.repository.Resource;
import org.wso2.carbon.repository.ResourcePath;
import org.wso2.carbon.repository.exceptions.RepositoryException;
import org.wso2.carbon.repository.handlers.Handler;
import org.wso2.carbon.repository.handlers.RequestContext;
import org.wso2.carbon.repository.utils.RepositoryUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

/**
 * CachingHandler using to handle the cached results of registry operation. We are removing the the
 * data from cache for all the write operations.
 */
public class CachingHandler extends Handler {

    private Map<String, DataBaseConfiguration> dbConfigs =
            new HashMap<String, DataBaseConfiguration>();
    private Map<String, DataBaseConfiguration> dbConfigsWithMounts =
        new HashMap<String, DataBaseConfiguration>();
    private Map<String, String> pathMap =
            new HashMap<String, String>();

    /**
     * Default Constructor
     */
    public CachingHandler() {
        RegistryContext registryContext = RegistryContext.getBaseInstance();
        for (Mount mount : registryContext.getMounts()) {
            for(RemoteConfiguration configuration : registryContext.getRemoteInstances()) {
                if (configuration.getDbConfig() != null &&
                        mount.getInstanceId().equals(configuration.getId())) {
                    dbConfigs.put(mount.getTargetPath(),
                            registryContext.getDBConfig(configuration.getDbConfig()));
                    dbConfigsWithMounts.put(mount.getPath(),
                            registryContext.getDBConfig(configuration.getDbConfig()));
                    pathMap.put(mount.getPath(), mount.getTargetPath());
                }
            }
        }
    }

    private static Cache<RegistryCacheKey, GhostResource> getCache() {
        return InternalUtils.getResourceCache(/*RepositoryConstants.*/ InternalConstants.REGISTRY_CACHE_BACKED_ID);
    }

    /**
     * used to clear cache for the registry write operations
     *
     * @param requestContext registryContext
     * @param cachePath      cached resource path
     * @param recursive      whether this operation must be recursively applied on child resources
     */
    private void clearCache(RequestContext requestContext, String cachePath, boolean recursive) {
        clearCache(requestContext, cachePath, recursive, false);
    }

    /**
     * used to clear cache for the registry write operations
     *
     * @param requestContext registryContext
     * @param cachePath      cached resource path
     * @param recursive      whether this operation must be recursively applied on child resources
     * @param local          whether this is to clear the local path.
     */
    private void clearCache(RequestContext requestContext, String cachePath, boolean recursive,
                            boolean local) {
        String connectionId = "";
        DataBaseConfiguration dataBaseConfiguration = null;
        boolean doLocalCleanup = false;

        String cleanupPath = cachePath;
        if (!local && dbConfigs.size() > 0) {
            for (String targetPath : dbConfigs.keySet()) {
                if (cachePath.startsWith(targetPath)) {
                    dataBaseConfiguration = dbConfigs.get(targetPath);
                    break;
                }
            }
            if (dataBaseConfiguration == null) {
                // our handler works fine after mounts are created, but does not work for mounted
                // paths before creating mounts. This is the solution for that.
                for (String targetPath : dbConfigsWithMounts.keySet()) {
                    if (cachePath.startsWith(targetPath)) {
                        dataBaseConfiguration = dbConfigsWithMounts.get(targetPath);
                        cleanupPath = pathMap.get(targetPath) +
                                cachePath.substring(targetPath.length());
                        break;
                    }
                }
            }
        }
        if (!local && dataBaseConfiguration != null) {
            doLocalCleanup = true;
        }

        if (dataBaseConfiguration == null) {
        	Registry registry = requestContext.getRegistry();
        	
        	RegistryContext registryContext = InternalUtils.getRegistryContext(registry);
        	
            if (registryContext == null) {
                registryContext = RegistryContext.getBaseInstance();
            }
            dataBaseConfiguration = registryContext.getDefaultDataBaseConfiguration();
        }
        if (dataBaseConfiguration != null) {
            connectionId = (dataBaseConfiguration.getUserName() != null
                    ? dataBaseConfiguration.getUserName().split("@")[0]:dataBaseConfiguration.getUserName()) + "@" + dataBaseConfiguration.getDbUrl();
        }
        int tenantId;
        tenantId = CurrentSession.getTenantId();
        if (tenantId == MultitenantConstants.INVALID_TENANT_ID) {
            tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        }
        if (local && CurrentSession.getLocalPathMap() != null &&
                CurrentSession.getLocalPathMap().get(cachePath) != null) {
            cleanupPath = CurrentSession.getLocalPathMap().get(cachePath);
        } else if (!local && doLocalCleanup) {
            if (cachePath.equals(cleanupPath)) {
                clearCache(requestContext, cachePath, recursive, true);
            } else if (CurrentSession.getLocalPathMap() == null) {
                CurrentSession.setLocalPathMap(
                        Collections.<String, String>singletonMap(cleanupPath, cachePath));
                try {
                    clearCache(requestContext, cleanupPath, recursive, true);
                } finally {
                    CurrentSession.removeLocalPathMap();
                }
            } else {
                Map<String, String> currentLocalPathMap = CurrentSession.getLocalPathMap();
                Map<String, String> newLocalPathMap =
                        new HashMap<String, String> (currentLocalPathMap);
                newLocalPathMap.put(cleanupPath, cachePath);
                CurrentSession.setLocalPathMap(newLocalPathMap);
                try {
                    clearCache(requestContext, cleanupPath, recursive, true);
                } finally {
                    CurrentSession.setLocalPathMap(currentLocalPathMap);
                }
            }
        }

        removeFromCache(connectionId, tenantId, cleanupPath);
        String parentPath = RepositoryUtils.getParentPath(cleanupPath);
        Cache<RegistryCacheKey, GhostResource> cache = getCache();
        Iterator<RegistryCacheKey> keys = cache.keys();
        while (keys.hasNext()) {
            RegistryCacheKey key = keys.next();
            String path = key.getPath();
            if (recursive) {
                if (path.startsWith(cleanupPath)) {
                    removeFromCache(connectionId, tenantId, path);
                }
            }
        }
        clearAncestry(connectionId, tenantId, parentPath);
    }

    private void clearAncestry(String connectionId, int tenantId, String parentPath) {
        boolean cleared = removeFromCache(connectionId, tenantId, parentPath);
        String pagedParentPathPrefix = "^" + Pattern.quote((parentPath == null) ? "" : parentPath)
                + "(" + RepositoryConstants.PATH_SEPARATOR + ")?(;start=.*)?$";
        Pattern pattern = Pattern.compile(pagedParentPathPrefix);
        Cache<RegistryCacheKey, GhostResource> cache = getCache();
        Iterator<RegistryCacheKey> keys = cache.keys();
        while (keys.hasNext()) {
            RegistryCacheKey key = keys.next();
            String path = key.getPath();
            if (pattern.matcher(path).matches()) {
                cleared = cleared || removeFromCache(connectionId, tenantId, path);
            }
        }
        if (!cleared && parentPath != null && !parentPath.equals(RepositoryConstants.ROOT_PATH)) {
            clearAncestry(connectionId, tenantId, RepositoryUtils.getParentPath(parentPath));
        }
    }

    private boolean removeFromCache(String connectionId, int tenantId, String path) {
        RegistryCacheKey cacheKey = InternalUtils.buildRegistryCacheKey(connectionId, tenantId, path);
        Cache<RegistryCacheKey, GhostResource> cache = getCache();
        if (cache.containsKey(cacheKey)) {
            cache.remove(cacheKey);
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public void put(RequestContext requestContext) throws RepositoryException {
        Resource resource = requestContext.getResource();
        if (resource.getProperty(RepositoryConstants.REGISTRY_LINK) != null) {
            String path = resource.getProperty(RepositoryConstants.REGISTRY_REAL_PATH);
            if (path != null) {
                path = path.substring(path.indexOf("/resourceContent?path=") +
                        "/resourceContent?path=".length());
                clearCache(requestContext, path,
                        requestContext.getResource() instanceof Collection);
            }
        }
        clearCache(requestContext, requestContext.getResourcePath().getPath(),
                requestContext.getResource() instanceof Collection ||
                        (requestContext.getResource() instanceof GhostResource
                                &&
                                ((GhostResource<Resource>) requestContext.getResource())
                                        .getResource() instanceof Collection));


        super.put(requestContext);
    }

    public void importResource(RequestContext requestContext) throws RepositoryException {
        clearCache(requestContext, requestContext.getResourcePath().getPath(), false);
        super.importResource(requestContext);
    }

    public String move(RequestContext requestContext) throws RepositoryException {
        clearCache(requestContext, requestContext.getSourcePath(), true);
        clearCache(requestContext, requestContext.getTargetPath(), true);
        return super.move(requestContext);
    }
    public String copy(RequestContext requestContext) throws RepositoryException {
        clearCache(requestContext, requestContext.getTargetPath(), true);
        return super.copy(requestContext);
    }

    public String rename(RequestContext requestContext) throws RepositoryException {
        clearCache(requestContext, requestContext.getSourcePath(), true);
        return super.rename(requestContext);
    }

    public void createLink(RequestContext requestContext) throws RepositoryException {
        clearCache(requestContext, requestContext.getResourcePath().getPath(), true);
        super.createLink(requestContext);
    }

    public void removeLink(RequestContext requestContext) throws RepositoryException {
        clearCache(requestContext, requestContext.getResourcePath().getPath(), true);
        super.removeLink(requestContext);
    }

    public void delete(RequestContext requestContext) throws RepositoryException {
        clearCache(requestContext, requestContext.getResourcePath().getPath(), true);
        super.delete(requestContext);
    }

    public void restore(RequestContext requestContext) throws RepositoryException {
        clearCache(requestContext, requestContext.getResourcePath().getPath(), true);
        super.restore(requestContext);
    }

    public void restoreVersion(RequestContext requestContext) throws RepositoryException {
        clearCache(requestContext, new ResourcePath(requestContext.getVersionPath()).getPath(),
                true);
        super.restoreVersion(requestContext);
    }
}