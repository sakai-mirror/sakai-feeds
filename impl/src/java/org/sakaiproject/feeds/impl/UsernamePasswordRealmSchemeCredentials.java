package org.sakaiproject.feeds.impl;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;


public class UsernamePasswordRealmSchemeCredentials extends UsernamePasswordCredentials {
	private String	userName;
	private String	password;
	private String	realm;
	private String	scheme;

	public UsernamePasswordRealmSchemeCredentials(String userName, String password, String realm, String scheme) {
		setUserName(userName);
		setPassword(password);
		setRealm(realm);
		setScheme(scheme);
	}

	/*public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}*/

	public String getRealm() {
		if(realm == null || realm.trim().equals(""))
			return AuthScope.ANY_REALM;
		else
			return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public String getScheme() {
		if(scheme == null || scheme.trim().equals(""))
			return AuthScope.ANY_SCHEME;
		else
			return scheme;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append((getUserName() == null) ? "null" : getUserName());
		result.append(":");
		result.append((getPassword() == null) ? "null" : getPassword());
		result.append(":");
		result.append((getRealm() == null) ? "null" : getRealm());
		result.append(":");
		result.append((getScheme() == null) ? "null" : getScheme());
		return result.toString();
	}	
	
}
