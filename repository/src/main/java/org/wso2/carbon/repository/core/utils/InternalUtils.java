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

package org.wso2.carbon.repository.core.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.repository.api.Collection;
import org.wso2.carbon.repository.api.Repository;
import org.wso2.carbon.repository.api.RepositoryConstants;
import org.wso2.carbon.repository.api.RepositoryService;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.ResourcePath;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.handlers.HandlerContext;
import org.wso2.carbon.repository.api.utils.Method;
import org.wso2.carbon.repository.api.utils.RepositoryUtils;
import org.wso2.carbon.repository.core.CollectionImpl;
import org.wso2.carbon.repository.core.CurrentContext;
import org.wso2.carbon.repository.core.EmbeddedRepository;
import org.wso2.carbon.repository.core.EmbeddedRepositoryService;
import org.wso2.carbon.repository.core.ResourceIDImpl;
import org.wso2.carbon.repository.core.ResourceImpl;
import org.wso2.carbon.repository.core.caching.CacheResource;
import org.wso2.carbon.repository.core.caching.RepositoryCacheEntry;
import org.wso2.carbon.repository.core.caching.RepositoryCacheKey;
import org.wso2.carbon.repository.core.config.RemoteConfiguration;
import org.wso2.carbon.repository.core.config.RepositoryContext;
import org.wso2.carbon.repository.core.config.StaticConfiguration;
import org.wso2.carbon.repository.core.handlers.HandlerLifecycleManager;
import org.wso2.carbon.repository.core.handlers.HandlerManager;
import org.wso2.carbon.repository.core.handlers.builtin.MountHandler;
import org.wso2.carbon.repository.core.handlers.builtin.SymLinkHandler;
import org.wso2.carbon.repository.core.handlers.builtin.URLMatcher;
import org.wso2.carbon.repository.spi.dao.ResourceDAO;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

public class InternalUtils {
	
    private static final Log log = LogFactory.getLog(RepositoryUtils.class);
//    private static final String ENCODING = System.getPropertyValue("carbon.registry.character.encoding");

    private InternalUtils() {
    }
    
    /**
     * Paths referring to a version of a resource are in the form /c1/c2/r1?v=2. Give a such version
     * path, this method extracts the valid pure path and the version number of the resource. Note
     * that this method should only be used for pure resource paths.
     *
     * @param resourcePath versioned resource path.
     *
     * @return VersionPath object containing the valid resource path and the version number.
     */
    public static VersionedPath getVersionedPath(ResourcePath resourcePath) {
        String versionString = resourcePath.getParameterValue("version");
        long versionNumber = -1;
        
        if (versionString != null) {
            versionNumber = Long.parseLong(versionString);
        }

        String plainPath = getPureResourcePath(resourcePath.getPath());

        VersionedPath versionedPath = new VersionedPath();
        versionedPath.setPath(plainPath);
        versionedPath.setVersion(versionNumber);

        return versionedPath;
    }
    
    /**
     * All "valid" paths pure resources should be in the form /c1/c2/r1. That is they should start
     * with "/" and should not end with "/". Given a path of a pure resource, this method prepares
     * the valid path for that path.
     *
     * @param resourcePath Path of a pure resource.
     *
     * @return Valid path of the pure resource.
     */
    public static String getPureResourcePath(String resourcePath) {
        if (resourcePath == null) {
            return null;
        }

        String preparedPath = resourcePath;
        
        if (preparedPath.equals(RepositoryConstants.ROOT_PATH)) {
            return preparedPath;
        } else {
            if (!preparedPath.startsWith(RepositoryConstants.ROOT_PATH)) {
                preparedPath = RepositoryConstants.ROOT_PATH + preparedPath;
            }
            
            if (preparedPath.endsWith(RepositoryConstants.PATH_SEPARATOR)) {
                preparedPath = preparedPath.substring(0, preparedPath.length() - 1);
            }
            
            if (preparedPath.contains("//")) {
                preparedPath = preparedPath.replace("//", "/");
            }
        }

        return preparedPath;
    }
    
    /**
     * Method to obtain a unique identifier for the database connection.
     *
     * @param connection the database connection.
     *
     * @return the unique identifier.
     */
    public static String getConnectionId(Connection connection) {
        try {
            // The connection URL is unique enough to be used as an identifier since one thread
            // makes one connection to the given URL according to our model.
            DatabaseMetaData connectionMetaData = connection.getMetaData();
            
            if (connectionMetaData != null) {
                return connectionMetaData.getUserName() + "@" + connectionMetaData.getURL();
            }
        } catch (SQLException ignore) {
        }
        return null;
    }

    /**
     * Builds the cache key for a resource path.
     *
     * @param connectionId the database connection identifier
     * @param tenantId     the tenant identifier
     * @param resourcePath the resource path
     *
     * @return the cache key.
     */
    public static RepositoryCacheKey buildRegistryCacheKey(String connectionId, int tenantId, String resourcePath) {
        RepositoryContext registryContext = RepositoryContext.getBaseInstance();
        String absoluteLocalRepositoryPath = getAbsolutePath(registryContext, RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH);
        
        if (resourcePath != null && resourcePath.startsWith(absoluteLocalRepositoryPath)) {
            return new RepositoryCacheKey(resourcePath, tenantId, registryContext.getNodeIdentifier() + ":" + connectionId.toLowerCase());
        } else {
            return new RepositoryCacheKey(resourcePath, tenantId, connectionId.toLowerCase());
        }
    }
    
    /**
     * Method used to retrieve cache object for resources
     * @param name the name of the cache
     * @return the cache object for the given cache manger and cache name
     */
    @SuppressWarnings("rawtypes")
	public static Cache<RepositoryCacheKey, CacheResource> getResourceCache(String name) {
        CacheManager manager = getCacheManager();
        return (manager != null) ? manager.<RepositoryCacheKey, CacheResource>getCache(name) :
                Caching.getCacheManager().<RepositoryCacheKey, CacheResource>getCache(name);
    }

