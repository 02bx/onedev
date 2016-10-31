package com.gitplex.core.manager;

import javax.annotation.Nullable;

import com.gitplex.core.entity.Account;
import com.gitplex.core.entity.Team;
import com.gitplex.commons.hibernate.dao.EntityManager;

public interface TeamManager extends EntityManager<Team> {
	/**
	 * Save specified team in specified organization
	 * 
	 * @param team
	 * 			team to save
	 * @param oldName
	 * 			in case of rename, this parameter should hold the original name 
	 * 			when above team object is initially loaded to ensure database 
	 * 			integrity. Use <tt>null</tt> if original name does not exist, 
	 * 			or the name is not changed
	 */
	void save(Team team, @Nullable String oldName);
	
	@Nullable
	Team find(Account organization, String name);
}
