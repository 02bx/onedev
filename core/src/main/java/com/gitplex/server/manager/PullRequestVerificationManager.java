package com.gitplex.server.manager;

import java.util.Collection;

import com.gitplex.server.model.PullRequest;
import com.gitplex.server.model.PullRequestVerification;
import com.gitplex.server.persistence.dao.EntityManager;

public interface PullRequestVerificationManager extends EntityManager<PullRequestVerification> {
	
	Collection<PullRequestVerification> findAll(PullRequest request, String commit);
	
	PullRequestVerification find(PullRequest request, String commit, String configuration);
	
	PullRequestVerification.Status getOverallStatus(Collection<PullRequestVerification> verifications);
}
