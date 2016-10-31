package com.gitplex.web.depotaccess;

import org.apache.wicket.Component;
import org.apache.wicket.model.LoadableDetachableModel;

import com.gitplex.core.GitPlex;
import com.gitplex.core.entity.TeamMembership;
import com.gitplex.core.manager.TeamMembershipManager;
import com.gitplex.core.security.privilege.DepotPrivilege;

public class IsTeamMember implements PrivilegeSource {

	private static final long serialVersionUID = 1L;
	
	private final Long membershipId;
	
	private final DepotPrivilege privilege;
	
	public IsTeamMember(TeamMembership membership, DepotPrivilege privilege) {
		this.membershipId = membership.getId();
		this.privilege = privilege;
	}
	
	@Override
	public DepotPrivilege getPrivilege() {
		return privilege;
	}

	@Override
	public Component render(String componentId) {
		return new IsTeamMemberPanel(componentId, new LoadableDetachableModel<TeamMembership>() {

			private static final long serialVersionUID = 1L;

			@Override
			protected TeamMembership load() {
				return GitPlex.getInstance(TeamMembershipManager.class).load(membershipId);
			}
			
		}, privilege);
	}

}
