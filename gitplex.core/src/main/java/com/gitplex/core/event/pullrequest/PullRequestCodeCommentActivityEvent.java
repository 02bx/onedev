package com.gitplex.core.event.pullrequest;

import java.util.Date;

import com.gitplex.core.entity.Account;
import com.gitplex.core.entity.PullRequest;
import com.gitplex.core.entity.support.CodeCommentActivity;

public abstract class PullRequestCodeCommentActivityEvent extends PullRequestCodeCommentEvent {

	private final CodeCommentActivity activity;
	
	public PullRequestCodeCommentActivityEvent(PullRequest request, CodeCommentActivity activity) {
		super(request, activity.getComment());
		this.activity = activity;
	}

	public CodeCommentActivity getActivity() {
		return activity;
	}

	@Override
	public String getMarkdown() {
		return activity.getNote();
	}

	@Override
	public Account getUser() {
		return activity.getUser();
	}

	@Override
	public Date getDate() {
		return activity.getDate();
	}

}
