package com.pmease.gitplex.core.event.pullrequest;

import com.pmease.gitplex.core.entity.PullRequest;

public class PullRequestPendingUpdate extends PullRequestNotificationEvent {

	public PullRequestPendingUpdate(PullRequest request) {
		super(request);
	}

}
