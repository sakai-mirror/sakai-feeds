package org.sakaiproject.feeds.api;

import java.io.Serializable;

public interface CustomTitleOptions extends Serializable {
	
	public boolean getUseCustomTitle();
	public void setUseCustomTitle(boolean custom);
	
	public String getCustomToolTitle();
	public void setCustomToolTitle(String title);

	public String getCustomPageTitle();
	public void setCustomPageTitle(String title);
	
}
