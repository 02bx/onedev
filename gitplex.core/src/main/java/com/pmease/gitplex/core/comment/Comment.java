package com.pmease.gitplex.core.comment;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.core.model.User;

public interface Comment extends Serializable {
	User getUser();
	
	Date getDate();
	
	String getContent();
	
	void saveContent(String content);
	
	boolean isResolved();
	
	void resolve(boolean resolved);
	
	Repository getRepository();
	
	void delete();
	
	Collection<? extends CommentReply> getReplies();
	
	CommentReply addReply(String content);
}
