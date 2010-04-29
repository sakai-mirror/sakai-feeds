package org.sakaiproject.feeds.impl;

import org.sakaiproject.feeds.api.CustomTitleOptions;

public class CustomTitleOptionsImpl implements CustomTitleOptions {
	private static final long	serialVersionUID	= 1L;
	
	private boolean useCustomTitle = false;
	private String customPageTitle = "";
	private String customToolTitle = "";
	
	public CustomTitleOptionsImpl() {		
	}
	
	public CustomTitleOptionsImpl(boolean useCustomTitle, String customPageTitle, String customToolTitle) {
		this.useCustomTitle = useCustomTitle;
		this.customPageTitle = customPageTitle;
		this.customToolTitle = customToolTitle;
	}

	public String getCustomPageTitle() {
		return customPageTitle;
	}

	public String getCustomToolTitle() {
		return customToolTitle;
	}

	public boolean getUseCustomTitle() {
		return useCustomTitle;
	}

	public void setCustomPageTitle(String title) {
		this.customPageTitle = title;
	}

	public void setCustomToolTitle(String title) {
		this.customToolTitle = title;
	}

	public void setUseCustomTitle(boolean custom) {
		this.useCustomTitle = custom;
	}

}
