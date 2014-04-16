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

package org.wso2.carbon.repository.core.config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.repository.api.RepositoryConstants;
import org.wso2.carbon.repository.api.RepositoryService;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.core.CurrentContext;
import org.wso2.carbon.repository.core.EmbeddedRepositoryService;
import org.wso2.carbon.repository.core.ResourceStorer;
import org.wso2.carbon.repository.core.VersionResourceStorer;
import org.wso2.carbon.repository.core.exceptions.RepositoryInitException;
import org.wso2.carbon.repository.core.handlers.CustomEditManager;
import org.wso2.carbon.repository.core.handlers.HandlerLifecycleManager;
import org.wso2.carbon.repository.core.handlers.HandlerManager;
import org.wso2.carbon.repository.core.jdbc.dataaccess.JDBCDataAccessManager;
import org.wso2.carbon.repository.core.queries.QueryProcessorManager;
import org.wso2.carbon.repository.core.utils.InternalConstants;
import org.wso2.carbon.repository.core.utils.LogQueue;
import org.wso2.carbon.repository.core.utils.LogWriter;
import org.wso2.carbon.repository.spi.dataaccess.DataAccessManager;

/**
 * This class provides access to core registry configurations. Registry context is associated with
 * each mounted registry instance. The base registry context can be accessed via the getBaseInstance
 * method.
 */
@SuppressWarnings("unused")
public class RepositoryContext {

    private static final Log log = LogFactory.getLog(RepositoryContext.class);
    
    /**
     * Classes which are allowed to directly call secured methods in this class
     *
     * Note that we have to use the String form of the class name and not, for example,
     * RegistryResolver.class.getName() since this may unnecessarily cause NoClassDefFoundErrors
     */
    private static final List<String> ALLOWED_CLASSES = Arrays.asList(RepositoryContext.class.getName(),
                          "org.wso2.carbon.registry.core.ResourceImpl",
                          "org.wso2.carbon.registry.core.jdbc.EmbeddedRegistry",
                          "org.wso2.carbon.registry.core.jdbc.Repository",
                          "org.wso2.carbon.registry.core.jdbc.dao.JDBCLogsDAO",
                          "org.wso2.carbon.registry.core.jdbc.dao.JDBCPathCache");

    private static final String NODE_IDENTIFIER = UUID.randomUUID().toString();

    private String resourceMediaTypes = null;
    private String collectionMediaTypes = null;
    private String customUIMediaTypes = null;

    private RegURLSupplier urlSupplier;

    private DataBaseConfiguration defaultDataBaseConfiguration = null;
    private Map<String, DataBaseConfiguration> dbConfigs = new HashMap<String, DataBaseConfiguration>();
    
    private HandlerLifecycleManager handlerManager = new HandlerLifecycleManager();
    private CustomEditManager customEditManager = new CustomEditManager();
    
    private boolean versionOnChange;
    
    private List<RemoteConfiguration> remoteInstances = new ArrayList<RemoteConfiguration>();
    private List<Mount> mounts = new ArrayList<Mount>();
    
    private List<QueryProcessorConfiguration> queryProcessors = new ArrayList<QueryProcessorConfiguration>();
    
    private String profilesPath = RepositoryConstants.CONFIG_REGISTRY_BASE_PATH + RepositoryConstants.PROFILES_PATH;
    private String servicePath = RepositoryConstants.GOVERNANCE_REGISTRY_BASE_PATH + InternalConstants.GOVERNANCE_SERVICE_PATH;

    private LogWriter logWriter = null;
    private boolean enableCache = false;

    private List<String> systemResourcePaths = new ArrayList<String>();
    private List<Pattern> noCachePaths = new ArrayList<Pattern>();

    /**
     * As long as this instance remains in memory, it will be used.
     */
    private static RepositoryContext repositoryContext = null;

    private String repositoryRoot;
    private boolean readOnly;
    private DataAccessManager dataAccessManager = null;
    private boolean setup = true;
    private boolean clone = false;
    
    private RepositoryService repositoryService = null ;

