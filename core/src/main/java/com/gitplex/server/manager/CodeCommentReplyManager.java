package com.gitplex.server.manager;

import com.gitplex.server.entity.CodeCommentReply;
import com.gitplex.server.persistence.dao.EntityManager;

public interface CodeCommentReplyManager extends EntityManager<CodeCommentReply> {

	void save(CodeCommentReply reply, boolean callListeners);
	
}
