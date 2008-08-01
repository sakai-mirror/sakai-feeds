package org.sakaiproject.feeds.tool.wicket.components;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.feeds.api.FeedsService;


public class CancelIndicator extends WebMarkupContainer {
	private static final long	serialVersionUID	= 1L;

	public CancelIndicator(String id) {
		super(id);
		setOutputMarkupId(true);
	}

	/*
	 * (non-Javadoc)
	 * @see wicket.Component#onComponentTag( wicket.markup.ComponentTag)
	 */
	protected void onComponentTag(ComponentTag tag) {
		super.onComponentTag(tag);
		String imagePath = ServerConfigurationService.getString(FeedsService.SAK_PROP_SHOW_CANCEL_IMG, "/library/image/silk/stop.png");
		tag.put("src", imagePath);
	}
}
