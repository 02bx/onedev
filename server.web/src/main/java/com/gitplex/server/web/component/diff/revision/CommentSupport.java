package com.gitplex.server.web.component.diff.revision;

import java.io.Serializable;

import javax.annotation.Nullable;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.gitplex.server.core.entity.CodeComment;
import com.gitplex.server.core.entity.support.CommentPos;

public interface CommentSupport extends Serializable {
	
	@Nullable CommentPos getMark();
	
	@Nullable String getAnchor();
	
	void onMark(AjaxRequestTarget target, CommentPos mark);
	
	String getMarkUrl(CommentPos mark);
	
	String getCommentUrl(CodeComment comment);
	
	@Nullable CodeComment getOpenComment();

	void onCommentOpened(AjaxRequestTarget target, @Nullable CodeComment comment);
	
	void onAddComment(AjaxRequestTarget target, CommentPos commentPos);

}