    /**
     * Method used to retrieve cache object for resource paths.
     * @param name the name of the cache
     * @return the cache object for the given cache manger and cache name
     */
    public static Cache<RepositoryCacheKey, RepositoryCacheEntry> getResourcePathCache(String name) {
        CacheManager manager = getCacheManager();
        return (manager != null) ? manager.<RepositoryCacheKey, RepositoryCacheEntry>getCache(name) :
                Caching.getCacheManager().<RepositoryCacheKey, RepositoryCacheEntry>getCache(name);
    }

    private static CacheManager getCacheManager() {
        return Caching.getCacheManagerFactory().getCacheManager(InternalConstants.REGISTRY_CACHE_MANAGER);
    }
    
    /**
     * Method to redirect a given response to some URL.
     *
     * @param response the HTTP Response.
     * @param url      The URL to redirect to.
     */
    public static void redirect(HttpServletResponse response, String url) {
        try {
            response.sendRedirect(url);
        } catch (IOException e) {
            String msg = "Failed to redirect to the URL " + url + ". \nCaused by " + e.getMessage();
            log.error(msg, e);
        }
    }
    
    /**
     * Prepare the given path as a general resource path to be used in the registry. General
     * resource paths may refer to pure resources or virtual resources.
     *
     * @param rawPath Raw path to be prepared
     *
     * @return Prepared general resource path
     */
    public static String prepareGeneralPath(String rawPath) {
        String path = rawPath;

        // path should always start with "/"
        if (!rawPath.startsWith(RepositoryConstants.ROOT_PATH)) {
            path = RepositoryConstants.ROOT_PATH + rawPath;
        }

        return path;
    }
    
    /**
     * Method to determine whether a system resource (or collection) is existing or whether it
     * should be created. Once it has been identified that a system collection is existing, it will
     * not be checked against a database until the server has been restarted.
     *
     * @param registry the base registry to use.
     * @param absolutePath the absolute path of the system resource (or collection)
     *
     * @return whether the system resource (or collection) needs to be added.
     *
     * @throws RepositoryException if an error occurred.
     */
    public static boolean systemResourceShouldBeAdded(Repository registry, String absolutePath)
        throws RepositoryException {
    	
        RepositoryContext registryContext = getRepositoryContext(registry);
        
        if (registryContext == null) {
            registryContext = RepositoryContext.getBaseInstance();
        }
        
        if (registryContext.isSystemResourcePathRegistered(absolutePath)) {
            return false;
        } else if (registry.resourceExists(absolutePath)) {
            registryContext.registerSystemResourcePath(absolutePath);
            return false;
        }
        
        return true;
    }
    
    /**
     * Method to determine whether a system resource (or collection) is existing or whether it
     * should be created. Once it has been identified that a system collection is existing, it will
     * not be checked against a database until the server has been restarted.
     *
     * @param dataAccessObject the resource data access object.
     * @param absolutePath the absolute path of the system resource (or collection)
     *
     * @return whether the system resource (or collection) needs to be added.
     *
     * @throws RepositoryException if an error occurred.
     */
    public static boolean systemResourceShouldBeAdded(ResourceDAO dataAccessObject, String absolutePath) throws RepositoryException {
        RepositoryContext registryContext = RepositoryContext.getBaseInstance();
        
        if (RepositoryContext.getBaseInstance().isSystemResourcePathRegistered(absolutePath)) {
            return false;
        } else if (dataAccessObject.resourceExists(absolutePath)) {
            registryContext.registerSystemResourcePath(absolutePath);
            return false;
        }
        
        return true;
    }
    
    /**
     * This method is used to remove LC information from artifacts when clean up operation is done.
     * @param registry
     * @throws RepositoryException
     */
    private static void cleanArtifactLCs(Repository registry) throws RepositoryException {
        String resourcePaths[];
        String collectionpaths[];
        
        String resourceQuery = "SELECT R.REG_PATH_ID, R.REG_NAME FROM REG_RESOURCE R, REG_PROPERTY PP, " +
                "REG_RESOURCE_PROPERTY RP WHERE";

        String collectionQuery = "SELECT R.REG_PATH_ID, R.REG_NAME FROM REG_RESOURCE R, REG_PROPERTY PP, " +
                "REG_RESOURCE_PROPERTY RP WHERE";

        if (StaticConfiguration.isVersioningProperties()) {
            resourceQuery += "R.REG_VERSION=RP.REG_VERSION AND RP.REG_PROPERTY_ID=PP.REG_ID AND lower(PP.REG_NAME) LIKE ?";
        } else {
            resourceQuery += "R.REG_PATH_ID=RP.REG_PATH_ID AND "
                    + "((R.REG_NAME = RP.REG_RESOURCE_NAME)) AND "
                    + "RP.REG_PROPERTY_ID=PP.REG_ID AND lower(PP.REG_NAME) LIKE ?";
        }

        if (StaticConfiguration.isVersioningProperties()) {
            collectionQuery += "R.REG_VERSION=RP.REG_VERSION AND RP.REG_PROPERTY_ID=PP.REG_ID AND  lower(PP.REG_NAME) LIKE ?  AND R.REG_NAME IS NULL";
        } else {
            collectionQuery += "R.REG_PATH_ID=RP.REG_PATH_ID AND "
                            + "R.REG_NAME IS NULL AND RP.REG_RESOURCE_NAME IS NULL AND "
                            + "RP.REG_PROPERTY_ID=PP.REG_ID AND  lower(PP.REG_NAME) LIKE ?  AND R.REG_NAME IS NULL";
        }

        Map<String,Object> paramMapResources = new HashMap<String, Object>();
        paramMapResources.put("1","registry.lc.name");
        paramMapResources.put("query", resourceQuery);

        Map<String,Object> paramMapCollections= new HashMap<String, Object>();
        paramMapCollections.put("1","registry.lc.name");
        paramMapCollections.put("query", collectionQuery);

        resourcePaths = (String[]) registry.executeQuery(null,paramMapResources).getContent();
        collectionpaths = (String[]) registry.executeQuery(null,paramMapCollections).getContent();

        for (String path : resourcePaths) {
            removeArtifactLC(path,registry);
        }

        for (String path : collectionpaths) {
            removeArtifactLC(path,registry);
        }

    }
    
