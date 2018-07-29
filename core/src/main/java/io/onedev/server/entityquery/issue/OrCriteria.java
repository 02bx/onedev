package io.onedev.server.entityquery.issue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.criteria.Predicate;

import io.onedev.server.entityquery.OrCriteriaHelper;
import io.onedev.server.entityquery.ParensAware;
import io.onedev.server.entityquery.QueryBuildContext;
import io.onedev.server.model.Issue;
import io.onedev.server.model.Project;
import io.onedev.server.web.page.project.issues.workflowreconcile.UndefinedFieldValue;

public class OrCriteria extends IssueCriteria implements ParensAware {
	
	private static final long serialVersionUID = 1L;

	private final List<IssueCriteria> criterias;
	
	public OrCriteria(List<IssueCriteria> criterias) {
		this.criterias = criterias;
	}

	@Override
	public Predicate getPredicate(Project project, QueryBuildContext<Issue> context) {
		return new OrCriteriaHelper<Issue>(criterias).getPredicate(project, context);
	}

	@Override
	public boolean matches(Issue issue) {
		return new OrCriteriaHelper<Issue>(criterias).matches(issue);
	}

	@Override
	public boolean needsLogin() {
		return new OrCriteriaHelper<Issue>(criterias).needsLogin();
	}

	@Override
	public String toString() {
		return new OrCriteriaHelper<Issue>(criterias).toString();
	}
	
	@Override
	public Collection<String> getUndefinedStates(Project project) {
		List<String> undefinedStates = new ArrayList<>();
		for (IssueCriteria criteria: criterias)
			undefinedStates.addAll(criteria.getUndefinedStates(project));
		return undefinedStates;
	}
	
	@Override
	public void onRenameState(String oldState, String newState) {
		for (IssueCriteria criteria: criterias)
			criteria.onRenameState(oldState, newState);
	}
	
	@Override
	public Collection<String> getUndefinedFields(Project project) {
		Set<String> undefinedFields = new HashSet<>();
		for (IssueCriteria criteria: criterias)
			undefinedFields.addAll(criteria.getUndefinedFields(project));
		return undefinedFields;
	}

	@Override
	public void onRenameField(String oldField, String newField) {
		for (IssueCriteria criteria: criterias)
			criteria.onRenameField(oldField, newField);
	}
	
	@Override
	public boolean onDeleteField(String fieldName) {
		for (Iterator<IssueCriteria> it = criterias.iterator(); it.hasNext();) {
			if (it.next().onDeleteField(fieldName))
				it.remove();
		}
		return criterias.isEmpty();
	}
	
	@Override
	public Collection<UndefinedFieldValue> getUndefinedFieldValues(Project project) {
		Set<UndefinedFieldValue> undefinedFieldValues = new HashSet<>();
		for (IssueCriteria criteria: criterias)
			undefinedFieldValues.addAll(criteria.getUndefinedFieldValues(project));
		return undefinedFieldValues;
	}
	
	@Override
	public void onRenameFieldValue(String fieldName, String oldValue, String newValue) {
		for (IssueCriteria criteria: criterias)
			criteria.onRenameFieldValue(fieldName, oldValue, newValue);
	}

	@Override
	public boolean onDeleteFieldValue(String fieldName, String fieldValue) {
		for (Iterator<IssueCriteria> it = criterias.iterator(); it.hasNext();) {
			if (it.next().onDeleteFieldValue(fieldName, fieldValue))
				it.remove();
		}
		return criterias.isEmpty();
	}

}
