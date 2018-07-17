package io.onedev.server.manager.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.onedev.launcher.loader.ListenerRegistry;
import io.onedev.server.event.issue.IssueCommented;
import io.onedev.server.manager.IssueCommentManager;
import io.onedev.server.model.IssueComment;
import io.onedev.server.model.support.LastActivity;
import io.onedev.server.persistence.annotation.Transactional;
import io.onedev.server.persistence.dao.AbstractEntityManager;
import io.onedev.server.persistence.dao.Dao;

@Singleton
public class DefaultIssueCommentManager extends AbstractEntityManager<IssueComment>
		implements IssueCommentManager {

	private final ListenerRegistry listenerRegistry;
	
	@Inject
	public DefaultIssueCommentManager(Dao dao, ListenerRegistry listenerRegistry) {
		super(dao);
		this.listenerRegistry = listenerRegistry;
	}

	@Transactional
	@Override
	public void save(IssueComment comment) {
		boolean isNew = comment.isNew();
		dao.persist(comment);
		if (isNew) {
			IssueCommented event = new IssueCommented(comment);
			listenerRegistry.post(event);
			LastActivity lastActivity = new LastActivity();
			lastActivity.setUser(comment.getUser());
			lastActivity.setDate(comment.getDate());
			lastActivity.setDescription("commented");
			comment.getIssue().setLastActivity(lastActivity);
			comment.getIssue().setNumOfComments(comment.getIssue().getNumOfComments()+1);
		}		
	}

	@Transactional
	@Override
	public void delete(IssueComment comment) {
		super.delete(comment);
		comment.getIssue().setNumOfComments(comment.getIssue().getNumOfComments()-1);
	}

}
