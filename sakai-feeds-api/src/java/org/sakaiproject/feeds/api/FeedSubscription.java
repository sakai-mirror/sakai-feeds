package org.sakaiproject.feeds.api;

import java.io.Serializable;

public interface FeedSubscription extends Serializable, Cloneable {
	
	/** Set the subscription title. */
	public void setTitle(String title);
	
	/** Get the subscription title. */
	public String getTitle();
	
	/** Set the subscription description. */
	public void setDescription(String description);
	
	/** Get the subscription description. */
	public String getDescription();
	
	/** Set the subscription URL. */
	public void setUrl(String url);
	
	/** Set the subscription URLs, when all feeds are aggregated into a single one. */
	public void setUrls(String[] urls);
	
	/** Get the subscription URL. */
	public String getUrl();
	
	/** Get the subscription URLs, when all feeds are aggregated into a single one. */
	public String[] getUrls();
	
	/** Set the subscription icon. */
	public void setIconUrl(String url);
	
	/** Get the subscription icon. */
	public String getIconUrl();
	
	/** Mark the subscription as selected. */
	public void setSelected(boolean selected);
	
	/** Check if subscription is selected. */
	public boolean isSelected();
	
	/** Check if subscription aggregates multiple feeds. */
	public boolean isAggregateMultipleFeeds();
	
	/** Set if subscription aggregates multiple feeds. */
	public void setAggregateMultipleFeeds(boolean aggregate);
	
	public Object clone() throws CloneNotSupportedException;
}
