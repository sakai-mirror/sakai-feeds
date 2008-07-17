package org.sakaiproject.feeds.impl;

import java.util.Date;
import java.util.List;

import org.sakaiproject.feeds.api.FeedEntry;
import org.sakaiproject.feeds.api.FeedEntryEnclosure;


public class FeedEntryImpl implements FeedEntry {
	private static final long			serialVersionUID	= 1L;
	private String						title;
	private String						description;
	private String						content;
	private Date						publishedDate;
	private String						link;
	private List<FeedEntryEnclosure>	enclosures;
	private boolean 					aggregate;
	private String						feedTitle;
	private String						feedLink;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getContent() {
		return content;
	}
	
	public void setContent(String content) {
		this.content = content;
	}

	public Date getPublishedDate() {
		return publishedDate;
	}

	public void setPublishedDate(Date publishedDate) {
		this.publishedDate = new Date(publishedDate.getTime());
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public List<FeedEntryEnclosure> getEnclosures() {
		return enclosures;
	}

	public void setEnclosures(List<FeedEntryEnclosure> enclosures) {
		this.enclosures = enclosures;
	}
	
	public boolean isAggregated() {
		return aggregate;
	}
	
	public void setAggregated(boolean aggregate) {
		this.aggregate = aggregate;
	}

	public String getFeedTitle() {
		return feedTitle;
	}

	public void setFeedTitle(String feedTitle) {
		this.feedTitle = feedTitle;
	}

	public String getFeedLink() {
		return feedLink;
	}

	public void setFeedLink(String feedLink) {
		this.feedLink = feedLink;
	}

}
