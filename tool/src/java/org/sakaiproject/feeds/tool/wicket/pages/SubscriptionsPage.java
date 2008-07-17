package org.sakaiproject.feeds.tool.wicket.pages;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.feeds.api.AggregateFeedOptions;
import org.sakaiproject.feeds.api.FeedSubscription;
import org.sakaiproject.feeds.api.FeedsService;
import org.sakaiproject.feeds.api.SavedCredentials;
import org.sakaiproject.feeds.api.exception.FeedAuthenticationException;
import org.sakaiproject.feeds.api.exception.FetcherException;
import org.sakaiproject.feeds.api.exception.InvalidFeedException;
import org.sakaiproject.feeds.tool.facade.SakaiFacade;
import org.sakaiproject.feeds.tool.wicket.components.ExternalImage;
import org.sakaiproject.feeds.tool.wicket.dataproviders.SubscriptionsDataProvider;
import org.sakaiproject.feeds.tool.wicket.panels.CSSFeedbackPanel;


public class SubscriptionsPage extends BasePage {
	private static final long			serialVersionUID			= 1L;
	private static Log					LOG	= LogFactory.getLog(SubscriptionsPage.class);

	@SpringBean
	private transient SakaiFacade		facade;
	private SubscriptionsDataProvider	allInstitutionalDataProvider;
	private SubscriptionsDataProvider	subscriptionsWithoutInstitutionalDataProvider;

	private FeedbackPanel				feedback;

	private String						newSubscribedUrl			= "";
	private String						username					= "";
	private String						password					= "";
	private boolean						rememberMe					= true;
	private boolean						aggregate					= false;
	private AggregateFeedOptions		aggregateOptions			= null;

	private String						authenticationRealm;
	private Set<SavedCredentials>		savedCredentials			= null;
	
	private Set<FeedSubscription>		previousInstitutionalSubscriptions = null;
	private Set<FeedSubscription>		previousUserSubscriptions = null;

