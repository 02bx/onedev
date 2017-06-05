package com.gitplex.server.event.pullrequest;

import java.util.Date;

import com.gitplex.server.event.MarkdownAware;
import com.gitplex.server.model.User;
import com.gitplex.server.model.PullRequestStatusChange;

public class PullRequestStatusChangeEvent extends PullRequestEvent implements MarkdownAware {

	private final PullRequestStatusChange statusChange;
	
	public PullRequestStatusChangeEvent(PullRequestStatusChange statusChange) {
		super(statusChange.getRequest());
		this.statusChange = statusChange;
	}

	public PullRequestStatusChange getStatusChange() {
		return statusChange;
	}

	@Override
	public String getMarkdown() {
		return statusChange.getNote();
	}

	@Override
	public User getUser() {
		return statusChange.getUser();
	}

	@Override
	public Date getDate() {
		return statusChange.getDate();
	}

}
