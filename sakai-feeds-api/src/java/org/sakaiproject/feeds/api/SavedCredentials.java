package org.sakaiproject.feeds.api;

import java.io.Serializable;
import java.net.URI;


public interface SavedCredentials extends Serializable {

	public URI getUri();
	public void setUri(URI uri);

	public String getUsername();
	public void setUsername(String username);

	public String getPassword();
	public void setPassword(String password);

	public String getRealm();
	public void setRealm(String realm);

	public String getScheme();
	public void setScheme(String scheme);

}
