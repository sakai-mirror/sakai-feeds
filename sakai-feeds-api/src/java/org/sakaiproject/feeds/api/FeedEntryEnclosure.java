package org.sakaiproject.feeds.api;

import java.io.Serializable;

public interface FeedEntryEnclosure extends Serializable {

	/** Get the url. */
	public String getUrl();
	
	/** Set the url. */
	public void setUrl(String url);

	/** Get the type. */
	public String getType();
	
	/** Set the type. */
	public void setType(String type);

	/** Get the length. */
	public long getLength();
	
	/** Set the length. */
	public void setLength(long length);

}
