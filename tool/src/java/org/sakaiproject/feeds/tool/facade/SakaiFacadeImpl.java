package org.sakaiproject.feeds.tool.facade;

import org.sakaiproject.feeds.api.FeedsService;
import org.sakaiproject.tool.api.SessionManager;


public class SakaiFacadeImpl implements SakaiFacade {
	private FeedsService	feedsService;
	private SessionManager	sessionManager;

	/*
	 * protected FeedsService feedsService() { return null; }
	 */

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
}
