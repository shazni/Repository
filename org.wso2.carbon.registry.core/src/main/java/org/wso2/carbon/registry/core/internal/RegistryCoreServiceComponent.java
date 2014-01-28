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

package org.wso2.carbon.registry.core.internal;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.registry.core.CollectionImpl;
import org.wso2.carbon.repository.Registry;
import org.wso2.carbon.repository.RepositoryConstants;
import org.wso2.carbon.repository.Resource;
import org.wso2.carbon.repository.SimulationService;
import org.wso2.carbon.repository.StatisticsCollector;
import org.wso2.carbon.registry.core.caching.CacheBackedRegistry;
import org.wso2.carbon.registry.core.caching.CachingHandler;
import org.wso2.carbon.registry.core.config.Mount;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.dao.LogsDAO;
import org.wso2.carbon.repository.exceptions.RepositoryException;
import org.wso2.carbon.repository.handlers.Handler;
import org.wso2.carbon.repository.handlers.HandlerManager;
import org.wso2.carbon.repository.handlers.filters.Filter;
import org.wso2.carbon.repository.handlers.filters.SimulationFilter;
import org.wso2.carbon.repository.utils.RepositoryUtils;
import org.wso2.carbon.registry.core.exceptions.RepositoryInitException;
import org.wso2.carbon.registry.core.jdbc.EmbeddedRegistry;
import org.wso2.carbon.registry.core.jdbc.EmbeddedRegistryService;
import org.wso2.carbon.registry.core.jdbc.handlers.HandlerLifecycleManager;
import org.wso2.carbon.registry.core.jdbc.handlers.builtin.*;
import org.wso2.carbon.registry.core.jdbc.handlers.filters.MediaTypeMatcher;
import org.wso2.carbon.registry.core.jdbc.handlers.filters.URLMatcher;
import org.wso2.carbon.registry.core.secure.AuthorizeRoleListener;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.service.RemoteRegistryService;
import org.wso2.carbon.registry.core.service.TenantRegistryLoader;
import org.wso2.carbon.registry.core.service.Utils;
import org.wso2.carbon.registry.core.session.CurrentSession;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.*;
import org.wso2.carbon.user.core.listener.AuthorizationManagerListener;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.*;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.management.ManagementPermission;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * The Registry Kernel Declarative Service Component.
 *
 * @scr.component name="registry.core.dscomponent" immediate="true"
 * @scr.reference name="user.realmservice.default"
 * interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 * @scr.reference name="statistics.collector"
 * interface="org.wso2.carbon.repository.StatisticsCollector"
 * cardinality="0..n" policy="dynamic" bind="setStatisticsCollector"
 * unbind="unsetStatisticsCollector"
 */
@SuppressWarnings("JavaDoc")
public class RegistryCoreServiceComponent {

    @SuppressWarnings("deprecation")
    private static org.wso2.carbon.registry.core.config.RegistryConfiguration registryConfig;

    private static RealmService realmService;

    private static final Log log = LogFactory.getLog(RegistryCoreServiceComponent.class);

    private static BundleContext bundleContext;

    private static Stack<ServiceRegistration> registrations = new Stack<ServiceRegistration>();

    private static RegistryService registryService;

