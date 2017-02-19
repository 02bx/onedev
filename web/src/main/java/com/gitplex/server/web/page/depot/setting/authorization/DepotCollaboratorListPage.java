package com.gitplex.server.web.page.depot.setting.authorization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.gitplex.server.GitPlex;
import com.gitplex.server.entity.Account;
import com.gitplex.server.entity.Depot;
import com.gitplex.server.entity.UserAuthorization;
import com.gitplex.server.manager.AccountManager;
import com.gitplex.server.manager.UserAuthorizationManager;
import com.gitplex.server.security.SecurityUtils;
import com.gitplex.server.security.privilege.DepotPrivilege;
import com.gitplex.server.web.WebConstants;
import com.gitplex.server.web.component.DropdownLink;
import com.gitplex.server.web.component.accountchoice.AbstractAccountChoiceProvider;
import com.gitplex.server.web.component.accountchoice.AccountChoiceResourceReference;
import com.gitplex.server.web.component.avatar.Avatar;
import com.gitplex.server.web.component.privilegeselection.PrivilegeSelectionPanel;
import com.gitplex.server.web.component.select2.Response;
import com.gitplex.server.web.component.select2.ResponseFiller;
import com.gitplex.server.web.component.select2.SelectToAddChoice;
import com.gitplex.server.web.page.account.collaborators.CollaboratorDepotListPage;
import com.gitplex.server.web.page.account.collaborators.CollaboratorPrivilegeSourcePage;
import com.gitplex.server.web.page.depot.setting.DepotSettingPage;
import com.gitplex.server.web.util.depotaccess.DepotAccess;

@SuppressWarnings("serial")
public class DepotCollaboratorListPage extends DepotSettingPage {

	private ListView<UserAuthorization> collaboratorsView;
	
	private WebMarkupContainer collaboratorsContainer; 
	
	private WebMarkupContainer noCollaboratorsContainer;
	
	private DepotPrivilege filterPrivilege;
	
	private Set<Long> pendingRemovals = new HashSet<>();
	
