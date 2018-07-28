package io.onedev.server.model.support.pullrequest.query;

import javax.persistence.criteria.Predicate;

import io.onedev.server.model.Project;
import io.onedev.server.model.PullRequest;
import io.onedev.server.util.query.NotCriteriaHelper;
import io.onedev.server.util.query.QueryBuildContext;

public class NotCriteria extends PullRequestCriteria {
	
	private static final long serialVersionUID = 1L;

	private final PullRequestCriteria criteria;
	
	public NotCriteria(PullRequestCriteria criteria) {
		this.criteria = criteria;
	}

	@Override
	public Predicate getPredicate(Project project, QueryBuildContext<PullRequest> context) {
		return new NotCriteriaHelper<PullRequest>(criteria).getPredicate(project, context);
	}

	@Override
	public boolean matches(PullRequest request) {
		return new NotCriteriaHelper<PullRequest>(criteria).matches(request);
	}

	@Override
	public boolean needsLogin() {
		return new NotCriteriaHelper<PullRequest>(criteria).needsLogin();
	}

	@Override
	public String toString() {
		return new NotCriteriaHelper<PullRequest>(criteria).toString();
	}
	
}
