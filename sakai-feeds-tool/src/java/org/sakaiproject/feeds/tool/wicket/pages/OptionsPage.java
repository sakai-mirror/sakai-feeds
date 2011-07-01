package org.sakaiproject.feeds.tool.wicket.pages;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.sakaiproject.feeds.api.CustomTitleOptions;
import org.sakaiproject.feeds.api.FeedSubscription;
import org.sakaiproject.feeds.api.FeedsService;
import org.sakaiproject.feeds.api.ViewOptions;
import org.sakaiproject.feeds.tool.facade.Locator;
import org.sakaiproject.feeds.tool.wicket.components.ExternalImage;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.ToolSession;
//import org.wicketstuff.dojo.markup.html.list.DojoOrderableListContainer;
//import org.wicketstuff.dojo.markup.html.list.DojoOrderableRepeatingView;


public class OptionsPage extends BasePage {
	/*private static final long	serialVersionUID	= 1L;
	
	private boolean hasSiteUpd = false;
	
	
	private boolean isAggregate;
	private ViewOptions viewOptions;
	private CustomTitleOptions titleOptions;
	private Set<FeedSubscription> subscriptions;
	private List<FeedSubscription> subscriptionsList;
	
	private DojoOrderableRepeatingView list;
	private DojoOrderableListContainer container;
	
	public OptionsPage() {
		viewOptions = Locator.getFacade().getFeedsService().getViewOptions();
		titleOptions = Locator.getFacade().getFeedsService().getCustomTitleOptions();
		isAggregate = Locator.getFacade().getFeedsService().isAggregateFeeds();
		try{
			String userId = Locator.getFacade().getSessionManager().getCurrentSessionUserId();
			String siteRef = Locator.getFacade().getSiteService().siteReference(Locator.getFacade().getToolManager().getCurrentPlacement().getContext());
			hasSiteUpd = Locator.getFacade().getAuthzGroupService().isAllowed(userId, SiteService.SECURE_UPDATE_SITE, siteRef);
		}catch(Exception e) {
			// something wrong happened, we can assume user is not maintainer on the current context
			hasSiteUpd = false;
		}
		if(isAggregate) {
			subscriptions = new HashSet<FeedSubscription>();
		}else{
			subscriptions = Locator.getFacade().getFeedsService().getSubscribedFeeds(FeedsService.MODE_SUBSCRIBED);
		}
		subscriptionsList = new LinkedList<FeedSubscription>(subscriptions);

		Form form = new Form("options");
		setDefaultModel(new CompoundPropertyModel(this));
		
		// custom title options
		boolean supportLocalizedPages = false;
		try{
			SitePage.class.getMethod("getTitleCustom", new Class[]{});
			supportLocalizedPages = true;
		}catch(Exception e){
			supportLocalizedPages = false;
		}
		WebMarkupContainer customtitleContainer = new WebMarkupContainer("customtitle-options");
		WebMarkupContainer customTitleChkTr = new WebMarkupContainer("customTitleChkTr");
		customTitleChkTr.add(new CheckBox("useCustomTitle"));
		customTitleChkTr.setVisible(supportLocalizedPages);
		customtitleContainer.add(customTitleChkTr);
		TextField customToolTitleTxt = new TextField("customToolTitle");
		TextField customPageTitleTxt = new TextField("customPageTitle");
		if(!getUseCustomTitle()) {
			customToolTitleTxt.add(new SimpleAttributeModifier("disabled", "disabled"));
			customPageTitleTxt.add(new SimpleAttributeModifier("disabled", "disabled"));
		}
		customtitleContainer.add(customToolTitleTxt);
		customtitleContainer.add(customPageTitleTxt);
		customtitleContainer.setVisible(hasSiteUpd);
		form.add(customtitleContainer);
		
		// view options
		final DropDownChoice viewFilter = new DropDownChoice("viewFilter", getViewFilterModes(), new IChoiceRenderer(){
			private static final long	serialVersionUID	= 0L;
			public Object getDisplayValue(Object obj) {
				return (String) new ResourceModel((String) obj).getObject();
			}
			public String getIdValue(Object obj, int arg1) {
				return (String) obj;
			}			
		});
		form.add(viewFilter);		
		final DropDownChoice viewDetail = new DropDownChoice("viewDetail", getViewDetailModes(), new IChoiceRenderer(){
			private static final long	serialVersionUID	= 0L;
			public Object getDisplayValue(Object obj) {
				return (String) new ResourceModel((String) obj).getObject();
			}
			public String getIdValue(Object obj, int arg1) {
				return (String) obj;
			}			
		});
		form.add(viewDetail);
		
		
		// subscriptions order
		container = new DojoOrderableListContainer("container");
		list = new DojoOrderableRepeatingView("subscriptionsList"){

			public void moveItem(int from, int to, AjaxRequestTarget target) {
				FeedSubscription drag = (FeedSubscription) subscriptionsList.remove(from);
				subscriptionsList.add(to, drag);
				
			}

			public void removeItem(Item item, AjaxRequestTarget target) {
				subscriptionsList.remove(item.getModelObject());
				
			}

			protected Iterator getItemModels() {
				ArrayList modelList = new ArrayList();
				Iterator it = subscriptionsList.iterator();
				while (it.hasNext()){
					modelList.add(new Model((FeedSubscription)it.next()));
				}
				return modelList.iterator();
			}

			protected void populateItem(Item item) {
				FeedSubscription subscription = (FeedSubscription) item.getModelObject();;
				item.add(new ExternalImage("iconUrl", subscription.getIconUrl()));
				item.add(new Label("title", subscription.getTitle()));
			}
			
		};
		container.add(list);
		container.setVisible(!isAggregate);
		form.add(container);
		
		// Subscriptions order instruction...
		WebMarkupContainer orderInstruction = new WebMarkupContainer("orderInstruction");
		orderInstruction.setVisible(getSubscriptionsList().size() > 0);
		form.add(orderInstruction);
		
		// No subscriptions...
		WebMarkupContainer noSubscriptions = new WebMarkupContainer("noSubscriptions");
		noSubscriptions.setVisible(getSubscriptionsList().size() == 0 && !isAggregate);
		form.add(noSubscriptions);
		
		// Can't order aggregated subscriptions
		WebMarkupContainer noSubscriptionsAggr = new WebMarkupContainer("noSubscriptionsAggr");
		noSubscriptionsAggr.setVisible(isAggregate);
		form.add(noSubscriptionsAggr);
		

		
		// Bottom Buttons
		Button save = new Button("save") {
			private static final long	serialVersionUID	= 1L;

			@Override
			public void onSubmit() {
				saveOptions();
				setResponsePage(MainPage.class);
				super.onSubmit();
			}
		};
		save.setDefaultFormProcessing(true);
		form.add(save);
		Button cancel = new Button("cancel") {
			private static final long	serialVersionUID	= 1L;

			@Override
			public void onSubmit() {
				setResponsePage(MainPage.class);
				super.onSubmit();
			}
		};
		cancel.setDefaultFormProcessing(false);
		form.add(cancel);
		
		add(form);
		
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		response.renderJavascriptReference("/library/js/jquery.js");
		super.renderHead(response);
	}

	private void saveOptions() {
		if(hasSiteUpd) {
			// custom titles
			Locator.getFacade().getFeedsService().setCustomTitleOptions(titleOptions);
			
			// schedule a top refresh - vm prop, currently has no effect!
			ToolSession session = Locator.getFacade().getSessionManager().getCurrentToolSession();
			if(session.getAttribute(MainPage.FORCE_TOP_REFRESH) == null){
				session.setAttribute(MainPage.FORCE_TOP_REFRESH, Boolean.TRUE);
			}
		}
		
		// view options
		Locator.getFacade().getFeedsService().setViewOptions(viewOptions);
		
		// save in session
		Session session = Locator.getFacade().getSessionManager().getCurrentSession();
		session.setAttribute(FeedsService.SESSION_ATTR_VIEWOPTIONS, viewOptions);
		
		// subscriptions order
		if(!isAggregate) {
			List<String> urls = new LinkedList<String>();
			for(FeedSubscription fs : subscriptionsList){
				urls.add(fs.getUrl());
			}		
			Locator.getFacade().getFeedsService().setSubscriptionsOrder(urls);
		}
	}
	
	public List<FeedSubscription> getSubscriptionsList() {
		return subscriptionsList;
	}

	public void setSubscriptionsList(List<FeedSubscription> subscriptionsList) {
		this.subscriptionsList = subscriptionsList;
	}

	public String getViewFilter() {
		return viewOptions.getViewFilter();
	}

	public void setViewFilter(String viewFilter) {
		viewOptions.setViewFilter(viewFilter);
	}

	public String getViewDetail() {
		return viewOptions.getViewDetail();
	}

	public void setViewDetail(String viewDetail) {
		viewOptions.setViewDetail(viewDetail);
	}
	
	public static List<String> getViewFilterModes(){
		List<String> modes = new ArrayList<String>();
		for(int i=0; i<ViewOptions.VIEW_FILTERS.length; i++) {
			modes.add(ViewOptions.VIEW_FILTERS[i]);
		}
		return modes;		
	}
	
	public static List<String> getViewDetailModes() {
		List<String> modes = new ArrayList<String>();
		for(int i=0; i<ViewOptions.VIEW_DETAILS.length; i++) {
			modes.add(ViewOptions.VIEW_DETAILS[i]);
		}
		return modes;
	}
	
	public boolean getUseCustomTitle() {
		return titleOptions.getUseCustomTitle();
	}
	
	public void setUseCustomTitle(boolean useCustom) {
		titleOptions.setUseCustomTitle(useCustom);
	}
	
	public String getCustomToolTitle() {
		return titleOptions.getCustomToolTitle();
	}
	
	public void setCustomToolTitle(String title) {
		titleOptions.setCustomToolTitle(title);
	}
	
	public String getCustomPageTitle() {
		return titleOptions.getCustomPageTitle();
	}
	
	public void setCustomPageTitle(String title) {
		titleOptions.setCustomPageTitle(title);
	}*/
}
