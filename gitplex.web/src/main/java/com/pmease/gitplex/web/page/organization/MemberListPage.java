package com.pmease.gitplex.web.page.organization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.google.common.base.Preconditions;
import com.pmease.commons.wicket.behavior.OnTypingDoneBehavior;
import com.pmease.commons.wicket.component.DropdownLink;
import com.pmease.commons.wicket.component.clearable.ClearableTextField;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.entity.OrganizationMembership;
import com.pmease.gitplex.core.entity.TeamMembership;
import com.pmease.gitplex.core.manager.OrganizationMembershipManager;
import com.pmease.gitplex.core.manager.TeamMembershipManager;
import com.pmease.gitplex.core.security.SecurityUtils;
import com.pmease.gitplex.web.Constants;
import com.pmease.gitplex.web.component.EmailLink;
import com.pmease.gitplex.web.component.avatar.Avatar;
import com.pmease.gitplex.web.page.account.AccountLayoutPage;
import com.pmease.gitplex.web.page.account.AccountOverviewPage;

import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.BootstrapPagingNavigator;
import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.ajax.BootstrapAjaxPagingNavigator;

@SuppressWarnings("serial")
public class MemberListPage extends AccountLayoutPage {

	private static final String ROLE_ADMIN = "Admin";
	
	private static final String ROLE_MEMBER = "Member";
	
	private PageableListView<OrganizationMembership> membersView;
	
	private BootstrapPagingNavigator pagingNavigator;
	
	private WebMarkupContainer membersContainer; 
	
	private WebMarkupContainer noMembersContainer;
	
	private String role;
	
	private Set<Long> pendingRemovals = new HashSet<>();
	
	public MemberListPage(PageParameters params) {
		super(params);
		
		Preconditions.checkState(getAccount().isOrganization());
	}