    private ResourceStorer repository;
    private VersionResourceStorer versionRepository;
    private QueryProcessorManager queryProcessorManager;
    private EmbeddedRepositoryService embeddedRepositoryService;

    /**
     * Get a unique identifier for this repository node. This is used to establish uniqueness among
     * multiple nodes of a cluster or a multi-product deployment. Please node that this identifier
     * is not persisted, and its regenerated once per each restart.
     *
     * @return the unique identifier of this node.
     */
    public String getNodeIdentifier() {
        return NODE_IDENTIFIER;
    }

    /**
     * Method to obtain resource media types.
     *
     * @return the resource media types.
     */
    public String getResourceMediaTypes() {
        return RepositoryContext.getBaseInstance().resourceMediaTypes;
    }

    /**
     * Method to set resource media types.
     *
     * @param resourceMediaTypes the resource media types.
     */
    public void setResourceMediaTypes(String resourceMediaTypes) {
        RepositoryContext.getBaseInstance().resourceMediaTypes = resourceMediaTypes;
    }

    /**
     * Method to obtain collection media types.
     *
     * @return the collection media types.
     */
    public String getCollectionMediaTypes() {
        return RepositoryContext.getBaseInstance().collectionMediaTypes;
    }

    /**
     * Method to set collection media types.
     *
     * @param collectionMediaTypes the collection media types.
     */
    public void setCollectionMediaTypes(String collectionMediaTypes) {
        RepositoryContext.getBaseInstance().collectionMediaTypes = collectionMediaTypes;
    }

    /**
     * Method to obtain custom UI media types.
     *
     * @return the custom UI media types.
     */
    public String getCustomUIMediaTypes() {
        return RepositoryContext.getBaseInstance().customUIMediaTypes;
    }

    /**
     * Method to set custom UI media types.
     *
     * @param customUIMediaTypes the custom UI media types.
     */
    public void setCustomUIMediaTypes(String customUIMediaTypes) {
        RepositoryContext.getBaseInstance().customUIMediaTypes = customUIMediaTypes;
    }

    /**
     * The interface to change the url supplement logic
     */
    public interface RegURLSupplier {
        String getURL();
    }

    /**
     * To check whether this is a clone or a base registry context.
     *
     * @return true if this is clone, false if this is the base registry context.
     */
    public boolean isClone() {
        return clone;
    }

    /**
     * Set the flag if the current registry context is a clone and not base
     *
     * @param clone whether it is a clone or not
     */
    public void setClone(boolean clone) {
        this.clone = clone;
    }

    /**
     * Get an instance of the base (not cloned),
     *
     * @return base registry context
     */
    public static RepositoryContext getBaseInstance() {
        return repositoryContext;
    }

    /**
     * Create an return a registry context.
     *
     * @return new registry context
     */
    public static RepositoryContext getCloneContext() {
        RepositoryContext context = new RepositoryContext();
        context.setClone(true);
        return context;
    }

    /**
     * destroy the registry context
     */
    public static void destroy() {
        setBaseInstance(null);
    }
    
    /**
     * Return a singleton object of the base registry context with custom realm service If a
     * registry context doesn't exist, it will create a new one and return it. Otherwise it will
     * create the current base registry context                    registryContext.setRegistryRoot(registryRoot);
                    registryService.setRegistryRoot(registryRoot);
                    StaticConfiguration.setRegistryRoot(registryRoot);
     *
     * @param repositoryService repository service
     *
     * @return the base registry context
     */
    public static RepositoryContext getBaseInstance(RepositoryService repositoryService) {
        return getBaseInstance(true, repositoryService);
    }

    /**
     * Return a singleton object of the base registry context with custom realm service If a
     * registry context doesn't exist, it will create a new one and return it. Otherwise it will
     * create the current base registry context
     *
     * @param populateConfiguration whether the configuration must be populated or not.
     * @param repositoryService repository service
     *
     * @return the base registry context
     */
    public static RepositoryContext getBaseInstance(boolean populateConfiguration, RepositoryService repositoryService) {
        try {
            if (getBaseInstance() != null) {
                return getBaseInstance(); 
            } 
            
            new RepositoryContext(populateConfiguration, repositoryService);
        } catch (RepositoryException e) {
            log.error("Unable to get instance of the registry context", e);
            return null;
        }
        return getBaseInstance();
    }

