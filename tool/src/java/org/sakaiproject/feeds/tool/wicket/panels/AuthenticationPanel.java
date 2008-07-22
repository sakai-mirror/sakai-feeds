package org.sakaiproject.feeds.tool.wicket.panels;

import java.net.URL;
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
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.feeds.api.SavedCredentials;
import org.sakaiproject.feeds.tool.facade.SakaiFacade;
import org.sakaiproject.feeds.tool.wicket.dataproviders.FeedDataProvider;


/**
 * @author Nuno Fernandes
 */
public class AuthenticationPanel extends Panel {
	private static final long		serialVersionUID	= 1L;
	private static Log				LOG	= LogFactory.getLog(AuthenticationPanel.class);

	@SpringBean
	private transient SakaiFacade	facade;
	private Set<SavedCredentials>	savedCredentials	= null;

	private Component				componentToRefresh;
	private FeedDataProvider		feedDataProvider;
	private String					username;
	private String					password;
	private boolean					rememberMe;
	private String					affectedFeed 		= null;

	public AuthenticationPanel(String id, FeedDataProvider feedDataProvider, Component componentToRefresh, String affectedFeed) {
		super(id);
		this.feedDataProvider = feedDataProvider;
		this.componentToRefresh = componentToRefresh;
		this.affectedFeed = affectedFeed;
		
		if(savedCredentials == null)
			savedCredentials = facade.getFeedsService().getSavedCredentials();
		
		init();
	}

	private void init() {
		setModel(new CompoundPropertyModel(this));

		WebMarkupContainer divAuthPanel = new WebMarkupContainer("divAuthPanel");
		
		Label message = new Label("message", "");
		if(affectedFeed == null) {
			message.setModel(new StringResourceModel("provide.auth.details", this, null));
		}else{
			message.setModel(new StringResourceModel("provide.auth.details2", this, new Model(this)));
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
				URL url = null;
				String realm = feedDataProvider.getAuthenticationRealm();
				String scheme = feedDataProvider.getAuthenticationScheme();
				
				if(getUsername() != null && !getUsername().trim().equals("")){
					try{
						url = new URL(affectedFeed);
						facade.getFeedsService().addCredentials(url, realm, username, password, scheme);
					}catch(Exception e){
						e.printStackTrace();
					}
				}
				if(!feedDataProvider.getFeedSubscription().isAggregateMultipleFeeds()) {
					feedDataProvider.getFeed();
				}else {
					feedDataProvider.getAggregatedFeeds();
				}
				boolean authSuccess = !feedDataProvider.requireAuthentication();
				if( 
					(authSuccess 
							|| realm.equals(feedDataProvider.getAuthenticationRealm()) && affectedFeed.equals(feedDataProvider.getAffectedFeed()) ) 
					&& isRememberMe()) {
					SavedCredentials newCrd = facade.getFeedsService().newSavedCredentials(url, realm, username, password, scheme);
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
					facade.getFeedsService().setSavedCredentials(savedCredentials);
					facade.getFeedsService().loadCredentials();
				}
				thisComponent.setVisible(!authSuccess);
				target.addComponent(componentToRefresh);
			}
		};
		form.add(ok);

		divAuthPanel.add(form);
		add(divAuthPanel);
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

}
