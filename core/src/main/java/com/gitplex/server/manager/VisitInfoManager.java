package com.gitplex.server.manager;

import java.util.Date;

import javax.annotation.Nullable;

import com.gitplex.server.model.User;
import com.gitplex.server.model.CodeComment;
import com.gitplex.server.model.Project;
import com.gitplex.server.model.PullRequest;

public interface VisitInfoManager {
	
	void visit(User user, Project project);
	
	void visit(User user, PullRequest request);
	
	void visit(User user, CodeComment comment);
	
	@Nullable
	Date getVisitDate(User user, Project project);
	
	@Nullable
	Date getVisitDate(User user, PullRequest request);
	
	@Nullable
	Date getVisitDate(User user, CodeComment comment);
	
}
