package com.pmease.gitplex.core.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jgit.lib.ObjectId;

import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.hibernate.dao.EntityCriteria;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.entity.Comment;
import com.pmease.gitplex.core.entity.Depot;
import com.pmease.gitplex.core.entity.PullRequest;
import com.pmease.gitplex.core.entity.Review;
import com.pmease.gitplex.core.entity.Team;
import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.manager.AccountManager;
import com.pmease.gitplex.core.permission.ObjectPermission;
import com.pmease.gitplex.core.permission.operation.DepotOperation;

public class SecurityUtils extends org.apache.shiro.SecurityUtils {
	
	public static Collection<Account> findUsersCan(Depot depot, DepotOperation operation) {
		Set<Account> authorizedUsers = new HashSet<Account>();
		for (Account user: GitPlex.getInstance(Dao.class).query(EntityCriteria.of(Account.class), 0, 0)) {
			if (user.asSubject().isPermitted(new ObjectPermission(depot, operation)))
				authorizedUsers.add(user);
		}
		return authorizedUsers;
	}

	public static boolean canModify(PullRequest request) {
		Depot depot = request.getTargetDepot();
		if (SecurityUtils.getSubject().isPermitted(ObjectPermission.ofDepotAdmin(depot))) {
			return true;
		} else {
			Account currentUser = GitPlex.getInstance(AccountManager.class).getCurrent();
			Account submitter = request.getSubmitter();
			return currentUser != null && currentUser.equals(submitter);
		}
	}
	
	public static boolean canModify(Review review) {
		Depot depot = review.getUpdate().getRequest().getTargetDepot();
		if (SecurityUtils.getSubject().isPermitted(ObjectPermission.ofDepotAdmin(depot))) {
			return true;
		} else {
			return review.getReviewer().equals(GitPlex.getInstance(AccountManager.class).getCurrent());
		}
	}
	
	public static boolean canModify(Comment comment) {
		Account currentUser = GitPlex.getInstance(AccountManager.class).getCurrent();
		if (currentUser == null) {
			return false;
		} else {
			if (currentUser.equals(comment.getUser())) {
				return true;
			} else {
				ObjectPermission adminPermission = ObjectPermission.ofDepotAdmin(comment.getDepot());
				return SecurityUtils.getSubject().isPermitted(adminPermission);
			}
		}
	}

	public static boolean canPushRef(Depot depot, String refName, ObjectId oldCommit, ObjectId newCommit) {
		Account currentUser = GitPlex.getInstance(AccountManager.class).getCurrent();
		return currentUser != null 
				&& currentUser.asSubject().isPermitted(ObjectPermission.ofDepotPush(depot))	
				&& depot.getGateKeeper().checkPush(currentUser, depot, refName, oldCommit, newCommit).isPassed();
	}
	
	public static boolean canManage(Account account) {
		Account currentUser = GitPlex.getInstance(AccountManager.class).getCurrent();
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
	
	public static boolean canPull(Depot depot) {
		return getSubject().isPermitted(ObjectPermission.ofDepotPull(depot));
	}
	
	public static boolean canPush(Depot depot) {
		return getSubject().isPermitted(ObjectPermission.ofDepotPush(depot));
	}
	
	public static boolean canManage(Depot depot) {
		return getSubject().isPermitted(ObjectPermission.ofDepotAdmin(depot));
	}

	public static boolean canManageSystem() {
		return getSubject().isPermitted(ObjectPermission.ofSystemAdmin());
	}
}
