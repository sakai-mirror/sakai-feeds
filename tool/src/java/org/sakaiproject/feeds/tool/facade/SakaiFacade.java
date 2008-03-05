package org.sakaiproject.feeds.tool.facade;

import org.sakaiproject.feeds.api.FeedsService;
import org.sakaiproject.tool.api.SessionManager;


public interface SakaiFacade {

	public FeedsService getFeedsService();

	public SessionManager getSessionManager();
}
