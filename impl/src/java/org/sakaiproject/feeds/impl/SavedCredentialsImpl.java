package org.sakaiproject.feeds.impl;

import java.net.URL;

import org.sakaiproject.feeds.api.SavedCredentials;


public class SavedCredentialsImpl implements SavedCredentials {
	private static final long	serialVersionUID	= 1L;
	private String				username;
	private String				password;
	private String				realm;
	private String				scheme;
	private URL					url;

	public SavedCredentialsImpl(URL url, String realm, String username, String password, String scheme) {
		this.url = url;
		this.realm = realm;
		this.username = username;
		this.password = password;
		this.scheme = scheme;
	}

	public SavedCredentialsImpl() {
	}

	public URL getUrl() {
		return url;
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

	public void setUrl(URL url) {
		this.url = url;
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
