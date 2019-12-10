package io.onedev.server.search.entity.pullrequest;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import io.onedev.server.model.PullRequest;

import io.onedev.server.search.entity.EntityCriteria;
import io.onedev.server.util.match.WildcardUtils;
import io.onedev.server.util.query.ProjectQueryConstants;
import io.onedev.server.util.query.PullRequestQueryConstants;

public class TargetProjectCriteria extends EntityCriteria<PullRequest> {

	private static final long serialVersionUID = 1L;
	
	private final String projectName;

	public TargetProjectCriteria(String projectName) {
		this.projectName = projectName;
	}

	@Override
	public Predicate getPredicate(Root<PullRequest> root, CriteriaBuilder builder) {
		Path<String> attribute = root
				.join(PullRequestQueryConstants.ATTR_TARGET_PROJECT, JoinType.INNER)
				.get(ProjectQueryConstants.ATTR_NAME);
		String normalized = projectName.toLowerCase().replace("*", "%");
		return builder.like(builder.lower(attribute), normalized);
	}

	@Override
	public boolean matches(PullRequest request) {
		return WildcardUtils.matchString(projectName.toLowerCase(), 
				request.getTargetProject().getName().toLowerCase());
	}

	@Override
	public String toString() {
		return PullRequestQuery.quote(PullRequestQueryConstants.FIELD_TARGET_PROJECT) + " " 
				+ PullRequestQuery.getRuleName(PullRequestQueryLexer.Is) + " " 
				+ PullRequestQuery.quote(projectName);
	}

}
