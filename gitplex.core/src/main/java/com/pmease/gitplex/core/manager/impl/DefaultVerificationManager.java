package com.pmease.gitplex.core.manager.impl;

import java.util.Collection;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.criterion.Restrictions;

import com.pmease.commons.hibernate.Sessional;
import com.pmease.commons.hibernate.Transactional;
import com.pmease.commons.hibernate.UnitOfWork;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.hibernate.dao.EntityCriteria;
import com.pmease.gitplex.core.extensionpoint.PullRequestListener;
import com.pmease.gitplex.core.manager.PullRequestManager;
import com.pmease.gitplex.core.manager.VerificationManager;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.model.PullRequestVerification;
import com.pmease.gitplex.core.model.PullRequestVerification.Status;

@Singleton
public class DefaultVerificationManager implements VerificationManager {

	private final Dao dao;
	
	private final PullRequestManager pullRequestManager;
	
	private final UnitOfWork unitOfWork;
	
	private final Set<PullRequestListener> pullRequestListeners;

	@Inject
	public DefaultVerificationManager(Dao dao, PullRequestManager pullRequestManager, UnitOfWork unitOfWork, 
			Set<PullRequestListener> pullRequestListeners) {
		this.dao = dao;
		this.pullRequestManager = pullRequestManager;
		this.unitOfWork = unitOfWork;
		this.pullRequestListeners = pullRequestListeners;
	}

	@Sessional
	@Override
	public Collection<PullRequestVerification> findBy(PullRequest request, String commit) {
		return dao.query(EntityCriteria.of(PullRequestVerification.class)
				.add(Restrictions.eq("request", request))
				.add(Restrictions.eq("commit", commit)), 0, 0);
	}

	@Sessional
	@Override
	public PullRequestVerification findBy(PullRequest request, String commit, String configuration) {
		return dao.find(EntityCriteria.of(PullRequestVerification.class)
				.add(Restrictions.eq("request", request))
				.add(Restrictions.eq("commit", commit))
				.add(Restrictions.eq("configuration", configuration)));
	}

	@Transactional
	@Override
	public void save(PullRequestVerification verification) {
		dao.persist(verification);

		onVerificationChange(verification.getRequest());
	}

	@Transactional
	@Override
	public void delete(PullRequestVerification verification) {
		dao.remove(verification);
		
		onVerificationChange(verification.getRequest());
	}
	
	private void onVerificationChange(PullRequest request) {
		for (PullRequestListener listener: pullRequestListeners)
			listener.onVerified(request);

		final Long requestId = request.getId();
		dao.afterCommit(new Runnable() {

			@Override
			public void run() {
				unitOfWork.asyncCall(new Runnable() {

					@Override
					public void run() {
						pullRequestManager.check(dao.load(PullRequest.class, requestId));
					}
					
				});
			}
			
		});
	}

	@Override
	public Status getOverallStatus(Collection<PullRequestVerification> verifications) {
		PullRequestVerification.Status overallStatus = null;
		for (PullRequestVerification verification: verifications) {
			if (verification.getStatus() == PullRequestVerification.Status.NOT_PASSED) {
				overallStatus = PullRequestVerification.Status.NOT_PASSED;
				break;
			} else if (verification.getStatus() == PullRequestVerification.Status.ONGOING) {
				overallStatus = PullRequestVerification.Status.ONGOING;
			} else if (overallStatus == null) {
				overallStatus = PullRequestVerification.Status.PASSED;
			}
		}
		return overallStatus;
	}

}
