package org.sakaiproject.feeds.tool.wicket.panels;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.feeds.tool.facade.SakaiFacade;
import org.sakaiproject.feeds.tool.wicket.pages.MainPage;
import org.sakaiproject.feeds.tool.wicket.pages.OptionsPage;
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

	public Menu(String id) {
		super(id);

		boolean subscriptionsVisible = facade.getFeedsService().allowSubscribeFeeds();

		PageParameters params = new PageParameters();
		params.put("forceExternalCheck", Boolean.TRUE);
		refreshLink = new BookmarkablePageLink("refreshLink", MainPage.class, params);
		add(refreshLink);

		subscriptionsLink = new BookmarkablePageLink("subscriptionsLink", SubscriptionsPage.class);
		subscriptionsLink.setVisible(subscriptionsVisible);
		add(subscriptionsLink);

		optionsLink = new BookmarkablePageLink("optionsLink", OptionsPage.class);
		add(optionsLink);	
	}

}
