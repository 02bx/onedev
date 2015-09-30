package com.pmease.gitplex.core.manager;

import com.pmease.commons.git.BlobIdent;
import com.pmease.gitplex.core.model.Comment;
import com.pmease.gitplex.core.model.PullRequest;

public interface CommentManager {

	/**
	 * Update specified inline comment to make sure it is up to date with latest pull request update
	 * 
	 * @param comment
	 *			comment to be updated 
	 */
	void updateInlineInfo(Comment comment);
	
	void save(Comment comment, boolean notify);
	
	Comment addInline(PullRequest request, BlobIdent blobInfo, BlobIdent compareWith, int line, String content);
}
