package org.sakaiproject.feeds.tool.wicket.components;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;


public abstract class AjaxLazyLoadPanel extends Panel {
	private static final long			serialVersionUID	= 1L;
	private boolean						componentRendered	= false;
	
	public AjaxLazyLoadPanel(String id, final Component loadingComponent) {
		this(id, null, loadingComponent);
	}

	public AjaxLazyLoadPanel(final String id, IModel model, final Component loadingComponent) {
		super(id, model);
		setOutputMarkupId(true);
		componentRendered = false;
		
		// content to be replaced
		Label contents = new Label("content", "");
		contents.setEscapeModelStrings(false);
		contents.setRenderBodyOnly(true);
		add(contents);
		
		// ajax loading indicator
		loadingComponent.add(new AttributeModifier("style", new Model("display: inline")));
		
		// content load
		add(new AbstractDefaultAjaxBehavior() {
			private static final long serialVersionUID = 1L;

			protected void respond(AjaxRequestTarget target) {
				Component component = getLazyLoadComponent("content");
				AjaxLazyLoadPanel.this.replace(component.setRenderBodyOnly(true));
				target.addComponent(AjaxLazyLoadPanel.this);
				loadingComponent.add(new AttributeModifier("style", new Model("display: none")));
				target.addComponent(loadingComponent);
				componentRendered = true;
			}

			public void renderHead(IHeaderResponse response) {
				super.renderHead(response);
				response.renderOnDomReadyJavascript(getCallbackScript().toString());
			}

			public boolean isEnabled(Component component) {
				return !componentRendered;
			}
		});
	}

	/**
	 * @param markupId The components markupid.
	 * @return The component that must be lazy created.
	 */
	public abstract Component getLazyLoadComponent(String markupId);

}
