package io.onedev.server.search.entity.issue;

import java.util.Date;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import io.onedev.server.model.Issue;
import io.onedev.server.model.Project;
import io.onedev.server.model.User;
import io.onedev.server.search.entity.QueryBuildContext;
import io.onedev.server.util.IssueConstants;

public class UpdateDateCriteria extends IssueCriteria {

	private static final long serialVersionUID = 1L;

	private final int operator;
	
	private final Date value;
	
	private final String rawValue;
	
	public UpdateDateCriteria(Date value, String rawValue, int operator) {
		this.operator = operator;
		this.value = value;
		this.rawValue = rawValue;
	}

	@Override
	public Predicate getPredicate(Project project, QueryBuildContext<Issue> context, User user) {
		Path<Date> attribute = IssueQuery.getPath(context.getRoot(), IssueConstants.ATTR_UPDATE_DATE);
		if (operator == IssueQueryLexer.IsBefore)
			return context.getBuilder().lessThan(attribute, value);
		else
			return context.getBuilder().greaterThan(attribute, value);
	}

	@Override
	public boolean matches(Issue issue, User user) {
		if (operator == IssueQueryLexer.IsBefore)
			return issue.getUpdateDate().before(value);
		else
			return issue.getUpdateDate().after(value);
	}

	@Override
	public boolean needsLogin() {
		return false;
	}

	@Override
	public String toString() {
		return IssueQuery.quote(IssueConstants.FIELD_UPDATE_DATE) + " " + IssueQuery.getRuleName(operator) + " " + IssueQuery.quote(rawValue);
	}

}