    /**
     * Method to remove Lifecycle from a given resource on the registry.
     *
     * @param path     the path of the resource.
     * @param registry the registry instance on which the resource is available.
     *
     * @throws RepositoryException if the operation failed.
     */
    private static void removeArtifactLC(String path, Repository registry) throws RepositoryException {
        try {
            /* set all the variables to the resource */
            Resource resource = registry.get(path);
            //List<Property> propList = new ArrayList<Property>();
            Iterator<?> iKeys = resource.getPropertyKeys().iterator();
            ArrayList<String> propertiesToRemove = new ArrayList<String>();

            while (iKeys.hasNext()) {
                String propKey = (String) iKeys.next();

                if (propKey.startsWith("registry.custom_lifecycle.votes.")
                        ||propKey.startsWith("registry.custom_lifecycle.user.")
                        ||propKey.startsWith("registry.custom_lifecycle.checklist.")
                        || propKey.startsWith("registry.LC.name")
                        || propKey.startsWith("registry.lifecycle.")
                        || propKey.startsWith("registry.Aspects")) {
                    propertiesToRemove.add(propKey);
                }
            }

            for(String propertyName : propertiesToRemove) {
                resource.removeProperty(propertyName);
            }

            registry.put(path, resource);
        } catch (RepositoryException e) {

            String msg = "Failed to remove LifeCycle from resource " + path + ". " + e.getMessage();
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }
    }
    
    /**
     * This method builds the base collection structure for the registry. The base <b>_system</b>
     * collection, and the remaining local, config and governance collections will be created as a
     * result.
     *
     * @param registry  the base registry to use.
     *
     * @throws RepositoryException if an error occurred.
     */
    public static void addBaseCollectionStructure(Repository registry) throws RepositoryException {
        RepositoryContext registryContext = getRepositoryContext(registry);
        
        // Registry Context stays the same during the scope of this operation.
        if (log.isTraceEnabled()) {
            log.trace("Checking the existence of '" + getAbsolutePath(
                    registryContext, RepositoryConstants.SYSTEM_COLLECTION_BASE_PATH)
                    + "' collection of the Registry.");
        }
        if (systemResourceShouldBeAdded(registry, getAbsolutePath(registryContext, RepositoryConstants.SYSTEM_COLLECTION_BASE_PATH))) {
            if (registryContext != null && registryContext.isClone()) {
                return;
            }
            
            if (log.isTraceEnabled()) {
                log.trace("Creating the '" + getAbsolutePath(registryContext,
                        RepositoryConstants.SYSTEM_COLLECTION_BASE_PATH)
                        + "' collection of the Registry.");
            }

            CollectionImpl systemCollection = (CollectionImpl) registry.newCollection();
            String systemDescription = "System collection of the Registry. This collection is " +
                    "used to store the resources required by the carbon server.";
            systemCollection.setDescription(systemDescription);
            registry.put(getAbsolutePath(registryContext, RepositoryConstants.SYSTEM_COLLECTION_BASE_PATH), systemCollection);

            CollectionImpl localRepositoryCollection = (CollectionImpl) registry.newCollection();
            String localRepositoryDescription =
                    "Local data repository of the carbon server. This " +
                            "collection is used to store the resources local to this carbon " +
                            "server instance.";
            
            localRepositoryCollection.setDescription(localRepositoryDescription);
            registry.put(getAbsolutePath(registryContext, RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH), localRepositoryCollection);

            CollectionImpl configRegistryCollection = (CollectionImpl) registry.newCollection();
            String configRegistryDescription = "Configuration registry of the carbon server. " +
                    "This collection is used to store the resources of this product cluster.";
            configRegistryCollection.setDescription(configRegistryDescription);
            registry.put(getAbsolutePath(registryContext, RepositoryConstants.CONFIG_REGISTRY_BASE_PATH), configRegistryCollection);

            CollectionImpl governanceRegistryCollection = (CollectionImpl) registry.newCollection();
            String governanceRegistryDescription = "Governance registry of the carbon server. " +
                    "This collection is used to store the resources common to the whole " +
                    "platform.";
            governanceRegistryCollection.setDescription(governanceRegistryDescription);
            registry.put(getAbsolutePath(registryContext, RepositoryConstants.GOVERNANCE_REGISTRY_BASE_PATH), governanceRegistryCollection);
        }
        
        // This is to create the repository collections for the various registries and clean
        // them at start-up, if needed.
        boolean cleanRegistry = false;
        
        if (System.getProperty(InternalConstants.CARBON_REGISTRY_CLEAN) != null) {
            cleanRegistry = true;
            System.clearProperty(InternalConstants.CARBON_REGISTRY_CLEAN);
        }
        
        if (cleanRegistry) {
            cleanArtifactLCs(registry);
        }
        
        for (String repositoryPath : new String[] {
            getAbsolutePath(registryContext, RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH + "/repository"),
            getAbsolutePath(registryContext, RepositoryConstants.GOVERNANCE_REGISTRY_BASE_PATH + "/repository"),
            getAbsolutePath(registryContext, RepositoryConstants.CONFIG_REGISTRY_BASE_PATH + "/repository")
            
        }) 
        {
            if (cleanRegistry) {
                if (registry.resourceExists(repositoryPath)) {
                    try {
                        registry.delete(repositoryPath);
                        log.info("Cleaned the registry space of carbon at path: " +
                                repositoryPath);
                    } catch (Exception e) {
                        log.error(
                                "An error occurred while cleaning of registry space of carbon " +
                                        "at path: " + repositoryPath, e);
                    }
                }
            } else if (systemResourceShouldBeAdded(registry, repositoryPath)) {
                registry.put(repositoryPath, registry.newCollection());
            }
        }
    }
    
