package com.gitplex.server.event.pullrequest;

import java.util.Date;

import com.gitplex.server.event.MarkdownAware;
import com.gitplex.server.model.User;
import com.gitplex.server.model.PullRequestStatusChange;

public class PullRequestStatusChangeEvent extends PullRequestEvent implements MarkdownAware {

	private final PullRequestStatusChange statusChange;
	
	private final Object statusData;
	
	public PullRequestStatusChangeEvent(PullRequestStatusChange statusChange, Object statusData) {
		super(statusChange.getRequest());
		this.statusChange = statusChange;
		this.statusData = statusData;
	}

	public PullRequestStatusChange getStatusChange() {
		return statusChange;
	}

	public Object getStatusData() {
		return statusData;
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
