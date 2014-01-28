package org.wso2.carbon.registry.core.exceptions;

@SuppressWarnings("serial")
public class RepositoryDBException extends RepositoryServerException {
	
    /**
     * Constructs a new DB exception with the specified detail message.
     *
     * @param message the detail message.
     */
	public RepositoryDBException(String message) {
		super(message);
	}

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause of this exception.
     */
	public RepositoryDBException(String message, Throwable cause) {
		super(message, cause);
	}
}
