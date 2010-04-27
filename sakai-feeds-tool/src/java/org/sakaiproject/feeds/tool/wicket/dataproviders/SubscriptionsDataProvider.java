package org.sakaiproject.feeds.tool.wicket.dataproviders;

import java.util.Iterator;
import java.util.Set;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.sakaiproject.feeds.api.FeedSubscription;
import org.sakaiproject.feeds.tool.facade.Locator;


public class SubscriptionsDataProvider implements IDataProvider {
	private static final long		serialVersionUID			= 1L;
	public static final int			MODE_SUBSCRIBED				= 0;
	public static final int			MODE_ALL_INSTITUTIONAL		= 1;
	public static final int			MODE_ALL_NON_INSTITUTIONAL	= 2;
	private Set<FeedSubscription>	feeds;
	private int						mode;

	public SubscriptionsDataProvider(int mode) {
		this.mode = mode;
	}

	public Set<FeedSubscription> getFeedSubscriptions() {
		if(feeds == null){
			feeds = Locator.getFacade().getFeedsService().getSubscribedFeeds(mode);
		}
		return feeds;
	}

	public void addTemporaryFeedSubscription(FeedSubscription subscription) {
		getFeedSubscriptions().add(subscription);
	}

	public Iterator<FeedSubscription> iterator(int first, int count) {
		return getFeedSubscriptions().iterator();
	}

	public IModel model(Object obj) {
		return new Model((FeedSubscription) obj);
	}

	public int size() {
		if(mode == MODE_SUBSCRIBED && Locator.getFacade().getFeedsService().isAggregateFeeds() && getFeedSubscriptions().size() > 0){
			try{
				return getFeedSubscriptions().iterator().next().getUrls().length;
			}catch(Exception e) {
				return 0;
			}
		}else{
			return getFeedSubscriptions().size();
		}
	}

	public void detach() {
		//System.out.println("SubscriptionsDataProvider.detach()");
//		if(!modified){
//			System.out.println("SubscriptionsDataProvider.detach()");
//			feeds = null;
//		}
	}
}
