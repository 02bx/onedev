package io.onedev.server.search.entity.build;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import io.onedev.server.model.Build;

import io.onedev.server.search.entity.EntityCriteria;
import io.onedev.server.util.query.BuildQueryConstants;

public class RunningCriteria extends EntityCriteria<Build> {

	private static final long serialVersionUID = 1L;

	@Override
	public Predicate getPredicate(Root<Build> root, CriteriaBuilder builder) {
		Path<?> attribute = root.get(BuildQueryConstants.ATTR_STATUS);
		return builder.equal(attribute, Build.Status.RUNNING);
	}

	@Override
	public boolean matches(Build build) {
		return build.getStatus() == Build.Status.RUNNING;
	}

	@Override
	public String asString() {
		return BuildQuery.getRuleName(BuildQueryLexer.Running);
	}

}
