package org.sakaiproject.feeds.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.cover.FunctionManager;
import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.entitybroker.EntityBroker;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.IdEntityReference;
import org.sakaiproject.entitybroker.entityprovider.EntityProviderManager;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.InUseException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.feeds.api.AggregateFeedOptions;
import org.sakaiproject.feeds.api.Feed;
import org.sakaiproject.feeds.api.FeedEntry;
import org.sakaiproject.feeds.api.FeedEntryEnclosure;
import org.sakaiproject.feeds.api.FeedSubscription;
import org.sakaiproject.feeds.api.FeedsService;
import org.sakaiproject.feeds.api.SavedCredentials;
import org.sakaiproject.feeds.api.ViewOptions;
import org.sakaiproject.feeds.api.entity.ExternalFeedEntityProvider;
import org.sakaiproject.feeds.api.entity.InternalFeedEntityProvider;
import org.sakaiproject.feeds.api.exception.FeedAuthenticationException;
import org.sakaiproject.feeds.api.exception.FetcherException;
import org.sakaiproject.feeds.api.exception.InvalidFeedException;
import org.sakaiproject.feeds.api.provider.InstitutionalFeedProvider;
import org.sakaiproject.feeds.api.provider.InternalFeedStorageProvider;
import org.sakaiproject.feeds.impl.SakaiFeedFetcher.CredentialSupplier;
import org.sakaiproject.feeds.impl.SakaiFeedFetcher.InnerFetcherException;
import org.sakaiproject.feeds.impl.entity.ExternalFeedEntityProviderImpl;
import org.sakaiproject.id.api.IdManager;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.user.api.Preferences;
import org.sakaiproject.user.api.PreferencesEdit;
import org.sakaiproject.user.api.PreferencesService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.util.commonscodec.CommonsCodecBase64;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndImage;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;


public class FeedsServiceImpl extends Observable implements FeedsService {
	private static Log						LOG	= LogFactory.getLog(FeedsServiceImpl.class);
	private SiteService						m_siteService;
	private EntityBroker					m_entityBroker;
	private EntityProviderManager			m_entityProviderManager;
	private ServerConfigurationService		m_serverConfigurationService;
	private IdManager						m_idManager;
	private SessionManager					m_sessionManager;
	private PreferencesService				m_preferencesService;
	private UserDirectoryService			m_userDirectoryService;
	private EventTrackingService			m_eventTrackingService;
	
	// providers
	private InstitutionalFeedProvider		m_institutionalFeedProvider;
	private InternalFeedStorageProvider		m_internalFeedStorageProvider;

	private SakaiFeedFetcherCache			feedInfoCache;
	private SakaiFeedFetcher				feedFetcherAuth;
	private FeedCredentialSupplier 			feedCredentialSupplier;
	
	
	/** Feed cache thread pool. */
	private ThreadPoolExecutor				feedCacheThreadsExecutor;
	/** Feed cache task queue. */
	private LinkedBlockingQueue<Runnable>	feedCacheQueue;
	/** Number of concurrent threads for feed caching. */
	private int 							feedCachingThreads  = 10; 

	
	private Set<FeedSubscription>			institutionalFeeds;
	private String							defaultFeedIcon;
	private String							defaultViewDetail;
	private String							defaultViewFilter;
	
	private	Cipher 							cipher;
	private SecretKey						secretKey;

	// ######################################################
	// Spring methods
	// ######################################################
	public void setSiteService(SiteService siteService) {
		this.m_siteService = siteService;
	}
	
	public void setPreferencesService(PreferencesService preferencesService) {
		this.m_preferencesService = preferencesService;
	}

	public void setEntityBroker(EntityBroker entityBroker) {
		this.m_entityBroker = entityBroker;
	}

	public void setEntityProviderManager(EntityProviderManager entityProviderManager) {
		this.m_entityProviderManager = entityProviderManager;
	}

	public void setServerConfigurationService(ServerConfigurationService serverConfigurationService) {
		this.m_serverConfigurationService = serverConfigurationService;
	}
	
	public void setSessionManager(SessionManager sessionManager) {
		this.m_sessionManager = sessionManager;
	}

	public void setIdManager(IdManager idManager) {
		this.m_idManager = idManager;
	}

	public void setEventTrackingService(EventTrackingService eventTrackingService) {
		this.m_eventTrackingService = eventTrackingService;
	}
	
	public void setInstitutionalFeedProvider(InstitutionalFeedProvider institutionalFeedProvider) {
		this.m_institutionalFeedProvider = institutionalFeedProvider;
	}
	
