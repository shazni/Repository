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

package org.wso2.carbon.repository.core;

import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.cache.Cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.repository.api.Activity;
import org.wso2.carbon.repository.api.Collection;
import org.wso2.carbon.repository.api.Repository;
import org.wso2.carbon.repository.api.RepositoryConstants;
import org.wso2.carbon.repository.api.RepositoryService;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.ResourcePath;
import org.wso2.carbon.repository.api.exceptions.RepositoryErrorCodes;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.exceptions.RepositoryResourceNotFoundException;
import org.wso2.carbon.repository.api.handlers.Handler;
import org.wso2.carbon.repository.api.handlers.HandlerContext;
import org.wso2.carbon.repository.core.caching.CacheResource;
import org.wso2.carbon.repository.core.caching.RepositoryCacheKey;
import org.wso2.carbon.repository.core.config.DataBaseConfiguration;
import org.wso2.carbon.repository.core.config.Mount;
import org.wso2.carbon.repository.core.config.RemoteConfiguration;
import org.wso2.carbon.repository.core.config.RepositoryContext;
import org.wso2.carbon.repository.core.exceptions.RepositoryInitException;
import org.wso2.carbon.repository.core.exceptions.RepositoryServerContentException;
import org.wso2.carbon.repository.core.handlers.HandlerLifecycleManager;
import org.wso2.carbon.repository.core.handlers.HandlerManager;
import org.wso2.carbon.repository.core.handlers.builtin.SimulationFilter;
import org.wso2.carbon.repository.core.queries.QueryProcessorManager;
import org.wso2.carbon.repository.core.statistics.DBQueryStatisticsLog;
import org.wso2.carbon.repository.core.statistics.StatisticsRecord;
import org.wso2.carbon.repository.core.utils.DumpReader;
import org.wso2.carbon.repository.core.utils.InternalConstants;
import org.wso2.carbon.repository.core.utils.InternalUtils;
import org.wso2.carbon.repository.core.utils.VersionedPath;
import org.wso2.carbon.repository.spi.ResourceActivity;
import org.wso2.carbon.repository.spi.dao.LogsDAO;
import org.wso2.carbon.repository.spi.dao.ResourceDAO;
import org.wso2.carbon.repository.spi.dataaccess.DataAccessManager;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

/**
 * This is a core class of the Embedded JDBC Based implementation of the Registry. This will be used
 * mostly as the back-end by other Registry implementations. This can use either an in-memory
 * database or a external database configured using data source.
 */
public class EmbeddedRepository implements Repository {

    private static final Log log = LogFactory.getLog(EmbeddedRepository.class);

    private static final Log dbQueryLog = DBQueryStatisticsLog.getLog();

    private static List<String> statEnabledOperations = new LinkedList<String>();

    private DataAccessManager dataAccessManager = null;

    private static ExecutorService executor = null;

    /**
     * Repository instance. This is used to handle basic resource storage operations of current
     * versions.
     */
    private ResourceStorer repository;
    
    /**
     * VersionRepository instance. This is used to handle resource storage operations of archived
     * (old) versions.
     */
    private VersionResourceStorer versionRepository;

    /**
     * This is used to execute custom queries.
     */
    private QueryProcessorManager queryProcessorManager;

    /**
     * This is used to interact with the database layer to perform activity logging related
     * operations.
     */
    private LogsDAO logsDAO = null;

    /**
     * The URL of the WS-Eventing Service
     */
    private String defaultEventingServiceURL;

    /**
     * Dictionary of URLs of the WS-Eventing Services
     */
    private Map<String, String> eventingServiceURLs = new TreeMap<String, String>();

    /**
     * Reference to the RegistryContext instance.
     */
    private RepositoryContext registryContext;
    
    /** 
     * RegistryService inside all the registry
     */
    private RepositoryService repositoryService = null;
    
    private String resourceMediaTypes = null;
    private String collectionMediaTypes = null;
    private String customUIMediaTypes = null;
    
    private boolean embeddedRegistryInitialized = false ;

    private Map<String, String> cacheIds = new HashMap<String, String>();
    private Map<String, DataBaseConfiguration> dbConfigs = new HashMap<String, DataBaseConfiguration>();
    private Map<String, String> pathMap = new HashMap<String, String>();
    
    private int tenantId = MultitenantConstants.INVALID_TENANT_ID; 
        
    /**
     * functionality related to chroot.
     */
    private ChrootWrapper chrootWrapper;
    private String userName ;
    private String concatenatedChroot ;

    static {
        if (dbQueryLog.isDebugEnabled()) {
            initializeStatisticsLogging();
        }
        String property = System.getProperty("carbon.registry.statistics.operations");
        if (property != null) {
            statEnabledOperations.addAll(Arrays.asList(property.split(",")));
        }
    }