    /**
     * Method to add the collection where the user profiles are stored.
     *
     * @param registry     the registry to use.
     * @param profilesPath the path at which user profiles are stored.
     *
     * @throws RepositoryException if an error occurred.
     */
    public static void addUserProfileCollection(Repository registry, String profilesPath) throws RepositoryException {
        if (log.isTraceEnabled()) {
            log.trace("Checking the existence of the '" + profilesPath + "' collection of the Registry.");
        }
        
        RepositoryContext registryContext = getRepositoryContext(registry);
        
        if (systemResourceShouldBeAdded(registry, profilesPath)) {
            if (registryContext != null && registryContext.isClone()) {
                return;
            }
            
            if (log.isTraceEnabled()) {
                log.trace("Creating the '" + profilesPath + "' collection of the Registry.");
            }
            
            CollectionImpl systemCollection = (CollectionImpl) registry.newCollection();
            String systemDescription = "Collection which contains user-specific details.";
            systemCollection.setDescription(systemDescription);
            registry.put(profilesPath, systemCollection);
        }
    }
    
    /**
     * Method to add the collection where the services are stored.
     *
     * @param registry    the registry to use.
     * @param servicePath the path at which services are stored.
     *
     * @throws RepositoryException if an error occurred.
     */
    public static void addServiceStoreCollection(Repository registry, String servicePath) throws RepositoryException {
        if (log.isTraceEnabled()) {
            log.trace("Checking the existence of the '" + servicePath + "' collection of the Registry.");
        }
        
        RepositoryContext registryContext = getRepositoryContext(registry);
        
        if (systemResourceShouldBeAdded(registry, servicePath)) {
            if (registryContext != null && registryContext.isClone()) {
                return;
            }
            
            if (log.isTraceEnabled()) {
                log.trace("Creating the '" + servicePath + "' collection of the Registry.");
            }
            
            CollectionImpl systemCollection = (CollectionImpl) registry.newCollection();
            String systemDescription = "Collection which contains all the Service information";
            systemCollection.setDescription(systemDescription);
            registry.put(servicePath, systemCollection);
        }
    }
    
    /**
     * Method to add the collection where the mount information is stored.
     *
     * @param repository the registry to use.
     *
     * @throws RepositoryException if an error occurred.
     */
    public static void addMountCollection(Repository repository) throws RepositoryException {
        if (log.isTraceEnabled()) {
            log.trace("Checking the existence of the '" +
                    RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH + RepositoryConstants.SYSTEM_MOUNT_PATH + "' collection of the Registry.");
        }
        
        if (!repository.resourceExists(RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH + RepositoryConstants.SYSTEM_MOUNT_PATH)) {
            RepositoryContext registryContext = getRepositoryContext(repository);
            
            if (registryContext != null && registryContext.isClone()) {
                return;
            }
            
            if (log.isTraceEnabled()) {
                log.trace("Creating the '" + RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH +
                        RepositoryConstants.SYSTEM_MOUNT_PATH + "' collection of the Registry.");
            }
            
            Collection mountCollection = repository.newCollection();
            
            String description = "Mount collection stores details of mount points of the repository.";
            mountCollection.setDescription(description);
            
            repository.put(RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH + RepositoryConstants.SYSTEM_MOUNT_PATH, mountCollection);
        }
    }

    /**
     * Method to register mount points.
     *
     * @param systemRepository the registry to use.
     * @param tenantId       the identifier of the tenant.
     *
     * @throws RepositoryException if an error occurred.
     */
    public static void registerMountPoints(Repository systemRepository, int tenantId) throws RepositoryException {
        String mountPath = RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH + RepositoryConstants.SYSTEM_MOUNT_PATH;
        
        if (!systemRepository.resourceExists(mountPath)) {
            return;
        }
        
        CollectionImpl mountCollection = (CollectionImpl) systemRepository.get(mountPath);
        
        String[] mountPoints = mountCollection.getChildPaths();
        Resource mountPoint;
        
        for (String mountPointString : mountPoints) {
            if (!systemRepository.resourceExists(mountPointString)) {
                log.warn("Unable to add mount. The mount point " + mountPointString + " was not found.");
                continue;
            }
            
            mountPoint = systemRepository.get(mountPointString);
            
            if (Boolean.toString(true).equals(mountPoint.getPropertyValue(InternalConstants.REGISTRY_FIXED_MOUNT))) {
                continue;
            }
            
            String path = mountPoint.getPropertyValue("path");
            String target = mountPoint.getPropertyValue("target");
            String targetSubPath = mountPoint.getPropertyValue("subPath");
            String author = mountPoint.getPropertyValue("author");

            try {
                CurrentContext.setCallerTenantId(tenantId);
                
                if (log.isTraceEnabled()) {
                    log.trace("Creating the mount point. path: " + path + ", target: " + target + ", target sub path " + targetSubPath + ".");
                }
                
                if (targetSubPath != null) {
                    registerHandlerForRemoteLinks(systemRepository, path, target, targetSubPath, author);
                } else {
                    registerHandlerForSymbolicLinks(systemRepository, path, target, author);
                }
            } catch (RepositoryException e) {
                log.warn("Couldn't mount " + target + ".");
                log.debug("Caused by: ", e);
            } finally {
                CurrentContext.removeCallerTenantId();
            }
        }
    }
    
    /**
     * Method to register handlers for symbolic links for the tenant on the current registry
     * session.
     *
     * @param repository the registry
     * @param path    the path at which the symbolic link is created.
     * @param target  the target path.
     * @param author  the creator of the symbolic link.
     *
     * @throws RepositoryException if the operation failed.
     */
    public static void registerHandlerForSymbolicLinks(Repository repository, String path, String target, String author) throws RepositoryException {
        SymLinkHandler handler = new SymLinkHandler();
        
        handler.setMountPoint(path);
        handler.setTargetPoint(target);
        handler.setAuthor(author);
        
        repository.getRepositoryService().addHandler(InternalUtils.getMountingMethods(), InternalUtils.getMountingMatcher(path), handler,
                HandlerLifecycleManager.TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE);
        
        // now we are going to iterate through all the already available symbolic links and resolve
        // the cyclic symbolic links
        Set<SymLinkHandler> symLinkHandlers = SymLinkHandler.getSymLinkHandlers();

        List<SymLinkHandler> handlersToRemove = new ArrayList<SymLinkHandler>();
        
        for (SymLinkHandler symLinkHandler: symLinkHandlers) {
            String symLinkTarget = symLinkHandler.getTargetPoint();

            // and then we remove old entries for the same mount path
            String mountPath = symLinkHandler.getMountPoint();
            
            if (path.equals(mountPath) && !target.equals(symLinkTarget)) {
                handlersToRemove.add(symLinkHandler);
            }
        }
        
        // removing the symlink handlers
        for (SymLinkHandler handlerToRemove : handlersToRemove) {
        	repository.getRepositoryService().removeHandler(handlerToRemove, HandlerLifecycleManager.TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE);
        }

        // and importantly add the new entry, the currently creating symlink information..
        symLinkHandlers.add(handler);
    }
    
