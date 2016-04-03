package com.pmease.gitplex.web.page.account.overview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.pmease.commons.wicket.behavior.OnTypingDoneBehavior;
import com.pmease.commons.wicket.component.clearable.ClearableTextField;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.entity.Depot;
import com.pmease.gitplex.core.manager.DepotManager;
import com.pmease.gitplex.core.security.SecurityUtils;
import com.pmease.gitplex.web.Constants;
import com.pmease.gitplex.web.page.depot.overview.DepotOverviewPage;
import com.pmease.gitplex.web.page.depot.setting.general.GeneralSettingPage;

import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.BootstrapPagingNavigator;

@SuppressWarnings("serial")
public class DepotListPanel extends GenericPanel<Account> {

	private PageableListView<Depot> depotsView;
	
	private BootstrapPagingNavigator pagingNavigator;
	
	private WebMarkupContainer depotsContainer; 
	
	private WebMarkupContainer noDepotsContainer;
	
	public DepotListPanel(String id, IModel<Account> accountModel) {
		super(id, accountModel);
	}

	private Account getAccount() {
		return getModelObject();
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		TextField<String> searchField;
		
		add(searchField = new ClearableTextField<String>("searchDepots", Model.of("")));
		searchField.add(new OnTypingDoneBehavior(100) {

			@Override
			protected void onTypingDone(AjaxRequestTarget target) {
				target.add(depotsContainer);
				target.add(noDepotsContainer);
				target.add(pagingNavigator);
			}
			
		});
		
		add(new Link<Void>("addNew") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(SecurityUtils.canManage(getAccount()));
			}

			@Override
			public void onClick() {
				setResponsePage(NewDepotPage.class, NewDepotPage.paramsOf(getAccount()));
			}
			
		});
		
		depotsContainer = new WebMarkupContainer("depotsContainer") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!depotsView.getModelObject().isEmpty());
			}
			
		};
		depotsContainer.setOutputMarkupPlaceholderTag(true);
		add(depotsContainer);
		
		noDepotsContainer = new WebMarkupContainer("noDepots") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(depotsView.getModelObject().isEmpty());
			}
			
		};
		noDepotsContainer.setOutputMarkupPlaceholderTag(true);
		add(noDepotsContainer);
		
		depotsContainer.add(depotsView = new PageableListView<Depot>("depots", 
				new LoadableDetachableModel<List<Depot>>() {

			@Override
			protected List<Depot> load() {
				List<Depot> depots = new ArrayList<>();
				for (Depot depot: GitPlex.getInstance(DepotManager.class)
						.getAccessibles(getAccount(), SecurityUtils.getAccount())) {
					if (depot.matches(searchField.getInput())) {
						depots.add(depot);
					}
				}
				
				Collections.sort(depots);
				return depots;
			}
			
		}, Constants.DEFAULT_PAGE_SIZE) {

			@Override
			protected void populateItem(final ListItem<Depot> item) {
				Depot depot = item.getModelObject();

				Link<Void> link = new BookmarkablePageLink<>("depotLink", 
						DepotOverviewPage.class, DepotOverviewPage.paramsOf(depot)); 
				link.add(new Label("depotName", depot.getName()));
				item.add(link);
						
				item.add(new Label("public", depot.isPublicRead()?"yes":"no"));
				
				item.add(new Link<Void>("setting") {

					@Override
					public void onClick() {
						PageParameters params = GeneralSettingPage.paramsOf(item.getModelObject());
						setResponsePage(GeneralSettingPage.class, params);
					}

					@Override
					protected void onConfigure() {
						super.onConfigure();
						setVisible(SecurityUtils.canManage(item.getModelObject()));
					}
					
				});
			}
			
		});

		add(pagingNavigator = new BootstrapPagingNavigator("depotsPageNav", depotsView) {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(depotsView.getPageCount() > 1);
			}
			
		});
		pagingNavigator.setOutputMarkupPlaceholderTag(true);
	}
	
}
