package org.wso2.carbon.repository.exceptions;

@SuppressWarnings("serial")
public class RepositoryAuthException extends RepositoryUserException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message.
     */
	public RepositoryAuthException(String message) {
		super(message);
	}
	
    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message.
     * @param errorCode error code of the error
     */
	public RepositoryAuthException(String message, int errorCode) {
		super(message, errorCode);
	}

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause of this exception.
     */
	public RepositoryAuthException(String message, Throwable cause) {
		super(message, cause);
	}
	
    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause of this exception.
     * @param errorCode error code of the error
     */
	public RepositoryAuthException(String message, Throwable cause, int errorCode) {
		super(message, cause, errorCode);
	}
}
