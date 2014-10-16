package com.pmease.gitplex.core.manager;

import java.util.Collection;

import com.google.inject.ImplementedBy;
import com.pmease.gitplex.core.comment.Comment;
import com.pmease.gitplex.core.manager.impl.DefaultAuthorizationManager;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.core.model.User;
import com.pmease.gitplex.core.model.PullRequestVote;
import com.pmease.gitplex.core.permission.operation.GeneralOperation;

@ImplementedBy(DefaultAuthorizationManager.class)
public interface AuthorizationManager {
	
	Collection<User> listAuthorizedUsers(Repository repository, GeneralOperation operation);
	
	boolean canModify(PullRequest request);
	
	boolean canModify(PullRequestVote vote);
	
	boolean canModify(Comment comment);
}
