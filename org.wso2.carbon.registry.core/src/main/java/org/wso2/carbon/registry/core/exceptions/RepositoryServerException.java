package org.wso2.carbon.registry.core.exceptions;

import org.wso2.carbon.repository.exceptions.RepositoryException;

@SuppressWarnings("serial")
public class RepositoryServerException extends RepositoryException {

    /**
     * Constructs a new server content exception with the specified detail message.
     *
     * @param message the detail message.
     */
	public RepositoryServerException(String message) {
		super(message);
	}

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause of this exception.
     */
	public RepositoryServerException(String message, Throwable cause) {
		super(message, cause);
	}
}
