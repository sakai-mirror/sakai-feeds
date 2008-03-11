package org.sakaiproject.feeds.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.cover.AuthzGroupService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.db.cover.SqlService;
import org.sakaiproject.feeds.api.FeedSubscription;
import org.sakaiproject.feeds.api.FeedsService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.user.cover.UserDirectoryService;


public class ToolMigration {
	private static Log			LOG					= LogFactory.getLog(ToolMigration.class);
	private static final String	TOOLID_NEWS			= "sakai.news";
	private static final String	TOOLID_NEWSFEEDS	= FeedsService.TOOL_ID;
	
	private static FeedsService	m_feedsService		= (FeedsService) ComponentManager.get(FeedsService.class);

	public static int convertFromOldNewsTool(boolean getOnlineInfo, boolean alwaysCreateTool, String defaultFeedUrl) {
		int siteCount = 0;
		Connection c = null;

		// establish an admin session
		Session sakaiSession = SessionManager.getCurrentSession();
		String currentUserId = sakaiSession.getUserId();
		String currentUserEid = sakaiSession.getUserEid();
		sakaiSession.setUserId(UserDirectoryService.ADMIN_ID);
		sakaiSession.setUserEid(UserDirectoryService.ADMIN_EID);
		AuthzGroupService.refreshUser(UserDirectoryService.ADMIN_ID);
		
		try{
			c = SqlService.borrowConnection();
			PreparedStatement ps = c.prepareStatement("select distinct(ST.SITE_ID) " +
					"from SAKAI_SITE_TOOL ST " +
					"where REGISTRATION = ?");
			ps.setString(1, TOOLID_NEWS);
			ResultSet rs = ps.executeQuery();
			while (rs.next()){
				String siteId = rs.getString("ST.SITE_ID");
				Site site = getSite(siteId, false);
				if(site == null)
					continue;
				
				boolean createTool = alwaysCreateTool;
				ToolConfiguration toolConfig = null;
				// get existing page for 'News Feeds' tool
				if(!alwaysCreateTool) {
					Set<ToolConfiguration> tcs = getToolsForCommonId(site, TOOLID_NEWSFEEDS);
					createTool = tcs.size() == 0;
					if(!createTool)
						toolConfig = (ToolConfiguration) tcs.toArray()[0];
				}				
				// add page for 'News Feeds' tool
				if(createTool) {
					SitePage page = site.addPage();
					toolConfig = page.addTool(TOOLID_NEWSFEEDS);
					page.setTitle("News Feeds");
					//Tool NOT registered yet: page.setTitle(ToolManager.getTool(TOOLID_NEWSFEEDS).getTitle());
					SiteService.save(site);
				}
				
				site = getSite(siteId, true);
				if(site == null)
					continue;
				
				// iterate over all old 'News' tools present in site
				Set<ToolConfiguration> tcs = getToolsForCommonId(site, TOOLID_NEWS);
				Set<FeedSubscription> subscriptions = new HashSet<FeedSubscription>();
				for(ToolConfiguration tc : tcs) {
					try{
						
						String feedUrl = tc.getConfig().getProperty("channel-url");
						if(feedUrl == null){
							try{
								feedUrl = tc.getTool().getFinalConfig().getProperty("channel-url");
							}catch(Exception e){
								LOG.info("Tool was configured with default url but 'sakai.news' has not been registered yet. Using configured default url: "+defaultFeedUrl);
								feedUrl = defaultFeedUrl;
							}
						}
						if(feedUrl == null){
							LOG.info("Skipping tool in site with id '"+siteId+"' because 'channel-url' property is empty.");
							continue;
						}
						// get feed url
						FeedSubscription fs = m_feedsService.getFeedSubscriptionFromFeedUrl(feedUrl, getOnlineInfo);
						subscriptions.add(fs);
						// remove tool
						SitePage parentPage = tc.getContainingPage();
						LOG.info("Tool is now converted: removing "+tc.getTitle());
						parentPage.removeTool(tc);
						if(parentPage.getTools().size() == 0){
							// remove page if no more tools inside page
							site.removePage(parentPage);
							LOG.info("SitePage is now empty: removing "+parentPage.getTitle());
						}
					}catch(Exception e) {
						LOG.info("Skipping tool in site with id '"+siteId+"'. Cause: "+e.getMessage(), e);
					}
				}
				SiteService.save(site);
			
				// save feed subscriptions in new tool
				addSubscribedFeeds(toolConfig, subscriptions);
				
				// add permission?
				// -- NOT NEEDED, There is a Permissions page now
				//if(addPermission) {
				//	AuthzGroup authz = AuthzGroupService.getAuthzGroup(site.getReference());
				//	authz.getRole(site.getMaintainRole()).allowFunction(FeedsService.AUTH_SUBSCRIBE);
				//	AuthzGroupService.save(authz);
				//}
				
				// increment site counter
				siteCount++;
				LOG.info("Site "+site.getTitle()+" ("+site.getId()+"): added one '"+TOOLID_NEWSFEEDS+"' tool with "+subscriptions.size()+" subscriptions (old tool instances removed).");
			}
			rs.close();
			ps.close();
		}catch(Exception e){
			LOG.error("Unable to retrieve list of placed tools with id 'sakai.news'. Conversion aborted.", e);
		}finally{
			if(c != null) SqlService.returnConnection(c);
			// restore previous session
			sakaiSession.setUserId(currentUserId);
			sakaiSession.setUserEid(currentUserEid);
			AuthzGroupService.refreshUser(currentUserId);
		}
		return siteCount;
	}
	
