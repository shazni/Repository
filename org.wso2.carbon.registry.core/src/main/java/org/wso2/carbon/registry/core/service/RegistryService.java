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

package org.wso2.carbon.registry.core.service;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.axiom.om.OMElement;
import org.wso2.carbon.repository.Collection;
import org.wso2.carbon.repository.Comment;
import org.wso2.carbon.repository.Registry;
import org.wso2.carbon.repository.Resource;
import org.wso2.carbon.repository.config.RemoteConfiguration;
import org.wso2.carbon.repository.exceptions.RepositoryException;
import org.wso2.carbon.repository.handlers.HandlerManager;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * This interface can be used to implement an OSGi Service of the Registry. By doing so, the
 * registry would become accessible beyond the Registry Kernel. The OSGi service also can be used as
 * means to make sure that no registry related logic executes before the registry has been fully
 * initialized, and made operational. Also, when the registry is no longer in service, all external
 * entities that use the registry would also become automatically suspended.
 */
@SuppressWarnings("unused")
public interface RegistryService extends org.wso2.carbon.repository.RegistryService {

    /**
     * Creates a UserRegistry instance for anonymous user. Permissions set for anonymous user will
     * be applied for all operations performed using this instance.
     * <p/>
     * This registry instance belongs to the super tenant of the system.
     *
     * @return UserRegistry for the anonymous user.
     * @throws RepositoryException if an error occurs
     */
    @Deprecated UserRegistry getUserRegistry() throws RepositoryException;

    /**
     * Returns a registry to be used for system operations. Human users should not be allowed log in
     * using this registry.
     * <p/>
     * This registry instance belongs to the super tenant of the system.
     *
     *
     * @return User registry for system user.
     * @throws RepositoryException if an error occurs
     */
    @Deprecated UserRegistry getSystemRegistry() throws RepositoryException;

    /**
     * Returns a registry to be used for system operations. Human users should not be allowed log in
     * using this registry.
     *
     * @param tenantId the tenant id of the system. The tenant id '0', corresponds to the super
     *                 tenant of the system, whereas identifiers greater than '0' correspond to
     *                 valid tenants.
     *
     * @return User registry for system user.
     * @throws RepositoryException if an error occurs
     */
    @Deprecated UserRegistry getSystemRegistry(int tenantId) throws RepositoryException;

    /**
     * Returns a registry to be used for system operations. Human users should not be allowed log in
     * using this registry.
     *
     * @param tenantId the tenant id of the system. The tenant id '0', corresponds to the super
     *                 tenant of the system, whereas identifiers greater than '0' correspond to
     *                 valid tenants.
     * @param chroot   to return a chrooted registry. The whole registry can be accessed by using
     *                 the chroot, '/', and a subset of the registry can be accessed by using a
     *                 chroot, '/x/y/z'. For example, the repository of the configuration local
     *                 registry can be obtained from '/_system/config/repository'.
     *
     * @return User registry for system user.
     * @throws RepositoryException if an error occurs
     */
    @Deprecated UserRegistry getSystemRegistry(int tenantId, String chroot)
            throws RepositoryException;

