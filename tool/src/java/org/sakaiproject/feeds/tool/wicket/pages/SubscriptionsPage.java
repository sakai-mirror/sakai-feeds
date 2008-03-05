package org.sakaiproject.feeds.tool.wicket.pages;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.net.ssl.SSLHandshakeException;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.feeds.api.FeedSubscription;
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

	@SpringBean
	private transient SakaiFacade		facade;
	private SubscriptionsDataProvider	allInstitutionalDataProvider;
	private SubscriptionsDataProvider	subscriptionsWithoutInstitutionalDataProvider;

	private FeedbackPanel				feedback;

	private String						newSubscribedUrl			= "";
	private String						username					= "";
	private String						password					= "";
	private boolean						rememberMe					= true;

	private String						authenticationRealm;
	private Set<SavedCredentials>		savedCredentials			= null;

	public SubscriptionsPage() {
		if(savedCredentials == null)
			savedCredentials = facade.getFeedsService().getSavedCredentials();
		
		feedback = new CSSFeedbackPanel("messages");
		add(feedback);
		// feedback.setOutputMarkupId(true);

		Form options = new Form("subscriptions");
		setModel(new CompoundPropertyModel(this));

		// Institutional subscriptions
		WebMarkupContainer institutionalWrapper = new WebMarkupContainer("institutionalWrapper");
		allInstitutionalDataProvider = new SubscriptionsDataProvider(SubscriptionsDataProvider.MODE_ALL_INSTITUTIONAL);
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

	private void saveSubscriptions() {
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
		facade.getFeedsService().setSavedCredentials(savedCredentials);
		facade.getFeedsService().loadCredentials();
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

	private FeedSubscription urlToFeedSubscription(String url, String username, String password) throws FeedAuthenticationException {
		if(url == null || url.trim().equals(""))
			return null;
		FeedSubscription feedSubscription = null;
		try{
			url = URLDecoder.decode(url, "UTF-8");			
			URL _url = new URL(url);
			if(username != null && !username.trim().equals("")){
				facade.getFeedsService().addCredentials(_url, authenticationRealm, username, password);
			}
			feedSubscription = facade.getFeedsService().getFeedSubscriptionFromFeedUrl(url);
			if(isRememberMe() && username != null && !username.trim().equals("")){
				SavedCredentials newCrd = facade.getFeedsService().newSavedCredentials(_url, authenticationRealm, username, password);
				// remove overrided credentials
				Set<SavedCredentials> toRemove = new HashSet<SavedCredentials>();
				for(SavedCredentials saved : savedCredentials){
					if(saved.getUrl().equals(newCrd.getUrl()) && saved.getRealm().equals(newCrd.getRealm()))
						toRemove.add(saved);
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
}
