package com.gitplex.core.manager.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gitplex.core.entity.PullRequestComment;
import com.gitplex.core.entity.support.LastEvent;
import com.gitplex.core.event.pullrequest.PullRequestCommentCreated;
import com.gitplex.core.manager.PullRequestCommentManager;
import com.gitplex.core.manager.PullRequestManager;
import com.gitplex.commons.hibernate.Transactional;
import com.gitplex.commons.hibernate.dao.AbstractEntityManager;
import com.gitplex.commons.hibernate.dao.Dao;
import com.gitplex.commons.loader.ListenerRegistry;
import com.gitplex.commons.wicket.editable.EditableUtils;

@Singleton
public class DefaultPullRequestCommentManager extends AbstractEntityManager<PullRequestComment> 
		implements PullRequestCommentManager {

	private final ListenerRegistry listenerRegistry;
	
	private final PullRequestManager pullRequestManager;
	
	@Inject
	public DefaultPullRequestCommentManager(Dao dao, PullRequestManager pullRequestManager, 
			ListenerRegistry listenerRegistry) {
		super(dao);

		this.listenerRegistry = listenerRegistry;
		this.pullRequestManager = pullRequestManager;
	}

	@Transactional
	@Override
	public void save(PullRequestComment comment) {
		save(comment, true);
	}

	@Transactional
	@Override
	public void save(PullRequestComment comment, boolean notifyListeners) {
		boolean isNew = comment.isNew();
		dao.persist(comment);
		if (notifyListeners && isNew) {
			PullRequestCommentCreated event = new PullRequestCommentCreated(comment);
			listenerRegistry.post(event);
			
			LastEvent lastEvent = new LastEvent();
			lastEvent.setDate(event.getDate());
			lastEvent.setType(EditableUtils.getName(event.getClass()));
			lastEvent.setUser(event.getUser());
			comment.getRequest().setLastEvent(lastEvent);
			pullRequestManager.save(event.getRequest());
		}
	}

}
