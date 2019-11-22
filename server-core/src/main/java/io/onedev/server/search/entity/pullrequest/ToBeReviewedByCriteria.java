package io.onedev.server.search.entity.pullrequest;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import io.onedev.server.model.PullRequest;
import io.onedev.server.model.PullRequestReview;
import io.onedev.server.model.User;
import io.onedev.server.search.entity.EntityQuery;
import io.onedev.server.util.PullRequestConstants;

public class ToBeReviewedByCriteria extends PullRequestCriteria {

	private static final long serialVersionUID = 1L;

	private final User user;
	
	private final String value;
	
	public ToBeReviewedByCriteria(String value) {
		user = EntityQuery.getUser(value);
		this.value = value;
	}
	
	@Override
	public Predicate getPredicate(Root<PullRequest> root, CriteriaBuilder builder, User user) {
		From<?, ?> join = root.join(PullRequestConstants.ATTR_REVIEWS, JoinType.LEFT);
		Path<?> userPath = EntityQuery.getPath(join, PullRequestReview.ATTR_USER);
		Path<?> excludeDatePath = EntityQuery.getPath(join, PullRequestReview.ATTR_EXCLUDE_DATE);
		Path<?> approvedPath = EntityQuery.getPath(join, PullRequestReview.ATTR_RESULT_APPROVED);
		return builder.and(
				builder.equal(userPath, this.user), 
				builder.isNull(excludeDatePath), 
				builder.isNull(approvedPath));
	}

	@Override
	public boolean matches(PullRequest request, User user) {
		PullRequestReview review = request.getReview(this.user);
		return review != null && review.getExcludeDate() == null && review.getResult() == null;
	}

	@Override
	public boolean needsLogin() {
		return false;
	}

	@Override
	public String toString() {
		return PullRequestQuery.getRuleName(PullRequestQueryLexer.ToBeReviewedBy) 
				+ " " + PullRequestQuery.quote(value);
	}

}