    /**
     * Creates UserRegistry instances for normal users. Applications should use this method to
     * create UserRegistry instances, unless there is a specific need documented in other methods.
     * User name and the password will be authenticated by the EmbeddedRegistry before creating the
     * requested UserRegistry instance.
     * <p/>
     * This registry instance belongs to the super tenant of the system.
     *
     *
     * @param userName User name of the user.
     * @param password Password of the user.
     *
     * @return UserRegistry instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    @Deprecated UserRegistry getUserRegistry(String userName, String password)
            throws RepositoryException;

    /**
     * Creates a Registry instance for the given user. This method will NOT authenticate the user
     * before creating the UserRegistry instance. It assumes that the user is authenticated outside
     * the EmbeddedRegistry.
     * <p/>
     * This registry instance belongs to the super tenant of the system.
     *
     *
     * @param userName User name of the user.
     *
     * @return UserRegistry instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    @Deprecated UserRegistry getUserRegistry(String userName) throws RepositoryException;

    /**
     * Creates UserRegistry instances for normal users. Applications should use this method to
     * create UserRegistry instances, unless there is a specific need documented in other methods.
     * User name and the password will be authenticated by the EmbeddedRegistry before creating the
     * requested UserRegistry instance.
     *
     * @param userName User name of the user.
     * @param password Password of the user.
     * @param tenantId tenant id of the user tenant. The tenant id '0', corresponds to the super
     *                 tenant of the system, whereas identifiers greater than '0' correspond to
     *                 valid tenants.
     *
     * @return UserRegistry instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    @Deprecated UserRegistry getUserRegistry(String userName, String password, int tenantId)
            throws RepositoryException;

    /**
     * Creates UserRegistry instances for normal users. Applications should use this method to
     * create UserRegistry instances, unless there is a specific need documented in other methods.
     * User name and the password will be authenticated by the EmbeddedRegistry before creating the
     * requested UserRegistry instance.
     *
     * @param userName User name of the user.
     * @param password Password of the user.
     * @param tenantId tenant id of the user tenant. The tenant id '0', corresponds to the super
     *                 tenant of the system, whereas identifiers greater than '0' correspond to
     *                 valid tenants.
     * @param chroot   to return a chrooted registry. The whole registry can be accessed by using
     *                 the chroot, '/', and a subset of the registry can be accessed by using a
     *                 chroot, '/x/y/z'. For example, the repository of the configuration local
     *                 registry can be obtained from '/_system/config/repository'.
     *
     * @return UserRegistry instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    @Deprecated UserRegistry getUserRegistry(String userName, String password,
                                             int tenantId, String chroot) throws RepositoryException;


    /**
     * Creates a Registry instance for the given user with tenant id. This method will NOT
     * authenticate the user before creating the Registry instance. It assumes that the user is
     * authenticated outside the registry service.
     *
     * @param userName User name of the user.
     * @param tenantId tenant id of the user tenant. The tenant id '0', corresponds to the super
     *                 tenant of the system, whereas identifiers greater than '0' correspond to
     *                 valid tenants.
     *
     * @return UserRegistry instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    @Deprecated UserRegistry getUserRegistry(String userName, int tenantId)
            throws RepositoryException;

    /**
     * Creates a Registry instance for the given user with tenant id. This method will NOT
     * authenticate the user before creating the Registry instance. It assumes that the user is
     * authenticated outside the registry service.
     *
     * @param userName User name of the user.
     * @param tenantId tenant id of the user tenant. The tenant id '0', corresponds to the super
     *                 tenant of the system, whereas identifiers greater than '0' correspond to
     *                 valid tenants.
     * @param chroot   to return a chrooted registry. The whole registry can be accessed by using
     *                 the chroot, '/', and a subset of the registry can be accessed by using a
     *                 chroot, '/x/y/z'. For example, the repository of the configuration local
     *                 registry can be obtained from '/_system/config/repository'.
     *
     * @return UserRegistry instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    @Deprecated UserRegistry getUserRegistry(String userName, int tenantId, String chroot)
            throws RepositoryException;

    /**
     * This will return a realm specific to the tenant.
     *
     * @param tenantId tenant id of the user tenant. The tenant id '0', corresponds to the super
     *                 tenant of the system, whereas identifiers greater than '0' correspond to
     *                 valid tenants.
     *
     * @return UserRealm instance associated with the tenant id.
     * @throws RepositoryException if an error occurs
     */
    UserRealm getUserRealm(int tenantId) throws RepositoryException;

    ////////////////////////////////////////////////////////
    // According to the registry separation concept, there
    // are 3 different registries..
    // 1. Local data repository - to store per instance
    //    data
    // 2. Configuration registry - to store data which
    //    should be shared among all nodes in a cluster
    // 3. Governance registry - to store data which should
    //    be shared through the platform
    //
    // The following methods can be used to access the above
    // three registries separately
    ////////////////////////////////////////////////////////

