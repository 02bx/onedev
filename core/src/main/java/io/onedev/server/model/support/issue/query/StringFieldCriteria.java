package io.onedev.server.model.support.issue.query;

import java.util.Objects;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import io.onedev.server.model.Issue;
import io.onedev.server.model.IssueFieldUnary;

public class StringFieldCriteria extends FieldCriteria {

	private static final long serialVersionUID = 1L;

	private final String value;
	
	private final int operator;
	
	public StringFieldCriteria(String name, String value, int operator) {
		super(name);
		this.value = value;
		this.operator = operator;
	}

	public String getValue() {
		return value;
	}

	@Override
	public Predicate getPredicate(QueryBuildContext context) {
		Path<String> attribute = context.getJoin(getFieldName()).get(IssueFieldUnary.VALUE);
		if (operator == IssueQueryLexer.Is)
			return context.getBuilder().equal(attribute, value);
		else if (operator == IssueQueryLexer.IsNot)
			return context.getBuilder().notEqual(attribute, value);
		else if (operator == IssueQueryLexer.Contains)
			return context.getBuilder().like(attribute, "%" + value + "%");
		else
			return context.getBuilder().notLike(attribute, "%" + value + "%");
	}

	@Override
	public boolean matches(Issue issue) {
		String fieldValue = (String) getFieldValue(issue);
		if (operator == IssueQueryLexer.Is)
			return Objects.equals(fieldValue, value);
		else if (operator == IssueQueryLexer.IsNot)
			return !Objects.equals(fieldValue, value);
		else if (operator == IssueQueryLexer.Contains)
			return fieldValue != null && fieldValue.toLowerCase().contains(value.toLowerCase());
		else
			return fieldValue == null || !fieldValue.toLowerCase().contains(value.toLowerCase());
	}

	@Override
	public boolean needsLogin() {
		return false;
	}

	@Override
	public String toString() {
		return quote(getFieldName()) + " " + IssueQuery.getOperatorName(operator) + " " + quote(value);
	}

}
