package io.onedev.server.event.codecomment;

import java.util.Date;

import io.onedev.server.model.CodeComment;
import io.onedev.server.model.PullRequest;
import io.onedev.server.model.User;
import io.onedev.server.web.editable.annotation.Editable;

@Editable(name="created")
public class CodeCommentAdded extends CodeCommentEvent {

	public CodeCommentAdded(CodeComment comment, PullRequest request) {
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
