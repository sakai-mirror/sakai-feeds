package org.sakaiproject.feeds.tool.wicket.dataproviders;

import java.io.Serializable;
import java.util.Comparator;

import org.sakaiproject.feeds.api.FeedEntry;

public class FeedEntryComparator implements Comparator<FeedEntry>, Serializable {
	private static final long	serialVersionUID	= 1L;

	public int compare(FeedEntry o1, FeedEntry o2) {
		if(o1 == null & o2 != null)
			return -1;
		if(o2 == null & o1 != null)
			return 1;
		if(o1 != null & o2 != null){
			if(o1.getPublishedDate() == null && o2.getPublishedDate() != null)
				return 1;
			if(o2.getPublishedDate() == null && o1.getPublishedDate() != null)
				return -1;
			if(o2.getPublishedDate() == null && o1.getPublishedDate() == null)
				return 1;
			return - (o1.getPublishedDate().compareTo(o2.getPublishedDate()));
		}
		return 0;
	}

}
