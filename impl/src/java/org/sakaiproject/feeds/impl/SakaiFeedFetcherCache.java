package org.sakaiproject.feeds.impl;

import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import com.sun.syndication.fetcher.impl.SyndFeedInfo;


/**
 * Hash table and linked list implementation of the Map interface,
 * access-ordered. Older entries will be removed if map exceeds the maximum
 * capacity specified.
 * @author nfernandes
 */
public class SakaiFeedFetcherCache implements FeedFetcherCache, Serializable {
	private static final long		serialVersionUID	= 1L;
	private Map<URI, SyndFeedInfo>	infoCache;
	private Map<URI, Date>			fetchDateCache;
	private long 					recentForMs;

	public SakaiFeedFetcherCache(int maxCachedEntries, long markFeedRecentForMs) {
		infoCache = (Collections.synchronizedMap(new CacheMap(maxCachedEntries)));
		fetchDateCache = (Collections.synchronizedMap(new CacheMap(maxCachedEntries)));
		this.recentForMs = markFeedRecentForMs;
	}

	public SyndFeedInfo getFeedInfo(URL url) {
//		int sizeInBytes = 0;
//		Iterator<SyndFeedInfo> it = infoCache.values().iterator();
//		while(it.hasNext()){
//			SyndFeedInfo sfi = it.next();
//			SyndFeed sf = sfi.getSyndFeed();
//			if(sf != null){
//				String feed = sf.toString();
//				if(feed != null)
//					sizeInBytes += feed.length();
//			}
//		}
//		System.out.println("FeedCache: feedCount: "+infoCache.size()+"  |  totalFeedSize: "+(sizeInBytes/1024)+" KB");
		return infoCache.get(urlToUri(url));
	}

	public boolean isRecent(URL url) {
		Date feedFetchDate = fetchDateCache.get(urlToUri(url));
		if(feedFetchDate != null){
			return (feedFetchDate.getTime() + recentForMs) >= (new Date().getTime()); 
		}
		return false;
	}

	public void setFeedInfo(URL url, SyndFeedInfo syndFeedInfo) {
		URI uri = urlToUri(url);
		if(uri != null) {
			infoCache.put(urlToUri(url), syndFeedInfo);
			fetchDateCache.put(urlToUri(url), new Date());
		}
	}
	
	private URI urlToUri(URL url) {
		try{
			return url.toURI();
		}catch(Exception e){
			return null;
		}
	}

	static class CacheMap extends LinkedHashMap implements Serializable {
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

		protected boolean removeEldestEntry(Map.Entry eldest) {
			return size() > maxCachedEntries;
		}
	}
}