    /**
     * Method to register handlers for symbolic links for the tenant on the current registry
     * session.
     *
     * @param context the registry context.
     * @param path    the path at which the symbolic link is created.
     * @param target  the target path.
     * @param author  the creator of the symbolic link.
     *
     * @throws RepositoryException if the operation failed.
     */
    public static void registerHandlerForSymbolicLinks(RepositoryContext context, String path, String target, String author) throws RepositoryException {
        SymLinkHandler handler = new SymLinkHandler();
        
        handler.setMountPoint(path);
        handler.setTargetPoint(target);
        handler.setAuthor(author);

        HandlerManager hm = context.getHandlerManager();
        hm.addHandler(InternalUtils.getMountingMethods(), InternalUtils.getMountingMatcher(path), handler,
                HandlerLifecycleManager.TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE);
        // now we are going to iterate through all the already available symbolic links and resolve
        // the cyclic symbolic links

        Set<SymLinkHandler> symLinkHandlers = SymLinkHandler.getSymLinkHandlers();

        List<SymLinkHandler> handlersToRemove = new ArrayList<SymLinkHandler>();
        
        for (SymLinkHandler symLinkHandler: symLinkHandlers) {
            String symLinkTarget = symLinkHandler.getTargetPoint();

            // and then we remove old entries for the same mount path
            String mountPath = symLinkHandler.getMountPoint();
            if (path.equals(mountPath) && !target.equals(symLinkTarget)) {
                handlersToRemove.add(symLinkHandler);
            }
        }
        
        // removing the symlink handlers
        for (SymLinkHandler handlerToRemove : handlersToRemove) {
            hm.removeHandler(handlerToRemove, HandlerLifecycleManager.TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE);
        }

        // and importantly add the new entry, the currently creating symlink information..
        symLinkHandlers.add(handler);
    }
    
    /**
     * Method to register handlers for remote links for the tenant on the current registry session.
     *
     * @param repository 		  the registry
     * @param path            the path at which the remote link is created.
     * @param target          the target path.
     * @param targetSubPath   the target sub-path.
     * @param author          the creator of the remote link.
     *
     * @throws RepositoryException if the operation failed.
     */
    public static void registerHandlerForRemoteLinks(Repository repository, String path, String target, String targetSubPath, String author)
            throws RepositoryException {
        registerHandlerForRemoteLinks(repository, path, target, targetSubPath, author, false);
    }
    
    /**
     * Method to register handlers for remote links for the tenant on the current registry session.
     *
     * @param registryContext the registry context.
     * @param path            the path at which the remote link is created.
     * @param target          the target path.
     * @param targetSubPath   the target sub-path.
     * @param author          the creator of the remote link.
     *
     * @throws RepositoryException if the operation failed.
     */
    public static void registerHandlerForRemoteLinks(RepositoryContext registryContext, String path, String target, String targetSubPath, String author)
            throws RepositoryException {
        registerHandlerForRemoteLinks(registryContext, path, target, targetSubPath, author, false);
    }

    /**
     * Method to register handlers for remote links.
     *
     * @param repository 	  the repository
     * @param path            the path at which the remote link is created.
     * @param target          the target path.
     * @param targetSubPath   the target sub-path.
     * @param author          the creator of the remote link.
     * @param forAllTenants   whether the remote link should be added to the tenant on the current
     *                        registry session or to all the tenants.
     *
     * @throws RepositoryException if the operation failed.
     */
    public static void registerHandlerForRemoteLinks(Repository repository, String path, String target, String targetSubPath, String author, boolean forAllTenants)
            throws RepositoryException {
    	List<RemoteConfiguration> remoteInstances = InternalUtils.getRepositoryContext(repository).getRemoteInstances();
    	
        for (RemoteConfiguration config : remoteInstances) {
            if (config.getId().equals(target)) {
                MountHandler handler = new MountHandler();
                
                handler.setUserName(config.getTrustedUser());
                handler.setPassword(config.getResolvedTrustedPwd());
                handler.setDbConfig(config.getDbConfig());
                handler.setRepositoryRoot(config.getRepositoryRoot());
                handler.setReadOnly(config.getReadOnly() != null && Boolean.toString(true).equals(config.getReadOnly().toLowerCase()));
                handler.setId(target);
                handler.setConURL(config.getUrl());
                handler.setMountPoint(path);
                handler.setSubPath(targetSubPath);
                handler.setAuthor(author);
                
                if (config.getTrustedUser() == null || config.getTrustedPwd() == null) {
                    handler.setRemote(false);
                } else {
                    handler.setRemote(true);
                    handler.setRegistryType(config.getType());
                }
                
                if (forAllTenants) {
                	repository.getRepositoryService().addHandler(InternalUtils.getMountingMethods(), InternalUtils.getMountingMatcher(path), handler);
                } else {
                	repository.getRepositoryService().addHandler(InternalUtils.getMountingMethods(),
                    		InternalUtils.getMountingMatcher(path), handler, HandlerLifecycleManager.TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE);
                }
                
                return;
            }
        }
        
        // We will get here, if somebody checks in to a remote registry with a no mount paths.
        if (remoteInstances.size() == 0) {
            String msg = "No remote instances have been found, The following mount point is not registered. " +
                    "path: " + path + ", " + "target: " + target + ", " +
                    ((targetSubPath == null)? "": ("target sub path: " + targetSubPath + ", "));
            log.debug(msg);
        } else {
            String msg = "Target mount path is not found, The following mount point is not registered. " +
                    "path: " + path + ", " + "target: " + target + ", " +
                    ((targetSubPath == null)? "": ("target sub path: " + targetSubPath + ", "));
            log.debug(msg);
        }
    }
    
