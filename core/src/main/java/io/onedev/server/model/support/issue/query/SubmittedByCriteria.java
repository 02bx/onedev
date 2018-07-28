package io.onedev.server.model.support.issue.query;

import java.util.Objects;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import io.onedev.server.model.Issue;
import io.onedev.server.model.Project;
import io.onedev.server.model.User;
import io.onedev.server.util.query.QueryBuildContext;

public class SubmittedByCriteria extends IssueCriteria {

	private static final long serialVersionUID = 1L;

	private final User value;
	
	public SubmittedByCriteria(User value) {
		this.value = value;
	}

	@Override
	public Predicate getPredicate(Project project, QueryBuildContext<Issue> context) {
		Path<User> attribute = context.getRoot().get(Issue.FIELD_PATHS.get(Issue.FIELD_SUBMITTER));
		return context.getBuilder().equal(attribute, value);
	}

	@Override
	public boolean matches(Issue issue) {
		return Objects.equals(issue.getSubmitter(), value);
	}

	@Override
	public boolean needsLogin() {
		return false;
	}

	@Override
	public String toString() {
		return IssueQuery.getRuleName(IssueQueryLexer.SubmittedBy) + " " + IssueQuery.quote(value.getName());
	}

}
