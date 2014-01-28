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

package org.wso2.carbon.repository;

/**
 * Constants used in the registry which are exposed to be used in APIs + used
 * across packages.
 */
public final class RepositoryConstants {

	private RepositoryConstants() {
	}

	/**
	 * The session resource path
	 */
	public static final String SESSION_RESOURCE_PATH = "session.resource.path";

	/**
	 * System property to indicate to run create table scripts
	 */
	public static final String SETUP_PROPERTY = "setup";

	/**
	 * The configuration path for the registry is used to specify the core
	 * registry to use in the registry server. This was specified as a init
	 * parameter of the Registry servlet.
	 */
	public static final String REGISTRY_CONFIG_PATH = "registry.config.path";

	/**
	 * Known path parameter names
	 */
	public static final String VERSION_PARAMETER_NAME = "version";

	/**
	 * System property to clean the registry
	 */
	public static final String CARBON_REGISTRY_CLEAN = "carbon.registry.clean";

	/**
	 * The name of the session attribute that keeps the root registry instance.
	 */
	public static final String ROOT_REGISTRY_INSTANCE = "WSO2RegistryRoot";

	/**
	 * Default size of byte buffers used inside the registry kernel.
	 */
	public static final int DEFAULT_BUFFER_SIZE = 1024;

	/**
	 * Default identifier for utf-8.
	 */
	public static final String DEFAULT_CHARSET_ENCODING = "utf-8";

	// //////////////////////////////////////////////////////
	// Base Collection paths
	// //////////////////////////////////////////////////////

	/**
	 * THe root path of the registry resource tree.
	 */
	public static final String ROOT_PATH = "/";

	/**
	 * The base path for the system collection , where all the local, config,
	 * governance registries kept.
	 */
	public static final String SYSTEM_COLLECTION_BASE_PATH = "/_system";

	/**
	 * The base path for the local registry.
	 */
	public static final String LOCAL_REPOSITORY_BASE_PATH = SYSTEM_COLLECTION_BASE_PATH
			+ "/local";

	/**
	 * The base path for the config registry.
	 */
	public static final String CONFIG_REGISTRY_BASE_PATH = SYSTEM_COLLECTION_BASE_PATH
			+ "/config";

	/**
	 * The base path for the governance registry.
	 */
	public static final String GOVERNANCE_REGISTRY_BASE_PATH = SYSTEM_COLLECTION_BASE_PATH
			+ "/governance";

	// //////////////////////////////////////////////////////
	// Configuration resource paths
	// //////////////////////////////////////////////////////

	/**
	 * The path to store registry configurations.
	 */
	public static final String REGISTRY_COMPONENT_PATH = "/repository/components/org.wso2.carbon.registry";

	/**
	 * The path to store the governance configurations.
	 */
	public static final String GOVERNANCE_COMPONENT_PATH = "/repository/components/org.wso2.carbon.governance";

	/**
	 * The path to store the mount meta data
	 */
	public static final String SYSTEM_MOUNT_PATH = REGISTRY_COMPONENT_PATH + "/mount";

	/**
	 * The media type of a mount entry
	 */
	public static final String MOUNT_MEDIA_TYPE = "application/vnd.wso2.mount";

	/**
	 * The path to store the users profiles.
	 */
	public static final String PROFILES_PATH = "/users/";

	/**
	 * The path to store the lifecycle configurations.
	 */
	public static final String LIFECYCLE_CONFIGURATION_PATH = GOVERNANCE_COMPONENT_PATH
			+ "/lifecycles/";

	/**
	 * The path to store the handler configurations.
	 */
	public static final String HANDLER_CONFIGURATION_PATH = GOVERNANCE_COMPONENT_PATH
			+ "/handlers/";

	/**
	 * The path to store the queries in custom queries.
	 */
	public static final String QUERIES_COLLECTION_PATH = REGISTRY_COMPONENT_PATH
			+ "/queries";

