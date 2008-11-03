package org.sakaiproject.feeds.api;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public interface Feed extends Serializable {
	
	/** Get the feed id. */
	//public EntityReference getReference();
	public String getReference();
	
	/** Set the feed id. */
	//public void setReference(EntityReference reference);
	public void setReference(String reference);
	
	/** Get the feed URL. */
	public String getFeedUrl();
	
	/** Set the feed URL. */
	public void setFeedUrl(String feedUrl);
	
	// --- Feed content ---

	/** Get the feed title. */
	public String getTitle();
	
	/** Set the feed title. */
	public void setTitle(String title);

	/** Get the feed description. */
	public String getDescription();
	
	/** Set the feed description. */
	public void setDescription(String description);

	/** Get the feed published date. */
	public java.util.Date getPublishedDate();
	
	/** Set the feed published date. */
	public void setPublishedDate(Date publishedDate);

	/** Get the feed copyright. */
	public String getCopyright();
	
	/** Set the feed copyright. */
	public void setCopyright(String copyright);

	/** Get the feed language. */
	public String getLanguage();
	
	/** Set the feed language. */
	public void setLanguage(String language);

	/** Get the feed link. */
	public String getLink();
	
	/** Set the feed link. */
	public void setLink(String link);

	/** Get the feed type. */
	public String getFeedType();
	
	/** Set the feed type. */
	public void setFeedType(String feedType);

	/** Get the feed encoding. */
	public String getFeedEncoding();
	
	/** Set the feed encoding. */
	public void setFeedEncoding(String feedEncoding);

	/** Get the feed image link. */
	public String getImageLink();
	
	/** Set the feed image link. */
	public void setImageLink(String imageLink);

	/** Get the feed image title. */
	public String getImageTitle();
	
	/** Set the feed image title. */
	public void setImageTitle(String imageTitle);

	/** Get the feed image url. */
	public String getImageUrl();
	
	/** Set the feed image url. */
	public void setImageUrl(String imageUrl);

	/** Get the feed image description. */
	public String getImageDescription();
	
	/** Set the feed image description. */
	public void setImageDescription(String imageDescription);

	/** Get the feed entries. */
	public List<FeedEntry> getEntries();
	
	/** Set the feed entries. */
	public void setEntries(List<FeedEntry> entries);
}
