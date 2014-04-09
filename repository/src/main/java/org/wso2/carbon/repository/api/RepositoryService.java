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

package org.wso2.carbon.repository.api;

import java.util.List;

import org.w3c.dom.Element;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.handlers.Filter;
import org.wso2.carbon.repository.api.handlers.Handler;
import org.wso2.carbon.repository.api.utils.Method;

/**
 * This interface can be used to implement an OSGi Service of the Repository. By doing so, the
 * Repository would become accessible beyond the Repository Kernel. The OSGi service also can be used as
 * means to make sure that no repository related logic executes before the repository has been fully
 * initialized, and made operational. Also, when the repository is no longer in service, all external
 * entities that use the repository would also become automatically suspended.
 */
public interface RepositoryService {
	
    /**
     * Creates a Repository instance for anonymous user which contains the entire Repository tree
     * starting from '/'. Permissions set for anonymous user will be applied for all operations
     * performed using this instance.
     * <p/>
     * This Repository instance belongs to the super tenant of the system.
     *
     *
     * @return 		Complete Repository for the anonymous user.
     * @throws 		RepositoryException if an error occurs
     */
    Repository getRepository() throws RepositoryException;

    /**
     * Creates a Repository instance for anonymous user which contains the entire Repository tree
     * starting from '/'. This method will NOT authenticate the user before creating the Repository
     * instance. It assumes that the user is authenticated outside the EmbeddedRepository.
     * <p/>
     * This repository instance belongs to the super tenant of the system.
     *
     *
     * @param userName User name of the user.
     *
     * @return 		Complete Repository instance for the given user.
     * @throws 		RepositoryException if an error occurs
     */
    Repository getRepository(String userName) throws RepositoryException;

    /**
     * Creates a Repository instance for anonymous user which contains the entire Repository tree
     * starting from '/'. This method will NOT authenticate the user before creating the Repository
     * instance. It assumes that the user is authenticated outside the EmbeddedRepository. This method
     * can be used to obtain instances of Repository belonging to users of multiple tenants.
     *
     * @param userName 		User name of the user.
     * @param tenantId 		tenant id of the user tenant. The tenant id '0', corresponds to the super
     *                 		tenant of the system, whereas identifiers greater than '0' correspond to
     *                 		valid tenants.
     *
     * @return 				Complete Repository instance for the given user.
     * @throws 				RepositoryException if an error occurs
     */
    Repository getRepository(String userName, int tenantId) throws RepositoryException;

    /**
     * Creates a Repository instance for the given user which contains the entire Repository tree
     * starting from '/'. This method will NOT authenticate the user before creating the Repository
     * instance. It assumes that the user is authenticated outside the EmbeddedRepository. This method
     * can be used to obtain instances of Repository belonging to users of multiple tenants. The
     * returned Repository will be chrooted to the given path, making it possible to use relative
     * paths.
     *
     * @param userName 		User name of the user.
     * @param tenantId 		tenant id of the user tenant. The tenant id '0', corresponds to the super
     *                 		tenant of the system, whereas identifiers greater than '0' correspond to
     *                 		valid tenants.
     * @param chroot   		to return a chrooted repository. The whole Repository can be accessed by using
     *                 		the chroot, '/', and a subset of the Repository can be accessed by using a
     *                 		chroot, '/x/y/z'. For example, the repository of the configuration local
     *                 		Repository can be obtained from '/_system/config/repository'.
     *
     * @return 				Complete Repository instance for the given user.
     * @throws 				RepositoryException if an error occurs
     */
    Repository getRepository(String userName, int tenantId, String chroot) throws RepositoryException;

    /**
     * Returns a Repository to be used for node-specific system operations. Human users should not be
     * allowed to log in to this Repository. User's are not recommended to use this repository
     * This is the Local Repository which can only be used by the system.
     * <p/>
     * This Repository instance belongs to the super tenant of the system.
     *
     * @return Local Repository for system user.
     * @throws RepositoryException if an error occurs
     */
    Repository getLocalRepository() throws RepositoryException;

    /**
     * Returns a Repository to be used for node-specific system operations. Human users should not be
     * allowed to log in to this repository. User's are not recommended to use this repository
     * This is the Local Repository which can only be used by the system.
     * <p/>
     * This Repository instance belongs to a valid tenant of the system.
     *
     * @param tenantId 	tenant id of the user tenant. The tenant id '0', corresponds to the super
     *                 	tenant of the system, whereas identifiers greater than '0' correspond to
     *                 	valid tenants.
     *
     * @return 			Local Repository for system user.
     * @throws 			RepositoryException if an error occurs
     */
    Repository getLocalRepository(int tenantId) throws RepositoryException;

    /**
     * Returns a repository to be used for system operations. Human users should not be allowed log in
     * using this repository. This is the Configuration repository space which is used by the system.
     * <p/>
     * This repository instance belongs to the super tenant of the system.
     *
     *
     * @return 			Config Repository for system user.
     * @throws 			RepositoryException if an error occurs
     */
    Repository getConfigSystemRepository() throws RepositoryException;

    /**
     * Returns a repository to be used for system operations. Human users should not be allowed log in
     * using this repository. This is the Configuration repository space which is used by the system.
     *
     * @param tenantId 	the tenant id of the system. The tenant id '0', corresponds to the super
     *                 	tenant of the system, whereas identifiers greater than '0' correspond to
     *                 	valid tenants.
     *
     * @return 			Configuration repository for system user.
     * @throws 			RepositoryException if an error occurs
     */
    Repository getConfigSystemRepository(int tenantId) throws RepositoryException;

