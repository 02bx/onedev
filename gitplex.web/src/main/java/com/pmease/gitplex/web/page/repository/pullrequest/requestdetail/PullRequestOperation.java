package com.pmease.gitplex.web.page.repository.pullrequest.requestdetail;

import javax.annotation.Nullable;

import com.pmease.commons.git.Git;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.manager.PullRequestManager;
import com.pmease.gitplex.core.manager.ReviewManager;
import com.pmease.gitplex.core.manager.UserManager;
import com.pmease.gitplex.core.model.IntegrationPreview;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.model.PullRequest.Status;
import com.pmease.gitplex.core.model.Review;
import com.pmease.gitplex.core.model.ReviewInvitation;
import com.pmease.gitplex.core.model.User;
import com.pmease.gitplex.core.permission.ObjectPermission;
import com.pmease.gitplex.core.security.SecurityUtils;

public enum PullRequestOperation {
	INTEGRATE {

		@Override
		public boolean canOperate(PullRequest request) {
			if (!SecurityUtils.getSubject().isPermitted(
					ObjectPermission.ofRepoPush(request.getTargetRepo()))) {
				return false;
			} else {
				return GitPlex.getInstance(PullRequestManager.class).canIntegrate(request);
			}
		}

		@Override
		public void operate(PullRequest request, String comment) {
			GitPlex.getInstance(PullRequestManager.class).integrate(request, comment);
		}
		
	},
	DISCARD {

		@Override
		public boolean canOperate(PullRequest request) {
			if (!SecurityUtils.canModify(request))
				return false;
			else 
				return request.isOpen();
		}

		@Override
		public void operate(PullRequest request, String comment) {
			GitPlex.getInstance(PullRequestManager.class).discard(request, comment);
		}
		
	},
	APPROVE {

		@Override
		public boolean canOperate(PullRequest request) {
			return canReview(request);
		}

		@Override
		public void operate(PullRequest request, String comment) {
			User user = GitPlex.getInstance(UserManager.class).getCurrent();
			GitPlex.getInstance(ReviewManager.class).review(
					request, user, Review.Result.APPROVE, comment);
		}		
		
	},
	DISAPPROVE {

		@Override
		public boolean canOperate(PullRequest request) {
			return canReview(request);
		}

		@Override
		public void operate(PullRequest request, String comment) {
			User user = GitPlex.getInstance(UserManager.class).getCurrent();
			GitPlex.getInstance(ReviewManager.class).review(
					request, user, Review.Result.DISAPPROVE, comment);
		}
		
	},
	REOPEN {

		@Override
		public boolean canOperate(PullRequest request) {
			PullRequestManager pullRequestManager = GitPlex.getInstance(PullRequestManager.class);
			if (request.isOpen() 
					|| !SecurityUtils.canModify(request)
					|| request.getTarget().getHead(false) == null
					|| request.getSourceRepo() == null 
					|| request.getSource().getHead(false) == null
					|| pullRequestManager.findOpen(request.getTarget(), request.getSource()) != null) {
				return false;
			}
			
			// now check if source branch is integrated into target branch
			Git git = request.getTargetRepo().git();
			String sourceHead = request.getSource().getHead();
			return git.parseRevision(sourceHead, false) == null 
					|| !git.isAncestor(sourceHead, request.getTarget().getHead());
		}

		@Override
		public void operate(PullRequest request, String comment) {
			GitPlex.getInstance(PullRequestManager.class).reopen(request, comment);
		}
		
	},
	DELETE_SOURCE_BRANCH {

		@Override
		public void operate(PullRequest request, String comment) {
			GitPlex.getInstance(PullRequestManager.class).deleteSourceBranch(request);
		}

		@Override
		public boolean canOperate(PullRequest request) {
			IntegrationPreview preview = request.getLastIntegrationPreview();
			PullRequestManager pullRequestManager = GitPlex.getInstance(PullRequestManager.class);
			return request.getStatus() == Status.INTEGRATED 
					&& request.getSourceRepo() != null		
					&& request.getSource().getHead(false) != null
					&& !request.getSource().isDefault()
					&& preview != null
					&& (request.getSource().getHead().equals(preview.getRequestHead()) 
							|| request.getSource().getHead().equals(preview.getIntegrated()))
					&& SecurityUtils.canModify(request)
					&& SecurityUtils.canModify(request.getSource())
					&& pullRequestManager.queryOpenTo(request.getSource(), null).isEmpty();
		}
		
	}, 
	RESTORE_SOURCE_BRANCH {

		@Override
		public void operate(PullRequest request, String comment) {
			GitPlex.getInstance(PullRequestManager.class).restoreSourceBranch(request);
		}

		@Override
		public boolean canOperate(PullRequest request) {
			return request.getSourceRepo() != null && request.getSource().getHead(false) == null 
					&& SecurityUtils.canModify(request) && SecurityUtils.canCreate(request.getSource());
		}
		
	};
	
	private static boolean canReview(PullRequest request) {
		User user = GitPlex.getInstance(UserManager.class).getCurrent();
		
		// call request.getStatus() in order to trigger generation of review
		// integrations which will be used in else condition 
		if (user == null  
				|| request.getStatus() == PullRequest.Status.INTEGRATED 
				|| request.getStatus() == PullRequest.Status.DISCARDED
				|| request.isReviewEffective(user)) { 
			return false;
		} else {
			for (ReviewInvitation invitation: request.getReviewInvitations()) {
				if (invitation.isPreferred() && invitation.getReviewer().equals(user))
					return true;
			}
			return false;
		}
	}

	public abstract void operate(PullRequest request, @Nullable String comment);
	
	public abstract boolean canOperate(PullRequest request);	
}
