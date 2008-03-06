package org.sakaiproject.feeds.tool.wicket.pages;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.wicket.markup.html.SakaiPortletWebPage;

public class BasePage extends SakaiPortletWebPage {
	private static final long	serialVersionUID	= 1L;
	
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		// This tool style (CSS) and Javascript
		response.renderCSSReference("/sakai-feeds-tool/css/style.css");	
		//response.renderJavascriptReference("/sakai-feeds-tool/js/common.js");
		//response.renderJavascriptReference("/library/js/jquery.js");
		
		// Set Sakai Locale
		ResourceLoader rl = new ResourceLoader();
		getSession().setLocale(rl.getLocale());
	}

	@Override
	protected void onBeforeRender() {
		// prevent wicket session expiration
		Session.findOrCreate();
		super.onBeforeRender();
	}
}