    /**
     * Return a singleton object of the repository context with a custom configuration
     * If a repository context doesn't exist, it will create a new one and return it.
     * Otherwise it will create the current base registry context
     *
     * @param configStream configuration stream (registry.xml)
     *
     * @return the repository context
     */
    public static RepositoryContext getBaseInstance(InputStream configStream, RepositoryService repositoryService) {
        try {
            if (getBaseInstance() != null) {
                return getBaseInstance();
            }
            
            new RepositoryContext(configStream, repositoryService);
        } catch (RepositoryException e) {
            log.error("Unable to get instance of the repository context", e);
            return null;
        }
        return getBaseInstance();
    }

    /**
     * Return a singleton object of the repository context with a custom url supplier and custom
     * config. If a repository context doesn't exist, it will create a new one and return it.
     * Otherwise it will create the current base registry context
     *
     * @param configStream config stream (registry.xml)
     * @param urlSupplier  url supplier
     *
     * @return the singleton object of the registry context.
     */
    public static RepositoryContext getBaseInstance(InputStream configStream, RegURLSupplier urlSupplier, RepositoryService repositoryService) {
        try {
            if (getBaseInstance() != null) {
                return getBaseInstance();
            }
            
            new RepositoryContext(configStream, urlSupplier, repositoryService);
        } catch (RepositoryException e) {
            log.error("Unable to get instance of the registry context", e);
            return null;
        }
        
        return getBaseInstance();
    }

    /**
     * set a singleton
     *
     * @param context registry context
     */
    private static synchronized void setBaseInstance(RepositoryContext context) {
        repositoryContext = context;
    }

    /**
     * Return the registry root. (configured in registry.xml)
     *
     * @return the registry root
     */
    public String getRepositoryRoot() {
        return repositoryRoot;
    }

    /**
     * Set the registry root.
     *
     * @param registryRoot the value of the registry root
     */
    public void setRegistryRoot(String registryRoot) {
        this.repositoryRoot = registryRoot;
        
        repositoryService.setRepositoryRoot(registryRoot);
        StaticConfiguration.setRepositoryRoot(registryRoot);
    }

    /**
     * Return whether the registry is read-only or not.
     *
     * @return true if readonly, false otherwise.
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Set whether the registry is read-only or not
     *
     * @param readOnly the read-only flag
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        repositoryService.setReadOnly(readOnly);
    }

    /**
     * Return whether the registry caching is enabled or not.
     *
     * @return true if enabled, false otherwise.
     */
    public boolean isCacheEnabled() {
        return enableCache;
    }

    /**
     * Set whether the registry caching is enabled or not.
     *
     * @param enableCache the enable-cache flag
     */
    public void setCacheEnabled(boolean enableCache) {
        this.enableCache = enableCache;
        repositoryService.setCacheEnabled(enableCache);
    }

    /**
     * Create a new registry context object with a custom realm service
     *
     * @param repositoryService repository service
     * @param populateConfiguration whether the configuration must be populated or not.
     *
     * @throws RepositoryException throws if the construction failed.
     */
    protected RepositoryContext(boolean populateConfiguration, RepositoryService repositoryService) throws RepositoryException {
        this(null, populateConfiguration, repositoryService);
    }

    /**
     * Create a registry context with custom configuration and realm service.
     *
     * @param configStream configuration stream. (registry.xml input stream)//
     * @param repositoryService repository service
     *
     * @throws RepositoryException throws if the construction failed.
     */
    protected RepositoryContext(InputStream configStream, RepositoryService repositoryService) throws RepositoryException {
        this(configStream, true, repositoryService);
    }
    
    /**
     * Create a registry context with custom configuration and realm service.
     *
     * @param configStream          configuration stream. (registry.xml input stream)
     * @param populateConfiguration whether the configuration must be populated or not.
     *
     * @throws RepositoryException throws if the construction failed.
     */
    protected RepositoryContext(InputStream configStream, boolean populateConfiguration, RepositoryService repositoryService) throws RepositoryException {
        setBaseInstance(this);
        this.repositoryService = repositoryService ;
        
        if (populateConfiguration) {
            RepositoryConfigurationProcessor.populateRepositoryConfig(configStream, this, repositoryService);
        }
    }

