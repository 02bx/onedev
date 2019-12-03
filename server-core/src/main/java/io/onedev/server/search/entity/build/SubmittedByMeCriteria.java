package io.onedev.server.search.entity.build;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import io.onedev.server.model.Build;
import io.onedev.server.model.User;
import io.onedev.server.search.entity.EntityCriteria;
import io.onedev.server.util.query.BuildQueryConstants;

public class SubmittedByMeCriteria extends EntityCriteria<Build> {

	private static final long serialVersionUID = 1L;

	@Override
	public Predicate getPredicate(Root<Build> root, CriteriaBuilder builder, User user) {
		if (user != null) {
			Path<User> attribute = root.get(BuildQueryConstants.ATTR_SUBMITTER);
			return builder.equal(attribute, user);
		} else {
			return builder.disjunction();
		}
	}

	@Override
	public boolean matches(Build build, User user) {
		if (user != null)
			return user.equals(build.getSubmitter());
		else
			return false;
	}

	@Override
	public boolean needsLogin() {
		return true;
	}

	@Override
	public String toString() {
		return BuildQuery.getRuleName(BuildQueryLexer.SubmittedByMe);
	}

}
