package com.gitplex.server.event.pullrequest;

import java.util.Date;

import javax.annotation.Nullable;

import com.gitplex.server.model.User;
import com.gitplex.server.model.PullRequest;

public abstract class PullRequestEvent {

	private final PullRequest request;
	
	public PullRequestEvent(PullRequest request) {
		this.request = request;
	}

	public PullRequest getRequest() {
		return request;
	}
	
	@Nullable
	public abstract User getUser();

	public abstract Date getDate();
	
}
