package org.sakaiproject.feeds.tool.wicket.panels;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.sakaiproject.feeds.api.FeedEntry;
import org.sakaiproject.feeds.api.FeedEntryEnclosure;
import org.sakaiproject.feeds.api.FeedSubscription;
import org.sakaiproject.feeds.api.ViewOptions;
import org.sakaiproject.feeds.tool.wicket.dataproviders.FeedDataProvider;
import org.sakaiproject.util.Validator;


public class FeedEntriesPanel extends Panel {
	private static final long			serialVersionUID		= 1L;
	private FeedDataProvider			feedDataProvider;
	
	private String						viewDetail				= ViewOptions.DEFAULT_VIEW_DETAIL;
	private Boolean 					forceExternalCheck		= Boolean.FALSE;

	private FeedbackPanel				feedback;
	private final WebMarkupContainer 	feedEntryHolder = new WebMarkupContainer("feedEntryHolder");
	private final WebMarkupContainer 	myJs;
	
	final transient DateTimeFormatter dateTimeFormatterToday = DateTimeFormat.forPattern(", HH:mm").withLocale(getSession().getLocale());
	final transient DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm").withLocale(getSession().getLocale());

	public FeedEntriesPanel(String id, FeedSubscription subscription, final String _viewFilter, final String _viewDetail, final Boolean forceExternalCheck) {
		super(id);
		this.viewDetail = _viewDetail;
		this.forceExternalCheck = forceExternalCheck;
		
		// create the DataView
		final Component component = this;
		feedDataProvider = new FeedDataProvider(subscription, _viewFilter, forceExternalCheck);
		final DataView view = new DataView("feedEntry", feedDataProvider) {
			private static final long	serialVersionUID	= 1L;

			@Override
			protected void populateItem(Item item) {
				final FeedEntry entry = (FeedEntry) item.getModelObject();
				
				// date
				Date feedEntryDate = entry.getPublishedDate();
				Model feedEntryDateModel = null;
				if(feedEntryDate != null) {
					Calendar cal = Calendar.getInstance();
					cal.set(Calendar.HOUR_OF_DAY, 0);
					cal.set(Calendar.MINUTE, 0);
					cal.set(Calendar.SECOND, 0);
					cal.set(Calendar.MILLISECOND, 0);
					Date today00 = cal.getTime(); 
					if(feedEntryDate.after(today00)){
						StringResourceModel todayStr = new StringResourceModel("today", component, null);
						feedEntryDateModel = new Model(todayStr.getString() + dateTimeFormatterToday.print(feedEntryDate.getTime()));
					}else
						feedEntryDateModel = new Model(dateTimeFormatter.print(feedEntryDate.getTime()));
				}else{
					feedEntryDateModel = new Model("");
				}
				Label feedEntryDateLabel = new Label("feedEntryDate", feedEntryDateModel); 
				item.add(feedEntryDateLabel);
				feedEntryDateLabel.setVisible(feedEntryDate != null);
				
				// feed body
				final WebMarkupContainer feedBodyWrapper = new WebMarkupContainer("feedBodyWrapper");
				feedBodyWrapper.setOutputMarkupId(true);
				final WebMarkupContainer feedBody = new WebMarkupContainer("feedBody");
				feedBody.setOutputMarkupId(true);
				// feed body - description
				final Label description = new Label("description", Validator.stripAllNewlines(entry.getDescription())){
					private static final long	serialVersionUID	= 1L;
					@Override
					protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
						String str = getModelObjectAsString();
						str += getLinkChangeJs(getMarkupId(), entry);
						replaceComponentTagBody(markupStream, openTag, str);
					}										
				};
				description.setEscapeModelStrings(false);
				description.setOutputMarkupId(true);
				feedBody.add(description);
				// feed body - contents
				final Label contents = new Label("contents",""){
					private static final long	serialVersionUID	= 1L;
					@Override
					protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
						String str = getModelObjectAsString();
						str += getLinkChangeJs(getMarkupId(), entry);
						replaceComponentTagBody(markupStream, openTag, str);
					}										
				};
				contents.setEscapeModelStrings(false);
				contents.setOutputMarkupId(true);
				contents.setVisible(false);
				contents.setEscapeModelStrings(false);
				feedBody.add(contents);
				
				// read external
				final WebMarkupContainer readExternalWrapper = new WebMarkupContainer("readExternalWrapper");
				final ExternalLink readExternal = new ExternalLink("readExternal", entry.getLink()!=null? entry.getLink():"");
				readExternal.add(new AttributeModifier("style", true, new Model("clear: left; float: left;")));
				readExternalWrapper.setOutputMarkupId(true);
				readExternalWrapper.add(readExternal);
				feedBody.add(readExternalWrapper);
				readExternal.setVisible(entry.getLink()!=null);

				// readmore
				final WebMarkupContainer readMoreWrapper = new WebMarkupContainer("readMoreWrapper");
				final IndicatingAjaxLink readMore = new IndicatingAjaxLink("readMore") {
					private static final long	serialVersionUID	= 1L;
					@Override
					public void onClick(AjaxRequestTarget target) {
						description.setModelObject("");
						String content = Validator.stripAllNewlines(entry.getContent());
						contents.setModelObject(content);
						description.setVisible(false);
						readMoreWrapper.setVisible(false);
						readExternalWrapper.setVisible(false);
						contents.setVisible(true);
						
						target.addComponent(feedBody);
						target.appendJavascript(getJs());
					}					
				};
				readMoreWrapper.setOutputMarkupId(true);
				readMoreWrapper.add(readMore);
				if(entry.getDescription() == null){
					readMoreWrapper.setVisible(false);
					String content = Validator.stripAllNewlines(entry.getContent());
					contents.setModelObject(content);
					description.setVisible(false);
					readMoreWrapper.setVisible(false);
					//readExternalWrapper.setVisible(false);
					contents.setVisible(true);
				}else if(entry.getContent() == null || entry.getContent().trim().equals("") || entry.getContent().trim().equals(entry.getDescription().trim()) ) {
					readMoreWrapper.setVisible(false);
				}
				feedBody.add(readMoreWrapper);
				
				// enclosures
				final List<FeedEntryEnclosure> enclosures = entry.getEnclosures();
				//int enclosuresSize = ViewOptions.VIEW_DETAIL_FULL_ENTRY.equals(viewDetail) ? enclosures.size() : 0;
				int enclosuresSize = enclosures.size();
				final WebMarkupContainer enclosuresWrapper = new WebMarkupContainer("enclosuresWrapper");
				enclosuresWrapper.setVisible(enclosuresSize != 0);
				final Loop enclosuresLoop = new Loop("enclosures", enclosuresSize) {
					private static final long	serialVersionUID	= 1L;

					@Override
					protected void populateItem(LoopItem loopItem) {
						FeedEntryEnclosure enc = enclosures.get(loopItem.getIteration());
						String enclosureName = null;
						try{
							String[] str = enc.getUrl().split("/");
							enclosureName = str[str.length-1];
							if(enclosureName == null || enclosureName.trim().equals(""))
								enclosureName = str[str.length-2];
						}catch(Exception e){
							enclosureName = new StringResourceModel("enclosure", component, null).getString();
						}
						
						ExternalLink externalLink = new ExternalLink("enclosureLink", enc.getUrl(), enclosureName);
						loopItem.add(externalLink);
						//loopItem.add(new Label("enclosureType", enc.getType()));
						double lengthMb = roundDouble((double)enc.getLength()/(double)1000000, 2);
						String lengthMbStr = lengthMb > 0? "("+ String.valueOf(lengthMb) +" MB)" : "";
						loopItem.add(new Label("enclosureSize", lengthMbStr));
					
						if(enc.getType().startsWith("audio")){
							externalLink.add(new AttributeModifier("class", true, new Model("audio")));
						}else if(enc.getType().startsWith("video")){
							externalLink.add(new AttributeModifier("class", true, new Model("video")));
						}else if(enc.getType().startsWith("application/x-shockwave-flash")){
							externalLink.add(new AttributeModifier("class", true, new Model("video")));
						}else if(enc.getType().startsWith("image")){
							externalLink.add(new AttributeModifier("class", true, new Model("image")));
						}else{
							externalLink.add(new AttributeModifier("class", true, new Model("other")));
						}
					}
					
				};
				enclosuresLoop.setOutputMarkupId(true);
				enclosuresWrapper.add(enclosuresLoop);
				enclosuresWrapper.setOutputMarkupId(true);
				//item.add(enclosuresWrapper);
				feedBody.add(enclosuresWrapper);
				
				if(ViewOptions.VIEW_DETAIL_FULL_ENTRY.equals(viewDetail)) {
					feedBody.setVisible(true);
				}else{
					feedBody.setVisible(false);
				}		
				feedBodyWrapper.add(feedBody);
				item.add(feedBodyWrapper);
				
				
				// feed title link
				final WebMarkupContainer titleLinkExpandImg = new WebMarkupContainer("titleLinkExpandImg") {
					private static final long	serialVersionUID	= 1L;
					@Override
					protected void onComponentTag(ComponentTag tag) {
						boolean contentVisible = feedBody.isVisible();
						if(contentVisible) {
							tag.put("src", "/sakai-feeds-tool/img/bullet_toggle_minus.png");
							tag.put("alt", "-");
						}else{
							tag.put("src", "/sakai-feeds-tool/img/bullet_toggle_plus.png");
							tag.put("alt", "+");
						}
						super.onComponentTag(tag);
					}
				};
				String entryTitle = entry.getTitle();
				if(entryTitle == null || entryTitle.trim().equals("")){
					entryTitle = new StringResourceModel("untitled.feed", component, null).getString();
				}
				final Label titleLinkLabel = new Label("titleLinkLabel", entryTitle); 
				final IndicatingAjaxLink link = new IndicatingAjaxLink("titleLink") {
					private static final long	serialVersionUID	= 1L;
					@Override
					public void onClick(AjaxRequestTarget target) {
						boolean contentVisible = feedBody.isVisible();
						feedBody.setVisible(!contentVisible);
						target.addComponent(titleLinkExpandImg);
						target.addComponent(feedBodyWrapper);
						target.addJavascript("setMainFrameHeightNoScroll(window.name);");
					}					
				};
				titleLinkExpandImg.setOutputMarkupId(true);
				link.setOutputMarkupId(true);
				link.add(titleLinkExpandImg);
				link.add(titleLinkLabel);
				item.add(link);
				
			}

		};
		view.setOutputMarkupId(true);
		feedEntryHolder.setOutputMarkupId(true);
		feedEntryHolder.add(view);
		
		// error messages
		feedback = new CSSFeedbackPanel("messages");
		feedEntryHolder.add(feedback);		
		String errorMessage = feedDataProvider.getErrorMessage();
		if(errorMessage != null)
			error(errorMessage);
		
		AuthenticationPanel authPanel = new AuthenticationPanel("authPanel", feedDataProvider, feedEntryHolder);
		authPanel.setVisible(feedDataProvider.requireAuthentication());
		authPanel.setOutputMarkupId(true);
		feedEntryHolder.add(authPanel);
		
		myJs = new WebMarkupContainer("myJs", new Model("")) {
			private static final long	serialVersionUID	= 1L;
			@Override
			protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
				replaceComponentTagBody(markupStream, openTag, getJs());
			}			
		};
		myJs.setOutputMarkupId(true);
		feedEntryHolder.add(myJs);
		
		
		add(feedEntryHolder);
	}
	
	public String getJs() {
		StringBuilder js = new StringBuilder();
		
		//update feed count
		int entryCount = 0;
		try{
			entryCount = getFeedDataProvider().getFeedEntries().size();			
		}catch(Exception e){
			entryCount = 0;
		}
		js.append("$(\"#" + feedEntryHolder.getMarkupId() + "\").parent().parent().parent().find(\".feedCount\").html(\" ( " + entryCount + " )\");");

		// adjust frame height
		js.append("setMainFrameHeightNoScroll(window.name);");
		
		return js.toString();
	}
	
	public String getLinkChangeJs(String markupId, FeedEntry feedEntry) {
		String hostAddress = getHostUrl(feedEntry);
		StringBuilder b = new StringBuilder();
		
		b.append("<script type=\"text/javascript\">");
		
		/* Fix server relative urls in images */
		b.append("$(\"#").append(markupId).append("\").find(\"img\")");
		b.append(".filter(function(index){ return $(this).attr(\"src\").indexOf('/')==0; })");
		b.append(".attr(\"src\",function(index){ return \"").append(hostAddress).append("\" + $(this).attr(\"src\"); });");
		
		/* Fix server relative urls in links */
		b.append("$(\"#").append(markupId).append("\").find(\"a\")");
		b.append(".filter(function(index){ return $(this).attr(\"href\").indexOf('/')==0; })");
		b.append(".attr(\"href\",function(index){ return \"").append(hostAddress).append("\" + $(this).attr(\"href\"); });");
		
		/* Add class '.external' to external links */
		b.append("$(\"#").append(markupId).append("\").find(\"a\")");
		b.append(".not(\".readMore\").not(\".audio\").not(\".video\").not(\".image\").not(\".other\")");
		b.append(".filter(function(index){ return $(this).attr(\"href\") != undefined; })");
		b.append(".addClass(\"external\");");
		
		/* Open external links in new window */
		b.append("$(\"#").append(markupId).append("\").find(\"a\")");
		b.append(".not(\".readMore\").not(\".audio\").not(\".video\").not(\".image\").not(\".other\")");
		b.append(".filter(function(index){ return $(this).attr(\"href\") != undefined; })");
		b.append(".attr(\"target\",\"_blank\");");
		
		b.append("</script>");
		
		return b.toString();
	}
	
	private String getHostUrl(FeedEntry feedEntry) {
		try{
			URL u = new URL(feedEntry.getLink());
			URL newUrl = new URL(u.getProtocol(), u.getHost(), u.getPort(), "");
			return newUrl.toExternalForm();
		}catch(MalformedURLException e){
			e.printStackTrace();			
		}
		return null;
	}

	public FeedDataProvider getFeedDataProvider() {
		return feedDataProvider;
	}
	
    public static final double roundDouble(double d, int places) {
        return Math.round(d * Math.pow(10, (double) places)) / Math.pow(10,
            (double) places);
    }

	public String getViewDetail() {
		return viewDetail;
	}

	public void setViewDetail(String viewDetail) {
		this.viewDetail = viewDetail;
	}

	public void setForceExternalCheck(Boolean forceExternalCheck) {
		this.forceExternalCheck = forceExternalCheck;
		getFeedDataProvider().setForceExternalCheck(forceExternalCheck);
	}
}