	public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
		this.m_userDirectoryService = userDirectoryService;
	}
	public void setInternalFeedStorageProvider(InternalFeedStorageProvider internalFeedStorageProvider) {
		this.m_internalFeedStorageProvider = internalFeedStorageProvider;
	}	
	
	public void init() {
		// initialize feed fetcher
		initFeedFetcher();
		
		// initialize AES Cipher for encrypting saved passwords
		initAESCipher();
		
		// initialize queue for feeds caching requests and controlling thread~
		long keepAliveFeedCachingThreads = /* unused */ 60L;		
		feedCachingThreads = m_serverConfigurationService.getInt(SAK_PROP_CACHINGTHREADS, feedCachingThreads);
		feedCacheQueue = new LinkedBlockingQueue<Runnable>();
		feedCacheThreadsExecutor = new ThreadPoolExecutor(
				feedCachingThreads, 
				feedCachingThreads, 
				(long) keepAliveFeedCachingThreads,
				TimeUnit.SECONDS,
				feedCacheQueue);
		
		// register security functions
		FunctionManager.registerFunction(AUTH_SUBSCRIBE);
		//if(m_internalFeedStorageProvider != null) {
			// Not implemented!!! - probably never?
			//FunctionManager.registerFunction(AUTH_NEW);
			//FunctionManager.registerFunction(AUTH_ADD);
			//FunctionManager.registerFunction(AUTH_EDIT);
			//FunctionManager.registerFunction(AUTH_DELETE);
		//}
		
		// default view options
		defaultViewDetail = m_serverConfigurationService.getString(SAK_PROP_DEFAULTVIEWDETAIL, ViewOptions.DEFAULT_VIEW_DETAIL);
		defaultViewFilter = m_serverConfigurationService.getString(SAK_PROP_DEFAULTVIEWFILTER, ViewOptions.DEFAULT_VIEW_FILTER);
		
		// register entities
		m_entityProviderManager.registerEntityProvider(new ExternalFeedEntityProviderImpl());
		
		// if no provider was set, see if we can find one
		if(m_institutionalFeedProvider == null){
			m_institutionalFeedProvider = (InstitutionalFeedProvider) ComponentManager.get(InstitutionalFeedProvider.class);			
		}
		
		LOG.info("init(): provider: " + ((m_institutionalFeedProvider == null) ? "none" : m_institutionalFeedProvider.getClass().getName()));
		
		if(m_serverConfigurationService.getBoolean(SAK_PROP_MIGRATE, false)){
			LOG.info("init(): feeds.convertOldNewsTool: starting conversion of 'sakai.news' tools to new 'sakai.feeds'...");
			boolean getOnlineFeedInfo = m_serverConfigurationService.getBoolean(SAK_PROP_MIGRATE_GETONLINEINFO, true);
			boolean alwaysCreateTool = m_serverConfigurationService.getBoolean(SAK_PROP_MIGRATE_ALWAYSCREATETOOL, false);
			String sakaiDefaultUrl = m_serverConfigurationService.getString("news.feedURL", MIGRATE_DEFAULTFEEDURL);
			String defaultFeedUrl = m_serverConfigurationService.getString(SAK_PROP_MIGRATE_DEFAULTFEEDURL, sakaiDefaultUrl);
			int count = ToolMigration.convertFromOldNewsTool(getOnlineFeedInfo, alwaysCreateTool, defaultFeedUrl);
			if(count > 0)
				LOG.info("init(): feeds.convertOldNewsTool: converted old 'sakai.news' tools to new 'sakai.feeds' in "+count+" sites.");
			else
				LOG.info("init(): feeds.convertOldNewsTool: NOTHING to convert => you may want to remove '"+SAK_PROP_MIGRATE+"' from sakai.properties?");
		}
	}
	
	public void destroy() {
		LOG.info("destroy()");
		feedCacheThreadsExecutor.shutdownNow();
		if(feedFetcherAuth != null)
			feedFetcherAuth.destroy();
	}
	
	private void initFeedFetcher() {
		int maxCachedFeeds = m_serverConfigurationService.getInt(SAK_PROP_MAXCACHEDFEEDS, 100);
		int cacheTimeInMin = m_serverConfigurationService.getInt(SAK_PROP_CACHETIMEINMIN, 15);
		feedInfoCache = new SakaiFeedFetcherCache(maxCachedFeeds, cacheTimeInMin * 60 * 1000L);
		feedFetcherAuth = new SakaiFeedFetcher(feedInfoCache, 30000, m_serverConfigurationService.getBoolean(SAK_PROP_IGNORECERTERR, true));
		feedFetcherAuth.setUsingDeltaEncoding(false);
		feedFetcherAuth.setUserAgent("SakaiFeeds");
		feedCredentialSupplier = new FeedCredentialSupplier();
		feedFetcherAuth.setCredentialSupplier(feedCredentialSupplier);
		
		defaultFeedIcon = m_serverConfigurationService.getServerUrl() + "/sakai-feeds-tool/img/feed.png";		
	}

	private void initAESCipher() {
		// Secret key
		String stringKey = m_serverConfigurationService.getString(SAK_PROP_SECRETKEY, DEFAULT_SECRETKEY);
		secretKey = new SecretKeySpec(hexStringToByte(stringKey), "AES");
		// Instantiate the cipher
		try{
			cipher = Cipher.getInstance("AES");
		}catch(Exception e){
			LOG.error("Unable to initialize AES Cipher. Feed passwords will not be stored!", e);
			cipher = null;
		}
	}
	
	private String encryptData(String plain) {
		try{
			byte[] plainBytes = plain.getBytes();
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			byte[] encrypted = cipher.doFinal(plainBytes);
			return byteToHexString(encrypted);
		}catch(Exception e){
			LOG.error("Unable to encrypt feed password. Feed password will not be stored!", e);
		}
		return null;
	}
	
	private String decryptData(String encrypted) {
		try{
			byte[] encryptedBytes = hexStringToByte(encrypted);
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			byte[] original = cipher.doFinal(encryptedBytes);
			return new String(original);
		}catch(Exception e){
			LOG.warn("Unable to decrypt feed password. You may need to re-enter feed password.");
		}
		return null;
	}

	private String byteToHexString(byte buf[]) {
		StringBuffer strbuf = new StringBuffer(buf.length * 2);
		for(int i = 0; i < buf.length; i++){
			if(((int) buf[i] & 0xff) < 0x10) strbuf.append("0");
			strbuf.append(Long.toString((int) buf[i] & 0xff, 16));
		}
		return strbuf.toString();
	}
	
	private byte[] hexStringToByte(String hex) {
		byte[] bts = new byte[hex.length() / 2];
		for (int i = 0; i < bts.length; i++) {
			bts[i] = (byte) Integer.parseInt(hex.substring(2*i, 2*i+2), 16);
		}
		return bts;
	}

	// ######################################################
	// Service implementation methods
	// ######################################################
	public void setSubscribedFeeds(Set<FeedSubscription> subscriptions) {
		Placement placement = ToolManager.getCurrentPlacement();
		Properties config = placement.getPlacementConfig();
		if(config != null){
			boolean first = true;
			String tmpStr = "";
			for(FeedSubscription subscription : subscriptions){
				if(!first) tmpStr += TC_PROP_LEVEL1_DELIMITER;
				first = false;

				tmpStr += subscription.getTitle();
				tmpStr += TC_PROP_LEVEL2_DELIMITER;
				tmpStr += subscription.getDescription();
				tmpStr += TC_PROP_LEVEL2_DELIMITER;
				tmpStr += subscription.getUrl();
				tmpStr += TC_PROP_LEVEL2_DELIMITER;
				tmpStr += subscription.getIconUrl();
			}

			config.setProperty(TC_PROP_SUBCRIPTIONS, tmpStr);
			placement.save();
		}
	}

	public Set<FeedSubscription> getSubscribedFeeds(int mode) {
		Set<FeedSubscription> subscriptions = new LinkedHashSet<FeedSubscription>();

		Placement placement = ToolManager.getCurrentPlacement();
		Properties config = placement.getPlacementConfig();
		if(config != null){
			String prop = config.getProperty(TC_PROP_SUBCRIPTIONS);
			if(prop != null){
				String[] chsPair = prop.split(TC_PROP_LEVEL1_DELIMITER);
				for(int i=0; i<chsPair.length; i++){
					String[] fields = chsPair[i].split(TC_PROP_LEVEL2_DELIMITER);
					FeedSubscription subscription = new FeedSubscriptionImpl();
					subscription.setTitle(fields[0]);
					subscription.setDescription(fields[1]);
					subscription.setUrl(fields[2]);
					subscription.setIconUrl(fields[3]);
					subscriptions.add(subscription);
				}
			}
		}
		
		if(mode == MODE_SUBSCRIBED && isAggregateFeeds()) {
			return aggregateFeeds(subscriptions);
		}else {
			Set<FeedSubscription> institutional = null;
			switch(mode){
				case MODE_SUBSCRIBED:
					return orderFeedSubscriptions( subscriptions );
				case MODE_ALL_INSTITUTIONAL:
					institutional = getInstitutionalFeeds();
					return orderFeedSubscriptions( getInstitutionalMarked(institutional, subscriptions) );
				case MODE_ALL_NON_INSTITUTIONAL:
					institutional = getInstitutionalFeeds();
					return orderFeedSubscriptions( getSubscribedWithoutInstitutional(institutional, subscriptions) );
			}
			return orderFeedSubscriptions(subscriptions);
		}
	}

	private Set<FeedSubscription> getInstitutionalMarked(Set<FeedSubscription> institutional, Set<FeedSubscription> subscribed) {
		Set<FeedSubscription> result = new LinkedHashSet<FeedSubscription>();
		for(FeedSubscription i : institutional){
			boolean selected = false;
			for(FeedSubscription s : subscribed){
				if(s.getUrl().equalsIgnoreCase(i.getUrl())){
					selected = true;
					break;
				}
			}
			i.setSelected(selected);
			result.add(i);
		}
		return result;
	}

	private Set<FeedSubscription> getSubscribedWithoutInstitutional(Set<FeedSubscription> institutional, Set<FeedSubscription> subscribed) {
		Set<FeedSubscription> result = new LinkedHashSet<FeedSubscription>();
		for(FeedSubscription s : subscribed){
			boolean isInstitutional = false;
			for(FeedSubscription i : institutional){
				if(s.getUrl().equalsIgnoreCase(i.getUrl())){
					isInstitutional = true;
					break;
				}
			}
			if(!isInstitutional){
				s.setSelected(true);
				result.add(s);
			}
		}
		return result;
	}
	
	public boolean isAggregateFeeds() {
		boolean aggregate = false;
		Placement placement = ToolManager.getCurrentPlacement();
		Properties config = placement.getPlacementConfig();
		if(config != null){
			String prop = config.getProperty(TC_PROP_AGGREGATE, "false");
			aggregate = Boolean.parseBoolean(prop);
		}
		return aggregate;
	}
	
	public void setAggregateFeeds(boolean aggregate) {
		Placement placement = ToolManager.getCurrentPlacement();
		Properties config = placement.getPlacementConfig();
		if(config != null){
			config.setProperty(TC_PROP_AGGREGATE, Boolean.toString(aggregate));
			placement.save();
		}
	}
	
	public void setAggregateFeedsOptions(AggregateFeedOptions opt) {
		Placement placement = ToolManager.getCurrentPlacement();
		Properties config = placement.getPlacementConfig();
		if(config != null){
			StringBuilder tmp = new StringBuilder();
			tmp.append(opt.getTitleDisplayOption());
			tmp.append(TC_PROP_LEVEL1_DELIMITER);
			tmp.append(opt.getCustomTitle());

			config.setProperty(TC_PROP_AGGREGATE_OPTIONS, tmp.toString());
			placement.save();
		}
	}
	
	public AggregateFeedOptions getAggregateFeedsOptions() {
		AggregateFeedOptions opt = new AggregateFeedOptions();
		Placement placement = ToolManager.getCurrentPlacement();
		Properties config = placement.getPlacementConfig();
		if(config != null){
			String prop = config.getProperty(TC_PROP_AGGREGATE_OPTIONS);
			if(prop != null){
				String[] values = prop.split(TC_PROP_LEVEL1_DELIMITER);
				for(int i=0; i<values.length; i++){
					opt.setTitleDisplayOption(Integer.parseInt(values[0]));
					if(opt.getTitleDisplayOption() == AggregateFeedOptions.TITLE_DISPLAY_CUSTOM) {
						opt.setCustomTitle(values[1]);
					}
				}
			}
		}
		return opt;		
	}
	
	private Set<FeedSubscription> aggregateFeeds(Set<FeedSubscription> subscriptions) {
		Set<FeedSubscription> aggregated = new LinkedHashSet<FeedSubscription>();
		FeedSubscription a = new FeedSubscriptionImpl();
		List<String> urls = new ArrayList<String>();
		for(FeedSubscription s : subscriptions) {
			urls.add( s.getUrl() );			
		}
		a.setTitle("");
		a.setUrl("");
		a.setIconUrl(defaultFeedIcon);
		a.setUrls(urls.toArray(new String[]{}));
		aggregated.add(a);
		return aggregated;
	}
	
	public Set<FeedSubscription> orderFeedSubscriptions(Set<FeedSubscription> subscriptions) {
		Set<FeedSubscription> ordered = new LinkedHashSet<FeedSubscription>();
		List<String> orderedUrls = getSubscriptionsOrder();		

		// if this url is in subscriptions list, remove from unordered list and add to ordered list
		Set<FeedSubscription> toRemove = new LinkedHashSet<FeedSubscription>();
		for(String url : orderedUrls) {
			for(FeedSubscription fs : subscriptions){
				if(url != null && fs.getUrl() != null && url.equalsIgnoreCase(fs.getUrl())){
					toRemove.add(fs);
					ordered.add(fs);
				}
			}
		}
		subscriptions.removeAll(toRemove);
		
		// add subscriptions not specified in configured ordered list
		ordered.addAll(subscriptions);
		
		return ordered;
	}
	
	public void setSubscriptionsOrder(List<String> subscriptionUrls) {
		PreferencesEdit prefsEdit = null;
		String userId = m_sessionManager.getCurrentSessionUserId();
		try{
			prefsEdit = m_preferencesService.edit(userId);
		}catch(IdUnusedException e){
			try{
				prefsEdit = m_preferencesService.add(userId);
			}catch(Exception e1){
				LOG.warn("Unable to add view options for user "+userId, e1);
			}
		}catch(PermissionException e){
			LOG.warn("Unable to save view options for user "+userId, e);
			return;
		}catch(InUseException e){
			LOG.warn("Unable to save view options for user "+userId, e);
			return;
		}
		if(prefsEdit != null) {
			try{
				ResourcePropertiesEdit props = prefsEdit.getPropertiesEdit(PREFS_SUBSCRIPTIONSORDER);
				props.removeProperty(PREFS_PROP_SUBSCRIPTIONSORDER);
				for(String url : subscriptionUrls) {
					props.addPropertyToList(PREFS_PROP_SUBSCRIPTIONSORDER, url);
				}
			}catch(Exception e){	
				if(prefsEdit != null)
					m_preferencesService.cancel(prefsEdit);
			}
			m_preferencesService.commit(prefsEdit);		
		}
	}
	
	public List<String> getSubscriptionsOrder() {
		List<String> subscriptionUrls = new ArrayList<String>();
		
		Preferences prefs = m_preferencesService.getPreferences(m_sessionManager.getCurrentSessionUserId());
		if(prefs != null) {
			ResourceProperties rp = prefs.getProperties(PREFS_SUBSCRIPTIONSORDER);
			List<String> list = rp.getPropertyList(PREFS_PROP_SUBSCRIPTIONSORDER);
			if(list != null)
				subscriptionUrls = list;
		}
		
		return subscriptionUrls;
	}
	
	public void setViewOptions(ViewOptions viewOptions) {
		PreferencesEdit prefsEdit = null;
		String userId = m_sessionManager.getCurrentSessionUserId();
		try{
			prefsEdit = m_preferencesService.edit(userId);
		}catch(IdUnusedException e){
			try{
				prefsEdit = m_preferencesService.add(userId);
			}catch(Exception e1){
				LOG.warn("Unable to add view options for user "+userId, e1);
			}
		}catch(PermissionException e){
			LOG.warn("Unable to save view options for user "+userId, e);
			return;
		}catch(InUseException e){
			LOG.warn("Unable to save view options for user "+userId, e);
			return;
		}
		try{
			ResourcePropertiesEdit props = prefsEdit.getPropertiesEdit(PREFS_VIEWOPTIONS);
			props.addProperty(PREFS_PROP_VIEWDETAIL, viewOptions.getViewDetail());
			props.addProperty(PREFS_PROP_VIEWFILTER, viewOptions.getViewFilter());
		}catch(Exception e){	
			if(prefsEdit != null)
				m_preferencesService.cancel(prefsEdit);
		}
		m_preferencesService.commit(prefsEdit);
	}

	public ViewOptions getViewOptions() {
		ViewOptions viewOptions = new ViewOptionsImpl(defaultViewDetail, defaultViewFilter);
		
		Preferences prefs = m_preferencesService.getPreferences(m_sessionManager.getCurrentSessionUserId());
		if(prefs != null) {
			ResourceProperties rp = prefs.getProperties(PREFS_VIEWOPTIONS);
			String vd = rp.getProperty(PREFS_PROP_VIEWDETAIL);
			String vf = rp.getProperty(PREFS_PROP_VIEWFILTER);
			if(vd != null)
				viewOptions.setViewDetail(vd);
			if(vf != null)
				viewOptions.setViewFilter(vf);
		}
		
		return viewOptions;
	}
	
	public boolean isAbleToSaveCredentials() {
		return cipher != null;
	}
	
	public void setSavedCredentials(Set<SavedCredentials> savedCredentials) {
		Placement placement = ToolManager.getCurrentPlacement();
		Properties config = placement.getPlacementConfig();
		if(config != null){
			boolean first = true;
			boolean abort = false;
			String tmpStr = "";
			for(SavedCredentials crd : savedCredentials){
				if(!first) tmpStr += TC_PROP_LEVEL1_DELIMITER;
				first = false;
				
				tmpStr += crd.getUrl();
				tmpStr += TC_PROP_LEVEL2_DELIMITER;
				tmpStr += crd.getRealm();
				tmpStr += TC_PROP_LEVEL2_DELIMITER;
				tmpStr += crd.getUsername();
				tmpStr += TC_PROP_LEVEL2_DELIMITER;
				String encrypedPassword = encryptData(crd.getPassword());
				if(encrypedPassword == null){
					abort = true;
					break;
				}
				tmpStr += encrypedPassword;
				tmpStr += TC_PROP_LEVEL2_DELIMITER;
				tmpStr += crd.getScheme();
			}
			if(!abort) {
				config.setProperty(TC_PROP_CREDENTIALS, tmpStr);
				placement.save();
			}
		}
	}
	
	public Set<SavedCredentials> getSavedCredentials() {
		Placement placement = ToolManager.getCurrentPlacement();
		Properties config = placement.getPlacementConfig();
		return getSavedCredentialsFromPlacementConfig(config);
	}
	
	public Set<SavedCredentials> getSavedCredentialsFromPlacementConfig(Properties config) {
		Set<SavedCredentials> savedCredentials = new HashSet<SavedCredentials>();
		if(config != null){
			String prop = config.getProperty(TC_PROP_CREDENTIALS);
			if(prop != null){
				String[] chsPair = prop.split(TC_PROP_LEVEL1_DELIMITER);
				for(int i=0; i<chsPair.length; i++){
					try{
						String[] fields = chsPair[i].split(TC_PROP_LEVEL2_DELIMITER);
						if(fields[0] != null && !fields[0].trim().equals("")) {
							SavedCredentials crd = new SavedCredentialsImpl();
							crd.setUrl(new URL(fields[0]));
							crd.setRealm(fields[1]);
							crd.setUsername(fields[2]);
							String password = decryptData(fields[3]);
							crd.setPassword(password);
							if(fields.length >= 5) {
								crd.setScheme(fields[4]);
							}
							if(crd.getUrl() != null && password != null){
								savedCredentials.add(crd);
							}
						}
					}catch(MalformedURLException e){
						LOG.warn("Saved credentials contains an invalid URL.", e);
					}catch(Exception e){
						LOG.warn("Invalid saved credentials.", e);
					}
				}
			}
		}		
		return savedCredentials;
	}
	
	public SavedCredentials newSavedCredentials(URL url, String realm, String username, String password, String scheme) {
		return new SavedCredentialsImpl(url, realm, username, password, scheme);
	}

	public Set<FeedSubscription> getInstitutionalFeeds() {
		Set<FeedSubscription> userInstitutionalFeeds = new LinkedHashSet<FeedSubscription>();
		
		if(institutionalFeeds == null){
			institutionalFeeds = new LinkedHashSet<FeedSubscription>();

			// add from sakai.properties
			String[] configured = m_serverConfigurationService.getStrings(SAK_PROP_INSTSUBSCRIPTIONS);
			if(configured != null) {
				for(int i=0; i<configured.length; i++) {
					try{
						FeedSubscription subs = getFeedSubscriptionFromFeedUrl(configured[i], true);
						LOG.info("Institutional Feed: "+subs.getTitle());
						institutionalFeeds.add(subs);
					}catch(Exception e){
						LOG.info("Unable to read Institutional Feed: "+configured[i], e);
					}
				}
			}
		}
		userInstitutionalFeeds.addAll(institutionalFeeds);

		// merge additional
		if(m_institutionalFeedProvider != null) {
			Site currentSite = null;
			try{
				currentSite = m_siteService.getSite(ToolManager.getCurrentPlacement().getContext());
			}catch(IdUnusedException e1){
				// site is null
			}
			User currentUser = m_userDirectoryService.getCurrentUser();
			Set<String> additional = m_institutionalFeedProvider.getAdditionalInstitutionalFeeds(currentSite, currentUser);
			if(additional != null){
				for(String feedUrl : additional){
					try{
						FeedSubscription subs = getFeedSubscriptionFromFeedUrl(feedUrl, true);
						LOG.info("Additional Institutional Feed: " + subs.getTitle());
						userInstitutionalFeeds.add(subs);
					}catch(Exception e){
						LOG.info("Unable to read Additional Institutional Feed: "+feedUrl, e);
					}
				}
			}
		}
		
		return userInstitutionalFeeds;
	}

	public void addCredentials(URL url, String realm, String username, String password, String scheme) {
		Credentials credentials = new UsernamePasswordRealmSchemeCredentials(username, password, realm, scheme);
		feedFetcherAuth.addCredentials(url, credentials);
	}
	
	public void loadCredentials() {
		Set<SavedCredentials> saved = getSavedCredentials();
		for(SavedCredentials crd : saved){
			Credentials credentials = new UsernamePasswordRealmSchemeCredentials(crd.getUsername(), crd.getPassword(), crd.getRealm(), crd.getScheme());
			feedFetcherAuth.addCredentials(crd.getUrl(), credentials);
		}
	}

	public Feed getFeed(EntityReference reference, boolean forceExternalCheck) throws IllegalArgumentException, MalformedURLException, IOException, InvalidFeedException, FetcherException, FeedAuthenticationException {
		Feed feed = null;
		if(reference instanceof IdEntityReference){
			IdEntityReference idEntityRef = (IdEntityReference) reference;
			if(InternalFeedEntityProvider.ENTITY_PREFIX.equals(idEntityRef.prefix)){
				if(m_internalFeedStorageProvider != null)
					return m_internalFeedStorageProvider.getFeed(reference);
				else
					return null;
			}else if(ExternalFeedEntityProvider.ENTITY_PREFIX.equals(idEntityRef.prefix)){
				String feedUrl = decodeUri(idEntityRef.id).replaceAll("feed://", "http://");
				URL url = new URL(feedUrl);
				SyndFeed syndFeed;
				try{
					syndFeed = feedFetcherAuth.retrieveFeed(url, forceExternalCheck);
				}catch(FeedException e){
					throw new InvalidFeedException(e);
				}catch(com.sun.syndication.fetcher.FetcherException e){
					if(/*e.getResponseCode() == 403 || */e.getResponseCode() == 401){
						FeedAuthenticationException ex = new FeedAuthenticationException(e);
						ex.setResponseCode(e.getResponseCode());
						if(e instanceof InnerFetcherException) {
							ex.setRealm(((InnerFetcherException)e).getAuthState().getRealm());
							ex.setScheme(((InnerFetcherException)e).getAuthState().getAuthScheme().getSchemeName());
						}
						throw ex;
					}else{
						FetcherException ex = new FetcherException(e);
						ex.setHttpCode(e.getResponseCode());
						throw ex;
					}
				}
				//LOG.info(syndFeed.toString());
				
				// feed
				feed = new FeedImpl(idEntityRef.toString(), feedUrl);
				feed.setTitle(syndFeed.getTitle());
				feed.setDescription(syndFeed.getDescription());
				feed.setPublishedDate(syndFeed.getPublishedDate());
				feed.setCopyright(syndFeed.getCopyright());
				feed.setLanguage(syndFeed.getLanguage());
				feed.setLink(syndFeed.getLink());
				feed.setFeedType(syndFeed.getFeedType());
				feed.setFeedEncoding(syndFeed.getEncoding());
				SyndImage syndImage = syndFeed.getImage();
				if(syndImage != null) {
					feed.setImageLink(syndImage.getLink());
					feed.setImageTitle(syndImage.getTitle());
					feed.setImageDescription(syndImage.getDescription());
					feed.setImageUrl(syndImage.getUrl());
				}
				// feed entries
				List<SyndEntry> syndEntries = syndFeed.getEntries();
				if(syndEntries != null) {
					List<FeedEntry> feedEntries = new ArrayList<FeedEntry>();
					for(SyndEntry syndEntry : syndEntries) {
						FeedEntry entry = new FeedEntryImpl();
						entry.setTitle(syndEntry.getTitle());
						entry.setDescription(syndEntry.getDescription() != null? syndEntry.getDescription().getValue() : null);
						List<SyndContent> content = syndEntry.getContents();
						if(content != null) {
							StringBuilder buff = new StringBuilder();
							for(int i=0; i<content.size(); i++){
								SyndContent sc = content.get(i);
								buff.append(sc.getValue());
								if(i != content.size() - 1)
									buff.append("<br/>");
							}
							entry.setContent(buff.toString());
						}
						entry.setLink(syndEntry.getLink());
						entry.setPublishedDate(syndEntry.getPublishedDate());
						// feed entry enclosures
						List<SyndEnclosure> syndEnclosures = syndEntry.getEnclosures();
						if(syndEnclosures != null) {
							List<FeedEntryEnclosure> feedEntrieEnclosures = new ArrayList<FeedEntryEnclosure>();
							for(SyndEnclosure syndEnclosure : syndEnclosures) {
								FeedEntryEnclosure enclosure = new FeedEntryEnclosureImpl();
								enclosure.setUrl(syndEnclosure.getUrl());
								enclosure.setType(syndEnclosure.getType());
								enclosure.setLength(syndEnclosure.getLength());
								feedEntrieEnclosures.add(enclosure);
							}
							entry.setEnclosures(feedEntrieEnclosures);
						}
						feedEntries.add(entry);
					}
					feed.setEntries(feedEntries);
				}
				
			}
		}else LOG.error("EntityReference " + reference + " is not a Feed.");
		return feed;
	}
	
	public String getFeedXml(EntityReference reference) throws IllegalArgumentException, MalformedURLException, IOException, InvalidFeedException, FetcherException {
		String feedXml = "";
		if(reference instanceof IdEntityReference){
			IdEntityReference idEntityRef = (IdEntityReference) reference;
			if(InternalFeedEntityProvider.ENTITY_PREFIX.equals(idEntityRef.prefix)){
				if(m_internalFeedStorageProvider != null)
					return m_internalFeedStorageProvider.getFeedXml(reference);
				else
					return null;
			}else if(ExternalFeedEntityProvider.ENTITY_PREFIX.equals(idEntityRef.prefix)){
				String feedUrl = decodeUri(idEntityRef.id).replaceAll("feed://", "http://");
				URL url = new URL(feedUrl);
				try{
					SyndFeed syndFeed = feedFetcherAuth.retrieveFeed(url, false);
					
					SyndFeedOutput output = new SyndFeedOutput();
					feedXml = output.outputString(syndFeed);
				}catch(FeedException e){
					throw new InvalidFeedException(e);
				}catch(com.sun.syndication.fetcher.FetcherException e){
					throw new FetcherException(e);
				}
			}
		}
		return feedXml;
	}
	
	public void cacheFeed(String feedUrl, boolean forceExternalCheck, Observer observer) {
		FeedCacheTask task = new FeedCacheTask
				(
				feedUrl, 
				forceExternalCheck,
				feedCredentialSupplier.getCredentialsMap(),
				/*getSavedCredentials(),*/
				getClientCookies(),
				m_sessionManager.getCurrentSessionUserId(),
				observer
				);
		feedCacheThreadsExecutor.execute(task);		
	}

	public boolean saveFeed(Feed feed) {
		//if(InternalFeedEntityProvider.ENTITY_PREFIX.equals(m_entityBroker.parseReference(feed.getReference()).prefix)){
		if(InternalFeedEntityProvider.ENTITY_PREFIX.equals(new IdEntityReference(feed.getReference()).prefix)){
			if(m_internalFeedStorageProvider != null)
				return m_internalFeedStorageProvider.saveFeed(feed);
			else
				LOG.error("FeedService.saveFeed(): internal feed " + feed.getFeedUrl() + " cannot be saved! There is no InternalFeedStorageProvider.");
		}else{
			LOG.warn("FeedService.saveFeed(): external feed " + feed.getFeedUrl() + " cannot be saved!");
		}
		return false;
	}
	
	public FeedSubscription getFeedSubscriptionFromFeedUrl(String feedUrl, boolean getOnlineInfo) throws IllegalArgumentException, MalformedURLException, IOException, InvalidFeedException, FetcherException, FeedAuthenticationException {
		EntityReference reference = getEntityReference(feedUrl);
		Feed feed = getOnlineInfo? getFeed(reference, false) : null;
		FeedSubscription subscription = new FeedSubscriptionImpl();
		if(feed != null) {
			subscription.setUrl(feedUrl);
			subscription.setTitle(feed.getTitle());
			subscription.setDescription(feed.getDescription());
			subscription.setIconUrl((feed.getImageUrl() == null || feed.getImageUrl().trim().equals(""))? defaultFeedIcon : feed.getImageUrl());			
		}else{
			subscription.setUrl(feedUrl);
			try{
				URL url = new URL(feedUrl);
				subscription.setTitle(url.getHost());
			}catch(Exception e){
				subscription.setTitle(feedUrl);
			}
			subscription.setDescription("");
			subscription.setIconUrl(defaultFeedIcon);
		}
		return subscription;
	}
	
	private String encodeUri(String uri) {
		// use Base64
		byte[] encoded = CommonsCodecBase64.encodeBase64(uri.getBytes());
		// '/' cannot be used in Reference => use ':' instead (not part of
		// Base64 alphabet)
		String encStr = new String(encoded).replaceAll("/", ":");
		return encStr;
	}

	private String decodeUri(String id) {
		// use Base64
		byte[] decoded = CommonsCodecBase64.decodeBase64(
				id
				.replaceAll(":", "/")
				.replaceAll(",", "/")   // backwards compatible
				.replaceAll("\\.", "/") // backwards compatible
				.getBytes()
				);
		return new String(decoded);
	}

	protected String getUniqueId() {
		return m_idManager.createUuid();
	}
	
	public boolean allowEditPermissions() {
		try{
			// do not require subscribe permission when tool is on My Workspace
			String siteId = ToolManager.getCurrentPlacement().getContext();
			if(m_siteService.isUserSite(siteId))
				return true;
			else
				return SecurityService.unlock(SiteService.SECURE_UPDATE_SITE, m_siteService.siteReference(siteId));
		}catch(Exception e){
			LOG.warn("allowEditPermissions()", e);
			return false;
		}
	}

	public boolean allowSubscribeFeeds() {
		try{
			// do not require subscribe permission when tool is on My Workspace
			String siteId = ToolManager.getCurrentPlacement().getContext();
			if(m_siteService.isUserSite(siteId))
				return true;
			else
				return SecurityService.unlock(AUTH_SUBSCRIBE, m_siteService.siteReference(siteId));
		}catch(Exception e){
			LOG.warn("allowSubscribeFeeds()", e);
			return false;
		}
	}

	public boolean allowCreateNewFeed() {
		try{
			return SecurityService.unlock(AUTH_NEW, m_siteService.siteReference(ToolManager.getCurrentPlacement().getContext()));
		}catch(Exception e){
			LOG.warn("allowCreateNewFeed()", e);
			return false;
		}
	}

	public boolean allowAddFeedEntry(Feed feed) {
		try{
			return SecurityService.unlock(AUTH_ADD, m_siteService.siteReference(ToolManager.getCurrentPlacement().getContext()));
		}catch(Exception e){
			LOG.warn("allowAddFeedEntry()", e);
			return false;
		}
	}

	public boolean allowEditFeedEntry(Feed feed) {
		try{
			return SecurityService.unlock(AUTH_EDIT, m_siteService.siteReference(ToolManager.getCurrentPlacement().getContext()));
		}catch(Exception e){
			LOG.warn("allowEditFeedEntry()", e);
			return false;
		}
	}

	public boolean allowDeleteFeedEntry(Feed feed) {
		try{
			return SecurityService.unlock(AUTH_DELETE, m_siteService.siteReference(ToolManager.getCurrentPlacement().getContext()));
		}catch(Exception e){
			LOG.warn("allowDeleteFeedEntry()", e);
			return false;
		}
	}

	// ######################################################
	// EntityProvider helper methods
	// ######################################################
	private String getInternalFeedUrlPrefix() {
		if(m_internalFeedStorageProvider != null) {
			String internalFeedUrlPrefix = m_internalFeedStorageProvider.getInternalFeedUrlPrefix();
			if(internalFeedUrlPrefix != null)
				return internalFeedUrlPrefix;
		}
		return m_serverConfigurationService.getServerUrl() + "/direct";
	}
	
	private String getInternalFeedUrl(EntityReference reference) {
		return getInternalFeedUrlPrefix() + reference.toString();
	}
	
	private boolean isInternalFeed(String feedUrl) {
		String internalFeedUrlPrefix = getInternalFeedUrlPrefix();
		if(feedUrl.startsWith(internalFeedUrlPrefix)){
			return true;
		}else{
			return false;
		}
	}
	
	public EntityReference getEntityReference(String feedUrl) {
		String reference = null;
		if(!isInternalFeed(feedUrl)){
			int index = feedUrl.indexOf(ExternalFeedEntityProvider.ENTITY_PREFIX);
			if(index >= 0)
				reference = feedUrl.substring(feedUrl.indexOf(ExternalFeedEntityProvider.ENTITY_PREFIX) - 1);
			else
				reference = EntityReference.SEPARATOR + ExternalFeedEntityProvider.ENTITY_PREFIX + EntityReference.SEPARATOR + encodeUri(feedUrl);
		}else
			reference = feedUrl.substring(feedUrl.indexOf(InternalFeedEntityProvider.ENTITY_PREFIX) - 1);
		//EntityReference ref = m_entityBroker.parseReference(reference);
		EntityReference ref = new IdEntityReference(reference);
		LOG.debug("getEntityReference("+feedUrl+"): "+reference+", "+ref);
		return ref;
	}
	
	public EntityReference buildEntityReference(String prefix, String uri) {
		//EntityReference entityReference = m_entityBroker.parseReference(EntityReference.SEPARATOR + prefix + EntityReference.SEPARATOR + encodeUri(uri));
		EntityReference entityReference = new IdEntityReference(prefix, encodeUri(uri));
		LOG.debug("buildEntityReference("+prefix+", "+uri+"): "+entityReference);
		return entityReference;
	}

	public boolean entityExists(String entityPrefix, String id) {
		LOG.debug("FeedsService.entityExists(): " + entityPrefix + ", " + id);
		if(entityPrefix == null || id == null || id.trim().equals(""))
			return false;
		if(InternalFeedEntityProvider.ENTITY_PREFIX.equals(entityPrefix)){
			// TODO Implement entityExists for Internal
			return false;
		}else if(ExternalFeedEntityProvider.ENTITY_PREFIX.equals(entityPrefix)){
			URL url = null;
			try{
				String feedUri = decodeUri(id).replaceFirst("feed://", "http://");
				url = new URL(feedUri);
				feedFetcherAuth.retrieveFeed(url, false);
				return true;
			}catch(RuntimeException e){
				return false;
			}catch(Exception e){
				return false;
			}
		}
		return false;
	}
	
	public void clearClientCokies() {
		Session session = m_sessionManager.getCurrentSession();
		Object o = session.getAttribute(FeedsService.SESSION_ATTR_HTTPSTATE);
		HttpState httpState = null;
		if(o == null){
			httpState = new HttpState();
		}else if(o != null && o instanceof HttpState){
			httpState = (HttpState) o;
		}
	
		if(httpState != null) {
			httpState.clearCookies();
			session.setAttribute(FeedsService.SESSION_ATTR_HTTPSTATE, httpState);
		}
	}
	
	public void addClientCookie(String domain, String name, String value, String path, int maxAge, boolean secure) {
		Session session = m_sessionManager.getCurrentSession();
		Object o = session.getAttribute(FeedsService.SESSION_ATTR_HTTPSTATE);
		HttpState httpState = null;
		if(o == null){
			httpState = new HttpState();
		}else if(o != null && o instanceof HttpState){
			httpState = (HttpState) o;
		}
	
		if(httpState != null) {
			Cookie cookie = new Cookie(domain, name, value, path, maxAge, secure);
			httpState.addCookie(cookie);
			session.setAttribute(FeedsService.SESSION_ATTR_HTTPSTATE, httpState);
		}
	}
	
	private Cookie[] getClientCookies() {
		Session session = m_sessionManager.getCurrentSession();
		Object o = session.getAttribute(FeedsService.SESSION_ATTR_HTTPSTATE);
		HttpState httpState = null;
		if(o == null){
			httpState = new HttpState();
		}else if(o != null && o instanceof HttpState){
			httpState = (HttpState) o;
		}
	
		return httpState != null? httpState.getCookies() : null;
	}
	
	private void addClientCookies(Cookie[] cookies, Session toSession) {
		Object o = toSession.getAttribute(FeedsService.SESSION_ATTR_HTTPSTATE);
		HttpState httpState = null;
		if(o == null){
			httpState = new HttpState();
		}else if(o != null && o instanceof HttpState){
			httpState = (HttpState) o;
		}
		
		httpState.addCookies(cookies);
		toSession.setAttribute(FeedsService.SESSION_ATTR_HTTPSTATE, httpState);
	}

	public void logEvent(String event, FeedSubscription feedSubscription, boolean modify) {
		StringBuilder ref = new StringBuilder();
		ref.append("/feeds/");
		ref.append(ToolManager.getCurrentPlacement().getContext());
		if(!LOG_EVENT_READ.equals(event)) {
			ref.append('/');
			ref.append(feedSubscription.getUrl());
		}
		Event e = m_eventTrackingService.newEvent(event, ref.toString(), modify);
		m_eventTrackingService.post(e);
	}

	class FeedCredentialSupplier implements CredentialSupplier {
		
		public FeedCredentialSupplier() {
		}
		
		public void addCredentials(URL url, Credentials credentials) {
			Map<URL,Credentials> credentialsMap = getCredentialsMap();
			credentialsMap.put(url, credentials);
			setCredentialsMap(credentialsMap);
		}

		public Credentials getCredentials(URL url) {
			Credentials credentials = getCredentialsMap().get(url);
			return credentials;
		}
		
		public void clearCredentialMap() {
			m_sessionManager.getCurrentSession().removeAttribute(SESSION_ATTR_CREDENTIALS);
		}
		
		protected Map<URL,Credentials> getCredentialsMap() {
			Object o = m_sessionManager.getCurrentSession().getAttribute(SESSION_ATTR_CREDENTIALS);
			if(o != null && o instanceof Map<?,?>){
				Map<URL,Credentials> credentialsMap = (Map<URL,Credentials>) o;
				return credentialsMap;
			}else{
				Map<URL,Credentials> credentialsMap = new HashMap<URL,Credentials>();
				m_sessionManager.getCurrentSession().setAttribute(SESSION_ATTR_CREDENTIALS, credentialsMap);
				return credentialsMap;
			}
		}
		
		protected void setCredentialMap(Map<URL,Credentials> credentialsMap, Session toSession) {
			if(toSession != null) {
				toSession.setAttribute(SESSION_ATTR_CREDENTIALS, credentialsMap);
			} else
				LOG.warn("copyCredentialsToSession: toSession is null");
		}
		
		private void setCredentialsMap(Map<URL,Credentials> credentialsMap) {
			m_sessionManager.getCurrentSession().setAttribute(SESSION_ATTR_CREDENTIALS, credentialsMap);			
		}
	}	
	
	class FeedCacheTask extends Observable implements Runnable {
		private Observer observer;
		private String feedUrl;
		private boolean forceExternalCheck;
		private Cookie[] cookies;
		private String userId;
		private Map<URL,Credentials> credentialsMap;
		
		public FeedCacheTask(
				String feedUrl, 
				boolean forceExternalCheck, 
				Observer observer) {
			this(feedUrl, forceExternalCheck, null, null, null, observer);
		}

		public FeedCacheTask(
				String feedUrl, 
				boolean forceExternalCheck, 
				Map<URL,Credentials> credentialsMap,
				Cookie[] cookies,
				String userId,
				Observer observer) {
			this.feedUrl = feedUrl;
			this.forceExternalCheck = forceExternalCheck;
			this.credentialsMap = credentialsMap != null? credentialsMap : feedCredentialSupplier.getCredentialsMap();
			this.cookies = cookies != null? cookies : getClientCookies();
			this.userId = userId != null? userId : m_sessionManager.getCurrentSessionUserId();
			this.observer = observer;
		}

		public void run() {
			// This runs in a different thread/session::
			Session thisSession = m_sessionManager.getCurrentSession();
			//  : load saved credentials
			feedCredentialSupplier.setCredentialMap(getCredentialsMap(), thisSession);

			//  : load client cookies
			addClientCookies(getCookies(), thisSession);
			//  : store userId
			thisSession.setAttribute(FeedsService.SESSION_ATTR_FEED_USER_ID, getUserId());
			
			// cache feed
			int activeTasksCount = feedCacheThreadsExecutor.getActiveCount();
			int poolSize = feedCacheThreadsExecutor.getPoolSize();
			LOG.debug("["+activeTasksCount+"/"+poolSize+"] Caching feed: "+getFeedUrl());
			try {
				EntityReference reference = getEntityReference(getFeedUrl());
				getFeed(reference, isForceExternalCheck());
				LOG.debug("["+activeTasksCount+"/"+poolSize+"] Feed is CACHED: "+getFeedUrl());
			} catch (FetcherException e) {								
				// exception will be handled by content panel in UI
				LOG.debug("["+activeTasksCount+"/"+poolSize+"] Feed NOT CACHED: "+getFeedUrl(), e);
			} catch (RuntimeException e) {								
				// exception will be handled by content panel in UI
				LOG.debug("["+activeTasksCount+"/"+poolSize+"] Feed NOT CACHED: "+getFeedUrl(), e);
			} catch (Exception e) {								
				// exception will be handled by content panel in UI
				LOG.debug("["+activeTasksCount+"/"+poolSize+"] Feed NOT CACHED: "+getFeedUrl(), e);
			}
			Observer observer = getObserver();
			if(observer != null) {
				observer.update(this, null);
			}
		}

		public String getFeedUrl() {
			return feedUrl;
		}

		public boolean isForceExternalCheck() {
			return forceExternalCheck;
		}
		
		public Map<URL, Credentials> getCredentialsMap() {
			return credentialsMap;
		}

		public Cookie[] getCookies() {
			return cookies;
		}

		public String getUserId() {
			return userId;
		}

		public Observer getObserver() {
			return observer;
		}		
	}
}
