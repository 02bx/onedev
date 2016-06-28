package com.pmease.gitplex.core.manager;

import java.util.Collection;

import javax.annotation.Nullable;

import com.pmease.commons.hibernate.dao.EntityManager;
import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.entity.OrganizationMembership;
import com.pmease.gitplex.core.entity.TeamMembership;

public interface OrganizationMembershipManager extends EntityManager<OrganizationMembership> {
	
	void save(OrganizationMembership membership);
	
	@Nullable
	OrganizationMembership find(Account organization, Account user);
	
	void save(Collection<OrganizationMembership> organizationMemberships, 
			Collection<TeamMembership> teamMemberships);
	
	void delete(OrganizationMembership organizationMembership);
	
	void delete(Collection<OrganizationMembership> organizationMemberships);

	void save(OrganizationMembership organizationMembership, 
			Collection<TeamMembership> teamMembershipsToAdd, 
			Collection<TeamMembership> teamMembershipsToRemove);
	
}
