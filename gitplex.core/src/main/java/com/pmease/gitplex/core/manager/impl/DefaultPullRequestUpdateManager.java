package com.pmease.gitplex.core.manager.impl;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.pmease.commons.hibernate.Transactional;
import com.pmease.commons.hibernate.UnitOfWork;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.util.FileUtils;
import com.pmease.gitplex.core.listeners.PullRequestListener;
import com.pmease.gitplex.core.manager.CommentManager;
import com.pmease.gitplex.core.manager.PullRequestUpdateManager;
import com.pmease.gitplex.core.manager.StorageManager;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.model.Comment;
import com.pmease.gitplex.core.model.PullRequestUpdate;

@Singleton
public class DefaultPullRequestUpdateManager implements PullRequestUpdateManager {
	
	private final Dao dao;
	
	private final StorageManager storageManager;
	
	private final Set<PullRequestListener> pullRequestListeners;
	
	private final CommentManager commentManager;
	
	private final UnitOfWork unitOfWork;
	
	@Inject
	public DefaultPullRequestUpdateManager(Dao dao, StorageManager storageManager, 
			Set<PullRequestListener> pullRequestListeners, UnitOfWork unitOfWork,
			CommentManager commentManager) {
		this.dao = dao;
		this.storageManager = storageManager;
		this.pullRequestListeners = pullRequestListeners;
		this.unitOfWork = unitOfWork;
		this.commentManager = commentManager;
	}

	@Transactional
	@Override
	public void save(PullRequestUpdate update, boolean notify) {
		dao.persist(update);
		
		FileUtils.cleanDir(storageManager.getCacheDir(update));

		PullRequest request = update.getRequest();
		String sourceHead = request.getSource().getObjectName();

		if (!request.getTargetRepo().equals(request.getSourceRepo())) {
			request.getTargetRepo().git().fetch(
					request.getSourceRepo().git(), 
					"+" + request.getSourceRef() + ":" + update.getHeadRef()); 
		} else {
			request.getTargetRepo().git().updateRef(update.getHeadRef(), 
					sourceHead, null, null);
		}
		
		if (notify) { 
			for (PullRequestListener listener: pullRequestListeners)
				listener.onUpdated(update);
		}

		// Inline comment update can be time consuming and we do it inside a separate thread 
		// in order not to blocking the push operation
		final Long requestId = request.getId();
		dao.afterCommit(new Runnable() {

			@Override
			public void run() {
				unitOfWork.asyncCall(new Runnable() {

					@Override
					public void run() {
						PullRequest request = dao.load(PullRequest.class, requestId);
						for (Comment comment: request.getComments()) {
							if (comment.getInlineInfo() != null)
								commentManager.updateInlineInfo(comment);
						}
					}
					
				});
			}
			
		});
	}

	@Transactional
	@Override
	public void delete(PullRequestUpdate update) {
		update.deleteRefs();
		dao.remove(update);
	}

}
