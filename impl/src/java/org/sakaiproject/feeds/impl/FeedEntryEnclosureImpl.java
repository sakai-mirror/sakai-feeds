package org.sakaiproject.feeds.impl;

import org.sakaiproject.feeds.api.FeedEntryEnclosure;


public class FeedEntryEnclosureImpl implements FeedEntryEnclosure {
	private static final long	serialVersionUID	= 1L;
	private String				url;
	private String				type;
	private long				length;

	public long getLength() {
		return length;
	}

	public String getType() {
		return type;
	}

	public String getUrl() {
		return url;
	}

	public void setLength(long length) {
		this.length = length;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setUrl(String url) {
		this.url = url;
	}

}
