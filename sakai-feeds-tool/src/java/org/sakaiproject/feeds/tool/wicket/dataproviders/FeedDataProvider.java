package org.sakaiproject.feeds.tool.wicket.dataproviders;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.injection.web.InjectorHolder;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.feeds.api.Feed;
import org.sakaiproject.feeds.api.FeedEntry;
import org.sakaiproject.feeds.api.FeedSubscription;
import org.sakaiproject.feeds.api.ViewOptions;
import org.sakaiproject.feeds.api.exception.FeedAuthenticationException;
import org.sakaiproject.feeds.api.exception.FetcherException;
import org.sakaiproject.feeds.api.exception.InvalidFeedException;
import org.sakaiproject.feeds.tool.facade.SakaiFacade;
import org.sakaiproject.feeds.tool.wicket.model.FeedErrorModel;


public final class FeedDataProvider implements IDataProvider {
	private static final long		serialVersionUID			= 1L;
	private static Log				LOG							= LogFactory.getLog(FeedDataProvider.class);
	@SpringBean
	private transient SakaiFacade	facade;
	private	String					viewDetail;
	private boolean					requireAuthentication		= false;
	private String					affectedFeed				= null;
	private String					authenticationRealm			= null;
	private String					authenticationScheme		= null;
	private FeedSubscription 		subscription;
	private Feed 					feed;
	private List<FeedEntry>			entries;
	private FeedErrorModel			feedErrorModel;
	private List<FeedErrorModel>	aggregateFeedErrorModels;
	private boolean					forceExternalCheck;

	public FeedDataProvider(FeedSubscription subscription, String viewDetail, Boolean forceExternalCheck) {
		if(facade == null)
			InjectorHolder.getInjector().inject(this);
		this.subscription = subscription;
		this.viewDetail = viewDetail;
		this.forceExternalCheck = forceExternalCheck.booleanValue();
		if(!subscription.isAggregateMultipleFeeds()) {
			getFeed();
		}else {
			getAggregatedFeeds();
		}
	}
	
	public String getViewDetail() {
		return viewDetail;
	}

	public void setViewDetail(String viewDetail) {
		this.viewDetail = viewDetail;
		entries = null;
	}

	public void setForceExternalCheck(Boolean forceExternalCheck) {
		this.forceExternalCheck = forceExternalCheck;
		if(forceExternalCheck == true)
			entries = null;
	}
	
	public FeedSubscription getFeedSubscription() {
		return subscription;
	}

	public Feed getFeed() {
		if(feed == null){
			setFeedErrorModel(null);
			feed = getFeed(subscription.getUrl());
		}
		return feed;
	}

	public Feed getAggregatedFeeds() {
		if(feed == null){
			setFeedErrorModel(null);			
			String[] urls = subscription.getUrls();
			List<FeedEntry> agEntries = new ArrayList<FeedEntry>();
			for(int i=0; i<urls.length; i++) {
				Feed t = getFeed(urls[i]);
				if(feed == null && t != null){
					feed = t;
				}
				if(t != null) {
					for(FeedEntry e : t.getEntries()){
						e.setFeedTitle(t.getTitle());
						e.setFeedLink(t.getLink());
						e.setAggregated(true);
						agEntries.add(e);
					}
				}
			}
			Collections.sort(agEntries, new FeedEntryComparator());
			if(feed != null) {
				feed.setTitle("All");
				feed.setLink("#");
				feed.setEntries(agEntries);
			}
		}
		return feed;
	}

	private Feed getFeed(String url) {
		EntityReference reference = facade.getFeedsService().getEntityReference(url);
		Feed _feed = null;
		try{
			_feed = facade.getFeedsService().getFeed(reference, forceExternalCheck);
			requireAuthentication = false;
			entries = null;
		}catch(FeedAuthenticationException e){
			requireAuthentication = true;
			affectedFeed = url;
			authenticationRealm = e.getRealm();
			authenticationScheme = e.getScheme();
			_feed = null;
		}catch(IllegalArgumentException e){
			setFeedErrorModel(new FeedErrorModel("err.subscribing", url, e));
			_feed = null;
		}catch(MalformedURLException e){
			setFeedErrorModel(new FeedErrorModel("err.malformed", url, e));
			_feed = null;
		}catch(SSLHandshakeException e){
			setFeedErrorModel(new FeedErrorModel("err.ssl", url, e));
			_feed = null;
		}catch(IOException e){
			setFeedErrorModel(new FeedErrorModel("err.io", url, e));
			_feed = null;
		}catch(InvalidFeedException e){
			setFeedErrorModel(new FeedErrorModel("err.invalid_feed", url, e));
			_feed = null;
		}catch(FetcherException e){
			if(e.getHttpCode() == 403)
				setFeedErrorModel(new FeedErrorModel("err.forbidden", url, e));
			else
				setFeedErrorModel(new FeedErrorModel("err.no_fetch", url, e));
			_feed = null;
		}catch(Exception e){
			setFeedErrorModel(new FeedErrorModel("err.subscribing", url, e));
			_feed = null;
		}
		return _feed;
	}

