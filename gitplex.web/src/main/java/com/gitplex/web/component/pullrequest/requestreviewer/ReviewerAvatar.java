package com.gitplex.web.component.pullrequest.requestreviewer;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.gitplex.core.GitPlex;
import com.gitplex.core.entity.Account;
import com.gitplex.core.entity.PullRequest;
import com.gitplex.core.entity.PullRequestReviewInvitation;
import com.gitplex.core.manager.PullRequestReviewInvitationManager;
import com.gitplex.core.security.SecurityUtils;
import com.gitplex.web.component.avatar.RemoveableAvatar;
import com.gitplex.web.model.EntityModel;
import com.gitplex.web.model.UserModel;

@SuppressWarnings("serial")
public class ReviewerAvatar extends RemoveableAvatar {

	private final PullRequestReviewInvitation invitation;
	
	private final IModel<PullRequest> requestModel;
	
	public ReviewerAvatar(String id, PullRequestReviewInvitation invitation) {
		super(id, new UserModel(invitation.getUser()));
		
		this.invitation = invitation;
		requestModel = new EntityModel<PullRequest>(invitation.getRequest());		
	}

	@Override
	protected void onConfigure() {
		super.onConfigure();

		PullRequest request = requestModel.getObject();
		setEnabled(request.isOpen() && SecurityUtils.canModify(request));
	}

	@Override
	public void onClick(AjaxRequestTarget target) {
		Date now = new Date();
		PullRequest request = requestModel.getObject();
		Set<Account> prevInvited = new HashSet<>();
		for (PullRequestReviewInvitation each: request.getReviewInvitations()) {
			if (each.getStatus() != PullRequestReviewInvitation.Status.EXCLUDED)
				prevInvited.add(each.getUser());
			if (each.getUser().equals(invitation.getUser())) {
				each.setStatus(PullRequestReviewInvitation.Status.EXCLUDED);
				each.setDate(new Date());
			}
		}
		request.checkGates(true);

		Set<Account> nowInvited = new HashSet<>();
		for (PullRequestReviewInvitation each: request.getReviewInvitations()) {
			if (each.getStatus() != PullRequestReviewInvitation.Status.EXCLUDED)
				nowInvited.add(each.getUser());
		}
		
		if (nowInvited.contains(invitation.getUser())) {
			getSession().warn("Reviewer '" + invitation.getUser().getDisplayName() 
					+ "' is required by gatekeeper and can not be removed");
		} else {
			nowInvited.removeAll(prevInvited);
			if (!nowInvited.isEmpty()) {
				getSession().warn("Reviewer '" + invitation.getUser().getDisplayName() 
						+ "' is removed and user '" + nowInvited.iterator().next().getDisplayName() 
						+ "' is added as reviewer automatically to satisfy gatekeeper requirement.");
			}
		}
		if (!request.isNew()) {
			PullRequestReviewInvitationManager reviewInvitationManager = 
					GitPlex.getInstance(PullRequestReviewInvitationManager.class);
			reviewInvitationManager.update(request.getReviewInvitations(), now);
		}
	}

	@Override
	protected void onDetach() {
		requestModel.detach();
		
		super.onDetach();
	}

}