	public SubscriptionsPage() {
		final Component component = this;
		
		if(savedCredentials == null)
			savedCredentials = facade.getFeedsService().getSavedCredentials();
		
		setAggregate(facade.getFeedsService().isAggregateFeeds());
		aggregateOptions = facade.getFeedsService().getAggregateFeedsOptions();
		
		feedback = new CSSFeedbackPanel("messages");
		add(feedback);
		// feedback.setOutputMarkupId(true);

		Form options = new Form("subscriptions");
		setModel(new CompoundPropertyModel(this));

		// Institutional subscriptions
		WebMarkupContainer institutionalWrapper = new WebMarkupContainer("institutionalWrapper");
		allInstitutionalDataProvider = new SubscriptionsDataProvider(SubscriptionsDataProvider.MODE_ALL_INSTITUTIONAL);
		if(previousInstitutionalSubscriptions == null) {
			previousInstitutionalSubscriptions = getDeepCopy(allInstitutionalDataProvider.getFeedSubscriptions());
		}
		DataView institutionalView = new DataView("institutionalFeeds", allInstitutionalDataProvider) {
			private static final long	serialVersionUID	= 1L;

			@Override
			protected void populateItem(Item item) {
				FeedSubscription subscription = (FeedSubscription) item.getModelObject();
				item.add(new ExternalImage("iconUrl", subscription.getIconUrl()));
				item.add(new Label("title", subscription.getTitle()));
				item.add(new CheckBox("selected", new PropertyModel(subscription, "selected")));
			}
		};
		institutionalWrapper.add(institutionalView);
		institutionalWrapper.setVisible(allInstitutionalDataProvider.size() > 0);
		options.add(institutionalWrapper);
		

		// Other subscriptions
		final FormComponent url = new TextField("newSubscribedUrl");
		options.add(url);

		// other Auth details
		final WebMarkupContainer otherAuthDetails = new WebMarkupContainer("other.auth.details");
		final TextField username = new TextField("username");
		otherAuthDetails.add(username);
		final PasswordTextField password = new PasswordTextField("password");
		otherAuthDetails.add(password);
		otherAuthDetails.setVisible(false);
		options.add(otherAuthDetails);

		// other Auth details - remember me
		final WebMarkupContainer otherAuthRememberMe = new WebMarkupContainer("other.auth.rememberme");
		final CheckBox rememberMeChk = new CheckBox("rememberMe");
		otherAuthRememberMe.add(rememberMeChk);
		otherAuthRememberMe.setVisible(false);
		options.add(otherAuthRememberMe);
		
		// Aggregation
		CheckBox aggr = new CheckBox("aggregate");
		aggr.add(new AttributeModifier("onclick", true, new Model("$('.aggregateOptionsClass').toggle();$('.aggregateCustomTitleClass').toggle(); setMainFrameHeightNoScroll(window.name);")));
		options.add(aggr);
		WebMarkupContainer aggregateOptions = new WebMarkupContainer("aggregateOptions");
		RadioGroup group = new RadioGroup("aggregateOptionsGroup");
		ListView persons = new ListView("aggregateOptions", getAggregateOptions()) {
			private static final long	serialVersionUID	= 1L;
			protected void populateItem(ListItem item) {
				IModel model = null;
				Radio radio = new Radio("radio", item.getModel());
				item.add(radio);
				if(item.getModelObjectAsString().equals(String.valueOf(AggregateFeedOptions.TITLE_DISPLAY_DEFAULT))) {
					model = new Model(component);
					radio.add(new AttributeModifier("onclick", true, new Model("$('.aggregateCustomTitleClass').attr('disabled','true'); setMainFrameHeightNoScroll(window.name);")));
				}else if(item.getModelObjectAsString().equals(String.valueOf(AggregateFeedOptions.TITLE_DISPLAY_CUSTOM))) {
					radio.add(new AttributeModifier("onclick", true, new Model("$('.aggregateCustomTitleClass').removeAttr('disabled'); setMainFrameHeightNoScroll(window.name);")));
				}else{
					radio.add(new AttributeModifier("onclick", true, new Model("$('.aggregateCustomTitleClass').attr('disabled','true'); setMainFrameHeightNoScroll(window.name);")));
				}
				item.add(new Label("optionName", new StringResourceModel("aggregateOption."+item.getModelObjectAsString(), component, model)));
			}
		};
		group.add(persons);
		TextField aggregateCustomTitle = new TextField("aggregateCustomTitle");
		if(!isAggregate()) {
			aggregateOptions.add(new AttributeModifier("style", true, new Model("display: none;")));
			aggregateCustomTitle.add(new AttributeModifier("style", true, new Model("display: none;")));
		}
		if(!getAggregateOptionsGroup().equals(String.valueOf(AggregateFeedOptions.TITLE_DISPLAY_CUSTOM))) {
			aggregateCustomTitle.add(new AttributeModifier("disabled", true, new Model("true")));
		}
		aggregateOptions.add(group);
		options.add(aggregateCustomTitle);
		options.add(aggregateOptions);
		
		
		// Buttons
		Button subscribe = new Button("subscribe") {
			private static final long	serialVersionUID	= 1L;

			@Override
			public void onSubmit() {
				FeedSubscription subscription = null;
				try{
					subscription = urlToFeedSubscription(getNewSubscribedUrl(), getUsername(), getPassword());
					otherAuthDetails.setVisible(false);
					otherAuthRememberMe.setVisible(false);
				}catch(FeedAuthenticationException e){
					otherAuthDetails.setVisible(true);
					otherAuthRememberMe.setVisible(true);
				}
				if(subscription != null){
					subscription.setSelected(true);
					subscriptionsWithoutInstitutionalDataProvider.addTemporaryFeedSubscription(subscription);
					setNewSubscribedUrl("");
					setUsername("");
					setPassword("");
				}
				super.onSubmit();
			}
		};
		options.add(subscribe);
		
		subscriptionsWithoutInstitutionalDataProvider = new SubscriptionsDataProvider(SubscriptionsDataProvider.MODE_ALL_NON_INSTITUTIONAL);
		if(previousUserSubscriptions == null) {
			previousUserSubscriptions = getDeepCopy(subscriptionsWithoutInstitutionalDataProvider.getFeedSubscriptions());
		}
		DataView otherView = new DataView("otherFeeds", subscriptionsWithoutInstitutionalDataProvider) {
			private static final long	serialVersionUID	= 1L;

			@Override
			protected void populateItem(Item item) {
				FeedSubscription subscription = (FeedSubscription) item.getModelObject();
				item.add(new ExternalImage("iconUrl", subscription.getIconUrl()));
				//item.add(new Label("title", subscription.getTitle()));
				item.add(new ExternalLink("title", subscription.getUrl(), subscription.getTitle()));
				item.add(new CheckBox("selected", new PropertyModel(subscription, "selected")));
			}
		};
		options.add(otherView);

		// Bottom Buttons
		Button save = new Button("save") {
			private static final long	serialVersionUID	= 1L;

			@Override
			public void onSubmit() {
				saveSubscriptions();
				setResponsePage(MainPage.class);
				super.onSubmit();
			}
		};
		save.setDefaultFormProcessing(true);
		options.add(save);
		Button cancel = new Button("cancel") {
			private static final long	serialVersionUID	= 1L;

			@Override
			public void onSubmit() {
				setResponsePage(MainPage.class);
				super.onSubmit();
			}
		};
		cancel.setDefaultFormProcessing(false);
		options.add(cancel);

		add(options);
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		response.renderJavascriptReference("/sakai-feeds-tool/js/common.js");
		response.renderJavascriptReference("/library/js/jquery.js");
		super.renderHead(response);
	}