	public DepotCollaboratorListPage(PageParameters params) {
		super(params);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
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
				return new PrivilegeSelectionPanel(id, false, filterPrivilege) {

					@Override
					protected void onSelect(AjaxRequestTarget target, DepotPrivilege privilege) {
						close();
						filterPrivilege = privilege;
						target.add(filterContainer);
						target.add(collaboratorsContainer);
						target.add(noCollaboratorsContainer);
					}

				};
			}
		});
		filterContainer.add(new AjaxLink<Void>("clear") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				filterPrivilege = null;
				target.add(filterContainer);
				target.add(collaboratorsContainer);
				target.add(noCollaboratorsContainer);
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(filterPrivilege != null);
			}
			
		});
		
		add(new SelectToAddChoice<Account>("addNew", new AbstractAccountChoiceProvider() {

			@Override
			public void query(String term, int page, Response<Account> response) {
				List<Account> collaborators = new ArrayList<>();
				term = term.toLowerCase();
				for (Account user: GitPlex.getInstance(AccountManager.class).findAllUsers()) {
					if (user.matches(term) && !user.equals(getAccount()) && !user.isAdministrator()) {
						boolean authorized = false;
						for (UserAuthorization authorization: depotModel.getObject().getAuthorizedUsers()) {
							if (authorization.getUser().equals(user)) {
								authorized = true;
								break;
							}
						}
						if (!authorized)
							collaborators.add(user);
					}
				}
				
				Collections.sort(collaborators);
				
				new ResponseFiller<Account>(response).fill(collaborators, page, WebConstants.DEFAULT_PAGE_SIZE);
			}

		}) {

			@Override
			protected void onInitialize() {
				super.onInitialize();
				
				getSettings().setPlaceholder("Add collaborator...");
				getSettings().setFormatResult("gitplex.server.accountChoiceFormatter.formatResult");
				getSettings().setFormatSelection("gitplex.server.accountChoiceFormatter.formatSelection");
				getSettings().setEscapeMarkup("gitplex.server.accountChoiceFormatter.escapeMarkup");
			}
			
			@Override
			protected void onSelect(AjaxRequestTarget target, Account selection) {
				UserAuthorization authorization = new UserAuthorization();
				authorization.setUser(selection);
				authorization.setDepot(depotModel.getObject());
				GitPlex.getInstance(UserAuthorizationManager.class).save(authorization);
				target.add(collaboratorsContainer);
				target.add(noCollaboratorsContainer);
			}
			
			@Override
			public void renderHead(IHeaderResponse response) {
				super.renderHead(response);
				
				response.render(JavaScriptHeaderItem.forReference(new AccountChoiceResourceReference()));
			}
			
		});
		
		AjaxLink<Void> confirmRemoveLink;
		add(confirmRemoveLink = new AjaxLink<Void>("confirmRemove") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				Collection<UserAuthorization> authorizations = new HashSet<>();
				UserAuthorizationManager authorizationManager = GitPlex.getInstance(UserAuthorizationManager.class);
				for (Long id: pendingRemovals) {
					authorizations.add(authorizationManager.load(id));
				}
				authorizationManager.delete(authorizations);
				pendingRemovals.clear();
				target.add(this);
				target.add(collaboratorsContainer);
				target.add(noCollaboratorsContainer);
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!pendingRemovals.isEmpty());
			}
			
		});
		confirmRemoveLink.setOutputMarkupPlaceholderTag(true);
		
		collaboratorsContainer = new WebMarkupContainer("collaborators") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!collaboratorsView.getModelObject().isEmpty());
			}
			
		};
		collaboratorsContainer.setOutputMarkupPlaceholderTag(true);
		add(collaboratorsContainer);
		
		collaboratorsContainer.add(collaboratorsView = new ListView<UserAuthorization>("collaborators", 
				new LoadableDetachableModel<List<UserAuthorization>>() {

			@Override
			protected List<UserAuthorization> load() {
				List<UserAuthorization> authorizations = new ArrayList<>();
				
				for (UserAuthorization authorization: depotModel.getObject().getAuthorizedUsers()) {
					if ((filterPrivilege == null || filterPrivilege == authorization.getPrivilege())							
							&& !authorization.getUser().isAdministrator() 
							&& !authorization.getUser().equals(getAccount())) {
						authorizations.add(authorization);
					}
				}
				
				authorizations.sort((authorization1, authorization2) 
						-> authorization1.getUser().getDisplayName().compareTo(authorization2.getUser().getDisplayName()));
				return authorizations;
			}
			
		}) {

			@Override
			protected void populateItem(ListItem<UserAuthorization> item) {
				UserAuthorization authorization = item.getModelObject();

				Link<Void> link = new BookmarkablePageLink<Void>("avatarLink", CollaboratorDepotListPage.class, 
						CollaboratorDepotListPage.paramsOf(getAccount(), authorization.getUser()));
				link.add(new Avatar("avatar", authorization.getUser()));
				item.add(link);
				
				link = new BookmarkablePageLink<Void>("nameLink", CollaboratorDepotListPage.class, 
						CollaboratorDepotListPage.paramsOf(getAccount(), authorization.getUser()));
				link.add(new Label("name", authorization.getUser().getDisplayName()));
				item.add(link);
				
				WebMarkupContainer greaterPrivileges = new WebMarkupContainer("greaterPrivileges") {

					@Override
					protected void onConfigure() {
						super.onConfigure();

						UserAuthorization authorization = item.getModelObject();
						DepotAccess depotAccess = new DepotAccess(authorization.getUser(), depotModel.getObject());
						setVisible(SecurityUtils.isGreater(depotAccess.getGreatestPrivilege(), authorization.getPrivilege()));
					}
					
				};
				
				greaterPrivileges.add(new BookmarkablePageLink<Void>("detail", 
						CollaboratorPrivilegeSourcePage.class, 
						CollaboratorPrivilegeSourcePage.paramsOf(authorization.getDepot(), authorization.getUser())));
				
				item.add(greaterPrivileges);
				item.add(new DropdownLink("privilege") {

					@Override
					protected void onInitialize() {
						super.onInitialize();
						
						add(new Label("label", new AbstractReadOnlyModel<String>() {

							@Override
							public String getObject() {
								return item.getModelObject().getPrivilege().toString();
							}
							
						}));
					}

					@Override
					protected Component newContent(String id) {
						return new PrivilegeSelectionPanel(id, false, item.getModelObject().getPrivilege()) {
							
							@Override
							protected void onSelect(AjaxRequestTarget target, DepotPrivilege privilege) {
								close();
								UserAuthorization authorization = item.getModelObject();
								authorization.setPrivilege(privilege);
								GitPlex.getInstance(UserAuthorizationManager.class).save(authorization);
								target.add(collaboratorsContainer);
								target.add(noCollaboratorsContainer);
								Session.get().success("Privilege updated");
							}

						};
					}
					
				});
				
				item.add(new AjaxLink<Void>("remove") {

					@Override
					protected void onConfigure() {
						super.onConfigure();

						setVisible(!pendingRemovals.contains(item.getModelObject().getId()));
					}

					@Override
					public void onClick(AjaxRequestTarget target) {
						pendingRemovals.add(item.getModelObject().getId());
						target.add(item);
						target.add(confirmRemoveLink);
					}

				});
				item.add(new WebMarkupContainer("pendingRemoval") {

					@Override
					protected void onConfigure() {
						super.onConfigure();
						setVisible(pendingRemovals.contains(item.getModelObject().getId()));
					}
					
				});
				item.add(new AjaxLink<Void>("undoRemove") {

					@Override
					public void onClick(AjaxRequestTarget target) {
						pendingRemovals.remove(item.getModelObject().getId());
						target.add(item);
						target.add(confirmRemoveLink);
					}
					
					@Override
					protected void onConfigure() {
						super.onConfigure();
						setVisible(pendingRemovals.contains(item.getModelObject().getId()));
					}
					
				});
				item.setOutputMarkupId(true);
			}
			
		});

		noCollaboratorsContainer = new WebMarkupContainer("noCollaborators") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(collaboratorsView.getModelObject().isEmpty());
			}
			
		};
		noCollaboratorsContainer.setOutputMarkupPlaceholderTag(true);
		add(noCollaboratorsContainer);
	}
	
	@Override
	protected void onSelect(AjaxRequestTarget target, Depot depot) {
		setResponsePage(DepotCollaboratorListPage.class, DepotCollaboratorListPage.paramsOf(depot));
	}
	
}
