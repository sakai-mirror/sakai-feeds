package org.sakaiproject.feeds.api;

import java.io.Serializable;


public class AggregateFeedOptions implements Serializable {
	private static final long	serialVersionUID		= 1L;

	public static final int		TITLE_DISPLAY_DEFAULT	= 0;
	public static final int		TITLE_DISPLAY_NONE		= 1;
	public static final int		TITLE_DISPLAY_CUSTOM	= 2;

	private int					titleDisplayOption		= TITLE_DISPLAY_NONE;
	private String				customTitle;

	public AggregateFeedOptions() {
	}

	public AggregateFeedOptions(int titleDisplayOption, String customTitle) {
		this.titleDisplayOption = titleDisplayOption;
		this.customTitle = customTitle;
	}

	public int getTitleDisplayOption() {
		return titleDisplayOption;
	}

	public void setTitleDisplayOption(int titleDisplayOption) {
		this.titleDisplayOption = titleDisplayOption;
	}

	public String getCustomTitle() {
		return customTitle;
	}

	public void setCustomTitle(String customTitle) {
		this.customTitle = customTitle;
	}

}
