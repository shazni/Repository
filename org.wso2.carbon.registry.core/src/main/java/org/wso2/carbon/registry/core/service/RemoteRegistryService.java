/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.registry.core.service;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.registry.core.CommentImpl;
import org.wso2.carbon.registry.core.ResourceImpl;
import org.wso2.carbon.registry.core.config.RegistryConfigurationProcessor;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.exceptions.RepositoryInitException;
import org.wso2.carbon.registry.core.exceptions.RepositoryServerException;
import org.wso2.carbon.registry.core.jdbc.EmbeddedRegistryService;
import org.wso2.carbon.registry.core.jdbc.handlers.HandlerLifecycleManager;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.InternalUtils;
import org.wso2.carbon.repository.Collection;
import org.wso2.carbon.repository.Comment;
import org.wso2.carbon.repository.Registry;
import org.wso2.carbon.repository.RepositoryConstants;
import org.wso2.carbon.repository.Resource;
import org.wso2.carbon.repository.config.RemoteConfiguration;
import org.wso2.carbon.repository.exceptions.RepositoryErrorCodes;
import org.wso2.carbon.repository.exceptions.RepositoryException;
import org.wso2.carbon.repository.exceptions.RepositoryUserContentException;
import org.wso2.carbon.repository.handlers.HandlerManager;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

/**
 * This is a core class used by application that use registry in the remote mode. This class is used
 * to create remote registry instances for user sessions. The class acts in a manner that is similar
 * to an {@link EmbeddedRegistryService}.
 */
public class RemoteRegistryService implements RegistryService {

    private static final Log log = LogFactory.getLog(RemoteRegistryService.class);
    private Registry registry;
    private RealmService realmService;
    private String chroot;
    private String url;
    
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
     * Creates a new remote registry service. This method is intended to be used at the remote end.
     *
     * @param registryURL URL to the registry.
     * @param username    the user name.
     * @param password    the password.
     *
     * @throws RepositoryException if an error occurred.
     */
    @SuppressWarnings("unused")
    public RemoteRegistryService(String registryURL, String username, String password)
            throws RepositoryException {
        this(registryURL, username, password, null);
    }

    /**
     * Creates a new remote registry service. This method is intended to be used at the remote end.
     *
     * @param registryURL the registry url.
     * @param username    the user name.
     * @param password    the password.
     * @param chroot      the chroot.
     *
     * @throws RepositoryException if an error occurred.
     */
    public RemoteRegistryService(String registryURL, String username, String password,
                                 String chroot) throws RepositoryException {
        this(registryURL, username, password, null, chroot, false);
    }

    /**
     * Creates a new remote registry service. This method is intended to be used at the local end.
     *
     * @param registryURL  the registry url.
     * @param username     the user name.
     * @param password     the password.
     * @param realmService the OSGi user realm service.
     * @param chroot       the chroot.
     *
     * @throws RepositoryException if an error occurred.
     */
    public RemoteRegistryService(String registryURL, String username,
                                 String password, RealmService realmService, String chroot)
            throws RepositoryException {
        this(registryURL, username, password, realmService, chroot, true);
    }

    /**
     * Creates a new remote registry service.
     *
     * @param registryURL           the registry url.
     * @param username              the user name.
     * @param password              the password.
     * @param realmService          the OSGi user realm service.
     * @param chroot                the chroot.
     * @param populateConfiguration whether the configuration must be populated or not.
     *
     * @throws RepositoryException if an error occurred.
     */
    public RemoteRegistryService(String registryURL, String username, String password,
                                 RealmService realmService, String chroot,
                                 boolean populateConfiguration) throws RepositoryException {
        try {
            RegistryContext.getBaseInstance(realmService, populateConfiguration, this);
            this.url = registryURL;
            this.realmService = realmService;
            this.chroot = chroot;

            registry = new RemoteRegistry(url, username, password, this);

            //Hack to authenticate the user with the remote registry as remote registry
            //doesn't provide a way to log in
            registry.get("/");

            if (realmService != null) {
                InternalUtils.getBootstrapRealm(realmService);
            }

            Registry systemRegistry = getSystemRegistry();
            InternalUtils.addMountCollection(systemRegistry);

        } catch (MalformedURLException e) {
            log.fatal("Registry URL is malformed, Registry configuration must be invalid", e);
            throw new RepositoryUserContentException("URL is malformed", e, RepositoryErrorCodes.INVALID_OR_MALFORMED_URL);
        } catch (Exception e) {
            log.fatal("Error initializing the remote registry, Registry " +
                    "configuration must be invalid", e);
            throw new RepositoryInitException("Error initializing the remote registry", e);
        }
    }

    public UserRegistry getUserRegistry() throws RepositoryException {
        return getUserRegistry(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME,
                MultitenantConstants.SUPER_TENANT_ID);
    }

    public UserRegistry getSystemRegistry() throws RepositoryException {
        return getSystemRegistry(MultitenantConstants.SUPER_TENANT_ID);
    }

    public UserRegistry getSystemRegistry(int tenantId) throws RepositoryException {
        return getSystemRegistry(tenantId, null);
    }


    public UserRegistry getSystemRegistry(int tenantId, String chroot) throws RepositoryException {
        String username = CarbonConstants.REGISTRY_SYSTEM_USERNAME;

        return getUserRegistry(username, tenantId, chroot);
    }