    private static synchronized void initializeStatisticsLogging() {
        if (executor != null) {
            return;
        }
        executor = Executors.newSingleThreadExecutor();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                executor.shutdownNow();
            }
        });
    }

    /**
     * Default constructor. Embedded registry should be configured using the {@link #configure}
     * method, if it is instantiated using this constructor.
     */
    public EmbeddedRepository() {
    }

    /**
     * Constructs a Embedded registry to be used with secure registries. Default authorizations will
     * be applied on the resources created using this registry.
     *
     * @param registryContext RegistryContext containing our configuration and data access manager.
     *                        Note that this may or may not be the same data source as the User
     *                        Manager.
     * @param realmService    User manager realm service handle authorizations.
     *
     * @throws RepositoryException If something went wrong
     */
    public EmbeddedRepository(RepositoryContext registryContext, RepositoryService registryService) throws RepositoryException {
        this.registryContext = registryContext;
        this.dataAccessManager = registryContext.getDataAccessManager();
        this.logsDAO = dataAccessManager.getDAOManager().getLogsDAO();
        this.repositoryService = registryService;
    }

    /**
     * Configures and initiates the Embedded registry with a (new) data source and a realm. This is
     * useful for changing underlying databases at run-time.
     *
     * @param dataAccessManager the data access manager to use
     * @param realmService      the User manager realm service handle authorizations.
     *
     * @throws RepositoryException If something went wrong while init
     */
    public void configure(DataAccessManager dataAccessManager) throws RepositoryException {
        this.dataAccessManager = dataAccessManager;
        this.logsDAO = dataAccessManager.getDAOManager().getLogsDAO();
    }
    
	public ResourceStorer getRepository() {
		return repository;
	}
	
    public void setTenantId(int tenantId) {
    	this.tenantId = tenantId ;
    }
    
    public void setChRoot(String chroot) {
    	concatenatedChroot = chroot ;
    }
    
    public String getConcatenatedRoot() {
    	return concatenatedChroot ;
    }

    public RepositoryContext getRegistryContext() {
        beginDBQueryLog(2);
        HandlerContext context = new HandlerContext(this);
        
        // We need to set the path of the registry that is making the request for the registry
        // context so that the handler manager can figure out which handler to invoke. In here,
        // we use the chroot of the registry as the path.
        String chroot = CurrentContext.getChroot();
        
        if (chroot == null) {
            chroot = "/";
        } else if (!chroot.endsWith("/")) {
            chroot += "/";
        }
        
        context.setResourcePath(new ResourcePath(chroot));
        
        RepositoryContext output = ((HandlerLifecycleManager) registryContext.getHandlerManager()).getRegistryContext(context);
        endDBQueryLog(2);
        
        if (output != null) {
            return output;
        }
        
        return registryContext;
    }

    public void setUserName(String userName) {
    	this.userName = userName ;
    }

    @Override
    public void beginTransaction() throws RepositoryException {
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
    	try {
    		setContextInformation();
	        beginDBQueryLog(3);
	        dataAccessManager.getTransactionManager().beginTransaction();
    	} finally {
    		clearContextInformation();
    	}
    }

    @Override
    public void rollbackTransaction() throws RepositoryException {
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
    	try {
    		setContextInformation();
	        dataAccessManager.getTransactionManager().rollbackTransaction();
	        endDBQueryLog(3);
    	} finally {
    		clearContextInformation();
    	}
    }

    @Override
    public void commitTransaction() throws RepositoryException {
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
    	try { 
    		setContextInformation();
	        dataAccessManager.getTransactionManager().commitTransaction();
	        endDBQueryLog(3);
    	} finally {
    		clearContextInformation();
    	}
    }
    
    @Override
    public Resource newResource() throws RepositoryException {
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
        try {
        	setContextInformation();
            beginDBQueryLog(2);
            ResourceImpl resource = new ResourceImpl();
            resource.setAuthorUserName(CurrentContext.getUser());
            return resource;
        } finally {
            endDBQueryLog(2);
            clearContextInformation();
        }
    }

    @Override
    public Collection newCollection() throws RepositoryException {
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
        try {
        	setContextInformation();
            beginDBQueryLog(2);
            CollectionImpl coll = new CollectionImpl();
            coll.setAuthorUserName(CurrentContext.getUser());
            return coll;
        } finally {
            endDBQueryLog(2);
            clearContextInformation();
        }
    }

    @Override
    public Resource getMetaData(String path) throws RepositoryException {
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
        boolean transactionSucceeded = false;
        
        try {
        	setContextInformation();
        	
        	path = chrootWrapper.getInPath(path);
        	
            beginTransaction();

            ResourcePath resourcePath = new ResourcePath(path);
            Resource resource;

            VersionedPath versionedPath = InternalUtils.getVersionedPath(resourcePath);
            if (versionedPath.getVersion() == -1) {
                resource = repository.getMetaData(resourcePath.getPath());
            } else {
                resource = versionRepository.getMetaData(versionedPath);
            }

            if (resource == null) {
                throw new RepositoryResourceNotFoundException(path, RepositoryErrorCodes.RESOURCE_PATH_ERROR);
            }

            transactionSucceeded = true;
            
            if (resource != null) {
                ((ResourceImpl) resource).setTenantId(tenantId);
                resource = chrootWrapper.getOutResource(resource);
            }

            return resource;
        } finally {
            if (transactionSucceeded) {
                commitTransaction();
            } else {
                rollbackTransaction();
            }
            clearContextInformation();
        }
    }

    @Override
    public String put(String suggestedPath, Resource resource) throws RepositoryException {
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
        if (InternalUtils.isRepositoryReadOnly(this)) {
            if (log.isTraceEnabled()) {
                log.trace("Cannot continue the operation put, the coreRegistry is read-only");
            }
            return suggestedPath;
        }

        boolean transactionSucceeded = false;
        HandlerContext context = new HandlerContext(this);
        boolean mustPutChild = false;
        try {
        	setContextInformation();
        	
            suggestedPath = chrootWrapper.getInPath(suggestedPath);
        	
            beginTransaction();

            ResourcePath resourcePath = new ResourcePath(suggestedPath);

            context.setResourcePath(resourcePath);
            context.setResource(resource);

            if (repository.resourceExists(suggestedPath)) {
                context.setOldResource(repository.get(suggestedPath));
            }

            if (!RepositoryConstants.ROOT_PATH.equals(resourcePath.getPath())) {
                mustPutChild = true;
                registryContext.getHandlerManager().putChild(context);
            }

            registryContext.getHandlerManager().put(context);

            if (!SimulationFilter.isSimulation()) {
                String actualPath = context.getActualPath();

                if (!context.isProcessingComplete()) {
                    ((ResourceImpl) resource).prepareContentForPut();

                    actualPath = suggestedPath;
                    try {
                        CurrentContext.setAttribute(ResourceStorer.IS_LOGGING_ACTIVITY, context.isLoggingActivity());
                        repository.put(suggestedPath, resource);
                    } finally {
                        CurrentContext.removeAttribute(ResourceStorer.IS_LOGGING_ACTIVITY);
                    }
                }

                if (mustPutChild) {
                    registryContext.getHandlerManager(HandlerLifecycleManager.COMMIT_HANDLER_PHASE).putChild(context);
                }

                registryContext.getHandlerManager(HandlerLifecycleManager.COMMIT_HANDLER_PHASE).put(context);
                transactionSucceeded = true;

                if (actualPath == null) {
                    return chrootWrapper.getOutPath(suggestedPath);
                } else {
                    return chrootWrapper.getOutPath(actualPath);
                }
            } else {
                return chrootWrapper.getOutPath(suggestedPath);
            }
        } finally {
            if (transactionSucceeded) {
                commitTransaction();
            } else {
                try {
                    if (mustPutChild) {
                        registryContext.getHandlerManager(
                                HandlerLifecycleManager.ROLLBACK_HANDLER_PHASE).putChild(context);
                    }
                    registryContext.getHandlerManager(
                            HandlerLifecycleManager.ROLLBACK_HANDLER_PHASE).put(context);
                } finally {
                    rollbackTransaction();
                }
            }
            clearContextInformation();
        }
    }

    @Override
    public String importResource(String suggestedPath, String sourceURL, Resource metaResource)
            throws RepositoryException {
    	
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
        if (InternalUtils.isRepositoryReadOnly(this)) {
            if (log.isTraceEnabled()) {
                log.trace("Cannot continue the operation import resource, the coreRegistry is " +
                        "read-only");
            }
            return suggestedPath;
        }
    	
        boolean transactionSucceeded = false;
        HandlerContext context = new HandlerContext(this);
        HandlerContext importChildContext = new HandlerContext(this);
        try {
        	setContextInformation();
        	
            suggestedPath = chrootWrapper.getInPath(suggestedPath);
        	
            beginTransaction();

            ResourcePath resourcePath = new ResourcePath(suggestedPath);

            importChildContext.setResourcePath(resourcePath);

            context.setResourcePath(resourcePath);
            context.setSourceURL(sourceURL);
            context.setResource(metaResource);

            if (repository.resourceExists(suggestedPath)) {
                Resource resource = repository.get(suggestedPath);
                importChildContext.setOldResource(resource);
                context.setOldResource(resource);
            }

            registryContext.getHandlerManager().importChild(importChildContext);
            registryContext.getHandlerManager().importResource(context);
            if (!SimulationFilter.isSimulation()) {
                String savedPath = context.getActualPath();

                if (!context.isProcessingComplete()) {
                    savedPath = suggestedPath;

                    // if some handlers have updated the meta data *without completing the request* we should
                    // capture the updated meta data here.
                    if (context.getResource() != null) {
                        metaResource = context.getResource();
                    }

                    try {
                        CurrentContext.setAttribute(ResourceStorer.IS_LOGGING_ACTIVITY,
                                context.isLoggingActivity());
                        repository.importResource(resourcePath.getPath(), sourceURL, metaResource);
                    } finally {
                        CurrentContext.removeAttribute(ResourceStorer.IS_LOGGING_ACTIVITY);
                    }
                }

                if (savedPath != null) {
                    if (context.isLoggingActivity()) {
                        registryContext.getLogWriter().addLog(
                                savedPath, CurrentContext.getUser(), Activity.UPDATE, null);
                    }
                }
                
                registryContext.getHandlerManager(HandlerLifecycleManager.COMMIT_HANDLER_PHASE).importChild(importChildContext);
                registryContext.getHandlerManager(HandlerLifecycleManager.COMMIT_HANDLER_PHASE).importResource(context);

                transactionSucceeded = true;

                if (savedPath != null) {
                    return chrootWrapper.getOutPath(savedPath);
                }
            }
            return chrootWrapper.getOutPath(suggestedPath);
        } finally {
            if (transactionSucceeded) {
                commitTransaction();
            } else {
                try {
                    registryContext.getHandlerManager(
                            HandlerLifecycleManager.ROLLBACK_HANDLER_PHASE).importChild(
                            importChildContext);
                    registryContext.getHandlerManager(
                            HandlerLifecycleManager.ROLLBACK_HANDLER_PHASE).importResource(context);
                } finally {
                    rollbackTransaction();
                }
            }
            clearContextInformation();
        }
    }

    @Override
    public void delete(String path) throws RepositoryException {
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
        if (log.isTraceEnabled()) {
            log.trace("Preparing operation import resource. path: " + path + ".");
        }
        
        if (InternalUtils.isRepositoryReadOnly(this)) {
            if (log.isTraceEnabled()) {
                log.trace("Cannot continue the operation delete, the coreRegistry is read-only");
            }
            return;
        }
    	
        boolean transactionSucceeded = false;
        HandlerContext context = new HandlerContext(this);
        try {
        	setContextInformation();
        	
            path = chrootWrapper.getInPath(path) ;
        	
            beginTransaction();

            ResourcePath resourcePath = new ResourcePath(path);
            context.setResourcePath(resourcePath);

            registryContext.getHandlerManager().delete(context);
            if (!SimulationFilter.isSimulation()) {
                if (!context.isProcessingComplete() &&
                        repository.resourceExists(resourcePath.getPath())) {
                    repository.delete(resourcePath.getPath());
                    if (context.isLoggingActivity()) {
                        registryContext.getLogWriter().addLog(
                                resourcePath.getPath(), CurrentContext.getUser(),
                                Activity.DELETE_RESOURCE,
                                null);
                    }
                }
                registryContext.getHandlerManager(
                        HandlerLifecycleManager.COMMIT_HANDLER_PHASE).delete(context);

                transactionSucceeded = true;
            }
        } finally {
            if (transactionSucceeded) {
                commitTransaction();
            } else {
                try {
                    registryContext.getHandlerManager(
                            HandlerLifecycleManager.ROLLBACK_HANDLER_PHASE).delete(context);
                } finally {
                    rollbackTransaction();
                }
            }
            clearContextInformation();
        }

    }

    @Override
    public String rename(String currentPath, String newName) throws RepositoryException {
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
        if (log.isTraceEnabled()) {
            log.trace("Preparing operation rename. source: " + currentPath + ", target: " + newName + ".");
        }

        if (InternalUtils.isRepositoryReadOnly(this)) {
            if (log.isTraceEnabled()) {
                log.trace("Cannot continue the operation rename, the coreRegistry is read-only");
            }
            return currentPath;
        }
    	
        boolean transactionSucceeded = false;
        HandlerContext context = new HandlerContext(this);
        try {
        	setContextInformation();
        	
        	currentPath = chrootWrapper.getInPath(currentPath);
            if (newName.startsWith(RepositoryConstants.ROOT_PATH)) {
            	newName = chrootWrapper.getInPath(newName);
            }
        	
            beginTransaction();

            ResourcePath resourcePath = new ResourcePath(currentPath);
            context.setSourcePath(currentPath);
            context.setInstanceId(newName);
            String newPath = registryContext.getHandlerManager().rename(context);
            
            if (!SimulationFilter.isSimulation()) {
                if (!context.isProcessingComplete()) {
                    try {
                        CurrentContext.setAttribute(ResourceStorer.IS_LOGGING_ACTIVITY, context.isLoggingActivity());
                        newPath = repository.rename(resourcePath, newName);
                    } finally {
                        CurrentContext.removeAttribute(ResourceStorer.IS_LOGGING_ACTIVITY);
                    }
                }
                if (context.isLoggingActivity()) {
                    registryContext.getLogWriter().addLog(newPath, CurrentContext.getUser(), Activity.RENAME, currentPath);
                }

                registryContext.getHandlerManager(HandlerLifecycleManager.COMMIT_HANDLER_PHASE).rename(context);
                
                transactionSucceeded = true;
            }
            
            return chrootWrapper.getOutPath(newPath) ;
        } finally {
            if (transactionSucceeded) {
                commitTransaction();
            } else {
                try {
                    registryContext.getHandlerManager(
                            HandlerLifecycleManager.ROLLBACK_HANDLER_PHASE).rename(context);
                } finally {
                    rollbackTransaction();
                }
            }
            clearContextInformation();
        }
    }

    @Override
    public String move(String currentPath, String newPath) throws RepositoryException {
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
        if (log.isTraceEnabled()) {
            log.trace("Preparing operation move. " +
                    "source: " + currentPath + ", " +
                    "target: " + newPath + ".");
        }

        if (InternalUtils.isRepositoryReadOnly(this)) {
            if (log.isTraceEnabled()) {
                log.trace("Cannot continue the operation move, the coreRegistry is read-only");
            }
            return currentPath;
        }
    	
        boolean transactionSucceeded = false;
        HandlerContext context = new HandlerContext(this);
        
        try {
        	setContextInformation();
        	
            currentPath = chrootWrapper.getInPath(currentPath) ;
            newPath = chrootWrapper.getInPath(newPath);
        	
            beginTransaction();

            ResourcePath currentResourcePath = new ResourcePath(currentPath);
            context.setSourcePath(currentPath);
            context.setInstanceId(newPath);
            String movedPath = registryContext.getHandlerManager().move(context);
            
            if (!SimulationFilter.isSimulation()) {
                if (!context.isProcessingComplete()) {
                    try {
                        CurrentContext.setAttribute(ResourceStorer.IS_LOGGING_ACTIVITY,
                                context.isLoggingActivity());
                        movedPath = repository.move(currentResourcePath, newPath);
                    } finally {
                        CurrentContext.removeAttribute(ResourceStorer.IS_LOGGING_ACTIVITY);
                    }
                }
                if (context.isLoggingActivity()) {
                    registryContext.getLogWriter().addLog(
                            newPath, CurrentContext.getUser(), Activity.MOVE, currentPath);
                }

                registryContext.getHandlerManager(
                        HandlerLifecycleManager.COMMIT_HANDLER_PHASE).move(context);

                transactionSucceeded = true;
            }
            return chrootWrapper.getOutPath(movedPath);
        } finally {
            if (transactionSucceeded) {
                commitTransaction();
            } else {
                try {
                    registryContext.getHandlerManager(
                            HandlerLifecycleManager.ROLLBACK_HANDLER_PHASE).move(context);
                } finally {
                    rollbackTransaction();
                }
            }
            clearContextInformation();
        }
    }

    @Override
    public String copy(String sourcePath, String targetPath) throws RepositoryException {
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
        if (log.isTraceEnabled()) {
            log.trace("Preparing operation copy. " +
                    "source: " + sourcePath + ", " +
                    "target: " + targetPath + ".");
        }

        if (InternalUtils.isRepositoryReadOnly(this)) {
            if (log.isTraceEnabled()) {
                log.trace("Cannot continue the operation copy, the coreRegistry is read-only");
            }
            return sourcePath;
        }
    	
        boolean transactionSucceeded = false;

        HandlerContext context = new HandlerContext(this);
        try {
        	setContextInformation();

            sourcePath = chrootWrapper.getInPath(sourcePath);
            targetPath = chrootWrapper.getInPath(targetPath);

            beginTransaction();

            ResourcePath sourceResourcePath = new ResourcePath(sourcePath);
            ResourcePath targetResourcePath = new ResourcePath(targetPath);
            context.setSourcePath(sourcePath);
            context.setInstanceId(targetPath);
            String copiedPath = registryContext.getHandlerManager().copy(context);
            if (!SimulationFilter.isSimulation()) {
                if (!context.isProcessingComplete()) {
                    try {
                        CurrentContext.setAttribute(ResourceStorer.IS_LOGGING_ACTIVITY,
                                context.isLoggingActivity());
                        copiedPath = repository.copy(sourceResourcePath, targetResourcePath);
                    } finally {
                        CurrentContext.removeAttribute(ResourceStorer.IS_LOGGING_ACTIVITY);
                    }
                }
                if (context.isLoggingActivity()) {
                    registryContext.getLogWriter().addLog(
                            sourcePath, CurrentContext.getUser(), Activity.COPY, targetPath);
                }

                registryContext.getHandlerManager(
                        HandlerLifecycleManager.COMMIT_HANDLER_PHASE).copy(context);

                transactionSucceeded = true;
            }
            return chrootWrapper.getOutPath(copiedPath);
        } finally {
            if (transactionSucceeded) {
                commitTransaction();
            } else {
                try {
                    registryContext.getHandlerManager(
                            HandlerLifecycleManager.ROLLBACK_HANDLER_PHASE).copy(context);
                } finally {
                    rollbackTransaction();
                }
            }
            clearContextInformation();
        }
    }

    @Override
    public void createVersion(String path) throws RepositoryException {    	
        if (log.isTraceEnabled()) {
            log.trace("Preparing operation create version, " +
                    "path: " + path + ".");
        }

        if (InternalUtils.isRepositoryReadOnly(this)) {
            if (log.isTraceEnabled()) {
                log.trace("Cannot continue the operation create version, the coreRegistry is " +
                        "read-only");
            }
            return;
        }
        
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
        boolean transactionSucceeded = false;

        HandlerContext context = new HandlerContext(this);
        try {
        	setContextInformation();
        	
            path = chrootWrapper.getInPath(path);

            beginTransaction();
            
            ResourcePath resourcePath = new ResourcePath(path);

            context.setResourcePath(resourcePath);
            registryContext.getHandlerManager().createVersion(context);
            if (!SimulationFilter.isSimulation()) {
                if (!context.isProcessingComplete()) {
                    versionRepository.createSnapshot(resourcePath, true, true);
                }
                registryContext.getHandlerManager(
                        HandlerLifecycleManager.COMMIT_HANDLER_PHASE).createVersion(context);

                transactionSucceeded = true;
            }
        } finally {
            if (transactionSucceeded) {
                commitTransaction();
            } else {
                try {
                    registryContext.getHandlerManager(
                            HandlerLifecycleManager.ROLLBACK_HANDLER_PHASE).createVersion(context);
                } finally {
                    rollbackTransaction();
                }
            }
            clearContextInformation();
        }
    }

    @Override
    public String[] getVersions(String path) throws RepositoryException {
        if (log.isTraceEnabled()) {
            log.trace("Preparing operation get versions, " +
                    "path: " + path + ".");
        }
    	
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
        boolean transactionSucceeded = false;
        HandlerContext context = new HandlerContext(this);
        
        try {
        	setContextInformation();
            
            path = chrootWrapper.getInPath(path);

            beginTransaction();

            context.setResourcePath(new ResourcePath(path));
            String[] output = registryContext.getHandlerManager().getVersions(context);
            if (!SimulationFilter.isSimulation()) {
                if (!context.isProcessingComplete()) {
                    output = versionRepository.getVersions(path);
                }
                registryContext.getHandlerManager(
                        HandlerLifecycleManager.COMMIT_HANDLER_PHASE).getVersions(context);

                transactionSucceeded = true;
            }
            return chrootWrapper.getOutPaths(output);
        } finally {
            if (transactionSucceeded) {
                commitTransaction();
            } else {
                try {
                    registryContext.getHandlerManager(
                            HandlerLifecycleManager.ROLLBACK_HANDLER_PHASE).getVersions(context);
                } finally {
                    rollbackTransaction();
                }
            }
            clearContextInformation();
        }
    }

    @Override
    public void restoreVersion(String versionPath) throws RepositoryException {
        if (log.isTraceEnabled()) {
            log.trace("Preparing operation restore version, version path: " + versionPath + ".");
        }
    	
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
        boolean transactionSucceeded = false;
        HandlerContext context = new HandlerContext(this);
        
        try {
        	setContextInformation();

            versionPath = chrootWrapper.getInPath(versionPath);

            beginTransaction();
            
            context.setVersionPath(versionPath);
            ResourcePath versionedResourcePath = new ResourcePath(versionPath);
            String path = versionedResourcePath.getPath();
            
            if (repository.resourceExists(path)) {
                context.setOldResource(repository.get(path));
            }
            
            registryContext.getHandlerManager().restoreVersion(context);
            if (!SimulationFilter.isSimulation()) {
                if (!context.isProcessingComplete()) {
                    if (repository.resourceExists(path)) {
                        repository.prepareVersionRestore(path);
                    }
                    versionRepository.restoreVersion(versionedResourcePath);
                    VersionedPath versionedPath =
                    		InternalUtils.getVersionedPath(versionedResourcePath);
                    if (context.isLoggingActivity()) {
                        registryContext.getLogWriter().addLog(
                                path, CurrentContext.getUser(), Activity.RESTORE,
                                Long.toString(versionedPath.getVersion()));
                    }
                }

                registryContext.getHandlerManager(
                        HandlerLifecycleManager.COMMIT_HANDLER_PHASE).restoreVersion(context);

                transactionSucceeded = true;
            }
        } finally {
            if (transactionSucceeded) {
                commitTransaction();
            } else {
                try {
                    registryContext.getHandlerManager(
                            HandlerLifecycleManager.ROLLBACK_HANDLER_PHASE).restoreVersion(context);
                } finally {
                    rollbackTransaction();
                }
            }
            clearContextInformation();
        }
    }

    @Override
    public Collection executeQuery(String path, Map<?, ?> parameters) throws RepositoryException {
    	
        if (log.isTraceEnabled()) {
            String msg = "Preparing operation execute query, path: " + path + ", values: ";
            Object[] paramValues = parameters.values().toArray();
            StringBuilder sb = new StringBuilder(msg);
            for (int i = 0; i < paramValues.length; i++) {
                String value = (String) paramValues[i];
                sb.append(value);
                if (i != paramValues.length - 1) {
                    sb.append(", ");
                } else {
                    sb.append(".");
                }
            }
            log.trace(sb.toString());
        }
    	
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
        boolean transactionSucceeded = false;
        boolean remote = false;
        if (parameters.get("remote") != null) {
            parameters.remove("remote");
            remote = true;
        }
        
        HandlerContext context = new HandlerContext(this);
        Resource query = null;
        
        try {
        	setContextInformation();
        	
            if (path != null) {
                String newPath = chrootWrapper.getInPath(path);
                if (newPath != null) {
                    path = newPath.replace(RepositoryConstants.GOVERNANCE_REGISTRY_BASE_PATH, RepositoryConstants.CONFIG_REGISTRY_BASE_PATH).replace(
                            RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH, RepositoryConstants.CONFIG_REGISTRY_BASE_PATH);
                    
                    // The '/' path is used in the remote registry case as a workaround, instead of
                    // passing null.
                    if (!path.contains(RepositoryConstants.CONFIG_REGISTRY_BASE_PATH) && !path.equals(chrootWrapper.getInPath(RepositoryConstants.ROOT_PATH))) {
                        log.warn("Running Query in Backwards-Compatible mode. Queries must be " +
                                "stored and accessed from the Configuration System Registry in the new model. Path: " + path);
                    }
                } else {
                    path = null;
                }
            }
        	
            // start the transaction
            beginTransaction();

            Repository systemRegistry = ((EmbeddedRepositoryService) getRepositoryService()).getSystemRepository(CurrentContext.getTenantId(), null);
            
            // we have to get the stored query without checking the user permissions.
            // all query actions are blocked for all users. they are allowed to read the
            // queries, only when executing them.
            if (path != null) {
                String purePath = InternalUtils.getPureResourcePath(path);
                if (systemRegistry.resourceExists(purePath)) {
                    query = systemRegistry.get(purePath);
                    // If no media type was specified, the query should not work at all.
                    // This is also used in the remote registry scenario, where we send '/' as the
                    // query path, when path is null.
                    if (query != null && (query.getMediaType() == null ||
                            query.getMediaType().length() == 0)) {
                        query = null;
                    }
                }
            }

            if (path != null) {
                context.setResourcePath(new ResourcePath(path));
            }
            
            context.setResource(query);
            context.setQueryParameters(parameters);
            Collection output = registryContext.getHandlerManager().executeQuery(context);
            
            if (!SimulationFilter.isSimulation()) {
                if (!context.isProcessingComplete()) {
                    if (query == null) {
                        query = newResource();
                        String mediaType = (String) parameters.get("mediaType");
                        query.setMediaType(mediaType != null ? mediaType : RepositoryConstants.SQL_QUERY_MEDIA_TYPE);
                    }

                    Collection temp = queryProcessorManager.executeQuery(this, query, parameters);
                    Set<String> results = new LinkedHashSet<String>();
                    
                    if (output != null) {
                        String[] children = output.getChildPaths();
                        if (children != null) {
                            for (String child : children) {
                                if (child != null && (remote || resourceExists(child))) {
                                    results.add(child);
                                }
                            }
                        }

                        if (temp != null) {
                            children = temp.getChildPaths();
                            if (children != null) {
                                for (String child : children) {
                                    if (child != null && (remote || resourceExists(child))) {
                                        results.add(child);
                                    }
                                }
                            }
                        } else {
                            temp = output;
                        }
                        temp.setContent(results.toArray(new String[results.size()]));
                    }
                    output = temp;
                }

                registryContext.getHandlerManager(HandlerLifecycleManager.COMMIT_HANDLER_PHASE).executeQuery(context);

                transactionSucceeded = true;
            }
            
            if (output != null) {
                ResourceImpl resourceImpl = (ResourceImpl) output;
                resourceImpl.setTenantId(tenantId);

                output = chrootWrapper.filterSearchResult(output);
                output = (Collection) chrootWrapper.getOutResource(output);
            }
                 
            return output;
        } finally {
            if (transactionSucceeded) {
                commitTransaction();
            } else {
                try {
                    registryContext.getHandlerManager(HandlerLifecycleManager.ROLLBACK_HANDLER_PHASE).executeQuery(context);
                } finally {
                    rollbackTransaction();
                }
            }
            clearContextInformation();
        }
    }

    @Override
    public Activity[] getLogs(String resourcePath, int action, String userName, Date from,
                              Date to, boolean recentFirst) throws RepositoryException {
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
        boolean transactionSucceeded = false;
        try {
        	setContextInformation();
        	
            resourcePath = chrootWrapper.getInPath(resourcePath);
        	
            // start the transaction
            beginTransaction();

            List<Activity> logEntryList = logsDAO.getLogs(resourcePath, action, userName, from, to, recentFirst);

            // We go on two iterations to avoid null values in the following array. Need better way
            // in a single iteration
            for (int i = logEntryList.size() - 1; i >= 0; i--) {
            	ResourceActivity logEntry = (ResourceActivity) logEntryList.get(i);
                if (logEntry == null) {
                    logEntryList.remove(i);
                }
            }

            ResourceActivity[] logEntries = new ResourceActivity[logEntryList.size()];
            for (int i = 0; i < logEntryList.size(); i++) {
                logEntries[i] = (ResourceActivity) logEntryList.get(i);
            }

            transactionSucceeded = true;

            return chrootWrapper.fixLogEntries(logEntries); 
        } finally {
            if (transactionSucceeded) {
                commitTransaction();
            } else {
                rollbackTransaction();
            }
            clearContextInformation();
        }
    }

    @Override
    public Collection searchContent(String keywords) throws RepositoryException {
        if (log.isTraceEnabled()) {
            log.trace("Preparing operation search content, " +
                    "keywords: " + keywords + ".");
        }
    	
    	if(!embeddedRegistryInitialized) {
    		init();
    	}

        return null;
    }

    @Override
    public void createLink(String path, String target) throws RepositoryException {
        if (log.isTraceEnabled()) {
            log.trace("Preparing operation create link, path: " + path + ", target: " + target + ".");
        }

        if (InternalUtils.isRepositoryReadOnly(this)) {
            if (log.isTraceEnabled()) {
                log.trace("Cannot continue the operation create link, the coreRegistry is read-only");
            }
            return;
        }
    	
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
        boolean transactionSucceeded = false;
        HandlerContext context = new HandlerContext(this);
        
        try {
        	setContextInformation();
        	
        	path = chrootWrapper.getInPath(path);
        	target = chrootWrapper.getInPath(target);
        	
            beginTransaction();

            if (path.equals(target)) {
                String msg = "Path and target are same, path = target = " + path + ". You can't create a symbolic link to itself.";
                log.error(msg);
                throw new RepositoryServerContentException(msg);
            }

            Resource oldResource = repository.getMetaData(target);
            Resource resource;
            
            if (repository.resourceExists(path)) {
                resource = repository.get(path);
                resource.addProperty(InternalConstants.REGISTRY_EXISTING_RESOURCE, "true");
            } else if (oldResource != null) {
                if (oldResource instanceof Collection) {
                    resource = new CollectionImpl();
                } else {
                    resource = new ResourceImpl();
                }
            } else {
                resource = new CollectionImpl();
            }
            
            resource.addProperty(RepositoryConstants.REGISTRY_NON_RECURSIVE, "true");
            resource.addProperty(InternalConstants.REGISTRY_LINK_RESTORATION,
                    path + RepositoryConstants.URL_SEPARATOR + target + RepositoryConstants.URL_SEPARATOR + CurrentContext.getUser());
            resource.setMediaType(InternalConstants.LINK_MEDIA_TYPE);
            
            try {
                CurrentContext.setAttribute(ResourceStorer.IS_LOGGING_ACTIVITY, false);
                repository.put(path, resource);
            } finally {
                CurrentContext.removeAttribute(ResourceStorer.IS_LOGGING_ACTIVITY);
            }

            HandlerManager hm = registryContext.getHandlerManager();

            ResourcePath resourcePath = new ResourcePath(path);
            context.setResourcePath(resourcePath);
            context.setInstanceId(target);
            hm.createLink(context);
            
            if (!SimulationFilter.isSimulation()) {
                if (!context.isProcessingComplete()) {
                    InternalUtils.registerHandlerForSymbolicLinks(this, path, target, CurrentContext.getUser());

                    String author = CurrentContext.getUser();
                    InternalUtils.addMountEntry(InternalUtils.getSystemRegistry(this), registryContext, path, target, false, author);

                    if (context.isLoggingActivity()) {
                        registryContext.getLogWriter().addLog(path, CurrentContext.getUser(), Activity.CREATE_SYMBOLIC_LINK, target);
                    }
                }

                registryContext.getHandlerManager(HandlerLifecycleManager.COMMIT_HANDLER_PHASE).createLink(context);

                transactionSucceeded = true;
            }
        } finally {
            if (transactionSucceeded) {
                commitTransaction();
            } else {
                try {
                    registryContext.getHandlerManager(HandlerLifecycleManager.ROLLBACK_HANDLER_PHASE).createLink(context);
                } finally {
                    rollbackTransaction();
                }
            }
            clearContextInformation();
        }
    }

    @Override
    public void createLink(String path, String instanceId, String targetSubPath) throws RepositoryException {
    	
        if (log.isTraceEnabled()) {
            log.trace("Preparing operation create link, path: " + path + ", instanceId(target): " + instanceId + ".");
        }

        if (InternalUtils.isRepositoryReadOnly(this)) {
            if (log.isTraceEnabled()) {
                log.trace("Cannot continue the operation create link, the core repository is read-only");
            }
            return;
        }
    	
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
        boolean transactionSucceeded = false;
        HandlerContext context = new HandlerContext(this);
        
        try {
        	setContextInformation();
        	
        	path = chrootWrapper.getInPath(path);

            beginTransaction();

            Resource resource;

            if (repository.resourceExists(path)) {
                resource = repository.get(path);
                resource.addProperty(InternalConstants.REGISTRY_EXISTING_RESOURCE, "true");
            } else {
                resource = new CollectionImpl();
            }
            
            resource.addProperty(RepositoryConstants.REGISTRY_NON_RECURSIVE, "true");
            resource.addProperty(InternalConstants.REGISTRY_LINK_RESTORATION, path + RepositoryConstants.URL_SEPARATOR + 
            		instanceId + RepositoryConstants.URL_SEPARATOR + targetSubPath + RepositoryConstants.URL_SEPARATOR + CurrentContext.getUser());
            resource.setMediaType(InternalConstants.LINK_MEDIA_TYPE);
            
            try {
                CurrentContext.setAttribute(ResourceStorer.IS_LOGGING_ACTIVITY, false);
                repository.put(path, resource);
            } finally {
                CurrentContext.removeAttribute(ResourceStorer.IS_LOGGING_ACTIVITY);
            }
            
            HandlerManager hm = registryContext.getHandlerManager();

            ResourcePath resourcePath = new ResourcePath(path);
            
            context.setResourcePath(resourcePath);
            context.setInstanceId(instanceId);
            context.setTargetSubPath(targetSubPath);
            
            hm.createLink(context);
            
            if (!SimulationFilter.isSimulation()) {
                if (!context.isProcessingComplete()) {
                    InternalUtils.registerHandlerForRemoteLinks(this, path, instanceId, targetSubPath, CurrentContext.getUser());

                    String author = CurrentContext.getUser();
                    InternalUtils.addMountEntry(InternalUtils.getSystemRegistry(this), registryContext, path, instanceId, targetSubPath, author);

                    if (context.isLoggingActivity()) {
                        registryContext.getLogWriter().addLog(path, CurrentContext.getUser(), Activity.CREATE_REMOTE_LINK, instanceId + ";" + targetSubPath);
                    }
                }

                registryContext.getHandlerManager(HandlerLifecycleManager.COMMIT_HANDLER_PHASE).createLink(context);

                transactionSucceeded = true;
            }
        } finally {
            if (transactionSucceeded) {
                commitTransaction();
            } else {
                try {
                    registryContext.getHandlerManager(
                            HandlerLifecycleManager.ROLLBACK_HANDLER_PHASE).createLink(context);
                } finally {
                    rollbackTransaction();
                }
            }
            clearContextInformation();
        }
    }

    @Override
    public void removeLink(String path) throws RepositoryException {
        if (log.isTraceEnabled()) {
            log.trace("Preparing operation remove link, path: " + path + ".");
        }

        if (InternalUtils.isRepositoryReadOnly(this)) {
            if (log.isTraceEnabled()) {
                log.trace("Cannot continue the operation remove link, the coreRegistry is read-only");
            }
            return;
        }
    	
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
        boolean transactionSucceeded = false;
        HandlerContext context = new HandlerContext(this);
        
        try {
        	setContextInformation();
        	
        	path = chrootWrapper.getInPath(path);

            beginTransaction();

            ResourcePath resourcePath = new ResourcePath(path);
            context.setResourcePath(resourcePath);
            registryContext.getHandlerManager().removeLink(context);

            Handler handlerToRemove = (Handler) context.getProperty(InternalConstants.SYMLINK_TO_REMOVE_PROPERTY_NAME);
            if (handlerToRemove != null) {
                registryContext.getHandlerManager().removeHandler(handlerToRemove, HandlerLifecycleManager.TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE);
            }

            if (!SimulationFilter.isSimulation()) {
                if (!context.isProcessingComplete()) {
                    try {
                        Collection mountCollection = (Collection) getResource(InternalUtils.getAbsolutePath(registryContext,
                                        RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH + RepositoryConstants.SYSTEM_MOUNT_PATH));
                        String[] mountResources = mountCollection.getChildPaths();
                        Resource resource = null;
                        
                        for (String mountResource : mountResources) {
                            String mountResName = mountResource.substring(mountResource.lastIndexOf('/') + 1);
                            String relativePath = InternalUtils.getRelativePath(registryContext, path);
                            
                            if (mountResName.equals(relativePath.replace("/", "-"))) {
                                resource = getResource(mountResource);
                                break;
                            }
                        }

                        if (resource == null) {
                            String msg = "Couldn't find the mount point to remove. ";
                            log.error(msg);
                            throw new RepositoryException(msg);
                        }

                        InternalUtils.getSystemRegistry(this).delete(resource.getPath());
                    } catch (RepositoryResourceNotFoundException ignored) {
                        // There can be situations where the mount resource is not found. In that
                        // case, we can simply ignore this exception being thrown. An example of
                        // such a situation is found in CARBON-12002.
                    }
                    
                    if (repository.resourceExists(path)) {
                        Resource r = repository.get(path);
                        if (!Boolean.toString(true).equals(
                                r.getPropertyValue(InternalConstants.REGISTRY_EXISTING_RESOURCE))) {
                            repository.delete(path);
                        }
                    }

                    if (context.isLoggingActivity()) {
                        registryContext.getLogWriter().addLog(path, CurrentContext.getUser(), Activity.REMOVE_LINK, null);
                    }
                }

                registryContext.getHandlerManager(
                            HandlerLifecycleManager.COMMIT_HANDLER_PHASE).removeLink(context);

                transactionSucceeded = true;
            }
        } finally {
            if (transactionSucceeded) {
                commitTransaction();
            } else {
                try {
                    registryContext.getHandlerManager(
                            HandlerLifecycleManager.ROLLBACK_HANDLER_PHASE).removeLink(context);
                } finally {
                    rollbackTransaction();
                }
            }
            clearContextInformation();
        }
    }

    @Override
    public void restore(String path, Reader reader) throws RepositoryException {
        if (log.isTraceEnabled()) {
            log.trace("Preparing operation restore dump, path: " + path + ".");
        }

        if (InternalUtils.isRepositoryReadOnly(this)) {
            log.warn("Cannot continue the operation restore dump, the coreRegistry is read-only");
            return;
        }
    	
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
        boolean transactionSucceeded = false;
        HandlerContext context = new HandlerContext(this);
        
        try {
        	setContextInformation();
        	
        	path = chrootWrapper.getInPath(path);

            beginTransaction();

            context.setDumpingReader(reader);
            context.setResourcePath(new ResourcePath(path));
            registryContext.getHandlerManager().restore(context);
            
            if (!SimulationFilter.isSimulation()) {
                if (!context.isProcessingComplete()) {
                    try {
                        CurrentContext.setAttribute(ResourceStorer.IS_LOGGING_ACTIVITY,
                                context.isLoggingActivity());
                        repository.restore(path, reader);
                    } finally {
                        CurrentContext.removeAttribute(ResourceStorer.IS_LOGGING_ACTIVITY);
                    }
                    if (context.isLoggingActivity()) {
                        registryContext.getLogWriter().addLog(
                                path, CurrentContext.getUser(), Activity.RESTORE, null);
                    }
                }

                registryContext.getHandlerManager(HandlerLifecycleManager.COMMIT_HANDLER_PHASE).restore(context);
                
                transactionSucceeded = true;
            }
        } finally {
            if (transactionSucceeded) {
                commitTransaction();
            } else {
                try {
                    registryContext.getHandlerManager(HandlerLifecycleManager.ROLLBACK_HANDLER_PHASE).restore(context);
                } finally {
                    rollbackTransaction();
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("total read: " + DumpReader.getTotalRead());
                log.debug("total buffered: " + DumpReader.getTotalBuffered());
                log.debug("maximum buffer size: " + DumpReader.getMaxBufferedSize());
                log.debug("total buffer read size: " + DumpReader.getTotalBufferedRead());
            }
            clearContextInformation();
        }
    }

    @Override
    public void dump(String path, Writer writer) throws RepositoryException {
        if (log.isTraceEnabled()) {
            log.trace("Preparing operation dump, path: " + path + ".");
        }
    	
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
        boolean transactionSucceeded = false;
        HandlerContext context = new HandlerContext(this);
        
        try {
        	setContextInformation();
        	
        	path = chrootWrapper.getInPath(path);

            beginTransaction();

            context.setResourcePath(new ResourcePath(path));
            context.setDumpingWriter(writer);
            registryContext.getHandlerManager().dump(context);
            if (!SimulationFilter.isSimulation()) {
                if (!context.isProcessingComplete()) {
                    repository.dump(path, writer);
                }

                registryContext.getHandlerManager(HandlerLifecycleManager.COMMIT_HANDLER_PHASE).dump(context);

                transactionSucceeded = true;
            }
        } finally {
            if (transactionSucceeded) {
                commitTransaction();
            } else {
                try {
                    registryContext.getHandlerManager(HandlerLifecycleManager.ROLLBACK_HANDLER_PHASE).dump(context);
                } finally {
                    rollbackTransaction();
                }
            }
            clearContextInformation();
        }
    }

    @Override
    public String getEventingServiceURL(String path) throws RepositoryException {
        if (log.isTraceEnabled()) {
            log.trace("Preparing operation get eventing service url, " +
                    "path: " + path + ".");
        }
    	
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
    	try {
    		setContextInformation();
	    	path = chrootWrapper.getInPath(path);
	    	
	        if (path == null || eventingServiceURLs.size() == 0) {
	            return defaultEventingServiceURL;
	        }
	        Set<Map.Entry<String, String>> entries = eventingServiceURLs.entrySet();
	        for (Map.Entry<String, String> e : entries) {
	            if (e.getValue() == null) {
	                eventingServiceURLs.remove(e.getKey());
	            } else if (path.matches(e.getKey())) {
	                return e.getValue();
	            }
	        } 
    	} finally {
        	clearContextInformation();
        }
    	
        return defaultEventingServiceURL;
    }

    @Override
    public void setEventingServiceURL(String path, String eventingServiceURL) throws RepositoryException {
        if (log.isTraceEnabled()) {
            log.trace("Preparing operation set eventing service url, path: " + path + ", eventing service url: " + eventingServiceURL + ".");
        }
        
        if (InternalUtils.isRepositoryReadOnly(this)) {
            if (log.isTraceEnabled()) {
                log.trace("Cannot continue the operation eventing service url, the coreRegistry is read-only");
            }
            return;
        }
    	
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
    	try {
    		setContextInformation();
	    	path = chrootWrapper.getInPath(path);
	    	
	        if (path == null) {
	            this.defaultEventingServiceURL = eventingServiceURL;
	        } else {
	            this.eventingServiceURLs.put(path, eventingServiceURL);
	        }
    	} finally {
    		clearContextInformation();
    	}
    }
    
    @Override
    public boolean removeVersionHistory(String path, long snapshotId) throws RepositoryException {    
    	
    	if (InternalUtils.isRepositoryReadOnly(this)) {
            if (log.isTraceEnabled()) {
                log.trace("Cannot continue the operation removing the version history, the coreRegistry is read-only");
            }
            return false;
        }
    	
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
    	boolean transactionSucceeded = false;    	
        
        try {
        	setContextInformation();

            beginTransaction();
        	
            versionRepository.removeVersionHistory(path, snapshotId);
        
            transactionSucceeded = true;
        } finally {
            if (transactionSucceeded) {
                commitTransaction();
            } else {                
            	rollbackTransaction();                
            }
            clearContextInformation();
        }   	
    	
    	return false;
    }
    
    @Override
    public RepositoryService getRepositoryService() {
    	return repositoryService;
    }
    
    @Override
    public Resource get(String path) throws RepositoryException {
        if (log.isTraceEnabled()) {
            log.trace("Preparing operation get, " +
                    "path: " + path + ".");
        }
    	
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
    	try {
    		setContextInformation();
	    	path = chrootWrapper.getInPath(path);
	    	
	        if (registryContext != null && registryContext.isCacheEnabled()) {
		    	if (getRepositoryService().isNoCachePath(path) || isCommunityFeatureRequest(path)) {
		            return getResource(path);
		        }
		    	
		        Resource resource;
		        RepositoryCacheKey registryCacheKey = getRegistryCacheKey(this, path);
	
	            Object ghostResourceObject;
	            
	            @SuppressWarnings("rawtypes")
				Cache<RepositoryCacheKey, CacheResource> cache = getCache();
	            if ((ghostResourceObject = cache.get(registryCacheKey)) == null) {
	                resource = getResource(path);
	                if (resource.getPropertyValue(RepositoryConstants.REGISTRY_LINK) == null ||
	                        resource.getPropertyValue(RepositoryConstants.REGISTRY_MOUNT) != null) {
	                    cache.put(registryCacheKey, new CacheResource<Resource>(resource));
	                }
	            } else {
	                @SuppressWarnings("unchecked")
					CacheResource<Resource> ghostResource = (CacheResource<Resource>) ghostResourceObject;
	                resource = ghostResource.getResource();
	                
	                if (resource == null) {
	                    resource = getResource(path);
	                    if (resource.getPropertyValue(RepositoryConstants.REGISTRY_LINK) == null ||
	                            resource.getPropertyValue(RepositoryConstants.REGISTRY_MOUNT) != null) {
	                        ghostResource.setResource(resource);
	                    }
	                }
	            }
            
	            if (resource != null) {
		          	if (getRegistryContext() != null && getRegistryContext().isCacheEnabled()) {
		                  if (resource instanceof CollectionVersionImpl) {
		                      resource = new CollectionVersionImpl((CollectionVersionImpl) resource);
		                  } else if (resource instanceof CollectionImpl) {
		                      resource = new CollectionImpl((CollectionImpl) resource);
		                  } else {
		                      resource = new ResourceImpl((ResourceImpl) resource);
		                  }
		          	}
	          	
		          	((ResourceImpl) resource).setTenantId(tenantId);

		          	resource = (ResourceImpl) chrootWrapper.getOutResource(resource);
	            }
	            
	            return resource;
	        } else {
	        	Resource resource = getResource(path); 
	        	
	            if (resource != null) {
		          	if (getRegistryContext() != null && getRegistryContext().isCacheEnabled()) {
		                  if (resource instanceof CollectionVersionImpl) {
		                      resource = new CollectionVersionImpl((CollectionVersionImpl) resource);
		                  } else if (resource instanceof CollectionImpl) {
		                      resource = new CollectionImpl((CollectionImpl) resource);
		                  } else {
		                      resource = new ResourceImpl((ResourceImpl) resource);
		                  }
		          	}
		          	
		          	((ResourceImpl) resource).setTenantId(tenantId);

		          	resource = (ResourceImpl) chrootWrapper.getOutResource(resource);
	            }
	            
	            return resource ;
	        }
    	} finally {
    		clearContextInformation();
    	}
    }
    
    @Override
    public Collection get(String path, int start, int pageSize) throws RepositoryException {
        if (log.isTraceEnabled()) {
            log.trace("Preparing operation get with pagination, path: " + path + ", start: " + start + ", page size: " + pageSize + ".");
        }
    	
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
    	try {
    		setContextInformation();
    		path = chrootWrapper.getInPath(path);
    	
	        if (registryContext != null && registryContext.isCacheEnabled()) {
	        	if (getRepositoryService().isNoCachePath(path) || isCommunityFeatureRequest(path)) {
	                return getCollection(path, start, pageSize);
	            }
	            Collection collection;
	            RepositoryCacheKey registryCacheKey = getRegistryCacheKey(this, path +
	                    ";start=" + start + ";pageSize=" + pageSize);
	
	            @SuppressWarnings("rawtypes")
				Cache<RepositoryCacheKey, CacheResource> cache = getCache();
	            if (!cache.containsKey(registryCacheKey)) {
	                collection = getCollection(path, start, pageSize);
	                if (collection.getPropertyValue(RepositoryConstants.REGISTRY_LINK) == null) {
	                    cache.put(registryCacheKey, new CacheResource<Resource>(collection));
	                }
	            } else {
	                @SuppressWarnings("unchecked")
					CacheResource<Resource> ghostResource = cache.get(registryCacheKey);
	                collection = (Collection) ghostResource.getResource();
	                if (collection == null) {
	                    collection = getCollection(path, start, pageSize);
	                    if (collection.getPropertyValue(RepositoryConstants.REGISTRY_LINK) == null) {
	                        ghostResource.setResource(collection);
	                    }
	                }
	            }
            
	            if (collection != null) {
	            	if(getRegistryContext() != null && getRegistryContext().isCacheEnabled()) {
						if (collection instanceof CollectionVersionImpl) {
							collection = new CollectionVersionImpl(((CollectionVersionImpl) collection));
						} else {
						     collection = new CollectionImpl(((CollectionImpl) collection));
						}
	            	}
	            	
					// collection implementation extends from the resource implementation.
					ResourceImpl resourceImpl = (ResourceImpl) collection;
					resourceImpl.setTenantId(tenantId);

					collection = chrootWrapper.getOutCollection(collection);
	            }
            
	            return collection;       	
			} else {
				Collection collection = getCollection(path, start, pageSize);
				
				if (collection != null) {
					if(getRegistryContext() != null && getRegistryContext().isCacheEnabled()) {
						if (collection instanceof CollectionVersionImpl) {
							collection = new CollectionVersionImpl(((CollectionVersionImpl) collection));
						} else {
							collection = new CollectionImpl(((CollectionImpl) collection));
						}
					}
				    	
					// collection implementation extends from the resource implementation.
					ResourceImpl resourceImpl = (ResourceImpl) collection;
					resourceImpl.setTenantId(tenantId);
	
					collection = chrootWrapper.getOutCollection(collection);
				}
            
			    return collection ;
			}
    	} finally {
    		clearContextInformation();
    	}
    }
    
    @Override
	public boolean resourceExists(String path) throws RepositoryException {
        if (log.isTraceEnabled()) {
            log.trace("Preparing operation resource exists, path: " + path + ".");
        }
		
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
    	try {
    		setContextInformation();
    		
	    	path = chrootWrapper.getInPath(path) ;

			if (getRepositoryService().isNoCachePath(path)) {
				return checkResourceExists(path);
			}
			
			@SuppressWarnings("rawtypes")
			Cache<RepositoryCacheKey, CacheResource> cache = getCache();
			RepositoryCacheKey registryCacheKey = getRegistryCacheKey(this, path);
			
			if (cache.containsKey(registryCacheKey)) {
				return true;
			} else if (checkResourceExists(path)) {
				cache.put(registryCacheKey, new CacheResource<Resource>(null));
				return true;
			}
			
			return false;
    	} finally {
    		clearContextInformation();
    	}
	}
    
	@Override
	public String getResourceMediaTypes() throws RepositoryException {
		return resourceMediaTypes;
	}

	@Override
	public void setResourceMediaTypes(String resourceMediaTypes) throws RepositoryException {
		this.resourceMediaTypes = resourceMediaTypes;
	}
	
	@Override
	public String getCollectionMediaTypes() throws RepositoryException {
		return collectionMediaTypes;
	}

	@Override
	public void setCollectionMediaTypes(String collectionMediaTypes) throws RepositoryException {
		this.collectionMediaTypes = collectionMediaTypes;
	}
	
	@Override
    public String getCustomUIMediaTypes() throws RepositoryException {
    	return customUIMediaTypes;
    }

	@Override
    public void setCustomUIMediaTypes(String customUIMediaTypes) throws RepositoryException {
    	this.customUIMediaTypes = customUIMediaTypes;
    }
    
    private void init() throws RepositoryException {
        beginDBQueryLog(2);
        if (log.isTraceEnabled()) {
            log.trace("Initializing main registry");
        }

        if (registryContext == null) {
            registryContext = RepositoryContext.getBaseInstance(repositoryService);
        }
        
        registryContext.setDataAccessManager(dataAccessManager);

        if (log.isTraceEnabled()) {
            log.trace("Initializing the version repository.");
        }
        
        versionRepository = new VersionResourceStorer(dataAccessManager);

        if (log.isTraceEnabled()) {
            log.trace("Initializing the repository.");
        }
        repository = new ResourceStorer(dataAccessManager,
                versionRepository, registryContext.isVersionOnChange(),
                new RecursionRepository(this));

        if (log.isTraceEnabled()) {
            log.trace("Initializing the query manager for processing custom queries.");
        }
        queryProcessorManager = new QueryProcessorManager(dataAccessManager, registryContext);

        registryContext.setRepository(repository);
        registryContext.setVersionRepository(versionRepository);
        registryContext.setQueryProcessorManager(queryProcessorManager);

        if (log.isTraceEnabled()) {
            log.trace("Initialing the content indexing system.");
        }

        if (log.isTraceEnabled()) {
            log.trace("Initializing DAOs depending on the static configurations.");
        }

        if (log.isTraceEnabled()) {
            log.trace("Main registry initialized successfully.");
        }
        endDBQueryLog(2);
        
        // For Caching ------------------------------------------------------------------
            	
        if (registryContext != null && registryContext.isCacheEnabled()) {
	        for (Mount mount : registryContext.getMounts()) {
	            for(RemoteConfiguration configuration : registryContext.getRemoteInstances()) {
	                if (configuration.getDbConfig() != null && mount.getInstanceId().equals(configuration.getId())) {
	                    dbConfigs.put(mount.getPath(), registryContext.getDBConfig(configuration.getDbConfig()));
	                    pathMap.put(mount.getPath(), mount.getTargetPath());
	                } else if (configuration.getCacheId() != null && mount.getInstanceId().equals(configuration.getId())) {
	                    cacheIds.put(mount.getPath(), configuration.getCacheId());
	                    pathMap.put(mount.getPath(), mount.getTargetPath());
	                }
	            }
	        }
        }
        
        embeddedRegistryInitialized = true ;
        
        chrootWrapper = new ChrootWrapper(concatenatedChroot);
        
        try {
            setContextInformation();
            if ((registryContext != null && registryContext.isClone()) || (repositoryService != null && repositoryService.isClone())) {
                try {
                    addRootCollection();
                } catch (Exception ignored) {
                    // There can be situations where the remote instance doesn't like creating the
                    // root collection, if it is already there. Hence, we can simply ignore this.
                }
            } else {
                addRootCollection();
            }
            if ((registryContext != null && !registryContext.isClone()) || (repositoryService != null && !repositoryService.isClone())) {
                addSystemCollections();
            }
        } finally {
            clearContextInformation();
        }
    }

    // Starts logging database query statistics.
    private void beginDBQueryLog(int level) {
        if (dbQueryLog.isDebugEnabled()) {
            StackTraceElement traceElement = Thread.currentThread().getStackTrace()[level];
            String methodName = traceElement.getMethodName();
            if (!statEnabledOperations.isEmpty() && statEnabledOperations.contains(methodName)) {
                if (traceElement.getClassName().equals(this.getClass().getCanonicalName())) {
                    StatisticsRecord statisticsRecord = DBQueryStatisticsLog.getStatisticsRecord();
                    if (statisticsRecord.increment() == 0) {
                        DBQueryStatisticsLog.clearStatisticsRecord();
                        statisticsRecord = DBQueryStatisticsLog.getStatisticsRecord();
                        statisticsRecord.increment();
                        statisticsRecord.setOperation(methodName);
                    }
                }
            }
        }
    }

    // Finishes logging database query statistics.
    private void endDBQueryLog(int level) {
        if (dbQueryLog.isDebugEnabled()) {
            StackTraceElement traceElement = Thread.currentThread().getStackTrace()[level];
            String methodName = traceElement.getMethodName();
            
            if (!statEnabledOperations.isEmpty() && statEnabledOperations.contains(methodName)) {
                if (traceElement.getClassName().equals(this.getClass().getCanonicalName())) {
                    StatisticsRecord statisticsRecord = DBQueryStatisticsLog.getStatisticsRecord();
                    if (statisticsRecord.decrement() == 0) {
                        final StatisticsRecord clone = new StatisticsRecord(statisticsRecord);
                        Runnable runnable = new Runnable() {
                            public void run() {
                                if (clone.getTableRecords().length > 0) {
                                    dbQueryLog.debug("");
                                    dbQueryLog.debug("---------------------------------------------------");
                                    dbQueryLog.debug("Registry Operation: " + clone.getOperation());
                                    dbQueryLog.debug("");
                                    for (String record : clone.getTableRecords()) {
                                        dbQueryLog.debug("Tables Accessed: " + record);
                                    }
                                    if (Boolean.toString(true).equals(System.getProperty("carbon.registry.statistics.output. queries.executed"))) {
                                        dbQueryLog.debug("");
                                        StringBuffer sb = new StringBuffer();
                                        for (String query : clone.getQueries()) {
                                            sb.append("\n").append(query);
                                        }
                                        dbQueryLog.debug("Queries Executed:" + sb.toString());
                                    }
                                    dbQueryLog.debug("---------------------------------------------------");
                                    dbQueryLog.debug("");
                                }
                            }
                        };
                        
                        if (executor != null) {
                            executor.submit(runnable);
                        } else {
                            initializeStatisticsLogging();
                            executor.submit(runnable);
                        }
                        
                        DBQueryStatisticsLog.clearStatisticsRecord();
                    }
                }
            }
        }
    }
    
    @SuppressWarnings("rawtypes")
	private static Cache<RepositoryCacheKey, CacheResource> getCache() {
        return InternalUtils.getResourceCache(InternalConstants.REGISTRY_CACHE_BACKED_ID);
    }
    
    /**
     * This method used to calculate the cache key
     *
     * @param registry Registry
     * @param path     Resource path
     *
     * @return RegistryCacheKey
     */
    private RepositoryCacheKey getRegistryCacheKey(Repository registry, String path) {
        String connectionId = "";

        int tenantId;
        if (this.tenantId == MultitenantConstants.INVALID_TENANT_ID) {
            tenantId = CurrentContext.getTenantId();
            if (tenantId == MultitenantConstants.INVALID_TENANT_ID) {
                tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            }
        } else {
            tenantId = this.tenantId;
        }
        
        String resourceCachePath;
    	
    	RepositoryContext registryContext = InternalUtils.getRepositoryContext(registry);

        if (registryContext == null) {
            registryContext = RepositoryContext.getBaseInstance();
        }
        if (registry instanceof EmbeddedRepository) {
            resourceCachePath = path;
        } else {
            resourceCachePath = InternalUtils.getAbsolutePath(registryContext, path);
        }
        
        DataBaseConfiguration dataBaseConfiguration = null;
        if (dbConfigs.size() > 0) {
            for (String sourcePath : dbConfigs.keySet()) {
                if (resourceCachePath.startsWith(sourcePath)) {
                    resourceCachePath = pathMap.get(sourcePath) + resourceCachePath.substring(sourcePath.length());
                    dataBaseConfiguration = dbConfigs.get(sourcePath);
                    break;
                }
            }
        } else if (cacheIds.size() > 0) {
            for (String sourcePath : cacheIds.keySet()) {
                if (resourceCachePath.startsWith(sourcePath)) {
                    resourceCachePath = pathMap.get(sourcePath) + resourceCachePath.substring(sourcePath.length());
                    connectionId = cacheIds.get(sourcePath);
                    break;
                }
            }
        }
        
        if (connectionId.length() == 0) {
            if (dataBaseConfiguration == null) {
                dataBaseConfiguration = registryContext.getDefaultDataBaseConfiguration();
            }
            if (dataBaseConfiguration != null) {
                connectionId = (dataBaseConfiguration.getUserName() != null
                        ? dataBaseConfiguration.getUserName().split("@")[0]:dataBaseConfiguration.getUserName()) + "@" + dataBaseConfiguration.getDbUrl();
            }
        }

        return InternalUtils.buildRegistryCacheKey(connectionId, tenantId, resourceCachePath);
    }
    
    private Resource getResource(String path) throws RepositoryException {
        boolean transactionSucceeded = false;
        HandlerContext context = new HandlerContext(this);
        try {
            beginTransaction();

            ResourcePath resourcePath = new ResourcePath(path);

            // check if this path refers to a resource referred by a URL query (e.g. comment)
            context.setResourcePath(resourcePath);

            Resource resource = registryContext.getHandlerManager().get(context);

            if (!SimulationFilter.isSimulation()) {
                // resource may have been fetched from the repository, to be used by handlers. if
                // it is done, it has to be stored in the request context. we can just use that
                // resource without fetching it again from the repository.
                if (resource == null) {
                    resource = context.getResource();
                }

                if (resource == null) {
                    VersionedPath versionedPath = InternalUtils.getVersionedPath(resourcePath);
                    if (versionedPath.getVersion() == -1) {
                        resource = repository.get(resourcePath.getPath());
                    } else {
                        resource = versionRepository.get(versionedPath);
                    }
                }

                if (resource == null) {
                    throw new RepositoryResourceNotFoundException(path, RepositoryErrorCodes.RESOURCE_PATH_ERROR);
                }

                context.setResource(resource);

                registryContext.getHandlerManager(HandlerLifecycleManager.COMMIT_HANDLER_PHASE).get(context);
                
                transactionSucceeded = true;
            }
            
            return resource;
        } finally {
            if (transactionSucceeded) {
                commitTransaction();
            } else {
                try {
                    registryContext.getHandlerManager(
                            HandlerLifecycleManager.ROLLBACK_HANDLER_PHASE).get(context);
                } finally {
                    rollbackTransaction();
                }
            }
        }
    }
    
    private Collection getCollection(String path, int start, int pageSize) throws RepositoryException { // Renamed to getCollection from get. Made to private
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
        boolean transactionSucceeded = false;
        try {
        	path = chrootWrapper.getInPath(path);
        	
            beginTransaction();

            Collection collection;

            ResourcePath resourcePath = new ResourcePath(path);
            VersionedPath versionedPath = InternalUtils.getVersionedPath(resourcePath);
            if (versionedPath.getVersion() == -1) {
                collection = repository.get(resourcePath.getPath(), start, pageSize);
            } else {
                collection = versionRepository.get(versionedPath, start, pageSize);
            }

            transactionSucceeded = true;
            
            return collection;
        } finally {
            if (transactionSucceeded) {
                commitTransaction();
            } else {
                rollbackTransaction();
            }
        }
    }

    private boolean checkResourceExists(String path) throws RepositoryException {
    	if(!embeddedRegistryInitialized) {
    		init();
    	}
    	
        if (log.isTraceEnabled()) {
            log.trace("Checking if resource exist in resource. path: " + path + ".");
        }
    	
        boolean transactionSucceeded = false;
        HandlerContext context = new HandlerContext(this);
        
        try {
            beginTransaction();

            ResourcePath resourcePath = new ResourcePath(path);

            context.setResourcePath(resourcePath);
            boolean output = registryContext.getHandlerManager().resourceExists(context);

            if (!SimulationFilter.isSimulation()) {
                if (!context.isProcessingComplete()) {

                    VersionedPath versionedPath = InternalUtils.getVersionedPath(resourcePath);

                    output = (versionedPath.getVersion() == -1) ? repository.resourceExists(resourcePath.getPath()) :
                            versionRepository.resourceExists(versionedPath);
                }

                registryContext.getHandlerManager(HandlerLifecycleManager.COMMIT_HANDLER_PHASE).resourceExists(context);
                
                transactionSucceeded = true;
            }

            return output;
        } finally {
            if (transactionSucceeded) {
                commitTransaction();
            } else {
                try {
                    registryContext.getHandlerManager(
                            HandlerLifecycleManager.ROLLBACK_HANDLER_PHASE).resourceExists(context);
                } finally {
                    rollbackTransaction();
                }
            }
        }
    }
    
    // Adds the root collection, if it doesn't exist.
    // This returns true if the root collection is added, false if it is already existing.
    private void addRootCollection() throws RepositoryException {
        try {
            beginTransaction();
                      
        	RepositoryContext registryContext = InternalUtils.getRepositoryContext(this);
            ResourceDAO resourceDAO = registryContext.getDataAccessManager(). getDAOManager().getResourceDAO();
            
            if (log.isTraceEnabled()) {
                log.trace("Checking the existence of the root collection of the Registry.");
            }
            
//            boolean addAuthorizations = !(RepositoryContext.getBaseInstance().isSystemResourcePathRegistered(RepositoryConstants.ROOT_PATH));
            
            if (InternalUtils.systemResourceShouldBeAdded(resourceDAO, RepositoryConstants.ROOT_PATH)) {
                if (log.isTraceEnabled()) {
                    log.trace("Creating the root collection of the Registry.");
                }
                
                CollectionImpl root = new CollectionImpl();
                root.setUUID(UUID.randomUUID().toString());
                resourceDAO.addRoot(root);
            }
            
            String chroot = chrootWrapper.getBasePrefix();
            if (chroot != null &&
                    !chroot.equals(RepositoryConstants.ROOT_PATH)) {
                if (InternalUtils.systemResourceShouldBeAdded(resourceDAO, chroot)) {
                    CollectionImpl chrootColl = new CollectionImpl();
                    put(RepositoryConstants.ROOT_PATH,chrootColl);
                }
            }

            commitTransaction();

        } catch (Exception e) {

            String msg = "Failed to add the root collection to the coreRegistry.";
            log.fatal(msg, e);

            rollbackTransaction();

            throw new RepositoryInitException(msg, e);
        }
    }
    
    // This will add the initial system collections to the registry..
    // system collections = /, /_system, /_system/config, /_system/governance/services, etc.
    private void addSystemCollections() throws RepositoryException {
        if (log.isTraceEnabled()) {
            log.trace("adding system collections.");
        }

        InternalUtils.addBaseCollectionStructure(this);
    }
    
    /**
     * Method to set the information related to users in to the current session.
     */
    public final void setContextInformation() {
        if (log.isTraceEnabled()) {
            log.trace("Setting the context for repository operation, chroot: " + chrootWrapper.getBasePrefix() + ", " +
                    "username: " + userName + ", tenantId: " + tenantId + ", callerTenantId: " + tenantId + ".");
        }
        
        CurrentContext.setUser(userName);

        CurrentContext.setTenantId(tenantId);
        CurrentContext.setCallerTenantId(tenantId);
        CurrentContext.setChroot(chrootWrapper.getBasePrefix());
        CurrentContext.setUserRegistry(this);
    }

    /**
     * Method to clear session information.
     */
    private final void clearContextInformation() {

        if (log.isTraceEnabled()) {
            log.trace("Clearing the context for repository operation, chroot: " + chrootWrapper.getBasePrefix() + ", " +
                    "username: " + CurrentContext.getUser() + ", tenantId: " + CurrentContext.getTenantId() + ".");
        }
        
        CurrentContext.removeUser();
        CurrentContext.removeTenantId();
        CurrentContext.removeCallerTenantId();
        CurrentContext.removeChroot();
        CurrentContext.removeUserRegistry();
        
        if (CurrentContext.getRespository() == null) {
            CurrentContext.removeAttributes();
        }
    }
    
    // test whether this request was made specifically for a tag, comment or a rating.
    private boolean isCommunityFeatureRequest(String path) {
        if (path == null) {
            return false;
        }
        String resourcePath = new ResourcePath(path).getPath();
        if (path.length() > resourcePath.length()) {
            String fragment = path.substring(resourcePath.length());
            for (String temp : new String[] {"tags", "comments", "ratings"}) {
                if (fragment.contains(temp)) {
                    return true;
                }
            }
        }
        return false;
    }
}
