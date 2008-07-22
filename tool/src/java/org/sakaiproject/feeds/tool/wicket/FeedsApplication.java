package org.sakaiproject.feeds.tool.wicket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.protocol.http.HttpSessionStore;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.session.ISessionStore;
import org.apache.wicket.settings.IExceptionSettings;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.feeds.tool.facade.SakaiFacade;
import org.sakaiproject.feeds.tool.wicket.pages.MainPage;

public class FeedsApplication extends WebApplication {
	private static Log LOG = LogFactory.getLog(FeedsApplication.class);

	private SakaiFacade facade;
	
	protected void init() {
		super.init();
		
		// Configure general wicket application settings
		addComponentInstantiationListener(new SpringComponentInjector(this));
		getResourceSettings().setThrowExceptionOnMissingResource(true);
		getMarkupSettings().setStripWicketTags(true);
		getDebugSettings().setAjaxDebugModeEnabled(ServerConfigurationService.getBoolean("feeds.ajaxDebugEnabled", false));
		
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
		// SecondLevelCacheSessionStore causes problems with Ajax requests;
		// => use HttpSessionStore instead.
		return new HttpSessionStore(this);
	}
}