    public UserRegistry getUserRegistry(String username, String password) throws RepositoryException {
        return getUserRegistry(username, password, MultitenantConstants.SUPER_TENANT_ID);
    }

    public UserRegistry getUserRegistry(String username, String password, int tenantId)
            throws RepositoryException {
        return getUserRegistry(username, password, tenantId, null);
    }

    public UserRegistry getUserRegistry(String username, String password, int tenantId,
                                        String chroot)
            throws RepositoryException {
        try {

            RemoteRegistry userRemote = new RemoteRegistry(url, username, password, this);
            return new UserRegistry(username, tenantId, userRemote, realmService,
            		InternalUtils.concatenateChroot(this.chroot, chroot));

        } catch (MalformedURLException e) {
            log.fatal("Registry URL is malformed, Registry configuration must be invalid", e);
            throw new RepositoryUserContentException("URL is malformed", RepositoryErrorCodes.INVALID_OR_MALFORMED_URL);
        } catch (Exception e) {
            log.fatal("Error initializing the remote registry, User credentials must be invalid",
                    e);
            throw new RepositoryInitException("Error initializing the remote registry");
        }
    }

    public UserRegistry getUserRegistry(String userName) throws RepositoryException {
        try {
            String tenantDomain = MultitenantUtils.getTenantDomain(userName);
            userName = MultitenantUtils.getTenantAwareUsername(userName);
            int tenantId = MultitenantConstants.SUPER_TENANT_ID;
            if (tenantDomain != null &&
            		!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                if (realmService == null) {
                    String msg = "Unable to obtain an instance of a UserRegistry. The realm " +
                            "service is not available.";
                    log.error(msg);
                    throw new RepositoryServerException(msg);
                }
                tenantId = realmService.getTenantManager().getTenantId(tenantDomain);

            }
            return getUserRegistry(userName, tenantId);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            String msg = "Failed in retrieving the tenant id for the user " + userName;
            log.error(msg);
            throw new RepositoryServerException(msg, e);
        }
    }

    public UserRegistry getUserRegistry(String userName, int tenantId) throws RepositoryException {
        return getUserRegistry(userName, tenantId, null);
    }

    public UserRegistry getUserRegistry(String userName, int tenantId, String chroot)
            throws RepositoryException {
        return new UserRegistry(userName, tenantId, registry, realmService,
        		InternalUtils.concatenateChroot(this.chroot, chroot));
    }

    public UserRealm getUserRealm(int tenantId) throws RepositoryException {
        if (realmService == null) {
            String msg = "Unable to obtain an instance of a UserRealm. The realm service is not " +
                    "available.";
            log.error(msg);
            throw new RepositoryServerException(msg);
        }
        // first we will get an anonymous user registry associated with the tenant
        realmService.getBootstrapRealmConfiguration();
        UserRegistry anonymousUserRegistry = new UserRegistry(
                CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME,
                tenantId,
                registry,
                realmService,
                null);
        return anonymousUserRegistry.getUserRealm();
    }

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

	@Override
	public void setRegistryRoot(String registryRoot) {
		chroot = registryRoot;
	}

	@Override
	public String getRegistryRoot() {
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

	@Override
	public HandlerManager getHandlerManager() {
		return handlerManager;
	}

	@Override
	public boolean isNoCachePath(String path) {
        for (Pattern noCachePath : noCachePaths) {
            if (noCachePath.matcher(path).matches()) {
                return true;
            }
        }
        return false;
	}

	@Override
	public void registerNoCachePath(String path) {
        noCachePaths.add(Pattern.compile(Pattern.quote(path) + "($|" +
                RepositoryConstants.PATH_SEPARATOR + ".*|" +
                RepositoryConstants.URL_SEPARATOR + ".*)"));		
	}

	@Override
	public Collection newCollection(String[] paths) {
		return (Collection) new org.wso2.carbon.registry.core.CollectionImpl(paths) ;
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly ;		
	}

	@Override
	public boolean isReadOnly() {
		return readOnly ;
	}

	@Override
	public List<RemoteConfiguration> getRemoteInstances() {
		return remoteInstances ;
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
	public Comment newComment(String comment) {
		return new CommentImpl(comment);
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
	public boolean updateHandler(OMElement configElement,
			Registry registry, String lifecyclePhase) throws RepositoryException {
        boolean status = RegistryConfigurationProcessor.updateHandler(configElement,
                InternalUtils.getRegistryContext(registry),
                HandlerLifecycleManager.USER_DEFINED_HANDLER_PHASE);
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
    	RegistryContext registryContext = InternalUtils.getRegistryContext(registry);
    	
    	if(registryContext != null)
    		return registryContext.isSystemResourcePathRegistered(absolutePath);
    	return false ;
    }

    /**
     * Method to register a system resource (or collection) path.
     *
     * @param absolutePath the absolute path of the system resource (or collection)
     */
    public void registerSystemResourcePath(String absolutePath) {
    	RegistryContext registryContext = InternalUtils.getRegistryContext(registry);
    	
    	if(registryContext != null)
    		registryContext.registerSystemResourcePath(absolutePath);
    }
}