	/**
	 * The path to store the governance service ui configurations.
	 */
	public static final String GOVERNANCE_SERVICES_CONFIG_PATH = GOVERNANCE_COMPONENT_PATH
			+ "/configuration/services/";

	/**
	 * The path to store the services
	 */
	public static final String GOVERNANCE_SERVICE_PATH = "/trunk/services";

	/**
	 * The path to store People artifacts
	 */
	public static final String GOVERNANCE_PEOPLE_PATH = "/people";

	/**
	 * The resource name for the profilesS
	 */
	public static final String PROFILE_RESOURCE_NAME = "/profiles";

	/**
	 * The separator of the paths of the registry resources.
	 */
	public static final String PATH_SEPARATOR = "/";

	/**
	 * The name of the session attribute for the user registry.
	 */
	public static final String USER_REGISTRY = "user_registry";

	/**
	 * The flag to indicate the deleted state in atom feeds.
	 */
	public static final int DELETED_STATE = 101;

	/**
	 * Number of items per pages entry.
	 */

	/**
	 * Items per page for pagination
	 */
	public static final int ITEMS_PER_PAGE = 10;

	// //////////////////////////////////////////////////////
	// Internal media types
	// //////////////////////////////////////////////////////

	/**
	 * name of the default media type
	 */
	public static final String DEFAULT_MEDIA_TYPE = "default";

	/**
	 * SQL Query media type.
	 */
	public static final String SQL_QUERY_MEDIA_TYPE = "application/vnd.sql.query";

	/**
	 * Media type: rating
	 */
	public static final String RATING_MEDIA_TYPE = "rating";

	/**
	 * Media type: tag
	 */
	public static final String TAG_MEDIA_TYPE = "tag";

	// //////////////////////////////////////////////////////
	// Built-in media types
	// //////////////////////////////////////////////////////

	/**
	 * The media type for WSDLs
	 */
	public static final String WSDL_MEDIA_TYPE = "application/wsdl+xml";

	/**
	 * The media type for XSDs
	 */
	public static final String XSD_MEDIA_TYPE = "application/x-xsd+xml";

	/**
	 * The media type for Policies
	 */
	public static final String POLICY_MEDIA_TYPE = "application/policy+xml";

	/**
	 * Synapse configuration collection media type
	 */
	public static final String SYNAPSE_CONF_COLLECTION_MEDIA_TYPE = "synapse-conf";

	/**
	 * Synapse sequence collection media type
	 */
	public static final String SYNAPSE_SEQUENCE_COLLECTION_MEDIA_TYPE = "synapse-sequences";

	/**
	 * Synapse endpoint collection media type
	 */
	public static final String SYNAPSE_ENDPOINT_COLLECTION_MEDIA_TYPE = "synapse-endpoints";

	/**
	 * Synapse prxy services collection media type
	 */
	public static final String SYNAPSE_PROXY_SERVICES_COLLECTION_MEDIA_TYPE = "synapse-proxy-services";

	/**
	 * Synapse task collection media type
	 */
	public static final String SYNAPSE_TASKS_COLLECTION_MEDIA_TYPE = "synapse-tasks";

	/**
	 * Synapse entries collection media type
	 */
	public static final String SYNAPSE_ENTRIES_COLLECTION_MEDIA_TYPE = "synapse-entries";

	/**
	 * Synapse configuration collection name
	 */
	public static final String SYNAPSE_CONF_COLLECTION_NAME = "conf";

	/**
	 * Synapse sequence collection name
	 */
	public static final String SYNAPSE_SEQUENCES_COLLECTION_NAME = "sequences";

	/**
	 * Synapse endpoint collection name
	 */
	public static final String SYNAPSE_ENDPOINT_COLLECTION_NAME = "endpoints";

	/**
	 * Synapse proxy services collection name
	 */
	public static final String SYNAPSE_PROXY_SERVICES_COLLECTION_NAME = "proxy-services";

	/**
	 * Synapse task collection name
	 */
	public static final String SYNAPSE_TASKS_COLLECTION_NAME = "tasks";

