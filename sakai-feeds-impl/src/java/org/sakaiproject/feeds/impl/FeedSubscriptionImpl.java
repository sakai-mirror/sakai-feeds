package org.sakaiproject.feeds.impl;

import org.sakaiproject.feeds.api.FeedSubscription;

public class FeedSubscriptionImpl implements FeedSubscription {
	private static final long	serialVersionUID	= 1L;
	private String title;
	private String description;
	private String url;
	private String[] urls;
	private String iconUrl;
	private boolean selected;
	private boolean aggregate;

	public String getDescription() {
		return description;
	}

	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return url;
	}
	
	public String[] getUrls() {
		return urls;
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

	public void setUrls(String[] urls) {
		this.urls = urls;
		setAggregateMultipleFeeds(true);
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
	
	public boolean isAggregateMultipleFeeds() {
		return aggregate;
	}
	
	/** Set if subscription aggregates multiple feeds. */
	public void setAggregateMultipleFeeds(boolean aggregate) {
		this.aggregate = aggregate;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		FeedSubscription fs = (FeedSubscription) super.clone();
		fs.setAggregateMultipleFeeds(isAggregateMultipleFeeds());
		fs.setDescription(getDescription());
		fs.setIconUrl(getIconUrl());
		fs.setSelected(isSelected());
		fs.setTitle(getTitle());
		fs.setUrl(getUrl());
		fs.setUrls(getUrls());
		return fs;
	}

	
}