    /**
     * Create a repository context with custom configuration and realm service.
     *
     * @param configStream configuration stream. (registry.xml input stream)
     * @param urlSupplier  url supplier object.
     *
     * @throws RepositoryException throws if the construction failed.
     */
    protected RepositoryContext(InputStream configStream, RegURLSupplier urlSupplier, RepositoryService registryService) throws RepositoryException {
        setBaseInstance(this);
        this.urlSupplier = urlSupplier;
        RepositoryConfigurationProcessor.populateRepositoryConfig(configStream, this, registryService);
    }

    /**
     * Create a registry context with default values.
     */
    protected RepositoryContext() {
        RepositoryContext baseContext = getBaseInstance();
        
        if (baseContext != null) {
            this.urlSupplier = baseContext.urlSupplier;
            this.defaultDataBaseConfiguration = baseContext.defaultDataBaseConfiguration;
            this.dbConfigs = baseContext.dbConfigs;
            this.handlerManager = baseContext.handlerManager;
            this.customEditManager = baseContext.customEditManager;
            this.versionOnChange = baseContext.versionOnChange;
            this.profilesPath = baseContext.profilesPath;
            this.remoteInstances = baseContext.remoteInstances;
            this.mounts = baseContext.mounts;
            this.queryProcessors = baseContext.queryProcessors;
            this.servicePath = baseContext.servicePath;
            this.logWriter = baseContext.logWriter;
            this.systemResourcePaths = baseContext.systemResourcePaths;
            this.noCachePaths = baseContext.noCachePaths;
        }
        
        this.setup = true;
    }

    /**
     * Return the repository object, which provides an interface to put, get resources to the
     * repository.
     *
     * @return the repository object
     */
    public ResourceStorer getRepository() {
        return repository;
    }

    /**
     * Set the repository object.
     *
     * @param repository the repository object.
     */
    public void setRepository(ResourceStorer repository) {
        this.repository = repository;
    }

    /**
     * Return the version repository object, which provides an interface to create versions,
     * retrieve old versions of resources
     *
     * @return a version repository object.
     */
    public VersionResourceStorer getVersionRepository() {
        return versionRepository;
    }

    /**
     * Set a version repository object.
     *
     * @param versionRepository version repository test
     */
    public void setVersionRepository(VersionResourceStorer versionRepository) {
        this.versionRepository = versionRepository;
    }

    /**
     * Return a query processor.
     *
     * @return the query processor object.
     */
    public QueryProcessorManager getQueryProcessorManager() {
        return queryProcessorManager;
    }

    /**
     * Set a query processor object
     *
     * @param queryProcessorManager the query processor object.
     */
    public void setQueryProcessorManager(QueryProcessorManager queryProcessorManager) {
        this.queryProcessorManager = queryProcessorManager;
    }

    /**
     * Whether the version should be created automatically on a change (only for non-collection
     * resources)
     *
     * @return true, if version is changing automatically on a change. false, otherwise.
     */
    public boolean isVersionOnChange() {
        return versionOnChange;
    }

    /**
     * Set whether the version should be created automatically on a change (only for non-collection
     * resources)
     *
     * @param versionOnChange Flag to set whether the version should be created,
     */
    public void setVersionOnChange(boolean versionOnChange) {
        this.versionOnChange = versionOnChange;
    }

    /**
     * Whether the "setup" system property is set at the start.
     *
     * @return true if the "setup" system property is set, false otherwise.
     */
    public boolean isSetup() {
        return setup;
    }

    /**
     * Set if the "setup" system property is set at the start.
     *
     * @param setup the flag for the setup property.
     */
    public void setSetup(boolean setup) {
        this.setup = setup;
    }

