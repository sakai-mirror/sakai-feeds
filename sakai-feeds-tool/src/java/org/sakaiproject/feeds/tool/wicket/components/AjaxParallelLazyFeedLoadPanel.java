package org.sakaiproject.feeds.tool.wicket.components;

import java.util.Observable;
import java.util.Observer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.time.Duration;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.feeds.api.FeedSubscription;
import org.sakaiproject.feeds.api.FeedsService;
import org.sakaiproject.feeds.tool.facade.Locator;


public abstract class AjaxParallelLazyFeedLoadPanel extends Panel implements Observer {
	private static final long			serialVersionUID		= 1L;
	private static final long			AJAX_TIMER_MS			= 500;
	private long						feedLoadCancelShowAfter	= ServerConfigurationService.getInt(FeedsService.SAK_PROP_SHOW_CANCEL_AFTER, 10) * 1000;
	private static Log					LOG						= LogFactory.getLog(AjaxParallelLazyFeedLoadPanel.class);
	private AbstractAjaxTimerBehavior	abstractAjaxTimerBehavior;
	private Component					lazyLoadcomponent;
	private int							feedsCount				= 0;
	private int							feedsCached				= 0;
	private boolean						componentReady			= false;
	private boolean						componentAborted		= false;
	private String						cancelFeedLoadId		= null;
	private CharSequence				showCancelButtonJs		= "";
	private CharSequence				hideCancelButtonJs		= "";
	private String						feedCacheTaskId			= null;

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
		
		// javascript to show/hide cancel button
		final WebMarkupContainer showCancelButton = new WebMarkupContainer("showCancelButton", new Model("")) {
			private static final long	serialVersionUID	= 1L;
			@Override
			protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
				replaceComponentTagBody(markupStream, openTag, showCancelButtonJs);
			}
		};
		showCancelButton.setOutputMarkupId(true);
		add(showCancelButton);
		final WebMarkupContainer hideCancelButton = new WebMarkupContainer("hideCancelButton", new Model("")) {
			private static final long	serialVersionUID	= 1L;
			@Override
			protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
				replaceComponentTagBody(markupStream, openTag, hideCancelButtonJs);
			}
		};
		hideCancelButton.setOutputMarkupId(true);
		add(hideCancelButton);
		
		// cache feed...
		componentReady = false;
		if(subscription != null) {
			if(!subscription.isAggregateMultipleFeeds() && subscription.getUrl() != null && !subscription.getUrl().trim().equals("")) {
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
		}else{
			componentReady = true;
		}
		
		// pool until feed is cached
		abstractAjaxTimerBehavior = new AbstractAjaxTimerBehavior(Duration.milliseconds(AJAX_TIMER_MS)) {
			private static final long 	serialVersionUID = 1L;
			private long millisecondsEllapsed = 0;
			private long startTime = System.currentTimeMillis();

			@Override
			protected void onTimer(AjaxRequestTarget target) {
				if(componentAborted) {
					LOG.debug("Cancel loading of feed: "+subscription.getUrl());
					// hide cancel button and loading component
					loadingComponent.add(new AttributeModifier("style", new Model("display: none")));
					target.addComponent(loadingComponent);
					hideCancelButtonJs = getHideCancelButtonJs();
					target.addComponent(hideCancelButton);
					// stop this
					stop();		
					
				}else if(componentReady) {
					LOG.debug("About to draw component with cached feed: "+subscription.getUrl());
					// stop this
					stop();
					
					// disable loading component
					loadingComponent.add(new AttributeModifier("style", new Model("display: none")));
					target.addComponent(loadingComponent);
					hideCancelButtonJs = getHideCancelButtonJs();
					target.addComponent(hideCancelButton);
					
					// load content from cached feed
					lazyLoadcomponent = getLazyLoadComponent("content");
					AjaxParallelLazyFeedLoadPanel.this.replace(lazyLoadcomponent.setRenderBodyOnly(true));
					target.addComponent(AjaxParallelLazyFeedLoadPanel.this);
					
				}else if(!subscription.isAggregateMultipleFeeds() && feedLoadCancelShowAfter > -1){
					millisecondsEllapsed = System.currentTimeMillis() - startTime;
					// show cancel button
					if(millisecondsEllapsed > feedLoadCancelShowAfter && cancelFeedLoadId != null) {
						showCancelButtonJs = getShowCancelButtonJs();
						target.addComponent(showCancelButton);						
					}
				}
			}

		};
		add(abstractAjaxTimerBehavior);
		
	}

	public void setCancelFeedLoadId(String markupId) {
		cancelFeedLoadId = markupId;
	}
	
	public void cancelFeedLoad() {
		componentAborted = true;
		Locator.getFacade().getFeedsService().cancelCacheFeed(feedCacheTaskId);
	}
	
	private CharSequence getShowCancelButtonJs() {
		if(cancelFeedLoadId != null)
			return "try{$('#"+cancelFeedLoadId+"').fadeIn();}catch(err){}";
		else
			return "";
	}	
	
	private CharSequence getHideCancelButtonJs() {
		if(cancelFeedLoadId != null)
			return "try{$('#"+cancelFeedLoadId+"').hide();}catch(err){}";
		else
			return "";
	}			
	
	private void cacheFeed(final String feedUrl, final boolean forceExternalCheck) {
		Locator.getFacade().getFeedsService().loadCredentials();
		feedCacheTaskId = Locator.getFacade().getFeedsService().cacheFeed(feedUrl, forceExternalCheck, this);			
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
