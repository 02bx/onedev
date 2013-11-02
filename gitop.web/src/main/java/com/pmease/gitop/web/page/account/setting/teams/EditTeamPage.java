package com.pmease.gitop.web.page.account.setting.teams;

import org.apache.shiro.SecurityUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.google.common.base.Preconditions;
import com.pmease.gitop.core.Gitop;
import com.pmease.gitop.core.manager.TeamManager;
import com.pmease.gitop.core.model.Team;
import com.pmease.gitop.core.permission.ObjectPermission;
import com.pmease.gitop.web.model.TeamModel;
import com.pmease.gitop.web.model.UserModel;
import com.pmease.gitop.web.page.account.setting.AccountSettingPage;
import com.pmease.gitop.web.util.WicketUtils;

@SuppressWarnings("serial")
public class EditTeamPage extends AccountSettingPage {

	protected final Long teamId;
	
	public static PageParameters newParams(Team team) {
		Preconditions.checkNotNull(team);
		return WicketUtils.newPageParams("teamId", team.getId());
	}
	
	public EditTeamPage() {
		this.teamId = null;
	}
	
	public EditTeamPage(PageParameters params) {
		this.teamId = params.get("teamId").toLongObject();
	}
	
	@Override
	public boolean isPermitted() {
		if (teamId == null) {
			return super.isPermitted();
		} else {
			Team team = getTeam();
			return SecurityUtils.getSubject().isPermitted(ObjectPermission.ofUserAdmin(team.getOwner()));
		}
	}
	
	@Override
	protected Category getSettingCategory() {
		return Category.TEAMS;
	}

	@Override
	protected String getPageTitle() {
		return "Edit Team";
	}

	@Override
	protected void onPageInitialize() {
		super.onPageInitialize();
		
		add(new TeamEditor("editor", new UserModel(getAccount()), new TeamModel(getTeam())));
		add(new Label("head", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getTeam().isNew() ? "Create Team" : "Edit Team";
			}
			
		}));
	}
	
	protected Team getTeam() {
		if (teamId == null) {
			throw new IllegalStateException("Team id cannot be null when editing team");
		} else {
			return Gitop.getInstance(TeamManager.class).get(teamId);
		}
	}
}
