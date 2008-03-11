package org.sakaiproject.feeds.tool.wicket;

import org.apache.wicket.protocol.http.HttpSessionStore;
import org.apache.wicket.session.ISessionStore;
import org.apache.wicket.settings.IExceptionSettings;
import org.sakaiproject.feeds.tool.facade.SakaiFacade;
import org.sakaiproject.feeds.tool.wicket.pages.MainPage;
import org.sakaiproject.wicket.protocol.http.SakaiWebApplication;

public class FeedsApplication extends SakaiWebApplication {
	//private static Log LOG = LogFactory.getLog(FeedsApplication.class);

	private SakaiFacade facade;
	
	protected void init() {
		super.init();
		
		// Home page
		mountBookmarkablePage("/home", MainPage.class);
		
		// On wicket session timeout or wicket exception, redirect to main page
		getApplicationSettings().setPageExpiredErrorPage(MainPage.class);
		getApplicationSettings().setInternalErrorPage(MainPage.class);
		
		// show internal error page rather than default developer page
		getExceptionSettings().setUnexpectedExceptionDisplay(IExceptionSettings.SHOW_INTERNAL_ERROR_PAGE);
	}
	
	@SuppressWarnings("unchecked")
	public Class getHomePage() {
		return MainPage.class;
	}

	public SakaiFacade getFacade() {
		return facade;
	}

	public void setFacade(SakaiFacade facade) {
		this.facade = facade;
	}

	@Override
	protected ISessionStore newSessionStore() {
		return new HttpSessionStore(this);
	}
}

