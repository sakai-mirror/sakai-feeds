package org.sakaiproject.feeds.api.exception;

public class InvalidFeedException extends Exception {
	private static final long	serialVersionUID	= 1L;

	public InvalidFeedException() {
		super();
	}

	public InvalidFeedException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidFeedException(String message) {
		super(message);
	}

	public InvalidFeedException(Throwable cause) {
		super(cause);
	}

}
