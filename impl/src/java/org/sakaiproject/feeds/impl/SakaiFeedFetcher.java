package org.sakaiproject.feeds.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.auth.AuthState;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.util.HttpURLConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.feeds.api.FeedsService;
import org.sakaiproject.feeds.api.exception.FeedAuthenticationException;
import org.sakaiproject.tool.cover.SessionManager;

import com.sun.org.apache.regexp.internal.RE;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FetcherEvent;
import com.sun.syndication.fetcher.FetcherException;
import com.sun.syndication.fetcher.impl.AbstractFeedFetcher;
import com.sun.syndication.fetcher.impl.SyndFeedInfo;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class SakaiFeedFetcher extends AbstractFeedFetcher {
	private static Log							LOG	= LogFactory.getLog(SakaiFeedFetcher.class);
	private SakaiFeedFetcherCache				feedInfoCache;
	private CredentialSupplier					credentialSupplier;
	private MultiThreadedHttpConnectionManager	connectionManager;
	private HttpClient							client;

	public SakaiFeedFetcher() {
		this(null, 30000, true);
	}

	public SakaiFeedFetcher(SakaiFeedFetcherCache cache, int timeout, boolean ignoreCertificateErrors) {
		super();
		
		// configure logging
		System.setProperty("org.apache.commons.httpclient", "warn");
				
		// configure cache
		setFeedInfoCache(cache);
		
		// configure connection manager
		connectionManager = new MultiThreadedHttpConnectionManager();
		connectionManager.getParams().setConnectionTimeout(timeout);

		// ignore SSL certificate errors
		if(ignoreCertificateErrors) {
			Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
			Protocol.registerProtocol("https", easyhttps);
		}
		
		// configure http client
		client = new HttpClient(connectionManager);
	}
	
	public HttpClient getHttpClient(URL feedUrl) {
		HostConfiguration config = new HostConfiguration();//client.getHostConfiguration();
		//if(config == null){
		//	config = new HostConfiguration();
		//}
		config.setHost(feedUrl.getHost());
		String nonProxyHosts = System.getProperty("http.nonProxyHosts");
		
		boolean proxiableHost = true;
		if(nonProxyHosts != null){
			StringTokenizer tok = new StringTokenizer(nonProxyHosts, "|");

			while (tok.hasMoreTokens()){
				String nonProxiableHost = tok.nextToken().trim();

				// XXX is there any other characters that need to be
				// escaped?
				//nonProxiableHost = StringUtils.replace(nonProxiableHost, ".", "\\.");
				//nonProxiableHost = StringUtils.replace(nonProxiableHost, "*", ".*");
				nonProxiableHost = nonProxiableHost.replaceAll("\\.", "\\\\.");
				nonProxiableHost = nonProxiableHost.replaceAll("\\*", ".*");

				// XXX do we want .example.com to match
				// computer.example.com? it seems to be a very common
				// idiom for the nonProxyHosts, in that case then we want
				// to change "^" to "^.*"
				RE re = null;
				try{
					re = new RE("^.*" + nonProxiableHost + "$");

					if(re.match(feedUrl.getHost())){
						proxiableHost = false;
						break;
					}
				}catch(Exception ex){
					LOG.warn("Unable to parse http.nonProxyHosts: "+nonProxyHosts);
				}
			}
		}

		if(proxiableHost && System.getProperty("http.proxyHost") != null){
			String proxyHost = System.getProperty("http.proxyHost");
			int proxyPort;
			try{
				proxyPort = Integer.parseInt(System.getProperty("http.proxyPort"));				
			}catch(Exception e){
				proxyPort = 80;
			}
			config.setProxy(proxyHost, proxyPort);
		}
		
		client.setHostConfiguration(config);
		
		return client;
	}
	
	public void destroy() {
		connectionManager.shutdown();
		client = null;
	}

	/**
	 * @return the feedInfoCache.
	 */
	public synchronized SakaiFeedFetcherCache getFeedInfoCache() {
		return feedInfoCache;
	}
	
    /**
	 * @param feedInfoCache the feedInfoCache to set
	 */
	public synchronized void setFeedInfoCache(SakaiFeedFetcherCache feedInfoCache) {
		this.feedInfoCache = feedInfoCache;
	}

	/**
     * @return Returns the credentialSupplier.
     */
    public CredentialSupplier getCredentialSupplier() {
        return credentialSupplier;
    }
    /**
     * @param credentialSupplier The credentialSupplier to set.
     */
    public void setCredentialSupplier(CredentialSupplier credentialSupplier) {
        this.credentialSupplier = credentialSupplier;
    }	
    
    public void addCredentials(URL url, Credentials credentials) {
    	if(getCredentialSupplier() != null)
    		getCredentialSupplier().addCredentials(url, credentials);
    }
	
	/**
	 * @throws FeedAuthenticationException 
	 * @see com.sun.syndication.fetcher.FeedFetcher#retrieveFeed(java.net.URL)
	 */
    public SyndFeed retrieveFeed(URL feedUrl) throws IllegalArgumentException, IOException, FeedException, FetcherException {
    	return retrieveFeed(feedUrl, true);
    }
    
	public SyndFeed retrieveFeed(URL feedUrl, boolean forceExternalCheck) throws IllegalArgumentException, IOException, FeedException, FetcherException {
		if (feedUrl == null) {
			throw new IllegalArgumentException("null is not a valid URL");
		}
		
		SyndFeed feed = null;
		Credentials credentials = getCredentialSupplier().getCredentials(feedUrl);
		
		// Check if feed was recently cached
		if(getFeedInfoCache() instanceof SakaiFeedFetcherCache) {
			SakaiFeedFetcherCache cache = (SakaiFeedFetcherCache) getFeedInfoCache();
			String userId = null;
			String feedUsername = null;
			if (credentials != null) {
				if(credentials instanceof UsernamePasswordRealmSchemeCredentials) {
					feedUsername = ((UsernamePasswordRealmSchemeCredentials) credentials).getUserName();
				}
				userId = SessionManager.getCurrentSessionUserId();
				if(userId == null){
					userId = (String) SessionManager.getCurrentSession().getAttribute(FeedsService.SESSION_ATTR_FEED_USER_ID);
				}
				if(!forceExternalCheck && cache.isRecent(feedUrl, userId, feedUsername)){
					// cached authenticated feed
					LOG.debug("Authenticated feed was recently cached - returning feed from cache: "+feedUrl.toString());
					return cache.getFeedInfo(feedUrl, userId, feedUsername).getSyndFeed();
				}
			}else if(!forceExternalCheck && cache.isRecent(feedUrl)){
				// cached non-authenticated feed
				LOG.debug("Feed was recently cached - returning feed from cache: "+feedUrl.toString());
				return cache.getFeedInfo(feedUrl).getSyndFeed();
			}
		}		
		
		// Use an HttpState per user
		HttpState httpState = getHttpState();
		HttpClient _client = getHttpClient(feedUrl);
		_client.setState(httpState);
		
		// Retrieve feed
		LOG.debug("Retrieving feed: "+feedUrl.toExternalForm());
		AuthScope authScope = null;
		if(credentials != null && credentials instanceof UsernamePasswordRealmSchemeCredentials) {
			UsernamePasswordRealmSchemeCredentials c = (UsernamePasswordRealmSchemeCredentials) credentials;
			authScope = new AuthScope(feedUrl.getHost(), feedUrl.getPort(), c.getRealm(), c.getScheme());
		}				
		feed = attemptRetrieveFeed(feedUrl, _client, authScope, credentials);
		
		return feed;
	}

	private HttpState getHttpState() {
		Object o = SessionManager.getCurrentSession().getAttribute(FeedsService.SESSION_ATTR_HTTPSTATE);
		HttpState httpState;
		if(o != null && o instanceof HttpState){
			httpState = (HttpState) o;
		}else{
			httpState = new HttpState();
			SessionManager.getCurrentSession().setAttribute(FeedsService.SESSION_ATTR_HTTPSTATE, httpState);
		}
		return httpState;
	}

	private SyndFeed attemptRetrieveFeed(URL feedUrl, HttpClient client, AuthScope authScope, Credentials credentials) throws IOException, HttpException, FetcherException, FeedException, MalformedURLException {
		System.setProperty("httpclient.useragent", getUserAgent());
		String urlStr = feedUrl.toString();
		SakaiFeedFetcherCache cache = getFeedInfoCache();
		if (cache != null) {
			// retrieve feed
			HttpMethod method = new GetMethod(urlStr);
			method.addRequestHeader("Accept-Encoding", "gzip");
			try {
				if (isUsingDeltaEncoding()) {
				    method.setRequestHeader("A-IM", "feed");
				}	    

				// get the feed info from the cache
			    // Note that syndFeedInfo will be null if it is not in the cache
				SyndFeedInfo syndFeedInfo = null;
				String userId = null;
				String feedUsername = null;
				if(authScope == null && credentials == null) {
					// cached non-authenticated feed
					syndFeedInfo = cache.getFeedInfo(feedUrl);			
				}else{
					// cached authenticated feed
					if(credentials instanceof UsernamePasswordRealmSchemeCredentials) 
						feedUsername = ((UsernamePasswordRealmSchemeCredentials) credentials).getUserName();
					userId = SessionManager.getCurrentSessionUserId();
					if(userId == null){
						userId = (String) SessionManager.getCurrentSession().getAttribute(FeedsService.SESSION_ATTR_FEED_USER_ID);
					}
					syndFeedInfo = cache.getFeedInfo(feedUrl, userId, feedUsername);
				}
			    if (syndFeedInfo != null) {
				    method.setRequestHeader("If-None-Match", syndFeedInfo.getETag());				    
				    if (syndFeedInfo.getLastModified() instanceof String) {
				        method.setRequestHeader("If-Modified-Since", (String)syndFeedInfo.getLastModified());
				    }
			    }
			    
			    method.setFollowRedirects(true);			    		    			    
			    int statusCode = executeHttpClientMethod(feedUrl, client, method, authScope, credentials);
			    SyndFeed feed = getFeed(syndFeedInfo, urlStr, method, statusCode);		
				syndFeedInfo = buildSyndFeedInfo(feedUrl, urlStr, method, feed, statusCode, credentials);
				if(authScope == null && credentials == null) {
					cache.setFeedInfo(new URL(urlStr), syndFeedInfo);
				}else{
					cache.setFeedInfo(new URL(urlStr), userId, feedUsername, syndFeedInfo);
				}
					
				// the feed may have been modified to pick up cached values
				// (eg - for delta encoding)
				feed = syndFeedInfo.getSyndFeed();
	
				return feed;
			} finally {
				method.releaseConnection();
			}
				
		} else {
		    // cache is not in use		    
			HttpMethod method = new GetMethod(urlStr);
			try {
			    method.setFollowRedirects(true);			    
				int statusCode = executeHttpClientMethod(feedUrl, client, method, authScope, credentials);			    
				return getFeed(null, urlStr, method, statusCode);
			} finally {			
				method.releaseConnection();
			}
		}
	}

	private int executeHttpClientMethod(URL feedUrl, HttpClient client, HttpMethod method, AuthScope authScope, Credentials credentials) throws IOException, HttpException, FetcherException {
		int statusCode = 0;
		if(authScope != null && credentials != null){
			client.getState().setCredentials(authScope, credentials);
		}
		method.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		Cookie[] cookies = client.getState().getCookies();
		for(int i=0; i<cookies.length; i++){
			if(cookies[i].getDomain().equals(feedUrl.getHost())){
				method.addRequestHeader("Cookie", cookies[i].toString());
				LOG.debug("Using cookie: "+cookies[i].getDomain()+", "+cookies[i].getName()+", "+cookies[i].getPath()+", "+cookies[i].getValue());
			}
		}
		statusCode = client.executeMethod(method);
		AuthState authState = method.getHostAuthState();

		fireEvent(FetcherEvent.EVENT_TYPE_FEED_POLLED, feedUrl.toString());
		switch(statusCode) {
			case 401:
				handleAuthenticationError(authState);
				break;
			default:
				handleErrorCodes(statusCode);
		}
		return statusCode;
	}


	/**
     * @param feedUrl
     * @param urlStr
     * @param method
     * @param feed
     * @return
     * @throws MalformedURLException
     */
    private SyndFeedInfo buildSyndFeedInfo(URL feedUrl, String urlStr, HttpMethod method, SyndFeed feed, int statusCode, Credentials credentials) throws MalformedURLException {
        SyndFeedInfo syndFeedInfo;
        syndFeedInfo = new SyndFeedInfo();
        
        // this may be different to feedURL because of 3XX redirects
        syndFeedInfo.setUrl(new URL(urlStr));
        syndFeedInfo.setId(feedUrl.toString());                					
                
        Header imHeader = method.getResponseHeader("IM");
        if (imHeader != null && imHeader.getValue().indexOf("feed") >= 0 && isUsingDeltaEncoding()) {
			SakaiFeedFetcherCache cache = getFeedInfoCache();
			if (cache != null && statusCode == 226) {
			    // client is setup to use http delta encoding and the server supports it and has returned a delta encoded response
			    // This response only includes new items
				SyndFeedInfo cachedInfo = null;
				if(credentials == null) {
					// cached non-authenticated feed
					cachedInfo = cache.getFeedInfo(feedUrl);
				}else{
					// cached authenticated feed
					String userId = SessionManager.getCurrentSessionUserId();
					if(userId == null){
						userId = (String) SessionManager.getCurrentSession().getAttribute(FeedsService.SESSION_ATTR_FEED_USER_ID);
					}
					String feedUsername = null;
					if(credentials instanceof UsernamePasswordRealmSchemeCredentials) 
						feedUsername = ((UsernamePasswordRealmSchemeCredentials) credentials).getUserName();
					cachedInfo = cache.getFeedInfo(feedUrl, userId, feedUsername);
				}
			    if (cachedInfo != null) {
				    SyndFeed cachedFeed = cachedInfo.getSyndFeed();
				    
				    // set the new feed to be the orginal feed plus the new items
				    feed = combineFeeds(cachedFeed, feed);			        
			    }            
			}
		}
        
        Header lastModifiedHeader = method.getResponseHeader("Last-Modified");
        if (lastModifiedHeader != null) {
            syndFeedInfo.setLastModified(lastModifiedHeader.getValue());
        }
        
        Header eTagHeader = method.getResponseHeader("ETag");
        if (eTagHeader != null) {
            syndFeedInfo.setETag(eTagHeader.getValue());
        }
        
        syndFeedInfo.setSyndFeed(feed);
        
        return syndFeedInfo;
    }

    /**
	 * @param client
	 * @param urlStr
	 * @param method
	 * @return
	 * @throws IOException
	 * @throws HttpException
	 * @throws FetcherException
	 * @throws FeedException
	 */
	private static SyndFeed retrieveFeed(String urlStr, HttpMethod method) throws IOException, HttpException, FetcherException, FeedException {
		//LOG.info("Retrieving feed: "+urlStr);
		InputStream stream = null;
		if ((method.getResponseHeader("Content-Encoding") != null) && ("gzip".equalsIgnoreCase(method.getResponseHeader("Content-Encoding").getValue()))) {
			stream = new GZIPInputStream(method.getResponseBodyAsStream());
		} else {			
			stream = method.getResponseBodyAsStream();
		}
		try {		
		    XmlReader reader = null;
		    if (method.getResponseHeader("Content-Type") != null) {			
				reader = new XmlReader(stream, method.getResponseHeader("Content-Type").getValue(), true);
		    } else {			
				reader = new XmlReader(stream, true);
		    }
		    SyndFeedInput syndFeedInput = new SyndFeedInput();
		    syndFeedInput.setXmlHealerOn(true);
			return syndFeedInput.build(reader);
		}finally {
		    if (stream != null) {
		        stream.close();
		    }
		}
	}

	private static String unEscapeHtml(String value) {
		if (value == null) return "";
		if (value.equals("")) return "";
		String unescaped = value.replaceAll("&lt;", "<");
		unescaped = unescaped.replaceAll("&gt;", ">");
		unescaped = unescaped.replaceAll("&amp;", "&");
		unescaped = unescaped.replaceAll("&quot;", "\"");
		return unescaped;
	}

	private SyndFeed getFeed(SyndFeedInfo syndFeedInfo, String urlStr, HttpMethod method, int statusCode) throws IOException, HttpException, FetcherException, FeedException {

		if (statusCode == HttpURLConnection.HTTP_NOT_MODIFIED && syndFeedInfo != null) {
		    fireEvent(FetcherEvent.EVENT_TYPE_FEED_UNCHANGED, urlStr);
		    return syndFeedInfo.getSyndFeed();
		}			
		
		SyndFeed feed = retrieveFeed(urlStr, method);	
		fireEvent(FetcherEvent.EVENT_TYPE_FEED_RETRIEVED, urlStr, feed);			
		return feed;
	}
	
	private void handleAuthenticationError(AuthState authState) throws FetcherException {
		InnerFetcherException e = new InnerFetcherException(401, "Authentication required for that resource. HTTP Response code was:" + 401);
		e.setAuthState(authState);
		throw e;
	}


	public interface CredentialSupplier {
		public void addCredentials(URL url, Credentials credentials);
        public Credentials getCredentials(URL url);
    }
	
	public static class InnerFetcherException extends FetcherException {
		private static final long	serialVersionUID	= 1L;
		private AuthState			authState			= null;

		public InnerFetcherException(int responseCode, String message) {
			super(responseCode, message);
		}

		public InnerFetcherException(String message, Throwable cause) {
			super(message, cause);
		}

		public InnerFetcherException(String message) {
			super(message);
		}

		public InnerFetcherException(Throwable cause) {
			super(cause);
		}

		public AuthState getAuthState() {
			return authState;
		}

		public void setAuthState(AuthState authState) {
			this.authState = authState;
		}
		
	}
}