    /**
     * Return a embedded repository service. If there is no registry service existing, this will
     * create a registry service an return
     *
     * @return the newly create registry service.
     * @throws RepositoryException throws if the retrieval of the embedded registry service is failed.
     */
    public EmbeddedRepositoryService getEmbeddedRepositoryService() throws RepositoryException {
        if (embeddedRepositoryService == null) {
            try {
                embeddedRepositoryService = new EmbeddedRepositoryService(this);
            } catch (RepositoryException e) {
                String msg = "Couldn't initialize EmbeddedRegistryService. " + e.getMessage();
                log.error(msg, e);
                throw new RepositoryInitException(msg, e);
            }
        }
        
        return embeddedRepositoryService;
    }

    /**
     * Return the default database configuration.
     *
     * @return the default database configuration.
     */
    public DataBaseConfiguration getDefaultDataBaseConfiguration() {
        return defaultDataBaseConfiguration;
    }

    /**
     * Sets the default database configuration.
     *
     * @param dataBaseConfiguration the default database configuration.
     */
    public void setDefaultDataBaseConfiguration(DataBaseConfiguration dataBaseConfiguration) {
        this.defaultDataBaseConfiguration = dataBaseConfiguration;
    }

    /**
     * Select a database configuration among the available database configuration.
     *
     * @param dbConfigName name of the selecting database configuration.
     *
     * @return selected database configuration.
     */
    public DataBaseConfiguration selectDBConfig(String dbConfigName) {
        DataBaseConfiguration config = dbConfigs.get(dbConfigName);
        
        if (config == null) {
            log.error("Couldn't find db configuration '" + dbConfigName + "'");
            return null;
        }

        dataAccessManager = new JDBCDataAccessManager(config);
        
        return config;
    }

    /**
     * Get the available database configuration names.
     *
     * @return string iterator of available database configurations
     */
    public Iterator<String> getDBConfigNames() {
        return dbConfigs.keySet().iterator();
    }

    /**
     * Get the database configuration of a given configuration name
     *
     * @param dbConfigName database configuration name
     *
     * @return database configuration object
     */
    public DataBaseConfiguration getDBConfig(String dbConfigName) {
        return dbConfigs.get(dbConfigName);
    }

    /**
     * Add database configuration with the given name.
     *
     * @param name   the name of the database configuration.
     * @param config database configuration.
     */
    public void addDBConfig(String name, DataBaseConfiguration config) {
        String url = config.getDbUrl();
        
        if (url != null) {
            config.setDbUrl(url.replace("$basedir$", getBasePath()));
        }
        
        dbConfigs.put(name, config);
    }

    /**
     * Return a list of available query processor.
     *
     * @return list of query processor
     */
    public List<QueryProcessorConfiguration> getQueryProcessors() {
        return queryProcessors;
    }

    /**
     * Set the query processor list.
     *
     * @param queryProcessors the list of query processors to be set.
     */
    public void setQueryProcessors(List<QueryProcessorConfiguration> queryProcessors) {
        this.queryProcessors = queryProcessors;
    }

    /**
     * Add a new query processor.
     *
     * @param queryProcessorConfiguration query processor to be set.
     */
    public void addQueryProcessor(QueryProcessorConfiguration queryProcessorConfiguration) {
        queryProcessors.add(queryProcessorConfiguration);
    }

    /**
     * Return the base path calculated using the url supplier.
     *
     * @return the base path calculated using the url supplier.
     */
    public String getBasePath() {
        String basePath = null;
        
        if (urlSupplier != null) {
            basePath = urlSupplier.getURL();
        }
        
        if (basePath == null) {
            basePath = System.getProperty("basedir", "");
        }
        
        return basePath;
    }

    /**
     * Return the data access manager, created using the database configuration associated with the
     * registry context
     *
     * @return the data access manager
     */
    public DataAccessManager getDataAccessManager() {
        return dataAccessManager;
    }

    /**
     * Set the data access manager.
     *
     * @param dataAccessManager data access manager to be set.
     */
    public void setDataAccessManager(DataAccessManager dataAccessManager) {
        this.dataAccessManager = dataAccessManager;
    }

    /**
     * Return the handler manager.
     *
     * @return handler manager
     */
    public HandlerManager getHandlerManager() {
        return handlerManager;
    }

