package com.pmease.gitplex.core.manager.impl;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.Query;

import com.google.common.collect.Maps;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.gitplex.core.manager.OldCommitCommentManager;
import com.pmease.gitplex.core.model.Repository;

@Singleton
public class DefaultOldCommitCommentManager implements OldCommitCommentManager {

	private final Dao dao;
	
	@Inject
	public DefaultOldCommitCommentManager(Dao dao) {
		this.dao = dao;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Integer> getCommitCommentStats(Repository repository) {
		String sql = "SELECT c.commit, count(c.id) from OldCommitComment c "
					+ "WHERE repository=:repository "
				    + "GROUP BY c.commit";
		
		Query query = dao.getSession().createQuery(sql);
		query.setParameter("repository", repository);
		List<Object[]> list = query.list();
		
		Map<String, Integer> map = Maps.newHashMap();
		for (Object[] each : list) {
			map.put((String) each[0], ((Long) each[1]).intValue());
		}
		
		return map;
	}
}
