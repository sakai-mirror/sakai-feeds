package org.sakaiproject.feeds.api;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public interface FeedEntry extends Serializable {

	/** Get the feed entry title. */
	public String getTitle();
	
	/** Set the feed entry title. */
	public void setTitle(String title);

	/** Get the feed entry description. */
	public String getDescription();
	
	/** Set the feed entry description. */
	public void setDescription(String description);

	/** Get the feed entry content. */
	public String getContent();
	
	/** Set the feed entry content. */
	public void setContent(String content);

	/** Get the feed entry published date. */
	public java.util.Date getPublishedDate();
	
	/** Set the feed entry published date. */
	public void setPublishedDate(Date publishedDate);
	
	/** Get the feed entry link. */
	public String getLink();
	
	/** Set the feed entry link. */
	public void setLink(String link);
	
	/** Get the feed entry enclosures. */
	public List<FeedEntryEnclosure> getEnclosures();
	
	/** Set the feed entry enclosures. */
	public void setEnclosures(List<FeedEntryEnclosure> enclosures);
	
	/** Check if feed entry is part of a subscription that aggregates multiple feeds. */
	public boolean isAggregated();
	
	/** Set if feed entry is part of a subscription that aggregates multiple feeds. */
	public void setAggregated(boolean aggregate);
	
	/** Get the feed title (for aggregated feeds). */
	public String getFeedTitle();
	
	/** Set the feed title (for aggregated feeds). */
	public void setFeedTitle(String feedTitle);
	
	/** Get the feed link (for aggregated feeds). */
	public String getFeedLink();
	
	/** Set the feed link (for aggregated feeds). */
	public void setFeedLink(String feedLink);
}