	/**
	 * Synapse entries collection name
	 */
	public static final String SYNAPSE_ENTRIES_COLLECTION_NAME = "entries";

	/**
	 * Axis2 configuration collection media type
	 */
	public static final String AXIS2_CONF_COLLECTION_MEDIA_TYPE = "axis2-conf";

	/**
	 * Axis2 services configuration media type
	 */
	public static final String AXIS2_SERVICES_COLLECTION_MEDIA_TYPE = "axis2-services";

	/**
	 * Axis2 modules collection media type
	 */
	public static final String AXIS2_MODULES_COLLECTION_MEDIA_TYPE = "axis2-modules";

	/**
	 * Axis2 configuration collection name
	 */
	public static final String AXIS2_CONF_COLLECTION_NAME = "conf";

	/**
	 * Axis2 services collection name
	 */
	public static final String AXIS2_SERVICES_COLLECTION_NAME = "services";

	/**
	 * Axis2 modules collection name
	 */
	public static final String AXIS2_MODULES_COLLECTION_NAME = "modules";

	/**
	 * Media type - endpoints.
	 */
	public static final String MEX_ENDPOINTS_COLLECTION_MEDIA_TYPE = "application/vnd.wso2-endpoints";

	/**
	 * The media type for Profiles
	 */
	public static final String PROFILES_MEDIA_TYPE = "application/vnd.wso2-profiles";

	/**
	 * The media type for Services
	 */
	public static final String SERVICE_MEDIA_TYPE = "application/vnd.wso2-service+xml";

	/**
	 * Result types of dynamic queries
	 */
	public static final String RESULT_TYPE_PROPERTY_NAME = "resultType";

	/**
	 * Result type: resource
	 */
	public static final String RESOURCES_RESULT_TYPE = "Resource";

	/**
	 * Result type: resource UUID
	 */
	public static final String RESOURCE_UUID_RESULT_TYPE = "ResourceUUID";

	/**
	 * Result type: comments
	 */
	public static final String COMMENTS_RESULT_TYPE = "Comments";

	/**
	 * Result type: ratings
	 */
	public static final String RATINGS_RESULT_TYPE = "Ratings";

	/**
	 * Result type: tags
	 */
	public static final String TAGS_RESULT_TYPE = "Tags";

	/**
	 * Result type : summary count of all tags
	 */
	public static final String TAG_SUMMARY_RESULT_TYPE = "TagSummary";

	/**
	 * Name of the symbolic link property.
	 */
	public static final String SYMLINK_PROPERTY_NAME = "SymlinkPropertyName";

	/**
	 * Used to pass handler throughout request context
	 */
	public static final String SYMLINK_TO_REMOVE_PROPERTY_NAME = "SymlinkToRemovePropertyName";

	/**
	 * Separator used to access Registry meta data - i.e. "/resource$tags"
	 */
	public static final String URL_SEPARATOR = ";";

	/**
	 * Parameter separator used to access Registry meta data - i.e.
	 * "/resource$tags"
	 */
	public static final String URL_PARAMETER_SEPARATOR = ":";

	/**
	 * Text Editor: Text input name
	 */
	public static final String TEXT_INPUT_NAME = "generic-text-input";

	/**
	 * Text Editor: processor key
	 */
	public static final String TEXT_EDIT_PROCESSOR_KEY = "system.text.edit.processor";

	/**
	 * Custom Editor: processor key
	 */
	public static final String CUSTOM_EDIT_PROCESSOR_KEY = "edit-processor";

	/**
	 * Servlet parameter for view key.
	 */
	public static final String VIEW_KEY = "view-key";

	/**
	 * Servlet parameter for view type.
	 */
	public static final String VIEW_TYPE = "view-type";

	/**
	 * Servlet parameter for edit view type.
	 */
	public static final String EDIT_VIEW_TYPE = "edit";

	/**
	 * Servlet parameter for new view type.
	 */
	public static final String NEW_VIEW_TYPE = "new";

	/**
	 * Namespace of the custom registry specific feed fields/entries.
	 */
	public static final String REGISTRY_NAMESPACE = "http://wso2.org/registry";

