package com.pmease.gitplex.web.page.organization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.FencedFeedbackPanel;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.google.common.base.Preconditions;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.entity.OrganizationMembership;
import com.pmease.gitplex.core.entity.Team;
import com.pmease.gitplex.core.entity.TeamMembership;
import com.pmease.gitplex.core.manager.OrganizationMembershipManager;
import com.pmease.gitplex.web.component.teamchoice.TeamChoiceProvider;
import com.pmease.gitplex.web.component.teamchoice.TeamMultiChoice;
import com.pmease.gitplex.web.page.account.AccountLayoutPage;
import com.pmease.gitplex.web.page.account.AccountOverviewPage;

@SuppressWarnings("serial")
public class NewMembersPage extends AccountLayoutPage {

	private static final String USERS_ID = "users";
	
	private static final List<String> ROLES = 
			Arrays.asList("Add as ordinary members", "Add as organization administrators"); 
	
	private Collection<Account> users;
	
	private String role = ROLES.get(0);
	
	private Collection<Team> teams;
	
	public NewMembersPage(PageParameters params) {
		super(params);
		
		Preconditions.checkState(getAccount().isOrganization());
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		Form<?> form = new Form<Void>("form") {

			@Override
			protected void onSubmit() {
				super.onSubmit();
				
				Collection<OrganizationMembership> organizationMemberships = new ArrayList<>();
				Collection<TeamMembership> teamMemberships = new ArrayList<>();
				for (Account user: users) {
					OrganizationMembership organizationMembership = new OrganizationMembership();
					organizationMembership.setUser(user);
					organizationMembership.setOrganization(getAccount());
					organizationMembership.setAdmin(!role.equals(ROLES.get(0)));
					organizationMemberships.add(organizationMembership);
					for (Team team: teams) {
						TeamMembership teamMembership = new TeamMembership();
						teamMembership.setUser(user);
						teamMembership.setTeam(team);
						teamMemberships.add(teamMembership);
					}
				}
				
				GitPlex.getInstance(OrganizationMembershipManager.class).save(organizationMemberships, teamMemberships);
				setResponsePage(MemberListPage.class, MemberListPage.paramsOf(getAccount()));
			}
			
		};
		add(form);
		
		WebMarkupContainer usersContainer = new WebMarkupContainer(USERS_ID);
		usersContainer.add(AttributeAppender.append("class", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				if (usersContainer.get(USERS_ID).hasErrorMessage())
					return "has-error";
				else
					return "";
			}
			
		}));
		form.add(usersContainer);

		usersContainer.add(new NonMemberChoices(USERS_ID, accountModel, new IModel<Collection<Account>>() {

			@Override
			public void detach() {
			}

			@Override
			public Collection<Account> getObject() {
				return users;
			}

			@Override
			public void setObject(Collection<Account> object) {
				users = object;
			}
			
		}).setRequired(true));
		
		usersContainer.add(new FencedFeedbackPanel("feedback", usersContainer));
		
		form.add(new RadioChoice<String>("role", new IModel<String>() {

			@Override
			public void detach() {
			}

			@Override
			public String getObject() {
				return role;
			}

			@Override
			public void setObject(String object) {
				role = object;
			}
			
		}, ROLES));
		
		form.add(new TeamMultiChoice("teams", new IModel<Collection<Team>>() {

			@Override
			public void detach() {
			}

			@Override
			public Collection<Team> getObject() {
				return teams;
			}

			@Override
			public void setObject(Collection<Team> object) {
				teams = object;
			}
			
		}, new TeamChoiceProvider(accountModel)));
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
