package org.sakaiproject.feeds.api;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Observer;
import java.util.Properties;
import java.util.Set;

import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.feeds.api.exception.FeedAuthenticationException;
import org.sakaiproject.feeds.api.exception.FetcherException;
import org.sakaiproject.feeds.api.exception.InvalidFeedException;


public interface FeedsService {
	public static final String	TOOL_ID								= "sakai.feeds";

	public static final String	TC_PROP_SUBCRIPTIONS				= "feedSubscriptions";
	public static final String	TC_PROP_LEVEL1_DELIMITER			= "_,_";
	public static final String	TC_PROP_LEVEL2_DELIMITER			= "_:_";
	public static final String	TC_PROP_SUBCRIPTIONS_ESC			= "__.__";
	public static final String	TC_PROP_CREDENTIALS					= "feedCredentials";
	public static final String	TC_PROP_AGGREGATE					= "aggregateFeeds";
	public static final String	TC_PROP_AGGREGATE_OPTIONS			= "aggregateFeedsOptions";

	public static final String	PREFS_VIEWOPTIONS					= "viewOptions";
	public static final String	PREFS_PROP_VIEWFILTER				= "viewFilter";
	public static final String	PREFS_PROP_VIEWDETAIL				= "viewDetail";
	public static final String	PREFS_SUBSCRIPTIONSORDER			= "subscriptionsOrder";
	public static final String	PREFS_PROP_SUBSCRIPTIONSORDER		= "subscriptionsOrder";

	public static final String	SAK_PROP_IGNORECERTERR				= "feeds.ignoreCertificateErrors";
	public static final String	SAK_PROP_INSTSUBSCRIPTIONS			= "feeds.institutional";
	public static final String	SAK_PROP_SECRETKEY					= "feeds.secret.key";
	public static final String	DEFAULT_SECRETKEY					= "a24896181a2a6bfac91b7e1b9b23018e";
	public static final String	SAK_PROP_MAXCACHEDFEEDS				= "feeds.maxCachedFeeds";
	public static final String	SAK_PROP_CACHETIMEINMIN				= "feeds.cacheTimeInMin";
	public static final String	SAK_PROP_MIGRATE					= "feeds.migrate";
	public static final String	SAK_PROP_MIGRATE_GETONLINEINFO		= "feeds.migrate.getOnlineFeedInfo";
	public static final String	SAK_PROP_MIGRATE_ALWAYSCREATETOOL	= "feeds.migrate.alwaysCreateTool";
	public static final String	SAK_PROP_MIGRATE_DEFAULTFEEDURL		= "feeds.migrate.defaultFeedUrl";
	public static final String	MIGRATE_DEFAULTFEEDURL				= "http://www.sakaiproject.org/cms/index2.php?option=com_rss&amp;feed=RSS2.0&amp;no_html=1";
	public static final String	SAK_PROP_DEFAULTVIEWDETAIL			= "feeds.defaultViewDetail";
	public static final String	SAK_PROP_DEFAULTVIEWFILTER			= "feeds.defaultViewFilter";
	public static final String	SAK_PROP_SHOW_CANCEL_AFTER			= "feeds.feedLoadCancel.showAfter";
	public static final String	SAK_PROP_SHOW_CANCEL_IMG			= "feeds.feedLoadCancel.image";

	public static final String	SAK_PROP_CACHINGTHREADS				= "feeds.cachingThreads";
	public static final String	SAK_PROP_TIMEOUT					= "feeds.timeoutMs";

	public static final String	SESSION_ATTR_CREDENTIALS			= "feedCredentials";
	public static final String	SESSION_ATTR_HTTPSTATE				= "httpState";
	public static final String	SESSION_ATTR_VIEWOPTIONS			= "viewOptions";
	public static final String	SESSION_ATTR_LOG_READ_EVENT			= "logReadEvent";
	public static final String	SESSION_ATTR_FEED_USER_ID			= "feedUserId";
	
	public static final String	LOG_EVENT_READ						= "feeds.read";
	public static final String	LOG_EVENT_SUBSCRIBE_INSTITUTIONAL	= "feeds.subscribe.institutional";
	public static final String	LOG_EVENT_SUBSCRIBE_USER			= "feeds.subscribe.user";
	public static final String	LOG_EVENT_UNSUBSCRIBE_INSTITUTIONAL	= "feeds.unsubscribe.institutional";
	public static final String	LOG_EVENT_UNSUBSCRIBE_USER			= "feeds.unsubscribe.user";

	public static final int		MODE_SUBSCRIBED						= 0;
	public static final int		MODE_ALL_INSTITUTIONAL				= 1;
	public static final int		MODE_ALL_NON_INSTITUTIONAL			= 2;
	

	/** Permission for subscribing feeds. */
	public static final String	AUTH_SUBSCRIBE						= "feeds.subscribe";
	/** Permission for creating new (internal) feeds. */
	public static final String	AUTH_NEW							= "feeds.new";
	/** Permission for adding entries to (internal) feeds. */
	public static final String	AUTH_ADD							= "feeds.add";
	/** Permission for editing entries on (internal) feeds. */
	public static final String	AUTH_EDIT							= "feeds.edit";
	/** Permission for deleting entries on (internal) feeds. */
	public static final String	AUTH_DELETE							= "feeds.delete";

