package org.sakaiproject.feeds.tool.wicket.pages;



public class OptionsPageOld extends BasePage {
	private static final long		serialVersionUID	= 1L;
/*
	@SpringBean
	private transient SakaiFacade	facade;

	private ViewOptions				viewOptions;
	private Set<FeedSubscription>	subscriptions;
	private List<FeedSubscription>	subscriptionsList;

	private DnDSortableHandler		dnd;

	public OptionsPageOld() {
		viewOptions = facade.getFeedsService().getViewOptions();
		subscriptions = facade.getFeedsService().getSubscribedFeeds();
		subscriptionsList = new LinkedList<FeedSubscription>(subscriptions);

		Form form = new Form("options");
		setModel(new CompoundPropertyModel(this));

		// view options
		final Component parent = this;
		final DropDownChoice viewFilter = new DropDownChoice("viewFilter", getViewFilterModes(), new IChoiceRenderer() {
			private static final long	serialVersionUID	= 0L;

			public Object getDisplayValue(Object obj) {
				return (String) new ResourceModel((String) obj).getObject();
			}

			public String getIdValue(Object obj, int arg1) {
				return (String) obj;
			}
		});
		form.add(viewFilter);
		final DropDownChoice viewDetail = new DropDownChoice("viewDetail", getViewDetailModes(), new IChoiceRenderer() {
			private static final long	serialVersionUID	= 0L;

			public Object getDisplayValue(Object obj) {
				return (String) new ResourceModel((String) obj).getObject();
			}

			public String getIdValue(Object obj, int arg1) {
				return (String) obj;
			}
		});
		form.add(viewDetail);

		// subscriptions order
//		container = new DojoOrderableListContainer("container");
//		list = new DojoOrderableRepeatingView("subscriptionsList") {
//
//			public void moveItem(int from, int to, AjaxRequestTarget target) {
//				FeedSubscription drag = (FeedSubscription) subscriptionsList.remove(from);
//				subscriptionsList.add(to, drag);
//
//			}
//
//			public void removeItem(Item item, AjaxRequestTarget target) {
//				subscriptionsList.remove(item.getModelObject());
//
//			}
//
//			protected Iterator getItemModels() {
//				ArrayList modelList = new ArrayList();
//				Iterator it = subscriptionsList.iterator();
//				while (it.hasNext()){
//					modelList.add(new Model((FeedSubscription) it.next()));
//				}
//				return modelList.iterator();
//			}
//
//			protected void populateItem(Item item) {
//				FeedSubscription subscription = (FeedSubscription) item.getModelObject();
//				;
//				item.add(new ExternalImage("iconUrl", subscription.getIconUrl()));
//				item.add(new Label("title", subscription.getTitle()));
//			}
//
//		};
//		container.add(list);
//		form.add(container);
		
		Options dndOptions = new Options();
		dndOptions.set("accept", "dndItem");
        dndOptions.set("containerclass", "container");
		dnd = new DnDSortableHandler("dnd", dndOptions) {
			private static final long	serialVersionUID	= 1L;

			@Override
			public boolean onDnD(AjaxRequestTarget target, MarkupContainer srcContainer, int srcPos, MarkupContainer destContainer, int destPos) {
				// apply modification on model
				try{
					FeedSubscription item = (FeedSubscription) subscriptionsList.remove(srcPos);
					subscriptionsList.add(destPos, item);
				}catch(Exception e){
					System.out.println("Unable to DnD move...");
				}
				return false;
			}			
		};
		form.add(dnd);
		
		// create a container
        WebMarkupContainer webList = new WebMarkupContainer("container",new Model(""));
        webList.setOutputMarkupId(true);
        dnd.registerContainer(webList);
        form.add(webList);

        // create items (add as children of the container)
        webList.add(new ListView("subscriptionsList", getSubscriptionsList()) {
            @Override
            protected void populateItem(ListItem item) {
				FeedSubscription subscription = (FeedSubscription) item.getModelObject();
				item.add(new ExternalImage("iconUrl", subscription.getIconUrl()));
				item.add(new Label("title", subscription.getTitle()));
				dnd.registerItem(item);
            }
        });

		// Bottom Buttons
		Button save = new Button("save") {
			private static final long	serialVersionUID	= 1L;

			@Override
			public void onSubmit() {
				saveOptions();
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
	
//	@Override
//	public void renderHead(IHeaderResponse response) {
//		// TODO Auto-generated method stub
//		super.renderHead(response);
//		response.renderJavascriptReference("/library/js/jquery.js");
//	}



	private void saveOptions() {
		// view options
		facade.getFeedsService().setViewOptions(viewOptions);
		
		// save in session
		Session session = facade.getSessionManager().getCurrentSession();
		session.setAttribute(FeedsService.SESSION_ATTR_VIEWOPTIONS, viewOptions);

		// subscriptions order
		List<String> urls = new LinkedList<String>();
		for(FeedSubscription fs : subscriptionsList){
			urls.add(fs.getUrl());
		}
		facade.getFeedsService().setSubscriptionsOrder(urls);
	}

	public List<FeedSubscription> getSubscriptionsList() {
		return subscriptionsList;
	}

	public void setSubscriptionsList(List<FeedSubscription> subscriptionsList) {
		this.subscriptionsList = subscriptionsList;
	}

	public String getViewFilter() {
		return viewOptions.getViewFilter();
	}

	public void setViewFilter(String viewFilter) {
		viewOptions.setViewFilter(viewFilter);
	}

	public String getViewDetail() {
		return viewOptions.getViewDetail();
	}

	public void setViewDetail(String viewDetail) {
		viewOptions.setViewDetail(viewDetail);
	}

	public static List<String> getViewFilterModes() {
		List<String> modes = new ArrayList<String>();
		modes.add(ViewOptions.VIEW_FILTER_ALL);
		modes.add(ViewOptions.VIEW_FILTER_TODAY);
		modes.add(ViewOptions.VIEW_FILTER_LAST_WEEK);
		modes.add(ViewOptions.VIEW_FILTER_LAST_MONTH);
		modes.add(ViewOptions.VIEW_FILTER_LAST_5);
		modes.add(ViewOptions.VIEW_FILTER_LAST_10);
		return modes;
	}

	public static List<String> getViewDetailModes() {
		List<String> modes = new ArrayList<String>();
		modes.add(ViewOptions.VIEW_DETAIL_FULL_ENTRY);
		modes.add(ViewOptions.VIEW_DETAIL_TITLE_ENTRY);
		return modes;
	}
	*/
}
