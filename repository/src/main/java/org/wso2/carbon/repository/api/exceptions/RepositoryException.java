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

import org.wso2.carbon.repository.api.exceptions.RepositoryErrorCodes;

/**
 * Base Class for capturing any type of exception that occurs when using the Registry APIs.
 */
@SuppressWarnings("serial")
public class RepositoryException extends Exception {
	
	private int errorCode = RepositoryErrorCodes.GENERAL_REGISTRY_ERROR ;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message.
     */
    public RepositoryException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message.
     * @param errorCode error code of the error
     */
	public RepositoryException(String message, int errorCode) {
		super(message) ;
		this.errorCode = errorCode ;
	}

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause of this exception.
     */
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause of this exception.
     * @param errorCode error code of the error
     */
	public RepositoryException(String message, Throwable cause, int errorCode) {
		super(message, cause);
		this.errorCode = errorCode ;
	}
	
	/**
	 * Returns the error code for the error
	 * @return error code
	 */
	public int getErrorCode() {
		return errorCode ;
	}

}