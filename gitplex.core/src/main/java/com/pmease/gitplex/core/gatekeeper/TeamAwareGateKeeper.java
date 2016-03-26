package com.pmease.gitplex.core.gatekeeper;

import java.util.Collection;
import java.util.HashSet;

import org.hibernate.validator.constraints.NotEmpty;

import com.google.common.base.Preconditions;
import com.pmease.commons.wicket.editable.annotation.Editable;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.annotation.TeamChoice;
import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.entity.Depot;
import com.pmease.gitplex.core.entity.Team;
import com.pmease.gitplex.core.entity.TeamMembership;
import com.pmease.gitplex.core.manager.TeamManager;

public abstract class TeamAwareGateKeeper extends AbstractGateKeeper {

	private static final long serialVersionUID = 1L;

	private String teamName;

    @Editable(name="Team", order=100)
    @TeamChoice
    @NotEmpty
	public String getTeamName() {
		return teamName;
	}

	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}

	@Override
	public void onTeamRename(String oldName, String newName) {
		if (teamName.equals(oldName))
			teamName = newName;
	}

	@Override
	public boolean onTeamDelete(String teamName) {
		return this.teamName.equals(teamName);
	}

	protected Team getTeam(Account organization) {
		return Preconditions.checkNotNull(GitPlex.getInstance(TeamManager.class).find(organization, teamName));
	}
	
	protected Collection<Account> getTeamMembers(Account organization) {
    	Collection<Account> members = new HashSet<>();
        for (TeamMembership membership: getTeam(organization).getMemberships()) {
        	members.add(membership.getUser());
        }        
        return members;
	}
	
	@Override
	public boolean onDepotTransfer(Depot depotDefiningGateKeeper, Depot transferredDepot, 
			Account originalAccount) {
		return depotDefiningGateKeeper.equals(transferredDepot);
	}
	
}
