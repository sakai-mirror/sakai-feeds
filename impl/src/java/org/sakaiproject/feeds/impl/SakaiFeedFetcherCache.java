package org.sakaiproject.feeds.impl;

import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

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
	private CacheMap<Object, SyndFeedInfo>	infoCache;
	private CacheMap<Object, Date>			fetchDateCache;
	private long 							recentForMs;

	public SakaiFeedFetcherCache(int maxCachedEntries, long markFeedRecentForMs) {
		infoCache = new CacheMap<Object, SyndFeedInfo>(maxCachedEntries);
		fetchDateCache = new CacheMap<Object, Date>(maxCachedEntries);
		recentForMs = markFeedRecentForMs;
	}

	public SyndFeedInfo getFeedInfo(URL url) {
		return infoCache.get(urlToUri(url));
	}

	public SyndFeedInfo getFeedInfo(URL url, String userId, String feedUsername) {
		return infoCache.get(urlToUri(url), userId, feedUsername);
	}

	public boolean isRecent(URL url) {
		Date feedFetchDate = fetchDateCache.get(urlToUri(url));
		if(feedFetchDate != null){
			return (feedFetchDate.getTime() + recentForMs) >= (new Date().getTime()); 
		}
		return false;
	}

	public boolean isRecent(URL url, String userId, String feedUsername) {
		URI uri = urlToUri(url);
		Date feedFetchDate = fetchDateCache.get(uri, userId, feedUsername);
		if(feedFetchDate != null){
			return (feedFetchDate.getTime() + recentForMs) >= (new Date().getTime()); 
		}
		return false;
	}

	public void setFeedInfo(URL url, SyndFeedInfo syndFeedInfo) {
		URI uri = urlToUri(url);
		if(uri != null) {
			infoCache.put(uri, syndFeedInfo);
			fetchDateCache.put(uri, new Date());
		}
	}

	public void setFeedInfo(URL url, String userId, String feedUsername, SyndFeedInfo syndFeedInfo) {
		URI uri = urlToUri(url);
		if(uri != null) {
			infoCache.put(uri, userId, feedUsername, syndFeedInfo);
			fetchDateCache.put(uri, userId, feedUsername, new Date());
		}
	}
	
	private URI urlToUri(URL url) {
		try{
			return url.toURI();
		}catch(Exception e){
			return null;
		}
	}

	static class CacheMap<K, V> extends LinkedHashMap<K, V> implements Serializable {
		private static final long	serialVersionUID	= 1L;
		private final static float	DEFAULT_LOAD_FACTOR	= 0.75f;
		private int					maxCachedEntries;

		public CacheMap(int maxCachedEntries) {
			super(maxCachedEntries, DEFAULT_LOAD_FACTOR, true);
			this.maxCachedEntries = maxCachedEntries;
		}

		public void setMaxCachedEntries(int maxCachedEntries) {
			this.maxCachedEntries = maxCachedEntries;
		}

		@Override
		protected boolean removeEldestEntry(Entry<K, V> eldest) {
			return size() > maxCachedEntries;
		}
		
		@Override
		public V get(Object key) {
			synchronized(this) {
				return super.get(key);
			}			
		}
		public V get(Object key, String userId, String feedUsername) {
			synchronized(this) {
				return super.get(new CompoundKey(key, userId, feedUsername));
			}			
		}

		@Override
		public V put(K key, V feed) {
			synchronized(this) {
				return super.put(key, feed);
			}		
		}
		@SuppressWarnings("unchecked")
		public V put(K key, String userId, String feedUsername, V feed) {
			synchronized(this) {
				return super.put((K) new CompoundKey(key, userId, feedUsername), feed);
			}		
		}

		@Override
		public Collection<V> values() {
			synchronized(this) {
				return super.values();
			}
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

		@Override
		public int hashCode() {
			return 	o!=null? o.hashCode() : 1
					+ userId!=null? userId.hashCode() : 2
					+ feedUsername!=null? feedUsername.hashCode() : 4;
		}

		@Override
		public boolean equals(Object obj) {
			return hashCode() == obj.hashCode();
		}
		
	}
}
