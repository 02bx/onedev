package io.onedev.server.entityquery.issue;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import io.onedev.server.entityquery.QueryBuildContext;
import io.onedev.server.model.Issue;
import io.onedev.server.model.Project;
import io.onedev.server.model.support.issue.IssueConstants;
import io.onedev.server.entityquery.issue.IssueQueryLexer;

public class VoteCountCriteria extends IssueCriteria {

	private static final long serialVersionUID = 1L;

	private final int operator;
	
	private final int value;
	
	public VoteCountCriteria(int value, int operator) {
		this.operator = operator;
		this.value = value;
	}

	@Override
	public Predicate getPredicate(Project project, QueryBuildContext<Issue> context) {
		Path<Integer> attribute = context.getRoot().get(IssueConstants.ATTR_VOTE_COUNT);
		if (operator == IssueQueryLexer.Is)
			return context.getBuilder().equal(attribute, value);
		else if (operator == IssueQueryLexer.IsGreaterThan)
			return context.getBuilder().greaterThan(attribute, value);
		else
			return context.getBuilder().lessThan(attribute, value);
	}

	@Override
	public boolean matches(Issue issue) {
		if (operator == IssueQueryLexer.Is)
			return issue.getVoteCount() == value;
		else if (operator == IssueQueryLexer.IsGreaterThan)
			return issue.getVoteCount() > value;
		else
			return issue.getVoteCount() < value;
	}

	@Override
	public boolean needsLogin() {
		return false;
	}

	@Override
	public String toString() {
		return IssueQuery.quote(IssueConstants.FIELD_VOTE_COUNT) + " " + IssueQuery.getRuleName(operator) + " " + IssueQuery.quote(String.valueOf(value));
	}

}