    /**
     * Creates a Registry instance for anonymous user which contains the entire registry tree
     * starting from '/'. Permissions set for anonymous user will be applied for all operations
     * performed using this instance.
     * <p/>
     * This registry instance belongs to the super tenant of the system.
     *
     *
     * @return Complete Registry for the anonymous user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getRegistry() throws RepositoryException;

    /**
     * Creates a Registry instance for anonymous user which contains the entire registry tree
     * starting from '/'. User name and the password will be authenticated by the EmbeddedRegistry
     * before creating the requested Registry instance.
     * <p/>
     * This registry instance belongs to the super tenant of the system.
     *
     *
     * @param userName User name of the user.
     * @param password Password of the user.
     *
     * @return Complete Registry instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getRegistry(String userName, String password) throws RepositoryException;

    /**
     * Creates a Registry instance for anonymous user which contains the entire registry tree
     * starting from '/'. This method will NOT authenticate the user before creating the Registry
     * instance. It assumes that the user is authenticated outside the EmbeddedRegistry.
     * <p/>
     * This registry instance belongs to the super tenant of the system.
     *
     *
     * @param userName User name of the user.
     *
     * @return Complete Registry instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getRegistry(String userName) throws RepositoryException;

    /**
     * Creates a Registry instance for anonymous user which contains the entire registry tree
     * starting from '/'. User name and the password will be authenticated by the EmbeddedRegistry
     * before creating the requested Registry instance. This method can be used to obtain instances
     * of Registry belonging to users of multiple tenants.
     *
     * @param userName User name of the user.
     * @param password Password of the user.
     * @param tenantId tenant id of the user tenant. The tenant id '0', corresponds to the super
     *                 tenant of the system, whereas identifiers greater than '0' correspond to
     *                 valid tenants.
     *
     * @return Complete Registry instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getRegistry(String userName, String password, int tenantId)
            throws RepositoryException;

    /**
     * Creates a Registry instance for anonymous user which contains the entire registry tree
     * starting from '/'. User name and the password will be authenticated by the EmbeddedRegistry
     * before creating the requested Registry instance. This method can be used to obtain instances
     * of Registry belonging to users of multiple tenants. The returned Registry will be chrooted to
     * the given path, making it possible to use relative paths.
     *
     * @param userName User name of the user.
     * @param password Password of the user.
     * @param tenantId tenant id of the user tenant. The tenant id '0', corresponds to the super
     *                 tenant of the system, whereas identifiers greater than '0' correspond to
     *                 valid tenants.
     * @param chroot   to return a chrooted registry. The whole registry can be accessed by using
     *                 the chroot, '/', and a subset of the registry can be accessed by using a
     *                 chroot, '/x/y/z'. For example, the repository of the configuration local
     *                 registry can be obtained from '/_system/config/repository'.
     *
     * @return Complete Registry instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getRegistry(String userName, String password,
                             int tenantId, String chroot) throws RepositoryException;


    /**
     * Creates a Registry instance for anonymous user which contains the entire registry tree
     * starting from '/'. This method will NOT authenticate the user before creating the Registry
     * instance. It assumes that the user is authenticated outside the EmbeddedRegistry. This method
     * can be used to obtain instances of Registry belonging to users of multiple tenants.
     *
     * @param userName User name of the user.
     * @param tenantId tenant id of the user tenant. The tenant id '0', corresponds to the super
     *                 tenant of the system, whereas identifiers greater than '0' correspond to
     *                 valid tenants.
     *
     * @return Complete Registry instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getRegistry(String userName, int tenantId) throws RepositoryException;

    /**
     * Creates a Registry instance for anonymous user which contains the entire registry tree
     * starting from '/'. This method will NOT authenticate the user before creating the Registry
     * instance. It assumes that the user is authenticated outside the EmbeddedRegistry. This method
     * can be used to obtain instances of Registry belonging to users of multiple tenants. The
     * returned Registry will be chrooted to the given path, making it possible to use relative
     * paths.
     *
     * @param userName User name of the user.
     * @param tenantId tenant id of the user tenant. The tenant id '0', corresponds to the super
     *                 tenant of the system, whereas identifiers greater than '0' correspond to
     *                 valid tenants.
     * @param chroot   to return a chrooted registry. The whole registry can be accessed by using
     *                 the chroot, '/', and a subset of the registry can be accessed by using a
     *                 chroot, '/x/y/z'. For example, the repository of the configuration local
     *                 registry can be obtained from '/_system/config/repository'.
     *
     * @return Complete Registry instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getRegistry(String userName, int tenantId, String chroot) throws RepositoryException;

    /**
     * Returns a registry to be used for node-specific system operations. Human users should not be
     * allowed to log in to this registry. This is the Local Repository which can only be used by
     * the system.
     * <p/>
     * This registry instance belongs to the super tenant of the system.
     *
     * @return Local Repository for system user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getLocalRepository() throws RepositoryException;

    /**
     * Returns a registry to be used for node-specific system operations. Human users should not be
     * allowed to log in to this registry. This is the Local Repository which can only be used by
     * the system.
     * <p/>
     * This registry instance belongs to a valid tenant of the system.
     *
     * @param tenantId tenant id of the user tenant. The tenant id '0', corresponds to the super
     *                 tenant of the system, whereas identifiers greater than '0' correspond to
     *                 valid tenants.
     *
     * @return Local Repository for system user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getLocalRepository(int tenantId) throws RepositoryException;

    /**
     * Returns a registry to be used for system operations. Human users should not be allowed log in
     * using this registry. This is the Configuration registry space which is used by the system.
     * <p/>
     * This registry instance belongs to the super tenant of the system.
     *
     *
     * @return Config Registry for system user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getConfigSystemRegistry() throws RepositoryException;

    /**
     * Returns a registry to be used for system operations. Human users should not be allowed log in
     * using this registry. This is the Configuration registry space which is used by the system.
     *
     * @param tenantId the tenant id of the system. The tenant id '0', corresponds to the super
     *                 tenant of the system, whereas identifiers greater than '0' correspond to
     *                 valid tenants.
     *
     * @return User registry for system user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getConfigSystemRegistry(int tenantId) throws RepositoryException;

    /**
     * Creates a Registry instance for anonymous user from the configuration registry space.
     * Permissions set for anonymous user will be applied for all operations performed using this
     * instance.
     * <p/>
     * This registry instance belongs to the super tenant of the system.
     *
     *
     * @return Config Registry for the anonymous user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getConfigUserRegistry() throws RepositoryException;

    /**
     * Creates Registry instances for normal users from the configuration registry space.
     * Applications should use this method to create Registry instances, unless there is a specific
     * need documented in other methods. User name and the password will be authenticated by the
     * EmbeddedRegistry before creating the requested Registry instance.
     * <p/>
     * This registry instance belongs to the super tenant of the system.
     *
     *
     * @param userName User name of the user.
     * @param password Password of the user.
     *
     * @return Config Registry instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getConfigUserRegistry(String userName, String password) throws RepositoryException;

    /**
     * Creates a Registry instance for the given user from the configuration registry space. This
     * method will NOT authenticate the user before creating the Registry instance. It assumes that
     * the user is authenticated outside the EmbeddedRegistry.
     * <p/>
     * This registry instance belongs to the super tenant of the system.
     *
     *
     * @param userName User name of the user.
     *
     * @return Config Registry instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getConfigUserRegistry(String userName) throws RepositoryException;

    /**
     * Creates a Registry instance for the given user from the configuration registry space with the
     * tenant id. This method will NOT authenticate the user before creating the Registry instance.
     * It assumes that the user is authenticated outside the registry service.
     *
     * @param userName User name of the user.
     * @param tenantId tenant id of the user tenant. The tenant id '0', corresponds to the super
     *                 tenant of the system, whereas identifiers greater than '0' correspond to
     *                 valid tenants.
     *
     * @return Config Registry instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getConfigUserRegistry(String userName, int tenantId) throws RepositoryException;

    /**
     * Creates Registry instances for normal users from the configuration registry space.
     * Applications should use this method to create Registry instances, unless there is a specific
     * need documented in other methods. User name and the password will be authenticated by the
     * EmbeddedRegistry before creating the requested Registry instance.
     *
     * @param userName User name of the user.
     * @param tenantId tenant id of the user tenant. The tenant id '0', corresponds to the super
     *                 tenant of the system, whereas identifiers greater than '0' correspond to
     *                 valid tenants.
     * @param password Password of the user.
     *
     * @return Config Registry instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getConfigUserRegistry(String userName, String password, int tenantId)
            throws RepositoryException;

    /**
     * Creates a Registry instance for the Governance space. This is the Governance registry space
     * which is used by the system.
     * <p/>
     * This registry instance belongs to the super tenant of the system.
     *
     *
     * @return Governance Registry for system user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getGovernanceSystemRegistry() throws RepositoryException;

    /**
     * Creates a Registry instance for the Governance space. This is the Governance registry space
     * which is used by the system.
     *
     * @param tenantId the tenant id of the system. The tenant id '0', corresponds to the super
     *                 tenant of the system, whereas identifiers greater than '0' correspond to
     *                 valid tenants.
     *
     * @return Governance registry for system user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getGovernanceSystemRegistry(int tenantId) throws RepositoryException;

    /**
     * Creates a Registry instance for anonymous user from the Governance space. Permissions set for
     * anonymous user will be applied for all operations performed using this instance.
     * <p/>
     * This registry instance belongs to the super tenant of the system.
     *
     *
     * @return Governance Registry for the anonymous user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getGovernanceUserRegistry() throws RepositoryException;

    /**
     * Creates a Registry instance for anonymous user from the Governance space. User name and the
     * password will be authenticated by the EmbeddedRegistry before creating the requested Registry
     * instance.
     * <p/>
     * This registry instance belongs to the super tenant of the system.
     *
     *
     * @param userName User name of the user.
     * @param password Password of the user.
     *
     * @return Governance Registry instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getGovernanceUserRegistry(String userName, String password)
            throws RepositoryException;

    /**
     * Creates a Registry instance for anonymous user from the Governance space. This method will
     * NOT authenticate the user before creating the Registry instance. It assumes that the user is
     * authenticated outside the EmbeddedRegistry.
     * <p/>
     * This registry instance belongs to the super tenant of the system.
     *
     *
     * @param userName User name of the user.
     *
     * @return Governance Registry instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getGovernanceUserRegistry(String userName) throws RepositoryException;

    /**
     * Creates a Registry instance for anonymous user from the Governance space. User name and the
     * password will be authenticated by the EmbeddedRegistry before creating the requested Registry
     * instance. This method can be used to obtain instances of Registry belonging to users of
     * multiple tenants.
     *
     * @param userName User name of the user.
     * @param password Password of the user.
     * @param tenantId tenant id of the user tenant. The tenant id '0', corresponds to the super
     *                 tenant of the system, whereas identifiers greater than '0' correspond to
     *                 valid tenants.
     *
     * @return Governance Registry instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getGovernanceUserRegistry(String userName, String password, int tenantId)
            throws RepositoryException;

    /**
     * Creates a Registry instance for anonymous user from the Governance space. This method will
     * NOT authenticate the user before creating the Registry instance. It assumes that the user is
     * authenticated outside the EmbeddedRegistry. This method can be used to obtain instances of
     * Registry belonging to users of multiple tenants.
     *
     * @param userName User name of the user.
     * @param tenantId tenant id of the user tenant. The tenant id '0', corresponds to the super
     *                 tenant of the system, whereas identifiers greater than '0' correspond to
     *                 valid tenants.
     *
     * @return Governance Registry instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    UserRegistry getGovernanceUserRegistry(String userName, int tenantId) throws RepositoryException;
    
    void setRegistryRoot(String registryRoot);
    
    String getRegistryRoot();
    
    void setServicePath(String storagePath);
    
    String getServicePath();
    
    HandlerManager getHandlerManager();
    
    boolean isNoCachePath(String path);

    /**
     * Method to register a no-cache path. If caching is disabled for a collection, all downstream
     * resources and collections won't be cached.
     *
     * @param path the path of a resource (or collection) for which caching is disabled.
     */
    void registerNoCachePath(String path) ;
    
    Collection newCollection(String[] paths);
    
    Resource newResource();
    
    Comment newComment(String comment);
    
    void setReadOnly(boolean readOnly);
    
    boolean isReadOnly();
    
    List<RemoteConfiguration> getRemoteInstances();
    
    boolean isCacheEnabled() ;

    /**
     * Set whether the registry caching is enabled or not.
     *
     * @param enableCache the enable-cache flag
     */
    void setCacheEnabled(boolean enableCache) ;
    
    boolean isClone();
    
    void setIsClone(boolean isClone);
    
    RealmService getRealmService();
    
    boolean updateHandler(OMElement configElement, Registry registryContext, String lifecyclePhase) throws RepositoryException ;
    
    /**
     * Method to determine whether a system resource (or collection) path has been registered.
     *
     * @param absolutePath the absolute path of the system resource (or collection)
     *
     * @return true if the system resource (or collection) path is registered or false if not.
     */
    boolean isSystemResourcePathRegistered(String absolutePath) ;

    /**
     * Method to register a system resource (or collection) path.
     *
     * @param absolutePath the absolute path of the system resource (or collection)
     */
    void registerSystemResourcePath(String absolutePath) ;
}