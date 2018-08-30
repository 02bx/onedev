package io.onedev.server.search.entity.codecomment;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import org.eclipse.jgit.lib.ObjectId;

import io.onedev.server.search.entity.codecomment.CodeCommentQueryLexer;
import io.onedev.server.model.CodeComment;
import io.onedev.server.model.Project;
import io.onedev.server.model.User;
import io.onedev.server.model.support.codecomment.CodeCommentConstants;
import io.onedev.server.search.entity.EntityCriteria;
import io.onedev.server.search.entity.QueryBuildContext;

public class CommitCriteria extends EntityCriteria<CodeComment>  {

	private static final long serialVersionUID = 1L;

	private final ObjectId value;
	
	public CommitCriteria(ObjectId value) {
		this.value = value;
	}

	@Override
	public Predicate getPredicate(Project project, QueryBuildContext<CodeComment> context, User user) {
		Path<?> attribute = CodeCommentQuery.getPath(context.getRoot(), CodeCommentConstants.ATTR_COMMIT);
		return context.getBuilder().equal(attribute, value);
	}

	@Override
	public boolean matches(CodeComment comment, User user) {
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
