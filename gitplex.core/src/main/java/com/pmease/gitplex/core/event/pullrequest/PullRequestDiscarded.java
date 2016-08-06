package com.pmease.gitplex.core.event.pullrequest;

import com.pmease.commons.wicket.editable.annotation.Editable;
import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.entity.PullRequest;

@Editable(name="discarded", icon="fa fa-ban")
public class PullRequestDiscarded extends PullRequestStatusChangeEvent {

	public PullRequestDiscarded(PullRequest request, Account user, String note) {
		super(request, user, note);
	}

}
