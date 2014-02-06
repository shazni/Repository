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
	 * System property to indicate to run create table scripts
	 */
	public static final String SETUP_PROPERTY = "setup";

	/**
	 * Known path parameter names
	 */
	public static final String VERSION_PARAMETER_NAME = "version";

	/**
	 * The name of the session attribute that keeps the root registry instance.
	 */
	public static final String ROOT_REGISTRY_INSTANCE = "WSO2RegistryRoot";

	/**
	 * Default size of byte buffers used inside the registry kernel.
	 */
	public static final int DEFAULT_BUFFER_SIZE = 1024;

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
	public static final String LOCAL_REPOSITORY_BASE_PATH = SYSTEM_COLLECTION_BASE_PATH + "/local";

	/**
	 * The base path for the config registry.
	 */
	public static final String CONFIG_REGISTRY_BASE_PATH = SYSTEM_COLLECTION_BASE_PATH + "/config";

	/**
	 * The base path for the governance registry.
	 */
	public static final String GOVERNANCE_REGISTRY_BASE_PATH = SYSTEM_COLLECTION_BASE_PATH + "/governance";

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
	 * The path to store the users profiles.
	 */
	public static final String PROFILES_PATH = "/users/";

	/**
	 * The path to store the lifecycle configurations.
	 */
	public static final String LIFECYCLE_CONFIGURATION_PATH = GOVERNANCE_COMPONENT_PATH + "/lifecycles/";

	/**
	 * The path to store the handler configurations.
	 */
	public static final String HANDLER_CONFIGURATION_PATH = GOVERNANCE_COMPONENT_PATH + "/handlers/";

	/**
	 * The path to store the queries in custom queries.
	 */
	public static final String QUERIES_COLLECTION_PATH = REGISTRY_COMPONENT_PATH + "/queries";

	/**
	 * The separator of the paths of the registry resources.
	 */
	public static final String PATH_SEPARATOR = "/";

	/**
	 * The name of the session attribute for the user registry.
	 */
	public static final String USER_REGISTRY = "user_registry";

	/**
	 * SQL Query media type.
	 */
	public static final String SQL_QUERY_MEDIA_TYPE = "application/vnd.sql.query";

	/**
	 * The media type for WSDLs
	 */
	public static final String WSDL_MEDIA_TYPE = "application/wsdl+xml";

	/**
	 * The media type for XSDs
	 */
	public static final String XSD_MEDIA_TYPE = "application/x-xsd+xml";

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
	 * Result type: ratings
	 */
	public static final String RATINGS_RESULT_TYPE = "Ratings";

	/**
	 * Result type: tags
	 */
	public static final String TAGS_RESULT_TYPE = "Tags";

	/**
	 * Name of the symbolic link property.
	 */
	public static final String SYMLINK_PROPERTY_NAME = "SymlinkPropertyName";

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
	 * Symbolic link property: real path
	 */
	public static final String REGISTRY_REAL_PATH = "registry.realpath";

	/**
	 * Symbolic link property: prevents recursive operations on the resource
	 */
	public static final String REGISTRY_NON_RECURSIVE = "registry.nonrecursive";

	/**
	 * Identifier to distinguish operations performed by the mount handler at a
	 * remote mounting instance.
	 */
	public static final String REMOTE_MOUNT_OPERATION = "registry.remotemount.operation";
    
    /**
     * Anonymous user name for the repository
     */
    public static final String ANONYMOUS_USER = "wso2.anonymous.user";
}