	public List<FeedEntry> getFeedEntries() {
		if(entries == null){
			
			if(feed != null){
				if(ViewOptions.VIEW_FILTER_ALL.equals(viewDetail)){
					entries = feed.getEntries();
				}else{
					List<FeedEntry> temp = feed.getEntries();
					entries = new ArrayList<FeedEntry>();
					
					if(ViewOptions.VIEW_FILTER_LAST_5.equals(viewDetail)){
						for(int i =0; i< Math.min(5, temp.size()); i++)
							entries.add(temp.get(i));
					
					}else if(ViewOptions.VIEW_FILTER_LAST_10.equals(viewDetail)){
						for(int i =0; i< Math.min(10, temp.size()); i++)
							entries.add(temp.get(i));
					
					}else if(ViewOptions.VIEW_FILTER_LAST_15.equals(viewDetail)){
						for(int i =0; i< Math.min(15, temp.size()); i++)
							entries.add(temp.get(i));
					
					}else if(ViewOptions.VIEW_FILTER_LAST_20.equals(viewDetail)){
						for(int i =0; i< Math.min(20, temp.size()); i++)
							entries.add(temp.get(i));
					
					}else if(ViewOptions.VIEW_FILTER_LAST_25.equals(viewDetail)){
						for(int i =0; i< Math.min(25, temp.size()); i++)
							entries.add(temp.get(i));
						
					}else if(ViewOptions.VIEW_FILTER_TODAY.equals(viewDetail)){
						Calendar cal = Calendar.getInstance();
						cal.set(Calendar.HOUR_OF_DAY, 0);
						cal.set(Calendar.MINUTE, 0);
						cal.set(Calendar.SECOND, 0);
						cal.set(Calendar.MILLISECOND, 0);
						Date today00 = cal.getTime(); 
						for(int i =0; i< temp.size(); i++){
							FeedEntry e = temp.get(i);
							Date feedDate = e.getPublishedDate();
							if(feedDate == null || feedDate.after(today00))
								entries.add(e);
						}
						
					}else if(ViewOptions.VIEW_FILTER_LAST_WEEK.equals(viewDetail)){
						Calendar cal = Calendar.getInstance();
						cal.add(Calendar.DAY_OF_WEEK, -7);
						cal.set(Calendar.HOUR_OF_DAY, 0);
						cal.set(Calendar.MINUTE, 0);
						cal.set(Calendar.SECOND, 0);
						cal.set(Calendar.MILLISECOND, 0);
						Date aWeekAgo = cal.getTime(); 
						for(int i =0; i< temp.size(); i++){
							FeedEntry e = temp.get(i);
							Date feedDate = e.getPublishedDate();
							if(feedDate == null || feedDate.after(aWeekAgo))
								entries.add(e);
						}
						
					}else if(ViewOptions.VIEW_FILTER_LAST_2WEEKS.equals(viewDetail)){
						Calendar cal = Calendar.getInstance();
						cal.add(Calendar.DAY_OF_WEEK, -14);
						cal.set(Calendar.HOUR_OF_DAY, 0);
						cal.set(Calendar.MINUTE, 0);
						cal.set(Calendar.SECOND, 0);
						cal.set(Calendar.MILLISECOND, 0);
						Date aWeekAgo = cal.getTime(); 
						for(int i =0; i< temp.size(); i++){
							FeedEntry e = temp.get(i);
							Date feedDate = e.getPublishedDate();
							if(feedDate == null || feedDate.after(aWeekAgo))
								entries.add(e);
						}
						
					}else if(ViewOptions.VIEW_FILTER_LAST_MONTH.equals(viewDetail)){
						Calendar cal = Calendar.getInstance();
						cal.add(Calendar.DAY_OF_WEEK, -30);
						cal.set(Calendar.HOUR_OF_DAY, 0);
						cal.set(Calendar.MINUTE, 0);
						cal.set(Calendar.SECOND, 0);
						cal.set(Calendar.MILLISECOND, 0);
						Date aMonthAgo = cal.getTime(); 
						for(int i =0; i< temp.size(); i++){
							FeedEntry e = temp.get(i);
							Date feedDate = e.getPublishedDate();
							if(feedDate == null || feedDate.after(aMonthAgo))
								entries.add(e);
						}
						
					}
				}
			}else
				entries = new ArrayList<FeedEntry>();
		}
		return entries;
	}

	public Iterator<FeedEntry> iterator(int first, int count) {
		return getFeedEntries().iterator();
	}

	public IModel model(Object obj) {
		return new Model((FeedEntry) obj);
	}

	public int size() {
		return getFeedEntries().size();
	}
	
	public boolean requireAuthentication() {
		return requireAuthentication;
	}
	
	public void setRrequireAuthentication(boolean required) {
		requireAuthentication = required;
	}

	public String getAuthenticationRealm() {
		return authenticationRealm;
	}

	public String getAuthenticationScheme() {
		return authenticationScheme;
	}

	public FeedErrorModel getFeedErrorModel() {
		return feedErrorModel;
	}

	public void setFeedErrorModel(FeedErrorModel feedErrorModel) {
		if(!subscription.isAggregateMultipleFeeds()) {
			this.feedErrorModel = feedErrorModel;
		}else{
			if(feedErrorModel != null) {
				addAggregateFeedErrorModel(feedErrorModel);
			}else{
				setAggregateFeedErrorModels(null);
			}
		}		
	}

	public void setAggregateFeedErrorModels(List<FeedErrorModel> aggregateFeedErrorModels) {
		this.aggregateFeedErrorModels = aggregateFeedErrorModels;
	}

	public void addAggregateFeedErrorModel(FeedErrorModel feedErrorModel) {
		if(aggregateFeedErrorModels == null) {
			aggregateFeedErrorModels = new ArrayList<FeedErrorModel>();
		}
		aggregateFeedErrorModels.add(feedErrorModel);
	}

	public List<FeedErrorModel> getAggregateFeedErrorModels() {
		return aggregateFeedErrorModels;
	}

	public void detach() {
		//System.out.println("FeedDataProvider.detach()");
	}

	public String getAffectedFeed() {
		return affectedFeed;
	}

	public void setAffectedFeed(String affectedFeed) {
		this.affectedFeed = affectedFeed;
	}
	
}
