package com.pmease.gitop.core.manager.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.pmease.commons.hibernate.dao.AbstractGenericDao;
import com.pmease.commons.hibernate.dao.GeneralDao;
import com.pmease.gitop.core.manager.CommitCommentManager;
import com.pmease.gitop.model.CommitComment;

@Singleton
public class DefaultCommitCommentManager extends AbstractGenericDao<CommitComment> 
		implements CommitCommentManager {

	@Inject
	public DefaultCommitCommentManager(GeneralDao generalDao) {
		super(generalDao);
	}
}
