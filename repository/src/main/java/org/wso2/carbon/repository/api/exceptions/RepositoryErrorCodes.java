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

package org.wso2.carbon.repository.api.exceptions;

public final class RepositoryErrorCodes {
	
	public static final int GENERAL_REGISTRY_ERROR = 9999;
	
	public static final int GENERAL_SERVER_ERROR = 9000;
	public static final int GENERAL_SERVER_CONTENT_ERROR = 9001;
	public static final int GENERAL_SESSION_ERROR = 9002;
	public static final int GENERAL_INIT_ERROR = 9003;
	public static final int GENERAL_CONFIGURATION_ERROR = 9004;
	public static final int GENERAL_DB_ERROR = 9005;
	public static final int GENERAL_CONCURRENCY_ERROR = 9006;
	
	/*
	 * Registry User Exceptions Error Codes
	 */
	
	// RegistryUserExcetion Error Codes (100 - 199)
	
	public static final int GENERAL_USER_ERROR = 100;
	
	// ------------------------------------------------------------------------
	
	// RegistryAuthException Error Codes (200 - 299)
	
	public static final int GENERAL_AUTH_ERROR = 200;
	public static final int USER_NOT_AUTHORISED = 201;
	public static final int AUTH_CHECK_ERROR = 202;
	public static final int COULD_NOT_CLEAR_AUTH_ERROR = 203;
	public static final int COULD_NOT_COPY_AUTH_ERROR = 204;
	public static final int COULD_NOT_PROVIDE_ROOT_AUTH_ERROR = 205;
	
	// ------------------------------------------------------------------------
	
	// RegistryUserContentException Error Codes (300 - 399)
	
	public static final int GENERAL_USER_CONTENT_ERROR = 300;
	public static final int INVALID_PATH_PROVIDED = 301;
	public static final int ILLEGAL_NAME_OF_RESOURCE = 302;
	public static final int INVALID_PROPERTY_WITH_NULL_KEY = 303;
	public static final int INVALID_OR_MALFORMED_URL = 304;
	public static final int HTTP_NOT_FOUND = 305;
	public static final int INVALID_CHARACTER_IN_NAME = 306;
	public static final int NON_EXISTENT_FILE_OR_DIRECTORY = 307;
	public static final int PATH_NOT_PROVIDED_AS_FILE_URL = 308;
	public static final int COULD_NOT_READ_FROM_FILE = 309;
	public static final int HTTP_REQUEST_ERROR = 310;
	public static final int VIEWKEY_OR_XSLT_PATH_MISSING = 311;
	public static final int VIEWKEY_OR_HTML_PATH_MISSING = 312;
	public static final int UNSUPPORTED_VIEW_TYPE_PROVIDED = 313;
	public static final int UNSUPPORTED_EDIT_VIEW_PROVIDED = 314;
	public static final int UNSUPPORTED_RESOURCE_CREATION_VIEW_PROVIDED = 315;
	public static final int REQUIRED_LIBRARY_MISSING_IN_CLASSPATH = 316;
	public static final int CARBON_HOME_SYSTEM_PROPERTY_NOT_SET = 317;
	public static final int UNAVAILABLE_FILE_REFERRED = 318;
	public static final int INVALID_SCHEMA_PROVIDED = 319;
	
	// ------------------------------------------------------------------------
	
	// RegistryResourceNotFoundException Error Codes (400 - 499)
	
	public static final int GENERAL_RESOURCE_NOT_FOUND_ERROR = 400;
	public static final int RESOURCE_VERSION_NOT_AVAILABLE = 401;
	public static final int RESOURCE_PATH_ERROR = 402;
	public static final int RESOURCE_UNAVAILABLE = 403;
	public static final int SNAPSHOT_NOT_AVAILABLE_FOR_ID = 404;
}