	public List<String> getAggregateOptions() {
		List<String> options = new ArrayList<String>();
		options.add(String.valueOf(AggregateFeedOptions.TITLE_DISPLAY_DEFAULT));
		options.add(String.valueOf(AggregateFeedOptions.TITLE_DISPLAY_NONE));
		options.add(String.valueOf(AggregateFeedOptions.TITLE_DISPLAY_CUSTOM));
		return options;
	}
	
	public void setAggregateOptionsGroup(String s) {
		aggregateOptions.setTitleDisplayOption(Integer.parseInt(s));
	}
	
	public String getAggregateOptionsGroup() {
		return String.valueOf(aggregateOptions.getTitleDisplayOption());
		//return "0";
	}
	
	public String getAggregateCustomTitle() {
		String t = aggregateOptions.getCustomTitle();
		return t != null && !t.equals("null")? t : "";
	}
	
	public void setAggregateCustomTitle(String title) {
		aggregateOptions.setCustomTitle(title);
	}

	private void saveSubscriptions() {
		// Log actions
		logSubscribeActions();
		
		// Institutional feeds
		Set<FeedSubscription> subscribed = new LinkedHashSet<FeedSubscription>();
		for(FeedSubscription subscription : allInstitutionalDataProvider.getFeedSubscriptions()) {
			if(subscription.isSelected()){
				subscribed.add(subscription);
			}
		}
		// Other feeds
		for(FeedSubscription subscription : subscriptionsWithoutInstitutionalDataProvider.getFeedSubscriptions()) {
			if(subscription.isSelected()) {
				subscribed.add(subscription);
			}
		}
		facade.getFeedsService().setSubscribedFeeds(subscribed);
		facade.getFeedsService().setAggregateFeeds(isAggregate());
		facade.getFeedsService().setAggregateFeedsOptions(aggregateOptions);
		facade.getFeedsService().setSavedCredentials(savedCredentials);
		facade.getFeedsService().loadCredentials();
	}

	private void logSubscribeActions() {
		// log unsubscriptions: Institutional
		for(FeedSubscription fs1 : previousInstitutionalSubscriptions) {
			boolean unsubscribed = false;
			for(FeedSubscription fs2 : allInstitutionalDataProvider.getFeedSubscriptions()) {
				if(fs1.getUrl().equals(fs2.getUrl()) && fs1.isSelected() && !fs2.isSelected()) {
					unsubscribed = true;
					continue;
				}
			}
			if(unsubscribed)
				facade.getFeedsService().logEvent(FeedsService.LOG_EVENT_UNSUBSCRIBE_INSTITUTIONAL, fs1, true);			
		}		
		// log unsubscriptions: User
		for(FeedSubscription fs1 : previousUserSubscriptions) {
			boolean found = false;
			boolean unsubscribed = false;
			for(FeedSubscription fs2 : subscriptionsWithoutInstitutionalDataProvider.getFeedSubscriptions()) {
				if(fs1.getUrl().equals(fs2.getUrl())) {
					found = true;
					if(fs1.isSelected() && !fs2.isSelected()) {
						unsubscribed = true;
					}
					continue;
				}
			}
			if(!found || unsubscribed)
				facade.getFeedsService().logEvent(FeedsService.LOG_EVENT_UNSUBSCRIBE_USER, fs1, true);				
		}

		
		// log subscriptions: Institutional
		for(FeedSubscription fs1 : allInstitutionalDataProvider.getFeedSubscriptions()) {
			boolean subscribed = false;
			for(FeedSubscription fs2 : previousInstitutionalSubscriptions) {
				if(fs1.getUrl().equals(fs2.getUrl()) && fs1.isSelected() && !fs2.isSelected()) {
					subscribed = true;
					continue;
				}
			}
			if(subscribed)
				facade.getFeedsService().logEvent(FeedsService.LOG_EVENT_SUBSCRIBE_INSTITUTIONAL, fs1, true);			
		}		
		// log subscriptions: User
		for(FeedSubscription fs1 : subscriptionsWithoutInstitutionalDataProvider.getFeedSubscriptions()) {
			boolean found = false;
			boolean subscribed = false;
			for(FeedSubscription fs2 : previousUserSubscriptions) {
				if(fs1.getUrl().equals(fs2.getUrl())) {
					found = true;
					if(fs1.isSelected() && !fs2.isSelected()) {
						subscribed = true;
					}
					continue;
				}
			}
			if(!found || subscribed)
				facade.getFeedsService().logEvent(FeedsService.LOG_EVENT_SUBSCRIBE_USER, fs1, true);			
		}
	}

