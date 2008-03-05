package org.sakaiproject.feeds.impl;

import org.sakaiproject.feeds.api.FeedSubscription;

public class FeedSubscriptionImpl implements FeedSubscription {
	private static final long	serialVersionUID	= 1L;
	private String title;
	private String description;
	private String url;
	private String iconUrl;
	private boolean selected;

	public String getDescription() {
		return description;
	}

	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return url;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public void setIconUrl(String url){
		this.iconUrl = url;
	}
	
	public String getIconUrl(){
		return iconUrl;
	}
	
	public void setSelected(boolean selected){
		this.selected = selected;
	}
	
	public boolean isSelected(){
		return selected;
	}

}
