package com.pmease.gitplex.web.page.organization.team;

import java.io.Serializable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.google.common.base.Preconditions;
import com.pmease.commons.wicket.editable.BeanContext;
import com.pmease.commons.wicket.editable.BeanEditor;
import com.pmease.commons.wicket.editable.PathSegment;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.entity.Team;
import com.pmease.gitplex.core.manager.TeamManager;
import com.pmease.gitplex.web.page.account.AccountLayoutPage;
import com.pmease.gitplex.web.page.account.AccountOverviewPage;
import com.pmease.gitplex.web.page.organization.OrganizationResourceReference;

@SuppressWarnings("serial")
public class TeamEditPage extends AccountLayoutPage {

	private static final String PARAM_TEAM = "team";
	
	private final IModel<Team> teamModel;
	
	private BeanEditor<?> editor;
	
	private String oldName;
	
	public static PageParameters paramsOf(Team team) {
		PageParameters params = paramsOf(team.getOrganization());
		params.add(PARAM_TEAM, team.getName());
		return params;
	}
	
	public TeamEditPage(PageParameters params) {
		super(params);

		String teamName = params.get(PARAM_TEAM).toString();
		teamModel = new LoadableDetachableModel<Team>() {

			@Override
			protected Team load() {
				TeamManager teamManager = GitPlex.getInstance(TeamManager.class);
				return Preconditions.checkNotNull(teamManager.find(getAccount(), teamName));
			}
			
		};
		Preconditions.checkState(getAccount().isOrganization());
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		editor = BeanContext.editModel("editor", new IModel<Serializable>() {

			@Override
			public void detach() {
			}

			@Override
			public Serializable getObject() {
				return teamModel.getObject();
			}

			@Override
			public void setObject(Serializable object) {
				// check contract of TeamManager.save on why we assign oldName here
				oldName = teamModel.getObject().getName();
				editor.getBeanDescriptor().copyProperties(object, teamModel.getObject());
			}
			
		});
		
		Form<?> form = new Form<Void>("form") {

			@Override
			protected void onSubmit() {
				super.onSubmit();

				TeamManager teamManager = GitPlex.getInstance(TeamManager.class);
				Team team = teamModel.getObject();
				if (!oldName.equals(team.getName()) && teamManager.find(getAccount(), team.getName()) != null) {
					editor.getErrorContext(new PathSegment.Property("name"))
							.addError("This name has already been used by another team in the organization");
				} else {
					teamManager.save(team, oldName);
					setResponsePage(TeamMemberListPage.class, TeamMemberListPage.paramsOf(team));
				}
			}
			
		};
		form.add(editor);
		
		add(form);
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(OrganizationResourceReference.INSTANCE));
	}

	@Override
	protected void onSelect(AjaxRequestTarget target, Account account) {
		if (account.isOrganization())
			setResponsePage(TeamListPage.class, paramsOf(account));
		else
			setResponsePage(AccountOverviewPage.class, paramsOf(account));
	}

	@Override
	protected void onDetach() {
		teamModel.detach();
		super.onDetach();
	}

}
