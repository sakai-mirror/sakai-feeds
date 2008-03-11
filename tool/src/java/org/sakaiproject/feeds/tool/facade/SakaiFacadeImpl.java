package org.sakaiproject.feeds.tool.facade;

import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.feeds.api.FeedsService;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;


public class SakaiFacadeImpl implements SakaiFacade {
	private FeedsService		feedsService;
	private SessionManager		sessionManager;
	private AuthzGroupService	authzGroupService;
	private SiteService			siteService;
	private ToolManager			toolManager;

	public void setFeedsService(FeedsService feedsService) {
		this.feedsService = feedsService;
	}

	public FeedsService getFeedsService() {
		// return feedsService();
		return feedsService;
	}

	public void setSessionManager(SessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}

	public SessionManager getSessionManager() {
		return sessionManager;
	}

	public AuthzGroupService getAuthzGroupService() {
		return authzGroupService;
	}

	public void setAuthzGroupService(AuthzGroupService authzGroupService) {
		this.authzGroupService = authzGroupService;
	}

	public SiteService getSiteService() {
		return siteService;
	}

	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}

	public ToolManager getToolManager() {
		return toolManager;
	}

	public void setToolManager(ToolManager toolManager) {
		this.toolManager = toolManager;
	}

}
