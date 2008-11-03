package org.sakaiproject.feeds.tool.wicket.pages;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
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
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
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
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
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
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.request.target.basic.EmptyRequestTarget;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.lang.Bytes;
import org.apache.xmlbeans.XmlException;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.feeds.api.AggregateFeedOptions;
import org.sakaiproject.feeds.api.FeedSubscription;
import org.sakaiproject.feeds.api.FeedsService;
import org.sakaiproject.feeds.api.SavedCredentials;
import org.sakaiproject.feeds.api.exception.FeedAuthenticationException;
import org.sakaiproject.feeds.api.exception.FetcherException;
import org.sakaiproject.feeds.api.exception.InvalidFeedException;
import org.sakaiproject.feeds.opml.OpmlDocument;
import org.sakaiproject.feeds.opml.BodyDocument.Body;
import org.sakaiproject.feeds.opml.HeadDocument.Head;
import org.sakaiproject.feeds.opml.OpmlDocument.Opml;
import org.sakaiproject.feeds.opml.OutlineDocument.Outline;
import org.sakaiproject.feeds.tool.facade.SakaiFacade;
import org.sakaiproject.feeds.tool.wicket.components.AjaxIndicator;
import org.sakaiproject.feeds.tool.wicket.components.CollapsiblePanel;
import org.sakaiproject.feeds.tool.wicket.components.ExternalImage;
import org.sakaiproject.feeds.tool.wicket.dataproviders.SubscriptionsDataProvider;
import org.sakaiproject.feeds.tool.wicket.model.FeedErrorModel;
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
	private boolean						validateFeedSubscription	= true;
	private boolean						validateImportedFeeds		= true;
	private boolean						aggregate					= false;
	private AggregateFeedOptions		aggregateOptions			= null;

	private String						authenticationRealm;
	private String						authenticationScheme;
	private Set<SavedCredentials>		savedCredentials			= null;
	
	private Set<FeedSubscription>		previousInstitutionalSubscriptions = null;
	private Set<FeedSubscription>		previousUserSubscriptions 	= null;

	private final Component 			component 					= this;
	private final WebMarkupContainer 	otherFeedsHolder;
	
	private transient OpmlUtil 			opmlUtil 					= new OpmlUtil();

	public SubscriptionsPage() {
		
		// credentials
		if(savedCredentials == null)
			savedCredentials = facade.getFeedsService().getSavedCredentials();
		
		setAggregate(facade.getFeedsService().isAggregateFeeds());
		aggregateOptions = facade.getFeedsService().getAggregateFeedsOptions();
		
		feedback = new CSSFeedbackPanel("messages");
		feedback.setOutputMarkupId(true);
		add(feedback);

		Form options = new Form("subscriptions");
		setModel(new CompoundPropertyModel(this));

		
		// -- Feed subscriptions --
		CollapsiblePanel subscPanel = new CollapsiblePanel("subscPanel", new StringResourceModel("subsc.title", this, null).getString(), true);
		options.add(subscPanel);		
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
		subscPanel.add(institutionalWrapper);
		

		// Other subscriptions
		otherFeedsHolder = new WebMarkupContainer("otherFeedsHolder");
		otherFeedsHolder.setOutputMarkupId(true);
		subscPanel.add(otherFeedsHolder);
		
		final FormComponent url = new TextField("newSubscribedUrl");
		otherFeedsHolder.add(url);
		
		final CheckBox validateFeedSubscriptionChkBox = new CheckBox("validateFeedSubscription");
		otherFeedsHolder.add(validateFeedSubscriptionChkBox);

		// other Auth details
		final WebMarkupContainer otherAuthDetails = new WebMarkupContainer("other.auth.details");
		final TextField username = new TextField("username");
		otherAuthDetails.add(username);
		final PasswordTextField password = new PasswordTextField("password");
		otherAuthDetails.add(password);
		otherAuthDetails.setVisible(false);
		otherFeedsHolder.add(otherAuthDetails);

		// other Auth details - remember me
		final WebMarkupContainer otherAuthRememberMe = new WebMarkupContainer("other.auth.rememberme");
		final CheckBox rememberMeChk = new CheckBox("rememberMe");
		otherAuthRememberMe.add(rememberMeChk);
		otherAuthRememberMe.setVisible(false);
		otherFeedsHolder.add(otherAuthRememberMe);		
		
		// Buttons
		IndicatingAjaxButton subscribe = new IndicatingAjaxButton("subscribe", options) {
			private static final long	serialVersionUID	= 1L;
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form form) {
				FeedSubscription subscription = null;
				try{
					subscription = urlToFeedSubscription(getNewSubscribedUrl(), getUsername(), getPassword(), isValidateFeedSubscription());
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

				target.addComponent(feedback);
				target.addComponent(otherFeedsHolder);
				target.appendJavascript("setMainFrameHeightNoScroll(window.name);");
			}
		};
		otherFeedsHolder.add(subscribe);
		
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
				item.add(new ExternalLink("title", subscription.getUrl(), subscription.getTitle()));
				item.add(new CheckBox("selected", new PropertyModel(subscription, "selected")));
			}
		};
		otherFeedsHolder.add(otherView);
		
		
		
		// Aggregation
		CollapsiblePanel aggrPanel = new CollapsiblePanel("aggrPanel", new StringResourceModel("aggr.title", this, null).getString(), false);
		options.add(aggrPanel);
		CheckBox aggr = new CheckBox("aggregate");
		aggr.add(new AttributeModifier("onclick", true, new Model("$('.aggregateOptionsClass').toggle();$('.aggregateCustomTitleClass').toggle(); setMainFrameHeightNoScroll(window.name);")));
		aggrPanel.add(aggr);
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
		aggrPanel.add(aggregateCustomTitle);
		aggrPanel.add(aggregateOptions);
		
		
		
		// Import/Export
		options.setMultiPart(true);
		CollapsiblePanel importExportPanel = new CollapsiblePanel("importExportPanel", new StringResourceModel("impexp.title", this, null).getString(), false);
		options.add(importExportPanel);
		final FileUploadField fileUploadField = new FileUploadField("fileImport");
		importExportPanel.add(fileUploadField);
		int contentUploadMax = ServerConfigurationService.getInt("content.upload.max", 20);
		options.setMaxSize(Bytes.megabytes(contentUploadMax));
		final AjaxIndicator importIndicator = new AjaxIndicator("importIndicator");
		importExportPanel.add(importIndicator);		
		final CheckBox validateImportedFeedsChkBox = new CheckBox("validateImportedFeeds");
		importExportPanel.add(validateImportedFeedsChkBox);
		Button importBt = new Button("import") {
			private static final long	serialVersionUID	= 1L;

			@Override
			public void onSubmit() {
				final FileUpload upload = fileUploadField.getFileUpload();
				if(upload != null){
					try{
						InputStream inputstream = upload.getInputStream();
						opmlUtil.importFromOpml(inputstream, upload.getClientFileName());
					}catch(IOException e){
						LOG.error("Unable to process uploaded file: " + upload.getClientFileName(), e);
					}
				}
				super.onSubmit();
			}
		};
		importBt.add(new AttributeAppender("onclick", new Model("$('#"+importIndicator.getMarkupId()+"').fadeIn();"), ";"));
		importExportPanel.add(importBt);
		
		
		Button exportBt = new Button("export") {
			private static final long	serialVersionUID	= 1L;
			@Override
			public void onSubmit() {
				RequestCycle.get().setRequestTarget(EmptyRequestTarget.getInstance());
				WebResponse response = (WebResponse) getResponse();
				response.setContentType("xml");
				response.setAttachmentHeader("subscriptions.opml");
				response.setHeader("Cache-Control", "max-age=0");

				OutputStream output = getResponse().getOutputStream();
				opmlUtil.exportToOpml(allInstitutionalDataProvider.getFeedSubscriptions(), subscriptionsWithoutInstitutionalDataProvider.getFeedSubscriptions(), output);

				super.onSubmit();
			}
		};
		importExportPanel.add(exportBt);
		
		

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
		response.renderJavascriptReference("/library/js/jquery.js");
		response.renderJavascriptReference("/sakai-feeds-tool/js/common.js");
		super.renderHead(response);
	}

	private List<String> getAggregateOptions() {
		List<String> options = new ArrayList<String>();
		options.add(String.valueOf(AggregateFeedOptions.TITLE_DISPLAY_DEFAULT));
		options.add(String.valueOf(AggregateFeedOptions.TITLE_DISPLAY_NONE));
		options.add(String.valueOf(AggregateFeedOptions.TITLE_DISPLAY_CUSTOM));
		return options;
	}
	
	public void setAggregateOptionsGroup(String optionNumber) {
		try{
			aggregateOptions.setTitleDisplayOption(Integer.parseInt(optionNumber));
		}catch(Exception e) {
			aggregateOptions.setTitleDisplayOption(AggregateFeedOptions.TITLE_DISPLAY_NONE);
		}
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

	public boolean isValidateFeedSubscription() {
		return validateFeedSubscription;
	}

	public void setValidateFeedSubscription(boolean validateFeeds) {
		this.validateFeedSubscription = validateFeeds;
	}

	public boolean isValidateImportedFeeds() {
		return validateImportedFeeds;
	}

	public void setValidateImportedFeeds(boolean validateImportedFeeds) {
		this.validateImportedFeeds = validateImportedFeeds;
	}

	private boolean isAggregate() {
		return aggregate;
	}

	private void setAggregate(boolean aggregate) {
		this.aggregate = aggregate;
	}
	
	public String getDefaultTitle() {
		return new StringResourceModel("aggregate.title", this, null).getString();
	}

	private FeedSubscription urlToFeedSubscription(String url, String username, String password, boolean validate) throws FeedAuthenticationException {
		if(url == null || url.trim().equals(""))
			return null;
		FeedSubscription feedSubscription = null;
		try{
			URI uri = new URI(url);
			if(username != null && !username.trim().equals("")){
				facade.getFeedsService().addCredentials(uri, authenticationRealm, username, password, authenticationScheme);
			}
			feedSubscription = facade.getFeedsService().getFeedSubscriptionFromFeedUrl(url, validate);
			if(isRememberMe() && username != null && !username.trim().equals("")){
				SavedCredentials newCrd = facade.getFeedsService().newSavedCredentials(uri, authenticationRealm, username, password, authenticationScheme);
				// remove overrided credentials
				Set<SavedCredentials> toRemove = new HashSet<SavedCredentials>();
				for(SavedCredentials saved : savedCredentials){
					try{
						if(saved.getUri().equals(newCrd.getUri()) && saved.getRealm().equals(newCrd.getRealm()))
							toRemove.add(saved);
					}catch(Exception e){
						LOG.warn("Unable to compare URLs: "+saved.getUri().toString()+" with "+newCrd.getUri().toString(), e);
					}
				}
				savedCredentials.removeAll(toRemove);
				// add new credential
				savedCredentials.add(newCrd);
			}
		}catch(FeedAuthenticationException e) {
			setError("err.reqauth", url, e);
			authenticationRealm = e.getRealm();
			authenticationScheme = e.getScheme();
			throw e;
		}catch(IllegalArgumentException e){
			setError("err.subscribing", url, e);
		}catch(MalformedURLException e){
			setError("err.malformed", url, e);
		}catch(SSLHandshakeException e){
			setError("err.ssl", url, e);
		}catch(IOException e){
			setError("err.io", url, e);
		}catch(InvalidFeedException e){
			setError("err.invalid_feed", url, e);
		}catch(FetcherException e){
			if(e.getHttpCode() == 403)
				setError("err.forbidden", url, e);
			else
				setError("err.no_fetch", url, e);
		}catch(Exception e){
			setError("err.subscribing", url, e);
		}
		return feedSubscription;
	}
	
	
	
	private Set<FeedSubscription> getDeepCopy(Set<FeedSubscription> set) {
		Set<FeedSubscription> result = new HashSet<FeedSubscription>();
		for(FeedSubscription fs : set) {
			try{
				result.add((FeedSubscription) fs.clone());
			}catch(CloneNotSupportedException e){
				LOG.warn("Ops... FeedSubscription is not clonable?!?", e);
			}
		}
		return result;
	}
    
	private void setError(String key, String url, Exception e) {
		FeedErrorModel errorModel = new FeedErrorModel(key, url, e);
		String errorMessage = null;
		try{
			errorMessage = new StringResourceModel(errorModel.getErrorMessageKey(), this, new Model(errorModel)).getString();
		}catch(Exception e1){
			LOG.warn("Unable to get text for bundle key '" + errorModel.getErrorMessageKey() + "'");
			errorMessage = errorModel.getErrorMessageKey();
		}
		feedback.error(errorMessage);
		LOG.warn(errorMessage, errorModel.getException());
	}
    
	
	/**
	 * Utility class for Import/Export of OPML files
	 * @author Nuno Fernandes
	 */
	class OpmlUtil {

		/** Get feed subscriptions from an OPML file. */
		public Set<FeedSubscription> importFromOpml(InputStream inputstream, String fileName) {
			Set<FeedSubscription> subscriptions = new HashSet<FeedSubscription>();
			OpmlDocument opmlDoc = null;
			
			// Import file
			try{
				opmlDoc = OpmlDocument.Factory.parse(inputstream);
			}catch(XmlException e){
				LOG.error("Unable to parse opml file '"+fileName+"'", e);
			}catch(Exception e){
				LOG.error("Unable to read opml file '"+fileName+"'", e);
			}
			
			if(opmlDoc != null) {
				// Extract feed urls
				try{
					Outline[] outline = opmlDoc.getOpml().getBody().getOutlineArray();
					for(int i=0; i<outline.length; i++) {
						String category = null;
						String feedUrl = outline[i].getXmlUrl();
						if(feedUrl == null) {
							category = outline[i].getText();
							Outline[] outline2 = outline[i].getOutlineArray();
							for(int j=0; j<outline2.length; j++) {
								feedUrl = outline2[j].getXmlUrl();
								try{
									subscriptions.addAll(subscribeToFeedUrl(feedUrl));
								}catch(NullPointerException e1) { /* ignore it */ }
							}
						}else{
							try{
								subscriptions.addAll(subscribeToFeedUrl(feedUrl));
							}catch(NullPointerException e1) { /* ignore it */ }
						}
						
					}
				}catch(Exception e) {
					LOG.error("Unable to process opml file '"+fileName+"'", e);
				}
				
			}else{
				LOG.debug("OPML Doc is null!!");
			}

			return subscriptions;
		}
		
		/** Get feed subscriptions from an OPML file. 
		 * @param outputStream */
		public void exportToOpml(Set<FeedSubscription> institutionalSubscriptions,
				Set<FeedSubscription> userSubscriptions, OutputStream outputStream) {
			try{
				OpmlDocument opmlDoc = OpmlDocument.Factory.newInstance();
				Opml opml = opmlDoc.addNewOpml();
				Head opmlHead = opml.addNewHead();
				// Use site title as opml title
				String context = facade.getToolManager().getCurrentPlacement().getContext();
				opmlHead.setTitle(facade.getSiteService().getSiteDisplay(context));
				Body opmlBody = opml.addNewBody();
				
				if(institutionalSubscriptions != null) {
					for(FeedSubscription fs : institutionalSubscriptions) {
						Outline ol = opmlBody.addNewOutline();
						ol.setText(fs.getTitle());
						ol.setXmlUrl(fs.getUrl());
					}
				}
				if(userSubscriptions != null) {
					for(FeedSubscription fs : userSubscriptions) {
						Outline ol = opmlBody.addNewOutline();
						ol.setText(fs.getTitle());
						ol.setXmlUrl(fs.getUrl());
					}
				}
				
				opmlDoc.save(outputStream);
				
				//opmlDoc.save(new File(""));
			}catch(Exception e) {
				LOG.error("Unable to export to opml file", e);
			}
		}
	
		private Set<FeedSubscription> subscribeToFeedUrl(String feedUrl) {
			Set<FeedSubscription> subscriptions = new HashSet<FeedSubscription>();
			try{
				FeedSubscription fs = urlToFeedSubscription(feedUrl, null, null, isValidateImportedFeeds());
				if(fs != null) {
					subscriptions.add(fs);
					fs.setSelected(true);
					subscriptionsWithoutInstitutionalDataProvider.addTemporaryFeedSubscription(fs);
				}
			}catch(FeedAuthenticationException e){
				LOG.warn("Authentication for opml files not yet implemented!");
			}
			return subscriptions;
		}
	}
}
