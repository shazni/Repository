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

package org.wso2.carbon.registry.core.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.registry.core.CommentImpl;
import org.wso2.carbon.registry.core.ResourceImpl;
import org.wso2.carbon.registry.core.config.DataBaseConfiguration;
import org.wso2.carbon.registry.core.config.RegistryConfigurationProcessor;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.config.RemoteConfiguration;
import org.wso2.carbon.registry.core.dataaccess.DataAccessManager;
import org.wso2.carbon.registry.core.exceptions.RepositoryDBException;
import org.wso2.carbon.registry.core.exceptions.RepositoryInitException;
import org.wso2.carbon.registry.core.exceptions.RepositoryServerException;
import org.wso2.carbon.registry.core.jdbc.dataaccess.JDBCDataAccessManager;
import org.wso2.carbon.registry.core.jdbc.handlers.HandlerManager;
import org.wso2.carbon.registry.core.jdbc.realm.RegistryRealm;
import org.wso2.carbon.registry.core.jdbc.utils.Transaction;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.InternalUtils;
import org.wso2.carbon.repository.Collection;
import org.wso2.carbon.repository.Comment;
import org.wso2.carbon.repository.Registry;
import org.wso2.carbon.repository.RepositoryConstants;
import org.wso2.carbon.repository.Resource;
import org.wso2.carbon.repository.exceptions.RepositoryException;
import org.wso2.carbon.repository.handlers.Handler;
import org.wso2.carbon.repository.handlers.filters.Filter;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

/**
 * This is a core class used by application that use registry in the embedded mode. This class is
 * used to create embedded registry instances for user sessions.
 * <p/>
 * UserRegistry is the embedded mode implementation of the Registry API. In this mode, all registry
 * accesses has to be done using a UserRegistry instance. And there has to be separate UserRegistry
 * instance for each user to access the registry. These UserRegistry instances has be obtained from
 * the {@link EmbeddedRegistry}. It is recommended to have only one EmbeddedRegistry instance per
 * application. But there can be exceptions, where it is required to maintain two or more registries
 * pointing to different data sources.
 * <p/>
 * Applications should initialize an EmbeddedRegistry instance at the start-up using following
 * code.
 * <p/>
 * InputStream configStream = new FileInputStream("/projects/registry.xml"); RegistryContext
 * registryContext = new RegistryContext(configStream); EmbeddedRegistry embeddedRegistry = new
 * EmbeddedRegistry(registryContext);
 * <p/>
 * After initializing an EmbeddedRegistry instance it should be stored in some globally accessible
 * location, so that it can be used by necessary modules to create UserRegistry instances. From
 * this, it is possible to create UserRegistry instances using various parameter combinations
 * documented in getXXRegistry methods.
 * <p/>
 * UserRegistry adminRegistry = embeddedRegistry.getRegistry("admin", "admin");
 *
 * @see EmbeddedRegistry
 * @see UserRegistry
 */
public class EmbeddedRegistryService implements RegistryService {

    private static final Log log = LogFactory.getLog(EmbeddedRegistryService.class);

    private EmbeddedRegistry embeddedRegistry;
    private RealmService realmService;
    private String chroot;
    
    /**
     * Shazni 
     * 
     */
    private String servicePath ;
    private HandlerManager handlerManager = null ;
    private List<Pattern> noCachePaths = new ArrayList<Pattern>();
    private boolean readOnly ;
    private List<RemoteConfiguration> remoteInstances = new ArrayList<RemoteConfiguration>();
    private boolean enableCache ;
    boolean clone ;

    /**
     * The registry context used by this registry service instance.
     */
    protected RegistryContext registryContext;

    /**private boolean readOnly ;
     * Instantiates the EmbeddedRegistry using the configuration given in the context and the given
     * UserRealm. Data source given in the context will be used for the resource store. User store
     * is accessed from the given UserRealm, which may point to a different source.
     *
     * @param context Registry Context containing the configuration.
     *
     * @throws RepositoryException if the creation of the embedded registry service fails.
     */
    public EmbeddedRegistryService(RegistryContext context) throws RepositoryException {
    	init(context);
    }