	public String getNewSubscribedUrl() {
		return newSubscribedUrl;
	}

	public void setNewSubscribedUrl(String url) {
		newSubscribedUrl = url;
	}
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isRememberMe() {
		return rememberMe;
	}

	public void setRememberMe(boolean rememberMe) {
		this.rememberMe = rememberMe;
	}

	public boolean isAggregate() {
		return aggregate;
	}

	public void setAggregate(boolean aggregate) {
		this.aggregate = aggregate;
	}
	
	public String getDefaultTitle() {
		return new StringResourceModel("aggregate.title", this, null).getString();
	}

	private FeedSubscription urlToFeedSubscription(String url, String username, String password) throws FeedAuthenticationException {
		if(url == null || url.trim().equals(""))
			return null;
		FeedSubscription feedSubscription = null;
		try{
			URL _url = new URL(url);
			if(username != null && !username.trim().equals("")){
				facade.getFeedsService().addCredentials(_url, authenticationRealm, username, password);
			}
			feedSubscription = facade.getFeedsService().getFeedSubscriptionFromFeedUrl(url, true);
			if(isRememberMe() && username != null && !username.trim().equals("")){
				SavedCredentials newCrd = facade.getFeedsService().newSavedCredentials(_url, authenticationRealm, username, password);
				// remove overrided credentials
				Set<SavedCredentials> toRemove = new HashSet<SavedCredentials>();
				for(SavedCredentials saved : savedCredentials){
					try{
						if(saved.getUrl().toURI().equals(newCrd.getUrl().toURI()) && saved.getRealm().equals(newCrd.getRealm()))
							toRemove.add(saved);
					}catch(Exception e){
						LOG.warn("Unable to compare URLs: "+saved.getUrl().toExternalForm()+" with "+newCrd.getUrl().toExternalForm(), e);
					}
				}
				savedCredentials.removeAll(toRemove);
				// add new credential
				savedCredentials.add(newCrd);
			}
		}catch(FeedAuthenticationException e) {
			feedback.error(new StringResourceModel("err.reqauth", this, null).getString());
			authenticationRealm = e.getRealm();
			System.out.println("Realm: "+e.getRealm());
			e.printStackTrace();
			throw e;
		}catch(IllegalArgumentException e){
			feedback.error((String) new ResourceModel("err.subscribing").getObject());
			e.printStackTrace();
		}catch(MalformedURLException e){
			feedback.error((String) new ResourceModel("err.malformed").getObject());
			e.printStackTrace();
		}catch(SSLHandshakeException e){
			feedback.error((String) new ResourceModel("err.ssl").getObject());
			e.printStackTrace();
		}catch(IOException e){
			feedback.error((String) new ResourceModel("err.io").getObject());
			e.printStackTrace();
		}catch(InvalidFeedException e){
			feedback.error((String) new ResourceModel("err.invalid_feed").getObject());
			e.printStackTrace();
		}catch(FetcherException e){
			if(e.getHttpCode() == 403)
				feedback.error((String) new ResourceModel("err.forbidden").getObject());
			else
				feedback.error((String) new ResourceModel("err.no_fetch").getObject());
			e.printStackTrace();
		}catch(Exception e){
			feedback.error((String) new ResourceModel("err.subscribing").getObject());
			e.printStackTrace();
		}
		return feedSubscription;
	}
	
	private Set<FeedSubscription> getDeepCopy(Set<FeedSubscription> set) {
		Set<FeedSubscription> result = new HashSet<FeedSubscription>();
		for(FeedSubscription fs : set)
			result.add(fs.clone());
		return result;
	}
}
