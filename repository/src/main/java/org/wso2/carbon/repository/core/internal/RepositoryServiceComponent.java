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

package org.wso2.carbon.repository.core.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.management.ManagementPermission;
import java.util.*;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
//import org.osgi.service.component.annotations.Component;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.repository.api.Repository;
import org.wso2.carbon.repository.api.RepositoryConstants;
import org.wso2.carbon.repository.api.RepositoryService;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.SimulationService;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.handlers.Filter;
import org.wso2.carbon.repository.api.handlers.Handler;
import org.wso2.carbon.repository.api.utils.Method;
import org.wso2.carbon.repository.api.utils.RepositoryUtils;
import org.wso2.carbon.repository.core.CollectionImpl;
import org.wso2.carbon.repository.core.CurrentContext;
import org.wso2.carbon.repository.core.EmbeddedRepositoryService;
import org.wso2.carbon.repository.core.caching.CachingHandler;
import org.wso2.carbon.repository.core.config.Mount;
import org.wso2.carbon.repository.core.config.RepositoryConfiguration;
import org.wso2.carbon.repository.core.config.RepositoryContext;
import org.wso2.carbon.repository.core.exceptions.RepositoryInitException;
import org.wso2.carbon.repository.core.handlers.HandlerLifecycleManager;
import org.wso2.carbon.repository.core.handlers.HandlerManager;
import org.wso2.carbon.repository.core.handlers.builtin.MediaTypeMatcher;
import org.wso2.carbon.repository.core.handlers.builtin.OperationStatisticsHandler;
import org.wso2.carbon.repository.core.handlers.builtin.RegexBaseRestrictionHandler;
import org.wso2.carbon.repository.core.handlers.builtin.SQLQueryHandler;
import org.wso2.carbon.repository.core.handlers.builtin.SimulationFilter;
import org.wso2.carbon.repository.core.handlers.builtin.SimulationHandler;
import org.wso2.carbon.repository.core.handlers.builtin.URLMatcher;
import org.wso2.carbon.repository.core.utils.InternalConstants;
import org.wso2.carbon.repository.core.utils.InternalUtils;
import org.wso2.carbon.repository.core.utils.LogQueue;
import org.wso2.carbon.repository.core.utils.LogRecord;
import org.wso2.carbon.repository.core.utils.LogWriter;
import org.wso2.carbon.repository.core.utils.MediaTypesUtils;
import org.wso2.carbon.repository.spi.dao.LogsDAO;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.WaitBeforeShutdownObserver;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

@Component (
        name = "registry.core.dscomponent",
        description = "This service  component is responsible for retrieving the Repository OSGi",
        immediate = true
)
@Reference (
        name = "statistics.collector",
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        bind = "setStatisticsCollector",
        unbind = "unsetStatisticsCollector"
)
public class RepositoryServiceComponent {

    private static RepositoryConfiguration repositoryConfig;

    private static final Log log = LogFactory.getLog(RepositoryServiceComponent.class);

    private static BundleContext bundleContext;

    @SuppressWarnings("rawtypes")
	private static Stack<ServiceRegistration> registrations = new Stack<ServiceRegistration>();

    private static RepositoryService repositoryService;

