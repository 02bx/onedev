package com.pmease.gitplex.core.manager.impl;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.criterion.Restrictions;

import com.pmease.commons.hibernate.Sessional;
import com.pmease.commons.hibernate.Transactional;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.hibernate.dao.EntityCriteria;
import com.pmease.gitplex.core.extensionpoint.PullRequestListener;
import com.pmease.gitplex.core.extensionpoint.PullRequestListeners;
import com.pmease.gitplex.core.manager.PullRequestManager;
import com.pmease.gitplex.core.manager.VoteManager;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.model.PullRequestAudit;
import com.pmease.gitplex.core.model.PullRequestComment;
import com.pmease.gitplex.core.model.PullRequestOperation;
import com.pmease.gitplex.core.model.PullRequestUpdate;
import com.pmease.gitplex.core.model.User;
import com.pmease.gitplex.core.model.Vote;
import com.pmease.gitplex.core.model.Vote.Result;

@Singleton
public class DefaultVoteManager implements VoteManager {

	private final Dao dao;
	
	private final PullRequestManager pullRequestManager;
	
	private final PullRequestListeners pullRequestListeners;
	
	@Inject
	public DefaultVoteManager(Dao dao, PullRequestManager pullRequestManager, 
			PullRequestListeners pullRequestListeners) {
		this.dao = dao;
		this.pullRequestManager = pullRequestManager;
		this.pullRequestListeners = pullRequestListeners;
	}

	@Sessional
	@Override
	public Vote find(User reviewer, PullRequestUpdate update) {
		return dao.find(EntityCriteria.of(Vote.class)
				.add(Restrictions.eq("voter", reviewer)) 
				.add(Restrictions.eq("update", update)));
	}

	@Transactional
	@Override
	public void vote(PullRequest request, User user, Result result, String comment) {
		Vote vote = new Vote();
		vote.setResult(result);
		vote.setUpdate(request.getLatestUpdate());
		vote.setVoter(user);
		
		vote.getVoter().getVotes().add(vote);
		vote.getUpdate().getVotes().add(vote);
		dao.persist(vote);	
		
		PullRequestAudit audit = new PullRequestAudit();
		if (result == Vote.Result.APPROVE)
			audit.setOperation(PullRequestOperation.APPROVE);
		else
			audit.setOperation(PullRequestOperation.DISAPPROVE);
		audit.setDate(new Date());
		audit.setRequest(request);
		audit.setUser(user);
		dao.persist(audit);
		
		if (comment != null) {
			PullRequestComment requestComment = new PullRequestComment();
			requestComment.setRequest(request);
			requestComment.setDate(audit.getDate());
			requestComment.setUser(user);
			requestComment.setContent(comment);
			dao.persist(requestComment);
		}

		pullRequestManager.onGateKeeperUpdate(request);
		
		final Long requestId = request.getId();
		
		dao.afterCommit(new Runnable() {

			@Override
			public void run() {
				pullRequestListeners.call(requestId, new PullRequestListeners.Callback() {

					@Override
					protected void call(PullRequestListener listener, PullRequest request) {
						listener.onVoted(request);
					}
					
				});
			}
			
		});
	}

}
