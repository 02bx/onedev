package io.onedev.server.manager;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import io.onedev.server.model.Issue;
import io.onedev.server.model.Milestone;
import io.onedev.server.model.Project;
import io.onedev.server.model.support.issue.query.IssueCriteria;
import io.onedev.server.model.support.issue.query.IssueQuery;
import io.onedev.server.persistence.dao.EntityManager;
import io.onedev.server.web.page.project.issues.workflowreconcile.InvalidFieldResolution;
import io.onedev.server.web.page.project.issues.workflowreconcile.UndefinedFieldValue;
import io.onedev.server.web.page.project.issues.workflowreconcile.UndefinedFieldValueResolution;
import io.onedev.server.web.page.project.issues.workflowreconcile.UndefinedStateResolution;

public interface IssueManager extends EntityManager<Issue> {
	
    @Nullable
    Issue find(Project target, long number);
    
	void open(Issue issue, Serializable fieldBean);
	
	List<Issue> query(Project project, IssueQuery issueQuery, int firstResult, int maxResults);
	
	int count(Project project, @Nullable IssueCriteria issueCriteria);
	
	List<Issue> query(Project project, @Nullable String term, int count);
	
	Collection<String> getUndefinedStates(Project project);
	
	void fixUndefinedStates(Project project, Map<String, UndefinedStateResolution> resolutions);
	
	Map<String, String> getInvalidFields(Project project);
	
	void fixInvalidFields(Project project, Map<String, InvalidFieldResolution> resolutions);
	
	Map<String, String> getUndefinedFieldValues(Project project);
	
	void fixUndefinedFieldValues(Project project, Map<UndefinedFieldValue, UndefinedFieldValueResolution> resolutions);
	
	void fixFieldValueOrders(Project project);
	
	void batchUpdate(Project project, IssueQuery issueQuery, Set<String> updateFields, String state, 
			@Nullable Milestone milestone, Serializable fieldBean);
}
