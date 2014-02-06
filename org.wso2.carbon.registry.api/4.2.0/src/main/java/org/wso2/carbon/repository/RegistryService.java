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

package org.wso2.carbon.repository;

import java.util.List;

import org.w3c.dom.Element;
import org.wso2.carbon.repository.exceptions.RepositoryException;
import org.wso2.carbon.repository.handlers.Handler;
import org.wso2.carbon.repository.handlers.filters.Filter;

/**
 * This interface can be used to implement an OSGi Service of the Repository. By doing so, the
 * Repository would become accessible beyond the Repository Kernel. The OSGi service also can be used as
 * means to make sure that no repository related logic executes before the repository has been fully
 * initialized, and made operational. Also, when the registry is no longer in service, all external
 * entities that use the registry would also become automatically suspended.
 */
public interface RegistryService {
	
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
    Registry getRegistry() throws RepositoryException;

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
    Registry getRegistry(String userName, String password) throws RepositoryException;

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
    Registry getRegistry(String userName) throws RepositoryException;

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
    Registry getRegistry(String userName, String password, int tenantId) throws RepositoryException;

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
    Registry getRegistry(String userName, String password, int tenantId, String chroot) throws RepositoryException;


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
    Registry getRegistry(String userName, int tenantId) throws RepositoryException;

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
    Registry getRegistry(String userName, int tenantId, String chroot) throws RepositoryException;

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
    Registry getLocalRepository() throws RepositoryException;

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
    Registry getLocalRepository(int tenantId) throws RepositoryException;

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
    Registry getConfigSystemRegistry() throws RepositoryException;

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
    Registry getConfigSystemRegistry(int tenantId) throws RepositoryException;

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
    Registry getConfigUserRegistry() throws RepositoryException;

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
    Registry getConfigUserRegistry(String userName, String password) throws RepositoryException;

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
    Registry getConfigUserRegistry(String userName) throws RepositoryException;

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
    Registry getConfigUserRegistry(String userName, int tenantId) throws RepositoryException;

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
    Registry getConfigUserRegistry(String userName, String password, int tenantId) throws RepositoryException;

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
    Registry getGovernanceSystemRegistry() throws RepositoryException;

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
    Registry getGovernanceSystemRegistry(int tenantId) throws RepositoryException;

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
    Registry getGovernanceUserRegistry() throws RepositoryException;

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
    Registry getGovernanceUserRegistry(String userName, String password) throws RepositoryException;

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
    Registry getGovernanceUserRegistry(String userName) throws RepositoryException;

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
    Registry getGovernanceUserRegistry(String userName, String password, int tenantId) throws RepositoryException;

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
    Registry getGovernanceUserRegistry(String userName, int tenantId) throws RepositoryException;
    
    /**
     * Returns a registry to be used for system operations.
     * 
     * @return returns the system registry 
     * @throws RepositoryException if an error occurs 
     */
    Registry getSystemRegistry() throws RepositoryException;
    
    /**
     * This method is not needed
     * @param registryRoot
     */
    void setRegistryRoot(String registryRoot);
    
    /**
     * Return the registry root. (configured in registry.xml)
     *
     * @return the registry root
     */
    String getRegistryRoot();
    
    /**
     * Get the service storage path.
     *
     * @return the service path.
     */
    void setServicePath(String storagePath);
    
    /**
     * Set the service storage path
     *
     * @param servicePath service path to be set.
     */
    String getServicePath();
    
    /**
     * Return the handler manager.
     *
     * @return handler manager
     */
//    HandlerManager getHandlerManager();
    
    /**
     * Method to determine whether caching is disabled for the given path.
     *
     * @param path the path to tes/t
     *
     * @return true if caching is disabled or false if not.
     */
    boolean isNoCachePath(String path);

    /**
     * Method to register a no-cache path. If caching is disabled for a collection, all downstream
     * resources and collections won't be cached.
     *
     * @param path the path of a resource (or collection) for which caching is disabled.
     */
    void registerNoCachePath(String path) ;
    
    /**
     * Creates a new Collection instance with the Resources with given paths  
     * 
     * @param paths paths to be be included in the Collection 
     * @return returns the Collection instance 
     */
    Collection newCollection(String[] paths);
    
    /**
     * Creates and returns a Resource instance
     * 
     * @return returns the Resource instance created
     */
    Resource newResource();
    
    /**
     * Creates a raw comment instance and returns
     * 
     * @param comment String value for the comment
     * @return the Comment instance created
     */
    Comment newComment(String comment);
    
    /**
     * sets by the caller if the RegistryService services need to be read only
     * 
     * @param readOnly set to true if the services are read-only
     */
    void setReadOnly(boolean readOnly);
    
    /**
     * Returns whether the services provided by the RegistryService is read only
     * 
     * @return returns true when operations are read-only 
     */
    boolean isReadOnly();
    
    /**
     * This method will return all the configured remote configurations
     * 
     * @return returns the list of all the RemoteConfiguration objects
     */
//    List<RemoteConfiguration> getRemoteInstances();
    List<String> getRemoteInstanceIds();

    /**
     * Returns true if cache is enabled 
     * 
     * @return
     */
	boolean isCacheEnabled();
	
    /**
     * Set whether the registry caching is enabled or not.
     *
     * @param enableCache the enable-cache flag
     */
    void setCacheEnabled(boolean enableCache) ;
    
    /**
     * Returns true if the RegistryService is a cloned instance
     * 
     * @return returns true if the RegistryService is a cloned instance
     */
    boolean isClone();
    
    /**
     * Setter method to set this instance to be a clone
     * 
     * @param isClone set to true if the instance is to be a clone
     */
    void setIsClone(boolean isClone);

//    RealmService getRealmService();
    
