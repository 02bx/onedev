package io.onedev.server.entityquery.codecomment;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import org.eclipse.jgit.lib.ObjectId;

import io.onedev.server.entityquery.EntityCriteria;
import io.onedev.server.entityquery.QueryBuildContext;
import io.onedev.server.model.CodeComment;
import io.onedev.server.model.Project;
import io.onedev.server.model.support.codecomment.CodeCommentConstants;

public class CommitCriteria extends EntityCriteria<CodeComment>  {

	private static final long serialVersionUID = 1L;

	private final ObjectId value;
	
	public CommitCriteria(ObjectId value) {
		this.value = value;
	}

	@Override
	public Predicate getPredicate(Project project, QueryBuildContext<CodeComment> context) {
		Path<?> attribute = CodeCommentQuery.getPath(context.getRoot(), CodeCommentConstants.ATTR_COMMIT);
		return context.getBuilder().equal(attribute, value);
	}

	@Override
	public boolean matches(CodeComment comment) {
		return comment.getMarkPos().getCommit().equals(value);
	}

	@Override
	public boolean needsLogin() {
		return false;
	}

	@Override
	public String toString() {
		return CodeCommentQuery.quote(CodeCommentConstants.FIELD_COMMIT) + " " + CodeCommentQuery.getRuleName(CodeCommentQueryLexer.Is) + " " + CodeCommentQuery.quote(value.name());
	}

}
