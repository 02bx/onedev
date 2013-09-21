package com.pmease.gitop.core.manager;

import com.google.inject.ImplementedBy;
import com.pmease.commons.hibernate.dao.GenericDao;
import com.pmease.commons.util.namedentity.EntityLoader;
import com.pmease.gitop.core.manager.impl.DefaultTeamManager;
import com.pmease.gitop.core.model.Team;
import com.pmease.gitop.core.model.User;

@ImplementedBy(DefaultTeamManager.class)
public interface TeamManager extends GenericDao<Team> {

	/**
	 * Find team of specified name belonging to specified owner.
	 * <p>
	 * @param owner
	 * 			user owns the team
	 * @param teamName
	 * 			name of the team
	 * @return
	 * 			matching team, or <tt>null</tt> if not found 
	 */
	Team find(User owner, String teamName);
	
	EntityLoader asEntityLoader(User owner);
	
}