    /**
     * Activates the Registry Kernel bundle.
     *
     * @param context the OSGi component context.
     */
    @SuppressWarnings("unused")
    protected void activate(ComponentContext context) {
        // for new cahing, every thread should has its own populated CC. During the deployment time we assume super tenant
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        carbonContext.setTenantDomain(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        carbonContext.setTenantId(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_ID);

        // Need permissions in order activate Registry
        SecurityManager securityManager = System.getSecurityManager();
        if(securityManager != null){
            securityManager.checkPermission(new ManagementPermission("control"));
        }
        try {
            bundleContext = context.getBundleContext();
            registryService = buildRegistryService();

            log.debug("Completed initializing the Registry Kernel");
            registrations.push(bundleContext.registerService(new String[]{RegistryService.class.getName(),
                                                                          org.wso2.carbon.repository.RegistryService.class.getName()},
                                                             registryService, null));
            registrations.push(bundleContext.registerService(SimulationService.class.getName(),
                    new DefaultSimulationService(), null));
            TenantDeploymentListenerImpl listener = new TenantDeploymentListenerImpl(registryService);
            registrations.push(bundleContext.registerService(
                    Axis2ConfigurationContextObserver.class.getName(),
                    listener, null));
            registrations.push(bundleContext.registerService(
                    AuthenticationObserver.class.getName(),
                    listener, null));
            registrations.push(bundleContext.registerService(
                    TenantRegistryLoader.class.getName(),
                    listener, null));

            log.debug("Registry Core bundle is activated ");
        } catch (Throwable e) {
            log.error("Failed to activate Registry Core bundle ", e);
        }
    }

    // Creates a queue service for logging events
    private void startLogWriter(RegistryService registryService) throws RepositoryException {

        Registry registry = registryService.getRegistry(
                CarbonConstants.REGISTRY_SYSTEM_USERNAME);
        
    	final RegistryContext registryContext = InternalUtils.getRegistryContext(registry);
//        final RegistryContext registryContext = ((EmbeddedRegistry) registry).getRegistryContext();
        if (bundleContext != null) {
            bundleContext.registerService(LogQueue.class.getName(),
                    registryContext.getLogWriter().getLogQueue(), null);
            bundleContext.registerService(WaitBeforeShutdownObserver.class.getName(),
                    new WaitBeforeShutdownObserver() {
                        public void startingShutdown() {
                            LogWriter logWriter = registryContext.getLogWriter();
                            LogQueue logQueue = logWriter.getLogQueue();
                            // if the log queue has work, interrupt the thread, and cause the log writer to flush the
                            // queue into the database.
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
                            // if the log queue has work, interrupt the thread, and cause the log writer to flush the
                            // queue into the database.
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
    @SuppressWarnings("unused")
    protected void deactivate(ComponentContext context) {
        while (!registrations.empty()) {
            registrations.pop().unregister();
        }
        registryService = null;
        bundleContext = null;
        log.debug("Registry Core bundle is deactivated ");
    }


    // Defines the fixed mount points.
    private void defineFixedMount(Registry registry, String path, boolean isSuperTenant)
            throws RepositoryException {
//        RegistryContext registryContext = registry.getRegistryContext();
//        String relativePath = RepositoryUtils.getRelativePath(registryContext, path);
//        String lookupPath = RepositoryUtils.getAbsolutePath(registryContext,
//                RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH +
//                        RepositoryConstants.SYSTEM_MOUNT_PATH) + "/" +
//                relativePath.replace("/", "-");
        String relativePath = RepositoryUtils.getRelativePath(registry, path);
        String lookupPath = RepositoryUtils.getAbsolutePath(registry,
                RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH +
                        RepositoryConstants.SYSTEM_MOUNT_PATH) + "/" +
                relativePath.replace("/", "-");
        Resource r;
        if (isSuperTenant) {
            if (!registry.resourceExists(lookupPath)) {
                return;
            }
            r = registry.get(lookupPath);
        } else {
            r = registry.newResource();
            r.setContent(RepositoryConstants.MOUNT_MEDIA_TYPE);
        }
        r.addProperty(RepositoryConstants.REGISTRY_FIXED_MOUNT, Boolean.toString(true));
        registry.put(lookupPath, r);
    }

    // Determines whether a given mount point is a fixed mount or not.
    private boolean isFixedMount(org.wso2.carbon.repository.Registry registry, String path, String targetPath,
                                 boolean isSuperTenant) throws RepositoryException {
//        RegistryContext registryContext = registry.getRegistryContext();
//        String relativePath = RepositoryUtils.getRelativePath(registryContext, path);
//        String lookupPath = RepositoryUtils.getAbsolutePath(registryContext,
//                RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH +
//                        RepositoryConstants.SYSTEM_MOUNT_PATH) + "/" +
//                relativePath.replace("/", "-");
    	
//        String relativePath = RepositoryUtils.getRelativePath(registryContext, path);
//        String lookupPath = RepositoryUtils.getAbsolutePath(registryContext,
//                RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH +
//                        RepositoryConstants.SYSTEM_MOUNT_PATH) + "/" +
//                relativePath.replace("/", "-");
        String relativePath = RepositoryUtils.getRelativePath(registry, path);
        String lookupPath = RepositoryUtils.getAbsolutePath(registry,
                RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH +
                        RepositoryConstants.SYSTEM_MOUNT_PATH) + "/" +
                relativePath.replace("/", "-");
        if (!registry.resourceExists(lookupPath)) {
            return false;
        }
        Resource r = registry.get(lookupPath);
        return (!isSuperTenant || targetPath.equals(r.getProperty("subPath")))
                && Boolean.toString(true).equals(
                r.getProperty(RepositoryConstants.REGISTRY_FIXED_MOUNT));
    }

    // Sets-up the media types for this instance.
    private static void setupMediaTypes(RegistryService registryService, int tenantId) {
        try {
            Registry registry = registryService.getConfigSystemRegistry(tenantId);
            MediaTypesUtils.getResourceMediaTypeMappings(registry);
            MediaTypesUtils.getCustomUIMediaTypeMappings(registry);
            MediaTypesUtils.getCollectionMediaTypeMappings(registry);
        } catch (RepositoryException e) {
            log.error("Unable to create fixed remote mounts.", e);
        }
    }

    // Do tenant-specific initialization.
    private static void initializeTenant(RegistryService registryService, int tenantId) {
        try {
            UserRegistry systemRegistry = registryService.getConfigSystemRegistry();
            if (systemRegistry.getRegistryContext() != null) {
                HandlerManager handlerManager =
                        systemRegistry.getRegistryContext().getHandlerManager();
                if (handlerManager instanceof HandlerLifecycleManager) {
                    ((HandlerLifecycleManager)handlerManager).init(tenantId);
                }
            }
            systemRegistry = registryService.getRegistry(
                    CarbonConstants.REGISTRY_SYSTEM_USERNAME, tenantId);
            InternalUtils.addMountCollection(systemRegistry);
            InternalUtils.registerMountPoints(systemRegistry, tenantId);
            new RegistryCoreServiceComponent().setupMounts(registryService, tenantId);
            setupMediaTypes(registryService, tenantId);

//            We need to set the tenant ID for current session. Otherwise the underlying operations fails
            try {
                CurrentSession.setTenantId(tenantId);

                RegistryContext registryContext = systemRegistry.getRegistryContext();
                // Adding collection to store user profile information.
//                RepositoryUtils.addUserProfileCollection(systemRegistry, RepositoryUtils.getAbsolutePath(
//                       registryContext, registryContext.getProfilesPath()));
                InternalUtils.addUserProfileCollection(systemRegistry, RepositoryUtils.getAbsolutePath(
                        registryService, registryContext.getProfilesPath()));
                // Adding collection to store services.
//                RepositoryUtils.addServiceStoreCollection(systemRegistry, RepositoryUtils.getAbsolutePath(
//                       registryContext, registryContext.getServicePath()));
                InternalUtils.addServiceStoreCollection(systemRegistry, RepositoryUtils.getAbsolutePath(
                        registryService, registryContext.getServicePath()));
                // Adding service configuration resources.
//                InternalUtils.addServiceConfigResources(systemRegistry);  // This seems not needed - Shazni
            } finally {
                CurrentSession.removeTenantId();
            }
        } catch (RepositoryException e) {
            log.error("Unable to initialize registry for tenant " + tenantId + ".", e);
        }
    }

    private void createPseudoLink(Registry registry, String path, String target,
                                  String targetSubPath) throws RepositoryException {
        Resource resource;
        if (registry.resourceExists(path)) {
            resource = registry.get(path);
            resource.addProperty(RepositoryConstants.REGISTRY_EXISTING_RESOURCE, "true");
        } else {
            resource = new CollectionImpl();
        }
        resource.addProperty(RepositoryConstants.REGISTRY_NON_RECURSIVE, "true");
        resource.addProperty(RepositoryConstants.REGISTRY_LINK_RESTORATION,
                path + RepositoryConstants.URL_SEPARATOR + target +
                        RepositoryConstants.URL_SEPARATOR + targetSubPath +
                        RepositoryConstants.URL_SEPARATOR + CurrentSession.getUser());
        resource.setMediaType(RepositoryConstants.LINK_MEDIA_TYPE);
        registry.put(path, resource);
        resource.discard();
    }

    // Sets-up the mounts for this instance.
    private void setupMounts(RegistryService registryService, int tenantId) {
        try {
            boolean isSuperTenant = (tenantId == MultitenantConstants.SUPER_TENANT_ID);
            Registry superTenantRegistry = registryService.getRegistry(
                    CarbonConstants.REGISTRY_SYSTEM_USERNAME);
            Registry registry = registryService.getRegistry(
                    CarbonConstants.REGISTRY_SYSTEM_USERNAME, tenantId);
            
        	RegistryContext registryContext = InternalUtils.getRegistryContext(registry);
            
//            RegistryContext registryContext = ((EmbeddedRegistry) superTenantRegistry).getRegistryContext();
            for (Mount mount : registryContext.getMounts()) {
                if (isFixedMount(registry, mount.getPath(), mount.getTargetPath(), isSuperTenant)) {
                    addFixedMount(tenantId, superTenantRegistry, mount.getPath());
                    continue;
                }
                if (!registry.resourceExists(mount.getPath())) {
                    if (isSuperTenant) {
                        superTenantRegistry.createLink(mount.getPath(), mount.getInstanceId(),
                                mount.getTargetPath());
                    } else {
                        createPseudoLink(registry, mount.getPath(), mount.getInstanceId(),
                                mount.getTargetPath());
                        addFixedMount(tenantId, superTenantRegistry, mount.getPath());
                    }
                    defineFixedMount(registry, mount.getPath(), isSuperTenant);
                } else if (mount.isOverwrite()) {
                    registry.delete(mount.getPath());
                    if (isSuperTenant) {
                        superTenantRegistry.createLink(mount.getPath(), mount.getInstanceId(),
                                mount.getTargetPath());
                    } else {
                        createPseudoLink(registry, mount.getPath(), mount.getInstanceId(),
                                mount.getTargetPath());
                        addFixedMount(tenantId, superTenantRegistry, mount.getPath());
                    }
                    defineFixedMount(registry, mount.getPath(), isSuperTenant);
                } else if (mount.isVirtual()) {
                    Resource r = registry.get(mount.getPath());
                    if (Boolean.toString(true).equals(r.getProperty(
                            RepositoryConstants.REGISTRY_LINK))) {
                        log.error("Unable to create virtual remote mount at location: " +
                                mount.getPath() + ". Virtual remote mounts can only be created " +
                                "for physical resources.");
                        continue;
                    } else {
                        if (isSuperTenant) {
                            superTenantRegistry.createLink(mount.getPath(), mount.getInstanceId(),
                                    mount.getTargetPath());
                        } else {
                            createPseudoLink(registry, mount.getPath(), mount.getInstanceId(),
                                    mount.getTargetPath());
                            addFixedMount(tenantId, superTenantRegistry, mount.getPath());
                        }
                        defineFixedMount(registry, mount.getPath(), isSuperTenant);
                    }
                } else {
                    log.error("Unable to create remote mount. A resource already exists at the " +
                            "given location: " + mount.getPath());
                    continue;
                }
                try {
                    if (!registry.resourceExists(mount.getPath())) {
                        log.debug("Target path does not exist for the given mount: " +
                                mount.getPath());
                    }
                } catch (Exception e) {
                    log.warn("Target path does not exist for the given mount: " +
                            mount.getPath(), e);
                }
            }
        } catch (RepositoryException e) {
            log.error("Unable to create fixed remote mounts.", e);
        }
    }

    private void addFixedMount(int tenantId, org.wso2.carbon.repository.Registry registry, String path)
            throws RepositoryException {
        CurrentSession.setCallerTenantId(tenantId);
        try {
//            RegistryContext registryContext = registry.getRegistryContext();
//            String relativePath = RepositoryUtils.getRelativePath(registryContext, path);
//            String lookupPath = RepositoryUtils.getAbsolutePath(registryContext,
//                    RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH +
//                            RepositoryConstants.SYSTEM_MOUNT_PATH) + "/" +
//                    relativePath.replace("/", "-");
          String relativePath = RepositoryUtils.getRelativePath(registry, path);
          String lookupPath = RepositoryUtils.getAbsolutePath(registry,
                  RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH +
                          RepositoryConstants.SYSTEM_MOUNT_PATH) + "/" +
                  relativePath.replace("/", "-");
          
            if (!registry.resourceExists(lookupPath)) {
                return;
            }
            Resource r = registry.get(lookupPath);
            String mountPath = r.getProperty("path");
            String target = r.getProperty("target");
            String targetSubPath = r.getProperty("subPath");
            String author = r.getProperty("author");

            try {
                if (log.isTraceEnabled()) {
                    log.trace("Creating the mount point. " +
                            "path: " + path + ", " +
                            "target: " + target + ", " +
                            "target sub path " + targetSubPath + ".");
                }
//                RepositoryUtils.registerHandlerForRemoteLinks(registry.getRegistryContext(),
//                        mountPath, target, targetSubPath, author);
                InternalUtils.registerHandlerForRemoteLinks(registry,
                        mountPath, target, targetSubPath, author);
            } catch (RepositoryException e) {
                log.error("Couldn't mount " + target + ".", e);
            }
        } finally {
            CurrentSession.removeCallerTenantId();
        }
    }

    /**
     * Method to register built-in handlers.
     * @param registryService the registry service instance.
     * @throws RepositoryException if an error occurred.
     */
    public void registerBuiltInHandlers(RegistryService registryService) throws RepositoryException {
        if (log.isTraceEnabled()) {
            log.trace("Registering the built-in handlers.");
        }
        Registry registry = registryService.getRegistry(
                CarbonConstants.REGISTRY_SYSTEM_USERNAME);
        
//        RegistryContext registryContext = InternalUtils.getRegistryContext(registry);
//        HandlerManager handlerManager = registryContext.getHandlerManager();

        HandlerManager handlerManager = registryService.getHandlerManager();
        
        if (log.isTraceEnabled()) {
            log.trace("Engaging the Operation Statistics Handler.");
        }
        // handler to record system statistics
        OperationStatisticsHandler systemStatisticsHandler = new OperationStatisticsHandler();
        URLMatcher systemStatisticsURLMatcher = new URLMatcher();
        systemStatisticsURLMatcher.setPattern(".*");
        handlerManager.addHandler(null, systemStatisticsURLMatcher, systemStatisticsHandler);

        if (log.isTraceEnabled()) {
            log.trace("Engaging the Comment URL Handler.");
        }
        // this handler will return the comment for a path ending as ;comments:<number>
        CommentURLHandler commentURLHandler = new CommentURLHandler();
        URLMatcher commentURLMatcher = new URLMatcher();
        commentURLMatcher.setGetPattern(".+;comments:[0-9]+");
        commentURLMatcher.setDeletePattern(".+;comments:[0-9]+");
        handlerManager.addHandler(
                new String[]{Filter.GET, Filter.DELETE}, commentURLMatcher, commentURLHandler);

        if (log.isTraceEnabled()) {
            log.trace("Engaging the Comment Collection URL Handler.");
        }
        // this will return all the comments for the path if the path ending as ;comments
        CommentCollectionURLHandler commentCollectionURLHandler = new CommentCollectionURLHandler();
        URLMatcher commentCollectionURLMatcher = new URLMatcher();
        commentCollectionURLMatcher.setGetPattern(".+;comments");
        handlerManager.addHandler(new String[]{Filter.GET},
                commentCollectionURLMatcher, commentCollectionURLHandler);

        if (log.isTraceEnabled()) {
            log.trace("Engaging the Rating URL Handler.");
        }
        // this will return the rating of a user if the path is ending as ;ratings:<username>
        RatingURLHandler ratingURLHandler = new RatingURLHandler();
        URLMatcher ratingURLMatcher = new URLMatcher();
        ratingURLMatcher.setGetPattern(".+;ratings:.+");
        handlerManager.addHandler(new String[]{Filter.GET}, ratingURLMatcher, ratingURLHandler);

        if (log.isTraceEnabled()) {
            log.trace("Engaging the Rating Collection URL Handler.");
        }
        // this will return all the ratings for a path if the path is ending as ;ratings
        RatingCollectionURLHandler ratingCollectionURLHandler = new RatingCollectionURLHandler();
        URLMatcher ratingCollectionURLMatcher = new URLMatcher();
        ratingCollectionURLMatcher.setGetPattern(".+;ratings");
        handlerManager.addHandler(
                new String[]{Filter.GET}, ratingCollectionURLMatcher, ratingCollectionURLHandler);

        if (log.isTraceEnabled()) {
            log.trace("Engaging the Tag URL Handler.");
        }
        // this will return the tags for a path  if the path is ending as
        // ;tags:<tag-name>:<user-name>
        TagURLHandler tagURLHandler = new TagURLHandler();
        URLMatcher tagURLMatcher = new URLMatcher();
        tagURLMatcher.setGetPattern(".+;.+:.+:.+");
        handlerManager.addHandler(new String[]{Filter.GET}, tagURLMatcher, tagURLHandler);

        if (log.isTraceEnabled()) {
            log.trace("Engaging the SQL Query Handler.");
        }
        // this will return the results for a custom query if the resource has the
        // media type:SQL_QUERY_MEDIA_TYPE
        SQLQueryHandler sqlQueryHandler = new SQLQueryHandler();
        MediaTypeMatcher sqlMediaTypeMatcher =
                new MediaTypeMatcher(RepositoryConstants.SQL_QUERY_MEDIA_TYPE);
        handlerManager.addHandler(
                new String[]{Filter.GET, Filter.PUT}, sqlMediaTypeMatcher, sqlQueryHandler);

        // Register Simulation Handler
        handlerManager.addHandler(null, new SimulationFilter(), new SimulationHandler(),
                HandlerLifecycleManager.DEFAULT_REPORTING_HANDLER_PHASE);

        if (log.isTraceEnabled()) {
            log.trace("Engaging the Cashing Registry Handler.");
        }
        // handler to cache registry operation results.
        CachingHandler cachingHandler = new CachingHandler();
        URLMatcher cachingURLMatcher = new URLMatcher();
        cachingURLMatcher.setPattern(".*");

        handlerManager.addHandler(null, cachingURLMatcher, cachingHandler);
        handlerManager.addHandler(null, cachingURLMatcher, cachingHandler,
                HandlerLifecycleManager.DEFAULT_REPORTING_HANDLER_PHASE);

        if (log.isTraceEnabled()) {
            log.trace("Engaging the RegexBase Restriction Handler.");
        }
        // handler to validate registry root's immediate directory paths prior to move and rename operations
        Handler regexBaseRestrictionHandler =  new RegexBaseRestrictionHandler();
        URLMatcher logUrlMatcher = new URLMatcher();
        logUrlMatcher.setPattern(".*");

        handlerManager.addHandler(new String[] {Filter.RENAME, Filter.MOVE}, logUrlMatcher, regexBaseRestrictionHandler,
                HandlerLifecycleManager.DEFAULT_SYSTEM_HANDLER_PHASE);

    }

    /**
     * Builds the OSGi service for this Registry instance.
     *
     * @return the OSGi service
     * @throws Exception if the creation of the Registry service fails.
     */
    @SuppressWarnings("deprecation")
    public RegistryService buildRegistryService() throws Exception {
        // this logic is used in check-in client
        updateRegistryConfiguration(getRegistryConfiguration());

        ServerConfiguration serverConfig = getServerConfiguration();
        setSystemTrustStore(serverConfig);

        RegistryService registryService;
        String registryRoot;
        if (org.wso2.carbon.registry.core.config.RegistryConfiguration.REMOTE_REGISTRY
                .equals(RegistryCoreServiceComponent.registryConfig.getRegistryType())) {
            registryService = getRemoteRegistryService(RegistryCoreServiceComponent.registryConfig);
            registryRoot = RegistryCoreServiceComponent.registryConfig.getValue(
                    org.wso2.carbon.registry.core.config.RegistryConfiguration.REGISTRY_ROOT);
        } else {
            registryService = getEmbeddedRegistryService();
            Utils.setEmbeddedRegistry((EmbeddedRegistryService) registryService);
            registryRoot = RegistryContext.getBaseInstance().getRegistryRoot();
        }

        UserRegistry systemRegistry = registryService.getRegistry(CarbonConstants.REGISTRY_SYSTEM_USERNAME);
        RegistryContext registryContext = systemRegistry.getRegistryContext();
        //Start LogWriter if only enables in registry.xml
            startLogWriter(registryService);
        setupMounts(registryService, MultitenantConstants.SUPER_TENANT_ID);
        registerBuiltInHandlers(registryService);
        // Media types must be set after setting up the mounts; so that the configuration
        // system registry is available.
        setupMediaTypes(registryService, MultitenantConstants.SUPER_TENANT_ID);
        // Adding collection to store user profile information.
        InternalUtils.addUserProfileCollection(systemRegistry, registryContext.getProfilesPath());
        // Adding collection to store services.
        InternalUtils.addServiceStoreCollection(systemRegistry, registryContext.getServicePath());
        // Adding service configuration resources.
        //InternalUtils.addServiceConfigResources(systemRegistry);  //   This seems not needed - Shazni
        if (log.isInfoEnabled()) {
            // Log registry configuration details at server start-up or reboot.
            String registryMode = "READ-WRITE";
            if (InternalUtils.isRegistryReadOnly(RegistryContext.getBaseInstance())) {
                registryMode = "READ-ONLY";
            }
            if (registryRoot != null) {
                log.info("Registry Root    : " + registryRoot);
            }
            log.info("Registry Mode    : " + registryMode);

            if (!org.wso2.carbon.registry.core.config.RegistryConfiguration.EMBEDDED_REGISTRY.equals(
                    RegistryCoreServiceComponent.registryConfig.getRegistryType())) {
                log.info("Registry Type    : "
                        + RegistryCoreServiceComponent.registryConfig.getRegistryType());
            }
        }

        return registryService;
    }

    // Get registry.xml instance.
    private File getConfigFile() throws RepositoryException {
        String configPath = CarbonUtils.getRegistryXMLPath();
        if (configPath != null) {
            File registryXML = new File(configPath);
            if (!registryXML.exists()) {
                String msg = "Registry configuration file (registry.xml) file does " +
                        "not exist in the path " + configPath;
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

    // Gets registry service for the Embedded Registry
    private RegistryService getEmbeddedRegistryService() throws Exception {

        InputStream configInputStream = new FileInputStream(getConfigFile());
        EmbeddedRegistryService embeddedRegistryService = new EmbeddedRegistryService() ;
        RegistryContext registryContext =
                RegistryContext.getBaseInstance(configInputStream, realmService, embeddedRegistryService);
        registryContext.setSetup(System.getProperty(RepositoryConstants.SETUP_PROPERTY) != null);
        embeddedRegistryService.init(registryContext);
        return embeddedRegistryService ;
        //return new EmbeddedRegistryService(registryContext);
    }

    // Gets registry service for the Remote Registry
    @SuppressWarnings("deprecation")
    private RegistryService getRemoteRegistryService(
            org.wso2.carbon.registry.core.config.RegistryConfiguration regConfig) throws Exception {

        String url =
                regConfig.getValue(org.wso2.carbon.registry.core.config.RegistryConfiguration.URL);
        String username = regConfig
                .getValue(org.wso2.carbon.registry.core.config.RegistryConfiguration.USERNAME);
        String password = regConfig
                .getValue(org.wso2.carbon.registry.core.config.RegistryConfiguration.PASSWORD);
        String chroot = regConfig
                .getValue(org.wso2.carbon.registry.core.config.RegistryConfiguration.REGISTRY_ROOT);

        return new RemoteRegistryService(url, username, password, realmService, chroot);
    }

    // Gets registry configuration instance.
    @SuppressWarnings("deprecation")
    private org.wso2.carbon.registry.core.config.RegistryConfiguration getRegistryConfiguration()
            throws RepositoryException {
        String path = CarbonUtils.getServerXml();
        return new org.wso2.carbon.registry.core.config.RegistryConfiguration(path);
    }

    // Sets system trust store.
    private void setSystemTrustStore(ServerConfiguration serverConfiguration)
            throws RepositoryException {
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

    // Get Server Configuration instance based on the carbon.xml.
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

    /**
     * Method to set the realm service used. This will be used when creating the registry context.
     * This method is called when the OSGi Realm Service is available.
     *
     * @param realmService the realm service.
     */
    protected void setRealmService(RealmService realmService) {
        // this is used in check-in client
        updateRealmService(realmService);
    }

    /**
     * This method is called when the current realm service becomes un-available.
     *
     * @param realmService the current realm service instance, to be used for any cleaning-up.
     */
    @SuppressWarnings("unused")
    protected void unsetRealmService(RealmService realmService) {
        updateRealmService(null);
    }

    /**
     * Method to set a register collector service.
     *
     * @param statisticsCollector the statistics collector service.
     */
    @SuppressWarnings("unused")
    protected synchronized void setStatisticsCollector(StatisticsCollector statisticsCollector) {
        RegistryContext.getBaseInstance().addStatisticsCollector(statisticsCollector);
        RepositoryUtils.addStatisticsCollector(statisticsCollector);
    }

    /**
     * Method to set a un-register collector service.
     *
     * @param statisticsCollector the statistics collector service.
     */
    @SuppressWarnings("unused")
    protected synchronized void unsetStatisticsCollector(StatisticsCollector statisticsCollector) {
        RegistryContext.getBaseInstance().removeStatisticsCollector(statisticsCollector);
    }

    // Method to update realm service.
    private static void updateRealmService(RealmService service) {
        realmService = service;
    }

    // Method to update registry configuration.
    @SuppressWarnings("deprecation")
    private static void updateRegistryConfiguration(
            org.wso2.carbon.registry.core.config.RegistryConfiguration config) {
        registryConfig = config;
    }

    @Deprecated
    @SuppressWarnings({"unused", "deprecation"})
    public static org.wso2.carbon.registry.core.config.RegistryConfiguration getRegistryConfig() {
        return registryConfig;
    }

    @Deprecated
    @SuppressWarnings({"unused", "deprecation"})
    public static void setRegistryConfig(
            org.wso2.carbon.registry.core.config.RegistryConfiguration registryConfig) {
        RegistryCoreServiceComponent.registryConfig = registryConfig;
    }

    /**
     * Method to add an authorize role listener.
     *
     * @param executionId   the execution order identifier
     * @param path          the resource (or collection) path
     * @param permission    the permission
     * @param executeAction the execute action required.
     * @param actions       the actions to which the role would be authorized.
     *
     * @see AuthorizeRoleListener
     */
    public static void addAuthorizeRoleListener(int executionId, String path, String permission,
                                                String executeAction, String[] actions) {
        if (bundleContext != null) {
            registrations.push(bundleContext.registerService(
                    AuthorizationManagerListener.class.getName(),
                    new AuthorizeRoleListener(executionId, path, permission, executeAction,
                            actions), null));
        }
    }

    /**
     * Gets the instance of the realm service in use.
     *
     * @return the instance of the realm service.
     */
    public static RealmService getRealmService() {
        return realmService;
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
    public static RegistryService getRegistryService() {
        return registryService;
    }

    /**
     * This is an implementation of the Simulation Service which is used to simulate handlers.
     */
    public static class DefaultSimulationService implements SimulationService {

        /**
         * Starts or stops simulation mode.
         *
         * @param simulation set this to <b>true</b> to start or <b>false</b> to stop simulation.
         */
        public void setSimulation(boolean simulation) {
            SimulationFilter.setSimulation(simulation);
        }

        /**
         * Retrieves results after running a simulation.
         *
         * @return the map of execution status of handlers. The key is the fully qualified handler
         *         class name and the values are a list of strings, which could contain,
         *         <b>Successful</b>, <b>Failed</b>, or the detail message of the exception that
         *         occurred.
         */
        public Map<String, List<String[]>> getSimulationStatus() {
            return SimulationHandler.getStatus();
        }
    }

    // An implementation of an Axis2 Configuration Context observer plus an Authentication Observer,
    // which is used to handle the requirement of initializing the registry space for a tenant.
    @SuppressWarnings("unused")
    private static class TenantDeploymentListenerImpl extends AbstractAxis2ConfigurationContextObserver
            implements TenantRegistryLoader, AuthenticationObserver {

        private RegistryService service;
        private List<Integer> initializedTenants = new LinkedList<Integer>();

        private TenantDeploymentListenerImpl(RegistryService service) {
            this.service = service;
        }

        private synchronized boolean canInitializeTenant(int tenantId) {
            if (initializedTenants.contains(tenantId)) {
                return false;
            }
            initializedTenants.add(tenantId);
            return true;
        }

        public void createdConfigurationContext(ConfigurationContext  configurationContext) {
            int tenantId = PrivilegedCarbonContext.getCurrentContext(configurationContext).getTenantId();
            loadTenantRegistry(tenantId);
        }

        public void startedAuthentication(int tenantId) {
            loadTenantRegistry(tenantId);
        }

        public void loadTenantRegistry(int tenantId) {
            // Only signed code can load the registry of a tenant, for security reasons. Accessing
            // the registry can be done by unsigned code, after the registry has been properly
            // loaded.
            if (tenantId != MultitenantConstants.INVALID_TENANT_ID &&
            		tenantId != MultitenantConstants.SUPER_TENANT_ID && 
            		canInitializeTenant(tenantId)) {
                RegistryCoreServiceComponent.initializeTenant(service, tenantId);
            }
        }

        public void completedAuthentication(int tenantId, boolean isSuccessful) {
            // Do nothing here.
        }
    }
}
