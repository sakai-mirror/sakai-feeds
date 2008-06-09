package org.sakaiproject.feeds.tool.wicket.panels;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.feeds.api.AggregateFeedOptions;
import org.sakaiproject.feeds.api.FeedSubscription;
import org.sakaiproject.feeds.api.FeedsService;
import org.sakaiproject.feeds.api.ViewOptions;
import org.sakaiproject.feeds.tool.facade.SakaiFacade;
import org.sakaiproject.feeds.tool.wicket.components.AjaxIndicator;
import org.sakaiproject.feeds.tool.wicket.components.AjaxParallelLazyLoadPanel;
import org.sakaiproject.feeds.tool.wicket.components.AjaxUpdatingBehaviorWithIndicator;
import org.sakaiproject.feeds.tool.wicket.components.ExternalImage;
import org.sakaiproject.feeds.tool.wicket.dataproviders.FeedDataProvider;
import org.sakaiproject.feeds.tool.wicket.dataproviders.SubscriptionsDataProvider;
import org.sakaiproject.tool.api.Session;

public class FeedsPanel extends Panel {
	private static final long	serialVersionUID	= 1L;

	@SpringBean
	private transient SakaiFacade facade;
	
	private ViewOptions viewOptions;
	private AggregateFeedOptions aggregateFeedOptions; 
	
