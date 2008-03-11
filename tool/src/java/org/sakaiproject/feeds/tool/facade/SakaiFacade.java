package org.sakaiproject.feeds.tool.facade;

import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.feeds.api.FeedsService;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;


public interface SakaiFacade {

	public FeedsService getFeedsService();

	public SessionManager getSessionManager();
	
	public AuthzGroupService getAuthzGroupService();
	
	public SiteService getSiteService();
	
	public ToolManager getToolManager();
}