	/** Check if user is allowed to edit permissions for tool. */
	public boolean allowEditPermissions();
	
	/** Check if user is allowed to subscribe feeds. */
	public boolean allowSubscribeFeeds();

	/** Check if user is allowed to create new (internal) feeds. */
	public boolean allowCreateNewFeed();

	/** Check if user is allowed to add entries to (internal) feeds. */
	public boolean allowAddFeedEntry(Feed feed);

	/** Check if user is allowed to edit entries on (internal) feeds. */
	public boolean allowEditFeedEntry(Feed feed);

	/** Check if user is allowed to delete entries on (internal) feeds. */
	public boolean allowDeleteFeedEntry(Feed feed);

	/** Subscribe a feed. */
	public void setSubscribedFeeds(Set<FeedSubscription> subscriptions);

	/** Get all subscribed feeds. */
	public Set<FeedSubscription> getSubscribedFeeds(int mode);
	
	/** Save feed view options. */
	public void setViewOptions(ViewOptions viewOptions);
	
	/** Save feed subscriptions order. */
	public void setSubscriptionsOrder(List<String> subscriptionUrls);
	
	/** Order feed subscriptions accordingly to user preferences. */
	public Set<FeedSubscription> orderFeedSubscriptions(Set<FeedSubscription> subscriptions);
	
	/** Get feed subscriptions order. */
	public List<String> getSubscriptionsOrder();
	
	/** Load feed view options. */	
	public ViewOptions getViewOptions();
	
	/** Check if feeds will be aggregated in a single feed. */
	public boolean isAggregateFeeds();
	
	/** Configure if feeds will be aggregated in a single feed. */
	public void setAggregateFeeds(boolean aggregate);

	/** Configure title display options for feeds that will be aggregated in a single feed. */
	public void setAggregateFeedsOptions(AggregateFeedOptions opt);
	
	/** Get title display options for feeds that will be aggregated in a single feed. */
	public AggregateFeedOptions getAggregateFeedsOptions();
	
	/** Check if it is possible to save feed credentials (dependes on AES Cipher initialization success) .*/
	public boolean isAbleToSaveCredentials();
	
	/** Create a new SavedCredentials object. */
	public SavedCredentials newSavedCredentials(URL url, String realm, String username, String password, String scheme);
	
	/** Save feed saved credentials. */
	public void setSavedCredentials(Set<SavedCredentials> savedCredentials);
	
	/** Load feed saved credentials. */	
	public Set<SavedCredentials> getSavedCredentials();
	
	/** Load feed saved credentials for a specific PlacementConfig. */	
	public Set<SavedCredentials> getSavedCredentialsFromPlacementConfig(Properties config);

	/** Get all institutional feeds. */
	public Set<FeedSubscription> getInstitutionalFeeds();

	/** Get a feed by reference. */
	//public Feed getFeed(String reference) throws IllegalArgumentException, MalformedURLException, IOException, InvalidFeedException, FetcherException, FeedAuthenticationException;

	/** Get a feed by reference. */
	public Feed getFeed(EntityReference reference, boolean forceExternalCheck) throws IllegalArgumentException, MalformedURLException, IOException, InvalidFeedException, FetcherException, FeedAuthenticationException;
	
	/** Get a feed in XML format by Reference. */
	public String getFeedXml(EntityReference reference) throws IllegalArgumentException, MalformedURLException, IOException, InvalidFeedException, FetcherException;
	
	/** Cache feed. Returns the task id. */
	public String cacheFeed(String feedUrl, boolean forceExternalCheck, Observer observer);
	
	/** Cancel a feed cache request by task id. */
	public void cancelCacheFeed(String id);
	
	/** Add site credentials for a given user. */
	public void addCredentials(URL url, String realm, String username, String password, String scheme);
	
	/** Load site credentials for the actual session user. */
	public void loadCredentials();
	
	/** Save a (internal) feed. */
	public boolean saveFeed(Feed feed);

	/** Build a FeedSubscription object from a Feed URL. */
	public FeedSubscription getFeedSubscriptionFromFeedUrl(String feedUrl, boolean getOnlineInfo) throws IllegalArgumentException, MalformedURLException, IOException, InvalidFeedException, FetcherException, FeedAuthenticationException;
	
	/** Build an entity reference based on feed uri. */
	public EntityReference getEntityReference(String feedUrl);
	
	/** Build an entity reference based on prefix and feed uri. */
	public EntityReference buildEntityReference(String prefix, String uri);

	/** Check if an entity with specified prefix and id exists. */
	public boolean entityExists(String entityPrefix, String id);

	/** Clear cookies (relevant to this domain) for the current user. */
	public void clearClientCokies();
	
	/** Add cookie (relevant to this domain) for the current user. */
	public void addClientCookie(String domain, String name, String value, String path, int maxAge, boolean secure);
	
	/** Log event through EventTrackingService. */
	public void logEvent(String event, FeedSubscription feedSubscription, boolean modify);
}