	/**
	 * The error message for unauthorised registry access
	 */
	public static final String REGISTRY_UNAUTHORIZED_ERROR = "You are not Authorized to perform this operation !";

	/**
	 * The error message the result set close fail.
	 */
	public static final String RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR = "A SQLException error has occurred "
			+ "when trying to close result set or prepared statement";

	/**
	 * When caching atom feeds, the maximum size of the cache.
	 */
	public static final long MAX_REG_CLIENT_CACHE_SIZE = 50000;
	
	/**
	 * The name of the registry cache manager
	 */
	public static final String REGISTRY_CACHE_MANAGER = "registryCacheManager";
	
	/**
	 * The id of the path cache for registry resources.
	 */
	public static final String PATH_CACHE_ID = "REG_PATH_CACHE";

	/**
	 * The id of the registry cache
	 */
	public static final String REGISTRY_CACHE_BACKED_ID = "REG_CACHE_BACKED_ID";

	/**
	 * Symbolic link property: actual path
	 */
	public static final String REGISTRY_ACTUAL_PATH = "registry.actualpath";

	/**
	 * Symbolic link property: mount point
	 */
	public static final String REGISTRY_MOUNT_POINT = "registry.mountpoint";

	/**
	 * Symbolic link property: target point
	 */
	public static final String REGISTRY_TARGET_POINT = "registry.targetpoint";

	/**
	 * Symbolic link property: registry link
	 */
	public static final String REGISTRY_LINK = "registry.link";

	/**
	 * Symbolic link property: registry link restoration details
	 */
	public static final String REGISTRY_LINK_RESTORATION = "registry.linkrestoration";

	/**
	 * Symbolic link property: registry user
	 */
	public static final String REGISTRY_USER = "registry.user";

	/**
	 * Symbolic link property: registry author
	 */
	public static final String REGISTRY_AUTHOR = "registry.author";

	/**
	 * Symbolic link property: registry mount
	 */
	public static final String REGISTRY_MOUNT = "registry.mount";

	/**
	 * Symbolic link property: registry fixed mount.
	 */
	public static final String REGISTRY_FIXED_MOUNT = "registry.fixedmount";

	/**
	 * Symbolic link property: real path
	 */
	public static final String REGISTRY_REAL_PATH = "registry.realpath";

	/**
	 * Symbolic link property: prevents recursive operations on the resource
	 */
	public static final String REGISTRY_NON_RECURSIVE = "registry.nonrecursive";

	/**
	 * Symbolic link property: existing resource.
	 */
	public static final String REGISTRY_EXISTING_RESOURCE = "registry.existingresource";

	/**
	 * The meta directory of a check-in client dump. The reserved name for
	 * resource/collection name
	 */
	public static final String CHECK_IN_META_DIR = ".meta";

	/**
	 * Identifier to distinguish operations performed by the mount handler at a
	 * remote mounting instance.
	 */
	public static final String REMOTE_MOUNT_OPERATION = "registry.remotemount.operation";

	/**
	 * Identifier to identify and separate "version" string
	 */
	public static final String VERSION_SEPARATOR = ";version:";

	/**
	 * Defines the media type used for symlink and remote link resources.
	 * */
	public static final String LINK_MEDIA_TYPE = "application/vnd.wso2-link";
	
	/**
	 * MEX media type
	 */
	public static final String MEX_MEDIA_TYPE = "application/vnd.wso2-mex+xml";
	
    /**
     * Built in user name: admin
     * Note that this is only used in tests and should not assume the
     * admin user name as a constant in any implementation.
     */
    public static final String ADMIN_USER = "admin";

    /**
     * Built in password: admin
     * Note that this is only used in tests and should not assume the
     * admin password as a constant in any implementation.
     */
    public static final String ADMIN_PASSWORD = "admin";
    
    /**
     * Anonymous user name for the repository
     */
    public static final String ANONYMOUS_USER = "wso2.anonymous.user";
}
