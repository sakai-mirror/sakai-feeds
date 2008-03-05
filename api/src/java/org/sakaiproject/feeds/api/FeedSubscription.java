package org.sakaiproject.feeds.api;

import java.io.Serializable;

public interface FeedSubscription extends Serializable {
	
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
	
	/** Get the subscription URL. */
	public String getUrl();
	
	/** Set the subscription icon. */
	public void setIconUrl(String url);
	
	/** Get the subscription icon. */
	public String getIconUrl();
	
	/** Mark the subscription as selected. */
	public void setSelected(boolean selected);
	
	/** Check if subscription is selected. */
	public boolean isSelected();
}