	public FeedsPanel(String id, final Boolean forceExternalCheck) {
		super(id);
		
		// do necessary client setup (feed auth, cookies, ...)
		setupClient();				
		
		SubscriptionsDataProvider subscriptionDataProvider = new SubscriptionsDataProvider(SubscriptionsDataProvider.MODE_SUBSCRIBED);
		int subscriptionsSize = subscriptionDataProvider.size();
		
		// create the feed filters
		final Component parent = this;
		setModel(new CompoundPropertyModel(this));
		
		WebMarkupContainer viewOptionsMarkup = new WebMarkupContainer("viewOptions");
		final DropDownChoice viewFilter = new DropDownChoice("viewFilter", getViewFilterModes(), new IChoiceRenderer(){
			private static final long	serialVersionUID	= 0L;
			public Object getDisplayValue(Object obj) {
				return (String) new ResourceModel((String) obj).getObject();
			}
			public String getIdValue(Object obj, int arg1) {
				return (String) obj;
			}			
		});
		viewFilter.setOutputMarkupId(true);
		viewOptionsMarkup.add(viewFilter);
		final DropDownChoice viewDetail = new DropDownChoice("viewDetail", getViewDetailModes(), new IChoiceRenderer(){
			private static final long	serialVersionUID	= 0L;
			public Object getDisplayValue(Object obj) {
				StringResourceModel model = new StringResourceModel((String)obj, parent, null); 
				return model.getString();
			}
			public String getIdValue(Object obj, int arg1) {
				return (String) obj;
			}			
		});
		viewDetail.setOutputMarkupId(true);
		viewOptionsMarkup.add(viewDetail);
		AjaxIndicator ajaxIndicator1 = new AjaxIndicator("viewFilterIndicator");
		viewOptionsMarkup.add(ajaxIndicator1);
		AjaxIndicator ajaxIndicator2 = new AjaxIndicator("viewDetailIndicator");
		viewOptionsMarkup.add(ajaxIndicator2);
		viewOptionsMarkup.setVisible(subscriptionsSize > 0);
		add(viewOptionsMarkup);
		
        // create the DataView
		final List<Label> feedDescriptions = new ArrayList<Label>();
		final List<AjaxParallelLazyLoadPanel> feedEntriesAjaxPanels = new ArrayList<AjaxParallelLazyLoadPanel>();
		final List<FeedEntriesPanel> feedEntriesPanels = new ArrayList<FeedEntriesPanel>();
		final List<FeedDataProvider> feedEntriesProviders = new ArrayList<FeedDataProvider>();
        final DataView view = new DataView("feed", subscriptionDataProvider) {
        	private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item item) {
				final FeedSubscription subscription = (FeedSubscription) item.getModelObject();
				
				setOutputMarkupId(true);
				item.setOutputMarkupId(true);
				
				ExternalImage iconUrl = new ExternalImage("iconUrl", subscription.getIconUrl());
				iconUrl.setVisible(!(subscription.isAggregateMultipleFeeds() && aggregateFeedOptions.getTitleDisplayOption() == AggregateFeedOptions.TITLE_DISPLAY_NONE));
				item.add(iconUrl);
				
				// feed external source link
				ExternalLink sourceLink = new ExternalLink("sourceLink", subscription.getUrl(), new StringResourceModel("sourcelink", parent, null).getString());
				sourceLink.add(new AttributeModifier("title", true, new StringResourceModel("sourcelink.title", parent, null)));
				sourceLink.setVisible(subscription.getUrl() != null && !subscription.isAggregateMultipleFeeds());
				item.add(sourceLink);
				
				// feed description
				final WebMarkupContainer descriptionWrapper = new WebMarkupContainer("descriptionWrapper");
				descriptionWrapper.setOutputMarkupId(true);
				final Label description = new Label("description", subscription.getDescription());
				description.setOutputMarkupId(true);
				description.setVisible(subscription.getDescription() != null && !subscription.getDescription().equals("null") && !subscription.isAggregateMultipleFeeds() && ViewOptions.VIEW_DETAIL_FULL_ENTRY.equals(getViewDetail()));
				descriptionWrapper.add(description);
				item.add(descriptionWrapper);				
				feedDescriptions.add(description);
				
				// feed entries count
				final Label feedCount = new Label("feedCount","");
				feedCount.setOutputMarkupId(true);
				feedCount.setVisible(!(subscription.isAggregateMultipleFeeds() && aggregateFeedOptions.getTitleDisplayOption() == AggregateFeedOptions.TITLE_DISPLAY_NONE));
				item.add(feedCount);
				
				// feed published date (unused)
				Label publishedDate = new Label("publishedDate", "");
				item.add(publishedDate);
				
				// feed entries
				AjaxIndicator entriesLoadIndicator = new AjaxIndicator("entriesLoadIndicator");
				item.add(entriesLoadIndicator);
				final WebMarkupContainer entriesWrapper = new WebMarkupContainer("entriesWrapper");
				final AjaxParallelLazyLoadPanel panel = new AjaxParallelLazyLoadPanel("entries", entriesLoadIndicator) {
					private static final long	serialVersionUID	= 1L;
					@Override
					public Component getLazyLoadComponent(String id) {
						FeedEntriesPanel panel = new FeedEntriesPanel(id, subscription, getViewFilter(), getViewDetail(), forceExternalCheck);
						FeedDataProvider provider = panel.getFeedDataProvider();
						provider.setForceExternalCheck(forceExternalCheck);
						feedEntriesProviders.add(provider);
						feedEntriesPanels.add(panel);
						return panel;
					}					
				};				
				if(getViewDetail().equals(ViewOptions.VIEW_DETAIL_NO_ENTRY))
					panel.add(new AttributeModifier("style", true, new Model("display: none")));
				entriesWrapper.add(panel);
				entriesWrapper.setOutputMarkupId(true);
				item.add(entriesWrapper);
				feedEntriesAjaxPanels.add(panel);
				
				// feed title link
				final WebMarkupContainer titleLinkExpandImg = new WebMarkupContainer("titleLinkExpandImg") {
					private static final long	serialVersionUID	= 1L;
					@Override
					protected void onComponentTag(ComponentTag tag) {					
						boolean entriesVisible = panel.isVisible() && !getViewDetail().equals(ViewOptions.VIEW_DETAIL_NO_ENTRY);
						if(entriesVisible) {
							tag.put("src", "/sakai-feeds-tool/img/bullet_toggle_minus.png");
							tag.put("alt", "-");
						}else{
							tag.put("src", "/sakai-feeds-tool/img/bullet_toggle_plus.png");
							tag.put("alt", "+");
						}
						super.onComponentTag(tag);
					}
				};
				titleLinkExpandImg.setOutputMarkupId(true);
				String titleStr = null;
				if(subscription.isAggregateMultipleFeeds()) {
					if(aggregateFeedOptions.getTitleDisplayOption() == AggregateFeedOptions.TITLE_DISPLAY_DEFAULT)
						titleStr = (new StringResourceModel("aggregate.title", parent, null)).getString();
					else if(aggregateFeedOptions.getTitleDisplayOption() == AggregateFeedOptions.TITLE_DISPLAY_CUSTOM)
						titleStr = aggregateFeedOptions.getCustomTitle();
					else
						titleStr = "";
				}else{
					titleStr = subscription.getTitle();
				}
				final Label titleLinkLabel = new Label("titleLinkLabel", titleStr);
				Link link = new Link("title") {	
					private static final long	serialVersionUID	= 1L;
					@Override
					protected void onComponentTag(ComponentTag tag) {
						tag.put("onclick", "showHide('"+panel.getMarkupId()+"','"+titleLinkExpandImg.getMarkupId()+"')");
						super.onComponentTag(tag);
					}
					@Override
					public void onClick() {
					}
					@Override
					protected CharSequence getURL() {
						return "#";
					}
				};
				boolean descriptionCanBeShown = subscription.getDescription() != null && !subscription.getDescription().equals("null");
				if(descriptionCanBeShown){
					link.add(new AttributeModifier("title",true, new Model(subscription.getDescription())));
				}				
				link.setOutputMarkupId(true);
				link.add(titleLinkExpandImg);
				link.add(titleLinkLabel);
				link.setVisible(!(subscription.isAggregateMultipleFeeds() && aggregateFeedOptions.getTitleDisplayOption() == AggregateFeedOptions.TITLE_DISPLAY_NONE));				
				item.add(link);
			}
        	
        };
        view.setOutputMarkupId(true);
        add(view);
     
