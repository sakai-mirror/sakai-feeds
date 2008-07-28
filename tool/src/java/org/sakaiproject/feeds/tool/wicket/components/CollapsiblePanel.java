package org.sakaiproject.feeds.tool.wicket.components;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupException;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainerWithAssociatedMarkup;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.parser.XmlTag;


/**
 * @author Nuno Fernandes
 */
public class CollapsiblePanel extends Border {
	private static final long			serialVersionUID	= 1L;
	private static Log					LOG					= LogFactory.getLog(CollapsiblePanel.class);
	private final WebMarkupContainer	content;

	/** If content is initially visible */
	private boolean						contentVisible		= true;

	public CollapsiblePanel(String id, String title, boolean contentVisible) {
		super(id);
		this.contentVisible = contentVisible;

		// content
		final boolean _contentVisible = contentVisible;
		content = new WebMarkupContainer("content") {
			private static final long	serialVersionUID	= 1L;
			
			@Override
			protected void onComponentTag(ComponentTag tag) {
				if(_contentVisible){
					tag.put("style", "display: block");
				}else{
					tag.put("style", "display: none");
				}
				super.onComponentTag(tag);
			}
		};
		content.setOutputMarkupId(true);
		content.add(getBodyContainer());
		add(content);

		// title link
		final WebMarkupContainer titleLinkExpandImg = new WebMarkupContainer("titleLinkExpandImg") {
			private static final long	serialVersionUID	= 1L;

			@Override
			protected void onComponentTag(ComponentTag tag) {
				if(content.isVisible()){
					tag.put("src", "/sakai-feeds-tool/img/bullet_toggle_minus.png");
					tag.put("alt", "-");
				}else{
					tag.put("src", "/sakai-feeds-tool/img/bullet_toggle_plus.png");
					tag.put("alt", "+");
				}
				tag.put("onclick", "showHide('" + content.getMarkupId() + "','" + this.getMarkupId() + "')");
				super.onComponentTag(tag);
			}
		};
		titleLinkExpandImg.setOutputMarkupId(true);
		add(titleLinkExpandImg);

		// title
		final Label titleLinkLabel = new Label("titleLinkLabel", title);
		Link link = new Link("title") {
			private static final long	serialVersionUID	= 1L;

			@Override
			protected void onComponentTag(ComponentTag tag) {
				tag.put("onclick", "showHide('" + content.getMarkupId() + "','" + titleLinkExpandImg.getMarkupId() + "')");
				super.onComponentTag(tag);
			}

			@Override
			public void onClick() {
			}

			@Override
			protected CharSequence getURL() {
				return "#";
			}
		};
		link.add(titleLinkLabel);
		add(link);
	}

}
