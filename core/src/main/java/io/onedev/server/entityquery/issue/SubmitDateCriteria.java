package io.onedev.server.entityquery.issue;

import java.util.Date;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import io.onedev.server.entityquery.QueryBuildContext;
import io.onedev.server.model.Issue;
import io.onedev.server.model.Project;
import io.onedev.server.model.support.issue.IssueConstants;
import io.onedev.server.entityquery.issue.IssueQueryLexer;

public class SubmitDateCriteria extends IssueCriteria {

	private static final long serialVersionUID = 1L;

	private final int operator;
	
	private final Date value;
	
	private final String rawValue;
	
	public SubmitDateCriteria(Date value, String rawValue, int operator) {
		this.operator = operator;
		this.value = value;
		this.rawValue = rawValue;
	}

	@Override
	public Predicate getPredicate(Project project, QueryBuildContext<Issue> context) {
		Path<Date> attribute = context.getRoot().get(IssueConstants.ATTR_SUBMIT_DATE);
		if (operator == IssueQueryLexer.IsBefore)
			return context.getBuilder().lessThan(attribute, value);
		else
			return context.getBuilder().greaterThan(attribute, value);
	}

	@Override
	public boolean matches(Issue issue) {
		if (operator == IssueQueryLexer.IsBefore)
			return issue.getSubmitDate().before(value);
		else
			return issue.getSubmitDate().after(value);
	}

	@Override
	public boolean needsLogin() {
		return false;
	}

	@Override
	public String toString() {
		return IssueQuery.quote(IssueConstants.FIELD_SUBMIT_DATE) + " " + IssueQuery.getRuleName(operator) + " " + IssueQuery.quote(rawValue);
	}

}
