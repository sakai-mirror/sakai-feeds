package org.sakaiproject.feeds.api;

import java.io.Serializable;


public interface ViewOptions extends Serializable {

	public static final String	VIEW_FILTER_ALL			= "view.all";
	public static final String	VIEW_FILTER_LAST_5		= "view.last.5";
	public static final String	VIEW_FILTER_LAST_10		= "view.last.10";
	public static final String	VIEW_FILTER_TODAY		= "view.today";
	public static final String	VIEW_FILTER_LAST_WEEK	= "view.last.week";
	public static final String	VIEW_FILTER_LAST_MONTH	= "view.last.month";
	public static final String	DEFAULT_VIEW_FILTER		= VIEW_FILTER_LAST_WEEK;
	
	public static final String	VIEW_DETAIL_FULL_ENTRY	= "view.entry.full";
	public static final String	VIEW_DETAIL_TITLE_ENTRY	= "view.entry.title";
	public static final String	VIEW_DETAIL_NO_ENTRY	= "view.entry.none";
	public static final String	DEFAULT_VIEW_DETAIL		= VIEW_DETAIL_TITLE_ENTRY;

	public String getViewFilter();
	public void setViewFilter(String viewFilter);

	public String getViewDetail();
	public void setViewDetail(String viewDetail);

}
