package org.sakaiproject.feeds.tool.wicket.pages;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.util.ResourceLoader;

public class BasePage  extends WebPage implements IHeaderContributor {
	private static final long	serialVersionUID	= 1L;
	protected static final String HEADSCRIPTS = "/library/js/headscripts.js";
	protected static final String BODY_ONLOAD_ADDTL="setMainFrameHeight( window.name )";
	
	public BasePage() {
		// Set Sakai Locale
		ResourceLoader rl = new ResourceLoader();
		getSession().setLocale(rl.getLocale());
	}
	
	public void renderHead(IHeaderResponse response) {
		// compute sakai skin
		String skinRepo = ServerConfigurationService.getString("skin.repo");
		response.renderCSSReference(skinRepo + "/tool_base.css");
		response.renderCSSReference(getToolSkinCSS(skinRepo));;
		
		// include sakai headscripts and resize iframe on load
		response.renderJavascriptReference(HEADSCRIPTS);
		response.renderOnLoadJavascript(BODY_ONLOAD_ADDTL);
		
		// include (this) tool style (CSS)
		response.renderCSSReference("/sakai-feeds-tool/css/style.css");	
	}
	
	protected String getToolSkinCSS(String skinRepo) {
		String skin = null;
		try{
			skin = SiteService.findTool(SessionManager.getCurrentToolSession().getPlacementId()).getSkin();
		}catch(Exception e){
			skin = ServerConfigurationService.getString("skin.default");
		}

		if(skin == null){
			skin = ServerConfigurationService.getString("skin.default");
		}

		return skinRepo + "/" + skin + "/tool.css";
	}
	
	protected Label newResourceLabel(String id, Component component) {
		return new Label(id, new StringResourceModel(id, component, null));
	}
	
	public String getResourceModel(String resourceKey, IModel model) {
		return new StringResourceModel(resourceKey, this, model).getString();
	}
}
