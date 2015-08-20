package com.pmease.gitplex.web.component.diff.blob.text;

import java.io.Serializable;

import com.pmease.gitplex.core.comment.InlineComment;

class CommentAndPos implements Serializable {

	private static final long serialVersionUID = 1L;

	InlineComment comment;
	
	int oldLineNo;
	
	int newLineNo;
}
