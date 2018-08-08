package io.onedev.server.entityquery.pullrequest;

import java.util.Objects;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import io.onedev.server.entityquery.QueryBuildContext;
import io.onedev.server.model.Project;
import io.onedev.server.model.PullRequest;
import io.onedev.server.model.support.pullrequest.PullRequestConstants;
import io.onedev.server.entityquery.pullrequest.PullRequestQueryLexer;
import io.onedev.server.security.SecurityUtils;

public class SubmittedByMeCriteria extends PullRequestCriteria {

	private static final long serialVersionUID = 1L;

	@Override
	public Predicate getPredicate(Project project, QueryBuildContext<PullRequest> context) {
		Path<?> attribute = context.getRoot().get(PullRequestConstants.ATTR_SUBMITTER);
		return context.getBuilder().equal(attribute, SecurityUtils.getUser());
	}

	@Override
	public boolean matches(PullRequest request) {
		return Objects.equals(request.getSubmitter(), SecurityUtils.getUser());
	}

	@Override
	public boolean needsLogin() {
		return true;
	}

	@Override
	public String toString() {
		return PullRequestQuery.getRuleName(PullRequestQueryLexer.SubmittedByMe);
	}

}
