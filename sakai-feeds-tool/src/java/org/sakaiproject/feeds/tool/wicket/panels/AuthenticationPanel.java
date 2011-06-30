package org.sakaiproject.feeds.tool.wicket.panels;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.feeds.api.SavedCredentials;
import org.sakaiproject.feeds.api.exception.FeedAuthenticationException;
import org.sakaiproject.feeds.tool.facade.Locator;
import org.sakaiproject.feeds.tool.wicket.dataproviders.FeedDataProvider;


/**
 * @author Nuno Fernandes
 */
public abstract class AuthenticationPanel extends Panel {
	private static final long		serialVersionUID		= 1L;
	private static Log				LOG						= LogFactory.getLog(AuthenticationPanel.class);

	private Set<SavedCredentials>	savedCredentials		= null;

	private FeedDataProvider		feedDataProvider;
	private String					authenticationRealm		= null;
	private String					authenticationScheme	= null;
	private String					username;
	private String					password;
	private boolean					rememberMe;
	private String					feedUrl					= null;

	public AuthenticationPanel(String id, FeedDataProvider feedDataProvider, String feedUrl) {
		this(id, feedDataProvider, feedUrl, null, null);
	}
	
	public AuthenticationPanel(String id, String feedUrl, String realm, String scheme) {
		this(id, null, feedUrl, realm, scheme);
	}
	
	public AuthenticationPanel(String id, FeedDataProvider feedDataProvider, String feedUrl, String realm, String scheme) {
		super(id);
		this.feedDataProvider = feedDataProvider;
		this.feedUrl = feedUrl;
		this.authenticationRealm = realm;
		this.authenticationScheme = scheme;
		
		if(savedCredentials == null)
			savedCredentials = Locator.getFacade().getFeedsService().getSavedCredentials();
		
		init(); // NOPMD by Nuno Fernandes on 12-09-2008 9:24
	}

	final void init() {
		setDefaultModel(new CompoundPropertyModel(this));

		WebMarkupContainer divAuthPanel = new WebMarkupContainer("divAuthPanel");
		
		Label message = new Label("message", "");
		if(feedUrl == null) {
			message.setDefaultModel(new StringResourceModel("provide.auth.details", this, null));
		}else{
			message.setDefaultModel(new StringResourceModel("provide.auth.details2", this, new Model(this)));
		}
		divAuthPanel.add(message);
		
		Form form = new Form("form");

		final TextField usernameTF = new TextField("username");
		form.add(usernameTF);

		final PasswordTextField passwordTF = new PasswordTextField("password");
		form.add(passwordTF);
		
		final CheckBox rememberMeChk = new CheckBox("rememberMe");
		form.add(rememberMeChk);

		final Component thisComponent = this;
		IndicatingAjaxButton ok = new IndicatingAjaxButton("ok", form) {
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form form) {
				URI uri = null;
				
				// get updated auth realm and scheme
				if(feedDataProvider != null) {
					authenticationRealm = feedDataProvider.getAuthenticationRealm();
					authenticationScheme = feedDataProvider.getAuthenticationScheme();
				}
				
				// add new provided credentials
				if(getUsername() != null && !getUsername().trim().equals("")){
					try{
						uri = new URI(feedUrl);
						Locator.getFacade().getFeedsService().addCredentials(uri, authenticationRealm, username, password, authenticationScheme);
					}catch(Exception e){
						e.printStackTrace();
					}
				}

				// check with new credentials
				boolean authSuccess = areCredentialsOk();
				if(authSuccess && isRememberMe()) {
					SavedCredentials newCrd = Locator.getFacade().getFeedsService().newSavedCredentials(uri, authenticationRealm, username, password, authenticationScheme);
					// remove overrided credentials
					Set<SavedCredentials> toRemove = new HashSet<SavedCredentials>();
					for(SavedCredentials saved : savedCredentials){
						try{
							if(saved.getUri().equals(newCrd.getUri()) && saved.getRealm().equals(newCrd.getRealm()))
								toRemove.add(saved);
						}catch(Exception e){
							LOG.warn("Unable to compare URLs: "+saved.getUri()+" with "+newCrd.getUri(), e);
						}
					}
					savedCredentials.removeAll(toRemove);
					// add new credentials
					savedCredentials.add(newCrd);
					Locator.getFacade().getFeedsService().setSavedCredentials(savedCredentials);
					Locator.getFacade().getFeedsService().loadCredentials();
				}
				
				//
				if(authSuccess) {
					onAuthSuccess(target);
				}else{
					onAuthFail(target);
				}
				thisComponent.setVisible(!authSuccess);				
			}
		};
		form.add(ok);

		divAuthPanel.add(form);
		add(divAuthPanel);
	}

	/**
	 * Reload feed and checks if it stills require authentication.
	 * @return True it it stills require authentication, false if supplied credentials are ok.
	 */
	final boolean areCredentialsOk() {
		if(feedDataProvider != null) {
			if(!feedDataProvider.getFeedSubscription().isAggregateMultipleFeeds()) {
				feedDataProvider.getFeed();
			}else {
				feedDataProvider.getAggregatedFeeds();
			}
			return !feedDataProvider.requireAuthentication()
					&& authenticationRealm.equals(feedDataProvider.getAuthenticationRealm()) 
					&& feedUrl.equals(feedDataProvider.getAffectedFeed());
		}else{
			EntityReference ref = Locator.getFacade().getFeedsService().getEntityReference(getFeedUrl());
			try{
				Locator.getFacade().getFeedsService().getFeed(ref, true);
			}catch(FeedAuthenticationException e){
				return false;
			}catch(Exception e){
				LOG.warn("An error occurred while checking credentials for feed "+getFeedUrl(), e);
			}
			return true;
		}
	}
	
	public abstract void onAuthSuccess(AjaxRequestTarget target);
	public abstract void onAuthFail(AjaxRequestTarget target);


	private String getUsername() {
		return username;
	}

	private void setUsername(String username) {
		this.username = username;
	}

	private String getPassword() {
		return password;
	}

	private void setPassword(String password) {
		this.password = password;
	}

	private boolean isRememberMe() {
		return rememberMe;
	}

	private void setRememberMe(boolean rememberMe) {
		this.rememberMe = rememberMe;
	}

	public String getFeedUrl() {
		return feedUrl;
	}

	public void setFeedUrl(String feedUrl) {
		this.feedUrl = feedUrl;
	}

	public String getAuthenticationRealm() {
		return authenticationRealm;
	}

	public void setAuthenticationRealm(String authenticationRealm) {
		this.authenticationRealm = authenticationRealm;
	}

	public String getAuthenticationScheme() {
		return authenticationScheme;
	}

	public void setAuthenticationScheme(String authenticationScheme) {
		this.authenticationScheme = authenticationScheme;
	}
	
}
