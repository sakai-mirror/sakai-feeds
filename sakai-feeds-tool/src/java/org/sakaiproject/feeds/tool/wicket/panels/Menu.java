package org.sakaiproject.feeds.tool.wicket.panels;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.sakaiproject.feeds.tool.facade.Locator;
import org.sakaiproject.feeds.tool.wicket.pages.MainPage;
import org.sakaiproject.feeds.tool.wicket.pages.OptionsPage;
import org.sakaiproject.feeds.tool.wicket.pages.PermissionsPage;
import org.sakaiproject.feeds.tool.wicket.pages.SubscriptionsPage;


/**
 * @author Nuno Fernandes
 */
public class Menu extends Panel {
	private static final long		serialVersionUID	= 1L;
	
	private BookmarkablePageLink	refreshLink;
	private BookmarkablePageLink	subscriptionsLink;
	private BookmarkablePageLink	optionsLink;
	private BookmarkablePageLink	permissionsLink;

	public Menu(String id) {
		super(id);

		boolean subscriptionsVisible = Locator.getFacade().getFeedsService().allowSubscribeFeeds() || Locator.getFacade().getFeedsService().allowEditPermissions();
		boolean isInMyWorkspace = Locator.getFacade().getSiteService().isUserSite(Locator.getFacade().getToolManager().getCurrentPlacement().getContext());
		boolean permissionsVisible = !isInMyWorkspace && Locator.getFacade().getFeedsService().allowEditPermissions();	
		boolean optionsVisible = Locator.getFacade().getSessionManager().getCurrentSessionUserId() != null;

		PageParameters params = new PageParameters();
		params.put("forceExternalCheck", Boolean.TRUE);
		refreshLink = new BookmarkablePageLink("refreshLink", MainPage.class, params);
		add(refreshLink);

		subscriptionsLink = new BookmarkablePageLink("subscriptionsLink", SubscriptionsPage.class);
		subscriptionsLink.setVisible(subscriptionsVisible);
		add(subscriptionsLink);

		optionsLink = new BookmarkablePageLink("optionsLink", OptionsPage.class);
		optionsLink.setVisible(optionsVisible);
		add(optionsLink);	
		
		permissionsLink = new BookmarkablePageLink("permissionsLink", PermissionsPage.class);
		permissionsLink.setVisible(permissionsVisible);
		add(permissionsLink);
	}

}
