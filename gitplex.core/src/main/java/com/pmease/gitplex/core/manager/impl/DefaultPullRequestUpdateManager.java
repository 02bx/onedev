package com.pmease.gitplex.core.manager.impl;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.pmease.commons.hibernate.Transactional;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.util.FileUtils;
import com.pmease.gitplex.core.extensionpoint.PullRequestListener;
import com.pmease.gitplex.core.manager.PullRequestCommentManager;
import com.pmease.gitplex.core.manager.PullRequestUpdateManager;
import com.pmease.gitplex.core.manager.StorageManager;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.model.PullRequestComment;
import com.pmease.gitplex.core.model.PullRequestUpdate;

@Singleton
public class DefaultPullRequestUpdateManager implements PullRequestUpdateManager {
	
	private final Dao dao;
	
	private final StorageManager storageManager;
	
	private final Set<PullRequestListener> pullRequestListeners;
	
	private final PullRequestCommentManager pullRequestCommentManager;
	
	@Inject
	public DefaultPullRequestUpdateManager(Dao dao, StorageManager storageManager, 
			Set<PullRequestListener> pullRequestListeners, 
			PullRequestCommentManager pullRequestCommentManager) {
		this.dao = dao;
		this.storageManager = storageManager;
		this.pullRequestListeners = pullRequestListeners;
		this.pullRequestCommentManager = pullRequestCommentManager;
	}

	@Transactional
	@Override
	public void save(PullRequestUpdate update) {
		dao.persist(update);
		
		FileUtils.cleanDir(storageManager.getCacheDir(update));

		PullRequest request = update.getRequest();
		String sourceHead = request.getSource().getHeadCommitHash();

		if (!request.getTarget().getRepository().equals(request.getSource().getRepository())) {
			request.getTarget().getRepository().git().fetch(
					request.getSource().getRepository().git(), 
					"+" + request.getSource().getHeadRef() + ":" + update.getHeadRef()); 
		} else {
			request.getTarget().getRepository().git().updateRef(update.getHeadRef(), 
					sourceHead, null, null);
		}
		
		for (PullRequestComment comment: request.getComments()) {
			if (comment.getInlineInfo() != null)
				pullRequestCommentManager.updateInline(comment);
		}

		for (PullRequestListener listener: pullRequestListeners)
			listener.onUpdated(request);
	}

	@Transactional
	@Override
	public void delete(PullRequestUpdate update) {
		update.deleteRefs();
		dao.remove(update);
	}

}
