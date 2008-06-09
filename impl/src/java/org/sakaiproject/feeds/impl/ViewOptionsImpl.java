package org.sakaiproject.feeds.impl;

import org.sakaiproject.feeds.api.ViewOptions;


public class ViewOptionsImpl implements ViewOptions {
	private static final long	serialVersionUID	= 1L;
	private String				viewDetail			= DEFAULT_VIEW_DETAIL;
	private String				viewFilter			= DEFAULT_VIEW_FILTER;

	public ViewOptionsImpl() {		
	}
	
	public ViewOptionsImpl(String defaultViewDetail, String defaultViewFilter) {
		this.viewDetail = defaultViewDetail;
		this.viewFilter = defaultViewFilter;
	}
	
	public String getViewDetail() {
		return viewDetail;
	}

	public String getViewFilter() {
		return viewFilter;
	}

	public void setViewDetail(String viewDetail) {
		this.viewDetail = viewDetail;
	}

	public void setViewFilter(String viewFilter) {
		this.viewFilter = viewFilter;
	}

}
