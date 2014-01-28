package org.wso2.carbon.repository.exceptions;

@SuppressWarnings("serial")
public class RepositoryUserException extends RepositoryException {

    /**
     * Constructs a new user exception with the specified detail message.
     *
     * @param message the detail message.
     */
	public RepositoryUserException(String message) {
		super(message);
	}
	
    /**
     * Constructs a new user exception with the specified detail message.
     *
     * @param message the detail message.
     * @param errorCode error code of the error
     */
	public RepositoryUserException(String message, int errorCode) {
		super(message, errorCode) ;
	}

    /**
     * Constructs a new user exception with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause of this exception.
     */
	public RepositoryUserException(String message, Throwable cause) {
		super(message, cause);
	}
	
    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause of this exception.
     * @param errorCode error code of the error
     */
	public RepositoryUserException(String message, Throwable cause, int errorCode) {
		super(message, cause, errorCode);
	}
}