    /**
     * Method to register handlers for remote links.
     *
     * @param registryContext the registry context.
     * @param path            the path at which the remote link is created.
     * @param target          the target path.
     * @param targetSubPath   the target sub-path.
     * @param author          the creator of the remote link.
     * @param forAllTenants   whether the remote link should be added to the tenant on the current
     *                        registry session or to all the tenants.
     *
     * @throws RepositoryException if the operation failed.
     */
    public static void registerHandlerForRemoteLinks(RepositoryContext registryContext, String path, String target,
                                                     String targetSubPath, String author, boolean forAllTenants) throws RepositoryException {
        HandlerManager hm = registryContext.getHandlerManager();
        List<RemoteConfiguration> remoteInstances = registryContext.getRemoteInstances();
        
        for (RemoteConfiguration config : remoteInstances) {
            if (config.getId().equals(target)) {
                MountHandler handler = new MountHandler();
                handler.setUserName(config.getTrustedUser());
                handler.setPassword(config.getResolvedTrustedPwd());
                handler.setDbConfig(config.getDbConfig());
                handler.setRepositoryRoot(config.getRepositoryRoot());
                handler.setReadOnly(config.getReadOnly() != null && Boolean.toString(true).equals(config.getReadOnly().toLowerCase()));
                handler.setId(target);
                handler.setConURL(config.getUrl());
                handler.setMountPoint(path);
                handler.setSubPath(targetSubPath);
                handler.setAuthor(author);
                
                if (config.getTrustedUser() == null || config.getTrustedPwd() == null) {
                    handler.setRemote(false);
                } else {
                    handler.setRemote(true);
                    handler.setRegistryType(config.getType());
                }
                
                if (forAllTenants) {
                    hm.addHandler(InternalUtils.getMountingMethods(), InternalUtils.getMountingMatcher(path), handler);
                } else {
                    hm.addHandler(InternalUtils.getMountingMethods(), InternalUtils.getMountingMatcher(path), handler,
                            HandlerLifecycleManager.TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE);
                }
                
                return;
            }
        }
        
        // We will get here, if somebody checks in to a remote registry with a no mount paths.
        // Such information ends up in our debug logs.
        if (remoteInstances.size() == 0) {
            String msg = "No remote instances have been found, The following mount point is not registered. " +
                    "path: " + path + ", " + "target: " + target + ", " +
                    ((targetSubPath == null)? "": ("target sub path: " + targetSubPath + ", "));
            log.debug(msg);
        } else {
            String msg = "Target mount path is not found, The following mount point is not registered. " +
                    "path: " + path + ", " + "target: " + target + ", " +
                    ((targetSubPath == null)? "": ("target sub path: " + targetSubPath + ", "));
            log.debug(msg);
        }
    }
    
    /**
     * Method to obtain the filter used with the mounting handler.
     *
     * @param path the path at which the mount was added.
     *
     * @return the built filter instance.
     */
    public static URLMatcher getMountingMatcher(String path) {
        URLMatcher matcher = new MountingMatcher();
        
        String matchedWith = Pattern.quote(path) + "($|" + RepositoryConstants.PATH_SEPARATOR + ".*|" + RepositoryConstants.URL_SEPARATOR + ".*)";
        matcher.setPattern(matchedWith);
        
        return matcher;
    }
    
    // This class is used to implement a URL Matcher that could be used for Mounting related
    // handlers.
    private static class MountingMatcher extends URLMatcher {
    	
        // Mounting related handlers support execute query for any path.
        public boolean handleExecuteQuery(HandlerContext requestContext)
                throws RepositoryException {
            return true;
        }

        // Mounting related handlers support get resource paths with tag for any path.
        public boolean handleGetResourcePathsWithTag(HandlerContext requestContext)
                throws RepositoryException {
            return true;
        }
    }
    
    /**
     * Utility method obtain the list of operations supported by the mount handler.
     *
     * @return the list of operations.
     */
    public static Method[] getMountingMethods() {
        return new Method[]{Method.RESOURCE_EXISTS, Method.GET, Method.PUT, Method.DELETE,
                Method.RENAME,
                Method.MOVE, Method.COPY, Method.GET_AVERAGE_RATING, Method.GET_RATING,
                Method.RATE_RESOURCE, Method.GET_COMMENTS, Method.ADD_COMMENT, Method.EDIT_COMMENT,
                Method.REMOVE_COMMENT, Method.GET_TAGS, Method.APPLY_TAG, Method.REMOVE_TAG,
                Method.GET_ALL_ASSOCIATIONS, Method.GET_ASSOCIATIONS, Method.ADD_ASSOCIATION,
                Method.DUMP, Method.RESTORE, Method.REMOVE_ASSOCIATION, Method.IMPORT,
                Method.EXECUTE_QUERY, Method.GET_RESOURCE_PATHS_WITH_TAG,
                Method.GET_REGISTRY_CONTEXT, Method.REMOVE_LINK };
    }
    
    /**
     * Method to determine whether this registry is running in Read-Only mode.
     *
     * @param registryContext the registry context.
     *
     * @return true if read-only or false otherwise.
     */
    public static boolean isRegistryReadOnly(RepositoryContext registryContext) {
        String repositoryWriteModeProperty = System.getProperty(ServerConstants.REPO_WRITE_MODE);
        
        if (repositoryWriteModeProperty != null) {
            return !(repositoryWriteModeProperty.equals("true"));
        }
        
        ServerConfiguration serverConfig = CarbonUtils.getServerConfiguration();

        String isRegistryReadOnly = serverConfig.getFirstProperty("Registry.ReadOnly");
        
        if (isRegistryReadOnly == null) {
            if (registryContext != null) {
                return registryContext.isReadOnly();
            }
            return RepositoryContext.getBaseInstance().isReadOnly();
        }
        
        return isRegistryReadOnly.equalsIgnoreCase(Boolean.TRUE.toString());
    }
    
