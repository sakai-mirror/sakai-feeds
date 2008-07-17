package org.sakaiproject.feeds.tool.wicket.pages;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.feeds.api.FeedSubscription;
import org.sakaiproject.feeds.api.FeedsService;
import org.sakaiproject.feeds.api.ViewOptions;
import org.sakaiproject.feeds.tool.facade.SakaiFacade;
import org.sakaiproject.feeds.tool.wicket.components.ExternalImage;
import org.sakaiproject.tool.api.Session;
import org.wicketstuff.dojo.markup.html.list.DojoOrderableListContainer;
import org.wicketstuff.dojo.markup.html.list.DojoOrderableRepeatingView;


public class OptionsPage extends BasePage {
	private static final long	serialVersionUID	= 1L;

	@SpringBean
	private transient SakaiFacade facade;
	
	private boolean isAggregate;
	private ViewOptions viewOptions;
	private Set<FeedSubscription> subscriptions;
	private List<FeedSubscription> subscriptionsList;
	
	private DojoOrderableRepeatingView list;
	private DojoOrderableListContainer container;
	
	public OptionsPage() {
		viewOptions = facade.getFeedsService().getViewOptions();
		isAggregate = facade.getFeedsService().isAggregateFeeds();
		if(isAggregate) {
			subscriptions = new HashSet<FeedSubscription>();
		}else{
			subscriptions = facade.getFeedsService().getSubscribedFeeds(FeedsService.MODE_SUBSCRIBED);
		}
		subscriptionsList = new LinkedList<FeedSubscription>(subscriptions);

		Form form = new Form("options");
		setModel(new CompoundPropertyModel(this));
		
		
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
		// TODO Auto-generated method stub
		//org.wicketstuff.dojo.
		super.renderHead(response);
	}



	private void saveOptions() {		
		// view options
		facade.getFeedsService().setViewOptions(viewOptions);
		
		// save in session
		Session session = facade.getSessionManager().getCurrentSession();
		session.setAttribute(FeedsService.SESSION_ATTR_VIEWOPTIONS, viewOptions);
		
		// subscriptions order
		if(!isAggregate) {
			List<String> urls = new LinkedList<String>();
			for(FeedSubscription fs : subscriptionsList){
				urls.add(fs.getUrl());
			}		
			facade.getFeedsService().setSubscriptionsOrder(urls);
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
		modes.add(ViewOptions.VIEW_FILTER_ALL);
		modes.add(ViewOptions.VIEW_FILTER_TODAY);
		modes.add(ViewOptions.VIEW_FILTER_LAST_WEEK);
		modes.add(ViewOptions.VIEW_FILTER_LAST_MONTH);
		modes.add(ViewOptions.VIEW_FILTER_LAST_5);
		modes.add(ViewOptions.VIEW_FILTER_LAST_10);
		return modes;		
	}
	
	public static List<String> getViewDetailModes() {
		List<String> modes = new ArrayList<String>();
		modes.add(ViewOptions.VIEW_DETAIL_FULL_ENTRY);
		modes.add(ViewOptions.VIEW_DETAIL_TITLE_ENTRY);
		modes.add(ViewOptions.VIEW_DETAIL_NO_ENTRY);
		return modes;
	}
}