    /**
     * Registers handlers with the handler manager of the RegistryService. Each handler should be registered with a Filter.
     * If a handler should be engaged only to a subset of allowed methods, those methods can be
     * specified as a string array.
     *
     * @param methods Methods for which the registered handler should be engaged. Allowed values in
     *                the string array are "GET", "PUT", "IMPORT", "DELETE", "PUT_CHILD",
     *                "IMPORT_CHILD", "MOVE", "COPY", "RENAME", "CREATE_LINK", "REMOVE_LINK",
     *                "ADD_ASSOCIATION", "RESOURCE_EXISTS", "REMOVE_ASSOCIATION",
     *                "GET_ASSOCIATIONS", "GET_ALL_ASSOCIATIONS", "APPLY_TAG",
     *                "GET_RESOURCE_PATHS_WITH_TAG", "GET_TAGS", "REMOVE_TAG", "ADD_COMMENT",
     *                "EDIT_COMMENT", "GET_COMMENTS", "RATE_RESOURCE", "GET_AVERAGE_RATING",
     *                "GET_RATING", "CREATE_VERSION", "GET_VERSIONS", "RESTORE_VERSION",
     *                "EXECUTE_QUERY", "SEARCH_CONTENT", and "INVOKE_ASPECT". If null is given,
     *                handler will be engaged to all methods.
     * @param filter  Filter instance associated with the handler.
     * @param handler Handler instance to be registered.
     */
    void addHandler(String[] methods, Filter filter, Handler handler);
    
    /**
     * Registers handlers belonging to the given lifecycle phase with the handler manager. Each
     * handler should be registered with a Filter. If a handler should be engaged only to a subset
     * of allowed methods, those methods can be specified as a string array.
     *
     * @param methods        Methods for which the registered handler should be engaged. Allowed
     *                       values in the string array are "GET", "PUT", "IMPORT", "DELETE",
     *                       "PUT_CHILD", "IMPORT_CHILD", "MOVE", "COPY", "RENAME", "CREATE_LINK",
     *                       "REMOVE_LINK", "ADD_ASSOCIATION", "RESOURCE_EXISTS",
     *                       "REMOVE_ASSOCIATION", "GET_ASSOCIATIONS", "GET_ALL_ASSOCIATIONS",
     *                       "APPLY_TAG", "GET_RESOURCE_PATHS_WITH_TAG", "GET_TAGS", "REMOVE_TAG",
     *                       "ADD_COMMENT", "EDIT_COMMENT", "GET_COMMENTS", "RATE_RESOURCE",
     *                       "GET_AVERAGE_RATING", "GET_RATING", "CREATE_VERSION", "GET_VERSIONS",
     *                       "RESTORE_VERSION", "EXECUTE_QUERY", "SEARCH_CONTENT", and
     *                       "INVOKE_ASPECT". If null is given, handler will be engaged to all
     *                       methods.
     * @param filter         Filter instance associated with the handler.
     * @param lifecyclePhase The name of the lifecycle phase.
     * @param handler        Handler instance to be registered.
     */
    void addHandler(String[] methods, Filter filter, Handler handler, String lifecyclePhase);
    
    /**
     * Removes handlers belonging to the given lifecycle phase with the handler manager. Each
     * handler should be registered with a Filter. If a handler should be disengaged only for a
     * subset of allowed methods, those methods can be specified as a string array.
     *
     * @param methods        Methods for which the registered handler should be disengaged. Allowed
     *                       values in the string array are "GET", "PUT", "IMPORT", "DELETE",
     *                       "PUT_CHILD", "IMPORT_CHILD", "MOVE", "COPY", "RENAME", "CREATE_LINK",
     *                       "REMOVE_LINK", "ADD_ASSOCIATION", "RESOURCE_EXISTS",
     *                       "REMOVE_ASSOCIATION", "GET_ASSOCIATIONS", "GET_ALL_ASSOCIATIONS",
     *                       "APPLY_TAG", "GET_RESOURCE_PATHS_WITH_TAG", "GET_TAGS", "REMOVE_TAG",
     *                       "ADD_COMMENT", "EDIT_COMMENT", "GET_COMMENTS", "RATE_RESOURCE",
     *                       "GET_AVERAGE_RATING", "GET_RATING", "CREATE_VERSION", "GET_VERSIONS",
     *                       "RESTORE_VERSION", "EXECUTE_QUERY", "SEARCH_CONTENT", and
     *                       "INVOKE_ASPECT". If null is given, handler will be disengaged to all
     *                       methods.
     * @param filter         Filter instance associated with the handler. Each filter that you pass
     *                       in must have the associated handler set to it.
     * @param handler        Handler instance to be unregistered.
     * @param lifecyclePhase The name of the lifecycle phase.
     */
    void removeHandler(String[] methods, Filter filter, Handler handler, String lifecyclePhase);
    
    /**
     * remove a handler belonging to the given lifecycle phase from all the filters, all the
     * methods
     *
     * @param handler        the handler to remove.
     * @param lifecyclePhase The name of the lifecycle phase.
     */
    public void removeHandler(Handler handler, String lifecyclePhase);
    
    /**
     * Updates a handler based on given configuration.
     *
     * @param configElement   the handler configuration element.
     * @param lifecyclePhase  the lifecycle phase to which this handler belongs. The possible values
     *                        are "default", "reporting" and "user".
     * @param registryContext the Registry Context used by this registry instance.
     *
     * @return Created handler
     * @throws RepositoryException if anything goes wrong.
     */
    boolean updateHandler(Element configElement, Registry registry, String lifecyclePhase) throws RepositoryException ;
    
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