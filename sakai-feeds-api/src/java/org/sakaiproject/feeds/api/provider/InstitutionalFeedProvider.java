package org.sakaiproject.feeds.api.provider;

import java.util.Set;

import org.sakaiproject.site.api.Site;
import org.sakaiproject.user.api.User;

public interface InstitutionalFeedProvider {
	
	/** Get additional Institutional feeds (URLs), specific or not for a given site and/or user. */
	public Set<String> getAdditionalInstitutionalFeeds(Site site, User user);
}
