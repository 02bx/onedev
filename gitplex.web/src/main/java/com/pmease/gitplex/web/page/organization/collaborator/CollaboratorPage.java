package com.pmease.gitplex.web.page.organization.collaborator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.google.common.base.Preconditions;
import com.pmease.commons.wicket.behavior.OnTypingDoneBehavior;
import com.pmease.commons.wicket.component.DropdownLink;
import com.pmease.commons.wicket.component.clearable.ClearableTextField;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.entity.Depot;
import com.pmease.gitplex.core.entity.UserAuthorization;
import com.pmease.gitplex.core.manager.AccountManager;
import com.pmease.gitplex.core.security.SecurityUtils;
import com.pmease.gitplex.core.security.privilege.DepotPrivilege;
import com.pmease.gitplex.web.Constants;
import com.pmease.gitplex.web.component.privilegeselection.PrivilegeSelectionPanel;
import com.pmease.gitplex.web.page.account.people.AccountPeoplePage;
import com.pmease.gitplex.web.page.depot.setting.authorization.DepotCollaboratorListPage;

import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.BootstrapPagingNavigator;
import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.ajax.BootstrapAjaxPagingNavigator;

@SuppressWarnings("serial")
public class CollaboratorPage extends AccountPeoplePage {

	private static final String PARAM_COLLABORATOR = "collaborator";

	private final IModel<Account> collaboratorModel;
	
	private PageableListView<UserAuthorization> depotsView;
	
	private BootstrapPagingNavigator pagingNavigator;
	
	private WebMarkupContainer depotsContainer; 
	
	private WebMarkupContainer noDepotsContainer;
	
	private DepotPrivilege filterPrivilege;
	
	public CollaboratorPage(PageParameters params) {
		super(params);
		
		String collaboratorName = params.get(PARAM_COLLABORATOR).toString();
		collaboratorModel = new LoadableDetachableModel<Account>() {

			@Override
			protected Account load() {
				AccountManager accountManager = GitPlex.getInstance(AccountManager.class);
				Account collaborator = accountManager.findByName(collaboratorName);
				return Preconditions.checkNotNull(collaborator);
			}
			
		};
	}
	
	public static PageParameters paramsOf(Account account, Account collaborator) {
		PageParameters params = paramsOf(account);
		params.add(PARAM_COLLABORATOR, collaborator.getName());
		return params;
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
				target.add(pagingNavigator);
				target.add(noDepotsContainer);
			}
			
		});
		
		WebMarkupContainer filterContainer = new WebMarkupContainer("filter");
		filterContainer.setOutputMarkupId(true);
		add(filterContainer);
		
		filterContainer.add(new DropdownLink("selection") {

			@Override
			protected void onInitialize() {
				super.onInitialize();
				add(new Label("label", new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						if (filterPrivilege == null)
							return "Filter by privilege";
						else 
							return filterPrivilege.toString();
					}
					
				}));
			}

			@Override
			protected Component newContent(String id) {
				return new PrivilegeSelectionPanel(id, true, filterPrivilege) {

					@Override
					protected void onSelect(AjaxRequestTarget target, DepotPrivilege privilege) {
						close();
						filterPrivilege = privilege;
						target.add(filterContainer);
						target.add(depotsContainer);
						target.add(pagingNavigator);
						target.add(noDepotsContainer);
					}

				};
			}
		});
		filterContainer.add(new AjaxLink<Void>("clear") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				filterPrivilege = null;
				target.add(filterContainer);
				target.add(depotsContainer);
				target.add(pagingNavigator);
				target.add(noDepotsContainer);
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(filterPrivilege != null);
			}
			
		});
		
		depotsContainer = new WebMarkupContainer("depots") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!depotsView.getModelObject().isEmpty());
			}
			
		};
		depotsContainer.setOutputMarkupPlaceholderTag(true);
		add(depotsContainer);
		
		depotsContainer.add(depotsView = new PageableListView<UserAuthorization>("depots", 
				new LoadableDetachableModel<List<UserAuthorization>>() {

			@Override
			protected List<UserAuthorization> load() {
				List<UserAuthorization> authorizations = new ArrayList<>();
				
				String searchInput = searchField.getInput();
				if (searchInput != null)
					searchInput = searchInput.toLowerCase().trim();
				else
					searchInput = "";

				for (UserAuthorization authorization: collaboratorModel.getObject().getAuthorizedDepots()) {
					Depot depot = authorization.getDepot();
					if (depot.getAccount().equals(getAccount()) 
							&& depot.getName().toLowerCase().contains(searchInput)) {
						if (authorization.getPrivilege() != DepotPrivilege.NONE 
								&& (filterPrivilege == null || filterPrivilege == authorization.getPrivilege())) {
							authorizations.add(authorization);
						}
					}
				}
				
				Collections.sort(authorizations, new Comparator<UserAuthorization>() {

					@Override
					public int compare(UserAuthorization authorization1, UserAuthorization authorization2) {
						return authorization1.getDepot().getName().compareTo(authorization2.getDepot().getName());
					}
					
				});
				return authorizations;
			}
			
		}, Constants.DEFAULT_PAGE_SIZE) {

			@Override
			protected void populateItem(ListItem<UserAuthorization> item) {
				UserAuthorization authorization = item.getModelObject();

				Link<Void> depotLink = new BookmarkablePageLink<Void>(
						"depotLink", 
						DepotCollaboratorListPage.class, 
						DepotCollaboratorListPage.paramsOf(authorization.getDepot()));
				depotLink.add(new Label("name", authorization.getDepot().getName()));
				item.add(depotLink);

				item.add(new Label("privilege", authorization.getPrivilege()));
			}
			
		});

		add(pagingNavigator = new BootstrapAjaxPagingNavigator("pageNav", depotsView) {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(depotsView.getPageCount() > 1);
			}
			
		});
		pagingNavigator.setOutputMarkupPlaceholderTag(true);
		
		noDepotsContainer = new WebMarkupContainer("noDepots") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(depotsView.getModelObject().isEmpty());
			}
			
		};
		noDepotsContainer.setOutputMarkupPlaceholderTag(true);
		add(noDepotsContainer);
	}

	@Override
	protected boolean isPermitted() {
		return SecurityUtils.canManage(getAccount());
	}
	
	@Override
	protected void onDetach() {
		collaboratorModel.detach();
		super.onDetach();
	}

	@Override
	protected void onSelect(AjaxRequestTarget target, Account account) {
		setResponsePage(AccountCollaboratorListPage.class, AccountCollaboratorListPage.paramsOf(account));
	}

}