    /**
     * Activates the Repository Kernel bundle.
     *
     * @param context the OSGi component context.
     */
    protected void activate(ComponentContext context) {
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        carbonContext.setTenantDomain(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        carbonContext.setTenantId(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_ID);

        // Need permissions in order to activate Registry
        SecurityManager securityManager = System.getSecurityManager();
        
        if(securityManager != null){
            securityManager.checkPermission(new ManagementPermission("control"));
        }
        
        try {
            bundleContext = context.getBundleContext();
            repositoryService = buildRepositoryService();

            log.debug("Completed initializing the Registry Kernel");
            
            registrations.push(bundleContext.registerService(new String[]{RepositoryService.class.getName()}, repositoryService, null));
            registrations.push(bundleContext.registerService(SimulationService.class.getName(), new DefaultSimulationService(), null));
            
            log.debug("Registry Core bundle is activated ");
        } catch (Throwable e) {
            log.error("Failed to activate Registry Core bundle ", e);
        }
    }

    // Creates a queue service for logging events
    private void startLogWriter(RepositoryService registryService) throws RepositoryException {

        Repository registry = registryService.getRepository(CarbonConstants.REGISTRY_SYSTEM_USERNAME);
        
    	final RepositoryContext registryContext = InternalUtils.getRepositoryContext(registry);
    	
        if (bundleContext != null) {
            bundleContext.registerService(LogQueue.class.getName(), registryContext.getLogWriter().getLogQueue(), null);
            bundleContext.registerService(WaitBeforeShutdownObserver.class.getName(),
                    new WaitBeforeShutdownObserver() {
                        public void startingShutdown() {
                            LogWriter logWriter = registryContext.getLogWriter();
                            LogQueue logQueue = logWriter.getLogQueue();
                            logWriter.setCanWriteLogs(false);
                            
                            if (logQueue.size() > 0) {
                                log.info("Writing logs ");
                                int queueLength = logQueue.size();
                                LogRecord[] logRecords = new LogRecord[queueLength];
                                
                                for (int a = 0; a < queueLength; a++) {
                                    LogRecord logRecord = (LogRecord) logQueue.poll();
                                    logRecords[a] = logRecord;
                                }
                                
                                LogsDAO logsDAO = registryContext.getDataAccessManager().getDAOManager().getLogsDAO();
                                
                                try {
                                    logsDAO.saveLogBatch(logRecords);
                                } catch (RepositoryException e) {
                                    log.error("Unable to save log records", e);
                                }
                            }
                        }

                        public boolean isTaskComplete() {
                            LogWriter logWriter = registryContext.getLogWriter();
                            LogQueue logQueue = logWriter.getLogQueue();

                            if (logQueue.size() > 0) {
                                logWriter.interrupt();
                                return false;
                            }
                            
                            return true;
                        }
                    }, null);
        }
    }

    /**
     * Deactivates the Registry Kernel bundle.
     *
     * @param context the OSGi component context.
     */
    protected void deactivate(ComponentContext context) {
        while (!registrations.empty()) {
            registrations.pop().unregister();
        }
        
        repositoryService = null;
        bundleContext = null;
        log.debug("Registry Core bundle is deactivated ");
    }

    // Defines the fixed mount points.
    private void defineFixedMount(Repository registry, String path, boolean isSuperTenant) throws RepositoryException {
        String relativePath = RepositoryUtils.getRelativePath(registry, path);
        String lookupPath = RepositoryUtils.getAbsolutePath(registry, RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH +
                        RepositoryConstants.SYSTEM_MOUNT_PATH) + "/" + relativePath.replace("/", "-");
        
        Resource r;
        
        if (isSuperTenant) {
            if (!registry.resourceExists(lookupPath)) {
                return;
            }
            
            r = registry.get(lookupPath);
        } else {
            r = registry.newResource();
            r.setContent(InternalConstants.MOUNT_MEDIA_TYPE);
        }
        
        r.addProperty(InternalConstants.REGISTRY_FIXED_MOUNT, Boolean.toString(true));
        registry.put(lookupPath, r);
    }

    // Determines whether a given mount point is a fixed mount or not.
    private boolean isFixedMount(Repository repository, String path, String targetPath, boolean isSuperTenant) throws RepositoryException {
        String relativePath = RepositoryUtils.getRelativePath(repository, path);
        String lookupPath = RepositoryUtils.getAbsolutePath(repository, RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH +
                        RepositoryConstants.SYSTEM_MOUNT_PATH) + "/" + relativePath.replace("/", "-");
        
        if (!repository.resourceExists(lookupPath)) {
            return false;
        }
        
        Resource r = repository.get(lookupPath);
        return (!isSuperTenant || targetPath.equals(r.getPropertyValue("subPath"))) && Boolean.toString(true).equals(r.getPropertyValue(InternalConstants.REGISTRY_FIXED_MOUNT));
    }

    // Sets-up the media types for this instance.
    private static void setupMediaTypes(RepositoryService registryService, int tenantId) {
        try {
            Repository registry = registryService.getConfigSystemRepository(tenantId);
            MediaTypesUtils.getResourceMediaTypeMappings(registry);
            MediaTypesUtils.getCustomUIMediaTypeMappings(registry);
            MediaTypesUtils.getCollectionMediaTypeMappings(registry);
        } catch (RepositoryException e) {
            log.error("Unable to create fixed remote mounts.", e);
        }
    }

    // Do tenant-specific initialization.
    public static void initializeTenant(RepositoryService registryService, int tenantId) {
        try {
            Repository systemRegistry = registryService.getConfigSystemRepository();
            
            if (InternalUtils.getRepositoryContext(systemRegistry) != null) {
                HandlerManager handlerManager = InternalUtils.getRepositoryContext(systemRegistry).getHandlerManager();
                if (handlerManager instanceof HandlerLifecycleManager) {
                    ((HandlerLifecycleManager)handlerManager).init(tenantId);
                }
            }
            
            systemRegistry = registryService.getRepository(CarbonConstants.REGISTRY_SYSTEM_USERNAME, tenantId);
            
            InternalUtils.addMountCollection(systemRegistry);
            InternalUtils.registerMountPoints(systemRegistry, tenantId);
            
            new RepositoryServiceComponent().setupMounts(registryService, tenantId);
            setupMediaTypes(registryService, tenantId);

//            We need to set the tenant ID for current context. Otherwise the underlying operations fails
            try {
                CurrentContext.setTenantId(tenantId);

                RepositoryContext registryContext = InternalUtils.getRepositoryContext(systemRegistry);

                InternalUtils.addUserProfileCollection(systemRegistry, RepositoryUtils.getAbsolutePath(registryService, registryContext.getProfilesPath()));
                InternalUtils.addServiceStoreCollection(systemRegistry, RepositoryUtils.getAbsolutePath(registryService, registryContext.getServicePath()));
            } finally {
                CurrentContext.removeTenantId();
            }
        } catch (RepositoryException e) {
            log.error("Unable to initialize registry for tenant " + tenantId + ".", e);
        }
    }

    private void createPseudoLink(Repository registry, String path, String instanceId, String targetPath) throws RepositoryException {
        Resource resource;
        
        if (registry.resourceExists(path)) {
            resource = registry.get(path);
            resource.addProperty(InternalConstants.REGISTRY_EXISTING_RESOURCE, "true");
        } else {
            resource = new CollectionImpl();
        }
        
        resource.addProperty(RepositoryConstants.REGISTRY_NON_RECURSIVE, "true");
        resource.addProperty(InternalConstants.REGISTRY_LINK_RESTORATION, path + RepositoryConstants.URL_SEPARATOR + instanceId +
                        RepositoryConstants.URL_SEPARATOR + targetPath + RepositoryConstants.URL_SEPARATOR + CurrentContext.getUser());
        resource.setMediaType(InternalConstants.LINK_MEDIA_TYPE);
        
        registry.put(path, resource);

    }

    private void setupMounts(RepositoryService repositoryService, int tenantId) {
        try {
            boolean isSuperTenant = (tenantId == MultitenantConstants.SUPER_TENANT_ID);
            
            Repository superTenantRepository = repositoryService.getRepository(CarbonConstants.REGISTRY_SYSTEM_USERNAME);
            Repository repository = repositoryService.getRepository(CarbonConstants.REGISTRY_SYSTEM_USERNAME, tenantId);
            
        	RepositoryContext repositoryContext = InternalUtils.getRepositoryContext(repository);
            
            for (Mount mount : repositoryContext.getMounts()) {
                if (isFixedMount(repository, mount.getPath(), mount.getTargetPath(), isSuperTenant)) {
                    addFixedMount(tenantId, superTenantRepository, mount.getPath());
                    continue;
                }
                
                if (!repository.resourceExists(mount.getPath())) {
                    if (isSuperTenant) {
                        superTenantRepository.createLink(mount.getPath(), mount.getInstanceId(), mount.getTargetPath());
                    } else {
                        createPseudoLink(repository, mount.getPath(), mount.getInstanceId(), mount.getTargetPath());
                        addFixedMount(tenantId, superTenantRepository, mount.getPath());
                    }
                    
                    defineFixedMount(repository, mount.getPath(), isSuperTenant);
                } else if (mount.isOverwrite()) {
                    repository.delete(mount.getPath());
                    
                    if (isSuperTenant) {
                        superTenantRepository.createLink(mount.getPath(), mount.getInstanceId(), mount.getTargetPath());
                    } else {
                        createPseudoLink(repository, mount.getPath(), mount.getInstanceId(), mount.getTargetPath());
                        addFixedMount(tenantId, superTenantRepository, mount.getPath());
                    }
                    
                    defineFixedMount(repository, mount.getPath(), isSuperTenant);
                } else if (mount.isVirtual()) {
                    Resource r = repository.get(mount.getPath());
                    
                    if (Boolean.toString(true).equals(r.getPropertyValue(RepositoryConstants.REGISTRY_LINK))) {
                        log.error("Unable to create virtual remote mount at location: " + mount.getPath() + ". Virtual remote mounts can only be created " +
                                "for physical resources.");
                        continue;
                    } else {
                        if (isSuperTenant) {
                            superTenantRepository.createLink(mount.getPath(), mount.getInstanceId(), mount.getTargetPath());
                        } else {
                            createPseudoLink(repository, mount.getPath(), mount.getInstanceId(), mount.getTargetPath());
                            addFixedMount(tenantId, superTenantRepository, mount.getPath());
                        }
                        
                        defineFixedMount(repository, mount.getPath(), isSuperTenant);
                    }
                } else {
                    log.error("Unable to create remote mount. A resource already exists at the " + "given location: " + mount.getPath());
                    continue;
                }
                try {
                    if (!repository.resourceExists(mount.getPath())) {
                        log.debug("Target path does not exist for the given mount: " + mount.getPath());
                    }
                } catch (Exception e) {
                    log.warn("Target path does not exist for the given mount: " + mount.getPath(), e);
                }
            }
        } catch (RepositoryException e) {
            log.error("Unable to create fixed remote mounts.", e);
        }
    }

    private void addFixedMount(int tenantId, Repository repository, String path) throws RepositoryException {
        CurrentContext.setCallerTenantId(tenantId);
        
        try {
        	String relativePath = RepositoryUtils.getRelativePath(repository, path);
        	String lookupPath = RepositoryUtils.getAbsolutePath(repository, RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH + RepositoryConstants.SYSTEM_MOUNT_PATH) + "/" +
        			relativePath.replace("/", "-");
          
            if (!repository.resourceExists(lookupPath)) {
                return;
            }
            
            Resource r = repository.get(lookupPath);
            
            String mountPath = r.getPropertyValue("path");
            String target = r.getPropertyValue("target");
            String targetSubPath = r.getPropertyValue("subPath");
            String author = r.getPropertyValue("author");

            try {
                if (log.isTraceEnabled()) {
                    log.trace("Creating the mount point. path: " + path + ", target: " + target + ", target sub path " + targetSubPath + ".");
                }

                InternalUtils.registerHandlerForRemoteLinks(repository, mountPath, target, targetSubPath, author);
            } catch (RepositoryException e) {
                log.error("Couldn't mount " + target + ".", e);
            }
        } finally {
            CurrentContext.removeCallerTenantId();
        }
    }

    /**
     * Method to register built-in handlers.
     * @param registryService the registry service instance.
     * @throws RepositoryException if an error occurred.
     */
    public void registerBuiltInHandlers(RepositoryService registryService) throws RepositoryException {
        if (log.isTraceEnabled()) {
            log.trace("Registering the built-in handlers.");
        }
        
        if(getRepositoryService() == null) {
        	setRepositoryService(registryService);
        }
        
        Repository registry = registryService.getRepository(CarbonConstants.REGISTRY_SYSTEM_USERNAME);

        RepositoryContext registryContext = InternalUtils.getRepositoryContext(registry);
        HandlerManager handlerManager = registryContext.getHandlerManager();
        
        if (log.isTraceEnabled()) {
            log.trace("Engaging the Operation Statistics Handler.");
        }
        
        // handler to record system statistics
        OperationStatisticsHandler systemStatisticsHandler = new OperationStatisticsHandler();
        URLMatcher systemStatisticsURLMatcher = new URLMatcher();
        systemStatisticsURLMatcher.setPattern(".*");
        Set<Filter> systemStatisticsFilterSet = new LinkedHashSet<Filter>();
        systemStatisticsFilterSet.add(systemStatisticsURLMatcher);
        systemStatisticsHandler.setFilters(systemStatisticsFilterSet);
        handlerManager.addHandler(null, systemStatisticsHandler);

        if (log.isTraceEnabled()) {
            log.trace("Engaging the SQL Query Handler.");
        }
        
        // this will return the results for a custom query if the resource has the
        // media type:SQL_QUERY_MEDIA_TYPE
        SQLQueryHandler sqlQueryHandler = new SQLQueryHandler();
        MediaTypeMatcher sqlMediaTypeMatcher = new MediaTypeMatcher(RepositoryConstants.SQL_QUERY_MEDIA_TYPE);
        Set<Filter> sqlQueryFilterSet = new LinkedHashSet<Filter>();
        sqlQueryFilterSet.add(sqlMediaTypeMatcher);
        sqlQueryHandler.setFilters(sqlQueryFilterSet);
        handlerManager.addHandler(new Method[]{Method.GET, Method.PUT}, sqlQueryHandler);

        // Register Simulation Handler
        Handler simulationHandler = new SimulationHandler();
        Set<Filter> simulationFilterSet = new LinkedHashSet<Filter>();
        simulationFilterSet.add(new SimulationFilter());
        simulationHandler.setFilters(simulationFilterSet);
        handlerManager.addHandler(null, simulationHandler, HandlerLifecycleManager.DEFAULT_REPORTING_HANDLER_PHASE);

        if (log.isTraceEnabled()) {
            log.trace("Engaging the Caching Registry Handler.");
        }
        
        // handler to cache registry operation results.
        CachingHandler cachingHandler = new CachingHandler();
        URLMatcher cachingURLMatcher = new URLMatcher();
        cachingURLMatcher.setPattern(".*");
        Set<Filter> cachingFilterSet = new LinkedHashSet<Filter>();
        cachingFilterSet.add(cachingURLMatcher);
        cachingHandler.setFilters(cachingFilterSet);
        handlerManager.addHandler(null, cachingHandler);
        handlerManager.addHandler(null, cachingHandler, HandlerLifecycleManager.DEFAULT_REPORTING_HANDLER_PHASE);

        if (log.isTraceEnabled()) {
            log.trace("Engaging the RegexBase Restriction Handler.");
        }
        // handler to validate registry root's immediate directory paths prior to move and rename operations
        Handler regexBaseRestrictionHandler =  new RegexBaseRestrictionHandler();
        URLMatcher logUrlMatcher = new URLMatcher();
        logUrlMatcher.setPattern(".*");
        Set<Filter> regexBaseRestrictionFilterSet = new LinkedHashSet<Filter>();
        regexBaseRestrictionFilterSet.add(logUrlMatcher);
        regexBaseRestrictionHandler.setFilters(regexBaseRestrictionFilterSet);

        handlerManager.addHandler(new Method[]{Method.RENAME, Method.MOVE}, regexBaseRestrictionHandler, HandlerLifecycleManager.DEFAULT_SYSTEM_HANDLER_PHASE);
    }

    /**
     * Builds the OSGi service for this Repository instance.
     *
     * @return the OSGi service
     * @throws Exception if the creation of the Registry service fails.
     */
    public RepositoryService buildRepositoryService() throws Exception {
        updateRepositoryConfiguration(getRegistryConfiguration());

        ServerConfiguration serverConfig = getServerConfiguration();
        setSystemTrustStore(serverConfig);

        RepositoryService repositoryService;
        String repositoryRoot;
        
        repositoryService = getEmbeddedRepositoryService();
        repositoryRoot = RepositoryContext.getBaseInstance().getRepositoryRoot();

        Repository systemRepository = repositoryService.getRepository(CarbonConstants.REGISTRY_SYSTEM_USERNAME);
        RepositoryContext registryContext = InternalUtils.getRepositoryContext(systemRepository);

        startLogWriter(repositoryService);
        
        setupMounts(repositoryService, MultitenantConstants.SUPER_TENANT_ID);
        
        registerBuiltInHandlers(repositoryService);
        
        // Media types must be set after setting up the mounts; so that the configuration
        // system registry is available.
        setupMediaTypes(repositoryService, MultitenantConstants.SUPER_TENANT_ID);
        
        InternalUtils.addUserProfileCollection(systemRepository, registryContext.getProfilesPath());
        
        InternalUtils.addServiceStoreCollection(systemRepository, registryContext.getServicePath());
        
        if (log.isInfoEnabled()) {
            String registryMode = "READ-WRITE";
            
            if (InternalUtils.isRegistryReadOnly(RepositoryContext.getBaseInstance())) {
                registryMode = "READ-ONLY";
            }
            
            if (repositoryRoot != null) {
                log.info("Registry Root    : " + repositoryRoot);
            }
            
            log.info("Registry Mode    : " + registryMode);

            if (!org.wso2.carbon.repository.core.config.RepositoryConfiguration.EMBEDDED_REGISTRY.equals(RepositoryServiceComponent.repositoryConfig.getRegistryType())) {
            	log.info("Registry Type    : " + RepositoryServiceComponent.repositoryConfig.getRegistryType());
            }
        }

        return repositoryService;
    }

    // Get registry.xml instance.
    private File getConfigFile() throws RepositoryException {
        String configPath = CarbonUtils.getRegistryXMLPath();
        
        if (configPath != null) {
            File registryXML = new File(configPath);
            
            if (!registryXML.exists()) {
                String msg = "Registry configuration file (registry.xml) file does not exist in the path " + configPath;
                log.error(msg);
                throw new RepositoryException(msg);
            }
            
            return registryXML;
        } else {
            String msg = "Cannot find registry.xml";
            log.error(msg);
            throw new RepositoryInitException(msg);
        }
    }

    private RepositoryService getEmbeddedRepositoryService() throws Exception {
        InputStream configInputStream = new FileInputStream(getConfigFile());
        
        EmbeddedRepositoryService embeddedRepositoryService = new EmbeddedRepositoryService() ;
        
        RepositoryContext repositoryContext = RepositoryContext.getBaseInstance(configInputStream, embeddedRepositoryService);
        repositoryContext.setSetup(System.getProperty(InternalConstants.SETUP_PROPERTY) != null);
        embeddedRepositoryService.init(repositoryContext);
        
        return embeddedRepositoryService ;
    }

    private RepositoryConfiguration getRegistryConfiguration() throws RepositoryException {
        String path = CarbonUtils.getServerXml();
        return new RepositoryConfiguration(path);
    }

    private void setSystemTrustStore(ServerConfiguration serverConfiguration) throws RepositoryException {
        if (serverConfiguration != null) {
            String location = serverConfiguration.getFirstProperty("Security.TrustStore.Location");
            String password = serverConfiguration.getFirstProperty("Security.TrustStore.Password");
            String type = serverConfiguration.getFirstProperty("Security.TrustStore.Type");

            if (location != null && password != null) {
                System.setProperty("javax.net.ssl.trustStoreType", type);
                System.setProperty("javax.net.ssl.trustStore", location);
                System.setProperty("javax.net.ssl.trustStorePassword", password);
            }
        }
    }

    private ServerConfiguration getServerConfiguration() throws RepositoryException {
        ServerConfiguration serverConfig = ServerConfiguration.getInstance();
        
        try {
            File carbonXML = new File(CarbonUtils.getServerXml());
            InputStream inputStream = new FileInputStream(carbonXML);
            
            try {
                serverConfig.init(inputStream);
            } finally {
                inputStream.close();
            }
        } catch (Exception e) {
            log.error("Error initializing the Server Configuration", e);
            throw new RepositoryInitException("Error initializing the Server Configuration", e);
        }
        
        return serverConfig;
    }

    private static void updateRepositoryConfiguration(RepositoryConfiguration config) {
        repositoryConfig = config;
    }

    public static void setRepositoryConfig(RepositoryConfiguration registryConfig) {
        RepositoryServiceComponent.repositoryConfig = registryConfig;
    }
    
    /**
     * Gets the instance of the bundle context in use.
     *
     * @return the instance of the bundle context.
     */
    public static BundleContext getBundleContext() {
        return bundleContext;
    }

    /**
     * Method to obtain an instance of the OSGi Registry Service.
     *
     * @return instance of the OSGi Registry Service.
     */
    public static RepositoryService getRepositoryService() {
        return repositoryService;
    }
    
    /**
     * Method to set the Repository Service.
     *
     * @return instance of the OSGi Registry Service.
     */
    private static void setRepositoryService(RepositoryService repoService) {
        repositoryService = repoService ;
    }

    /**
     * This is an implementation of the Simulation Service which is used to simulate handlers.
     */
    public static class DefaultSimulationService implements SimulationService {
        
		@Override
		public void startSimulation() {
			SimulationFilter.setSimulation(true);
		}

		@Override
		public void stopSimulation() {
			SimulationFilter.setSimulation(false);
		}

        /**
         * Retrieves results after running a simulation.
         *
         * @return the map of execution status of handlers. The key is the fully qualified handler
         *         class name and the values are a list of strings, which could contain,
         *         <b>Successful</b>, <b>Failed</b>, or the detail message of the exception that
         *         occurred.
         */
		@Override
        public Map<String, List<String[]>> getSimulationStatus() {
            return SimulationHandler.getStatus();
        }
    }
}
