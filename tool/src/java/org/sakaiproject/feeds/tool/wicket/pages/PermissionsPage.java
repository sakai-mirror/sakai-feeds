package org.sakaiproject.feeds.tool.wicket.pages;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.feeds.api.FeedsService;
import org.sakaiproject.feeds.tool.facade.SakaiFacade;


/**
 * @author Nuno Fernandes
 */
public class PermissionsPage extends BasePage {
	private static final long	serialVersionUID	= 1L;

	private static Log				LOG			= LogFactory.getLog(PermissionsPage.class);

	@SpringBean
	private transient SakaiFacade	facade;

	private List<PermissionWrapper>	permissions;

	public PermissionsPage() {
		setModel(new CompoundPropertyModel(this));

		Form form = new Form("permissionsForm");

		ListView permissionsView = new ListView("permissions") {
			private static final long	serialVersionUID	= 1L;

			@Override
			protected void populateItem(ListItem item) {
				PermissionWrapper pw = (PermissionWrapper) item.getModelObject();
				String maintainRole = getMaintainRole();
				
				item.add(new Label("role", pw.getRole()));
				CheckBox chk = new CheckBox("feeds.subscribe", new PropertyModel(pw, "feedsSubscribe"));
				if(pw.getRole().equals(maintainRole))
					chk.setEnabled(false);
				item.add(chk);
			}
		};
		form.add(permissionsView);


		// Bottom Buttons
		Button save = new Button("save") {
			private static final long	serialVersionUID	= 1L;

			@Override
			public void onSubmit() {
				savePermissions();
				setResponsePage(MainPage.class);
				super.onSubmit();
			}
		};
		save.setDefaultFormProcessing(true);
		form.add(save);
		Button cancel = new Button("cancel") {
			private static final long	serialVersionUID	= 1L;

			@Override
			public void onSubmit() {
				setResponsePage(MainPage.class);
				super.onSubmit();
			}
		};
		cancel.setDefaultFormProcessing(false);
		form.add(cancel);

		add(form);
	}

	public List<PermissionWrapper> getPermissions() {
		if(permissions == null) {
			permissions	= new ArrayList<PermissionWrapper>();
			try{
				String siteId = facade.getToolManager().getCurrentPlacement().getContext();
				String siteReference = facade.getSiteService().siteReference(siteId);
				AuthzGroup authz = facade.getAuthzGroupService().getAuthzGroup(siteReference);
				Iterator<Role> it = authz.getRoles().iterator();
				while(it.hasNext()) {
					Role r = it.next();
					permissions.add(new PermissionWrapper(r.getId(), new Boolean(r.isAllowed(FeedsService.AUTH_SUBSCRIBE))));
				}
			}catch(Exception e){
				LOG.error("Unable to get permission list", e);
			}
		}
		return permissions;
	}

	public void setPermissions(List<PermissionWrapper> permissions) {
		this.permissions = permissions;
	}
	
	private String getMaintainRole() {
		try{
			String siteId = facade.getToolManager().getCurrentPlacement().getContext();
			String siteReference = facade.getSiteService().siteReference(siteId);
			AuthzGroup authz = facade.getAuthzGroupService().getAuthzGroup(siteReference);
			return authz.getMaintainRole();
		}catch(Exception e){
			LOG.error("Unable to determine maintain role", e);
		}
		return null;
	}
	
	private void savePermissions() {
		try{
			String siteId = facade.getToolManager().getCurrentPlacement().getContext();
			String siteReference = facade.getSiteService().siteReference(siteId);
			AuthzGroup authz = facade.getAuthzGroupService().getAuthzGroup(siteReference);
			Iterator<PermissionWrapper> it = permissions.iterator();
			while(it.hasNext()) {
				PermissionWrapper pw = it.next();
				if(pw.getFeedsSubscribe())
					authz.getRole(pw.getRole()).allowFunction(FeedsService.AUTH_SUBSCRIBE);
				else
					authz.getRole(pw.getRole()).disallowFunction(FeedsService.AUTH_SUBSCRIBE);
			}
			facade.getAuthzGroupService().save(authz);
		}catch(Exception e){
			LOG.error("Unable to set permission list", e);
		}
	}

	// ----------------------------------------

	class PermissionWrapper {
		private String	role;
		private Boolean	feedsSubscribe;

		public PermissionWrapper(String role, Boolean feedsSubscribe) {
			this.role = role;
			this.feedsSubscribe = feedsSubscribe;
		}

		public String getRole() {
			return role;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public Boolean getFeedsSubscribe() {
			return feedsSubscribe;
		}

		public void setFeedsSubscribe(Boolean feedsSubscribe) {
			this.feedsSubscribe = feedsSubscribe;
		}
	}

}
