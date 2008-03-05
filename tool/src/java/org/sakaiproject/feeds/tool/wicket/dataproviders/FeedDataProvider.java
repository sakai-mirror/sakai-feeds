package org.sakaiproject.feeds.tool.wicket.dataproviders;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;

import org.apache.wicket.injection.web.InjectorHolder;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
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


public class FeedDataProvider implements IDataProvider {
	private static final long		serialVersionUID			= 1L;
	@SpringBean
	private transient SakaiFacade	facade;
	private	String					viewDetail;
	private boolean					requireAuthentication		= false;
	private String					authenticationRealm			= null;
	private FeedSubscription 		subscription;
	private Feed 					feed;
	private List<FeedEntry>			entries;
	private String					errorMessage;
	private boolean					forceExternalCheck;

	public FeedDataProvider(FeedSubscription subscription, String viewDetail, Boolean forceExternalCheck) {
		if(facade == null)
			InjectorHolder.getInjector().inject(this);
		this.subscription = subscription;
		this.viewDetail = viewDetail;
		this.forceExternalCheck = forceExternalCheck.booleanValue();
		getFeed();
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
			EntityReference reference = facade.getFeedsService().getEntityReference(subscription.getUrl());
			try{
				feed = facade.getFeedsService().getFeed(reference, forceExternalCheck);
				requireAuthentication = false;
				setErrorMessage(null);
				entries = null;
				setErrorMessage(null);
			}catch(FeedAuthenticationException e){
				requireAuthentication = true;
				authenticationRealm = e.getRealm();
				feed = null;
			}catch(IllegalArgumentException e){
				setErrorMessage((String) new ResourceModel("err.subscribing").getObject());
				e.printStackTrace();
				feed = null;
			}catch(MalformedURLException e){
				setErrorMessage((String) new ResourceModel("err.malformed").getObject());
				e.printStackTrace();
				feed = null;
			}catch(SSLHandshakeException e){
				setErrorMessage((String) new ResourceModel("err.ssl").getObject());
				e.printStackTrace();
				feed = null;
			}catch(IOException e){
				setErrorMessage((String) new ResourceModel("err.io").getObject());
				e.printStackTrace();
				feed = null;
			}catch(InvalidFeedException e){
				setErrorMessage((String) new ResourceModel("err.invalid_feed").getObject());
				e.printStackTrace();
				feed = null;
			}catch(FetcherException e){
				if(e.getHttpCode() == 403)
					setErrorMessage((String) new ResourceModel("err.forbidden").getObject());
				else
					setErrorMessage((String) new ResourceModel("err.no_fetch").getObject());
				e.printStackTrace();
			}catch(Exception e){
				setErrorMessage((String) new ResourceModel("err.subscribing").getObject());
				e.printStackTrace();
				feed = null;
			}
//			}catch(Exception e){
//				e.printStackTrace();
//				feed = null;
//			}
		}
		return feed;
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

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public void detach() {
		//System.out.println("FeedDataProvider.detach()");
	}
}
