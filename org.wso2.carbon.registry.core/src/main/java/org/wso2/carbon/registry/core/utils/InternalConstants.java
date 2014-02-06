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

package org.wso2.carbon.registry.core.utils;

public class InternalConstants {
	
	/**
	 * System property to clean the registry
	 */
	public static final String CARBON_REGISTRY_CLEAN = "carbon.registry.clean";
	
	/**
	 * The media type of a mount entry
	 */
	public static final String MOUNT_MEDIA_TYPE = "application/vnd.wso2.mount";
	
	/**
	 * The path to store the services
	 */
	public static final String GOVERNANCE_SERVICE_PATH = "/trunk/services";
	
	/**
	 * The flag to indicate the deleted state in atom feeds.
	 */
	public static final int DELETED_STATE = 101;
	
	/**
	 * Media type: rating
	 */
	public static final String RATING_MEDIA_TYPE = "rating";
	
	/**
	 * Media type: tag
	 */
	public static final String TAG_MEDIA_TYPE = "tag";
	
	/**
	 * Result type: resource UUID
	 */
	public static final String RESOURCE_UUID_RESULT_TYPE = "ResourceUUID";
	
	/**
	 * Result type: comments
	 */
	public static final String COMMENTS_RESULT_TYPE = "Comments";
	
	/**
	 * Result type : summary count of all tags
	 */
	public static final String TAG_SUMMARY_RESULT_TYPE = "TagSummary";
	
	/**
	 * Used to pass handler throughout request context
	 */
	public static final String SYMLINK_TO_REMOVE_PROPERTY_NAME = "SymlinkToRemovePropertyName";
	
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
	 * The error message the result set close fail.
	 */
	public static final String RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR = "A SQLException error has occurred "
			+ "when trying to close result set or prepared statement";
	
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
	 * Symbolic link property: registry link restoration details
	 */
	public static final String REGISTRY_LINK_RESTORATION = "registry.linkrestoration";
	
	/**
	 * Symbolic link property: registry fixed mount.
	 */
	public static final String REGISTRY_FIXED_MOUNT = "registry.fixedmount";
	
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
	 * Identifier to identify and separate "version" string
	 */
	public static final String VERSION_SEPARATOR = ";version:";
	
	/**
	 * Defines the media type used for symlink and remote link resources.
	 * */
	public static final String LINK_MEDIA_TYPE = "application/vnd.wso2-link";
}
