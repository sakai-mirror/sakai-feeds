package org.sakaiproject.feeds.tool.wicket.components;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;

public class AjaxUpdatingBehaviorWithIndicator extends AjaxFormComponentUpdatingBehavior implements IAjaxIndicatorAware {
	private static final long	serialVersionUID	= 1L;
	private AjaxIndicator ajaxIndicator;
	
	public AjaxUpdatingBehaviorWithIndicator(String event, AjaxIndicator ajaxIndicator) {
		super(event);
		this.ajaxIndicator = ajaxIndicator;
	}

	@Override
	protected void onUpdate(AjaxRequestTarget arg0) {
	}

	public String getAjaxIndicatorMarkupId() {
		return ajaxIndicator.getMarkupId();
	}

}
