package org.sakaiproject.feeds.impl.provider;

import java.io.IOException;
import java.net.MalformedURLException;

import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.feeds.api.Feed;
import org.sakaiproject.feeds.api.exception.FetcherException;
import org.sakaiproject.feeds.api.exception.InvalidFeedException;
import org.sakaiproject.feeds.api.provider.InternalFeedStorageProvider;

public class DBFeedsStorageImpl implements InternalFeedStorageProvider {

	public Feed getFeed(EntityReference reference) throws IllegalArgumentException, MalformedURLException, IOException, InvalidFeedException, FetcherException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getFeedXml(EntityReference reference) throws IllegalArgumentException, MalformedURLException, IOException, InvalidFeedException, FetcherException {
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean saveFeed(Feed feed) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public String getInternalFeedUrlPrefix(){
		return null;
	}

	public boolean allowAddFeedEntry(Feed feed) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean allowCreateNewFeed() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean allowDeleteFeedEntry(Feed feed) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean allowEditFeedEntry(Feed feed) {
		// TODO Auto-generated method stub
		return false;
	}

}
