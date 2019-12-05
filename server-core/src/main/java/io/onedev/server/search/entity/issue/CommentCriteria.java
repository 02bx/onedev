package io.onedev.server.search.entity.issue;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import io.onedev.server.model.Issue;
import io.onedev.server.model.IssueComment;

import io.onedev.server.util.query.IssueQueryConstants;

public class CommentCriteria extends IssueCriteria {

	private static final long serialVersionUID = 1L;

	private final String value;
	
	public CommentCriteria(String value) {
		this.value = value;
	}

	@Override
	public Predicate getPredicate(Root<Issue> root, CriteriaBuilder builder) {
		From<?, ?> join = root.join(IssueQueryConstants.ATTR_COMMENTS, JoinType.LEFT);
		Path<String> attribute = join.get(IssueComment.PATH_CONTENT);
		return builder.like(builder.lower(attribute), "%" + value.toLowerCase() + "%");
	}

	@Override
	public boolean matches(Issue issue) {
		for (IssueComment comment: issue.getComments()) {
			if (comment.getContent().toLowerCase().contains(value.toLowerCase()))
				return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return IssueQuery.quote(IssueQueryConstants.FIELD_COMMENT) + " " 
				+ IssueQuery.getRuleName(IssueQueryLexer.Contains) + " " + IssueQuery.quote(value);
	}

}
