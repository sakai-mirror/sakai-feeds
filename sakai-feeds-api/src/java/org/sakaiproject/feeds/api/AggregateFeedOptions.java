package org.sakaiproject.feeds.api;

import java.io.Serializable;


public class AggregateFeedOptions implements Serializable {
	private static final long	serialVersionUID		= 1L;

	public static final int		TITLE_DISPLAY_DEFAULT	= 0;
	public static final int		TITLE_DISPLAY_NONE		= 1;
	public static final int		TITLE_DISPLAY_CUSTOM	= 2;

	private int					titleDisplayOption		= TITLE_DISPLAY_NONE;
	private String				customTitle;

	/**
	 * Simple constructor.
	 */
	public AggregateFeedOptions() {
	}

	/**
	 * Constructor specifying title display option and title, if appropriate.
	 * @param titleDisplayOption One of TITLE_DISPLAY_DEFAULT, TITLE_DISPLAY_NONE, TITLE_DISPLAY_CUSTOM
	 * @param customTitle Title to be displayed if titleDisplayOption = TITLE_DISPLAY_CUSTOM
	 */
	public AggregateFeedOptions(final int titleDisplayOption, final String customTitle) {
		this.titleDisplayOption = titleDisplayOption;
		this.customTitle = customTitle;
	}

	public int getTitleDisplayOption() {
		return titleDisplayOption;
	}

	public void setTitleDisplayOption(final int titleDisplayOption) {
		this.titleDisplayOption = titleDisplayOption;
	}

	public String getCustomTitle() {
		return customTitle;
	}

	public void setCustomTitle(final String customTitle) {
		this.customTitle = customTitle;
	}

}