    /**
     * Return the handler manager.
     *
     * @param lifecyclePhase The name of the lifecycle phase.
     *
     * @return handler manager
     */
    public HandlerManager getHandlerManager(String lifecyclePhase) {
        return handlerManager.getHandlerManagerForPhase(lifecyclePhase);
    }

    /**
     * Return a custom edit manager, which is used by custom UI implementations.
     *
     * @return the CustomEditManager object.
     */
    public CustomEditManager getCustomEditManager() {
        return customEditManager;
    }

    /**
     * Set a custom edit manager.
     *
     * @param customEditManager the CustomEditManager to be set.
     */
    public void setCustomEditManager(CustomEditManager customEditManager) {
        this.customEditManager = customEditManager;
    }

    /**
     * Return a list of mounted remote instances.
     *
     * @return remote instance list.
     */
    public List<RemoteConfiguration> getRemoteInstances() {
        return remoteInstances;
    }

    /**
     * Set list of remote instances.
     *
     * @param remoteInstances the list of remote instances to be set.
     */
    public void setRemoteInstances(List<RemoteConfiguration> remoteInstances) {
        this.remoteInstances = remoteInstances;
    }

    /**
     * Return a list of mounted registry configurations (Mount object).
     *
     * @return a list of mount
     */
    public List<Mount> getMounts() {
        return mounts;
    }

    /**
     * Set a list of mounted registry configurations.
     *
     * @param mounts list of mount to be set.
     */
    public void setMounts(List<Mount> mounts) {
        this.mounts = mounts;
    }

    /**
     * Set profile storage path.
     *
     * @param path the path to be set
     */
    public void setProfilesPath(String path) {
        this.profilesPath = path;
    }

    /**
     * Return the profile storage path.
     *
     * @return path of the profile
     */
    public String getProfilesPath() {
        return this.profilesPath;
    }

    /**
     * Get the service storage path.
     *
     * @return the service path.
     */
    public String getServicePath() {
        return servicePath;
    }

    /**
     * Set the service storage path
     *
     * @param servicePath service path to be set.
     */
    public void setServicePath(String servicePath) {
        this.servicePath = servicePath;
        repositoryService.setServicePath(servicePath);
    }   

    /**
     * Method to obtain the logWriter instance.
     * @return the logWriter instance.
     */
    public LogWriter getLogWriter() {
        if (logWriter == null) {
            logWriter = new LogWriter(new LogQueue(), dataAccessManager);
            logWriter.start();
        }
        
        return logWriter;
    }

    /**
     * Method to set the logWriter instance.
     * @param logWriter the logWriter instance.
     */
    public void setLogWriter(LogWriter logWriter) {
        this.logWriter = logWriter;
    }

    /**
     * Method to determine whether a system resource (or collection) path has been registered.
     *
     * @param absolutePath the absolute path of the system resource (or collection)
     *
     * @return true if the system resource (or collection) path is registered or false if not.
     */
    public boolean isSystemResourcePathRegistered(String absolutePath) {
        return systemResourcePaths.contains(CurrentContext.getTenantId() + ":" + absolutePath);
    }

    /**
     * Method to register a system resource (or collection) path.
     *
     * @param absolutePath the absolute path of the system resource (or collection)
     */
    public void registerSystemResourcePath(String absolutePath) {
        systemResourcePaths.add(CurrentContext.getTenantId() + ":" + absolutePath);
    }

    /**
     * Method to determine whether caching is disabled for the given path.
     *
     * @param path the path to test
     *
     * @return true if caching is disabled or false if not.
     */
    public boolean isNoCachePath(String path) {
        for (Pattern noCachePath : noCachePaths) {
            if (noCachePath.matcher(path).matches()) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Method to register a no-cache path. If caching is disabled for a collection, all downstream
     * resources and collections won't be cached.
     *
     * @param path the path of a resource (or collection) for which caching is disabled.
     */
    public void registerNoCachePath(String path) {
        noCachePaths.add(Pattern.compile(Pattern.quote(path) + "($|" + RepositoryConstants.PATH_SEPARATOR + ".*|" + RepositoryConstants.URL_SEPARATOR + ".*)"));
    }
}
