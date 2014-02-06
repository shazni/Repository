package org.wso2.carbon.registry.core.exceptions;

@SuppressWarnings("serial")
public class RepositorySessionException extends RepositoryServerException {

    /**
     * Constructs a new session exception with the specified detail message.
     *
     * @param message the detail message.
     */
	public RepositorySessionException(String message) {
		super(message);
	}
	
    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause of this exception.
     */
	public RepositorySessionException(String message, Throwable cause) {
		super(message, cause);
	}
}
