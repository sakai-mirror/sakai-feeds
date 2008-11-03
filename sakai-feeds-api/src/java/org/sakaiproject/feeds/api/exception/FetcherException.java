package org.sakaiproject.feeds.api.exception;

public class FetcherException extends Exception {
	private static final long	serialVersionUID	= 1L;
	private int httpCode;

	public FetcherException() {
		super();
	}

	public FetcherException(String message, Throwable cause) {
		super(message, cause);
	}

	public FetcherException(String message) {
		super(message);
	}

	public FetcherException(Throwable cause) {
		super(cause);
	}

	public int getHttpCode() {
		return httpCode;
	}

	public void setHttpCode(int httpCode) {
		this.httpCode = httpCode;
	}
	
}
