package com.pmease.gitplex.core.manager.impl;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import com.pmease.commons.hibernate.Sessional;
import com.pmease.commons.hibernate.dao.DefaultDao;
import com.pmease.commons.hibernate.dao.EntityCriteria;
import com.pmease.gitplex.core.entity.BranchWatch;
import com.pmease.gitplex.core.entity.Depot;
import com.pmease.gitplex.core.entity.User;
import com.pmease.gitplex.core.manager.BranchWatchManager;

@Singleton
public class DefaultBranchWatchManager extends DefaultDao implements BranchWatchManager {

	@Inject
	public DefaultBranchWatchManager(Provider<Session> sessionProvider) {
		super(sessionProvider);
	}

	@Sessional
	@Override
	public Collection<BranchWatch> findBy(User user, Depot depot) {
		EntityCriteria<BranchWatch> criteria = EntityCriteria.of(BranchWatch.class);
		criteria.add(Restrictions.eq("user", user));
		criteria.add(Restrictions.eq("depot", depot));
		return query(criteria);
	}

	@Override
	public Collection<BranchWatch> findBy(Depot depot, String branch) {
		EntityCriteria<BranchWatch> criteria = EntityCriteria.of(BranchWatch.class);
		criteria.add(Restrictions.eq("depot", depot));
		criteria.add(Restrictions.eq("branch", branch));
		return query(criteria);
	}

}
