package io.onedev.server.search.entity.pullrequest;

import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import io.onedev.server.model.Project;
import io.onedev.server.model.PullRequest;
import io.onedev.server.model.PullRequestReview;
import io.onedev.server.model.User;
import io.onedev.server.search.entity.EntityQuery;
import io.onedev.server.search.entity.QueryBuildContext;
import io.onedev.server.util.PullRequestConstants;

public class ToBeReviewedByMeCriteria extends PullRequestCriteria {

	private static final long serialVersionUID = 1L;

	@Override
	public Predicate getPredicate(Project project, QueryBuildContext<PullRequest> context, User user) {
		From<?, ?> join = context.getJoin(PullRequestConstants.ATTR_REVIEWS);
		Path<?> userPath = EntityQuery.getPath(join, PullRequestReview.ATTR_USER);
		Path<?> excludeDatePath = EntityQuery.getPath(join, PullRequestReview.ATTR_EXCLUDE_DATE);
		Path<?> approvedPath = EntityQuery.getPath(join, PullRequestReview.ATTR_RESULT_APPROVED);
		return context.getBuilder().and(
				context.getBuilder().equal(userPath, user), 
				context.getBuilder().isNull(excludeDatePath), 
				context.getBuilder().isNull(approvedPath));
	}

	@Override
	public boolean matches(PullRequest request, User user) {
		PullRequestReview review = request.getReview(user);
		return review != null && review.getExcludeDate() == null && review.getResult() == null;
	}

	@Override
	public boolean needsLogin() {
		return true;
	}

	@Override
	public String toString() {
		return PullRequestQuery.getRuleName(PullRequestQueryLexer.ToBeReviewedByMe);
	}

}
