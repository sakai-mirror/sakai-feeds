package org.sakaiproject.feeds.tool.wicket.panels;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.feeds.api.SavedCredentials;
import org.sakaiproject.feeds.tool.facade.SakaiFacade;
import org.sakaiproject.feeds.tool.wicket.dataproviders.FeedDataProvider;


/**
 * @author Nuno Fernandes
 */
public class AuthenticationPanel extends Panel {
	private static final long		serialVersionUID	= 1L;

	@SpringBean
	private transient SakaiFacade	facade;
	private Set<SavedCredentials>	savedCredentials	= null;

	private Component				componentToRefresh;
	private FeedDataProvider		feedDataProvider;
	private String					username;
	private String					password;
	private boolean					rememberMe;

	public AuthenticationPanel(String id, FeedDataProvider feedDataProvider, Component componentToRefresh) {
		super(id);
		this.feedDataProvider = feedDataProvider;
		this.componentToRefresh = componentToRefresh;
		
		if(savedCredentials == null)
			savedCredentials = facade.getFeedsService().getSavedCredentials();
		
		init();
	}

	private void init() {
		setModel(new CompoundPropertyModel(this));

		//WebMarkupContainer divAuthPanelWrapper = new WebMarkupContainer("divAuthPanelWrapper");
		WebMarkupContainer divAuthPanel = new WebMarkupContainer("divAuthPanel");
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
				if(getUsername() != null && !getUsername().trim().equals("")){
					try{
						url = new URL(feedDataProvider.getFeedSubscription().getUrl());
						facade.getFeedsService().addCredentials(url, feedDataProvider.getAuthenticationRealm(), username, password);
						System.out.println("AuthenticationPanel: " + url.toString() + ", " + feedDataProvider.getAuthenticationRealm() + ", " + getUsername() + ", " + getPassword());
					}catch(Exception e){
						e.printStackTrace();
					}
				}
				feedDataProvider.getFeed();
				boolean authSuccess = !feedDataProvider.requireAuthentication();
				if(authSuccess && isRememberMe()) {
					SavedCredentials newCrd = facade.getFeedsService().newSavedCredentials(url, feedDataProvider.getAuthenticationRealm(), username, password);
					// remove overrided credentials
					Set<SavedCredentials> toRemove = new HashSet<SavedCredentials>();
					for(SavedCredentials saved : savedCredentials){
						if(saved.getUrl().equals(newCrd.getUrl()) && saved.getRealm().equals(newCrd.getRealm()))
							toRemove.add(saved);
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
//		divAuthPanelWrapper.add(divAuthPanel);
//		add(divAuthPanelWrapper);
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