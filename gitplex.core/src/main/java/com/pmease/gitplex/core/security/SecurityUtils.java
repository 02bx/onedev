package com.pmease.gitplex.core.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jgit.lib.ObjectId;

import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.entity.Comment;
import com.pmease.gitplex.core.entity.Depot;
import com.pmease.gitplex.core.entity.PullRequest;
import com.pmease.gitplex.core.entity.Review;
import com.pmease.gitplex.core.manager.AccountManager;
import com.pmease.gitplex.core.security.privilege.DepotPrivilege;
import com.pmease.gitplex.core.security.privilege.Privilege;

public class SecurityUtils extends org.apache.shiro.SecurityUtils {
	
	public static Collection<Account> findUsersCan(Depot depot, DepotPrivilege operation) {
		Set<Account> authorizedUsers = new HashSet<Account>();
		for (Account user: GitPlex.getInstance(AccountManager.class).all()) {
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
			Account currentUser = getAccount();
			Account submitter = request.getSubmitter();
			return currentUser != null && currentUser.equals(submitter);
		}
	}

	public static Account getAccount() {
		return GitPlex.getInstance(AccountManager.class).getCurrent();
	}
	
	public static boolean canModify(Review review) {
		Depot depot = review.getUpdate().getRequest().getTargetDepot();
		if (SecurityUtils.getSubject().isPermitted(ObjectPermission.ofDepotAdmin(depot))) {
			return true;
		} else {
			return review.getReviewer().equals(getAccount());
		}
	}
	
	public static boolean canModify(Comment comment) {
		Account currentUser = getAccount();
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
		Account currentUser = getAccount();
		return currentUser != null 
				&& currentUser.asSubject().isPermitted(ObjectPermission.ofDepotWrite(depot))	
				&& depot.getGateKeeper().checkPush(currentUser, depot, refName, oldCommit, newCommit).isPassed();
	}
	
	public static boolean canManage(Account account) {
		return getSubject().isPermitted(ObjectPermission.ofAccountAdmin(account));
	}
	
	public static boolean canAccess(Account organization) {
		return getSubject().isPermitted(ObjectPermission.ofAccountAccess(organization));
	}
	
	public static boolean canRead(Depot depot) {
		return getSubject().isPermitted(ObjectPermission.ofDepotRead(depot));
	}
	
	public static boolean canWrite(Depot depot) {
		return getSubject().isPermitted(ObjectPermission.ofDepotWrite(depot));
	}
	
	public static boolean canManage(Depot depot) {
		return getSubject().isPermitted(ObjectPermission.ofDepotAdmin(depot));
	}

	public static boolean canManageSystem() {
		Account currentUser = getAccount();
		return currentUser != null && currentUser.isAdministrator();
	}

	public static boolean isGreater(Privilege greater, Privilege lesser) {
		return greater.can(lesser) && !lesser.can(greater);
	}
	
}
