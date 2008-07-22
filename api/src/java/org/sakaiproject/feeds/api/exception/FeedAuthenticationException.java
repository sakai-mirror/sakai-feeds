package org.sakaiproject.feeds.api.exception;


public class FeedAuthenticationException extends Exception {
	private static final long	serialVersionUID	= 1L;
	private int 				responseCode		= 0;
	private String				realm				= null;
	private String				scheme				= null;

	public FeedAuthenticationException(int responseCode, String msg) {
		super(msg);
		this.responseCode = responseCode;
	}

	public FeedAuthenticationException() {
		super();
	}

	public FeedAuthenticationException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public FeedAuthenticationException(String arg0) {
		super(arg0);
	}

	public FeedAuthenticationException(Throwable arg0) {
		super(arg0);
	}

	public int getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}
	
	public String getRealm() {
		return realm;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}
	
	public String getScheme() {
		return scheme;
	}
	
}
