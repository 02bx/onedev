package io.onedev.server.search.entity.build;

import javax.annotation.Nullable;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import io.onedev.server.model.Build;
import io.onedev.server.model.Project;
import io.onedev.server.model.PullRequest;
import io.onedev.server.model.PullRequestBuild;

import io.onedev.server.search.entity.EntityCriteria;
import io.onedev.server.search.entity.EntityQuery;
import io.onedev.server.util.query.BuildQueryConstants;

public class RequiredByPullRequestCriteria extends EntityCriteria<Build> {

	private static final long serialVersionUID = 1L;

	private final PullRequest request;
	
	private final String value;
	
	public RequiredByPullRequestCriteria(@Nullable Project project, String value) {
		request = EntityQuery.getPullRequest(project, value);
		this.value = value;
	}

	@Override
	public Predicate getPredicate(Root<Build> root, CriteriaBuilder builder) {
		From<?, ?> join = root.join(BuildQueryConstants.ATTR_PULL_REQUEST_BUILDS, JoinType.LEFT);
		
		return builder.and(
				builder.equal(root.get(BuildQueryConstants.ATTR_PROJECT), request.getTargetProject()),
				builder.equal(join.get(PullRequestBuild.ATTR_REQUEST), request), 
				builder.equal(join.get(PullRequestBuild.ATTR_REQUIRED), true)); 
	}

	@Override
	public boolean matches(Build build) {
		return build.getProject().equals(request.getTargetProject()) 
				&& build.getPullRequestBuilds().stream().anyMatch(it -> it.getRequest().equals(request) && it.isRequired());
	}

	@Override
	public String toString() {
		return BuildQuery.getRuleName(BuildQueryLexer.RequiredByPullRequest) + " " + BuildQuery.quote(value);
	}

}
