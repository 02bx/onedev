package io.onedev.server.search.entity.issue;

import java.util.Objects;

import javax.annotation.Nullable;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;

import io.onedev.server.model.Build;
import io.onedev.server.model.Issue;
import io.onedev.server.model.IssueField;
import io.onedev.server.model.Project;

import io.onedev.server.search.entity.EntityQuery;
import io.onedev.server.util.query.IssueQueryConstants;

public class BuildFieldCriteria extends FieldCriteria {

	private static final long serialVersionUID = 1L;

	private final Build build;
	
	private final String value;
	
	public BuildFieldCriteria(String name, @Nullable Project project, String value) {
		super(name);
		build = EntityQuery.getBuild(project, value);
		this.value = value;
	}

	@Override
	protected Predicate getValuePredicate(Join<?, ?> field, CriteriaBuilder builder) {
		return builder.and(
				builder.equal(field.getParent().get(IssueQueryConstants.ATTR_PROJECT), build.getProject()),
				builder.equal(field.get(IssueField.ATTR_ORDINAL), build.getNumber()));
	}

	@Override
	public boolean matches(Issue issue) {
		Object fieldValue = issue.getFieldValue(getFieldName());
		return issue.getProject().equals(build.getProject()) && Objects.equals(fieldValue, build.getNumber());
	}

	@Override
	public String toString() {
		return IssueQuery.quote(getFieldName()) + " " 
				+ IssueQuery.getRuleName(IssueQueryLexer.Is) + " " 
				+ IssueQuery.quote(value);
	}

}