        // no subscriptions
        WebMarkupContainer noSubscriptions = new WebMarkupContainer("noSubscriptions");
        noSubscriptions.setVisible(subscriptionsSize == 0);
        add(noSubscriptions);
        
        
        // create the ajax behaviors
		viewFilter.add(new AjaxUpdatingBehaviorWithIndicator("onchange", ajaxIndicator1){
			private static final long	serialVersionUID	= 1L;
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				String viewMode = viewFilter.getModelValue();
				for(FeedDataProvider p : feedEntriesProviders) {
					p.setViewDetail(viewMode);
				}	
				for(AjaxParallelLazyLoadPanel p : feedEntriesAjaxPanels) {
					target.addComponent(p);
				}
				target.appendJavascript("setMainFrameHeightNoScroll(window.name);");
			}	
			
		});
		viewDetail.add(new AjaxUpdatingBehaviorWithIndicator("onchange", ajaxIndicator2){
			private static final long	serialVersionUID	= 1L;
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				String viewDetailMode = viewDetail.getModelValue();
				for(FeedEntriesPanel p : feedEntriesPanels) {
					p.setViewDetail(viewDetailMode);
				}	
				for(AjaxParallelLazyLoadPanel p : feedEntriesAjaxPanels) {
					if(getViewDetail().equals(ViewOptions.VIEW_DETAIL_NO_ENTRY))
						p.add(new AttributeModifier("style", true, new Model("display: none")));
					else
						p.add(new AttributeModifier("style", true, new Model("display: block")));
					target.addComponent(p);
				}
				for(Label l : feedDescriptions) {
					l.setVisible(ViewOptions.VIEW_DETAIL_FULL_ENTRY.equals(getViewDetail()));
					target.addComponent(l.getParent().getParent());
				}
				target.appendJavascript("setMainFrameHeightNoScroll(window.name);");
			}
		});
	}

	private void setupClient() {
		// load saved credentials
		facade.getFeedsService().loadCredentials();
		
		// load viewOptions
		Session session = facade.getSessionManager().getCurrentSession();
		viewOptions = (ViewOptions) session.getAttribute(FeedsService.SESSION_ATTR_VIEWOPTIONS); 
		if(viewOptions == null) {
			viewOptions = facade.getFeedsService().getViewOptions();
			session.setAttribute(FeedsService.SESSION_ATTR_VIEWOPTIONS, viewOptions);
		}
		
		// load aggregateFeedOptions
		if(facade.getFeedsService().isAggregateFeeds()) {
			aggregateFeedOptions = facade.getFeedsService().getAggregateFeedsOptions();
			if(aggregateFeedOptions.getTitleDisplayOption() == AggregateFeedOptions.TITLE_DISPLAY_NONE) {
				viewOptions.setViewDetail(ViewOptions.VIEW_DETAIL_TITLE_ENTRY);
				session.setAttribute(FeedsService.SESSION_ATTR_VIEWOPTIONS, viewOptions);
			}
		}
	}
	
	public String getViewFilter() {
		return viewOptions.getViewFilter();
	}

	public void setViewFilter(String viewFilter) {
		viewOptions.setViewFilter(viewFilter);
		
		// save in session
		Session session = facade.getSessionManager().getCurrentSession();
		session.setAttribute(FeedsService.SESSION_ATTR_VIEWOPTIONS, viewOptions);
	}

	public String getViewDetail() {
		return viewOptions.getViewDetail();
	}

	public void setViewDetail(String viewDetail) {
		viewOptions.setViewDetail(viewDetail);
		
		// save in session
		Session session = facade.getSessionManager().getCurrentSession();
		session.setAttribute(FeedsService.SESSION_ATTR_VIEWOPTIONS, viewOptions);
	}
	

	public List<String> getViewFilterModes(){
		List<String> modes = new ArrayList<String>();
		modes.add(ViewOptions.VIEW_FILTER_ALL);
		modes.add(ViewOptions.VIEW_FILTER_TODAY);
		modes.add(ViewOptions.VIEW_FILTER_LAST_WEEK);
		modes.add(ViewOptions.VIEW_FILTER_LAST_MONTH);
		modes.add(ViewOptions.VIEW_FILTER_LAST_5);
		modes.add(ViewOptions.VIEW_FILTER_LAST_10);
		return modes;		
	}
	


	public List<String> getViewDetailModes() {
		List<String> modes = new ArrayList<String>();
		modes.add(ViewOptions.VIEW_DETAIL_FULL_ENTRY);
		modes.add(ViewOptions.VIEW_DETAIL_TITLE_ENTRY);
		if(!(facade.getFeedsService().isAggregateFeeds() && aggregateFeedOptions.getTitleDisplayOption() == AggregateFeedOptions.TITLE_DISPLAY_NONE)) {
			modes.add(ViewOptions.VIEW_DETAIL_NO_ENTRY);
		}
		return modes;
	}
	
	
}