    /**
     * This constructor is used by the inherited InMemoryEmbeddedRegistry class as it has to be
     * instantiated using the default constructor.
     */
    public EmbeddedRegistryService() {
    }
    
    public void init(RegistryContext context) throws RepositoryException {
        this.registryContext = context;
        long start = System.nanoTime();
        configure(context.getRealmService());
        if (log.isInfoEnabled()) {
            try {
                Connection connection =
                        ((JDBCDataAccessManager) context.getDataAccessManager()).getDataSource()
                                .getConnection();
                try {
                    String jdbcURL = connection.getMetaData().getURL();
                    Iterator<String> dbConfigNames = context.getDBConfigNames();
                    while (dbConfigNames.hasNext()) {
                        String name = dbConfigNames.next();
                        DataBaseConfiguration configuration = context.getDBConfig(name);
                        if (jdbcURL != null && jdbcURL.equals(configuration.getDbUrl())) {
                            String dbConfigName = configuration.getConfigName();
                            DataBaseConfiguration defaultDBConfiguration =
                                    context.getDefaultDataBaseConfiguration();
                            if (dbConfigName != null && defaultDBConfiguration != null &&
                                    dbConfigName.equals(defaultDBConfiguration.getConfigName())) {
                                log.info("Configured Registry in " +
                                        ((System.nanoTime() - start) / 1000000L) + "ms");
                            } else {
                                log.info("Connected to mount at " + dbConfigName + " in " +
                                        ((System.nanoTime() - start) / 1000000L) + "ms");
                            }
                        }
                    }

                } finally {
                    connection.close();
                }
            } catch (SQLException ignored) {
                // We are only interested in logging the connection time in here. So, simply ignore
                // any exceptions that might result in the process.
            }
        }   	
    }

    /**
     * Method to configure the embedded registry service.
     *
     * @param realmService the user realm service instance.
     *
     * @throws RepositoryException if an error occurs.
     */
    protected void configure(RealmService realmService) throws RepositoryException {
        if (realmService == null) {
            String msg = "The realm service is not available.";
            log.error(msg);
            throw new RepositoryInitException(msg);
        }

        this.realmService = realmService;

        if (log.isTraceEnabled()) {
            log.trace("Configuring the embedded registry.");
        }

        DataAccessManager dataAccessManager = registryContext.getDataAccessManager();
        if (!(dataAccessManager instanceof JDBCDataAccessManager)) {
            String msg = "Failed to configure the embedded registry. Invalid data access manager.";
            log.error(msg);
            throw new RepositoryInitException(msg);
        }
//        NodeGroupLock.init(dataAccessManager);
        Transaction.init(dataAccessManager);

        try {
            if (log.isTraceEnabled()) {
                log.trace("Obtaining a cluster wide database lock.");
            }

//            NodeGroupLock.lock(NodeGroupLock.INITIALIZE_LOCK);

            if (log.isTraceEnabled()) {
                log.trace("Cluster wide database lock obtained successfully.");
            }
            if (registryContext.isSetup()) {
                if (!dataAccessManager.isDatabaseExisting()) {
                    // mean the database tables are needed
                    if (log.isTraceEnabled()) {
                        log.trace("Creating database tables.");
                    }
                    try {
                        dataAccessManager.createDatabase();
                    } catch (Exception ex) {
                        String msg = "Error occurred while creating the database";
                        log.error(msg);
                        throw new RepositoryDBException(msg, ex);
                    }

                    if (log.isTraceEnabled()) {
                        log.trace("Database tables created successfully.");
                    }
                } else if (log.isTraceEnabled()) {
                    log.trace("Continue the use of existing database tables");
                }
            } else if (log.isTraceEnabled()) {
                log.trace("Registry is not initialized in setup mode. " +
                        "Registry database tables will not be created.");
            }
            if (log.isTraceEnabled()) {
                log.trace("Creating the JDBC Registry instance ..");
            }

            embeddedRegistry = new EmbeddedRegistry(registryContext, realmService, this);
            chroot = embeddedRegistry.getRegistryContext().getRegistryRoot();
            handlerManager = embeddedRegistry.getRegistryContext().getHandlerManager();

            if (!registryContext.isClone()) {

                // adding initial system collection as system user of tenant 0
                if (log.isTraceEnabled()) {
                    log.trace("Adding mount collection and register mount points.");
                }
                UserRegistry systemRegistry = getSystemRegistry();

                InternalUtils.addMountCollection(systemRegistry);
                InternalUtils.registerMountPoints(systemRegistry,
                        MultitenantConstants.SUPER_TENANT_ID);
            }

            if (log.isTraceEnabled()) {
                log.trace("JDBC Registry instance created successfully.");
            }

        } finally {
            if (log.isTraceEnabled()) {
                log.trace("Releasing a cluster wide database lock.");
            }

//            NodeGroupLock.unlock(NodeGroupLock.INITIALIZE_LOCK);
            if (log.isTraceEnabled()) {
                log.trace("Cluster wide database lock released successfully.");
            }
        }
    }