	private static Set<ToolConfiguration> getToolsForCommonId(Site site, String commonToolId) {
		Set<ToolConfiguration> tcs = new HashSet<ToolConfiguration>();		
		List<SitePage> pages = site.getPages();
		for(SitePage page : pages) {
			List<ToolConfiguration> toolCfgs = page.getTools();
			for(ToolConfiguration tc : toolCfgs) {
				if(tc.getToolId().equals(commonToolId))
					tcs.add(tc);
			}
		}
		return tcs;
	}

	private static Site getSite(String siteId, boolean loadAll) {
		try{
			Site site = SiteService.getSite(siteId);
			if(loadAll)
				site.loadAll();
			return site;
		}catch(Exception e){
			LOG.info("Skipping site with id '"+siteId+"'. Cause: "+e.getMessage());
			return null;
		}
	}

	private static void addSubscribedFeeds(ToolConfiguration toolConfig, Set<FeedSubscription> subscriptions) {
		subscriptions.addAll(getSubscribedFeeds(toolConfig));
		
		Properties config = toolConfig.getPlacementConfig();
		if(config != null){
			boolean first = true;
			String tmpStr = "";
			for(FeedSubscription subscription : subscriptions){
				if(!first) tmpStr += FeedsService.TC_PROP_LEVEL1_DELIMITER;
				first = false;

				tmpStr += subscription.getTitle();
				tmpStr += FeedsService.TC_PROP_LEVEL2_DELIMITER;
				tmpStr += subscription.getDescription();
				tmpStr += FeedsService.TC_PROP_LEVEL2_DELIMITER;
				tmpStr += subscription.getUrl();
				tmpStr += FeedsService.TC_PROP_LEVEL2_DELIMITER;
				tmpStr += subscription.getIconUrl();
			}

			config.setProperty(FeedsService.TC_PROP_SUBCRIPTIONS, tmpStr);
			toolConfig.save();
		}
	}
	
	public static Set<FeedSubscription> getSubscribedFeeds(ToolConfiguration toolConfig) {
		Set<FeedSubscription> subscriptions = new LinkedHashSet<FeedSubscription>();

		Properties config = toolConfig.getPlacementConfig();
		if(config != null){
			String prop = config.getProperty(FeedsService.TC_PROP_SUBCRIPTIONS);
			if(prop != null){
				String[] chsPair = prop.split(FeedsService.TC_PROP_LEVEL1_DELIMITER);
				for(int i=0; i<chsPair.length; i++){
					String[] fields = chsPair[i].split(FeedsService.TC_PROP_LEVEL2_DELIMITER);
					FeedSubscription subscription = new FeedSubscriptionImpl();
					subscription.setTitle(fields[0]);
					subscription.setDescription(fields[1]);
					subscription.setUrl(fields[2]);
					subscription.setIconUrl(fields[3]);
					subscriptions.add(subscription);
				}
			}
		}
		
		return subscriptions;
	}
}