    /**
     * Method to obtain the relative path for the given absolute path.
     *
     * @param context      the registry context.
     * @param absolutePath the absolute path.
     *
     * @return the relative path.
     */
    public static String getRelativePath(RepositoryContext context, String absolutePath) {
        if (context == null) {
            return RepositoryUtils.getRelativePathToOriginal(absolutePath, RepositoryContext.getBaseInstance().getRepositoryRoot());
        }
        
        return RepositoryUtils.getRelativePathToOriginal(absolutePath, context.getRepositoryRoot());
    }
    
    public static String getRelativePath(Repository registry, String absolutePath) {
        return RepositoryUtils.getRelativePathToOriginal(absolutePath, registry.getRepositoryService().getRepositoryRoot());
    }
    
    /**
     * Method to obtain the absolute path for the given relative path.
     *
     * @param context      the registry context.
     * @param relativePath the relative path.
     *
     * @return the absolute path.
     */
    public static String getAbsolutePath(RepositoryContext context, String relativePath) {
        if (context == null) {
            return RepositoryUtils.getAbsolutePathToOriginal(relativePath, RepositoryContext.getBaseInstance().getRepositoryRoot());
        }
        
        return RepositoryUtils.getAbsolutePathToOriginal(relativePath, context.getRepositoryRoot());
    }
    
    /**
     * Method to concatenate two chroot paths.
     *
     * @param chroot1 the base chroot path (or the registry root).
     * @param chroot2 the the relative chroot path.
     *
     * @return the full chroot path.
     */
    public static String concatenateChroot(String chroot1, String chroot2) {
        String chroot1out = chroot1;
        
        if (chroot1out == null || chroot1out.equals(RepositoryConstants.ROOT_PATH)) {
            return chroot2;
        }
        
        if (chroot2 == null || chroot2.equals(RepositoryConstants.ROOT_PATH)) {
            return chroot1out;
        }
        
        if (!chroot1out.endsWith(RepositoryConstants.PATH_SEPARATOR) && !chroot2.startsWith(RepositoryConstants.PATH_SEPARATOR)) {
            chroot1out += RepositoryConstants.PATH_SEPARATOR;
        } else if (chroot1out.endsWith(RepositoryConstants.PATH_SEPARATOR) && chroot2.startsWith(RepositoryConstants.PATH_SEPARATOR)) {
            chroot1out = chroot1out.substring(0, chroot1out.length() - 1);
        }
        
        String chroot = chroot1out + chroot2;
        
        if (chroot.endsWith(RepositoryConstants.PATH_SEPARATOR)) {
            chroot = chroot.substring(0, chroot.length() - 1);
        }
        
        return chroot;
    }
    
    /**
     * this method can only be called if the registry context is initialized.
     *
     * @param coreRegistry the core registry instance.
     *
     * @return the user registry instance for the special system user.
     * @throws RepositoryException if the operation failed.
     */
    public static Repository getSystemRegistry(Repository coreRegistry) throws RepositoryException {
        String systemUser = CarbonConstants.REGISTRY_SYSTEM_USERNAME;

        if (systemUser.equals(CurrentContext.getUser())) {
            return CurrentContext.getRespository();
        }

        return ((EmbeddedRepositoryService) coreRegistry.getRepositoryService()).getRepository(systemUser, CurrentContext.getTenantId(), CurrentContext.getChroot());
    }
    
    /**
     * Method to obtain the unchrooted path for the given relative path.
     *
     * @param path the relative path.
     *
     * @return the unchrooted path.
     */
    public static String getUnChrootedPath(String path) {
        if (path == null) {
            return null;
        }
        
        String localPath = path;
        
        if (CurrentContext.getLocalPathMap() != null) {
            String temp = CurrentContext.getLocalPathMap().get(path);
            if (temp != null) {
                localPath = temp;
            }
        }
        
        String chrootPrefix = getChrootPrefix();
        
        if (chrootPrefix == null) {
            return localPath;
        } else if (!localPath.startsWith("/")) {
            return localPath;
        } else if (localPath.equals("/")) {
            return chrootPrefix;
        }

        return chrootPrefix + localPath;
    }

    private static String getChrootPrefix() {
        RepositoryContext registryContext = RepositoryContext.getBaseInstance();
        
        if (registryContext == null) {
            return null;
        }
        
        String chrootPrefix = registryContext.getRepositoryRoot();
        
        if (chrootPrefix == null || chrootPrefix.length() == 0 || chrootPrefix.equals(RepositoryConstants.ROOT_PATH)) {
            return null;
        }
        return chrootPrefix;
    }
    
    /**
     * Method to add a mount entry.
     *
     * @param repository        the repository instance to use.
     * @param repositoryContext the registry context.
     * @param path            the source path.
     * @param instanceId          the target path or instance.
     * @param targetSubPath   the target sub-path.
     * @param author          the author
     *
     * @throws RepositoryException if the operation failed.
     */
    public static void addMountEntry(Repository repository, RepositoryContext repositoryContext, String path, String instanceId, String targetSubPath,
                                     String author) throws RepositoryException {
        Resource r = new ResourceImpl();
        
        String relativePath = getRelativePath(repositoryContext, path);
        
        r.addProperty("path", relativePath);
        r.addProperty("target", instanceId);
        r.addProperty("author", author);
        r.addProperty("subPath", targetSubPath);
        r.setMediaType(InternalConstants.MOUNT_MEDIA_TYPE);
        
        String mountPath = RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH + RepositoryConstants.SYSTEM_MOUNT_PATH + "/" + relativePath.replace("/", "-");
        
        if (!repository.resourceExists(mountPath)) {
            repository.put(mountPath, r);
        }
    }
    
