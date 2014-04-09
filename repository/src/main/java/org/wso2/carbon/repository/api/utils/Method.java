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

package org.wso2.carbon.repository.api.utils;

public enum Method {
    GET,
    PUT,
    DELETE,
    IMPORT,
    PUT_CHILD,
    IMPORT_CHILD,
    INVOKE_ASPECT,
    MOVE,
    COPY,
    RENAME,
    CREATE_LINK,
    REMOVE_LINK,
    ADD_ASSOCIATION,
    REMOVE_ASSOCIATION,
    GET_ASSOCIATION,
    GET_ASSOCIATIONS,
    GET_ALL_ASSOCIATIONS,
    APPLY_TAG,
    GET_RESOURCE_PATHS_WITH_TAG,
    GET_TAGS,
    REMOVE_TAG,
    REMOVE_TAGS,
    ADD_COMMENT,
    EDIT_COMMENT,
    REMOVE_COMMENT,
    GET_COMMENT,
    GET_COMMENTS,
    RATE_RESOURCE,
    GET_AVERAGE_RATING,
    GET_RATING,
    CREATE_VERSION,
    GET_VERSIONS,
    RESTORE_VERSION,
    EXECUTE_QUERY,
    SEARCH_CONTENT,
    RESOURCE_EXISTS,
    GET_REGISTRY_CONTEXT,
    DUMP,
    RESTORE;
};
