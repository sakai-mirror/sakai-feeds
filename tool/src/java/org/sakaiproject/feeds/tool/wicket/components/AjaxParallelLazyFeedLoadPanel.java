package org.sakaiproject.feeds.tool.wicket.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.time.Duration;
import org.sakaiproject.feeds.api.FeedEntry;
import org.sakaiproject.feeds.api.FeedSubscription;
import org.sakaiproject.feeds.tool.facade.SakaiFacade;


public abstract class AjaxParallelLazyFeedLoadPanel extends Panel implements Observer {
	private static final long			serialVersionUID	= 1L;
	private static Log					LOG	= LogFactory.getLog(AjaxParallelLazyFeedLoadPanel.class);
	private AbstractAjaxTimerBehavior	abstractAjaxTimerBehavior;
	private Component					lazyLoadcomponent;
	private int							feedsCount			= 0;
	private int							feedsCached			= 0;
	private boolean						componentReady		= false;

	@SpringBean
	private transient SakaiFacade facade;
	
	public AjaxParallelLazyFeedLoadPanel(String id, final Component loadingComponent, FeedSubscription subscription, boolean forceExternalCheck) {
		this(id, null, loadingComponent, subscription, forceExternalCheck);
	}

	public AjaxParallelLazyFeedLoadPanel(final String id, IModel model, final Component loadingComponent, final FeedSubscription subscription, final boolean forceExternalCheck) {
		super(id, model);
		setOutputMarkupId(true);
		
		// content to be replaced
		final Label contents = new Label("content", "");
		contents.setEscapeModelStrings(false);
		contents.setRenderBodyOnly(true);
		contents.setOutputMarkupId(true);
		add(contents);
		
		// ajax loading indicator
		loadingComponent.add(new AttributeModifier("style", new Model("display: inline")));
		
		// cache feed...
		componentReady = false;
		if(subscription != null && !subscription.isAggregateMultipleFeeds() && subscription.getUrl() != null && !subscription.getUrl().trim().equals("")) {
			feedsCount = 1;
			feedsCached = 0;
			cacheFeed(subscription.getUrl(), forceExternalCheck);
		}else if(subscription.isAggregateMultipleFeeds()) {
			String[] urls = subscription.getUrls();
			feedsCount = urls.length;
			feedsCached = 0;
			for(int i=0; i<urls.length; i++) {
				cacheFeed(urls[i], forceExternalCheck);
			}
		}else{
			componentReady = true;
		}
		
		// pool until feed is cached
		abstractAjaxTimerBehavior = new AbstractAjaxTimerBehavior(Duration.milliseconds(500)) {
			private static final long 	serialVersionUID = 1L;

			@Override
			protected void onTimer(AjaxRequestTarget target) {
				if(componentReady) {
					LOG.debug("About to draw component with cached feed: "+subscription.getUrl());
					// stop this
					stop();
					
					// disable loading component
					loadingComponent.add(new AttributeModifier("style", new Model("display: none")));
					target.addComponent(loadingComponent);
					
					// load content from cached feed
					lazyLoadcomponent = getLazyLoadComponent("content");
					AjaxParallelLazyFeedLoadPanel.this.replace(lazyLoadcomponent.setRenderBodyOnly(true));
					target.addComponent(AjaxParallelLazyFeedLoadPanel.this);
				}
			}

		};
		add(abstractAjaxTimerBehavior);
		
	}
	
	private void cacheFeed(final String feedUrl, final boolean forceExternalCheck) {
			facade.getFeedsService().cacheFeed(feedUrl, forceExternalCheck, this);			
	}

	public void update(Observable o, Object arg) {
		feedsCached++;
		if(feedsCached == feedsCount) {
			componentReady = true;
		}
	}

	/**
	 * @param markupId The components markupid.
	 * @return The component that must be lazy created.
	 */
	public abstract Component getLazyLoadComponent(String markupId);
	
}
