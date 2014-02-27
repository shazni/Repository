/*
 * Copyright (c) 2007, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.wso2.carbon.repository.api.exceptions.RepositoryUserException;

/**
 * This is thrown when a requested resource cannot be located in the Registry.
 */
@SuppressWarnings("serial")
public class RepositoryResourceNotFoundException extends RepositoryUserException {

	/**
     * Constructs a new exception for a resource not found in the given path.
     *
     * @param path the give path at which the resource was not found.
     */
    public RepositoryResourceNotFoundException(String path) {
        super("Resource does not exist at path " + path);
    }
    
    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message 		the detail message.
     * @param errorCode     the error code of the error
     */
    public RepositoryResourceNotFoundException(String message, int errorCode) {
		super(message, errorCode);
	}

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param path  the give path at which the resource was not found.
     * @param cause the cause of this exception.
     */
    public RepositoryResourceNotFoundException(String path, Throwable cause) {
        super("Resource does not exist at path " + path, cause);
    }
    
    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause of this exception.
     * @param errorCode error code of the error
     */
	public RepositoryResourceNotFoundException(String message, Throwable cause, int errorCode) {
		super(message, cause, errorCode);
	}
}
