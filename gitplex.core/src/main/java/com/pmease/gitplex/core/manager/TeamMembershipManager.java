package com.pmease.gitplex.core.manager;

import java.util.Collection;

import com.pmease.commons.hibernate.dao.EntityManager;
import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.entity.TeamMembership;

public interface TeamMembershipManager extends EntityManager<TeamMembership> {

	Collection<TeamMembership> findAll(Account organization, Account user);
	
	Collection<TeamMembership> findAll(Account organization);
	
	void delete(Collection<TeamMembership> memberships);
}
