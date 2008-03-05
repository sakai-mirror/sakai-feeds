package org.sakaiproject.feeds.api.provider;

import java.io.IOException;
import java.net.MalformedURLException;

import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.feeds.api.Feed;
import org.sakaiproject.feeds.api.exception.FetcherException;
import org.sakaiproject.feeds.api.exception.InvalidFeedException;

public interface InternalFeedStorageProvider {
	
	/** Get a feed by Reference. */
	public Feed getFeed(EntityReference reference) throws IllegalArgumentException, MalformedURLException, IOException, InvalidFeedException, FetcherException;

	/** Get a feed in XML format by Reference. */
	public String getFeedXml(EntityReference reference) throws IllegalArgumentException, MalformedURLException, IOException, InvalidFeedException, FetcherException;

	/** Check if user is allowed to create new (internal) feeds. */
	public boolean allowCreateNewFeed();

	/** Check if user is allowed to add entries to (internal) feeds. */
	public boolean allowAddFeedEntry(Feed feed);

	/** Check if user is allowed to edit entries on (internal) feeds. */
	public boolean allowEditFeedEntry(Feed feed);

	/** Check if user is allowed to delete entries on (internal) feeds. */
	public boolean allowDeleteFeedEntry(Feed feed);
	
	/** Save a (internal) feed. */
	public boolean saveFeed(Feed feed);
	
	/**
	 *  Get the URL prefix for internal feeds.<br>
	 *  If null, Sakai will handle this ([sakai_server_url]/direct/feed/[id]).<br>
	 *  The returning URL prefix concatenated with /feed/[id] must produce the feed URL.
	 */
	public String getInternalFeedUrlPrefix(); 
}
