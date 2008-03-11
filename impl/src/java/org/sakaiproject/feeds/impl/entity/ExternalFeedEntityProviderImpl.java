package org.sakaiproject.feeds.impl.entity;

import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.feeds.api.FeedsService;
import org.sakaiproject.feeds.api.entity.ExternalFeedEntityProvider;

public class ExternalFeedEntityProviderImpl implements ExternalFeedEntityProvider, CoreEntityProvider/*, AutoRegisterEntityProvider*/ {

	private FeedsService feedsService;
	public void setFeedsService(FeedsService feedsService) {
		this.feedsService = feedsService;
	}
	
	public String getEntityPrefix() {
		return ENTITY_PREFIX;
	}

	public boolean entityExists(String id) {
		return feedsService.entityExists(ENTITY_PREFIX, id);
	}

}
