package org.sakaiproject.feeds.tool.wicket.pages;

import javax.servlet.http.Cookie;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.protocol.http.WebRequest;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.feeds.tool.facade.Locator;
import org.sakaiproject.feeds.tool.wicket.panels.FeedsPanel;
import org.sakaiproject.feeds.tool.wicket.panels.Menu;
import org.sakaiproject.tool.api.ToolSession;


public class MainPage extends BasePage {
	private static final long serialVersionUID = 1L;
	public static final String FORCE_TOP_REFRESH = "sakai.vppa.top.refresh";
	
	public MainPage() {
		this(null);
	}
	
	public MainPage(PageParameters params) {
		setupClientCookies();
		
		Menu menu = new Menu("menu");	
		add(menu);
		
		Boolean forceExternalCheck = Boolean.FALSE;
		if(params != null)
			forceExternalCheck = params.getBoolean("forceExternalCheck");
		FeedsPanel feeds = new FeedsPanel("feeds", forceExternalCheck);
		add(feeds);
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		ToolSession session = Locator.getFacade().getSessionManager().getCurrentToolSession();
		if(session.getAttribute(FORCE_TOP_REFRESH) != null){
			session.removeAttribute(FORCE_TOP_REFRESH);
			response.renderJavascript("parent.location.reload()", null);
		}else{
			response.renderJavascriptReference("/library/js/jquery.js");
			response.renderJavascriptReference("/sakai-feeds-tool/js/common.js");
		}
		super.renderHead(response);
	}
	
	/** Get cookies for feed authentication on this same server */
	private void setupClientCookies() {	
		final Cookie[] cookies = ((WebRequest)getRequestCycle().getRequest()).getCookies();
		Locator.getFacade().getFeedsService().clearClientCokies();
		if(cookies != null) {
			for(int i=0; i<cookies.length; i++){
				String domain = cookies[i].getDomain();
				String path = cookies[i].getPath();
				if(domain == null)
					domain = ServerConfigurationService.getServerName();
				if(path == null)
					path = "/";
				Locator.getFacade().getFeedsService().addClientCookie(
					domain, cookies[i].getName(), 
					cookies[i].getValue(), path,
					cookies[i].getMaxAge(), cookies[i].getSecure());
			}
		}
	}
}
