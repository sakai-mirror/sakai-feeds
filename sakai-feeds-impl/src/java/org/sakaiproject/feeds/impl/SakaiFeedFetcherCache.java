package org.sakaiproject.feeds.impl;

import java.io.Serializable;
import java.net.URI;
import java.net.URL;

import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.MemoryService;

import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import com.sun.syndication.fetcher.impl.SyndFeedInfo;


/**
 * Hash table and linked list implementation of the Map interface,
 * access-ordered. Older entries will be removed if map exceeds the maximum
 * capacity specified.
 * @author nfernandes
 */
public class SakaiFeedFetcherCache implements FeedFetcherCache, Serializable {
	private static final long				serialVersionUID	= 1L;
	private Cache							infoCache;

	// ######################################################
	// Spring methods
	// ######################################################
	private MemoryService					memoryService;
	public void setMemoryService(MemoryService memoryService) {
		this.memoryService = memoryService;
	}
	
	public void init() {
		infoCache = memoryService.newCache(this.getClass().getName());
	}
	
	public void destroy() {
		infoCache.clear();
	}
	
	
	// ######################################################
	// SakaiFeedFetcherCache methods
	// ######################################################

	public SyndFeedInfo getFeedInfo(URL url) {
		return (SyndFeedInfo) infoCache.get(urlToUri(url));
	}

	public SyndFeedInfo getFeedInfo(URL url, String userId, String feedUsername) {
		CompoundKey key = new CompoundKey(urlToUri(url), userId, feedUsername);
		return (SyndFeedInfo) infoCache.get(key);
	}

	public boolean isRecent(URL url) {
		return infoCache.containsKey(urlToUri(url));
	}

	public boolean isRecent(URL url, String userId, String feedUsername) {
		CompoundKey key = new CompoundKey(urlToUri(url), userId, feedUsername);
		return infoCache.containsKey(key);
	}

	public void setFeedInfo(URL url, SyndFeedInfo syndFeedInfo) {
		URI uri = urlToUri(url);
		if(uri != null) {
			infoCache.put(uri, syndFeedInfo);
		}
	}

	public void setFeedInfo(URL url, String userId, String feedUsername, SyndFeedInfo syndFeedInfo) {
		URI uri = urlToUri(url);
		if(uri != null) {
			CompoundKey key = new CompoundKey(uri, userId, feedUsername);
			infoCache.put(key, syndFeedInfo);
		}
	}

	public void clearFeedInfo(URL url) {
		URI uri = urlToUri(url);
		if(uri != null) {
			infoCache.remove(uri);
		}
	}

	public void clearFeedInfo(URL url, String userId, String feedUsername) {
		URI uri = urlToUri(url);
		if(uri != null) {
			CompoundKey key = new CompoundKey(uri, userId, feedUsername);
			infoCache.remove(key);
		}
	}

	public void clear() {
		infoCache.clear();
	}

	public SyndFeedInfo remove(URL url) {
		URI uri = urlToUri(url);
		if(uri != null) {
			SyndFeedInfo feedInfo = (SyndFeedInfo) infoCache.get(uri);
			infoCache.remove(uri);
			return feedInfo;
		}
		return null;
	}
	
	private URI urlToUri(URL url) {
		try{
			return url.toURI();
		}catch(Exception e){
			return null;
		}
	}
	
	static class CompoundKey implements Serializable {
		private static final long	serialVersionUID	= 1L;
		private Object o;
		private String userId;
		private String feedUsername;
		
		public CompoundKey(Object o, String userId, String feedUsername) {
			this.o = o;
			this.userId = userId;
			this.feedUsername = feedUsername;
		}
		
		public String getUserId() {
			return userId;
		}
		
		public String getFeedUsername() {
			return feedUsername;
		}

		@Override
		public int hashCode() {
			return 	o!=null? o.hashCode() : 1
					+ userId!=null? userId.hashCode() : 2
					+ feedUsername!=null? feedUsername.hashCode() : 4;
		}

		@Override
		public boolean equals(Object obj) {
			if(obj == null || !(obj instanceof CompoundKey)) {
				return false;
			}else{
				CompoundKey ck = (CompoundKey) obj;
				return userId.equals(ck.getUserId())
					&& feedUsername.equals(ck.getFeedUsername());
			}
		}
		
	}
}
