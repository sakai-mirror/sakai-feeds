package org.sakaiproject.feeds.api;

import java.io.Serializable;
import java.net.URL;


public interface SavedCredentials extends Serializable {

	public URL getUrl();
	public void setUrl(URL url);

	public String getUsername();
	public void setUsername(String username);

	public String getPassword();
	public void setPassword(String password);

	public String getRealm();
	public void setRealm(String realm);

	public String getScheme();
	public void setScheme(String scheme);

}
