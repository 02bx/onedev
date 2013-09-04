package com.pmease.gitop.core.manager.impl;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.pmease.commons.persistence.Transactional;
import com.pmease.commons.persistence.dao.DefaultGenericDao;
import com.pmease.commons.persistence.dao.GeneralDao;
import com.pmease.gitop.core.manager.VoteManager;
import com.pmease.gitop.core.model.MergeRequestUpdate;
import com.pmease.gitop.core.model.User;
import com.pmease.gitop.core.model.Vote;

@Singleton
public class DefaultVoteManager extends DefaultGenericDao<Vote> implements VoteManager {

	public DefaultVoteManager(GeneralDao generalDao, Provider<Session> sessionProvider) {
		super(generalDao, sessionProvider);
	}

	@Transactional
	@Override
	public Vote lookupVote(User reviewer, MergeRequestUpdate update) {
		return lookup(new Criterion[]{Restrictions.eq("reviewer", reviewer), Restrictions.eq("update", update)});
	}

}
