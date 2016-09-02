package com.pmease.gitplex.web.component.teamchoice;

import java.util.Collection;
import java.util.List;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.common.collect.Lists;
import com.pmease.commons.wicket.component.select2.ChoiceProvider;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.entity.Team;
import com.pmease.gitplex.core.manager.TeamManager;

public abstract class AbstractTeamChoiceProvider extends ChoiceProvider<Team> {

	private static final long serialVersionUID = 1L;

	@Override
	public void toJson(Team choice, JSONWriter writer) throws JSONException {
		writer.key("id").value(choice.getId()).key("name").value(choice.getName());
	}

	@Override
	public Collection<Team> toChoices(Collection<String> ids) {
		List<Team> teams = Lists.newArrayList();
		TeamManager teamManager = GitPlex.getInstance(TeamManager.class);
		for (String each : ids) {
			Long id = Long.valueOf(each);
			teams.add(teamManager.load(id));
		}

		return teams;
	}

}