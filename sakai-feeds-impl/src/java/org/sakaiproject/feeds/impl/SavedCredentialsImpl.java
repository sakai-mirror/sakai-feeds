package org.sakaiproject.feeds.impl;

import java.net.URI;

import org.sakaiproject.feeds.api.SavedCredentials;


public class SavedCredentialsImpl implements SavedCredentials {
	private static final long	serialVersionUID	= 1L;
	private String				username;
	private String				password;
	private String				realm;
	private String				scheme;
	private URI					uri;

	public SavedCredentialsImpl(final URI uri, final String realm, final String username, final String password, final String scheme) {
		this.uri = uri;
		this.realm = realm;
		this.username = username;
		this.password = password;
		this.scheme = scheme;
	}

	public SavedCredentialsImpl() {
	}

	public URI getUri() {
		return uri;
	}

	public String getUsername() {
		return username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public String getRealm() {
		return realm;
	}

	public String getScheme() {
		return scheme;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;		
	}

}
