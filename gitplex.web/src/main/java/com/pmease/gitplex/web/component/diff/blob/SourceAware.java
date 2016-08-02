package com.pmease.gitplex.web.component.diff.blob;

import javax.annotation.Nullable;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.pmease.gitplex.core.entity.CodeComment;
import com.pmease.gitplex.core.entity.support.CommentPos;

public interface SourceAware {
	
	void onCommentDeleted(AjaxRequestTarget target, CodeComment comment);
	
	void onCommentClosed(AjaxRequestTarget target, CodeComment comment);

	void onCommentAdded(AjaxRequestTarget target, CodeComment comment);
	
	void mark(AjaxRequestTarget target, @Nullable CommentPos mark);

	void onUnblame(AjaxRequestTarget target);
}
