package com.pmease.gitplex.core.manager.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.pmease.commons.hibernate.dao.AbstractEntityDao;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.gitplex.core.entity.PullRequestActivity;
import com.pmease.gitplex.core.manager.PullRequestActivityManager;

@Singleton
public class DefaultPullRequestActivityManager 
		extends AbstractEntityDao<PullRequestActivity> implements PullRequestActivityManager {

	@Inject
	public DefaultPullRequestActivityManager(Dao dao) {
		super(dao);
	}

}
