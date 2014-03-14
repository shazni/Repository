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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.repository.api.Collection;
import org.wso2.carbon.repository.api.Repository;
import org.wso2.carbon.repository.api.RepositoryConstants;
import org.wso2.carbon.repository.api.RepositoryService;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.handlers.Filter;
import org.wso2.carbon.repository.api.handlers.Handler;
import org.wso2.carbon.repository.api.utils.METHODS;
import org.wso2.carbon.repository.core.config.DataBaseConfiguration;
import org.wso2.carbon.repository.core.config.RemoteConfiguration;
import org.wso2.carbon.repository.core.config.RepositoryConfigurationProcessor;
import org.wso2.carbon.repository.core.config.RepositoryContext;
import org.wso2.carbon.repository.core.exceptions.RepositoryDBException;
import org.wso2.carbon.repository.core.exceptions.RepositoryInitException;
import org.wso2.carbon.repository.core.handlers.HandlerManager;
import org.wso2.carbon.repository.core.utils.InternalUtils;
import org.wso2.carbon.repository.spi.dataaccess.DataAccessManager;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

/**
 * This is a core class used by application that use repository in the embedded mode. This class is
 * used to create embedded repository instances.
 * <p/>
 * These EmbeddedRepository instances has be obtained from
 * the {@link EmbeddedRepositoryService}.There are various types of Repository instances a user may
 * request from the EmbeddedRepositoryService, such as system repository, config repository and
 * governance repository.
 * <p/>
 * Applications should retrieve a Repository instance through RepositoryService OSGI service
 * <p/>
 * Registry registryInstance = registryService.getRegistry();
 *
 * @see EmbeddedRepository
 */

public class EmbeddedRepositoryService implements RepositoryService {

    private static final Log log = LogFactory.getLog(EmbeddedRepositoryService.class);

    private String chroot;
    
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
    protected RepositoryContext repositoryContext;

    /**private boolean readOnly ;
     * Instantiates the EmbeddedRegistry using the configuration given in the context and the given
     * UserRealm. Data source given in the context will be used for the resource store. User store
     * is accessed from the given UserRealm, which may point to a different source.
     *
     * @param context Registry Context containing the configuration.
     *
     * @throws RepositoryException if the creation of the embedded registry service fails.
     */
    public EmbeddedRepositoryService(RepositoryContext context) throws RepositoryException {
    	init(context);
    }

    /**
     * This constructor is used by the inherited InMemoryEmbeddedRegistry class as it has to be
     * instantiated using the default constructor.
     */
    public EmbeddedRepositoryService() {
    }
    
