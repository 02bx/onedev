package com.gitplex.server.event.codecomment;

import java.util.Date;

import com.gitplex.server.model.User;
import com.gitplex.server.model.CodeComment;
import com.gitplex.server.model.PullRequest;
import com.gitplex.server.util.editable.annotation.Editable;

@Editable(name="created")
public class CodeCommentCreated extends CodeCommentEvent {

	public CodeCommentCreated(CodeComment comment, PullRequest request) {
		super(comment, request);
	}

	@Override
	public String getMarkdown() {
		return getComment().getContent();
	}

	@Override
	public User getUser() {
		return getComment().getUser();
	}

	@Override
	public Date getDate() {
		return getComment().getDate();
	}

}