    /**
     * Method to add a mount entry.
     *
     * @param registry        the registry instance to use.
     * @param registryContext the registry context.
     * @param path            the source path.
     * @param target          the target path or instance.
     * @param remote          whether local or remote link.
     * @param author          the author
     *
     * @throws RepositoryException if the operation failed.
     */
    public static void addMountEntry(Repository registry, RepositoryContext registryContext,
                                     String path, String target, boolean remote, String author) throws RepositoryException {
        //persist mount details
        Resource r = new ResourceImpl();
        
        String relativePath;
        
        if (remote) {
            relativePath = path;
        } else {
            relativePath = getRelativePath(registryContext, path);
        }
        
        r.addProperty("path", relativePath);
        r.addProperty("target", getRelativePath(registryContext, target));
        r.addProperty("author", author);
        r.setMediaType(InternalConstants.MOUNT_MEDIA_TYPE);
        
        String mountPath = RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH + RepositoryConstants.SYSTEM_MOUNT_PATH + "/" + relativePath.replace("/", "-");
        
        if (!registry.resourceExists(mountPath)) {
            registry.put(mountPath, r);
        }
    }
    
    /**
     * Gets the resource with sufficient data to differentiate it from another resource. This would
     * populate a {@link ResourceImpl} with the <b>path</b>, <b>name</b> and <b>path identifier</b>
     * of a resource.
     *
     * @param path        the path of the resource.
     * @param resourceDAO the resource data access object to use.
     * @param versioned   whether version or not.
     *
     * @return the resource with minimum data.
     * @throws RepositoryException if an error occurs while retrieving resource data.
     */
    public static ResourceImpl getResourceWithMinimumData(String path, ResourceDAO resourceDAO, boolean versioned) throws RepositoryException {
        ResourceIDImpl resourceID = resourceDAO.getResourceID(path);
        
        if (resourceID == null) {
            return null;
        }
        
        ResourceImpl resourceImpl;
        
        if (resourceID.isCollection()) {
            resourceImpl = new CollectionImpl();
        } else {
            resourceImpl = new ResourceImpl();
        }
        
        if (versioned) {
            resourceImpl.setVersionNumber(resourceDAO.getVersion(resourceID));
        } else {
            resourceImpl.setName(resourceID.getName());
            resourceImpl.setPathID(resourceID.getPathID());
        }
        
        resourceImpl.setPath(path);
        
        return resourceImpl;
    }
    
    public static RepositoryContext getRepositoryContext(Repository repository) throws RepositoryException {
    	RepositoryContext registryContext = null ;

        if(repository instanceof EmbeddedRepository) {
        	registryContext = ((EmbeddedRepository) repository).getRegistryContext();
        } else {
        	registryContext = RepositoryContext.getBaseInstance();
        }
        
        return registryContext;
    }
    
    /**
     * Set-up the system properties required to access the trust-store in Carbon. This is used in
     * the atom-based Remote Registry implementation.
     */
    public static void setTrustStoreSystemProperties() {
        ServerConfiguration config = ServerConfiguration.getInstance();
        String type = config.getFirstProperty("Security.TrustStore.Type");
        String password = config.getFirstProperty("Security.TrustStore.Password");
        
        String storeFile = new File(config.getFirstProperty("Security.TrustStore.Location")).getAbsolutePath();

        System.setProperty("javax.net.ssl.trustStore", storeFile);
        System.setProperty("javax.net.ssl.trustStoreType", type);
        System.setProperty("javax.net.ssl.trustStorePassword", password);
    }
    
    public static boolean isRepositoryReadOnly(Repository repository) throws RepositoryException {
        String repositoryWriteModeProperty = System.getProperty(ServerConstants.REPO_WRITE_MODE);
        
        if (repositoryWriteModeProperty != null) {
            return !(repositoryWriteModeProperty.equals("true"));
        }
        
        ServerConfiguration serverConfig = CarbonUtils.getServerConfiguration();

        String isRegistryReadOnly = serverConfig.getFirstProperty("Registry.ReadOnly");
        RepositoryService registryService = repository.getRepositoryService();
        RepositoryContext registryContext = getRepositoryContext(repository);
        
        if (isRegistryReadOnly == null) {
            if (registryService != null) {
                return registryService.isReadOnly();
            } else if(registryContext != null) {
            	return registryContext.isReadOnly();
            }
        } else {
        	return isRegistryReadOnly.equalsIgnoreCase(Boolean.TRUE.toString());
        }
        
        return false ;
    }
    
    public static String getExtensionLibDirectoryPath() {
        CarbonContext carbonContext = CarbonContext.getCurrentContext();
        int tempTenantId = carbonContext.getTenantId();
        
        return ((tempTenantId != MultitenantConstants.INVALID_TENANT_ID &&
        		tempTenantId != MultitenantConstants.SUPER_TENANT_ID) ?
                (CarbonUtils.getCarbonTenantsDirPath() + File.separator +
                        carbonContext.getTenantId()) :
                (CarbonUtils.getCarbonHome() + File.separator + "repository" + File.separator +
                        "deployment" + File.separator + "server")) +
                File.separator + "registryextensions";
    }
    
    /**
     * Load the class with the given name
     *
     * @param name name of the class
     *
     * @return java class
     * @throws ClassNotFoundException if the class does not exists in the classpath
     */
    public static Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            return Class.forName(name);
        } catch(ClassNotFoundException e) {
            File extensionLibDirectory = new File(getExtensionLibDirectoryPath());
            if (extensionLibDirectory.exists() && extensionLibDirectory.isDirectory()) {
                File[] files = extensionLibDirectory.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name != null && name.endsWith(".jar");
                    }
                });
                if (files != null && files.length > 0) {
                    List<URL> urls = new ArrayList<URL>(files.length);
                    for(File file : files) {
                        try {
                            urls.add(file.toURI().toURL());
                        } catch (MalformedURLException ignore) { }
                    }
                    ClassLoader origTCCL = Thread.currentThread().getContextClassLoader();
                    try {
                        ClassLoader cl = new URLClassLoader(urls.toArray(new URL[urls.size()]),
                                RepositoryUtils.class.getClassLoader());
                        return cl.loadClass(name);
                    } finally {
                        Thread.currentThread().setContextClassLoader(origTCCL);
                    }
                }
            }
            throw e;
        }
    }
}
