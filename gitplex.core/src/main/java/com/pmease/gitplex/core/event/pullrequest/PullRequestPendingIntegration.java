package com.pmease.gitplex.core.event.pullrequest;

import com.pmease.gitplex.core.entity.PullRequest;

public class PullRequestPendingIntegration extends PullRequestNotificationEvent {

	public PullRequestPendingIntegration(PullRequest request) {
		super(request);
	}

}
