package org.sakaiproject.feeds.tool.wicket.panels;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.feeds.tool.facade.SakaiFacade;
import org.sakaiproject.feeds.tool.wicket.pages.MainPage;
import org.sakaiproject.feeds.tool.wicket.pages.OptionsPage;
import org.sakaiproject.feeds.tool.wicket.pages.PermissionsPage;
import org.sakaiproject.feeds.tool.wicket.pages.SubscriptionsPage;


/**
 * @author Nuno Fernandes
 */
public class Menu extends Panel {
	private static final long		serialVersionUID	= 1L;
	
	@SpringBean
	private transient SakaiFacade facade;
	
	private BookmarkablePageLink	refreshLink;
	private BookmarkablePageLink	subscriptionsLink;
	private BookmarkablePageLink	optionsLink;
	private BookmarkablePageLink	permissionsLink;

	public Menu(String id) {
		super(id);

		boolean subscriptionsVisible = facade.getFeedsService().allowSubscribeFeeds() || facade.getFeedsService().allowEditPermissions();
		boolean isInMyWorkspace = facade.getSiteService().isUserSite(facade.getToolManager().getCurrentPlacement().getContext());
		boolean permissionsVisible = !isInMyWorkspace && facade.getFeedsService().allowEditPermissions();		

		PageParameters params = new PageParameters();
		params.put("forceExternalCheck", Boolean.TRUE);
		refreshLink = new BookmarkablePageLink("refreshLink", MainPage.class, params);
		add(refreshLink);

		subscriptionsLink = new BookmarkablePageLink("subscriptionsLink", SubscriptionsPage.class);
		subscriptionsLink.setVisible(subscriptionsVisible);
		add(subscriptionsLink);

		optionsLink = new BookmarkablePageLink("optionsLink", OptionsPage.class);
		add(optionsLink);	
		
		permissionsLink = new BookmarkablePageLink("permissionsLink", PermissionsPage.class);
		permissionsLink.setVisible(permissionsVisible);
		add(permissionsLink);
	}

}