    /**
     * Creates a Repository instance for anonymous user from the configuration repository space.
     * Permissions set for anonymous user will be applied for all operations performed using this
     * instance.
     * <p/>
     * This repository instance belongs to the super tenant of the system.
     *
     * @return 			Configuration Repository for the anonymous user.
     * @throws 			RepositoryException if an error occurs
     */
    Repository getConfigUserRepository() throws RepositoryException;

    /**
     * Creates a Repository instance for the given user from the configuration Repository space. This
     * method will NOT authenticate the user before creating the Repository instance. It assumes that
     * the user is authenticated outside the EmbeddedRepository.
     * <p/>
     * This Repository instance belongs to the super tenant of the system.
     *
     * @param 			userName User name of the user.
     *
     * @return 			Config Repository instance for the given user.
     * @throws 			RepositoryException if an error occurs
     */
    Repository getConfigUserRepository(String userName) throws RepositoryException;

    /**
     * Creates a Repository instance for the given user from the configuration repository space with the
     * tenant id. This method will NOT authenticate the user before creating the Repository instance.
     * It assumes that the user is authenticated outside the repository service.
     *
     * @param userName User name of the user.
     * @param tenantId tenant id of the user tenant. The tenant id '0', corresponds to the super
     *                 tenant of the system, whereas identifiers greater than '0' correspond to
     *                 valid tenants.
     *
     * @return Config Repository instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    Repository getConfigUserRepository(String userName, int tenantId) throws RepositoryException;

    /**
     * Creates a Repository instance for the Governance space. This is the Governance repository space
     * which is used by the system.
     * <p/>
     * This repository instance belongs to the super tenant of the system.
     *
     *
     * @return Governance Repository for system user.
     * @throws RepositoryException if an error occurs
     */
    Repository getGovernanceSystemRepository() throws RepositoryException;

    /**
     * Creates a Repository instance for the Governance space. This is the Governance repository space
     * which is used by the system.
     *
     * @param tenantId the tenant id of the system. The tenant id '0', corresponds to the super
     *                 tenant of the system, whereas identifiers greater than '0' correspond to
     *                 valid tenants.
     *
     * @return Governance repository for system user.
     * @throws RepositoryException if an error occurs
     */
    Repository getGovernanceSystemRepository(int tenantId) throws RepositoryException;

    /**
     * Creates a Repository instance for anonymous user from the Governance space. Permissions set for
     * anonymous user will be applied for all operations performed using this instance.
     * <p/>
     * This repository instance belongs to the super tenant of the system.
     *
     *
     * @return Governance Repository for the anonymous user.
     * @throws RepositoryException if an error occurs
     */
    Repository getGovernanceUserRepository() throws RepositoryException;

    /**
     * Creates a Repository instance for anonymous user from the Governance space. This method will
     * NOT authenticate the user before creating the Repository instance. It assumes that the user is
     * authenticated outside the EmbeddedRegistry.
     * <p/>
     * This repository instance belongs to the super tenant of the system.
     *
     *
     * @param userName User name of the user.
     *
     * @return Governance Repository instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    Repository getGovernanceUserRepository(String userName) throws RepositoryException;

    /**
     * Creates a Repository instance for anonymous user from the Governance space. This method will
     * NOT authenticate the user before creating the Repository instance. It assumes that the user is
     * authenticated outside the EmbeddedRegistry. This method can be used to obtain instances of
     * Repository belonging to users of multiple tenants.
     *
     * @param userName User name of the user.
     * @param tenantId tenant id of the user tenant. The tenant id '0', corresponds to the super
     *                 tenant of the system, whereas identifiers greater than '0' correspond to
     *                 valid tenants.
     *
     * @return Governance Repository instance for the given user.
     * @throws RepositoryException if an error occurs
     */
    Repository getGovernanceUserRepository(String userName, int tenantId) throws RepositoryException;
    
    /**
     * Returns a repository to be used for system operations.
     * 
     * @return returns the system repository 
     * @throws RepositoryException if an error occurs 
     */
    Repository getSystemRepository() throws RepositoryException;
    
    /**
     * This method is not needed
     * @param repositoryRoot
     */
    void setRepositoryRoot(String repositoryRoot);
    
    /**
     * Return the repository root. (configured in repository.xml)
     *
     * @return the repository root
     */
    String getRepositoryRoot();
    
    /**
     * Get the service storage path.
	 *
     */
    void setServicePath(String storagePath);
    
    /**
     * Set the service storage path
     *
     * @return servicePath service path of the service.
     */
    String getServicePath();
        
    /**
     * Method to determine whether caching is disabled for the given path.
     *
     * @param path the path to test
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
    List<String> getRemoteInstanceIds();

    /**
     * Returns true if cache is enabled 
     * 
     * @return return true if caching is enabled.
     */
	boolean isCacheEnabled();
	
    /**
     * Set whether the repository caching is enabled or not.
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
    void addHandler(Method[] methods, Filter filter, Handler handler);
    
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
    void addHandler(Method[] methods, Filter filter, Handler handler, String lifecyclePhase);

    /**
     * remove a handler belonging to the given lifecycle phase from all the filters, all the
     * methods
     *
     * @param handler        the handler to remove.
     */
    public void removeHandler(Handler handler);

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
     * @param lifecyclePhase  the life-cycle phase to which this handler belongs. The possible values
     *                        are "default", "reporting" and "user".
     * @param repository        the repository instance.
     *
     * @return Created handler
     * @throws RepositoryException if anything goes wrong.
     */
    boolean updateHandler(Element configElement, Repository repository, String lifecyclePhase) throws RepositoryException ;
    
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