    public void init(RepositoryContext context) throws RepositoryException {
        this.repositoryContext = context;
        long start = System.nanoTime();
        
        configure();
        
        if (log.isInfoEnabled()) {
            try {
                Connection connection = (context.getDataAccessManager()).getConnection();

                try {
                    String jdbcURL = connection.getMetaData().getURL();
                    Iterator<String> dbConfigNames = context.getDBConfigNames();
                    
                    while (dbConfigNames.hasNext()) {
                        String name = dbConfigNames.next();
                        DataBaseConfiguration configuration = context.getDBConfig(name);
                        
                        if (jdbcURL != null && jdbcURL.equals(configuration.getDbUrl())) {
                            String dbConfigName = configuration.getConfigName();
                            DataBaseConfiguration defaultDBConfiguration = context.getDefaultDataBaseConfiguration();
                            
                            if (dbConfigName != null && defaultDBConfiguration != null && dbConfigName.equals(defaultDBConfiguration.getConfigName())) {
                                log.info("Configured Registry in " + ((System.nanoTime() - start) / 1000000L) + "ms");
                            } else {
                                log.info("Connected to mount at " + dbConfigName + " in " + ((System.nanoTime() - start) / 1000000L) + "ms");
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
     * Method to configure the embedded repository service.
     *
     * @throws RepositoryException if an error occurs.
     */
    protected void configure() throws RepositoryException {

        if (log.isTraceEnabled()) {
            log.trace("Configuring the embedded registry.");
        }

        DataAccessManager dataAccessManager = repositoryContext.getDataAccessManager();
        
        if(dataAccessManager == null) {
			String msg = "Failed to configure the embedded registry. DataAccessManager is invalid";
			log.error(msg);
			throw new RepositoryInitException(msg);        	
        }

        Transaction.init(dataAccessManager);

        try {            
            if (repositoryContext.isSetup()) {
                if (!dataAccessManager.isDatabaseExisting()) {
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
                log.trace("Repositpry is not initialized in setup mode. Repository Database tables will not be created.");
            }
            
            if (log.isTraceEnabled()) {
                log.trace("Creating the JDBC Repository instance ..");
            }

            chroot = repositoryContext.getRepositoryRoot();
            handlerManager = repositoryContext.getHandlerManager();

            if (!repositoryContext.isClone()) {
                if (log.isTraceEnabled()) {
                    log.trace("Adding mount collection and repository mount points.");
                }
                
                Repository systemRepository = getSystemRepository();

                InternalUtils.addMountCollection(systemRepository);
                InternalUtils.registerMountPoints(systemRepository, MultitenantConstants.SUPER_TENANT_ID);
            }

            if (log.isTraceEnabled()) {
                log.trace("JDBC Registry instance created successfully.");
            }

        } finally {
            if (log.isTraceEnabled()) {
                log.trace("Releasing a cluster wide database lock.");
            }

            if (log.isTraceEnabled()) {
                log.trace("Cluster wide database lock released successfully.");
            }
        }
    }
    
    @Override
    public void setRepositoryRoot(String registryRoot) {
    	chroot = registryRoot;
    }
    
    @Override
    public String getRepositoryRoot() {
    	return chroot;
    }
    
    @Override
    public void setServicePath(String servicePath) {
    	this.servicePath = servicePath;
    }
    
    @Override
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
        noCachePaths.add(Pattern.compile(Pattern.quote(path) + "($|" + RepositoryConstants.PATH_SEPARATOR + ".*|" + RepositoryConstants.URL_SEPARATOR + ".*)"));
    }
    
    public Collection newCollection(String[] paths) {
    	return (Collection) new  CollectionImpl(paths) ;
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
	public boolean updateHandler(Element configElement, Repository registry, String lifecyclePhase) throws RepositoryException {
        boolean status = RepositoryConfigurationProcessor.updateHandler(configElement,
                InternalUtils.getRepositoryContext(registry),
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
	@Override
    public boolean isSystemResourcePathRegistered(String absolutePath) {
    	return repositoryContext.isSystemResourcePathRegistered(absolutePath);
    }

    /**
     * Method to register a system resource (or collection) path.
     *
     * @param absolutePath the absolute path of the system resource (or collection)
     */
	@Override
    public void registerSystemResourcePath(String absolutePath) {
    	repositoryContext.registerSystemResourcePath(absolutePath);
    }

	@Override
	public void addHandler(METHODS[] methods, Filter filter, Handler handler) {
		getHandlerManager().addHandler(methods, filter, handler);
	}

	@Override
	public void addHandler(METHODS[] methods, Filter filter, Handler handler, String lifecyclePhase) {
		getHandlerManager().addHandler(methods, filter, handler, lifecyclePhase);
	}

	@Override
	public void removeHandler(Handler handler) {
		getHandlerManager().removeHandler(handler);
	}

	@Override
	public void removeHandler(Handler handler, String lifecyclePhase) {
		getHandlerManager().removeHandler(handler, lifecyclePhase);
	}

	@Override
    public Repository getRepository() throws RepositoryException {
        return getRepository(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME);
    }

	@Override
    public Repository getRepository(String userName) throws RepositoryException {
        return getRepository(userName, MultitenantConstants.SUPER_TENANT_ID);
    }

	@Override
    public Repository getRepository(String userName, int tenantId) throws RepositoryException {
        return getRepository(userName, tenantId, null);
    }

	@Override
    public Repository getLocalRepository() throws RepositoryException {
        return getLocalRepository(MultitenantConstants.SUPER_TENANT_ID);
    }

	@Override
    public Repository getLocalRepository(int tenantId) throws RepositoryException {
        return getSystemRepository(tenantId, RepositoryConstants.LOCAL_REPOSITORY_BASE_PATH);
    }

	@Override
    public Repository getConfigSystemRepository(int tenantId) throws RepositoryException {
        return getSystemRepository(tenantId, RepositoryConstants.CONFIG_REGISTRY_BASE_PATH);
    }

	@Override
    public Repository getConfigSystemRepository() throws RepositoryException {
        return getConfigSystemRepository(MultitenantConstants.SUPER_TENANT_ID);
    }

	@Override
    public Repository getConfigUserRepository(String userName, int tenantId) throws RepositoryException {
        return getRepository(userName, tenantId, RepositoryConstants.CONFIG_REGISTRY_BASE_PATH);
    }

	@Override
    public Repository getConfigUserRepository() throws RepositoryException {
        return getConfigUserRepository(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME);
    }

	@Override
    public Repository getConfigUserRepository(String userName) throws RepositoryException {
        return getConfigUserRepository(userName, MultitenantConstants.SUPER_TENANT_ID);
    }

	@Override
    public Repository getGovernanceSystemRepository(int tenantId) throws RepositoryException {
        return getSystemRepository(tenantId, RepositoryConstants.GOVERNANCE_REGISTRY_BASE_PATH);
    }

	@Override
    public Repository getGovernanceSystemRepository() throws RepositoryException {
        return getGovernanceSystemRepository(MultitenantConstants.SUPER_TENANT_ID);
    }

	@Override
    public Repository getGovernanceUserRepository(String userName, int tenantId) throws RepositoryException {
        return getRepository(userName, tenantId, RepositoryConstants.GOVERNANCE_REGISTRY_BASE_PATH);
    }

	@Override
    public Repository getGovernanceUserRepository() throws RepositoryException {
        return getGovernanceUserRepository(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME);
    }

	@Override
    public Repository getGovernanceUserRepository(String userName) throws RepositoryException {
        return getGovernanceUserRepository(userName, MultitenantConstants.SUPER_TENANT_ID);
    }

    /**
     * Returns a registry to be used for system operations. Human users should not be allowed log in
     * using this registry.
     *
     * @return Repository			 repository instance
     * @throws RepositoryException
     */
	@Override
    public Repository getSystemRepository() throws RepositoryException {
        return getSystemRepository(MultitenantConstants.SUPER_TENANT_ID);
    }

    /**
     * Creates a UserRegistry instance for the given user. This method will NOT authenticate the
     * user before creating the UserRegistry instance. It assumes that the user is authenticated
     * outside the EmbeddedRegistry.
     *
     * @param userName 			User name of the user.
     * @param tenantId 			Tenant id of the user tenant.
     * @param chroot   			to return a chrooted registry
     *
     * @return Repository 		repository instance.
     * @throws RepositoryException
     */
    @Override
    public Repository getRepository(String userName, int tenantId, String chroot) throws RepositoryException {
        String concatenatedChroot = InternalUtils.concatenateChroot(this.chroot, chroot);
        
        EmbeddedRepository embeddedRegistry = new EmbeddedRepository(repositoryContext, this);
        embeddedRegistry.setTenantId(tenantId);
        embeddedRegistry.setUserName(userName);
        embeddedRegistry.setChRoot(concatenatedChroot);
        
        return embeddedRegistry ;
    }
    
    /**
     * Returns a registry to be used for system operations. Human users should not be allowed log in
     * using this registry.
     *
     * @param tenantId tenant id of the user tenant.
     *
     * @return Repository			 repository instance
     * @throws RepositoryException
     */
    public Repository getSystemRepository(int tenantId) throws RepositoryException {
        return getSystemRepository(tenantId, null);
    }

    /**
     * Returns a registry to be used for system operations. Human users should not be allowed log in
     * using this registry.
     *
     * @param tenantId 				tenant id of the user tenant.
     * @param chroot   				to return a chrooted registry
     *
     * @return Repository			 repository instance for the tenant
     * @throws RepositoryException
     */
    public Repository getSystemRepository(int tenantId, String chroot) throws RepositoryException {
        String username = CarbonConstants.REGISTRY_SYSTEM_USERNAME;

        return getRepository(username, tenantId, chroot);
    }

}
