package com.pmease.gitplex.core.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jgit.lib.Constants;

import com.pmease.commons.git.GitUtils;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.hibernate.dao.EntityCriteria;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.manager.UserManager;
import com.pmease.gitplex.core.model.Comment;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.model.RepoAndBranch;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.core.model.Review;
import com.pmease.gitplex.core.model.Team;
import com.pmease.gitplex.core.model.User;
import com.pmease.gitplex.core.permission.ObjectPermission;
import com.pmease.gitplex.core.permission.operation.RepositoryOperation;

public class SecurityUtils extends org.apache.shiro.SecurityUtils {
	
	public static Collection<User> findUsersCan(Repository repository, RepositoryOperation operation) {
		Set<User> authorizedUsers = new HashSet<User>();
		for (User user: GitPlex.getInstance(Dao.class).query(EntityCriteria.of(User.class), 0, 0)) {
			if (user.asSubject().isPermitted(new ObjectPermission(repository, operation)))
				authorizedUsers.add(user);
		}
		return authorizedUsers;
	}

	public static boolean canModify(PullRequest request) {
		Repository repository = request.getTargetRepo();
		if (SecurityUtils.getSubject().isPermitted(ObjectPermission.ofRepoAdmin(repository))) {
			return true;
		} else {
			User currentUser = GitPlex.getInstance(UserManager.class).getCurrent();
			User submitter = request.getSubmitter();
			return currentUser != null && currentUser.equals(submitter);
		}
	}
	
	public static boolean canModify(Review review) {
		Repository repository = review.getUpdate().getRequest().getTargetRepo();
		if (SecurityUtils.getSubject().isPermitted(ObjectPermission.ofRepoAdmin(repository))) {
			return true;
		} else {
			return review.getReviewer().equals(GitPlex.getInstance(UserManager.class).getCurrent());
		}
	}

	public static boolean canModify(Comment comment) {
		User currentUser = GitPlex.getInstance(UserManager.class).getCurrent();
		if (currentUser == null) {
			return false;
		} else {
			if (currentUser.equals(comment.getUser())) {
				return true;
			} else {
				ObjectPermission adminPermission = ObjectPermission.ofRepoAdmin(comment.getRepository());
				return SecurityUtils.getSubject().isPermitted(adminPermission);
			}
		}
	}

	public static boolean canCreate(RepoAndBranch repoAndBranch) {
		return canCreate(repoAndBranch.getRepository(), repoAndBranch.getBranch());
	}
	
	public static boolean canCreate(Repository repository, String branch) {
		User currentUser = GitPlex.getInstance(UserManager.class).getCurrent();
		return currentUser != null 
				&& currentUser.asSubject().isPermitted(ObjectPermission.ofRepoPush(repository))	
				&& repository.getGateKeeper().checkRef(currentUser, repository, Constants.R_HEADS + branch).isPassed();
	}

	public static boolean canModify(RepoAndBranch repoAndBranch) {
		return canModify(repoAndBranch.getRepository(), GitUtils.branch2ref(repoAndBranch.getBranch()));
	}
	
	public static boolean canModify(Repository repository, String refName) {
		User currentUser = GitPlex.getInstance(UserManager.class).getCurrent();
		return currentUser != null 
				&& currentUser.asSubject().isPermitted(ObjectPermission.ofRepoPush(repository))	
				&& repository.getGateKeeper().checkRef(currentUser, repository, refName).isPassed();
	}
	
	public static boolean canManage(User account) {
		User currentUser = GitPlex.getInstance(UserManager.class).getCurrent();
		if (currentUser != null) {
			if (currentUser.isRoot())
				return true;
			
			for (Team team: currentUser.getTeams()) {
				if (team.isOwners() && team.getOwner().equals(account))
					return true;
			}
		}  
		return false;
	}
	
	public static boolean canPull(Repository repository) {
		return getSubject().isPermitted(ObjectPermission.ofRepoPull(repository));
	}
	
	public static boolean canPush(Repository repository) {
		return getSubject().isPermitted(ObjectPermission.ofRepoPush(repository));
	}
	
	public static boolean canManage(Repository repository) {
		return getSubject().isPermitted(ObjectPermission.ofRepoAdmin(repository));
	}

	public static boolean canManageSystem() {
		return getSubject().isPermitted(ObjectPermission.ofSystemAdmin());
	}
}