    public UserRealm getUserRealm(int tenantId) throws RepositoryException {
        try {
            UserRealm realm = (UserRealm) realmService.getTenantUserRealm(tenantId);
            RegistryRealm regRealm = new RegistryRealm(realm);
            return regRealm;
        } catch (UserStoreException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryServerException(e.getMessage(), e);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.wso2.carbon.registry.core.service.RegistryService#setRegistryRoot(java.lang.String)
     * This method is not needed
     */
    public void setRegistryRoot(String registryRoot) {
    	chroot = registryRoot;
    }
    
    public String getRegistryRoot() {
    	return chroot;
    }
    
    public void setServicePath(String servicePath) {
    	this.servicePath = servicePath;
    }
    
    public String getServicePath() {
    	return servicePath;
    }
    
    public HandlerManager getHandlerManager() {
    	return handlerManager;
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
        noCachePaths.add(Pattern.compile(Pattern.quote(path) + "($|" +
                RepositoryConstants.PATH_SEPARATOR + ".*|" +
                RepositoryConstants.URL_SEPARATOR + ".*)"));
    }
    
    public Collection newCollection(String[] paths) {
    	return (Collection) new org.wso2.carbon.registry.core.CollectionImpl(paths) ;
    }
    
    public void setReadOnly(boolean readOnly) {
    	this.readOnly = readOnly ;
    }
    
    public boolean isReadOnly() {
    	return readOnly ;
    }
    
    /**
     * Set list of remote instances.
     *
     * @param remoteInstances the list of remote instances to be set.
     */
    public void setRemoteInstances(List<RemoteConfiguration> remoteInstances) {
        this.remoteInstances = remoteInstances;
    }
    
    public List<String> getRemoteInstanceIds() {
    	List<String> remoteInstanceIds = new ArrayList<String>() ;
    	
    	for(int i=0; i < remoteInstances.size(); i++) {
    		remoteInstanceIds.add(remoteInstances.get(i).getId());
    	}
    	
    	return remoteInstanceIds ;
    	//return remoteInstances ;
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
    }

	@Override
	public Resource newResource() {
		return new ResourceImpl();
	}

	@Override
	public boolean isClone() {
		return clone;
	}

	@Override
	public void setIsClone(boolean isClone) {
		clone = isClone ;
	}

	@Override
	public RealmService getRealmService() {
		return realmService;
	}

	@Override
	public boolean updateHandler(Element configElement,
			Registry registry, String lifecyclePhase) throws RepositoryException {
        boolean status = RegistryConfigurationProcessor.updateHandler(configElement,
                InternalUtils.getRegistryContext(registry),
                lifecyclePhase);
		return status;
	}
	
    /**
     * Method to determine whether a system resource (or collection) path has been registered.
     *
     * @param absolutePath the absolute path of the system resource (or collection)
     *
     * @return true if the system resource (or collection) path is registered or false if not.
     */
    public boolean isSystemResourcePathRegistered(String absolutePath) {
    	return registryContext.isSystemResourcePathRegistered(absolutePath);
    }

    /**
     * Method to register a system resource (or collection) path.
     *
     * @param absolutePath the absolute path of the system resource (or collection)
     */
    public void registerSystemResourcePath(String absolutePath) {
    	registryContext.registerSystemResourcePath(absolutePath);
    }

	@Override
	public void addHandler(String[] methods, Filter filter, Handler handler) {
		getHandlerManager().addHandler(methods, filter, handler);
	}

	@Override
	public void addHandler(String[] methods, Filter filter, Handler handler, String lifecyclePhase) {
		getHandlerManager().addHandler(methods, filter, handler, lifecyclePhase);
	}

	@Override
	public void removeHandler(String[] methods, Filter filter, Handler handler, String lifecyclePhase) {
		getHandlerManager().removeHandler(methods, filter, handler, lifecyclePhase);
	}

	@Override
	public void removeHandler(Handler handler, String lifecyclePhase) {
		getHandlerManager().removeHandler(handler, lifecyclePhase);
	}
	
    // Following methods are deprecated and eventually move out of the code ---------------------------------------------------------
	
	@Override
	public Comment newComment(String comment) {
		return new CommentImpl(comment);
	}
	
	// ----- What about the following ------------------
	
    public UserRegistry getRegistry(String userName, int tenantId, String chroot)
            throws RepositoryException {
        return getUserRegistry(userName, tenantId, chroot);
    }

    public UserRegistry getRegistry(String userName, String password, int tenantId, String chroot)
            throws RepositoryException {
        return getUserRegistry(userName, password, tenantId, chroot);
    }

    public UserRegistry getRegistry() throws RepositoryException {
        return getRegistry(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME);
    }

    public UserRegistry getRegistry(String userName) throws RepositoryException {
        return getRegistry(userName, MultitenantConstants.SUPER_TENANT_ID);
    }

    public UserRegistry getRegistry(String userName, int tenantId) throws RepositoryException {
        return getRegistry(userName, tenantId, null);
    }

    public UserRegistry getRegistry(String userName, String password) throws RepositoryException {
        return getRegistry(userName, password, MultitenantConstants.SUPER_TENANT_ID);
    }

    public UserRegistry getRegistry(String userName, String password, int tenantId)
            throws RepositoryException {
        return getRegistry(userName, password, tenantId, null);
    }

    public UserRegistry getLocalRepository() throws RepositoryException {
        return getLocalRepository(MultitenantConstants.SUPER_TENANT_ID);
    }

    public UserRegistry getLocalRepository(int tenantId) throws RepositoryException {
        return getSystemRegistry(tenantId, RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH);
    }

    public UserRegistry getConfigSystemRegistry(int tenantId) throws RepositoryException {
        return getSystemRegistry(tenantId, RepositoryConstants.CONFIG_REGISTRY_BASE_PATH);
    }

    public UserRegistry getConfigSystemRegistry() throws RepositoryException {
        return getConfigSystemRegistry(MultitenantConstants.SUPER_TENANT_ID);
    }

    public UserRegistry getConfigUserRegistry(String userName, int tenantId)
            throws RepositoryException {
        return getRegistry(userName, tenantId, RepositoryConstants.CONFIG_REGISTRY_BASE_PATH);
    }

    public UserRegistry getConfigUserRegistry(String userName, String password, int tenantId)
            throws RepositoryException {
        return getRegistry(userName, password, tenantId,
                RepositoryConstants.CONFIG_REGISTRY_BASE_PATH);
    }

    public UserRegistry getConfigUserRegistry() throws RepositoryException {
        return getConfigUserRegistry(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME);
    }

    public UserRegistry getConfigUserRegistry(String userName) throws RepositoryException {
        return getConfigUserRegistry(userName, MultitenantConstants.SUPER_TENANT_ID);
    }

    public UserRegistry getConfigUserRegistry(String userName, String password)
            throws RepositoryException {
        return getConfigUserRegistry(userName, password, MultitenantConstants.SUPER_TENANT_ID);
    }

    public UserRegistry getGovernanceSystemRegistry(int tenantId) throws RepositoryException {
        return getSystemRegistry(tenantId, RepositoryConstants.GOVERNANCE_REGISTRY_BASE_PATH);
    }

    public UserRegistry getGovernanceSystemRegistry() throws RepositoryException {
        return getGovernanceSystemRegistry(MultitenantConstants.SUPER_TENANT_ID);
    }

    public UserRegistry getGovernanceUserRegistry(String userName, int tenantId)
            throws RepositoryException {
        return getRegistry(userName, tenantId, RepositoryConstants.GOVERNANCE_REGISTRY_BASE_PATH);
    }

    public UserRegistry getGovernanceUserRegistry(String userName, String password, int tenantId)
            throws RepositoryException {
        return getRegistry(userName, password, tenantId,
                RepositoryConstants.GOVERNANCE_REGISTRY_BASE_PATH);
    }

    public UserRegistry getGovernanceUserRegistry() throws RepositoryException {
        return getGovernanceUserRegistry(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME);
    }

    public UserRegistry getGovernanceUserRegistry(String userName) throws RepositoryException {
        return getGovernanceUserRegistry(userName, MultitenantConstants.SUPER_TENANT_ID);
    }

    public UserRegistry getGovernanceUserRegistry(String userName, String password)
            throws RepositoryException {
        return getGovernanceUserRegistry(userName, password, MultitenantConstants.SUPER_TENANT_ID);
    }
    
    /**
     * Creates a UserRegistry instance for anonymous user. Permissions set for anonymous user will
     * be applied for all operations performed using this instance.
     *
     * @return UserRegistry for the anonymous user.
     * @throws RepositoryException
     */
    public UserRegistry getUserRegistry() throws RepositoryException {
        return getUserRegistry(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME,
                MultitenantConstants.SUPER_TENANT_ID);
    }

    /**
     * Returns a registry to be used for system operations. Human users should not be allowed log in
     * using this registry.
     *
     * @return User registry for system user.
     * @throws RepositoryException
     */
    public UserRegistry getSystemRegistry() throws RepositoryException {
        return getSystemRegistry(MultitenantConstants.SUPER_TENANT_ID);
    }

    /**
     * Returns a registry to be used for system operations. Human users should not be allowed log in
     * using this registry.
     *
     * @param tenantId tenant id of the user tenant.
     *
     * @return User registry for system user.
     * @throws RepositoryException
     */
    public UserRegistry getSystemRegistry(int tenantId) throws RepositoryException {

        return getSystemRegistry(tenantId, null);
    }

    /**
     * Returns a registry to be used for system operations. Human users should not be allowed log in
     * using this registry.
     *
     * @param tenantId tenant id of the user tenant.
     * @param chroot   to return a chrooted registry
     *
     * @return User registry for system user.
     * @throws RepositoryException
     */
    public UserRegistry getSystemRegistry(int tenantId, String chroot) throws RepositoryException {
        String username = CarbonConstants.REGISTRY_SYSTEM_USERNAME;

        return getUserRegistry(
                username,
                tenantId,
                chroot);
    }

    /**
     * Creates UserRegistry instances for normal users. Applications should use this method to
     * create UserRegistry instances, unless there is a specific need documented in other methods.
     * User name and the password will be authenticated by the EmbeddedRegistry before creating the
     * requested UserRegistry instance.
     *
     * @param userName User name of the user.
     * @param password Password of the user.
     *
     * @return UserRegistry instance for the given user.
     * @throws RepositoryException
     */
    public UserRegistry getUserRegistry(String userName, String password) throws RepositoryException {
        try {
            String tenantDomain = MultitenantUtils.getTenantDomain(userName);
            userName = MultitenantUtils.getTenantAwareUsername(userName);
            int tenantId = MultitenantConstants.SUPER_TENANT_ID;
            if (tenantDomain != null &&
            		!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
            }
            return getUserRegistry(userName, password, tenantId);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            String msg = "Failed in retrieving the tenant id for the tenant domain for " + userName;
            log.error(msg);
            throw new RepositoryServerException(msg, e);
        }
    }

    /**
     * Creates UserRegistry instances for normal users. Applications should use this method to
     * create UserRegistry instances, unless there is a specific need documented in other methods.
     * User name and the password will be authenticated by the EmbeddedRegistry before creating the
     * requested UserRegistry instance.
     *
     * @param userName User name of the user.
     * @param password Password of the user.
     * @param tenantId Tenant id of the user tenant.
     *
     * @return UserRegistry instance for the given user.
     * @throws RepositoryException
     */
    public UserRegistry getUserRegistry(String userName, String password, int tenantId)
            throws RepositoryException {
        return getUserRegistry(userName, password, tenantId, null);
    }

    /**
     * Creates UserRegistry instances for normal users. Applications should use this method to
     * create UserRegistry instances, unless there is a specific need documented in other methods.
     * User name and the password will be authenticated by the EmbeddedRegistry before creating the
     * requested UserRegistry instance.
     *
     * @param userName User name of the user.
     * @param password Password of the user.
     * @param tenantId Tenant id of the user tenant.
     * @param chroot   to return a chrooted registry
     *
     * @return UserRegistry instance for the given user.
     * @throws RepositoryException
     */
    public UserRegistry getUserRegistry(String userName, String password,
                                        int tenantId, String chroot) throws RepositoryException {
        String concatenatedChroot = InternalUtils.concatenateChroot(this.chroot, chroot);
        return new UserRegistry(userName,
                password,
                tenantId,
                embeddedRegistry,
                realmService,
                concatenatedChroot);
    }

    /**
     * Creates a UserRegistry instance for the given user. This method will NOT authenticate the
     * user before creating the UserRegistry instance. It assumes that the user is authenticated
     * outside the EmbeddedRegistry.
     *
     * @param userName User name of the user.
     *
     * @return UserRegistry instance for the given user.
     * @throws RepositoryException
     */
    public UserRegistry getUserRegistry(String userName) throws RepositoryException {
        try {
            String tenantDomain = MultitenantUtils.getTenantDomain(userName);
            userName = MultitenantUtils.getTenantAwareUsername(userName);
            int tenantId = MultitenantConstants.SUPER_TENANT_ID;
            if (tenantDomain != null &&
            		!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
            }
            return getUserRegistry(userName, tenantId);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            String msg = "Failed in retrieving the tenant id for the tenant for user " + userName;
            log.error(msg);
            throw new RepositoryServerException(msg, e);
        }
    }

    /**
     * Creates a UserRegistry instance for the given user. This method will NOT authenticate the
     * user before creating the UserRegistry instance. It assumes that the user is authenticated
     * outside the EmbeddedRegistry.
     *
     * @param userName User name of the user.
     * @param tenantId Tenant id of the user tenant.
     *
     * @return UserRegistry instance for the given user.
     * @throws RepositoryException
     */
    public UserRegistry getUserRegistry(String userName, int tenantId) throws RepositoryException {
        return getUserRegistry(userName, tenantId, null);
    }

    /**
     * Creates a UserRegistry instance for the given user. This method will NOT authenticate the
     * user before creating the UserRegistry instance. It assumes that the user is authenticated
     * outside the EmbeddedRegistry.
     *
     * @param userName User name of the user.
     * @param tenantId Tenant id of the user tenant.
     * @param chroot   to return a chrooted registry
     *
     * @return UserRegistry instance for the given user.
     * @throws RepositoryException
     */
    public UserRegistry getUserRegistry(String userName, int tenantId, String chroot)
            throws RepositoryException {
        String concatenatedChroot = InternalUtils.concatenateChroot(this.chroot, chroot);
        return new UserRegistry(userName,
                tenantId,
                embeddedRegistry,
                realmService,
                concatenatedChroot);
    }
}
