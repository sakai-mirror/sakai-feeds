package org.sakaiproject.feeds.tool.facade;

import org.apache.wicket.RequestCycle;
import org.sakaiproject.feeds.tool.wicket.FeedsApplication;

public class Locator {

	private static transient SakaiFacade facade;
	
	public static SakaiFacade getFacade() {
		if(facade == null) {
			facade = ((FeedsApplication) RequestCycle.get().getApplication()).getFacade();
		}
		return facade;
	}
	
}
