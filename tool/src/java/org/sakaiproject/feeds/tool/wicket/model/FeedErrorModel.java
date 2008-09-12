package org.sakaiproject.feeds.tool.wicket.model;

import java.io.Serializable;

import org.apache.wicket.model.IModel;


public class FeedErrorModel implements IModel, Serializable {
	private static final long	serialVersionUID	= 1L;
	private String				errorMessageKey		= "";
	private String				url					= "";
	private Exception 			exception;

	public FeedErrorModel(final String errorMessageKey, final String feedUrl, final Exception exception) {
		this.url = feedUrl;
		this.errorMessageKey = errorMessageKey;
		this.exception = exception;
	}

	public void detach() {
		// NOPMD by Nuno Fernandes on 12-09-2008 13:47
	}

	public Object getObject() {
		return getUrl();
	}

	public void setObject(final Object obj) {
		setUrl(obj.toString());
	}

	public void setUrl(final String feedUrl) {
		this.url = feedUrl;
	}

	public String getUrl() {
		return url;
	}

	public String getErrorMessageKey() {
		return errorMessageKey;
	}

	public void setErrorMessageKey(final String errorMessageKey) {
		this.errorMessageKey = errorMessageKey;
	}

	public Exception getException() {
		return exception;
	}

	public void setException(final Exception exception) {
		this.exception = exception;
	}

}
