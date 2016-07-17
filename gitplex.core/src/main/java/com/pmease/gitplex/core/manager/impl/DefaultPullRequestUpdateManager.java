package com.pmease.gitplex.core.manager.impl;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.transport.RefSpec;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.google.common.base.Throwables;
import com.pmease.commons.git.GitUtils;
import com.pmease.commons.hibernate.Sessional;
import com.pmease.commons.hibernate.TransactionInterceptor;
import com.pmease.commons.hibernate.Transactional;
import com.pmease.commons.hibernate.dao.AbstractEntityManager;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.hibernate.dao.EntityCriteria;
import com.pmease.gitplex.core.entity.Depot;
import com.pmease.gitplex.core.entity.PullRequest;
import com.pmease.gitplex.core.entity.PullRequestUpdate;
import com.pmease.gitplex.core.entity.component.PullRequestEvent;
import com.pmease.gitplex.core.listener.PullRequestListener;
import com.pmease.gitplex.core.manager.PullRequestCommentManager;
import com.pmease.gitplex.core.manager.PullRequestManager;
import com.pmease.gitplex.core.manager.PullRequestUpdateManager;

@Singleton
public class DefaultPullRequestUpdateManager extends AbstractEntityManager<PullRequestUpdate> 
		implements PullRequestUpdateManager {
	
	private final PullRequestManager pullRequestManager;
	
	private final Provider<Set<PullRequestListener>> pullRequestListenersProvider;
	
	@Inject
	public DefaultPullRequestUpdateManager(Dao dao, PullRequestManager pullRequestManager, 
			Provider<Set<PullRequestListener>> pullRequestListenersProvider,
			PullRequestCommentManager commentManager) {
		super(dao);
		
		this.pullRequestManager = pullRequestManager;
		this.pullRequestListenersProvider = pullRequestListenersProvider;
	}

	@Transactional
	@Override
	public void save(PullRequestUpdate update) {
		PullRequest request = update.getRequest();
		if (TransactionInterceptor.isInitiating()) {
			for (PullRequestListener listener: pullRequestListenersProvider.get())
				listener.onUpdateRequest(update);
			request.setLastEvent(PullRequestEvent.UPDATED);
			request.setLastEventDate(update.getDate());
			request.setLastEventUser(null);
			pullRequestManager.save(request);
		}
		
		String sourceHead = request.getSource().getObjectName();

		update.setMergeCommitHash(GitUtils.getMergeBase(request.getTargetDepot().getRepository(), 
				request.getTarget().getObjectId(), ObjectId.fromString(update.getHeadCommitHash())).name());
		
		dao.persist(update);
		
		if (!request.getTargetDepot().equals(request.getSourceDepot())) {
			try {
				request.getTargetDepot().git().fetch()
						.setRemote(request.getSourceDepot().getDirectory().getAbsolutePath())
						.setRefSpecs(new RefSpec(request.getSourceRef() + ":" + update.getHeadRef()))
						.call();
			} catch (Exception e) {
				Throwables.propagate(e);
			}
		} else {
			RefUpdate refUpdate = request.getTargetDepot().updateRef(update.getHeadRef());
			refUpdate.setNewObjectId(ObjectId.fromString(sourceHead));
			GitUtils.updateRef(refUpdate);
		}
	}

	@Sessional
	@Override
	public PullRequestUpdate find(String uuid) {
		EntityCriteria<PullRequestUpdate> criteria = newCriteria();
		criteria.add(Restrictions.eq("uuid", uuid));
		return find(criteria);
	}

	@Sessional
	@Override
	public List<PullRequestUpdate> findAllAfter(Depot depot, String updateUUID) {
		EntityCriteria<PullRequestUpdate> criteria = newCriteria();
		criteria.createCriteria("request").add(Restrictions.eq("targetDepot", depot));
		criteria.addOrder(Order.asc("id"));
		if (updateUUID != null) {
			PullRequestUpdate update = find(updateUUID);
			if (update != null) {
				criteria.add(Restrictions.gt("id", update.getId()));
			}
		}
		return findAll(criteria);
	}

}
