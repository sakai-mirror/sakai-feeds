package org.sakaiproject.feeds.tool.wicket.components;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;


public abstract class AjaxParallelLazyLoadPanel extends Panel {
	private static final long			serialVersionUID	= 1L;
	private boolean						componentReady		= false;
	private boolean						componentRendered	= false;
	private AbstractAjaxTimerBehavior	abstractAjaxTimerBehavior;
	private Component					lazyLoadcomponent;
	private Thread 						thread;
	
	public AjaxParallelLazyLoadPanel(String id, final Component loadingComponent) {
		this(id, null, loadingComponent);
	}

	public AjaxParallelLazyLoadPanel(final String id, IModel model, final Component loadingComponent) {
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

//		abstractAjaxTimerBehavior = new AbstractAjaxTimerBehavior(Duration.milliseconds(500)) {
//			private static final long	serialVersionUID	= 1L;

//			@Override
//			protected void onTimer(AjaxRequestTarget target) {
//				System.out.println("onTimer(): "+id);
//				if(componentReady && !componentRendered) {
//					AjaxParallelLazyLoadPanel.this.replace(lazyLoadcomponent.setRenderBodyOnly(true));
//					target.addComponent(AjaxParallelLazyLoadPanel.this);
//					System.out.println("FEED ADDED TO PANEL: "+id);
//					componentRendered = true;
//					stop();
//				}
////				else if(job == null){
////					job = new Runnable() {
////						public void run() {
////							System.out.println("FETCHING FEED: "+id);
////							if(lazyLoadcomponent == null) {
////								lazyLoadcomponent = getLazyLoadComponent("content");
////								componentReady = true;
////								System.out.println("FEED FETCHED: "+id);
////							}
////						}			
////					};
////					job.run();
////				}
//			}
//
//			@Override
//			public void renderHead(IHeaderResponse response) {
//				super.renderHead(response);
//				response.renderOnDomReadyJavascript(getCallbackScript().toString());
//			}
//
//			@Override
//			public boolean isEnabled(Component component) {
//				return !componentRendered;//return get("content") == loadingComponent;
//			}
//
//		};
//		add(abstractAjaxTimerBehavior);
//		
//		thread = new Thread(this);
		
		 add(new AbstractDefaultAjaxBehavior() {
			private static final long	serialVersionUID	= 1L;

			protected void respond(AjaxRequestTarget target) {
				Component component = getLazyLoadComponent("content");
				AjaxParallelLazyLoadPanel.this.replace(component.setRenderBodyOnly(true));
				target.addComponent(AjaxParallelLazyLoadPanel.this);
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
	
//	public void run() {
//		System.out.println("FETCHING FEED");
//		lazyLoadcomponent = getLazyLoadComponent("content");
//		componentReady = true;
//		System.out.println("FEED FETCHED");
//	}

	/**
	 * @param markupId The components markupid.
	 * @return The component that must be lazy created.
	 */
	public abstract Component getLazyLoadComponent(String markupId);

}
