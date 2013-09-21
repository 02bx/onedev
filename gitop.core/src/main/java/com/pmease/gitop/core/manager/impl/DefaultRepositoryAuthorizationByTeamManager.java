package com.pmease.gitop.core.manager.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.pmease.commons.hibernate.dao.DefaultGenericDao;
import com.pmease.commons.hibernate.dao.GeneralDao;
import com.pmease.gitop.core.manager.RepositoryAuthorizationByTeamManager;
import com.pmease.gitop.core.model.RepositoryAuthorizationByTeam;

@Singleton
public class DefaultRepositoryAuthorizationByTeamManager extends DefaultGenericDao<RepositoryAuthorizationByTeam> 
		implements RepositoryAuthorizationByTeamManager {

	@Inject
	public DefaultRepositoryAuthorizationByTeamManager(GeneralDao generalDao) {
		super(generalDao);
	}

}