	@Override
	protected String getPageTitle() {
		return "Members - " + getAccount();
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		TextField<String> searchField;
		
		add(searchField = new ClearableTextField<String>("searchMembers", Model.of("")));
		searchField.add(new OnTypingDoneBehavior(100) {

			@Override
			protected void onTypingDone(AjaxRequestTarget target) {
				target.add(membersContainer);
				target.add(pagingNavigator);
				target.add(noMembersContainer);
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
						if (role == null)
							return "Filter by role";
						else 
							return WordUtils.capitalize(role);
					}
					
				}));
			}

			@Override
			protected Component newContent(String id) {
				return new RoleSelectionPanel(id) {

					@Override
					protected void onSelectAdmin(AjaxRequestTarget target) {
						close();
						role = ROLE_ADMIN;
						target.add(filterContainer);
						target.add(membersContainer);
						target.add(pagingNavigator);
						target.add(noMembersContainer);
					}

					@Override
					protected void onSelectMember(AjaxRequestTarget target) {
						close();
						role = ROLE_MEMBER;
						target.add(filterContainer);
						target.add(membersContainer);
						target.add(pagingNavigator);
						target.add(noMembersContainer);
					}
					
				};
			}
		});
		filterContainer.add(new AjaxLink<Void>("clear") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				role = null;
				target.add(filterContainer);
				target.add(membersContainer);
				target.add(pagingNavigator);
				target.add(noMembersContainer);
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(role != null);
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
				setResponsePage(NewMembersPage.class, NewMembersPage.paramsOf(getAccount()));
			}
			
		});
		
		AjaxLink<Void> confirmRemoveLink;
		add(confirmRemoveLink = new AjaxLink<Void>("confirmRemove") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				Set<OrganizationMembership> organizationMemberships = new HashSet<>();
				Set<TeamMembership> teamMemberships = new HashSet<>();
				OrganizationMembershipManager organizationMembershipManager = 
						GitPlex.getInstance(OrganizationMembershipManager.class);
				TeamMembershipManager teamMembershipManager = GitPlex.getInstance(TeamMembershipManager.class);
				for (Long pendingRemoval: pendingRemovals) {
					OrganizationMembership organizationMembership = organizationMembershipManager.load(pendingRemoval);
					organizationMemberships.add(organizationMembership);
					for (TeamMembership teamMembership: 
							teamMembershipManager.query(getAccount(), organizationMembership.getUser())) {
						teamMemberships.add(teamMembership);
					}
				}
				GitPlex.getInstance(OrganizationMembershipManager.class).delete(organizationMemberships, teamMemberships);
				pendingRemovals.clear();
				target.add(this);
				target.add(pagingNavigator);
				target.add(membersContainer);
				target.add(noMembersContainer);
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!pendingRemovals.isEmpty());
			}
			
		});
		confirmRemoveLink.setOutputMarkupPlaceholderTag(true);
		
		membersContainer = new WebMarkupContainer("members") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!membersView.getModelObject().isEmpty());
			}
			
		};
		membersContainer.setOutputMarkupPlaceholderTag(true);
		add(membersContainer);
		
		membersContainer.add(membersView = new PageableListView<OrganizationMembership>("members", 
				new LoadableDetachableModel<List<OrganizationMembership>>() {

			@Override
			protected List<OrganizationMembership> load() {
				List<OrganizationMembership> memberships = new ArrayList<>();
				
				for (OrganizationMembership membership: getAccount().getUserMemberships()) {
					Account user = membership.getUser();
					if (user.matches(searchField.getInput())) {
						if (role == null 
								|| role.equals(ROLE_ADMIN) && membership.isAdmin() 
								|| role.equals(ROLE_MEMBER) && !membership.isAdmin()) {
							memberships.add(membership);
						}
					}
				}
				
				Collections.sort(memberships, new Comparator<OrganizationMembership>() {

					@Override
					public int compare(OrganizationMembership membership1, OrganizationMembership membership2) {
						return membership1.getUser().getName().compareTo(membership2.getUser().getName());
					}
					
				});
				return memberships;
			}
			
		}, Constants.DEFAULT_PAGE_SIZE) {

			@Override
			protected void populateItem(ListItem<OrganizationMembership> item) {
				OrganizationMembership membership = item.getModelObject();

				item.add(new Avatar("avatar", membership.getUser()));
				
				Link<Void> link = new BookmarkablePageLink<>("link", MemberPage.class, 
						MemberPage.paramsOf(membership)); 
				link.add(new Label("name", membership.getUser().getDisplayName()));
				item.add(link);
						
				item.add(new EmailLink("email", Model.of(membership.getUser().getEmail())));
				
				item.add(new DropdownLink("role") {

					@Override
					protected void onInitialize() {
						super.onInitialize();
						
						add(new Label("label", new AbstractReadOnlyModel<String>() {

							@Override
							public String getObject() {
								if (membership.isAdmin())
									return ROLE_ADMIN;
								else
									return ROLE_MEMBER;
							}
							
						}));
						Account user = item.getModelObject().getUser();
						if (!SecurityUtils.canManage(getAccount()) || user.equals(getLoginUser()))
							add(AttributeAppender.append("disabled", "disabled"));
					}

					@Override
					public String getAfterDisabledLink() {
						return null;
					}

					@Override
					public String getBeforeDisabledLink() {
						return null;
					}

					@Override
					protected void onConfigure() {
						super.onConfigure();
						Account user = item.getModelObject().getUser();
						setEnabled(SecurityUtils.canManage(getAccount()) && !user.equals(getLoginUser()));
					}

					@Override
					protected Component newContent(String id) {
						return new RoleSelectionPanel(id) {
							
							@Override
							protected void onSelectMember(AjaxRequestTarget target) {
								close();
								membership.setAdmin(false);
								GitPlex.getInstance(OrganizationMembershipManager.class).save(membership);
								target.add(pagingNavigator);
								target.add(membersContainer);
								target.add(noMembersContainer);
							}
							
							@Override
							protected void onSelectAdmin(AjaxRequestTarget target) {
								close();
								membership.setAdmin(true);
								GitPlex.getInstance(OrganizationMembershipManager.class).save(membership);
								target.add(pagingNavigator);
								target.add(membersContainer);
								target.add(noMembersContainer);
							}
							
						};
					}
					
				});
				item.add(new AjaxLink<Void>("remove") {

					@Override
					protected void onConfigure() {
						super.onConfigure();

						Account user = item.getModelObject().getUser();
						setVisible(SecurityUtils.canManage(getAccount()) 
								&& !user.equals(getLoginUser())
								&& !pendingRemovals.contains(item.getModelObject().getId()));
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

		add(pagingNavigator = new BootstrapAjaxPagingNavigator("pageNav", membersView) {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(membersView.getPageCount() > 1);
			}
			
		});
		pagingNavigator.setOutputMarkupPlaceholderTag(true);
		
		noMembersContainer = new WebMarkupContainer("noMembers") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(membersView.getModelObject().isEmpty());
			}
			
		};
		noMembersContainer.setOutputMarkupPlaceholderTag(true);
		add(noMembersContainer);
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(OrganizationResourceReference.INSTANCE));
	}

	@Override
	protected void onSelect(AjaxRequestTarget target, Account account) {
		if (account.isOrganization())
			setResponsePage(MemberListPage.class, paramsOf(account));
		else
			setResponsePage(AccountOverviewPage.class, paramsOf(account));
	}
	
}
